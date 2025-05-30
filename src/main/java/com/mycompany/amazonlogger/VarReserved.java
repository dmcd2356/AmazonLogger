/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

/**
 *
 * @author dan
 */
public class VarReserved {
    
    private static final String CLASS_NAME = VarReserved.class.getSimpleName();
    
    // reserved static Variables
    private static final ArrayList<String> strResponse = new ArrayList<>(); // responses from RUN commands
    private static String  subRetValue;     // ret value from the last subroutine call
    private static boolean bStatus = false; // true/false status indications
    private static long    maxRandom = 1000000000; // for random values 0 - 999999999

    
    public enum ReservedVars {
        RESPONSE,       // StrArray value from various commands
        RETVAL,         // String return value from subroutine call
        STATUS,         // Boolean return from various commands
        RANDOM,         // Integer random number output
        DATE,           // String current date (or Integer if Traits are added)
        TIME,           // String current time
        OCRTEXT,        // String output of OCRSCAN command
    }
    
    /**
     * indicates if the name is reserved and can't be used for a variable.
     * 
     * @param name - the name to check
     * 
     * @return the reserved variable name if valid, null if not
     * 
     * @throws ParserException
     */
    public static ReservedVars isReservedName (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null) {
            throw new ParserException(functionId + "Null input value");
        }
        for (ReservedVars entry : ReservedVars.values()) {
            if (entry.toString().contentEquals(name)) {
                return entry;
            }
        }
        return null;
    }
    
    /**
     * initializes the saved Variables
     */
    public static void initVariables () {
        bStatus = false;
        subRetValue = "";
        strResponse.clear();
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
     * set the value of the $RETVAL Variable
     * 
     * @param value - value to set the subroutine return Variable to
     */
    public static void putSubRetValue (String value) {
        subRetValue = value;
    }

    /**
     * set the max value for the $RANDOM variable.
     * (the max range for random value will be 0 to maxRandom - 1)
     * 
     * @param value - max value to use for random call
     */
    public static void setMaxRandom (Long value) {
        maxRandom = value;
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
     * resets the specified variable value.
     * 
     * @param varName - the reserved variable name
     * 
     * @throws ParserException 
     */
    public static void resetVar (String varName) throws ParserException {
        ReservedVars reserved = isReservedName (varName);
        if (reserved != null) {
            switch (reserved) {
                case RESPONSE:
                    strResponse.clear();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * determines if the specified variable is an array type.
     * 
     * @param varName - the reserved variable name
     * 
     * @return true if it is an array type
     * 
     * @throws ParserException 
     */
    public static boolean isArray (String varName) throws ParserException {
        ReservedVars reserved = isReservedName (varName);
        if (reserved != null) {
            switch (reserved) {
                case RESPONSE:
                    return true;
                default:
                    break;
            }
        }
        return false;
    }

    /**
     * gets the size of the specified array variable.
     * 
     * @param varName - the reserved variable name
     * 
     * @return the size if it is an array type, 0 if not
     * 
     * @throws ParserException 
     */
    public static int getArraySize (String varName) throws ParserException {
        ReservedVars reserved = isReservedName (varName);
        if (reserved != null) {
            switch (reserved) {
                case RESPONSE:
                    return strResponse.size();
                default:
                    break;
            }
        }
        return 0;
    }
    
    /**
     * determines the type of Variable by searching for the name.
     * 
     * @param name - name of the Variable
     * 
     * @return the corresponding Variable type (null if not a reserved var)
     * 
     * @throws ParserException
     */    
    public static ParameterStruct.ParamType getVariableTypeFromName (String name) throws ParserException {
        ParameterStruct.ParamType vartype = null;
        ReservedVars reserved = isReservedName (name);
        if (reserved != null) {
            switch (reserved) {
                case RESPONSE:
                    vartype = ParameterStruct.ParamType.StrArray;
                    break;
                case STATUS:
                    vartype = ParameterStruct.ParamType.Boolean;
                    break;
                case RANDOM:
                    vartype = ParameterStruct.ParamType.Unsigned;
                    break;
                case RETVAL:
                    vartype = ParameterStruct.ParamType.String;
                    break;
                case DATE:
                    vartype = ParameterStruct.ParamType.String;  // can also be Unsigned
                    break;
                case TIME:
                    vartype = ParameterStruct.ParamType.String;
                    break;
                case OCRTEXT:
                    vartype = ParameterStruct.ParamType.String;
                    break;
                default:
                    break;
            }
        }
        return vartype;
    }
    
    /**
     * returns the value of a RESERVED reference Variable along with its data type.
     * 
     * This is only performed during the Execution stage when evaluating the value
     *  of the reference variables just prior to executing each command.
     * It is only at this point where we can do the run-time evaluation, just
     *  prior to executing the command.
     * 
     * @param paramInfo  - Variable reference information
     * @param varName    - reference variable name
     * @param pType      - data type of variable
     * 
     * @return the Variable value (null if not found)
     * 
     * @throws ParserException - if Variable not found
     */
    public static ParameterStruct getVariableInfo (VarExtensions paramInfo, String varName, ParameterStruct.ParamType pType) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // create a new parameter with all null entries
        ParameterStruct paramValue = new ParameterStruct();

        ReservedVars reserved = isReservedName (varName);
        if (reserved == null) {
            return null;
        }
        switch (reserved) {
            case RESPONSE:
                paramValue.setStrArray(strResponse);
                pType = ParameterStruct.ParamType.StrArray;
                break;
            case STATUS:
                paramValue.setBooleanValue(bStatus);
                pType = ParameterStruct.ParamType.Boolean;
                break;
            case RANDOM:
                paramValue.setIntegerValue(getRandomValue());
                pType = ParameterStruct.ParamType.Integer;
                break;
            case RETVAL:
                paramValue.setStringValue(subRetValue);
                pType = ParameterStruct.ParamType.String;
                break;
            case TIME:
                LocalTime currentTime = LocalTime.now();
                paramValue.setStringValue(currentTime.toString().substring(0,12));
                pType = ParameterStruct.ParamType.String;
                break;
            case DATE:
                LocalDate currentDate = LocalDate.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
                String strDate = currentDate.format(formatter);
                paramValue.setStringValue(strDate);
                break;
            case OCRTEXT:
                String ocrText = OCRReader.getContent();
                paramValue.setStringValue(ocrText);
                break;
            default:
                paramValue = null;
                break;
        }
        
        return paramValue;
    }

    /**
     * determines if a Variable has been found with the specified name.
     * 
     * @param name     - name of the Variable to search for
     * @param traitVal - the trait associated with the variable (null if none)
     * 
     * @return type of Variable if found, null if not found
     * 
     * @throws ParserException
     */
    public static Long getNumericValue (String name, TraitInfo.Trait traitVal) throws ParserException {
        Long iValue = null;
        ReservedVars reserved = isReservedName (name);
        if (reserved != null) {
            switch (reserved) {
                case RESPONSE:
                    if (! strResponse.isEmpty()) {
                        iValue = ParameterStruct.getLongOrUnsignedValue(strResponse.getFirst());
                    }
                    break;
                case STATUS:
                    iValue = bStatus ? 1L : 0;
                    break;
                case RANDOM:
                    iValue = getRandomValue();
                    break;
                case RETVAL:
                    iValue = Utils.getIntValue(subRetValue);
                    break;
                case DATE:
                    iValue = TraitInfo.getTraitIntValues(traitVal, name, ParameterStruct.ParamType.String);
                    break;
                default:
                case TIME:
                case OCRTEXT:
                    // these can't be converted to Integer
                    break;
                }
            }
        
        return iValue;
    }
    
}
