package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.AmazonReader.keyword;
import static com.mycompany.amazonlogger.AmazonReader.props;
import com.mycompany.amazonlogger.PropertiesFile.Property;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 *
 * @author dan
 */
public class ParseOrders {

    private static int iQtyPossible = 0;
    
    public ParseOrders() {
    }

    /**
     * parses the clipboard data line by line to extract the order information from a "Your Orders" clip.
     * 
     * @param line    - the line previously caught and passed to this module to execute
     * @param keyType - the Key type associated with the line
     * 
     * @return an array of AmazonOrder entries that were extracted from the clip
     * 
     * @throws ParserException 
     */
    public ArrayList<AmazonOrder> parseOrders (String line, Keyword.KeyTyp keyType) throws ParserException {
        String descript1 = null;
        Keyword.KeywordEntry keywordInfo = null;
        Keyword.KeywordEntry savedKey = null;
        boolean bReadData = false;
        boolean bSkipRead = false;
        LocalDate delivered = null;
        LocalDate lastDeliveryDate = null;
        int itemCount = 0;

        // init the array list of Amazon transactions
        ArrayList<AmazonOrder> amazonList = new ArrayList<>();
            
        // create an entry for the first item
        AmazonOrder newOrder = new AmazonOrder();
        AmazonItem newItem = newOrder.addNewItem();

        // if a command was passed in, use it on the current line
        if (keyType != Keyword.KeyTyp.NONE) {
            keywordInfo = keyword.findOrdersKey (keyType);
            savedKey = keywordInfo;
            bSkipRead = true;
        }
        
        do {
            // if we don't have a pending command in the queue, get next line from clipboard
            if (!bSkipRead) {
                line = ClipboardReader.webClipGetLine();
                if (line == null)
                    break;
                if (line.isBlank())
                    continue;

                // parse line to check for command
                savedKey = keyword.getKeyword(line, Keyword.ClipTyp.ORDERS);
                if (savedKey.eKeyId == Keyword.KeyTyp.END_OF_RECORD) {
                    // if an entry is already in process, it must have been completed, so add completed order to list
                    if (newOrder.isOrderDefined()) {
                        amazonList.add(newOrder);
                        frame.outputInfoMsg (UIFrame.STATUS_DEBUG, "* Added new ORDER entry to AMAZON LIST");
                    }

                    // exit if we completed loop
                    newOrder = null;
                    frame.outputInfoMsg (UIFrame.STATUS_PARSER, "END OF ORDER (" + amazonList.size() + ")");
                    frame.outputInfoMsg (UIFrame.STATUS_PARSER, "END OF LIST");
                    break;
                }
            }

            // reset the skip read flag
            bSkipRead = false;
            
            // see if we have a pending command (next line contains the data)
            if (savedKey == null) {
                savedKey = keyword.makeKeyword("", Keyword.KeyTyp.NONE, Keyword.DataTyp.NONE);
            }
            if (keywordInfo == null || keywordInfo.eKeyId == Keyword.KeyTyp.NONE) {
                keywordInfo = savedKey;
                if (keywordInfo.eKeyId == Keyword.KeyTyp.NONE) {
                    continue;
                }
            } else {
                frame.outputInfoMsg (UIFrame.STATUS_DEBUG, "  KeyTyp." + savedKey.eKeyId + " (unparsed line): " + line);
            }

            // now run the state machine...

            // these entries can be processed immediately because the information they need
            // is contained in the same line as the keyword (or they don't need any information)
            if (keywordInfo.eDataType != Keyword.DataTyp.NEXTLINE) {
                // if data was inline with the command, advance the string
                // past the keyword to access the data.
                if (keywordInfo.eDataType == Keyword.DataTyp.INLINE) {
                    line = line.substring(keywordInfo.strKeyword.length());
                }
                frame.outputInfoMsg (UIFrame.STATUS_INFO, "* Executing KeyTyp." + keywordInfo.eKeyId.name() + " as " + keywordInfo.eDataType.name());
                
                switch (keywordInfo.eKeyId) {
                    case Keyword.KeyTyp.ORDER_NUMBER:
                        String strOrderNum = Utils.getNextWord (line, 19, 19);
                        newOrder.setOrderNumber(strOrderNum);
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Order #: " + strOrderNum);
                        lastDeliveryDate = null;  // reset the delivery date to unknown
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.DELIVERED: // fall through...
                    case Keyword.KeyTyp.ARRIVING:  // fall through...
                    case Keyword.KeyTyp.NOW_ARRIVING:
                        delivered = DateFormat.getFormattedDate(line, true);
                        if (delivered == null) {
                            throw new ParserException("parseOrders: invalid char in " + keywordInfo.eKeyId.name() + " date ", line);
                        }
                        if (newItem.isItemDefined()) {
                            newItem = newOrder.addNewItem();
                            frame.outputInfoMsg (UIFrame.STATUS_INFO, "* Added new ITEM (" + newOrder.getItemCount() + ") in multi-item ORDER");
                        }
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    " + keywordInfo.eKeyId.name() + ": " + delivered);
                        // if the current item has already been defined, create a new one
                        newItem.setDeliveryDate(delivered);
                        lastDeliveryDate = delivered;  // save last delivery date
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.REFUNDED:
                        if (newItem.isItemDefined()) {
                            newItem = newOrder.addNewItem();
                            frame.outputInfoMsg (UIFrame.STATUS_INFO, "* Added new ITEM (" + newOrder.getItemCount() + ") in multi-item ORDER");
                        }
                        newItem.setReturned();
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Item refunded");
                        keywordInfo = null; // command complete
                        break;
                        
                    case Keyword.KeyTyp.VENDOR_RATING:
                        // if the DELIVERED date was skipped, it is another item arriving in the same package
                        //  so the delivery date is the same.
                        if (newItem.isItemDefined()) {
                            newItem = newOrder.addNewItem();
                            frame.outputInfoMsg (UIFrame.STATUS_INFO, "* Added new ITEM (" + newOrder.getItemCount() + ") in multi-item ORDER");

                            if (lastDeliveryDate == null) {
                                frame.outputInfoMsg (UIFrame.STATUS_WARN, "* Delivery date not found for item!");
                            } else {
                                frame.outputInfoMsg (UIFrame.STATUS_PARSER, "      (using last delivery date : " + lastDeliveryDate + ")");
                                newItem.setDeliveryDate(lastDeliveryDate);
                            }
                        }
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    (Vendor Rating): " + line);
                        
                        // the vendor rating is a single character that is one of: A,B,C,D,F,?
                        // we don't need it, but the item description will be in the next line,
                        // so advance to the next state.
                        keywordInfo = keyword.makeKeyword("", Keyword.KeyTyp.DESCRIPTION, Keyword.DataTyp.INLINE);
                        bReadData = false; // this will prevent us from parsing the command until we've read the next line
                        break;

                    case Keyword.KeyTyp.DESCRIPTION:
                        descript1 = line;
                        int quantity = 1;
                        int maxlen = descript1.length();
                        // get the max length of the description to save
                        int iMaxDescrLen = props.getPropertiesItem(Property.MaxLenDescription, 90);
                        String truncDescript = descript1.substring(0, (maxlen > iMaxDescrLen) ? iMaxDescrLen : maxlen);
                        newItem.setDescription(truncDescript);
                        newItem.setQuantity(quantity);
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Description: " + truncDescript);

                        // first see if if the description ends in a 1 or 2 digit value that may be the quantity
                        int offset = descript1.lastIndexOf(" ");
                        int wordlen = maxlen - offset - 1;
                        int maxdigits = 2;
                        iQtyPossible = 0;
                        if (offset > 0 && wordlen >= 1 && wordlen <= maxdigits) {
                            String lastWord = descript1.substring(offset + 1);
                            for (int ix = 0; ix < wordlen; ix++) {
                                int charVal = lastWord.charAt(ix);
                                if (charVal >= '0' && charVal <= '9') {
                                    iQtyPossible = (10 * iQtyPossible) + charVal - '0';
                                } else {
                                    iQtyPossible = 0;
                                    break;
                                }
                            }
                        }
                        
                        // check if we have an optional quantity value in the next line (that is > 1)
                        if (iQtyPossible > 1) {
                            frame.outputInfoMsg (UIFrame.STATUS_DEBUG, "  - possible quantity value found: " + iQtyPossible);
                            keywordInfo = keyword.makeKeyword("", Keyword.KeyTyp.DESCRIPTION_2, Keyword.DataTyp.NEXTLINE);
                            bReadData = false; // this will prevent us from parsing the command until we've read the next line
                        } else {
                            // nope - we're done
                            frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Quantity: " + quantity);

                            // the delivery date will have been previously set here.
                            // we must have this value in a seperate variable from newOrder, since multi-item
                            // entries may change this value on the previous item entries if we set the value
                            // before we have instantiated a new newOrder object
                            if (delivered == null && ! newItem.getReturned()) {
                                throw new ParserException("Delivery date not setup prior to item description");
                            }
                            newItem.setDeliveryDate(delivered);
                            frame.outputInfoMsg (UIFrame.STATUS_PARSER, "END OF ITEM (" + newOrder.getItemCount() + ")");
                            keywordInfo = null; // command complete
                        }
                        itemCount++;
                        break;
                        
                    default:
                        break;
                }

                continue;
            }

            // if the command has data on a following line and the next line has not yet been read,
            // go fetch the next line before executing command.
            if (!bReadData) {
                frame.outputInfoMsg (UIFrame.STATUS_DEBUG, "  - setting bReadData true for next line");
                bReadData = true;
            } else {
                // these commands must be completed after the next line of input is read,
                // so the command line parsing is skipped for them
                bReadData = false;
                frame.outputInfoMsg (UIFrame.STATUS_DEBUG, "  - bReadData true: parsing data: " + line);
                frame.outputInfoMsg (UIFrame.STATUS_INFO, "* Executing KeyTyp." + keywordInfo.eKeyId.name() + " as " + keywordInfo.eDataType.name());

                switch (keywordInfo.eKeyId) {
                    case Keyword.KeyTyp.ORDER_PLACED:
                        // this is the start of a new entry and the transaction date will be on the next line

                        // if an entry is already in process, it must have been completed, so add completed order to list
                        if (newOrder.isOrderDefined()) {
                            amazonList.add(newOrder);
                            frame.outputInfoMsg (UIFrame.STATUS_PARSER, "END OF ORDER (" + amazonList.size() + ")");
                            frame.outputInfoMsg (UIFrame.STATUS_INFO, "* Added new ORDER entry to AMAZON LIST");

                            // start a new order
                            newOrder = new AmazonOrder();
                            newItem = newOrder.addNewItem();
                            frame.outputInfoMsg (UIFrame.STATUS_INFO, "* Creating new ORDER & ITEM entries");
                        }

                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "Order placed: " + line);
                        LocalDate date = DateFormat.getFormattedDate (line, true);
                        newOrder.setOrderDate(date);
                        if (date == null) {
                            throw new ParserException("parseOrders: invalid char in 'Order placed'", line);
                        }
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.TOTAL_COST:
                        // the next line will contain the total amount of the purchase
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Total: " + line);
                        int amount = Utils.getAmountValue(line.substring(1));
                        newOrder.setTotalCost(amount);
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.DESCRIPTION_2:
                        if (descript1 == null) {
                            throw new ParserException("parseOrders: 1st line of description wasn't found", line);
                        }
                        
                        String descript2 = line;
                        
                        // if the 2 lines match or they are completely different, qty is 1 and use 1st description
                        int quantity;
                        if (descript1.indexOf(descript2) != 0 || (descript1.equals(descript2))) {
                            quantity = 1;
                        } else {
                            // otherwise, the qty is appended to the item description in the 1st entry
                            quantity = iQtyPossible;
                        }

                        newItem.setQuantity(quantity);
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Quantity: " + quantity);

                        // the delivery date will have been previously set here.
                        // we must have this value in a seperate variable from newOrder, since multi-item
                        // entries may change this value on the previous item entries if we set the value
                        // before we have instantiated a new newOrder object
                        if (delivered == null && ! newItem.getReturned()) {
                            throw new ParserException("Delivery date not setup prior to item description");
                        }
                        newItem.setDeliveryDate(delivered);
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "END OF ITEM (" + newOrder.getItemCount() + ")");
                        keywordInfo = null; // command complete
                        break;

                    default:
                        break;
                }
            }
        } while (line != null);

        // if an entry is already in process, it must have been completed, so add completed order to list
        if (newOrder != null && newOrder.isOrderDefined()) {
            amazonList.add(newOrder);
            frame.outputInfoMsg (UIFrame.STATUS_DEBUG, "* Added new ORDER entry to AMAZON LIST");
        }

        // check if we have valid entries
        if (amazonList.isEmpty()) {
            frame.outputInfoMsg(UIFrame.STATUS_WARN, "Clipboard did not contain any items.");
            return amazonList;
        }
        
        return amazonList;
    }
}
