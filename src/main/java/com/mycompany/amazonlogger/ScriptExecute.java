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
    
    private static final String CLASS_NAME = "ScriptExecute";
    
    // this handles the command line options via the RUN command
    private final CmdOptions cmdOptionParser;
    
    // identifies the current loopStack entry.
    private LoopId curLoopId = null;
    
    // the map of column names to column indices in the sheet
    // for now, we only allow 1 file open at a time (either a read or a write file)
    private BufferedReader fileReader = null;
    private PrintWriter    fileWriter = null;
    private String fileName;

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
        String functionId = CLASS_NAME + ".executeProgramCommand: ";
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
                if (cmdStruct.params.get(0).getParamType() == ParameterStruct.ParamType.StringArray) {
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
            case FEXISTS:
                // arg 0: filename
                String fname = cmdStruct.params.get(0).getStringValue();
                fname = Utils.getTestPath() + "/" + fname;
                File file = new File(fname);
                Long value = file.exists() ? 1L : 0L;
                ParameterStruct.putResultValue(value);
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "File " + file + " exists = " + value);
                break;
            case FDELETE:
                // arg 0: filename
                fname = cmdStruct.params.get(0).getStringValue();
                fname = Utils.getTestPath() + "/" + fname;
                file = new File(fname);
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
                fname = Utils.getTestPath() + "/" + fname;
                file = new File(fname);
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
                fname = Utils.getTestPath() + "/" + fname;
                file = new File(fname);
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
                fname = Utils.getTestPath() + "/" + fname;
                file = new File(fname);
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
                fname = Utils.getTestPath() + "/" + fname;
                file = new File(fname);
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
                        ParameterStruct.putResponseValue(line);
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
            case DEFINE:
                // nothing to do
                break;
            case SET:
                ParameterStruct parmRef   = cmdStruct.params.get(0); // element 0 is the param ref to assigned a value to
                ParameterStruct parmEqu   = cmdStruct.params.get(1); // element 1 is the assignment type (could be: =, +=, -=, etc)
                ParameterStruct parmValue = cmdStruct.params.get(2); // element 2 is the value being assigned to it
                String parmName = parmRef.getVariableRefName();
                ParameterStruct.ParamType type = parmRef.getVariableRefType();
                if (type == null || parmName == null) {
                    parmName = parmRef.getStringValue();
                    type = ParameterStruct.ParamType.String;
                }
                // make sure we are converting to the type of the reference parameter
                switch (type) {
                    case ParameterStruct.ParamType.Integer:
                    case ParameterStruct.ParamType.Unsigned:
                        // TODO: use 'parmEqu' (the type of assignment) in the calculation
                        Long result;
                        if (parmValue.isCalculation()) {
                            result = parmValue.getCalculationValue(type);
                        } else {
                            result = parmValue.getIntegerValue();
                        }
                        if (type == ParameterStruct.ParamType.Unsigned) {
                            result &= 0xFFFFFFFF;
                        }
                        ParameterStruct.modifyIntegerVariable(parmName, result);
                        break;
                    case ParameterStruct.ParamType.Boolean:
                        // TODO: allow comparison on right-hand assignment of Boolean
                        ParameterStruct.modifyBooleanVariable(parmName, parmValue.getBooleanValue());
                        break;
                    case ParameterStruct.ParamType.IntArray:
                        ParameterStruct.setIntArrayVariable(parmName, parmValue.getIntArray());
                        break;
                    case ParameterStruct.ParamType.StringArray:
                        ParameterStruct.setStrArrayVariable(parmName, parmValue.getStrArray());
                        break;
                    default:
                    case ParameterStruct.ParamType.String:
                        // The entries should be a list of 1 or more Strings to concatenate into 1
                        // (any parameter references should have been converted to their appropriate value
                        //  at the begining of the execution phase)
                        String concat = "";
                        for (int ix = 2; ix < cmdStruct.params.size(); ix++) {
                            concat += cmdStruct.params.get(ix).getStringValue();
                        }
                        ParameterStruct.modifyStringVariable(parmName, concat);
                        break;
                }
                break;

            // TODO: these are the Array-only commands
            case INSERT:
                // ParamName, Value (String or Integer)
                parmRef   = cmdStruct.params.get(0); // element 0 is the param ref to be inserted into
                parmValue = cmdStruct.params.get(1); // element 1 is the value being inserted
                boolean bSuccess = ParameterStruct.arrayInsertEntry (parmRef.getStringValue(), 0, parmValue.getStringValue());
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + parmRef.getVariableRefType().toString() +
                                                " parameter ref not found: " + parmRef.getStringValue());
                }
                break;
            case APPEND:
                // ParamName, Value (String or Integer)
                parmRef   = cmdStruct.params.get(0); // element 0 is the param ref to be appended to
                parmValue = cmdStruct.params.get(1); // element 1 is the value being appended
                bSuccess = ParameterStruct.arrayAppendEntry (parmRef.getStringValue(), parmValue.getStringValue());
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + parmRef.getVariableRefType().toString() +
                                                " parameter ref not found: " + parmRef.getStringValue());
                }
                break;
            case MODIFY:
                // ParamName, Index (Integer), Value (String or Integer)
                ParameterStruct parmIndex;
                parmRef   = cmdStruct.params.get(0); // element 0 is the param ref to be modified
                parmIndex = cmdStruct.params.get(1); // element 1 is the index element being modified
                parmValue = cmdStruct.params.get(2); // element 2 is the value to set the entry to
                int index = parmIndex.getIntegerValue().intValue();
                bSuccess = ParameterStruct.arrayModifyEntry (parmRef.getStringValue(), index, parmValue.getStringValue());
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + parmRef.getVariableRefType().toString() +
                                                " parameter ref not found: " + parmRef.getStringValue());
                }
                break;
            case REMOVE:
                // ParamName, Index (Integer)
                parmRef   = cmdStruct.params.get(0); // element 0 is the param ref to be modified
                parmIndex = cmdStruct.params.get(1); // element 1 is the index element being removed
                index = parmIndex.getIntegerValue().intValue();
                bSuccess = ParameterStruct.arrayRemoveEntries (parmRef.getStringValue(), index, 1);
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + parmRef.getVariableRefType().toString() +
                                                " parameter ref not found: " + parmRef.getStringValue());
                }
                break;
            case TRUNCATE:
                // ParamName, Count (Integer - optional)
                parmRef = cmdStruct.params.get(0); // element 0 is the param ref to be modified
                int size = ParameterStruct.getArraySize(parmRef.getStringValue());
                int iCount = 1;
                if (cmdStruct.params.size() > 1) {
                    parmIndex = cmdStruct.params.get(1); // element 1 is the (optional) number of entries being removed
                    iCount = parmIndex.getIntegerValue().intValue();
                    if (iCount > size) {
                        throw new ParserException(exceptPreface + "item count " + iCount +
                                " exceeds size of " + parmRef.getVariableRefType().toString());
                    }
                }
                int iStart = size - iCount;
                bSuccess = ParameterStruct.arrayRemoveEntries (parmRef.getStringValue(), iStart, iCount);
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + parmRef.getVariableRefType().toString() +
                                                " parameter ref not found: " + parmRef.getStringValue());
                }
                break;
            case POP:
                // ParamName, Index (Integer - optional)
                parmRef = cmdStruct.params.get(0); // element 0 is the param ref to be modified
                size = ParameterStruct.getArraySize(parmRef.getStringValue());
                iCount = 1;
                iStart = 0;
                if (cmdStruct.params.size() > 1) {
                    parmIndex = cmdStruct.params.get(1); // element 1 is the (optional) number of entries being removed
                    iCount = parmIndex.getIntegerValue().intValue();
                    if (iCount > size) {
                        throw new ParserException(exceptPreface + "item count " + iCount +
                                " exceeds size of " + parmRef.getVariableRefType().toString());
                    }
                }
                bSuccess = ParameterStruct.arrayRemoveEntries (parmRef.getStringValue(), iStart, iCount);
                if (!bSuccess) {
                    throw new ParserException(exceptPreface + parmRef.getVariableRefType().toString() +
                                                " parameter ref not found: " + parmRef.getStringValue());
                }
                break;
            case CLEAR:
                // ParamName
                parmRef = cmdStruct.params.get(0); // element 0 is the param ref to be modified
                parmName = parmRef.getStringValue();
                ParameterStruct.arrayRemoveAll(parmName);
                break;
                
            case IF:
                ParameterStruct parm1 = cmdStruct.params.get(0);
                String comp           = cmdStruct.params.get(1).getStringValue();
                ParameterStruct parm2 = cmdStruct.params.get(2);

                // add entry to the current loop stack
                IFStruct.stackPush(cmdIndex);
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "new IF level " + IFStruct.getStackSize() + " " + parm1.getStringValue() + " " + comp + " " + parm2.getStringValue());

                // check status to see if true of false.
                boolean bBranch;
                if ((parm1.getParamType() == ParameterStruct.ParamType.Integer || parm1.getParamType() == ParameterStruct.ParamType.Unsigned) &&
                    (parm2.getParamType() == ParameterStruct.ParamType.Integer || parm2.getParamType() == ParameterStruct.ParamType.Unsigned)    ) {
                    bBranch = Utils.compareParameterValues (parm1.getIntegerValue(), parm2.getIntegerValue(), comp);
                } else {
                    bBranch = Utils.compareParameterValues (parm1.getStringValue(), parm2.getStringValue(), comp);
                }
                IFStruct ifInfo = IFStruct.getIfListEntry(cmdIndex);
                if (bBranch) {
                    newIndex = ifInfo.getElseIndex(cmdIndex);
                    frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "goto next IF case @ " + newIndex);
                } else {
                    ifInfo.setConditionMet(); // we are running the condition, so ELSEs will be skipped
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
                    frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "goto ENDIF @ " + newIndex);
                } else {
                    parm1 = cmdStruct.params.get(0);
                    comp  = cmdStruct.params.get(1).getStringValue();
                    parm2 = cmdStruct.params.get(2);
                    frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "IF level " + IFStruct.getStackSize() + ": " + parm1.getStringValue() + " " + comp + " " + parm2.getStringValue());

                    // check status to see if true of false.
                    if ((parm1.getParamType() == ParameterStruct.ParamType.Integer || parm1.getParamType() == ParameterStruct.ParamType.Unsigned) &&
                        (parm2.getParamType() == ParameterStruct.ParamType.Integer || parm2.getParamType() == ParameterStruct.ParamType.Unsigned)    ) {
                        bBranch = Utils.compareParameterValues (parm1.getIntegerValue(), parm2.getIntegerValue(), comp);
                    } else {
                        bBranch = Utils.compareParameterValues (parm1.getStringValue(), parm2.getStringValue(), comp);
                    }
                    if (bBranch) {
                        newIndex = ifInfo.getElseIndex(cmdIndex);
                        frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "goto next IF case @ " + newIndex);
                    } else {
                        ifInfo.setConditionMet(); // we are running the condition, so ELSEs will be skipped
                    }
                }
                break;
            case ENDIF:
                if (IFStruct.isIfStackEnpty()) {
                    throw new ParserException(exceptPreface + "Received when not in a IF structure");
                }
                // save the current command index in the current if structure
                IFStruct.stackPop();
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + "new IF level " + IFStruct.getStackSize() + ": " + cmdStruct.command + " on line " + cmdIndex);
                break;
            case FOR:
                String loopName  = cmdStruct.params.get(0).getStringValue();
                curLoopId = new LoopId(loopName, cmdIndex);
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                    
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
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + cmdStruct.command.toString() + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                break;
            case CONTINUE:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(exceptPreface + "Received when not in a FOR loop");
                }
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
                frame.outputInfoMsg(STATUS_PROGRAM, debugPreface + cmdStruct.command.toString() + " command for Loop level " + loopSize
                                    + " parameter " + curLoopId.name + " index @ " + curLoopId.index);
                break;
            case NEXT:
                loopSize = LoopStruct.getStackSize();
                if (loopSize == 0 || curLoopId == null) {
                    throw new ParserException(exceptPreface + "Received when not in a FOR loop");
                }
                newIndex = ParameterStruct.getLoopNextIndex (cmdStruct.command, cmdIndex, curLoopId);
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
                curLoopId = LoopStruct.popStack();
                loopSize = LoopStruct.getStackSize();
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

}
