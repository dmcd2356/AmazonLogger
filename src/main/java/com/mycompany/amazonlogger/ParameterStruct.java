/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    
    static final int NAME_MAXLEN = 20;  // the max # chars in a param name

    private String              strParam;       // value for the String  param type
    private Long                longParam;      // value for the Integer param type (64 bit signed)
    private Boolean             boolParam;      // value for the Boolean param type
    private ArrayList<Long>     intArrayParam;  // value for Integer Array param type
    private ArrayList<String>   strArrayParam;  // value for String  List  param type
    private ArrayList<CalcEntry> calcParam;     // value for Calculation param type
    private ParamClass          paramClass;     // class of the parameter
    private ParamType           paramType;      // parameter classification
    private char                paramTypeID;    // ID corresponding to the paramType
    private VariableInfo        variableRef;    // info if a referenced Variable is used instead of a value
    
    // reserved static Variables
    private static ArrayList<String>  strResponse = new ArrayList<>();    // responses from RUN commands
    private static Long               intResult = 0L;      // result of last CALC command
    // user-defined static Variables
    private static final HashMap<String, String>  strParams  = new HashMap<>();
    private static final HashMap<String, Long>    longParams = new HashMap<>();
    private static final HashMap<String, Long>    uintParams = new HashMap<>();
    private static final HashMap<String, Boolean> boolParams = new HashMap<>();
    private static final HashMap<String, ArrayList<Long>>    intArrayParams = new HashMap<>();
    private static final HashMap<String, ArrayList<String>>  strArrayParams = new HashMap<>();
    
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

    public enum ParamClass {
        Discrete,       // a Hard-coded value
        Reference,      // a Variable reference
        Calculation,    // a Calculation formula
    }
    
    public enum ParamType {
        Integer,        // 'I' type
        Unsigned,       // 'U' type
        Boolean,        // 'B' type
        String,         // 'S' type
        IntArray,       // 'A' type
        StringArray,    // 'L' type
    }

    public ParameterStruct() {
        strParam = null;
        longParam = null;
        boolParam = null;
        intArrayParam = null;
        strArrayParam = null;
        calcParam = null;
        paramClass = null;
        variableRef = new VariableInfo();
        paramType = null;
        paramTypeID = '?';
    }

    /**
     * Creates a parameter having the specified characteristics.
     * This is only used in the Compilation phase, so we are creating the parameter
     *   entry and verifying the type is valid, but if it is a Variable reference,
     *   don't replace the Variable with its value.
     *   That can only be done during execution phase.
     * 
     * @param strValue   - the parameter value to use
     * @param pClass     - the parameter classification (Hard-coded, Variable, or Calculation)
     * @param dataType   - the parameter type desired
     * 
     * @throws ParserException
     */
    public ParameterStruct (String strValue, ParamClass pClass, ParamType dataType) throws ParserException {
        String functionId = CLASS_NAME + " (new): ";
        String msgGap = "      ";
        
        variableRef = new VariableInfo();
        calcParam = null;
        strParam = strValue;
        paramClass = pClass;
        paramType = dataType;
        paramTypeID = getParamTypeID (paramType);

        try {
            // Need to do these special cases before we handle the mundane hard-coded entries.
            if (pClass == ParamClass.Calculation) {
                // CALCULATION ENTRY:
                // save the calculation entry
                Calculation calc = new Calculation(strValue, dataType);
                calcParam = calc.copyCalc();

                // if calc is a single entry, don't use Calculation type, switch to Discreet or Variable type
                if (calc.getCalcCount() == 1) {
                    String paramName = calc.getCalcParam();
                    if (paramName != null) {
                        VariableExtract paramInfo = new VariableExtract(paramName);
                        variableRef = new VariableInfo(paramInfo);
                        paramClass = ParamClass.Reference;
                        frame.outputInfoMsg(STATUS_DEBUG, msgGap + "Converted Calculation parameter to single Reference value: " + variableRef.getName());
                    } else {
                        Long value = calc.getCalcValue();
                        if (value != null) {
                            longParam = value;
                            paramClass = ParamClass.Discrete;
                            frame.outputInfoMsg(STATUS_DEBUG, msgGap + "Converted Calculation parameter to single " + paramType + " value: " + value);
                        }
                    }
                } else {
                    frame.outputInfoMsg(STATUS_DEBUG, msgGap + "New ParamStruct: Calculation type " + paramType + " value: " + strValue);
                }
                return;
            }
            if (pClass == ParamClass.Reference) {
                // VARIABLE REFERENCE ENTRY:
                // extract any extension added to the Variable
                VariableExtract paramInfo = new VariableExtract(strParam);
                variableRef = new VariableInfo(paramInfo);
                frame.outputInfoMsg(STATUS_DEBUG, msgGap + "New ParamStruct: Reference type " + paramType + " name: " + variableRef.getName());
                return;
            }

            // HARD-CODED ENTRIES:
            String invalidMsg = "Invalid value for '" + dataType + "' type param: " + strValue;
            switch (dataType) {
                case ParamType.Boolean:
                    if (!strParam.equalsIgnoreCase("TRUE") &&
                        !strParam.equalsIgnoreCase("FALSE") &&
                        !strParam.contentEquals("0") &&
                        !strParam.contentEquals("1") ) {
                        throw new ParserException(functionId + invalidMsg);
                    }
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
                    break;
                case ParamType.Integer:
                    try {
                        longParam = getLongOrUnsignedValue(strParam);
                    } catch (ParserException ex) {
                        throw new ParserException(functionId + invalidMsg);
                    }
                    break;
                case ParamType.IntArray:
                case ParamType.StringArray:
                    // first transfer the array entries to the String Array param
                    strArrayParam = new ArrayList<>(Arrays.asList(strParam.split(",")));
                    intArrayParam = new ArrayList<>();
                    for (int ix = 0; ix < strArrayParam.size(); ix++) {
                        try {
                            // now check if all entries were Integer, even if it was String Array
                            String cleanStr = strArrayParam.get(ix).strip();
                            strArrayParam.set(ix, cleanStr); // remove leading & trailing spaces
                            longParam = getLongOrUnsignedValue(cleanStr);
                            intArrayParam.add(longParam);
                            if (paramType == ParamType.StringArray) {
                                // if it was a String Array but was all Integers, reclassify it
                                paramType = ParamType.IntArray;
                                paramTypeID = getParamTypeID (paramType);
                            }
                        } catch (ParserException ex) {
                            if (paramType == ParamType.IntArray) {
                                throw new ParserException(functionId + invalidMsg);
                            }
                        }
                    }
                    if (paramType == ParamType.IntArray) {
                        frame.outputInfoMsg(STATUS_DEBUG, msgGap + "new " + paramType + " size " + intArrayParam.size());
                    } else {
                        frame.outputInfoMsg(STATUS_DEBUG, msgGap + "new " + paramType + " size " + strArrayParam.size());
                    }
                    break;
                case ParamType.String:
                    // the data has already been added to the String entry, so we are done
                    break;
                default:
                    break;
            }
            frame.outputInfoMsg(STATUS_DEBUG, msgGap + "New ParamStruct: Discreet type " + paramType + " value: " + strParam);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
    }

    /**
     * sets the parameter type value.
     * 
     * @param type 
     */
    private static char getParamTypeID (ParamType type) {
        switch (type) {
            case ParamType.Integer:     return 'I';
            case ParamType.Unsigned:    return 'U';
            case ParamType.Boolean:     return 'B';
            case ParamType.String:      return 'S';
            case ParamType.IntArray:    return 'A';
            case ParamType.StringArray: return 'L';
//            case ParamType.Calculation: return 'C';
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
     * This is used during the Compile phase, so we don't know what the actual
     *   values are at runtime for Variable references. So we need to base
     *   it on the types of parameters that can be converted.
     * 
     * @param typeID - the type of parameter desired
     * 
     * @return true if the parameter is valid for that type
     */
    public boolean isValidForType (char typeID) {
        boolean bParmRef = false;
        ParamType pType = paramType;
        if (variableRef != null && variableRef.getName() != null && !variableRef.getName().isEmpty()) {
            pType = variableRef.getType();
            bParmRef = true;
        }
        switch (typeID) {
            case 'I':
                if (pType == ParamType.Integer ||
                    pType == ParamType.Unsigned   ) {
                    return true;
                }
                break;
            case 'U':
                if (pType == ParamType.Unsigned) {
                    return true;
                } else if (pType == ParamType.Integer && bParmRef) {
                    // only allow Integer type param ref, since it may be in range
                    return true;
                }
                break;
            case 'B':
                if (pType == ParamType.Boolean) {
                    return true;
                }
                break;
            case 'A':
                // Int Array allows single entries for array types
                if (pType == ParamType.IntArray ||
                    pType == ParamType.Integer  ||
                    pType == ParamType.Unsigned   ) {
                    return true;
                }
                break;
            case 'S':
                // anything is good for String except an Array
                if (pType != ParamType.StringArray &&
                    pType != ParamType.IntArray) {
                    return true;
                }
            case 'L':
                // anything works here
                return true;
            default:
                break;
        }
        return false;
    }

    /**
     * determines if the parameter is expressed as a calculation
     * 
     * @return true if the parameter is a Calculation
     */
    public boolean isCalculation () {
        return paramClass == ParamClass.Calculation && (calcParam != null);
    }
       
    /**
     * determines if the parameter is a Variable reference
     * 
     * @return true if the parameter is a Variable Reference
     */
    public boolean isVariableRef () {
        return paramClass == ParamClass.Reference && (variableRef != null) && (variableRef.getName() != null);
    }
       
    /**
     * returns the Variable name if it is a reference.
     * 
     * @return the reference Variable name (null if parameter is not a reference)
     */
    public String getVariableRefName () {
        if (isVariableRef())
            return variableRef.getName();
        else
            return null;
    }
       
    /**
     * returns the parameter type if it is a Variable reference
     * 
     * @return the reference parameter type (null if parameter is not a Variable reference)
     */
    public ParamType getVariableRefType () {
        if (isVariableRef())
            return variableRef.getType();
        else
            return null;
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
     * @param type - the data type being calculated
     * 
     * @return the Integer value of the calculation
     * 
     * @throws com.mycompany.amazonlogger.ParserException
     */
    public Long getCalculationValue (ParamType type) throws ParserException {
        String functionId = CLASS_NAME + ".getCalculationValue: ";

        if (calcParam == null) {
            throw new ParserException(functionId + "Calculation value is null");
        }
        try {
            Calculation calc = new Calculation(calcParam);
            longParam = calc.compute(type);
            return longParam;
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
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
     * returns the StrArray data value
     * 
     * @return the StrArray value
     */
    public ArrayList<String> getStrArray () {
        return strArrayParam;
    }

    /**
     * returns the IntArray data value
     * 
     * @return the IntArray value
     */
    public ArrayList<Long> getIntArray () {
        return intArrayParam;
    }

    /**
     * returns the List data element value
     * 
     * @param index - the entry from the list to get
     * 
     * @return the List element value
     */
    public String getStrArrayElement (int index) {
        if (index >= strArrayParam.size()) {
            return null;
        }
        return strArrayParam.get(index);
    }

    /**
     * returns the Array data element value
     * 
     * @param index - the entry from the array to get
     * 
     * @return the Array element value
     */
    public Long getIntArrayElement (int index) {
        if (index >= intArrayParam.size()) {
            return null;
        }
        return intArrayParam.get(index);
    }

    /**
     * returns the Array data element value
     * 
     * @return the number of elements in the Array
     */
    public int getStrArraySize () {
        if (strArrayParam == null)
            return 0;
        return strArrayParam.size();
    }

    /**
     * returns the Array data element value
     * 
     * @return the number of elements in the Array
     */
    public int getIntArraySize () {
        if (intArrayParam == null)
            return 0;
        return intArrayParam.size();
    }

    /**
     * checks if the parameter is a Variable reference and, if so, reads the value into the structure.
     * It also marks the type of data placed and converts values to other types where possible.
     * 
     * @throws ParserException 
     */
    public void updateFromReference () throws ParserException {
        
        ParameterStruct value = getVariableInfo (variableRef);
        if (value != null) {
            this.paramType      = value.paramType;
            this.paramTypeID    = value.paramTypeID;
            this.longParam      = value.longParam;
            this.boolParam      = value.boolParam;
            this.strParam       = value.strParam;
            this.intArrayParam  = value.intArrayParam;
            this.strArrayParam  = value.strArrayParam;
            this.calcParam      = value.calcParam;
            this.paramClass     = value.paramClass;
            this.variableRef    = value.variableRef;
            
            frame.outputInfoMsg(STATUS_DEBUG, "    unpacked param " + variableRef.getName() + " as type '" + paramTypeID);
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
                frame.outputInfoMsg(STATUS_DEBUG, "    - Converted " + paramType + " value: " + strParam);
                break;
            case ParamType.Unsigned:
                strParam = longParam.toString();
                boolParam = longParam != 0;
                frame.outputInfoMsg(STATUS_DEBUG, "    - Converted " + paramType + " value: " + strParam);
                break;
            case ParamType.Boolean:
                strParam = boolParam.toString();
                longParam = (boolParam) ? 1L : 0L;
                frame.outputInfoMsg(STATUS_DEBUG, "    - Converted " + paramType + " value: " + strParam);
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
                    frame.outputInfoMsg(STATUS_DEBUG, "    - Converted " + paramType + " value: " + strParam);
                }
                break;
            case ParamType.IntArray:
                strParam = intArrayParam.getFirst().toString();
                longParam = intArrayParam.getFirst();
                boolParam = !intArrayParam.isEmpty(); // set to true if array has an entry
                frame.outputInfoMsg(STATUS_DEBUG, "    - Converted " + paramType + " value: " + strParam);
                break;
            case ParamType.StringArray:  // String List type
                strParam = strArrayParam.getFirst();
                frame.outputInfoMsg(STATUS_DEBUG, "    - Converted " + paramType + " value: " + strParam);
                break;
            default:
                break;
        }
    }
    
    /**
     * returns a String for displaying the current param data type and value.
     * 
     * @return a String indicating the parameter type and value
     */
    public String showParam () {
        String strValue;
        String strID = "" + paramTypeID;
        switch (paramClass) {
            case ParamClass.Reference:
                strValue = variableRef.getName();
                strID += "ref";
                break;
            case ParamClass.Calculation:
                strValue = strParam;
                strID += "calc";
                break;
            default:
                switch (paramType) {
                    case ParamType.Integer, ParamType.Unsigned -> {
                        strValue = longParam + "";
                    }
                    case ParamType.Boolean -> {
                        strValue = boolParam + "";
                    }
                    case ParamType.IntArray -> {
                        strValue = intArrayParam.toString();
                    }
                    case ParamType.StringArray -> {
                        strValue = strArrayParam.toString();
                    }
                    default -> {
                        strValue = "'" + strParam + "'";
                    }
                }
                break;
        }
            
        return "  " + strID + ": " + strValue;
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
     * initializes the saved Variables
     */
    public static void initVariables () {
        strParams.clear();
        longParams.clear();
        uintParams.clear();
        boolParams.clear();
        intArrayParams.clear();
        strArrayParams.clear();
        loopParams.clear();
        loopNames.clear();
        strResponse.clear();
        intResult = 0L;
    }

    /**
     * adds a String value to the $RESPONSE Variable
     * 
     * @param value - value to add to the response Variable
     */
    public static void putResponseValue (String value) {
        strResponse.add(value);
    }
    
    /**
     * adds an array of values to the $RESPONSE Variable
     * 
     * @param value - value to add to the response Variable
     */
    public static void putResponseValue (ArrayList<String> value) {
        strResponse.addAll(value);
    }
    
    /**
     * set the value of the $RESULT Variable
     * 
     * @param value - value to set the result Variable to
     */
    public static void putResultValue (Long value) {
        intResult = value;
    }

    /**
     * creates a new entry in the Variable table and sets the initial value.
     * 
     * @param name  - Variable name
     * 
     * @throws ParserException - if Variable was already defined
     */
    public static void allocateVariable (String name) throws ParserException {
        String functionId = CLASS_NAME + ".allocateVariable: ";

        // first, verify Variable name to make sure it is valid format and
        //  not already used.
        boolean bIsDefined;
        try {
            bIsDefined = isValidVariableName(name);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        if (bIsDefined) {
            throw new ParserException(functionId + "Variable " + name + " already defined");
        }

        String typeName = "String";
        switch (getVariableTypeFromName(name)) {
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
                intArrayParams.put(name, new ArrayList<>());
                break;
            case ParamType.StringArray:
                typeName = "List";
                strArrayParams.put(name, new ArrayList<>());
                break;
            case ParamType.String:
                strParams.put(name, "");        // default value to empty String
                break;
            default:
                break;
        }
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Allocated " + typeName + " variable: " + name);
    }

    /**
     * returns the value of a reference Variable along with its data type.
     * 
     * @param paramInfo  - Variable reference information
     * 
     * @return the Variable value
     * 
     * @throws ParserException - if Variable not found
     */
    public static ParameterStruct getVariableInfo (VariableInfo paramInfo) throws ParserException {
        String functionId = CLASS_NAME + ".getVariableInfo: ";

        if (paramInfo == null || paramInfo.getName() == null || paramInfo.getType() == null) {
            return null;
        }
        
        // create a new parameter with all null entries
        ParameterStruct paramValue = new ParameterStruct();

        String name = paramInfo.getName();
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }
        ParamType pType = paramInfo.getType();
        ParamType findType = isVariableDefined(name);
        if (findType == null) {
            frame.outputInfoMsg(STATUS_PROGRAM, "    - Variable Ref '" + name + "' as type " + pType + " was not found in any Variable database");
        }
        if (pType == null || findType != pType) {
            frame.outputInfoMsg(STATUS_PROGRAM, "    - Incorrect Variable Ref type for '" + name + "' as type " + pType + " is actually " + findType);
            pType = findType;
        }
        
        switch (pType) {
            case ParamType.Integer:
                paramValue.longParam = longParams.get(name);
                if (paramValue.longParam == null) {
                    throw new ParserException(functionId + "Parameter " + name + " not found");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    - Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.longParam);
                break;
            case ParamType.Unsigned:
                paramValue.longParam = uintParams.get(name);
                if (paramValue.longParam == null) {
                    throw new ParserException(functionId + "Parameter " + name + " not found");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    - Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.longParam);
                break;
            case ParamType.Boolean:
                paramValue.boolParam = boolParams.get(name);
                if (paramValue.boolParam == null) {
                    throw new ParserException(functionId + "Parameter " + name + " not found");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    - Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.boolParam);
                break;
            case ParamType.IntArray:
                paramValue.intArrayParam = intArrayParams.get(name);
                if (paramValue.intArrayParam == null) {
                    throw new ParserException(functionId + "Parameter " + name + " not found");
                }
                String arrayValue = paramValue.intArrayParam.toString();
                if (arrayValue.length() > 100) {
                    arrayValue = arrayValue.substring(0,100) + "...";
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    - Lookup Ref '" + name + "' as type " + pType + ": " + arrayValue);
                // check for extensions to reference param
                if (paramInfo.getIndexStart() != null) {
                    int iStart = paramInfo.getIndexStart();
                    if (iStart < paramValue.intArrayParam.size()) {
                        paramValue.longParam = paramValue.intArrayParam.get(iStart);
                        pType = ParamType.Integer;
                        frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + "index[" + iStart + "] ' as type Integer: " + paramValue.longParam);
                    } else {
                        throw new ParserException(functionId + "Parameter " + name + " index " + iStart + " exceeds array");
                    }
                } else if (paramInfo.getTrait() == VariableExtract.Trait.SIZE) {
                    paramValue.longParam = (long)paramValue.intArrayParam.size();
                    pType = ParamType.Integer;
                    frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + ".SIZE ' as type Integer: " + paramValue.longParam);
                } else if (paramInfo.getTrait() == VariableExtract.Trait.ISEMPTY) {
                    paramValue.boolParam = paramValue.intArrayParam.isEmpty();
                    pType = ParamType.Boolean;
                    frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + ".ISEMPTY ' as type Boolean: " + paramValue.boolParam);
                }
                break;
            case ParamType.StringArray:
                if (name.contentEquals("RESPONSE")) {
                    paramValue.strArrayParam = strResponse;
                } else  {
                    paramValue.strArrayParam = strArrayParams.get(name);
                    if (paramValue.strArrayParam == null) {
                        throw new ParserException(functionId + "Parameter " + name + " not found");
                    }
                }
                arrayValue = paramValue.strArrayParam.toString();
                if (arrayValue.length() > 100) {
                    arrayValue = arrayValue.substring(0,100) + "...";
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    - Lookup Ref '" + name + "' as type " + pType + ": " + arrayValue);
                // check for extensions to reference param
                if (paramInfo.getIndexStart() != null) {
                    int iStart = paramInfo.getIndexStart();
                    if (iStart < paramValue.strArrayParam.size()) {
                        paramValue.strParam = paramValue.strArrayParam.get(iStart);
                        pType = ParamType.String;
                        frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + "index[" + iStart + "] ' as type String: " + paramValue.strParam);
                    } else {
                        throw new ParserException(functionId + "Parameter " + name + " index " + iStart + " exceeds array");
                    }
                } else if (paramInfo.getTrait() == VariableExtract.Trait.SIZE) {
                    paramValue.longParam = (long)paramValue.strArrayParam.size();
                    pType = ParamType.Integer;
                    frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + ".SIZE ' as type Integer: " + paramValue.longParam);
                } else if (paramInfo.getTrait() == VariableExtract.Trait.ISEMPTY) {
                    paramValue.boolParam = paramValue.strArrayParam.isEmpty();
                    pType = ParamType.Boolean;
                    frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + ".ISEMPTY ' as type Boolean: " + paramValue.boolParam);
                }
                break;
            default:
                if (name.contentEquals("RESULT")) {
                    paramValue.longParam = intResult;
                    if (isUnsignedInt(paramValue.longParam)) {
                        pType = ParamType.Unsigned;
                    } else {
                        pType = ParamType.Integer;
                    }
                } else {
                    paramValue.strParam = strParams.get(name);
                    if (paramValue.strParam != null) {
                        pType = ParamType.String;
                        // check for extensions to reference param
                        if (paramInfo.getIndexStart() != null) {
                            int iStart = paramInfo.getIndexStart();
                            int iEnd = iStart + 1;
                            String ixRange = "[" + iStart + "]";
                            if(paramInfo.getIndexEnd() != null) {
                                iEnd = paramInfo.getIndexEnd();
                                ixRange = "[" + iStart + "-" + iEnd + "]";
                            }
                            if (iEnd <= paramValue.strArrayParam.size()) {
                                paramValue.strParam = paramValue.strParam.substring(iStart, iEnd);
                                frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + "index" + ixRange + " ' as type String: " + paramValue.strParam);
                            } else {
                                throw new ParserException(functionId + "Parameter " + name + " index" + ixRange + " exceeds array");
                            }
                        } else if (paramInfo.getTrait() == VariableExtract.Trait.SIZE) {
                            paramValue.longParam = (long)paramValue.strParam.length();
                            pType = ParamType.Integer;
                            frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + ".SIZE ' as type Integer: " + paramValue.longParam);
                        } else if (paramInfo.getTrait() == VariableExtract.Trait.ISEMPTY) {
                            paramValue.boolParam = paramValue.strParam.isEmpty();
                            pType = ParamType.Boolean;
                            frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + ".ISEMPTY ' as type Boolean: " + paramValue.boolParam);
                        }
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
                frame.outputInfoMsg(STATUS_PROGRAM, "    - Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.strParam);
                break;
        }

        // save the parameter type and name
        paramValue.variableRef = new VariableInfo (paramInfo);
        paramValue.paramType   = pType;
        paramValue.paramTypeID = getParamTypeID (pType);
        paramValue.paramClass  = ParamClass.Discrete;

        // convert value to other forms where possible
        paramValue.updateConversions();
        return paramValue;
    }

    /**
     * modifies the value of an existing entry in the String Variable table.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param value - Variable value
     * 
     * @throws ParserException
     */
    public static void modifyStringVariable (String name, String value) throws ParserException {
        String functionId = CLASS_NAME + ".modifyStringVariable: ";

        if (!strParams.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        strParams.replace(name, value);
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified String param: " + name + " = " + value);
    }

    /**
     * modifies the value of an existing entry in the Integer Variable table.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param value - Variable value
     * 
     * @throws ParserException
     */
    public static void modifyIntegerVariable (String name, Long value) throws ParserException {
        String functionId = CLASS_NAME + ".modifyIntegerVariable: ";

        if (!longParams.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        longParams.replace(name, value);
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified Integer param: " + name + " = " + value);
    }

    /**
     * modifies the value of an existing entry in the Unsigned Variable table.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param value - Variable value
     * 
     * @throws ParserException
     */
    public static void modifyUnsignedVariable (String name, Long value) throws ParserException {
        String functionId = CLASS_NAME + ".modifyUnsignedVariable: ";

        if (!uintParams.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        if (! isUnsignedInt(value)) {
            throw new ParserException(functionId + "value for Variable " + name + " exceeds limits for Unsigned: " + value);
        }
        uintParams.replace(name, value);
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified Unsigned param: " + name + " = " + value);
    }

    /**
     * modifies the value of an existing entry in the Boolean Variable table.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param value - Variable value
     * 
     * @throws ParserException
     */
    public static void modifyBooleanVariable (String name, Boolean value) throws ParserException {
        String functionId = CLASS_NAME + ".modifyBooleanVariable: ";

        if (!boolParams.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        boolParams.replace(name, value);
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified Boolean param: " + name + " = " + value);
    }

    /**
     * get the number of elements in an existing Array Variable.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name   - Variable name
     * 
     * @return number of entries in array
     * 
     * @throws ParserException
     */
    public static int getIntArraySize (String name) throws ParserException {
        String functionId = CLASS_NAME + ".getArraySize: ";

        if (intArrayParams.containsKey(name)) {
            ArrayList<Long> entry = intArrayParams.get(name);
            return entry.size();
        } else if (strArrayParams.containsKey(name)) {
            ArrayList<String> entry = strArrayParams.get(name);
            return entry.size();
        } else if (name.contentEquals("RESPONSE")) {
            return strResponse.size();
        }
        throw new ParserException(functionId + "Array Variable " + name + " not found");
    }
    
    /**
     * saves the array in a String Array Variable.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param value - Variable value
     * 
     * @throws ParserException
     */
    public static void setStrArrayVariable (String name, ArrayList<String> value) throws ParserException {
        String functionId = CLASS_NAME + ".setStrArrayVariable: ";

        if (!strArrayParams.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        strArrayParams.replace(name, value);
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Saved StrArray param: " + name + " = " + value);
    }

    /**
     * saves the array in a Integer Array Variable.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param value - Variable value
     * 
     * @throws ParserException
     */
    public static void setIntArrayVariable (String name, ArrayList<Long> value) throws ParserException {
        String functionId = CLASS_NAME + ".setIntArrayVariable: ";

        if (!intArrayParams.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        intArrayParams.replace(name, value);
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Saved IntArray param: " + name + " = " + value);
    }

    /**
     * clears all entries of an existing Array Variable.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * 
     * @return true if successful, false if the Variable was not found
     */
    public static boolean arrayRemoveAll (String name) {
        if (name.contentEquals("RESPONSE")) {
            int size = strResponse.size();
            strResponse.clear();
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted " + size + " entries in Array param: " + name);
            return true;
        }
        else if (intArrayParams.containsKey(name)) {
            ArrayList<Long> entry = intArrayParams.get(name);
            int size = entry.size();
            entry.clear();
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted " + size + " entries in Array param: " + name);
            return true;
        }
        else if (strArrayParams.containsKey(name)) {
            ArrayList<String> entry = strArrayParams.get(name);
            int size = entry.size();
            entry.clear();
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted " + size + " entries in List param: " + name);
            return true;
        }
        return false;
    }

    /**
     * clears selected entries of an existing Array Variable.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name   - Variable name
     * @param iStart - index of starting entry in array to delete
     * @param iCount - number of entries in array to delete
     * 
     * @return true if successful, false if the Variable was not found
     * 
     * @throws ParserException
     */
    public static boolean arrayRemoveEntries (String name, int iStart, int iCount) throws ParserException {
        String functionId = CLASS_NAME + ".arrayRemoveEntries: ";

        if (iCount < 1 || iStart < 0) {
            throw new ParserException(functionId + "Array Variable " + name + " index range exceeded: " + iStart + " to " + (iStart + iCount) + " is invalid");
        }
        int size;
        String arrayContents;
        if (intArrayParams.containsKey(name)) {
            ArrayList<Long> entry = intArrayParams.get(name);
            size = entry.size();
            if (iStart + iCount > size) {
                throw new ParserException(functionId + "Array Variable " + name + " index range exceeded: " + iStart + " to " + (iStart + iCount) + " (max " + entry.size() + ")");
            }
            if (iCount == entry.size()) {
                entry.clear();
            } else {
                for (int ix = 0; ix < iCount; ix++) {
                    entry.remove(iStart);
                }
            }
            arrayContents = entry.toString();
        } else if (strArrayParams.containsKey(name)) {
            ArrayList<String> entry = strArrayParams.get(name);
            size = entry.size();
            if (iStart + iCount > size) {
                throw new ParserException(functionId + "Array Variable " + name + " index range exceeded: " + iStart + " to " + (iStart + iCount) + " (max " + entry.size() + ")");
            }
            if (iCount == entry.size()) {
                entry.clear();
            } else {
                for (int ix = 0; ix < iCount; ix++) {
                    entry.remove(iStart);
                }
            }
            arrayContents = entry.toString();
        } else {
            return false;
        }
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Removed " + iCount + " entries from Array param " + name + ": (new size = "+ size  + ")");
        frame.outputInfoMsg(STATUS_PROGRAM, "   - " + name + ": " + arrayContents);
        return true;
    }

    /**
     * modifies the value of an existing entry in the Array or List Variable table.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param index - index of entry in list to change
     * @param value - Variable value
     * 
     * @return true if successful, false if the Variable was not found
     * 
     * @throws ParserException
     */
    public static boolean arrayModifyEntry (String name, int index, String value) throws ParserException {
        String functionId = CLASS_NAME + ".arrayModifyEntry: ";

        try {
            String arrayContents;
            if (intArrayParams.containsKey(name)) {
                ArrayList<Long> entry = intArrayParams.get(name);
                if (index >= entry.size()) {
                    throw new ParserException(functionId + "Array Variable " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
                }
                Long longVal = getLongOrUnsignedValue (value);
                entry.set(index, longVal);
                arrayContents = entry.toString();
            }
            else if (strArrayParams.containsKey(name)) {
                ArrayList<String> entry = strArrayParams.get(name);
                if (index >= entry.size()) {
                    throw new ParserException(functionId + "List Variable " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
                }
                entry.set(index, value);
                arrayContents = entry.toString();
            } else {
                return false;
            }
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified List param: " + name + " = " + value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - " + name + ": " + arrayContents);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        return true;
    }

    /**
     * inserts a value into an existing Array Variable table.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param index - index of where to insert the value
     *                (moves current index value and all following values back 1 entry)
     * @param value - Variable value
     * 
     * @return true if successful, false if the Variable was not found
     * 
     * @throws ParserException
     */
    public static boolean arrayInsertEntry (String name, int index, String value) throws ParserException {
        String functionId = CLASS_NAME + ".arrayInsertEntry: ";

        try {
            String arrayContents;
            if (intArrayParams.containsKey(name)) {
                ArrayList<Long> entry = intArrayParams.get(name);
                Long longVal = getLongOrUnsignedValue (value);
                if (index >= entry.size() || entry.isEmpty()) {
                    throw new ParserException(functionId + "Variable " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
                }
                // bump current entries back 1
                entry.addLast(entry.getLast());
                for (int ix = entry.size()-2; ix >= index; ix--) {
                    entry.set(ix+1, entry.get(ix));
                }
                entry.set(index, longVal);
                arrayContents = entry.toString();
            }
            else if (strArrayParams.containsKey(name)) {
                ArrayList<String> entry = strArrayParams.get(name);
                if (index >= entry.size() || entry.isEmpty()) {
                    throw new ParserException(functionId + "Variable " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
                }
                // bump current entries back 1
                entry.addLast(entry.getLast());
                for (int ix = entry.size()-2; ix >= index; ix--) {
                    entry.set(ix+1, entry.get(ix));
                }
                entry.set(index, value);
                arrayContents = entry.toString();
            } else {
                return false;
            }
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Inserted entry[" + index + "] in param: " + name + " = " + value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - " + name + ": " + arrayContents);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        return true;
    }

    /**
     * appends a value to the end of an existing Array Variable table.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param value - Variable value
     * 
     * @return true if successful, false if the Variable was not found
     * 
     * @throws ParserException
     */
    public static boolean arrayAppendEntry (String name, String value) throws ParserException {
        String functionId = CLASS_NAME + ".arrayAppendEntry: ";

        String arrayContents;
        if (intArrayParams.containsKey(name)) {
            ArrayList<Long> entry = intArrayParams.get(name);
            try {
                Long longVal = getLongOrUnsignedValue (value);
                entry.addLast(longVal);
                arrayContents = entry.toString();
            } catch (ParserException exMsg) {
                throw new ParserException(exMsg + "\n  -> " + functionId);
            }
        }
        else if (strArrayParams.containsKey(name)) {
            ArrayList<String> entry = strArrayParams.get(name);
            entry.addLast(value);
            arrayContents = entry.toString();
        } else {
            return false;
        }
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Appended entry to Variable: " + name + " = " + value);
        frame.outputInfoMsg(STATUS_PROGRAM, "   - " + name + ": " + arrayContents);
        return true;
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
        for (Map.Entry pair : loopParams.entrySet()) {
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
     * @return value of the loop Variable, null if not found
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
     * @param loopId  - the loop Variable currently running
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
     * adds a new entry in the Loop Variables table.
     * This should only be called by 'CommandParser.compileProgram' when stepping
     * through the commands to verify and create the compiled list of commands.
     * 
     * @param name     - loop Variable name
     * @param loopId   - loop name-index combination to uniquely identify the loop param
     * @param loopInfo - the loop Variable to add
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

        // first check if it is a Variable
        if (strValue.startsWith("$")) {
            char pType = 'S';
            if (strValue.charAt(2) == '_') {
                pType = strValue.charAt(1);
            }
            dataType = switch (pType) {
                case 'I', 'U', 'B', 'A', 'L' -> pType;
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
        String functionId = CLASS_NAME + ".getLongOrUnsignedValue: ";
        
        Long longVal;
        try {
            Integer iVal = Utils.getHexValue (strValue);
            if (iVal == null) {
                longVal = Utils.getIntValue (strValue);
            } else {
                longVal = iVal.longValue();
            }
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
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
     * deletes the specified Variable.
     * Indicates if the Variable was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * 
     * @return true if successful, false if the Variable was not found
     */
    public static boolean variableDelete (String name) {
        if (longParams.containsKey(name)) {
            longParams.remove(name);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted Integer Variable: " + name);
        }
        if (uintParams.containsKey(name)) {
            uintParams.remove(name);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted Unsigned Variable: " + name);
        }
        if (strParams.containsKey(name)) {
            strParams.remove(name);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted String Variable: " + name);
        }
        if (boolParams.containsKey(name)) {
            boolParams.remove(name);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted Boolean Variable: " + name);
        }
        if (intArrayParams.containsKey(name)) {
            intArrayParams.remove(name);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted Array Variable: " + name);
        }
        if (strArrayParams.containsKey(name)) {
            strArrayParams.remove(name);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted List Variable: " + name);
        }
        return false;
    }

    /**
     * determines the type of Variable from the first 2 chars of the Variable name.
     * 
     * @param name - name of the Variable
     * 
     * @return the corresponding Variable type
     */    
    public static ParamType getVariableTypeFromName (String name) {
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }
        if (name.length() > 1 && name.charAt(1) == '_') {
            switch (name.charAt(0)) {
                case 'I': return ParamType.Integer;
                case 'U': return ParamType.Unsigned;
                case 'B': return ParamType.Boolean;
                case 'A': return ParamType.IntArray;
                case 'L': return ParamType.StringArray;
                default:  return ParamType.String;
            }
        } else if (name.contentEquals("RESPONSE")) {
            return ParamType.StringArray;
        } else if (name.contentEquals("RESULT")) {
            return ParamType.Integer;
        }
        // default
        return ParamType.String;
    }
    
    /**
     * checks if a Variable is an Integer type
     * 
     * @param name - the name to check
     * 
     * @return  true if Integer Variable
     */
    public static boolean isIntegerParam (String name) {
        return name.startsWith("I_");
    }
    
    /**
     * determines if a Variable has been found with the specified name.
     * (Does not check for loop Variables)
     * 
     * @param name - name of the Variable to search for
     * 
     * @return type of Variable if found, null if not found
     */
    public static ParamType isVariableDefined (String name) {
        if (longParams.containsKey(name)) {
            return ParamType.Integer;
        }
        if (uintParams.containsKey(name)) {
            return ParamType.Unsigned;
        }
        if (strParams.containsKey(name)) {
            return ParamType.String;
        }
        if (boolParams.containsKey(name)) {
            return ParamType.Boolean;
        }
        if (intArrayParams.containsKey(name)) {
            return ParamType.IntArray;
        }
        if (strArrayParams.containsKey(name)) {
            return ParamType.StringArray;
        }
        if (name.contentEquals("RESPONSE")) {
            return ParamType.StringArray;
        }
        if (name.contentEquals("RESULT")) {
            return ParamType.Integer;
        }
        return null;
    }

    /**
     * determines if a loop Variable has been found with the specified name.
     * 
     * @param name - name of the loop Variable to search for
     * 
     * @return true if the loop Variable was found
     */
    public static boolean isLoopParamDefined (String name) {
        return loopNames.containsKey(name);
    }

    /**
     * checks if a Variable name is valid.
     *   - name must begin with an alpha character
     *   - name must be only alphanumeric or '_' chars,
     *   - cannot be a reserved Variable name (RESPONSE, RESULT)
     *   - cannot be a command name or an operation name
     *   - cannot be a Loop Variable name.
     *   - checks if Variable is already defined
     * 
     * @param name - the name to check
     * 
     * @return  true if Variable is already defined, false if not
     * 
     * @throws ParserException - if not valid
     */
    public static boolean isValidVariableName (String name) throws ParserException {
        String functionId = CLASS_NAME + ".isValidParamName: ";

        try {
            if (name.startsWith("$")) {
                name = name.substring(1);
            }

            // verify the formaat of the Variable name
            verifyVariableFormat(name);

            // check if it is a reserved param name
            if (name.contentEquals("RESPONSE") ||
                name.contentEquals("RESULT")  ) {
                return true;
            }

            // make sure it is not a command namee
            if (CommandStruct.isValidCommand(name) != null) {
                throw new ParserException(functionId + "using Reserved command name: " + name);
            }

            if (isLoopParamDefined(name)) {
                throw new ParserException(functionId + "using Loop Variable name: " + name);
            }

            // see if its already defined
            return isVariableDefined(name) != null;
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId + name);
        }
    }

    /**
     * checks if a Loop Variable name is valid.
     * 
     * @param name - the name to check
     *               name must be only alphanumeric or '_' chars,
     *               cannot be a reserved name (RESPONSE, RESULT)
     *               or a String or Integer Variable name.
     * @param index - the command index for the FOR command
     * 
     * @throws ParserException - if not valid
     */
    public static void isValidLoopName (String name, int index) throws ParserException {
        String functionId = CLASS_NAME + ".isValidLoopName: ";

        try {
            if (name.startsWith("$")) {
                name = name.substring(1);
            }

            // verify the formaat of the Variable name
            verifyVariableFormat(name);

            // make sure it is not a command name of a reserved param name
            if (name.contentEquals("RESPONSE") ||
                name.contentEquals("RESULT")  ) {
                throw new ParserException(functionId + "using Reserved Variable name: " + name);
            }
            if (CommandStruct.isValidCommand(name) != null) {
                throw new ParserException(functionId + "using Reserved command name: " + name);
            }

            // make sure its not the same as a reference Variable
            ParamType type = isVariableDefined(name);
            if (type != null) {
                throw new ParserException(": using " + type.toString() + " Variable name: " + name);
            }

            // now check if this loop name is nested in a loop having same name
            // get the list of loops using this Variable name (if any)
            ArrayList<LoopId> loopList = loopNames.get(name);
            if (loopList != null && ! loopList.isEmpty()) {
                // we have one or more uses of the same name, check if this is nested in one
                frame.outputInfoMsg(STATUS_PROGRAM, "   - checking previous uses of FOR Loop Variable " + name + " to see if we have a nesting problem");
                for (int ix = 0; ix < loopList.size(); ix++) {
                    LoopId loopEntry = loopList.get(ix);
                    LoopStruct loopInfo = getLoopStruct (loopEntry);
                    if (loopInfo == null || ! loopInfo.isLoopComplete()) {
                        throw new ParserException(functionId + ": Loop param " + name + " @ " + index + " is nested in same name at " + loopEntry.index);
                    } else {
                        frame.outputInfoMsg(STATUS_PROGRAM, "   - FOR Loop Variable " + name + " @ " + loopEntry.index + " was complete");
                    }
                }
            }
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
    }
    
    //========================================================================
    // LOCAL METHODS
    //========================================================================

    /**
     * checks if a Variable name is valid.
     *   name must be only alphanumeric or '_' chars and start with an alpha.
     * 
     * @param name - the name to check
     * 
     * @return true if Variable name is syntactically valid
     * 
     * @throws ParserException - if not valid
     */
    private static boolean verifyVariableFormat (String name) throws ParserException {
        String functionId = CLASS_NAME + ".verifyParamFormat: ";
        
        if (name == null) {
            throw new ParserException(functionId + "Variable name is null");
        }
        if (name.isBlank()) {
            throw new ParserException(functionId + "Variable name is blank");
        }
        boolean bRighthand = false;
        if (name.startsWith("$")) {
            name = name.substring(1);
            bRighthand = true;
        }
        if (! Character.isLetter(name.charAt(0))) {
            // 1st character must be a letter
            throw new ParserException(functionId + "invalid initial character in Variable name: " + name);
        }

        // determine if we have a special param type that can take on appendages
        ParamType type = getVariableTypeFromName (name);
        
            // TODO: we need to do this for the '.' operator as well
            int indexStart = 0;
            int indexEnd = 0;
            for (int ix = 0; ix < name.length(); ix++) {
                char curch = name.charAt(ix);
                // valid char for Variable
                if ( (curch == '_') || Character.isLetterOrDigit(curch) ) {
                    if (ix > NAME_MAXLEN) {
                        throw new ParserException(functionId + "Variable name too long (max len " + NAME_MAXLEN + ") in name: " + name.substring(0, ix));
                    }
                } else {
                    // this will terminate the Variable search
                    if (curch == ' ' || curch == '=') {
                        break;
                    }
                    if (!bRighthand) {
                        throw new ParserException(functionId + "Variable assignment should not include '$': " + name);
                    }
                    if (type != ParamType.String && type != ParamType.StringArray && type != ParamType.IntArray) {
                        throw new ParserException(functionId + "Variable extensions are only valid for String and Array types: " + name);
                    }
                    // check for bracket index on Array, List and String Variable
                    switch (curch) {
                        case '[':
                            int offset = name.indexOf(']');
                            if (offset <= 0 || offset >= name.length() - 1) {
                                throw new ParserException(functionId + "missing end bracket in Variable name: " + name);
                            }
                            try {
                                indexStart = Utils.getIntValue(name.substring(ix+1)).intValue();
                            } catch (ParserException exMsg) {
                                throw new ParserException(functionId + "invalid numeric in brackets");
                            }
                            // TODO: evaluate indexEnd
                            break;
                        case '.':
                            // TODO:
                            break;
                        default:
                            throw new ParserException(functionId + "invalid character '" + curch + "' in Variable name: " + name);
                    }
                }
            }
            if (indexStart == 0 && name.length() > NAME_MAXLEN) {
                throw new ParserException(functionId + "Variable name too long (max len " + NAME_MAXLEN + ") in name: " + name);
            }
            if (indexStart > 0 && indexEnd == 0) {
                throw new ParserException(functionId + "Variable name index missing ending bracket: " + name);
            }
            
        return true;
    }
    
}
