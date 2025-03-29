/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_ERROR;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 *
 * @author dan
 */
public class ScriptParser {
    
    ScriptParser() {
        // create an instance of the command options parser for any RUN commands
        cmdOptionParser = new CmdOptions();
    }
    
    private static final String CLASS_NAME = "ScriptParser";
    
    private static final String [] CommandTable = {
        "EXIT",
        "DEFINE",
        "SET",
        "IF",
        "ELSE",
        "ELSEIF",
        "ENDIF",
        "FOR",
        "BREAK",
        "CONTINUE",
        "NEXT",
        "ENDLOOP",
        "RUN",
    };
            
    // IF List is built during Compile phase and referenced during Execution phase.
    // IF Stack is used during Compile and Execution phases. Compile time for
    //   verification, and Execution for running the branches.
    private final ArrayList<IFStruct> ifList  = new ArrayList<>();
    private final Stack<Integer>      ifStack = new Stack<>();

    // identifies the current loopStack entry.
    private LoopId curLoopId = null;
    
    // this handles the command line options via the RUN command
    private final CmdOptions cmdOptionParser;
    

    private IFStruct getIfEntry (int cmdIndex) throws ParserException {
        String functionId = CLASS_NAME + ".getIfEntry: ";
        
        for (int ix = 0; ix < ifList.size(); ix++) {
            if (ifList.get(ix).ixIf == cmdIndex) {
                return ifList.get(ix);
            }
        }
        throw new ParserException(functionId + "IF stack index " + cmdIndex + " not found in IF list");
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
     * checks if a string is one of the reserved command values
     * 
     * @param strValue - the string to check
     * 
     * @return true if it is a reserved command value
     */
    public static boolean isValidCommand (String strValue) {
        for (int ix = 0; ix < CommandTable.length; ix++) {
            if (strValue.contentEquals(CommandTable[ix])) {
                return true;
            }
        }
        return false;
    }
    
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
    public void runFromFile (String[] args) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + ".runCommandLine: ";
        
        frame.outputInfoMsg(STATUS_PROGRAM, "command line entered: " + String.join(" ", args));

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
    }

    private static String getParamTypes (CommandStruct command) {
        String paramTypes = "";
        for (int ix = 0; ix < command.params.size(); ix++) {
            paramTypes += command.params.get(ix).getParamType();
        }
        
        return paramTypes;
    }
    
    private static String checkParamTypes (CommandStruct command, String validTypes, int linenum) throws ParserException {
        String functionId = CLASS_NAME + ".checkParamTypes: ";
        String prefix = functionId + "line " + linenum + ", " + command + " - ";
        
        // determine the min and max number of parameters
        int min = 0;
        int max = validTypes.length();
        for (int ix = 0; ix < max; ix++) {
            if (validTypes.charAt(ix) >= 'A' && validTypes.charAt(ix) <= 'Z') {
                min++;
            } else {
                break;
            }
        }
        
        // verify we have the correct number of parameters
        if (command.params.size() < min || command.params.size() > max) {
            throw new ParserException(prefix + "Invalid number of parameters: " + command.params.size() + " (range " + min + " to " + max + ")");
        }
        
        // now verify the types
        String paramTypes = "";
        for (int ix = 0; ix < command.params.size(); ix++) {
            char type = command.params.get(ix).getParamType();
            paramTypes += type;
            switch (Character.toUpperCase(validTypes.charAt(ix))) {
                case 'I': if (type == 'I' || type == 'U') continue;
                    break;
                case 'U': if (type == 'I' || type == 'U') continue;
                    break;
                case 'B': if (type == 'B') continue;
                    break;
                case 'S': if (type != 'A') continue;
                    break;
                case 'A': if (type == 'A' || type == 'I' || type == 'U') continue;
                    break;
                case 'L':
                    continue; // allow anything
                default:
                    break;
            }
            throw new ParserException(prefix + "Invalid param[" + ix + "] type '" + validTypes.charAt(ix));
        }
        
        return paramTypes;
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
        int cmdIndex = 0;

        // open the file to compile and extract the commands from it
        File scriptFile = Utils.checkFilename (fname, ".scr", "Script", false);
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
            cmdIndex = cmdList.size(); // the command index

            // first, extract the 1st word as the command keyword
            String command = line;
            String parmString = "";
            int offset = command.indexOf(" ");
            if (offset > 0) {
                command = command.substring(0, offset).strip();
                parmString = line.substring(offset).strip();
            }
            
            // check for parameter names in the case of an assignment statement
            ParamExtract parmInfo = new ParamExtract(line);
            if (parmInfo.name != null && parmInfo.delimiter == '=' && parmInfo.remainder != null) {
                command = "SET";
                parmString = parmInfo.name + " " + parmInfo.remainder;
            } else if (line.startsWith("-")) {
                // if the optional RUN command was omitted from an option command, let's add it here
                String argTypes = cmdOptionParser.getOptionParams(command);
                if (argTypes == null) {
                    throw new ParserException(functionId + "option is not valid: " + command);
                }
                command = "RUN";
                parmString = line;
            }

            // 'parmString' is a string containing the parameters following the command
            // 'cmdStruct' will receive the command, with the params yet to be placed.
            CommandStruct cmdStruct = new CommandStruct(command, lineNum);
            ArrayList<String> listParms;
            
            // extract the parameters to pass to the command
            frame.outputInfoMsg(STATUS_PROGRAM, "PROGIX [" + cmdIndex + "]: " + cmdStruct.command + " " + parmString);
            cmdStruct.params = packParameters (parmString);
            String parmTypeList = getParamTypes (cmdStruct);
            frame.outputInfoMsg(STATUS_PROGRAM, "     dataTypes: " + parmTypeList);

            // now let's check for valid command keywords and extract the parameters
            //  into the cmdStruct structure.
            switch (cmdStruct.command) {
                case "DEFINE":
                    // must be either a String or a List of parameter name entries
                    checkParamTypes(cmdStruct, "L", cmdIndex);

                    // this defines the parameter names, and must be done prior to their use.
                    // This Compile method will allocate them, so the Execute does not need
                    //  to do anything with this command.
                    // Multiple parameters can be defined on one line, with the parameter names
                    //  comma separated.
                    ParameterStruct list = cmdStruct.params.getFirst();
                    for (int ix = 0; ix < list.getListSize(); ix++) {
                        String pName = list.getListElement(ix);
                        try {
                            // allocate the parameter
                            ParameterStruct.allocateParameter(pName);
                        } catch (ParserException exMsg) {
                            throw new ParserException(exMsg + "\n -> " + functionId + lineInfo + "command " + cmdStruct.command);
                        }
                    }
                    cmdStruct = null; // don't bother to run the command in execution phase
                    break;
                case "SET":
                    // TODO: for now, we are only accepting a value or a parameter and are not doing any calculations.
                    // (the first arg is the parameter name)
                    if (parmTypeList.length() != 2) {
                        throw new ParserException(functionId + lineInfo + "Only 1 value allowed for parameter assignment for now: " + parmString);
                    }
                    String strParmName = cmdStruct.params.get(0).getStringValue();
                    String strParmVal  = cmdStruct.params.get(1).getStringValue();
                    
                    // check for valid data type assignment
                    boolean bValid = false;
                    char parmType = parmTypeList.charAt(1);
                    switch (parmInfo.type) {
                        case 'I':
                            bValid = (parmType == 'I' || parmType == 'U' || parmType == 'S');
                            break;
                        case 'B':
                            bValid = (parmType == 'B' || parmType == 'I' || parmType == 'U' || parmType == 'S');
                            break;
                        case 'S':
                            bValid = (parmType != 'A');
                            break;
                        case 'A':
                            bValid = (parmType == 'A' || parmType == 'I' || parmType == 'U');
                            break;
                        case 'L':
                            bValid = (parmType == 'L' || parmType == 'S');
                            break;
                        default:
                            break;
                    }
                    if (!bValid) {
                        throw new ParserException(functionId + lineInfo + "Invalid assignment type for " + strParmName + ": " + strParmVal);
                    }
                    break;
                case "IF":
                    // verify number and type of arguments
                    checkParamTypes(cmdStruct, "ISI", cmdIndex);

                    // read the arguments passed
                    // assumed format is: IF Name1 >= Name2  (where Names can be Integers, Strings or Parameters)
                    String ifName = cmdStruct.params.get(0).getStringValue();

                    // if not first IF statement, make sure previous IF had an ENDIF
                    IFStruct ifInfo;
                    if (!ifList.isEmpty() && !ifStack.empty()) {
                        ifInfo = getIfEntry(ifStack.peek());
                        if (!ifInfo.isValid()) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when previous IF has no matching ENDIF");
                        }
                    }
                    
                    // add entry to the current loop stack
                    ifInfo = new IFStruct (cmdIndex, LoopStruct.getStackSize());
                    ifList.add(ifInfo);
                    ifStack.push(cmdIndex);
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - new IF level " + ifStack.size() + " parameter " + ifName);
                    break;
                case "ELSE":
                    if (ifList.isEmpty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                    }
                    // save the current command index in the current if structure
                    ifInfo = getIfEntry(ifStack.peek());
                    ifInfo.setElseIndex(cmdIndex, false, LoopStruct.getStackSize());
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + ifStack.size() + " " + cmdStruct.command + " on line " + cmdIndex);
                    break;
                case "ELSEIF":
                    if (ifStack.empty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                    }
                    
                    // read the arguments passed
                    // assumed format is: IF Name1 >= Name2  (where Names can be Integers, Strings or Parameters)
                    ifName = cmdStruct.params.get(0).getStringValue();
                    
                    // save the current command index in the current if structure
                    ifInfo = getIfEntry(ifStack.peek());
                    ifInfo.setElseIndex(cmdIndex, true, LoopStruct.getStackSize());
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + ifStack.size() + " " + cmdStruct.command + " on line " + cmdIndex + " parameter " + ifName);
                    break;
                case "ENDIF":
                    if (ifStack.empty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                    }
                    // save the current command index in the current if structure
                    ifInfo = getIfEntry(ifStack.peek());
                    ifInfo.setEndIfIndex(cmdIndex, LoopStruct.getStackSize());
                    ifStack.pop();
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + ifStack.size() + " " + cmdStruct.command + " on line " + cmdIndex);
                    break;
                case "FOR":
                    // read the arguments passed
                    // assumed format is: FOR Name = StartIx ; < EndIx ; IncrVal
                    // (and trailing "; IncrVal" is optional)
                    listParms = extractUserParams ("S=I;CI;I", parmString);
                    if (listParms.size() < 4) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " missing parameters");
                    } else if (listParms.size() < 5) {
                        listParms.add("1"); // use 1 as a default value
                        frame.outputInfoMsg(STATUS_PROGRAM, "    (using default step size of 1)");
                    }

                    // get the parameters and format them for use
                    String loopStart, loopEnd, loopStep;
                    String loopName, loopComp;
                    loopName  = listParms.get(0);
                    loopStart = listParms.get(1);
                    loopComp  = listParms.get(2);
                    loopEnd   = listParms.get(3);
                    loopStep  = listParms.get(4);

                    // create a new loop ID (name + command index) for the entry and add it
                    // to the list of IDs for the loop parameter name
                    LoopId loopId = new LoopId(loopName, cmdIndex);
                    LoopStruct loopInfo;
                    try {
                        loopInfo = new LoopStruct (loopName, loopStart, loopEnd, loopStep, loopComp, cmdIndex, ifStack.size());
                    } catch (ParserException exMsg) {
                        throw new ParserException(exMsg + "\n -> " + functionId + lineInfo + "command " + cmdStruct.command);
                    }
                    ParameterStruct.saveLoopParameter (loopName, loopId, loopInfo);
                    
                    // add entry to the current loop stack
                    LoopStruct.pushStack(loopId);
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - new FOR Loop level " + LoopStruct.getStackSize() + " parameter " + loopName + " index @ " + cmdIndex);
                    break;
                case "BREAK":
                    // make sure we are in a FOR ... NEXT loop
                    if (LoopStruct.getStackSize() == 0) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    // verify the IF loop level hasn't been exceeded
                    LoopId curLoop = LoopStruct.peekStack();
                    ParameterStruct.checkLoopIfLevel (cmdStruct.command, ifStack.size(), curLoop);
                    break;
                case "CONTINUE":
                    // make sure we are in a FOR ... NEXT loop
                    if (LoopStruct.getStackSize() == 0) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    // verify the IF loop level hasn't been exceeded
                    curLoop = LoopStruct.peekStack();
                    ParameterStruct.checkLoopIfLevel (cmdStruct.command, ifStack.size(), curLoop);
                    break;
                case "NEXT":
                    // make sure we are in a FOR ... NEXT loop
                    if (LoopStruct.getStackSize() == 0) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    // verify the IF loop level hasn't been exceeded
                    curLoop = LoopStruct.peekStack();
                    ParameterStruct.checkLoopIfLevel (cmdStruct.command, ifStack.size(), curLoop);
                    break;
                case "ENDFOR":
                    // make sure we are in a FOR ... NEXT loop
                    if (LoopStruct.getStackSize() == 0) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    // store line location in labelsMap
                    curLoop = LoopStruct.peekStack();
                    ParameterStruct.setLoopEndIndex(cmdList.size(), curLoop);

                    // remove entry from loop stack
                    LoopStruct.popStack();
                    break;
                case "RUN":
                    // verify the option command and its parameters
                    // NOTE: when we place the command in cmdStruct, we remove the RUN label,
                    //       so executeProgramCommand does not need to check for it.
                    ArrayList<String> optCmd = new ArrayList<>(Arrays.asList(parmString.split(" ")));
                    ArrayList<CommandStruct> runList = cmdOptionParser.formatCmdOptions (optCmd, lineNum);
                    
                    // append all option commands on the line to the command list, 1 option per command line
                    while (! runList.isEmpty()) {
                        cmdList.add(runList.removeFirst());
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
        
        int loopSize = LoopStruct.getStackSize();
        if (loopSize != 0) {
            throw new ParserException(functionId + "FOR loop not complete for " + loopSize + " entries");
        }
        if (!ifStack.isEmpty() && !getIfEntry(ifStack.peek()).isValid()) {
            throw new ParserException(functionId + "Last IF has no matching ENDIF");
        }

        fileReader.close();
        
        // the last line will be the one to end the program flow
        cmdList.add(new CommandStruct("EXIT", lineNum));
        frame.outputInfoMsg(STATUS_PROGRAM, "PROGIX [" + cmdIndex + "]: EXIT  (appended)");
        return cmdList;
    }

    /**
     * Executes a command from the list of CommandStruct entries created by the compileProgramCommand method.
     * 
     * @param cmdIndex  - index of current command in the CommandStruct list
     * @param cmdStruct - the command to execute
     * 
     * @return index of next command in the CommandStruct list
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException 
     */
    private int executeProgramCommand (int cmdIndex, CommandStruct cmdStruct) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + ".executeProgramCommand: " + showLineNumberInfo(cmdStruct.line);
        String lineInfo = "PROGIX [" + cmdIndex + "]: " + showLineNumberInfo(cmdStruct.line);
        int newIndex = -1;
        
        // replace all program references in the command to their corresponding values.
        for (int ix = 0; ix < cmdStruct.params.size(); ix++) {
            ParameterStruct param = cmdStruct.params.get(ix);
            param.updateFromReference();
        }

        String command = cmdStruct.command;
        frame.outputInfoMsg(STATUS_PROGRAM, lineInfo + cmdStruct.showCommand());

        try {
        switch (command) {
            case "EXIT":
                return -1; // this will terminate the program
            case "DEFINE":
                break;
            case "SET":
                String parmName = cmdStruct.params.get(0).getStringValue();
                ParameterStruct parmValue = cmdStruct.params.get(1);
                if (parmValue.getParamType() == 'I' || parmValue.getParamType() == 'U') {
                    ParameterStruct.modifyIntegerParameter(parmName, parmValue.getIntegerValue());
                } else {
                    ParameterStruct.modifyStringParameter(parmName, parmValue.getStringValue());
                }
                break;
            case "IF":
                ParameterStruct parm1 = cmdStruct.params.get(0);
                String comp           = cmdStruct.params.get(1).getStringValue();
                ParameterStruct parm2 = cmdStruct.params.get(2);

                // add entry to the current loop stack
                ifStack.push(cmdIndex);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - new IF level " + ifStack.size() + " " + parm1.getStringValue() + " " + comp + " " + parm2.getStringValue());

                // check status to see if true of false.
                boolean bBranch;
                if ((parm1.getParamType() == 'I' || parm1.getParamType() == 'U') &&
                    (parm2.getParamType() == 'I' || parm2.getParamType() == 'U')    ) {
                    bBranch = Utils.compareParameterValues (parm1.getIntegerValue(), parm2.getIntegerValue(), comp);
                } else {
                    bBranch = Utils.compareParameterValues (parm1.getStringValue(), parm2.getStringValue(), comp);
                }
                IFStruct ifInfo = getIfEntry(cmdIndex);
                if (bBranch) {
                    newIndex = ifInfo.getElseIndex(cmdIndex);
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - goto next IF case @ " + newIndex);
                } else {
                    ifInfo.setConditionMet(); // we are running the condition, so ELSEs will be skipped
                }
                break;
            case "ELSE":
                if (ifStack.empty()) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a IF structure");
                }

                // if the IF condition has already been met, jump to the ENDIF statement
                ifInfo = getIfEntry(ifStack.peek());
                if (ifInfo.isConditionMet()) {
                    newIndex = ifInfo.getEndIndex();
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - goto ENDIF @ " + newIndex);
                } else {
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + ifStack.size() + " " + cmdStruct.command + " on line " + cmdIndex);
                }
                break;
            case "ELSEIF":
                if (ifStack.empty()) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a IF structure");
                }

                // if the IF condition has already been met, jump to the ENDIF statement
                ifInfo = getIfEntry(ifStack.peek());
                if (ifInfo.isConditionMet()) {
                    newIndex = ifInfo.getEndIndex();
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - goto ENDIF @ " + newIndex);
                } else {
                    parm1 = cmdStruct.params.get(0);
                    comp  = cmdStruct.params.get(1).getStringValue();
                    parm2 = cmdStruct.params.get(2);
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + ifStack.size() + ": " + parm1.getStringValue() + " " + comp + " " + parm2.getStringValue());

                    // check status to see if true of false.
                    if ((parm1.getParamType() == 'I' || parm1.getParamType() == 'U') &&
                        (parm2.getParamType() == 'I' || parm2.getParamType() == 'U')    ) {
                        bBranch = Utils.compareParameterValues (parm1.getIntegerValue(), parm2.getIntegerValue(), comp);
                    } else {
                        bBranch = Utils.compareParameterValues (parm1.getStringValue(), parm2.getStringValue(), comp);
                    }
                    if (bBranch) {
                        newIndex = ifInfo.getElseIndex(cmdIndex);
                        frame.outputInfoMsg(STATUS_PROGRAM, "   - goto next IF case @ " + newIndex);
                    } else {
                        ifInfo.setConditionMet(); // we are running the condition, so ELSEs will be skipped
                    }
                }
                break;
            case "ENDIF":
                if (ifStack.empty()) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a IF structure");
                }
                // save the current command index in the current if structure
                ifStack.pop();
                frame.outputInfoMsg(STATUS_PROGRAM, "   - new IF level " + ifStack.size() + ": " + cmdStruct.command + " on line " + cmdIndex);
                break;
            case "FOR":
                String loopName  = cmdStruct.params.get(0).getStringValue();
                curLoopId = new LoopId(loopName, cmdIndex);
                newIndex = ParameterStruct.getLoopNextIndex (command, cmdIndex, curLoopId);
                    
                // add entry to the current loop stack
                LoopStruct.pushStack(curLoopId);
                int loopSize = LoopStruct.getStackSize();
                frame.outputInfoMsg(STATUS_PROGRAM, "   - new FOR Loop level " + loopSize+ " parameter " + loopName + " index @ " + cmdIndex);
                break;
            case "BREAK":
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - " + command + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                break;
            case "CONTINUE":
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - " + command + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                break;
            case "NEXT":
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - " + command + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                break;
            case "ENDFOR":
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "   - " + command + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                curLoopId = LoopStruct.popStack();
                loopSize = LoopStruct.getStackSize();
                if (curLoopId == null) {
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - All loops completed so far");
                } else {
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - current Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                }
                break;
            case "RUN":
                // remove the RUN tag from the command line
                command = cmdStruct.params.removeFirst().getStringValue();
                // fall through...
            default:
                cmdOptionParser.runCmdOption (cmdStruct);
                break;
        }
        } catch (ParserException exMsg) {
            frame.outputInfoMsg(STATUS_ERROR, functionId + "command: " + command + "\n  -> " + exMsg);
            throw new ParserException();
        }
        
        // by default, the command will proceed to the next command
        if (newIndex >= 0) {
            cmdIndex = newIndex;
        } else {
            cmdIndex++;
        }
        
        return cmdIndex;
    }

    /**
     * extracts the number of chars read from a command line that match the type of 
     * parameter we are looking for.
     * 
     * @param type - the type of parameter to look for
     * @param line - the command line that starts with the next entry to parse
     * 
     * @return the number of chars found in the string that match the param type
     * 
     * @throws ParserException 
     */
    private int getValidStringLen (char type, String line) throws ParserException {
        String functionId = CLASS_NAME + ".getValidStringLen: ";

        boolean bParam = false;
        for (int ix = 0; ix < line.length(); ix++) {
            char curChar = line.charAt(ix);
            frame.outputInfoMsg(STATUS_DEBUG, functionId + "char type " + type + ": char '" + curChar + "'");
            switch (type) {
                case 'S':
                    // String or Parameter
                    if (ix == 0 && curChar == '$') {
                        bParam = true;
                        continue;
                    }
                    if (Character.isLetterOrDigit(curChar) || curChar == '_') {
                        continue;
                    }
                    return ix;
                case 'I':
                    // Integer or Parameter
                    if (ix == 0 && curChar == '$') {
                        bParam = true;
                        continue;
                    }
                    if (Character.isDigit(curChar)) {
                        continue;
                    }
                    if (bParam) {
                        if (Character.isLetterOrDigit(curChar) || curChar == '_') {
                            continue;
                        }
                    }
                    return ix;
                case 'X':
                    // Hexadecimal
                    curChar = Character.toUpperCase(curChar);
                    if (Character.isDigit(curChar) || (curChar >= 'A' && curChar <= 'F')) {
                        continue;
                    }
                    return ix;
                case 'C':
                    // Comparison sign
                    if (curChar == '=' || curChar == '!' || curChar == '>' || curChar == '<') {
                        continue;
                    }
                    return ix;
                default:
                    if (curChar == type) {
                        return ix + 1;
                    }
                    throw new ParserException (functionId + "Invalid char for type " + type + ": " + curChar);
            }
        }
        return line.length();
    }
    
    /**
     * extracts the specified list of parameter types from the string passed.
     * The 'parmArr' value should be the String containing the param list for the command
     * and the 'strType' contains a representation of the parameters and format that
     * we expect. NOTE: this does not verify all params have been read, so that
     * trailing params can be optional, so you have to chack for the number of
     * params returned.
     * 
     * parameter types:
     * S is a String or String parameter
     * I is an Integer or String/Integer parameter
     * X is a Hexadecimal value
     * C is a comparison string (==, !=, >, >=, <, <=)
     * other entries are taken as separator chars, such as ; , = etc.
     * 
     * @param strType - the format string for the param list
     * @param parmArr - the string of params to parse
     * 
     * @return an ArrayList of Strings containing the parameters
     * 
     * @throws ParserException 
     */
    private ArrayList<String> extractUserParams (String strType, String parmArr) throws ParserException {
        String functionId = CLASS_NAME + ".extractUserParams: ";

        ArrayList<String> parmList = new ArrayList<>();
        for (int parmIx = 0; parmIx < strType.length() && !parmArr.isBlank(); parmIx++) {
            frame.outputInfoMsg(STATUS_DEBUG, functionId + parmArr);

            // get the next entry type to read
            char curType = strType.charAt(parmIx);
            
            // remove any leading spaces and extract the next parameter type
            parmArr = parmArr.strip();
            int offset = getValidStringLen (curType, parmArr);
            if (offset <= 0) {
                throw new ParserException (functionId + "Missing parameter for param [" + parmList.size() + "] type " + curType);
            }
            
            // get the parameter we are searching for and remove it from the input list
            String param = parmArr.substring(0, offset);
            parmArr = parmArr.substring(offset);
            frame.outputInfoMsg(STATUS_DEBUG, "    offset = " + offset);

            switch (curType) {
                case 'S':
                case 'I':
                case 'X':
                case 'C':
                    frame.outputInfoMsg(STATUS_DEBUG, "    extracted param[" + parmList.size() + "]: '" + param + "'");
                    parmList.add(param);
                    break;
                default:
                    frame.outputInfoMsg(STATUS_DEBUG, "    extracted character: '" + curType + "'");
                    break;
            }
        }
        return parmList;
    }
    
    /**
     * This takes a command line and extracts the parameter list from it.
     * This simply seperates the String of arguments into seperate parameter
     *   values based on where it finds commas and quotes.
     * 
     * @param line - the string of parameters to seperate and classify
     * 
     * @return the ArrayList of arguments for the command
     * 
     * @throws ParserException 
     */
    private ArrayList<ParameterStruct> packParameters (String line) throws ParserException {
        ArrayList<ParameterStruct> params = new ArrayList<>();
        ParameterStruct parm;

        for (int ix = 0; ! line.isEmpty(); ix++) {
            // read next entry
            char paramType = 'S';
            String nextArg = getNextWord (line);
            line = line.substring(nextArg.length());
            
            // determine if we have a series of Strings or Integers
            if (nextArg.charAt(0) == '{') {
                // check if matching brace
                line = line.strip();
                int offset = line.indexOf('}');
                if (offset >= 0) {
                    // matching brace found...
                    // remove the begining brace and copy the characters up to
                    // the end brace into the arg parameter
                    nextArg = nextArg.substring(1);
                    nextArg += line.substring(0, offset-1);

                    // now remove the rest of the list from the line
                    if (offset == line.length() - 1) {
                        line = "";
                    } else {
                        line = line.substring(offset + 1);
                    }
                    
                    // place them in the proper list structure
                    ArrayList<String> list = new ArrayList<>(Arrays.asList(nextArg.split(",")));
                    paramType = 'L';
                    parm = new ParameterStruct(list);
                    frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + paramType + " value: [ " + nextArg + " ]");
                    params.add(parm);
                    continue;
                }
            }
            
            // determine if we have a quoted String
            if (nextArg.charAt(0) == '"') {
                // check if matching quote
                int offset = line.indexOf('"');
                if (offset >= 0) {
                    // matching quote found...
                    // remove the begining quote and copy the characters up to
                    // the quote into the arg parameter
                    nextArg = nextArg.substring(1);
                    nextArg += line.substring(0, offset);

                    // now remove the rest of the quoted string from the line
                    if (offset == line.length() - 1) {
                        line = "";
                    } else {
                        line = line.substring(offset + 1);
                    }
                }
            }
            line = line.strip();
            
            // determine the data type
            if (paramType == 'S') {
                if (nextArg.equalsIgnoreCase("TRUE") ||
                    nextArg.equalsIgnoreCase("FALSE")) {
                    paramType = 'B';
                } else {
                    try {
                        Long longVal = ParameterStruct.getLongOrUnsignedValue(nextArg);
                        if (ParameterStruct.isUnsignedInt(longVal))
                            paramType = 'U';
                        else
                            paramType = 'I';
                    } catch (ParserException ex) {
                        paramType = 'S';
                    }
                }
            }
            
            // create the parameter entry and add it to the list of parameters
            parm = new ParameterStruct(nextArg, paramType);
            frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + paramType + " value: " + nextArg);
            params.add(parm);
        }
        
        return params;
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
    
}
