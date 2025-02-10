package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.AmazonReader.props;
import com.mycompany.amazonlogger.PropertiesFile.Property;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
    
    // This directory is at the same level as the execution directory.
    // This is the base dir of the credit card files.
    // NOTE: There should be a series of YYYY year directories under this that
    //  represent each year.
    public static final String CC_BASE_DIR = "Credit_card_statements";
    
    // This is the directory under CC_BASE_DIR/YYYY where the PDF files reside
    //  that contain the Amazon credits & charges. There is a separate file for each month.
    public static final String CC_RECORD_DIR = "Chase_VISA_3996";
    
    private static File pdfFile = null;
    
    // this class is the information that is extracted from the charge card PDF file for
    // balancing the amounts charged to the account with the Amazon purchases.
    private class CardTransaction {
        String  trans_date;     // the date of the transaction
        String  order_num;      // the Amazon order number
        int     amount;         // the amount of the transaction in cents (credits are -, debits are +)
        String  vendor;         // the vendor name
        boolean completed;      // true when the item has been found in the spreadsheet
    }

    public PdfReader () {
        
    }
    
    public PdfReader (File pFile) {
        pdfFile = pFile;
    }
    
    /**
    * parses the credit card credits and debits from the PDF file.
    *  This extracts vital info from the credit card file for Amazon charges
    *    and refunds and saves it in an array.
    *  It then looks for the entries in the spreadsheet file for the corresponding
    *   order numbers and modifies the spreadsheet file to highlight the rows that
    *   match up with the charges/credits.
    * 
    * @throws ParserException
    * @throws IOException
    * @throws SAXException
    * @throws TikaException
    */
    public void readPdfContents () throws IOException, ParserException, SAXException, TikaException {

        if (pdfFile == null) {
            // file was not passed, so we must let the user select one.
            // see if we have a properties file that has a previously saved PDF directory
            // if so, let's start the file selection process from there
            String pdfPath = Utils.getPathFromPropertiesFile(Property.PdfPath);
            if (pdfPath == null) {
                // else, get the base directory of the credit card files (it's at same dir level as current dir)
                pdfPath = System.getProperty("user.dir");
                int offset = pdfPath.lastIndexOf('/');
                if (offset > 0) {
                    String pdfPathName = pdfPath.substring(0, offset) + "/" + CC_BASE_DIR;
                    File tempDir = new File(pdfPathName);
                    if (tempDir.exists() && tempDir.isDirectory()) {
                        pdfPath = pdfPathName;
                    }
                }

                // now find the latest year directory under this path advance to it
                String strYear = getLatestYearDir(pdfPath);
                if (strYear.isEmpty()) {
                    frame.outputInfoMsg(UIFrame.STATUS_WARN, "PdfReader.readPdfContents: No year directory found in: " + pdfPath);
                } else {
                    frame.outputInfoMsg(UIFrame.STATUS_INFO, "Latest directory year: " + strYear);
                    pdfPath += '/' + strYear;
                    // now check to make sure the specific credit card directory is located here
                    // and has files under it
                    String tempPath = pdfPath + '/' + CC_RECORD_DIR;
                    File tempDir = new File(tempPath);
                    if (tempDir.exists() && tempDir.isDirectory() && tempDir.listFiles() != null) {
                        pdfPath = tempPath;
                    }
                }
            }

            // select the PDF file to read from
            JFileChooser jfc = new JFileChooser();
            jfc.setCurrentDirectory(new File(pdfPath));
            jfc.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));
            jfc.showDialog(null,"Select the File");
            jfc.setVisible(true);
            pdfFile = jfc.getSelectedFile();
            if (pdfFile == null) {
                frame.outputInfoMsg(UIFrame.STATUS_WARN, "PdfReader.readPdfContents: No file chosen");
                return;
            }

            // update the Pdf path selection
            pdfPath = Utils.getFilePath(pdfFile);
            if (!pdfPath.isEmpty()) {
                props.setPropertiesItem(Property.PdfPath, pdfPath);
                frame.outputInfoMsg(UIFrame.STATUS_INFO, "PDF Path name: " + pdfPath);
            }
        }

        // get the name of the selected file, minus the file extension
        String strPdfName = Utils.getFileRootname(pdfFile);
        frame.outputInfoMsg(UIFrame.STATUS_INFO, "PDF File name: " + strPdfName);
            
        // check if the file has already been balanced in the spreadsheet
        String strTabSelect = "";
        frame.outputInfoMsg(UIFrame.STATUS_INFO, "Checking if file has been already balanced");
        if (! Spreadsheet.findCreditCardEntry("Dan", strPdfName)) {
            strTabSelect = "Dan";
        }
        if (! Spreadsheet.findCreditCardEntry("Connie", strPdfName)) {
            if (strTabSelect.isBlank()) {
                strTabSelect = "Connie";
            } else {
                strTabSelect = "Both";
            }
        }
        if (strTabSelect.isEmpty()) {
            return;
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

        // init the array list of Amazon transactions
        ArrayList<CardTransaction> transactionList = new ArrayList<>();
        Boolean bValid = false;

      try (
            // Read the contents of the PDF file line at a time
            Scanner scanner = new Scanner(contenthandler.toString())) {
            while (scanner.hasNextLine()) {
                   
                String line = scanner.nextLine();
                    
                // this will only be true if the previous line read was a valid Amazon entry.
                // The line following that entry will contain the Amazon order number.
                if (bValid) {
                    if (line.length() < 36 || ! line.contains("Order Number")) {
                        // we must be at a page crossing where we have some invalid lines
                        // prior to the order number, so just skip to the next line.
                        continue;
                    }
                    frame.outputInfoMsg(UIFrame.STATUS_DEBUG, "  order number: " + line);
                    // we have a valid order number, let's post it to the list of transactions
                        
                    String ordernum = line.substring(19);
                    if (transactionList.isEmpty()) {
                        throw new ParserException("PdfReader.readPdfContents: Order # " + ordernum + " received prior to any transactions");
                    }
                    CardTransaction newEntry = transactionList.removeLast();
                    if (newEntry.order_num == null || newEntry.order_num.isEmpty()) {
                        newEntry.order_num = ordernum;
                    } else {
                        throw new ParserException("PdfReader.readPdfContents: Order # " + ordernum + " received with no preceding data");
                    }
                    transactionList.add(newEntry);
                    bValid = false;
                }
                    
                // let's weed out the unimportant lines
                Integer amountIx = line.length();
                if (amountIx > 16) {
                    // this is checking for the date section and the gap preceding the vendor name
                    bValid = true;
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
                    }
                    if (bValid) {
                        CardTransaction newEntry = new CardTransaction();
                        frame.outputInfoMsg(UIFrame.STATUS_DEBUG, "  transaction: " + line);
                            
                        // we have a valid debit/credit line - save useful contents
                        newEntry.completed = false;
                        newEntry.vendor = line.substring(10, 20);
                        newEntry.trans_date = line.substring(0, 5);
                        newEntry.amount = Utils.getAmountValue(line.substring(amountIx));
                            
                        // now let's check for Amazon receipts only
                        if (newEntry.vendor.contentEquals("AMAZON MKT") ||
                                newEntry.vendor.contentEquals("AMZN Mktp ") ||
                                newEntry.vendor.contentEquals("Amazon.com") ) {
                            transactionList.add(newEntry);
                        } else {
                            bValid = false;
                        }
                    }
                }
            }
        }

        // combine the entries that have same order_num and positive amounts
        for (int ix = 0; ix < transactionList.size() - 1; ix++) {
            CardTransaction newEntry = transactionList.get(ix);
            for (int icmp = ix + 1; icmp < transactionList.size(); icmp++) {
                CardTransaction cmpEntry = transactionList.get(icmp);
                if (newEntry.order_num.equals(cmpEntry.order_num) &&
                    newEntry.amount > 0 && cmpEntry.amount > 0) {
                    newEntry.amount += cmpEntry.amount;
                    cmpEntry.order_num = "";
                }
            }
        }
        for (int ix = transactionList.size() - 1; ix >= 0; ix--) {
            CardTransaction newEntry = transactionList.get(ix);
            if (newEntry.order_num.isEmpty()) {
                transactionList.remove(ix);
            }
        }

        // find the valid entries for each user
        ArrayList<CardTransaction> danList = null;
        ArrayList<CardTransaction> connieList = null;
        String strTab = "Dan";
        if (strTabSelect.contentEquals(strTab) || strTabSelect.contentEquals("Both")) {
            danList = checkForNewEntries (strTab, transactionList);
            if (danList.isEmpty()) {
                danList = null;
                frame.outputInfoMsg(UIFrame.STATUS_INFO, "No entries usable in " + strTab + "'s list...");
            }
        }
        strTab = "Connie";
        if (strTabSelect.contentEquals(strTab) || strTabSelect.contentEquals("Both")) {
            connieList = checkForNewEntries (strTab, transactionList);
            if (connieList.isEmpty()) {
                connieList = null;
                frame.outputInfoMsg(UIFrame.STATUS_INFO, "No entries usable in " + strTab + "'s list...");
            }
        }
            
        if (danList != null || connieList != null) {
            // make a backup copy of the current file before saving.
            Spreadsheet.makeBackupCopy("-pdf-bak");
            
            // process the applicable sheets
            if (danList != null) {
                balanceSpreadsheetEntries("Dan", danList, strPdfName);
            }
            if (connieList != null) {
                balanceSpreadsheetEntries("Connie", connieList, strPdfName);
            }
        }
    }

    /*********************************************************************
    ** returns the most recent 'year' directory.
    * 
    *  @param path - the path to look for directories
    * 
    *  @return the directory name that has the form "20XX" that is the highest
    *          numeric value from 2020 to 2030
    */
    public static String getLatestYearDir (String path) {
        File f = new File(path);
        ArrayList<String> aDirList = new ArrayList<>(Arrays.asList(f.list()));
        String strYear = "";
        int maxyear = 0;
        for (int ix = 0; ix < aDirList.size(); ix++) {
            int year = Utils.getIntegerValue (aDirList.get(ix), 4);
            if (year > maxyear && year >= 2020 && year <= 2030) {
                // check if this is a valid directory with files in it
                String tempPath = path + "/" + strYear;
                File tempDir = new File(tempPath);
                if (tempDir.exists() && tempDir.isDirectory() && tempDir.listFiles() != null) {
                    maxyear = year;
                    strYear = String.valueOf(year);
                }
            }
        }
        
        return strYear;
    }

    private ArrayList<CardTransaction> checkForNewEntries (String sheetName,
                                                           ArrayList<CardTransaction> transactionList) throws ParserException {
        // select the user tab
        frame.outputInfoMsg(UIFrame.STATUS_INFO, "Checking for entries in " + sheetName + "'s list...");
        Spreadsheet.selectSpreadsheetTab (sheetName);
        ArrayList<CardTransaction> newList = new ArrayList<>();
        
        // now check each entry read from the pdf to see if they are applicable to the tab
        for (int ix = 0; ix < transactionList.size(); ix++) {
            CardTransaction cardEntry = transactionList.get(ix);

            // for each entry from the statement that has not been found...
            if (cardEntry.completed) {
                frame.outputInfoMsg(UIFrame.STATUS_INFO, 
                                      '\t' + cardEntry.order_num + "\t"
                                           + Utils.cvtAmountToString(cardEntry.amount) + "\t"
                                           + cardEntry.trans_date + "\t"
                                           + "- ALREADY COMPLETED");
                continue; // entry already completed - skip it
            }
                
            // ...search each entry in the spreadsheet for a matching order number
            int foundRow = Spreadsheet.findItemNumber (cardEntry.order_num);
            if (foundRow <= 0) {
                frame.outputInfoMsg(UIFrame.STATUS_INFO, 
                                      '\t' + cardEntry.order_num + "\t"
                                           + Utils.cvtAmountToString(cardEntry.amount) + "\t"
                                           + cardEntry.trans_date + "\t"
                                           + "- NOT FOUND");
                continue; // order number not found in spreadsheet, skip this entry
            }
            
            // else add the entry to the list
            newList.add(cardEntry);
        }
        return newList;
    }
            
    /*********************************************************************
    ** parses the credit card credits and debits from the PDF file.
    *  This extracts vital info from the credit card file for Amazon charges
    *    and refunds and saves it in an array.
    *  It then looks for the entries in the spreadsheet file for the corresponding
    *    order numbers and modifies the spreadsheet file to highlight the rows that
    *    match up with the charges/credits.
    * 
    * @param sheetName       - the name of the sheet in the spreadsheet (Dan or Connie)
    * @param transactionList - the list of credit card transactions pulled from the PDF file
    * @param strPdfName      - the name of the PDF file
    * 
    * @throws ParserException
    * @throws IOException
    * @throws NumberFormatException
    */
    private void balanceSpreadsheetEntries(String sheetName,
                                              ArrayList<CardTransaction> transactionList,
                                              String strPdfName) throws ParserException, IOException, NumberFormatException {

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

        frame.outputInfoMsg(UIFrame.STATUS_INFO, "Checking for entries in " + sheetName + "'s list...");

        // select the specified spreadsheet tab
        Spreadsheet.selectSpreadsheetTab (sheetName);

        // find the last row in the selected sheet. the next line is where we will add entries
        int lastRow = Spreadsheet.getLastRowIndex();
        frame.outputInfoMsg(UIFrame.STATUS_INFO, "spreadsheet " + sheetName + " last row: " + lastRow);

        // search the spreadsheet for each order found in the credit card statement
        for (int ix = 0; ix < transactionList.size(); ix++) {
            CardTransaction cardEntry = transactionList.get(ix);

            // ...search each entry in the spreadsheet for a matching order number
            int foundRow = Spreadsheet.findItemNumber (cardEntry.order_num);
            if (foundRow <= 0) {
                continue; // order number not found in spreadsheet, skip this entry
            }
                    
            // get the info for the row in the spreadsheet
            Integer iTotalCost = Spreadsheet.getTotalCost (foundRow);
            Integer iPayment   = Spreadsheet.getPaymentAmount (foundRow);
            Integer iRefund    = Spreadsheet.getRefundAmount (foundRow);
            frame.outputInfoMsg(UIFrame.STATUS_INFO, "found match to: " + cardEntry.order_num);

            // if Payment (or Refund) already has a value in the spreadsheet entry, the
            // charge on the card must have been split for multiple items 
            // (should only happen on multi-item entries, but have seen it on single items once)
            // So add the new charge amount to the current entry value to get the total paid (or refunded).
            boolean bPayment = (cardEntry.amount >= 0);
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
            if (iAmtAdj != null) {
                frame.outputInfoMsg(UIFrame.STATUS_INFO, "adjustment amount from current cell: "
                                            + Utils.cvtAmountToString(iAmtAdj));
            } else {
                iAmtAdj = 0;
            }

            // add the payment amount from the credit card sheet to the current amount
            //  either paid or received. and update the Payment/Refund column accordingly.
            // 'iTotalAmt' is then the current total of payments/refund for the order.
            int iTotalAmt = cardEntry.amount + iAmtAdj;
            if (bPayment) {
                Spreadsheet.setSpreadsheetPayment (foundRow, iTotalAmt);
            } else {
                Spreadsheet.setSpreadsheetRefund (foundRow, iTotalAmt);
            }

            // check if the payment is complete. it's possible that there were multiple
            //  items in an order and some merchants have not collected yet, so we may
            //  get the payments in pieces. 'complete' means the total payments add up
            //  to the total order amount.
            boolean bRemaining = false;
            if (bPayment) {
                // save the lowest row selection in which we had a payment match
                firstPaymentRow = foundRow < firstPaymentRow ? foundRow : firstPaymentRow;

                // 'iTotalCost' is the total cost (in cents) of the order from the spreadsheet,
                //   but is only found on the 1st entry of the order.
                if (iTotalCost != null) {
                    // check if the total amount has been accounted for
                    // 'iTotalCost' is the total cost of the order from the spreadsheet,
                    //   but is only found on the 1st entry of the order.
                    int iRemaining = iTotalCost - iTotalAmt;
                    if (iRemaining != 0) {
                        bRemaining = true;
                        Spreadsheet.setSpreadsheetPending (foundRow, iRemaining);
                        frame.outputInfoMsg(UIFrame.STATUS_INFO,
                                "Total cost: "   + Utils.cvtAmountToString(iTotalCost) +
                                "Payment: "      + Utils.cvtAmountToString(cardEntry.amount) +
                                "Prev balance: " + Utils.cvtAmountToString(iAmtAdj) +
                                "Remainder: "    + Utils.cvtAmountToString(iRemaining));
                    }
                }
            }

            for (int count = 0; Spreadsheet.getOrderNumber(foundRow + count).contentEquals(cardEntry.order_num); count++) {
                // mark row with color of the month to mark as complete
                Spreadsheet.highlightOrderInfo(foundRow + count, bPayment, bRemaining, colorOfMonth);
                if (count > 0)
                    frame.outputInfoMsg(UIFrame.STATUS_INFO, "       (index " + count + ")");
            }

            frame.outputInfoMsg(UIFrame.STATUS_NORMAL, 
                          '\t' + strTransType + '\t' + cardEntry.order_num + '\t'
                               + Utils.cvtAmountToString(cardEntry.amount) + "\t"
                               + cardEntry.trans_date );

            // mark card transaction entry as complete
            cardEntry.completed = true;
        }
            
        // this will be the 1st payment entry in the spreadsheet that was found
        if (firstPaymentRow != 999999) {
            Spreadsheet.setSpreadsheetCreditCard(firstPaymentRow, strPdfName);
        }

        // save the data to the spreadsheet file
        Spreadsheet.saveSpreadsheetFile();
    }

}
