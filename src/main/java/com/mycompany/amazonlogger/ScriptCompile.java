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

/**
 *
 * @author dan
 */
public class ScriptCompile {
    
    private static final String CLASS_NAME = ScriptCompile.class.getSimpleName();

    // this handles the command line options via the RUN command
    private final CmdOptions cmdOptionParser;
    // this contains the line numbers corresponding to each command index value
    private static ArrayList<Integer> lineNumbers = new ArrayList<>();
    private int scriptLineLength = 0;
    
    private final ParseScript parseScript = new ParseScript();

    ScriptCompile (Variables preVars) {
        // create an instance of the command options parser for any RUN commands
        cmdOptionParser = new CmdOptions();
        lineNumbers = new ArrayList<>();
    }

    public int getMaxLines () {
        return scriptLineLength;
    }

    public static int getLineNumber (int cmdIx) {
        if (cmdIx >= lineNumbers.size() || cmdIx < 0) {
            return lineNumbers.getLast() + 1;
//            cmdIx = lineNumbers.size() - 1;
        }
        return lineNumbers.get(cmdIx);
    }

    private static void checkNoArgs (CommandStruct.CommandTable command, String strParams) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (! strParams.isEmpty()) {
            throw new ParserException(functionId + "command " + command + " should have no arguments: " + strParams);
        }
    }
    
    /**
     * compiles the external script file (when -f option used) into a series of
     * CommandStruct entities to execute.
     * 
     * @param scriptFile - the script file
     * 
     * @return the list of commands to execute
     * 
     * @throws ParserException
     * @throws IOException 
     */
    public ArrayList<CommandStruct> build (File scriptFile) throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        frame.outputInfoMsg(STATUS_COMPILE, "Compiling file: " + scriptFile.getAbsolutePath());
        ArrayList<CommandStruct> cmdList = new ArrayList<>();
        int cmdIndex = 0;
        String lineInfo = "";
        CommandStruct cmdStruct;

        // open the file to compile and extract the commands from it
        FileReader fReader = new FileReader(scriptFile);
        BufferedReader fileReader = new BufferedReader(fReader);

        // clear out the static Variable values
//        Variables.initVariables();

        // access the Subroutine class to define them
        Subroutine subs = new Subroutine();

        // read the program and compile into ArrayList 'cmdList'
        int lineNum = 0;
        String line;
        boolean bStartup = false;
        while ((line = fileReader.readLine()) != null) {
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
                Subroutine.setCurrentIndex(lineNum);

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
                        VarExtract parmInfo = new VarExtract(line);
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
                
                // THIS ALLOWS A SECTION TO BE CREATED THAT IS ONLY RUN DURING PRE-COMPILE,
                // AND SHOULD ONLY INVOLVE COMMAND OPTIONS THAT WILL SET UP PATHS, ETC.
                if (command == CommandStruct.CommandTable.ENDSTARTUP) {
                    bStartup = false;
                    frame.outputInfoMsg(STATUS_COMPILE, "PROGIX [" + cmdIndex + "] (line " + lineNum + "): " + command + " - STARTUP commands completed");
                    continue;
                }
                if (command == CommandStruct.CommandTable.STARTUP) {
                    bStartup = true;
                    frame.outputInfoMsg(STATUS_COMPILE, "PROGIX [" + cmdIndex + "] (line " + lineNum + "): " + command + " - Ignoring STARTUP commands");
                    continue;
                }
                if (bStartup) {
                    continue;
                }

                // 'parmString' is a string containing the arguments following the command
                // 'cmdStruct'  will receive the command, with the arguments yet to be placed.
                cmdStruct = new CommandStruct(command, lineNum);

                // extract the arguments to pass to the command
                frame.outputInfoMsg(STATUS_COMPILE, "PROGIX [" + cmdIndex + "] (line " + lineNum + "): " + command + " " + parmString);
                boolean bParamAssign = (CommandStruct.CommandTable.SET == command);
                cmdStruct.params = ParseScript.packParameters (parmString, bParamAssign);

                // now let's check for valid command keywords and extract the arguments
                //  into the cmdStruct structure.
                switch (command) {
                    case EXIT:
                        checkNoArgs(command, parmString);
                        break;
                    case ENDMAIN:
                        checkNoArgs(command, parmString);
                        break;
                    case SUB:
                        // verify 1 String argument: name of subroutine
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        String subName = ParseScript.checkArgTypeString (0, cmdStruct.params);
                        subs.compileSubStartCmdIx(subName, cmdIndex);
                        break;
                    case ENDSUB:
                        checkNoArgs(command, parmString);
                        break;
                    case GOSUB:
                        // verify 1 String argument: name of subroutine (and optionally a list of various args)
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        subName = ParseScript.checkArgTypeString (0, cmdStruct.params);
                        subs.compileSubGosub (subName);
                        break;
                    case RETURN:
                        // optional String argument returned
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        if (!cmdStruct.params.isEmpty()) {
                            ParseScript.checkArgTypeString (0, cmdStruct.params);
                        }
                        subs.compileSubReturn ();
                        break;
                    case PRINT:
                        // if no params, we will print a blank line
                        // if more than 1 parameter, check for a quoted string or concatenated string
                        ParseScript.showPackedParams(cmdStruct.params);
                        if (cmdStruct.params.size() > 1) {
                            cmdStruct.params = parseScript.packStringConcat (cmdStruct.params, 2);
                        }
                        break;
                    case DIRECTORY:
                        // verify 1 String argument: directory & 1 optional String -d or -f
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        if (cmdStruct.params.size() == 1) {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                            ParameterStruct newParm = new ParameterStruct ("");
                            cmdStruct.params.add(0, newParm); // insert default value as empty string
                        } else {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                            ParseScript.checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.params);
                            String option = cmdStruct.params.get(0).getStringValue();
                            switch (option) {
                                case "-f", "-d" -> {  }
                                default -> throw new ParserException(functionId + "option is not valid: " + option);
                            }
                        }
                        break;
                    case CD:
                        // verify 1 String argument: directory
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                    case FEXISTS:
                        // verify 1 optional argument String: type & 1 required String argument: file name
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        if (cmdStruct.params.size() == 1) {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                            ParameterStruct newParm = new ParameterStruct ("-x");
                            cmdStruct.params.add(0, newParm); // insert default value "-x"
                        } else {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                            ParseScript.checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.params);
                            String option = cmdStruct.params.get(0).getStringValue();
                            switch (option) {
                                case "-x", "-r", "-w", "-d" -> {  }
                                default -> throw new ParserException(functionId + "option is not valid: " + option);
                            }
                        }
                        break;
                    case MKDIR:
                        // verify 1 String argument: dir name
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                    case RMDIR:
                        // verify 1 String argument: dir name & 1 optional argument String: FORCE (if dir is not empty)
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        if (cmdStruct.params.size() == 1) {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                            ParameterStruct newParm = new ParameterStruct (" ");
                            cmdStruct.params.add(0, newParm); // insert default value "-x"
                        } else {
                            ParseScript.checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.params);
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                            String option = cmdStruct.params.get(0).getStringValue();
                            if (! option.contentEquals("-f")) {
                                throw new ParserException(functionId + "option is not valid: " + option);
                            }
                        }
                        break;
                    case FDELETE:
                        // verify 1 String argument: file name
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                    case FCREATE:
                        // verify 1 optional argument String: type & 1 required String argument: file name
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        if (cmdStruct.params.size() == 1) {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                            ParameterStruct newParm = new ParameterStruct ("-r");
                            cmdStruct.params.add(0, newParm); // insert default value "-r"
                        } else {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                            ParseScript.checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.params);
                            String option = cmdStruct.params.get(0).getStringValue();
                            switch (option) {
                                case "-r", "-w" -> {  }
                                default -> throw new ParserException(functionId + "option is not valid: " + option);
                            }
                        }
                        break;
                    case FOPEN:
                        // verify 1 optional argument String: type & 1 required String argument: file name
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        if (cmdStruct.params.size() == 1) {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                            ParameterStruct newParm = new ParameterStruct ("-r");
                            cmdStruct.params.add(0, newParm); // insert default value "-r"
                        } else {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                            ParseScript.checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.params);
                            String option = cmdStruct.params.get(0).getStringValue();
                            switch (option) {
                                case "-r", "-w" -> {  }
                                default -> throw new ParserException(functionId + "option is not valid: " + option);
                            }
                        }
                        break;
                    case FCLOSE:
                        // verify 1 String argument: file name
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                    case FGETSIZE:
                        // verify 1 String argument: file name
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                    case FGETLINES:
                        // verify 1 String argument: file name
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                    case FREAD:
                        // verify 1 optional number of lines to read
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        if (! cmdStruct.params.isEmpty()) {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.Integer, cmdStruct.params);
                            ParameterStruct arg0 = cmdStruct.params.get(0);
                            Long count = arg0.getIntegerValue();
                            if (count < 1) {
                                throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Count value < 1");
                            }
                        }
                        break;
                    case FWRITE:
                        // verify 1 argument: message to write
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                        
                    case OCRSCAN:
                        // verify 1 String argument: file name
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.params);
                        break;
                        
                    case ALLOCATE:
                        // This is handled during the Pre-Compile, so nothing to do here!
                        break;
                        
                    case SET:
                        if (cmdStruct.params.size() < 3) {
                            ParseScript.showPackedParams(cmdStruct.params);
                            throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing value to set variable to");
                        }
                        // we pack parameters differently for calculations, so if the param
                        //  is a numeric parameter and it is more than a simple assignment to
                        //  a discrete value or a single parameter reference, let's pepack.
                        // The arguments are: ParamName = Calculation
                        ParameterStruct.ParamType vartype = ParseScript.checkVariableName (0, cmdStruct.params);
                        ParseScript.checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.params);
                        String option = cmdStruct.params.get(1).getStringValue();
                        switch (option) {
                            case "=", "+=", "-=", "*=", "/=", "%=" -> {  }
                            default -> {
                                ParseScript.showPackedParams(cmdStruct.params);
                                throw new ParserException(functionId + lineInfo + "command " + cmdStruct.command + " : Missing required '=' following variable name");
                            }
                        }
                        if (cmdStruct.params.size() == 3 && option.contentEquals("=")) {
                            // if it's a single arg, verify it is an acceptable type for the variable
                            ParseScript.showPackedParams(cmdStruct.params);
                            ParseScript.checkArgType (2, vartype, cmdStruct.params);
                        } else {
                            // else, we must either have a calculation (for numerics or booleans) or a String concatenation
                            switch (vartype) {
                                case ParameterStruct.ParamType.Integer,
                                     ParameterStruct.ParamType.Unsigned,
                                     ParameterStruct.ParamType.Boolean -> {
                                    cmdStruct.params = parseScript.packCalculation (parmString, vartype);
                                }
                                case ParameterStruct.ParamType.String -> {
                                    cmdStruct.params = parseScript.packStringConcat (cmdStruct.params, 2);
                                }
                                default -> {
                                    // Strings are handled in the execution phase
                                    // Arrays are not allowed to have any operations, just simple assignments
                                    ParseScript.showPackedParams(cmdStruct.params);
                                }
                            }
                        }
                        break;

                    // these are the Array-only commands
                    case INSERT:
                    case APPEND:
                        ParseScript.showPackedParams(cmdStruct.params);
                        // VarName, Data Value
                        // arg 0 should be the Array variable name
                        vartype = ParseScript.checkVariableName (0, cmdStruct.params);
                        // arg 1 is the data value and should be an Integer or String based on Array type
                        //       but we also allow Calculation for IntArray and Concatenation for StrArray
                        switch (vartype) {
                            case IntArray -> {
                                // you can either assign a single numeric value or a Calculation that results in a numeric
                                cmdStruct.params = parseScript.checkArgIntOrCalc (1, vartype, cmdStruct.params, parmString);
                            }
                            case StrArray -> {
                                // you can either assign a single string value or a list of strings connected with '+' signs
                                cmdStruct.params = parseScript.checkArgStringOrConcat (1, cmdStruct.params);
                            }
                            default -> {
                                throw new ParserException(functionId + lineInfo + cmdStruct.command + " command not valid for " + vartype + ": " + parmString);
                            }
                        }
                        break;

                    case MODIFY:
                        // VarName, Index, Data Value
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(3, cmdStruct);
                        // arg 0 should be the Array variable name
                        vartype = ParseScript.checkVariableName (0, cmdStruct.params);
                        // arg 1 is the Array index and should be an Integer or Unsigned
                        ParseScript.checkArgType (1, ParameterStruct.ParamType.Integer, cmdStruct.params);
                        // determine argument type needed for the data for the Array
                        ParameterStruct.ParamType argType;
                        switch (vartype) {
                            case IntArray -> { argType = ParameterStruct.ParamType.Integer;  }
                            case StrArray -> { argType = ParameterStruct.ParamType.String;  }
                            default -> throw new ParserException(functionId + lineInfo + cmdStruct.command + " command not valid for " + vartype + ": " + parmString);
                        }
                        // arg 2 is the data value and should be an Integer or String based on Array type
                        ParseScript.checkArgType (2, argType, cmdStruct.params);
                        break;

                    case REMOVE:
                        // VarName, Index
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        // arg 0 should be the Array variable name
                        ParseScript.checkVariableName (0, cmdStruct.params);
                        // arg 1 is the Array index and should be an Integer or Unsigned
                        ParseScript.checkArgType (1, ParameterStruct.ParamType.Integer, cmdStruct.params);
                        break;

                    case TRUNCATE:
                    case POP:
                        // VarName, Index (optional)
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        // arg 0 should be the Array variable name
                        ParseScript.checkVariableName (0, cmdStruct.params);
                        // arg 1 (OPTIONAL) is the Array index and should be an Integer or Unsigned
                        if (cmdStruct.params.size() == 2) {
                            ParseScript.checkArgType (1, ParameterStruct.ParamType.Integer, cmdStruct.params);
                        }
                        break;

                    case CLEAR:
                        // VarName
                        // arg 0 should be the Array variable name or RESPONSE
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        String varName = cmdStruct.params.get(0).getStringValue();
                        if (! varName.contentEquals("RESPONSE")) {
                            ParseScript.checkVariableName (0, cmdStruct.params);
                        }
                        break;

                    case FILTER:
                        // ARGS: 0 = ParamName or RESET, 1 (optional) the filter string
                        // verify there are the correct number and type of arguments
                        ParseScript.showPackedParams(cmdStruct.params);
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        if (cmdStruct.params.size() < 1) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " command requires at least 1 argument : " + parmString);
                        }

                        // if entry was RESET, no more checking to do
                        varName = cmdStruct.params.get(0).getStringValue();
                        if (! varName.contentEquals("RESET")) {
                            // otherwise,
                            // arg 0 should be the Array variable name
                            vartype = ParseScript.checkVariableName (0, cmdStruct.params);
                            // arg 1 (and possibly 2) should be the filter values
                            parseScript.checkArgFilterValue (1, vartype, cmdStruct.params);
                        }
                        break;

                    case IF:
                        // verify number and type of arguments
                        cmdStruct.params = parseScript.packComparison (parmString);

                        // read the arguments passed
                        // assumed format is: IF Name1 >= Name2  (where Names can be Integers, Strings or Variables)
                        String ifName = cmdStruct.params.get(0).getStringValue();

                        // add entry to the current loop stack
                        IFStruct ifInfo;
                        String sname = Subroutine.getSubName();
                        ifInfo = new IFStruct (cmdIndex, LoopStruct.getStackSize(), sname);
                        IFStruct.ifListPush(ifInfo);
                        IFStruct.stackPush(cmdIndex);
                        frame.outputInfoMsg(STATUS_COMPILE, "   - new IF level " + IFStruct.getStackSize() + " Variable " + ifName);
                        break;
                    case ELSE:
                        checkNoArgs(command, parmString);
                        if (IFStruct.isIfListEnpty()) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                        }
                        // save the current command index in the current if structure
                        ifInfo = IFStruct.getIfListEntry();
                        if (! ifInfo.isSameSubroutine(Subroutine.getSubName())) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " was outside subroutine of matching IF statement");
                        }
                        ifInfo.setElseIndex(cmdIndex, false, LoopStruct.getStackSize());
                        frame.outputInfoMsg(STATUS_COMPILE, "   - IF level " + IFStruct.getStackSize() + " " + cmdStruct.command + " on line " + cmdIndex);
                        break;
                    case ELSEIF:
                        if (IFStruct.isIfStackEnpty()) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                        }

                        // read the arguments passed
                        // assumed format is: IF Name1 >= Name2  (where Names can be Integers, Strings or Variables)
                        cmdStruct.params = parseScript.packComparison (parmString);
                        ifName = cmdStruct.params.get(0).getStringValue();

                        // save the current command index in the current if structure
                        ifInfo = IFStruct.getIfListEntry();
                        if (! ifInfo.isSameSubroutine(Subroutine.getSubName())) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " was outside subroutine of matching IF statement");
                        }
                        ifInfo.setElseIndex(cmdIndex, true, LoopStruct.getStackSize());
                        frame.outputInfoMsg(STATUS_COMPILE, "   - IF level " + IFStruct.getStackSize() + " " + cmdStruct.command + " on line " + cmdIndex + " Variable " + ifName);
                        break;
                    case ENDIF:
                        checkNoArgs(command, parmString);
                        if (IFStruct.isIfStackEnpty()) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                        }
                        // save the current command index in the current if structure
                        ifInfo = IFStruct.getIfListEntry();
                        if (! ifInfo.isSameSubroutine(Subroutine.getSubName())) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " was outside subroutine of matching IF statement");
                        }
                        ifInfo.setEndIfIndex(cmdIndex, LoopStruct.getStackSize());
                        IFStruct.stackPop();
                        frame.outputInfoMsg(STATUS_COMPILE, "   - IF level " + IFStruct.getStackSize() + " " + cmdStruct.command + " on line " + cmdIndex);
                        break;
                    case FOR:
                        // assumed format is: FOR VarName [=] StartIx TO EndIx [STEP StepVal]
                        // the other format is: "FOR EVER" - check for single argument of "EVER"
                        ParseScript.showPackedParams(cmdStruct.params);
                        String loopName, strVal;
                        ParameterStruct loopStart = null;
                        ParameterStruct loopEnd   = null;
                        ParameterStruct loopStep  = null;
                        boolean bInclEnd = false;
                        ParameterStruct nameParam = cmdStruct.params.get(0);
                        ParseScript.checkMinArgs(1, cmdStruct);
                        if (cmdStruct.params.size() <= 2 && nameParam.getStringValue().contentEquals("EVER")) {
                            Integer maxLoops = null;
                            if (cmdStruct.params.size() == 2) {
                                maxLoops = cmdStruct.params.get(1).getIntegerValue().intValue();
                            }
                            // create loop forever structure
                            try {
                                LoopStruct loopInfo = new LoopStruct (maxLoops, cmdIndex, IFStruct.getStackSize());
                                LoopParam.saveLoopParameter (loopInfo);
                                String newLoopName = loopInfo.getLoopName(); // the name gets changed to make it unique from user defined names
                                nameParam.setStringValue(newLoopName);
                                frame.outputInfoMsg(STATUS_COMPILE, "   - new FOR EVER Loop level " + LoopStruct.getStackSize() +
                                        " " + newLoopName + " index @ " + cmdIndex);
                            } catch (ParserException exMsg) {
                                throw new ParserException(exMsg + "\n -> " + functionId + lineInfo + "command " + cmdStruct.command);
                            }
                        } else {
                            // verify the arguments passed
                            ParseScript.checkMaxArgs(7, cmdStruct);
                            ParseScript.checkMinArgs(5, cmdStruct);
                            // arg 0 should be the Loop variable name
                            loopName = ParseScript.checkArgTypeString (0, cmdStruct.params);
                            // check for optional '=' sign and eliminate if found
                            strVal   = ParseScript.checkArgTypeString (1, cmdStruct.params);
                            if (strVal.contentEquals("=")) {
                                cmdStruct.params.remove(1);
                            }
                            boolean bValid = true;
                            if (cmdStruct.params.size() != 4 && cmdStruct.params.size() != 6) {
                                bValid = false;
                            } else {
                                // this checks the required start and end loop index values
                                loopStart = cmdStruct.params.get(1);
                                strVal    = ParseScript.checkArgTypeString  (2, cmdStruct.params);
                                loopEnd   = cmdStruct.params.get(3);
                                if (strVal.contentEquals("TO")) {
                                    bInclEnd = true;
                                }
                                if (! strVal.contentEquals("TO") && ! strVal.contentEquals("UPTO")) {
                                    bValid = false;
                                }
                                if (cmdStruct.params.size() == 6) {
                                    // this checks the optional loop index step value
                                    strVal   = ParseScript.checkArgTypeString  (4, cmdStruct.params);
                                    loopStep = cmdStruct.params.get(5);
                                    if (! strVal.contentEquals("STEP")) {
                                        bValid = false;
                                    }
                                }
                            }
                            if (! bValid) {
                                throw new ParserException(functionId + lineInfo + cmdStruct.command + " invalid command format" +
                                        ": should be of form VarName [=] StartIx TO | UPTO EndIx [STEP StepIx]");
                            }

                            // create a new loop ID (name + command index) for the entry and add it
                            // to the list of IDs for the loop parameter name
                            try {
                                LoopStruct loopInfo = new LoopStruct (loopName, loopStart, loopEnd, loopStep,
                                                bInclEnd, cmdIndex, IFStruct.getStackSize());
                                LoopParam.saveLoopParameter (loopInfo);
                            } catch (ParserException exMsg) {
                                throw new ParserException(exMsg + "\n -> " + functionId + lineInfo + "command " + cmdStruct.command);
                            }
                            frame.outputInfoMsg(STATUS_COMPILE, "   - new FOR Loop level " + LoopStruct.getStackSize() + " Variable " + loopName + " index @ " + cmdIndex);
                        }
                        break;
                    case BREAK:
                        checkNoArgs(command, parmString);
                        // make sure we are in a FOR ... NEXT loop
                        if (LoopStruct.getStackSize() == 0) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                        }
                        break;
                    case SKIP:
                        checkNoArgs(command, parmString);
                        // make sure we are in a FOR ... NEXT loop
                        if (LoopStruct.getStackSize() == 0) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                        }
                        break;
                    case BREAKIF:
                        // verify number and type of arguments
                        cmdStruct.params = parseScript.packComparison (parmString);
                        // make sure we are in a FOR ... NEXT loop
                        if (LoopStruct.getStackSize() == 0) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                        }
                        break;
                    case SKIPIF:
                        // verify number and type of arguments
                        cmdStruct.params = parseScript.packComparison (parmString);
                        // make sure we are in a FOR ... NEXT loop
                        if (LoopStruct.getStackSize() == 0) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                        }
                        break;
                    case NEXT:
                        checkNoArgs(command, parmString);
                        // make sure we are in a FOR ... NEXT loop
                        if (LoopStruct.getStackSize() == 0) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                        }
                        
                        // add a token ENDFOR command following the NEXT, so we have a location to go to on exiting loop
                        lineNumbers.add(lineNum);
                        cmdList.add(cmdStruct); // place the NEXT command here and queue up the ENDFOR command
                        cmdStruct = new CommandStruct(CommandStruct.CommandTable.ENDFOR, lineNum);
                        frame.outputInfoMsg(STATUS_COMPILE, "PROGIX [" + (cmdIndex + 1) + "]: " + cmdStruct.command + " (added to follow NEXT)");
                        // (this will be added at the end of this switch statement)

                        // verify the IF loop level hasn't been exceeded
                        LoopId curLoop = LoopStruct.peekStack();
                        LoopParam.checkLoopIfLevel (cmdStruct.command.toString(), IFStruct.getStackSize(), curLoop);

                        // set the added ENDFOR command to be the location to go to upon completion
                        curLoop = LoopStruct.peekStack();
                        LoopParam.setLoopEndIndex(cmdList.size(), curLoop);

                        // remove entry from loop stack
                        LoopStruct.popStack();
                        break;
                    case ENDFOR:
                        checkNoArgs(command, parmString);
                        // ignore the user entry of this command - we place our own ENDFOR when the NEXT command is found.
                        continue;
                    case RUN:
                        // verify the option command and its parameters
                        // NOTE: when we place the command in cmdStruct, we remove the RUN label,
                        //       so executeProgramCommand does not need to check for it.
                        ParseScript.showPackedParams(cmdStruct.params);
                        cmdStruct.option = cmdStruct.params.getFirst().getStringValue();
                        cmdStruct.params.removeFirst();
                        cmdOptionParser.checkCmdOptions(cmdStruct.option, cmdStruct.params, lineNum);
                        break;

                    default:
                        throw new ParserException(functionId + lineInfo + "Unknown command: " + cmdStruct.command);
                }

                // all good, add command to list
                ParameterStruct.showParamTypeList(cmdStruct.params);
                lineNumbers.add(lineNum);
                cmdList.add(cmdStruct);
                
            } catch (ParserException exMsg) {
                String errMsg = exMsg + "\n  -> " + functionId + lineInfo + "PROGIX[" + cmdIndex + "]: " + line;
                if (AmazonReader.isRunModeCompileOnly()) {
                    // if only running compiler, just log the messages but don't exit
                    frame.outputInfoMsg(STATUS_ERROR, errMsg);
                } else {
                    throw new ParserException(errMsg);
                }
            }
        }  // end of while loop
        
        int loopSize = LoopStruct.getStackSize();
        if (loopSize != 0) {
            throw new ParserException(functionId + "FOR loop not complete for " + loopSize + " entries");
        }
        if (!IFStruct.isIfStackEnpty() && !IFStruct.getIfListEntry().isValid()) {
            throw new ParserException(functionId + "Last IF has no matching ENDIF");
        }
        Subroutine.checkSubroutineMissing();

        scriptLineLength = lineNum;
        fileReader.close();
        
//        // the last line will be the one to end the program flow
//        cmdList.add(new CommandStruct(CommandStruct.CommandTable.EXIT, lineNum));
//        frame.outputInfoMsg(STATUS_COMPILE, "PROGIX [" + cmdIndex + "]: EXIT  (appended)");
        return cmdList;
    }

}
