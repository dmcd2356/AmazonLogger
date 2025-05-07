/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.AmazonReader.props;
import static com.mycompany.amazonlogger.UIFrame.STATUS_COMPILE;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 *
 * @author dan
 */
public class CmdOptions {
    
    private static final String CLASS_NAME = CmdOptions.class.getSimpleName();
    
    // List of all the command line options and the argument types each takes
    // S = String, L = String array, U = Unsigned Int, I = Int, A = Int array, B = Boolean
    //   (lowercase if optional, but must be at end of list)
    private final OptionList [] OptionTable = {
        new OptionList ("-help"     , ""),
        new OptionList ("-debug"    , "U"),
        new OptionList ("-sfile"    , "S"),
        new OptionList ("-load"     , "UB"),
        new OptionList ("-tab"      , "U"),
        new OptionList ("-cfile"    , "S"),
        new OptionList ("-clip"     , "b"),
        new OptionList ("-pfile"    , "S"),
        new OptionList ("-prun"     , ""),
        new OptionList ("-update"   , ""),
        new OptionList ("-tpath"    , "s"),
        new OptionList ("-ofile"    , "s"),
        new OptionList ("-save"     , ""),

        new OptionList ("-date"     , "S"),
        new OptionList ("-datep"    , "S"),
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
        new OptionList ("-cellput"  , "UUS"),
        new OptionList ("-rowget"   , "UUU"),
        new OptionList ("-colget"   , "UUU"),
        new OptionList ("-rowput"   , "UUL"),
        new OptionList ("-colput"   , "UUL"),
        new OptionList ("-rowcolor" , "UUA"),
        new OptionList ("-colcolor" , "UUA"),
    };
    
    private class OptionList {
        String  optName;        // the option name
        String  argTypes;       // argument types list
        
        OptionList (String opt, String args) {
            optName  = opt;
            argTypes = args;
        }
    }

    /**
     * converts the OptionTable values to parameter type values.
     * 
     * @param dataType - the char representation of the data type
     * 
     * @return the corresponding ParameterType value
     */
    public static ParameterStruct.ParamType getParameterType (char dataType) {
        switch(dataType) {
            case 'I':   return ParameterStruct.ParamType.Integer;
            case 'U':   return ParameterStruct.ParamType.Unsigned;
            case 'B':   return ParameterStruct.ParamType.Boolean;
            case 'A':   return ParameterStruct.ParamType.IntArray;
            case 'L':   return ParameterStruct.ParamType.StrArray;
            default:
            case 'S':   return ParameterStruct.ParamType.String;
        }
    }
    
    /**
     * displays the program line number if the command was issued from a program file.
     * 
     * @param cmd - the command being executed
     * 
     * @return String containing line number info
     */
    private String showLineNumberInfo (int lineNum) {
        if (lineNum > 0) {
            return "(line " + lineNum + ") ";
        }
        return "";
    }

    /**
     * gets the list of argument types for a given option command.
     * 
     * @param command - the option command
     * 
     * @return the string of arg types for the command
     *         (empty String if none, null if invalid command)
     */
    public String getOptionArgs(String command) {
        OptionList optInfo = null;
        for (OptionList tblEntry : OptionTable) {
            if (tblEntry.optName.contentEquals(command)) {
                optInfo = tblEntry;
                break;
            }
        }
        if (optInfo == null)
            return null;
        
        return optInfo.argTypes;
    }

    /**
     * gets the value of a parameter and verifies it is valid.
     * 
     * @param parm  - the argument list
     * @param index - the index of the argument to get
     * 
     * @return unsigned value
     * 
     * @throws ParserException if not value Unsigned value
     */
    public Integer getUnsignedValue (ArrayList<ParameterStruct> parm, int index) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (index > parm.size()) {
            throw new ParserException(functionId + "Index " + index + " exceeds max arg list of " + parm.size());
        }
        ParameterStruct parmVal = parm.get(index);
        ParameterStruct param = ParameterStruct.verifyArgEntry (parmVal, ParameterStruct.ParamType.Unsigned);
        return param.getIntegerValue().intValue();
    }
        
    /**
     * run a command option from the the command line (NOT from the program file).
     * This will format the array of strings into 1 or more commands to execute
     *   and then execute them one by one.
     * 
     * @param args - an array of Strings that consist of one or more command options and their arguments
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException 
     */
    public void runCommandLine (String [] args) throws ParserException, IOException, SAXException, TikaException {
        // check for help message request
        if (args[0].contentEquals("-help")) {
            helpMessage();
            return;
        }
        
        // read the command line and separate into individual option commands, in case there
        //  were more than one on the command line
        ArrayList<String> optArgs = new ArrayList<>(Arrays.asList(args));
                
        // since this comes from the user and not a file, there is no line number for the command
        // so we set it to 0 to indicate this.
        ArrayList<CommandStruct> commandList = formatCmdOptions (optArgs, 0);
        ArrayList<String> response = new ArrayList<>();
        for (int ix = 0; ! commandList.isEmpty(); ix++) {
            // get each command option line and convert to an ArrayList (command followed by args)
            CommandStruct cmdLine = commandList.removeFirst();

            // execute the next command option
            ArrayList<String> rsp = executeCmdOption (cmdLine);
            if (rsp != null) {
                response.addAll(rsp);
            }
        }
        if (response.isEmpty()) {
            System.out.println("<OK>");
        } else {
            System.out.println("<" + String.join(",", response) + ">");
        }
    }
    
    /**
     * run a command option from the the program file.
     * This will execute the single command option passed.
     * 
     * @param cmdOption - the command to execute
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException 
     */
    public void runCmdOption (CommandStruct cmdOption) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": " + showLineNumberInfo(cmdOption.line);
        
        if (cmdOption.params == null) {
            throw new ParserException(functionId + "Null or empty param list");
        }

        // now run the command line option command and save any response msg
        ArrayList<String> rsp = executeCmdOption (cmdOption);
        if (rsp != null) {
            VarArray.putResponseValue(rsp);
        }
    }
    
    /**
     * creates a list of CommandStruct entries from the command line options.
     * Does some verification of the command line and splits it into multiple
     * lines if more than 1 option command is present on the line.
     * 
     * NOTE: that these are 'option' commands only, not the program commands
     * that can direct program flow and assign parameter values.
     * 
     * @param argList - the command line arguments expressed as a list of Strings
     * @param lineNum - the program file source line number for the option command(s)
     * 
     * @return a list of 1 or more CommandStruct entries of commands to execute
     * 
     * @throws ParserException 
     */
    public ArrayList<CommandStruct> formatCmdOptions (ArrayList<String> argList, int lineNum) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": " + showLineNumberInfo(lineNum);

        if (argList == null || argList.isEmpty()) {
            throw new ParserException(functionId + showLineNumberInfo(lineNum) + "Null command line");
        }

        ArrayList<CommandStruct> commands = new ArrayList<>(); // array of command lines extracted
        frame.outputInfoMsg(STATUS_COMPILE, showLineNumberInfo(lineNum) + "  splitting command option: " + String.join(" ", argList));
        
        // 1st entry is option, which may have additional args. let's see how many
        String cmdArg = argList.removeFirst();
        CommandStruct newCommand = new CommandStruct(cmdArg, lineNum);
        
        OptionList optInfo = null;
        for (OptionList tblEntry : OptionTable) {
            if (tblEntry.optName.contentEquals(cmdArg)) {
                optInfo = tblEntry;
                break;
            }
        }
        if (optInfo == null) {
            throw new ParserException(functionId + "option is not valid: " + cmdArg);
        }
        if (optInfo.argTypes.isEmpty()) {
            frame.outputInfoMsg(STATUS_COMPILE, "  option cmd: " + cmdArg + " (no args)");
        } else {
            frame.outputInfoMsg(STATUS_COMPILE, "  option cmd: " + cmdArg + " (arglist: " + optInfo.argTypes + ")");
        }
        int minArgs = 0;
        int maxArgs = (optInfo.argTypes == null || optInfo.argTypes.isEmpty()) ? 0 : optInfo.argTypes.length();
        for (int off = 0; off < maxArgs; off++) {
            // uppercase letters are required, which will indicate the min mumber
            if (optInfo.argTypes.charAt(off) >= 'A' && optInfo.argTypes.charAt(off) <= 'Z') {
                minArgs++;
            }
        }
        
        // remove entries 1 at a time starting with the command option and then
        //  adding each of its args to 'newCommand' until either another command option is found
        //  or the max number of args has been read. check for too few or too many args were
        //  found for the option.
        // Then, place this complete command into the array of commands in 'command'.
        int parmCnt = 0;
        for (int ix = 0; ! argList.isEmpty(); ix++) {
            // get next entry, whic can be either and arg for current option or a new option
            String nextArg = argList.removeFirst();
            OptionList newInfo = null;
            for (OptionList tblEntry : OptionTable) {
                if (tblEntry.optName.contentEquals(nextArg)) {
                    newInfo = tblEntry;
                    break;
                }
            }
            if (newInfo != null && parmCnt >= minArgs) {
                // new command option
                cmdArg = nextArg;
                // add current command string to list of commands
                commands.add(newCommand);
                // restart the new command list with the new option
                newCommand = new CommandStruct(cmdArg, lineNum);
                // update the option parameter list info
                parmCnt = 0;
                optInfo = newInfo;
                minArgs = 0;
                maxArgs = (optInfo.argTypes == null || optInfo.argTypes.isEmpty()) ? 0 : optInfo.argTypes.length();
                for (int off = 0; off < maxArgs; off++) {
                    // uppercase letters are required, which will indicate the min mumber
                    if (optInfo.argTypes.charAt(off) >= 'A' && optInfo.argTypes.charAt(off) <= 'Z') {
                        minArgs++;
                    }
                }
                if (optInfo.argTypes.isEmpty()) {
                    frame.outputInfoMsg(STATUS_COMPILE, showLineNumberInfo(lineNum) + "  option cmd: " + cmdArg + " (no args)");
                } else {
                    frame.outputInfoMsg(STATUS_COMPILE, showLineNumberInfo(lineNum) + "  option cmd: " + cmdArg + " (arglist: " + optInfo.argTypes + ")");
                }
            } else {
                // assume it is a parameter - verify the option takes another parameter
                if (maxArgs == 0) {
                    throw new ParserException(functionId + "Invalid entry: option " + newCommand.command
                                        + " has no params and " + nextArg + " is not a valid option");
                }
                int pix = (parmCnt < maxArgs) ? parmCnt : maxArgs - 1;
                char nextType = Character.toUpperCase(optInfo.argTypes.charAt(pix));
                ParameterStruct.ParamType parmType = getParameterType(nextType);
                if (parmCnt >= maxArgs && (parmType != ParameterStruct.ParamType.StrArray || parmType != ParameterStruct.ParamType.IntArray)) {
                    throw new ParserException(functionId + "Too many args for option " + newCommand.command
                                        + ": " + (parmCnt+1) + ", arglist = " + optInfo.argTypes);
                }

                // verify and format arg values
                ParameterStruct.ParamClass pClass = ParameterStruct.ParamClass.Discrete;
                if (nextArg.startsWith("$")) {
                    pClass = ParameterStruct.ParamClass.Reference;
                }
                ParameterStruct parmData = new ParameterStruct(nextArg, pClass, parmType);
                newCommand.params.add(parmData);
                parmCnt += 1;
            }
        }
            
        // add the remaining entry to the list of commands
        commands.add(newCommand);
        frame.outputInfoMsg(STATUS_COMPILE, commands.size() + " options found");
        return commands;
    }

    /**
     * extracts the absolute path from a filename.
     * The absolute path (if included) must begin with either a '/' or a '~' (home dir).
     * If not, it is assumed the filename either includes no path info or is relative
     *   to the current path assignment.
     * 
     * @param fname - the user-entered filename (may or may not have path included)
     * 
     * @return the path portion of the filename
     */
    private String getPathFromFilename (String fname) {
        if (fname.startsWith("/") || fname.startsWith("~")) {
            int offset = fname.lastIndexOf('/');
            if (offset == 0) {
                return "/";
            }
            if (offset > 0) {
                return fname.substring(0, offset);
            }
        }
        return "";
    }
    
    /**
     * verifies the argument types found in the command line match what is specified for the command.
     * 
     * @param command    - the command line to run
     * @param validTypes - the list of data types to match to
     * @param linenum    - the current line number of the program (for debug msgs)
     * 
     * @throws ParserException 
     */
    private static void checkArgTypes (CommandStruct command, String validTypes) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        // determine the min and max number of arguments
        int min = 0;
        int max = validTypes.length();
        for (int ix = 0; ix < max; ix++) {
            if (validTypes.charAt(ix) >= 'A' && validTypes.charAt(ix) <= 'Z') {
                min++;
            } else {
                break;
            }
        }
        
        // verify we have the correct number of arguments
        if (command.params.size() < min || command.params.size() > max) {
            throw new ParserException(functionId + command + " - Invalid number of arguments: " + command.params.size() + " (valid = " + validTypes + ")");
        }
        
        // now verify the types
        int ix = 0;
        try {
            for (ix = 0; ix < command.params.size(); ix++) {
                char type = validTypes.charAt(ix);
                type = Character.toUpperCase(type);
                ParameterStruct.ParamType expType = CmdOptions.getParameterType(type);
                ParameterStruct.verifyArgEntry (command.params.get(ix), expType);
            }
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId + command + " - arg[" + ix + "]");
        }
    }

    /**
     * executes the command line option specified
     * 
     * @param cmdLine - the option command to execute
     *        (the command entry is ignored so the option is actually the 1st param entry)
     * 
     * @return a response String if the command was a query type, else null
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException 
     */
    private ArrayList<String> executeCmdOption (CommandStruct cmdLine) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": " + showLineNumberInfo(cmdLine.line);
        ArrayList<String> response = new ArrayList<>();
        Utils.PathType pathtype;
        String fname;
        String option = cmdLine.option;
        PdfReader pdfReader = null;
        ArrayList<ParameterStruct> params = cmdLine.params;

        cmdLine.showCommand("");

        String argTypes = getOptionArgs(cmdLine.option);
        if (argTypes == null) {
            throw new ParserException(functionId + "option is not valid: " + cmdLine.option);
        }

        // verify integrity of params
        checkArgTypes (cmdLine, argTypes);
                
        // the rest will be the parameters associated with the option (if any) plus any additional options
        try {
            switch (option) {
                case "-debug":
                    frame.setMessageFlags(getUnsignedValue(params, 0));
                    break;
                case "-sfile":
                    pathtype = Utils.PathType.Spreadsheet;
                    fname = params.get(0).getStringValue();
                    String absPath = getPathFromFilename (fname);
                    if (! absPath.isEmpty() && fname.length() > absPath.length() + 1) {
                        Utils.setDefaultPath(pathtype, absPath);
                        fname = fname.substring(absPath.length() + 1);
                    }
                    File ssheetFile = Utils.checkFilename (fname, ".ods", pathtype, true);
                    Spreadsheet.selectSpreadsheet(ssheetFile);
                    break;
                case "-load":
                    Integer numTabs = getUnsignedValue(params, 0);
                    boolean bCheckHeader = params.get(1).getBooleanValue();
                    if (numTabs <= 0) {
                        throw new ParserException(functionId + "Invalid number of tabs to load: " + numTabs);
                    }
                    Spreadsheet.loadSheets(numTabs, bCheckHeader);
                    break;
                case "-tab":
                    Integer tab = getUnsignedValue(params, 0);
                    Spreadsheet.selectSpreadsheetTab (tab.toString());
                    break;
                case "-cfile":
                    // clipbaord uses the Test path for its base dir
                    pathtype = Utils.PathType.Test;
                    fname = params.get(0).getStringValue();
                    File fClip = Utils.checkFilename (fname, ".txt", pathtype, false);
                    frame.outputInfoMsg(STATUS_PROGRAM, "  " + pathtype + " file: " + fClip.getAbsolutePath());
                    AmazonParser amazonParser = new AmazonParser(fClip);
                    amazonParser.parseWebData();
                    break;
                case "-update":
                    frame.outputInfoMsg(STATUS_PROGRAM, "  Updating spreadsheet from clipboards");
                    AmazonParser.updateSpreadsheet();
                    break;
                case "-pfile":
                    pathtype = Utils.PathType.PDF;
                    fname = params.get(0).getStringValue();
                    absPath = getPathFromFilename (fname);
                    if (! absPath.isEmpty() && fname.length() > absPath.length() + 1) {
                        Utils.setDefaultPath(pathtype, absPath);
                        fname = fname.substring(absPath.length() + 1);
                    }
                    File pdfFile = Utils.checkFilename (fname, ".pdf", pathtype, false);
                    frame.outputInfoMsg(STATUS_PROGRAM, "  " + pathtype + " file: " + pdfFile.getAbsolutePath());
                    pdfReader = new PdfReader();
                    pdfReader.readPdfContents(pdfFile);
                    response.addAll(pdfReader.getContents());
                    break;
                case "-prun":
                    if (pdfReader == null) {
                        throw new ParserException(functionId + "Invalid date conversion");
                    }
                    pdfReader.processData();
                    break;
                case "-clip":
                    boolean bStrip = false;
                    if (! params.isEmpty()) {
                        bStrip = params.get(0).getBooleanValue();
                    }
                    
                    // read from clipboard into response
                    ClipboardReader clipReader = new ClipboardReader();
                    String line;
                    while ((line = clipReader.getLine()) != null) {
                        if (bStrip) {
                            line = line.strip();
                            if (line.isEmpty()) {
                                continue;
                            }
                        }
                        response.add(line);
                    }
                    break;
                case "-tpath":
                    String path;
                    if (params.isEmpty()) {
                        path = System.getProperty("user.dir");
                        frame.outputInfoMsg(STATUS_PROGRAM, "  set Test path to current running directory: " + path);
                    } else {
                        path = params.get(0).getStringValue();
                        frame.outputInfoMsg(STATUS_PROGRAM, "  set Test path to: " + path);
                    }
                    Utils.setDefaultPath(Utils.PathType.Test, path);
                    frame.setTestOutputFile(path);
                    break;
                case "-ofile":
                    if (params.isEmpty()) {
                        frame.outputInfoMsg(STATUS_PROGRAM, "  Output messages to stdout");
                        frame.setTestOutputFile(null);
                    } else {
                        fname = params.get(0).getStringValue();
                        frame.outputInfoMsg(STATUS_PROGRAM, "  Output messages to file: " + fname);
                        frame.setTestOutputFile(fname);
                    }
                    break;
                case "-save":
                    // save the spreadsheet and reload so another spreadsheet change can be made
                    Spreadsheet.saveSpreadsheetFile();
                    break;
                case "-date":
                    String strDate = params.get(0).getStringValue();
                    LocalDate date = DateFormat.getFormattedDate (strDate, false);
                    String convDate = DateFormat.convertDateToString(date, true);
                    if (convDate == null) {
                        throw new ParserException(functionId + "Invalid date conversion");
                    }
                    response.add(convDate);
                    break;
                case "-datep":
                    strDate = params.get(0).getStringValue();
                    date = DateFormat.getFormattedDate (strDate, true);
                    convDate = DateFormat.convertDateToString(date, true);
                    if (convDate == null) {
                        throw new ParserException(functionId + "Invalid date conversion");
                    }
                    response.add(convDate);
                    break;
                case "-default":
                    numTabs = getUnsignedValue(params, 0);
                    bCheckHeader = params.get(1).getBooleanValue();
                    if (numTabs <= 0) {
                        throw new ParserException(functionId + "Invalid number of tabs to load: " + numTabs);
                    }
                    String ssPath = Utils.getPathFromPropertiesFile(PropertiesFile.Property.SpreadsheetPath);
                    String ssFname = props.getPropertiesItem(PropertiesFile.Property.SpreadsheetFile, "");
                    if (ssPath != null && ssFname != null) {
                        File ssFile = new File(ssPath + "/" + ssFname);
                        Spreadsheet.selectSpreadsheet(ssFile);
                        Spreadsheet.loadSheets(numTabs, bCheckHeader);
                    }
                    String strTab = props.getPropertiesItem(PropertiesFile.Property.SpreadsheetTab, "0");
                    Spreadsheet.selectSpreadsheetTab (strTab);
                    break;
                case "-maxcol":
                    Integer iCol = Spreadsheet.getSpreadsheetColSize ();
                    response.add("" + iCol);
                    break;
                case "-maxrow":
                    Integer iRow = Spreadsheet.getSpreadsheetRowSize ();
                    response.add("" + iRow);
                    break;
                case "-setsize":
                    iCol = getUnsignedValue(params, 0);
                    iRow = getUnsignedValue(params, 1);
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    Spreadsheet.setSpreadsheetSize (iCol, iRow);
                    break;
                case "-find":
                    String order = params.get(0).getStringValue();
                    iRow = Spreadsheet.findItemNumber(order);
                    response.add("" + iRow);
                    break;
                case "-class":
                    iCol = getUnsignedValue(params, 0);
                    iRow = getUnsignedValue(params, 1);
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    String strValue = Spreadsheet.getSpreadsheetCellClass(iCol, iRow);
                    response.add("" + strValue);
                    break;
                case "-color":
                    Integer iColor;
                    iCol   = getUnsignedValue(params, 0);
                    iRow   = getUnsignedValue(params, 1);
                    iColor = getUnsignedValue(params, 2);
                    if (iCol == null || iRow == null || iColor == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow + ", color = " + iColor);
                    }
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColorOfTheMonth(iColor));
                    break;
                case "-RGB":
                    Integer iRGB;
                    iCol = getUnsignedValue(params, 0);
                    iRow = getUnsignedValue(params, 1);
                    iRGB = getUnsignedValue(params, 2);
                    if (iCol == null || iRow == null || iRGB == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow + ", RGB = " + iRGB);
                    }
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColor("RGB", iRGB));
                    break;
                case "-HSB":
                    Integer iHSB;
                    iCol = getUnsignedValue(params, 0);
                    iRow = getUnsignedValue(params, 1);
                    iHSB = getUnsignedValue(params, 2);
                    if (iCol == null || iRow == null || iHSB == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow + ", HSB = " + iHSB);
                    }
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColor("HSB", iHSB));
                    break;
                case "-cellget":
                    iCol = getUnsignedValue(params, 0);
                    iRow = getUnsignedValue(params, 1);
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    String cellValue = Spreadsheet.getSpreadsheetCell(iCol, iRow);
                    response.add(cellValue);
                    break;
                case "-cellclr":
                    iCol = getUnsignedValue(params, 0);
                    iRow = getUnsignedValue(params, 1);
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    cellValue = Spreadsheet.putSpreadsheetCell(iCol, iRow, null);
                    response.add(cellValue);
                    break;
                case "-cellput":
                    iCol = getUnsignedValue(params, 0);
                    iRow = getUnsignedValue(params, 1);
                    String strText = params.get(2).getStringValue();
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    cellValue = Spreadsheet.putSpreadsheetCell(iCol, iRow, strText);
                    response.add(cellValue);
                    break;
                case "-rowget":
                    Integer iCount;
                    iCol   = getUnsignedValue(params, 0);
                    iRow   = getUnsignedValue(params, 1);
                    iCount = getUnsignedValue(params, 2);
                    if (iCol == null || iRow == null || iCount == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow + ", count = " + iCount);
                    }
                    ArrayList<String> arrValue = Spreadsheet.getSpreadsheetRow(iCol, iRow, iCount);
                    response.addAll(arrValue);
                    break;
                case "-colget":
                    iCol   = getUnsignedValue(params, 0);
                    iRow   = getUnsignedValue(params, 1);
                    iCount = getUnsignedValue(params, 2);
                    if (iCol == null || iRow == null || iCount == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow + ", count = " + iCount);
                    }
                    arrValue = Spreadsheet.getSpreadsheetCol(iCol, iRow, iCount);
                    response.addAll(arrValue);
                    break;
                case "-rowput":
                    ArrayList<String> arrList;
                    iCol    = getUnsignedValue(params, 0);
                    iRow    = getUnsignedValue(params, 1);
                    arrList = params.get(2).getStrArray();
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    if (arrList == null) {
                        throw new ParserException(functionId + "Invalid array value: strArray is null");
                    }
                    Spreadsheet.putSpreadsheetRow(iCol, iRow, arrList);
                    break;
                case "-colput":
                    iCol    = getUnsignedValue(params, 0);
                    iRow    = getUnsignedValue(params, 1);
                    arrList = params.get(2).getStrArray();
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    if (arrList == null) {
                        throw new ParserException(functionId + "Invalid array value: strArray is null");
                    }
                    Spreadsheet.putSpreadsheetCol(iCol, iRow, arrList);
                    break;
                case "-rowcolor":
                    ArrayList<Long> arrLong;
                    iCol    = getUnsignedValue(params, 0);
                    iRow    = getUnsignedValue(params, 1);
                    arrLong  = params.get(2).getIntArray();
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    if (arrLong == null) {
                        throw new ParserException(functionId + "Invalid array value: intArray is null");
                    }
                    Spreadsheet.putSpreadsheetColorRow(iCol, iRow, arrLong);
                    break;
                case "-colcolor":
                    iCol    = getUnsignedValue(params, 0);
                    iRow    = getUnsignedValue(params, 1);
                    arrLong  = params.get(2).getIntArray();
                    if (iCol == null || iRow == null || arrLong == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    if (arrLong == null) {
                        throw new ParserException(functionId + "Invalid array value: intArray is null");
                    }
                    Spreadsheet.putSpreadsheetColorCol(iCol, iRow, arrLong);
                    break;
                default:
                    throw new ParserException(functionId + "Invalid option: " + option);
            }
        } catch (IndexOutOfBoundsException ex) {
            if (params.isEmpty()) {
                throw new ParserException(functionId + "Attempt to retrieve parameter when parameter list is empty"
                                    + " for option " + option + "\n  -> " + ex);
            } else {
                throw new ParserException(functionId + "Parameter index exceeded max of " + (params.size()-1)
                                    + " for option " + option + "\n  -> " + ex);
            }
        }
        
        return response;
    }

    private static void helpMessage() {
        System.out.println(" -f <file>      = to execute commands from a script file (*.scr)");
        System.out.println(" -c <file>      = to run compiler on a script file (*.scr) for error checking");
        System.out.println("");
        System.out.println(" -ofile <file>  = the name of the file to output results to (default: use site.properties reference)");
        System.out.println(" -pfile <file>  = the name of the PDF file to execute (*.pdf) and loads the contents into memory");
        System.out.println(" -cfile <file>  = the name of the clipboard file to load (*.txt)");
        System.out.println(" -sfile <file>  = the name of the spreadsheet file to modify (*.ods)");
        System.out.println(" -load <tabs> <bool> = the number of tabs to load from the spreadsheet");
        System.out.println("                  FALSE if don't check for header, TRUE if normal header check");
        System.out.println(" -tab <tab>     = the name (or zero-ref number) of the tab selection in the spreadsheet");
        System.out.println(" -clip <bool>   = loads the sys clipboard contents into memory (optional TRUE to strip whitespace)");
        System.out.println(" -prun          = execute the PDF file loaded from the -pfile option");
        System.out.println(" -update        = execute the update of the clipboards loaded");
        System.out.println(" -save          = save current data to spreadsheet file and reload");
        System.out.println(" -debug <flags> = the debug messages to enable when running");
        System.out.println("");
        System.out.println("     The debug flag values are hex bit values and defined as:");
        System.out.println("     x01  =    1 = STATUS_NORMAL");
        System.out.println("     x02  =    2 = STATUS_PARSER");
        System.out.println("     x04  =    4 = STATUS_SPREADSHEET");
        System.out.println("     x08  =    8 = STATUS_INFO");
        System.out.println("     x10  =   16 = STATUS_PROPS");
        System.out.println("     x20  =   32 = STATUS_PROGRAM");
        System.out.println("     x40  =   64 = STATUS_COMPILE");
        System.out.println("     x80  =  126 = STATUS_VARS");
        System.out.println("     x800 = 2048 = STATUS_DEBUG");
        System.out.println("     e.g. -d xFFFF will enable all msgs");
        System.out.println();
        System.out.println("The following commands test special features:");
        System.out.println();
        System.out.println(" -date  <date value>  = get the date converted to YYYY-MM-DD format (assume future)");
        System.out.println(" -datep <date value>  = get the date converted to YYYY-MM-DD format (assume past)");
        System.out.println(" -default <0|1>       = load the last spreadsheet and tab selection");
        System.out.println("                        0 if don't check for header, 1 if normal header check");
        System.out.println(" -maxcol              = get the number of columns in the spreadsheet");
        System.out.println(" -maxrow              = get the number of rows    in the spreadsheet");
        System.out.println(" -setsize <col> <row> = set the col and row size of the loaded spreadsheet");
        System.out.println(" -find    <order #>   = get the spreadsheet 1st row containing order#");
        System.out.println(" -class   <col> <row>          = get the spreadsheet cell class type");
        System.out.println(" -color   <col> <row> <color>  = set cell background to color of the month (0 to clear)");
        System.out.println(" -RGB     <col> <row> <RGB>    = set cell background to specified RGB hexadecimal color");
        System.out.println(" -HSB     <col> <row> <HSB>    = set cell background to specified HSB hexadecimal color");
        System.out.println(" -cellclr <col> <row>          = clear the spreadsheet cell data");
        System.out.println(" -cellget <col> <row>          = get the spreadsheet cell data");
        System.out.println(" -cellput <col> <row> <text>   = set the spreadsheet cell data");
        System.out.println(" -rowget  <col> <row>          = get the spreadsheet cell data from a row");
        System.out.println(" -rowput  <col> <row> <array>  = set the spreadsheet cell data for a row");
        System.out.println(" -rowcolor <col> <row> <array> = set the spreadsheet background color for a row");
        System.out.println(" -colget  <col> <row>          = get the spreadsheet cell data from a column");
        System.out.println(" -colput  <col> <row> <array>  = set the spreadsheet cell data for a column");
        System.out.println(" -colcolor <col> <row> <array> = set the spreadsheet background color for a column");
        System.out.println();
        System.out.println(" The path used for the all files is the value of the 'TestPath' entry in the");
        System.out.println("   site.properties file. If the properties file doesn't exist or 'TestPath'");
        System.out.println("   is not defined in it, the current directory will be used as the path.");
        System.out.println();
    }

}
