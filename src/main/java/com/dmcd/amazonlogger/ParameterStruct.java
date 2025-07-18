/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import com.dmcd.amazonlogger.GUILogPanel.MsgType;
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

    private static final String CLASS_NAME = ParameterStruct.class.getSimpleName();
    
    private String              strParam;       // value for the String  param type
    private Long                longParam;      // value for the Integer and Unsigned param types
    private Boolean             boolParam;      // value for the Boolean param type
    private ArrayList<Long>     intArrayParam;  // value for Integer Array param type
    private ArrayList<String>   strArrayParam;  // value for String  List  param type
    private ArrayList<CalcEntry> calcParam;     // value for Calculation param type
    
    private ParamClass          paramClass;     // class of the parameter
    private ParamType           paramType;      // parameter classification
    private VarExtensions       variableRef;    // info if a referenced Variable is used instead of a value

    
    public enum ParamClass {
        Discrete,       // a Hard-coded value
        Reference,      // a Variable reference
        Calculation,    // a Calculation formula
    }

    public enum ParamType {
        Integer,
        Unsigned,
        Boolean,
        String,
        IntArray,
        StrArray,
    }

    public ParameterStruct() {
        strParam = null;
        longParam = null;
        boolParam = null;
        intArrayParam = null;
        strArrayParam = null;
        calcParam = null;
        paramClass = null;
        variableRef = new VarExtensions();
        paramType = null;
    }

    /**
     * Creates a Discrete String parameter.
     * 
     * @param strValue   - the parameter value to use
     */
    public ParameterStruct (String strValue) {
        strParam = strValue;
        longParam = null;
        boolParam = null;
        intArrayParam = null;
        strArrayParam = null;
        calcParam = null;
        paramClass = ParamClass.Discrete;
        variableRef = new VarExtensions();
        paramType = ParamType.String;
    }

    /**
     * Creates a Discrete Integer parameter.
     * 
     * @param iValue   - the parameter value to use
     */
    public ParameterStruct (Long iValue) {
        strParam = iValue.toString();
        longParam = iValue;
        boolParam = null;
        intArrayParam = null;
        strArrayParam = null;
        calcParam = null;
        paramClass = ParamClass.Discrete;
        variableRef = new VarExtensions();
        paramType = ParamType.Integer;
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
        
        variableRef = new VarExtensions();
        calcParam = null;
        strParam = strValue;
        paramClass = pClass;
        paramType = dataType;

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
                        VarExtract paramInfo = new VarExtract(paramName);
                        variableRef = new VarExtensions(paramInfo);
                        paramClass = ParamClass.Reference;
                        GUILogPanel.outputInfoMsg(MsgType.DEBUG, msgGap + "Converted Calculation parameter to single Reference value: " + variableRef.getName());
                    } else {
                        Long value = calc.getCalcValue();
                        if (value != null) {
                            longParam = value;
                            paramClass = ParamClass.Discrete;
                            GUILogPanel.outputInfoMsg(MsgType.DEBUG, msgGap + "Converted Calculation parameter to single " + paramType + " value: " + value);
                        }
                    }
                } else {
                    GUILogPanel.outputInfoMsg(MsgType.DEBUG, msgGap + "New ParamStruct: Calculation " + paramType + ": " + strValue);
                }
                return;
            }
            if (pClass == ParamClass.Reference) {
                // VARIABLE REFERENCE ENTRY:
                // extract any extension added to the Variable
                VarExtract paramInfo = new VarExtract(strParam);
                variableRef = new VarExtensions(paramInfo);
                GUILogPanel.outputInfoMsg(MsgType.DEBUG, msgGap + "New ParamStruct: Reference " + paramType + " name: " + variableRef.getName());
                return;
            }

            // HARD-CODED ENTRIES:
            String invalidMsg = "Invalid value for '" + dataType + "' type param: " + strValue;
            switch (dataType) {
                case Boolean:
                    if (!strParam.equalsIgnoreCase("TRUE") &&
                        !strParam.equalsIgnoreCase("FALSE") &&
                        !strParam.contentEquals("0") &&
                        !strParam.contentEquals("1") ) {
                        throw new ParserException(functionId + invalidMsg);
                    }
                    boolParam = strParam.equalsIgnoreCase("TRUE") || strParam.contentEquals("1");
                    break;
                case Unsigned:
                    try {
                        longParam = Utils.getLongOrUnsignedValue(strParam);
                    } catch (ParserException ex) {
                        throw new ParserException(functionId + invalidMsg);
                    }
                    if (! isUnsignedInt(longParam)) {
                        throw new ParserException(functionId + invalidMsg);
                    }
                    break;
                case Integer:
                    try {
                        longParam = Utils.getLongOrUnsignedValue(strParam);
                    } catch (ParserException ex) {
                        throw new ParserException(functionId + invalidMsg);
                    }
                    break;
                case IntArray:
                case StrArray:
                    // first, remove the braces if they are included
                    if (strParam.charAt(0) == '{' && strParam.charAt(strParam.length()-1) == '}') {
                        strParam = strParam.substring(1, strParam.length()-1).strip();
                    }
                    // transfer the array entries to the String Array param
                    strArrayParam = new ArrayList<>(Arrays.asList(strParam.split(",")));
                    intArrayParam = new ArrayList<>();
                    boolean bAllInts = true;
                    for (int ix = 0; ix < strArrayParam.size(); ix++) {
                        try {
                            // now check if all entries were Integer, even if it was String Array
                            String cleanStr = strArrayParam.get(ix).strip();
                            strArrayParam.set(ix, cleanStr); // remove leading & trailing spaces
                            longParam = Utils.getLongOrUnsignedValue(cleanStr);
                            intArrayParam.add(longParam);
                        } catch (ParserException ex) {
                            bAllInts = false;
                            if (paramType == ParamType.IntArray) {
                                throw new ParserException(functionId + invalidMsg);
                            }
                        }
                    }
                    // if it was a String Array but was all Integers, reclassify it
                    if (paramType == ParamType.StrArray && bAllInts) {
                        paramType = ParamType.IntArray;
                        GUILogPanel.outputInfoMsg(MsgType.DEBUG, msgGap + "Reclassified StrArray as IntArray: " + variableRef.getName());
                    }
                    if (paramType == ParamType.IntArray) {
                        GUILogPanel.outputInfoMsg(MsgType.DEBUG, msgGap + "new " + paramType + " size " + intArrayParam.size());
                    } else {
                        GUILogPanel.outputInfoMsg(MsgType.DEBUG, msgGap + "new " + paramType + " size " + strArrayParam.size());
                    }
                    break;
                case String:
                    // the data has already been added to the String entry, so we are done
                    break;
                default:
                    break;
            }
            GUILogPanel.outputInfoMsg(MsgType.DEBUG, msgGap + "New ParamStruct: Discreet " + paramType + ": '" + strParam + "'");
        } catch (ParserException exMsg) {
            Utils.throwAddendum (exMsg.getMessage(), functionId);
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
    
    public void setVariableRef (VarExtensions value) {
        variableRef = value;
        paramClass  = ParamClass.Reference;
    }
    
    public void setParamType (ParamType value) {
        paramType = value;
    }

    public void setParamTypeDiscrete (ParamType ptype) {
        paramType   = ptype;
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

    public ParamClass getParamClass () {
        return paramClass;
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
    public VarExtensions getVariableRef () {
        return variableRef;
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
     * determines if the parameter type is valid
     * 
     * @param pname - the string name of the parameter type
     * 
     * @return the ParamType if value, null if not
     */
    public static ParamType checkParamType (String pname) {
        for (ParamType entry : ParamType.values()) {
            if (entry.toString().contentEquals(pname)) {
                return entry;
            }
        }
        return null;
    }
    
    /**
     * returns the Calculation data value
     * 
     * @param type - the data type being calculated
     * 
     * @return the Integer value of the calculation
     * 
     * @throws com.dmcd.amazonlogger.ParserException
     */
    public Long getCalculationValue (ParamType type) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (calcParam == null) {
            throw new ParserException(functionId + "Calculation value is null");
        }
        try {
            Calculation calc = new Calculation(calcParam);
            longParam = calc.compute(type);
        } catch (ParserException exMsg) {
            Utils.throwAddendum (exMsg.getMessage(), functionId);
        }
        return longParam;
    }
        
    /**
     * checks if the parameter is a Variable reference and, if so, reads the value into the structure.
     * It also marks the type of data placed and converts values to other types where possible.
     * 
     * @throws ParserException 
     */
    public void updateFromReference () throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        ParameterStruct value = null;
        try {
            value = Variables.getVariableInfo (variableRef);
        } catch (ParserException exMsg) {
            Utils.throwAddendum (exMsg.getMessage(), functionId);
        }
        if (value != null) {
            this.paramType      = value.paramType;
            this.longParam      = value.longParam;
            this.boolParam      = value.boolParam;
            this.strParam       = value.strParam;
            this.intArrayParam  = value.intArrayParam;
            this.strArrayParam  = value.strArrayParam;
            this.calcParam      = value.calcParam;
            this.paramClass     = value.paramClass;
            this.variableRef    = value.variableRef;
            
            GUILogPanel.outputInfoMsg(MsgType.DEBUG, "    unpacked param " + variableRef.getName() + " as type '" + paramType);
        }
    }

    /**
     * returns a String for displaying the current param data type and value.
     * 
     * @param ix - the index of the parameter
     * 
     * @return a String indicating the parameter type and value
     */
    public String showParam (int ix) {
        String strValue;
        String strID = paramType.toString();
        switch (paramClass) {
            case ParamClass.Reference:
                strValue = variableRef.getName();
                strID += "(ref)";
                break;
            case ParamClass.Calculation:
                strValue = strParam;
                strID += "(calc)";
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
                    case ParamType.StrArray -> {
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
            
        return "arg[" + ix + "]: " + strID + ", value: " + strValue;
    }

    /**
     * displays a String of the data types for all the parameters in a command.
     * 
     * @param params - the array of parameters for a command
     */
    public static void showParamTypeList (ArrayList<ParameterStruct> params) {
        if (params != null && ! params.isEmpty()) {
            String paramTypes = "";
            for (int ix = 0; ix < params.size(); ix++) {
                String strID = params.get(ix).paramType.toString();
                paramTypes += " " + strID;
            }
            GUILogPanel.outputInfoMsg(MsgType.DEBUG, "     arg dataTypes: " + paramTypes);
        } else {
            GUILogPanel.outputInfoMsg(MsgType.DEBUG, "     no args");
        }
    }
    
    /**
     * determines the type of data in a String value.
     * 
     * Only called in Compile stage, so we don't know the contents of the parameters yet.
     * This only works on right side parameters because we need to see that
     *   it starts with a '$' to indicate a parameter, but this is only true
     *   for parameters when they are not on the left-side of an assignment.
     * 
     * @param strValue - the String value to check
     * 
     * @return the data type found
     * 
     * @throws ParserException
     */
    public static ParamType classifyDataType (String strValue) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (strValue == null) {
            throw new ParserException(functionId + "Null input value");
        }
        
        // first check if it is a Variable
        strValue = strValue.strip();
        ParamType varType;
        if (strValue.startsWith("$")) {
            // it's a parameter - determine its data type
            varType = Variables.getVariableTypeFromName (strValue);
            int iLBracket = strValue.indexOf('[');
            int iRBracket = strValue.indexOf(']');
            int iRange = strValue.indexOf('-');
            if (iRange < 0 && iLBracket > 0 && iRBracket > iLBracket) {
                switch (varType) {
                    case StrArray:
                        varType = ParamType.String;
                        break;
                    case IntArray:
                        varType = ParamType.Integer;
                        break;
                    default:
                        break;
                }
            }
        }
        else {
            // not a parameter, so it must be a discreet value
            if (strValue.equalsIgnoreCase("TRUE") ||
                strValue.equalsIgnoreCase("FALSE")) {
                varType = ParamType.Boolean;
            } else {
                try {
                    Long longVal = Utils.getLongOrUnsignedValue (strValue);
                    varType = isUnsignedInt(longVal) ? ParamType.Unsigned : ParamType.Integer;
                } catch (ParserException ex) {
                    int offset1 = strValue.indexOf('{');
                    int offset2 = strValue.indexOf('}');
                    if (offset1 == 0 && offset2 == strValue.length() - 1) {
                        if (isDiscreteIntArray(strValue)) {
                            varType = ParamType.IntArray;
                        } else {
                            varType = ParamType.StrArray;
                        }
                    } else {
                        varType = ParamType.String;
                    }
                }
            }
        }
        
        return varType;
    }

    /**
     * checks to see if the entry is an IntArray of integer values.
     * 
     * @param entry - the string representing the parameter value
     * 
     * @return true if the entry qualifies as an integer array
     */
    private static boolean isDiscreteIntArray (String entry) {
        if (entry.charAt(0) != '{' || entry.charAt(entry.length()-1) != '}')
            return false;
        for (int ix = 1; ix < entry.length() - 1; ix++) {
            char ch = entry.charAt(ix);
            if (!Character.isDigit(ch) && ch != '-' && ch != ' ' && ch != ',') {
                return false;
            }
        }
        return true;
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
     * verifies the parameter has valid content for the expected type & returns it.
     * 
     * This is to be used during Execution stage only!
     * 
     * @param parm    - the parameter to check
     * @param expType - the expected data type
     * 
     * @return unsigned value
     * 
     * @throws ParserException if not valid Unsigned value
     *
     */
    public static ParameterStruct verifyArgEntry (ParameterStruct parm, ParamType expType) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // verify type is correct and entry is not null
        ParameterStruct.ParamType ptype = parm.getParamType();
        boolean bValid = ptype == expType;
        if (! bValid) {
            GUILogPanel.outputInfoMsg(MsgType.DEBUG, "    Param type " + ptype + " when expected: " + expType);
        }

        // check for other alternate types that are allowed and if entry is not null
        String value;
        switch (expType) {
            case Integer:
                Long iValue = parm.getIntegerValue();
                if (iValue == null) {
                    throw new ParserException(functionId + "Param type " + ptype + ": entry was null");
                }
                value = iValue.toString();
                bValid = true;
                break;
            case Unsigned:
                iValue = parm.getIntegerValue();
                if (iValue == null) {
                    throw new ParserException(functionId + "Param type " + ptype + ": entry was null");
                }
                // make sure bounds aren't exceeded for unsigned
                if (! ParameterStruct.isUnsignedInt(iValue)) {
                    throw new ParserException(functionId + "Parameter value exceeds bounds for Unsigned: " + iValue);
                }
                value = iValue.toString();
                bValid = true;
                break;
            case Boolean:
                Boolean bValue = parm.getBooleanValue();
                if (bValue == null) {
                    throw new ParserException(functionId + "Param type " + ptype + ": entry was null");
                }
                value = bValue.toString();
                bValid = true;
                break;
            case String:
                value = parm.getStringValue();
                if (value == null) {
                    throw new ParserException(functionId + "Param type " + ptype + ": entry was null");
                }
                bValid = true;
                break;
            case IntArray:
                ArrayList<Long> iArray = parm.getIntArray();
                switch (ptype) {
                    case Integer:
                    case Unsigned:
                        Long entry = parm.getIntegerValue();
                        iArray = new ArrayList<>();
                        iArray.add(entry);
                        parm.setIntArray(iArray);
                        GUILogPanel.outputInfoMsg(MsgType.DEBUG, "Param type " + ptype + ": converted from " + expType);
                        break;
                    default:
                        break;
                }
                if (iArray == null) {
                    throw new ParserException(functionId + "Param type " + ptype + ": entry was null");
                }
                value = "(size " + iArray.size() + ")";
                bValid = true;
                break;
            case StrArray:
                // we will allow IntArray as well and just copy the data into the StrArray as strings
                ArrayList<String> sArray = parm.getStrArray();
                iArray = parm.getIntArray();
                switch (ptype) {
                    case String:
                        String entry = parm.getStringValue();
                        sArray = new ArrayList<>();
                        sArray.add(entry);
                        parm.setStrArray(sArray);
                        GUILogPanel.outputInfoMsg(MsgType.DEBUG, "Param type " + ptype + ": converted from " + expType);
                        break;
                    case IntArray:
                        if (iArray != null) {
                            sArray = new ArrayList<>();
                            for (int ix = 0; ix < iArray.size(); ix++) {
                                sArray.add(iArray.get(ix).toString());
                            }
                            parm.setStrArray(sArray);
                            GUILogPanel.outputInfoMsg(MsgType.DEBUG, "Param type " + ptype + ": converted from " + expType);
                        }
                        break;
                    default:
                        break;
                }
                if (sArray == null) {
                    throw new ParserException(functionId + "Param type " + ptype + ": entry was null");
                }
                value = "(size " + sArray.size() + ")";
                bValid = true;
                break;
            default:
                throw new ParserException(functionId + "Param has invalid type: " + expType);
        }
        if (! bValid) {
            throw new ParserException(functionId + "Param expected type " + expType + ", was type: " + ptype);
        }
        GUILogPanel.outputInfoMsg(MsgType.DEBUG, "Param type " + ptype + ": " + value);
        return parm;
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
            case Integer:
                strParam = longParam.toString();
                if (longParam == 0L || longParam == 1L) {
                    boolParam = longParam != 0L;
                }
                GUILogPanel.outputInfoMsg(MsgType.DEBUG, "    - Converted " + paramType + " value: " + strParam);
                break;
            case Unsigned:
                strParam = longParam.toString();
                if (longParam == 0L || longParam == 1L) {
                    boolParam = longParam != 0L;
                }
                GUILogPanel.outputInfoMsg(MsgType.DEBUG, "    - Converted " + paramType + " value: " + strParam);
                break;
            case Boolean:
                strParam = boolParam.toString();
                longParam = (boolParam) ? 1L : 0L;
                GUILogPanel.outputInfoMsg(MsgType.DEBUG, "    - Converted " + paramType + " value: " + strParam);
                break;
            case String:
                if (!strParam.isBlank()) {
                    if (strParam.equalsIgnoreCase("TRUE")) {
                        boolParam = true;
                        paramType = ParamType.Boolean;
                    } else if (strParam.equalsIgnoreCase("FALSE")) {
                        boolParam = false;
                        paramType = ParamType.Boolean;
                    } else {
                        try {
                            Long longVal = Utils.getLongOrUnsignedValue (strParam);
                            longParam = longVal;
                            if (isUnsignedInt(longVal)) {
                                paramType = ParamType.Unsigned;
                            } else {
                                paramType = ParamType.Integer;
                            }
                            if (longParam == 0L || longParam == 1L) {
                                boolParam = longParam != 0L;
                            }
                        } catch (ParserException ex) {
                            // keep param as String type and we can't do any conversions
                        }
                    }
                    GUILogPanel.outputInfoMsg(MsgType.DEBUG, "    - Converted " + paramType + " value: " + strParam);
                }
                break;
            case IntArray:
                if (intArrayParam.isEmpty()) {
                    longParam = 0L;
                    strParam = "";
                } else {
                    longParam = intArrayParam.getFirst();
                    strParam = longParam.toString();
                }
                GUILogPanel.outputInfoMsg(MsgType.DEBUG, "    - Converted " + paramType + " value: " + strParam);
                break;
            case StrArray:  // String List type
                if (strArrayParam.isEmpty()) {
                    strParam = "";
                } else {
                    strParam = strArrayParam.getFirst();
                }
                GUILogPanel.outputInfoMsg(MsgType.DEBUG, "    - Converted " + paramType + " value: " + strParam);
                break;
            default:
                break;
        }
    }
    
}
