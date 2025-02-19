package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.AmazonReader.keyword;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 *
 * @author dan
 */
public class AmazonParser {
    
    private static final String CLASS_NAME = "AmazonParser";
    
    private ClipboardReader clipReader = null;
    private static String strSheetSel = null;
    private static ArrayList<AmazonOrder> amazonList = new ArrayList<>();
    private static ArrayList<AmazonOrder> detailList = new ArrayList<>();

    public AmazonParser () {
        // run using input from system clipboard
        clipReader = new ClipboardReader();
    }
    
    public AmazonParser (File clipFile) {
        // run from using input from file
        clipReader = new ClipboardReader (clipFile);
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
        String functionId = CLASS_NAME + ".parseWebData: ";
        
        String line;
        Keyword.KeyTyp eKeyId;
        Keyword.ClipTyp eClipType = Keyword.ClipTyp.NONE;
        ArrayList<AmazonOrder> newList;

        // create a keyword instance to use
        keyword = new Keyword();

        // first, we check for which type of file we are reading
        while (eClipType == Keyword.ClipTyp.NONE) {
            // get next line from clipboard
            line = clipReader.getLine();
            if (line == null)
                break;
            line = line.stripLeading();
            if (line.isBlank())
                continue;

            Keyword.KeywordClipEntry keywordInfo = keyword.getKeywordClip(line);
            eClipType = keywordInfo.eClipType;
            eKeyId = keywordInfo.eKeyId;
            LocalDate startDate, endDate;

            switch (eKeyId) {
                default:
                    break;
                case Keyword.KeyTyp.HELLO_D:
                    if (strSheetSel == null || strSheetSel.contentEquals("Dan")) {
                        strSheetSel = "Dan";
                        Spreadsheet.selectSpreadsheetTab(strSheetSel);
                        frame.setTabOwner(strSheetSel.toUpperCase());
                        frame.outputInfoMsg(UIFrame.STATUS_PARSER, strSheetSel + "'s list selected");
                    } else {
                        throw new ParserException(functionId + "Invalid clip: current tab selection is Dan but previous clips are " + strSheetSel);
                    }
                    break;
                case Keyword.KeyTyp.HELLO_C:
                    if (strSheetSel == null || strSheetSel.contentEquals("Connie")) {
                        strSheetSel = "Connie";
                        Spreadsheet.selectSpreadsheetTab(strSheetSel);
                        frame.setTabOwner(strSheetSel.toUpperCase());
                        frame.outputInfoMsg(UIFrame.STATUS_PARSER, strSheetSel + "'s list selected");
                    } else {
                        throw new ParserException(functionId + "Invalid clip: current tab selection is Connie but previous clips are " + strSheetSel);
                    }
                    break;
                case Keyword.KeyTyp.ORDER_PLACED:
                    frame.outputInfoMsg (UIFrame.STATUS_PARSER, "'ORDERS' clipboard");
                    ParseOrders parseOrd = new ParseOrders();
                    newList = parseOrd.parseOrders(clipReader, line, eKeyId);
                    // merge list with current running list (in chronological order)
                    amazonList = addOrdersToList (amazonList, newList);
                    int itemCount = 0;
                    for (int ix = 0; ix < amazonList.size(); ix++) {
                        itemCount += amazonList.get(ix).item.size();
                    }
                    if (itemCount > 0) {
                        startDate = amazonList.get(0).getOrderDate();
                        endDate = amazonList.get(amazonList.size()-1).getOrderDate();
                        frame.setOrderCount(amazonList.size(), itemCount, startDate, endDate);
                        frame.outputInfoMsg(UIFrame.STATUS_PARSER, "Total orders in list = " + amazonList.size());
                    }
                    break;
                case Keyword.KeyTyp.DETAILS:
                    frame.outputInfoMsg (UIFrame.STATUS_PARSER, "'DETAILS' clipboard");
                    ParseDetails parseDet = new ParseDetails();
                    AmazonOrder newOrder = parseDet.parseDetails(clipReader, line);
                    // add the new order to the current detailed orders we have accumulated,
                    //  but keep them in chronological order (oldest to newest)
                    boolean bPlaced = false;
                    for (int ix = 0; ix < detailList.size(); ix++) {
                        if (newOrder.getOrderDate().isBefore(detailList.get(ix).getOrderDate())) {
                            detailList.add(ix, newOrder);
                            bPlaced = true;
                            frame.outputInfoMsg (UIFrame.STATUS_PARSER, "- inserted entry at index " + ix);
                            break;
                        }
                    }
                    if (!bPlaced) {
                        // later date than all the rest, add it to the end
                        detailList.add(newOrder);
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "- added entry to end of list");
                    }
                    itemCount = 0;
                    for (int ix = 0; ix < detailList.size(); ix++) {
                        itemCount += detailList.get(ix).item.size();
                    }
                    if (itemCount > 0) {
                        startDate = detailList.get(0).getOrderDate();
                        endDate = detailList.get(detailList.size()-1).getOrderDate();
                        frame.setDetailCount(detailList.size(), itemCount, startDate, endDate);
                        frame.outputInfoMsg(UIFrame.STATUS_PARSER, "Total items in detailed list = " + itemCount);
                    }
                    break;
                case Keyword.KeyTyp.INVOICE:
                    frame.outputInfoMsg (UIFrame.STATUS_PARSER, "'INVOICE' clipboard");
//                    parseInvoice();
                    eClipType = Keyword.ClipTyp.NONE;
                    break;
            }
        }

        // file has been parsed, close the file
        clipReader.close();
            
        // if we captured any orders, we can now allow the spreadsheet to be updated
        if (! amazonList.isEmpty() || ! detailList.isEmpty()) {
            frame.enableUpdateButton(true);
        }
    }

    /**
     * updates the spreadsheet file with the lists of AmazonOrders
     * 
     * @throws com.mycompany.amazonlogger.ParserException
     */
    public static void updateSpreadsheet () throws ParserException, IOException {
        String functionId = CLASS_NAME + ".updateSpreadsheet: ";
        
        if (strSheetSel == null) {
            throw new ParserException(functionId + "spreadsheet sheet selection not made");
        }

        if (amazonList.isEmpty() && detailList.isEmpty()) {
            frame.outputInfoMsg(UIFrame.STATUS_WARN, functionId + "nothing to update");
            return;
        }

        try {
            // make a backup copy of the current file before saving.
            Spreadsheet.makeBackupCopy("-web-bak");

            // select the specified spreadsheet tab
            Spreadsheet.selectSpreadsheetTab (strSheetSel);

            // process the 'Your Orders' pages if any
            boolean bExit = false;
            boolean bUpdate = false;
            if (! amazonList.isEmpty()) {
                // get the date range of the entries in the current page.
                // If they are all older, ignore this page.
                LocalDate dateStart = amazonList.get(0).getOrderDate();
                LocalDate dateEnd   = amazonList.get(amazonList.size()-1).getOrderDate();
                int startDate = DateFormat.convertDateToInteger(dateStart, false);
                int endDate   = DateFormat.convertDateToInteger(dateEnd, false);
                frame.outputInfoMsg(UIFrame.STATUS_INFO, "Date of newest entry in page:      "
                                            + DateFormat.convertDateToString(dateStart, false) + " (" + startDate + ")");
                frame.outputInfoMsg(UIFrame.STATUS_INFO, "Date of oldest entry in page:      "
                                            + DateFormat.convertDateToString(dateEnd, false) + " (" + endDate + ")");

                // find the last row in the selected sheet. the next line is where we will add entries
                // and also start at the end of the amazon list, to add the entries in reverse order
                int lastRow = Spreadsheet.getLastRowIndex();
                int startIx = amazonList.size() - 1;
                if (Spreadsheet.isSheetEmpty()) {
                    frame.outputInfoMsg(UIFrame.STATUS_INFO, "This spreadsheet tab is currently empty");
                    frame.outputInfoMsg(UIFrame.STATUS_INFO, "All Amazon page entries will be copied to spreadsheet.");
                } else {
                    frame.outputInfoMsg(UIFrame.STATUS_INFO, "spreadsheet " + strSheetSel + " last row: " + lastRow);
                    String ssOrderDate   = Spreadsheet.getDateOrdered (lastRow - 1);
                    if (ssOrderDate == null || (ssOrderDate.length() != 5 && ssOrderDate.length() != 10)) {
                        throw new ParserException(functionId + "Invalid date in spreadsheet on row " + lastRow + ": " + ssOrderDate);
                    }
                    if (ssOrderDate.length() == 10) { // if it includes the year, trim it off
                        ssOrderDate = ssOrderDate.substring(5);
                    }

                    // get the date of the last entry in the spreadsheet
                    // (this gets returned in format: "MM-DD")
                    Integer lastOrderDate = DateFormat.cvtSSDateToInteger(ssOrderDate, false);
                    frame.outputInfoMsg(UIFrame.STATUS_INFO, "Date of last entry in spreadsheet: " + ssOrderDate + " (" + lastOrderDate + ")");

                    if (startDate < lastOrderDate) {
                        // all entries should already be in spreadsheet
                        bExit = true;
                    } else if (endDate > lastOrderDate) {
                        // the entire list of the entries in the page occurred after the last entry in
                        // the spreadsheet, so we just copy the entire list.
                        frame.outputInfoMsg(UIFrame.STATUS_INFO, "All Amazon page entries will be copied to spreadsheet.");
                    } else {
                        // OK, so either this page list contains the last entry or they are all new entries.
                        // search the list for the last entry from the spreadsheet to see if we only copy a partial list.
                        boolean bFound = false;
                        String ssOrderNumber = Spreadsheet.getOrderNumber(lastRow - 1);
                        frame.outputInfoMsg(UIFrame.STATUS_INFO, "Last order # in spreadsheet: " + ssOrderNumber);
                        for (startIx = amazonList.size() - 1; startIx >= 0; startIx--) {
                            // find matching order number (if it is in there)
                            AmazonOrder ixOrder = amazonList.get(startIx);
                            if (ssOrderNumber.contentEquals(ixOrder.getOrderNumber())) {
                                bFound = true;
                                frame.outputInfoMsg(UIFrame.STATUS_INFO, "Order # found at index: " + startIx + ", " + ixOrder.item.size() + " items");
                                startIx--;  // go to next item to copy
                                break;
                            }
                        }
                        if (startIx < 0) {
                            // the latest entry in the list was the one we were looking for, therefore there are no entries to add.
                             bExit = true;
                        } else if (!bFound) {
                            // entry wasn't found in list, so the list must all be just after the current last item
                            //  in spreadsheet, so we copy all entries.
                            startIx = amazonList.size() - 1;
                            frame.outputInfoMsg(UIFrame.STATUS_INFO, "All Amazon page entries will be copied to spreadsheet.");
                        }
                    }
                }

                // to get the entries in chronological order, start with the last entry and work backwards.
                // let's proceed from the item number that matched and loop backwards to the more recent entries.
                if (bExit) {
                    frame.outputInfoMsg(UIFrame.STATUS_WARN, functionId + "All Amazon page entries are already contained in spreadsheet.");
                    frame.outputInfoMsg(UIFrame.STATUS_INFO, "If there is a more recent page, copy it to the file and try again.");
                } else {
                    frame.outputInfoMsg(UIFrame.STATUS_NORMAL, "Appending the following rows starting at row: " + (lastRow + 1));
                    int row = lastRow;
                    for (int ixOrder = startIx; ixOrder >= 0; ixOrder--) {
                        AmazonOrder order = amazonList.get(ixOrder);
                        showItemListing(ixOrder, order);
                        
                        // output order item(s) to spreadsheet
                        int count = Spreadsheet.setSpreadsheetOrderInfo (row, order, true);
                        row += count;
                        bUpdate = true;
                    }
                }
            }

            // now process the 'Order Details' pages if any
            if (! detailList.isEmpty()) {
                for (int ixOrder = 0; ixOrder < detailList.size(); ixOrder++) {
                    // find the row entry in the spreadsheet to the order we are amending
                    AmazonOrder order = detailList.get(ixOrder);
                    String strOrderNum = order.getOrderNumber();
                    int row = Spreadsheet.findItemNumber (strOrderNum);
                    if (row < 0) {
                        frame.outputInfoMsg(UIFrame.STATUS_WARN, functionId + "Index " + ixOrder +
                                            " Order " + strOrderNum + " not found in spreadsheet");
                    } else {
                        // save the detailed info to spreadsheet class
                        showItemListing(ixOrder, order);
                        Spreadsheet.setSpreadsheetOrderInfo (row, order, false);
                        bUpdate = true;
                    }
                }
            }

            // output changes to file, if any
            if (bUpdate) {
                Spreadsheet.saveSpreadsheetFile();
            }

            // erase the update button until we read in more data
            frame.enableUpdateButton(false);

            // reset the lists, since we used it already
            strSheetSel = null;
            amazonList.clear();
            detailList.clear();
            frame.clearTabOwner();
            frame.clearOrderCount();
            frame.clearDetailCount();

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
        String functionId = CLASS_NAME + ".addOrdersToList: ";
        
        ArrayList<AmazonOrder> finalList, appendList;
        
        // if the new list is empty, just use the original list passed
        if (newList == null || newList.isEmpty())
            return oldList;

        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "Checking validity of the " + newList.size() + " orders in the list...");

        // eliminate any entries from new list being added that are from wrong year
        // (work from last to first so we don't get messed up when deleting entries)
        boolean bError = false;
        for (int ix = newList.size()-1; ix >= 0; ix--) {
            AmazonOrder order = newList.get(ix);
            String orderNum = order.getOrderNumber();
            LocalDate orderDate = order.getOrderDate();
            String strDate = DateFormat.convertDateToString (orderDate, true);

            if (! order.isOrderComplete()) {
                frame.outputInfoMsg (UIFrame.STATUS_WARN, functionId + "Incomplete data in entry " + ix + ": order #: " + orderNum);
                bError = true;
            }
            Integer ssYear = Spreadsheet.getSpreadsheetYear();
            if (ssYear == null) {
                throw new ParserException(functionId + "Spreadsheet header is missing year");
            }
            if (orderDate.getYear() != ssYear) {
                frame.outputInfoMsg(UIFrame.STATUS_PARSER, "skip order # " + orderNum + " - wrong year: " + strDate);
                newList.remove(ix);
            }
        }

        if (bError) {
            frame.outputInfoMsg (UIFrame.STATUS_WARN,functionId + "Missing data in list entries");
            return oldList;
        }
        if (newList.isEmpty()) {
            frame.outputInfoMsg (UIFrame.STATUS_WARN, functionId + "No valid orders to add");
            return oldList;
        }
        
        // if old list is empty, we can just use the new list as is
        if (oldList == null || oldList.isEmpty()) {
            return newList;
        }

        // both lists are valid...
        // determine which list is older - we want the oldest orders first in the list
        LocalDate newDateStart = newList.get(0).getOrderDate();
        LocalDate newDateEnd   = newList.get(newList.size()-1).getOrderDate();
        LocalDate oldDateStart = oldList.get(0).getOrderDate();
        LocalDate oldDateEnd   = oldList.get(oldList.size()-1).getOrderDate();
        if (newDateStart.isAfter(oldDateStart)) {
            // starting dates check...
            // newList is more recent, copy newList first
            finalList = newList;
            appendList = oldList;
            frame.outputInfoMsg(UIFrame.STATUS_PARSER, "new list is newer than orig list on start dates: "
                        + newDateStart.getYear() + "-" + newDateStart.getMonthValue() + "-" + newDateStart.getDayOfMonth() + "  vs  "
                        + oldDateStart.getYear() + "-" + oldDateStart.getMonthValue() + "-" + oldDateStart.getDayOfMonth()  );
        } else if (newDateStart.isBefore(oldDateStart)) {
            // oldList is more recent, copy oldList first
            finalList = oldList;
            appendList = newList;
            frame.outputInfoMsg(UIFrame.STATUS_PARSER, "new list is older than orig list on start dates: "
                        + newDateStart.getYear() + "-" + newDateStart.getMonthValue() + "-" + newDateStart.getDayOfMonth() + "  vs  "
                        + oldDateStart.getYear() + "-" + oldDateStart.getMonthValue() + "-" + oldDateStart.getDayOfMonth()  );
        } else if (newDateEnd.isAfter(oldDateEnd)) {
            // the starting dates are the same, so the ending dates must be different
            //   or they are the same date ranges, which means we could do either.
            // ending dates check
            // newList is more recent, copy newList first
            finalList = newList;
            appendList = oldList;
            frame.outputInfoMsg(UIFrame.STATUS_PARSER, "new list is newer than orig list on end dates: "
                        + newDateEnd.getYear() + "-" + newDateEnd.getMonthValue() + "-" + newDateEnd.getDayOfMonth() + "  vs  "
                        + oldDateEnd.getYear() + "-" + oldDateEnd.getMonthValue() + "-" + oldDateEnd.getDayOfMonth()  );
        } else if (newDateEnd.isBefore(oldDateEnd)) {
            // oldList is more recent, copy oldList first
            finalList = oldList;
            appendList = newList;
            frame.outputInfoMsg(UIFrame.STATUS_PARSER, "new list is older than orig list on end dates: "
                        + newDateEnd.getYear() + "-" + newDateEnd.getMonthValue() + "-" + newDateEnd.getDayOfMonth() + "  vs  "
                        + oldDateEnd.getYear() + "-" + oldDateEnd.getMonthValue() + "-" + oldDateEnd.getDayOfMonth()  );
        } else {
            // both have the same date ranges (must either be the same list repeated or all purchases
            // are on the same date, so it doesn't matter because we will throw out all duplicate entries.
            // let's just copy the newList first.
            finalList = newList;
            appendList = oldList;
        }

        // append the older entries to the newer
        for (int ix = 0; ix < appendList.size(); ix++) {
            // skip any entries already in list
            String orderNum = appendList.get(ix).getOrderNumber();
            int entry = findOrderNumberInList (orderNum, finalList);
            if (entry < 0) {
                // new entry, add to end of list
                finalList.add(appendList.get(ix));
            } else {
                frame.outputInfoMsg(UIFrame.STATUS_PARSER, "skip order # " + orderNum + " - duplicate entry");
            }
        }

        return finalList;
    }

    private static void showItemListing (int ixOrder, AmazonOrder order) {
        int multi_count = order.item.size();
        for (int ixItem = 0; ixItem < multi_count; ixItem++) {
            AmazonItem item = order.item.get(ixItem);
            frame.outputInfoMsg( UIFrame.STATUS_NORMAL,
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
