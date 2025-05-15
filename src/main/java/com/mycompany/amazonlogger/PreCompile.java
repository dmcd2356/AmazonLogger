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
public class PreCompile {
    
    private static final String CLASS_NAME = PreCompile.class.getSimpleName();

    private static final Variables var = new Variables();
    private static final VarLocal varLocal = var.varLocal;

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
     */
    public Variables compile (String fname) throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        frame.outputInfoMsg(STATUS_COMPILE, "Pre-Compiling file: " + fname);
        int cmdIndex = 0;
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
        while ((line = fileReader.readLine()) != null) {
            try {
                lineNum++;
                line = line.strip();
                if (line.isBlank() || line.charAt(0) == '#' || line.charAt(0) == '-') {
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
                    continue;
                }
                cmdIndex++;
                Subroutine.setCurrentIndex(cmdIndex);
                switch (command) {
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
                params = ScriptCompile.packParameters (parmString, bParamAssign);

                // we're only handling allocations and subroutine definitions on this first pass, so that
                //  we will know all the GLOBALS and subroutine names that may be referenced in
                //  other code locations that are not sequentail in the script.
                switch (command) {
                    case ALLOCATE:
                        // must be a Data Type followed by a List of Variable name entries
                        String access   = ScriptCompile.checkArgType (0, ParameterStruct.ParamType.String, params);
                        String dataType = ScriptCompile.checkArgType (1, ParameterStruct.ParamType.String, params);
                        ScriptCompile.checkArgType (2, ParameterStruct.ParamType.StrArray, params);
                        ParameterStruct list = params.get(2);

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
                        for (int ix = 0; ix < list.getStrArraySize(); ix++) {
                            String pName = list.getStrArrayElement(ix);
                            var.allocateVariable(access, dataType, pName, subName);
                        }
                        break;
                    case ENDMAIN:
                        subs.compileEndOfMain (cmdIndex);
                        break;
                    case SUB:
                        // verify 1 String argument: name of subroutine
                        if (params.isEmpty()) {
                            throw new ParserException(functionId + "Missing argument");
                        }
                        subName = params.get(0).getStringValue();
                        subs.compileSubStart (subName, cmdIndex);
                        varLocal.allocSubroutine(subName);
                        break;
                    case ENDSUB:
                        subs.compileSubEnd (cmdIndex);
                        break;
                    default:
                        break;
                }
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

        fileReader.close();
        return var;
    }
        
}
