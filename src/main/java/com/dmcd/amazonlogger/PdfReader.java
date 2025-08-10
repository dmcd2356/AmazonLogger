/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import com.dmcd.amazonlogger.GUILogPanel.MsgType;
import com.dmcd.amazonlogger.PropertiesFile.Property;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author dan
 */
public class PdfReader {
    
    private static final String CLASS_NAME = PdfReader.class.getSimpleName();

    private static File pdfFile = null;
    private static ArrayList<String> contents = new ArrayList<>();  // the contents of the pdf file read
    private static HashMap<String, ArrayList<CardTransaction>> mapList = new HashMap<>();


    public PdfReader () {
        pdfFile = null;
        contents = new ArrayList<>();
    }

    public static ArrayList<String> getContents () {
        return contents;
    }

    public static boolean isBalanceListEmpty() {
        return mapList.isEmpty();
    }
    
    public static int getBalanceListSize(String tabSelect) {
        return mapList.get(tabSelect).size();
    }
    
    public static CardTransaction getBalanceListEntry(String tabSelect, int ix) {
        return mapList.get(tabSelect).get(ix);
    }
    
    /**
     * loads the selected PDF file and reads its contents into a String array.
     * 
     * @param pFile - the name of the pdf file to load (null to run user interface to request file)
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public static void readPdfContents (File pFile) throws IOException, ParserException, SAXException, TikaException {

        pdfFile = pFile;
        
        if (pdfFile == null) {
            // file was not passed, so we must let the user select one.
            // see if we have a properties file that has a previously saved PDF directory
            // if so, let's start the file selection process from there
            String pdfPath = Utils.getPathFromPropertiesFile(Property.PdfPath);
            if (pdfPath == null) {
                // else, use the dir path application is being run from
                pdfPath = System.getProperty("user.dir");
            }

            // select the PDF file to read from
            JFileChooser jfc = new JFileChooser();
            jfc.setCurrentDirectory(new File(pdfPath));
            jfc.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));
            jfc.showDialog(null,"Select the File");
            jfc.setVisible(true);
            pdfFile = jfc.getSelectedFile();
            if (pdfFile == null) {
                GUILogPanel.outputInfoMsg(MsgType.WARN, "PdfReader.readPdfContents: No file chosen");
                return;
            }

            // update the Pdf path selection
            pdfPath = Utils.getFilePath(pdfFile);
            if (!pdfPath.isEmpty()) {
                PropertiesFile.setPropertiesItem(Property.PdfPath, pdfPath);
                GUILogPanel.outputInfoMsg(MsgType.INFO, "PDF Path name: " + pdfPath);
            }
        }

        // Create a file in local directory
        File f = new File(pdfFile.getAbsolutePath());

        // Create a file input stream on specified path with the created file
        FileInputStream fstream = new FileInputStream(f);

        // Create an object of type Metadata to use
        Metadata data = new Metadata();

        // Create a context parser for the pdf document
        ParseContext context = new ParseContext();

        // PDF document can be parsed using the PDFparser class
        PDFParser pdfparser = new PDFParser();

        // Create a content handler
        BodyContentHandler contenthandler = new BodyContentHandler();

        // Method parse invoked on PDFParser class
        pdfparser.parse(fstream, contenthandler, data, context);

        // now load the data into an array for processing
        try (
            // Read the contents of the PDF file line at a time
            Scanner scanner = new Scanner(contenthandler.toString())) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (! line.isBlank()) {
                    contents.add(line);
                }
            }
        }
    }

    /**
     * parses the credit card credits and debits from the PDF file.
     *  This extracts vital info from the credit card file for Amazon charges
     *    and refunds and saves it in an array.
     *  It then looks for the entries in the spreadsheet file for the corresponding
     *   order numbers and modifies the spreadsheet file to highlight the rows that
     *   match up with the charges/credits.
     * 
     * It assumes the pdf file data has been placed in the array 'contents'.
     * 
     * @return true if entries were added to the list of items to balance, false if not
     * 
     * @throws ParserException
     * @throws IOException
     */
    public boolean processData () throws ParserException, IOException {
        // get the name of the selected file, minus the file extension
        String strPdfName = Utils.getFileRootname(pdfFile);
        GUILogPanel.outputInfoMsg(MsgType.INFO, "PDF File name: " + strPdfName);
            
        // check if the file has already been balanced in the spreadsheet
        GUILogPanel.outputInfoMsg(MsgType.INFO, "Checking if file has been already balanced");
        ArrayList<String> tabSelect = Spreadsheet.findCreditCardEntry (strPdfName);
        if (tabSelect.size() == Spreadsheet.getTabCount()) {
            GUILogPanel.outputInfoMsg(MsgType.WARN, "PDF File already balanced in tabs: " + tabSelect);
            return false;
        }
            
        // init the array list of Amazon transactions
        ArrayList<CardTransaction> transactionList = new ArrayList<>();
        Boolean bValid = false;

        // Read the contents of the PDF file line at a time
        for (int lineix = 0; lineix < contents.size(); lineix++) {
            // process next line
            String line = contents.get(lineix);
                    
            // this will only be true if the previous line read was a valid Amazon entry.
            // The line following that entry will contain the Amazon order number.
            if (bValid) {
                if (line.length() < 36 || ! line.contains("Order Number")) {
                    // we must be at a page crossing where we have some invalid lines
                    // prior to the order number, so just skip to the next line.
                    continue;
                }
                GUILogPanel.outputInfoMsg(MsgType.DEBUG, "  order number: " + line);
                // we have a valid order number, let's post it to the list of transactions
                    
                String ordernum = line.substring(19);
                if (transactionList.isEmpty()) {
                    throw new ParserException("PdfReader.readPdfContents: Order # " + ordernum + " received prior to any transactions");
                }
                CardTransaction newEntry = transactionList.removeLast();
                if (newEntry.getOrderNumber() == null || newEntry.getOrderNumber().isEmpty()) {
                    newEntry.setOrderNumber(ordernum);
                } else {
                    throw new ParserException("PdfReader.readPdfContents: Order # " + ordernum + " received with no preceding data");
                }
                transactionList.add(newEntry);
                bValid = false;
            }
                    
            // let's weed out the unimportant lines and put valid ones in transaction list
            if (! bValid) {
                Integer amountVal = isValidEntry (line);
                if (amountVal != null) {
                    String strDate   = line.substring(0, 5);
                    String strVendor = line.substring(10, 20);
                    CardTransaction newEntry = new CardTransaction (strDate, strVendor, amountVal);
                    
                    if (newEntry.isAmazonEntry()) {
                        transactionList.add(newEntry);
                        GUILogPanel.outputInfoMsg(MsgType.DEBUG, "  transaction: " + line);
                        bValid = true;
                    } else {
                        bValid = false;
                    }
                }
            }
        }

        // combine the entries that have same order_num and positive amounts
        for (int ix = 0; ix < transactionList.size() - 1; ix++) {
            CardTransaction newEntry = transactionList.get(ix);
            for (int icmp = ix + 1; icmp < transactionList.size(); icmp++) {
                CardTransaction cmpEntry = transactionList.get(icmp);
                if (newEntry.getOrderNumber().equals(cmpEntry.getOrderNumber()) &&
                    newEntry.getAmount() > 0 && cmpEntry.getAmount() > 0) {
                    newEntry.modifyAmount(cmpEntry.getAmount());
                    cmpEntry.setOrderNumber("");
                }
            }
        }
        // now eliminate the extraneous entries that were combined
        for (int ix = transactionList.size() - 1; ix >= 0; ix--) {
            CardTransaction newEntry = transactionList.get(ix);
            if (newEntry.getOrderNumber().isEmpty()) {
                transactionList.remove(ix);
            }
        }

        // find the valid entries for each user
        ArrayList<CardTransaction> cardList;
        for (int ix = 0; ix < Spreadsheet.getTabCount(); ix++) {
            String tabName = Spreadsheet.getTabName(ix);
            if (tabSelect.indexOf(tabName) >= 0) {
                // skip entry if this tab has already been processed
                GUILogPanel.outputInfoMsg(MsgType.INFO, "Skipping " + tabName + "'s list, since it was already balanced");
                continue;
            }
            cardList = checkForNewEntries (tabName, transactionList);
            if (cardList.isEmpty()) {
                GUILogPanel.outputInfoMsg(MsgType.INFO, "No entries usable in " + tabName + "'s list...");
            } else {
                mapList.put(tabName, cardList);

                // add in the corresponding order info from the spreadsheet
                addSpreadsheetInfoToTransaction (tabName, cardList);
            }
        }

        // re-order the entries based on the row numbers
        for (HashMap.Entry<String, ArrayList<CardTransaction>> entry : mapList.entrySet()) {
            cardList = entry.getValue();
            for (int cardix_1 = 0; cardix_1 < cardList.size() - 1; cardix_1++) {
                for (int cardix_2 = cardix_1 + 1; cardix_2 < cardList.size(); cardix_2++) {
                    CardTransaction entry_1 = cardList.get(cardix_1);
                    CardTransaction entry_2 = cardList.get(cardix_2);
                    if (entry_2.getRow() < entry_1.getRow()) {
                        cardList.set(cardix_1, entry_2);
                        cardList.set(cardix_2, entry_1);
                    }
                }
            }
        }
        
        // re-order the entries that have the same order number by putting payment entries first
        for (HashMap.Entry<String, ArrayList<CardTransaction>> entry : mapList.entrySet()) {
            cardList = entry.getValue();
            for (int cardix_1 = 0; cardix_1 < cardList.size() - 1; cardix_1++) {
                for (int cardix_2 = cardix_1 + 1; cardix_2 < cardList.size(); cardix_2++) {
                    CardTransaction entry_1 = cardList.get(cardix_1);
                    CardTransaction entry_2 = cardList.get(cardix_2);
                    // if same order number, but 2nd amount is payment and 1st amount is refund, swap
                    if (entry_2.getOrderNumber().contentEquals(entry_1.getOrderNumber()) &&
                        entry_2.getAmount() >= 0 && entry_1.getAmount() < 0) {
                        cardList.set(cardix_1, entry_2);
                        cardList.set(cardix_2, entry_1);
                    }
                }
            }
        }
        
        // now output info to the GUI display
        displaySpreadsheetChanges();
        
        return ! mapList.isEmpty();
    }

    /**
     * makes the changes to the spreadsheet file to add in the balancing info from the pdf file.
     * 
     * @throws ParserException
     * @throws java.io.IOException
     */
    public static void balanceSpreadsheet() throws IOException, ParserException {
        // something changed - so first make a backup copy of the current file before saving.
        if (! mapList.isEmpty()) {
            Spreadsheet.makeBackupCopy("-pdf-bak");
        } else {
            GUILogPanel.outputInfoMsg(MsgType.WARN, "No changes were made.");
            return;
        }

        String strPdfName = Utils.getFileRootname(pdfFile);

        for (HashMap.Entry<String, ArrayList<CardTransaction>> entry : mapList.entrySet()) {
            String tabName = entry.getKey();
            ArrayList<CardTransaction> cardList = entry.getValue();

            // perform the balances
            balanceSpreadsheetEntries(tabName, cardList, strPdfName);

            // copy updated spreadsheet info into card transactions for GUI display
            addSpreadsheetInfoToTransaction (tabName, cardList);
        }

        // now update info to the GUI display
        displaySpreadsheetChanges();

        // update display of last balance performed
        String lastBalance = Spreadsheet.showLastLineInfo();
        GUIMain.showLastBalance(lastBalance);
    }
    
    /**
     * checks if the current line from the PDF file is a valid transaction listing.
     * 
     * @param line - the line read from the PDF file
     * 
     * @return the amount (in cents) of the transaction found (null if not a transaction)
     * 
     * @throws ParserException 
     */
    private static Integer isValidEntry (String line) throws ParserException {
        Integer amountVal = null;

        // line must have min number of chars in it
        Integer amountIx = line.length();
        if (amountIx <= 16) {
            return amountVal;
        }
        
        // this is checking for the date section and the gap preceding the vendor name
        boolean bValid = true;
        for (int ix = 0; ix < 9; ix++) {
            char c = line.charAt(ix);
            if ((ix < 2 && (c < '0' || c > '9')) ||
                (ix == 2 && c != '/') ||
                (ix > 2 && ix < 5 && (c < '0' || c > '9')) ||
                (ix >= 5 && c != ' ') ) {
                bValid = false;
                break;
            }
        }

        // this is checking for the cost at the end of the string
        if (bValid) {
            while (line.charAt(amountIx - 1) != ' ') {
                char c = line.charAt(amountIx - 1);
                if (c != '.' && c != '-' && (c < '0' || c > '9')) {
                    bValid = false;
                    break;
                }
                amountIx--;
            }
            String amountStr = line.substring(amountIx);
            amountVal = Utils.getAmountValue(amountStr);
        }
        
        return amountVal;
    }
    
    /**
     * checks for uncompleted card transactions in the list.
     * 
     * @param sheetName - name of the spreadsheet tab
     * @param transactionList - list of transactions found in the pdf file
     * 
     * @return list of transactions that have not been marked as completed
     * 
     * @throws ParserException 
     */
    private static ArrayList<CardTransaction> checkForNewEntries (String sheetName,
                                                           ArrayList<CardTransaction> transactionList) throws ParserException {
        // select the user tab
        GUILogPanel.outputInfoMsg(MsgType.INFO, "Checking for entries in " + sheetName + "'s list...");
        Spreadsheet.selectSpreadsheetTab (sheetName);
        ArrayList<CardTransaction> newList = new ArrayList<>();
        
        // now check each entry read from the pdf to see if they are applicable to the tab
        for (int ix = 0; ix < transactionList.size(); ix++) {
            CardTransaction cardEntry = transactionList.get(ix);

            // for each entry from the statement that has not been found...
            if (cardEntry.isCompleted()) {
                GUILogPanel.outputInfoMsg(MsgType.INFO, 
                                      '\t' + cardEntry.getOrderNumber() + "\t"
                                           + Utils.cvtAmountToString(cardEntry.getAmount()) + "\t"
                                           + cardEntry.getTransDate() + "\t"
                                           + "- ALREADY COMPLETED");
                continue; // entry already completed - skip it
            }
                
            // ...search each entry in the spreadsheet for a matching order number
            int foundRow = Spreadsheet.findItemNumber (cardEntry.getOrderNumber());
            if (foundRow <= 0) {
                GUILogPanel.outputInfoMsg(MsgType.INFO, 
                                      '\t' + cardEntry.getOrderNumber() + "\t"
                                           + Utils.cvtAmountToString(cardEntry.getAmount()) + "\t"
                                           + cardEntry.getTransDate() + "\t"
                                           + "- NOT FOUND");
                continue; // order number not found in spreadsheet, skip this entry
            }
            
            // else add the entry to the list
            newList.add(cardEntry);
        }
        return newList;
    }
            
    /**
     * adds the spreadsheet info to the card transactions for each order being balanced.
     * 
     * @param tabName  - name of tab being processed
     * @param cardList - list of card transactions from the PDF file (this will be updated)
     * 
     * @throws ParserException 
     */
    private static void addSpreadsheetInfoToTransaction (String tabName, ArrayList<CardTransaction> cardList) throws ParserException {
        for (int cardix = 0; cardix < cardList.size(); cardix++) {
            CardTransaction trans = cardList.get(cardix);
            int row = Spreadsheet.findColumnEntry (tabName, Spreadsheet.Column.OrderNumber, 2, trans.getOrderNumber());
            if (row >= 0) {
                ArrayList<Spreadsheet.Column> cols = new ArrayList<>();
                cols.add(Spreadsheet.Column.ItemIndex);
                cols.add(Spreadsheet.Column.Total);
                cols.add(Spreadsheet.Column.Payment);
                cols.add(Spreadsheet.Column.Pending);
                cols.add(Spreadsheet.Column.Refund);
                ArrayList<String> rowData = Spreadsheet.getRowValues(tabName, row, cols);
                trans.setSpreadsheetEntries(tabName, row, rowData, cols);
            }
        }
    }
    
    /**
     * if in GUI mode, updates the display to show the PDF credit card entries
     *   that will change the spreadsheet.
     */
    private static void displaySpreadsheetChanges() {
        if (GUIMain.isGUIMode()) {
            ArrayList<CardTransaction> cardList;
            boolean bFirstTime = true;
            for (HashMap.Entry<String, ArrayList<CardTransaction>> entry : mapList.entrySet()) {
                String tabName = entry.getKey();
                cardList = entry.getValue();
                GUIOrderPanel.printBalanceHeader(bFirstTime);
                bFirstTime = false;
                String lastOrderNum = "";
                for (int cardix = 0; cardix < cardList.size(); cardix++) {
                    CardTransaction trans = cardList.get(cardix);
                    GUIOrderPanel.printBalance (tabName, trans, lastOrderNum);
                    lastOrderNum = trans.getOrderNumber();
                }
                GUIOrderPanel.printNewLine();
            }
        }
    }

    /**
     * parses the credit card credits and debits from the PDF file.
     *  This extracts vital info from the credit card file for Amazon charges
     *    and refunds and saves it in an array.
     *  It then looks for the entries in the spreadsheet file for the corresponding
     *    order numbers and modifies the spreadsheet file to highlight the rows that
     *    match up with the charges/credits.
     * 
     * @param sheetName       - the name of the tab selection for the sheet
     * @param transactionList - the list of credit card transactions pulled from the PDF file
     * @param strPdfName      - the name of the PDF file
     * 
     * @throws ParserException
     * @throws IOException
     */
    private static void balanceSpreadsheetEntries(String sheetName,
                                              ArrayList<CardTransaction> transactionList,
                                              String strPdfName) throws ParserException, IOException {

        // get the highlight color (changes monthly)
        int month = 0;
        int offset = strPdfName.lastIndexOf('-');
        if (offset >= 0 && strPdfName.length() >= offset + 3) {
            month = Utils.getIntegerValue(strPdfName.substring(offset+1), 2);
        }
        Color colorOfMonth = Utils.getColorOfTheMonth(month);

        // indicate we haven't yet found the first payment entry
        // (this is used to mark the CREDIT_CARD column with the PDF file used for the entries)
        int firstPaymentRow = 999999;

        GUILogPanel.outputInfoMsg(MsgType.INFO, "Checking for entries in " + sheetName + "'s list...");

        // select the specified spreadsheet tab
        Spreadsheet.selectSpreadsheetTab (sheetName);

        // find the last row in the selected sheet. the next line is where we will add entries
        int lastRow = Spreadsheet.getLastRowIndex();
        int startingRow = transactionList.get(0).getRow();
        GUILogPanel.outputInfoMsg(MsgType.INFO, "spreadsheet " + sheetName + " last row: " + lastRow + ", first item: row " + startingRow);

        // search the spreadsheet for each order found in the credit card statement
        // (these should already be arranged in order of rows, from smallest to largest)
        for (int ix = 0; ix < transactionList.size(); ix++) {
            CardTransaction cardEntry = transactionList.get(ix);

            // get the starting row for the order number and the number of items (rows) in the order
            int firstRow = cardEntry.getRow();
            
            // get the info for the row in the spreadsheet
            Integer itemCount  = cardEntry.getItemCount();
            Integer iTotalCost = cardEntry.getIntTotal();
            Integer iPayment   = cardEntry.getIntPayment();
            Integer iRefund    = cardEntry.getIntRefund();
            GUILogPanel.outputInfoMsg(MsgType.INFO, "found match to: " + cardEntry.getOrderNumber());

            // if Payment (or Refund) already has a value in the spreadsheet entry, the
            // charge on the card must have been split for multiple items 
            // (should only happen on multi-item entries, but have seen it on single items once)
            // So add the new charge amount to the current entry value to get the total paid (or refunded).
            boolean bPayment = (cardEntry.getAmount() >= 0);
            String strTransType;
            Integer iAmtAdj;
            if (bPayment) {
                iAmtAdj = iPayment;
                strTransType = "Payment";
            } else {
                iAmtAdj = iRefund;
                strTransType = "Refund";
            }

            // 'iAmtAdj' is the payment or refund amount that is currently specified
            //  in the corresponding spreadsheet column, in cents
            GUILogPanel.outputInfoMsg(MsgType.INFO, "adjustment amount from current cell: "
                                            + Utils.cvtAmountToString(iAmtAdj));

            // add the payment amount from the credit card sheet to the current amount
            //  either paid or received. and update the Payment/Refund column accordingly.
            // 'iTotalAmt' is then the current total of payments/refund for the order.
            int iTotalAmt = cardEntry.getAmount() + iAmtAdj;
            if (bPayment) {
                Spreadsheet.setSpreadsheetPayment (firstRow, iTotalAmt);
            } else {
                Spreadsheet.setSpreadsheetRefund (firstRow, iTotalAmt);
            }

            // check if the payment is complete. it's possible that there were multiple
            //  items in an order and some merchants have not collected yet, so we may
            //  get the payments in pieces. 'complete' means the total payments add up
            //  to the total order amount.
            boolean bRemaining = false;
            if (bPayment) {
                // save the lowest row selection in which we had a payment match
                firstPaymentRow = firstRow < firstPaymentRow ? firstRow : firstPaymentRow;

                // 'iTotalCost' is the total cost (in cents) of the order from the spreadsheet,
                //   but is only found on the 1st entry of the order.
                if (cardEntry.getTotal() != null && ! cardEntry.getTotal().isEmpty()) {
                    // check if the total amount has been accounted for
                    // 'iTotalCost' is the total cost of the order from the spreadsheet,
                    //   but is only found on the 1st entry of the order.
                    int iRemaining = iTotalCost - iTotalAmt;
                    if (iRemaining != 0) {
                        bRemaining = true;
                        Spreadsheet.setSpreadsheetPending (firstRow, iRemaining);
                        GUILogPanel.outputInfoMsg(MsgType.INFO,
                                "Total cost: "   + Utils.cvtAmountToString(iTotalCost) +
                                "Payment: "      + Utils.cvtAmountToString(cardEntry.getAmount()) +
                                "Prev balance: " + Utils.cvtAmountToString(iAmtAdj) +
                                "Remainder: "    + Utils.cvtAmountToString(iRemaining));
                    }
                }
            }

            // mark row with color of the month to mark as complete
            for (int count = 0; count < itemCount; count++) {
                Spreadsheet.highlightOrderInfo(firstRow + count, bPayment, bRemaining, colorOfMonth);
                if (count > 0)
                    GUILogPanel.outputInfoMsg(MsgType.INFO, "       (index " + count + ")");
            }

            GUILogPanel.outputInfoMsg(MsgType.NORMAL, 
                          '\t' + strTransType + '\t' + cardEntry.getOrderNumber() + '\t'
                               + Utils.cvtAmountToString(cardEntry.getAmount()) + "\t"
                               + cardEntry.getTransDate() );

            // mark card transaction entry as complete
            cardEntry.setCompleted();
        }
        
        // check for any entries in spreadsheet that are in the range processed that had no charge
        //  associated with it (because of using points or whatever) and mark these as complete
        //  as well, since they will not end up in the ledger.
        boolean bNoCharge = false;
        for (int ix = startingRow; ix < lastRow; ix++) {
            Integer total = Spreadsheet.getTotalCost(ix);
            if ((total == null && bNoCharge) || (total != null && total == 0)) {
                Spreadsheet.highlightOrderInfo(ix, false, false, colorOfMonth);
                bNoCharge = true;
            } else {
                bNoCharge = false;
            }
        }
        
        // this will be the 1st payment entry in the spreadsheet that was found
        if (firstPaymentRow != 999999) {
            Spreadsheet.setSpreadsheetCreditCard(firstPaymentRow, strPdfName);
        }

        // save the data to the spreadsheet file
        File ssFile = Spreadsheet.getFileSelection();
        Spreadsheet.saveSheet(ssFile, sheetName);
    }

}
