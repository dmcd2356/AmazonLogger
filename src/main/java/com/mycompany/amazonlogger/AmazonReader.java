package com.mycompany.amazonlogger;

// Importing java input/output classes
import com.mycompany.amazonlogger.PropertiesFile.Property;
import static com.mycompany.amazonlogger.UIFrame.STATUS_ERROR;
import static com.mycompany.amazonlogger.UIFrame.STATUS_NORMAL;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
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
         
            // run the command line arguments
            try {
                runCommandLine (args);
            } catch (ParserException | IOException | SAXException | TikaException ex) {
                frame.outputInfoMsg (STATUS_ERROR, ex.getMessage());
            }
            
            // close the test output file
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

        ArrayList<String> cmdLine = new ArrayList<>(Arrays.asList(args));
        frame.outputInfoMsg(STATUS_NORMAL, "command: " + String.join(" ", cmdLine));
        while (! cmdLine.isEmpty()) {
            // extract the command option from the list
            option = cmdLine.get(0);
            cmdLine.remove(0);
            // the rest will be the parameters associated with the option (if any) plus any additional options
            ArrayList<String> params = new ArrayList();
            try {
                switch (option) {
                    case "-x":
                        System.out.println("EXITING");
                        break;
                    case "-h":
                        System.out.println(" -h         = to print this message");
                        System.out.println(" -s <file> <0|1> = the name of the spreadsheet file to modify");
                        System.out.println("              0 if don't check for header, 1 if normal header check");
                        System.out.println(" -t <tab>   = the name (or number) of the tab selection in the spreadsheet");
                        System.out.println(" -p <file>  = the name of the PDF file to execute");
                        System.out.println(" -c <file>  = the name of the clipboard file to load");
                        System.out.println(" -u         = execute the update of the clipboards loaded");
                        System.out.println(" -o <file>  = the name of the file to output results to (default: use stdout)");
                        System.out.println(" -d <flags> = the debug messages to enable when running (default: 0F)");
                        System.out.println("");
                        System.out.println("     The debug flag values are hex bit values and defined as:");
                        System.out.println("     01 = STATUS_NORMAL");
                        System.out.println("     02 = STATUS_PARSER");
                        System.out.println("     04 = STATUS_SPREADSHEET");
                        System.out.println("     08 = STATUS_INFO");
                        System.out.println("     10 = STATUS_DEBUG");
                        System.out.println("     20 = STATUS_PROPS");
                        System.out.println("     e.g. -d 3F will enable all msgs");
                        System.out.println();
                        System.out.println("The following commands test special features:");
                        System.out.println();
                        System.out.println(" -date  <date value>  = display the date converted to YYYY-MM-DD format (assume future)");
                        System.out.println(" -datep <date value>  = display the date converted to YYYY-MM-DD format (assume past)");
                        System.out.println(" -default <0|1>       = load the last spreadsheet and tab selection");
                        System.out.println("                        0 if don't check for header, 1 if normal header check");
                        System.out.println(" -maxcol              = display the number of columns in the spreadsheet");
                        System.out.println(" -maxrow              = display the number of rows    in the spreadsheet");
                        System.out.println(" -setsize <col> <row> = set the col and row size of the loaded spreadsheet");
                        System.out.println(" -find    <order #>   = display the spreadsheet 1st row containing order#");
                        System.out.println(" -class   <col> <row> = display the spreadsheet cell class type");
                        System.out.println(" -cellget <col> <row> = display the spreadsheet cell data");
                        System.out.println(" -cellclr <col> <row> = clear the spreadsheet cell data");
                        System.out.println(" -cellput <col> <row> <text> = write the spreadsheet cell data");
                        System.out.println("          (if more than 1 word, must wrap in quotes)");
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
                        params = checkReqParams (1, option, cmdLine);
                        Integer debugFlags = Integer.parseUnsignedInt(params.get(0), 16);
                        frame.setMessageFlags(debugFlags);
                        System.out.println("<OK>");
                        break;
                    case "-s":
                        params = checkReqParams (1, option, cmdLine);
                        filetype = "Spreadsheet";
                        fname = params.get(0);
                        File ssheetFile = checkFilename (fname, ".ods", filetype, true);
                        // load the spreadsheet file
                        Spreadsheet.selectSpreadsheet(ssheetFile);
                        System.out.println("<OK>");
                        break;
                    case "-l":
                        params = checkReqParams (2, option, cmdLine);
                        Integer numTabs = Integer.parseUnsignedInt(params.get(0), 10);
                        String strCheck = params.get(1);
                        // load the spreadsheet file
                        Spreadsheet.loadSheets(numTabs, strCheck.contentEquals("1"));
                        System.out.println("<OK>");
                        break;
                    case "-t":
                        params = checkReqParams (1, option, cmdLine);
                        String tab = params.get(0);
                        // make the tab selection
                        Spreadsheet.selectSpreadsheetTab (tab);
                        System.out.println("<OK>");
                        break;
                    case "-c":
                        params = checkReqParams (1, option, cmdLine);
                        filetype = "Clipboard";
                        fname = params.get(0);
                        File fClip = checkFilename (fname, ".txt", filetype, false);
                        System.out.println(filetype + " file: " + fClip.getAbsolutePath());
                        // read from this file instead of clipboard
                        AmazonParser amazonParser = new AmazonParser(fClip);
                        amazonParser.parseWebData();
                        break;
                    case "-u":
                        checkReqParams (0, option, cmdLine);
                        System.out.println("Updating spreadsheet from clipboards");
                        AmazonParser.updateSpreadsheet();
                        System.out.println("<OK>");
                        break;
                    case "-p":
                        params = checkReqParams (1, option, cmdLine);
                        filetype = "PDF";
                        fname = params.get(0);
                        pdfFile = checkFilename (fname, ".pdf", filetype, false);
                        System.out.println(filetype + " file: " + pdfFile.getAbsolutePath());
                        PdfReader pdfReader = new PdfReader();
                        pdfReader.readPdfContents(pdfFile);
                        break;
                    case "-o":
                        params = checkReqParams (0, option, cmdLine);
                        if (params.isEmpty()) {
                            System.out.println("<Output messages to stdout>");
                            frame.setTestOutputFile(null);
                        } else {
                            fname = params.get(0);
                            fname = getTestPath() + "/" + fname;
                            System.out.println("<Output messages to file: " + fname + ">");
                            frame.setTestOutputFile(fname);
                        }
                        break;
                    case "-date":
                        params = checkReqParams (1, option, cmdLine);
                        String strDate = String.join(" ", params);
                        LocalDate date = DateFormat.getFormattedDate (strDate, false);
                        String convDate = DateFormat.convertDateToString(date, true);
                        if (convDate == null) {
                            throw new ParserException("Invalid date conversion");
                        }
                        System.out.println("<" + convDate + ">");
                        break;
                    case "-datep":
                        params = checkReqParams (1, option, cmdLine);
                        strDate = String.join(" ", params);
                        date = DateFormat.getFormattedDate (strDate, true);
                        convDate = DateFormat.convertDateToString(date, true);
                        if (convDate == null) {
                            throw new ParserException("Invalid date conversion");
                        }
                        System.out.println("<" + convDate + ">");
                        break;
                    case "-default":
                        params = checkReqParams (2, option, cmdLine);
                        numTabs = Integer.parseUnsignedInt(params.get(0), 10);
                        strCheck = params.get(1);
                        String ssPath = Utils.getPathFromPropertiesFile(Property.SpreadsheetPath);
                        String ssFname = props.getPropertiesItem(Property.SpreadsheetFile, "");
                        if (ssPath != null && ssFname != null) {
                            File ssFile = new File(ssPath + "/" + ssFname);
                            Spreadsheet.selectSpreadsheet(ssFile);
                            Spreadsheet.loadSheets(numTabs, strCheck.contentEquals("1"));
                        }
                        tab = props.getPropertiesItem(Property.SpreadsheetTab, "0");
                        Spreadsheet.selectSpreadsheetTab (tab);
                        System.out.println("<OK>");
                        break;
                    case "-maxcol":
                        checkReqParams (0, option, cmdLine);
                        Integer iCol = Spreadsheet.getSpreadsheetColSize ();
                        System.out.println("<" + iCol + ">");
                        break;
                    case "-maxrow":
                        checkReqParams (0, option, cmdLine);
                        Integer iRow = Spreadsheet.getSpreadsheetRowSize ();
                        System.out.println("<" + iRow + ">");
                        break;
                    case "-setsize":
                        params = checkReqParams (2, option, cmdLine);
                        String strCol,strRow;
                        strCol = params.get(0);
                        strRow = params.get(1);
                        iCol = Utils.getIntFromString(strCol, 0, 0);
                        iRow = Utils.getIntFromString(strRow, 0, 0);
                        if (iCol == null || iRow == null) {
                            throw new ParserException("Invalid values: col = " + strCol + ", row = " + strRow);
                        }
                        Spreadsheet.setSpreadsheetSize (iCol, iRow);
                        System.out.println("<OK>");
                        break;
                    case "-find":
                        params = checkReqParams (1, option, cmdLine);
                        String order = params.get(0);
                        iRow = Spreadsheet.findItemNumber(order);
                        System.out.println("<" + iRow + ">");
                        break;
                    case "-class":
                        params = checkReqParams (2, option, cmdLine);
                        strCol = params.get(0);
                        strRow = params.get(1);
                        iCol = Utils.getIntFromString(strCol, 0, 0);
                        iRow = Utils.getIntFromString(strRow, 0, 0);
                        if (iCol == null || iRow == null) {
                            throw new ParserException("Invalid values: col = " + strCol + ", row = " + strRow);
                        }
                        String strValue = Spreadsheet.getSpreadsheetCellClass(iCol, iRow);
                        System.out.println("<" + strValue + ">");
                        break;
                    case "-color":
                        params = checkReqParams (3, option, cmdLine);
                        strCol = params.get(0);
                        strRow = params.get(1);
                        String strColor = params.get(2);
                        iCol = Utils.getIntFromString(strCol, 0, 0);
                        iRow = Utils.getIntFromString(strRow, 0, 0);
                        Integer iColor = Utils.getIntFromString(strColor, 0, 0);
                        if (iCol == null || iRow == null || iColor == null) {
                            throw new ParserException("Invalid values: col = " + strCol + ", row = " + strRow + ", color = " + strColor);
                        }
                        Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColorOfTheMonth(iColor));
                        System.out.println("<OK>");
                        break;
                    case "-RGB":
                        params = checkReqParams (3, option, cmdLine);
                        strCol = params.get(0);
                        strRow = params.get(1);
                        String colorRGB = params.get(2);
                        iCol = Utils.getIntFromString(strCol, 0, 0);
                        iRow = Utils.getIntFromString(strRow, 0, 0);
                        Integer iRGB = Integer.parseUnsignedInt(colorRGB, 16);
                        if (iCol == null || iRow == null || iRGB == null) {
                            throw new ParserException("Invalid values: col = " + strCol + ", row = " + strRow + ", RGB = " + colorRGB);
                        }
                        Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColor("RGB", iRGB));
                        System.out.println("<OK>");
                        break;
                    case "-HSB":
                        params = checkReqParams (3, option, cmdLine);
                        strCol = params.get(0);
                        strRow = params.get(1);
                        String colorHSB = params.get(2);
                        iCol = Utils.getIntFromString(strCol, 0, 0);
                        iRow = Utils.getIntFromString(strRow, 0, 0);
                        Integer iHSB = Integer.parseUnsignedInt(colorHSB, 16);
                        if (iCol == null || iRow == null || iHSB == null) {
                            throw new ParserException("Invalid values: col = " + strCol + ", row = " + strRow + ", HSB = " + colorHSB);
                        }
                        Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColor("HSB", iHSB));
                        System.out.println("<OK>");
                        break;
                    case "-cellget":
                        params = checkReqParams (2, option, cmdLine);
                        strCol = params.get(0);
                        strRow = params.get(1);
                        iCol = Utils.getIntFromString(strCol, 0, 0);
                        iRow = Utils.getIntFromString(strRow, 0, 0);
                        if (iCol == null || iRow == null) {
                            throw new ParserException("Invalid values: col = " + strCol + ", row = " + strRow);
                        }
                        String cellValue = Spreadsheet.getSpreadsheetCell(iCol, iRow);
                        System.out.println("<" + cellValue + ">");
                        break;
                    case "-cellclr":
                        params = checkReqParams (2, option, cmdLine);
                        strCol = params.get(0);
                        strRow = params.get(1);
                        iCol = Utils.getIntFromString(strCol, 0, 0);
                        iRow = Utils.getIntFromString(strRow, 0, 0);
                        if (iCol == null || iRow == null) {
                            throw new ParserException("Invalid values: col = " + strCol + ", row = " + strRow);
                        }
                        cellValue = Spreadsheet.putSpreadsheetCell(iCol, iRow, null);
                        System.out.println("<" + cellValue + ">");
                        break;
                    case "-cellput":
                        params = checkReqParams (3, option, cmdLine);
                        strCol = params.get(0);
                        strRow = params.get(1);
                        params.remove(0);
                        params.remove(0);
                        String strText = String.join(" ", params);
                        iCol = Utils.getIntFromString(strCol, 0, 0);
                        iRow = Utils.getIntFromString(strRow, 0, 0);
                        if (iCol == null || iRow == null) {
                            throw new ParserException("Invalid values: col = " + strCol + ", row = " + strRow);
                        }
                        cellValue = Spreadsheet.putSpreadsheetCell(iCol, iRow, strText);
                        System.out.println("<" + cellValue + ">");
                        break;
                    default:
                        throw new ParserException("Invalid option: " + option);
                }
            } catch (IndexOutOfBoundsException ex) {
                throw new ParserException("AmazonReader. : Index entry exceeded max of " + (params.size()-1) + " for option " + option);
            }
        }
    }

    /**
     * This reads the arguments following the option and places them in an ArrayList.
     * It assumes the arg list for the option ends when it finds the next arument
     * that begins with a '-', indicating it is another option. It, of course,
     * will also terminate at the end of the command line. If an argument is to
     * be passed that contains a word begining with a '-', it must be preceded
     * by the '\' character to indicate it is part of the options arg list.
     * 
     * @param min    - the min number of required parameters for the option
     * @param option - the option being examined
     * @param list   - the ArrayList of the parameters for the option, plus any additional options
     *                 (this will be modified to skip the current option & its parameters)
     * 
     * @return the parameter list for the current option
     * 
     * @throws ParserException 
     */
    private static ArrayList<String> checkReqParams (int min, String option, ArrayList<String> cmdList) throws ParserException {
        ArrayList<String> paramList = new ArrayList<>();

        if (cmdList == null) {
            throw new ParserException("Null command list for " + option + " option");
        }
        if (cmdList.size() < min) {
            throw new ParserException("Missing parameter(s) for " + option + " option");
        }
        if (cmdList.isEmpty()) { // min must have been 0
            frame.outputInfoMsg(STATUS_NORMAL, "- option: " + option + " - no args");
            return paramList; // empty list
        }
        
        // now find the next option (an entry that begins with a '-')
        while (! cmdList.isEmpty() && ! cmdList.get(0).startsWith("-")) {
            // if we have a parameter that has an escaped '-' as its
            // 1st character, eliminate the escape character.
            int offset = 0;
            String entry = cmdList.get(0);
            if (entry.startsWith("\\-")) {
                offset = 1;
            }

            // '-' not found yet, copy to the params list and remove from command list
            paramList.addLast(entry.substring(offset));
            cmdList.remove(0);
        }
        if (paramList.size() < min) {
            throw new ParserException("Missing parameter(s) for " + option + " option: " + min + " required, found " + paramList.size());
        }

        frame.outputInfoMsg(STATUS_NORMAL, "- option: " + option + " " + String.join(" ", paramList));
        frame.outputInfoMsg(STATUS_NORMAL, "- params: " + paramList.size());
        return paramList;
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

