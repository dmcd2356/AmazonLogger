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
import java.util.Stack;
import java.util.StringTokenizer;
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
        "TABLE",
        "ENDTABLE",
        "RUN",
    };
            
    // IF List is built during Compile phase and referenced during Execution phase.
    // IF Stack is used during Compile and Execution phases. Compile time for
    //   verification, and Execution for running the branches.
    private final ArrayList<IFStruct> ifList  = new ArrayList<>();
    private final Stack<Integer>      ifStack = new Stack<>();

    // FOR loop stack for keeping track of current nesting of loops as program runs
    //  and curLoopId for identifying the current entry.
    private final Stack<LoopId> loopStack = new Stack<>();
    private LoopId curLoopId = null;
    
    // this handles the command line options via the RUN command
    private CmdOptions cmdOptionParser;
    

    // the key for loops uses both the name and the command index of the FOR statement.
    //  this way, loop names can be reused as long as they aren't nested within each other.
    public class LoopId {
        String  name;       // name of the loop
        int     index;      // command index of the start of the loop
        
        LoopId (String name, int index) {
            this.name  = name;
            this.index = index;
        }
    }

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
            int cmdIndex = cmdList.size(); // the command index

            // first, extract the 1st word as the command keyword
            // 'cmdStruct' will receive the command, with the params yet to be placed.
            // 'parmList'  will be a string containing the remainder of the command line
            // 'parmArr'   will be an array of Strings corresponding to 'parmList'
            CommandStruct cmdStruct;
            String [] parmArr = null;
            int paramCnt = 0;
            ArrayList<String> listParms;
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
                    if (parmArr[0].startsWith("I_")) {
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

                    // for integer type parameters, verify it is an integer value being assigned
                    char dataType = ParameterStruct.classifyDataType(strParmVal);
                    if (dataType == 'I' || dataType == 'U') {
                        Integer intParmVal = Utils.getIntValue (strParmVal);
                        ParameterStruct.putIntegerParameter(strParmName, intParmVal);
                    } else {
                        ParameterStruct.putStringParameter(strParmName, strParmVal);
                    }
                    break;
                case "IF":
                    // read the arguments passed
                    // assumed format is: IF Name1 >= Name2  (where Names can be Integers, Strings or Parameters)
                    cmdStruct.params = packParamList (line, "ISI");
                    
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
                    ifInfo = new IFStruct (cmdIndex, loopStack.size());
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
                    ifInfo.setElseIndex(cmdIndex, false, loopStack.size());
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + ifStack.size() + " " + cmdStruct.command + " on line " + cmdIndex);
                    break;
                case "ELSEIF":
                    if (ifStack.empty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                    }
                    
                    // read the arguments passed
                    // assumed format is: IF Name1 >= Name2  (where Names can be Integers, Strings or Parameters)
                    cmdStruct.params = packParamList (line, "ISI");
                    ifName = cmdStruct.params.get(0).getStringValue();
                    
                    // save the current command index in the current if structure
                    ifInfo = getIfEntry(ifStack.peek());
                    ifInfo.setElseIndex(cmdIndex, true, loopStack.size());
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + ifStack.size() + " " + cmdStruct.command + " on line " + cmdIndex + " parameter " + ifName);
                    break;
                case "ENDIF":
                    if (ifStack.empty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in an IF case");
                    }
                    // save the current command index in the current if structure
                    ifInfo = getIfEntry(ifStack.peek());
                    ifInfo.setEndIfIndex(cmdIndex, loopStack.size());
                    ifStack.pop();
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + ifStack.size() + " " + cmdStruct.command + " on line " + cmdIndex);
                    break;
                case "FOR":
                    // read the arguments passed
                    // assumed format is: FOR Name = StartIx ; < EndIx ; IncrVal
                    // (and trailing "; IncrVal" is optional)
                    listParms = extractUserParams ("S=I;CI;I", parms);
                    if (listParms.size() < 4) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " missing parameters");
                    } else if (listParms.size() < 5) {
                        listParms.add("1"); // use 1 as a default value
                        frame.outputInfoMsg(STATUS_PROGRAM, "    (using default step size of 1)");
                    }

                    // get the parameters and format them for use
                    // NOTE: the null entries in the packParamEntry calls because we are not passing them to the execution phase
                    ParameterStruct loopStart, loopEnd, loopStep;
                    String loopName, loopComp;
                    loopName  = listParms.get(0);
                    loopStart = packParamEntry (listParms.get(1), 'I', null);
                    loopComp  = listParms.get(2);
                    loopEnd   = packParamEntry (listParms.get(3), 'I', null);
                    loopStep  = packParamEntry (listParms.get(4), 'I', null);

                    // create the parameter list for execution
                    // (we only need the loop name, the rest is saved in the Loop parameter hashmap)
                    packParamEntry (loopName, 'S', cmdStruct.params);
                    
                    // create a new loop ID (name + command index) for the entry and add it
                    // to the list of IDs for the loop parameter name
                    LoopId loopId = new LoopId(loopName, cmdIndex);
                    LoopStruct loopInfo = new LoopStruct (loopName, loopStart, loopEnd, loopStep, loopComp, cmdIndex, ifStack.size());
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
                    // verify the IF loop level hasn't been exceeded
                    LoopId curLoop = loopStack.firstElement();
                    ParameterStruct.checkLoopIfLevel (cmdStruct.command, ifStack.size(), curLoop);
                    break;
                case "CONTINUE":
                    cmdStruct.params = packParamList (line, "");
                    // make sure we are in a FOR ... NEXT loop
                    if (loopStack.empty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    // verify the IF loop level hasn't been exceeded
                    curLoop = loopStack.firstElement();
                    ParameterStruct.checkLoopIfLevel (cmdStruct.command, ifStack.size(), curLoop);
                    break;
                case "NEXT":
                    cmdStruct.params = packParamList (line, "");
                    // make sure we are in a FOR ... NEXT loop
                    if (loopStack.empty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    // verify the IF loop level hasn't been exceeded
                    curLoop = loopStack.firstElement();
                    ParameterStruct.checkLoopIfLevel (cmdStruct.command, ifStack.size(), curLoop);
                    break;
                case "ENDFOR":
                    cmdStruct.params = packParamList (line, "");
                    // make sure we are in a FOR ... NEXT loop
                    if (loopStack.empty()) {
                        throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                    }
                    // store line location in labelsMap
                    curLoop = loopStack.firstElement();
                    ParameterStruct.setLoopEndIndex(cmdList.size(), curLoop);

                    // remove entry from loop stack
                    loopStack.pop();
                    break;
                case "TABLE":
                    // TODO: 
                    break;
                case "ENDTABLE":
                    // TODO: 
                    break;
                case "RUN":
                    // verify the option command and its parameters
                    // NOTE: when we place the command in cmdStruct, we remove the RUN label,
                    //       so executeProgramCommand does not need to check for it.
                    ArrayList<String> optCmd = new ArrayList<>(Arrays.asList(parmArr));
                    ArrayList<CommandStruct> runList = cmdOptionParser.formatCmdOptions (optCmd);
                    
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
        if (!ifStack.isEmpty() && !getIfEntry(ifStack.peek()).isValid()) {
            throw new ParserException(functionId + "Last IF has no matching ENDIF");
        }

        fileReader.close();
        
        // the last line will be the one to end the program flow
        cmdList.add(new CommandStruct("EXIT"));
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
        String functionId = CLASS_NAME + ".executeProgramCommand: ";
        String lineInfo = "PROGIX [" + cmdIndex + "]: ";
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
            case "IF":
                // get the params
                verifyParamList(cmdStruct.params, 3); // check for 3 params
                ParameterStruct parm1 = cmdStruct.params.get(0);
                String comp           = cmdStruct.params.get(1).unpackStringValue();
                ParameterStruct parm2 = cmdStruct.params.get(2);

                // add entry to the current loop stack
                ifStack.push(cmdIndex);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - new IF level " + ifStack.size() + " " + parm1.getStringValue() + " " + comp + " " + parm2.getStringValue());

                // check status to see if true of false.
                boolean bBranch = Utils.compareParameterValues (parm1, parm2, comp);
                if (bBranch) {
                    IFStruct ifInfo = getIfEntry(cmdIndex);
                    newIndex = ifInfo.getElseIndex(cmdIndex);
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - goto next IF case @ " + newIndex);
                }
                break;
            case "ELSE":
                if (ifStack.empty()) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a IF structure");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + ifStack.size() + " " + cmdStruct.command + " on line " + cmdIndex);
                break;
            case "ELSEIF":
                if (ifStack.empty()) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a IF structure");
                }

                // get the params
                verifyParamList(cmdStruct.params, 3); // check for 3 params
                parm1 = cmdStruct.params.get(0);
                comp  = cmdStruct.params.get(1).unpackStringValue();
                parm2 = cmdStruct.params.get(2);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - IF level " + ifStack.size() + ": " + parm1.getStringValue() + " " + comp + " " + parm2.getStringValue());

                // check status to see if true of false.
                bBranch = Utils.compareParameterValues (parm1, parm2, comp);
                if (bBranch) {
                    IFStruct ifInfo = getIfEntry(ifStack.peek());
                    newIndex = ifInfo.getElseIndex(cmdIndex);
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - goto next IF case @ " + newIndex);
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
                verifyParamList(cmdStruct.params, 1);
                String loopName  = cmdStruct.params.get(0).unpackStringValue();
                curLoopId = new LoopId(loopName, cmdIndex);
                newIndex = ParameterStruct.getLoopNextIndex (command, cmdIndex, curLoopId);
                    
                // add entry to the current loop stack
                loopStack.push(curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - new FOR Loop level " + loopStack.size() + " parameter " + loopName + " index @ " + cmdIndex);
                break;
            case "BREAK":
                if (loopStack.empty() || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - " + command + " command for Loop level " + loopStack.size()
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                break;
            case "CONTINUE":
                if (loopStack.empty() || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - " + command + " command for Loop level " + loopStack.size()
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                break;
            case "NEXT":
                if (loopStack.empty() || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, "   - " + command + " command for Loop level " + loopStack.size()
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                break;
            case "ENDFOR":
                if (loopStack.empty() || curLoopId == null) {
                    throw new ParserException(functionId + lineInfo + cmdStruct.command + " received when not in a FOR loop");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "   - " + command + " command for Loop level " + loopStack.size()
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                curLoopId = loopStack.pop();
                if (curLoopId == null) {
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - All loops completed so far");
                } else {
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - current Loop level " + loopStack.size()
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                }
                break;
            case "TABLE":
                // TODO: 
                break;
            case "ENDTABLE":
                // TODO: 
                break;
            case "RUN":
                // remove the RUN tag from the command line
                command = cmdStruct.params.removeFirst().getStringValue();
                // fall through...
            default:
                cmdOptionParser.runCmdOption (command, cmdStruct.params);
                break;
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
                    if (Character.isLetterOrDigit(curChar) || curChar == '-' || curChar == '_') {
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
                        if (Character.isLetterOrDigit(curChar) || curChar == '-' || curChar == '_') {
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
    
}
