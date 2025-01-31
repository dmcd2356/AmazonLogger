package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.AmazonReader.keyword;
import static com.mycompany.amazonlogger.AmazonReader.props;
import com.mycompany.amazonlogger.PropertiesFile.Property;
import java.time.LocalDate;

/**
 *
 * @author dan
 */
public class ParseDetails {
    
    public ParseDetails() {
    }
    
    /**
     * parses the clipboard data line by line to extract the order information from a "Order Details" clip.
     * 
     * @param line    - the line previously caught and passed to this module to execute
     * 
     * @return the AmazonOrder entry that was extracted from the clip
     * 
     * @throws ParserException 
     */
    public AmazonOrder parseDetails (String line) throws ParserException {
        Keyword.KeywordEntry keywordInfo = null;
        Keyword.KeywordEntry savedKey = null;
        boolean bReadData = false;
        boolean bItemFound = false;
        boolean bSkipRead = false;
        LocalDate lastDeliveryDate = null;

        // create an entry for the first item
        AmazonOrder newOrder = new AmazonOrder();
        AmazonItem newItem = newOrder.addNewItem();
        
        do {
            // if we don't have a pending command in the queue, get next line from clipboard
            if (!bSkipRead) {
                line = ClipboardReader.webClipGetLine();
                if (line == null)
                    break;
                if (line.isBlank())
                    continue;

                // parse line to check for command
                savedKey = keyword.getKeyword(line, Keyword.ClipTyp.DETAILS);
                if (savedKey.eKeyId == Keyword.KeyTyp.END_OF_RECORD) {
                    // exit if we completed loop
                    frame.outputInfoMsg (UIFrame.STATUS_PARSER, "END OF ITEMS");
                    keywordInfo = keyword.makeKeyword("", Keyword.KeyTyp.COMPLETE, Keyword.DataTyp.NONE);
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
                
                switch (keywordInfo.eKeyId) {
                    case Keyword.KeyTyp.ORDER_PLACED:
                        // if an entry is already in process, it must have been completed, so add completed order to list
//                        if (newOrder.isOrderDefined()) {
//                            amazonList.add(newOrder);
//                            frame.outputInfoMsg (UIFrame.STATUS_DEBUG, "* Added new ORDER entry to AMAZON LIST");
//                        }

                        // this contains the date of the order, followed immediatly on the
                        // same line with the 'Order#'. We don't care about the order date, but
                        // we do need the order number.
                        int offset = line.indexOf("Order# ");
                        if (offset <= 0 || line.length() != offset + 7 + 19) {
                            throw new ParserException("Order number not found in 'Ordered on' line");
                        }
                        String strOrderNum = line.substring(offset + 7);
                        // now extract date from begining of string
                        LocalDate date = DateFormat.getFormattedDate (line, true);
                        if (date == null)
                            throw new ParserException("parseDetails: invalid char in " + keywordInfo.eKeyId.name() + " date ", line);
                        newOrder.setOrderDate(date);
                        newOrder.setOrderNumber(strOrderNum);
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Order date: " + date.toString());
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Order #: " + strOrderNum);
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.DELIVERED: // fall through...
                    case Keyword.KeyTyp.ARRIVING:  // fall through...
                    case Keyword.KeyTyp.NOW_ARRIVING:
                        LocalDate delivered = DateFormat.getFormattedDate(line, true);
                        if (delivered == null) {
                            throw new ParserException("parseDetails: invalid char in " + keywordInfo.eKeyId.name() + " date ", line);
                        }
                        if (newItem.isItemDefined()) {
                            frame.outputInfoMsg (UIFrame.STATUS_INFO, "* Added new ITEM in multi-item ORDER");
                            newItem = newOrder.addNewItem();
                        }
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    " + keywordInfo.eKeyId.name() + ": " + delivered);
                        // if the current item has already been defined, create a new one
                        newItem.setDeliveryDate(delivered);
                        lastDeliveryDate = delivered;  // save last delivery date
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.SELLER:
                        newItem.setSeller(line);
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Seller: " + line);
                        keywordInfo = null; // command complete
                        continue;

                    case Keyword.KeyTyp.ITEM_COST:
                        // we only count it if it follows an item
                        if (bItemFound) {
                            newItem.setItemCost(Utils.getAmountValue(line));
                            frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Item cost: " + line);
                            bItemFound = false;
                        }
                        else {
                            frame.outputInfoMsg (UIFrame.STATUS_INFO, "skipping ITEM_COST - item not defined yet");
                        }
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.REFUNDED:
                        newItem.setReturned();
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Item refunded");
                        keywordInfo = null; // command complete
                        break;
                        
                    case Keyword.KeyTyp.VENDOR_RATING:
                        if (newItem.isItemDefined()) {
                            frame.outputInfoMsg (UIFrame.STATUS_INFO, "* Added new ITEM in multi-item ORDER");
                            newItem = newOrder.addNewItem();
                        }
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    (Vendor Rating): " + line);
                        // the vendor rating is a single character that is one of: A,B,C,D,F,?
                        // we don't need it, but the item description will be in the next line,
                        // so advance to the next state.
                        keywordInfo = keyword.makeKeyword("", Keyword.KeyTyp.DESCRIPTION, Keyword.DataTyp.INLINE);
                        bReadData = false; // this will prevent us from parsing the command until we've read the next line
                        break;

                    case Keyword.KeyTyp.DESCRIPTION:
                        String descript1 = line;
                        int quantity = 1;
                        // get the max length of the description to save
                        int iMaxDescrLen = props.getPropertiesItem(Property.MaxLenDescription, 90);
                        if (descript1.length() > iMaxDescrLen) descript1 = descript1.substring(0, iMaxDescrLen);
                        newItem.setDescription(descript1);
                        newItem.setQuantity(quantity);
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Description: " + descript1);
                        
                        // if delivery date was not presented for the item, use date of last item
                        if (newItem.getDeliveryDate() == null) {
                            if (lastDeliveryDate == null) {
                                frame.outputInfoMsg (UIFrame.STATUS_WARN, "* Delivery date not found for item!");
                            } else {
                                frame.outputInfoMsg (UIFrame.STATUS_PARSER, "      (using last delivery date : " + lastDeliveryDate + ")");
                                newItem.setDeliveryDate(lastDeliveryDate);
                            }
                        }
                        // item has been found, so we can now check for item cost
                        bItemFound = true;
                        // check if we have an optional quantity value in the next line
                        keywordInfo = keyword.makeKeyword("", Keyword.KeyTyp.QUANTITY, Keyword.DataTyp.INLINE);
                        break;

                    case Keyword.KeyTyp.QUANTITY:
                        quantity = Utils.getIntegerValue(line, 0);
                        if (quantity > 0 && line.length() < 3) { // our max number of items we order will always be < 100
                            newItem.setQuantity(quantity);
                        } else {
                            // the line following the description was not a quantity value.
                            // set this flag to re-run the current line read as a potential command
                            bSkipRead = true;
                            quantity = 1;
                        }
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Quantity: " + quantity);
                        keywordInfo = null;
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

                switch (keywordInfo.eKeyId) {
                    case Keyword.KeyTyp.TOTAL_COST:
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Total: " + line);
                        newOrder.setTotalCost(Utils.getAmountValue(line));
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.GROSS_COST:
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Gross: " + line);
                        newOrder.setGrossCost(Utils.getAmountValue(line));
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.TAXES:
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "    Taxes: " + line);
                        newOrder.setTaxCost(Utils.getAmountValue(line));
                        keywordInfo = null; // command complete
                        break;

                    default:
                        break;
                }
            }
        } while (keywordInfo == null || keywordInfo.eKeyId != Keyword.KeyTyp.COMPLETE);

        // check if we were in the middle of a command when we terminated
        if (keywordInfo != null && keywordInfo.eKeyId != Keyword.KeyTyp.COMPLETE
                                && keywordInfo.eKeyId != Keyword.KeyTyp.NONE) {
            throw new ParserException("EOF while parsing Order Details (state = " + keywordInfo.eKeyId.name() + ")");
        }
        if (newOrder.item.isEmpty()) {
            frame.outputInfoMsg(UIFrame.STATUS_WARN, "Clipboard did not contain any items.");
        }
        
        return newOrder;
    }

}
