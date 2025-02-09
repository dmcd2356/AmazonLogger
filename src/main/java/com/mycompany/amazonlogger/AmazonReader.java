package com.mycompany.amazonlogger;

// Importing java input/output classes
import com.mycompany.amazonlogger.PropertiesFile.Property;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AmazonReader {

    // GLOBALS
    public  static UIFrame frame;
    public  static Keyword keyword;
    public  static PropertiesFile props;

    private  ClipboardReader clipReader = null;
    private static File   ssheetFile = null;
    private static String strSheetSel = null;
    private static ArrayList<AmazonOrder> amazonList = new ArrayList<>();
    private static ArrayList<AmazonOrder> detailList = new ArrayList<>();


    public AmazonReader () {
        // run using input from system clipboard
        clipReader = new ClipboardReader();
    }
    
    public AmazonReader (File clipFile) {
        // run from using input from file
        clipReader = new ClipboardReader (clipFile);
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
                frame.outputInfoMsg (UIFrame.STATUS_ERROR, "AmazonReader.addOrdersToList: Incomplete data in entry " + ix + ": order #: " + orderNum);
                bError = true;
            }
            if (orderDate.getYear() != Spreadsheet.getSpreadsheetYear()) {
                frame.outputInfoMsg(UIFrame.STATUS_PARSER, "skip order # " + orderNum + " - wrong year: " + strDate);
                newList.remove(ix);
            }
        }

        if (bError) {
            frame.outputInfoMsg (UIFrame.STATUS_ERROR,"AmazonReader.addOrdersToList: Missing data in list entries");
            return oldList;
        }
        if (newList.isEmpty()) {
            frame.outputInfoMsg (UIFrame.STATUS_WARN, "AmazonReader.addOrdersToList: No valid orders to add");
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
            // first, we check for which type of file we are reading
            while (eClipType == Keyword.ClipTyp.NONE) {
                // get next line from clipboard
                line = clipReader.getLine();
                if (line == null)
                    break;
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
                            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "AmazonReader.parseWebData: Invalid clip: current tab selection is Dan but previous clips are " + strSheetSel);
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
                            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "AmazonReader.parseWebData: Invalid clip: current tab selection is Connie but previous clips are " + strSheetSel);
                            return false;
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
                            // later date than all the reast, add it to the end
                            detailList.add(newOrder);
                            frame.outputInfoMsg (UIFrame.STATUS_PARSER, "- added entry to end of list");
                        }
                        itemCount = 0;
                        for (int ix = 0; ix < detailList.size(); ix++) {
                            itemCount += detailList.get(ix).item.size();
                        }
                        if (itemCount > 0) {
                            startDate = amazonList.get(0).getOrderDate();
                            endDate = amazonList.get(amazonList.size()-1).getOrderDate();
                            frame.setDetailCount(detailList.size(), itemCount, startDate, endDate);
                            frame.outputInfoMsg(UIFrame.STATUS_PARSER, "Total items in detailed list = " + itemCount);
                        }
                        break;
                    case Keyword.KeyTyp.INVOICE:
                        frame.outputInfoMsg (UIFrame.STATUS_PARSER, "'INVOICE' clipboard");
//                        parseInvoice();
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
            
        } catch (ParserException ex) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, ex.getMessage());
            return false;
        }
        
        return true;
    }

    /**
     * updates the spreadsheet file with the lists of AmazonOrders
     */
    public static void updateSpreadsheet () {
        if (strSheetSel == null) {
            frame.outputInfoMsg(UIFrame.STATUS_WARN, "AmazonReader.updateSpreadsheet: spreadsheet sheet selection not made");
            return;
        }

        if (amazonList.isEmpty() && detailList.isEmpty()) {
            frame.outputInfoMsg(UIFrame.STATUS_WARN, "AmazonReader.updateSpreadsheet: nothing to update");
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
                        throw new ParserException("AmazonReader.updateSpreadsheet: Invalid date in spreadsheet on row " + lastRow + ": " + ssOrderDate);
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
                    frame.outputInfoMsg(UIFrame.STATUS_WARN, "AmazonReader.updateSpreadsheet: All Amazon page entries are already contained in spreadsheet.");
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
                        frame.outputInfoMsg(UIFrame.STATUS_WARN, "AmazonReader.updateSpreadsheet: Index " + ixOrder + " Order " + order.getOrderNumber() + " not found in spreadsheet");
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

        } catch (ParserException ex) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, ex.getMessage());
            frame.disableAllButton();
        }
        catch (IOException ex) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "AmazonReader.updateSpreadsheet: " + ex.getMessage());
            frame.disableAllButton();
        }
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

    private static String getTestPath () {
        String pathname = Utils.getPathFromPropertiesFile (Property.TestPath);
        if (pathname == null || pathname.isBlank()) {
            pathname = System.getProperty("user.dir");
        }
        return pathname;
    }
    
    private static File checkFilename (String fname, String type, String filetype, boolean bWritable) {
        if (filetype == null) {
            filetype = "";
        }
        if (type != null && !type.isBlank() && !fname.endsWith(type)) {
            System.out.println("ERROR: Invalid " + filetype + " filename: " + fname);
            System.exit(1);
        }
        if (fname == null || fname.isBlank()) {
            System.out.println("ERROR: Invalid " + filetype + " filename is blank");
            System.exit(1);
        }
        
        fname = getTestPath() + "/" + fname;
        File myFile = new File(fname);
        if (!myFile.canRead()) {
            System.out.println("ERROR: Invalid " + filetype + " file - no read access: " + fname);
            System.exit(1);
        }
        if (bWritable && !myFile.canWrite()) {
            System.out.println("ERROR: Invalid " + filetype + " file - no write access: " + fname);
            System.exit(1);
        }
        return myFile;
    }

    private static void checkReqParams (int params, String option, int ix, int argcount) throws ParserException {
        if (ix >= argcount - params)
            throw new ParserException("ERROR: Missing parameter for " + option + " option");
    }
    
    private static void runCommandLine (String[] args) throws ParserException {
        File pdfFile;
        String fname;
        String filetype;
        String option;

        for (int ix = 0; ix < args.length; ix++) {
            option = args[ix];
            switch (option) {
                case "-x":
                    System.out.println("EXITING");
                    break;
                case "-h":
                    System.out.println(" -h         = to print this message");
                    System.out.println(" -s <file>  = the name of the spreadsheet file to modify");
                    System.out.println(" -t <tab>   = the name of the tab selection in the spreadsheet (Dan or Connie)");
                    System.out.println(" -p <file>  = the name of the PDF file to execute");
                    System.out.println(" -c <file>  = the name of the clipboard file to load");
                    System.out.println(" -u         = execute the update of the clipboards loaded");
                    System.out.println(" -o <file>  = the name of the file to output results to (default: use stdout)");
                    System.out.println(" -d <flags> = the debug messages to enable when running (default: 0F)");
                    System.out.println("");
                    System.out.println("     The debug flag values are hex bit values and defined as:");
                    System.out.println("     01 = STATUS_PARSER");
                    System.out.println("     02 = STATUS_SPREADSHEET");
                    System.out.println("     04 = STATUS_INFO");
                    System.out.println("     08 = STATUS_DEBUG");
                    System.out.println("     10 = STATUS_PROPS");
                    System.out.println("     e.g. -d 1F will enable all msgs");
                    System.out.println();
                    System.out.println("The following commands test special features:");
                    System.out.println();
                    System.out.println(" -date [p] <value>    = display the date converted to YYYY-MM-DD format (p = past date)");
                    System.out.println(" -fdate <date value>  = display the future date converted to YYYY-MM-DD format");
                    System.out.println(" -find  <order #>     = display the spreadsheet 1st row containing order#");
                    System.out.println(" -class   <col> <row> = display the spreadsheet cell class type");
                    System.out.println(" -cellget <col> <row> = display the spreadsheet cell data");
                    System.out.println(" -cellput <col> <row> <text> = write the spreadsheet cell data (erases if text omitted)");
                    System.out.println("          (displays the previous value that was overwritten)");
                    System.out.println(" -color   <col> <row> <color> = set cell background to color of the month (0 to clear)");
                    System.out.println(" -RGB     <col> <row> <RGB> = set cell background to specified RGB hexadecimal color");
                    System.out.println(" -HSB     <col> <row> <HSB> = set cell background to specified HSB hexadecimal color");
                    System.out.println();
                    System.out.println(" The -s option is required, since it specifies the spreadsheet to work with.");
                    System.out.println("");
                    System.out.println(" The -p and the -c options are optional and specify the input files to parse.");
                    System.out.println("   Multiple Clipboard files can be specified to run back to back.");
                    System.out.println("   If neither is specified, it will simply open the Spreadsheet file and close it.");
                    System.out.println("");
                    System.out.println(" The -o option is optional. If not given, it will be output to the file specified");
                    System.out.println("   by the 'TestFileOut' entry in the site.properties file.");
                    System.out.println("   If the properties file doesn't exist or 'TestFileOut' is not defined in it or");
                    System.out.println("   the -o option omitted a <file> entry, all reporting will be output to stdout.");
                    System.out.println("   If outputting to a file and the file currently exists, it will be overwritten.");
                    System.out.println("");
                    System.out.println(" The path used for the all files is the value of the 'TestPath' entry in the");
                    System.out.println("   site.properties file. If the properties file doesn't exist or 'TestPath'");
                    System.out.println("   is not defined in it, the current directory will be used as the path.");
                    System.out.println();
                    return;
                case "-d":
                    checkReqParams (1, option, ix, args.length);
                    Integer debugFlags = Integer.parseUnsignedInt(args[++ix], 16);
                    frame.setMessageFlags(debugFlags);
                    break;
                case "-s":
                    checkReqParams (1, option, ix, args.length);
                    filetype = "Spreadsheet";
                    fname = args[++ix];
                    ssheetFile = checkFilename (fname, ".ods", filetype, true);
                    System.out.println(filetype + " file: " + ssheetFile.getAbsolutePath());
                    // load the spreadsheet file
                    Spreadsheet.loadSpreadsheet(ssheetFile);
                    break;
                case "-t":
                    checkReqParams (1, option, ix, args.length);
                    strSheetSel = args[++ix];
                    // make the tab selection
                    Spreadsheet.selectSpreadsheetTab (strSheetSel);
                    break;
                case "-c":
                    checkReqParams (1, option, ix, args.length);
                    filetype = "Clipboard";
                    fname = args[++ix];
                    File fClip = checkFilename (fname, ".txt", filetype, false);
                    System.out.println(filetype + " file: " + fClip.getAbsolutePath());
                    // read from this file instead of clipboard
                    AmazonReader amazonReader = new AmazonReader(fClip);
                    amazonReader.parseWebData();
                    break;
                case "-u":
                    System.out.println("Updating spreadsheet from clipboards");
                    AmazonReader.updateSpreadsheet();
                    break;
                case "-p":
                    checkReqParams (1, option, ix, args.length);
                    filetype = "PDF";
                    fname = args[++ix];
                    pdfFile = checkFilename (fname, ".pdf", filetype, false);
                    System.out.println(filetype + " file: " + pdfFile.getAbsolutePath());
                    PdfReader pdfReader = new PdfReader(pdfFile);
                    pdfReader.readPdfContents();
                    break;
                case "-o":
                    if (ix >= args.length - 1 || args[ix+1].startsWith("-")) {
                        System.out.println("Output messages to stdout");
                        frame.setTestOutputFile(null);
                    } else {
                        fname = args[++ix];
                        fname = getTestPath() + "/" + fname;
                        System.out.println("Output messages to file: " + fname);
                        frame.setTestOutputFile(fname);
                    }
                    break;
                case "-date":
                    checkReqParams (1, option, ix, args.length);
                    // the date will consume the remainder of the command line
                    // convert arg array to a list, remove everything up to the command, then compress into a string
                    List<String> list = new ArrayList<>(Arrays.asList(args));
                    for (int index = 0; index < list.size() && ! list.get(0).contentEquals(option); index++) {
                        list.remove(0);
                    }
                    list.remove(0); // now remove the "-date" entry
                    boolean bPast = false;
                    if (args[++ix].contentEquals("p")) {
                        if (list.isEmpty())
                            throw new ParserException("ERROR: Missing parameter for " + option + " option");
                        bPast = true;
                        list.remove(0);
                    }
                    String strDate = String.join(" ", list);
                    LocalDate date = DateFormat.getFormattedDate (strDate, bPast);
                    String convDate = DateFormat.convertDateToString(date, true);
                    if (convDate == null) {
                        throw new ParserException("ERROR: invalid date conversion");
                    }
                    System.out.println("<" + convDate + ">");
                    // since we don't know how many words are in the date, this must be last in the command line
                    return;
                case "-find":
                    checkReqParams (1, option, ix, args.length);
                    String strCol,strRow, order;
                    Integer iRow, iCol;
                    order = args[++ix];
                    iRow = Spreadsheet.findItemNumber(order);
                    System.out.println("<" + iRow + ">");
                    break;
                case "-class":
                    checkReqParams (2, option, ix, args.length);
                    strCol = args[++ix];
                    strRow = args[++ix];
                    iCol = Utils.getIntFromString(strCol, 0, 0);
                    iRow = Utils.getIntFromString(strRow, 0, 0);
                    if (iCol == null || iRow == null) {
                        throw new ParserException("ERROR: invalid values: col = " + strCol + ", row = " + strRow);
                    }
                    String strValue = Spreadsheet.getSpreadsheetCellClass(iCol, iRow);
                    System.out.println("<" + strValue + ">");
                    break;
                case "-color":
                    checkReqParams (3, option, ix, args.length);
                    strCol = args[++ix];
                    strRow = args[++ix];
                    String strColor = args[++ix];
                    iCol = Utils.getIntFromString(strCol, 0, 0);
                    iRow = Utils.getIntFromString(strRow, 0, 0);
                    Integer iColor = Utils.getIntFromString(strColor, 0, 0);
                    if (iCol == null || iRow == null || iColor == null) {
                        throw new ParserException("ERROR: invalid values: col = " + strCol + ", row = " + strRow + ", color = " + strColor);
                    }
                    Spreadsheet.setSpreadsheetCellColor(strSheetSel, iCol, iRow, Utils.getColorOfTheMonth(iColor));
                    System.out.println("<OK>");
                    break;
                case "-RGB":
                    checkReqParams (3, option, ix, args.length);
                    strCol = args[++ix];
                    strRow = args[++ix];
                    String colorRGB = args[++ix];
                    iCol = Utils.getIntFromString(strCol, 0, 0);
                    iRow = Utils.getIntFromString(strRow, 0, 0);
                    Integer iRGB = Integer.parseUnsignedInt(colorRGB, 16);
                    if (iCol == null || iRow == null || iRGB == null) {
                        throw new ParserException("ERROR: invalid values: col = " + strCol + ", row = " + strRow + ", RGB = " + colorRGB);
                    }
                    Spreadsheet.setSpreadsheetCellColor(strSheetSel, iCol, iRow, Utils.getColor("RGB", iRGB));
                    System.out.println("<OK>");
                    break;
                case "-HSB":
                    checkReqParams (3, option, ix, args.length);
                    strCol = args[++ix];
                    strRow = args[++ix];
                    String colorHSB = args[++ix];
                    iCol = Utils.getIntFromString(strCol, 0, 0);
                    iRow = Utils.getIntFromString(strRow, 0, 0);
                    Integer iHSB = Integer.parseUnsignedInt(colorHSB, 16);
                    if (iCol == null || iRow == null || iHSB == null) {
                        throw new ParserException("ERROR: invalid values: col = " + strCol + ", row = " + strRow + ", HSB = " + colorHSB);
                    }
                    Spreadsheet.setSpreadsheetCellColor(strSheetSel, iCol, iRow, Utils.getColor("HSB", iHSB));
                    System.out.println("<OK>");
                    break;
                case "-cellget":
                    checkReqParams (2, option, ix, args.length);
                    strCol = args[++ix];
                    strRow = args[++ix];
                    iCol = Utils.getIntFromString(strCol, 0, 0);
                    iRow = Utils.getIntFromString(strRow, 0, 0);
                    if (iCol == null || iRow == null) {
                        throw new ParserException("ERROR: invalid values: col = " + strCol + ", row = " + strRow);
                    }
                    String cellValue = Spreadsheet.getSpreadsheetCell(strSheetSel, iCol, iRow);
                    System.out.println("<" + cellValue + ">");
                    break;
                case "-cellput":
                    checkReqParams (2, option, ix, args.length);
                    strCol = args[++ix];
                    strRow = args[++ix];
                    String strText = null;
                    // the cell text will consume the remainder of the command line
                    // convert arg array to a list, remove everything up to the command, then compress into a string
                    // if no parameter, we erase the cell by passing a null
                    if (ix < args.length - 1) {
                        list = new ArrayList<>(Arrays.asList(args));
                        for (int index = 0; index < list.size() && ! list.get(0).contentEquals(option); index++) {
                            list.remove(0);
                        }
                        // now remove the "-cellput" entry and the col and row entries
                        for (int index = 0; index < list.size() && index < 3; index++) {
                            list.remove(0);
                        }
                        strText = String.join(" ", list);
                    }
                    iCol = Utils.getIntFromString(strCol, 0, 0);
                    iRow = Utils.getIntFromString(strRow, 0, 0);
                    if (iCol == null || iRow == null) {
                        throw new ParserException("ERROR: invalid values: col = " + strCol + ", row = " + strRow);
                    }
                    cellValue = Spreadsheet.putSpreadsheetCell(strSheetSel, iCol, iRow, strText);
                    System.out.println("<" + cellValue + ">");
                    // since we don't know how many words are in the date, this must be last in the command line
                    return;
                default:
                    throw new ParserException("ERROR: Invalid option: " + args[ix]);
            }
        }
    }
    
    // Main driver method
    public static void main(String[] args)
    {
        // check for arguments passed (non-GUI interface for testing):
        if (args.length > 0) {
            frame = new UIFrame(false);
            props = new PropertiesFile();
            // set defaults from properties file
            frame.setDefaultStatus ();
            Spreadsheet.setDefaultSettings();
            
            try {
                runCommandLine (args);
            } catch (ParserException ex) {
                System.out.println(ex);
            }
            frame.closeTestFile();
                
//            boolean bExit = false;
//            Scanner scanner = new Scanner(System.in);
//            while (!bExit) {
//                System.out.println("Running in background");
//                String[] command = scanner.nextLine().split(" ");
//                bExit = runCommandLine (command);
//            }
//            System.out.println("Exiting background");
        } else {
            // create the user interface to control things
            frame = new UIFrame(true);
            props = new PropertiesFile();

            // enable the messages as they were from prevous run
            frame.setDefaultStatus ();
         }
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

