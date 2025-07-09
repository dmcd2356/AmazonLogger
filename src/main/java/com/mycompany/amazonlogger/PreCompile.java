/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import com.mycompany.amazonlogger.GUILogPanel.MsgType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 *
 * @author dan
 */
public class PreCompile {
    
    private static final String CLASS_NAME = PreCompile.class.getSimpleName();

    public static final Variables variables = new Variables();

    public PreCompile() {
        init();
    }
    
    /**
     * initializes all the static variable entities
     */
    public static void init() {
        Variables.initVariables();
        Subroutine.init();
        LoopStruct.resetStack();
        Spreadsheet.init();
        OpenDoc.init();
        IFStruct.init();
    }
    
    /**
     * compiles the external script file (when -f option used) into a series of
     * CommandStruct entities to execute.
     * 
     * @param scriptFile - the script file
     * 
     * @return the Global & Local Variables that have been setup
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public Variables build (File scriptFile) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        GUILogPanel.outputInfoMsg(MsgType.COMPILE, "Pre-Compiling file: " + scriptFile.getAbsolutePath());
        String lineInfo = "";

        // open the file to compile and extract the commands from it
        FileReader fReader = new FileReader(scriptFile);
        BufferedReader fileReader = new BufferedReader(fReader);

        // access the Subroutine class to define them
        Subroutine subs = new Subroutine();

        // init the static entities
        init();
        
        // read the program and compile into ArrayList 'cmdList'
        int lineNum = 0;
        int cmdIndex = 0;
        String line;
        boolean bEnableSetup = false;
        while ((line = fileReader.readLine()) != null) {
            try {
                lineNum++;
                line = line.strip();
                if (line.isBlank() || line.charAt(0) == '#') {
                    continue;
                }

                lineInfo = "LINE " + lineNum + ": ";

                // first, extract the 1st word as the command keyword
                String strCmd = line;
                String parmString = "";
                int offset = strCmd.indexOf(" ");
                if (offset > 0) {
                    strCmd = strCmd.substring(0, offset).strip();
                    parmString = line.substring(offset).strip();
                }

                // we're only handling allocations on this first pass, so that we know all the GLOBALS
                //  that may be defined in subroutines that are used in another function than may precede it.
                CommandStruct.CommandTable command = CommandStruct.isValidCommand(strCmd);
                if (command == null) {
                    continue; // ignore any command line option type commands
                }
                Subroutine.setCurrentIndex(lineNum);

                // get the parameters to be passed
                ArrayList<ParameterStruct> params = packSimple (parmString);

                // CHECK FOR STARTUP SECTION (MUST BE FIRST COMMAND EXECUTED)
                if (CommandStruct.CommandTable.STARTUP == command) {
                    if (cmdIndex > 0) {
                        throw new ParserException(functionId + lineInfo + "STARTUP command must be first command in script!");
                    }
                    // THIS ALLOWS A SECTION TO BE CREATED THAT IS ONLY RUN DURING PRE-COMPILE,
                    // AND SHOULD ONLY INVOLVE COMMAND OPTIONS THAT WILL SET UP PATHS, ETC.
                    bEnableSetup = true;
                    GUILogPanel.outputInfoMsg(MsgType.COMPILE, lineInfo + command + " - Begining STARTUP code");
                    cmdIndex++;
                    continue;
                }

                // THESE ARE THE COMMANDS THAT CAN BE PROCESSED IN STARTUP MODE
                if (bEnableSetup) {
                    switch (command) {
                        case ENDSTARTUP:
                            bEnableSetup = false;
                            GUILogPanel.outputInfoMsg(MsgType.COMPILE, lineInfo + command + " - Ending STARTUP code");
                            break;
                        case TESTPATH:
                            ParseScript.showPackedParams(params);
                            String path;
                            if (params.isEmpty()) {
                                path = System.getProperty("user.dir");
                                GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "  set Test path to current running directory: " + path);
                            } else {
                                path = ParseScript.checkArgTypeString (0, params);
                                GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "  set Test path to: " + path);
                            }
                            FileIO.setBaseTestPath(path);
                            path = FileIO.getCurrentFilePath(); // make sure we have an absolute path
                            Utils.setDefaultPath(Utils.PathType.Test, path);
                            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "  set Test path to: " + path);
                            break;
                        case LOGFILE:
                            ParseScript.showPackedParams(params);
                            String logname = null;
                            boolean bAppend = false;
                            // set the debug filter value
                            Long dbugEnable = ParseScript.checkArgTypeInteger (0, params);
                            GUIMain.setMessageFlags(dbugEnable.intValue());
                            if (params.size() > 2) {
                                bAppend = ParseScript.checkArgTypeBoolean (1, params);
                                logname = ParseScript.checkArgTypeString  (2, params);
                            } else if (params.size() > 1) {
                                logname = ParseScript.checkArgTypeString  (1, params);
                            }
                            if (logname != null) {
                                logname = subsScriptName(logname);
                                GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "  Output messages to file: " + logname);
                            } else {
                                GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "  Output messages to stdout");
                            }
                            GUILogPanel.setTestOutputFile(logname, bAppend);
                            break;
                        default:
                            throw new ParserException(functionId + lineInfo + "Invalid command in STARTUP section: " + command);
                    }
                }
                else {
                    // we're only handling allocations and subroutine definitions on this first pass, so that
                    //  we will know all the GLOBALS and subroutine names that may be referenced in
                    //  other code locations that are not sequentail in the script.
                    GUILogPanel.outputInfoMsg(MsgType.COMPILE, lineInfo + command + " " + parmString);
                    switch (command) {
                        case ALLOCATE:
                            // must be a Data Type followed by a List of Variable name entries
                            ParseScript.showPackedParams(params);
                            String access   = ParseScript.checkArgTypeString (0, params);
                            String dataType = ParseScript.checkArgTypeString (1, params);
                            ArrayList<String> strArray = ParseScript.checkArgTypeStrArray (2, params);
                            // if a single entry was made instead of list enclosed in braces, the entry
                            // will be in the String section, so copy it over from there
                            if (strArray == null) {
                                strArray = new ArrayList<>();
                            }
                            if (strArray.isEmpty()) {
                                strArray.add(params.get(2).getStringValue());
                            }

                            // get the data type and access type for the variable
                            if (ParameterStruct.checkParamType (dataType) == null) {
                                throw new ParserException(functionId + lineInfo + "command " + command + " : invalid data type: " + dataType);
                            }

                            // get the current function name (Main or Subroutine).
                            // The parameters can be accessed globally, but can only be set in the
                            //   function that allocated them.
                            String subName = Subroutine.getSubName();

                            // this defines the Variable names, and must be done prior to their use.
                            // This Compile method will allocate them, so the Execute does not need
                            //  to do anything with this command.
                            // Multiple Variables can be defined on one line, with the names comma separated
                            for (int ix = 0; ix < strArray.size(); ix++) {
                                String pName = strArray.get(ix).strip();
                                variables.allocateVariable(access, dataType, pName, subName);
                            }
                            break;
                        case ENDMAIN:
                            subs.compileEndOfMain (lineNum);
                            break;
                        case SUB:
                            // verify 1 String argument: name of subroutine
                            if (params.isEmpty()) {
                                throw new ParserException(functionId + "Missing argument");
                            }
                            ParseScript.showPackedParams(params);
                            subName = params.get(0).getStringValue();
                            subs.compileSubStart (subName, lineNum);
                            variables.varLocal.allocSubroutine(subName);
                            break;
                        case ENDSUB:
                            subs.compileSubEnd (lineNum);
                            break;
                        default:
                            break;
                    }
                }
                cmdIndex++;
            } catch (ParserException exMsg) {
                String errMsg = exMsg + "\n  -> " + functionId + lineInfo + line;
                if (AmazonReader.isRunModeCompileOnly()) {
                    // if only running compiler, just log the messages but don't exit
                    GUILogPanel.outputInfoMsg(MsgType.ERROR, errMsg);
                } else {
                    throw new ParserException(errMsg);
                }
            }
        }  // end of while loop

        fileReader.close();
        return variables;
    }

    /**
     * checks for the script name reference in the string and substitutes it for the name of the script file.
     * 
     * @param entry - the string to check
     * 
     * @return the string with the script name reference changed to the filenane (if found)
     */
    private static String subsScriptName (String entry) {
        String searchName = "<$SCRIPTNAME>";
        int offset = entry.indexOf(searchName);
        if (offset >= 0) {
            String prefix = entry.substring(0, offset);
            String suffix = entry.substring(offset + searchName.length());
            String replace = AmazonReader.getScriptName();
            entry = prefix + replace + suffix;
        }
        return entry;
    }
    
    /**
     * get the next word in a string of words.
     * 
     * @param line - the string containing 0 or more words
     * 
     * @return the next word in the line (empty string if no more words)
     */
    private static String getNextWord (String line) {
        if (line.isBlank()) {
            return "";
        }
        int offset = line.indexOf(" ");
        if (offset <= 0) {
            return line;
        }
        return line.substring(0, offset);
    }

    /**
     * This takes a command line and extracts the parameter list from it.
     * This simply separates the String of arguments into separate parameter
     *   values based on where it finds commas and quotes.
     * 
     * @param line - the string of parameters to separate and classify
     * 
     * @return the ArrayList of arguments for the command
     * 
     * @throws ParserException 
     */
    private static ArrayList<ParameterStruct> packSimple (String line) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        ArrayList<ParameterStruct> params = new ArrayList<>();
        ParameterStruct arg;

        try {
        for (int ix = 0; ! line.isEmpty(); ix++) {
            // read next entry
            ParameterStruct.ParamType paramType = ParameterStruct.ParamType.String;
            line = line.strip();
            String nextArg = getNextWord (line);
            line = line.substring(nextArg.length());
            nextArg = nextArg.strip();

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
                    paramType = ParameterStruct.ParamType.StrArray;
                    arg = new ParameterStruct (nextArg, ParameterStruct.ParamClass.Discrete, paramType);
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
                        Long longVal = Utils.getLongOrUnsignedValue(nextArg);
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
            arg = new ParameterStruct(nextArg, pClass, paramType);
            params.add(arg);
        }
        } catch (ParserException exMsg) {
            Utils.throwAddendum (exMsg.getMessage(), functionId);
        }
        
        return params;
    }

}
