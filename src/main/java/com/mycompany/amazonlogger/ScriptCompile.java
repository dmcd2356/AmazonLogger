/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_COMPILE;
import static com.mycompany.amazonlogger.UIFrame.STATUS_ERROR;
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
    
    private static final String CLASS_NAME = ScriptCompile.class.getSimpleName();

    // this handles the command line options via the RUN command
    private final CmdOptions cmdOptionParser;
     

    ScriptCompile() {
        // create an instance of the command options parser for any RUN commands
        cmdOptionParser = new CmdOptions();
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        frame.outputInfoMsg(STATUS_COMPILE, "Compiling file: " + fname);
        ArrayList<CommandStruct> cmdList = new ArrayList<>();
        String line = "";
        int cmdIndex = 0;
        String lineInfo = "";
        CommandStruct cmdStruct;

        // open the file to compile and extract the commands from it
        File scriptFile = Utils.checkFilename (fname, ".scr", Utils.PathType.Test, false);
        FileReader fReader = new FileReader(scriptFile);
        BufferedReader fileReader = new BufferedReader(fReader);

        // clear out the static Variable values
        Variables.initVariables();

        // read the program and compile into ArrayList 'cmdList'
        int lineNum = 0;
        boolean bExit = false;
        while (!bExit && (line = fileReader.readLine()) != null) {
            try {
                lineNum++;
                line = line.strip();
                // check for comment at end of lines
                int comment = line.indexOf("##");
                if (comment > 0) {
                    line = line.substring(0, comment).strip();
                }
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

                // extract the arguments to pass to the command
                frame.outputInfoMsg(STATUS_COMPILE, "PROGIX [" + cmdIndex + "]: " + cmdStruct.command + " " + parmString);
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
                        // we print anything, so no checking
                        break;
                    case DIRECTORY:
                        // verify 1 String argument: directory & 1 optional String -d or -f
                        checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        if (cmdStruct.params.size() > 1) {
                            checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.params);
                            String option = cmdStruct.params.get(1).getStringValue();
                            switch (option) {
                                case "-f", "-d" -> {  }
                                default -> throw new ParserException(functionId + "option is not valid: " + option);
                            }
                        }
                        break;
                    case CD:
                        // verify 1 String argument: directory
                        checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                    case FCREATER:
                        // verify 1 String argument: file name
                        checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                    case FEXISTS:
                        // verify 1 String argument: file name & 1 optional argument String: READABLE / WRITABLE / DIRECTORY
                        checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        if (cmdStruct.params.size() > 1) {
                            checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.params);
                            String option = cmdStruct.params.get(1).getStringValue();
                            switch (option) {
                                case "EXISTS", "READABLE", "WRITABLE", "DIRECTORY" -> {  }
                                default -> throw new ParserException(functionId + "option is not valid: " + option);
                            }
                        }
                        break;
                    case FDELETE:
                        // verify 1 String argument: file name
                        checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                    case FCREATEW:
                        // verify 1 String argument: file name
                        checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                    case FOPENR:
                        // verify 1 String argument: file name
                        checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                    case FOPENW:
                        // verify 1 String argument: file name
                        checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                    case FCLOSE:
                        // verify 1 String argument: file name
                        checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                    case FREAD:
                        // verify 1 optional number of lines to read
                        if (cmdStruct.params.isEmpty()) {
                            // argument is missing, supply the default value
                            ParameterStruct lines = new ParameterStruct("1",
                                                    ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.Unsigned);
                            frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + cmdStruct.params.size() + "]: type Unsigned value: 1");
                            cmdStruct.params.add(lines);
                        } else {
                            checkArgType (0, ParameterStruct.ParamType.Integer, cmdStruct.params);
                            ParameterStruct arg0 = cmdStruct.params.get(0);
                            Long count = arg0.getIntegerValue();
                            if (count < 1) {
                                throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Count value < 1");
                            }
                        }
                        break;
                    case FWRITE:
                        // verify 1 argument: message to write
                        checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                        
                    case OCRSCAN:
                        // verify 1 String argument: file name
                        checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                        
                    case ALLOCATE:
                        // must be a Data Type followed by a List of Variable name entries
                        checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        checkArgType (1, ParameterStruct.ParamType.StrArray, cmdStruct.params);

                        // get the data type first
                        String dataType = cmdStruct.params.get(0).getStringValue();
                        if (ParameterStruct.checkParamType (dataType) == null) {
                            throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : invalid data type: " + dataType);
                        }

                        // this defines the Variable names, and must be done prior to their use.
                        // This Compile method will allocate them, so the Execute does not need
                        //  to do anything with this command.
                        // Multiple Variables can be defined on one line, with the names comma separated.
                        ParameterStruct list = cmdStruct.params.get(1);
                        for (int ix = 0; ix < list.getStrArraySize(); ix++) {
                            String pName = list.getStrArrayElement(ix);
                            Variables.allocateVariable(dataType, pName);
                        }
                        break;
                        
                    case CommandStruct.CommandTable.SET:
                        if (cmdStruct.params.size() < 3) {
                            throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing value to set variable to");
                        }
                        // we pack parameters differently for calculations, so if the param
                        //  is a numeric parameter and it is more than a simple assignment to
                        //  a discrete value or a single parameter reference, let's pepack.
                        // The arguments are: ParamName = Calculation
                        ParameterStruct.ParamType vartype = checkVariableName (0, cmdStruct.params);
                        checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.params);
                        String option = cmdStruct.params.get(1).getStringValue();
                        switch (option) {
                            case "=", "+=", "-=", "*=", "/=", "%=" -> {  }
                            default -> 
                                throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing required '=' following variable name");
                        }
                        if (cmdStruct.params.size() == 3) {
                            // if it's a single arg, verify it is an acceptable type for the variable
                            checkArgType (2, vartype, cmdStruct.params);
                        } else {
                            // else, we must either have a calculation (for numerics or booleans) or a String concatenation
                            switch (vartype) {
                                case ParameterStruct.ParamType.Integer,
                                     ParameterStruct.ParamType.Unsigned,
                                     ParameterStruct.ParamType.Boolean -> {
                                    cmdStruct.params = packCalculation (parmString, vartype);
                                }
                                case ParameterStruct.ParamType.String -> {
                                    cmdStruct.params = packStringConcat (cmdStruct.params, 2);
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
                        // VarName, Data Value
                        // arg 0 should be the Array variable name
                        vartype = checkVariableName (0, cmdStruct.params);
                        // arg 2 is the data value and should be an Integer or String based on Array type
                        //       but we also allow Calculation for IntArray and Concatenation for StrArray
                        switch (vartype) {
                            case IntArray -> {
                                // you can either assign a single numeric value or a Calculation that results in a numeric
                                cmdStruct.params = checkArgIntOrCalc (1, vartype, cmdStruct.params, parmString);
                            }
                            case StrArray -> {
                                // you can either assign a single string value or a list of strings connected with '+' signs
                                cmdStruct.params = checkArgStringOrConcat (1, cmdStruct.params);
                            }
                            default -> {
                                throw new ParserException(functionId + lineInfo + cmdStruct.command + " command not valid for " + vartype + ": " + parmString);
                            }
                        }
                        break;

                    case CommandStruct.CommandTable.MODIFY:
                        // VarName, Index, Data Value
                        // arg 0 should be the Array variable name
                        vartype = checkVariableName (0, cmdStruct.params);
                        // arg 1 is the Array index and should be an Integer or Unsigned
                        checkArgType (1, ParameterStruct.ParamType.Integer, cmdStruct.params);
                        // determine argument type needed for the data for the Array
                        ParameterStruct.ParamType argType;
                        switch (vartype) {
                            case IntArray -> { argType = ParameterStruct.ParamType.Integer;  }
                            case StrArray -> { argType = ParameterStruct.ParamType.String;  }
                            default -> throw new ParserException(functionId + lineInfo + cmdStruct.command + " command not valid for " + vartype + ": " + parmString);
                        }
                        // arg 2 is the data value and should be an Integer or String based on Array type
                        checkArgType (2, argType, cmdStruct.params);
                        break;

                    case CommandStruct.CommandTable.REMOVE:
                        // VarName, Index
                        // arg 0 should be the Array variable name
                        checkVariableName (0, cmdStruct.params);
                        // arg 1 is the Array index and should be an Integer or Unsigned
                        checkArgType (1, ParameterStruct.ParamType.Integer, cmdStruct.params);
                        break;

                    case CommandStruct.CommandTable.TRUNCATE:
                    case CommandStruct.CommandTable.POP:
                        // VarName, Index (optional)
                        // arg 0 should be the Array variable name
                        checkVariableName (0, cmdStruct.params);
                        // arg 1 (OPTIONAL) is the Array index and should be an Integer or Unsigned
                        if (cmdStruct.params.size() == 2) {
                            checkArgType (1, ParameterStruct.ParamType.Integer, cmdStruct.params);
                        }
                        break;

                    case CLEAR:
                        // VarName
                        // arg 0 should be the Array variable name or RESPONSE
                        String varName = cmdStruct.params.get(0).getStringValue();
                        if (! varName.contentEquals("RESPONSE")) {
                            checkVariableName (0, cmdStruct.params);
                        }
                        break;

                    case FILTER:
                        // ARGS: 0 = ParamName or RESET, 1 (optional) the filter string
                        // verify there are the correct number and type of arguments
                        if (cmdStruct.params.size() < 1) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " command requires at least 1 argument : " + parmString);
                        }

                        // if entry was RESET, no more checking to do
                        varName = cmdStruct.params.get(0).getStringValue();
                        if (! varName.contentEquals("RESET")) {
                            // otherwise,
                            // arg 0 should be the Array variable name
                            vartype = checkVariableName (0, cmdStruct.params);
                            // arg 1 (and possibly 2) should be the filter values
                            checkArgFilterValue (1, vartype, cmdStruct.params);
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
                        frame.outputInfoMsg(STATUS_COMPILE, "   - new IF level " + IFStruct.getStackSize() + " Variable " + ifName);
                        break;
                    case CommandStruct.CommandTable.ELSE:
                        if (IFStruct.isIfListEnpty()) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                        }
                        // save the current command index in the current if structure
                        ifInfo = IFStruct.getIfListEntry();
                        ifInfo.setElseIndex(cmdIndex, false, LoopStruct.getStackSize());
                        frame.outputInfoMsg(STATUS_COMPILE, "   - IF level " + IFStruct.getStackSize() + " " + cmdStruct.command + " on line " + cmdIndex);
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
                        frame.outputInfoMsg(STATUS_COMPILE, "   - IF level " + IFStruct.getStackSize() + " " + cmdStruct.command + " on line " + cmdIndex + " Variable " + ifName);
                        break;
                    case CommandStruct.CommandTable.ENDIF:
                        if (IFStruct.isIfStackEnpty()) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                        }
                        // save the current command index in the current if structure
                        ifInfo = IFStruct.getIfListEntry();
                        ifInfo.setEndIfIndex(cmdIndex, LoopStruct.getStackSize());
                        IFStruct.stackPop();
                        frame.outputInfoMsg(STATUS_COMPILE, "   - IF level " + IFStruct.getStackSize() + " " + cmdStruct.command + " on line " + cmdIndex);
                        break;
                    case CommandStruct.CommandTable.FOR:
                        // read the arguments passed
                        // assumed format is: FOR VarName [=] StartIx TO EndIx [STEP StepVal]
                        String loopName, loopStart, loopEnd, strVal;
                        String loopStep = "1";
                        // arg 0 should be the Loop variable name
                        loopName = checkArgType (0, ParameterStruct.ParamType.String,  cmdStruct.params);
                        // check for optional '=' sign and eliminate if found
                        strVal   = checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.params);
                        if (strVal.contentEquals("=")) {
                            cmdStruct.params.remove(1);
                        }
                        boolean bValid = true;
                        if (cmdStruct.params.size() != 4 && cmdStruct.params.size() != 6) {
                            bValid = false;
                        } else {
                            // this checks the required start and end loop index values
                            loopStart = checkArgType (1, ParameterStruct.ParamType.Integer, cmdStruct.params);
                            strVal    = checkArgType (2, ParameterStruct.ParamType.String,  cmdStruct.params);
                            loopEnd   = checkArgType (3, ParameterStruct.ParamType.Integer, cmdStruct.params);
                            if (! strVal.contentEquals("TO")) {
                                bValid = false;
                            }
                            else if (cmdStruct.params.size() == 6) {
                                // this checks the optional loop index step value
                                strVal   = checkArgType (4, ParameterStruct.ParamType.String,  cmdStruct.params);
                                loopStep = checkArgType (5, ParameterStruct.ParamType.Integer, cmdStruct.params);
                                if (! strVal.contentEquals("STEP")) {
                                    bValid = false;
                                }
                            }
                            // create a new loop ID (name + command index) for the entry and add it
                            // to the list of IDs for the loop parameter name
                            LoopId loopId = new LoopId(loopName, cmdIndex);
                            try {
                                LoopStruct loopInfo = new LoopStruct (loopName, loopStart, loopEnd, loopStep,
                                                cmdIndex, IFStruct.getStackSize());
                                LoopParam.saveLoopParameter (loopName, loopId, loopInfo);
                            } catch (ParserException exMsg) {
                                throw new ParserException(exMsg + "\n -> " + functionId + lineInfo + "command " + cmdStruct.command);
                            }

                            // add entry to the current loop stack
                            LoopStruct.pushStack(loopId);
                            frame.outputInfoMsg(STATUS_COMPILE, "   - new FOR Loop level " + LoopStruct.getStackSize() + " Variable " + loopName + " index @ " + cmdIndex);
                        }
                        if (! bValid) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " invalid command format" +
                                    ": should be of form VarName [=] StartIx TO EndIx [STEP StepIx]");
                        }
                        break;
                    case CommandStruct.CommandTable.BREAK:
                        // make sure we are in a FOR ... NEXT loop
                        if (LoopStruct.getStackSize() == 0) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                        }
                        // verify the IF loop level hasn't been exceeded
                        LoopId curLoop = LoopStruct.peekStack();
                        LoopParam.checkLoopIfLevel (cmdStruct.command, IFStruct.getStackSize(), curLoop);
                        break;
                    case CommandStruct.CommandTable.CONTINUE:
                        // make sure we are in a FOR ... NEXT loop
                        if (LoopStruct.getStackSize() == 0) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                        }
                        // verify the IF loop level hasn't been exceeded
                        curLoop = LoopStruct.peekStack();
                        LoopParam.checkLoopIfLevel (cmdStruct.command, IFStruct.getStackSize(), curLoop);
                        break;
                    case CommandStruct.CommandTable.NEXT:
                        // make sure we are in a FOR ... NEXT loop
                        if (LoopStruct.getStackSize() == 0) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                        }
                        
                        // add a token ENDFOR command following the NEXT, so we have a location to go to on exiting loop
                        cmdList.add(cmdStruct); // place the NEXT command here and queue up the ENDFOR command
                        cmdStruct = new CommandStruct(CommandStruct.CommandTable.ENDFOR, lineNum);
                        frame.outputInfoMsg(STATUS_COMPILE, "PROGIX [" + (cmdIndex + 1) + "]: " + cmdStruct.command + " (added to follow NEXT)");
                        // (this will be added at the end of this switch statement)

                        // verify the IF loop level hasn't been exceeded
                        curLoop = LoopStruct.peekStack();
                        LoopParam.checkLoopIfLevel (cmdStruct.command, IFStruct.getStackSize(), curLoop);

                        // set the added ENDFOR command to be the location to go to upon completion
                        curLoop = LoopStruct.peekStack();
                        LoopParam.setLoopEndIndex(cmdList.size(), curLoop);

                        // remove entry from loop stack
                        LoopStruct.popStack();
                        break;
                    case CommandStruct.CommandTable.ENDFOR:
                        // ignore the user entry of this command - we place our own ENDFOR when the NEXT command is found.
                        continue;
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
            } catch (ParserException exMsg) {
                String msg = exMsg + "\n  -> " + functionId + lineInfo + "PROGIX[" + cmdIndex + "]: " + line;
                if (AmazonReader.isRunModeCompileOnly()) {
                    // if only running compiler, just log the messages but don't exit
                    frame.outputInfoMsg(STATUS_ERROR, msg);
                } else {
                    throw new ParserException(msg);
                }
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
        frame.outputInfoMsg(STATUS_COMPILE, "PROGIX [" + cmdIndex + "]: EXIT  (appended)");
        return cmdList;
    }

    /**
     * verifies that the argument data type is valid for the expected type.
     * 
     * @param expType - the expected data type of the argument
     * @param arg     - the argument in question
     * 
     * @throws ParserException 
     */
    private void verifyArgDataType (ParameterStruct.ParamType expType, ParameterStruct arg) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        boolean bValid = true;
        ParameterStruct.ParamType argtype = ParameterStruct.classifyDataType(arg.getStringValue());
        if (! arg.isVariableRef()) {
            // only verify type if not a variable reference, since we allow type conversion
            switch (expType) {
                case Integer:
                case Unsigned:
                    switch (argtype) {
                        // these are not allowed
                        case StrArray, IntArray -> bValid = false;
                    }
                    break;

                case Boolean:
                    switch (argtype) {
                        // these are not allowed
                        case StrArray, IntArray, String -> bValid = false;
                    }
                    break;

                case IntArray:
                    switch (argtype) {
                        // these are not allowed
                        case Boolean -> bValid = false;
                    }
                    break;

                case String:
                case StrArray:
                default:
                    // allow anything
                    break;
            }
            if (! bValid) {
                throw new ParserException(functionId + "Mismatched data type for reference Variable. Expected " + expType + ", got: " + argtype);
            }
        }
    }

    /**
     * checks if the specified argument in the arg list is a valid variable name for assignment to.
     * 
     * @param index    - the index of the argument in the arg list
     * @param parmList - the list of args
     * 
     * @return the data type of the variable
     * 
     * @throws ParserException 
     */    
    private ParameterStruct.ParamType checkVariableName (int index, ArrayList<ParameterStruct> parmList) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (parmList == null) {
            throw new ParserException(functionId + "Null parameter list");
        }
        if (parmList.size() <= index) {
            throw new ParserException(functionId + "Missing arguments in list - number found: " + parmList.size());
        }
        String varName = parmList.get(index).getStringValue();
        if (varName == null || varName.isBlank()) {
            throw new ParserException(functionId + "Variable name is null value");
        }

        // verify the parameter name is valid for assigning a value to
        Variables.checkValidVariable(Variables.VarCheck.SET, varName);
        
        // return the datatype
        ParameterStruct.ParamType vtype = Variables.getVariableTypeFromName(varName);
        return vtype;
    }

    /**
     * checks if the specified argument(s) comply for an Integer or Calculation value.
     * 
     * @param index      - the index of the argument in the arg list
     * @param vartype    - the type of variable it is being assigned to
     * @param parmList   - the list of args
     * @param parmString - the parameter list as a String (for repacking)
     * 
     * @return the parameter list (may be replaced with Calculation params)
     * 
     * @throws ParserException 
     */    
    private ArrayList<ParameterStruct> checkArgIntOrCalc (int index, ParameterStruct.ParamType vartype,
            ArrayList<ParameterStruct> parmList, String parmString) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (parmList == null) {
            throw new ParserException(functionId + "Null parameter list");
        }
        if (parmList.size() <= index) {
            throw new ParserException(functionId + "Missing arguments in list - number found: " + parmList.size());
        }
        if (parmList.size() == index + 1) {
            verifyArgDataType (ParameterStruct.ParamType.Integer, parmList.get(index));
        } else if (vartype != null) {
            // re-pack param list as as calculation entry
            parmList = packCalculation (parmString, vartype);
        } else {
            throw new ParserException(functionId + "Too many arguments in list - number found: " + parmList.size());
        }
        return parmList;
    }

    /**
     * checks if the specified argument(s) comply for a String or String Concatenation value.
     * 
     * @param index      - the index of the argument in the arg list
     * @param parmList   - the list of args
     * 
     * @return the parameter list (may be replaced with Concatenation params)
     * 
     * @throws ParserException 
     */    
    private ArrayList<ParameterStruct> checkArgStringOrConcat (int index, ArrayList<ParameterStruct> parmList) throws ParserException {
         String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (parmList == null) {
            throw new ParserException(functionId + "Null parameter list");
        }
        if (parmList.size() <= index) {
            throw new ParserException(functionId + "Missing arguments in list - number found: " + parmList.size());
        }
        if (parmList.size() == index + 1) {
            verifyArgDataType (ParameterStruct.ParamType.String, parmList.get(index));
        } else {
            parmList = packStringConcat (parmList, 1);
        }
        return parmList;
    }

    /**
     * checks if the specified argument compiles with the specified data type.
     * 
     * @param index      - the index of the argument in the arg list
     * @param exptype    - the type of variable the command is expecting
     * @param parmList   - the list of args
     * 
     * @throws ParserException 
     */    
    private String checkArgType (int index, ParameterStruct.ParamType expType, ArrayList<ParameterStruct> parmList) throws ParserException {
         String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (parmList == null) {
            throw new ParserException(functionId + "Null parameter list");
        }
        if (parmList.size() <= index) {
            throw new ParserException(functionId + "Missing arguments in list - number found: " + parmList.size());
        }
        verifyArgDataType (expType, parmList.get(index));
        return parmList.get(index).getStringValue();
    }

    /**
     * checks if the specified argument compiles with the Array Filter type.
     * 
     * @param index      - the index of the argument in the arg list
     * @param vartype    - the type of variable it is being assigned to (Array types only)
     * @param parmList   - the list of args
     * 
     * @throws ParserException 
     */    
    private void checkArgFilterValue (int index, ParameterStruct.ParamType vartype, ArrayList<ParameterStruct> parmList) throws ParserException {
         String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (parmList == null) {
            throw new ParserException(functionId + "Null parameter list");
        }
        if (parmList.size() <= index) {
            throw new ParserException(functionId + "Missing arguments in list - number found: " + parmList.size());
        }

        ParameterStruct arg = parmList.get(index);
        String argValue = arg.getStringValue();
        switch (vartype) {
            case StrArray:
                // String array just requires whether we are filtering left or right side and if inverting the filter
                if (arg.isVariableRef()) {
                    ParameterStruct.ParamType vtype = Variables.getVariableTypeFromName(argValue);
                    if (vtype == null || (vtype != ParameterStruct.ParamType.String && vtype != ParameterStruct.ParamType.StrArray)) {
                        throw new ParserException(functionId + "Invalid variable type " + vtype + " for " + vartype + " Filter argument: " + argValue);
                    }
                } else {
                    switch (argValue) {
                        case "!":
                        case "!LEFT":
                        case "!RIGHT":
                        case "LEFT":
                        case "RIGHT":
                            break;
                        default:
                            throw new ParserException(functionId + "Invalid Filter argument for " + vartype + " type: " + argValue);
                    }
                }
                break;
            case IntArray:
                // Int Array should have 2 arguments
                if (parmList.size() <= index + 1) {
                    throw new ParserException(functionId + "Missing arguments in list - number found: " + parmList.size());
                }
                
                // this 1st arg is the comparison type to use for filtering
                if (arg.isVariableRef()) {
                    ParameterStruct.ParamType vtype = Variables.getVariableTypeFromName(argValue);
                    if (vtype == null || (vtype != ParameterStruct.ParamType.String && vtype != ParameterStruct.ParamType.StrArray)) {
                        throw new ParserException(functionId + "Invalid Filter argument for " + vartype + " type: " + argValue);
                    }
                } else {
                    switch (argValue) {
                        case "==":
                        case "!=":
                        case ">=":
                        case "<=":
                        case ">":
                        case "<":
                            break;
                        default:
                            throw new ParserException(functionId + "Invalid Filter argument for " + vartype + " type: " + argValue);
                    }
                }
                
                // now verify we have an integer value for the filter data
                checkArgType (index + 1, ParameterStruct.ParamType.Integer, parmList);
                break;
            default:
                throw new ParserException(functionId + "Invalid variable type (must be an Array type): " + vartype);
        }
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

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
                    Variables.checkValidVariable(Variables.VarCheck.SET, nextArg);
                    if (line.isEmpty()) {
                        throw new ParserException(functionId + "no arguments following Variable name: " + nextArg);
                    }
                    // check if this is a String parameter (we may have extra stuff to do here)
                    if (Variables.getVariableTypeFromName(nextArg) == ParameterStruct.ParamType.String) {
                        paramName = "$" + nextArg;
                    }
                } else if (ix == 1 && paramName != null && nextArg.contentEquals("+=")) {
                    // if it is a String parameter and the 2nd entry is a "+=", then we need to
                    //  insert the current parameter value at the begining of the list of Strings to add
                    line = line.strip();
                    nextArg = "=";
                    arg = new ParameterStruct (nextArg, ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.String);
                    frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + paramType + " value: " + nextArg);
                    params.add(arg);
                    arg = new ParameterStruct (paramName, ParameterStruct.ParamClass.Reference, ParameterStruct.ParamType.String);
                    frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + paramType + " value: " + paramName);
                    params.add(arg);
                    nextArg = "+";
                    arg = new ParameterStruct (nextArg, ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.String);
                    frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + paramType + " value: " + nextArg);
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
                    arg = new ParameterStruct (nextArg, ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.StrArray);
                    frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + paramType + " value: [ " + nextArg + " ]");
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
                paramType = Variables.getVariableTypeFromName(nextArg);
            }
            
            arg = new ParameterStruct(nextArg, pClass, paramType);
            frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + paramType + " value: " + nextArg);
            params.add(arg);
        }
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        
        return params;
    }

    /**
     * Converts a packed list of arguments into a simple list of String entries.
     * This takes an already packed list of arguments of the string concatenation
     *  and converts it into a simple list of string entries to be combined.
     *  We start at the end of the already packed string list so we can eliminate
     *  entries without screwing up the indexing.
     * Since the list is assumed to be: { value + value + value ... + value },
     *  we simply start at the last + entry and work to the first, verifying
     *  it is a + sign and eliminating it.
     * 
     * @param params - the list of string arguments having '+' signs separating the values
     * @param offset - the number of arguments in the list that preceed the string arguments
     *                 (such as the name of the param being assigned and the = sign)
     * 
     * @return a list of the strings without the + entries
     */
    private ArrayList<ParameterStruct> packStringConcat (ArrayList<ParameterStruct> params, int offset) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // start at the end of the list with the last + entry and work backwards.
        // The last  + will be the 2nd to the last entry at params.size() - 2.
        // The first + will be the 2nd entry
        for (int ix = params.size() - 2; ix > 0 + offset; ix-=2) {
            String sign = params.get(ix).getStringValue();
            if (sign.contentEquals("+")) {
                params.remove(ix);
            }
            else {
                throw new ParserException(functionId + "Invalid String concatenation");
            }
        }
        return params;
    }
    
    /**
     * This takes a command line and extracts the calculation parameter list from it.
     * For Integers and Unsigneds this will be in the form:
     *      VarName = Calculation     (+=, -=, *=, ... also allowed in place of =)
     * For Booleans, it will be:
     *      VarName = Calculation compSign Calculation  (compSign: ==, !=, >=, ...}
     * For IntArrays and StringArrays it will be:
     *      Command VarName Calculation
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (ptype == ParameterStruct.ParamType.String) {
            throw new ParserException(functionId + "Assignment command not allowed for type: " + ptype);
        }
        
        ArrayList<ParameterStruct> params = new ArrayList<>();
        ParameterStruct parm;
        
        // 1st entry should be the parameter name
        String paramName = getParamName (line);
        try {
            Variables.checkValidVariable(Variables.VarCheck.SET, paramName);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        if (line.contentEquals(paramName)) {
            throw new ParserException(functionId + "no arguments following parameter name: " + line);
        }

        frame.outputInfoMsg(STATUS_COMPILE, "     * Repacking parameters for Calculation");
        
        // the 1st argument of a SET command is the parameter name to assign the value to
        line = line.substring(paramName.length()).strip();
        parm = new ParameterStruct(paramName, ParameterStruct.ParamClass.Reference, ptype);
        frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + paramName);
        params.add(parm);

        // next entry should be the equality sign (except for Arrays)
        switch (ptype) {
            case Integer:
            case Unsigned:
            case Boolean:
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
                frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + newParam + " value: =");
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
                break;
            default:
                break;
        }

        // check if Boolean type, which must have a comparison of 2 calculations
        if (ptype == ParameterStruct.ParamType.Boolean) {
            ArrayList<ParameterStruct> compParams = packComparison(line);
            params.addAll(compParams);
        } else {
            // else, numeric type: remaining data is a single Calculation
            parm = new ParameterStruct(line, ParameterStruct.ParamClass.Calculation, ptype);
            frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + line);
            params.add(parm);
        }
        
        ParameterStruct.showParamTypeList(params);
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        ArrayList<ParameterStruct> params = new ArrayList<>();
        ParameterStruct parm;
        ParameterStruct.ParamType ptype;
        ParameterStruct.ParamClass pclass1;
        ParameterStruct.ParamClass pclass2;
        
        frame.outputInfoMsg(STATUS_COMPILE, "     * Repacking parameters for Comparison");
        
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
            frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + line);
            params.add(parm);
            
            if (bNot) {
                String value = "!";
                pclass2 = ParameterStruct.ParamClass.Discrete;
                ptype = ParameterStruct.ParamType.String;
                parm = new ParameterStruct(value, pclass2, ptype);
                frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + value);
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
            ParameterStruct.ParamType ctype1, ctype2;
            ctype1 = ParameterStruct.classifyDataType(prefix);
            ctype2 = ParameterStruct.classifyDataType(line);
            if (ctype1 == ParameterStruct.ParamType.Integer  ||
                ctype1 == ParameterStruct.ParamType.Unsigned ||
                ctype2 == ParameterStruct.ParamType.Integer  ||
                ctype2 == ParameterStruct.ParamType.Unsigned) {
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
        frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + prefix);
        params.add(parm);
        
            // now add the comparison sign
        parm = new ParameterStruct(compSign, ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.String);
        frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + compSign);
        params.add(parm);
            
        // remaining data is the Calculation, which may be a single value or a complex formula
        parm = new ParameterStruct(line, pclass2, ptype);
        frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + line);
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

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
