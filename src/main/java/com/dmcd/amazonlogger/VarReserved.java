/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

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
    private static final String DATA_SEP = Variables.getDataSeparator();
    
    // reserved static Variables
    private static final ArrayList<String> strResponse = new ArrayList<>(); // responses from RUN commands
    private static String  subRetValue;     // ret value from the last subroutine call
    private static boolean bStatus = false; // true/false status indications
    private static String  OcrText = "";    // the OCR data read
    private static String  curDirectory = ""; // the current directory value
    private static String  scriptName = ""; // the current running script base name
    private static final ArrayList<VarInfo> varInfo = new ArrayList<>();
    
    private static long    maxRandom = 1000000000; // for random values 0 - 999999999

    // The list of Reserved variables
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
     * THIS CLASS KEEPS TRACK OF EACH RESERVED VARIABLE FOR PASSING INFORMATION 
     * BACK TO THE CLIENT WHEN RUNNING IN NETWORK MODE.
     */
    private class VarInfo {
        boolean   bUpdate;      // true when a value has been written to
        ReservedVars name;      // name of variable
        String    type;         // data type
        String    value;        // value saved as a String
        String    writer;       // subroutine that last wrote to it
        String    line;         // script line that wrote to it
        String    time;         // time it was written
        
        VarInfo (ReservedVars name) {
            String dataType;
            String val = "";
            switch (name) {
                case STATUS:
                    dataType = "Boolean";
                    val = "false";
                    break;
                case RANDOM:
                    dataType = "Integer";
                    val = "0";
                    break;
                case RESPONSE:
                    dataType = "StrArray";
                    val = "[]";
                    break;
                default:
                    dataType = "String";
                    break;
            }
            this.bUpdate = false;
            this.name   = name;
            this.type   = dataType;
            this.value  = val;
            this.writer = "";
            this.line   = "";
            this.time   = "";
        }
        
        public void reset() {
            this.bUpdate = false;
        }
        
        public void init() {
            switch (this.name) {
                case STATUS:
                    this.type = "Boolean";
                    this.value = "false";
                    break;
                case RANDOM:
                    this.type = "Integer";
                    this.value = "0";
                    break;
                case RESPONSE:
                    this.type = "StrArray";
                    this.value = "[]";
                    break;
                default:
                    this.type = "String";
                    this.value = "";
                    break;
            }
        }
        
        public void setValue (String value) {
            String curTime = GUIMain.elapsedTimerGet();
            if (curTime == null || curTime.isEmpty()) {
                curTime = "00:00.000";
            }
            if (this.type.contentEquals("String")) {
                value = Utils.formatNetworkString(value);
            }
            Integer lineNum = ScriptCompile.getLineNumber(Subroutine.getCurrentIndex());

            this.bUpdate = true;
            this.value   = value;
            this.writer  = Subroutine.getSubName();
            this.line    = lineNum.toString();
            this.time    = curTime;
        }
        
        public void sendVarInfo() {
            if (this.bUpdate) {
                String showValue = this.value;
                if (showValue == null) {
                    switch (this.type) {
                        case "String":
                            showValue = "\"\"";
                            break;
                        case "StrArray":
                        case "IntArray":
                            showValue = "[]";
                            break;
                        default:
                            break;
                    }
                }
                if (this.type.contentEquals("String") && this.value.isEmpty()) {
                    showValue = "\"\"";
                }
                String response = "[<section> RESERVED"
                        + " " + DATA_SEP + " <name> "   + this.name.toString()
                        + " " + DATA_SEP + " <type> "   + this.type
                        + " " + DATA_SEP + " <value> "  + showValue
                        + " " + DATA_SEP + " <writer> " + this.writer
                        + " " + DATA_SEP + " <line> "   + this.line
                        + " " + DATA_SEP + " <time> "   + this.time + "]";

                // send info to client
                TCPServerThread.sendVarInfo(response);
            }
        }
    }

    VarReserved() {
        // init the VarInfo table
        for (ReservedVars varEnum : ReservedVars.values()) {
            varInfo.add (new VarInfo(varEnum));
        }
    }
    
    /**
     * initializes the saved Variables.
     * This is done at the start of each run to reset all values to their initial
     *   settings, as well as clearing the flag that indicates values have changed.
     */
    public static void initVariables () {
        strResponse.clear();
        subRetValue = "";
        bStatus = false;
        maxRandom = 1000000000;
        
        for (int ix = 0; ix < varInfo.size(); ix++) {
            varInfo.get(ix).init();
            varInfo.get(ix).reset();
        }
    }
    
    /**
     * resets the changed status of the variables.
     * this is done at the start of RESUME and STEP so we know what values have changed
     */
    public static void resetUpdate () {
        for (int ix = 0; ix < varInfo.size(); ix++) {
            varInfo.get(ix).reset();
        }
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
     * This should be called when the RUN or STEP action is completed in NETWORK mode
     */
    public static void sendVarChange () {
        if (! AmazonReader.isOpModeNetwork()) {
            return;
        }
        for (int ix = 0; ix < varInfo.size(); ix++) {
            VarInfo info = varInfo.get(ix);
            info.sendVarInfo();
        }
    }
    
    /**
     * updates the reserved variable value.
     * This is only for NETWORK mode
     * 
     * @param varName - the name of the reserved parameter
     * @param value   - parameter value
     */
    private static void setVarChange (ReservedVars varName, String value) {
        if (! AmazonReader.isOpModeNetwork()) {
            return;
        }
        for (int ix = 0; ix < varInfo.size(); ix++) {
            VarInfo info = varInfo.get(ix);
            if (info.name == varName) {
                info.setValue(value);
                break;
            }
        }
    }
    
    /**
     * indicates if the name is reserved and can't be used for a variable.
     * 
     * @param name - the name to check
     * 
     * @return the reserved variable name if valid, null if not
     */
    public static ReservedVars isReservedName (String name) {
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
     * adds a String value to the $RESPONSE Variable
     * 
     * @param value - value to add to the response Variable
     */
    public static void putResponseValue (String value) {
        strResponse.add(value);
        setVarChange (ReservedVars.RESPONSE, strResponse.toString());
    }
    
    /**
     * adds an array of values to the $RESPONSE Variable
     * 
     * @param value - value to add to the response Variable
     */
    public static void putResponseValue (ArrayList<String> value) {
        strResponse.addAll(value);
        setVarChange (ReservedVars.RESPONSE, value.toString());
    }
    
    /**
     * set the value of the $STATUS Variable
     * 
     * @param value - value to set the result Variable to
     */
    public static void putStatusValue (Boolean value) {
        bStatus = value;
        setVarChange (ReservedVars.STATUS, value.toString());
    }

    /**
     * set the value of the $RETVAL Variable
     * 
     * @param value - value to set the subroutine return Variable to
     */
    public static void putSubRetValue (String value) {
        subRetValue = value;
        setVarChange (ReservedVars.RETVAL, value);
    }

    /**
     * set the value of the $OCRTEXT Variable
     * 
     * @param value - value to set the subroutine return Variable to
     */
    public static void putOcrDataValue (String value) {
        OcrText = value;
        setVarChange (ReservedVars.OCRTEXT, value);
    }
    
    /**
     * set the value of the $CURDIR Variable
     * 
     * @param value - value to set the subroutine return Variable to
     */
    public static void putCurDirValue (String value) {
        curDirectory = value;
        setVarChange (ReservedVars.CURDIR, value);
    }
    
    /**
     * set the value of the $SCRIPTNAME Variable
     * 
     * @param value - value to set the subroutine return Variable to
     */
    public static void putScriptNameValue (String value) {
        scriptName = value;
        setVarChange (ReservedVars.SCRIPTNAME, value);
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
                    setVarChange (reserved, null);
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
                setVarChange (reserved, value.toString());
                break;
            case TIME:
                LocalTime currentTime = LocalTime.now();
                String strTime = currentTime.toString().substring(0,12);
                paramValue.setStringValue(strTime);
                pType = ParameterStruct.ParamType.String;
                setVarChange (reserved, strTime);
                break;
            case DATE:
                LocalDate currentDate = LocalDate.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
                String strDate = currentDate.format(formatter);
                paramValue.setStringValue(strDate);
                setVarChange (reserved, strDate);
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
