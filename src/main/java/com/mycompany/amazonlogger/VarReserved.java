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
    
    // the chars used to seperate entries in reporting variable contents to the client
    private static final String DATA_SEP = "::";
    
    // reserved static Variables
    private static final ArrayList<String> strResponse = new ArrayList<>(); // responses from RUN commands
    private static String  subRetValue;     // ret value from the last subroutine call
    private static boolean bStatus = false; // true/false status indications
    private static String  OcrText = "";    // the OCR data read
    private static String  curDirectory = ""; // the current directory value
    private static String  scriptName = ""; // the current running script base name
    
    private static long    maxRandom = 1000000000; // for random values 0 - 999999999

    
    public enum ReservedVars {
        RESPONSE,       // StrArray value from various commands
        RETVAL,         // String return value from subroutine call
        STATUS,         // Boolean return from various commands
        RANDOM,         // Integer random number output
        DATE,           // String current date (or Integer if Traits are added)
        TIME,           // String current time
        OCRTEXT,        // String output of OCRSCAN command
        CURDIR,         // String the current directory for file i/o
        SCRIPTNAME,     // String name of the current script that is running
    }
    
    /**
     * initializes the saved Variables
     */
    public static void initVariables () {
        strResponse.clear();
        subRetValue = "";
        bStatus = false;
        maxRandom = 1000000000;
    }
    
    /**
     * indicates if the name is reserved and can't be used for a variable.
     * 
     * @param name - the name to check
     * 
     * @return the reserved variable name if valid, null if not
     */
    public static ReservedVars isReservedName (String name) {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name != null) {
            for (ReservedVars entry : ReservedVars.values()) {
                if (entry.toString().contentEquals(name)) {
                    return entry;
                }
            }
        }
        return null;
    }

    /**
     * returns a list of the variables defined here.
     * 
     * @return a list of the defined variables
     */
    public static ArrayList<String> getVarAlloc() {
        ArrayList<String> response = new ArrayList<>();
        for (ReservedVars entry : ReservedVars.values()) {
            String name = entry.toString();
            response.add("[<name> " + name
                      + " " + DATA_SEP + " <type> " + getVariableTypeFromName(name).toString() + "]");
        }
        return response;
    }
    
    /**
     * sends the reserved variable info to the client.
     * 
     * @param varName - the name of the reserved parameter
     * @param varType - parameter type
     * @param value   - parameter value
     */
    private static void sendVarInfo (ReservedVars varName, ParameterStruct.ParamType varType, String value) {
        String curTime = UIFrame.elapsedTimerGet();
        if (curTime == null || curTime.isEmpty()) {
            curTime = "00:00.000";
        }
        if (varType == ParameterStruct.ParamType.String) {
            value = Utils.formatNetworkString(value);
        }
        String response = "[<section> RESERVED"
                        + " " + DATA_SEP + " <name> "   + varName
                        + " " + DATA_SEP + " <type> "   + varType
                        + " " + DATA_SEP + " <value> "  + value
                        + " " + DATA_SEP + " <writer> " + Subroutine.getSubName()
                        + " " + DATA_SEP + " <line> "   + Subroutine.getCurrentIndex()
                        + " " + DATA_SEP + " <time> "   + curTime + "]";

        // send info to client
        TCPServerThread.sendVarInfo(response);
    }
    
    /**
     * adds a String value to the $RESPONSE Variable
     * 
     * @param value - value to add to the response Variable
     */
    public static void putResponseValue (String value) {
        strResponse.add(value);
        sendVarInfo (ReservedVars.RESPONSE, ParameterStruct.ParamType.StrArray, value);
    }
    
    /**
     * adds an array of values to the $RESPONSE Variable
     * 
     * @param value - value to add to the response Variable
     */
    public static void putResponseValue (ArrayList<String> value) {
        strResponse.addAll(value);
        sendVarInfo (ReservedVars.RESPONSE, ParameterStruct.ParamType.StrArray, value.toString());
    }
    
    /**
     * set the value of the $STATUS Variable
     * 
     * @param value - value to set the result Variable to
     */
    public static void putStatusValue (Boolean value) {
        bStatus = value;
        sendVarInfo (ReservedVars.STATUS, ParameterStruct.ParamType.Boolean, value.toString());
    }

    /**
     * set the value of the $RETVAL Variable
     * 
     * @param value - value to set the subroutine return Variable to
     */
    public static void putSubRetValue (String value) {
        subRetValue = value;
        sendVarInfo (ReservedVars.RETVAL, ParameterStruct.ParamType.String, value);
    }

    /**
     * set the value of the $OCRTEXT Variable
     * 
     * @param value - value to set the subroutine return Variable to
     */
    public static void putOcrDataValue (String value) {
        OcrText = value;
        sendVarInfo (ReservedVars.OCRTEXT, ParameterStruct.ParamType.String, value);
    }
    
    /**
     * set the value of the $CURDIR Variable
     * 
     * @param value - value to set the subroutine return Variable to
     */
    public static void putCurDirValue (String value) {
        curDirectory = value;
        sendVarInfo (ReservedVars.CURDIR, ParameterStruct.ParamType.String, value);
    }
    
    /**
     * set the value of the $SCRIPTNAME Variable
     * 
     * @param value - value to set the subroutine return Variable to
     */
    public static void putScriptNameValue (String value) {
        scriptName = value;
        sendVarInfo (ReservedVars.SCRIPTNAME, ParameterStruct.ParamType.String, value);
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
     */
    public static void resetVar (String varName) {
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
     */
    public static boolean isArray (String varName) {
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
     */
    public static int getArraySize (String varName) {
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
     */    
    public static ParameterStruct.ParamType getVariableTypeFromName (String name) {
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
                case DATE:  // can also be Unsigned
                case TIME:
                case OCRTEXT:
                case CURDIR:
                case SCRIPTNAME:
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
     * @param varName    - reference variable name
     * @param pType      - data type of variable
     * 
     * @return the Variable value (null if not found)
     */
    public static ParameterStruct getVariableInfo (String varName, ParameterStruct.ParamType pType) {

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
            case RETVAL:
                paramValue.setStringValue(subRetValue);
                pType = ParameterStruct.ParamType.String;
                break;
            case RANDOM:
                Long value = getRandomValue();
                paramValue.setIntegerValue(value);
                pType = ParameterStruct.ParamType.Integer;
                sendVarInfo (reserved, ParameterStruct.ParamType.Integer, value.toString());
                break;
            case TIME:
                LocalTime currentTime = LocalTime.now();
                String strTime = currentTime.toString().substring(0,12);
                paramValue.setStringValue(strTime);
                pType = ParameterStruct.ParamType.String;
                sendVarInfo (reserved, ParameterStruct.ParamType.String, strTime);
                break;
            case DATE:
                LocalDate currentDate = LocalDate.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
                String strDate = currentDate.format(formatter);
                paramValue.setStringValue(strDate);
                sendVarInfo (reserved, ParameterStruct.ParamType.String, strDate);
                break;
            case OCRTEXT:
                paramValue.setStringValue(OcrText);
                break;
            case CURDIR:
                paramValue.setStringValue(curDirectory);
                break;
            case SCRIPTNAME:
                paramValue.setStringValue(scriptName);
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
                        iValue = Utils.getLongOrUnsignedValue(strResponse.getFirst());
                    }
                    break;
                case STATUS:
                    iValue = bStatus ? 1L : 0;
                    break;
                case RANDOM:
                    iValue = getRandomValue();
                    break;
                case RETVAL:
                    try {
                        iValue = Utils.getIntValue(subRetValue);
                    } catch (ParserException exMsg) {
                        iValue = 0L;
                    }
                    break;
                case DATE:
                    iValue = TraitInfo.getTraitIntValues(traitVal, name, ParameterStruct.ParamType.String);
                    break;
                default:
                    // these can't be converted to Integer
                    break;
                }
            }
        
        return iValue;
    }
    
}
