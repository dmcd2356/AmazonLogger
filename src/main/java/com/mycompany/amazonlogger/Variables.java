/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import static com.mycompany.amazonlogger.UIFrame.STATUS_WARN;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

/**
 *
 * @author dan
 */
public class Variables {
    
    private static final String CLASS_NAME = "Variables";
    
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
        String functionId = CLASS_NAME + ".allocateVariable: ";

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
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Allocated " + type.toString() + " variable: " + name);
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
        paramValue.setVariableRef(new VariableInfo (paramInfo));

        String name = paramInfo.getName();
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }
        // first, check for reserved variables
        ParameterStruct.ParamType pType = null;
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
                Random rand = new Random();
                paramValue.setIntegerValue(rand.nextLong(maxRandom));
                pType = ParameterStruct.ParamType.Integer;
                break;
            case "TIME":
                LocalTime currentTime = LocalTime.now();
                paramValue.setStringValue(currentTime.toString().substring(0,12));
                pType = ParameterStruct.ParamType.String;
                break;
            case "DATE":
                LocalDate currentDate = LocalDate.now();
                if (paramInfo.getTrait() == null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
                    paramValue.setStringValue(currentDate.format(formatter));
                    pType = ParameterStruct.ParamType.String;
                } else {
                    // these traits are only valid for DATE
                    switch (paramInfo.getTrait()) {
                        default:
                        case VariableExtract.Trait.DOW:
                            DayOfWeek dow = currentDate.getDayOfWeek();
                            paramValue.setIntegerValue((long) dow.getValue());
                            pType = ParameterStruct.ParamType.Unsigned;
                            break;
                        case VariableExtract.Trait.DOM:
                            int ivalue = currentDate.getDayOfMonth();
                            paramValue.setIntegerValue((long) ivalue);
                            pType = ParameterStruct.ParamType.Unsigned;
                            break;
                        case VariableExtract.Trait.DOY:
                            ivalue = currentDate.getDayOfYear();
                            paramValue.setIntegerValue((long) ivalue);
                            pType = ParameterStruct.ParamType.Unsigned;
                            break;
                        case VariableExtract.Trait.MOY:
                            ivalue = currentDate.getMonthValue();
                            paramValue.setIntegerValue((long) ivalue);
                            pType = ParameterStruct.ParamType.Unsigned;
                            break;
                        case VariableExtract.Trait.DAY:
                            paramValue.setStringValue(currentDate.getDayOfWeek().toString());
                            pType = ParameterStruct.ParamType.String;
                            break;
                        case VariableExtract.Trait.MONTH:
                            paramValue.setStringValue(currentDate.getMonth().toString());
                            pType = ParameterStruct.ParamType.String;
                            break;
                    }
                }
                break;
            default:
                break;
        }

        // otherwise, let's check for standard (and loop) variables for name match
        if (pType == null) {
            pType = paramInfo.getType();
            ParameterStruct.ParamType findType = isVariableDefined(name);
            if (findType == null) {
                if (LoopParam.isLoopParamDefined(name)) {
                    findType = ParameterStruct.ParamType.Integer;
                } else {
                    frame.outputInfoMsg(STATUS_WARN, "    - Variable Ref '" + name + "' as type " + pType + " was not found in any Variable database");
                }
            }
            if (pType == null || findType != pType) {
                frame.outputInfoMsg(STATUS_PROGRAM, "    - Incorrect Variable Ref type for '" + name + "' as type " + pType + " is actually " + findType);
                pType = findType;
            }

            switch (pType) {
                case Integer:
                    // first check the standard parameters
                    paramValue.setIntegerValue(longParams.get(name));
                    Long varValue = paramValue.getIntegerValue();
                    if (varValue == null) {
                        // if not, check if it is in loop parameters
                        varValue = LoopParam.getLoopCurValue(name);
                        if (varValue != null) {
                            paramValue.setIntegerValue(varValue);
                            pType = ParameterStruct.isUnsignedInt(varValue) ? ParameterStruct.ParamType.Unsigned : ParameterStruct.ParamType.Integer;
                        } else {
                            throw new ParserException(functionId + "Parameter " + name + " not found");
                        }
                    }
                    frame.outputInfoMsg(STATUS_PROGRAM, "    - Lookup Ref '" + name + "' as type " + pType + ": " + varValue);
                    break;
                case Unsigned:
                    paramValue.setIntegerValue(uintParams.get(name));
                    varValue = paramValue.getIntegerValue();
                    if (varValue == null) {
                        // if not, check if it is in loop parameters
                        Long loopVal = LoopParam.getLoopCurValue(name);
                        if (loopVal != null) {
                            paramValue.setIntegerValue(loopVal);
                            pType = ParameterStruct.isUnsignedInt(varValue) ? ParameterStruct.ParamType.Unsigned : ParameterStruct.ParamType.Integer;
                        } else {
                            throw new ParserException(functionId + "Parameter " + name + " not found");
                        }
                    }
                    frame.outputInfoMsg(STATUS_PROGRAM, "    - Lookup Ref '" + name + "' as type " + pType + ": " + varValue);
                    break;
                case Boolean:
                    paramValue.setBooleanValue(boolParams.get(name));
                    if (paramValue.getBooleanValue() == null) {
                        throw new ParserException(functionId + "Parameter " + name + " not found");
                    }
                    frame.outputInfoMsg(STATUS_PROGRAM, "    - Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.getBooleanValue());
                    break;
                case IntArray:
                    paramValue.setIntArray(VarArray.getIntArray(name));
                    if (paramValue.getIntArray() == null) {
                        throw new ParserException(functionId + "Parameter " + name + " not found");
                    }
                    String arrayValue = paramValue.getIntArray().toString();
                    if (arrayValue.length() > 100) {
                        arrayValue = arrayValue.substring(0,20) + "...";
                    }
                    frame.outputInfoMsg(STATUS_PROGRAM, "    - Lookup Ref '" + name + "' as type " + pType + ": " + arrayValue);
                    break;
                case StringArray:
                    paramValue.setStrArray(VarArray.getStrArray(name));
                    if (paramValue.getStrArray() == null) {
                        throw new ParserException(functionId + "Parameter " + name + " not found");
                    }
                    arrayValue = paramValue.getStrArray().toString();
                    if (arrayValue.length() > 100) {
                        arrayValue = arrayValue.substring(0,20) + "...";
                    }
                    frame.outputInfoMsg(STATUS_PROGRAM, "    - Lookup Ref '" + name + "' as type " + pType + ": " + arrayValue);
                    break;
                default:
                case String:
                    paramValue.setStringValue(strParams.get(name));
                    if (paramValue.getStringValue() != null) {
                        pType = ParameterStruct.ParamType.String;
                    } else {
                        throw new ParserException(functionId + "Parameter " + name + " not found");
                    }
                    frame.outputInfoMsg(STATUS_PROGRAM, "    - Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.getStringValue());
                    break;
            }
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
                        frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + "[" + iStart + "] as type " + pType + ": " + paramValue.getIntegerValue());
                    } else {
                        throw new ParserException(functionId + "Parameter " + name + " index " + iStart + " exceeds array");
                    }
                    break;
                case ParameterStruct.ParamType.StringArray:
                    if (iStart < paramValue.getStrArraySize()) {
                        paramValue.setStringValue(paramValue.getStrArrayElement(iStart));
                        pType = ParameterStruct.ParamType.String;
                        frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + "[" + iStart + "] as type " + pType + ": " + paramValue.getStringValue());
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
                        frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + "index" + ixRange + " as type " + pType + ": " + paramValue.getStringValue());
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
                    frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + "." + trait.toString() + " as type " + pType + ": " + paramValue.getStringValue());
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
                    frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + "." + trait.toString() + " as type " + pType + ": " + strValue);
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
                    frame.outputInfoMsg(STATUS_PROGRAM, "    " + name + "." + trait.toString() + " as type Boolean: " + strValue);
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
        String functionId = CLASS_NAME + ".modifyStringVariable: ";

        if (strParams.containsKey(name)) {
            strParams.replace(name, value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified String param: " + name + " = " + value);
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
        String functionId = CLASS_NAME + ".modifyIntegerVariable: ";

        if (longParams.containsKey(name)) {
            longParams.replace(name, value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified Integer param: " + name + " = " + value);
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
        String functionId = CLASS_NAME + ".modifyUnsignedVariable: ";

        if (! ParameterStruct.isUnsignedInt(value)) {
            throw new ParserException(functionId + "value for Variable " + name + " exceeds limits for Unsigned: " + value);
        }
        if (name.contentEquals("RANDOM")) {
            maxRandom = value;  // this will set the max range for random value (0 to maxRandom - 1)
        } else if (uintParams.containsKey(name)) {
            uintParams.replace(name, value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified Unsigned param: " + name + " = " + value);
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
        String functionId = CLASS_NAME + ".modifyBooleanVariable: ";

        if (boolParams.containsKey(name)) {
            boolParams.replace(name, value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified Boolean param: " + name + " = " + value);
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
        if (VarArray.isIntArray(name)) {
            VarArray.removeArrayEntry(name, ParameterStruct.ParamType.IntArray);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted IntArray Variable: " + name);
        }
        if (VarArray.isStrArray(name)) {
            VarArray.removeArrayEntry(name, ParameterStruct.ParamType.StringArray);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted StrArray Variable: " + name);
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
     * indicates if the name is reserved and can't be used for a variable.
     * 
     * @param name - the name to check
     * 
     * @return true if it is a reserved name
     */
    public static boolean isReservedName (String name) {
        for (ReservedVars entry : ReservedVars.values()) {
            if (entry.toString().contentEquals(name)) {
                frame.outputInfoMsg(STATUS_PROGRAM, "    Reserved name found: " + name);
                return true;
            }
        }
        return false;
    }
    
    /**
     * determines if a Variable has been found with the specified name.
     * (Does not check for loop Variables)
     * 
     * @param name - name of the Variable to search for
     * 
     * @return type of Variable if found, null if not found
     */
    public static Long getNumericValue (String name) {
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }
        if (longParams.containsKey(name)) {
            return longParams.get(name);
        }
        if (uintParams.containsKey(name)) {
            return uintParams.get(name);
        }
        return (long) LoopStruct.getCurrentLoopValue(name);
    }
    
    /**
     * determines if a Variable has been found with the specified name.
     * (Does not check for loop Variables)
     * 
     * @param name - name of the Variable to search for
     * 
     * @return type of Variable if found, null if not found
     */
    public static ParameterStruct.ParamType isVariableDefined (String name) {
        if (longParams.containsKey(name)) {
            return ParameterStruct.ParamType.Integer;
        }
        if (uintParams.containsKey(name)) {
            return ParameterStruct.ParamType.Unsigned;
        }
        if (strParams.containsKey(name)) {
            return ParameterStruct.ParamType.String;
        }
        if (boolParams.containsKey(name)) {
            return ParameterStruct.ParamType.Boolean;
        }
        if (VarArray.isIntArray(name)) {
            return ParameterStruct.ParamType.IntArray;
        }
        if (VarArray.isStrArray(name)) {
            return ParameterStruct.ParamType.StringArray;
        }
        return null;
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
        String functionId = CLASS_NAME + ".isValidVariableName: ";

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
            boolean bLoop = LoopParam.isLoopParamDefined(name);
            boolean bReserved = Variables.isReservedName(name);
            boolean bVariable = Variables.isVariableDefined(name) != null;

            String errMsg = null;
            switch (use) {
                default:
                case DEFINE:
                    if (bLoop)
                        errMsg = "using Loop Variable name: " + name;
                    else if (bReserved)
                        errMsg = "using Reserved command name: " + name;
                    else if (bVariable)
                        errMsg = "Variable name already in use: " + name;
                    break;
                case SET:
                    if (bLoop)
                        errMsg = "using Loop Variable name: " + name;
                    else if (bReserved && ! name.contentEquals("RANDOM"))
                        errMsg = "using Reserved command name: " + name;
                    if (! bVariable && ! name.contentEquals("RANDOM"))
                        errMsg = "Variable name not found: " + name;
                    break;
                case REFERENCE:
                    if (!bLoop && !bVariable && !bReserved)
                        errMsg = "Variable name not found: " + name;
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
        String functionId = CLASS_NAME + ".verifyParamFormat: ";
        
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
