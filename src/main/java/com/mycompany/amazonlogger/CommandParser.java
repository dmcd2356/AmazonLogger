package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.AmazonReader.props;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.StringTokenizer;
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
    
    // List of all the command line options and the argument types each takes
    // S = String, L = list, D = dir, F = file, U = unsigned int, I = Int, B = boolean
    //   (lowercase if optional, but must be at end of list)
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
    
    private class OptionList {
        String  optName;        // the option name
        String  argTypes;       // argument types list
        
        OptionList (String opt, String args) {
            optName  = opt;
            argTypes = args;
        }
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
                strCommand += parStc.showParam();
            }
            
            return strCommand;
        }
    } 

    // the key for loops uses both the name and the command index of the FOR
    // statement. this way, loop names can be reused as long as they aren't
    // nested within each other.
    public class LoopId {
        String  name;       // name of the loop
        int     index;      // command index of the start of the loop
        
        LoopId (String name, int index) {
            this.name  = name;
            this.index = index;
        }
    }
    
    // loop stack for keeping track of current nesting of loops as program runs
    private Stack<LoopId> loopStack = new Stack<>();
    private LoopId curLoopId = null;
    
    /**
     * runs the program from command line input
     * 
     * @param args - the list of options to execute (-f to run commands from a file)
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException 
     */
    public void runCommandLine (String[] args) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + ".runCommandLine: ";
        
        frame.outputInfoMsg(STATUS_PROGRAM, "command line entered: " + String.join(" ", args));

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
                frame.outputInfoMsg(STATUS_PROGRAM, "BEGINING PROGRAM COMPILE");
                ArrayList<CommandStruct> cmdList = compileProgram(args[1]);

                // execute the program by running each 'cmdList' entry
                frame.outputInfoMsg(STATUS_PROGRAM, "BEGINING PROGRAM EXECUTION");
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
                
                ArrayList<CommandStruct> commandList = formatCmdOptions (optArgs);
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

    /**
     * compiles the external script file (when -f option used) into a series of
     * CommandStruct entities to execute.
     * 
     * @param fname - the script filename
     * 
     * @return the list of commands to execute
     * 
     * @throws ParserException
     * @throws IOException 
     */
    private ArrayList<CommandStruct> compileProgram (String fname) throws ParserException, IOException {
        String functionId = CLASS_NAME + ".compileProgram: ";

        frame.outputInfoMsg(STATUS_PROGRAM, "Compiling file: " + fname);
        ArrayList<CommandStruct> cmdList = new ArrayList<>();
        String line;

        // open the file to compile and extract the commands from it
        File scriptFile = checkFilename (fname, ".scr", "Script", false);
        FileReader fReader = new FileReader(scriptFile);
        BufferedReader fileReader = new BufferedReader(fReader);

        // clear out the parameter values
        ParameterStruct.initParameters();

        // read the program and compile into ArrayList 'cmdList'
        int lineNum = 0;
        while ((line = fileReader.readLine()) != null) {
            lineNum++;
            line = line.strip();
            if (line.isBlank() || line.charAt(0) == '#') {
                continue;
            }

            String lineInfo = "LINE " + lineNum + ": ";
            int cmdIndex = cmdList.size(); // the command index

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
                String parmList = line.substring(offset).strip();
                parmArr = parmList.split(" ");
                paramCnt = parmArr.length;
            }
            String parms = (parmArr == null) ? "" : " " + String.join(" ", parmArr);
            frame.outputInfoMsg(STATUS_PROGRAM, "PROGIX [" + cmdIndex + "]: " + cmdStruct.command + parms);

            // now let's check for valid command keywords and extract the parameters
            //  into the cmdStruct structure.
            switch (cmdStruct.command) {
                case "SET":
                    String argList = (paramCnt > 2) ? "SL" : "SS";
                    if (ParameterStruct.isIntegerParam(parmArr[0])) {
                        argList = "SI";
                    }
                    cmdStruct.params = packParamList (line, argList);
                    
                    // now get the parameter values so we can do some verification
                    String strParmName = cmdStruct.params.get(0).getStringValue();
                    String strParmVal  = cmdStruct.params.get(1).getStringValue();

                    // make sure we are not using a reserved parameter name
                    try {
                        ParameterStruct.isValidParamName(strParmName);
                    } catch (ParserException exMsg) {
                        throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + exMsg);
                    }

                    // if value is a parameter itself, we can only do run-time check, so just pass it
                    if (strParmVal != null && strParmVal.startsWith("$")) {
                        ParameterStruct.putStringParameter(strParmName, strParmVal);
                    }
                    
                    // for integer type parameters, verify it is an integer value being assigned
                    if (ParameterStruct.isIntegerParam(strParmName)) {
                        Integer intParmVal = cmdStruct.params.get(1).getIntegerValue();
                        if (intParmVal == null) {
                            throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " Integer arg not defined for: " + strParmName);
                        }
                        ParameterStruct.putIntegerParameter(strParmName, intParmVal);
                    } else {
                        ParameterStruct.putStringParameter(strParmName, strParmVal);
                    }
                    break;
                    
                case "CALC":
                    // TODO: store calc result value in intResult
                    break;
                case "INCR":
                    // TODO: 
                    break;
                case "IF":
                    // TODO: 
                    break;
                case "ELSE":
                    // TODO: store line location in labelsMap
                    break;
                case "ELSEIF":
                    // TODO: store line location in labelsMap
                    break;
                case "ENDIF":
                    // TODO: store line location in labelsMap
                    break;
                case "FOR":
                    // read the arguments passed
                    String loopName = parmArr[0];
                    String strStart = parmArr[1];
                    String strEnd   = parmArr[2];
                    String strStep  = parmArr[3];
                    String loopComp = parmArr[4];
                    
                    // create the parameter list for execution (we only need the loop name,
                    //  the rest is saved in the Loop parameter hashmap.
                    packParamEntry (loopName, 'S', cmdStruct.params);
                    
                    // these entries may be Integers or Parameters, so we send them as
                    //  ParameterStruct entries.
                    ParameterStruct loopStart = packParamEntry (strStart,  'I', null);
                    ParameterStruct loopEnd   = packParamEntry (strEnd,    'I', null);
                    ParameterStruct loopStep  = packParamEntry (strStep,   'I', null);
                    
                    // create a new loop ID (name + command index) for the entry and add it
                    // to the list of IDs for the loop parameter name
                    LoopId loopId = new LoopId(loopName, cmdIndex);
                    LoopStruct loopInfo = new LoopStruct (loopName, loopStart, loopEnd, loopStep, loopComp, cmdIndex);
                    ParameterStruct.saveLoopParameter (loopName, loopId, loopInfo);
                    
                    // add entry to the current loop stack
                    loopStack.push(loopId);
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - new FOR Loop level " + loopStack.size() + " parameter " + loopName + " index @ " + cmdIndex);
                    break;
                case "BREAK":
                    cmdStruct.params = packParamList (line, "");
                    // make sure we are in a FOR ... NEXT loop
                    if (loopStack.empty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    break;
                case "CONTINUE":
                    cmdStruct.params = packParamList (line, "");
                    // make sure we are in a FOR ... NEXT loop
                    if (loopStack.empty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    break;
                case "NEXT":
                    cmdStruct.params = packParamList (line, "");
                    // make sure we are in a FOR ... NEXT loop
                    if (loopStack.empty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    break;
                case "ENDFOR":
                    cmdStruct.params = packParamList (line, "");
                    // make sure we are in a FOR ... NEXT loop
                    if (loopStack.empty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    // store line location in labelsMap
                    LoopId curLoop = loopStack.firstElement();
                    ParameterStruct.setLoopEndIndex(cmdList.size(), curLoop);

                    // remove entry from loop stack
                    loopStack.pop();
                    break;
                case "RUN":
                    // verify the option command and its parameters
                    // NOTE: when we place the command in cmdStruct, we remove the RUN label,
                    //       so executeProgramCommand does not need to check for it.
                    ArrayList<String> optCmd = new ArrayList<>(Arrays.asList(parmArr));
                    ArrayList<CommandStruct> runList = formatCmdOptions (optCmd);
                    
                    // if there was more than 1 command on the line, move all but the last here
                    for (int ix = 0; ix < runList.size(); ix++) {
                        cmdStruct = runList.removeFirst();
                        cmdList.add(cmdStruct);
                    }
                    cmdStruct = null; // clear this since we have copied all the commands from here
                    break;

                default:
                    throw new ParserException(functionId + lineInfo + "Unknown command: " + cmdStruct.command);
            }

            // all good, add command to list
            if (cmdStruct != null) {
                cmdList.add(cmdStruct);
            }
        }
        
        if (! loopStack.empty()) {
            throw new ParserException(functionId + "FOR loop not complete for " + loopStack.size() + " entries");
        }

        fileReader.close();
        
        // the last line will be the one to end the program flow
        cmdList.add(new CommandStruct("EXIT"));
        return cmdList;
    }
    
    /**
     * Executes a command from the list of CommandStruct entries created by the compileProgramCommand method.
     * 
     * @param index     - index of current command in the CommandStruct list
     * @param cmdStruct - the command to execute
     * 
     * @return index of next command in the CommandStruct list
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException 
     */
    private int executeProgramCommand (int index, CommandStruct cmdStruct) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + ".executeProgramCommand: ";
        String lineInfo = "PROGIX [" + index + "]: ";
        int newIndex = -1;
        
        String command = cmdStruct.command;
        frame.outputInfoMsg(STATUS_PROGRAM, lineInfo + cmdStruct.showCommand());
        switch (command) {
            case "EXIT":
                return -1; // this will terminate the program
            case "SET":
                verifyParamList(cmdStruct.params, 2); // check for 2 params
                String parmName = cmdStruct.params.get(0).unpackStringValue();

                if (ParameterStruct.isIntegerParam(parmName)) {
                    Integer intValue = cmdStruct.params.get(1).unpackIntegerValue();
                    if (! ParameterStruct.modifyIntegerParameter(parmName, intValue)) {
                        throw new ParserException(functionId + lineInfo + "Integer param not found: " + parmName);
                    }
                } else {
                    String strValue = cmdStruct.params.get(1).unpackStringValue();
                    if (! ParameterStruct.modifyStringParameter(parmName, strValue)) {
                        throw new ParserException(functionId + lineInfo + "String param not found: " + parmName);
                    }
                }
                break;
            case "CALC":
                // TODO: store calc result value in intResult
                break;
            case "INCR":
                // TODO: 
                break;
            case "IF":
                // TODO: 
                break;
            case "ELSE":
                // TODO: store line location in labelsMap
                break;
            case "ELSEIF":
                // TODO: store line location in labelsMap
                break;
            case "ENDIF":
                // TODO: store line location in labelsMap
                break;
            case "FOR":
                verifyParamList(cmdStruct.params, 1);
                String loopName  = cmdStruct.params.get(0).unpackStringValue();
                curLoopId = new LoopId(loopName, index);
                newIndex = ParameterStruct.getLoopNextIndex (command, index, curLoopId);
                    
                // add entry to the current loop stack
                loopStack.push(curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - new FOR Loop level " + loopStack.size() + " parameter " + loopName + " index @ " + index);
                break;
            case "BREAK":
                if (loopStack.empty() || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, index, curLoopId);
                break;
            case "CONTINUE":
                if (loopStack.empty() || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, index, curLoopId);
                break;
            case "NEXT":
                if (loopStack.empty() || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, index, curLoopId);
                break;
            case "ENDFOR":
                if (loopStack.empty() || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                curLoopId = loopStack.pop();
                break;
            case "RUN":
                command = cmdStruct.params.removeFirst().getStringValue();
                // fall through...
            default:
                // convert the list of Strings into a struct of a command and list of args
                CommandStruct cmdOption = new CommandStruct (command);
                OptionList optInfo = null;
                for (OptionList tblEntry : OptionTable) {
                    if (tblEntry.optName.contentEquals(command)) {
                        optInfo = tblEntry;
                        break;
                    }
                }
                if (optInfo == null) {
                    throw new ParserException(functionId + "option is not valid: " + cmdStruct.command);
                }
                String argTypes = optInfo.argTypes;

                // do any parameter conversions of the parameters passed and add
                // the converted values to the command struct.
                verifyParamList(cmdStruct.params, argTypes.length());
                for (int ix = 0; ix < argTypes.length(); ix++) {
                    ParameterStruct argToPass = cmdStruct.params.get(ix);
                    argToPass.convertType(argTypes.charAt(ix));
                    cmdOption.params.add(argToPass);
                }
                
                // now run the command line option command and save any response msg
                String rsp = executeCmdOption (cmdOption);
                if (rsp != null) {
                    ParameterStruct.putResponseValue(rsp);
                }
                break;
        }
        
        // by default, the command will proceed to the next command
        if (newIndex >= 0) {
            index = newIndex;
        } else {
            index++;
        }
        
        return index;
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
     * 
     * @return a list of 1 or more CommandStruct entries of commands to execute
     * 
     * @throws ParserException 
     */
    private ArrayList<CommandStruct> formatCmdOptions (ArrayList<String> argList) throws ParserException {
        String functionId = CLASS_NAME + ".formatCmdOptions: ";

        if (argList == null || argList.isEmpty()) {
            throw new ParserException(functionId + "Null command line");
        }

        ArrayList<CommandStruct> commands = new ArrayList<>(); // array of command lines extracted
        frame.outputInfoMsg(STATUS_PROGRAM, "  splitting command option: " + String.join(" ", argList));
        
        // 1st entry is option, which may have additional args. let's see how many
        String cmdArg = argList.removeFirst();
        CommandStruct newCommand = new CommandStruct(cmdArg);
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
            frame.outputInfoMsg(STATUS_PROGRAM, "  option cmd: " + cmdArg + " (no args)");
        } else {
            frame.outputInfoMsg(STATUS_PROGRAM, "  option cmd: " + cmdArg + " (arglist: " + optInfo.argTypes + ")");
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
                    frame.outputInfoMsg(STATUS_PROGRAM, "  option cmd: " + cmdArg + " (no args)");
                } else {
                    frame.outputInfoMsg(STATUS_PROGRAM, "  option cmd: " + cmdArg + " (arglist: " + optInfo.argTypes + ")");
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

                // verify and format arg values
                ParameterStruct parmData = new ParameterStruct(nextArg, parmType);
                newCommand.params.add(parmData);
                parmCnt += 1;
            }
        }
            
        // add the remaining entry to the list of commands
        commands.add(newCommand);
        frame.outputInfoMsg(STATUS_PROGRAM, commands.size() + " options found");
        return commands;
    }

    /**
     * executes the command line option specified
     * 
     * @param cmdLine - the option command to execute
     * 
     * @return a response String if the command was a query type, else null
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException 
     */
    private String executeCmdOption (CommandStruct cmdLine) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + ".executeCmdOption: ";
        String response = null;
        String filetype;
        String fname;
        String option = cmdLine.command;
        ArrayList<ParameterStruct> params = cmdLine.params;
        frame.outputInfoMsg(STATUS_PROGRAM, "  Executing: " + cmdLine.showCommand());

        // the rest will be the parameters associated with the option (if any) plus any additional options
        try {
            switch (option) {
                case "-d":
                    frame.setMessageFlags(params.get(0).getIntegerValue());
                    break;
                case "-s":
                    filetype = "Spreadsheet";
                    fname = params.get(0).getStringValue();
                    File ssheetFile = checkFilename (fname, ".ods", filetype, true);
                    Spreadsheet.selectSpreadsheet(ssheetFile);
                    break;
                case "-l":
                    Integer numTabs = params.get(0).getIntegerValue();
                    boolean bCheckHeader = params.get(1).getBooleanValue();
                    if (numTabs <= 0) {
                        throw new ParserException(functionId + "Invalid number of tabs to load: " + numTabs);
                    }
                    Spreadsheet.loadSheets(numTabs, bCheckHeader);
                    break;
                case "-t":
                    Integer tab = params.get(0).getIntegerValue();
                    Spreadsheet.selectSpreadsheetTab (tab.toString());
                    break;
                case "-c":
                    filetype = "Clipboard";
                    fname = params.get(0).getStringValue();
                    File fClip = checkFilename (fname, ".txt", filetype, false);
                    frame.outputInfoMsg(STATUS_PROGRAM, "  " + filetype + " file: " + fClip.getAbsolutePath());
                    AmazonParser amazonParser = new AmazonParser(fClip);
                    amazonParser.parseWebData();
                    break;
                case "-u":
                    frame.outputInfoMsg(STATUS_PROGRAM, "  Updating spreadsheet from clipboards");
                    AmazonParser.updateSpreadsheet();
                    break;
                case "-p":
                    filetype = "PDF";
                    fname = params.get(0).getStringValue();
                    File pdfFile = checkFilename (fname, ".pdf", filetype, false);
                    frame.outputInfoMsg(STATUS_PROGRAM, "  filetype + \" file: \" + pdfFile.getAbsolutePath()");
                    PdfReader pdfReader = new PdfReader();
                    pdfReader.readPdfContents(pdfFile);
                    break;
                case "-o":
                    if (params.isEmpty()) {
                        frame.outputInfoMsg(STATUS_PROGRAM, "  Output messages to stdout");
                        frame.setTestOutputFile(null);
                    } else {
                        fname = params.get(0).getStringValue();
                        fname = Utils.getTestPath() + "/" + fname;
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
                    response = convDate;
                    break;
                case "-datep":
                    strDate = params.get(0).getStringValue();
                    date = DateFormat.getFormattedDate (strDate, true);
                    convDate = DateFormat.convertDateToString(date, true);
                    if (convDate == null) {
                        throw new ParserException(functionId + "Invalid date conversion");
                    }
                    response = convDate;
                    break;
                case "-default":
                    numTabs = params.get(0).getIntegerValue();
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
                    response = "" + iCol;
                    break;
                case "-maxrow":
                    Integer iRow = Spreadsheet.getSpreadsheetRowSize ();
                    response = "" + iRow;
                    break;
                case "-setsize":
                    iCol = params.get(0).getIntegerValue();
                    iRow = params.get(1).getIntegerValue();
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    Spreadsheet.setSpreadsheetSize (iCol, iRow);
                    break;
                case "-find":
                    String order = params.get(0).getStringValue();
                    iRow = Spreadsheet.findItemNumber(order);
                    response = "" + iRow;
                    break;
                case "-class":
                    iCol = params.get(0).getIntegerValue();
                    iRow = params.get(1).getIntegerValue();
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    String strValue = Spreadsheet.getSpreadsheetCellClass(iCol, iRow);
                    response = strValue;
                    break;
                case "-color":
                    iCol = params.get(0).getIntegerValue();
                    iRow = params.get(1).getIntegerValue();
                    Integer iColor = params.get(2).getIntegerValue();
                    if (iCol == null || iRow == null || iColor == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow + ", color = " + iColor);
                    }
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColorOfTheMonth(iColor));
                    break;
                case "-RGB":
                    iCol = params.get(0).getIntegerValue();
                    iRow = params.get(1).getIntegerValue();
                    Integer iRGB = params.get(2).getIntegerValue();
                    if (iCol == null || iRow == null || iRGB == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow + ", RGB = " + iRGB);
                    }
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColor("RGB", iRGB));
                    break;
                case "-HSB":
                    iCol = params.get(0).getIntegerValue();
                    iRow = params.get(1).getIntegerValue();
                    Integer iHSB = params.get(2).getIntegerValue();
                    if (iCol == null || iRow == null || iHSB == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow + ", HSB = " + iHSB);
                    }
                    Spreadsheet.setSpreadsheetCellColor(iCol, iRow, Utils.getColor("HSB", iHSB));
                    break;
                case "-cellget":
                    iCol = params.get(0).getIntegerValue();
                    iRow = params.get(1).getIntegerValue();
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    String cellValue = Spreadsheet.getSpreadsheetCell(iCol, iRow);
                    response = cellValue;
                    break;
                case "-cellclr":
                    iCol = params.get(0).getIntegerValue();
                    iRow = params.get(1).getIntegerValue();
                    if (iCol == null || iRow == null) {
                        throw new ParserException(functionId + "Invalid values: col = " + iCol + ", row = " + iRow);
                    }
                    cellValue = Spreadsheet.putSpreadsheetCell(iCol, iRow, null);
                    response = cellValue;
                    break;
                case "-cellput":
                    iCol = params.get(0).getIntegerValue();
                    iRow = params.get(1).getIntegerValue();
                    String strText = params.get(2).getStringValue();
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

    /**
     * This adds an entry to the parameter list.
     *  NOTE: it allows for $ parameter substitution, but does not do any conversion here.
     * 
     * @param entry     - the command line along with all of the arguments it contains
     * @param paramType - the list of data types for each parameter it has
     * @param paramList - the parameter list to add entry to
     * 
     * @return the ArrayList of arguments for the command
     * 
     * @throws ParserException 
     */
    private ParameterStruct packParamEntry (String entry, char paramType, ArrayList<ParameterStruct> paramList) throws ParserException {
        paramType = Character.toUpperCase(paramType); // make sure it is in uppercase
        ParameterStruct parmStc = new ParameterStruct (entry, paramType);
        if (paramList != null) {
            int index = paramList.size();
            paramList.add(parmStc);
            frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + index + "]: type " + paramType + " value: " + entry);
        }
        return parmStc;
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
    private ArrayList<ParameterStruct> packParamList (String line, String argTypes) throws ParserException {
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
        char lastParam = ' ';
        if (! argTypes.isEmpty())
            lastParam = Character.toUpperCase(argTypes.charAt(maxParams-1));
        
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

        String command = line.substring(0, offset).strip();
        line = line.substring(offset).strip();
        int wordCount = new StringTokenizer(line).countTokens();

        // make sure we have the correct number of parameters
        if (wordCount > maxParams && lastParam != 'L') {
            throw new ParserException(functionId + "Args list for option "
                        + command + " exceeded max allowed: " + wordCount + " (max " + maxParams + ")");
        } else if (wordCount < minParams) {
            throw new ParserException(functionId + "Args list for option "
                        + command + " less than min allowed: " + wordCount + " (min " + minParams + ")");
        }
        
        for (int ix = 0; ix < maxParams && ! line.isEmpty(); ix++) {
            // get the next parameter type
            char parmType = argTypes.charAt(ix);
            parmType = Character.toUpperCase(parmType); // make sure it is in uppercase

            // get the next entry in the string (L(ist) type takes the remainder of the string
            String nextArg;
            if (parmType == 'L') {
                // take the remaining entries for a L(ist) parameter type
                nextArg = line;
                line = "";
            } else {
                // else, just get the next word and remove it from the param list
                nextArg = getNextWord (line);
                line = line.substring(nextArg.length()).strip();
            }

            // create the parameter entry and add it to the parameter list
            packParamEntry (nextArg, parmType, params);
        }

        return params;
    }

    /**
     * verifies the parameter list is valid size
     * 
     * @param params   - the parameter list
     * @param paramCnt - the number of params defined for the command
     * 
     * @throws ParserException 
     */
    private void verifyParamList (ArrayList <ParameterStruct> params, int paramCnt) throws ParserException {
        String functionId = CLASS_NAME + ".verifyParamList: ";
       
        if (params == null || (params.isEmpty() && paramCnt > 0)) {
            throw new ParserException(functionId + "Null or empty param list");
        }
        if (paramCnt < 0 || paramCnt > params.size()) {
            throw new ParserException(functionId + "Missing parameters in list: required " + paramCnt + ", only found " + params.size());
        }
    }
    
    /**
     * get the next word in a string of words.
     * 
     * @param line - the string containing 0 or more words
     * 
     * @return the next word in the line (empty string if no more words)
     */
    private static String getNextWord (String line) {
        line = line.strip();
        if (line.isBlank()) {
            return "";
        }
        int offset = line.indexOf(" ");
        if (offset <= 0) {
            return line;
        }
        return line.substring(0, offset).strip();
    }
    
    /**
     * test if a file name is valid
     * 
     * @param fname     - name of the file (referenced from base path)
     * @param type      - the file extension allowed (null of blank if don't check)
     * @param filetype  - name of file type (only used for debug msgs & can be null)
     * @param bWritable - true if check if file is writable (else only check if readable)
     * 
     * @return the File
     * 
     * @throws ParserException 
     */
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
        
        fname = Utils.getTestPath() + "/" + fname;
        File myFile = new File(fname);
        if (!myFile.canRead()) {
            throw new ParserException(functionId + "Invalid " + filetype + " file - no read access: " + fname);
        }
        if (bWritable && !myFile.canWrite()) {
            throw new ParserException(functionId + "Invalid " + filetype + " file - no write access: " + fname);
        }
        if (bWritable) {
            frame.outputInfoMsg(UIFrame.STATUS_PROGRAM, "  File exists & is readable and writable: " + fname);
        } else {
            frame.outputInfoMsg(UIFrame.STATUS_PROGRAM, "  File exists & is readable: " + fname);
        }
        return myFile;
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
        System.out.println("     x01 =   1 = STATUS_NORMAL");
        System.out.println("     x02 =   2 = STATUS_PARSER");
        System.out.println("     x04 =   4 = STATUS_SPREADSHEET");
        System.out.println("     x08 =   8 = STATUS_INFO");
        System.out.println("     x10 =  16 = STATUS_PROPS");
        System.out.println("     x20 =  32 = STATUS_PROGRAM");
        System.out.println("     x80 = 128 = STATUS_DEBUG");
        System.out.println("     e.g. -d xFF will enable all msgs");
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
