package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.AmazonReader.props;
import static com.mycompany.amazonlogger.UIFrame.STATUS_NORMAL;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PARSER;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 *
 * @author dan
 */
public class CommandParser {
    
    CommandParser() {
    }
    
    private static final String CLASS_NAME = "CommandParser";
    
    private class OptionList {
        String  optName;        // the option name
        String  argTypes;       // argument types: S = String, L = list,
                                //   D = dir, F = file, U = unsigned int, I = Int, B = 0/1
                                //   (lowercase if optional, but must be at end of list)
        
        OptionList (String name, String args) {
            optName = name;
            argTypes = args;
        }
    }
    
    private final OptionList [] OptionTable = {
        new OptionList ("-h"        , ""),
        new OptionList ("-d"        , "U"),
        new OptionList ("-s"        , "F"),
        new OptionList ("-l"        , "UB"),
        new OptionList ("-t"        , "U"),
        new OptionList ("-c"        , "F"),
        new OptionList ("-u"        , ""),
        new OptionList ("-p"        , "F"),
        new OptionList ("-o"        , "f"),

        new OptionList ("-date"     , "L"),
        new OptionList ("-datep"    , "L"),
        new OptionList ("-default"  , "UB"),
        new OptionList ("-maxcol"   , ""),
        new OptionList ("-maxrow"   , ""),
        new OptionList ("-setsize"  , "UU"),
        new OptionList ("-find"     , "S"),
        new OptionList ("-class"    , "UU"),
        new OptionList ("-color"    , "UUU"),
        new OptionList ("-RGB"      , "UUU"),
        new OptionList ("-HSB"      , "UUU"),
        new OptionList ("-cellget"  , "UU"),
        new OptionList ("-cellclr"  , "UU"),
        new OptionList ("-cellput"  , "UUL"),
    };
    
    private OptionList getCommandInfo (String cmd) throws ParserException {
        for (OptionList entry : OptionTable) {
            if (entry.optName.contentEquals(cmd)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * defines the structure for commands
     */
    public class CommandStruct {
        public String            commandName;
        public ArrayList<String> params;
        
        CommandStruct(String cmd) {
            commandName = cmd;
            params = new ArrayList<>();
        }
    } 

    public void runCommandLine (String[] args) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + ".runCommandLine: ";
        
        frame.outputInfoMsg(STATUS_NORMAL, "command: " + String.join(" ", args));

        switch (args[0]) {
            case "-h":
                // display the help message
                helpMessage();
                return;
            case "-f":
                // we will run commands from a file instead of the command line.
                // get the file name and verify it exists
                if (args.length < 2) {
                    throw new ParserException(functionId + "missing argument for option: " + args[0]);
                }
                String filetype = "Script";
                String fname = args[1];
                File scriptFile = checkFilename (fname, ".scr", filetype, false);
                FileReader fReader = new FileReader(scriptFile);
                BufferedReader fileReader = new BufferedReader(fReader);
                String scriptLine;
                while ((scriptLine = fileReader.readLine()) != null) {
                    scriptLine = scriptLine.stripIndent();
                    if (scriptLine.isBlank() || scriptLine.charAt(0) == '#') {
                        continue;
                    }
                    ArrayList<CommandStruct> cmdList =  extractCmdOptions (scriptLine.split(" "));
                    frame.outputInfoMsg(STATUS_PARSER, cmdList.size() + " options found");
                    while (! cmdList.isEmpty()) {
                        // extract the command option from the list
                        CommandStruct cmdLine = cmdList.removeFirst();
                        String rsp = executeCommand (cmdLine);
                    }
                }
                fileReader.close();
                break;
            default:
                // read the command line arguments and execute them
                ArrayList<CommandStruct> cmdList =  extractCmdOptions (args);
                ArrayList<String> response = new ArrayList<>();
                frame.outputInfoMsg(STATUS_PARSER, cmdList.size() + " options found");

                while (! cmdList.isEmpty()) {
                    // extract the command option from the list
                    CommandStruct cmdLine = cmdList.removeFirst();
                    String rsp = executeCommand (cmdLine);
                    if (rsp == null)
                        return;
                    if (! rsp.isEmpty())
                        response.add(rsp);
                }
                if (response.isEmpty()) {
                    System.out.println("<OK>");
                } else {
                    System.out.println("<" + String.join(",", response) + ">");
                }
                break;
        }
    }
    
    private ArrayList<CommandStruct> extractCmdOptions (String[] args) throws ParserException {
        String functionId = CLASS_NAME + ".extractCmdOptions: ";
        
        String nextArg = args[0];
        ArrayList<CommandStruct> cmdList = new ArrayList<>();
        OptionList optInfo = getCommandInfo(nextArg);
        if (optInfo == null) {
            throw new ParserException(functionId + "option is not valid: " + nextArg);
        }
        CommandStruct cmdEntry = new CommandStruct(nextArg);
        frame.outputInfoMsg(STATUS_PARSER, "  new option: " + nextArg + ", arglist = '" + optInfo.argTypes + "'");
        
        for (int ix = 1; ix < args.length; ix++) {
            nextArg = args[ix];
            OptionList newInfo = getCommandInfo(nextArg);
            if (newInfo != null) {
                // next option found - check if prev option has correct # of args
                frame.outputInfoMsg(STATUS_PARSER, "  new option: " + nextArg + ", arglist = '" + newInfo.argTypes + "'");
                int minArgs = 0;
                int maxArgs = (optInfo.argTypes == null) ? 0 : optInfo.argTypes.length();
                for (int off = 0; off < maxArgs; off++) {
                    // uppercase letters are required, which will indicate the min mumber
                    if (optInfo.argTypes.charAt(off) >= 'A' && optInfo.argTypes.charAt(off) <= 'Z') {
                        minArgs++;
                    }
                }
                if (minArgs > cmdEntry.params.size()) {
                    throw new ParserException(functionId + "Missing args for option "
                                            + cmdEntry.commandName + ": " + cmdEntry.params.size()
                                            + " (min = " + minArgs + ": " +optInfo.argTypes + ")");
                }
                cmdList.add(cmdEntry);
                cmdEntry = new CommandStruct(nextArg);
                optInfo = newInfo;
            } else {
                // not a valid command, assume it is a parameter
                // verify the option takes a parameter
                if (optInfo.argTypes == null || optInfo.argTypes.isEmpty()) {
                    throw new ParserException(functionId + "Invalid entry: option " + cmdEntry.commandName
                                        + " has no params and " + nextArg + " is not a valid option");
                }
                // get type of parameter it should be and verify it is the correct type
                int parmIx = cmdEntry.params.size();        // this is the index of the current parameter for this option
                int maxParams = optInfo.argTypes.length();  // this is the max number of params for this option
                char lastType = optInfo.argTypes.charAt(maxParams-1);  // this is the type of the last param for the option
                if (parmIx < maxParams) {
                    char parmType = optInfo.argTypes.charAt(parmIx);
                    switch (parmType) {
                        case 'U':
                        case 'u':
                            getUnsignedIntValue(nextArg);
                            break;
                        case 'I':
                        case 'i':
                            getSignedIntValue(nextArg);
                            break;
                        case 'B':
                        case 'b':
                            if (! nextArg.contentEquals("0") && ! nextArg.contentEquals("1")) {
                                throw new ParserException(functionId + "Invalid boolean argument for option "
                                                + cmdEntry.commandName + ": " + nextArg);
                            }
                            break;
                        default:
                            break;
                    }
                    frame.outputInfoMsg(STATUS_PARSER, "     new '" + parmType + "' param: " + nextArg);
                } else if (lastType == 'L' || lastType == 'l') {
                    // if we have exceeded the number of items defined for the option,
                    // but the last arg type indicates a list, we can have any number
                    // of additional String parameters, so don't check any further.
                    frame.outputInfoMsg(STATUS_PARSER, "     new '" + lastType + "' param: " + nextArg);
                } else {
                    // Otherwise, we have exceeded the max allowed arguments for this option.
                    throw new ParserException(functionId + "Args list for option "
                                            + cmdEntry.commandName + " exceeded max allowed: " + maxParams);
                }
                cmdEntry.params.add(nextArg);
            }
        }
            
        // add the last entry
        cmdList.add(cmdEntry);
        return cmdList;
    }
    
    public String executeCommand (CommandStruct cmdLine) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + ".executeCommand: ";
        String response = "";
        String filetype;
        String fname;
        String option = cmdLine.commandName;
        ArrayList<String> params = cmdLine.params;
        frame.outputInfoMsg(STATUS_PARSER, "  execute option: " + option + " " + String.join(" ", params));

        // the rest will be the parameters associated with the option (if any) plus any additional options
        try {
            switch (option) {
                case "-d":
                    Integer debugFlags = getUnsignedIntValue(params.get(0));
                    frame.setMessageFlags(debugFlags);
                    break;
                case "-s":
                    filetype = "Spreadsheet";
                    fname = params.get(0);
                    File ssheetFile = checkFilename (fname, ".ods", filetype, true);
                    Spreadsheet.selectSpreadsheet(ssheetFile);
                    break;
                case "-l":
                    Integer numTabs = getUnsignedIntValue(params.get(0));
                    String strCheck = params.get(1);
                    if (numTabs <= 0) {
                        throw new ParserException(functionId + "Invalid number of tabs to load: " + numTabs);
                    }
                    Spreadsheet.loadSheets(numTabs, strCheck.contentEquals("1"));
                    break;
                case "-t":
                    String tab = params.get(0);
                    Spreadsheet.selectSpreadsheetTab (tab);
                    break;
                case "-c":
                    filetype = "Clipboard";
                    fname = params.get(0);
                    File fClip = checkFilename (fname, ".txt", filetype, false);
                    frame.outputInfoMsg(STATUS_PARSER, filetype + " file: " + fClip.getAbsolutePath());
                    AmazonParser amazonParser = new AmazonParser(fClip);
                    amazonParser.parseWebData();
                    break;
                case "-u":
                    frame.outputInfoMsg(STATUS_PARSER, "Updating spreadsheet from clipboards");
                    AmazonParser.updateSpreadsheet();
                    break;
                case "-p":
                    filetype = "PDF";
                    fname = params.get(0);
                    File pdfFile = checkFilename (fname, ".pdf", filetype, false);
                    frame.outputInfoMsg(STATUS_PARSER, "filetype + \" file: \" + pdfFile.getAbsolutePath()");
                    PdfReader pdfReader = new PdfReader();
                    pdfReader.readPdfContents(pdfFile);
                    break;
                case "-o":
                    if (params.isEmpty()) {
                        frame.outputInfoMsg(STATUS_PARSER, "Output messages to stdout");
                        frame.setTestOutputFile(null);
                    } else {
                        fname = params.get(0);
                        fname = getTestPath() + "/" + fname;
                    frame.outputInfoMsg(STATUS_PARSER, "Output messages to file: " + fname);
                        frame.setTestOutputFile(fname);
                    }
                    break;
                case "-date":
                    String strDate = String.join(" ", params);
                    LocalDate date = DateFormat.getFormattedDate (strDate, false);
                    String convDate = DateFormat.convertDateToString(date, true);
                    if (convDate == null) {
                        throw new ParserException(functionId + "Invalid date conversion");
                    }
                    response = convDate;
                    break;
                case "-datep":
                    strDate = String.join(" ", params);
                    date = DateFormat.getFormattedDate (strDate, true);
                    convDate = DateFormat.convertDateToString(date, true);
                    if (convDate == null) {
                        throw new ParserException(functionId + "Invalid date conversion");
                    }
                    response = convDate;
                    break;
                case "-default":
                    numTabs = getUnsignedIntValue(params.get(0));
                    strCheck = params.get(1);
                    if (numTabs <= 0) {
                        throw new ParserException(functionId + "Invalid number of tabs to load: " + numTabs);
                    }
                    String ssPath = Utils.getPathFromPropertiesFile(PropertiesFile.Property.SpreadsheetPath);
                    String ssFname = props.getPropertiesItem(PropertiesFile.Property.SpreadsheetFile, "");
                    if (ssPath != null && ssFname != null) {
                        File ssFile = new File(ssPath + "/" + ssFname);
                        Spreadsheet.selectSpreadsheet(ssFile);
                        Spreadsheet.loadSheets(numTabs, strCheck.contentEquals("1"));
                    }
                    tab = props.getPropertiesItem(PropertiesFile.Property.SpreadsheetTab, "0");
                    Spreadsheet.selectSpreadsheetTab (tab);
                    break;
                case "-maxcol":
                    Integer iCol = Spreadsheet.getSpreadsheetColSize ();
                    response = "" + iCol;
                    break;
                case "-maxrow":
                    Integer iRow = Spreadsheet.getSpreadsheetRowSize ();
                    response = "" + iRow;
                    break;
                case "-setsize":
                    String strCol,strRow;
                    strCol = params.get(0);
                    strRow = params.get(1);
                    iCol = Utils.getIntFromString(strCol, 0, 0);
                    iRow = Utils.getIntFromString(strRow, 0, 0);
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + strCol + ", row = " + strRow);
                    }
                    Spreadsheet.setSpreadsheetSize (iCol, iRow);
                    break;
                case "-find":
                    String order = params.get(0);
                    iRow = Spreadsheet.findItemNumber(order);
                    response = "" + iRow;
                    break;
                case "-class":
                    strCol = params.get(0);
                    strRow = params.get(1);
                    iCol = Utils.getIntFromString(strCol, 0, 0);
                    iRow = Utils.getIntFromString(strRow, 0, 0);
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + strCol + ", row = " + strRow);
                    }
                    String strValue = Spreadsheet.getSpreadsheetCellClass(iCol, iRow);
                    response = strValue;
                    break;
                case "-color":
                    strCol = params.get(0);
                    strRow = params.get(1);
                    String strColor = params.get(2);
                    iCol = Utils.getIntFromString(strCol, 0, 0);
                    iRow = Utils.getIntFromString(strRow, 0, 0);
                    Integer iColor = getUnsignedIntValue(strColor);
                    if (iCol == null || iRow == null || iColor == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + strCol + ", row = " + strRow + ", color = " + strColor);
                    }
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColorOfTheMonth(iColor));
                    break;
                case "-RGB":
                    strCol = params.get(0);
                    strRow = params.get(1);
                    String colorRGB = params.get(2);
                    iCol = Utils.getIntFromString(strCol, 0, 0);
                    iRow = Utils.getIntFromString(strRow, 0, 0);
                    Integer iRGB = getUnsignedIntValue(colorRGB);
                    if (iCol == null || iRow == null || iRGB == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + strCol + ", row = " + strRow + ", RGB = " + colorRGB);
                    }
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColor("RGB", iRGB));
                    break;
                case "-HSB":
                    strCol = params.get(0);
                    strRow = params.get(1);
                    String colorHSB = params.get(2);
                    iCol = Utils.getIntFromString(strCol, 0, 0);
                    iRow = Utils.getIntFromString(strRow, 0, 0);
                    Integer iHSB = getUnsignedIntValue(colorHSB);
                    if (iCol == null || iRow == null || iHSB == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + strCol + ", row = " + strRow + ", HSB = " + colorHSB);
                    }
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColor("HSB", iHSB));
                    break;
                case "-cellget":
                    strCol = params.get(0);
                    strRow = params.get(1);
                    iCol = Utils.getIntFromString(strCol, 0, 0);
                    iRow = Utils.getIntFromString(strRow, 0, 0);
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + strCol + ", row = " + strRow);
                    }
                    String cellValue = Spreadsheet.getSpreadsheetCell(iCol, iRow);
                    response = cellValue;
                    break;
                case "-cellclr":
                    strCol = params.get(0);
                    strRow = params.get(1);
                    iCol = Utils.getIntFromString(strCol, 0, 0);
                    iRow = Utils.getIntFromString(strRow, 0, 0);
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + strCol + ", row = " + strRow);
                    }
                    cellValue = Spreadsheet.putSpreadsheetCell(iCol, iRow, null);
                    response = cellValue;
                    break;
                case "-cellput":
                    strCol = params.get(0);
                    strRow = params.get(1);
                    params.remove(0);
                    params.remove(0);
                    String strText = String.join(" ", params);
                    iCol = Utils.getIntFromString(strCol, 0, 0);
                    iRow = Utils.getIntFromString(strRow, 0, 0);
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + strCol + ", row = " + strRow);
                    }
                    cellValue = Spreadsheet.putSpreadsheetCell(iCol, iRow, strText);
                    response = cellValue;
                    break;
                default:
                    throw new ParserException(functionId + "Invalid option: " + option);
            }
        } catch (IndexOutOfBoundsException ex) {
            throw new ParserException(functionId + "Index entry exceeded max of "
                                    + (params.size()-1) + " for option " + option + "\n" + ex);
        }
        
        return response;
    }

    private static Integer getUnsignedIntValue (String strValue) throws ParserException {
        String functionId = CLASS_NAME + ".getUnsignedIntValue: ";
        int radix = 10;
        Integer retVal;
        if (strValue.charAt(0) == 'x' || strValue.charAt(0) == 'X') {
            radix = 16;
            strValue = strValue.substring(1);
        }
        try {
            retVal = Integer.parseUnsignedInt(strValue, radix);
        } catch (NumberFormatException ex) {
            throw new ParserException(functionId + "Number format exception: " + strValue);
        }
        return retVal;
    }
    
    private static Integer getSignedIntValue (String strValue) throws ParserException {
        String functionId = CLASS_NAME + ".getUnsignedIntValue: ";
        Integer retVal;
        try {
            retVal = Integer.valueOf(strValue);
        } catch (NumberFormatException ex) {
            throw new ParserException(functionId + "Number format exception: " + strValue);
        }
        return retVal;
    }
    
    private static File checkFilename (String fname, String type, String filetype, boolean bWritable) throws ParserException {
        String functionId = CLASS_NAME + ".checkFilename: ";
        if (filetype == null) {
            filetype = "";
        }
        if (type != null && !type.isBlank() && !fname.endsWith(type)) {
            throw new ParserException(functionId + "Invalid " + filetype + " filename: " + fname);
        }
        if (fname == null || fname.isBlank()) {
            throw new ParserException(functionId + "Invalid " + filetype + " filename is blank");
        }
        
        fname = getTestPath() + "/" + fname;
        File myFile = new File(fname);
        if (!myFile.canRead()) {
            throw new ParserException(functionId + "Invalid " + filetype + " file - no read access: " + fname);
        }
        if (bWritable && !myFile.canWrite()) {
            throw new ParserException(functionId + "Invalid " + filetype + " file - no write access: " + fname);
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

    private static void helpMessage() {
        System.out.println(" -h         = to print this message");
        System.out.println(" -f <file>  = to execute commands from a script file (*.scr)");
        System.out.println(" -s <file>  = the name of the spreadsheet file to modify (*.ods)");
        System.out.println(" -l <tabs> <0|1> = the number of tabs to load from the spreadsheet");
        System.out.println("              0 if don't check for header, 1 if normal header check");
        System.out.println(" -t <tab>   = the name (or number) of the tab selection in the spreadsheet");
        System.out.println(" -p <file>  = the name of the PDF file to execute (*.pdf)");
        System.out.println(" -c <file>  = the name of the clipboard file to load (*.txt)");
        System.out.println(" -u         = execute the update of the clipboards loaded");
        System.out.println(" -o <file>  = the name of the file to output results to (default: use stdout)");
        System.out.println(" -d <flags> = the debug messages to enable when running");
        System.out.println("");
        System.out.println("     The debug flag values are hex bit values and defined as:");
        System.out.println("     x01 =  1 = STATUS_NORMAL");
        System.out.println("     x02 =  2 = STATUS_PARSER");
        System.out.println("     x04 =  4 = STATUS_SPREADSHEET");
        System.out.println("     x08 =  8 = STATUS_INFO");
        System.out.println("     x10 = 16 = STATUS_DEBUG");
        System.out.println("     x20 = 32 = STATUS_PROPS");
        System.out.println("     e.g. -d x3F will enable all msgs");
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
    }
    
}
