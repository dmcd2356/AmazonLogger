/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import com.dmcd.amazonlogger.GUILogPanel.MsgType;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.DEFAULT_OPTION;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;

/**
 *
 * @author dan
 */
public class AmazonParser {
    
    private static final String CLASS_NAME = AmazonParser.class.getSimpleName();
    
    private ClipboardReader clipReader = null;
    private static String strSheetSel = null;
    private static ArrayList<AmazonOrder> amazonList = new ArrayList<>();

    public enum ClipTyp { NONE, ORDERS, INVOICE };


    public AmazonParser () {
        // run using input from system clipboard
        clipReader = new ClipboardReader();
        GUIOrderPanel.clearMessages();
    }
    
    public AmazonParser (File clipFile) {
        // run from using input from file
        clipReader = new ClipboardReader (clipFile);
        GUIOrderPanel.clearMessages();
    }

    public static void initLists () {
        amazonList.clear();
        GUIOrderPanel.clearMessages();
    }
    
    /**
     * parses the data from the web text file (or clipboard).
     *  This extracts vital info from the web page data and saves it in an array.
     *  It then determines which page of the spreadsheet the page referred to and
     *   appends the data to the end of that spreadsheet page.
     * 
     * @return true if there is info in the Orders list
     * 
     * @throws ParserException
     * @throws IOException
     */
    public boolean parseWebData () throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        String line;
        Keyword.KeyTyp eKeyId;
        ClipTyp eClipType = ClipTyp.NONE;
        ArrayList<AmazonOrder> newList;

        // create a keyword instance to use
        Keyword keyword = new Keyword();
        ParseOrders parseOrd = new ParseOrders(clipReader);

        // first, we check for which type of file we are reading
        while (eClipType == ClipTyp.NONE) {
            // get next line from clipboard
            line = clipReader.getLine();
            if (line == null)
                break;
            line = line.stripLeading();
            if (line.isBlank())
                continue;

            Keyword.KeywordInfo keywordInfo = Keyword.getKeyword(line);
            if (keywordInfo == null) {
                continue;
            }
            eKeyId = keywordInfo.getKeyType();

            switch (eKeyId) {
                default:
                    break;
                case Keyword.KeyTyp.HELLO:
                    // this will identify the owner of the Amazon Orders clip
                    int offset = line.indexOf(' ');
                    String name = line.substring(offset).strip();
                    if (!name.isEmpty() && (strSheetSel == null || strSheetSel.contentEquals(name))) {
                        strSheetSel = name;
                        Spreadsheet.selectSpreadsheetTab(strSheetSel);
                        GUIMain.setTabOwner(strSheetSel.toUpperCase());
                        GUILogPanel.outputInfoMsg(MsgType.PARSER, strSheetSel + "'s list selected");
                    } else {
                        throw new ParserException(functionId + "Invalid clip: current tab selection is " + strSheetSel + " but previous clips are " + strSheetSel);
                    }
                    break;
                case Keyword.KeyTyp.ORDER_PLACED:
                    // this indicates the clip was an Amazon Orders type, which usually contains 10 orders
                    eClipType = ClipTyp.ORDERS;
                    GUILogPanel.outputInfoMsg (MsgType.PARSER, "'" + eClipType + "' clipboard");
                    try {
                        newList = parseOrd.parseOrders(eClipType, line, eKeyId);
                        if (newList != null && ! newList.isEmpty()) {
                            // reverse the order so the oldest entry is first and newest is last (this is how the spreadsheet is ordered)
                            Collections.reverse(newList);
                            // merge list with current running list (in chronological order)
                            amazonList = addOrdersToList (amazonList, newList);
                        }
                        // update the current order info
                        updateOrderListing(amazonList);
                    } catch (ParserException exMsg) {
                        Utils.throwAddendum(exMsg.getMessage(), "ORDER_PLACED failure");
                    }
                    break;
                case Keyword.KeyTyp.ORDER_DETAILS:
                case Keyword.KeyTyp.ORDER_SUMMARY:
                    // this indicates the clip was an Invoice type, which contains more details of a single order
                    eClipType = ClipTyp.INVOICE;
                    eKeyId = Keyword.KeyTyp.NONE; // we don't need to re-process this keyword
                    GUILogPanel.outputInfoMsg (MsgType.PARSER, "'" + eClipType + "' clipboard");
                    try {
                        newList = parseOrd.parseOrders(eClipType, line, eKeyId);
                        AmazonOrder newOrder = newList.get(0); // there should only be 1 order entry in the list

                        if (amazonList.isEmpty()) {
                            amazonList.add(newOrder);
                        } else {
                            // add the new order to the current orders we have accumulated
//                            addDetailsToList (newOrder);
                            insertOrderInList (newOrder);
                        }

                        // update the current order info
                        updateOrderListing(amazonList);
                    } catch (ParserException exMsg) {
                        Utils.throwAddendum(exMsg.getMessage(), "ORDER_PLACED failure");
                    }
                    break;
            }
        }

        // file has been parsed, close the file
        clipReader.close();
            
        // indicate if we placed any orders in the list
        return ! amazonList.isEmpty();
    }

    /**
     * updates the spreadsheet file with the lists of AmazonOrders
     * 
     * @return true if the spreadsheet file was updated and the old file was saved to backup.
     * 
     * @throws ParserException
     * @throws IOException
     */
    public static boolean updateSpreadsheet () throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        boolean bSuccess = false;
        if (amazonList.isEmpty()) {
            GUILogPanel.outputInfoMsg(MsgType.WARN, functionId + "nothing to update");
            return bSuccess;
        }

        // no tab selection made. If GUI mode, let the user choose. Else this is a failure.
        if (strSheetSel == null) {
            userSelectSpreadsheetTab();
        }

        try {
            // select the specified spreadsheet tab
            Spreadsheet.selectSpreadsheetTab (strSheetSel);

            // find the last row in the selected sheet. the next line is where we will add entries
            // and also start at the end of the amazon list, to add the entries in reverse order
            int lastRow = Spreadsheet.getLastRowIndex();
            if (Spreadsheet.isSheetEmpty()) {
                GUILogPanel.outputInfoMsg(MsgType.INFO, "This spreadsheet tab is currently empty");
                GUILogPanel.outputInfoMsg(MsgType.INFO, "All Amazon page entries will be copied to spreadsheet.");
            } else {
                GUILogPanel.outputInfoMsg(MsgType.INFO, "spreadsheet " + strSheetSel + " last row: " + lastRow);
                String ssOrderDate = Spreadsheet.getDateOrdered (lastRow - 1);
                if (ssOrderDate == null || (ssOrderDate.length() != 5 && ssOrderDate.length() != 10)) {
                    throw new ParserException(functionId + "Invalid date in spreadsheet on row " + lastRow + ": " + ssOrderDate);
                }
                if (ssOrderDate.length() == 10) { // if it includes the year, trim it off
                    ssOrderDate = ssOrderDate.substring(5);
                }

                // get the date of the last entry in the spreadsheet
                // (this gets returned in format: "MM-DD")
                Integer lastOrderDate = DateFormat.cvtSSDateToInteger(ssOrderDate, false);
                String ssLastOrderNumber = Spreadsheet.getOrderNumber(lastRow - 1);
                GUILogPanel.outputInfoMsg(MsgType.INFO, "Date of last entry in spreadsheet: " + ssOrderDate + " (" + lastOrderDate + ")");
                GUILogPanel.outputInfoMsg(MsgType.INFO, "Last order # in spreadsheet: " + ssLastOrderNumber);
            }

            
            // find the starting point: the oldest entry in the list that isn't in the spreadsheet already
            boolean bUpdate = false;
            int ixOldest = -1;
            for (int ix = 0; ix < amazonList.size(); ix++) {
                if (! amazonList.get(ix).isInvalidDate()) {
                    ixOldest = ix;
                    break;
                }
            }
            if (ixOldest < 0) {
                GUILogPanel.outputInfoMsg(MsgType.INFO, functionId + "All Amazon page entries are already contained in spreadsheet.");
                GUILogPanel.outputInfoMsg(MsgType.INFO, "If there is a more recent page, copy it to the file and try again.");
            } else {
                // get the date range of the entries in the current page.
                LocalDate dateStart = amazonList.get(ixOldest).getOrderDate();
                LocalDate dateEnd   = amazonList.getLast().getOrderDate();
                int startDate = DateFormat.convertDateToInteger(dateStart, false);
                int endDate   = DateFormat.convertDateToInteger(dateEnd, false);
                GUILogPanel.outputInfoMsg(MsgType.INFO, "Date of newest entry in page:      "
                                            + DateFormat.convertDateToString(dateStart, false) + " (" + startDate + ")");
                GUILogPanel.outputInfoMsg(MsgType.INFO, "Date of oldest entry in page:      "
                                            + DateFormat.convertDateToString(dateEnd, false) + " (" + endDate + ")");

                // copy the entries to the spreadsheet image.
                int appendRow = lastRow; // this is where we append any new listings
                for (int ixOrder = ixOldest; ixOrder < amazonList.size(); ixOrder++) {
                    AmazonOrder order = amazonList.get(ixOrder);
                    String orderNumber = order.getOrderNumber();
                    showItemListing(ixOrder, order);

                    // first, check if any of the entries is already in the spreadsheet.
                    // if so, we will simply overwrite them and remove them from the list to append.
                    int foundRow = Spreadsheet.findItemNumber(orderNumber); // this will be any we are overwriting
                    if (foundRow >= 0) {
                        Spreadsheet.setSpreadsheetOrderInfo (foundRow, order, true);
                    } else {
                        // otherwise, append entry to end of spreadsheet listings
                        int count = Spreadsheet.setSpreadsheetOrderInfo (appendRow, order, true);
                        appendRow += count;
                    }
                    bUpdate = true;
                }
            }

            // output changes to file, if any
            if (bUpdate) {
                // update display that shows the last entries in the spreadsheet
                Spreadsheet.showLastLineInfo();
                Integer newLastLine = Spreadsheet.getLastRowIndex();
            
                // make a backup copy of the current file before saving new one.
                Spreadsheet.makeBackupCopy(Spreadsheet.BackupType.Order);
                
                // now save the updates to the file
                File ssFile = Spreadsheet.getFileSelection();
                Spreadsheet.saveSheet(ssFile, strSheetSel);
                Spreadsheet.resizeSheets();
                Integer actLastLine = Spreadsheet.getLastRowIndex();
                
                // TODO: verify the updates took place (last lines are correct) before clearing display
                if (!Objects.equals(newLastLine, actLastLine)) {
                    GUILogPanel.outputInfoMsg(MsgType.WARN, "WARNING: Spreadsheet file was not updated correctly - last line is " + actLastLine + " instead of " + newLastLine);
                } else {
                    GUIOrderPanel.clearMessages();
                }
                bSuccess = true;
            } else {
                GUILogPanel.outputInfoMsg(MsgType.WARN, "NOTE: No order entries were valid to add to spreadsheet");
                GUIOrderPanel.clearMessages();
            }

            // reset the lists, since we used it already
            strSheetSel = null;
            amazonList.clear();
            GUIMain.setTabOwner(null);
            GUIMain.clearOrderCount();

        } catch (IOException ex) {
            throw new IOException(functionId + ex.getMessage());
        }
        
        return bSuccess;
    }

    /**
     * Allows the user to select the tab for output if this is being run from the GUI.
     * If not, it indicates an error.
     * 
     * @throws ParserException 
     */
    private static void userSelectSpreadsheetTab() throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (!AmazonReader.isOpModeGUI()) {
            throw new ParserException(functionId + "spreadsheet sheet selection not made");
        }

        int count = Spreadsheet.getTabCount();
        if (count <= 0) {
            throw new ParserException(functionId + "spreadsheet not selected");
        }
        ArrayList<String> nameSelection = new ArrayList<>();
        for (int ix = 0; ix < count; ix++) {
            nameSelection.add(Spreadsheet.getTabName(ix));
        }
        int select = JOptionPane.showOptionDialog(null, "Please select which account this belongs to",
                "No spreadsheet tab selection made", OK_CANCEL_OPTION, QUESTION_MESSAGE, null,
                nameSelection.toArray(), nameSelection.getFirst());
        if (select < 0 || select >= count) {
            throw new ParserException(functionId + "spreadsheet sheet selection not made");
        }
        strSheetSel = nameSelection.get(select);
        GUILogPanel.outputInfoMsg(MsgType.SSHEET, "USER ENTRY: Spreadsheet tab selection manually set to: " + strSheetSel);
    }

    /**
     * Allows the user to acknowledge an error that he can view on the screen before continuing.
     * 
     * @return true if user accepts the data as it is
     * 
     * @throws ParserException 
     */
    private static boolean userSelectAcknowledgeError(String message) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        GUILogPanel.outputInfoMsg (MsgType.WARN,functionId + message);

        // check if user wants to use the data as is (he will have to manually make corrections in the spreadsheet)
        if (AmazonReader.isOpModeGUI()) {
            String[] selections = { "Accept", "Reject" };
            int select = JOptionPane.showOptionDialog(null, "Error in Clipboard Parsing",
                message, DEFAULT_OPTION, ERROR_MESSAGE, null, selections, "Reject");
            if (select == 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * updates the GUI display with the order information loaded from the clipboard.
     */
    private static void updateOrderListing(ArrayList<AmazonOrder> orderList) {
        int itemCount = 0;
        int orderCount = 0;
        LocalDate startDate = null;

        if (!AmazonReader.isOpModeGUI()) {
            return;
        }
        
        GUIOrderPanel.printOrderHeader();

        // count and display the entries found.
        for (int ix = 0; ix < orderList.size(); ix++) {
            AmazonOrder entry = orderList.get(ix);
            boolean bIsListed = entry.isInvalidDate();
            if (! bIsListed) {
                orderCount++;
                itemCount += entry.getItemCount();
                if (startDate == null) {
                    startDate = entry.getOrderDate();
                }
            }
            GUIOrderPanel.printOrder(entry, bIsListed);
        }
        if (itemCount > 0) {
            LocalDate endDate = orderList.getLast().getOrderDate();
            GUIMain.setOrderCount(orderCount, itemCount, startDate, endDate);
            GUILogPanel.outputInfoMsg(MsgType.PARSER, "Total orders in list = " + orderList.size());
        }
    }

    /**
     * adds the specified AmazonOrder entry into the current Amazon list.
     * The entry order number is searched for in the current Amazon list and, if found,
     *  updates the entry with the new contents.
     * If not found, the entry is inserted based on the date. It is assumed the Amazon list
     *  is currently ordered by earliest to latest date.
     * 
     * @param newEntry - the entry to add
     * 
     * @return 
     */
    private void insertOrderInList (AmazonOrder newEntry) throws ParserException {
        // search for order number match & replace entry with new entry if found
        String orderNumber  = newEntry.getOrderNumber();
        for (int ix = 0; ix < amazonList.size(); ix++) {
            AmazonOrder listEntry = amazonList.get(ix);
            if (listEntry.getOrderNumber().contentEquals(orderNumber)) {
                amazonList.set(ix, newEntry);
                return;
            }
        }

        // order number not found...
        // search for 1st order date that is greater (more recent) than the new entry and
        // insert the new entry before that one.
        LocalDate orderDate = newEntry.getOrderDate();
        for (int ix = 0; ix < amazonList.size(); ix++) {
            AmazonOrder listEntry = amazonList.get(ix);
            if (listEntry.getOrderDate().isAfter(orderDate)) {
                amazonList.add(ix, newEntry);
                return;
            }
        }
        
        // obviously, this order entry must be the most recent, so let's add to end of list
        amazonList.addLast(newEntry);
    }
    
    /**
     * adds the specified AmazonOrder list to another.
     * The lists as they are read from the web pages are from newest entry to oldest.
     * The desired order in the spreadsheet is oldest entries at the top and newer entries appended.
     * However, we reverse the order of the list later when we copy the final 
     * concatenated list to the spreadsheet. Therefore, we want to order these
     * so the newest list is first, followed by the oldest list, so the compiled
     * list this outputs should be the newest order followed by older and older orders.
     * 
     * @param oldList - the first list
     * @param newList - the list to add
     * 
     * @return 
     */
    private ArrayList<AmazonOrder> addOrdersToList (ArrayList<AmazonOrder> oldList, ArrayList<AmazonOrder> newList) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        ArrayList<AmazonOrder> finalList, appendList;
        
        // if the new list is empty, just use the original list passed
        if (newList == null || newList.isEmpty())
            return oldList;

        // must know what year the spreadsheet is for. if missing, we can't eliminate any prev year entries
        Integer ssYear = Spreadsheet.getSpreadsheetYear();
        if (ssYear == null) {
            throw new ParserException(functionId + "Spreadsheet header is missing year");
        }
        
        // get the last date in the listing from the current spreadsheet
        String strDate = Spreadsheet.getLastDate ();
        Integer lastOrderDate = DateFormat.cvtSSDateToInteger(strDate, false);
        
        GUILogPanel.outputInfoMsg (MsgType.PARSER, "Checking validity of the " + newList.size() + " orders in the list from year " + ssYear + "...");

        // mark entries as invalid that are from wrong year or are already in the spreadsheet
        boolean bError = false;
        int invalidDates = 0;
        for (int ix = 0; ix < newList.size(); ix++) {
            AmazonOrder order = newList.get(ix);
            String orderNum = order.getOrderNumber();
            LocalDate orderDate = order.getOrderDate();
            strDate = DateFormat.convertDateToString (orderDate, true);

            if (! order.isOrderComplete()) {
                GUILogPanel.outputInfoMsg (MsgType.WARN, functionId + "Incomplete data in entry " + ix + ": order #: " + orderNum);
                bError = true;
            }
            // mark invalid if entry is not for the current year
            if (orderDate.getYear() != ssYear) {
                GUILogPanel.outputInfoMsg(MsgType.PARSER, "skip order # " + orderNum + " - wrong year: " + strDate);
                order.setInvalidDate();
                invalidDates++;
            }
            // mark invalid those entries prior to the last date in the spreadsheet
            int entryDate = DateFormat.convertDateToInteger (order.getOrderDate(), false);
            if (entryDate > 0 && lastOrderDate != null) {
                if (entryDate < lastOrderDate) {
                    GUILogPanel.outputInfoMsg(MsgType.PARSER, "skip order # " + orderNum + " - already in spreadsheet: " + strDate);
                    order.setInvalidDate();
                    invalidDates++;
                } else if (entryDate == lastOrderDate) {
                    // if the date matches the last entry in the spreadsheet, we must verify whether the order number is found.
                    // if so, eliminate it
                    try {
                        int row = Spreadsheet.findItemNumber(orderNum);
                        if (row > 0) {
                            GUILogPanel.outputInfoMsg(MsgType.PARSER, "skip order # " + orderNum + " - already in spreadsheet: " + strDate);
                            order.setInvalidDate();
                            invalidDates++;
                        }
                    } catch (ParserException exMsg) {
                        // ignore error
                    }
                }
            }
        }

        if (newList.isEmpty() || newList.size() == invalidDates) {
            GUILogPanel.outputInfoMsg (MsgType.WARN, functionId + "No valid orders to add");
            finalList = oldList;
        }
        else if (bError) {
            updateOrderListing(newList); // show the errors
            boolean bAccept = userSelectAcknowledgeError("Missing required data in one or more list entries");
            if (bAccept) {
                finalList = newList;
            } else {
                finalList = oldList;
            }
        }
        
        // if old list is empty, we can just use the new list as is
        else if (oldList == null || oldList.isEmpty()) {
            finalList = newList;
        }
        else {
            // both lists are valid...
            // determine which list is older - we want the oldest orders first in the list
            LocalDate newDateStart = newList.getFirst().getOrderDate();
            LocalDate newDateEnd   = newList.getLast().getOrderDate();
            LocalDate oldDateStart = oldList.getFirst().getOrderDate();
            LocalDate oldDateEnd   = oldList.getLast().getOrderDate();
            if (newDateStart.isAfter(oldDateStart)) {
                // newList is more recent, copy newList first
                appendList = newList;
                finalList  = oldList;
                GUILogPanel.outputInfoMsg(MsgType.PARSER, "new list is newer than orig list on start dates: "
                            + getYYYYMMDD(newDateStart) + "  vs  " + getYYYYMMDD(oldDateStart));
            } else if (newDateStart.isBefore(oldDateStart)) {
                // oldList is more recent, copy oldList first
                appendList = oldList;
                finalList  = newList;
                GUILogPanel.outputInfoMsg(MsgType.PARSER, "new list is older than orig list on start dates: "
                            + getYYYYMMDD(newDateStart) + "  vs  " + getYYYYMMDD(oldDateStart));
            } else if (newDateEnd.isAfter(oldDateEnd)) {
                // the starting dates are the same, so the ending dates may be different
                // newList is more recent, copy newList first
                appendList = newList;
                finalList  = oldList;
                GUILogPanel.outputInfoMsg(MsgType.PARSER, "new list is newer than orig list on end dates: "
                            + getYYYYMMDD(newDateEnd) + "  vs  " + getYYYYMMDD(oldDateEnd));
            } else if (newDateEnd.isBefore(oldDateEnd)) {
                // oldList is more recent, copy oldList first
                appendList = oldList;
                finalList  = newList;
                GUILogPanel.outputInfoMsg(MsgType.PARSER, "new list is older than orig list on end dates: "
                            + getYYYYMMDD(newDateEnd) + "  vs  " + getYYYYMMDD(oldDateEnd));
            } else {
                // both have the same date ranges (must either be the same list repeated or all purchases
                // are on the same date, so it doesn't matter because we will throw out all duplicate entries.
                // let's just copy the newList first.
                appendList = newList;
                finalList  = oldList;
            }

            // append the newer entries to the end of the older list
            for (int ix = 0; ix < appendList.size(); ix++) {
                // skip any entries already in list
                String orderNum = appendList.get(ix).getOrderNumber();
                int entry = findOrderNumberInList (orderNum, finalList);
                if (entry < 0) {
                    // new entry, add to end of list
                    finalList.add(appendList.get(ix));
                } else {
                    GUILogPanel.outputInfoMsg(MsgType.PARSER, "skip order # " + orderNum + " - duplicate entry");
                }
            }
        }

        return finalList;
    }

    /**
     * finds the specified order number entry in the list
     * 
     * @param orderNumber - the order number to search for
     * @param aList       - the list to search
     * 
     * @return the index of the order (-1 if not found)
     */
    private int findOrderNumberInList (String orderNumber, ArrayList<AmazonOrder> aList) {
        for (int ix = 0; ix < aList.size(); ix++) {
            if (orderNumber.contentEquals(aList.get(ix).getOrderNumber())) {
                return ix;
            }
        }
        return -1;
    }

    /**
     * convert LocalDate entry into a String of format: "YYYY-MM-DD".
     * 
     * @param date the date to convert
     * 
     * @return the formatted string as "YYYY-MM-DD" (empty string if null entry)
     */
    private static String getYYYYMMDD (LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.getYear() + "-" + date.getMonthValue() + "-" + date.getDayOfMonth();
    }
    
    private static void showItemListing (int ixOrder, AmazonOrder order) {
        int multi_count = order.getItemCount();
        for (int ixItem = 0; ixItem < multi_count; ixItem++) {
            AmazonItem item = order.getItem(ixItem);
            GUILogPanel.outputInfoMsg( MsgType.NORMAL,
                             "Order " + ixOrder + "-" + ixItem
                    + '\t' + DateFormat.convertDateToString(order.getOrderDate(), true)
                    + '\t' + order.getOrderNumber()
                    + '\t' + order.getTotalCost()
                    + '\t' + DateFormat.convertDateToString(item.getDeliveryDate(), true)
                    + '\t' + (ixItem + 1) + " of " + multi_count
                    + '\t' + item.getQuantity()
                    + '\t' + item.getDescription()
                    );
        }
    }

}
