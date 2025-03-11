/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class defines the structure of the parameters stored for the commands.
 * 
 * It allows Integer, Boolean and String data types and has a paramType that
 * corresponds to the data type.
 * 
 * @author dan
 */
public final class ParameterStruct {

    private static final String CLASS_NAME = "ParameterStruct";
    
    private String   strParam;      // value for the String  param types
    private Integer  intParam;      // value for the Integer param types
    private Boolean  boolParam;     // value for the Boolean param types
    private char     paramType;     // the corresponding data type stored
    
    // saved static parameters
    private static String  strResponse = "";    // response from last RUN command
    private static Integer intResult = 0;       // result of last CALC command
    private static final HashMap<String, String>  strParams = new HashMap<>();
    private static final HashMap<String, Integer> intParams = new HashMap<>();
    
    // for loops, the loopParams will find the loop parameter for the loop at the
    // specified command index. In order to determine if we have a nested loop
    // using the same param name, we use loopNames that contains an array of
    // all the loop params having the same name. When a name is being reused,
    // we must verify that all the occurrances of FOR loops using that name
    // are all completely defined, meaning that the ENDFOR has already been found
    // for each one. When compiling, we simply proceed through the instructions
    // sequentially, so if all current uses of the FOR parameter indicate they
    // are complete (i.e. ENDFOR has been found), we are safe to reuse the loop name.
    private static final HashMap<ScriptParser.LoopId, LoopStruct> loopParams = new HashMap<>();
    private static final HashMap<String, ArrayList<ScriptParser.LoopId>> loopNames = new HashMap<>();

    /**
     * Creates a parameter and determines the data type.
     * This is only used in the execution phase, so if a parameter value is found,
     *  do a replacement of it.
     * Note that the data is always stored in String format.
     * 
     * I = signed integer   ( LT 0 )
     * U = unsigned integer ( GE 0 )
     * B = boolean
     * S = string (single word)
     * L = list (multi-word string)
     * 
     * @param objValue - the parameter value to use
     * 
     * @throws ParserException
     */
    public ParameterStruct (Object objValue) throws ParserException {
         String functionId = CLASS_NAME + " (new Object): ";
        
        String classType = objValue.getClass().toString();
        switch (classType) {
            case "class java.lang.Integer":
                Integer iVal = (Integer) objValue;
                strParam = iVal.toString();
                if (iVal >= 0)
                    paramType = 'U';
                else
                    paramType = 'I';
                break;
            case "class java.lang.Boolean":
                Boolean bValue = (Boolean) objValue;
                strParam = bValue.toString();
                paramType = 'B';
                break;
            case "class java.lang.String":
                strParam = (String) objValue;
                if (strParam.equalsIgnoreCase("TRUE") || strParam.equalsIgnoreCase("FALSE")) {
                    paramType = 'B';
                }
                try {
                    iVal = Utils.getIntValue (strParam);
                    if (iVal >= 0)
                        paramType = 'U';
                    else
                        paramType = 'I';
                } catch (ParserException ex) {
                    if (strParam.startsWith("$")) {
                        paramType = 'R';
                        String parmVal = findStringParam (strParam);
                        if (parmVal != null) {
                            // parameter found - use the saved value and return its type
                            strParam = parmVal;
                            paramType = classifyDataType (parmVal);
                        } else {
                            // not found, use the entry simply as it is: a String
                            paramType = 'S';
                        }
                    }
                    else if (strParam.indexOf(' ') > 0)
                        paramType = 'L';
                    else
                        paramType = 'S';
                }
                break;
            default:
                throw new ParserException(functionId + "Invalid class type for param: " + classType);
        }
        
        frame.outputInfoMsg(STATUS_DEBUG, functionId + "type " + paramType + ", " + classType);
    }
        
    /**
     * Creates a parameter having the specified characteristics.
     * This is only used in the Compilation phase, so we are verifying the type is valid,
     *   but if it is a reference parameter, don't replace the parameter with its value.
     *   That can only be done during execution phase.
     * 
     * @param strValue - the parameter value to use (can be a parameter reference)
     * @param dataType - the parameter type desired
     * 
     * @throws ParserException
     */
    public ParameterStruct (String strValue, char dataType) throws ParserException {
        String functionId = CLASS_NAME + " (new): ";
        
        Integer iVal;
        strParam = strValue;
        if (strParam.startsWith("$")) {
            String parmVal = findStringParam (strValue);
            if (parmVal != null) {
                strParam = parmVal;
            }
        }
        
        switch (dataType) {
            case 'B':
                if (!strParam.equalsIgnoreCase("TRUE") &&
                    !strParam.equalsIgnoreCase("FALSE")) {
                    throw new ParserException(functionId + "Invalid value for '" + dataType + "' type param: " + strValue);
                }
                paramType = 'B';
                break;
            case 'I':
                try {
                    Utils.getIntValue (strParam);
                    paramType = 'I';
                } catch (ParserException ex) {
                    throw new ParserException(functionId + "Invalid value for '" + dataType + "' type param: " + strValue);
                }
                break;
            case 'U':
                try {
                    iVal = Utils.getIntValue (strParam);
                    if (iVal >= 0)
                        paramType = 'U';
                } catch (ParserException ex) {
                    throw new ParserException(functionId + "Invalid value for '" + dataType + "' type param: " + strValue);
                }
                break;
            case 'S':
                int offset = strParam.indexOf(' ');
                if (offset > 0) {
                    strParam = strParam.substring(0, offset);
                }
                paramType = 'S';
                break;
            case 'L':
                paramType = 'L';
                break;
        }
    }

    /**
     * determines the type of data in a String value.
     * 
     * @param strValue - the String value to check
     * 
     * @return the data type found
     */
    public static char classifyDataType (String strValue) {
        char dataType;

        if (strValue.startsWith("$I_")) {
            dataType = 'I'; // these should always be integers (unsure if they are unsigned or not)
        }
        else if (strValue.startsWith("$")) {
            dataType = 'S'; // these can be anything, but during compile we won't know runtime values
        }
        else if (strValue.equalsIgnoreCase("TRUE") ||
            strValue.equalsIgnoreCase("FALSE")) {
            dataType = 'B';
        } else {
            try {
                Integer iVal = Utils.getIntValue (strValue);
                if (iVal >= 0)
                    dataType = 'U';
                else
                    dataType = 'I';
            } catch (ParserException ex) {
                int offset = strValue.indexOf(' ');
                if (offset > 0) {
                    dataType = 'L';
                } else {
                    dataType = 'S';
                }
            }
        }
        
        return dataType;
    }
    
    /**
     * returns the parameter type (I, U, B, S, L, R)
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
     * converts the current parameter value to the specified type.
     * This will replace 'strParam' with the reference parameter value if the
     *  current value is a parameter reference, and will convert the 'strParam'
     *  into Integer or Boolean value and save to 'intParam' or 'boolParam'
     *  if the dataType param specifies it.
     * 
     * @param dataType - type of data to convert the current value to
     * 
     * @throws ParserException 
     */
    public void convertType (char dataType) throws ParserException {
        switch (Character.toUpperCase(dataType)) {
            case 'U', 'I' -> 
                setIntegerValue(unpackIntegerValue(), dataType);
            case 'B' -> 
                setBooleanValue(unpackBooleanValue(), dataType);
            case 'S' -> {
                int offset = strParam.indexOf(' ');
                if (offset > 0) {
                    strParam = strParam.substring(0, offset);
                }
            }
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
        if (strVal != null) {
            strParam = strVal;
        }
        frame.outputInfoMsg(STATUS_DEBUG, "    unpacked String from '" + paramType + "' value: '" + strParam + "'");
        return strParam;
    }
        
    /**
     * gets the Integer value of the current parameter, which may be a saved parameter
     * 
     * @return the Integer value, which may be a parameter reference & may require type conversion
     * 
     * @throws ParserException 
     */
    public Integer unpackIntegerValue () throws ParserException {
        String functionId = CLASS_NAME + ".unpackIntegerValue: ";
        
        // check for entry in params first
        Integer intVal;
        if (paramType == 'R') {
            intVal = findIntegerParam(strParam);
            if (intVal == null) {
                throw new ParserException(functionId + "Parameter Reference not found for dataType " + paramType + ": param: " + strParam);
            }
        } else {
            intVal = Utils.getHexValue(strParam);
            if (intVal == null) {
                intVal = Utils.getIntValue(strParam);
            }
        }
        intParam = intVal;
        frame.outputInfoMsg(STATUS_DEBUG, "    unpacked Integer from '" + paramType + "' value: " + intParam);
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
        String functionId = CLASS_NAME + ".unpackBooleanValue: ";

        // check for entry in params first
        Boolean boolVal;
        if (paramType == 'R') {
            String strVal = findStringParam(strParam);
            if (strVal == null) {
                throw new ParserException(functionId + "Parameter Reference not found for dataType " + paramType + ": param: " + strParam);
            }
            boolVal = Utils.getBooleanValue(strVal);
        }
        else {
            boolVal = Utils.getBooleanValue(strParam);
        }
        boolParam = boolVal;
        frame.outputInfoMsg(STATUS_DEBUG, "    unpacked Boolean from '" + paramType + "' value: " + boolParam);
        return boolVal;
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
    private void setIntegerValue (Integer intVal, char dataType) throws ParserException {
        intParam = intVal;
        paramType = dataType;
        frame.outputInfoMsg(STATUS_PROGRAM, "     '" + paramType + "' param: " + intParam);
    }
        
    /**
     * sets the current parameter to the specified Boolean value.
     * Performs and data type conversion necessary.
     * 
     * @param boolVal  - the Boolean value
     * @param dataType - the data type the parameter should be
     */
    private void setBooleanValue (Boolean boolVal, char dataType) {
        boolParam = boolVal;
        paramType = dataType;
        frame.outputInfoMsg(STATUS_PROGRAM, "     '" + paramType + "' param: " + boolParam);
    }

    /**
     * returns a String for displaying the current param data type and value.
     * 
     * @return a String indicating the parameter type and value
     */
    public String showParam () {
        String strCommand;
        switch (paramType) {
            case 'I', 'U', 'B' -> {
                strCommand = " [" + paramType + "] " + strParam + "";
            }
            default -> {
                strCommand = " [" + paramType + "] '" + strParam + "'";
            }
        }
            
        return strCommand;
    }
    
    //========================================================================
    // THESE STATIC METHODS ARE USED FOR ACCESSING THE STATIC PARAMETER VALUES
    //========================================================================

    /**
     * initializes the saved parameters
     */
    public static void initParameters () {
        strParams.clear();
        intParams.clear();
        loopParams.clear();
        loopNames.clear();
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
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Added String parameter " + name + " init to '" + value + "'");
        } else {
            frame.outputInfoMsg(STATUS_PROGRAM, "   - String parameter " + name + " already defined");
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
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified String param: " + name + " = " + value);
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
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Added Integer parameter " + name + " init to " + value);
        } else {
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Integer parameter " + name + " already defined");
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
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified Integer param: " + name + " = " + value);
            return true;
        }
        return false;
    }

    /**
     * gets the LoopStruct entry corresponding to the LoopId value.
     * 
     * @param loopId - the loop name-index combo that uniquely defines a LoopStruct entry
     * 
     * @return the corresponding LoopStruct value from loopParams table
     */
    private static LoopStruct getLoopStruct (ScriptParser.LoopId loopId) {
        if (loopParams== null || loopParams.isEmpty()) {
            return null;
        }
        // search for a LoopId match
        Iterator it = loopParams.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            ScriptParser.LoopId mapId = (ScriptParser.LoopId) pair.getKey();
            LoopStruct mapInfo = (LoopStruct) pair.getValue();
            if (loopId.name.contentEquals(mapId.name) && loopId.index == mapId.index) {
                return mapInfo;
            }
        }
        return null;
    }
    
    /**
     * checks if the current Loop command is at the same IF level as its corresponding FOR statement.
     * 
     * @param command - the FOR command being run
     * @param level  - current IF nest level for current FOR command
     * @param loopId - the name-index ID for the current loop
     * 
     * @throws ParserException 
     */    
    public static void checkLoopIfLevel (String command, int level, ScriptParser.LoopId loopId) throws ParserException {
        String functionId = CLASS_NAME + ".setLoopEnd: ";
        
        LoopStruct loopInfo = getLoopStruct (loopId);
        if (loopInfo == null) {
            throw new ParserException(functionId + "FOR Loop " + loopId.name + " @ " + loopId.index + " not found");
        }
        if (! loopInfo.isLoopIfLevelValid(level)) {
            throw new ParserException(functionId + command + "exceeded bounds of enclosing IF block: IF level = " + level);
        }
    }
        
    /**
     * sets the location of the end of the loop when the ENDLOOP command is parsed
     * 
     * @param index  - current command index for ENDLOOP command
     * @param loopId - the name-index ID for the current loop
     * 
     * @throws ParserException 
     */    
    public static void setLoopEndIndex (int index, ScriptParser.LoopId loopId) throws ParserException {
        String functionId = CLASS_NAME + ".setLoopEnd: ";
        
        LoopStruct loopInfo = getLoopStruct (loopId);
        if (loopInfo == null) {
            throw new ParserException(functionId + "FOR Loop " + loopId.name + " @ " + loopId.index + " not found");
        }
        loopInfo.setLoopEnd(index);
    }
        
    /**
     * gets the next command index based on the loop command specified for current loop.
     * This should be called by 'CommandParser.executeProgramCommand' when a FOR
     * loop is in progress and one of the loop commands was found that may change
     * the next location to execute from.
     * 
     * @param command - the loop command to execute
     * @param index   - the current command index
     * @param loopId  - the loop parameter currently running
     * 
     * @return the next command index to run
     * 
     * @throws ParserException
     */
    public static int getLoopNextIndex (String command, int index, ScriptParser.LoopId loopId) throws ParserException {
        String functionId = CLASS_NAME + ".getLoopNextIndex: ";
        
        int nextIndex = index;
        
        LoopStruct loopInfo = getLoopStruct (loopId);
        if (loopInfo == null) {
            throw new ParserException(functionId + "FOR Loop " + loopId.name + " @ " + loopId.index + " not found");
        }
        
        String action = "";
        switch (command) {
            case "FOR":
                nextIndex = loopInfo.startLoop(index);
                action = "starting";
                break;
            case "BREAK":
                nextIndex = loopInfo.loopBreak();
                action = "exiting";
                break;
            case "NEXT":
            case "CONTINUE":
                nextIndex = loopInfo.loopNext();
                if (nextIndex < index)
                    action = "restarting";
                else
                    action = "exiting";
                break;
            default:
                break;
        }
        
        frame.outputInfoMsg(STATUS_PROGRAM, command + " command " + action + " at index: " + nextIndex);
        return nextIndex;
    }

    /**
     * adds a new entry in the Loop parameters table.
     * This should only be called by 'CommandParser.compileProgram' when stepping
     * through the commands to verify and create the compiled list of commands.
     * 
     * @param name     - loop parameter name
     * @param loopId   - loop name-index combination to uniquely identify the loop param
     * @param loopInfo - the loop parameter to add
     */
    public static void saveLoopParameter (String name, ScriptParser.LoopId loopId, LoopStruct loopInfo) {
        String functionId = CLASS_NAME + ".saveLoopParameter: ";
        
        // create a new loop ID (name + command index) for the entry and add it
        // to the list of IDs for the loop parameter name
        ArrayList<ScriptParser.LoopId> loopList;
        if (loopNames.isEmpty()) {
            // first loop defined, create an empty array list and add it to the list of names for this name.
            loopList = new ArrayList<>();
            loopNames.put(name, loopList);
        } else {
            loopList = loopNames.get(name);
        }
        loopList.add(loopId);
        frame.outputInfoMsg(STATUS_DEBUG, functionId + "Number of loops with name " + name + ": " + loopList.size());
        
        // now add loop entry to hashmap based on name/index ID
        frame.outputInfoMsg(STATUS_DEBUG, functionId + "loopParams [" + loopParams.size() + "] " + loopId.name + " @ " + loopId.index);
        loopParams.put(loopId, loopInfo);
    }

    /**
     * checks if a parameter name is valid
     * 
     * @param name - the name to check
     *               name must be only alphanumeric or '_' or '-' chars,
     *               cannot be a reserved name (RESPONSE, RESULT) or a Loop parameter name.
     * 
     * @return  true if valid
     * 
     * @throws ParserException
     */
    public static boolean isValidParamName (String name) throws ParserException {
        if (name.startsWith("$")) {
            name = name.substring(1);
        }
        if (name.contentEquals("RESPONSE")) {
            throw new ParserException(": using Reserved parameter name: " + name);
        }
        if (name.contentEquals("RESULT")) {
            throw new ParserException(": using Reserved parameter name: " + name);
        }
        if (ScriptParser.isValidCommand(name)) {
            throw new ParserException(": using Reserved command name: " + name);
        }
        for (int ix = 0; ix < name.length(); ix++) {
            if (  (name.charAt(ix) != '_' && name.charAt(ix) != '-') &&
                 ! Character.isLetter(name.charAt(ix)) &&
                 ! Character.isDigit(name.charAt(ix)) ) {
                throw new ParserException(": invalid character '" + name.charAt(ix) + "' in parameter name: " + name);
            }
        }
        if (! Character.isLetter(name.charAt(0))) {
            // 1st character must be a letter
            throw new ParserException(": invalid first character (must be A-Z or a-z) in parameter name: " + name);
        }
        if (loopNames.containsKey(name)) {
            throw new ParserException(": using Loop parameter name: " + name);
        }
        return true;
    }
    
    /**
     * checks if a Loop parameter name is valid
     * 
     * @param name - the name to check
     *               name must be only alphanumeric or '_' or '-' chars,
     *               cannot be a reserved name (RESPONSE, RESULT) or a String
     *               or Integer parameter name.
     * @param index - the command index for the FOR command
     * 
     * @return  true if valid
     * 
     * @throws ParserException
     */
    public static boolean isValidLoopName (String name, int index) throws ParserException {
        if (name.startsWith("$")) {
            name = name.substring(1);
        }
        if (name.contentEquals("RESPONSE")) {
            throw new ParserException(": using Reserved parameter name: " + name);
        }
        if (name.contentEquals("RESULT")) {
            throw new ParserException(": using Reserved parameter name: " + name);
        }
        for (int ix = 0; ix < name.length(); ix++) {
            if (  (name.charAt(ix) != '_' && name.charAt(ix) != '-') &&
                 ! Character.isLetter(name.charAt(ix)) &&
                 ! Character.isDigit(name.charAt(ix)) ) {
                throw new ParserException(": invalid character '" + name.charAt(ix) + "' in parameter name: " + name);
            }
        }
        // make sure its not the same as a regular parameter
        if (intParams.containsKey(name)) {
            throw new ParserException(": using Integer parameter name: " + name);
        }
        if (strParams.containsKey(name)) {
            throw new ParserException(": using String parameter name: " + name);
        }
        
        // now check if this loop name is nested in a loop having same name
        // get the list of loops using this parameter name (if any)
        ArrayList<ScriptParser.LoopId> loopList = loopNames.get(name);
        if (loopList != null && ! loopList.isEmpty()) {
            // we have one or more uses of the same name, check if this is nested in one
            frame.outputInfoMsg(STATUS_PROGRAM, "   - checking previous uses of FOR Loop parameter " + name + " to see if we have a nesting problem");
            for (int ix = 0; ix < loopList.size(); ix++) {
                ScriptParser.LoopId loopEntry = loopList.get(ix);
                LoopStruct loopInfo = getLoopStruct (loopEntry);
                if (loopInfo == null || ! loopInfo.isLoopComplete()) {
                    throw new ParserException(": Loop param " + name + " @ " + index + " is nested in same name at " + loopEntry.index);
                } else {
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - FOR Loop parameter " + name + " @ " + loopEntry.index + " was complete");
                }
            }
        }
        
        return true;
    }
    
    /**
     * checks if a parameter is an Integer type
     * 
     * @param name - the name to check
     * 
     * @return  true if Integer parameter
     */
    public static boolean isIntegerParam (String name) {
        return name.startsWith("I_");
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
            } else {
                if (strParams.containsKey(parmName)) {
                    strVal = strParams.get(parmName);
                } else {
                    throw new ParserException(functionId + "String param not found: " + parmName);
                }
            }
            if (intVal == null && strVal != null) {
                try {
                    intVal = Utils.getHexValue(strVal);
                    if (intVal == null) {
                        intVal = Integer.valueOf(strVal);
                    }
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
            } else {
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
