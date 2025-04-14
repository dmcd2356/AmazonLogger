/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 *
 * @author dan
 */
public class ScriptParser {
    
    private static final String CLASS_NAME = "ScriptParser";
    
    // this handles the command line options via the RUN command
    private final CmdOptions cmdOptionParser;
     

    ScriptParser() {
        // create an instance of the command options parser for any RUN commands
        cmdOptionParser = new CmdOptions();
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
        ScriptExecute exec = new ScriptExecute();
        int cmdIx = 0;
        while (cmdIx >= 0 && cmdIx < cmdList.size()) {
            cmdIx = exec.executeProgramCommand (cmdIx, cmdList.get(cmdIx));
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
        boolean bExit = false;
        while (!bExit && (line = fileReader.readLine()) != null) {
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
                String parmEqu  = parmInfo.getEquality();
                String parmCalc = parmInfo.getEvaluation();
                if (parmInfo.isEquation() && parmName != null && parmCalc != null) {
                    command = CommandStruct.CommandTable.SET;
                    parmString = parmName + " " + parmEqu + " " + parmCalc;
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
            try {
            frame.outputInfoMsg(STATUS_PROGRAM, "PROGIX [" + cmdIndex + "]: " + cmdStruct.command + " " + parmString);
            boolean bParamAssign = (CommandStruct.CommandTable.SET == command);
            cmdStruct.params = packParameters (parmString, bParamAssign);
            ParameterStruct.showParamTypeList(cmdStruct.params);

            // now let's check for valid command keywords and extract the parameters
            //  into the cmdStruct structure.
            switch (cmdStruct.command) {
                case CommandStruct.CommandTable.EXIT:
                    bExit = true;
                    break;
                case PRINT:
                    // verify 1 String argument: text message
                    if (cmdStruct.params.size() != 1) {
                        throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing argument: text");
                    }
                    break;
                case OPENR:
                    // verify 1 String argument: file name
                    if (cmdStruct.params.size() != 1) {
                        throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing argument: file name");
                    }
                    // TODO: verify file exists and is readable
                    break;
                case OPENW:
                    // verify 1 String argument: file name
                    if (cmdStruct.params.size() != 1) {
                        throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing argument: file name");
                    }
                    break;
                case CLOSE:
                    // verify 1 String argument: file name
                    if (cmdStruct.params.size() != 1) {
                        throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing argument: file name ");
                    }
                    break;
                case READ:
                    // verify 1 optional number of lines to read
                    if (cmdStruct.params.isEmpty()) {
                        // argument is missing, supply the default value
                        ParameterStruct lines = new ParameterStruct("1",
                                                ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.Unsigned);
                        frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + cmdStruct.params.size() + "]: type Unsigned value: 1");
                        cmdStruct.params.add(lines);
                    } else {
                        Long count = cmdStruct.params.get(0).getIntegerValue();
                        if (count == null || count < 1) {
                            throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Count value missing or < 0");
                        }
                    }
                    break;
                case WRITE:
                    // verify 2 arguments: file name and message to write
                    if (cmdStruct.params.size() != 1) {
                        throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing argument: message");
                    }
                    break;
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
                    // we pack parameters differently for calculations, so if the param
                    //  is a numeric parameter and it is more than a simple assignment to
                    //  a discrete value or a single parameter reference, let's pepack.
                    // The arguments are: ParamName = Calculation
                    ParameterStruct.ParamType ptype = ParameterStruct.getParamTypeFromName(parmString);
                    if (cmdStruct.params.size() > 3) {
                        switch (ptype) {
                            case ParameterStruct.ParamType.Integer:
                            case ParameterStruct.ParamType.Unsigned:
                                cmdStruct.params = packCalculation (parmString, ptype);
                                ParameterStruct.showParamTypeList(cmdStruct.params);
                                break;
                            case ParameterStruct.ParamType.Boolean:
                                // TODO: The form should be: ParamName = Calculation compSign Calculation
                                throw new ParserException(functionId + lineInfo + cmdStruct.command + " Boolean entries cannot perform complex comparisons yet!");
                                //break;
                            case ParameterStruct.ParamType.String:
                                // go through all the arg list and remove all the "+" entries
                                // that way, all we have left is a list of all the Strings to add
                                for (int ix = cmdStruct.params.size() - 2; ix >= 3; ix-=2) {
                                    String sign = cmdStruct.params.get(ix).getStringValue();
                                    if (sign.contentEquals("+"))
                                        cmdStruct.params.remove(ix);
                                    else
                                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " Invalid String concatenation");
                                }
                                break;
                            default:
                                // Strings are handled in the execution phase
                                // Arrays are not allowed to have any operations, just simple assignments
                                break;
                        }
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
                    if (!IFStruct.isIfListEnpty() && !IFStruct.isIfStackEnpty()) {
                        ifInfo = IFStruct.getIfListEntry();
                        if (!ifInfo.isValid()) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when previous IF has no matching ENDIF");
                        }
                    }
                    
                    // add entry to the current loop stack
                    ifInfo = new IFStruct (cmdIndex, LoopStruct.getStackSize());
                    IFStruct.ifListPush(ifInfo);
                    IFStruct.stackPush(cmdIndex);
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - new IF level " + IFStruct.getStackSize() + " parameter " + ifName);
                    break;
                case CommandStruct.CommandTable.ELSE:
                    if (IFStruct.isIfListEnpty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                    }
                    // save the current command index in the current if structure
                    ifInfo = IFStruct.getIfListEntry();
                    ifInfo.setElseIndex(cmdIndex, false, LoopStruct.getStackSize());
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + IFStruct.getStackSize() + " " + cmdStruct.command + " on line " + cmdIndex);
                    break;
                case CommandStruct.CommandTable.ELSEIF:
                    if (IFStruct.isIfStackEnpty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                    }
                    
                    // read the arguments passed
                    // assumed format is: IF Name1 >= Name2  (where Names can be Integers, Strings or Parameters)
                    ifName = cmdStruct.params.get(0).getStringValue();
                    
                    // save the current command index in the current if structure
                    ifInfo = IFStruct.getIfListEntry();
                    ifInfo.setElseIndex(cmdIndex, true, LoopStruct.getStackSize());
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + IFStruct.getStackSize() + " " + cmdStruct.command + " on line " + cmdIndex + " parameter " + ifName);
                    break;
                case CommandStruct.CommandTable.ENDIF:
                    if (IFStruct.isIfStackEnpty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                    }
                    // save the current command index in the current if structure
                    ifInfo = IFStruct.getIfListEntry();
                    ifInfo.setEndIfIndex(cmdIndex, LoopStruct.getStackSize());
                    IFStruct.stackPop();
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + IFStruct.getStackSize() + " " + cmdStruct.command + " on line " + cmdIndex);
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
                        loopInfo = new LoopStruct (loopName, loopStart, loopEnd, loopStep, loopComp, cmdIndex, IFStruct.getStackSize());
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
                    ParameterStruct.checkLoopIfLevel (cmdStruct.command, IFStruct.getStackSize(), curLoop);
                    break;
                case CommandStruct.CommandTable.CONTINUE:
                    // make sure we are in a FOR ... NEXT loop
                    if (LoopStruct.getStackSize() == 0) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    // verify the IF loop level hasn't been exceeded
                    curLoop = LoopStruct.peekStack();
                    ParameterStruct.checkLoopIfLevel (cmdStruct.command, IFStruct.getStackSize(), curLoop);
                    break;
                case CommandStruct.CommandTable.NEXT:
                    // make sure we are in a FOR ... NEXT loop
                    if (LoopStruct.getStackSize() == 0) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    // verify the IF loop level hasn't been exceeded
                    curLoop = LoopStruct.peekStack();
                    ParameterStruct.checkLoopIfLevel (cmdStruct.command, IFStruct.getStackSize(), curLoop);
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
            } catch (ParserException exMsg) {
                throw new ParserException(exMsg + "\n  -> " + functionId + lineInfo + "PROGIX[" + cmdIndex + "]: " + cmdStruct.command.toString());
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
        if (!IFStruct.isIfStackEnpty() && !IFStruct.getIfListEntry().isValid()) {
            throw new ParserException(functionId + "Last IF has no matching ENDIF");
        }

        fileReader.close();
        
        // the last line will be the one to end the program flow
        cmdList.add(new CommandStruct(CommandStruct.CommandTable.EXIT, lineNum));
        frame.outputInfoMsg(STATUS_PROGRAM, "PROGIX [" + cmdIndex + "]: EXIT  (appended)");
        return cmdList;
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
        String functionId = CLASS_NAME + ".packParameters: ";

        ArrayList<ParameterStruct> params = new ArrayList<>();
        ParameterStruct arg;
        String paramName = null;

        try {
        for (int ix = 0; ! line.isEmpty(); ix++) {
            // read next entry
            ParameterStruct.ParamType paramType = ParameterStruct.ParamType.String;
            String nextArg = getNextWord (line);
            line = line.substring(nextArg.length());

            // if this is the 1st parameter of a parameter assignment, the 1st param
            // must be the parameter name. verify it is valid.
            if (bParamAssign) {
                if (ix == 0) {
                    if (! ParameterStruct.isValidParamName(nextArg)) {
                        throw new ParserException(functionId + "parameter name not found: " + nextArg);
                    }
                    if (line.isEmpty()) {
                        throw new ParserException(functionId + "no arguments following parameter name: " + nextArg);
                    }
                    // check if this is a String parameter ( we may have extra stuff to do here)
                    if (ParameterStruct.getParamTypeFromName(nextArg) == ParameterStruct.ParamType.String) {
                        paramName = "$" + nextArg;
                    }
                } else if (ix == 1 && paramName != null && nextArg.contentEquals("+=")) {
                    // if it is a String parameter and the 2nd entry is a "+=", then we need to
                    //  insert the current parameter value at the begining of the list of Strings to add
                    line = line.strip();
                    nextArg = "=";
                    arg = new ParameterStruct (nextArg, ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.String);
                    frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + paramType + " value: " + nextArg);
                    params.add(arg);
                    arg = new ParameterStruct (paramName, ParameterStruct.ParamClass.Reference, ParameterStruct.ParamType.String);
                    frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + paramType + " value: " + paramName);
                    params.add(arg);
                    nextArg = "+";
                    arg = new ParameterStruct (nextArg, ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.String);
                    frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + paramType + " value: " + nextArg);
                    params.add(arg);
                    continue;
                }
            }
            
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
                    arg = new ParameterStruct (nextArg, ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.StringArray);
                    frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + paramType + " value: [ " + nextArg + " ]");
                    params.add(arg);
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
            
            arg = new ParameterStruct(nextArg, pClass, paramType);
            frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + paramType + " value: " + nextArg);
            params.add(arg);
        }
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        
        return params;
    }

    /**
     * This takes a command line and extracts the calculation parameter list from it.
     * This will be in the form:  ParamName = Calculation
     *      (+=, -=, *=, ... also allowed in place of =)
     * where: Calculation will be a string or one or more value/parameters with
     *        associated parenthesis and operations.
     * Note that this method is only valid for Integer and Unsigned parameters.
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

        if (ptype != ParameterStruct.ParamType.Unsigned && ptype != ParameterStruct.ParamType.Integer) {
            throw new ParserException(functionId + "Assignment command not allowed for type: " + ptype);
        }
        
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
        frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + paramName);
        params.add(parm);

        // next entry should be the equality sign
        String nextArg = getNextWord (line);
        line = line.substring(nextArg.length()).strip();
        String newOp = "";
        switch (nextArg) {
            case "=":
                break;
            case "+=":
            case "-=":
            case "*=":
            case "/=":
            case "%=":
            case "AND=":
            case "OR=":
            case "XOR=":
                int opLen = nextArg.length() - 1;
                newOp = nextArg.substring(0, opLen);
                break;
            default:
                throw new ParserException(functionId + "invalid equality sign: " + nextArg);
        }
        
        // bitwise ops are only allowed for Unsigned type
        if (ptype != ParameterStruct.ParamType.Unsigned) {
            if (newOp.equals("AND") || newOp.equals("OR") || newOp.equals("XOR")) {
                throw new ParserException(functionId + "Bitwise assignments not allowed for type: " + ptype);
            }
        }
        
        // this will pack the "=" sign
        parm = new ParameterStruct("=", ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.String);
        frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type S value: =");
        params.add(parm);
        
        // if there was an operation preceeding the "=" sign, let's sneek the operation in here
        //  by adding it to the rest of the line
        if (!newOp.isEmpty()) {
            // first we add the parameter name that has the $ attached, so it becomes a reference value,
            // followed by the operation to perform, followed by the opening parenthesis and then
            // the remainder of the calculation, then we end it with the closing parenthesis.
            // (the parenthesis are included to assure that the newOp operation is performed last.
            line = "$" + paramName + " " + newOp + " (" + line + ")";
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

    /**
     * extracts the parameter name from the string.
     * It searches only for the valid chars in a parameter name and stops on the first value
     * that i not valid, so that it doesn't depend on any particular delimiter, such as whitespace.
     * Therefore, if the parameter name has an extension added to the end of it, this
     * will only return the parameter name portion of it.
     * 
     * @param line - the string to extract the parameter name from
     * 
     * @return the parameter name (if one existed)
     */
    private static String getParamName (String line) {
        for (int ix = 0; ix < line.length(); ix++) {
            if (! Character.isLetterOrDigit(line.charAt(ix)) && line.charAt(ix) != '_')
                return line.substring(0, ix);
        }
        return line;
    }
}
