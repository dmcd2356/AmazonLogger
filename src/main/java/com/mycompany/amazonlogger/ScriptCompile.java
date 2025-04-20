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

/**
 *
 * @author dan
 */
public class ScriptCompile {
    
    private static final String CLASS_NAME = "ScriptCompile";
    
    // this handles the command line options via the RUN command
    private final CmdOptions cmdOptionParser;
     

    ScriptCompile() {
        // create an instance of the command options parser for any RUN commands
        cmdOptionParser = new CmdOptions();
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
    public static void checkArgTypes (CommandStruct command, String validTypes, int linenum) throws ParserException {
        String functionId = CLASS_NAME + ".checkArgTypes: ";
        String prefix = command + " - ";
        if (linenum >= 0) {  // omit the line numberinfo if < 0
            prefix = "line " + linenum + ", " + prefix;
        }
        
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
            throw new ParserException(functionId + prefix + "Invalid number of arguments: " + command.params.size() + " (valid = " + validTypes + ")");
        }
        
        // now verify the types
        for (int ix = 0; ix < command.params.size(); ix++) {
            char reqType = Character.toUpperCase(validTypes.charAt(ix));
            if (! command.params.get(ix).isValidForType (reqType)) {
                throw new ParserException(functionId + prefix + "Invalid arg[" + ix + "] type '" + reqType + "'");
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
    public ArrayList<CommandStruct> compileProgram (String fname) throws ParserException, IOException {
        String functionId = CLASS_NAME + ".compileProgram: ";

        frame.outputInfoMsg(STATUS_PROGRAM, "Compiling file: " + fname);
        ArrayList<CommandStruct> cmdList = new ArrayList<>();
        String line = "";
        int cmdIndex = 0;
        String lineInfo = "";
        CommandStruct cmdStruct;

        // open the file to compile and extract the commands from it
        try {
        File scriptFile = Utils.checkFilename (fname, ".scr", "Script", false);
        FileReader fReader = new FileReader(scriptFile);
        BufferedReader fileReader = new BufferedReader(fReader);

        // clear out the static Variable values
        ParameterStruct.initVariables();

        // read the program and compile into ArrayList 'cmdList'
        int lineNum = 0;
        boolean bExit = false;
        while (!bExit && (line = fileReader.readLine()) != null) {
            lineNum++;
            line = line.strip();
            if (line.isBlank() || line.charAt(0) == '#') {
                continue;
            }

            lineInfo = "LINE " + lineNum + ": ";
            cmdIndex = cmdList.size(); // the command index

            // first, extract the 1st word as the command keyword
            String strCmd = line;
            String parmString = "";
            int offset = strCmd.indexOf(" ");
            if (offset > 0) {
                strCmd = strCmd.substring(0, offset).strip();
                parmString = line.substring(offset).strip();
            }
            CommandStruct.CommandTable command;
            if (line.startsWith("-")) {
                // if the optional RUN command was omitted from an option command, let's add it here
                String argTypes = cmdOptionParser.getOptionArgs(strCmd);
                if (argTypes == null) {
                    throw new ParserException(functionId + "command option is not valid: " + strCmd);
                }
                command = CommandStruct.CommandTable.RUN;
                parmString = line;
            } else {
                // next, check if it is a standard program command
                command = CommandStruct.isValidCommand(strCmd);
                if (command == null) {
                    // lastly, check for variable names in the case of an assignment statement
                    VariableExtract parmInfo = new VariableExtract(line);
                    String parmName = parmInfo.getName();
                    String parmEqu  = parmInfo.getEquality();
                    String parmCalc = parmInfo.getEvaluation();
                    if (parmInfo.isEquation() && parmName != null && parmCalc != null) {
                        command = CommandStruct.CommandTable.SET;
                        parmString = parmName + " " + parmEqu + " " + parmCalc;
                    }
                }
            }
            
            if (command == null) {
                throw new ParserException(functionId + lineInfo + "Invalid command " + strCmd);
            }

            // 'parmString' is a string containing the arguments following the command
            // 'cmdStruct'  will receive the command, with the arguments yet to be placed.
            cmdStruct = new CommandStruct(command, lineNum);
            ArrayList<String> listParms;
            
            // extract the arguments to pass to the command
            frame.outputInfoMsg(STATUS_PROGRAM, "PROGIX [" + cmdIndex + "]: " + cmdStruct.command + " " + parmString);
            boolean bParamAssign = (CommandStruct.CommandTable.SET == command);
            cmdStruct.params = packParameters (parmString, bParamAssign);
            ParameterStruct.showParamTypeList(cmdStruct.params);

            // now let's check for valid command keywords and extract the arguments
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
                case FCREATER:
                    // verify 1 String argument: file name
                    if (cmdStruct.params.size() != 1) {
                        throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing argument: file name");
                    }
                    break;
                case FEXISTS:
                    // verify 1 String argument: file name
                    if (cmdStruct.params.size() != 1) {
                        throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing argument: file name");
                    }
                    break;
                case FDELETE:
                    // verify 1 String argument: file name
                    if (cmdStruct.params.size() != 1) {
                        throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing argument: file name");
                    }
                    break;
                case FCREATEW:
                    // verify 1 String argument: file name
                    if (cmdStruct.params.size() != 1) {
                        throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing argument: file name");
                    }
                    break;
                case FOPENR:
                    // verify 1 String argument: file name
                    if (cmdStruct.params.size() != 1) {
                        throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing argument: file name");
                    }
                    break;
                case FOPENW:
                    // verify 1 String argument: file name
                    if (cmdStruct.params.size() != 1) {
                        throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing argument: file name");
                    }
                    break;
                case FCLOSE:
                    // verify 1 String argument: file name
                    if (cmdStruct.params.size() != 1) {
                        throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing argument: file name ");
                    }
                    break;
                case FREAD:
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
                case FWRITE:
                    // verify 2 arguments: file name and message to write
                    if (cmdStruct.params.size() != 1) {
                        throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing argument: message");
                    }
                    break;
                case CommandStruct.CommandTable.DEFINE:
                    // must be a List of Variable name entries
                    checkArgTypes(cmdStruct, "L", cmdIndex);

                    // this defines the Variable names, and must be done prior to their use.
                    // This Compile method will allocate them, so the Execute does not need
                    //  to do anything with this command.
                    // Multiple Variables can be defined on one line, with the names comma separated.
                    ParameterStruct list = cmdStruct.params.getFirst();
                    for (int ix = 0; ix < list.getStrArraySize(); ix++) {
                        String pName = list.getStrArrayElement(ix);
                        try {
                            // allocate the Variable
                            ParameterStruct.allocateVariable(pName);
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
                    ParameterStruct.ParamType ptype = ParameterStruct.getVariableTypeFromName(parmString);
                    if (cmdStruct.params.size() > 3) {
                        switch (ptype) {
                            case ParameterStruct.ParamType.Integer,
                                 ParameterStruct.ParamType.Unsigned,
                                 ParameterStruct.ParamType.Boolean -> {
                                cmdStruct.params = packCalculation (parmString, ptype);
                                ParameterStruct.showParamTypeList(cmdStruct.params);
                            }
                            case ParameterStruct.ParamType.String -> {
                                // go through all the arg list and remove all the "+" entries
                                // that way, all we have left is a list of all the Strings to add
                                for (int ix = cmdStruct.params.size() - 2; ix >= 3; ix-=2) {
                                    String sign = cmdStruct.params.get(ix).getStringValue();
                                    if (sign.contentEquals("+")) {
                                        cmdStruct.params.remove(ix);
                                    }
                                    else {
                                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " Invalid String concatenation");
                                    }
                                }
                            }
                            default -> {
                                // Strings are handled in the execution phase
                                // Arrays are not allowed to have any operations, just simple assignments
                            }
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
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command 1st argument must be Variable reference name : " + parmString);
                    }
                    ptype = ParameterStruct.isVariableDefined(param1.getStringValue());
                    ParameterStruct.ParamType argtype = param2.getParamType();
                    switch (ptype) {
                        case ParameterStruct.ParamType.IntArray -> {
                            if (argtype != ParameterStruct.ParamType.Integer &&
                                argtype != ParameterStruct.ParamType.Unsigned) {
                                throw new ParserException(functionId + lineInfo + cmdStruct.command + " command has mismatched data type for reference Variable: " + parmString);
                            }
                    }
                        case ParameterStruct.ParamType.StringArray -> {
                            if (argtype != ParameterStruct.ParamType.String) {
                                throw new ParserException(functionId + lineInfo + cmdStruct.command + " command has mismatched data type for reference Variable: " + parmString);
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
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command 1st argument must be Variable reference name : " + parmString);
                    }
                    if (param2.getParamType() != ParameterStruct.ParamType.Integer &&
                        param2.getParamType() != ParameterStruct.ParamType.Unsigned) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command has invalid index value type: " + parmString);
                    }
                    ptype = ParameterStruct.isVariableDefined(param1.getStringValue());
                    argtype = param3.getParamType();
                    switch (ptype) {
                        case ParameterStruct.ParamType.IntArray -> {
                            if (argtype != ParameterStruct.ParamType.Integer &&
                                argtype != ParameterStruct.ParamType.Unsigned) {
                                throw new ParserException(functionId + lineInfo + cmdStruct.command + " command has mismatched data type for reference Variable: " + parmString);
                            }
                    }
                        case ParameterStruct.ParamType.StringArray -> {
                            if (argtype != ParameterStruct.ParamType.String) {
                                throw new ParserException(functionId + lineInfo + cmdStruct.command + " command has mismatched data type for reference Variable: " + parmString);
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
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command 1st argument must be Variable reference name : " + parmString);
                    }
                    if (param2.getParamType() != ParameterStruct.ParamType.Integer &&
                        param2.getParamType() != ParameterStruct.ParamType.Unsigned) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command has invalid index value type: " + parmString);
                    }
                    ptype = ParameterStruct.isVariableDefined(param1.getStringValue());
                    if (ptype != ParameterStruct.ParamType.IntArray &&
                        ptype != ParameterStruct.ParamType.StringArray) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command not valid for Variable " + param1.getStringValue());
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
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command 1st argument must be Variable reference name : " + parmString);
                    }
                    ptype = ParameterStruct.isVariableDefined(param1.getStringValue());
                    if (ptype != ParameterStruct.ParamType.IntArray &&
                        ptype != ParameterStruct.ParamType.StringArray) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command not valid for Variable " + param1.getStringValue());
                    }
                    if (cmdStruct.params.size() == 2) {
                        param2 = cmdStruct.params.get(1);
                        if (param2.getParamType() != ParameterStruct.ParamType.Integer &&
                            param2.getParamType() != ParameterStruct.ParamType.Unsigned) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " command has invalid index value type: " + parmString);
                        }
                    }
                    break;

                case CLEAR:
                    // ARGS: 0 = ParamName
                    // verify there are the correct number and type of arguments
                    if (cmdStruct.params.size() != 1) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command requires 1 argument : " + parmString);
                    }
                    param1 = cmdStruct.params.get(0);
                    if (param1.getParamType() != ParameterStruct.ParamType.String) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command 1st argument must be Variable reference name : " + parmString);
                    }
                    ptype = ParameterStruct.isVariableDefined(param1.getStringValue());
                    if (ptype == null && ! param1.getStringValue().contentEquals("RESPONSE")) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " command not valid for " + ptype + ": " + parmString);
                    }
                    break;
                    
                case CommandStruct.CommandTable.IF:
                    // verify number and type of arguments
                    cmdStruct.params = packComparison (parmString);
                    ParameterStruct.showParamTypeList(cmdStruct.params);

                    // read the arguments passed
                    // assumed format is: IF Name1 >= Name2  (where Names can be Integers, Strings or Variables)
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
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - new IF level " + IFStruct.getStackSize() + " Variable " + ifName);
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
                    // assumed format is: IF Name1 >= Name2  (where Names can be Integers, Strings or Variables)
                    cmdStruct.params = packComparison (parmString);
                    ifName = cmdStruct.params.get(0).getStringValue();
                    
                    // save the current command index in the current if structure
                    ifInfo = IFStruct.getIfListEntry();
                    ifInfo.setElseIndex(cmdIndex, true, LoopStruct.getStackSize());
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + IFStruct.getStackSize() + " " + cmdStruct.command + " on line " + cmdIndex + " Variable " + ifName);
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
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - new FOR Loop level " + LoopStruct.getStackSize() + " Variable " + loopName + " index @ " + cmdIndex);
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
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId + lineInfo + "PROGIX[" + cmdIndex + "]: " + line);
        }
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
                throw new ParserException (functionId + "Missing parameter for arg[" + parmList.size() + "] type " + curType);
            }
            
            // get the parameter we are searching for and remove it from the input list
            String param = parmArr.substring(0, offset);
            parmArr = parmArr.substring(offset);
            frame.outputInfoMsg(STATUS_DEBUG, "    offset = " + offset);

            switch (curType) {
                case 'S':
                case 'I':
                case 'C':
                    frame.outputInfoMsg(STATUS_DEBUG, "    extracted arg[" + parmList.size() + "]: '" + param + "'");
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
                    if (! ParameterStruct.isValidVariableName(nextArg)) {
                        throw new ParserException(functionId + "Variable name not found: " + nextArg);
                    }
                    if (line.isEmpty()) {
                        throw new ParserException(functionId + "no arguments following Variable name: " + nextArg);
                    }
                    // check if this is a String parameter ( we may have extra stuff to do here)
                    if (ParameterStruct.getVariableTypeFromName(nextArg) == ParameterStruct.ParamType.String) {
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
                paramType = ParameterStruct.getVariableTypeFromName(nextArg);
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
     * For Integers and Unsigneds this will be in the form:
     *      VarName = Calculation     (+=, -=, *=, ... also allowed in place of =)
     * For Booleans, it will be:
     *      VarName = Calculation compSign Calculation  (compSign: { ==, !=, >=, <=, >, < }
     * 
     * where: Calculation will be a string or one or more values/variables with
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

        if (ptype != ParameterStruct.ParamType.Unsigned &&
            ptype != ParameterStruct.ParamType.Integer  &&
            ptype != ParameterStruct.ParamType.Boolean) {
            throw new ParserException(functionId + "Assignment command not allowed for type: " + ptype);
        }
        
        ArrayList<ParameterStruct> params = new ArrayList<>();
        ParameterStruct parm;
        
        // 1st entry should be the parameter name
        String paramName = getParamName (line);
        boolean bValid;
        try {
            bValid = ParameterStruct.isValidVariableName(paramName);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        if (! bValid) {
            throw new ParserException(functionId + "parameter name not found: " + paramName);
        }
        if (line.contentEquals(paramName)) {
            throw new ParserException(functionId + "no arguments following parameter name: " + line);
        }

        frame.outputInfoMsg(STATUS_PROGRAM, "     * Repacking parameters for Calculation");
        
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
                throw new ParserException(functionId + "Bitwise assignments not allowed for type: " + ptype + ": " + newOp);
            }
        } else if (ptype == ParameterStruct.ParamType.Boolean && ! newOp.equals("=")) {
            throw new ParserException(functionId + "No modifiers in equals allowed for type: " + ptype + ": " + newOp);
        }
        
        // this will pack the "=" sign
        ParameterStruct.ParamType newParam = ParameterStruct.ParamType.String;
        parm = new ParameterStruct("=", ParameterStruct.ParamClass.Discrete, newParam);
        frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + newParam + " value: =");
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

        // check if Boolean type, which must have a comparison of 2 calculations
        if (ptype == ParameterStruct.ParamType.Boolean) {
            ArrayList<ParameterStruct> compParams = packComparison(line);
            params.addAll(compParams);
        } else {
            // else, numeric type: remaining data is a single Calculation
            parm = new ParameterStruct(line, ParameterStruct.ParamClass.Calculation, ptype);
            frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + line);
            params.add(parm);
        }
        
        return params;
    }
    
    /**
     * This extracts a comparison parameter list from it.
     * This is defines as the following format:
     *      Calculation compSign Calculation
     * 
     * Where: compSign: { ==, !=, >=, <=, >, < }
     * 
     *        Calculation is a string or one or more values/variables with
     *        associated parenthesis and operations.
     * 
     * @param line  - the string of parameters to separate and classify
     * 
     * @return the ArrayList of arguments for the command
     * 
     * @throws ParserException 
     */
    private ArrayList<ParameterStruct> packComparison (String line) throws ParserException {
        String functionId = CLASS_NAME + ".packComparison: ";
        
        ArrayList<ParameterStruct> params = new ArrayList<>();
        ParameterStruct parm;
        ParameterStruct.ParamType ptype;
        ParameterStruct.ParamClass pclass1;
        ParameterStruct.ParamClass pclass2;
        
        frame.outputInfoMsg(STATUS_PROGRAM, "     * Repacking parameters for Comparison");
        
        // check for 'NOT' character
        boolean bNot = false;
        line = line.strip();
        if (line.startsWith("!")) {
            bNot = true;
            line = line.substring(1).strip();
        } else if (line.startsWith("NOT")) {
            bNot = true;
            line = line.substring(1).strip();
        }

        // search for required comparison sign
        String compSign = "==";
        int offset = line.indexOf(compSign);
        if (offset <= 0) {
            compSign = ">=";
            offset = line.indexOf(compSign);
        }
        if (offset <= 0) {
            compSign = "<=";
            offset = line.indexOf(compSign);
        }
        if (offset <= 0) {
            compSign = ">";
            offset = line.indexOf(compSign);
        }
        if (offset <= 0) {
            compSign = "<";
            offset = line.indexOf(compSign);
        }
        if (offset <= 0) {
            // this is a single boolean entry rather than a comparison
            pclass1 = (line.startsWith("$") ? ParameterStruct.ParamClass.Reference : ParameterStruct.ParamClass.Discrete);
            ptype = ParameterStruct.ParamType.Boolean;
            parm = new ParameterStruct(line, pclass1, ptype);
            frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + line);
            params.add(parm);
            
            if (bNot) {
                String value = "!";
                pclass2 = ParameterStruct.ParamClass.Discrete;
                ptype = ParameterStruct.ParamType.String;
                parm = new ParameterStruct(value, pclass2, ptype);
                frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + value);
                params.add(parm);
            }
            return params;
        }
        if (bNot) {
            throw new ParserException(functionId + "Can't use '!' modifier on comparison: ! " + compSign);
        }

        String prefix = line.substring(0, offset).strip();
        line = line.substring(offset + compSign.length()).strip();

        // Need to determine if the each comparison entry is a single word and
        //   neither is a numeric. That would make this a String comparison.
        // So if either entry is more than 1 word, we must assume a numeric comparison.
        // Otherwise, if either comparison is in quotes, we must assume a String comparison.
        // Otherwise, if either side is a numeric (value or Variable), assume numeric comparison.
        // Otherwise, do String comparison.
        boolean bCalc = false;
        boolean bQuote = false;
        // if we have more than 1 entry on either side, it must be a numeric calculation
        if (countArgs(prefix) > 1 || countArgs(line) > 1) {
            bCalc = true;
        } else {
            // or, if either entry is a quoted string, remove the quotes from it
            bQuote = (prefix.charAt(0) == '\"' || line.charAt(0) == '\"');
            prefix = extractQuotedString (prefix);
            line   = extractQuotedString (line);
        }
            
        if (! bCalc && ! bQuote) {
            // if neither of the above, let's determine if the entries are numeric or not.
            char ctype1 = ParameterStruct.classifyDataType(prefix);
            char ctype2 = ParameterStruct.classifyDataType(line);
            if ((ctype1 == 'I' || ctype1 == 'U') || (ctype2 == 'I' || ctype2 == 'U')) {
                bCalc = true;
            }
        }
            
        // now set the type of comparison we are doing: Integer or String.
        if (bCalc) {
            ptype = ParameterStruct.ParamType.Integer;
            pclass1 = ParameterStruct.ParamClass.Calculation;
            pclass2 = ParameterStruct.ParamClass.Calculation;
        } else {
            ptype = ParameterStruct.ParamType.String;
            pclass1 = (prefix.startsWith("$") ? ParameterStruct.ParamClass.Reference : ParameterStruct.ParamClass.Discrete);
            pclass2 = (line.startsWith("$") ? ParameterStruct.ParamClass.Reference : ParameterStruct.ParamClass.Discrete);
        }

        // first add the initial Calculation value, which will usually be a Variable or a Discreet value.
        parm = new ParameterStruct(prefix, pclass1, ptype);
        frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + prefix);
        params.add(parm);
        
            // now add the comparison sign
        parm = new ParameterStruct(compSign, ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.String);
        frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + compSign);
        params.add(parm);
            
        // remaining data is the Calculation, which may be a single value or a complex formula
        parm = new ParameterStruct(line, pclass2, ptype);
        frame.outputInfoMsg(STATUS_PROGRAM, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + line);
        params.add(parm);

        return params;
    }

    /**
     * determine the number of arguments in the string.
     * 
     * @param line - the string containing 0 or more words
     * 
     * @return the number of arguments found
     * 
     * @throws ParserException
     */
    private static int countArgs (String line) throws ParserException {
        line = line.strip();
        if (line.isBlank()) {
            return 0;
        }
        if (line.charAt(0) == '\"' && line.charAt(line.length()-1) == '\"') {
            return 1;
        }

        int count = 1;
        for (int ix = 0; ix < line.length(); ix++) {
            if (line.charAt(ix) != ' ') {
                int offset = line.indexOf(" ", ix);
                if (offset < 0) {
                    break;
                }
                ix = offset;
                count++;
            }
        }
        return count;
    }

    /**
     * extracts the contents within a quoted string.
     * 
     * @param line - the string to extract the data from
     * 
     * @return the string value between the quotes (original string if it is not a quoted string)
     * 
     * @throws ParserException
     */
    private static String extractQuotedString (String line) throws ParserException {
        String functionId = CLASS_NAME + ".extractQuotedString: ";

        if (line == null || line.isBlank() || line.charAt(0) != '\"') {
            return line;
        }
        int offset = line.indexOf('\"', 1);
        if (offset < 0) {
            throw new ParserException(functionId + "Missing ending quote for String");
        }
        return line.substring(1, offset);
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
