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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 *
 * @author dan
 */
public class ScriptExecute {
    
    private static final String CLASS_NAME = ScriptExecute.class.getSimpleName();
    
    // this handles the command line options via the RUN command
    private final CmdOptions cmdOptionParser;
    
    // identifies the current loopStack entry.
    private LoopId curLoopId = null;
    
    // the map of column names to column indices in the sheet
    // for now, we only allow 1 file open at a time (either a read or a write file)
    private BufferedReader fileReader = null;
    private PrintWriter    fileWriter = null;
    private String fileName;
    private String fileDir = Utils.getDefaultPath (Utils.PathType.Test);

    ScriptExecute () {
        cmdOptionParser = new CmdOptions();
    }

    /**
     * cleanup upon exit.
     * 
     * @throws IOException 
     */
    public void close() throws IOException {
        if (fileReader != null) {
            fileReader.close();
        } if (fileWriter != null) {
            fileWriter.close();
        }
        frame.outputInfoMsg(STATUS_DEBUG, "Closed file: " + fileName);
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
     * gets the file or directory specified by the given path.
     * 
     * A single '.' will represent the current directory and '~' indicates
     *  the root directory for testing: the Test default directory.
     *  If the path does not start with a '/' char, it is a relative path.
     *  Absolute paths are not supported to prevent exceeding the confines
     *  of the Test path.
     * 
     * @param path - the path of the dir or file
     * 
     * @return a file of the path specified
     */
    private File getFilePath (String path) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (path.contentEquals(".")) {
            // the '.' indicates the current directory
            path = fileDir;
        } else if (path.contains("..")) {
            throw new ParserException(functionId + "Path using .. not supported: " + path);
        } else if (path.startsWith("~")) {
            // a leading '~' refers to the base Test path
            if (path.length() > 1) {
                path = Utils.getDefaultPath (Utils.PathType.Test) + path.substring(1);
            } else {
                path = Utils.getDefaultPath (Utils.PathType.Test);
            }
        } else if (! path.startsWith("/")) {
            // if missing the leading '/' char, it is a path relative to current dir
            path = fileDir + "/" + path;
        } else {
            throw new ParserException(functionId + "Absolute path not supported: " + path);
        }
        File file = new File(path);
        return (file);
    }
    
    /**
     * gets the file or directory specified by the given path.
     * 
     * A single '.' will represent the current directory and '~' indicates
     *  the root directory for testing: the Test default directory.
     *  If the path does not start with a '/' char, it is a relative path.
     *  Absolute paths are not supported to prevent exceeding the confines
     *  of the Test path.
     * 
     * @param path - the path of the dir or file
     * 
     * @return a file of the path specified
     */
    private void setFilePath (String path) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (path.contentEquals(".")) {
            // the '.' indicates the current directory, so no change needed
        } else if (path.contains("..")) {
            throw new ParserException(functionId + "Path using .. not supported: " + path);
        } else if (path.startsWith("~")) {
            // a leading '~' refers to the base Test path
            if (path.length() > 1) {
                fileDir = Utils.getDefaultPath (Utils.PathType.Test) + path.substring(1);
            } else {
                fileDir = Utils.getDefaultPath (Utils.PathType.Test);
            }
        } else if (! path.startsWith("/")) {
            // if missing the leading '/' char, it is a path relative to current dir
            fileDir = fileDir + "/" + path;
        } else {
            throw new ParserException(functionId + "Absolute path not supported: " + path);
        }
        File file = new File(fileDir);
        if (! file.exists() || ! file.isDirectory()) {
            throw new ParserException(functionId + "Invalid directory selection: " + fileDir);
        }
        frame.outputInfoMsg(STATUS_PROGRAM, "    File path changed to: " + fileDir);
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
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
        try {
            for (int ix = 0; ix < command.params.size(); ix++) {
                ParameterStruct.verifyArgEntry (command.params, ix, validTypes.charAt(ix));
            }
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
    }

    private String getArrayAssignment (ParameterStruct parmRef) throws ParserException {
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
     * @param parmList - the argument list to parse
     * @param offset   - the index of the 1st entry for the String value (in case there are multiple entries)
     * 
     * @return the string argument (concatenates the entries if there are more than 1)
     * 
     * @throws ParserException 
     */
    private String getStringArg (ArrayList<ParameterStruct> parmList, int offset) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        String strValue = "";
        for (int ix = offset; ix < parmList.size(); ix++) {
            strValue += parmList.get(ix).getStringValue();
        }
        return strValue;
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
        String linePreface = "PROGIX [" + cmdIndex + "]: " + cmdStruct.command + " " + showLineNumberInfo(cmdStruct.line);
        String exceptPreface = functionId + linePreface;
        String debugPreface = "    ";
        int newIndex = -1;
        
        // replace all program references in the command to their corresponding values.
        // (skip for SET command so as not to modify the parameter we are setting.
        //  the conversion for this will be done in Calculation)
        for (int ix = 0; ix < cmdStruct.params.size(); ix++) {
            if (ix > 0 || cmdStruct.command != CommandStruct.CommandTable.SET) {
                ParameterStruct param = cmdStruct.params.get(ix);
                param.updateFromReference();
            }
        }
        frame.outputInfoMsg(STATUS_PROGRAM, linePreface + cmdStruct.showCommand());

        try {
        switch (cmdStruct.command) {
            case EXIT:
                return -1; // this will terminate the program
            case PRINT:
                // arg 0: text to output
                String text;
                if (cmdStruct.params.get(0).getParamType() == ParameterStruct.ParamType.StrArray) {
                    ArrayList<String> list = cmdStruct.params.get(0).getStrArray();
                    for (int ix = 0; ix < list.size(); ix++) {
                        text = list.get(ix);
                        System.out.println(text);
                    }
                } else {
                    text = cmdStruct.params.get(0).getStringValue();
                    System.out.println(text);
                }
                break;
            case DIRECTORY:
                // arg 0: directory path, optional arg 1: filter selection (-f or -d)
                String fname = cmdStruct.params.get(0).getStringValue();
                String filter = "";
                if (cmdStruct.params.size() > 1) {
                    filter  = cmdStruct.params.get(1).getStringValue();
                }
                File file = getFilePath(fname);
                if (! file.isDirectory()) {
                    throw new ParserException(exceptPreface + "Invalid directory selection: " + file.getAbsolutePath());
                }
                File[] list = file.listFiles();
                for (File list1 : list) {
                    if ((filter.isEmpty() || filter.contentEquals("-d")) && list1.isDirectory()) {
                        VarArray.putResponseValue(list1.getName());
                    } else if ((filter.isEmpty() || filter.contentEquals("-f")) && list1.isFile()) {
                        VarArray.putResponseValue(list1.getName());
                    }
                }
                break;
            case CD:
                // arg 0: directory path
                fname = cmdStruct.params.get(0).getStringValue();
                setFilePath (fname);
                break;
            case FEXISTS:
                // arg 0: filename
                fname = cmdStruct.params.get(0).getStringValue();
                file = getFilePath(fname);

                boolean value;
                String strCheck = "EXISTS";
                if (cmdStruct.params.size() > 1) {
                    strCheck = cmdStruct.params.get(1).getStringValue();
                }
                switch (strCheck) {
                    case "WRITABLE":
                        value = file.isFile() && file.canWrite();
                        break;
                    case "READABLE":
                        value = file.isFile() && file.canRead();
                        break;
                    case "DIRECTORY":
                        value = file.isDirectory();
                        break;
                    case "EXISTS":
                        value = file.exists();
                        break;
                    default:
                        throw new ParserException(exceptPreface + "Unknown file check argument: " + strCheck);
                }
                Variables.putStatusValue(value);
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "File " + file + " exists = " + value);
                break;
            case FDELETE:
                // arg 0: filename
                fname = cmdStruct.params.get(0).getStringValue();
                file = getFilePath(fname);
                if (! file.isFile()) {
                    throw new ParserException(exceptPreface + "File not found: " + fname);
                }
                if (fileReader != null || fileWriter != null) {
                    throw new ParserException(exceptPreface + "File is currently open: " + fileName);
                }
                file.delete();
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "File deleted: " + file);
                break;
            case FCREATER:
                // arg 0: filename
                fname = cmdStruct.params.get(0).getStringValue();
                if (fileReader != null || fileWriter != null) {
                    throw new ParserException(exceptPreface + "File already open: " + fileName);
                }
                file = getFilePath(fname);
                if (file.exists()) {
                    throw new ParserException(exceptPreface + "File already exists: " + fname);
                }
                file.createNewFile();
                fileReader = new BufferedReader(new FileReader(file));
                fileName = fname;
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "File created for reading: " + fname);
                break;
            case FCREATEW:
                // arg 0: filename
                fname = cmdStruct.params.get(0).getStringValue();
                if (fileReader != null || fileWriter != null) {
                    throw new ParserException(exceptPreface + "File already open: " + fileName);
                }
                file = getFilePath(fname);
                if (file.exists()) {
                    throw new ParserException(exceptPreface + "File already exists: " + fname);
                }
                file.createNewFile();
                file.setWritable(true);
                fileWriter = new PrintWriter(new FileWriter(fname, true));
                fileName = fname;
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "File created for writing: " + fname);
                break;
            case FOPENR:
                // arg 0: filename
                fname = cmdStruct.params.get(0).getStringValue();
                if (fileReader != null || fileWriter != null) {
                    throw new ParserException(exceptPreface + "File already open: " + fileName);
                }
                file = getFilePath(fname);
                if (! file.isFile()) {
                    throw new ParserException(exceptPreface + "File not found: " + fname);
                }
                if (! file.canRead()) {
                    throw new ParserException(exceptPreface + "Invalid file - no read access: " + fname);
                }
                fileReader = new BufferedReader(new FileReader(file));
                fileName = fname;
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "File opened for reading: " + fname);
                break;
            case FOPENW:
                // arg 0: filename
                fname = cmdStruct.params.get(0).getStringValue();
                if (fileReader != null || fileWriter != null) {
                    throw new ParserException(exceptPreface + "File already open: " + fileName);
                }
                file = getFilePath(fname);
                if (! file.isFile()) {
                    throw new ParserException(exceptPreface + "File not found: " + fname);
                }
                if (! file.canWrite()) {
                    throw new ParserException(exceptPreface + "Invalid file - no write access: " + fname);
                }
                fileWriter = new PrintWriter(new FileWriter(fname, true));
                fileName = fname;
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "File opened for writing: " + fname);
                break;
            case FCLOSE:
                // arg 0: filename
                fname = cmdStruct.params.get(0).getStringValue();
                if (fileName == null) {
                    throw new ParserException(exceptPreface + "Filename not found: " + fname);
                }
                fileName = null;
                // close the open file
                if (fileReader != null) {
                    fileReader.close();
                    fileReader = null;
                } else if (fileWriter != null) {
                    fileWriter.close();
                    fileWriter = null;
                } else {
                    throw new ParserException(exceptPreface + "File not open: " + fname);
                }
                break;
            case FREAD:
                // arg 0: number of lines to read
                int count;
                count = cmdStruct.params.get(0).getIntegerValue().intValue();
                if (fileReader == null) {
                    throw new ParserException(exceptPreface + "Read file not open");
                }
                try {
                    // add the lines to $RESPONSE parameter
                    for (int ix = 0; ix < count; ix++) {
                        String line = fileReader.readLine();
                        if (line == null)
                            break;
                        VarArray.putResponseValue(line);
                    }
                } catch (IOException ex) {
                    throw new IOException(exceptPreface + ex);
                }
                break;
            case FWRITE:
                // arg 0: text to write
                text  = cmdStruct.params.get(0).getStringValue();
                if (fileWriter == null) {
                    throw new ParserException(exceptPreface + "Write file not open");
                }
                fileWriter.println(text);
                break;
            case ALLOCATE:
                // nothing to do
                break;
            case SET:
                // the 1st 3 entries are always required: the param name, the equate sign and the value to set
                //   and we ignore the equate sign at offset 1.
                // for Boolean case, we have 1 or 2 additional entries that may be added
                ParameterStruct parmRef = cmdStruct.params.get(0);
                ParameterStruct parm1   = cmdStruct.params.get(2);
                ParameterStruct parm2   = cmdStruct.params.size() > 3 ? cmdStruct.params.get(3) : null;
                ParameterStruct parm3   = cmdStruct.params.size() > 4 ? cmdStruct.params.get(4) : null;
                String parmName = parmRef.getVariableRefName();
                ParameterStruct.ParamType type = parmRef.getVariableRefType();
                if (type == null || parmName == null) {
                    parmName = parmRef.getStringValue();
                    type = ParameterStruct.ParamType.String;
                }
                // make sure we are converting to the type of the reference parameter
                switch (type) {
                    case ParameterStruct.ParamType.Integer:
                        Long result = getIntegerArg (parm1);
                        Variables.modifyIntegerVariable(parmName, result);
                        break;
                    case ParameterStruct.ParamType.Unsigned:
                        result = getIntegerArg (parm1);
                        result &= 0xFFFFFFFF;
                        Variables.modifyUnsignedVariable(parmName, result);
                        break;
                    case ParameterStruct.ParamType.Boolean:
                        Boolean bResult = getComparison(parm1, parm2, parm3);
                        frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "Boolean Variable " + parmName + " = " + bResult);
                        break;

                    case ParameterStruct.ParamType.IntArray:
                        VarArray.setIntArrayVariable(parmName, parm1.getIntArray());
                        break;
                    case ParameterStruct.ParamType.StrArray:
                        VarArray.setStrArrayVariable(parmName, parm1.getStrArray());
                        break;
                    case ParameterStruct.ParamType.String:
                        // The entries should be a list of 1 or more Strings to concatenate into 1
                        // (any parameter references should have been converted to their appropriate value
                        //  at the begining of the execution phase)
                        String concat = getStringArg (cmdStruct.params, 2);
                        Variables.modifyStringVariable(parmName, concat);
                        break;
                    default:
                        throw new ParserException(exceptPreface + parmRef.getVariableRefType().toString() +
                                                " Invalid data type: " + type);
                }
                break;

            // TODO: these are the Array-only commands
            case INSERT:
                ParameterStruct parmValue;
                parmRef   = cmdStruct.params.get(0); // element 0 is the param ref to be appended to
                parmValue = cmdStruct.params.get(1); // element 1 is the value being appended
                parmName = getArrayAssignment(parmRef);
                ParameterStruct.ParamType parmType = Variables.getVariableTypeFromName (parmName);
                String strValue;
                switch (parmType) {
                    case IntArray:
                        Long result = getIntegerArg (parmValue);
                        strValue = result.toString();
                        break;
                    case StrArray:
                    case String:
                        strValue = getStringArg (cmdStruct.params, 1);
                        break;
                    default:
                        throw new ParserException(exceptPreface + "Invalid parameter type: " + parmRef.getParamType());
                }

                boolean bSuccess = VarArray.arrayInsertEntry (parmName, 0, strValue);
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + " parameter ref not found: " + parmName);
                }

            case APPEND:
                parmRef   = cmdStruct.params.get(0); // element 0 is the param ref to be appended to
                parmValue = cmdStruct.params.get(1); // element 1 is the value being appended
                parmName = getArrayAssignment(parmRef);
                parmType = Variables.getVariableTypeFromName (parmName);
                switch (parmType) {
                    case IntArray:
                        Long result = getIntegerArg (parmValue);
                        strValue = result.toString();
                        break;
                    case StrArray:
                        strValue = getStringArg (cmdStruct.params, 1);
                        break;
                    default:
                        throw new ParserException(exceptPreface + "Invalid parameter type: " + parmType);
                }

                bSuccess = VarArray.arrayAppendEntry (parmName, strValue);
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + " parameter ref not found: " + parmName);
                }
                break;
            case MODIFY:
                // ParamName, Index (Integer), Value (String or Integer)
                ParameterStruct parmIndex;
                parmRef   = cmdStruct.params.get(0); // element 0 is the param ref to be modified
                parmIndex = cmdStruct.params.get(1); // element 1 is the index element being modified
                parmValue = cmdStruct.params.get(2); // element 2 is the value to set the entry to
                parmName  = getArrayAssignment(parmRef);
                parmType  = Variables.getVariableTypeFromName (parmName);
                int index = parmIndex.getIntegerValue().intValue();

                switch (parmType) {
                    case IntArray:
                        Long result = getIntegerArg (parmValue);
                        strValue = result.toString();
                        break;
                    case StrArray:
                        strValue = getStringArg (cmdStruct.params, 2);
                        break;
                    default:
                        throw new ParserException(exceptPreface + "Invalid parameter type: " + parmRef.getParamType());
                }
                
                bSuccess = VarArray.arrayModifyEntry (parmName, index, strValue);
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + " parameter ref not found: " + parmName);
                }
                break;
            case REMOVE:
                // ParamName, Index (Integer)
                parmRef   = cmdStruct.params.get(0); // element 0 is the param ref to be modified
                parmIndex = cmdStruct.params.get(1); // element 1 is the index element being removed
                parmName  = getArrayAssignment(parmRef);
                index = parmIndex.getIntegerValue().intValue();
                bSuccess = VarArray.arrayRemoveEntries (parmName, index, 1);
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + " parameter ref not found: " + parmName);
                }
                break;
            case TRUNCATE:
                // ParamName, Count (Integer - optional)
                parmRef = cmdStruct.params.get(0); // element 0 is the param ref to be modified
                parmName = getArrayAssignment(parmRef);
                int size = VarArray.getArraySize(parmName);
                int iCount = 1;
                if (cmdStruct.params.size() > 1) {
                    parmIndex = cmdStruct.params.get(1); // element 1 is the (optional) number of entries being removed
                    iCount = parmIndex.getIntegerValue().intValue();
                    if (iCount > size) {
                        throw new ParserException(exceptPreface + "item count " + iCount + " exceeds size of " + parmName);
                    }
                }
                int iStart = size - iCount;
                bSuccess = VarArray.arrayRemoveEntries (parmName, iStart, iCount);
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + " parameter ref not found: " + parmName);
                }
                break;
            case POP:
                // ParamName, Index (Integer - optional)
                parmRef = cmdStruct.params.get(0); // element 0 is the param ref to be modified
                parmName = getArrayAssignment(parmRef);
                size = VarArray.getArraySize(parmRef.getStringValue());
                iCount = 1;
                iStart = 0;
                if (cmdStruct.params.size() > 1) {
                    parmIndex = cmdStruct.params.get(1); // element 1 is the (optional) number of entries being removed
                    iCount = parmIndex.getIntegerValue().intValue();
                    if (iCount > size) {
                        throw new ParserException(exceptPreface + "item count " + iCount + " exceeds size of " + parmName);
                    }
                }
                bSuccess = VarArray.arrayRemoveEntries (parmName, iStart, iCount);
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + " parameter ref not found: " + parmName);
                }
                break;
            case CLEAR:
                // ParamName
                parmRef = cmdStruct.params.get(0); // element 0 is the param ref to be modified
                parmName = getArrayAssignment(parmRef);
                VarArray.arrayRemoveAll(parmName);
                break;
                
            case FILTER:
                // ParamName or RESET, 1 (optional) the filter string
                parmRef = cmdStruct.params.get(0); // element 0 is the param ref or RESET
                parmName = getArrayAssignment(parmRef);
                parmType = Variables.getVariableTypeFromName (parmName);
                if (parmName.contentEquals("RESET")) {
                    VarArray.arrayFilterReset();
                } else {
                    switch (parmType) {
                        case StrArray:
                            filter = cmdStruct.params.get(1).getStringValue();
                            String opts = "NONE";
                            if (cmdStruct.params.size() == 3) {
                                opts = cmdStruct.params.get(2).getStringValue();
                            }
                            VarArray.arrayFilterString(parmName, filter, opts);
                            break;
                        case IntArray:
                            String compSign = cmdStruct.params.get(1).getStringValue();
                            Long iValue = cmdStruct.params.get(2).getIntegerValue();
                            VarArray.arrayFilterInt(parmName, compSign, iValue);
                            break;
                        default:
                            throw new ParserException(exceptPreface + "Invalid data type for FILTER: " + parmType);
                    }
                }
                break;
            case IF:
                // check status to see if true of false.
                parm1 = cmdStruct.params.get(0);
                parm2 = cmdStruct.params.size() > 1 ? cmdStruct.params.get(1) : null;
                parm3 = cmdStruct.params.size() > 2 ? cmdStruct.params.get(2) : null;
                Boolean bResult = getComparison(parm1, parm2, parm3);
                
                // add entry to the current loop stack
                IFStruct.stackPush(cmdIndex);
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "new IF level " + IFStruct.getStackSize() + " " +
                        parm1.getStringValue() + " " + ((parm2 != null) ? parm2.getStringValue() : "") +
                                                 " " + ((parm3 != null) ? parm3.getStringValue() : ""));

                IFStruct ifInfo = IFStruct.getIfListEntry(cmdIndex);
                if (! bResult) {
                    newIndex = ifInfo.getElseIndex(cmdIndex);
                    frame.outputInfoMsg(STATUS_DEBUG, debugPreface + "IFCONDITION skipped");
                    frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "goto next IF case @ " + newIndex);
                    ifInfo.clearConditionMet();     // starting new IF and condition was not met
                } else {
                    frame.outputInfoMsg(STATUS_DEBUG, debugPreface + "IFCONDITION executed");
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
                    frame.outputInfoMsg(STATUS_DEBUG, debugPreface + "IFCONDITION already met");
                    frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "goto ENDIF @ " + newIndex);
                } else {
                    frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "IF level " + IFStruct.getStackSize() + " " + cmdStruct.command + " on line " + cmdIndex);
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
                    frame.outputInfoMsg(STATUS_DEBUG, debugPreface + "IFCONDITION already met");
                    frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "goto ENDIF @ " + newIndex);
                } else {
                    // check status to see if true of false.
                    parm1 = cmdStruct.params.get(0);
                    parm2 = cmdStruct.params.size() > 1 ? cmdStruct.params.get(1) : null;
                    parm3 = cmdStruct.params.size() > 2 ? cmdStruct.params.get(2) : null;
                    bResult = getComparison(parm1, parm2, parm3);

                    // add entry to the current loop stack
                    frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "new IF level " + IFStruct.getStackSize() + " " +
                            parm1.getStringValue() + " " + ((parm2 != null) ? parm2.getStringValue() : "") +
                                                     " " + ((parm3 != null) ? parm3.getStringValue() : ""));

                    if (! bResult) {
                        newIndex = ifInfo.getElseIndex(cmdIndex);
                        frame.outputInfoMsg(STATUS_DEBUG, debugPreface + "IFCONDITION skipped");
                        frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "goto next IF case @ " + newIndex);
                    } else {
                        frame.outputInfoMsg(STATUS_DEBUG, debugPreface + "IFCONDITION executed");
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
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "new IF level " + IFStruct.getStackSize() + ": " + cmdStruct.command + " on line " + cmdIndex);
                break;
            case FOR:
                String loopName  = cmdStruct.params.get(0).getStringValue();
                curLoopId = new LoopId(loopName, cmdIndex);
                newIndex = LoopParam.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                    
                // add entry to the current loop stack
                LoopStruct.pushStack(curLoopId);
                int loopSize = LoopStruct.getStackSize();
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "new FOR Loop level " + loopSize+ " parameter " + loopName + " index @ " + cmdIndex);
                break;
            case BREAK:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(exceptPreface + "Received when not in a FOR loop");
                }
                newIndex = LoopParam.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + cmdStruct.command.toString() + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                break;
            case CONTINUE:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(exceptPreface + "Received when not in a FOR loop");
                }
                newIndex = LoopParam.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + cmdStruct.command.toString() + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                break;
            case NEXT:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(exceptPreface + "Received when not in a FOR loop");
                }
                newIndex = LoopParam.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + cmdStruct.command.toString() + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                break;
            case ENDFOR:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(exceptPreface + "Received when not in a FOR loop");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + cmdStruct.command.toString() + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
//                curLoopId = LoopStruct.popStack();
                LoopStruct.popStack();
                loopSize = LoopStruct.getStackSize();
                if (loopSize > 0) {
                    curLoopId = LoopStruct.peekStack();
                }
                if (curLoopId == null) {
                    frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "All loops completed so far");
                } else {
                    frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "Current Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                }
                break;
            case RUN:
                // fall through...
            default:
                cmdOptionParser.runCmdOption (cmdStruct);
                break;
        }
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + exceptPreface);
        }
        
        // by default, the command will proceed to the next command
        if (newIndex >= 0) {
            cmdIndex = newIndex;
        } else {
            cmdIndex++;
        }
        
        return cmdIndex;
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
}
