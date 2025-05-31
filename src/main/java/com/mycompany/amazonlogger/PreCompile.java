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
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 *
 * @author dan
 */
public class PreCompile {
    
    private static final String CLASS_NAME = PreCompile.class.getSimpleName();

    public static final Variables variables = new Variables();

    /**
     * compiles the external script file (when -f option used) into a series of
     * CommandStruct entities to execute.
     * 
     * @param fname - the script filename
     * 
     * @return the Global & Local Variables that have been setup
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public Variables build (String fname) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        frame.outputInfoMsg(STATUS_COMPILE, "Pre-Compiling file: " + fname);
        String lineInfo = "";

        // open the file to compile and extract the commands from it
        File scriptFile = Utils.checkFilename (fname, ".scr", Utils.PathType.Test, false);
        FileReader fReader = new FileReader(scriptFile);
        BufferedReader fileReader = new BufferedReader(fReader);

        // access the Subroutine class to define them
        Subroutine subs = new Subroutine();

        // read the program and compile into ArrayList 'cmdList'
        int lineNum = 0;
        String line;
        boolean bEnableSetup = false;
        CmdOptions cmdOptionParser = new CmdOptions();
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
                //  that may be defined in subroutines that are used in another function than may preceed it.
                boolean bValid = false;
                CommandStruct.CommandTable command = CommandStruct.isValidCommand(strCmd);
                if (command == null) {
                    if (! bEnableSetup) {
                        continue;
                    } else {
                        // all of these commands should be simple command options that have discrete string arguments
                        if (parmString.isEmpty()) {
                            throw new ParserException(functionId + lineInfo + "invalid Pre-compiler command: " + line);
                        }
                        // these are the only setup commands that are allowed in the pre-compiler
                        CommandStruct cmdStruct = new CommandStruct(strCmd, lineNum);
                        switch (strCmd) {
                            case "-tpath":
                            case "-spath":
                            case "-ppath":
                            case "-ofile":
                            case "-sfile":
                                cmdStruct.params.add(new ParameterStruct(parmString));
                                cmdOptionParser.runCmdOption (cmdStruct);
                                break;
                            case "-debug":
                                Long longVal;
                                try {
                                    Integer iVal = Utils.getHexValue (parmString);
                                    if (iVal == null) {
                                        longVal = Utils.getIntValue (parmString);
                                    } else {
                                        longVal = iVal.longValue();
                                    }
                                } catch (ParserException exMsg) {
                                    throw new ParserException(functionId + lineInfo + "invalid Pre-compiler command: " + line);
                                }
                                cmdStruct.params.add(new ParameterStruct(longVal));
                                cmdOptionParser.runCmdOption (cmdStruct);
                                break;
                            default:
                                throw new ParserException(functionId + lineInfo + "invalid Pre-compiler command: " + strCmd);
                        }
                        continue;
                    }
                }
                Subroutine.setCurrentIndex(lineNum);
                switch (command) {
                    case STARTUP:
                        // THIS ALLOWS A SECTION TO BE CREATED THAT IS ONLY RUN DURING PRE-COMPILE,
                        // AND SHOULD ONLY INVOLVE COMMAND OPTIONS THAT WILL SET UP PATHS, ETC.
                        bEnableSetup = true;
                        frame.outputInfoMsg(STATUS_COMPILE, lineInfo + command + " - Begining STARTUP code");
                        continue;
                    case ENDSTARTUP:
                        bEnableSetup = false;
                        frame.outputInfoMsg(STATUS_COMPILE, lineInfo + command + " - Ending STARTUP code");
                        continue;
                    case ALLOCATE:
                    case ENDMAIN:
                    case SUB:
                    case ENDSUB:
                        bValid = true;
                        break;
                    default:
                        break;
                }
                if (! bValid) {
                    continue;
                }

                // get the parameters to be passed
                ArrayList<ParameterStruct> params;
                frame.outputInfoMsg(STATUS_COMPILE, lineInfo + command + " " + parmString);
                boolean bParamAssign = (CommandStruct.CommandTable.SET == command);
                params = ParseScript.packParameters (parmString, bParamAssign);

                // we're only handling allocations and subroutine definitions on this first pass, so that
                //  we will know all the GLOBALS and subroutine names that may be referenced in
                //  other code locations that are not sequentail in the script.
                switch (command) {
                    case ALLOCATE:
                        // must be a Data Type followed by a List of Variable name entries
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
            } catch (ParserException exMsg) {
                String errMsg = exMsg + "\n  -> " + functionId + lineInfo + line;
                if (AmazonReader.isRunModeCompileOnly()) {
                    // if only running compiler, just log the messages but don't exit
                    frame.outputInfoMsg(STATUS_ERROR, errMsg);
                } else {
                    throw new ParserException(errMsg);
                }
            }
        }  // end of while loop

        fileReader.close();
        return variables;
    }
        
}
