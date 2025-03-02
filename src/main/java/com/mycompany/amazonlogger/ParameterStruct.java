package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PARSER;
import java.io.File;
import java.util.HashMap;

/**
 * This class defines the structure of the parameters stored for the commands.
 * 
 * It allows Integer, Boolean and String data types and has a paramType that
 * corresponds to the data type:
 *  'I' - Integer (signed)
 *  'U' - Integer (unsigned and can also be specified as a hex value using 'x' or '0x')
 *  'B' - Boolean (true/false or numeric where 0 = false, all others are true)
 *  'S' - String (single word)
 *  'L' - String list (multi-word)
 *  'D' - String inferring a directory located off the base directory
 *  'F' - String inferring a file name
 * 
 * @author dan
 */
public final class ParameterStruct {

    private static final String CLASS_NAME = "CommandParser";
    
    private String   strParam;      // value for the String  param types
    private Integer  intParam;      // value for the Integer param types
    private Boolean  boolParam;     // value for the Boolean param types
    private char     paramType;     // the corresponding data type stored
    
    // saved static parameters
    private static String  strResponse = "";    // response from last RUN command
    private static Integer intResult = 0;       // result of last CALC command
    private static final HashMap<String, String>  strParams = new HashMap<>();
    private static final HashMap<String, Integer> intParams = new HashMap<>();
    
    /**
     * Creates a parameter having the specified characteristics
     * 
     * @param objValue - the parameter value to use
     * @param dataType - the parameter type desired
     * 
     * @throws ParserException
     */
    public ParameterStruct (Object objValue, char dataType) throws ParserException {
        switch (objValue.getClass().toString()) {
            case "class java.lang.Integer":
                setIntegerValue ((Integer) objValue, dataType);
                break;
            case "class java.lang.Boolean":
                setBooleanValue ((Boolean) objValue, dataType);
                break;
            case "class java.lang.String":
            default:
                setStringValue ((String) objValue, dataType);
                break;
        }
    }
        
    /**
     * Creates a parameter having the specified characteristics
     * 
     * @param strValue - the parameter value to use (can be a parameter reference)
     * @param dataType - the parameter type desired
     * 
     * @throws ParserException
     */
    public ParameterStruct (String strValue, char dataType) throws ParserException {
         String functionId = CLASS_NAME + " (new): ";
        
        if (strValue == null) {
            throw new ParserException(functionId + "Null parameter value passed");
        }
        dataType = Character.toUpperCase(dataType);
        String nextArg = getNextWord (strValue);

        // if entry is a parameter, verification will be a runtime check
        if (strValue.startsWith("$")) {
            setStringValue (nextArg, 'S');
            return;
        }
        
        switch (dataType) {
            case 'U':
                Integer intVal = Utils.getHexValue (nextArg);
                if (intVal == null) {
                    intVal = Utils.getIntValue (nextArg);
                }
                setIntegerValue (intVal, dataType);
                break;
            case 'I':
                intVal = Utils.getIntValue (nextArg);
                setIntegerValue (intVal, dataType);
                break;
            case 'B':
                Boolean boolVal = Utils.getBooleanValue (nextArg);
                setBooleanValue (boolVal, dataType);
                break;
            case 'S':
            case 'D':
            case 'F':
                setStringValue (nextArg, dataType);
                break;
            case 'L':
                // lists take the remaining string info in the command line
                nextArg = strValue;
                setStringValue (nextArg, dataType);
                break;
            default:
                throw new ParserException(functionId + "Invalid data type for param: " + dataType);
        }
            
        // do additional check for specific cases
        if (dataType == 'D')
            checkDir (nextArg);
        else if (dataType == 'F')
            checkFilename (nextArg);
    }
    
    /**
     * returns the parameter type (I, U, B, S, L, D, F)
     * 
     * @return the parameter type
     */
    public char getParamType () {
        return paramType;
    }
       
    /**
     * returns the String data value
     * 
     * @return the String value
     */
    public String getStringValue () {
        return strParam;
    }
        
    /**
     * returns the Integer data value
     * 
     * @return the Integer value
     */
    public Integer getIntegerValue () {
        return intParam;
    }
        
    /**
     * returns the Boolean data value
     * 
     * @return the Boolean value
     */
    public Boolean getBooleanValue () {
        return boolParam;
    }

    /**
     * converts the current parameter value to the specifed type
     * 
     * @param dataType - type of data to convert the current value to
     * 
     * @throws ParserException 
     */
    public void convertType (char dataType) throws ParserException {
        String functionId = CLASS_NAME + ".convertType: ";
        
        switch (Character.toUpperCase(dataType)) {
            case 'U', 'I' -> 
                setIntegerValue(unpackIntegerValue(), dataType);
            case 'B' -> 
                setBooleanValue(unpackBooleanValue(), dataType);
            case 'S', 'D', 'F', 'L' ->
                setStringValue(unpackStringValue(), dataType);
            default ->
                throw new ParserException(functionId + "Invalid data type: " + dataType);
        }
    }
    
    /**
     * gets the String value of the current parameter, which may be a saved parameter
     * 
     * @return the String value, which may be a parameter reference & may require type conversion
     * 
     * @throws ParserException 
     */
    public String unpackStringValue () throws ParserException {
        // check for entry in params first
        String strVal = findStringParam(strParam);
        if (strVal == null) {
            // nope, use the actual "string"ified value
            strVal = switch (paramType) {
                case 'U', 'I' -> intParam.toString();
                case 'B'      -> boolParam.toString();
                default       -> strParam;  // default to String type
            };
        }
        frame.outputInfoMsg(STATUS_PARSER, "    unpacked '" + paramType + "' value: '" + strVal + "'");
        return strVal;
    }
        
    /**
     * gets the Integer value of the current parameter, which may be a saved parameter
     * 
     * @return the Integer value, which may be a parameter reference & may require type conversion
     * 
     * @throws ParserException 
     */
    public Integer unpackIntegerValue () throws ParserException {
        // check for entry in params first
        Integer intVal = findIntegerParam(strParam);
        if (intVal == null) {
            // nope, use the actual "int"ified value
            switch (paramType) {
                case 'U', 'I' -> // fall through...
                    intVal  = intParam;
                case 'B' -> intVal = (boolParam == true) ? 1 : 0;
                default -> {
                    // default to String type
                    intVal = Utils.getHexValue(strParam);
                    if (intVal == null)
                        intVal = Utils.getIntValue(strParam);
                }
            }
            // fall through...
                    }
        frame.outputInfoMsg(STATUS_PARSER, "    unpacked '" + paramType + "' value: " + intVal);
        return intVal;
    }
        
    /**
     * gets the Boolean value of the current parameter, which may be a saved parameter
     * 
     * @return the Boolean value, which may be a parameter reference & may require type conversion
     * 
     * @throws ParserException 
     */
    public Boolean unpackBooleanValue () throws ParserException {
        // check for entry in params first
        Boolean boolVal;
        String strVal = findStringParam(strParam);
        if (strVal != null) {
            boolVal = Utils.getBooleanValue(strVal);
        }
        else {
            // nope, use the actual stringified value
            boolVal = switch (paramType) {
                case 'U', 'I' -> intParam != 0;
                case 'B' -> boolParam;
                default -> Utils.getBooleanValue(strParam);
            }; // fall through...
            // default to String type
        }
        frame.outputInfoMsg(STATUS_PARSER, "    unpacked '" + paramType + "' value: " + boolVal);
        return boolVal;
    }

    /**
     * sets the current parameter to the specified String value, which may be a saved parameter.
     * Performs and data type conversion necessary.
     * 
     * @param strVal   - the String value (could be a parameter reference)
     * @param dataType - the data type the parameter should be
     * 
     * @throws ParserException 
     */
    public void setStringValue (String strVal, char dataType) throws ParserException {
        String functionId = CLASS_NAME + ".setStringValue: ";
        
        intParam = null;
        boolParam = null;
        strParam = strVal;

        // if it is a parameter, save the param name as a string and allow it
        if (strVal.startsWith("$")) {
            frame.outputInfoMsg(STATUS_PARSER, "     'S' param: " + strParam);
            paramType = 'S';
            return;
        }
        try {
            switch (dataType) {
                case 'I' -> intParam = Utils.getIntValue(strVal);
                case 'U' -> {
                    intParam = Utils.getHexValue (strVal);
                    if (intParam == null) {
                        intParam = Utils.getIntValue(strVal);
                        if (intParam < 0) {
                            throw new NumberFormatException("");
                        }
                    }
                }
                case 'B' -> boolParam = Utils.getBooleanValue(strVal);
                default -> {
                }
            }
            // all string types
                    } catch (NumberFormatException ex) {
            throw new ParserException(functionId + "Invalid param data for dataType " + dataType + ": param: " + strVal);
        }
            
        frame.outputInfoMsg(STATUS_PARSER, "     '" + dataType + "' param: " + strParam);
        paramType = dataType;
    }
        
    /**
     * sets the current parameter to the specified Integer value.
     * Performs and data type conversion necessary.
     * 
     * @param intVal   - the Integer value
     * @param dataType - the data type the parameter should be
     * 
     * @throws ParserException 
     */
    public void setIntegerValue (Integer intVal, char dataType) throws ParserException {
        String functionId = CLASS_NAME + ".setIntegerValue: ";
        
        strParam = null;
        boolParam = null;
        intParam = intVal;

        switch (dataType) {
            case 'I' -> {
            }
            case 'U' -> {
                if (intParam < 0) {
                    throw new ParserException(functionId + "Invalid param data for dataType " + dataType + ": param: " + intVal);
                }
            }
            case 'B' ->
                boolParam = intParam != 0;
            default -> // all string types
                strParam = intParam.toString();
        }
            
        paramType = dataType;
        frame.outputInfoMsg(STATUS_PARSER, "     '" + paramType + "' param: " + intParam);
    }
        
    /**
     * sets the current parameter to the specified Boolean value.
     * Performs and data type conversion necessary.
     * 
     * @param boolVal  - the Boolean value
     * @param dataType - the data type the parameter should be
     */
    public void setBooleanValue (Boolean boolVal, char dataType) {
        
        intParam = null;
        strParam = null;
        boolParam = boolVal;

        switch (dataType) {
            case 'I', 'U' ->
                intParam = boolVal ? 1 : 0;
            case 'B' -> {
            }
            default -> // all string types
                strParam = boolParam.toString();
        }
            
        paramType = dataType;
        frame.outputInfoMsg(STATUS_PARSER, "     '" + paramType + "' param: " + boolParam);
    }

    /**
     * returns a String for displaying the current param data type and value.
     * 
     * @return a String indicating the parameter type and value
     */
    public String showParam () {
        String strCommand = " [" + paramType + "]";
        switch (paramType) {
            case 'I', 'U' -> {
                if (intParam == null)
                    strCommand += " (null)";
                else
                    strCommand += " " + intParam.toString();
            }
            case 'B' -> {
                if (boolParam == null)
                    strCommand += " (null)";
                else
                    strCommand += " " + boolParam.toString();
            }
            default -> {
                if (strParam == null)
                    strCommand += " (null)";
                else
                    strCommand += " '" + strParam + "'";
            }
        }
            
        return strCommand;
    }
    
    //========================================================================
    // THESE STATIC METHODS ARE USED FOR ACCESSING THE STATIC PARAMETER VALUES
    //========================================================================

    /**
     * inits the saved parameters
     */
    public static void initParameters () {
        strParams.clear();
        intParams.clear();
        strResponse = "";
        intResult = 0;
    }

    /**
     * set the value of the $RESPONSE parameter
     * 
     * @param value - value to set the response param to
     */
    public static void putResponseValue (String value) {
        strResponse = value;
    }
    
    /**
     * set the value of the $RESULT parameter
     * 
     * @param value - value to set the result param to
     */
    public static void putResultValue (Integer value) {
        intResult = value;
    }

    /**
     * creates a new entry in the String params table and sets the initial value.
     * Indicates if the param was already defined.
     * 
     * @param name  - parameter name
     * @param value - parameter value
     */
    public static void putStringParameter (String name, String value) {
        if (! strParams.containsKey(name)) {
            strParams.put(name, value);
            frame.outputInfoMsg(STATUS_PARSER, "   - Added String parameter " + name + " init to '" + value + "'");
        } else {
            frame.outputInfoMsg(STATUS_PARSER, "   - String parameter " + name + " already defined");
        }
    }

    /**
     * modifies the value of an existing entry in the String params table.
     * Indicates if the param was not found (does NOT create a new entry).
     * 
     * @param name  - parameter name
     * @param value - parameter value
     * 
     * @return true if successful, false if the parameter was not found
     */
    public static boolean modifyStringParameter (String name, String value) {
        if (strParams.containsKey(name)) {
            strParams.replace(name, value);
            frame.outputInfoMsg(STATUS_PARSER, "   - Modified String param: " + name + " = " + value);
            return true;
        }
        return false;
    }

    /**
     * creates a new entry in the Integer params table and sets the initial value.
     * Indicates if the param was already defined.
     * 
     * @param name  - parameter name
     * @param value - parameter value
     */
    public static void putIntegerParameter (String name, Integer value) {
        if (! intParams.containsKey(name)) {
            intParams.put(name, value);
            frame.outputInfoMsg(STATUS_PARSER, "   - Added Integer parameter " + name + " init to '" + value + "'");
        } else {
            frame.outputInfoMsg(STATUS_PARSER, "   - Integer parameter " + name + " already defined");
        }
    }

    /**
     * modifies the value of an existing entry in the Integer params table.
     * Indicates if the param was not found (does NOT create a new entry).
     * 
     * @param name  - parameter name
     * @param value - parameter value
     * 
     * @return true if successful, false if the parameter was not found
     */
    public static boolean modifyIntegerParameter (String name, Integer value) {
        if (intParams.containsKey(name)) {
            intParams.replace(name, value);
            frame.outputInfoMsg(STATUS_PARSER, "   - Modified Integer param: " + name + " = " + value);
            return true;
        }
        return false;
    }
    
    //========================================================================
    // LOCAL METHODS
    //========================================================================

    /**
     * searches for a parameter that references a numeric integer value.
     * The parameter itself may be a String type, but is allowed as long as
     * the data it holds can be converted to an Integer.
     * 
     * @param parmName - name of the parameter (will start with a '$')
     * 
     * @return the Integer value of the specified saved parameter
     * 
     * @throws ParserException 
     */
    private static Integer findIntegerParam (String parmName) throws ParserException {
        String functionId = CLASS_NAME + ".findIntegerParam: ";
       
        Integer intVal = null;
        String strVal = null;

        // first, check if a param is being used
        if (parmName != null && parmName.charAt(0) == '$') {
            parmName = parmName.substring(1); // strip off the leading $
            if (parmName.contentEquals("RESPONSE")) {
                strVal = strResponse;
            }
            else if (parmName.contentEquals("RESULT")) {
                intVal = intResult;
            }
            else if (parmName.startsWith("I_")) {
                if (intParams.containsKey(parmName)) {
                    intVal = intParams.get(parmName);
                } else {
                    throw new ParserException(functionId + "Integer param not found: " + parmName);
                }
            }
            if (intVal == null) {
                if (strParams.containsKey(parmName)) {
                    strVal = strParams.get(parmName);
                } else {
                    throw new ParserException(functionId + "String param not found: " + parmName);
                }
            }
            if (strVal != null) {
                try {
                    intVal = Integer.valueOf(strVal);
                } catch (NumberFormatException ex) {
                    throw new ParserException(functionId + "String value is not an Integer: " + strVal);
                }
            }
        }        
        return intVal;
    }
    
    /**
     * searches for a parameter that references a String value.
     * 
     * @param parmName - name of the parameter (will start with a '$')
     * 
     * @return the String value of the specified saved parameter
     * 
     * @throws ParserException 
     */
    private static String findStringParam (String parmName) throws ParserException {
        String functionId = CLASS_NAME + ".findStringParam: ";
       
        String strVal = null;

        // first, check if a param is being used
        if (parmName != null && parmName.charAt(0) == '$') {
            parmName = parmName.substring(1); // strip off the leading $
            if (parmName.contentEquals("RESPONSE")) {
                strVal = strResponse;
            }
            else if (parmName.contentEquals("RESULT")) {
                strVal = intResult.toString();
            }
            else if (parmName.startsWith("I_")) {
                if (intParams.containsKey(parmName)) {
                    strVal = intParams.get(parmName).toString();
                } else {
                    throw new ParserException(functionId + "Integer param not found: " + parmName);
                }
            }
            if (strVal == null) {
                if (strParams.containsKey(parmName)) {
                    strVal = strParams.get(parmName);
                } else {
                    throw new ParserException(functionId + "String param not found: " + parmName);
                }
            }
        }        
        return strVal;
    }

    /**
     * simple test if a directory path is valid
     * 
     * @param dirname - name of the directory
     * 
     * @return the directory File
     * 
     * @throws ParserException 
     */
    private static File checkDir (String dirname) throws ParserException {
        String functionId = CLASS_NAME + ".checkDir: ";

        if (dirname == null || dirname.isBlank()) {
            throw new ParserException(functionId + "Path name is blank");
        }
        
        File myPath = new File(dirname);
        if (!myPath.isDirectory()) {
            throw new ParserException(functionId + "Path not found: " + dirname);
        }
        frame.outputInfoMsg(UIFrame.STATUS_PARSER, "  Path param valid: " + dirname);
        return myPath;
    }

    /**
     * simple test if a file name is valid
     * 
     * @param fname - name of the file (referenced from base path)
     * 
     * @return the File
     * 
     * @throws ParserException 
     */
    private static File checkFilename (String fname) throws ParserException {
        String functionId = CLASS_NAME + ".checkFilename: ";
        
        if (fname == null || fname.isBlank()) {
            throw new ParserException(functionId + "Invalid filename is blank");
        }
        
        fname = Utils.getTestPath() + "/" + fname;
        File myFile = new File(fname);
        if (!myFile.canRead()) {
            throw new ParserException(functionId + "Invalid file - no read access: " + fname);
        }
        frame.outputInfoMsg(UIFrame.STATUS_PARSER, "  File exists & is readable: " + fname);
        return myFile;
    }

    /**
     * extracts the next word from a string of words
     * 
     * @param line - the String containing 0 or more words
     * 
     * @return the next word in the string
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
