/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_VARS;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

/**
 *
 * @author dan
 */
public class Variables {
    
    private static final String CLASS_NAME = Variables.class.getSimpleName();
    
    static final int NAME_MAXLEN = 20;  // the max # chars in a param name

    // user-defined static Variables
    private static final HashMap<String, String>  strParams  = new HashMap<>();
    private static final HashMap<String, Long>    longParams = new HashMap<>();
    private static final HashMap<String, Long>    uintParams = new HashMap<>();
    private static final HashMap<String, Boolean> boolParams = new HashMap<>();

    // reserved static Variables
    private static boolean            bStatus = false;          // true/false status indications
    private static long               maxRandom = 1000000000;   // for random values 0 - 999999999

    public enum ReservedVars {
        RESPONSE,
        STATUS,
        RANDOM,
        DATE,
        TIME,
    }
    
    public enum VarClass {
        UNKNOWN,            // not a valid variable
        STRING,             // a String   variable
        NUMERIC,            // a Numeric  variable (Integer or Unsigned)
        BOOLEAN,            // a Boolean  variable
        ARRAY,              // an Array   variable (String or Integer)
        LOOP,               // a Loop     variable
        RESERVED,           // a Reserved variable
    }

    public enum VarCheck {
        DEFINE,         // defining a Variable or loop parameter
        SET,            // setting a standard Variable value (left side of =)
        REFERENCE,      // referencing a Variable (standard, reserved or loop)
    }
    
    /**
     * initializes the saved Variables
     */
    public static void initVariables () {
        strParams.clear();
        longParams.clear();
        uintParams.clear();
        boolParams.clear();
        bStatus = false;
        VarArray.initVariables();
        LoopParam.initVariables();
    }

    /**
     * set the value of the $STATUS Variable
     * 
     * @param value - value to set the result Variable to
     */
    public static void putStatusValue (boolean value) {
        bStatus = value;
    }

    /**
     * creates a new entry in the Variable table and sets the initial value.
     * 
     * @param name  - Variable name
     * 
     * @throws ParserException - if Variable was already defined
     */
    public static void allocateVariable (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // first, verify Variable name to make sure it is valid format and
        //  not already used.
        boolean bValid;
        try {
            bValid = isValidVariableName(VarCheck.DEFINE, name);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        if (! bValid) {
            throw new ParserException(functionId + "Variable " + name + " already defined");
        }

        ParameterStruct.ParamType type = getVariableTypeFromName(name);
        switch (type) {
            case Integer:
                longParams.put(name, 0L);
                break;
            case Unsigned:
                uintParams.put(name, 0L);
                break;
            case Boolean:
                boolParams.put(name, false);
                break;
            case IntArray:
            case StringArray:
                VarArray.allocateVariable(name, type);
                break;
            case String:
                strParams.put(name, "");        // default value to empty String
                break;
            default:
                break;
        }
        frame.outputInfoMsg(STATUS_VARS, "   - Allocated " + type.toString() + " variable: " + name);
    }

    /**
     * gets the next random value
     * 
     * @return the next random value
     */
    private static Long getRandomValue () {
        Random rand = new Random();
        return rand.nextLong(maxRandom);
    }
    
    /**
     * gets the date value as a String
     * 
     * @param trait - the trait of the DATE variable (null if use the raw date format)
     * 
     * @return the selected date format
     */
    private static String getDateStringValue (VariableExtract.Trait trait) {
        String response;
        LocalDate currentDate = LocalDate.now();
        if (trait == null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
            response = currentDate.format(formatter);
        } else {
            // these traits are only valid for DATE
            switch (trait) {
                default:
                case DOW:
                    DayOfWeek dow = currentDate.getDayOfWeek();
                    response = "" + dow.getValue();
                    break;
                case DOM:
                    int ivalue = currentDate.getDayOfMonth();
                    response = "" +  ivalue;
                    break;
                case DOY:
                    ivalue = currentDate.getDayOfYear();
                    response = "" + ivalue;
                    break;
                case MOY:
                    ivalue = currentDate.getMonthValue();
                    response = "" + ivalue;
                    break;
                case DAY:
                    response = currentDate.getDayOfWeek().toString();
                    break;
                case MONTH:
                    response = currentDate.getMonth().toString();
                    break;
            }
        }
        return response;
    }
    
    /**
     * gets the date value as an Integer
     * 
     * @param trait - the trait of the DATE variable
     * 
     * @return the selected date format (null if not an Integer type)
     */
    private static Integer getDateIntValue (VariableExtract.Trait trait) throws ParserException {
        Integer response = null;
        if (trait != null) {
            // these traits are only valid for DATE
            LocalDate currentDate = LocalDate.now();
            switch (trait) {
                default:
                case VariableExtract.Trait.DOW:
                    DayOfWeek dow = currentDate.getDayOfWeek();
                    response = dow.getValue();
                    break;
                case VariableExtract.Trait.DOM:
                    response = currentDate.getDayOfMonth();
                    break;
                case VariableExtract.Trait.DOY:
                    response = currentDate.getDayOfYear();
                    break;
                case VariableExtract.Trait.MOY:
                    response = currentDate.getMonthValue();
                    break;
                case VariableExtract.Trait.DAY:
                case VariableExtract.Trait.MONTH:
                    // invalid for Integer
                    break;
            }
        }
        return response;
    }
    
    /**
     * returns the value of a reference Variable along with its data type.
     * 
     * This is only performed during the Execution stage when evaluating the value
     *  of the reference variables just prior to executing each command.
     * It is only at this point where we can do the run-time evaluation, just
     *  prior to executing the command.
     * 
     * @param paramInfo  - Variable reference information
     * 
     * @return the Variable value
     * 
     * @throws ParserException - if Variable not found
     */
    public static ParameterStruct getVariableInfo (VariableInfo paramInfo) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (paramInfo == null || paramInfo.getName() == null || paramInfo.getType() == null) {
            return null;
        }
        
        // create a new parameter with all null entries
        ParameterStruct paramValue = new ParameterStruct();
        paramValue.setVariableRef(new VariableInfo (paramInfo));

        String name = paramInfo.getName();
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }
        
        ParameterStruct.ParamType pType = paramInfo.getType();
        ParameterStruct.ParamType findType;
        Long varValue;
        switch (getVariableClass (name)) {
            // check the reserved params list
            case RESERVED:
                switch (name) {
                    case "RESPONSE":
                        paramValue.setStrArray(VarArray.getResponseValue());
                        pType = ParameterStruct.ParamType.StringArray;
                        break;
                    case "STATUS":
                        paramValue.setBooleanValue(bStatus);
                        pType = ParameterStruct.ParamType.Boolean;
                        break;
                    case "RANDOM":
                        paramValue.setIntegerValue(getRandomValue());
                        pType = ParameterStruct.ParamType.Integer;
                        break;
                    case "TIME":
                        LocalTime currentTime = LocalTime.now();
                        paramValue.setStringValue(currentTime.toString().substring(0,12));
                        pType = ParameterStruct.ParamType.String;
                        break;
                    case "DATE":
                        Integer iDate = getDateIntValue (paramInfo.getTrait());
                        if (iDate != null) {
                            paramValue.setIntegerValue((long) iDate);
                            pType = ParameterStruct.ParamType.Unsigned;
                        } else {
                            String strDate = getDateStringValue (paramInfo.getTrait());
                            paramValue.setStringValue(strDate);
                            pType = ParameterStruct.ParamType.String;
                        }
                        break;
                    default:
                        break;
                }
                break;
            // next check for loop variables for name match
            case LOOP:
                varValue = LoopParam.getLoopCurValue(name);
                pType = ParameterStruct.ParamType.Integer;
                paramValue.setIntegerValue(varValue);
                frame.outputInfoMsg(STATUS_VARS, "    - Lookup Ref '" + name + "' as type " + pType + ": " + varValue);
                break;
            // otherwise, let's check for standard variables for name match
            case NUMERIC:
                if (longParams.containsKey(name)) {
                    varValue = longParams.get(name);
                    pType = ParameterStruct.ParamType.Integer;
                } else {
                    varValue = uintParams.get(name);
                    pType = ParameterStruct.ParamType.Unsigned;
                }
                paramValue.setIntegerValue(varValue);
                frame.outputInfoMsg(STATUS_VARS, "    - Lookup Ref '" + name + "' as type " + pType + ": " + varValue);
                break;
            case BOOLEAN:
                paramValue.setBooleanValue(boolParams.get(name));
                pType = ParameterStruct.ParamType.Boolean;
                frame.outputInfoMsg(STATUS_VARS, "    - Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.getBooleanValue());
                break;
            case STRING:
                paramValue.setStringValue(strParams.get(name));
                pType = ParameterStruct.ParamType.String;
                frame.outputInfoMsg(STATUS_VARS, "    - Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.getStringValue());
                break;
            case ARRAY:
                String arrayValue;
                ArrayList<Long> intList = VarArray.getIntArray(name);
                if (intList != null) {
                    paramValue.setIntArray(intList);
                    arrayValue = intList.toString();
                    if (arrayValue.length() > 100) {
                        int length = arrayValue.length();
                        arrayValue = arrayValue.substring(0,60) + "..." + arrayValue.substring(length-30);
                    }
                } else {
                    ArrayList<String> strList = VarArray.getStrArray(name);
                    paramValue.setStrArray(strList);
                    arrayValue = strList.toString();
                    if (arrayValue.length() > 100) {
                        int length = arrayValue.length();
                        arrayValue = arrayValue.substring(0,60) + "..." + arrayValue.substring(length-30);
                    }
                }
                frame.outputInfoMsg(STATUS_VARS, "    - Lookup Ref '" + name + "' as type " + pType + ": " + arrayValue);
                break;
            case UNKNOWN:
                throw new ParserException(functionId + "    - Variable Ref '" + name + "' was not found in any Variable database");
        }

        // now check for brackets being applied (this will change the type for Arrays)
        if (paramInfo.getIndexStart() != null) {
            int iStart = paramInfo.getIndexStart();
            switch (pType) {
                case ParameterStruct.ParamType.IntArray:
                    if (iStart < paramValue.getIntArraySize()) {
                        paramValue.setIntegerValue(paramValue.getIntArrayElement(iStart));
                        paramValue.setStringValue(paramValue.getIntegerValue().toString());
                        pType = ParameterStruct.ParamType.Integer;
                        frame.outputInfoMsg(STATUS_VARS, "    " + name + "[" + iStart + "] as type " + pType + ": " + paramValue.getIntegerValue());
                    } else {
                        throw new ParserException(functionId + "Parameter " + name + " index " + iStart + " exceeds array");
                    }
                    break;
                case ParameterStruct.ParamType.StringArray:
                    if (iStart < paramValue.getStrArraySize()) {
                        paramValue.setStringValue(paramValue.getStrArrayElement(iStart));
                        pType = ParameterStruct.ParamType.String;
                        frame.outputInfoMsg(STATUS_VARS, "    " + name + "[" + iStart + "] as type " + pType + ": " + paramValue.getStringValue());
                    } else {
                        throw new ParserException(functionId + "Parameter " + name + " index " + iStart + " exceeds array");
                    }
                    break;
                case ParameterStruct.ParamType.String:
                    int iEnd = iStart + 1;
                    String ixRange = "[" + iStart + "]";
                    if(paramInfo.getIndexEnd() != null) {
                        iEnd = paramInfo.getIndexEnd();
                        ixRange = "[" + iStart + "-" + iEnd + "]";
                    }
                    if (iEnd <= paramValue.getStrArraySize()) {
                        paramValue.setStringValue(paramValue.getStringValue().substring(iStart, iEnd));
                        frame.outputInfoMsg(STATUS_VARS, "    " + name + "index" + ixRange + " as type " + pType + ": " + paramValue.getStringValue());
                    } else {
                        throw new ParserException(functionId + "Parameter " + name + " index" + ixRange + " exceeds array");
                    }
                    break;
                default:
                    break;
            }
        }
        
        // now check for traits being applied (this can also change the data type returned)
        VariableExtract.Trait trait = paramInfo.getTrait();
        if (trait != null) {
            switch (pType) {
                case ParameterStruct.ParamType.IntArray:
                    int psize = paramValue.getIntArraySize();
                    switch (trait) {
                        case SIZE:
                            paramValue.setIntegerValue((long)psize);
                            paramValue.setStringValue(paramValue.getIntegerValue().toString());
                            pType = ParameterStruct.ParamType.Integer;
                            break;
                        case ISEMPTY:
                            paramValue.setBooleanValue(paramValue.isIntArrayEmpty());
                            paramValue.setStringValue(paramValue.getBooleanValue().toString());
                            pType = ParameterStruct.ParamType.Boolean;
                            break;
                        case FILTER:
                            if (VarArray.getFilterArray() == null) {
                                throw new ParserException(functionId + trait.toString() + " has not been initialized yet");
                            }
                            if (VarArray.getFilterArray().size() != psize) {
                                throw new ParserException(functionId + trait.toString() + " has size " + VarArray.getFilterArray().size() + ", but array is size " + psize);
                            }
                            // remove the selected entries
                            for (int ix = psize - 1; ix >= 0; ix--) {
                                if (!VarArray.getFilterArray().get(ix)) {
                                    paramValue.getIntArray().remove(ix);
                                }
                            }
                            break;
                        default:
                            throw new ParserException(functionId + "Invalid trait " + trait.toString() + " for data type " + pType);
                    }
                    frame.outputInfoMsg(STATUS_VARS, "    " + name + "." + trait.toString() + " as type " + pType + ": " + paramValue.getStringValue());
                    break;
                case ParameterStruct.ParamType.StringArray:
                    psize = paramValue.getStrArraySize();
                    String strValue = "";
                    switch (trait) {
                        case SORT:
                            Collections.sort(paramValue.getStrArray().subList(0, psize));
                            strValue = paramValue.getStrArray().toString();
                            if (strValue.length() > 20) strValue = strValue.substring(0, 20) + "...";
                            break;
                        case REVERSE:
                            Collections.reverse(paramValue.getStrArray());
                            strValue = paramValue.getStrArray().toString();
                            if (strValue.length() > 20) strValue = strValue.substring(0, 20) + "...";
                            break;
                        case SIZE:
                            paramValue.setIntegerValue((long)psize);
                            strValue = paramValue.getIntegerValue().toString();
                            pType = ParameterStruct.ParamType.Integer;
                            break;
                        case ISEMPTY:
                            paramValue.setBooleanValue(paramValue.isStrArrayEmpty());
                            strValue = paramValue.getBooleanValue().toString();
                            pType = ParameterStruct.ParamType.Boolean;
                            break;
                        case FILTER:
                            if (VarArray.getFilterArray() == null) {
                                throw new ParserException(functionId + trait.toString() + " has not been initialized yet");
                            }
                            if (VarArray.getFilterArray().size() != psize) {
                                throw new ParserException(functionId + trait.toString() + " has size " + VarArray.getFilterArray().size() + ", but array is size " + psize);
                            }
                            // remove the selected entries
                            for (int ix = psize - 1; ix >= 0; ix--) {
                                if (!VarArray.getFilterArray().get(ix)) {
                                    paramValue.getStrArray().remove(ix);
                                }
                            }
                            break;
                        default:
                            throw new ParserException(functionId + "Invalid trait " + trait.toString() + " for data type " + pType);
                    }
                    frame.outputInfoMsg(STATUS_VARS, "    " + name + "." + trait.toString() + " as type " + pType + ": " + strValue);
                    break;
                case ParameterStruct.ParamType.String:
                    strValue = "";
                    switch (trait) {
                        case UPPER:
                            paramValue.setStringValue(paramValue.getStringValue().toUpperCase());
                            strValue = paramValue.getStringValue();
                            break;
                        case LOWER:
                            paramValue.setStringValue(paramValue.getStringValue().toLowerCase());
                            strValue = paramValue.getStringValue();
                            break;
                        case SIZE:
                            paramValue.setIntegerValue((long)paramValue.getStringValue().length());
                            strValue = paramValue.getIntegerValue().toString();
                            pType = ParameterStruct.ParamType.Integer;
                            break;
                        case ISEMPTY:
                            paramValue.setBooleanValue(paramValue.getStringValue().isEmpty());
                            strValue = paramValue.getBooleanValue().toString();
                            pType = ParameterStruct.ParamType.Boolean;
                            break;
                        case VariableExtract.Trait.DOW:
                        case VariableExtract.Trait.DOM:
                        case VariableExtract.Trait.DOY:
                        case VariableExtract.Trait.MOY:
                        case VariableExtract.Trait.DAY:
                        case VariableExtract.Trait.MONTH:
                            // nothing to do for these - they were handled in the DATE section
                            break;
                        default:
                            throw new ParserException(functionId + "Invalid trait " + trait.toString() + " for data type " + pType.toString());
                    }
                    frame.outputInfoMsg(STATUS_VARS, "    " + name + "." + trait.toString() + " as type Boolean: " + strValue);
                    break;
                default:
                    break;
            }
        }
        
        // save the parameter type
        paramValue.setParamTypeDiscrete (pType);

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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (strParams.containsKey(name)) {
            strParams.replace(name, value);
            frame.outputInfoMsg(STATUS_VARS, "   - Modified String param: " + name + " = " + value);
        } else {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (longParams.containsKey(name)) {
            longParams.replace(name, value);
            frame.outputInfoMsg(STATUS_VARS, "   - Modified Integer param: " + name + " = " + value);
        } else {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (! ParameterStruct.isUnsignedInt(value)) {
            throw new ParserException(functionId + "value for Variable " + name + " exceeds limits for Unsigned: " + value);
        }
        if (name.contentEquals("RANDOM")) {
            maxRandom = value;  // this will set the max range for random value (0 to maxRandom - 1)
        } else if (uintParams.containsKey(name)) {
            uintParams.replace(name, value);
            frame.outputInfoMsg(STATUS_VARS, "   - Modified Unsigned param: " + name + " = " + value);
        } else {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (boolParams.containsKey(name)) {
            boolParams.replace(name, value);
            frame.outputInfoMsg(STATUS_VARS, "   - Modified Boolean param: " + name + " = " + value);
        } else {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
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
            frame.outputInfoMsg(STATUS_VARS, "   - Deleted Integer Variable: " + name);
        }
        if (uintParams.containsKey(name)) {
            uintParams.remove(name);
            frame.outputInfoMsg(STATUS_VARS, "   - Deleted Unsigned Variable: " + name);
        }
        if (strParams.containsKey(name)) {
            strParams.remove(name);
            frame.outputInfoMsg(STATUS_VARS, "   - Deleted String Variable: " + name);
        }
        if (boolParams.containsKey(name)) {
            boolParams.remove(name);
            frame.outputInfoMsg(STATUS_VARS, "   - Deleted Boolean Variable: " + name);
        }
        if (VarArray.isIntArray(name)) {
            VarArray.removeArrayEntry(name, ParameterStruct.ParamType.IntArray);
            frame.outputInfoMsg(STATUS_VARS, "   - Deleted IntArray Variable: " + name);
        }
        if (VarArray.isStrArray(name)) {
            VarArray.removeArrayEntry(name, ParameterStruct.ParamType.StringArray);
            frame.outputInfoMsg(STATUS_VARS, "   - Deleted StrArray Variable: " + name);
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
    public static ParameterStruct.ParamType getVariableTypeFromName (String name) {
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }
        if (name.length() > 1 && name.charAt(1) == '_') {
            switch (name.charAt(0)) {
                case 'I': return ParameterStruct.ParamType.Integer;
                case 'U': return ParameterStruct.ParamType.Unsigned;
                case 'B': return ParameterStruct.ParamType.Boolean;
                case 'A': return ParameterStruct.ParamType.IntArray;
                case 'L': return ParameterStruct.ParamType.StringArray;
                default:  return ParameterStruct.ParamType.String;
            }
        } else if (name.contentEquals("RESPONSE")) {
            return ParameterStruct.ParamType.StringArray;
        } else if (name.contentEquals("STATUS")) {
            return ParameterStruct.ParamType.Integer;
        } else if (name.contentEquals("RANDOM")) {
            return ParameterStruct.ParamType.Unsigned;
        } else if (name.contentEquals("DATE")) {
            return ParameterStruct.ParamType.String;  // can also be Unsigned
        } else if (name.contentEquals("TIME")) {
            return ParameterStruct.ParamType.String;
        }
        // default
        return ParameterStruct.ParamType.String;
    }

    /**
     * indicates if the name is reserved and can't be used for a variable.
     * 
     * @param name - the name to check
     * 
     * @return true if it is a reserved name
     */
    public static boolean isReservedName (String name) {
        for (ReservedVars entry : ReservedVars.values()) {
            if (entry.toString().contentEquals(name)) {
                frame.outputInfoMsg(STATUS_VARS, "    Reserved name found: " + name);
                return true;
            }
        }
        return false;
    }
    
    /**
     * this returns the type of variable found.
     * 
     * @param name - name of the variable
     * 
     * @return type of variable (UNKNOWN if not found)
     */
    public static VarClass getVariableClass (String name) {
        if (name == null || name.isEmpty()) {
            return VarClass.UNKNOWN;
        }
        // if name contains brackets or a Trait, eliminate that portion of the name for comparing type
        int offset = name.indexOf('.');
        if (offset > 0) name = name.substring(0, offset);
        else {
            offset = name.indexOf('[');
            if (offset > 0) name = name.substring(0, offset);
        }
        // classify the type
        if (isReservedName (name))           return VarClass.RESERVED;
        if (longParams.containsKey(name)) return VarClass.NUMERIC;
        if (uintParams.containsKey(name)) return VarClass.NUMERIC;
        if (strParams.containsKey(name))  return VarClass.STRING;
        if (boolParams.containsKey(name)) return VarClass.BOOLEAN;
        if (VarArray.isIntArray(name))       return VarClass.ARRAY;
        if (VarArray.isStrArray(name))       return VarClass.ARRAY;
        if (LoopStruct.getCurrentLoopValue(name) != null)   return VarClass.LOOP;
        return VarClass.UNKNOWN;
    }
    
    /**
     * determines if a Variable has been found with the specified name.
     * 
     * @param name     - name of the Variable to search for
     * @param traitVal - the trait associated with the variable (null if none)
     * @param bNoLoops - true if don't include loop params in search
     * 
     * @return type of Variable if found, null if not found
     * 
     * @throws ParserException
     */
    public static Long getNumericValue (String name, VariableExtract.Trait traitVal, boolean bNoLoops) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }
        Long iValue = null;
        try {
            Integer loopVal = LoopStruct.getCurrentLoopValue(name);
            if (loopVal != null) {
                iValue = (long) loopVal;
                if (bNoLoops) {
                    return null; // value is a loop var, but is not allowed
                }
            }
            else if (longParams.containsKey(name)) {
                iValue = longParams.get(name);
            }
            else if (uintParams.containsKey(name)) {
                iValue = uintParams.get(name);
            }
            else if (strParams.containsKey(name)) {
                String strValue = strParams.get(name);
                if (traitVal == null) {
                    iValue = ParameterStruct.getLongOrUnsignedValue(strValue);
                    frame.outputInfoMsg(STATUS_DEBUG, "Converting variable " + name + " to Integer: " + iValue);
                }
                else if (traitVal == VariableExtract.Trait.SIZE) {
                    iValue = (long) strValue.length();
                    frame.outputInfoMsg(STATUS_DEBUG, "Extracted SIZE of variable " + name + " as: " + iValue);
                }
            }
            else if (boolParams.containsKey(name)) {
                frame.outputInfoMsg(STATUS_DEBUG, "Converting from Boolean to Integer: " + name);
                iValue = boolParams.get(name) ? 1L : 0;
            }
            else if (VarArray.isIntArray(name)) {
                if (traitVal == null) {
                    iValue = VarArray.getIntArray(name).getFirst();
                    frame.outputInfoMsg(STATUS_DEBUG, "Extracted 1st entry of IntArray " + name + " to Integer: " + iValue);
                }
                else if (traitVal == VariableExtract.Trait.SIZE) {
                    iValue = (long) VarArray.getIntArray(name).size();
                    frame.outputInfoMsg(STATUS_DEBUG, "Extracted SIZE of IntArray " + name + " as: " + iValue);
                }
            }
            else if (VarArray.isStrArray(name)) {
                if (traitVal == null) {
                    String strValue = VarArray.getStrArray(name).getFirst();
                    iValue = ParameterStruct.getLongOrUnsignedValue(strValue);
                    frame.outputInfoMsg(STATUS_DEBUG, "Extracted 1st entry of StrArray " + name + " to Integer: " + iValue);
                }
                else if (traitVal == VariableExtract.Trait.SIZE) {
                    iValue = (long) VarArray.getStrArray(name).size();
                    frame.outputInfoMsg(STATUS_DEBUG, "Extracted SIZE of StrArray " + name + " as: " + iValue);
                }
            }
            else {
                switch (name) {
                    case "RESPONSE":
                        String strValue = VarArray.getResponseValue().getFirst();
                        iValue = ParameterStruct.getLongOrUnsignedValue(strValue);
                        break;
                    case "STATUS":
                        iValue = bStatus ? 1L : 0;
                        break;
                    case "RANDOM":
                        iValue = getRandomValue();
                        break;
                    case "DATE":
                        LocalDate currentDate = LocalDate.now();
                        switch (traitVal) {
                            case DOW:
                                iValue = (long) currentDate.getDayOfWeek().getValue();
                                break;
                            case DOM:
                                iValue = (long) currentDate.getDayOfMonth();
                                break;
                            case DOY:
                                iValue = (long) currentDate.getDayOfYear();
                                break;
                            case MOY:
                                iValue = (long) currentDate.getMonthValue();
                                break;
                            default:
                                // no other value is allowed for Integers
                                break;
                        }
                        break;
                    case "TIME":  // can't be converted to Integer
                    default:
                        break;
                }
            }
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        if (iValue == null) {
            throw new ParserException(functionId + "Numeric value not found for variable: " + name);
        }
        return iValue;
    }

    /**
     * checks if a name is valid to use for assigning to.
     *   - name must begin with an alpha character
     *   - name must be only alphanumeric or '_' chars,
     *   - cannot be a command name or an operation name
     *   - cannot be a Loop Variable name.
     *   - must be either a Variable that is defined or a reserved Variable.
     * 
     * @param use  - how the variable is being used
     * @param name - the name to check
     * 
     * @return  true if Variable is already defined, false if not
     * 
     * @throws ParserException - if not valid
     */
    public static boolean isValidVariableName (VarCheck use, String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        try {
            if (name.startsWith("$")) {
                name = name.substring(1);
            }

            // verify the formaat of the Variable name
            if (!verifyVariableFormat(name)) {
                throw new ParserException(functionId + "Invalid format for Variable: " + name);
            }

            // make sure it is not a command name
            if (CommandStruct.isValidCommand(name) != null) {
                throw new ParserException(functionId + "using Reserved command name: " + name);
            }

            // check for variable lists
            VarClass varClass = getVariableClass (name);
            String errMsg = null;
            switch (use) {
                default:
                case DEFINE:
                    switch (varClass) {
                        case UNKNOWN  -> { // not found in any group is good
                        }
                        case LOOP     -> errMsg = "using Loop Variable name: " + name;
                        case RESERVED -> errMsg = "using Reserved command name: " + name;
                        default       -> errMsg = "Variable name already in use: " + name;
                    }
                    break;

                case SET:
                    // you can only set user variables plus this one reserved variable
                    if (name.contentEquals("RANDOM")) {
                        break;
                    }
                    switch (varClass) {
                        case LOOP     -> errMsg = "using Loop Variable name: " + name;
                        case RESERVED -> errMsg = "using Reserved command name: " + name;
                        case UNKNOWN  -> errMsg = "Variable name not found: " + name;
                        default       -> {  // any variable is good
                        }
                    }
                    break;

                case REFERENCE:
                    // if it is a member of any of the groups, it is good
                    if (varClass == VarClass.UNKNOWN) {
                        errMsg = "Variable name not found: " + name;
                    }
                    break;
            }
            if (errMsg != null) {
                throw new ParserException(functionId + errMsg);
            }
            return true;
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId + name);
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
    public static boolean verifyVariableFormat (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (name == null) {
            frame.outputInfoMsg(STATUS_DEBUG, "Variable name is null");
            return false;
        }
        if (name.isBlank()) {
            frame.outputInfoMsg(STATUS_DEBUG, "Variable name is blank");
            return false;
        }
        boolean bRighthand = false;
        if (name.startsWith("$")) {
            name = name.substring(1);
            bRighthand = true;
        }
        if (! Character.isLetter(name.charAt(0))) {
            // 1st character must be a letter
            frame.outputInfoMsg(STATUS_DEBUG, "invalid initial character in Variable name: " + name);
            return false;
        }

        // determine if we have a special param type that can take on appendages
        ParameterStruct.ParamType type = Variables.getVariableTypeFromName (name);
        
            // TODO: we need to do this for the '.' operator as well
            int indexStart = 0;
            int indexEnd = 0;
            for (int ix = 0; ix < name.length(); ix++) {
                char curch = name.charAt(ix);
                // valid char for Variable
                if ( (curch == '_') || Character.isLetterOrDigit(curch) ) {
                    if (ix > NAME_MAXLEN) {
                        frame.outputInfoMsg(STATUS_DEBUG, "Variable name too long (max len " + NAME_MAXLEN + ") in name: " + name.substring(0, ix));
                        return false;
                    }
                } else {
                    // this will terminate the Variable search
                    if (curch == ' ' || curch == '=') {
                        break;
                    }
                    if (!bRighthand) {
                        frame.outputInfoMsg(STATUS_DEBUG, "Variable assignment should not include '$': " + name);
                        return false;
                    }
                    if (type != ParameterStruct.ParamType.String && type != ParameterStruct.ParamType.StringArray && type != ParameterStruct.ParamType.IntArray) {
                        frame.outputInfoMsg(STATUS_DEBUG,  "Variable extensions are only valid for String and Array types: " + name);
                        return false;
                    }
                    // check for bracket index on Array, List and String Variable
                    switch (curch) {
                        case '[':
                            int offset = name.indexOf(']');
                            if (offset <= 0 || offset >= name.length() - 1) {
                                frame.outputInfoMsg(STATUS_DEBUG,  "missing end bracket in Variable name: " + name);
                                return false;
                            }
                            try {
                                indexStart = Utils.getIntValue(name.substring(ix+1)).intValue();
                            } catch (ParserException exMsg) {
                                frame.outputInfoMsg(STATUS_DEBUG, "invalid numeric in brackets");
                                return false;
                            }
                            // TODO: evaluate indexEnd
                            break;
                        case '.':
                            // TODO:
                            break;
                        default:
                            frame.outputInfoMsg(STATUS_DEBUG, "invalid character '" + curch + "' in Variable name: " + name);
                            return false;
                    }
                }
            }
            if (indexStart == 0 && name.length() > NAME_MAXLEN) {
                frame.outputInfoMsg(STATUS_DEBUG, "Variable name too long (max len " + NAME_MAXLEN + ") in name: " + name);
                return false;
            }
            if (indexStart > 0 && indexEnd == 0) {
                frame.outputInfoMsg(STATUS_DEBUG, "Variable name index missing ending bracket: " + name);
                return false;
            }
            
        return true;
    }
    
}
