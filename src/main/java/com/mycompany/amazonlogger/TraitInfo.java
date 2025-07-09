/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import com.mycompany.amazonlogger.GUILogPanel.MsgType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author dan
 */
public class TraitInfo {
    
    private static final String CLASS_NAME = TraitInfo.class.getSimpleName();

    
    // traits extensions
    // KEY: I = Integer, U = Unsigned, B = Boolean, S = String, L = StrArray, A = IntArray
    public enum Trait {
        HEX,                //  UI -> S, A -> L : convert to hexadecimal String (or StrArray)
        UPPER,              //   S -> S : convert all chars to uppercase
        LOWER,              //   S -> S : convert all chars to lowercase
        TOLINES,            //   S -> L : convert to StrArray of lines of text
        TOWORDS,            //   S -> L : convert to StrArray of words
        SORT,               //   L -> L : sort from A-Z
        REVERSE,            //   L -> L : sort from A-Z
        FILTER,             //   L -> L, A -> A : filtered contents
        SIZE,               // SLA -> I : number of chars for String, number of elements for Arrays
        LENGTH,             // SLA -> I : (this is the same as SIZE)
        ISEMPTY,            // SLA -> B : check if item is zero-length
        TOSTRING,           // SLA -> S : convert Array entries to a String
        DOW,                //   S -> I : (DATE only): for day of week
        DOM,                //   S -> I : (DATE only): for day of month
        DOY,                //   S -> I : (DATE only): for day of year
        MOY,                //   S -> I : (DATE only): for month of year
        DAY,                //   S -> S : (DATE only): for Day of week
        MONTH,              //   S -> S : (DATE only): for Month
        WRITER,             // Any -> S : the function that last wrote the variable
        WRITETIME,          // Any -> S : the timestamp when the variable was last written
    }

    /**
     * finds a Trait match for the selected traitName & verifies it is valid for variable name and type.
     * 
     * @param traitName - portion of variable name that represents the trait (follows the '.' char)
     * @param varName   - the base name of the variable
     * @param varType  - the data type of the variable
     * 
     * @return the Trait value
     * 
     * @throws ParserException if trait not found or not valid for selected variable
     */
    public static Trait getTrait (String traitName, String varName, ParameterStruct.ParamType varType) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
    
        // ignore the leading $ char if present
        if (varName.charAt(0) == '$') {
            varName = varName.substring(1);
        }
        
        Trait traitVal = null;
        for (Trait entry : Trait.values()) {
            if (entry.toString().contentEquals(traitName)) {
                traitVal = entry;
                GUILogPanel.outputInfoMsg(MsgType.VARS, "Variable trait found: ." + traitVal + " in " + varName);
                break;
            }
        }
        if (traitVal == null) {
            throw new ParserException(functionId + "Invalid Trait for Variable: " + varName);
        }

        switch (traitVal) {
            case DOW:
            case DOM:
            case DOY:
            case MOY:
            case DAY:
            case MONTH:
                // these are only valid for $DATE
                if ( ! varName.contentEquals("DATE") ) {
                    traitVal = null;
                }
                break;

            case HEX:
                // these are only valid for Unsigned and Integer types
                if (varType != ParameterStruct.ParamType.Unsigned &&
                    varType != ParameterStruct.ParamType.Integer  &&
                    varType != ParameterStruct.ParamType.IntArray) {
                    traitVal = null;
                }
                break;
                
            case LOWER:
            case UPPER:
            case TOLINES:
            case TOWORDS:
                // these are only valid for String types
                if (varType != ParameterStruct.ParamType.String) {
                    traitVal = null;
                }
                break;

            case SORT:
            case REVERSE:
                // these are only valid for StrArray types
                if (varType != ParameterStruct.ParamType.StrArray) {
                    traitVal = null;
                }
                break;

            case FILTER:
            case TOSTRING:
                // these are only valid for StrArray and IntArray types
                if (varType != ParameterStruct.ParamType.StrArray &&
                    varType != ParameterStruct.ParamType.IntArray)   {
                    traitVal = null;
                }
                break;
                    
            case SIZE:
            case LENGTH:
            case ISEMPTY:
                // these are allowed for Strings and Arrays
                if (varType != ParameterStruct.ParamType.StrArray &&
                    varType != ParameterStruct.ParamType.IntArray &&
                    varType != ParameterStruct.ParamType.String)     {
                    traitVal = null;
                }
                break;
                
            case WRITER:
            case WRITETIME:
                String subName = Subroutine.getSubName();
                if (! VarGlobal.isDefined(varName) && ! PreCompile.variables.varLocal.isDefined(subName, varName)) {
                    traitVal = null;
                }
                break;
                
            default:
                throw new ParserException(functionId + "Invalid Trait for " + varType + " Variable " + varName + ": " + traitName);
        }

        if (traitVal == null) {
            throw new ParserException(functionId + "Invalid Trait for " + varType + " Variable " + varName + ": " + traitName);
        }
        return traitVal;
    }

    /**
     * returns the data type of the variable after the Trait is applied.
     * 
     * @param traitVal - the Trait to check
     * @param varName  - the base name of the variable
     * @param varType  - the data type of the variable
     * 
     * @return the type of data returned by the trait
     * 
     * @throws ParserException if trait not found or not valid for selected variable
     */
    public static ParameterStruct.ParamType getTraitDataType (Trait traitVal, String varName, ParameterStruct.ParamType varType) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        ParameterStruct.ParamType ptype;
        switch (traitVal) {
            case DOW:
            case DOM:
            case DOY:
            case MOY:
            case SIZE:
            case LENGTH:
                ptype = ParameterStruct.ParamType.Unsigned;
                break;

            case DAY:
            case MONTH:
            case LOWER:
            case UPPER:
            case TOSTRING:
                ptype = ParameterStruct.ParamType.String;
                break;

            case HEX:
                if (varType == ParameterStruct.ParamType.IntArray) {
                    ptype = ParameterStruct.ParamType.StrArray;
                } else {
                    ptype = ParameterStruct.ParamType.String;
                }
                break;

            case SORT:
            case REVERSE:
            case TOLINES:
            case TOWORDS:
                ptype = ParameterStruct.ParamType.StrArray;
                break;

            case ISEMPTY:
                ptype = ParameterStruct.ParamType.Boolean;
                break;
                
            case FILTER:
                // keep the Variable type
                ptype = PreCompile.variables.getVariableTypeFromName (varName);
                break;
                
            case WRITER:
            case WRITETIME:
                ptype = ParameterStruct.ParamType.String;
                break;

            default:
                throw new ParserException(functionId + "Invalid Trait: " + traitVal.toString());
        }
        return ptype;
    }

    /**
     * returns Trait values that are Integer values.
     * This is used in the Execution stage for Loop parameters and Index values
     *  in an Array bracket.
     * 
     * @param traitVal - the Trait selection
     * @param varName  - the name of the variable
     * @param varType  - the data type of the variable
     * 
     * @return the integer Trait value (null if not a Trait that returns an Integer value
     * 
     * @throws com.mycompany.amazonlogger.ParserException
     */
    public static Long getTraitIntValues (Trait traitVal, String varName, ParameterStruct.ParamType varType) throws ParserException {
        Long iValue = null;
        if (varName.contentEquals("DATE")) {
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
                    break;
            }
            if (iValue != null) {
                GUILogPanel.outputInfoMsg(MsgType.DEBUG, "Extracted " + traitVal + " of DATE as: " + iValue);
            }
        } else if (traitVal == Trait.SIZE || traitVal == Trait.LENGTH) {
            switch (varType) {
                case String:
                    iValue = (long) PreCompile.variables.getStringSize(varName);
                    break;
                case StrArray:
                    iValue = (long) PreCompile.variables.getStrArray(varName).size();
                    break;
                case IntArray:
                    iValue = (long) PreCompile.variables.getIntArray(varName).size();
                    break;
                default:
                    break;
            }
            if (iValue != null) {
                GUILogPanel.outputInfoMsg(MsgType.DEBUG, "Extracted SIZE of " + varType + " " + varName + " as: " + iValue);
            }
        }
        return iValue;
    }

    /**
     * returns Trait info about the variable itself.
     * 
     * @param trait   - the trait that applies to a feature of all variables
     * @param varName - the name of the variable
     * 
     * @return the feature of the variable (as a String response)
     * 
     * @throws ParserException 
     */
    public static String getReferenceDetails (Trait trait, String varName) throws ParserException {
        String strValue = null;
        
        // these apply to all data types (GLOBAL and LOCAL vars only)
        switch (trait) {
            case WRITER:
                strValue = PreCompile.variables.getWriterIndex(varName);
                break;
            case WRITETIME:
                strValue = PreCompile.variables.getWriterTime(varName);
                break;
            default:
                break;
        }

        return strValue;
    }
    
    /**
     * extracts and applies the Trait found for the parameter value.
     * This is used during the execution stage to modify the parameter value
     *  based on the Trait applied.
     * 
     * Does nothing if no Trait listed. Checks to verify the Trait is applicable to
     *  the data type of the parameter.
     * 
     * @param paramValue - the parameter value to check the Trait of
     * 
     * @return the parameter value representing the Trait
     * 
     * @throws ParserException 
     */
    public static ParameterStruct applyTraitValue (ParameterStruct paramValue) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        VarExtensions paramInfo = paramValue.getVariableRef();
        if (paramInfo == null) {
            return paramValue;
        }
        Trait trait = paramInfo.getTrait();
        if (trait == null) {
            return paramValue;
        }
        String name = paramInfo.getName();
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }

        String refInfo = getReferenceDetails(trait, name);
        if (refInfo != null) {
            // save the parameter type
            paramValue.setStringValue(refInfo);
            paramValue.setParamTypeDiscrete (ParameterStruct.ParamType.String);
            return paramValue;
        }
        
        ParameterStruct.ParamType pType = paramValue.getParamType();
        switch (pType) {
            case Integer:
            case Unsigned:
                switch (trait) {
                    case HEX:
                        Long intValue = paramValue.getIntegerValue();
                        if (intValue == null) {
                            throw new ParserException(functionId + "Integer value not defined for variable: " + name);
                        }
                        if (intValue > 0xFFFFFFFFL) {
                            throw new ParserException(functionId + "Integer value exceeds 32-bit max: " + name + " = " + intValue);
                        }
                        String strValue = Long.toHexString(intValue);
                        paramValue.setStringValue(strValue);
                        pType = ParameterStruct.ParamType.String;
                        break;
                    default:
                        throw new ParserException(functionId + "Invalid trait " + trait.toString() + " for data type " + pType);
                }
                break;
                
            case IntArray:
                Long psize = (long) paramValue.getIntArraySize();
                Boolean bEmpty = paramValue.isIntArrayEmpty();
                String strValue = "";
                switch (trait) {
                    case SIZE:
                    case LENGTH:
                        paramValue.setIntegerValue(psize);
                        paramValue.setStringValue(psize.toString());
                        strValue = paramValue.getStringValue();
                        pType = ParameterStruct.ParamType.Integer;
                        break;
                    case ISEMPTY:
                        paramValue.setBooleanValue(bEmpty);
                        paramValue.setStringValue(bEmpty.toString());
                        strValue = paramValue.getStringValue();
                        pType = ParameterStruct.ParamType.Boolean;
                        break;
                    case HEX:
                        ArrayList<String> strArray = new ArrayList<>();
                        for (int ix = 0; ix < psize.intValue(); ix++) {
                            Long iValue = paramValue.getIntArrayElement(ix);
                            strArray.add(Long.toHexString(iValue));
                        }
                        paramValue.setStrArray(strArray);
                        strValue = paramValue.getStrArray().toString();
                        if (strValue.length() > 50) {
                            strValue = strValue.substring(0, 50) + "...";
                        }
                        pType = ParameterStruct.ParamType.StrArray;
                        break;
                    case FILTER:
                        if (VarArray.getFilterArray() == null) {
                            throw new ParserException(functionId + trait.toString() + " has not been initialized yet");
                        }
                        if (VarArray.getFilterArray().size() != psize) {
                            throw new ParserException(functionId + trait.toString() + " has size " + VarArray.getFilterArray().size() + ", but array is size " + psize);
                        }
                        // remove the selected entries
                        for (int ix = psize.intValue() - 1; ix >= 0; ix--) {
                            if (!VarArray.getFilterArray().get(ix)) {
                                paramValue.getIntArray().remove(ix);
                            }
                        }
                        strValue = paramValue.getIntArray().toString();
                        if (strValue.length() > 50) {
                            strValue = strValue.substring(0, 50) + "...";
                        }
                        break;
                    case TOSTRING:
                        strValue = paramValue.getIntArray().toString();
                        paramValue.setStringValue(strValue);
                        if (strValue.length() > 50) {
                            strValue = strValue.substring(0, 50) + "...";
                        }
                        pType = ParameterStruct.ParamType.String;
                        break;
                    default:
                        throw new ParserException(functionId + "Invalid trait " + trait.toString() + " for data type " + pType);
                }
                GUILogPanel.outputInfoMsg(MsgType.VARS, "    " + name + "." + trait.toString() + " as type " + pType + ": " + strValue);
                break;
                
            case StrArray:
                psize = (long) paramValue.getStrArraySize();
                bEmpty = paramValue.isStrArrayEmpty();
                strValue = "";
                switch (trait) {
                    case SORT:
                        Collections.sort(paramValue.getStrArray().subList(0, psize.intValue()));
                        strValue = paramValue.getStrArray().toString();
                        if (strValue.length() > 20) strValue = strValue.substring(0, 20) + "...";
                        break;
                    case REVERSE:
                        Collections.reverse(paramValue.getStrArray());
                        strValue = paramValue.getStrArray().toString();
                        if (strValue.length() > 20) strValue = strValue.substring(0, 20) + "...";
                        break;
                    case SIZE:
                    case LENGTH:
                        paramValue.setIntegerValue(psize);
                        strValue = psize.toString();
                        pType = ParameterStruct.ParamType.Integer;
                        break;
                    case ISEMPTY:
                        paramValue.setBooleanValue(bEmpty);
                        strValue = bEmpty.toString();
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
                        for (int ix = psize.intValue() - 1; ix >= 0; ix--) {
                            if (!VarArray.getFilterArray().get(ix)) {
                                paramValue.getStrArray().remove(ix);
                            }
                        }
                        break;
                    case TOSTRING:
                        strValue = paramValue.getStrArray().toString();
                        paramValue.setStringValue(strValue);
                        if (strValue.length() > 50) {
                            strValue = strValue.substring(0, 50) + "...";
                        }
                        pType = ParameterStruct.ParamType.String;
                        break;
                    default:
                        throw new ParserException(functionId + "Invalid trait " + trait.toString() + " for data type " + pType);
                }
                GUILogPanel.outputInfoMsg(MsgType.VARS, "    " + name + "." + trait.toString() + " as type " + pType + ": " + strValue);
                break;
                
            case String:
                // these only apply to the DATE variable, so make sure the variable is correct
                switch (trait) {
                    case DOW:
                    case DOM:
                    case DOY:
                    case MOY:
                    case DAY:
                    case MONTH:
                        if (! name.contentEquals("DATE")) {
                            throw new ParserException(functionId + "Invalid trait " + trait.toString() + " for variable " + name + " (only allowed for $DATE)");
                        }
                        break;
                    default:
                        break;
                }
                
                Integer intValue;
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
                    case TOLINES:
                        strValue = paramValue.getStringValue();
                        List<String> list = Arrays.asList(strValue.split("\n"));
                        ArrayList<String> strList = new ArrayList<>(list);
                        paramValue.setStrArray(strList);
                        break;
                    case TOWORDS:
                        strValue = paramValue.getStringValue();
                        list = Arrays.asList(strValue.split(" "));
                        strList = new ArrayList<>(list);
                        paramValue.setStrArray(strList);
                        break;
                    case SIZE:
                    case LENGTH:
                        paramValue.setIntegerValue((long)paramValue.getStringValue().length());
                        strValue = paramValue.getIntegerValue().toString();
                        pType = ParameterStruct.ParamType.Integer;
                        break;
                    case ISEMPTY:
                        paramValue.setBooleanValue(paramValue.getStringValue().isEmpty());
                        strValue = paramValue.getBooleanValue().toString();
                        pType = ParameterStruct.ParamType.Boolean;
                        break;
                    case DOW:
                        intValue = LocalDate.now().getDayOfWeek().getValue();
                        strValue = intValue.toString();
                        paramValue.setIntegerValue((long)intValue);
                        paramValue.setStringValue(strValue);
                        pType = ParameterStruct.ParamType.Integer;
                        break;
                    case DOM:
                        intValue = LocalDate.now().getDayOfMonth();
                        strValue = intValue.toString();
                        paramValue.setIntegerValue((long)intValue);
                        paramValue.setStringValue(strValue);
                        pType = ParameterStruct.ParamType.Integer;
                        break;
                    case DOY:
                        intValue = LocalDate.now().getDayOfYear();
                        strValue = intValue.toString();
                        paramValue.setIntegerValue((long)intValue);
                        paramValue.setStringValue(strValue);
                        pType = ParameterStruct.ParamType.Integer;
                        break;
                    case MOY:
                        intValue = LocalDate.now().getMonthValue();
                        strValue = intValue.toString();
                        paramValue.setIntegerValue((long)intValue);
                        paramValue.setStringValue(strValue);
                        pType = ParameterStruct.ParamType.Integer;
                        break;
                    case DAY:
                        strValue = LocalDate.now().getDayOfWeek().toString();
                        paramValue.setStringValue(strValue);
                        pType = ParameterStruct.ParamType.String;
                        break;
                    case MONTH:
                        strValue = LocalDate.now().getMonth().toString();
                        paramValue.setStringValue(strValue);
                        pType = ParameterStruct.ParamType.String;
                        break;
                    default:
                        throw new ParserException(functionId + "Invalid trait " + trait.toString() + " for data type " + pType.toString());
                }
                GUILogPanel.outputInfoMsg(MsgType.VARS, "    " + name + "." + trait.toString() + " as type Boolean: " + strValue);
                break;
            default:
                throw new ParserException(functionId + "Invalid trait " + trait.toString() + " for data type " + pType.toString());
        }
        
        // save the parameter type
        paramValue.setParamTypeDiscrete (pType);
        return paramValue;
    }

}
