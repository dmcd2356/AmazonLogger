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

/**
 *
 * @author dan
 */
public class ParseDetails {
    
    private static final String CLASS_NAME = ParseDetails.class.getSimpleName();

    public ParseDetails() {
    }
    
    // This creates the order information for the parser
    class OrderInfo {
        Keyword.KeyTyp  orderId;    // the order id
        Keyword.DataTyp dataType;   // type of data (INLINE, NEXTLINE, NONE)
        int             keyLength;  // length of keyword found
        
        // this is used when executing a line read from the clipboard
        OrderInfo (String line) {
            Keyword.KeywordInfo keywordInfo = Keyword.getKeyword(line);
            
            if (keywordInfo != null) {
                orderId   = keywordInfo.eKeyId;
                keyLength = keywordInfo.keyLength;
                dataType  = Keyword.getDataTypeInvoice (keywordInfo.eKeyId);
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
            dataType  = Keyword.getDataTypeInvoice (order);
        }
    }
    
    /**
     * parses the clipboard data line by line to extract the order information from a "Order Details" clip.
     * 
     * @param clip    - the clipboard reader to read from
     * @param line    - the line previously caught and passed to this module to execute
     * 
     * @return the AmazonOrder entry that was extracted from the clip
     * 
     * @throws ParserException 
     * @throws IOException 
     */
    public AmazonOrder parseDetails (ClipboardReader clip, String line) throws ParserException, IOException {
        OrderInfo keywordInfo = null;
        OrderInfo savedKey = null;
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
                line = clip.getLine();
                if (line == null)
                    break;
                line = line.stripLeading();
                if (line.isBlank())
                    continue;

                // parse line to check for command
                savedKey = new OrderInfo(line);
                if (savedKey.orderId == Keyword.KeyTyp.END_OF_RECORD) {
                    // exit if we completed loop
                    GUILogPanel.outputInfoMsg (MsgType.PARSER, "END OF ITEMS");
                    keywordInfo = new OrderInfo();
                    keywordInfo.makeOrder(Keyword.KeyTyp.COMPLETE);
                    break;
                }
            }

            // reset the skip read flag
            bSkipRead = false;
            
            // see if we have a pending command (next line contains the data)
            if (savedKey == null) {
                keywordInfo = new OrderInfo();
            }
            if (keywordInfo == null || keywordInfo.orderId == Keyword.KeyTyp.NONE) {
                keywordInfo = savedKey;
                if (keywordInfo.orderId == Keyword.KeyTyp.NONE) {
                    continue;
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
                
                switch (keywordInfo.orderId) {
                    case Keyword.KeyTyp.DELIVERED: // fall through...
                    case Keyword.KeyTyp.ARRIVING:  // fall through...
                    case Keyword.KeyTyp.NOW_ARRIVING:
                        LocalDate delivered = DateFormat.getFormattedDate(line, true);
                        if (delivered == null) {
                            throw new ParserException("ParseDetails.parseDetails: invalid char in " + keywordInfo.orderId + " date: " + line);
                        }
                        if (newItem.isItemDefined()) {
                            GUILogPanel.outputInfoMsg (MsgType.INFO, "* Added new ITEM in multi-item ORDER");
                            newItem = newOrder.addNewItem();
                        }
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    " + keywordInfo.orderId + ": " + delivered);
                        // if the current item has already been defined, create a new one
                        newItem.setDeliveryDate(delivered);
                        lastDeliveryDate = delivered;  // save last delivery date
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.SELLER:
                        newItem.setSeller(line);
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Seller: " + line);
                        keywordInfo = null; // command complete
                        continue;

                    case Keyword.KeyTyp.ITEM_COST:
                        // we only count it if it follows an item
                        if (bItemFound) {
                            newItem.setItemCost(Utils.getAmountValue(line));
                            GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Item cost: " + line);
                            bItemFound = false;
                        }
                        else {
                            GUILogPanel.outputInfoMsg (MsgType.INFO, "skipping ITEM_COST - item not defined yet");
                        }
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.RETURNED:
                        newItem.setReturned();
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Item returned");
                        keywordInfo = null; // command complete
                        break;
                        
                    case Keyword.KeyTyp.REFUNDED:
                        newItem.setReturned();
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Item refunded");
                        keywordInfo = null; // command complete
                        break;
                        
                    case Keyword.KeyTyp.PACKAGE_LEFT:
                        if (newItem.isItemDefined()) {
                            GUILogPanel.outputInfoMsg (MsgType.INFO, "* Added new ITEM in multi-item ORDER");
                            newItem = newOrder.addNewItem();
                        }
                        // item description should be the next entry, so advance to the next state.
                        keywordInfo = new OrderInfo();
                        keywordInfo.makeOrder(Keyword.KeyTyp.DESCRIPTION);
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
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Description: " + descript1);
                        
                        // if delivery date was not presented for the item, use date of last item
                        if (newItem.getDeliveryDate() == null) {
                            if (lastDeliveryDate == null) {
                                GUILogPanel.outputInfoMsg (MsgType.WARN, "ParseDetails.parseDetails: Delivery date not found for item!");
                            } else {
                                GUILogPanel.outputInfoMsg (MsgType.PARSER, "      (using last delivery date : " + lastDeliveryDate + ")");
                                newItem.setDeliveryDate(lastDeliveryDate);
                            }
                        }
                        // item has been found, so we can now check for item cost
                        bItemFound = true;
                        // check if we have an optional quantity value in the next line
                        keywordInfo = new OrderInfo();
                        keywordInfo.makeOrder(Keyword.KeyTyp.QUANTITY);
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
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Quantity: " + quantity);
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
                GUILogPanel.outputInfoMsg (MsgType.DEBUG, "  - setting bReadData true for next line");
                bReadData = true;
            } else {
                // these commands must be completed after the next line of input is read,
                // so the command line parsing is skipped for them
                bReadData = false;
                GUILogPanel.outputInfoMsg (MsgType.DEBUG, "  - bReadData true: parsing data: " + line);

                switch (keywordInfo.orderId) {
                    case Keyword.KeyTyp.ORDER_PLACED:
                        // now extract date from begining of string
                        LocalDate date = DateFormat.getFormattedDate (line, true);
                        if (date == null) {
                            throw new ParserException("ParseDetails.parseDetails: invalid char in " + keywordInfo.orderId + " date: " + line);
                        }
                        newOrder.setOrderDate(date);
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Order date: " + date.toString());
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.ORDER_NUMBER:
                        String strOrderNum = line;
                        if (strOrderNum.length() != 19) {
                            throw new ParserException("ParseDetails.parseDetails: Order number incorrect length: " + strOrderNum.length());
                        }
                        newOrder.setOrderNumber(strOrderNum);
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Order #: " + strOrderNum);
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.TOTAL_COST:
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Total: " + line);
                        newOrder.setTotalCost(Utils.getAmountValue(line));
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.GROSS_COST:
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Gross: " + line);
                        newOrder.setGrossCost(Utils.getAmountValue(line));
                        keywordInfo = null; // command complete
                        break;

                    case Keyword.KeyTyp.TAXES:
                        GUILogPanel.outputInfoMsg (MsgType.PARSER, "    Taxes: " + line);
                        newOrder.setTaxCost(Utils.getAmountValue(line));
                        keywordInfo = null; // command complete
                        break;

                    default:
                        break;
                }
            }
        } while (keywordInfo == null || keywordInfo.orderId != Keyword.KeyTyp.COMPLETE);

        // check if we were in the middle of a command when we terminated
        if (keywordInfo != null && keywordInfo.orderId != Keyword.KeyTyp.COMPLETE
                                && keywordInfo.orderId != Keyword.KeyTyp.NONE) {
            throw new ParserException("ParseDetails.parseDetails: EOF while parsing Order Details (state = " + keywordInfo.orderId + ")");
        }
        if (newOrder.item.isEmpty()) {
            GUILogPanel.outputInfoMsg(MsgType.WARN, "ParseDetails.parseDetails: Clipboard did not contain any items.");
        }
        
        return newOrder;
    }

}
