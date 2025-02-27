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
import java.util.Arrays;
import java.util.HashMap;
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
        
        OptionList (String opt, String args) {
            optName  = opt;
            argTypes = args;
        }
    }

    private static String  strResponse = "";    // response from last RUN command
    private static Integer intResult = 0;       // result of last CALC command
    private static final HashMap<String, String>  strParams = new HashMap<>();
    private static final HashMap<String, Integer> intParams = new HashMap<>();
    
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
        new OptionList ("-save"     , ""),

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

    // defines the structure for file commands
    private class CommandStruct {
        public String                     command;
        public ArrayList<ParameterStruct> params;
        
        CommandStruct(String cmd) {
            command = cmd;
            params  = new ArrayList<>();
        }
        
        String showCommand () {
            String strCommand = command;
            for (int ix = 0; ix < params.size(); ix++) {
                ParameterStruct parStc = params.get(ix);
                strCommand += " [" + parStc.paramType + "]";
                switch (parStc.paramType) {
                    case 'I':
                    case 'U':
                        if (parStc.intParam == null)
                            strCommand += " (null)";
                        else
                            strCommand += " " + parStc.intParam.toString();
                        break;
                    case 'B':
                        if (parStc.boolParam == null)
                            strCommand += " (null)";
                        else
                            strCommand += " " + parStc.boolParam.toString();
                        break;
                    default:
                        if (parStc.strParam == null)
                            strCommand += " (null)";
                        else
                            strCommand += " '" + parStc.strParam + "'";
                        break;
                }
            }
            
            return strCommand;
        }
    } 

    private final class ParameterStruct {
        String   strParam;
        Integer  intParam;
        Boolean  boolParam;
        char     paramType;
        
        ParameterStruct (Object objParam, char dataType) throws ParserException {
            switch (objParam.getClass().toString()) {
                case "class java.lang.Integer":
                    setIntegerValue ((Integer) objParam, dataType);
                    break;
                case "class java.lang.Boolean":
                    setBooleanValue ((Boolean) objParam, dataType);
                    break;
                case "class java.lang.String":
                    setStringValue ((String) objParam, dataType);
                    break;
                default:
                    break;
            }
        }
        
        void setStringValue (String strVal, char dataType) throws ParserException {
        String functionId = CLASS_NAME + ".setStringValue: ";
        
            intParam = null;
            boolParam = null;
            strParam = strVal;

            // if it is a parameter, save the param name as a string and allow it
            if (strVal.startsWith("$")) {
                frame.outputInfoMsg(STATUS_PARSER, "     'S' param: " + strParam);
                paramType = 'S';
                return;
            }
            try {
                switch (dataType) {
                    case 'I':
                        intParam = Utils.getIntValue(strVal);
                        break;
                    case 'U':
                        intParam = Utils.getHexValue (strVal);
                        if (intParam == null) {
                            intParam = Utils.getIntValue(strVal);
                            if (intParam < 0) {
                                throw new NumberFormatException("");
                            }
                        }
                        break;
                    case 'B':
                        boolParam = Utils.getBooleanValue(strVal);
                        break;
                    default:
                        // all string types
                        break;
                }
            } catch (NumberFormatException ex) {
                throw new ParserException(functionId + "Invalid param data for dataType " + dataType + ": param: " + strVal);
            }
            
            frame.outputInfoMsg(STATUS_PARSER, "     '" + dataType + "' param: " + strParam);
            paramType = dataType;
        }
        
        void setIntegerValue (Integer intVal, char dataType) throws ParserException {
        String functionId = CLASS_NAME + ".setIntegerValue: ";
        
            strParam = null;
            boolParam = null;
            intParam = intVal;

            switch (dataType) {
                case 'I':
                    break;
                case 'U':
                    if (intParam < 0) {
                        throw new ParserException(functionId + "Invalid param data for dataType " + dataType + ": param: " + intVal);
                    }
                    break;
                case 'B':
                    boolParam = intParam != 0;
                    break;

                default:
                    // all string types
                    strParam = intParam.toString();
                    break;
            }
            
            paramType = dataType;
            frame.outputInfoMsg(STATUS_PARSER, "     '" + paramType + "' param: " + intParam);
        }
        
        void setBooleanValue (Boolean boolVal, char dataType) {
        
            intParam = null;
            strParam = null;
            boolParam = boolVal;

            switch (dataType) {
                case 'I':
                case 'U':
                    intParam = boolVal ? 1 : 0;
                    break;
                case 'B':
                    break;
                default:
                    // all string types
                    strParam = boolParam.toString();
                    break;
            }
            
            paramType = dataType;
            frame.outputInfoMsg(STATUS_PARSER, "     '" + paramType + "' param: " + boolParam);
        }
    } 

    public void runCommandLine (String[] args) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + ".runCommandLine: ";
        
        frame.outputInfoMsg(STATUS_NORMAL, "command line entered: " + String.join(" ", args));

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

                // enable timestamp on log messages
                frame.elapsedTimerEnable();

                // compile the program
                frame.outputInfoMsg(STATUS_PARSER, "BEGINING PROGRAM COMPILE");
                ArrayList<CommandStruct> cmdList = compileProgram(args[1]);

                // execute the program by running each 'cmdList' entry
                frame.outputInfoMsg(STATUS_PARSER, "BEGINING PROGRAM EXECUTION");
                int cmdIx = 0;
                while (cmdIx >= 0 && cmdIx < cmdList.size()) {
                    cmdIx = executeProgramCommand (cmdIx, cmdList.get(cmdIx));
                }
                frame.elapsedTimerDisable();
                break;
            default:
                // read the command line and separate into individual option commands, in case there
                //  were more than one on the command line
                ArrayList<String> optArgs = new ArrayList<>(Arrays.asList(args));
                
                ArrayList<CommandStruct> commandList = SplitCmdOptionsLine (optArgs);
                ArrayList<String> response = new ArrayList<>();
                for (int ix = 0; ! commandList.isEmpty(); ix++) {
                    // get each command option line and convert to an ArrayList (command followed by args)
                    CommandStruct cmdLine = commandList.removeFirst();

                    // execute the next command option
                    String rsp = executeCmdOption (cmdLine);
                    if (rsp != null) {
                        if (rsp.isEmpty())
                            rsp = "---";
                        response.add(rsp);
                    }
                }
                if (response.isEmpty()) {
                    System.out.println("<OK>");
                } else {
                    System.out.println("<" + String.join(",", response) + ">");
                }
                break;
        }
    }

    private ArrayList<CommandStruct> compileProgram (String fname) throws ParserException, IOException {
        String functionId = CLASS_NAME + ".compileProgram: ";

        frame.outputInfoMsg(STATUS_NORMAL, "Compiling file: " + fname);
        ArrayList<CommandStruct> cmdList = new ArrayList<>();
        String line;

        // open the file to compile and extract the commands from it
        File scriptFile = checkFilename (fname, ".scr", "Script", false);
        FileReader fReader = new FileReader(scriptFile);
        BufferedReader fileReader = new BufferedReader(fReader);

        // clear out the parameter values
        strParams.clear();
        intParams.clear();
        strResponse = "";
        intResult = 0;

        // read the program and compile into ArrayList 'cmdList'
        int lineNum = 1;
        while ((line = fileReader.readLine()) != null) {
            line = line.strip();
            if (line.isBlank() || line.charAt(0) == '#') {
                continue;
            }

            String lineInfo = "LINE " + lineNum + ": ";

            // first, extract the 1st word as the command keyword
            // 'cmdStruct' will receive the command, with the params yet to be placed.
            // 'parmList'  will be a string containing the remainder of the command line
            // 'parmArr'   will be an array of Strings corresponding to 'parmList'
            CommandStruct cmdStruct;
            String [] parmArr = null;
            int paramCnt = 0;
            int offset = line.indexOf(" ");
            if (offset <= 0) {
                cmdStruct = new CommandStruct(line);
            } else {
                cmdStruct = new CommandStruct(line.substring(0, offset).stripTrailing());
                String parmList = line.substring(offset).stripLeading();
                parmArr = parmList.split(" ");
                paramCnt = parmArr.length;
            }
            frame.outputInfoMsg(STATUS_PARSER, "compiling program command: " + cmdStruct.command + " " + String.join(" ", parmArr));

            // now let's check for valid command keywords and extract the parameters
            //  into the cmdStruct structure.
            switch (cmdStruct.command) {
                case "SET":
                    String argList = (paramCnt > 2) ? "SL" : "SS";
                    if (parmArr[0].startsWith("I_")) {
                        argList = "SI";
                    }
                    cmdStruct.params = packParamList (line, argList, false);
                    ParameterStruct parmName  = cmdStruct.params.get(0);   // Name of the parameter
                    ParameterStruct parmValue = cmdStruct.params.get(1);   // Value to set the parameter to

                    // make sure we are not using a reserved parameter name
                    switch (parmName.strParam) {
                        case "RESPONSE":
                        case "RESULT":
                            throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " using reserved name: " + parmName);
                        default:
                            break;
                    }

                    // if value is a parameter itself, we can only do run-time check, so just pass it
                    if (parmValue.strParam != null && parmValue.strParam.startsWith("$")) {
                        strParams.put(parmName.strParam, parmValue.strParam);
                    }
                    
                    // for integer type parameters, verify it is an integer value being assigned
                    if (parmName.strParam.startsWith("I_")) {
                        if (parmValue.intParam == null) {
                            throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " Integer arg not defined for: " + parmName);
                        }
                        if (! intParams.containsKey(parmName.strParam)) {
                            intParams.put(parmName.strParam, parmValue.intParam);
                            frame.outputInfoMsg(STATUS_PARSER, "   - Added Integer parameter " + parmName.strParam + " init to " + parmValue.intParam);
                        } else {
                            frame.outputInfoMsg(STATUS_PARSER, "   - Integer parameter " + parmName.strParam + " already defined");
                        }
                    } else {
                        if (! strParams.containsKey(parmName.strParam)) {
                            strParams.put(parmName.strParam, parmValue.strParam);
                            frame.outputInfoMsg(STATUS_PARSER, "   - Added String parameter " + parmName.strParam + " init to '" + parmValue.strParam + "'");
                        } else {
                            frame.outputInfoMsg(STATUS_PARSER, "   - String parameter " + parmName.strParam + " already defined");
                        }
                    }
                    break;
                    
                case "RUN":
                default:
                    // verify the option command and its parameters
                    ArrayList<String> optCmd = new ArrayList<>(Arrays.asList(parmArr));
                    ArrayList<CommandStruct> runList = SplitCmdOptionsLine (optCmd);
                    
                    // if there was more than 1 command on the line, move all but the last here
                    for (int ix = 0; ix < runList.size(); ix++) {
                        cmdStruct = runList.removeFirst();
                        cmdList.add(cmdStruct);
                    }
                    cmdStruct = null; // clear this since we have copied all the commands from here
                    break;
            }

            // all good, add command to list
            if (cmdStruct != null) {
                cmdList.add(cmdStruct);
                lineNum++;
            }
        }

        fileReader.close();
        
        // the last line will be the one to end the program flow
        cmdList.add(new CommandStruct("EXIT"));
        return cmdList;
    }
    
    private int executeProgramCommand (int index, CommandStruct cmdStruct) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + ".executeProgramCommand: ";
        String lineInfo = "PROGIX " + index + ": ";
        
        // by default, the command will proceed to the next command
        index++;
        
        String command = cmdStruct.command;
        frame.outputInfoMsg(STATUS_PARSER, lineInfo + command);
        switch (command) {
            case "SET":
                String parmName  = unpackParamString (cmdStruct.params, 0);

                if (parmName.startsWith("I_")) {
                    Integer intValue = unpackParamInteger (cmdStruct.params, 1);
                    if (intParams.containsKey(parmName)) {
                        intParams.replace(parmName, intValue);
                        frame.outputInfoMsg(STATUS_PARSER, "   - Modified Integer param: " + parmName + " = " + intValue);
                    } else {
                        throw new ParserException(functionId + lineInfo + "Integer param not found: " + parmName);
                    }
                } else {
                    String strValue = unpackParamString (cmdStruct.params, 1);
                    if (strParams.containsKey(parmName)) {
                        strParams.replace(parmName, strValue);
                        frame.outputInfoMsg(STATUS_PARSER, "   - Modified String param: " + parmName + " = " + strValue);
                    } else {
                        throw new ParserException(functionId + lineInfo + "Integer param not found: " + parmName);
                    }
                }
                break;
            case "CALC":
                // TODO: store calc result value in intResult
                break;
            case "EXIT":
                index = -1; // this will terminate the program
                break;
            case "RUN":
                command = cmdStruct.params.removeFirst().strParam;
                // fall through...
            default:
                // convert the list of Strings into a struct of a command and list of args
                CommandStruct cmdOption = new CommandStruct (command);
                OptionList optInfo = getCommandInfo(command);
                if (optInfo == null) {
                    throw new ParserException(functionId + "option is not valid: " + cmdStruct.command);
                }
                String argTypes = optInfo.argTypes;

                // do any parameter conversions of the parameters passed and add
                // the converted values to the command struct.
                for (int ix = 0; ix < argTypes.length(); ix++) {
                    char parmType = Character.toUpperCase(argTypes.charAt(ix));
                    ParameterStruct argToPass = cmdStruct.params.get(ix);
                    switch (parmType) {
                        case 'I':
                        case 'U':
                            Integer intVal = unpackParamInteger(cmdStruct.params, ix);
                            argToPass.setIntegerValue(intVal, parmType);
                            break;
                        case 'B':
                            Boolean boolVal = unpackParamBoolean(cmdStruct.params, ix);
                            argToPass.setBooleanValue(boolVal, parmType);
                            break;
                        case 'S':
                        case 'D':
                        case 'F':
                        case 'L':
                            String strVal = unpackParamString(cmdStruct.params, ix);
                            argToPass.setStringValue(strVal, parmType);
                            break;
                        default:
                            throw new ParserException(functionId + "Invalid data type for RUN option " + cmdOption.command + ": " + parmType);
                    }
                    cmdOption.params.add(argToPass);
                }
                
                // now run the command line option command and save any response msg
                String rsp = executeCmdOption (cmdOption);
                if (rsp != null) {
                    strResponse = rsp;
                }
                break;
        }
        
        return index;
    }

    private ArrayList<CommandStruct> SplitCmdOptionsLine (ArrayList<String> argList) throws ParserException {
        String functionId = CLASS_NAME + ".SplitCmdOptionsLine: ";

        if (argList == null || argList.isEmpty()) {
            throw new ParserException(functionId + "Null command line");
        }

        ArrayList<CommandStruct> commands = new ArrayList<>(); // array of command lines extracted
        frame.outputInfoMsg(STATUS_PARSER, "  splitting command option: " + String.join(" ", argList));
        
        // 1st entry is option, which may have additional args. let's see how many
        String cmdArg = argList.removeFirst();
        CommandStruct newCommand = new CommandStruct(cmdArg);
        OptionList optInfo = getCommandInfo(cmdArg);
        if (optInfo == null) {
            throw new ParserException(functionId + "option is not valid: " + cmdArg);
        }
        if (optInfo.argTypes.isEmpty()) {
            frame.outputInfoMsg(STATUS_PARSER, "  option cmd: " + cmdArg + " (no args)");
        } else {
            frame.outputInfoMsg(STATUS_PARSER, "  option cmd: " + cmdArg + " (arglist: " + optInfo.argTypes + ")");
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
            OptionList newInfo = getCommandInfo(nextArg);
            if (newInfo != null && parmCnt >= minArgs) {
                // new command option
                cmdArg = nextArg;
                // add current command string to list of commands
                commands.add(newCommand);
                // restart the new command list with the new option
                newCommand = new CommandStruct(cmdArg);
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
                    frame.outputInfoMsg(STATUS_PARSER, "  option cmd: " + cmdArg + " (no args)");
                } else {
                    frame.outputInfoMsg(STATUS_PARSER, "  option cmd: " + cmdArg + " (arglist: " + optInfo.argTypes + ")");
                }
            } else {
                // assume it is a parameter - verify the option takes another parameter
                if (maxArgs == 0) {
                    throw new ParserException(functionId + "Invalid entry: option " + newCommand.command
                                        + " has no params and " + nextArg + " is not a valid option");
                }
                int pix = (parmCnt < maxArgs) ? parmCnt : maxArgs - 1;
                char parmType = Character.toUpperCase(optInfo.argTypes.charAt(pix));
                if (parmCnt >= maxArgs && parmType != 'L') {
                    throw new ParserException(functionId + "Too many args for option " + newCommand.command
                                        + ": " + (parmCnt+1) + ", arglist = " + optInfo.argTypes);
                }

                // verify data types that are restrictive
                Integer intVal;
                Boolean boolVal;
                ParameterStruct parmData = new ParameterStruct(nextArg, parmType);
                switch (parmType) {
                    case 'D':
                        checkDir (nextArg);
                        break;
                    case 'F':
                        checkFilename (nextArg, null, null, false);
                        break;
                    default:
                        break;
                }
                newCommand.params.add(parmData);
                parmCnt += 1;
            }
        }
            
        // add the remaining entry to the list of commands
        commands.add(newCommand);
        frame.outputInfoMsg(STATUS_PARSER, commands.size() + " options found");
        return commands;
    }
    
    private String executeCmdOption (CommandStruct cmdLine) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + ".executeCmdOption: ";
        String response = null;
        String filetype;
        String fname;
        String option = cmdLine.command;
        ArrayList<ParameterStruct> params = cmdLine.params;
        frame.outputInfoMsg(STATUS_PARSER, "  Executing: " + cmdLine.showCommand());

        // the rest will be the parameters associated with the option (if any) plus any additional options
        try {
            switch (option) {
                case "-d":
                    frame.setMessageFlags(params.get(0).intParam);
                    break;
                case "-s":
                    filetype = "Spreadsheet";
                    fname = params.get(0).strParam;
                    File ssheetFile = checkFilename (fname, ".ods", filetype, true);
                    Spreadsheet.selectSpreadsheet(ssheetFile);
                    break;
                case "-l":
                    Integer numTabs = params.get(0).intParam;
                    boolean bCheckHeader = params.get(1).boolParam;
                    if (numTabs <= 0) {
                        throw new ParserException(functionId + "Invalid number of tabs to load: " + numTabs);
                    }
                    Spreadsheet.loadSheets(numTabs, bCheckHeader);
                    break;
                case "-t":
                    Integer tab = params.get(0).intParam;
                    Spreadsheet.selectSpreadsheetTab (tab.toString());
                    break;
                case "-c":
                    filetype = "Clipboard";
                    fname = params.get(0).strParam;
                    File fClip = checkFilename (fname, ".txt", filetype, false);
                    frame.outputInfoMsg(STATUS_PARSER, "  " + filetype + " file: " + fClip.getAbsolutePath());
                    AmazonParser amazonParser = new AmazonParser(fClip);
                    amazonParser.parseWebData();
                    break;
                case "-u":
                    frame.outputInfoMsg(STATUS_PARSER, "  Updating spreadsheet from clipboards");
                    AmazonParser.updateSpreadsheet();
                    break;
                case "-p":
                    filetype = "PDF";
                    fname = params.get(0).strParam;
                    File pdfFile = checkFilename (fname, ".pdf", filetype, false);
                    frame.outputInfoMsg(STATUS_PARSER, "  filetype + \" file: \" + pdfFile.getAbsolutePath()");
                    PdfReader pdfReader = new PdfReader();
                    pdfReader.readPdfContents(pdfFile);
                    break;
                case "-o":
                    if (params.isEmpty()) {
                        frame.outputInfoMsg(STATUS_PARSER, "  Output messages to stdout");
                        frame.setTestOutputFile(null);
                    } else {
                        fname = params.get(0).strParam;
                        fname = getTestPath() + "/" + fname;
                    frame.outputInfoMsg(STATUS_PARSER, "  Output messages to file: " + fname);
                        frame.setTestOutputFile(fname);
                    }
                    break;
                case "-save":
                    // save the spreadsheet and reload so another spreadsheet change can be made
                    Spreadsheet.saveSpreadsheetFile();
                    break;
                case "-date":
                    String strDate = params.get(0).strParam;
                    LocalDate date = DateFormat.getFormattedDate (strDate, false);
                    String convDate = DateFormat.convertDateToString(date, true);
                    if (convDate == null) {
                        throw new ParserException(functionId + "Invalid date conversion");
                    }
                    response = convDate;
                    break;
                case "-datep":
                    strDate = params.get(0).strParam;
                    date = DateFormat.getFormattedDate (strDate, true);
                    convDate = DateFormat.convertDateToString(date, true);
                    if (convDate == null) {
                        throw new ParserException(functionId + "Invalid date conversion");
                    }
                    response = convDate;
                    break;
                case "-default":
                    numTabs = params.get(0).intParam;
                    bCheckHeader = params.get(1).boolParam;
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
                    response = "" + iCol;
                    break;
                case "-maxrow":
                    Integer iRow = Spreadsheet.getSpreadsheetRowSize ();
                    response = "" + iRow;
                    break;
                case "-setsize":
                    String strCol,strRow;
                    iCol = params.get(0).intParam;
                    iRow = params.get(1).intParam;
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    Spreadsheet.setSpreadsheetSize (iCol, iRow);
                    break;
                case "-find":
                    String order = params.get(0).strParam;
                    iRow = Spreadsheet.findItemNumber(order);
                    response = "" + iRow;
                    break;
                case "-class":
                    iCol = params.get(0).intParam;
                    iRow = params.get(1).intParam;
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    String strValue = Spreadsheet.getSpreadsheetCellClass(iCol, iRow);
                    response = strValue;
                    break;
                case "-color":
                    iCol = params.get(0).intParam;
                    iRow = params.get(1).intParam;
                    Integer iColor = params.get(2).intParam;
                    if (iCol == null || iRow == null || iColor == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow + ", color = " + iColor);
                    }
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColorOfTheMonth(iColor));
                    break;
                case "-RGB":
                    iCol = params.get(0).intParam;
                    iRow = params.get(1).intParam;
                    Integer iRGB = params.get(2).intParam;
                    if (iCol == null || iRow == null || iRGB == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow + ", RGB = " + iRGB);
                    }
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColor("RGB", iRGB));
                    break;
                case "-HSB":
                    iCol = params.get(0).intParam;
                    iRow = params.get(1).intParam;
                    Integer iHSB = params.get(2).intParam;
                    if (iCol == null || iRow == null || iHSB == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow + ", HSB = " + iHSB);
                    }
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColor("HSB", iHSB));
                    break;
                case "-cellget":
                    iCol = params.get(0).intParam;
                    iRow = params.get(1).intParam;
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    String cellValue = Spreadsheet.getSpreadsheetCell(iCol, iRow);
                    response = cellValue;
                    break;
                case "-cellclr":
                    iCol = params.get(0).intParam;
                    iRow = params.get(1).intParam;
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    cellValue = Spreadsheet.putSpreadsheetCell(iCol, iRow, null);
                    response = cellValue;
                    break;
                case "-cellput":
                    iCol = params.get(0).intParam;
                    iRow = params.get(1).intParam;
                    String strText = params.get(2).strParam;
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
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

    private String removeStringWord (String parmList) {
        int offset = parmList.indexOf(" ");
        if (offset > 0) {
            parmList = parmList.substring(offset).strip();
        } else {
            parmList = "";
        }
        return parmList;
    }
    
    private Integer findIntegerParam (String parmName) throws ParserException {
        String functionId = CLASS_NAME + ".findIntegerParam: ";
       
        Integer intVal = null;
        String strVal = null;

        // first, check if a param is being used
        if (parmName != null && parmName.charAt(0) == '$') {
            parmName = parmName.substring(1); // strip off the leading $
            if (parmName.contentEquals("RESPONSE")) {
                strVal = strResponse;
            }
            else if (parmName.contentEquals("RESULT")) {
                intVal = intResult;
            }
            else if (parmName.startsWith("I_")) {
                if (intParams.containsKey(parmName)) {
                    intVal = intParams.get(parmName);
                } else {
                    throw new ParserException(functionId + "Integer param not found: " + parmName);
                }
            }
            if (intVal == null) {
                if (strParams.containsKey(parmName)) {
                    strVal = strParams.get(parmName);
                } else {
                    throw new ParserException(functionId + "Integer param not found: " + parmName);
                }
            }
            if (strVal != null) {
                try {
                    intVal = Integer.valueOf(strVal);
                } catch (NumberFormatException ex) {
                    throw new ParserException(functionId + "String value is not an Integer: " + strVal);
                }
            }
        }        
        return intVal;
    }
    
    private String findStringParam (String parmName) throws ParserException {
        String functionId = CLASS_NAME + ".findStringParam: ";
       
        String strVal = null;

        // first, check if a param is being used
        if (parmName != null && parmName.charAt(0) == '$') {
            parmName = parmName.substring(1); // strip off the leading $
            if (parmName.contentEquals("RESPONSE")) {
                strVal = strResponse;
            }
            else if (parmName.contentEquals("RESULT")) {
                strVal = intResult.toString();
            }
            else if (parmName.startsWith("I_")) {
                if (intParams.containsKey(parmName)) {
                    strVal = intParams.get(parmName).toString();
                } else {
                    throw new ParserException(functionId + "String param not found: " + parmName);
                }
            }
            if (strVal == null) {
                if (strParams.containsKey(parmName)) {
                    strVal = strParams.get(parmName);
                } else {
                    throw new ParserException(functionId + "String param not found: " + parmName);
                }
            }
        }        
        return strVal;
    }
    
    private String unpackParamString (ArrayList <ParameterStruct> params, int ix) throws ParserException {
        String functionId = CLASS_NAME + ".unpackParamString: ";
       
        if (params == null || params.isEmpty()) {
            throw new ParserException(functionId + "Null or empty param list");
        }
        if (ix < 0 || ix >= params.size()) {
            throw new ParserException(functionId + "Invalid index to param list: " + ix + " (range is 0 to " + params.size() + ")");
        }
        ParameterStruct param = params.get(ix);
        
        // first, check if a param is being used
        String strVal = findStringParam(param.strParam);
        if (strVal == null) {
            // nope, use the actual stringified value
            switch (param.paramType) {
                case 'U':   // fall through...
                case 'I':
                    strVal = param.intParam.toString();
                    break;
                case 'B':
                    strVal = param.boolParam.toString();
                    break;
                default:    // default to String type
                    strVal = param.strParam;
                    break;
            }
        }

        frame.outputInfoMsg(STATUS_PARSER, "    unpacked 'S' value: '" + strVal + "'");
        return strVal;
    }
    
    private Integer unpackParamInteger (ArrayList <ParameterStruct> params, int ix) throws ParserException {
        String functionId = CLASS_NAME + ".unpackParamInteger: ";
       
        if (params == null || params.isEmpty()) {
            throw new ParserException(functionId + "Null or empty param list");
        }
        if (ix < 0 || ix >= params.size()) {
            throw new ParserException(functionId + "Invalid index to param list: " + ix + " (range is 0 to " + params.size() + ")");
        }
        ParameterStruct param = params.get(ix);

        // first, check if a param is being used
        Integer intVal = findIntegerParam(param.strParam);
        if (intVal == null) {
            // nope, use the actual stringified value
            switch (param.paramType) {
                case 'U':   // fall through...
                case 'I':
                    intVal  = param.intParam;
                    break;
                case 'B':
                    intVal = (param.boolParam == true) ? 1 : 0;
                    break;
                default:    // default to String type
                    intVal = Utils.getHexValue(param.strParam);
                    if (intVal == null)
                        intVal = Utils.getIntValue(param.strParam);
                    break;
            }
        }

        frame.outputInfoMsg(STATUS_PARSER, "    unpacked 'I' value: " + intVal);
        return intVal;
    }
    
    private Boolean unpackParamBoolean (ArrayList <ParameterStruct> params, int ix) throws ParserException {
        String functionId = CLASS_NAME + ".unpackParamBoolean: ";
       
        if (params == null || params.isEmpty()) {
            throw new ParserException(functionId + "Null or empty param list");
        }
        if (ix < 0 || ix >= params.size()) {
            throw new ParserException(functionId + "Invalid index to param list: " + ix + " (range is 0 to " + params.size() + ")");
        }
        ParameterStruct param = params.get(ix);
        
        // first, check if a param is being used
        Boolean boolVal = null;
        String strVal = findStringParam(param.strParam);
        if (strVal != null) {
            boolVal = Utils.getBooleanValue(strVal);
        }
        else {
            // nope, use the actual stringified value
            switch (param.paramType) {
                case 'U':   // fall through...
                case 'I':
                    boolVal = (param.intParam != 0);
                    break;
                case 'B':
                    boolVal = param.boolParam;
                    break;
                default:    // default to String type
                    boolVal = Utils.getBooleanValue(param.strParam);
                    break;
            }
        }

        frame.outputInfoMsg(STATUS_PARSER, "    unpacked 'B' value: " + boolVal);
        return boolVal;
    }
    
    /**
     * This takes a command line and a list of arg types for the command,
     *   extracts the parameter list from it, verifies the arg types,
     *   and places them in an ArrayList.
     *   NOTE: it allows for $ parameter substitution, but does not convert
     *         the values;.
     * 
     * @param line     - the command line along with all of the arguments it contains
     * @param argTypes - the list of data types for each parameter it has
     * 
     * @return the ArrayList of arguments for the command
     * 
     * @throws ParserException 
     */
    private ArrayList<ParameterStruct> packParamList (String line, String argTypes, boolean bReplace) throws ParserException {
        String functionId = CLASS_NAME + ".packParamList: ";

        int minParams = 0;  // count the min number of parameters for the command
        int maxParams = 0;  // this is the max number of params for this option
        if (argTypes == null) {
            argTypes = "";
        } else if (! argTypes.isEmpty()) {
            maxParams = argTypes.length();
            for (int ix = 0; ix < argTypes.length(); ix++) {
                if (argTypes.charAt(ix) > 'Z')
                    minParams++;
            }
        }
        
        // first, extract the 1st word as the command keyword
        ArrayList<ParameterStruct> params = new ArrayList<>();
        int offset = line.indexOf(" ");
        if (offset <= 0) {
            // single word, which would be the command with no params
            if (minParams > 0) {
                throw new ParserException(functionId + "command is missing arguments: ");
            }
            return params;
        }

        String command = line.substring(0, offset).stripTrailing();
        line = line.substring(offset).stripLeading();
        String [] strArr = line.split(" ");
        ArrayList<String> entries = new ArrayList<>(Arrays.asList(strArr));
        int ix;
        
//        // do a replacement for all parameter entries found in string
//        if (bReplace) {
//            for (ix = 0; ix < entries.size() && ix < maxParams; ix++) {
//                String nextArg = entries.get(ix);
//                if (nextArg.charAt(0) == '$') {
//                    String replacement = null;
//                    offset = line.indexOf(nextArg);
//                    if (offset < 0) {
//                        throw new ParserException(functionId + "Wierd - extracted arg was not found in string: " + nextArg);
//                    }
//                    int arglen = nextArg.length();
//                    if (nextArg.contentEquals("$RESPONSE")) {
//                        replacement = strResponse;
//                    }
//                    else if (nextArg.contentEquals("$RESULT")) {
//                        replacement = intResult.toString();
//                    }
//                    else if (nextArg.startsWith("$I_")) {
//                        if (intParams.containsKey(nextArg.substring(1))) {
//                            replacement = intParams.get(nextArg.substring(1)).toString();
//                        }
//                    } else {
//                        if (strParams.containsKey(nextArg.substring(1))) {
//                            replacement = strParams.get(nextArg.substring(1));
//                        }
//                    }
//                    // if a parameter was found, make the substitution in the line
//                    if (replacement != null) {
//                        line = line.substring(0, offset) + replacement
//                             + line.substring(offset + arglen);
//                    }
//                }
//            }
//        }
        
        for (ix = 0; ix < entries.size() && ix < maxParams; ix++) {
            char parmType = argTypes.charAt(ix);
            ParameterStruct parmStc = null;
            String nextArg = entries.get(ix);

            parmType = Character.toUpperCase(parmType);
            switch (parmType) {
                case 'L':
                    // lists take the remaining string info in the command line
                    nextArg = line;
                    parmStc = new ParameterStruct(nextArg, parmType);
                    ix = entries.size(); // make sure we exit here
                    break;
                case 'U':
                    // unsigned allows hex values if they begin with 'x' or '0x'
                    Integer intVal = Utils.getHexValue (nextArg);
                    if (intVal == null) {
                        intVal = Utils.getIntValue (nextArg);
                    }
                    parmStc = new ParameterStruct(intVal, parmType);
                    break;
                case 'I':
                    intVal = Utils.getIntValue (nextArg);
                    parmStc = new ParameterStruct(intVal, parmType);
                    break;
                case 'B':
                    Boolean boolVal = Utils.getBooleanValue (nextArg);
                    parmStc = new ParameterStruct(boolVal, parmType);
                    break;
                case 'S':
                    parmStc = new ParameterStruct(nextArg, parmType);
                    break;
                case 'D':
                    checkDir (nextArg);
                    parmStc = new ParameterStruct(nextArg, parmType);
                    break;
                case 'F':
                    checkFilename (nextArg, null, null, false);
                    parmStc = new ParameterStruct(nextArg, parmType);
                    break;
                default:
                    throw new ParserException(functionId + "Invalid data type for param: " + parmType);
            }
            frame.outputInfoMsg(STATUS_PARSER, "    packed command " + command + " parmType '" + parmType + "' value: " + nextArg);

            // make sure param type looks ok
            if (parmStc == null) {
                throw new ParserException(functionId + "ParameterStruct value not setup");
            }
            
            // OK, then add the param to the arg list
            params.add(parmStc);
           
            // remove that param from the String of params (in case we have a List param)
            line = removeStringWord(line);
        }

        // if we have exceeded the number of items defined for the option
        if (ix < entries.size() - 1) {
            throw new ParserException(functionId + "Args list for option "
                                        + command + " exceeded max allowed: " + maxParams);
        }
        return params;
    }

    private static File checkDir (String dirname) throws ParserException {
        String functionId = CLASS_NAME + ".checkDir: ";

        if (dirname == null || dirname.isBlank()) {
            throw new ParserException(functionId + "Path name is blank");
        }
        
        File myPath = new File(dirname);
        if (!myPath.isDirectory()) {
            throw new ParserException(functionId + "Path not found: " + dirname);
        }
        frame.outputInfoMsg(UIFrame.STATUS_PARSER, "  Path param valid: " + dirname);
        return myPath;
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
        if (bWritable) {
            frame.outputInfoMsg(UIFrame.STATUS_PARSER, "  File exists & is readable and writable: " + fname);
        } else {
            frame.outputInfoMsg(UIFrame.STATUS_PARSER, "  File exists & is readable: " + fname);
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
        System.out.println(" -save      = save current data to spreadsheet file and reload");
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
