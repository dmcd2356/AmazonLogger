package com.mycompany.amazonlogger;

// Importing java input/output classes
import com.mycompany.amazonlogger.PropertiesFile.Property;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

public class AmazonReader {

    // GLOBALS
    public  static UIFrame frame;
    public  static Keyword keyword;
    public  static PropertiesFile props;
    
    
    private static String strSheetSel = null;
    private static ArrayList<AmazonOrder> amazonList = new ArrayList<>();
    private static ArrayList<AmazonOrder> detailList = new ArrayList<>();


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
    private ArrayList<AmazonOrder> addOrdersToList (ArrayList<AmazonOrder> oldList, ArrayList<AmazonOrder> newList) {
        ArrayList<AmazonOrder> finalList, appendList;
        
        // if the new list is empty, just use the original list passed
        if (newList == null || newList.isEmpty())
            return oldList;

        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "Checking validity of the " +
                                    newList.size() + " orders in the list...");

        // eliminate any entries from new list being added that are from wrong year
        // (work from last to first so we don't get messed up when deleting entries)
        boolean bError = false;
        for (int ix = newList.size()-1; ix >= 0; ix--) {
            AmazonOrder order = newList.get(ix);
            String orderNum = order.getOrderNumber();
            LocalDate orderDate = order.getOrderDate();
            String strDate = DateFormat.convertDateToString (orderDate, true);

            if (! order.isOrderComplete()) {
                frame.outputInfoMsg (UIFrame.STATUS_ERROR, "Incomplete data in entry " + ix + ": order #: " + orderNum);
                bError = true;
            }
            if (orderDate.getYear() != Spreadsheet.getSpreadsheetYear()) {
                frame.outputInfoMsg(UIFrame.STATUS_PARSER, "skipping order # " + orderNum + " - wrong year: " + strDate);
                newList.remove(ix);
            }
        }

        if (bError) {
            frame.outputInfoMsg (UIFrame.STATUS_ERROR,"addOrdersToList: Missing data in list entries");
            return oldList;
        }
        if (newList.isEmpty()) {
            frame.outputInfoMsg (UIFrame.STATUS_WARN, "No valid orders to add");
            return oldList;
        }
        
        // if old list is empty, we can just use the new list as is
        if (oldList == null || oldList.isEmpty()) {
            return newList;
        }

        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "Everything looks good!");
        
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
                frame.outputInfoMsg(UIFrame.STATUS_PARSER, "skipping order # " + orderNum + " - duplicate entry");
            }
        }

        return finalList;
    }

    /**
     * returns the start and end dates of an amazon list.
     * 
     * @param myList - the list to get the begin and end dates of
     * 
     * @return the date range in the form: "MM-DD - MM-DD" (null if empty list)
     */
    private String getListDateRange (ArrayList<AmazonOrder> myList) {
        if (myList == null || myList.isEmpty()) {
            return null;
        }
        LocalDate startDate = myList.get(myList.size()-1).getOrderDate();
        LocalDate endDate = myList.get(0).getOrderDate();
        if (startDate.isAfter(endDate)) {
            LocalDate tempDate = startDate;
            startDate = endDate;
            endDate = tempDate;
        }
        String strStart = DateFormat.convertDateToString(startDate, false);
        String strEnd   = DateFormat.convertDateToString(endDate, false);
        return strStart + " - " + strEnd;
    }
    
    /**
     * parses the data from the web text file (or clipboard).
     *  This extracts vital info from the web page data and saves it in an array.
     *  It then determines which page of the spreadsheet the page referred to and
     *   appends the data to the end of that spreadsheet page.
     * 
     * @return true if success
     */
    public boolean parseWebData () {
        String line;
        Keyword.KeyTyp eKeyId;
        Keyword.ClipTyp eClipType = Keyword.ClipTyp.NONE;
        ArrayList<AmazonOrder> newList;

        // create a keyword instance to use
        keyword = new Keyword();

	try {
            // read the clipboard
            boolean bSuccess = ClipboardReader.webClipOpen();
            if (! bSuccess) {
                frame.outputInfoMsg(UIFrame.STATUS_WARN, "No data found in clipboard.");
                return false;
            }

            // first, we check for which type of file we are reading
            while (eClipType == Keyword.ClipTyp.NONE) {
                // get next line from clipboard
                line = ClipboardReader.webClipGetLine();
                if (line == null)
                    break;
                if (line.isBlank())
                    continue;

                Keyword.KeywordClipEntry keywordInfo = keyword.getKeywordClip(line);
                eClipType = keywordInfo.eClipType;
                eKeyId = keywordInfo.eKeyId;

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
                            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Invalid clip: current tab selection is Dan but previous clips are " + strSheetSel);
                            return false;
                        }
                        break;
                    case Keyword.KeyTyp.HELLO_C:
                        if (strSheetSel == null || strSheetSel.contentEquals("Connie")) {
                            strSheetSel = "Connie";
                            Spreadsheet.selectSpreadsheetTab(strSheetSel);
                            frame.setTabOwner(strSheetSel.toUpperCase());
                            frame.outputInfoMsg(UIFrame.STATUS_PARSER, strSheetSel + "'s list selected");
                        } else {
                            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Invalid clip: current tab selection is Connie but previous clips are " + strSheetSel);
                            return false;
                        }
                        break;
                    case Keyword.KeyTyp.ORDER_PLACED:
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "'ORDERS' clipboard");
                        ParseOrders parseOrd = new ParseOrders();
                        newList = parseOrd.parseOrders(line, eKeyId);
                        // append list to current running list
                        amazonList = addOrdersToList (amazonList, newList);
                        int itemCount = 0;
                        for (int ix = 0; ix < amazonList.size(); ix++) {
                            itemCount += amazonList.get(ix).item.size();
                        }
                        String dateRange = getListDateRange (amazonList);
                        frame.setOrderCount(amazonList.size(), itemCount, dateRange);
                        frame.outputInfoMsg(UIFrame.STATUS_PARSER, "Total orders in list = " + amazonList.size());
                        break;
                    case Keyword.KeyTyp.DETAILS:
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "'DETAILS' clipboard");
                        ParseDetails parseDet = new ParseDetails();
                        AmazonOrder newOrder = parseDet.parseDetails(line);
                        // there should only be 1 order in list (may have multiple items in it, though)
                        // append entry to any current detailed orders we have accumulated
                        detailList.add(newOrder);
                        itemCount = 0;
                        for (int ix = 0; ix < detailList.size(); ix++) {
                            itemCount += detailList.get(ix).item.size();
                        }
                        dateRange = getListDateRange (detailList);
                        frame.setDetailCount(detailList.size(), itemCount, dateRange);
                        frame.outputInfoMsg(UIFrame.STATUS_PARSER, "Total items in detailed list = " + itemCount);
                        break;
                    case Keyword.KeyTyp.INVOICE:
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "'INVOICE' clipboard");
//                        parseInvoice();
                        eClipType = Keyword.ClipTyp.NONE;
                        break;
                }
            }

            // file has been parsed, close the file
            ClipboardReader.webClipClose();
            
            // if we captured any orders, we can now allow the spreadsheet to be updated
            if (! amazonList.isEmpty() || ! detailList.isEmpty()) {
                frame.enableUpdateButton(true);
            }
            
        } catch (ParserException | IOException ex) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, ex.getMessage());
            return false;
        }
        
        return true;
    }

    /**
     * updates the spreadsheet file with the lists of AmazonOrders
     */
    public void updateSpreadsheet () {
        if (strSheetSel == null) {
            frame.outputInfoMsg(UIFrame.STATUS_WARN, "updateSpreadsheet: spreadsheet sheet selection not made");
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
                        throw new ParserException("Invalid date in spreadsheet on row " + lastRow + ": " + ssOrderDate);
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
                    frame.outputInfoMsg(UIFrame.STATUS_WARN, "All Amazon page entries are already contained in spreadsheet.");
                    frame.outputInfoMsg(UIFrame.STATUS_WARN, "If there is a more recent page, copy it to the file and try again.");
                } else {
                    frame.outputInfoMsg(UIFrame.STATUS_NORMAL, "Appending the following rows starting at row: " + (lastRow + 1));
                    int row = lastRow;
                    for (int ixOrder = startIx; ixOrder >= 0; ixOrder--) {
                        AmazonOrder order = amazonList.get(ixOrder);
                        int count = Spreadsheet.setSpreadsheetOrderInfo (row, order, true);
                        row += count;
                        bUpdate = true;

                        // show what we did
                        int multi_count = order.item.size();
                        for (int ixItem = 0; ixItem < multi_count; ixItem++) {
                            AmazonItem item = order.item.get(ixItem);
                            frame.outputInfoMsg( UIFrame.STATUS_NORMAL,
                                             DateFormat.convertDateToString(order.getOrderDate(), true)
                                    + '\t' + order.getOrderNumber()
                                    + '\t' + order.getTotalCost()
                                    + '\t' + DateFormat.convertDateToString(item.getDeliveryDate(), true)
                                    + '\t' + ixItem + " of " + multi_count
                                    + '\t' + item.getQuantity()
                                    + '\t' + item.getDescription()
                                    );
                        }
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

                    // save the detailed info to spreadsheet class
                    Spreadsheet.setSpreadsheetOrderInfo (row, order, false);
                    bUpdate = true;
                }
            }

            // output changes to file, if any
            if (bUpdate) {
                Spreadsheet.saveSpreadsheetFile();
            }

            // erase the update button until we read in more data
            frame.enableUpdateButton(false);

            // reset the lists, since we used it already
            amazonList.clear();
            detailList.clear();
            strSheetSel = null;
            frame.setTabOwner("NONE");
            frame.setOrderCount(0, 0, null);
            frame.setDetailCount(0, 0, null);

        } catch (ParserException | IOException ex) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, ex.getMessage());
            frame.disableAllButton();
        }
    }
    
    // Main driver method
    public static void main(String[] args) throws Exception
    {
        // create the user interface to control things
        frame = new UIFrame();
        props = new PropertiesFile();
        
        // enable the messages as they were from prevous run
        frame.enableMessage(UIFrame.STATUS_PARSER, props.getPropertiesItem(Property.MsgParser     , "0").contentEquals("1"));
        frame.enableMessage(UIFrame.STATUS_SSHEET, props.getPropertiesItem(Property.MsgSpreadsheet, "0").contentEquals("1"));
        frame.enableMessage(UIFrame.STATUS_INFO  , props.getPropertiesItem(Property.MsgInfo       , "0").contentEquals("1"));
        frame.enableMessage(UIFrame.STATUS_DEBUG , props.getPropertiesItem(Property.MsgDebug      , "0").contentEquals("1"));
        frame.enableMessage(UIFrame.STATUS_PROPS , props.getPropertiesItem(Property.MsgProperties , "0").contentEquals("1"));
    }
}

class ParserException extends Exception
{
    // Parameterless Constructor
    public ParserException() {}

    // Constructor that accepts a message
    public ParserException(String message)
    {
        super(message);
    }

    // Constructor that accepts a message along with the line and its line number
    public ParserException(String message, String line)
    {
        super(message + line);
    }
}

