/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.ParameterStruct.getLongOrUnsignedValue;
import static com.mycompany.amazonlogger.ParameterStruct.isUnsignedInt;
import static com.mycompany.amazonlogger.ParameterStruct.isValidVariableName;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import static com.mycompany.amazonlogger.UIFrame.STATUS_WARN;
import static com.mycompany.amazonlogger.VariableExtract.Trait.FILTER;
import static com.mycompany.amazonlogger.VariableExtract.Trait.ISEMPTY;
import static com.mycompany.amazonlogger.VariableExtract.Trait.LOWER;
import static com.mycompany.amazonlogger.VariableExtract.Trait.REVERSE;
import static com.mycompany.amazonlogger.VariableExtract.Trait.SIZE;
import static com.mycompany.amazonlogger.VariableExtract.Trait.SORT;
import static com.mycompany.amazonlogger.VariableExtract.Trait.UPPER;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/**
 *
 * @author dan
 */
public class Variables {
    
    private static final String CLASS_NAME = "Variables";
    
    // user-defined static Variables
    private static final HashMap<String, String>  strParams  = new HashMap<>();
    private static final HashMap<String, Long>    longParams = new HashMap<>();
    private static final HashMap<String, Long>    uintParams = new HashMap<>();
    private static final HashMap<String, Boolean> boolParams = new HashMap<>();
    private static final HashMap<String, ArrayList<Long>>    intArrayParams = new HashMap<>();
    private static final HashMap<String, ArrayList<String>>  strArrayParams = new HashMap<>();

    // reserved static Variables
    private static final ArrayList<String>  strResponse = new ArrayList<>();    // responses from RUN commands
    private static boolean            bStatus = false;          // true/false status indications
    private static long               maxRandom = 1000000000;   // for random values 0 - 999999999

    // array filter info
    private static ArrayList<Boolean> ixFilter = null;
    
    public enum ReservedVars {
        RESPONSE,
        STATUS,
        RANDOM,
        DATE,
        TIME,
    }
    
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
        strResponse.clear();
        bStatus = false;
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
            case ParameterStruct.ParamType.Integer:
                typeName = "Integer";
                longParams.put(name, 0L);
                break;
            case ParameterStruct.ParamType.Unsigned:
                typeName = "Unsigned";
                uintParams.put(name, 0L);
                break;
            case ParameterStruct.ParamType.Boolean:
                typeName = "Boolean";
                boolParams.put(name, false);
                break;
            case ParameterStruct.ParamType.IntArray:
                typeName = "Array";
                intArrayParams.put(name, new ArrayList<>());
                break;
            case ParameterStruct.ParamType.StringArray:
                typeName = "List";
                strArrayParams.put(name, new ArrayList<>());
                break;
            case ParameterStruct.ParamType.String:
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
        paramValue.setVariableRef(new VariableInfo (paramInfo));

        String name = paramInfo.getName();
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }
        // first, check for reserved variables
        ParameterStruct.ParamType pType = null;
        switch (name) {
            case "RESPONSE":
                paramValue.setStrArray(strResponse);
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
                frame.outputInfoMsg(STATUS_WARN, "    - Variable Ref '" + name + "' as type " + pType + " was not found in any Variable database");
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
                        Long loopVal = LoopParam.getLoopCurValue(name);
                        if (loopVal != null) {
                            paramValue.setIntegerValue(loopVal);
                            pType = isUnsignedInt(varValue) ? ParameterStruct.ParamType.Unsigned : ParameterStruct.ParamType.Integer;
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
                            pType = isUnsignedInt(varValue) ? ParameterStruct.ParamType.Unsigned : ParameterStruct.ParamType.Integer;
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
                    paramValue.setIntArray(intArrayParams.get(name));
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
                    paramValue.setStrArray(strArrayParams.get(name));
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
                            if (ixFilter == null) {
                                throw new ParserException(functionId + trait.toString() + " has not been initialized yet");
                            }
                            if (ixFilter.size() != psize) {
                                throw new ParserException(functionId + trait.toString() + " has size " + ixFilter.size() + ", but array is size " + psize);
                            }
                            // remove the selected entries
                            for (int ix = psize - 1; ix >= 0; ix--) {
                                if (!ixFilter.get(ix)) {
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
                            if (ixFilter == null) {
                                throw new ParserException(functionId + trait.toString() + " has not been initialized yet");
                            }
                            if (ixFilter.size() != psize) {
                                throw new ParserException(functionId + trait.toString() + " has size " + ixFilter.size() + ", but array is size " + psize);
                            }
                            // remove the selected entries
                            for (int ix = psize - 1; ix >= 0; ix--) {
                                if (!ixFilter.get(ix)) {
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

        if (! isUnsignedInt(value)) {
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

        if (name.contentEquals("RESPONSE")) {
            return strResponse.size();
        } else if (intArrayParams.containsKey(name)) {
            ArrayList<Long> entry = intArrayParams.get(name);
            return entry.size();
        } else if (strArrayParams.containsKey(name)) {
            ArrayList<String> entry = strArrayParams.get(name);
            return entry.size();
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
     * resets the array filter.
     */
    public static void arrayFilterReset () {
        ixFilter = null;
    }
    
    /**
     * sets the filter entries based on the matching conditions in the StringArray.
     * 
     * @param varName   - name of the variable to check
     * @param strFilter - the filter match criteria
     * @param opts      - '!' to invert the logic and LEFT or RIGHT for searching only ends
     * 
     * @throws ParserException
     * 
     */
    public static void arrayFilterString (String varName, String strFilter, String opts) throws ParserException {
        String functionId = CLASS_NAME + ".arrayFilterString: ";

        if (! strArrayParams.containsKey(varName)) {
            throw new ParserException(functionId + "Array parameter not found: " + varName);
        }
        ArrayList<String> var = strArrayParams.get(varName);

        // if this is 1st filter being performed, init entries to all true
        if (ixFilter == null) {
            ixFilter = new ArrayList<>();
            for (String var1 : var) {
                ixFilter.add(true);
            }
        } else if (var.size() != ixFilter.size()) {
            throw new ParserException(functionId + "Filter Array size mismatch: " + varName + " = " + var.size() + ", filter = " + ixFilter.size());
        }

        // check for leading or trailing filter checks
        boolean bInvert = false;
        if (opts.charAt(0) == '!') {
            bInvert = true;
            opts = opts.substring(1);
        }
        
        // now we only mark the entries that do not match the criteria, so that filters
        // can be stacked upon each other.
        for (int ix = 0; ix < var.size(); ix++) {
            String entry = var.get(ix);

            // search for matching chars anywhere in string
            boolean bMatch = entry.contains(strFilter);
            if (bMatch) {
                // the pattern was found somewhere in the string.
                //  If we are matching LEFT or RIGHT only, we need to check
                //  if these cases matched.
                int endix = entry.length() - strFilter.length();
                int offset = entry.lastIndexOf(strFilter);
                switch (opts) {
                    case "LEFT" -> {
                        if (offset != 0) {      // the match did not occur at the start
                            bMatch = false;
                        }
                    }
                    case "RIGHT" -> {
                        if (offset != endix) {  // the match did not occur at the end
                            bMatch = false;
                        }
                    }
                    default -> {}
                }
            }

            // now we should eliminate the entry if we are not inverting and we did not have a math,
            // or if we are inverting and we did have a match.
            if (bMatch == bInvert) {
                ixFilter.set(ix, false);
            }
        }
    }
    
    /**
     * sets the filter entries based on the matching conditions in the IntArray.
     * 
     * @param varName   - name of the variable to check
     * @param compSign  - the comparison sign
     * @param value     - value to compare the entries to
     * 
     * @throws ParserException
     * 
     */
    public static void arrayFilterInt (String varName, String compSign, Long value) throws ParserException {
        String functionId = CLASS_NAME + ".arrayFilterInt: ";

        if (! intArrayParams.containsKey(varName)) {
            throw new ParserException(functionId + "Array parameter not found: " + varName);
        }
        ArrayList<Long> var = intArrayParams.get(varName);
        for (int ix = 0; ix < var.size(); ix++) {
            Long entry = var.get(ix);
            boolean bMatch = false;
            switch (compSign) {
                default:
                case "==": bMatch = (Objects.equals(entry, value));   break;
                case "!=": bMatch = (!Objects.equals(entry, value));  break;
                case ">=": bMatch = (entry >= value);   break;
                case "<=": bMatch = (entry <= value);   break;
                case ">":  bMatch = (entry > value);    break;
                case "<":  bMatch = (entry < value);    break;
            }
            
            if (!bMatch) {
                ixFilter.set(ix, false);
            }
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
        if (intArrayParams.containsKey(name)) {
            return ParameterStruct.ParamType.IntArray;
        }
        if (strArrayParams.containsKey(name)) {
            return ParameterStruct.ParamType.StringArray;
        }
        return null;
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
    
}
