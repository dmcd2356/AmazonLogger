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
    
    private String              strParam;       // value for the String  param type
    private Long                longParam;      // value for the Integer and Unsigned param types
    private Boolean             boolParam;      // value for the Boolean param type
    private ArrayList<Long>     intArrayParam;  // value for Integer Array param type
    private ArrayList<String>   strArrayParam;  // value for String  List  param type
    private ArrayList<CalcEntry> calcParam;     // value for Calculation param type
    
    private ParamClass          paramClass;     // class of the parameter
    private ParamType           paramType;      // parameter classification
    private char                paramTypeID;    // ID corresponding to the paramType
    private VariableInfo        variableRef;    // info if a referenced Variable is used instead of a value
    
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

    //==========================================    
    // Setter functions
    //==========================================
    
    public void setStringValue (String value) {
        strParam = value;
    }
    
    public void setIntegerValue (Long value) {
        longParam = value;
    }
    
    public void setBooleanValue (Boolean value) {
        boolParam = value;
    }
    
    public void setStrArray (ArrayList<String> value) {
        strArrayParam = value;
    }

    public void setIntArray (ArrayList<Long> value) {
        intArrayParam = value;
    }
    
    public void setVariableRef (VariableInfo value) {
        variableRef = value;
    }
    
    public void setParamType (ParamType value) {
        paramType = value;
    }

    public void setParamTypeDiscrete (ParamType ptype) {
        paramType   = ptype;
        paramTypeID = getParamTypeID (ptype);
        paramClass  = ParamClass.Discrete;
    }
    
    //==========================================    
    // Getter functions
    //==========================================
    
    public String getStringValue () {
        return strParam;
    }
        
    public Long getIntegerValue () {
        return longParam;
    }
        
    public Boolean getBooleanValue () {
        return boolParam;
    }

    public ArrayList<String> getStrArray () {
        return strArrayParam;
    }

    public ArrayList<Long> getIntArray () {
        return intArrayParam;
    }

    public ParamType getParamType () {
        return paramType;
    }

    public String getStrArrayElement (int index) {
        if (strArrayParam == null || index >= strArrayParam.size())
            return null;
        return strArrayParam.get(index);
    }

    public Long getIntArrayElement (int index) {
        if (intArrayParam == null || index >= intArrayParam.size())
            return null;
        return intArrayParam.get(index);
    }

    public int getStrArraySize () {
        if (strArrayParam == null)
            return 0;
        return strArrayParam.size();
    }

    public int getIntArraySize () {
        if (intArrayParam == null)
            return 0;
        return intArrayParam.size();
    }

    public boolean isStrArrayEmpty () {
        return (strArrayParam == null) ? true : strArrayParam.isEmpty();
    }

    public boolean isIntArrayEmpty () {
        return (intArrayParam == null) ? true : intArrayParam.isEmpty();
    }

    /**
     * determines if the parameter is a Variable reference
     * 
     * @return true if the parameter is a Variable Reference
     */
    public boolean isVariableRef () {
        return (variableRef != null) && (variableRef.getName() != null);
    }
       
    /**
     * returns the Variable name if it is a reference.
     * 
     * @return the reference Variable name (null if parameter is not a reference)
     */
    public String getVariableRefName () {
        return isVariableRef() ? variableRef.getName() : null;
    }
       
    /**
     * returns the parameter type if it is a Variable reference
     * 
     * @return the reference parameter type (null if parameter is not a Variable reference)
     */
    public ParamType getVariableRefType () {
        return isVariableRef() ? variableRef.getType() : null;
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
     * sets the parameter type value.
     * 
     * @param type 
     */
    public static char getParamTypeID (ParamType type) {
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
     * checks if the parameter is a Variable reference and, if so, reads the value into the structure.
     * It also marks the type of data placed and converts values to other types where possible.
     * 
     * @throws ParserException 
     */
    public void updateFromReference () throws ParserException {
        
        ParameterStruct value = Variables.getVariableInfo (variableRef);
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
                        if (strValue.length() > 100) {
                            int offset = strValue.length() - 25;
                            strValue = strValue.substring(0, 60) + " ... " + strValue.substring(offset);
                        }
                    }
                    case ParamType.StringArray -> {
                        strValue = strArrayParam.toString();
                        if (strValue.length() > 100) {
                            int offset = strValue.length() - 25;
                            strValue = strValue.substring(0, 60) + " ... " + strValue.substring(offset);
                        }
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
     * gets the Integer value of a parameter and verifies it qualifies as an Unsigned.
     * 
     * This is to be used during Compile stage only!
     * 
     * @param parm  - the argument list
     * @param index - the index of the argument to get
     * @param type  - the expected type of argument ('I', 'U', etc)
     * 
     * @throws ParserException if not valid Unsigned value
     *
     */
    public static void verifyArgEntryCompile (ArrayList<ParameterStruct> parm, int index, char type) throws ParserException {
        String functionId = CLASS_NAME + ".verifyArgEntry: ";

        // verify index does not exceed bounds
        if (parm.size() <= index) {
            throw new ParserException(functionId + "Parameter index " + index + " exceeds list size of " + parm.size());
        }
        
        // these types can be lowercase if entry is optional
        type = Character.toUpperCase(type);
        ParameterStruct.ParamType expType = CmdOptions.getParameterType(type);
        
        // verify type is correct and entry is not null
        ParameterStruct.ParamType ptype = parm.get(index).getParamType();
        boolean bVariable = parm.get(index).isVariableRef();
        if (bVariable) {
            // if it's a variable, we can't really say what the data type is at this
            // point, since it could be a String that has an Integer value.
            return;
//            ptype = parm.get(index).getVariableRefType();
        }
        boolean bValid = ptype == expType;
        if (! bValid) {
            // check for other alternate types that are allowed and if entry is not null
            switch (expType) {
                case Integer:
                    if (ptype == ParameterStruct.ParamType.Unsigned)
                        bValid = true;
                    break;
                case Unsigned:
                    if (ptype == ParameterStruct.ParamType.Integer)
                        bValid = true;
                    break;
                case Boolean:
                    break;
                case String:
                    break;
                case IntArray:
                    switch (ptype) {
                        case Integer:
                        case Unsigned:
                            bValid = true;
                            break;
                        default:
                            break;
                    }
                    break;
                case StringArray:
                    switch (ptype) {
                        case String:
                        case IntArray:
                            bValid = true;
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
        if (! bValid) {
            throw new ParserException(functionId + "Param[" + index + "] expected type " + expType + ", was type: " + ptype);
        }
    }
    
    /**
     * gets the Integer value of a parameter and verifies it qualifies as an Unsigned.
     * 
     * This is to be used during Execution stage only!
     * 
     * @param parm  - the argument list
     * @param index - the index of the argument to get
     * @param type  - the expected type of argument ('I', 'U', etc)
     * 
     * @return unsigned value
     * 
     * @throws ParserException if not valid Unsigned value
     *
     */
    public static ParameterStruct verifyArgEntry (ArrayList<ParameterStruct> parm, int index, char type) throws ParserException {
        String functionId = CLASS_NAME + ".verifyArgEntry: ";

        // verify index does not exceed bounds
        if (parm.size() <= index) {
            throw new ParserException(functionId + "Parameter index " + index + " exceeds list size of " + parm.size());
        }
        
        // these types can be lowercase if entry is optional
        type = Character.toUpperCase(type);
        ParameterStruct.ParamType expType = CmdOptions.getParameterType(type);
        
        // verify type is correct and entry is not null
        ParameterStruct.ParamType ptype = parm.get(index).getParamType();
        boolean bVariable = parm.get(index).isVariableRef();
        if (bVariable) {
            ptype = parm.get(index).getVariableRefType();
        }
        boolean bValid = ptype == expType;
        if (! bValid) {
            frame.outputInfoMsg(STATUS_DEBUG, "Param[" + index + "] type " + ptype + " when expected: " + expType);
        }

        // check for other alternate types that are allowed and if entry is not null
        String value;
        switch (expType) {
            case Integer:
//                if (ptype == ParameterStruct.ParamType.Unsigned)
//                    bValid = true;
                Long iValue = parm.get(index).getIntegerValue();
                if (iValue == null) {
                    throw new ParserException(functionId + "Param[" + index + "] type " + ptype + ": entry was null");
                }
                value = iValue.toString();
                bValid = true;
                break;
            case Unsigned:
//                if (ptype == ParameterStruct.ParamType.Integer)
//                    bValid = true;
                iValue = parm.get(index).getIntegerValue();
                if (iValue == null) {
                    throw new ParserException(functionId + "Param[" + index + "] type " + ptype + ": entry was null");
                }
                // make sure bounds aren't exceeded for unsigned
                if (! ParameterStruct.isUnsignedInt(iValue)) {
                    throw new ParserException(functionId + "Parameter value exceeds bounds for Unsigned: " + iValue);
                }
                value = iValue.toString();
                bValid = true;
                break;
            case Boolean:
                Boolean bValue = parm.get(index).getBooleanValue();
                if (bValue == null) {
                    throw new ParserException(functionId + "Param[" + index + "] type " + ptype + ": entry was null");
                }
                value = bValue.toString();
                bValid = true;
                break;
            case String:
                value = parm.get(index).getStringValue();
                if (value == null) {
                    throw new ParserException(functionId + "Param[" + index + "] type " + ptype + ": entry was null");
                }
                bValid = true;
                break;
            case IntArray:
                ArrayList<Long> iArray = parm.get(index).getIntArray();
                switch (ptype) {
                    case Integer:
                    case Unsigned:
                        Long entry = parm.get(index).getIntegerValue();
                        iArray = new ArrayList<>();
                        iArray.add(entry);
                        parm.get(index).setIntArray(iArray);
                        frame.outputInfoMsg(STATUS_DEBUG, "Param[" + index + "] type " + ptype + ": converted from " + expType);
                        break;
                    default:
                        break;
                }
                if (iArray == null) {
                    throw new ParserException(functionId + "Param[" + index + "] type " + ptype + ": entry was null");
                }
                value = "(size " + iArray.size() + ")";
                bValid = true;
                break;
            case StringArray:
                // we will allow IntArray as well and just copy the data into the StrArray as strings
                ArrayList<String> sArray = parm.get(index).getStrArray();
                iArray = parm.get(index).getIntArray();
                switch (ptype) {
                    case String:
                        String entry = parm.get(index).getStringValue();
                        sArray = new ArrayList<>();
                        sArray.add(entry);
                        parm.get(index).setStrArray(sArray);
                        frame.outputInfoMsg(STATUS_DEBUG, "Param[" + index + "] type " + ptype + ": converted from " + expType);
                        break;
                    case IntArray:
                        if (iArray != null) {
                            sArray = new ArrayList<>();
                            for (int ix = 0; ix < iArray.size(); ix++) {
                                sArray.add(iArray.get(ix).toString());
                            }
                            parm.get(index).setStrArray(sArray);
                            frame.outputInfoMsg(STATUS_DEBUG, "Param[" + index + "] type " + ptype + ": converted from " + expType);
                        }
                        break;
                    default:
                        break;
                }
                if (sArray == null) {
                    throw new ParserException(functionId + "Param[" + index + "] type " + ptype + ": entry was null");
                }
                value = "(size " + sArray.size() + ")";
                bValid = true;
                break;
            default:
                throw new ParserException(functionId + "Param[" + index + "] has invalid type: " + expType);
        }
        if (! bValid) {
            throw new ParserException(functionId + "Param[" + index + "] expected type " + expType + ", was type: " + ptype);
        }
        frame.outputInfoMsg(STATUS_DEBUG, "Param[" + index + "] type " + ptype + ": " + value);
        return parm.get(index);
    }
    
    /**
     * updates the other parameters types (conversion) in the structure where possible.
     * If the value was a String type, it will reclassify the data type to I, U or B
     * if the value happens to be an Integer, Unsigned or Boolean.
     * 
     * @throws ParserException 
     */
    public void updateConversions () throws ParserException {
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
                if (intArrayParam.isEmpty()) {
                    strParam = "";
                    longParam = 0L;
                    boolParam = true;
                } else {
                    strParam = intArrayParam.getFirst().toString();
                    longParam = intArrayParam.getFirst();
                    boolParam = false;
                }
                frame.outputInfoMsg(STATUS_DEBUG, "    - Converted " + paramType + " value: " + strParam);
                break;
            case ParamType.StringArray:  // String List type
                if (strArrayParam.isEmpty()) {
                    strParam = "";
                    boolParam = true;
                } else {
                    strParam = strArrayParam.getFirst();
                    boolParam = false;
                }
                frame.outputInfoMsg(STATUS_DEBUG, "    - Converted " + paramType + " value: " + strParam);
                break;
            default:
                break;
        }
    }
    
}
