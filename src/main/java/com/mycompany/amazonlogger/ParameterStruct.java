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
    
    static final int NAME_MAXLEN = 20;  // the max # chars in a param name

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
        return paramClass == ParamClass.Reference && (variableRef != null) && (variableRef.getName() != null);
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
     * checks if a Variable name is valid.
     *   - name must begin with an alpha character
     *   - name must be only alphanumeric or '_' chars,
     *   - cannot be a reserved Variable name (RESPONSE, STATUS, ...)
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
            if (Variables.isReservedName(name)) {
                return true;
            }

            // make sure it is not a command name
            if (CommandStruct.isValidCommand(name) != null) {
                throw new ParserException(functionId + "using Reserved command name: " + name);
            }

            if (LoopParam.isLoopParamDefined(name)) {
                throw new ParserException(functionId + "using Loop Variable name: " + name);
            }

            // see if its already defined
            return Variables.isVariableDefined(name) != null;
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId + name);
        }
    }

    /**
     * checks if a Loop Variable name is valid.
     * 
     * @param name - the name to check
     *               name must be only alphanumeric or '_' chars,
     *               cannot be a reserved name (RESPONSE, STATUS, ...)
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

            // check if it is a reserved param name
            if (Variables.isReservedName(name)) {
                throw new ParserException(functionId + "using Reserved Variable name: " + name);
            }

            // make sure it is not a command name
            if (CommandStruct.isValidCommand(name) != null) {
                throw new ParserException(functionId + "using Reserved command name: " + name);
            }

            // make sure its not the same as a reference Variable
            ParamType type = Variables.isVariableDefined(name);
            if (type != null) {
                throw new ParserException(": using " + type.toString() + " Variable name: " + name);
            }

            // now check if this loop name is nested in a loop having same name
            // get the list of loops using this Variable name (if any)
            Integer loopIx = LoopParam.checkLoopNesting(name);
            if (loopIx != null) {
                throw new ParserException(functionId + ": Loop param " + name + " @ " + index + " is nested in same name at " + loopIx);
            }
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
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
        ParamType type = Variables.getVariableTypeFromName (name);
        
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
