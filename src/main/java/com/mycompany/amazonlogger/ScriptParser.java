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

    /**
     * verifies the parameter types found in the command line match what is specified for the command.
     * 
     * @param command    - the command line to run
     * @param validTypes - the list of data types to match to
     * @param linenum    - the current line number of the program (for debug msgs)
     * 
     * @throws ParserException 
     */
    private static void checkParamTypes (CommandStruct command, String validTypes, int linenum) throws ParserException {
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
        for (int ix = 0; ix < command.params.size(); ix++) {
            char reqType = Character.toUpperCase(validTypes.charAt(ix));
            if (! command.params.get(ix).isValidForType (reqType)) {
                throw new ParserException(prefix + "Invalid param[" + ix + "] type '" + reqType + "'");
            }
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
            String strCmd = line;
            String parmString = "";
            int offset = strCmd.indexOf(" ");
            if (offset > 0) {
                strCmd = strCmd.substring(0, offset).strip();
                parmString = line.substring(offset).strip();
            }
            CommandStruct.CommandTable command = CommandStruct.isValidCommand(strCmd);
            if (command == null) {
                // check for parameter names in the case of an assignment statement
                ParamExtract parmInfo = new ParamExtract(line);
                String parmName = parmInfo.getName();
                String parmCalc = parmInfo.getEvaluation();
                if (parmInfo.isEquation() && parmName != null && parmCalc != null) {
                    command = CommandStruct.CommandTable.SET;
                    parmString = parmName + " = " + parmCalc;
                } else if (line.startsWith("-")) {
                    // if the optional RUN command was omitted from an option command, let's add it here
                    String argTypes = cmdOptionParser.getOptionParams(strCmd);
                    if (argTypes == null) {
                        throw new ParserException(functionId + "option is not valid: " + strCmd);
                    }
                    command = CommandStruct.CommandTable.RUN;
                    parmString = line;
                }
            }
            
            if (command == null) {
                throw new ParserException(functionId + lineInfo + "Invalid command " + strCmd);
            }

            // 'parmString' is a string containing the parameters following the command
            // 'cmdStruct' will receive the command, with the params yet to be placed.
            CommandStruct cmdStruct = new CommandStruct(command, lineNum);
            ArrayList<String> listParms;
            
            // extract the parameters to pass to the command
            frame.outputInfoMsg(STATUS_PROGRAM, "PROGIX [" + cmdIndex + "]: " + cmdStruct.command + " " + parmString);
            boolean bParamAssign = (CommandStruct.CommandTable.SET == command);
            cmdStruct.params = packParameters (parmString, bParamAssign);
            ParameterStruct.showParamTypeList(cmdStruct.params);

            // now let's check for valid command keywords and extract the parameters
            //  into the cmdStruct structure.
            switch (cmdStruct.command) {
                case CommandStruct.CommandTable.DEFINE:
                    // must be a List of parameter name entries
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
                    break;
                case CommandStruct.CommandTable.SET:
                    // 1st entry is always the parameter the calc is being applied to. get its type.
                    ParameterStruct.ParamType ptype = ParameterStruct.getParamTypeFromName(parmString);
                    if (cmdStruct.params.size() > 3 &&
                        (ptype == ParameterStruct.ParamType.Integer ||
                         ptype == ParameterStruct.ParamType.Unsigned  )  ) {
                        // we pack parameters differently for calculations, so let's repack params for calcs
                        cmdStruct.params = packCalculation (parmString, ptype);
                        ParameterStruct.showParamTypeList(cmdStruct.params);
                    }
                    
                    String eqSign = cmdStruct.params.get(1).getStringValue();
                    if (! eqSign.contentEquals("=")) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command missing = sign: " + parmString);
                    }
                    break;

                // these are the Array-only commands
                case CommandStruct.CommandTable.INSERT:
                case CommandStruct.CommandTable.APPEND:
                    // ARGS: ParamName, Value (String or Integer)
                    // verify there are the correct number and type of arguments
                    if (cmdStruct.params.size() != 2) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command requires 2 arguments : " + parmString);
                    }
                    ParameterStruct param1 = cmdStruct.params.get(0);
                    ParameterStruct param2 = cmdStruct.params.get(1);
                    if (param1.getParamType() != ParameterStruct.ParamType.String) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command 1st argument must be parameter reference name : " + parmString);
                    }
                    ptype = ParameterStruct.isParamDefined(param1.getStringValue());
                    ParameterStruct.ParamType argtype = param2.getParamType();
                    switch (ptype) {
                        case ParameterStruct.ParamType.IntArray -> {
                            if (argtype != ParameterStruct.ParamType.Integer &&
                                argtype != ParameterStruct.ParamType.Unsigned) {
                                throw new ParserException(functionId + lineInfo + cmdStruct.command + " command has mismatched data type for reference parameter: " + parmString);
                            }
                    }
                        case ParameterStruct.ParamType.StringArray -> {
                            if (argtype != ParameterStruct.ParamType.String) {
                                throw new ParserException(functionId + lineInfo + cmdStruct.command + " command has mismatched data type for reference parameter: " + parmString);
                            }
                    }
                        default -> throw new ParserException(functionId + lineInfo + cmdStruct.command + " command not valid for " + ptype + ": " + parmString);
                    }
                    break;

                case CommandStruct.CommandTable.MODIFY:
                    // ParamName, Index (Integer), Value (String or Integer)
                    // verify there are the correct number and type of arguments
                    if (cmdStruct.params.size() != 3) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command requires 3 arguments : " + parmString);
                    }
                    ParameterStruct param3;
                    param1 = cmdStruct.params.get(0);
                    param2 = cmdStruct.params.get(1);
                    param3 = cmdStruct.params.get(2);
                    if (param1.getParamType() != ParameterStruct.ParamType.String) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command 1st argument must be parameter reference name : " + parmString);
                    }
                    if (param2.getParamType() != ParameterStruct.ParamType.Integer &&
                        param2.getParamType() != ParameterStruct.ParamType.Unsigned) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command has invalid index value type: " + parmString);
                    }
                    ptype = ParameterStruct.isParamDefined(param1.getStringValue());
                    argtype = param3.getParamType();
                    switch (ptype) {
                        case ParameterStruct.ParamType.IntArray -> {
                            if (argtype != ParameterStruct.ParamType.Integer &&
                                argtype != ParameterStruct.ParamType.Unsigned) {
                                throw new ParserException(functionId + lineInfo + cmdStruct.command + " command has mismatched data type for reference parameter: " + parmString);
                            }
                    }
                        case ParameterStruct.ParamType.StringArray -> {
                            if (argtype != ParameterStruct.ParamType.String) {
                                throw new ParserException(functionId + lineInfo + cmdStruct.command + " command has mismatched data type for reference parameter: " + parmString);
                            }
                    }
                        default -> throw new ParserException(functionId + lineInfo + cmdStruct.command + " command not valid for " + ptype + ": " + parmString);
                    }
                    break;

                case CommandStruct.CommandTable.REMOVE:
                    // ParamName, Index (Integer)
                    // verify there are the correct number and type of arguments
                    if (cmdStruct.params.size() != 2) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command requires 2 arguments : " + parmString);
                    }
                    param1 = cmdStruct.params.get(0);
                    param2 = cmdStruct.params.get(1);
                    if (param1.getParamType() != ParameterStruct.ParamType.String) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command 1st argument must be parameter reference name : " + parmString);
                    }
                    if (param2.getParamType() != ParameterStruct.ParamType.Integer &&
                        param2.getParamType() != ParameterStruct.ParamType.Unsigned) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command has invalid index value type: " + parmString);
                    }
                    ptype = ParameterStruct.isParamDefined(param1.getStringValue());
                    if (ptype != ParameterStruct.ParamType.IntArray &&
                        ptype != ParameterStruct.ParamType.StringArray) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command not valid for parameter " + param1.getStringValue());
                    }
                    break;

                case CommandStruct.CommandTable.TRUNCATE:
                case CommandStruct.CommandTable.POP:
                    // ParamName, Index (Integer - optional)
                    // verify there are the correct number and type of arguments
                    if (cmdStruct.params.size() != 1 && cmdStruct.params.size() != 2) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command requires 2 arguments : " + parmString);
                    }
                    param1 = cmdStruct.params.get(0);
                    if (param1.getParamType() != ParameterStruct.ParamType.String) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command 1st argument must be parameter reference name : " + parmString);
                    }
                    ptype = ParameterStruct.isParamDefined(param1.getStringValue());
                    if (ptype != ParameterStruct.ParamType.IntArray &&
                        ptype != ParameterStruct.ParamType.StringArray) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command not valid for parameter " + param1.getStringValue());
                    }
                    if (cmdStruct.params.size() == 2) {
                        param2 = cmdStruct.params.get(1);
                        if (param2.getParamType() != ParameterStruct.ParamType.Integer &&
                            param2.getParamType() != ParameterStruct.ParamType.Unsigned) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " command has invalid index value type: " + parmString);
                        }
                    }
                    break;

                case CommandStruct.CommandTable.IF:
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
                case CommandStruct.CommandTable.ELSE:
                    if (ifList.isEmpty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                    }
                    // save the current command index in the current if structure
                    ifInfo = getIfEntry(ifStack.peek());
                    ifInfo.setElseIndex(cmdIndex, false, LoopStruct.getStackSize());
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + ifStack.size() + " " + cmdStruct.command + " on line " + cmdIndex);
                    break;
                case CommandStruct.CommandTable.ELSEIF:
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
                case CommandStruct.CommandTable.ENDIF:
                    if (ifStack.empty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                    }
                    // save the current command index in the current if structure
                    ifInfo = getIfEntry(ifStack.peek());
                    ifInfo.setEndIfIndex(cmdIndex, LoopStruct.getStackSize());
                    ifStack.pop();
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + ifStack.size() + " " + cmdStruct.command + " on line " + cmdIndex);
                    break;
                case CommandStruct.CommandTable.FOR:
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
                case CommandStruct.CommandTable.BREAK:
                    // make sure we are in a FOR ... NEXT loop
                    if (LoopStruct.getStackSize() == 0) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    // verify the IF loop level hasn't been exceeded
                    LoopId curLoop = LoopStruct.peekStack();
                    ParameterStruct.checkLoopIfLevel (cmdStruct.command, ifStack.size(), curLoop);
                    break;
                case CommandStruct.CommandTable.CONTINUE:
                    // make sure we are in a FOR ... NEXT loop
                    if (LoopStruct.getStackSize() == 0) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    // verify the IF loop level hasn't been exceeded
                    curLoop = LoopStruct.peekStack();
                    ParameterStruct.checkLoopIfLevel (cmdStruct.command, ifStack.size(), curLoop);
                    break;
                case CommandStruct.CommandTable.NEXT:
                    // make sure we are in a FOR ... NEXT loop
                    if (LoopStruct.getStackSize() == 0) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    // verify the IF loop level hasn't been exceeded
                    curLoop = LoopStruct.peekStack();
                    ParameterStruct.checkLoopIfLevel (cmdStruct.command, ifStack.size(), curLoop);
                    break;
                case CommandStruct.CommandTable.ENDFOR:
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
                case CommandStruct.CommandTable.RUN:
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
        cmdList.add(new CommandStruct(CommandStruct.CommandTable.EXIT, lineNum));
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
        // (skip for SET command so as not to modify the parameter we are setting.
        //  the conversion for this will be done in Calculation)
//        if (cmdStruct.command != CommandStruct.CommandTable.SET) {
            for (int ix = 0; ix < cmdStruct.params.size(); ix++) {
                if (ix > 0 || cmdStruct.command != CommandStruct.CommandTable.SET) {
                ParameterStruct param = cmdStruct.params.get(ix);
                param.updateFromReference();
                }
            }
//        }
        frame.outputInfoMsg(STATUS_PROGRAM, lineInfo + cmdStruct.showCommand());

        try {
        switch (cmdStruct.command) {
            case CommandStruct.CommandTable.EXIT:
                return -1; // this will terminate the program
            case CommandStruct.CommandTable.DEFINE:
                break;
            case CommandStruct.CommandTable.SET:
                ParameterStruct parmRef   = cmdStruct.params.get(0); // element 0 is the param ref to assigned a value to
                ParameterStruct parmEqu   = cmdStruct.params.get(1); // element 1 is the assignment type (could be: =, +=, -=, etc)
                ParameterStruct parmValue = cmdStruct.params.get(2); // element 2 is the value being assigned to it
                String parmName = parmRef.getParamRefName();
                ParameterStruct.ParamType type = parmRef.getParamRefType();
                if (type == null || parmName == null) {
                    parmName = parmRef.getStringValue();
                    type = ParameterStruct.ParamType.String;
                }
                // make sure we are converting to the type of the reference parameter
                switch (type) {
                    case ParameterStruct.ParamType.Integer:
                    case ParameterStruct.ParamType.Unsigned:
                        // TODO: use 'parmEqu' (the type of assignment) in the calculation
                        Long result;
                        if (parmValue.isCalculation()) {
                            result = parmValue.getCalculationValue(type);
                        } else {
                            result = parmValue.getIntegerValue();
                        }
                        if (type == ParameterStruct.ParamType.Unsigned) {
                            result &= 0xFFFFFFFF;
                        }
                        ParameterStruct.modifyIntegerParameter(parmName, result);
                        break;
                    case ParameterStruct.ParamType.Boolean:
                        // TODO: allow comparison on right-hand assignment of Boolean
                        ParameterStruct.modifyBooleanParameter(parmName, parmValue.getBooleanValue());
                        break;
                    case ParameterStruct.ParamType.IntArray:
                        ParameterStruct.setIntArrayParameter(parmName, parmValue.getIntArray());
                        break;
                    case ParameterStruct.ParamType.StringArray:
                        ParameterStruct.setStrArrayParameter(parmName, parmValue.getStrArray());
                        break;
                    default:
                    case ParameterStruct.ParamType.String:
                        String concat = parmValue.getStringValue();
                        if (parmEqu.getStringValue().equals("+=")) {
                            concat += parmRef.getStringValue();
                        }
                        if (cmdStruct.params.size() > 3) {
                            // need to concatenate the strings into 1.
                            for (int ix = 3; ix + 1 < cmdStruct.params.size(); ix+=2) {
                                String sign = cmdStruct.params.get(ix).getStringValue();
                                String next = cmdStruct.params.get(ix+1).getStringValue();
                                if (sign.contentEquals("+"))
                                    concat += next;
                                else
                                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " Invalid String concatenation");
                            }
                        }
                        ParameterStruct.modifyStringParameter(parmName, concat);
                        break;
                }
                break;

            // TODO: these are the Array-only commands
            case CommandStruct.CommandTable.INSERT:
                // ParamName, Value (String or Integer)
                parmRef   = cmdStruct.params.get(0); // element 0 is the param ref to be inserted into
                parmValue = cmdStruct.params.get(1); // element 1 is the value being inserted
                boolean bSuccess = ParameterStruct.arrayInsertEntry (parmRef.getStringValue(), 0, parmValue.getStringValue());
                if (!bSuccess) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " " + parmRef.getParamRefType().toString() +
                                                " parameter ref not found: " + parmRef.getStringValue());
                }
                break;
            case CommandStruct.CommandTable.APPEND:
                // ParamName, Value (String or Integer)
                parmRef   = cmdStruct.params.get(0); // element 0 is the param ref to be appended to
                parmValue = cmdStruct.params.get(1); // element 1 is the value being appended
                bSuccess = ParameterStruct.arrayAppendEntry (parmRef.getStringValue(), parmValue.getStringValue());
                if (!bSuccess) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " " + parmRef.getParamRefType().toString() +
                                                " parameter ref not found: " + parmRef.getStringValue());
                }
                break;
            case CommandStruct.CommandTable.MODIFY:
                // ParamName, Index (Integer), Value (String or Integer)
                ParameterStruct parmIndex;
                parmRef   = cmdStruct.params.get(0); // element 0 is the param ref to be modified
                parmIndex = cmdStruct.params.get(1); // element 1 is the index element being modified
                parmValue = cmdStruct.params.get(2); // element 2 is the value to set the entry to
                int index = parmIndex.getIntegerValue().intValue();
                bSuccess = ParameterStruct.arrayModifyEntry (parmRef.getStringValue(), index, parmValue.getStringValue());
                if (!bSuccess) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " " + parmRef.getParamRefType().toString() +
                                                " parameter ref not found: " + parmRef.getStringValue());
                }
                break;
            case CommandStruct.CommandTable.REMOVE:
                // ParamName, Index (Integer)
                parmRef   = cmdStruct.params.get(0); // element 0 is the param ref to be modified
                parmIndex = cmdStruct.params.get(1); // element 1 is the index element being removed
                index = parmIndex.getIntegerValue().intValue();
                bSuccess = ParameterStruct.arrayRemoveEntries (parmRef.getStringValue(), index, 1);
                if (!bSuccess) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " " + parmRef.getParamRefType().toString() +
                                                " parameter ref not found: " + parmRef.getStringValue());
                }
                break;
            case CommandStruct.CommandTable.TRUNCATE:
                // ParamName, Count (Integer - optional)
                parmRef = cmdStruct.params.get(0); // element 0 is the param ref to be modified
                int size = ParameterStruct.getArraySize(parmRef.getStringValue());
                int iCount = 1;
                if (cmdStruct.params.size() > 1) {
                    parmIndex = cmdStruct.params.get(1); // element 1 is the (optional) number of entries being removed
                    iCount = parmIndex.getIntegerValue().intValue();
                    if (iCount > size) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " item count " + iCount +
                                " exceeds size of " + parmRef.getParamRefType().toString());
                    }
                }
                int iStart = size - iCount;
                bSuccess = ParameterStruct.arrayRemoveEntries (parmRef.getStringValue(), iStart, iCount);
                if (!bSuccess) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " " + parmRef.getParamRefType().toString() +
                                                " parameter ref not found: " + parmRef.getStringValue());
                }
                break;
            case CommandStruct.CommandTable.POP:
                // ParamName, Index (Integer - optional)
                parmRef = cmdStruct.params.get(0); // element 0 is the param ref to be modified
                size = ParameterStruct.getArraySize(parmRef.getStringValue());
                iCount = 1;
                iStart = 0;
                if (cmdStruct.params.size() > 1) {
                    parmIndex = cmdStruct.params.get(1); // element 1 is the (optional) number of entries being removed
                    iCount = parmIndex.getIntegerValue().intValue();
                    if (iCount > size) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " item count " + iCount +
                                " exceeds size of " + parmRef.getParamRefType().toString());
                    }
                }
                bSuccess = ParameterStruct.arrayRemoveEntries (parmRef.getStringValue(), iStart, iCount);
                if (!bSuccess) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " " + parmRef.getParamRefType().toString() +
                                                " parameter ref not found: " + parmRef.getStringValue());
                }
                break;
                
            case CommandStruct.CommandTable.IF:
                ParameterStruct parm1 = cmdStruct.params.get(0);
                String comp           = cmdStruct.params.get(1).getStringValue();
                ParameterStruct parm2 = cmdStruct.params.get(2);

                // add entry to the current loop stack
                ifStack.push(cmdIndex);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - new IF level " + ifStack.size() + " " + parm1.getStringValue() + " " + comp + " " + parm2.getStringValue());

                // check status to see if true of false.
                boolean bBranch;
                if ((parm1.getParamType() == ParameterStruct.ParamType.Integer || parm1.getParamType() == ParameterStruct.ParamType.Unsigned) &&
                    (parm2.getParamType() == ParameterStruct.ParamType.Integer || parm2.getParamType() == ParameterStruct.ParamType.Unsigned)    ) {
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
            case CommandStruct.CommandTable.ELSE:
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
            case CommandStruct.CommandTable.ELSEIF:
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
                    if ((parm1.getParamType() == ParameterStruct.ParamType.Integer || parm1.getParamType() == ParameterStruct.ParamType.Unsigned) &&
                        (parm2.getParamType() == ParameterStruct.ParamType.Integer || parm2.getParamType() == ParameterStruct.ParamType.Unsigned)    ) {
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
            case CommandStruct.CommandTable.ENDIF:
                if (ifStack.empty()) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a IF structure");
                }
                // save the current command index in the current if structure
                ifStack.pop();
                frame.outputInfoMsg(STATUS_PROGRAM, "   - new IF level " + ifStack.size() + ": " + cmdStruct.command + " on line " + cmdIndex);
                break;
            case CommandStruct.CommandTable.FOR:
                String loopName  = cmdStruct.params.get(0).getStringValue();
                curLoopId = new LoopId(loopName, cmdIndex);
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                    
                // add entry to the current loop stack
                LoopStruct.pushStack(curLoopId);
                int loopSize = LoopStruct.getStackSize();
                frame.outputInfoMsg(STATUS_PROGRAM, "   - new FOR Loop level " + loopSize+ " parameter " + loopName + " index @ " + cmdIndex);
                break;
            case CommandStruct.CommandTable.BREAK:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - " + cmdStruct.command.toString() + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                break;
            case CommandStruct.CommandTable.CONTINUE:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - " + cmdStruct.command.toString() + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                break;
            case CommandStruct.CommandTable.NEXT:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - " + cmdStruct.command.toString() + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                break;
            case CommandStruct.CommandTable.ENDFOR:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "   - " + cmdStruct.command.toString() + " command for Loop level " + loopSize
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
            case CommandStruct.CommandTable.RUN:
                // fall through...
            default:
                cmdOptionParser.runCmdOption (cmdStruct);
                break;
        }
        } catch (ParserException exMsg) {
            frame.outputInfoMsg(STATUS_ERROR, functionId + "command: " + cmdStruct.command.toString() + "\n  -> " + exMsg);
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
     * This simply separates the String of arguments into separate parameter
     *   values based on where it finds commas and quotes.
     * 
     * @param line - the string of parameters to separate and classify
     * @param bParamAssign - true if parameter assignment, so 1st value is a parameter
     * 
     * @return the ArrayList of arguments for the command
     * 
     * @throws ParserException 
     */
    private ArrayList<ParameterStruct> packParameters (String line, boolean bParamAssign) throws ParserException {
        ArrayList<ParameterStruct> params = new ArrayList<>();
        ParameterStruct parm;

        for (int ix = 0; ! line.isEmpty(); ix++) {
            // read next entry
            ParameterStruct.ParamType paramType = ParameterStruct.ParamType.String;
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
                    parm = new ParameterStruct (nextArg, ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.StringArray);
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
            if (paramType == ParameterStruct.ParamType.String) {
                if (nextArg.equalsIgnoreCase("TRUE") ||
                    nextArg.equalsIgnoreCase("FALSE")) {
                    paramType = ParameterStruct.ParamType.Boolean;
                } else {
                    try {
                        Long longVal = ParameterStruct.getLongOrUnsignedValue(nextArg);
                        if (ParameterStruct.isUnsignedInt(longVal))
                            paramType = ParameterStruct.ParamType.Unsigned;
                        else
                            paramType = ParameterStruct.ParamType.Integer;
                    } catch (ParserException ex) {
                        paramType = ParameterStruct.ParamType.String;
                    }
                }
            }
            
            // create the parameter entry and add it to the list of parameters
            ParameterStruct.ParamClass pClass = ParameterStruct.ParamClass.Discrete;
            if (nextArg.startsWith("$") || (bParamAssign && params.isEmpty())) {
                pClass = ParameterStruct.ParamClass.Reference;
                paramType = ParameterStruct.getParamTypeFromName(nextArg);
            }
            
            parm = new ParameterStruct(nextArg, pClass, paramType);
            frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + paramType + " value: " + nextArg);
            params.add(parm);
        }
        
        return params;
    }

    /**
     * This takes a command line and extracts the calculation parameter list from it.
     * This will be in the form:  ParamName = Calculation
     * where: Calculation will be a string or one or more value/parameters with
     *        associated parenthesis and operations.
     * 
     * @param line  - the string of parameters to separate and classify
     * @param ptype - the type of parameter being set
     * 
     * @return the ArrayList of arguments for the command
     * 
     * @throws ParserException 
     */
    private ArrayList<ParameterStruct> packCalculation (String line, ParameterStruct.ParamType ptype) throws ParserException {
        String functionId = CLASS_NAME + ".packCalculation: ";

        ArrayList<ParameterStruct> params = new ArrayList<>();
        ParameterStruct parm;
        
        // 1st entry should be the parameter name
        String paramName = getParamName (line);
        if (! ParameterStruct.isValidParamName(paramName)) {
            throw new ParserException(functionId + "parameter name not found: " + paramName);
        }
        if (line.contentEquals(paramName)) {
            throw new ParserException(functionId + "no arguments following parameter name: " + line);
        }
        // the 1st argument of a SET command is the parameter name to assign the value to
        line = line.substring(paramName.length()).strip();
        parm = new ParameterStruct(paramName, ParameterStruct.ParamClass.Reference, ptype);
        frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type S value: " + paramName);
        params.add(parm);

        // next entry should be the equality sign
        // TODO: need to categorize these and save the type
        String nextArg = getNextWord (line);
        switch (nextArg) {
            case "=":
            case "+=":
            case "-=":
            case "*=":
            case "/=":
            case "%=":
                // these are the integer assignment entries
                break;
            case "AND=":
            case "OR=":
            case "XOR=":
                // these are the unsigned bitwise assignment entries
                break;
            default:
                throw new ParserException(functionId + "invalid equality sign: " + nextArg);
        }
        line = line.substring(nextArg.length()).strip();
        parm = new ParameterStruct(nextArg, ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.String);
        frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type S value: " + nextArg);
        params.add(parm);
        
        // make sure the type is correct for the equate sign
        switch (ptype) {
            case ParameterStruct.ParamType.Integer:
                if (nextArg.equals("AND=") || nextArg.equals("OR=") || nextArg.equals("XOR=")) {
                    throw new ParserException(functionId + "Bitwise assignments not allowed for type: " + ptype);
                }
                break;
            case ParameterStruct.ParamType.Unsigned:
                // all equates are valid for Unsigned
                break;
            default:
                throw new ParserException(functionId + "Assignment command not allowed for type: " + ptype);
        }
        
        // remaining data is the Calculation, which may be a single value or a complex formula
        parm = new ParameterStruct(line, ParameterStruct.ParamClass.Calculation, ptype);
        frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type C value: " + line);
        params.add(parm);
        
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
    
    private static String getParamName (String line) {
        for (int ix = 0; ix < line.length(); ix++) {
            if (! Character.isLetterOrDigit(line.charAt(ix)) && line.charAt(ix) != '_')
                return line.substring(0, ix);
        }
        return line;
    }
}
