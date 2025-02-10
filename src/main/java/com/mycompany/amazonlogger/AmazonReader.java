package com.mycompany.amazonlogger;

// Importing java input/output classes
import static com.mycompany.amazonlogger.UIFrame.STATUS_ERROR;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

public class AmazonReader {

    // GLOBALS
    public  static UIFrame frame;
    public  static Keyword keyword;
    public  static PropertiesFile props;


    // Main driver method
    public static void main(String[] args)
    {
        // check for arguments passed (non-GUI interface for testing):
        if (args.length > 0) {
            // command line version for testing
            frame = new UIFrame(false);
            props = new PropertiesFile();
            // set defaults from properties file
            frame.setDefaultStatus ();
            
            try {
                Spreadsheet.setDefaultSettings();
                runCommandLine (args);
            } catch (ParserException | IOException | SAXException | TikaException ex) {
                frame.outputInfoMsg (STATUS_ERROR, ex.getMessage());
            }
            frame.closeTestFile();
        } else {
            // create the user interface to control things
            frame = new UIFrame(true);
            props = new PropertiesFile();

            // enable the messages as they were from prevous run
            frame.setDefaultStatus ();
         }
    }

    private static void runCommandLine (String[] args) throws ParserException, IOException, SAXException, TikaException {
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
                    System.out.println("<OK>");
                    break;
                case "-s":
                    checkReqParams (1, option, ix, args.length);
                    filetype = "Spreadsheet";
                    fname = args[++ix];
                    File ssheetFile = checkFilename (fname, ".ods", filetype, true);
                    System.out.println(filetype + " file: " + ssheetFile.getAbsolutePath());
                    // load the spreadsheet file
                    Spreadsheet.loadSpreadsheet(ssheetFile);
                    System.out.println("<OK>");
                    break;
                case "-t":
                    checkReqParams (1, option, ix, args.length);
                    String tab = args[++ix];
                    // make the tab selection
                    Spreadsheet.selectSpreadsheetTab (tab);
                    System.out.println("<OK>");
                    break;
                case "-c":
                    checkReqParams (1, option, ix, args.length);
                    filetype = "Clipboard";
                    fname = args[++ix];
                    File fClip = checkFilename (fname, ".txt", filetype, false);
                    System.out.println(filetype + " file: " + fClip.getAbsolutePath());
                    // read from this file instead of clipboard
                    AmazonParser amazonParser = new AmazonParser(fClip);
                    amazonParser.parseWebData();
                    break;
                case "-u":
                    System.out.println("Updating spreadsheet from clipboards");
                    AmazonParser.updateSpreadsheet();
                    System.out.println("<OK>");
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
                        System.out.println("<Output messages to stdout>");
                        frame.setTestOutputFile(null);
                    } else {
                        fname = args[++ix];
                        fname = getTestPath() + "/" + fname;
                        System.out.println("<Output messages to file: " + fname + ">");
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
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColorOfTheMonth(iColor));
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
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColor("RGB", iRGB));
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
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColor("HSB", iHSB));
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
                    String cellValue = Spreadsheet.getSpreadsheetCell(iCol, iRow);
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
                    cellValue = Spreadsheet.putSpreadsheetCell(iCol, iRow, strText);
                    System.out.println("<" + cellValue + ">");
                    // since we don't know how many words are in the date, this must be last in the command line
                    return;
                default:
                    throw new ParserException("ERROR: Invalid option: " + args[ix]);
            }
        }
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
    
    private static String getTestPath () {
        String pathname = Utils.getPathFromPropertiesFile (PropertiesFile.Property.TestPath);
        if (pathname == null || pathname.isBlank()) {
            pathname = System.getProperty("user.dir");
        }
        return pathname;
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

