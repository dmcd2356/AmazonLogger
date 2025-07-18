/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import com.dmcd.amazonlogger.GUILogPanel.MsgType;
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
    private static ArrayList<CommandStruct> cmdList = null;
    private static int scriptLineLength = 0;
    private static String compiledFilename = "";
    
    private final ParseScript parseScript = new ParseScript();

    ScriptCompile () {
        // create an instance of the command options parser for any RUN commands
        cmdOptionParser = new CmdOptions();
        lineNumbers = new ArrayList<>();
    }

    public static int getMaxLines () {
        return scriptLineLength;
    }

    public static String getFilename() {
        return compiledFilename;
    }
    
    public static Integer getCompiledSize() {
        if (cmdList == null) {
            return -1;
        }
        return cmdList.size();
    }
    
    public static CommandStruct getExecCommand(int cmdIx) {
        return cmdList.get(cmdIx);
    }
    
    /**
     * converts a command index to the script line number it represents.
     * 
     * @param cmdIx - the command index value
     * 
     * @return the corresponding line number (9999 if invalid index)
     */
    public static int getLineNumber (int cmdIx) {
        if (cmdIx >= lineNumbers.size() || cmdIx < 0) {
            return ScriptThread.getEndOfFileID();
        }
        return lineNumbers.get(cmdIx);
    }

    /**
     * gets the first valid line number for the script.
     * 
     * @return the starting line number
     */
    public static int getMinLineNumber () {
        return lineNumbers.getFirst();
    }

    /**
     * gets the last valid line number for the script.
     * 
     * @return the last line number
     */
    public static int getMaxLineNumber () {
        return lineNumbers.getLast();
    }

    /**
     * converts a line number to the corresponding command index,
     * 
     * @param lineNum - the script line number
     * 
     * @return the corresponding command index value if found
     */
    public static int getCommandIndex (int lineNum) {
        for (int ix = 0; ix < lineNumbers.size(); ix++) {
            if (lineNumbers.get(ix) == lineNum) {
                return ix;
            }
            if (lineNumbers.get(ix) > lineNum) {
                break;
            }
        }
        return ScriptThread.getEndOfFileID();
    }

    private static void checkNoArgs (CommandStruct.CommandTable command, String strParams) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (! strParams.isEmpty()) {
            throw new ParserException(functionId + "command " + command + " should have no arguments: " + strParams);
        }
    }
    
    /**
     * compiles the external script file into an array of CommandStruct entities to execute.
     * 
     * @param scriptFile - the script file
     * 
     * @throws ParserException
     * @throws IOException 
     */
    public void build (File scriptFile) throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        compiledFilename = scriptFile.getAbsolutePath();
        GUILogPanel.outputInfoMsg(MsgType.COMPILE, "Compiling file: " + compiledFilename);
        cmdList = new ArrayList<>();
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
                    GUILogPanel.outputInfoMsg(MsgType.COMPILE, "PROGIX [" + cmdIndex + "] (line " + lineNum + "): " + command + " - STARTUP commands completed");
                    continue;
                }
                if (command == CommandStruct.CommandTable.STARTUP) {
                    bStartup = true;
                    GUILogPanel.outputInfoMsg(MsgType.COMPILE, "PROGIX [" + cmdIndex + "] (line " + lineNum + "): " + command + " - Ignoring STARTUP commands");
                    continue;
                }
                if (bStartup) {
                    continue;
                }

                // 'parmString' is a string containing the arguments following the command
                // 'cmdStruct'  will receive the command, with the arguments yet to be placed.
                cmdStruct = new CommandStruct(command, lineNum);

                // skip the ALLOCATE command since it has already been handled by the PreCompiler
                if (command == CommandStruct.CommandTable.ALLOCATE) {
                    continue;
                }
                
                // extract the arguments to pass to the command
                GUILogPanel.outputInfoMsg(MsgType.COMPILE, "PROGIX [" + cmdIndex + "] (line " + lineNum + "): " + command + " " + parmString);
                boolean bParamAssign = (CommandStruct.CommandTable.SET == command);
                ArrayList<ParameterStruct> list = ParseScript.packParameters (parmString, bParamAssign);
                cmdStruct.setParamList(list);

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
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        String subName = ParseScript.checkArgTypeString (0, cmdStruct.getParamList());
                        subs.compileSubStartCmdIx(subName, cmdIndex);
                        break;
                    case ENDSUB:
                        checkNoArgs(command, parmString);
                        break;
                    case GOSUB:
                        // verify 1 String argument: name of subroutine (and optionally a list of various args)
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        subName = ParseScript.checkArgTypeString (0, cmdStruct.getParamList());
                        subs.compileSubGosub (subName);
                        break;
                    case RETURN:
                        // optional String argument returned
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        if (!cmdStruct.isParamEmpty()) {
                            ParseScript.checkArgTypeString (0, cmdStruct.getParamList());
                        }
                        subs.compileSubReturn ();
                        break;
                    case PRINT:
                        // if no params, we will print a blank line
                        // if more than 1 parameter, check for a quoted string or concatenated string
                        cmdStruct.showParams();
                        if (cmdStruct.getParamSize() > 1) {
                            list = parseScript.packStringConcat (cmdStruct.getParamList(), 2);
                            cmdStruct.setParamList(list);
                        }
                        break;
                    case DIRECTORY:
                        // verify 1 String argument: directory & 1 optional String -d or -f
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        if (cmdStruct.getParamSize() == 1) {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            ParameterStruct newParm = new ParameterStruct ("");
                            cmdStruct.insertParamEntry(newParm); // insert default value as empty string
                        } else {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            ParseScript.checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            String option = cmdStruct.getParamEntry(0).getStringValue();
                            switch (option) {
                                case "-f", "-d" -> {  }
                                default -> throw new ParserException(functionId + "option is not valid: " + option);
                            }
                        }
                        break;
                    case CD:
                        // verify 1 String argument: directory
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                        break;
                    case FEXISTS:
                        // verify 1 optional argument String: type & 1 required String argument: file name
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        if (cmdStruct.getParamSize() == 1) {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            ParameterStruct newParm = new ParameterStruct ("-x");
                            cmdStruct.insertParamEntry(newParm); // insert default value "-x"
                        } else {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            ParseScript.checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            String option = cmdStruct.getParamEntry(0).getStringValue();
                            switch (option) {
                                case "-x", "-r", "-w", "-d" -> {  }
                                default -> throw new ParserException(functionId + "option is not valid: " + option);
                            }
                        }
                        break;
                    case MKDIR:
                        // verify 1 String argument: dir name
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                        break;
                    case RMDIR:
                        // verify 1 String argument: dir name & 1 optional argument String: FORCE (if dir is not empty)
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        if (cmdStruct.getParamSize() == 1) {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            ParameterStruct newParm = new ParameterStruct (" ");
                            cmdStruct.insertParamEntry(newParm); // insert default value "-x"
                        } else {
                            ParseScript.checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            String option = cmdStruct.getParamEntry(0).getStringValue();
                            if (! option.contentEquals("-f")) {
                                throw new ParserException(functionId + "option is not valid: " + option);
                            }
                        }
                        break;
                    case FDELETE:
                        // verify 1 String argument: file name
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                        break;
                    case FCREATE:
                        // verify 1 optional argument String: type & 1 required String argument: file name
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        if (cmdStruct.getParamSize() == 1) {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            ParameterStruct newParm = new ParameterStruct ("-r");
                            cmdStruct.insertParamEntry(newParm); // insert default value "-r"
                        } else {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            ParseScript.checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            String option = cmdStruct.getParamEntry(0).getStringValue();
                            switch (option) {
                                case "-r", "-w" -> {  }
                                default -> throw new ParserException(functionId + "option is not valid: " + option);
                            }
                        }
                        break;
                    case FOPEN:
                        // verify 1 optional argument String: type & 1 required String argument: file name
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        if (cmdStruct.getParamSize() == 1) {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            ParameterStruct newParm = new ParameterStruct ("-r");
                            cmdStruct.insertParamEntry(newParm); // insert default value "-r"
                        } else {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            ParseScript.checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                            String option = cmdStruct.getParamEntry(0).getStringValue();
                            switch (option) {
                                case "-r", "-w" -> {  }
                                default -> throw new ParserException(functionId + "option is not valid: " + option);
                            }
                        }
                        break;
                    case FCLOSE:
                        // verify 1 String argument: file name
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                        break;
                    case FGETSIZE:
                        // verify 1 String argument: file name
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                        break;
                    case FGETLINES:
                        // verify 1 String argument: file name
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                        break;
                    case FREAD:
                        // verify 1 optional number of lines to read
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        if (! cmdStruct.isParamEmpty()) {
                            ParseScript.checkArgType (0, ParameterStruct.ParamType.Integer, cmdStruct.getParamList());
                            ParameterStruct arg0 = cmdStruct.getParamEntry(0);
                            Long count = arg0.getIntegerValue();
                            if (count < 1) {
                                throw new ParserException(functionId + lineInfo + "command " + cmdStruct.getCommand() + " : Count value < 1");
                            }
                        }
                        break;
                    case FWRITE:
                        // verify 1 argument: message to write
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                        break;
                        
                    case OCRSCAN:
                        // verify 1 String argument: file name
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        ParseScript.checkArgType (0, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                        break;
                        
                    case ALLOCATE:
                        // This is handled during the Pre-Compile, so nothing to do here!
                        break;
                        
                    case SET:
                        if (cmdStruct.getParamSize() < 3) {
                            cmdStruct.showParams();
                            throw new ParserException(functionId + lineInfo + "command " + cmdStruct.getCommand() + " : Missing value to set variable to");
                        }
                        // we pack parameters differently for calculations, so if the param
                        //  is a numeric parameter and it is more than a simple assignment to
                        //  a discrete value or a single parameter reference, let's pepack.
                        // The arguments are: ParamName = Calculation
                        ParameterStruct.ParamType vartype = ParseScript.checkVariableName (0, cmdStruct.getParamList());
                        ParseScript.checkArgType (1, ParameterStruct.ParamType.String, cmdStruct.getParamList());
                        String option = cmdStruct.getParamEntry(1).getStringValue();
                        switch (option) {
                            case "=", "+=", "-=", "*=", "/=", "%=" -> {  }
                            default -> {
                                cmdStruct.showParams();
                                throw new ParserException(functionId + lineInfo + "command " + cmdStruct.getCommand() + " : Missing required '=' following variable name");
                            }
                        }
                        if (cmdStruct.getParamSize() == 3 && option.contentEquals("=")) {
                            // if it's a single arg, verify it is an acceptable type for the variable
                            cmdStruct.showParams();
                            ParseScript.checkArgType (2, vartype, cmdStruct.getParamList());
                        } else {
                            // else, we must either have a calculation (for numerics or booleans) or a String concatenation
                            switch (vartype) {
                                case ParameterStruct.ParamType.Integer,
                                     ParameterStruct.ParamType.Unsigned,
                                     ParameterStruct.ParamType.Boolean -> {
                                    list = parseScript.packCalculation (parmString, vartype);
                                    cmdStruct.setParamList(list);
                                }
                                case ParameterStruct.ParamType.String -> {
                                    list = parseScript.packStringConcat (cmdStruct.getParamList(), 2);
                                    cmdStruct.setParamList(list);
                                }
                                default -> {
                                    // Strings are handled in the execution phase
                                    // Arrays are not allowed to have any operations, just simple assignments
                                    cmdStruct.showParams();
                                }
                            }
                        }
                        break;

                    // these are the Array-only commands
                    case INSERT:
                    case APPEND:
                        cmdStruct.showParams();
                        // VarName, Data Value
                        // arg 0 should be the Array variable name
                        vartype = ParseScript.checkVariableName (0, cmdStruct.getParamList());
                        // arg 1 is the data value and should be an Integer or String based on Array type
                        //       but we also allow Calculation for IntArray and Concatenation for StrArray
                        switch (vartype) {
                            case IntArray -> {
                                // you can either assign a single numeric value or a Calculation that results in a numeric
                                list = parseScript.checkArgIntOrCalc (1, vartype, cmdStruct.getParamList(), parmString);
                                cmdStruct.setParamList(list);
                            }
                            case StrArray -> {
                                // you can either assign a single string value or a list of strings connected with '+' signs
                                list = parseScript.checkArgStringOrConcat (1, cmdStruct.getParamList());
                                cmdStruct.setParamList(list);
                            }
                            default -> {
                                throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " command not valid for " + vartype + ": " + parmString);
                            }
                        }
                        break;

                    case MODIFY:
                        // VarName, Index, Data Value
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(3, cmdStruct);
                        // arg 0 should be the Array variable name
                        vartype = ParseScript.checkVariableName (0, cmdStruct.getParamList());
                        // arg 1 is the Array index and should be an Integer or Unsigned
                        ParseScript.checkArgType (1, ParameterStruct.ParamType.Integer, cmdStruct.getParamList());
                        // determine argument type needed for the data for the Array
                        ParameterStruct.ParamType argType;
                        switch (vartype) {
                            case IntArray -> { argType = ParameterStruct.ParamType.Integer;  }
                            case StrArray -> { argType = ParameterStruct.ParamType.String;  }
                            default -> throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " command not valid for " + vartype + ": " + parmString);
                        }
                        // arg 2 is the data value and should be an Integer or String based on Array type
                        ParseScript.checkArgType (2, argType, cmdStruct.getParamList());
                        break;

                    case REMOVE:
                        // VarName, Index
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        // arg 0 should be the Array variable name
                        ParseScript.checkVariableName (0, cmdStruct.getParamList());
                        // arg 1 is the Array index and should be an Integer or Unsigned
                        ParseScript.checkArgType (1, ParameterStruct.ParamType.Integer, cmdStruct.getParamList());
                        break;

                    case TRUNCATE:
                    case POP:
                        // VarName, Index (optional)
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        // arg 0 should be the Array variable name
                        ParseScript.checkVariableName (0, cmdStruct.getParamList());
                        // arg 1 (OPTIONAL) is the Array index and should be an Integer or Unsigned
                        if (cmdStruct.getParamSize() == 2) {
                            ParseScript.checkArgType (1, ParameterStruct.ParamType.Integer, cmdStruct.getParamList());
                        }
                        break;

                    case CLEAR:
                        // VarName
                        // arg 0 should be the Array variable name or RESPONSE
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(1, cmdStruct);
                        String varName = cmdStruct.getParamEntry(0).getStringValue();
                        if (! varName.contentEquals("RESPONSE")) {
                            ParseScript.checkVariableName (0, cmdStruct.getParamList());
                        }
                        break;

                    case FILTER:
                        // ARGS: 0 = ParamName or RESET, 1 (optional) the filter string
                        // verify there are the correct number and type of arguments
                        cmdStruct.showParams();
                        ParseScript.checkMaxArgs(2, cmdStruct);
                        if (cmdStruct.getParamSize() < 1) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " command requires at least 1 argument : " + parmString);
                        }

                        // if entry was RESET, no more checking to do
                        varName = cmdStruct.getParamEntry(0).getStringValue();
                        if (! varName.contentEquals("RESET")) {
                            // otherwise,
                            // arg 0 should be the Array variable name
                            vartype = ParseScript.checkVariableName (0, cmdStruct.getParamList());
                            // arg 1 (and possibly 2) should be the filter values
                            parseScript.checkArgFilterValue (1, vartype, cmdStruct.getParamList());
                        }
                        break;

                    case IF:
                        // verify number and type of arguments
                        list = parseScript.packComparison (parmString);
                        cmdStruct.setParamList(list);

                        // read the arguments passed
                        // assumed format is: IF Name1 >= Name2  (where Names can be Integers, Strings or Variables)
                        String ifName = cmdStruct.getParamEntry(0).getStringValue();

                        // add entry to the current loop stack
                        IFStruct ifInfo;
                        String sname = Subroutine.getSubName();
                        ifInfo = new IFStruct (cmdIndex, LoopStruct.getStackSize(), sname);
                        IFStruct.ifListPush(ifInfo);
                        IFStruct.stackPush(cmdIndex);
                        GUILogPanel.outputInfoMsg(MsgType.COMPILE, "   - new IF level " + IFStruct.getStackSize() + " Variable " + ifName);
                        break;
                    case ELSE:
                        checkNoArgs(command, parmString);
                        if (IFStruct.isIfListEnpty()) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " received when not in an IF case");
                        }
                        // save the current command index in the current if structure
                        ifInfo = IFStruct.getIfListEntry();
                        if (! ifInfo.isSameSubroutine(Subroutine.getSubName())) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " was outside subroutine of matching IF statement");
                        }
                        ifInfo.setElseIndex(cmdIndex, false, LoopStruct.getStackSize());
                        GUILogPanel.outputInfoMsg(MsgType.COMPILE, "   - IF level " + IFStruct.getStackSize() + " " + cmdStruct.getCommand() + " on line " + cmdIndex);
                        break;
                    case ELSEIF:
                        if (IFStruct.isIfStackEnpty()) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " received when not in an IF case");
                        }

                        // read the arguments passed
                        // assumed format is: IF Name1 >= Name2  (where Names can be Integers, Strings or Variables)
                        list = parseScript.packComparison (parmString);
                        cmdStruct.setParamList(list);
                        ifName = cmdStruct.getParamEntry(0).getStringValue();

                        // save the current command index in the current if structure
                        ifInfo = IFStruct.getIfListEntry();
                        if (! ifInfo.isSameSubroutine(Subroutine.getSubName())) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " was outside subroutine of matching IF statement");
                        }
                        ifInfo.setElseIndex(cmdIndex, true, LoopStruct.getStackSize());
                        GUILogPanel.outputInfoMsg(MsgType.COMPILE, "   - IF level " + IFStruct.getStackSize() + " " + cmdStruct.getCommand() + " on line " + cmdIndex + " Variable " + ifName);
                        break;
                    case ENDIF:
                        checkNoArgs(command, parmString);
                        if (IFStruct.isIfStackEnpty()) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " received when not in an IF case");
                        }
                        // save the current command index in the current if structure
                        ifInfo = IFStruct.getIfListEntry();
                        if (! ifInfo.isSameSubroutine(Subroutine.getSubName())) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " was outside subroutine of matching IF statement");
                        }
                        ifInfo.setEndIfIndex(cmdIndex, LoopStruct.getStackSize());
                        IFStruct.stackPop();
                        GUILogPanel.outputInfoMsg(MsgType.COMPILE, "   - IF level " + IFStruct.getStackSize() + " " + cmdStruct.getCommand() + " on line " + cmdIndex);
                        break;
                    case FOR:
                        // assumed format is: FOR VarName [=] StartIx TO EndIx [STEP StepVal]
                        // the other format is: "FOR EVER" - check for single argument of "EVER"
                        cmdStruct.showParams();
                        String loopName, strVal;
                        ParameterStruct loopStart = null;
                        ParameterStruct loopEnd   = null;
                        ParameterStruct loopStep  = null;
                        boolean bInclEnd = false;
                        ParameterStruct nameParam = cmdStruct.getParamEntry(0);
                        ParseScript.checkMinArgs(1, cmdStruct);
                        if (cmdStruct.getParamSize() <= 2 && nameParam.getStringValue().contentEquals("EVER")) {
                            Integer maxLoops = null;
                            if (cmdStruct.getParamSize() == 2) {
                                maxLoops = cmdStruct.getParamEntry(1).getIntegerValue().intValue();
                            }
                            // create loop forever structure
                            try {
                                LoopStruct loopInfo = new LoopStruct (maxLoops, cmdIndex, IFStruct.getStackSize());
                                LoopParam.saveLoopParameter (loopInfo);
                                String newLoopName = loopInfo.getLoopName(); // the name gets changed to make it unique from user defined names
                                nameParam.setStringValue(newLoopName);
                                GUILogPanel.outputInfoMsg(MsgType.COMPILE, "   - new FOR EVER Loop level " + LoopStruct.getStackSize() +
                                        " " + newLoopName + " index @ " + cmdIndex);
                            } catch (ParserException exMsg) {
                                Utils.throwAddendum (exMsg.getMessage(), functionId + lineInfo + "command " + cmdStruct.getCommand());
                            }
                        } else {
                            // verify the arguments passed
                            ParseScript.checkMaxArgs(7, cmdStruct);
                            ParseScript.checkMinArgs(5, cmdStruct);
                            // arg 0 should be the Loop variable name
                            loopName = ParseScript.checkArgTypeString (0, cmdStruct.getParamList());
                            // check for optional '=' sign and eliminate if found
                            strVal   = ParseScript.checkArgTypeString (1, cmdStruct.getParamList());
                            if (strVal.contentEquals("=")) {
                                cmdStruct.removeParamEntry(1);
                            }
                            boolean bValid = true;
                            if (cmdStruct.getParamSize() != 4 && cmdStruct.getParamSize() != 6) {
                                bValid = false;
                            } else {
                                // this checks the required start and end loop index values
                                loopStart = cmdStruct.getParamEntry(1);
                                strVal    = ParseScript.checkArgTypeString  (2, cmdStruct.getParamList());
                                loopEnd   = cmdStruct.getParamEntry(3);
                                if (strVal.contentEquals("TO")) {
                                    bInclEnd = true;
                                }
                                if (! strVal.contentEquals("TO") && ! strVal.contentEquals("UPTO")) {
                                    bValid = false;
                                }
                                if (cmdStruct.getParamSize() == 6) {
                                    // this checks the optional loop index step value
                                    strVal   = ParseScript.checkArgTypeString  (4, cmdStruct.getParamList());
                                    loopStep = cmdStruct.getParamEntry(5);
                                    if (! strVal.contentEquals("STEP")) {
                                        bValid = false;
                                    }
                                }
                            }
                            if (! bValid) {
                                throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " invalid command format" +
                                        ": should be of form VarName [=] StartIx TO | UPTO EndIx [STEP StepIx]");
                            }

                            // create a new loop ID (name + command index) for the entry and add it
                            // to the list of IDs for the loop parameter name
                            try {
                                LoopStruct loopInfo = new LoopStruct (loopName, loopStart, loopEnd, loopStep,
                                                bInclEnd, cmdIndex, IFStruct.getStackSize());
                                LoopParam.saveLoopParameter (loopInfo);
                            } catch (ParserException exMsg) {
                                Utils.throwAddendum (exMsg.getMessage(), functionId + lineInfo + "command " + cmdStruct.getCommand());
                            }
                            GUILogPanel.outputInfoMsg(MsgType.COMPILE, "   - new FOR Loop level " + LoopStruct.getStackSize() + " Variable " + loopName + " index @ " + cmdIndex);
                        }
                        break;
                    case BREAK:
                        checkNoArgs(command, parmString);
                        // make sure we are in a FOR ... NEXT loop
                        if (LoopStruct.getStackSize() == 0) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " received when not in a FOR loop");
                        }
                        break;
                    case SKIP:
                        checkNoArgs(command, parmString);
                        // make sure we are in a FOR ... NEXT loop
                        if (LoopStruct.getStackSize() == 0) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " received when not in a FOR loop");
                        }
                        break;
                    case BREAKIF:
                        // verify number and type of arguments
                        list = parseScript.packComparison (parmString);
                        cmdStruct.setParamList(list);
                        // make sure we are in a FOR ... NEXT loop
                        if (LoopStruct.getStackSize() == 0) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " received when not in a FOR loop");
                        }
                        break;
                    case SKIPIF:
                        // verify number and type of arguments
                        list = parseScript.packComparison (parmString);
                        cmdStruct.setParamList(list);
                        // make sure we are in a FOR ... NEXT loop
                        if (LoopStruct.getStackSize() == 0) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " received when not in a FOR loop");
                        }
                        break;
                    case NEXT:
                        checkNoArgs(command, parmString);
                        // make sure we are in a FOR ... NEXT loop
                        if (LoopStruct.getStackSize() == 0) {
                            throw new ParserException(functionId + lineInfo + cmdStruct.getCommand() + " received when not in a FOR loop");
                        }
                        
                        // add a token ENDFOR command following the NEXT, so we have a location to go to on exiting loop
                        lineNumbers.add(lineNum);
                        cmdList.add(cmdStruct); // place the NEXT command here and queue up the ENDFOR command
                        cmdStruct = new CommandStruct(CommandStruct.CommandTable.ENDFOR, lineNum);
                        GUILogPanel.outputInfoMsg(MsgType.COMPILE, "PROGIX [" + (cmdIndex + 1) + "]: " + cmdStruct.getCommand() + " (added to follow NEXT)");
                        // (this will be added at the end of this switch statement)

                        // verify the IF loop level hasn't been exceeded
                        LoopId curLoop = LoopStruct.peekStack();
                        LoopParam.checkLoopIfLevel (cmdStruct.getCommand().toString(), IFStruct.getStackSize(), curLoop);

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
                        cmdStruct.showParams();
                        cmdStruct.setCmdOption (cmdStruct.getParamEntry(0).getStringValue());
                        cmdStruct.removeParamEntry(0);
                        cmdOptionParser.checkCmdOptions(cmdStruct.getCmdOption(), cmdStruct.getParamList(), lineNum);
                        break;

                    default:
                        throw new ParserException(functionId + lineInfo + "Unknown command: " + cmdStruct.getCommand());
                }

                // all good, add command to list
                ParameterStruct.showParamTypeList(cmdStruct.getParamList());
                lineNumbers.add(lineNum);
                cmdList.add(cmdStruct);
                
            } catch (ParserException exMsg) {
                GUILogPanel.outputInfoMsg(MsgType.ERROR, exMsg.getMessage());
                if (! AmazonReader.isRunModeCompileOnly()) {
                    // if running script after compile, exit after logging msg
                    String newMsg = "  -> " + functionId + lineInfo + "PROGIX[" + cmdIndex + "]: " + line;
                    Utils.throwAddendum (exMsg.getMessage(), newMsg);
                }
            }
        }  // end of while loop

        String errorMsg;
        int loopSize = LoopStruct.getStackSize();
        if (loopSize != 0) {
            errorMsg = functionId + "FOR loop not complete for " + loopSize + " entries";
            compilerError (errorMsg);
        }
        if (!IFStruct.isIfStackEnpty() && !IFStruct.getIfListEntry().isValid()) {
            errorMsg = functionId + "Last IF has no matching ENDIF";
            compilerError (errorMsg);
        }
        try {
            Subroutine.checkSubroutineMissing();
        } catch (ParserException exMsg) {
            errorMsg = exMsg.getMessage();
            compilerError (errorMsg);
        }

        scriptLineLength = lineNum;
        fileReader.close();
    }

    /**
     * This handles the compiler errors.
     * If we are running compiler only, we don't want to stop the process, just report
     *  the errors in the log.
     * 
     * @param errorMsg - the error to report
     * 
     * @throws ParserException 
     */
    private static void compilerError (String errorMsg) throws ParserException {
        if (AmazonReader.isRunModeCompileOnly()) {
            GUILogPanel.outputInfoMsg(MsgType.ERROR, errorMsg);
        } else {
            throw new ParserException(errorMsg);
        }
    }
    
}
