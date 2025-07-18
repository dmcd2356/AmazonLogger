/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import com.dmcd.amazonlogger.GUILogPanel.MsgType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 *
 * @author dan
 */
public class ScriptExecute {
    
    private static final String CLASS_NAME = ScriptExecute.class.getSimpleName();
    
    private static String scriptName = "";
    private static int    scriptSize = 0;
    
    // this handles the command line options via the RUN command
    private final CmdOptions cmdOptionParser;
    
    // identifies the current loopStack entry.
    private LoopId curLoopId;

    

    ScriptExecute (String name, int size) {
        this.cmdOptionParser = new CmdOptions();
        curLoopId = null;
        scriptName = name;
        scriptSize = size;
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "    Running compiled file: " + scriptName + " (size " + scriptSize + " lines)");
    }

    public static int getMaxLines() {
        return scriptSize;
    }
    
    /**
     * cleanup of any open resources.
     * 
     * @throws IOException 
     */
    public static void exit() throws IOException {
        Subroutine.beginExecution(); // make sure to run the exit commands in EXECUTE mode
        FileIO.exit();
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
     * returns the name of the array variable specified by the parameter.
     * 
     * @param parmRef the parameter (should specify an array variable)
     * 
     * @return the array variable name
     * 
     * @throws ParserException if variable reference name not found
     */
    private static String getArrayAssignment (ParameterStruct parmRef) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        String name =  parmRef.getVariableRefName();
        if (name == null) {
            name = parmRef.getStringValue();
        }
        if (name == null) {
            throw new ParserException(functionId + "Array name is null");
        }
        return name;
    }
    
    /**
     * extracts the numeric (or Calculation) value from a parameter
     * 
     * @param parmValue - the parameter to get the numeric value from
     * 
     * @return the numeric value
     * 
     * @throws ParserException 
     */
    private Long getIntegerArg (ParameterStruct parmValue) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        Long result;
        if (parmValue.isCalculation()) {
            result = parmValue.getCalculationValue(ParameterStruct.ParamType.Integer);
        } else {
            result = parmValue.getIntegerValue();
        }
        if (result == null) {
            throw new ParserException(functionId + "Integer value is null");
        }
        return result;
    }

    /**
     * extracts the numeric (or Calculation) value from a parameter
     * 
     * @param cmdStruct - the structure containing the argument list to parse
     * @param offset    - the index of the 1st entry for the String value (in case there are multiple entries)
     * 
     * @return the string argument (concatenates the entries if there are more than 1)
     * 
     * @throws ParserException 
     */
    private String getStringArg (CommandStruct cmdStruct, int offset) throws ParserException {
        String strValue = "";
        for (int ix = offset; ix < cmdStruct.getParamSize(); ix++) {
            strValue += cmdStruct.getParamEntry(ix).getStringValue();
        }
        return strValue;
    }

    private boolean getComparison (ParameterStruct parm1, ParameterStruct parm2, ParameterStruct parm3) throws ParserException {
        Boolean bValue;
        if (parm2 == null) {
            // simple assignment: VariableName = Boolean
            bValue = parm1.getBooleanValue();
        } else if (parm3 == null) {
            // simple NOTted assignment: VariableName = Boolean !
            bValue = ! parm1.getBooleanValue();
        } else {
            // Boolean comparison: VariableName = Calculation1 CompSign Calculation2
            Comparison comp = new Comparison (parm1, parm3, parm2.getStringValue());
            bValue = comp.getStatus();
        }
        return bValue;
    }

    /**
     * replaces an embedded reference value in String parameter with its current value .
     * 
     * @param param - the parameter to check
     * 
     * @return String with the reference string replaced by the its current value
     * 
     * @throws ParserException 
     */
    public static String extractEmbeddedVar (ParameterStruct param) throws ParserException {
        // exit if argument is not a discrete String
        if (param.getParamType() != ParameterStruct.ParamType.String ||
            param.getParamClass() != ParameterStruct.ParamClass.Discrete ||
            param.getStringValue() == null) {
            return null;
        }
        // return if we don't see an embedded reference name in the String
        String strValue = param.getStringValue();
        int offset = strValue.indexOf("<$");
        if (offset < 0 || offset >= strValue.length() - 5) {
            return null;
        }
        int offset2 = strValue.substring(offset+2).indexOf(">");
        if (offset2 < 0) {
            return null;
        }
        
        // seperate into pre and post Strings and the variable reference name
        offset2 += offset + 2;
        String prefix  = strValue.substring(0, offset);
        String varname = strValue.substring(offset+2, offset2);
        String postfix = "";
        if (offset2 < strValue.length() - 1) {
            postfix = strValue.substring(offset2 + 1);
        }

        // exit if the variable reference is not valid
        Variables.checkValidVariable(Variables.VarCheck.REFERENCE, varname);
        
        // get the value for the reference
        ParameterStruct arg = new ParameterStruct (varname,
                ParameterStruct.ParamClass.Reference, ParameterStruct.ParamType.String);
        arg.updateFromReference();
        String refval = arg.getStringValue();
        
        // return the value with the pre and post values attached
        String response = prefix + refval + postfix;
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "    Replaced arg value with expanded value: " + response);
        return response;
    }

    /**
     * prints the user-specified text to standard output.
     * 
     * @param text - the text to output
     */
    private static void printUserText (String text) {
        if (text == null) {
            System.out.println();
        } else {
            System.out.println(text);
        }
        if (AmazonReader.isOpModeNetwork()) {
            if (text == null) {
                text = "";
            }
            TCPServerThread.sendUserOutputInfo(text);
        }
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
    public int executeProgramCommand (int cmdIndex, CommandStruct cmdStruct) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        String linePreface = "PROGIX [" + cmdIndex + "]: " + showLineNumberInfo(cmdStruct.getLine());
        String exceptPreface = functionId + linePreface + " " + cmdStruct.getCommand();
        String debugPreface = "    ";
        int newIndex = -1;
        
        // replace all program references in the command to their corresponding values.
        // (skip for SET command so as not to modify the parameter we are setting.
        //  the conversion for this will be done in Calculation)
        cmdStruct.showCommand(linePreface);
        for (int ix = 0; ix < cmdStruct.getParamSize(); ix++) {
            if (ix > 0 || cmdStruct.getCommand() != CommandStruct.CommandTable.SET) {
                ParameterStruct param = cmdStruct.getParamEntry(ix);
                try {
                    param.updateFromReference();
                } catch (ParserException exMsg) {
                    Utils.throwAddendum (exMsg.getMessage(), exceptPreface + " - replacing reference: " + param.getVariableRefName());
                }
                // see if there are any variable references embedded in any of the parameters
                String strVal = extractEmbeddedVar(param);
                if (strVal != null) {
                    param.setStringValue(strVal);
                }
            }
        }
        Subroutine subroutine = new Subroutine();
        Subroutine.setCurrentIndex(cmdIndex);

        try {
        switch (cmdStruct.getCommand()) {
            case ENDMAIN:
            case EXIT:
                return -1; // this will terminate the program
            case SUB:
                GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "Subroutine entered at level: " + Subroutine.getSubroutineLevel());
                break;
            case GOSUB:
                String subName = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                           ParameterStruct.ParamType.String).getStringValue();
                newIndex = subroutine.subBegin(subName, cmdIndex + 1);
                break;
            case ENDSUB:
            case RETURN:
                String retArg = "";
                if (! cmdStruct.isParamEmpty()) {
                    retArg = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                           ParameterStruct.ParamType.String).getStringValue();
                }
                newIndex = Subroutine.subReturn(retArg);
                break;
            case PRINT:
                // arg 0: text to output
                if (cmdStruct.isParamEmpty()) {
                    // treat no arguments as outputting an empty newline
                    printUserText(null);
                } else if (cmdStruct.getParamSize() > 1) {
                    // if multiple arguments, must be a concatenated string.
                    //  pull the pieces together and print
                    String text = "";
                    for (int ix = 0; ix < cmdStruct.getParamSize(); ix++) {
                        String entry = cmdStruct.getParamEntry(ix).getStringValue();
                        if (! entry.contentEquals("+")) {
                            text += entry;
                        }
                    }
                    printUserText(text);
                } else if (cmdStruct.getParamEntry(0).getParamType() == ParameterStruct.ParamType.StrArray) {
                    // if entry is a string array, print each entry on a new line
                    ArrayList<String> list = cmdStruct.getParamEntry(0).getStrArray();
                    for (int ix = 0; ix < list.size(); ix++) {
                        String text = list.get(ix);
                        printUserText(text);
                    }
                } else if (cmdStruct.getParamEntry(0).getParamType() == ParameterStruct.ParamType.IntArray) {
                    // if entry is a string array, print each entry on a new line
                    ArrayList<Long> list = cmdStruct.getParamEntry(0).getIntArray();
                    for (int ix = 0; ix < list.size(); ix++) {
                        String text = list.get(ix).toString();
                        printUserText(text);
                    }
                } else {
                    // otherwise, just print the single entry
                    String text = cmdStruct.getParamEntry(0).getStringValue();
                    printUserText(text);
                }
                break;
            case DIRECTORY:
                // arg 0: filter selection (-f -d or blank)
                // arg 0: directory path
                String filter = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                   ParameterStruct.ParamType.String).getStringValue();
                String fname = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(1),
                         ParameterStruct.ParamType.String).getStringValue();
                FileIO.getDirFileList (fname, filter);
                break;
            case CD:
                // arg 0: directory path
                fname = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                  ParameterStruct.ParamType.String).getStringValue();
                FileIO.setFilePath (fname);
                break;
            case FEXISTS:
                // arg 0: type of check on file
                // arg 1: filename
                String ftype = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                  ParameterStruct.ParamType.String).getStringValue();
                fname = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(1),
                  ParameterStruct.ParamType.String).getStringValue();

                File file = FileIO.getFilePath(fname);
                boolean value;
                switch (ftype) {
                    case "-w":
                        value = file.isFile() && file.canWrite();
                        break;
                    case "-r":
                        value = file.isFile() && file.canRead();
                        break;
                    case "-d":
                        value = file.isDirectory();
                        break;
                    case "-x":
                        value = file.exists();
                        break;
                    default:
                        throw new ParserException(exceptPreface + "Unknown file check argument: " + ftype);
                }
                VarReserved.putStatusValue(value);
                GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "File " + file + " exists = " + value);
                break;
            case FGETSIZE:
                // arg 0: filename
                fname = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                  ParameterStruct.ParamType.String).getStringValue();
                Long fsize = FileIO.fileGetSize(fname);
                VarReserved.putResponseValue(fsize.toString());
                break;
            case FGETLINES:
                // arg 0: filename
                fname = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                  ParameterStruct.ParamType.String).getStringValue();
                Integer lines = FileIO.fileGetLines(fname);
                VarReserved.putResponseValue(lines.toString());
                break;
            case MKDIR:
                // arg 0: dir name
                fname = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                  ParameterStruct.ParamType.String).getStringValue();
                FileIO.createDir(fname);
                break;
            case RMDIR:
                // arg 0: -f if force, else don't remove if not empty
                // arg 1: dir name
                ftype = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                  ParameterStruct.ParamType.String).getStringValue();
                fname = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(1),
                  ParameterStruct.ParamType.String).getStringValue();
                FileIO.removeDir(fname, ftype.contentEquals("-f"));
                break;
            case FDELETE:
                // arg 0: filename
                fname = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                  ParameterStruct.ParamType.String).getStringValue();
                FileIO.delete(fname);
                break;
            case FCREATE:
                // arg 0: type (-r / -w)
                // arg 1: filename
                ftype = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                  ParameterStruct.ParamType.String).getStringValue();
                fname = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(1),
                  ParameterStruct.ParamType.String).getStringValue();
                FileIO.createFile(fname, ftype.contentEquals("-w"));
                break;
            case FOPEN:
                // arg 0: type (-r / -w)
                // arg 1: filename
                ftype = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                  ParameterStruct.ParamType.String).getStringValue();
                fname = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(1),
                  ParameterStruct.ParamType.String).getStringValue();
                FileIO.openFile(fname, ftype.contentEquals("-w"));
                break;
            case FCLOSE:
                // arg 0: filename
                fname = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                  ParameterStruct.ParamType.String).getStringValue();
                FileIO.close(fname);
                break;
            case FREAD:
                // optional arg 0: number of lines to read
                Integer count = null;
                if (! cmdStruct.isParamEmpty()) {
                    count = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                          ParameterStruct.ParamType.Integer).getIntegerValue().intValue();
                }
                FileIO.read(count);
                break;
            case FWRITE:
                // arg 0: text to write
                ParameterStruct parm = cmdStruct.getParamEntry(0);
                ArrayList<String> strArray = ParameterStruct.verifyArgEntry (parm,
                  ParameterStruct.ParamType.StrArray).getStrArray();
                if (strArray != null && strArray.size() > 1) {
                    FileIO.write(strArray);
                } else {
                    FileIO.write(parm.getStringValue());
                }
                break;
            case OCRSCAN:
                // verify 1 String argument: file name
                fname = ParameterStruct.verifyArgEntry (cmdStruct.getParamEntry(0),
                  ParameterStruct.ParamType.String).getStringValue();
                OCRReader ocr = new OCRReader();
                ocr.run (fname);
                break;
            case ALLOCATE:
                // nothing to do
                break;
            case SET:
                // the 1st 3 entries are always required: the param name, the equate sign and the value to set
                //   and we ignore the equate sign at offset 1.
                // for Boolean case, we have 1 or 2 additional entries that may be added
                ParameterStruct parmRef = cmdStruct.getParamEntry(0);
                ParameterStruct parm1   = cmdStruct.getParamEntry(2);
                ParameterStruct parm2   = cmdStruct.getParamSize() > 3 ? cmdStruct.getParamEntry(3) : null;
                ParameterStruct parm3   = cmdStruct.getParamSize() > 4 ? cmdStruct.getParamEntry(4) : null;
                String varName = parmRef.getVariableRefName();
                ParameterStruct.ParamType varType = parmRef.getVariableRefType();
                ParameterStruct.ParamType type = varType;
                if (varType == null || varName == null) {
                    varName = parmRef.getStringValue();
                    type = ParameterStruct.ParamType.String;
                }
                
                // make sure we have write access to the variable
                Variables.checkWriteAccess (varName);
                
                // make sure we are converting to the type of the reference parameter
                switch (type) {
                    case ParameterStruct.ParamType.Integer:
                        Long result = getIntegerArg (parm1);
                        Variables.setIntegerVariable(varName, result);
                        break;
                    case ParameterStruct.ParamType.Unsigned:
                        result = getIntegerArg (parm1);
                        result &= 0xFFFFFFFF;
                        Variables.setUnsignedVariable(varName, result);
                        break;
                    case ParameterStruct.ParamType.Boolean:
                        Boolean bResult = getComparison(parm1, parm2, parm3);
                        Variables.setBooleanVariable(varName, bResult);
                        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "Boolean Variable " + varName + " = " + bResult);
                        break;

                    case ParameterStruct.ParamType.IntArray:
                        Variables.setIntArray(varName, parm1.getIntArray());
                        break;
                    case ParameterStruct.ParamType.StrArray:
                        Variables.setStrArray(varName, parm1.getStrArray());
                        break;
                    case ParameterStruct.ParamType.String:
                        // The entries should be a list of 1 or more Strings to concatenate into 1
                        // (any parameter references should have been converted to their appropriate value
                        //  at the begining of the execution phase)
                        String concat = getStringArg (cmdStruct, 2);
                        Variables.setStringVariable(varName, concat);
                        break;
                    default:
                        throw new ParserException(exceptPreface + varType + " Invalid data type: " + type);
                }
                break;

            case INSERT:
                ParameterStruct parmValue;
                parmRef   = cmdStruct.getParamEntry(0); // element 0 is the param ref to be appended to
                parmValue = cmdStruct.getParamEntry(1); // element 1 is the value being appended
                varName = getArrayAssignment(parmRef);
                ParameterStruct.ParamType parmType = Variables.getVariableTypeFromName (varName);

                // make sure we have write access to the variable
                Variables.checkWriteAccess (varName);
                
                boolean bSuccess = false;
                Variables.VarClass varClass;
                String strValue;
                switch (parmType) {
                    case IntArray:
                        if (parmValue.getIntArray() != null && parmValue.getParamType() == ParameterStruct.ParamType.IntArray) {
                            for (int ix = parmValue.getIntArray().size() - 1; ix >= 0; ix--) {
                                strValue = parmValue.getIntArrayElement(ix).toString();
                                bSuccess = VarArray.arrayInsertEntry (varName, 0, strValue);
                            }
                        } else {
                            strValue = getIntegerArg (parmValue).toString();
                            bSuccess = VarArray.arrayInsertEntry (varName, 0, strValue);
                        }
                        break;
                    case StrArray:
                    case String:
                        if (parmValue.getStrArray() != null && parmValue.getParamType() == ParameterStruct.ParamType.StrArray) {
                            for (int ix = parmValue.getStrArray().size() - 1; ix >= 0; ix--) {
                                strValue = parmValue.getStrArrayElement(ix);
                                bSuccess = VarArray.arrayInsertEntry (varName, 0, strValue);
                            }
                        } else {
                            strValue = getStringArg (cmdStruct, 1);
                            bSuccess = VarArray.arrayInsertEntry (varName, 0, strValue);
                        }
                        break;
                    default:
                        throw new ParserException(exceptPreface + "Invalid parameter type: " + parmRef.getParamType());
                }

                // AmazonReader.isOpModeNetwork()
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + " variable ref not found: " + varName);
                }

            case APPEND:
                parmRef   = cmdStruct.getParamEntry(0); // element 0 is the param ref to be appended to
                parmValue = cmdStruct.getParamEntry(1); // element 1 is the value being appended
                varName = getArrayAssignment(parmRef);
                parmType = Variables.getVariableTypeFromName (varName);

                // make sure we have write access to the variable
                Variables.checkWriteAccess (varName);
                
                bSuccess = false;
                switch (parmType) {
                    case IntArray:
                        if (parmValue.getIntArray() != null && parmValue.getParamType() == ParameterStruct.ParamType.IntArray) {
                            for (int ix = 0; ix < parmValue.getIntArray().size(); ix++) {
                                strValue = parmValue.getIntArrayElement(ix).toString();
                                bSuccess = VarArray.arrayAppendEntry (varName, strValue);
                            }
                        } else {
                            strValue = getIntegerArg (parmValue).toString();
                            bSuccess = VarArray.arrayAppendEntry (varName, strValue);
                        }
                        break;
                    case StrArray:
                        if (parmValue.getStrArray() != null && parmValue.getParamType() == ParameterStruct.ParamType.StrArray) {
                            for (int ix = 0; ix < parmValue.getStrArray().size(); ix++) {
                                strValue = parmValue.getStrArrayElement(ix);
                                bSuccess = VarArray.arrayAppendEntry (varName, strValue);
                            }
                        } else {
                            strValue = getStringArg (cmdStruct, 1);
                            bSuccess = VarArray.arrayAppendEntry (varName, strValue);
                        }
                        break;
                    default:
                        throw new ParserException(exceptPreface + "Invalid parameter type: " + parmType);
                }

                if (!bSuccess) {
                    throw new ParserException(exceptPreface + " variable ref not found: " + varName);
                }
                break;
            case MODIFY:
                // ParamName, Index (Integer), Value (String or Integer)
                ParameterStruct parmIndex;
                parmRef   = cmdStruct.getParamEntry(0); // element 0 is the param ref to be modified
                parmIndex = cmdStruct.getParamEntry(1); // element 1 is the index element being modified
                parmValue = cmdStruct.getParamEntry(2); // element 2 is the value to set the entry to
                varName  = getArrayAssignment(parmRef);
                parmType  = Variables.getVariableTypeFromName (varName);
                int index = parmIndex.getIntegerValue().intValue();

                // make sure we have write access to the variable
                Variables.checkWriteAccess (varName);
                
                switch (parmType) {
                    case IntArray:
                        Long result = getIntegerArg (parmValue);
                        strValue = result.toString();
                        break;
                    case StrArray:
                        strValue = getStringArg (cmdStruct, 2);
                        break;
                    default:
                        throw new ParserException(exceptPreface + "Invalid parameter type: " + parmRef.getParamType());
                }
                
                bSuccess = VarArray.arrayModifyEntry (varName, index, strValue);
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + " variable ref not found: " + varName);
                }
                break;
            case REMOVE:
                // ParamName, Index (Integer)
                parmRef   = cmdStruct.getParamEntry(0); // element 0 is the param ref to be modified
                parmIndex = cmdStruct.getParamEntry(1); // element 1 is the index element being removed
                varName  = getArrayAssignment(parmRef);
                index = parmIndex.getIntegerValue().intValue();

                // make sure we have write access to the variable
                Variables.checkWriteAccess (varName);
                
                bSuccess = VarArray.arrayClearEntries (varName, index, 1);
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + " variable ref not found: " + varName);
                }
                break;
            case TRUNCATE:
                // ParamName, Count (Integer - optional)
                parmRef = cmdStruct.getParamEntry(0); // element 0 is the param ref to be modified
                varName = getArrayAssignment(parmRef);

                // make sure we have write access to the variable
                Variables.checkWriteAccess (varName);
                
                int size = VarArray.getArraySize(varName);
                int iCount = 1;
                if (cmdStruct.getParamSize() > 1) {
                    parmIndex = cmdStruct.getParamEntry(1); // element 1 is the (optional) number of entries being removed
                    iCount = parmIndex.getIntegerValue().intValue();
                    if (iCount > size) {
                        throw new ParserException(exceptPreface + "item count " + iCount + " exceeds size of " + varName);
                    }
                }
                int iStart = size - iCount;
                bSuccess = VarArray.arrayClearEntries (varName, iStart, iCount);
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + " variable ref not found: " + varName);
                }
                break;
            case POP:
                // ParamName, Index (Integer - optional)
                parmRef = cmdStruct.getParamEntry(0); // element 0 is the param ref to be modified
                varName = getArrayAssignment(parmRef);

                // make sure we have write access to the variable
                Variables.checkWriteAccess (varName);
                
                size = VarArray.getArraySize(parmRef.getStringValue());
                iCount = 1;
                iStart = 0;
                if (cmdStruct.getParamSize() > 1) {
                    parmIndex = cmdStruct.getParamEntry(1); // element 1 is the (optional) number of entries being removed
                    iCount = parmIndex.getIntegerValue().intValue();
                    if (iCount > size) {
                        throw new ParserException(exceptPreface + "item count " + iCount + " exceeds size of " + varName);
                    }
                }
                bSuccess = VarArray.arrayClearEntries (varName, iStart, iCount);
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + " variable ref not found: " + varName);
                }
                break;
            case CLEAR:
                // ParamName
                parmRef = cmdStruct.getParamEntry(0); // element 0 is the param ref to be modified
                varName = getArrayAssignment(parmRef);

                // make sure we have write access to the variable
                Variables.checkWriteAccess (varName);
                
                VarArray.arrayClearAll(varName);
                break;
                
            case FILTER:
                // ParamName or RESET, 1 (optional) the filter string
                parmRef = cmdStruct.getParamEntry(0); // element 0 is the param ref or RESET
                varName = getArrayAssignment(parmRef);
                if (varName.contentEquals("RESET")) {
                    VarArray.arrayFilterReset();
                } else {
                    // make sure we have write access to the variable
                    Variables.checkWriteAccess (varName);
                
                    parmType = Variables.getVariableTypeFromName (varName);
                    switch (parmType) {
                        case StrArray:
                            filter = cmdStruct.getParamEntry(1).getStringValue();
                            String opts = "NONE";
                            if (cmdStruct.getParamSize() == 3) {
                                opts = cmdStruct.getParamEntry(2).getStringValue();
                            }
                            VarArray.arrayFilterString(varName, filter, opts);
                            break;
                        case IntArray:
                            String compSign = cmdStruct.getParamEntry(1).getStringValue();
                            Long iValue = cmdStruct.getParamEntry(2).getIntegerValue();
                            VarArray.arrayFilterInt(varName, compSign, iValue);
                            break;
                        default:
                            throw new ParserException(exceptPreface + "Invalid data type for FILTER: " + parmType);
                    }
                }
                break;
            case IF:
                // check status to see if true of false.
                parm1 = cmdStruct.getParamEntry(0);
                parm2 = cmdStruct.getParamSize() > 1 ? cmdStruct.getParamEntry(1) : null;
                parm3 = cmdStruct.getParamSize() > 2 ? cmdStruct.getParamEntry(2) : null;
                Boolean bResult = getComparison(parm1, parm2, parm3);
                
                // add entry to the current loop stack
                IFStruct.stackPush(cmdIndex);
                GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "new IF level " + IFStruct.getStackSize() + " " +
                        parm1.getStringValue() + " " + ((parm2 != null) ? parm2.getStringValue() : "") +
                                                 " " + ((parm3 != null) ? parm3.getStringValue() : ""));

                IFStruct ifInfo = IFStruct.getIfListEntry(cmdIndex);
                if (! bResult) {
                    newIndex = ifInfo.getElseIndex(cmdIndex);
                    GUILogPanel.outputInfoMsg(MsgType.DEBUG, debugPreface + "IFCONDITION skipped");
                    GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "goto next IF case @ " + newIndex);
                    ifInfo.clearConditionMet();     // starting new IF and condition was not met
                } else {
                    GUILogPanel.outputInfoMsg(MsgType.DEBUG, debugPreface + "IFCONDITION executed");
                    ifInfo.setConditionMet();       // we are running the condition, so ELSEs will be skipped
                }
                break;
            case ELSE:
                if (IFStruct.isIfStackEnpty()) {
                    throw new ParserException(exceptPreface + "Received when not in a IF structure");
                }

                // if the IF condition has already been met, jump to the ENDIF statement
                ifInfo = IFStruct.getIfListEntry();
                if (ifInfo.isConditionMet()) {
                    newIndex = ifInfo.getEndIndex();
                    GUILogPanel.outputInfoMsg(MsgType.DEBUG, debugPreface + "IFCONDITION already met");
                    GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "goto ENDIF @ " + newIndex);
                } else {
                    GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "IF level " + IFStruct.getStackSize() + " " + cmdStruct.getCommand() + " on line " + cmdIndex);
                }
                break;
            case ELSEIF:
                if (IFStruct.isIfStackEnpty()) {
                    throw new ParserException(exceptPreface + "Received when not in a IF structure");
                }

                // if the IF condition has already been met, jump to the ENDIF statement
                ifInfo = IFStruct.getIfListEntry();
                if (ifInfo.isConditionMet()) {
                    newIndex = ifInfo.getEndIndex();
                    GUILogPanel.outputInfoMsg(MsgType.DEBUG, debugPreface + "IFCONDITION already met");
                    GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "goto ENDIF @ " + newIndex);
                } else {
                    // check status to see if true of false.
                    parm1 = cmdStruct.getParamEntry(0);
                    parm2 = cmdStruct.getParamSize() > 1 ? cmdStruct.getParamEntry(1) : null;
                    parm3 = cmdStruct.getParamSize() > 2 ? cmdStruct.getParamEntry(2) : null;
                    bResult = getComparison(parm1, parm2, parm3);

                    // add entry to the current loop stack
                    GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "new IF level " + IFStruct.getStackSize() + " " +
                            parm1.getStringValue() + " " + ((parm2 != null) ? parm2.getStringValue() : "") +
                                                     " " + ((parm3 != null) ? parm3.getStringValue() : ""));

                    if (! bResult) {
                        newIndex = ifInfo.getElseIndex(cmdIndex);
                        GUILogPanel.outputInfoMsg(MsgType.DEBUG, debugPreface + "IFCONDITION skipped");
                        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "goto next IF case @ " + newIndex);
                    } else {
                        GUILogPanel.outputInfoMsg(MsgType.DEBUG, debugPreface + "IFCONDITION executed");
                        ifInfo.setConditionMet(); // we are running the condition, so ELSEs will be skipped
                    }
                }
                break;
            case ENDIF:
                if (IFStruct.isIfStackEnpty()) {
                    throw new ParserException(exceptPreface + "Received when not in a IF structure");
                }
                // reset the condition met flag
                ifInfo = IFStruct.getIfListEntry();
                ifInfo.clearConditionMet();
                
                // save the current command index in the current if structure
                IFStruct.stackPop();
                GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "new IF level " + IFStruct.getStackSize() + ": " + cmdStruct.getCommand() + " on line " + cmdIndex);
                break;
            case FOR:
                String loopName  = cmdStruct.getParamEntry(0).getStringValue();
                curLoopId = new LoopId(loopName, cmdIndex);
                newIndex = LoopParam.getLoopNextIndex (cmdStruct.getCommand(), cmdIndex, curLoopId);
                    
                // add entry to the current loop stack
                LoopStruct.pushStack(curLoopId);
                int loopSize = LoopStruct.getStackSize();
                GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "new FOR Loop level " + loopSize+ " parameter " + loopName + " index @ " + cmdIndex);
//                LoopStruct.sendCurrentLoopInfo();
                break;
            case BREAK:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(exceptPreface + "Received when not in a FOR loop");
                }
                newIndex = LoopParam.getLoopNextIndex (cmdStruct.getCommand(), cmdIndex, curLoopId);
                GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + cmdStruct.getCommand().toString() + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.printLoopId());
//                LoopStruct.sendCurrentLoopInfo();
                break;
            case SKIP:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(exceptPreface + "Received when not in a FOR loop");
                }
                newIndex = LoopParam.getLoopNextIndex (cmdStruct.getCommand(), cmdIndex, curLoopId);
                GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + cmdStruct.getCommand().toString() + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.printLoopId());
//                LoopStruct.sendCurrentLoopInfo();
                break;
            case BREAKIF:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(exceptPreface + "Received when not in a FOR loop");
                }
                // check status to see if true of false.
                parm1 = cmdStruct.getParamEntry(0);
                parm2 = cmdStruct.getParamSize() > 1 ? cmdStruct.getParamEntry(1) : null;
                parm3 = cmdStruct.getParamSize() > 2 ? cmdStruct.getParamEntry(2) : null;
                bResult = getComparison(parm1, parm2, parm3);

                if (! bResult) {
                    GUILogPanel.outputInfoMsg(MsgType.DEBUG, debugPreface + "BREAKIF not TRUE - remain in loop");
                } else {
                    GUILogPanel.outputInfoMsg(MsgType.DEBUG, debugPreface + "BREAKIF is TRUE - exit loop");
                    newIndex = LoopParam.getLoopNextIndex (cmdStruct.getCommand(), cmdIndex, curLoopId);
                    GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "Loop " + curLoopId.printLoopId() + " exiting to index " + newIndex);
                }
//                LoopStruct.sendCurrentLoopInfo();
                break;
            case SKIPIF:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(exceptPreface + "Received when not in a FOR loop");
                }
                // check status to see if true of false.
                parm1 = cmdStruct.getParamEntry(0);
                parm2 = cmdStruct.getParamSize() > 1 ? cmdStruct.getParamEntry(1) : null;
                parm3 = cmdStruct.getParamSize() > 2 ? cmdStruct.getParamEntry(2) : null;
                bResult = getComparison(parm1, parm2, parm3);

                if (! bResult) {
                    GUILogPanel.outputInfoMsg(MsgType.DEBUG, debugPreface + "SKIPIF not TRUE - do nothing");
                } else {
                    GUILogPanel.outputInfoMsg(MsgType.DEBUG, debugPreface + "SKIPIF is TRUE - skip to next iteration");
                    newIndex = LoopParam.getLoopNextIndex (cmdStruct.getCommand(), cmdIndex, curLoopId);
                    GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + curLoopId.printLoopId() + " exiting to index " + newIndex);
                }
//                LoopStruct.sendCurrentLoopInfo();
                break;
            case NEXT:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(exceptPreface + "Received when not in a FOR loop");
                }
                newIndex = LoopParam.getLoopNextIndex (cmdStruct.getCommand(), cmdIndex, curLoopId);
                GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + cmdStruct.getCommand().toString() + " command for Loop level " + loopSize
                                    + " " + curLoopId.printLoopId());
//                LoopStruct.sendCurrentLoopInfo();
                break;
            case ENDFOR:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(exceptPreface + "Received when not in a FOR loop");
                }
                GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + cmdStruct.getCommand().toString() + " command for Loop level " + loopSize
                                    + " " + curLoopId.printLoopId());
                LoopStruct.popStack();
                loopSize = LoopStruct.getStackSize();
                if (loopSize > 0) {
                    curLoopId = LoopStruct.peekStack();
                }
                if (curLoopId == null) {
                    GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "All loops completed so far");
                } else {
                    GUILogPanel.outputInfoMsg(MsgType.PROGRAM, debugPreface + "Current Loop level " + loopSize
                                    + " " + curLoopId.printLoopId());
                }
//                LoopStruct.sendCurrentLoopInfo();
                break;
            case RUN:
                // fall through...
            default:
                cmdOptionParser.runCmdOption (cmdStruct);
                break;
        }
        } catch (IOException | TikaException | SAXException | ParserException exMsg) {
            if (AmazonReader.isOpModeNetwork()) {
                String response = ScriptThread.limitNetworkString(exMsg.getMessage());
                TCPServerThread.sendStatus("ERROR: " + response);
                return -1;
            } else {
                Utils.throwAddendum (exMsg.getMessage(), exceptPreface);
            }
        }
        
        // by default, the command will proceed to the next command
        if (newIndex >= 0) {
            cmdIndex = newIndex;
        } else {
            cmdIndex++;
        }
        
        return cmdIndex;
    }

}
