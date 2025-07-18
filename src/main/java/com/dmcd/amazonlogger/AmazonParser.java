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
     * @throws ParserException
     * @throws IOException
     */
    public void parseWebData () throws ParserException, IOException {
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
                case Keyword.KeyTyp.HELLO_D:
                    if (strSheetSel == null || strSheetSel.contentEquals("Dan")) {
                        strSheetSel = "Dan";
                        Spreadsheet.selectSpreadsheetTab(strSheetSel);
                        GUIMain.setTabOwner(strSheetSel.toUpperCase());
                        GUILogPanel.outputInfoMsg(MsgType.PARSER, strSheetSel + "'s list selected");
                    } else {
                        throw new ParserException(functionId + "Invalid clip: current tab selection is Dan but previous clips are " + strSheetSel);
                    }
                    break;
                case Keyword.KeyTyp.HELLO_C:
                    if (strSheetSel == null || strSheetSel.contentEquals("Connie")) {
                        strSheetSel = "Connie";
                        Spreadsheet.selectSpreadsheetTab(strSheetSel);
                        GUIMain.setTabOwner(strSheetSel.toUpperCase());
                        GUILogPanel.outputInfoMsg(MsgType.PARSER, strSheetSel + "'s list selected");
                    } else {
                        throw new ParserException(functionId + "Invalid clip: current tab selection is Connie but previous clips are " + strSheetSel);
                    }
                    break;
                case Keyword.KeyTyp.ORDER_PLACED:
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
                        updateOrderListing();
                    } catch (ParserException exMsg) {
                        updateOrderListing();
                        Utils.throwAddendum(line, "ORDER_PLACED failure");
                    }
                    break;
                case Keyword.KeyTyp.ORDER_DETAILS:
                case Keyword.KeyTyp.ORDER_SUMMARY:
                    eClipType = ClipTyp.INVOICE;
                    eKeyId = Keyword.KeyTyp.NONE; // we don't need to re-process this keyword
                    GUILogPanel.outputInfoMsg (MsgType.PARSER, "'" + eClipType + "' clipboard");
                    try {
                        newList = parseOrd.parseOrders(eClipType, line, eKeyId);
                        AmazonOrder newOrder = newList.get(0); // there should only be 1 order entry in the list

                        // add the new order to the current orders we have accumulated
                        addDetailsToList (newOrder);
                        // update the current order info
                        updateOrderListing();
                    } catch (ParserException exMsg) {
                        updateOrderListing();
                        Utils.throwAddendum(line, "ORDER_PLACED failure");
                    }
                    break;
            }
        }

        // file has been parsed, close the file
        clipReader.close();
            
        // if we captured any orders, we can now allow the spreadsheet to be updated
        if (! amazonList.isEmpty()) {
            GUIMain.enableUpdateButton(true);
        }
    }

    private static void updateOrderListing() {
        int itemCount = 0;
        int orderCount = 0;
        LocalDate startDate = null;

        GUIOrderPanel.printOrderHeader();

        // count and display the entries found.
        for (int ix = 0; ix < amazonList.size(); ix++) {
            AmazonOrder entry = amazonList.get(ix);
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
            LocalDate endDate = amazonList.getLast().getOrderDate();
            GUIMain.setOrderCount(orderCount, itemCount, startDate, endDate);
            GUILogPanel.outputInfoMsg(MsgType.PARSER, "Total orders in list = " + amazonList.size());
        }
    }
    
    /**
     * updates the spreadsheet file with the lists of AmazonOrders
     * 
     * @throws ParserException
     * @throws IOException
     */
    public static void updateSpreadsheet () throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (strSheetSel == null) {
            throw new ParserException(functionId + "spreadsheet sheet selection not made");
        }

        if (amazonList.isEmpty()) {
            GUILogPanel.outputInfoMsg(MsgType.WARN, functionId + "nothing to update");
            return;
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
            if (! amazonList.isEmpty()) {
                // find date of oldest valid entry to be added
                for (int ix = 0; ix < amazonList.size(); ix++) {
                    if (! amazonList.get(ix).isInvalidDate()) {
                        ixOldest = ix;
                        break;
                    }
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

                // to get the entries in chronological order, start with the last entry and work backwards.
                // let's proceed from the item number that matched and loop backwards to the more recent entries.
                int row = lastRow;
                for (int ixOrder = ixOldest; ixOrder < amazonList.size(); ixOrder++) {
                    AmazonOrder order = amazonList.get(ixOrder);
                    showItemListing(ixOrder, order);
                        
                    // output order item(s) to spreadsheet
                    int count = Spreadsheet.setSpreadsheetOrderInfo (row, order, true);
                    row += count;
                    bUpdate = true;
                }
            }

            // output changes to file, if any
            if (bUpdate) {
                // update display that shows the last entries in the spreadsheet
                Spreadsheet.showLastLineInfo();
                Spreadsheet.selectSpreadsheetTab (strSheetSel);
                Integer newLastLine = Spreadsheet.getLastRowIndex();
            
                // make a backup copy of the current file before saving new one.
                Spreadsheet.makeBackupCopy("-web-bak");
                
                // now save the updates to the file
                OpenDoc.saveToFile(strSheetSel);
                Integer actLastLine = Spreadsheet.getLastRowIndex();
                
                // TODO: verify the updates took place (last lines are correct) before clearing display
                if (!Objects.equals(newLastLine, actLastLine)) {
                    GUILogPanel.outputInfoMsg(MsgType.WARN, "Spreadsheet file was not updated correctly - last line is " + actLastLine + " instead of " + newLastLine);
                } else {
                    GUIOrderPanel.clearMessages();
                }
            }

            // erase the update button until we read in more data
            GUIMain.enableUpdateButton(false);

            // reset the lists, since we used it already
            strSheetSel = null;
            amazonList.clear();
            GUIMain.clearTabOwner();
            GUIMain.clearOrderCount();

        } catch (IOException ex) {
            throw new IOException(functionId + ex.getMessage());
        }
    }

    /**
     * finds the specified order number entry in the list
     * 
     * @param strOrdNum - the order number to search for
     * @param aList     - the list to search
     * 
     * @return the index of the order (-1 if not found)
     */
    private int findOrderNumberInList (String strOrdNum, ArrayList<AmazonOrder> aList) {
        for (int ix = 0; ix < aList.size(); ix++) {
            if (strOrdNum.contentEquals(aList.get(ix).getOrderNumber())) {
                return ix;
            }
        }
        return -1;
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
        String strDate = Spreadsheet.getLastDate (strSheetSel);
        Integer lastOrderDate = DateFormat.cvtSSDateToInteger(strDate, false);
        
        GUILogPanel.outputInfoMsg (MsgType.PARSER, "Checking validity of the " + newList.size() + " orders in the list from year " + ssYear + "...");

        // mark entries as invalid that are from wrong year or are already in the spreadsheet
        boolean bError = false;
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
            }
            // mark invalid those entries prior to the last date in the spreadsheet
            int entryDate = DateFormat.convertDateToInteger (order.getOrderDate(), false);
            if (entryDate > 0 && lastOrderDate != null) {
                if (entryDate < lastOrderDate) {
                    GUILogPanel.outputInfoMsg(MsgType.PARSER, "skip order # " + orderNum + " - already in spreadsheet: " + strDate);
                    order.setInvalidDate();
                } else if (entryDate == lastOrderDate) {
                    // if the date matches the last entry in the spreadsheet, we must verify whether the order number is found.
                    // if so, eliminate it
                    try {
                        int row = Spreadsheet.findItemNumber(orderNum);
                        if (row > 0) {
                            GUILogPanel.outputInfoMsg(MsgType.PARSER, "skip order # " + orderNum + " - already in spreadsheet: " + strDate);
                            order.setInvalidDate();
                        }
                    } catch (ParserException exMsg) {
                        // ignore error
                    }
                }
            }
        }

        if (bError) {
            GUILogPanel.outputInfoMsg (MsgType.WARN,functionId + "Missing required data in list entries");
            return oldList;
        }
        if (newList.isEmpty()) {
            GUILogPanel.outputInfoMsg (MsgType.WARN, functionId + "No valid orders to add");
            return oldList;
        }
        
        // if old list is empty, we can just use the new list as is
        if (oldList == null || oldList.isEmpty()) {
            return newList;
        }

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

        return finalList;
    }

    /**
     * adds the specified AmazonOrder list containing detail info to amazonList.
     * If amazonList is not empty, this will search for a match to the order number and
     *  and its additional info to that entry, if found.
     * If not found, it will find the location in the array in which to place it to keep
     *  entries in order of oldest to newest dates.
     * If amazonList is empty, it simply adds the entry to it.
     * 
     * @param entry - the detailed order info
     * 
     * @return true if entry was added, false if an entry was modified or not added
     */
    private boolean addDetailsToList (AmazonOrder newOrder) throws ParserException {
        String orderNum = newOrder.getOrderNumber();
        LocalDate orderDate = newOrder.getOrderDate();
        int length = amazonList.size();
        boolean bFound = false;

        if (! amazonList.isEmpty()) {
            int newDate = DateFormat.convertDateToInteger (orderDate, false);
            AmazonOrder entry = findOrderInList (orderNum);
            if (entry != null) {
                // update the added info not included in order info
                entry.addDetails (newOrder);
                GUILogPanel.outputInfoMsg(MsgType.PARSER, "modified order # " + orderNum + " in order list");
                return false;
            }
            
            if (newDate >= 0) {
                // find location to add entry to list
                for (int ix = 0; ix < length; ix++) {
                    int entryDate = DateFormat.convertDateToInteger (amazonList.get(ix).getOrderDate(), false);
                    if (newDate <= entryDate) {
                        amazonList.set(ix, newOrder);
                        GUILogPanel.outputInfoMsg(MsgType.PARSER, "inserted order # " + orderNum + " into order list at index " + ix);
                        length = ix;
                        bFound = true;
                        break;
                    }
                }
            }
        }
        
        boolean bEntryAdded = true;
        if (! bFound) {
            // date is past all entries, so add to end of list
            amazonList.add(newOrder);
            GUILogPanel.outputInfoMsg(MsgType.PARSER, "added order # " + orderNum + " to order list at index " + length);
        }
        
        // new entry added, check if the item is already in the spreadsheet
        if (Spreadsheet.findItemNumber(orderNum) >= 0) {
            amazonList.get(length).setInvalidDate();
            bEntryAdded = false;
        }
        
        return bEntryAdded;
    }
    
    private static AmazonOrder findOrderInList (String orderNumber) {
        for (int ix = 0; ix < amazonList.size(); ix++) {
            if (orderNumber.contentEquals(amazonList.get(ix).getOrderNumber())) {
                return amazonList.get(ix);
            }
        }
        return null;
    }
    
    private static String getYYYYMMDD (LocalDate date) {
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
