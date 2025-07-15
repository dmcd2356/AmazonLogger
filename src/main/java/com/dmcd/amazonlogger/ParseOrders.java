/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import com.dmcd.amazonlogger.GUILogPanel.MsgType;
import com.dmcd.amazonlogger.PropertiesFile.Property;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 *
 * @author dan
 */
public class ParseOrders {

    private static final String CLASS_NAME = ParseOrders.class.getSimpleName();

    private ClipboardReader clipReader;
    
    public ParseOrders(ClipboardReader clip) {
        clipReader = clip;
    }

    // This creates the order information for the parser
    class OrderInfo {
        Keyword.KeyTyp  orderId;    // the order id
        int             keyLength;  // length of keyword found
        String          data;       // the data value contaied in the line (if any)
        
        // this is used when executing a line read from the clipboard
        OrderInfo (String line) {
            Keyword.KeywordInfo keywordInfo = Keyword.getKeyword(line);
            
            if (keywordInfo != null) {
                boolean bMoreData = line.length() > keyLength;
                orderId   = keywordInfo.getKeyType();
                keyLength = keywordInfo.getKeyLength();
                data      = bMoreData ? line.substring(keyLength).strip() : "";
            } else {
                orderId   = Keyword.KeyTyp.NONE;
                keyLength = 0;
                data = "";
            }
        }
        
        // this is used when executing a saved order id (data occurs on next line)
        OrderInfo (Keyword.KeyTyp order) {
            Keyword.KeywordInfo keywordInfo = Keyword.findOrdersKey (order);
            
            if (keywordInfo != null) {
                orderId   = order;
                keyLength = 0;
                data = "";
            } else {
                orderId   = Keyword.KeyTyp.NONE;
                keyLength = 0;
                data = "";
            }
        }
        
        // this is used to insert a command to execute (the case when there is
        //  no leading keywords and we rely on that it comes after another keyword)
        public void makeOrder (Keyword.KeyTyp order) {
            orderId   = order;
            keyLength = 0;
            data = "";
        }
    }

    /**
     * reads the next line from the clipboard.
     * 
     * @return the next line of text, null if EOF or error
     */
    private String readLine () {
        String line;
        try {
            while ((line = clipReader.getLine()) != null) {
                line = line.strip();
                if (! line.isEmpty()) {
                    return line;
                }
            }
        } catch (IOException exMsg) {
            // ignore
        }
        return null;
    }
    
    /**
     * parses the clipboard data line by line to extract the order information from a "Your Orders" clip.
     * 
     * @param type    - type of order: ORDERS or INVOICE
     * @param line    - the line previously caught and passed to this module to execute
     * @param keyType - the Key type associated with the line
     * 
     * @return an array of AmazonOrder entries that were extracted from the clip
     */
    public ArrayList<AmazonOrder> parseOrders (AmazonParser.ClipTyp type, String line, Keyword.KeyTyp keyType) {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        boolean bSkipRead = false;
        boolean bDescrFound = false;
        String lastLine = "";

        // init the array list of Amazon transactions
        ArrayList<AmazonOrder> amazonList = new ArrayList<>();
            
        // create an entry for the first item
        AmazonOrder newOrder = new AmazonOrder();

        // if a command was passed in, use it on the current line, else read new line
        OrderInfo keywordInfo = new OrderInfo (keyType);
        if (keyType != Keyword.KeyTyp.NONE) {
            bSkipRead = true;
        }
        
        while (line != null) {
            // if we don't have a pending command in the queue, get next line from clipboard
            if (bSkipRead) {
                bSkipRead = false;
            } else {
                // read the next line
                line = readLine();
                if (line == null) {
                    break;
                }

                // check for an end of Amazon Order record indicator
                keywordInfo = new OrderInfo(line);
                if (keywordInfo.orderId == Keyword.KeyTyp.BUTTON ||
                    keywordInfo.orderId == Keyword.KeyTyp.NOTICE ) {
                    continue; // ignore all buttons and nonsense info
                }
                if (keywordInfo.orderId == Keyword.KeyTyp.END_OF_RECORD) {
                    if (type == AmazonParser.ClipTyp.ORDERS) {
                        // if an entry is already in process, it must have been completed, so add completed order to list
                        if (newOrder.isOrderDefined()) {
                            amazonList.add(newOrder);
                            GUILogPanel.outputInfoMsg (MsgType.DEBUG, "* Added new ORDER entry to AMAZON LIST");
                        }

                        // order completed - exit loop to clean up
                        newOrder = null;
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "END OF ORDER (" + amazonList.size() + ")");
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "END OF LIST");
                    } else {
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "END OF ITEMS");
                    }
                    break;
                }
            }

            // check for item cost, which is the first line that starts with a '$' that follows the description
            if (line.charAt(0) == '$' && bDescrFound) {
                parseCommand (Keyword.KeyTyp.ITEM_COST, line, newOrder);
                continue;
            }

            // If we have 2 lines of sufficiet length in a row that match, except
            //  possibly for the last couple of chars, this must be the description.
            // The 1st line will have a space and the quantity added to it and the
            //  second one won't.
            // For some silly reason, the 1st entry limits the description to 125 chars,
            //  but not the second. So if the 2nd entry exceeds 125, just snip it off.
            if (Keyword.KeyTyp.NONE == keywordInfo.orderId) {
                Integer qtyValue = 1;
                int linelen = line.length();
                int prevlen = lastLine.length();
                // make sure we had 2 NOT FOUND lines in a row
                if (prevlen > 10) {
                    // eliminate the extraneous material from the non-truncated entry
                    if (linelen > 125 && prevlen > 125) {
                        linelen = 125;
                        line = line.substring(0, linelen);
                    }
                    // check if line 1 and 2 are exact match
                    if (line.contentEquals(lastLine)) {
                        // exact match: must be single qty - log entries and exit
                        GUILogPanel.outputInfoMsg (MsgType.DEBUG, "Exact match of Description entries");
                        setDescriptionAndQty (line, qtyValue, newOrder);
                        lastLine = "";
                        bDescrFound = true;
                        continue;
                    }
                    // else, see if there is a qty number attached to the 1st entry
                    int offset = lastLine.lastIndexOf(' ');
                    if (prevlen < offset + 3) { // this would allow for a space & 2 digits for qty
                        String qtystr = lastLine.substring(offset).strip();
                        try {
                            qtyValue = Integer.valueOf(qtystr);
                            if (qtyValue < 1 || qtyValue > 100) {
                                GUILogPanel.outputInfoMsg (MsgType.DEBUG, "Invalid range of Qty value: " + qtystr);
                                qtyValue = 1;
                            }
                        } catch (NumberFormatException ex) {
                            GUILogPanel.outputInfoMsg (MsgType.DEBUG, "Last word of Description was not an Integer value: " + qtystr);
                            qtyValue = 1;
                        }
                    }
                    // check if 2nd line is an exact substring of 1st line
                    if (linelen == offset && line.contentEquals(lastLine.substring(0, offset))) {
                        GUILogPanel.outputInfoMsg (MsgType.DEBUG, "Match of Description entries up to Qty value of " + qtyValue);
                        setDescriptionAndQty (line, qtyValue, newOrder);
                        lastLine = "";
                        bDescrFound = true;
                        continue;
                    }
                    // last ditch attempt, since sometimes they change the wording in the 2 descriptions
                    int minMatchLen = 6;
                    if (linelen >= minMatchLen) {
                        String truncline = line.substring(0, minMatchLen);
                        if (truncline.contentEquals(lastLine.substring(0, truncline.length()))) {
                            GUILogPanel.outputInfoMsg (MsgType.DEBUG, "Partial match of Description entries up to length " + minMatchLen);
                            setDescriptionAndQty (line, qtyValue, newOrder);
                            lastLine = "";
                            bDescrFound = true;
                            continue;
                        }
                    }
                }
                // unsuccessful match - save current line as last line
                lastLine = line;
                continue;
            } else {
                lastLine = "";
            }

            // check for initial command
            if (keywordInfo.orderId == Keyword.KeyTyp.ORDER_PLACED) {
                // this is the start of a new entry and the transaction date will be given for it.
                String data = keywordInfo.data;
                if (data == null || data.isEmpty()) {
                    data = readLine();
                    if (data == null) {
                        break;
                    }
                }

                // if an entry is already in process, it must have been completed, so add completed order to list
                if (newOrder.isOrderDefined()) {
                    amazonList.add(newOrder);
                    GUILogPanel.outputInfoMsg (MsgType.PARSER, "END OF ORDER (" + amazonList.size() + ")");
                    GUILogPanel.outputInfoMsg (MsgType.INFO, "* Added new ORDER entry to AMAZON LIST");

                    // start a new order
                    newOrder = new AmazonOrder();
//                    newItem = newOrder.addNewItem();
                    GUILogPanel.outputInfoMsg (MsgType.INFO, "* Creating new ORDER & ITEM entries");
                }

                GUILogPanel.outputInfoMsg (MsgType.PARSER, "Order placed: " + data);
                LocalDate date = null;
                try {
                    date = DateFormat.getFormattedDate (data, true);
                    newOrder.setOrderDate(date);
                } catch (ParserException exMsg) {
                    // next line will catch error
                }
                if (date == null) {
                    GUILogPanel.outputInfoMsg(MsgType.WARN, functionId + "invalid char in 'Order placed': " + data);
                }
                continue;
            }

            // these entries can be processed immediately because the information they need
            // is contained in the same line as the keyword (or they don't need any information)
            String data = keywordInfo.data;
            if (data == null || data.isEmpty()) {
                data = readLine();
                if (data == null) {
                    break;
                }
            }
            GUILogPanel.outputInfoMsg (MsgType.DEBUG, "  - data content: " + line);
            GUILogPanel.outputInfoMsg (MsgType.INFO, "* Executing KeyTyp." + keywordInfo.orderId);
            parseCommand (keywordInfo.orderId, data, newOrder);

        }

        // if an entry is already in process, it must have been completed, so add completed order to list
        if (newOrder != null && newOrder.isOrderDefined()) {
            amazonList.add(newOrder);
            GUILogPanel.outputInfoMsg (MsgType.DEBUG, "* Added new ORDER entry to AMAZON LIST");
        }

        // check if we have valid entries
        if (amazonList.isEmpty()) {
            GUILogPanel.outputInfoMsg(MsgType.WARN, functionId + "Clipboard did not contain any items.");
            return amazonList;
        }
        
        return amazonList;
    }

    /**
     * saves the description and quantity for the current item in the order.
     * if the description is already defined for the order, it creates a new entry.
     * 
     * @param descr    - the description if the item
     * @param qty      - the quantity
     * @param newOrder - the current order contents
     */
    private static void setDescriptionAndQty (String descr, Integer qty, AmazonOrder newOrder) {
        // get the item selection in the order
        AmazonItem itemEntry = newOrder.getLastItem();
        LocalDate lastDeliveryDate = newOrder.getDeliveryDate();

        // if a description is already defined, we must have a new item in the order
        if (newOrder.getLastItem().getDescription() != null) {
            itemEntry = newOrder.addNewItem();
            GUILogPanel.outputInfoMsg (MsgType.INFO, "* Added new ITEM (" + newOrder.getItemCount() + ") in multi-item ORDER");
        }
        
        // get the max length of the description to save
        int maxlen = descr.length();
        int iMaxDescrLen = PropertiesFile.getPropertiesItem(Property.MaxLenDescription, 90);
        String truncDescript = descr.substring(0, (maxlen > iMaxDescrLen) ? iMaxDescrLen : maxlen);
        itemEntry.setDescription(truncDescript);
        itemEntry.setQuantity(qty);
        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Description: " + truncDescript);
        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Quantity: " + qty);

        // the delivery date will have been previously set here.
        // we must have this value in a seperate variable from newOrder, since multi-item
        // entries may change this value on the previous item entries if we set the value
        // before we have instantiated a new newOrder object
        if (lastDeliveryDate == null && ! itemEntry.getReturned()) {
            GUILogPanel.outputInfoMsg (MsgType.WARN, "Delivery date not setup prior to item description");
        }
        itemEntry.setDeliveryDate(lastDeliveryDate);
        GUILogPanel.outputInfoMsg (MsgType.PARSER, "END OF ITEM (" + newOrder.getItemCount() + ")");
    }

    /**
     * executes the action for the specified keyword found.
     * 
     * @param key      - the key entry found in the parsed line
     * @param line     - the contents of the line following the keyword (or the following line)
     * @param newOrder - the current order contents
     * 
     * @return true if the keyword was found and executed
     */    
    private boolean parseCommand (Keyword.KeyTyp key, String line, AmazonOrder newOrder) {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // get the item selection in the order
        AmazonItem itemEntry = newOrder.getLastItem();
        LocalDate lastDeliveryDate = newOrder.getDeliveryDate();
        boolean bSuccess = false;
        
        switch (key) {
            case ORDER_PLACED:
                // handled in parseOrder
                bSuccess = true;
                break;

            case ORDER_NUMBER:
                try {
                    String strOrderNum = Utils.getNextWord (line, 19, 19);
                    newOrder.setOrderNumber(strOrderNum);
                    GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Order #: " + strOrderNum);
                    newOrder.setDeliveryDate(null); // clear delivery date for new order
                    bSuccess = true;
                } catch (ParserException exMsg) {
                    GUILogPanel.outputInfoMsg (MsgType.WARN, "invalid format for Order #: " + line);
                }
                break;

            case DELIVERED: // fall through...
            case ARRIVING:
            case NOW_ARRIVING:
                LocalDate delivered = null;
                try {
                    delivered = DateFormat.getFormattedDate(line, true);
                } catch (ParserException exMsg) {
                    // next line will catch
                }
                if (delivered == null) {
                    GUILogPanel.outputInfoMsg (MsgType.WARN, "invalid char in " + key + " date: " + line);
                }
                if (itemEntry.isItemDefined()) {
                    itemEntry = newOrder.addNewItem();
                    GUILogPanel.outputInfoMsg (MsgType.INFO, "* Added new ITEM (" + newOrder.getItemCount() + ") in multi-item ORDER");
                }
                GUILogPanel.outputInfoMsg (MsgType.PARSER, "    " + key + ": " + delivered);
                // if the current item has already been defined, create a new one
                itemEntry.setDeliveryDate(delivered);
                newOrder.setDeliveryDate(delivered);
                bSuccess = true;
                break;

            case RETURNED: // fall through...
            case REFUNDED:
                if (itemEntry.isItemDefined()) {
                    itemEntry = newOrder.addNewItem();
                    GUILogPanel.outputInfoMsg (MsgType.INFO, "* Added new ITEM (" + newOrder.getItemCount() + ") in multi-item ORDER");
                }
                itemEntry.setReturned();
                GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Item " + key);
                bSuccess = true;
                break;

            case PACKAGE_LEFT:
                // if the DELIVERED date was skipped, it is another item arriving in the same package
                //  so the delivery date is the same.
                if (itemEntry.isItemDefined()) {
                    itemEntry = newOrder.addNewItem();
                    GUILogPanel.outputInfoMsg (MsgType.INFO, "* Added new ITEM (" + newOrder.getItemCount() + ") in multi-item ORDER");

                    if (lastDeliveryDate == null) {
                        GUILogPanel.outputInfoMsg (MsgType.WARN, functionId + "Delivery date not found for item!");
                    } else {
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "      (using last delivery date : " + lastDeliveryDate + ")");
                        itemEntry.setDeliveryDate(lastDeliveryDate);
                    }
                }
                bSuccess = true;
                break;

            case SHIP_TO:
                // we don't care about this
                GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Ship to: " + line);
                break;

            case SELLER:
                itemEntry.setSeller(line);
                GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Seller: " + line);
                break;

            case TOTAL_COST:
                // the next line will contain the total amount of the purchase
                GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Total: " + line);
                try {
                    int amount = Utils.getAmountValue(line.substring(1));
                    newOrder.setTotalCost(amount);
                    bSuccess = true;
                } catch (ParserException exMsg) {
                    GUILogPanel.outputInfoMsg (MsgType.WARN, "invalid format for Total cost: " + line);
                }
                break;

            case GROSS_COST:
                GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Gross: " + line);
                try {
                    newOrder.setGrossCost(Utils.getAmountValue(line));
                } catch (ParserException exMsg) {
                    GUILogPanel.outputInfoMsg (MsgType.WARN, "invalid format for Gross cost: " + line);
                }
                break;

            case TAXES:
                GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Taxes: " + line);
                try {
                    newOrder.setTaxCost(Utils.getAmountValue(line));
                } catch (ParserException exMsg) {
                    GUILogPanel.outputInfoMsg (MsgType.WARN, "invalid format for Taxes: " + line);
                }
                break;

            case ITEM_COST:
                try {
                    itemEntry.setItemCost(Utils.getAmountValue(line));
                    GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Item cost: " + line);
                } catch (ParserException exMsg) {
                    GUILogPanel.outputInfoMsg (MsgType.WARN, functionId + "Invalid format for Item cost: " + line);
                }
                break;
                        
            default:
                break;
        }
        return bSuccess;
    }
    
}
