/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.props;
import com.mycompany.amazonlogger.GUILogPanel.MsgType;
import com.mycompany.amazonlogger.PropertiesFile.Property;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 *
 * @author dan
 */
public class ParseOrders {

    private static final String CLASS_NAME = ParseOrders.class.getSimpleName();

    private int iQtyPossible = 0;
    
    public ParseOrders() {
        iQtyPossible = 0;
    }

    // This creates the order information for the parser
    class OrderInfo {
        Keyword.KeyTyp  orderId;    // the order id
        Keyword.DataTyp dataType;   // type of data (INLINE, NEXTLINE, NONE)
        int             keyLength;  // length of keyword found
        
        // this is used when executing a saved order id
        OrderInfo (Keyword.KeyTyp order) {
            Keyword.KeywordInfo keywordInfo = Keyword.findOrdersKey (order);
            
            if (keywordInfo != null) {
                orderId   = order;
                keyLength = keywordInfo.keyLength;
                dataType  = Keyword.getDataTypeOrder (order);
            } else {
                orderId   = Keyword.KeyTyp.NONE;
                keyLength = 0;
                dataType  = Keyword.DataTyp.NONE;
            }
        }
        
        // this is used when executing a line read from the clipboard
        OrderInfo (String line) {
            Keyword.KeywordInfo keywordInfo = Keyword.getKeyword(AmazonParser.ClipTyp.ORDERS, line);
            
            if (keywordInfo != null) {
                orderId   = keywordInfo.eKeyId;
                keyLength = keywordInfo.keyLength;
                dataType  = Keyword.getDataTypeOrder (orderId);
            } else {
                orderId   = Keyword.KeyTyp.NONE;
                keyLength = 0;
                dataType  = Keyword.DataTyp.NONE;
            }
        }
        
        // this is used to clear out the current order
        OrderInfo () {
            orderId   = Keyword.KeyTyp.NONE;
            keyLength = 0;
            dataType  = Keyword.DataTyp.NONE;
        }
        
        // this is used to insert a command to execute
        public void makeOrder (Keyword.KeyTyp order) {
            orderId   = order;
            keyLength = 0;
            dataType  = Keyword.getDataTypeOrder (order);
        }
    }
    
    /**
     * parses the clipboard data line by line to extract the order information from a "Your Orders" clip.
     * 
     * @param clip    - the clipboard reader to read from
     * @param line    - the line previously caught and passed to this module to execute
     * @param keyType - the Key type associated with the line
     * 
     * @return an array of AmazonOrder entries that were extracted from the clip
     * 
     * @throws ParserException 
     * @throws IOException 
     */
    public ArrayList<AmazonOrder> parseOrders (ClipboardReader clip, String line, Keyword.KeyTyp keyType) throws ParserException, IOException {
        String descript1 = null;
        OrderInfo keywordInfo = null;
        OrderInfo savedKey = null;
        boolean bReadData = false;
        boolean bSkipRead = false;
        boolean bDescPending = false;
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
            keywordInfo = new OrderInfo (keyType);
            savedKey = keywordInfo;
            bSkipRead = true;
        }
        
        do {
            // if we don't have a pending command in the queue, get next line from clipboard
            if (!bSkipRead) {
                line = clip.getLine();
                if (line == null)
                    break;
                line = line.stripLeading();
                if (line.isBlank())
                    continue;

                // parse line to check for command
                savedKey = new OrderInfo(line);
                if (savedKey.orderId == Keyword.KeyTyp.END_OF_RECORD) {
                    // if an entry is already in process, it must have been completed, so add completed order to list
                    if (newOrder.isOrderDefined()) {
                        amazonList.add(newOrder);
                        GUILogPanel.outputInfoMsg (MsgType.DEBUG, "* Added new ORDER entry to AMAZON LIST");
                    }

                    // exit if we completed loop
                    newOrder = null;
                    GUILogPanel.outputInfoMsg (MsgType.PARSER, "END OF ORDER (" + amazonList.size() + ")");
                    GUILogPanel.outputInfoMsg (MsgType.PARSER, "END OF LIST");
                    break;
                }
            }

            // reset the skip read flag
            bSkipRead = false;
            
            // see if we have a pending command (next line contains the data)
            if (savedKey == null) {
                savedKey = new OrderInfo();
            }
            if (keywordInfo == null || keywordInfo.orderId == Keyword.KeyTyp.NONE) {
                keywordInfo = savedKey;
                if (keywordInfo.orderId == Keyword.KeyTyp.NONE) {
                    // sometimes the "Package was left" entry is omitted from the record.
                    // It should be the line following the "Delivered" entry. If it is missing,
                    //  the line should be the description.
                    if (bDescPending) {
                        keywordInfo = new OrderInfo();
                        keywordInfo.makeOrder(Keyword.KeyTyp.DESCRIPTION);
                        bDescPending = false;
                    } else {
                        continue;
                    }
                }
            } else {
                GUILogPanel.outputInfoMsg (MsgType.DEBUG, "  KeyTyp." + savedKey.orderId + " (unparsed line): " + line);
            }

            // now run the state machine...

            // these entries can be processed immediately because the information they need
            // is contained in the same line as the keyword (or they don't need any information)
            if (keywordInfo.dataType != Keyword.DataTyp.NEXTLINE) {
                // if data was inline with the command, advance the string
                // past the keyword to access the data.
                if (keywordInfo.dataType == Keyword.DataTyp.INLINE) {
                    line = line.substring(keywordInfo.keyLength);
                }
                GUILogPanel.outputInfoMsg (MsgType.INFO, "* Executing KeyTyp." + keywordInfo.orderId + " as " + keywordInfo.dataType);

                switch (keywordInfo.orderId) {
                    case Keyword.KeyTyp.ORDER_NUMBER:
                        String strOrderNum = Utils.getNextWord (line, 19, 19);
                        newOrder.setOrderNumber(strOrderNum);
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Order #: " + strOrderNum);
                        lastDeliveryDate = null;  // reset the delivery date to unknown
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.DELIVERED: // fall through...
                        bDescPending = true;
                    case Keyword.KeyTyp.ARRIVING:  // fall through...
                    case Keyword.KeyTyp.NOW_ARRIVING:
                        delivered = DateFormat.getFormattedDate(line, true);
                        if (delivered == null) {
                            throw new ParserException("ParseOrders.parseOrders: invalid char in " + keywordInfo.orderId + " date: " + line);
                        }
                        if (newItem.isItemDefined()) {
                            newItem = newOrder.addNewItem();
                            GUILogPanel.outputInfoMsg (MsgType.INFO, "* Added new ITEM (" + newOrder.getItemCount() + ") in multi-item ORDER");
                        }
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    " + keywordInfo.orderId + ": " + delivered);
                        // if the current item has already been defined, create a new one
                        newItem.setDeliveryDate(delivered);
                        lastDeliveryDate = delivered;  // save last delivery date
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.RETURNED:
                        if (newItem.isItemDefined()) {
                            newItem = newOrder.addNewItem();
                            GUILogPanel.outputInfoMsg (MsgType.INFO, "* Added new ITEM (" + newOrder.getItemCount() + ") in multi-item ORDER");
                        }
                        newItem.setReturned();
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Item returned");
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.REFUNDED:
                        if (newItem.isItemDefined()) {
                            newItem = newOrder.addNewItem();
                            GUILogPanel.outputInfoMsg (MsgType.INFO, "* Added new ITEM (" + newOrder.getItemCount() + ") in multi-item ORDER");
                        }
                        newItem.setReturned();
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Item refunded");
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.PACKAGE_LEFT:
                        // if the DELIVERED date was skipped, it is another item arriving in the same package
                        //  so the delivery date is the same.
                        if (newItem.isItemDefined()) {
                            newItem = newOrder.addNewItem();
                            GUILogPanel.outputInfoMsg (MsgType.INFO, "* Added new ITEM (" + newOrder.getItemCount() + ") in multi-item ORDER");

                            if (lastDeliveryDate == null) {
                                GUILogPanel.outputInfoMsg (MsgType.WARN, "ParseOrders.parseOrders: Delivery date not found for item!");
                            } else {
                                GUILogPanel.outputInfoMsg (MsgType.PARSER, "      (using last delivery date : " + lastDeliveryDate + ")");
                                newItem.setDeliveryDate(lastDeliveryDate);
                            }
                        }
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    (Vendor Rating): " + line);
                        
                        // the item description will be in the next line, so advance to the next state.
                        keywordInfo = new OrderInfo();
                        keywordInfo.makeOrder(Keyword.KeyTyp.DESCRIPTION);
                        bReadData = false; // this will prevent us from parsing the command until we've read the next line
                        bDescPending = false;
                        break;

                    case Keyword.KeyTyp.DESCRIPTION:
                        bDescPending = false;
                        descript1 = line;
                        int quantity = 1;
                        int maxlen = descript1.length();
                        // get the max length of the description to save
                        int iMaxDescrLen = props.getPropertiesItem(Property.MaxLenDescription, 90);
                        String truncDescript = descript1.substring(0, (maxlen > iMaxDescrLen) ? iMaxDescrLen : maxlen);
                        newItem.setDescription(truncDescript);
                        newItem.setQuantity(quantity);
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Description: " + truncDescript);

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
                            GUILogPanel.outputInfoMsg (MsgType.DEBUG, "  - possible quantity value found: " + iQtyPossible);
                            keywordInfo = new OrderInfo();
                            keywordInfo.makeOrder(Keyword.KeyTyp.DESCRIPTION_2);
                            bReadData = false; // this will prevent us from parsing the command until we've read the next line
                        } else {
                            // nope - we're done
                            GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Quantity: " + quantity);

                            // the delivery date will have been previously set here.
                            // we must have this value in a seperate variable from newOrder, since multi-item
                            // entries may change this value on the previous item entries if we set the value
                            // before we have instantiated a new newOrder object
                            if (delivered == null && ! newItem.getReturned()) {
                                throw new ParserException("ParseOrders.parseOrders: Delivery date not setup prior to item description");
                            }
                            newItem.setDeliveryDate(delivered);
                            GUILogPanel.outputInfoMsg (MsgType.PARSER, "END OF ITEM (" + newOrder.getItemCount() + ")");
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
                GUILogPanel.outputInfoMsg (MsgType.DEBUG, "  - setting bReadData true for next line");
                bReadData = true;
            } else {
                // these commands must be completed after the next line of input is read,
                // so the command line parsing is skipped for them
                bReadData = false;
                GUILogPanel.outputInfoMsg (MsgType.DEBUG, "  - bReadData true: parsing data: " + line);
                GUILogPanel.outputInfoMsg (MsgType.INFO, "* Executing KeyTyp." + keywordInfo.orderId + " as " + keywordInfo.dataType);

                switch (keywordInfo.orderId) {
                    case Keyword.KeyTyp.ORDER_PLACED:
                        // this is the start of a new entry and the transaction date will be on the next line

                        // if an entry is already in process, it must have been completed, so add completed order to list
                        if (newOrder.isOrderDefined()) {
                            amazonList.add(newOrder);
                            GUILogPanel.outputInfoMsg (MsgType.PARSER, "END OF ORDER (" + amazonList.size() + ")");
                            GUILogPanel.outputInfoMsg (MsgType.INFO, "* Added new ORDER entry to AMAZON LIST");

                            // start a new order
                            newOrder = new AmazonOrder();
                            newItem = newOrder.addNewItem();
                            GUILogPanel.outputInfoMsg (MsgType.INFO, "* Creating new ORDER & ITEM entries");
                        }

                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "Order placed: " + line);
                        LocalDate date = DateFormat.getFormattedDate (line, true);
                        newOrder.setOrderDate(date);
                        if (date == null) {
                            throw new ParserException("ParseOrders.parseOrders: invalid char in 'Order placed': " + line);
                        }
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.TOTAL_COST:
                        // the next line will contain the total amount of the purchase
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Total: " + line);
                        int amount = Utils.getAmountValue(line.substring(1));
                        newOrder.setTotalCost(amount);
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.DESCRIPTION_2:
                        if (descript1 == null) {
                            throw new ParserException("ParseOrders.parseOrders: 1st line of description wasn't found: " + line);
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
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Quantity: " + quantity);

                        // the delivery date will have been previously set here.
                        // we must have this value in a seperate variable from newOrder, since multi-item
                        // entries may change this value on the previous item entries if we set the value
                        // before we have instantiated a new newOrder object
                        if (delivered == null && ! newItem.getReturned()) {
                            throw new ParserException("ParseOrders.parseOrders: Delivery date not setup prior to item description");
                        }
                        newItem.setDeliveryDate(delivered);
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "END OF ITEM (" + newOrder.getItemCount() + ")");
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
            GUILogPanel.outputInfoMsg (MsgType.DEBUG, "* Added new ORDER entry to AMAZON LIST");
        }

        // check if we have valid entries
        if (amazonList.isEmpty()) {
            GUILogPanel.outputInfoMsg(MsgType.WARN, "ParseOrders.parseOrders: Clipboard did not contain any items.");
            return amazonList;
        }
        
        return amazonList;
    }
}
