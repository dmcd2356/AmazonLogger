/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import static com.mycompany.amazonlogger.UIFrame.STATUS_WARN;
import java.util.ArrayList;
import java.util.Arrays;
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
    
    private String      strParam;           // value for the String  param type
    private Long        longParam;          // value for the Integer param type (64 bit signed)
    private Boolean     boolParam;          // value for the Boolean param type
    private ArrayList<Long>     arrayParam; // value for Integer Array param type
    private ArrayList<String>   listParam;  // value for String  List  param type
    private ArrayList<CalcEntry> calcParam; // value for Calculation param type
    private String      paramName;          // name of parameter if references a saved parameter
    private ParamType   paramType;          // parameter classification
    private char        paramTypeID;        // ID corresponding to the paramType
    
    // saved static parameters
    private static String  strResponse = "";    // response from last RUN command
    private static Long    intResult = 0L;      // result of last CALC command
    private static final HashMap<String, String>  strParams  = new HashMap<>();
    private static final HashMap<String, Long>    longParams = new HashMap<>();
    private static final HashMap<String, Long>    uintParams = new HashMap<>();
    private static final HashMap<String, Boolean> boolParams = new HashMap<>();
    private static final HashMap<String, ArrayList<Long>>    arrayParams = new HashMap<>();
    private static final HashMap<String, ArrayList<String>>  listParams  = new HashMap<>();
    
    // for loops, the loopParams will find the loop parameter for the loop at the
    // specified command index. In order to determine if we have a nested loop
    // using the same param name, we use loopNames that contains an array of
    // all the loop params having the same name. When a name is being reused,
    // we must verify that all the occurrances of FOR loops using that name
    // are all completely defined, meaning that the ENDFOR has already been found
    // for each one. When compiling, we simply proceed through the instructions
    // sequentially, so if all current uses of the FOR parameter indicate they
    // are complete (i.e. ENDFOR has been found), we are safe to reuse the loop name.
    private static final HashMap<LoopId, LoopStruct> loopParams = new HashMap<>();
    private static final HashMap<String, ArrayList<LoopId>> loopNames = new HashMap<>();

    public enum ParamType {
        Integer,        // 'I' type
        Unsigned,       // 'U' type
        Boolean,        // 'B' type
        String,         // 'S' type
        IntArray,       // 'A' type
        StringArray,    // 'L' type
        Calculation,    // 'C' type
    }
    
    public ParameterStruct() {
        strParam = null;
        longParam = null;
        boolParam = null;
        arrayParam = null;
        listParam = null;
        calcParam = null;
        paramName = "";
        paramType = null;
        paramTypeID = '?';
    }

    /**
     * Creates a parameter having the specified characteristics.
     * This is only used in the Compilation phase, so we are creating the parameter
     *   entry and verifying the type is valid, but if it is a reference parameter,
     *   don't replace the parameter with its value.
     *   That can only be done during execution phase.
     * 
     * @param strValue - the parameter value to use (can be a parameter reference)
     * @param dataType - the parameter type desired
     * 
     * @throws ParserException
     */
    public ParameterStruct (String strValue, ParamType dataType) throws ParserException {
        String functionId = CLASS_NAME + " (new): ";
        
        paramName = null;
        calcParam = null;
        strParam = strValue;
        
        if (strParam.startsWith("$") && dataType != ParamType.Calculation) {
            paramName = strValue.substring(1);
//            strParam = paramName;
            // set type based on the parameter name prefix rather than desired data type
            ParamType type = getParamTypeFromName(paramName);
            paramType = type;
            paramTypeID = getParamTypeID (paramType);
            return;
        }

        String invalidMsg = "Invalid value for '" + dataType + "' type param: " + strValue;
        switch (dataType) {
            case ParamType.Boolean:
                if (!strParam.equalsIgnoreCase("TRUE") &&
                    !strParam.equalsIgnoreCase("FALSE") &&
                    !strParam.contentEquals("0") &&
                    !strParam.contentEquals("1") ) {
                    throw new ParserException(functionId + invalidMsg);
                }
                paramType = dataType;
                paramTypeID = getParamTypeID (paramType);
                boolParam = strParam.equalsIgnoreCase("TRUE") || strParam.contentEquals("1");
                break;
            case ParamType.Unsigned:
                try {
                    longParam = getLongOrUnsignedValue(strParam);
                } catch (ParserException ex) {
                    throw new ParserException(functionId + invalidMsg);
                }
                if (! isUnsignedInt(longParam)) {
                    throw new ParserException(functionId + invalidMsg);
                }
                arrayParam = new ArrayList<>();
                arrayParam.add(longParam);
                paramType = dataType;
                paramTypeID = getParamTypeID (paramType);
                break;
            case ParamType.Integer:
                try {
                    longParam = getLongOrUnsignedValue(strParam);
                } catch (ParserException ex) {
                    throw new ParserException(functionId + invalidMsg);
                }
                arrayParam = new ArrayList<>();
                arrayParam.add(longParam);
                paramType = dataType;
                paramTypeID = getParamTypeID (paramType);
                break;
            case ParamType.IntArray:
            case ParamType.StringArray:
                paramType = ParamType.IntArray;
                paramTypeID = getParamTypeID (paramType);
                listParam = new ArrayList<>(Arrays.asList(strParam.split(",")));
                arrayParam = new ArrayList<>();
                for (int ix = 0; ix < listParam.size(); ix++) {
                    try {
                        String cleanStr = listParam.get(ix).strip();
                        listParam.set(ix, cleanStr); // remove leading & trailing spaces
                        longParam = getLongOrUnsignedValue(cleanStr);
                        arrayParam.add(longParam);
                    } catch (ParserException ex) {
                        paramType = ParamType.StringArray;
                        paramTypeID = getParamTypeID (paramType);
                    }
                }
                if (paramType == ParamType.IntArray) {
                    longParam = arrayParam.getFirst();
                    frame.outputInfoMsg(STATUS_DEBUG, functionId + "type " + paramTypeID + ", " + arrayParam.size() + " entries");
                } else {
                    frame.outputInfoMsg(STATUS_DEBUG, functionId + "type " + paramTypeID + ", " + listParam.size() + " entries");
                }
                break;
            case ParamType.String:
                paramType = dataType;
                paramTypeID = getParamTypeID (paramType);
                listParam = new ArrayList<>();
                listParam.add(strParam);
                break;
            case ParamType.Calculation:
                // save the calculation entry
                Calculation calc = new Calculation(strValue);
                calcParam = calc.copyCalc();
                paramType = dataType;
                paramTypeID = getParamTypeID (paramType);
                
                // if calc is a single entry, don't use Calculation type, switch to Integer type
                Long value = calc.getCalcValue();
                if (value != null) {
                    longParam = value;
                    paramType = ParamType.Integer;
                    paramTypeID = getParamTypeID (paramType);
                    frame.outputInfoMsg(STATUS_DEBUG, "Converted Calculation parameter to Integer value: " + value);
                }
                break;
        }
    }

    /**
     * sets the parameter type value.
     * 
     * @param type 
     */
    private char getParamTypeID (ParamType type) {
        switch (type) {
            case ParamType.Integer:     return 'I';
            case ParamType.Unsigned:    return 'U';
            case ParamType.Boolean:     return 'B';
            case ParamType.String:      return 'S';
            case ParamType.IntArray:    return 'A';
            case ParamType.StringArray: return 'L';
            case ParamType.Calculation: return 'C';
            default:
                break;
        }
        return '?';
    }
    
    /**
     * returns the parameter type
     * 
     * @return the parameter type
     */
    public ParamType getParamType () {
        return paramType;
    }

    /**
     * determines if the parameter is valid for the specified requested type.
     * 
     * @param typeID - the type of parameter desired
     * 
     * @return true if the parameter is valid for that type
     */
    public boolean isValidForType (char typeID) {
        switch (typeID) {
            case 'I':
            case 'U':
                if (paramType == ParamType.Integer ||
                    paramType == ParamType.Unsigned   ) {
                    return true;
                }
                break;
            case 'B':
                if (paramType == ParamType.Boolean) {
                    return true;
                }
                break;
            case 'A':
                if (paramType == ParamType.IntArray ||
                    paramType == ParamType.Integer  ||
                    paramType == ParamType.Unsigned   ) {
                    return true;
                }
                break;
            case 'L':
            case 'S':
                return true;
            default:
                break;
        }
        return false;
    }

    /**
     * determines the type of parameter from the first 2 chars of the parameter name.
     * 
     * @param name - name of the parameter (don't include the '$' char)
     * 
     * @return the corresponding parameter type
     */    
    private static ParamType getParamTypeFromName (String name) {
        if (name.charAt(1) == '_') {
            char firstChar = name.charAt(0);
            switch (firstChar) {
                case 'I': return ParamType.Integer;
                case 'U': return ParamType.Unsigned;
                case 'B': return ParamType.Boolean;
                case 'A': return ParamType.IntArray;
                case 'L': return ParamType.StringArray;
                default:  return ParamType.String;
            }
        }
        return ParamType.String;
    }
    
    /**
     * returns the parameter name
     * 
     * @return the parameter name (null if parameter is not a reference to a saved parameter)
     */
    public String getParamName () {
        return paramName;
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
    public Long getIntegerValue () {
        return longParam;
    }
        
    /**
     * returns the Calculation data value
     * 
     * @return the Integer value of the calculation
     * 
     * @throws com.mycompany.amazonlogger.ParserException
     */
    public Long getCalculationValue () throws ParserException {
        Calculation calc = new Calculation(calcParam);
        longParam = calc.compute();
        return longParam;
    }
        
    /**
     * returns the Unsigned Integer data value
     * 
     * @return the Integer value
     * 
     * @throws ParserException
     */
    public Integer getUnsignedValue () throws ParserException {
        String functionId = CLASS_NAME + ".getUnsignedValue: ";

        if (! isUnsignedInt(longParam)) {
            throw new ParserException(functionId + "Parameter value exceeds bounds for Unsigned: " + longParam);
        }
        return longParam.intValue();
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
     * returns the Array data element value
     * 
     * @return the number of elements in the Array
     */
    public int getArraySize () {
        if (arrayParam == null)
            return 0;
        return arrayParam.size();
    }

    /**
     * checks if the parameter is a reference and, if so, reads the value into the structure.
     * It also marks the type of data placed and converts values to other types where possible.
     * 
     * @throws ParserException 
     */
    public void updateFromReference () throws ParserException {
        
        ParameterStruct value = getParameterEntry (paramName);
        if (value != null) {
            this.paramType   = value.paramType;
            this.paramTypeID = value.paramTypeID;
            this.longParam   = value.longParam;
            this.boolParam   = value.boolParam;
            this.strParam    = value.strParam;
            this.arrayParam  = value.arrayParam;
            this.listParam   = value.listParam;
            
            frame.outputInfoMsg(STATUS_DEBUG, "    unpacked param " + paramName + " as type '" + paramTypeID + "' value: " + boolParam);
        }
    }

    /**
     * updates the other parameters types (conversion) in the structure where possible.
     * If the value was a String type, it will reclassify the data type to I, U or B
     * if the value happens to be an Integer, Unsigned or Boolean.
     * 
     * @throws ParserException 
     */
    private void updateConversions () throws ParserException {
        switch (paramType) {
            case ParamType.Integer:
                strParam = longParam.toString();
                boolParam = longParam != 0;
                break;
            case ParamType.Unsigned:
                strParam = longParam.toString();
                boolParam = longParam != 0;
                break;
            case ParamType.Boolean:
                strParam = boolParam.toString();
                longParam = (boolParam) ? 1L : 0L;
                break;
            case ParamType.String:
                if (!strParam.isBlank()) {
                    if (strParam.equalsIgnoreCase("TRUE")) {
                        boolParam = true;
                        paramType = ParamType.Boolean;
                        paramTypeID = getParamTypeID (paramType);
                    } else if (strParam.equalsIgnoreCase("FALSE")) {
                        boolParam = false;
                        paramType = ParamType.Boolean;
                        paramTypeID = getParamTypeID (paramType);
                    } else {
                        try {
                            Long longVal = getLongOrUnsignedValue (strParam);
                            longParam = longVal;
                            if (isUnsignedInt(longVal)) {
                                paramType = ParamType.Unsigned;
                                paramTypeID = getParamTypeID (paramType);
                            } else {
                                paramType = ParamType.Integer;
                                paramTypeID = getParamTypeID (paramType);
                            }
                            boolParam = longParam != 0;
                        } catch (ParserException ex) {
                            // keep param as String type and we can't do any conversions
                        }
                    }
                }
                break;
            case ParamType.IntArray:
                strParam = arrayParam.getFirst().toString();
                longParam = arrayParam.getFirst();
                boolParam = !arrayParam.isEmpty(); // set to true if array has an entry
                break;
            case ParamType.StringArray:  // String List type
                strParam = listParam.getFirst();
                break;
            default:
                break;
        }
    }
    
    /**
     * returns the Array data element value
     * 
     * @param index - the entry from the array to get
     * 
     * @return the Array element value
     */
    public Long getArrayElement (int index) {
        if (index >= arrayParam.size()) {
            return null;
        }
        return arrayParam.get(index);
    }

    /**
     * returns the List data element value
     * 
     * @return the number of elements in the List
     */
    public int getListSize () {
        if (listParam == null)
            return 0;
        return listParam.size();
    }

    /**
     * returns the List data element value
     * 
     * @param index - the entry from the list to get
     * 
     * @return the List element value
     */
    public String getListElement (int index) {
        if (index >= listParam.size()) {
            return null;
        }
        return listParam.get(index);
    }

    /**
     * returns a String for displaying the current param data type and value.
     * 
     * @return a String indicating the parameter type and value
     */
    public String showParam () {
        String strValue;
        switch (paramType) {
            case ParamType.Integer, ParamType.Unsigned -> {
                strValue = longParam + "";
            }
            case ParamType.Boolean -> {
                strValue = boolParam + "";
            }
            case ParamType.IntArray -> {
                strValue = arrayParam.toString();
            }
            case ParamType.StringArray -> {
                strValue = listParam.toString();
            }
            default -> {
                strValue = "'" + strParam + "'";
            }
        }
            
        return "  " + paramTypeID + ": " + strValue;
    }

    /**
     * displays a String of the data types for all the parameters in a command.
     * 
     * @param params - the array of parameters for a command
     */
    public static void showParamTypeList (ArrayList<ParameterStruct> params) {
        String paramTypes = "";
        for (int ix = 0; ix < params.size(); ix++) {
            paramTypes += params.get(ix).paramTypeID;
        }
        frame.outputInfoMsg(STATUS_PROGRAM, "     dataTypes: " + paramTypes);
    }
    
    //========================================================================
    // THESE STATIC METHODS ARE USED FOR ACCESSING THE STATIC PARAMETER VALUES
    //========================================================================

    /**
     * initializes the saved parameters
     */
    public static void initParameters () {
        strParams.clear();
        longParams.clear();
        uintParams.clear();
        boolParams.clear();
        arrayParams.clear();
        listParams.clear();
        loopParams.clear();
        loopNames.clear();
        strResponse = "";
        intResult = 0L;
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
    public static void putResultValue (Long value) {
        intResult = value;
    }

    /**
     * creates a new entry in the Integer params table and sets the initial value.
     * Indicates if the param was already defined.
     * 
     * @param name  - parameter name
     * 
     * @throws ParserException - if name already exists
     */
    public static void allocateParameter (String name) throws ParserException {
        String functionId = CLASS_NAME + ".allocateParameter: ";

        // first, verify parameter name to make sure it is valid format and
        //  not already used.
        boolean bIsDefined = isValidParamName(name);
        if (bIsDefined) {
            throw new ParserException(functionId + "Parameter " + name + " already defined");
        }

        String typeName = "String";
        switch (getParamTypeFromName(name)) {
            case ParamType.Integer:
                typeName = "Integer";
                longParams.put(name, 0L);
                break;
            case ParamType.Unsigned:
                typeName = "Unsigned";
                uintParams.put(name, 0L);
                break;
            case ParamType.Boolean:
                typeName = "Boolean";
                boolParams.put(name, false);
                break;
            case ParamType.IntArray:
                typeName = "Array";
                arrayParams.put(name, new ArrayList<>());
                break;
            case ParamType.StringArray:
                typeName = "List";
                listParams.put(name, new ArrayList<>());
                break;
            case ParamType.Calculation:
                // we don't store calculations, so do nothing
                break;
            default:
            case ParamType.String:
                strParams.put(name, "");        // default value to empty String
                break;
        }
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Added " + typeName + " parameter: " + name);
    }

    /**
     * returns the value of a reference parameter along with its data type.
     * 
     * @param name  - parameter name
     * 
     * @return the parameter value
     * 
     * @throws ParserException - if parameter not found
     */
    public ParameterStruct getParameterEntry (String name) throws ParserException {
        String functionId = CLASS_NAME + ".getParameterEntry: ";

        if (name == null) {
            return null;
        }
        
        // create a new parameter with all null entries
        ParameterStruct paramValue = new ParameterStruct();

        // this allows us to take as a name input either the name itself or with the '$' preceeding it.
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }
        ParamType pType = getParamTypeFromName (name);
        
        switch (pType) {
            case ParamType.Integer:
                paramValue.longParam = longParams.get(name);
                if (paramValue.longParam == null) {
                    throw new ParserException(functionId + "Parameter " + name + " not found");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.longParam);
                break;
            case ParamType.Unsigned:
                paramValue.longParam = uintParams.get(name);
                if (paramValue.longParam == null) {
                    throw new ParserException(functionId + "Parameter " + name + " not found");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.longParam);
                break;
            case ParamType.Boolean:
                paramValue.boolParam = boolParams.get(name);
                if (paramValue.boolParam == null) {
                    throw new ParserException(functionId + "Parameter " + name + " not found");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.boolParam);
                break;
            case ParamType.IntArray:
                paramValue.arrayParam = arrayParams.get(name);
                if (paramValue.arrayParam == null) {
                    throw new ParserException(functionId + "Parameter " + name + " not found");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.arrayParam.toString());
                break;
            case ParamType.StringArray:
                paramValue.listParam = listParams.get(name);
                if (paramValue.listParam == null) {
                    throw new ParserException(functionId + "Parameter " + name + " not found");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.listParam.toString());
                break;
            default:
                if (name.contentEquals("RESULT")) {
                    paramValue.longParam = intResult;
                    if (isUnsignedInt(paramValue.longParam)) {
                        pType = ParamType.Unsigned;
                    } else {
                        pType = ParamType.Integer;
                    }
                } else if (name.contentEquals("RESPONSE")) {
                    paramValue.strParam = strResponse;
                    pType = ParamType.String;
                } else {
                    paramValue.strParam = strParams.get(name);
                    if (paramValue.strParam != null) {
                        pType = ParamType.String;
                    } else if (loopNames.containsKey(name)) {
                        LoopStruct loopInfo = new LoopStruct();
                        paramValue.longParam = loopInfo.getCurrentLoopValue(name).longValue();
                        if (isUnsignedInt(paramValue.longParam)) {
                            pType = ParamType.Unsigned;
                        } else {
                            pType = ParamType.Integer;
                        }
                    } else {
                        throw new ParserException(functionId + "Parameter " + name + " not found");
                    }
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.strParam);
                break;
        }

        // save the parameter type and name
        paramValue.paramName = name;
        paramValue.paramType = pType;
        paramValue.paramTypeID = getParamTypeID (pType);

        // convert value to other forms where possible
        paramValue.updateConversions();
        return paramValue;
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
     * modifies the value of an existing entry in the Integer params table.
     * Indicates if the param was not found (does NOT create a new entry).
     * 
     * @param name  - parameter name
     * @param value - parameter value
     * 
     * @return true if successful, false if the parameter was not found
     */
    public static boolean modifyIntegerParameter (String name, Long value) {
        if (longParams.containsKey(name)) {
            longParams.replace(name, value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified Integer param: " + name + " = " + value);
            return true;
        }
        return false;
    }

    /**
     * modifies the value of an existing entry in the Unsigned params table.
     * Indicates if the param was not found (does NOT create a new entry).
     * 
     * @param name  - parameter name
     * @param value - parameter value
     * 
     * @return true if successful, false if the parameter was not found
     * 
     * @throws ParserException
     */
    public static boolean modifyUnsignedParameter (String name, Long value) throws ParserException {
        String functionId = CLASS_NAME + ".modifyUnsignedParameter: ";

        if (! isUnsignedInt(value)) {
            throw new ParserException(functionId + "value for parameter " + name + " exceeds limits for Unsigned: " + value);
        }
        if (uintParams.containsKey(name)) {
            uintParams.replace(name, value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified Unsigned param: " + name + " = " + value);
            return true;
        }
        return false;
    }

    /**
     * modifies the value of an existing entry in the Boolean params table.
     * Indicates if the param was not found (does NOT create a new entry).
     * 
     * @param name  - parameter name
     * @param value - parameter value
     * 
     * @return true if successful, false if the parameter was not found
     */
    public static boolean modifyBooleanParameter (String name, Boolean value) {
        if (boolParams.containsKey(name)) {
            boolParams.replace(name, value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified Boolean param: " + name + " = " + value);
            return true;
        }
        return false;
    }

    /**
     * clears all entries of an existing Array.
     * Indicates if the param was not found (does NOT create a new entry).
     * 
     * @param name  - parameter name
     * 
     * @return true if successful, false if the parameter was not found
     */
    public static boolean arrayRemoveAll (String name) {
        if (arrayParams.containsKey(name)) {
            ArrayList<Long> entry = arrayParams.get(name);
            int size = entry.size();
            entry.clear();
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted " + size + " entries in Array param: " + name);
            return true;
        }
        else if (listParams.containsKey(name)) {
            ArrayList<String> entry = listParams.get(name);
            int size = entry.size();
            entry.clear();
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted " + size + " entries in List param: " + name);
            return true;
        }
        return false;
    }

    /**
     * clears selected entry of an existing Array.
     * Indicates if the param was not found (does NOT create a new entry).
     * 
     * @param name  - parameter name
     * @param index - index of entry in array to delete
     * 
     * @return true if successful, false if the parameter was not found
     * 
     * @throws ParserException
     */
    public static boolean arrayRemoveEntry (String name, int index) throws ParserException {
        String functionId = CLASS_NAME + ".arrayClearEntry: ";

        if (arrayParams.containsKey(name)) {
            ArrayList<Long> entry = arrayParams.get(name);
            if (index >= entry.size()) {
                throw new ParserException(functionId + "Array Parameter " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
            }
            entry.remove(index);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Removed Array param element: " + name + "[" + index + "]");
            return true;
        }
        return false;
    }

    /**
     * modifies the value of an existing entry in the Array or List params table.
     * Indicates if the param was not found (does NOT create a new entry).
     * 
     * @param name  - parameter name
     * @param index - index of entry in list to change
     * @param value - parameter value
     * 
     * @return true if successful, false if the parameter was not found
     * 
     * @throws ParserException
     */
    public static boolean arrayModifyEntry (String name, int index, String value) throws ParserException {
        String functionId = CLASS_NAME + ".arrayModifyEntry: ";

        if (arrayParams.containsKey(name)) {
            ArrayList<Long> entry = arrayParams.get(name);
            if (index >= entry.size()) {
                throw new ParserException(functionId + "Array Parameter " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
            }
            Long longVal = getLongOrUnsignedValue (value);
            entry.set(index, longVal);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified Array param: " + name + " = " + value);
            return true;
        }
        else if (listParams.containsKey(name)) {
            ArrayList<String> entry = listParams.get(name);
            if (index >= entry.size()) {
                throw new ParserException(functionId + "List Parameter " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
            }
            entry.set(index, value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified List param: " + name + " = " + value);
            return true;
        }
        return false;
    }

    /**
     * inserts a value into an existing Array or List params table.
     * Indicates if the param was not found (does NOT create a new entry).
     * 
     * @param name  - parameter name
     * @param index - index of where to insert the value
     *                (moves current index value and all following values back 1 entry)
     * @param value - parameter value
     * 
     * @return true if successful, false if the parameter was not found
     * 
     * @throws ParserException
     */
    public static boolean arrayInsertEntry (String name, int index, String value) throws ParserException {
        String functionId = CLASS_NAME + ".arrayModifyEntry: ";

        if (arrayParams.containsKey(name)) {
            ArrayList<Long> entry = arrayParams.get(name);
            Long longVal = getLongOrUnsignedValue (value);
            if (index >= entry.size() || entry.isEmpty()) {
                throw new ParserException(functionId + "Array Parameter " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
            }
            // bump current entries back 1
            entry.addLast(entry.getLast());
            for (int ix = entry.size()-2; ix >= index; ix--) {
                entry.set(ix+1, entry.get(ix));
            }
            entry.set(index, longVal);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Inserted entry[" + index + "] in Array param: " + name + " = " + value);
            return true;
        }
        else if (listParams.containsKey(name)) {
            ArrayList<String> entry = listParams.get(name);
            if (index >= entry.size() || entry.isEmpty()) {
                throw new ParserException(functionId + "List Parameter " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
            }
            // bump current entries back 1
            entry.addLast(entry.getLast());
            for (int ix = entry.size()-2; ix >= index; ix--) {
                entry.set(ix+1, entry.get(ix));
            }
            entry.set(index, value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Inserted entry[" + index + "] in List param: " + name + " = " + value);
            return true;
        }
        return false;
    }

    /**
     * appends a value to the end of an existing Array or List params table.
     * Indicates if the param was not found (does NOT create a new entry).
     * 
     * @param name  - parameter name
     * @param value - parameter value
     * 
     * @return true if successful, false if the parameter was not found
     * 
     * @throws ParserException
     */
    public static boolean arrayAppendEntry (String name, String value) throws ParserException {
        if (arrayParams.containsKey(name)) {
            ArrayList<Long> entry = arrayParams.get(name);
            Long longVal = getLongOrUnsignedValue (value);
            entry.addLast(longVal);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Appended entry to Array param: " + name + " = " + value);
            return true;
        }
        else if (listParams.containsKey(name)) {
            ArrayList<String> entry = listParams.get(name);
            entry.addLast(value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Appended entry to List param: " + name + " = " + value);
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
    private static LoopStruct getLoopStruct (LoopId loopId) {
        if (loopParams== null || loopParams.isEmpty()) {
            return null;
        }
        // search for a LoopId match
        Iterator it = loopParams.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            LoopId mapId = (LoopId) pair.getKey();
            LoopStruct mapInfo = (LoopStruct) pair.getValue();
            if (loopId.name.contentEquals(mapId.name) && loopId.index == mapId.index) {
                return mapInfo;
            }
        }
        return null;
    }
    
    /**
     * get the current loop value for the specified loop id
     * 
     * @param loopId - the name-index ID for the currently active loop
     * 
     * @return value of the loop parameter, null if param not found
     */    
    public static Integer getLoopValue (LoopId loopId) {
        LoopStruct loopInfo = getLoopStruct (loopId);
        if (loopInfo == null) {
            return null;
        }
        return loopInfo.getLoopValue();
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
    public static void checkLoopIfLevel (CommandStruct.CommandTable command, int level, LoopId loopId) throws ParserException {
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
    public static void setLoopEndIndex (int index, LoopId loopId) throws ParserException {
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
    public static int getLoopNextIndex (CommandStruct.CommandTable command, int index, LoopId loopId) throws ParserException {
        String functionId = CLASS_NAME + ".getLoopNextIndex: ";
        
        int nextIndex = index;
        
        LoopStruct loopInfo = getLoopStruct (loopId);
        if (loopInfo == null) {
            throw new ParserException(functionId + "FOR Loop " + loopId.name + " @ " + loopId.index + " not found");
        }
        
        String action = "";
        switch (command) {
            case CommandStruct.CommandTable.FOR:
                nextIndex = loopInfo.startLoop(index);
                action = "starting";
                break;
            case CommandStruct.CommandTable.BREAK:
                nextIndex = loopInfo.loopBreak();
                action = "exiting";
                break;
            case CommandStruct.CommandTable.NEXT:
            case CommandStruct.CommandTable.CONTINUE:
                nextIndex = loopInfo.loopNext();
                if (nextIndex < index)
                    action = "restarting";
                else
                    action = "exiting";
                break;
            default:
                break;
        }
        
        frame.outputInfoMsg(STATUS_PROGRAM, command.toString() + " command " + action + " at index: " + nextIndex);
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
    public static void saveLoopParameter (String name, LoopId loopId, LoopStruct loopInfo) {
        String functionId = CLASS_NAME + ".saveLoopParameter: ";
        
        // create a new loop ID (name + command index) for the entry and add it
        // to the list of IDs for the loop parameter name
        ArrayList<LoopId> loopList;
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
     * determines the type of data in a String value.
     * 
     * @param strValue - the String value to check
     * 
     * @return the data type found
     */
    public static char classifyDataType (String strValue) {
        char dataType;

        // first check if it is a parameter
        if (strValue.startsWith("$")) {
            char pType = 'S';
            if (strValue.charAt(2) == '_') {
                pType = strValue.charAt(1);
            }
            dataType = switch (pType) {
                case 'I', 'B', 'A', 'L' -> pType;
                default -> 'S';
            };
        }
        else if (strValue.equalsIgnoreCase("TRUE") ||
                 strValue.equalsIgnoreCase("FALSE")) {
            dataType = 'B';
        } else {
            try {
                Long longVal = getLongOrUnsignedValue (strValue);
                dataType = isUnsignedInt(longVal) ? 'U' : 'I';
            } catch (ParserException ex) {
                int offset = strValue.indexOf('{');
                if (offset > 0) {
                    dataType = 'L'; // NOTE: could also be Array, but let's leave it at List
                } else {
                    dataType = 'S';
                }
            }
        }
        
        return dataType;
    }
    
    /**
     * converts a String hexadecimal unsigned value or 64-bit signed Integer value to a Long
     * 
     * @param strValue - value to convert
     * 
     * @return the corresponding unsigned Long value
     * 
     * @throws ParserException 
     */
    public static Long getLongOrUnsignedValue (String strValue) throws ParserException {
        Long longVal;
        Integer iVal = Utils.getHexValue (strValue);
        if (iVal == null) {
            longVal = Utils.getIntValue (strValue);
        } else {
            longVal = iVal.longValue();
        }
        return longVal;
    }

    /**
     * checks if a value is an unsigned integer
     * 
     * @param longVal - value to check
     * 
     * @return true if value is an Unsigned Integer
     */
    public static boolean isUnsignedInt (Long longVal) {
        return longVal >= 0L && longVal <= 0xFFFFFFFFL;
    }
    
    /**
     * deletes the specified parameter.
     * Indicates if the param was not found (does NOT create a new entry).
     * 
     * @param name  - parameter name
     * 
     * @return true if successful, false if the parameter was not found
     */
    public static boolean parameterDelete (String name) {
        if (longParams.containsKey(name)) {
            longParams.remove(name);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted Integer parameter: " + name);
        }
        if (uintParams.containsKey(name)) {
            uintParams.remove(name);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted Unsigned parameter: " + name);
        }
        if (strParams.containsKey(name)) {
            strParams.remove(name);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted String parameter: " + name);
        }
        if (boolParams.containsKey(name)) {
            boolParams.remove(name);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted Boolean parameter: " + name);
        }
        if (arrayParams.containsKey(name)) {
            arrayParams.remove(name);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted Array parameter: " + name);
        }
        if (listParams.containsKey(name)) {
            listParams.remove(name);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted List parameter: " + name);
        }
        return false;
    }

    /**
     * determines if a parameter has been found with the specified name.
     * (Does not check for loop parameters)
     * 
     * @param name - name of the parameter to search for
     * 
     * @return type of parameter if found, null if not found
     */
    public static String isParamDefined (String name) {
        if (longParams.containsKey(name)) {
            return "I";
        }
        if (uintParams.containsKey(name)) {
            return "U";
        }
        if (strParams.containsKey(name)) {
            return "S";
        }
        if (boolParams.containsKey(name)) {
            return "B";
        }
        if (arrayParams.containsKey(name)) {
            return "A";
        }
        if (listParams.containsKey(name)) {
            return "L";
        }
        return null;
    }

    /**
     * determines if a loop parameter has been found with the specified name.
     * 
     * @param name - name of the loop parameter to search for
     * 
     * @return true if the loop parameter was found
     */
    public static boolean isLoopParamDefined (String name) {
        return loopNames.containsKey(name);
    }
    
    /**
     * checks if a parameter name is valid.
     *   - name must begin with an alpha character
     *   - name must be only alphanumeric or '_' chars,
     *   - cannot be a reserved param name (RESPONSE, RESULT)
     *   - cannot be a command name or an operation name
     *   - cannot be a Loop parameter name.
     *   - checks if param is already defined
     * 
     * @param name - the name to check
     * 
     * @return  true if param is already defined, false if not
     * 
     * @throws ParserException - if not valid
     */
    public static boolean isValidParamName (String name) throws ParserException {
        String functionId = CLASS_NAME + ".isValidParamName: ";
        
        if (name.startsWith("$")) {
            name = name.substring(1);
        }
        
        // verify the formaat of the parameter name
        verifyParamFormat(name);
        verifyNotReservedName(name);
        
        if (isLoopParamDefined(name)) {
            throw new ParserException(functionId + "using Loop parameter name: " + name);
        }

        // see if its already defined
        return isParamDefined(name) != null;
    }

    /**
     * checks if a Loop parameter name is valid.
     * 
     * @param name - the name to check
     *               name must be only alphanumeric or '_' chars,
     *               cannot be a reserved name (RESPONSE, RESULT)
     *               or a String or Integer parameter name.
     * @param index - the command index for the FOR command
     * 
     * @return  true if valid
     * 
     * @throws ParserException
     */
    public static boolean isValidLoopName (String name, int index) throws ParserException {
        String functionId = CLASS_NAME + ".isValidLoopName: ";
        
        if (name.startsWith("$")) {
            name = name.substring(1);
        }

        // verify the formaat of the parameter name
        verifyParamFormat(name);
        verifyNotReservedName(name);

        // make sure its not the same as a reference parameter
        String type = isParamDefined(name);
        if (type != null) {
            throw new ParserException(": using " + type + " parameter name: " + name);
        }
        
        // now check if this loop name is nested in a loop having same name
        // get the list of loops using this parameter name (if any)
        ArrayList<LoopId> loopList = loopNames.get(name);
        if (loopList != null && ! loopList.isEmpty()) {
            // we have one or more uses of the same name, check if this is nested in one
            frame.outputInfoMsg(STATUS_PROGRAM, "   - checking previous uses of FOR Loop parameter " + name + " to see if we have a nesting problem");
            for (int ix = 0; ix < loopList.size(); ix++) {
                LoopId loopEntry = loopList.get(ix);
                LoopStruct loopInfo = getLoopStruct (loopEntry);
                if (loopInfo == null || ! loopInfo.isLoopComplete()) {
                    throw new ParserException(functionId + ": Loop param " + name + " @ " + index + " is nested in same name at " + loopEntry.index);
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
     * checks if a parameter name is valid.
     *   name must be only alphanumeric or '_' chars and start with an alpha.
     * 
     * @param name - the name to check
     * 
     * @throws ParserException - if not valid
     */
    private static void verifyParamFormat (String name) throws ParserException {
        String functionId = CLASS_NAME + ".verifyParamFormat: ";
        
        if (name.startsWith("$")) {
            name = name.substring(1);
        }
        if (! Character.isLetter(name.charAt(0))) {
            // 1st character must be a letter
            throw new ParserException(functionId + "invalid initial character in parameter name: " + name);
        }
        for (int ix = 0; ix < name.length(); ix++) {
            if (  (name.charAt(ix) != '_') && ! Character.isLetterOrDigit(name.charAt(ix)) ) {
                throw new ParserException(functionId + "invalid character '" + name.charAt(ix) + "' in parameter name: " + name);
            }
        }
    }

    /**
     * checks if a parameter name matches a reserved name.
     *   checks against command names, operation names and reserved param names.
     * 
     * @param name - the name to check
     * 
     * @throws ParserException - if not valid
     */
    private static void verifyNotReservedName (String name) throws ParserException {
        String functionId = CLASS_NAME + ".verifyNotReservedName: ";
        
        if (name.contentEquals("RESPONSE") || name.contentEquals("RESULT")) {
            throw new ParserException(functionId + "using Reserved parameter name: " + name);
        }
        if (CommandStruct.isValidCommand(name) != null) {
            throw new ParserException(functionId + "using Reserved command name: " + name);
        }
        // TODO: verify against operation names
    }
    
}
