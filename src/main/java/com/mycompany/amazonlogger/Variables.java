/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_VARS;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    
    // the value returned from the last subroutine call
    private static String subRetValue;

    // table to keep track of variable access in functions
    private static final ArrayList<AccessInfo> varAccess = new ArrayList<>();

    private class AccessInfo {
        String      subName;            // name of function that allocated the variable
        String      varName;            // name of the variable owned by subroutine
        ParameterStruct.ParamType type; // parameter type
        AccessType  access;             // type of access permitted to variable
        
        AccessInfo (String subName, String varName, ParameterStruct.ParamType type, AccessType access) {
            this.subName  = subName;
            this.varName  = varName;
            this.type     = type;
            this.access   = access;
        }
    }

    /**
     * NOTE.
     * For MAIN allocations, external access is for all subroutines.
     * For subroutine allocations, it only applies to subroutines called by it,
     *  since it deletes all allocations upon exit.
     * By 'local', it means the function (or MAIN) that created it.
     */
    public enum AccessType {
        GLOBAL,     // global access for Read & Write (default)
        READONLY,   // only only local function can Write, local & called functions can Read
        LOCAL,      // only local function can R/W
     }
    
    public enum ReservedVars {
        RESPONSE,       // StrArray value from various commands
        RETVAL,         // String return value from subroutine call
        STATUS,         // Boolean return from various commands
        RANDOM,         // Integer random number output
        DATE,           // String current date (or Integer if Traits are added)
        TIME,           // String current time
        OCRTEXT,        // String output of OCRSCAN command
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
        ALLOCATE,       // defining a Variable or loop parameter
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
     * checks whether specified variable has Read permission for current function.
     * 
     * @param varName - name of variable to check
     * @param progIx  - current command index (indicates whether command is in Main or Subroutine)
     * 
     * @throws ParserException 
     */
    public static void checkReadAccess (String varName, int progIx) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // check if already defined
        for (AccessInfo entry : varAccess) {
            if (entry.varName.contentEquals(varName)) {
                switch (entry.access) {
                    default:
                    case GLOBAL:
                    case READONLY:
                        return;
                    case LOCAL:
                        String curSub = Subroutine.getSubName(progIx);
                        if (! entry.subName.contentEquals(curSub)) {
                            throw new ParserException(functionId + "Read access restricted on " + entry.access + " Variable: " + varName);
                        }
                        break;
                }
                return;
            }
        }
        // if variable not found, do nothing. It may be a reserved variable
        // which would have GLOBAL access.
    }
    
    /**
     * checks whether specified variable has Write permission for current function.
     * 
     * @param varName - name of variable to check
     * @param progIx  - current command index (indicates whether command is in Main or Subroutine)
     * 
     * @throws ParserException 
     */
    public static void checkWriteAccess (String varName, int progIx) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // check if already defined
        for (AccessInfo entry : varAccess) {
            if (entry.varName.contentEquals(varName)) {
                switch (entry.access) {
                    default:
                    case GLOBAL:
                        return;
                    case READONLY:
                    case LOCAL:
                        String curSub = Subroutine.getSubName(progIx);
                        if (! entry.subName.contentEquals(curSub)) {
                            throw new ParserException(functionId + "Write access restricted on " + entry.access + " Variable: " + varName);
                        }
                        break;
                }
                return;
            }
        }
        // if variable not found, do nothing. It may be a reserved variable
        // which would have GLOBAL access.
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
     * creates a new entry in the Variable table and sets the initial value.
     * 
     * @param dataType - the data type to allocate
     * @param varName  - Variable name
     * @param subName  - the function the variable is defined in
     * @param access   - type of access to grant variable
     * 
     * @throws ParserException - if Variable was already defined
     */
    public void allocateVariable (String dataType, String varName, String subName, AccessType access) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // first, verify Variable name to make sure it is valid format and
        //  not already used.
        if (dataType == null || varName == null) {
            throw new ParserException(functionId + "Null input value");
        }
        if (subName == null) {
            subName = "*MAIN*";
        }
        try {
            checkValidVariable(VarCheck.ALLOCATE, varName);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }

        ParameterStruct.ParamType ptype = ParameterStruct.checkParamType (dataType);
        if (ptype == null) {
            throw new ParserException(functionId + "Invalid variable type: " + dataType);
        }
        
        // check if already defined
        for (AccessInfo entry : varAccess) {
            if (entry.varName.contentEquals(varName)) {
                throw new ParserException(functionId + "Variable already defined: " + varName);
            }
        }
        
        // add to list of variable access
        AccessInfo entry = new AccessInfo(subName, varName, ptype, access);
        varAccess.add(entry);
        
        // allocate and set default value for the variable
        switch (ptype) {
            case Integer:
                longParams.put(varName, 0L);
                break;
            case Unsigned:
                uintParams.put(varName, 0L);
                break;
            case Boolean:
                boolParams.put(varName, false);
                break;
            case String:
                strParams.put(varName, "");
                break;
            case IntArray:
            case StrArray:
                VarArray.allocateVariable(varName, ptype);
                break;
            default:
                throw new ParserException(functionId + "Invalid variable type: " + dataType);
        }
        frame.outputInfoMsg(STATUS_VARS, "   - Allocated " + dataType + " variable: " + varName + " in " + subName + " with access: " + access);
    }

        /**
     * releases the allocations for the specified subroutine.
     * 
     * @param subName  - the function exiting
     * 
     * @throws ParserException - if Variable was already defined
     */
    public static void releaseSubVariables (String subName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (subName == null || subName.contentEquals("*MAIN*")) {
            throw new ParserException(functionId + "Variables allocated by MAIN can't be deleted");
        }
        for (AccessInfo entry : varAccess) {
            if (entry.subName.contentEquals(subName)) {
                ParameterStruct.ParamType ptype = entry.type;
                String varName = entry.varName;
                switch (ptype) {
                    case Integer:
                        longParams.remove(varName);
                        break;
                    case Unsigned:
                        uintParams.remove(varName);
                        break;
                    case Boolean:
                        boolParams.remove(varName);
                        break;
                    case String:
                        strParams.remove(varName);
                        break;
                    case IntArray:
                    case StrArray:
                        VarArray.releaseVariable(varName, ptype);
                        break;
                    default:
                        throw new ParserException(functionId + "Invalid variable type: " + ptype);
                }
                frame.outputInfoMsg(STATUS_VARS, "   - Released " + subName + " allocation for: " + varName + " (type " + ptype + ")");
            }
        }
    }

    /**
     * returns the length of the specified string variable
     * 
     * @param name - name of the variable
     * 
     * @return the string length (0 if not found)
     */
    public static int getStringSize (String name) {
        String strValue = strParams.get(name);
        if (strValue != null) {
            return strValue.length();
        }
        return 0;
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
     * extracts and applies the Bracketing found for the parameter value.
     * Does nothing if no Brackets listed. Checks to verify the Brackets are applicable to
     *  the data type of the parameter.
     * 
     * @param paramValue - the parameter value to check the Trait of
     * 
     * @return the parameter value modified by the Bracketing
     * 
     * @throws ParserException 
     */
    private static ParameterStruct applyBracketing (ParameterStruct paramValue) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        VariableInfo paramInfo = paramValue.getVariableRef();
        if (paramInfo == null) {
            return paramValue;
        }
        String name = paramInfo.getName();
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }

        ParameterStruct.ParamType pType = paramValue.getParamType();
        
        // check for brackets being applied (this will change the type for Arrays)
        if (paramInfo.getIndexStart() != null) {
            int iStart = paramInfo.getIndexStart();
            int iEnd = iStart;
            String ixRange = "[" + iStart + "]";
            if(paramInfo.getIndexEnd() != null) {
                iEnd = paramInfo.getIndexEnd();
                if (iEnd > iStart) {
                    ixRange = "[" + iStart + "-" + iEnd + "]";
                }
            }
            switch (pType) {
                case ParameterStruct.ParamType.IntArray:
                    if (iStart >= 0 && iEnd < paramValue.getIntArraySize()) {
                        if (iEnd == iStart) {
                            paramValue.setIntegerValue(paramValue.getIntArrayElement(iStart));
                            paramValue.setStringValue(paramValue.getIntegerValue().toString());
                            pType = ParameterStruct.ParamType.Integer;
                        } else {
                            int count = paramValue.getIntArraySize() - iEnd - 1;
                            for (int ix = 0; ix < count; ix++) {
                                paramValue.getIntArray().remove(iEnd + 1);
                            }
                            for (int ix = 0; ix < iStart; ix++) {
                                paramValue.getIntArray().remove(0);
                            }
                            pType = ParameterStruct.ParamType.IntArray;
                        }
                        frame.outputInfoMsg(STATUS_VARS, "    " + name + "[" + iStart + "] as type " + pType + ": " + paramValue.getIntegerValue());
                    } else {
                        throw new ParserException(functionId + "Parameter " + name + " index " + iStart + " exceeds array");
                    }
                    break;
                case ParameterStruct.ParamType.StrArray:
                    if (iStart >= 0 && iEnd < paramValue.getStrArraySize()) {
                        if (iEnd == iStart) {
                            paramValue.setStringValue(paramValue.getStrArrayElement(iStart));
                            pType = ParameterStruct.ParamType.String;
                        } else {
                            int count = paramValue.getStrArraySize() - iEnd - 1;
                            for (int ix = 0; ix < count; ix++) {
                                paramValue.getStrArray().remove(iEnd + 1);
                            }
                            for (int ix = 0; ix < iStart; ix++) {
                                paramValue.getStrArray().remove(0);
                            }
                            pType = ParameterStruct.ParamType.StrArray;
                        }
                        frame.outputInfoMsg(STATUS_VARS, "    " + name + "[" + iStart + "] as type " + pType + ": " + paramValue.getStringValue());
                    } else {
                        throw new ParserException(functionId + "Parameter " + name + " index " + iStart + " exceeds array");
                    }
                    break;
                case ParameterStruct.ParamType.String:
                    if (iStart >= 0 && iEnd < paramValue.getStrArraySize()) {
                        paramValue.setStringValue(paramValue.getStringValue().substring(iStart, iEnd + 1));
                        frame.outputInfoMsg(STATUS_VARS, "    " + name + "index" + ixRange + " as type " + pType + ": " + paramValue.getStringValue());
                    } else {
                        throw new ParserException(functionId + "Parameter " + name + " index" + ixRange + " exceeds array");
                    }
                    break;
                default:
                    break;
            }
        }

        // save the parameter type (in case it changed)
        paramValue.setParamTypeDiscrete (pType);
        return paramValue;
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
        Long varValue;
        switch (getVariableClass (name)) {
            // check the reserved params list
            case RESERVED:
                ReservedVars reserved = isReservedName (name);
                switch (reserved) {
                    case RESPONSE:
                        paramValue.setStrArray(VarArray.getResponseValue());
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
                    pType = ParameterStruct.ParamType.IntArray;
                } else {
                    ArrayList<String> strList = VarArray.getStrArray(name);
                    paramValue.setStrArray(strList);
                    arrayValue = strList.toString();
                    if (arrayValue.length() > 100) {
                        int length = arrayValue.length();
                        arrayValue = arrayValue.substring(0,60) + "..." + arrayValue.substring(length-30);
                    }
                    pType = ParameterStruct.ParamType.StrArray;
                }
                frame.outputInfoMsg(STATUS_VARS, "    - Lookup Ref '" + name + "' as type " + pType + ": " + arrayValue);
                break;
            case UNKNOWN:
                throw new ParserException(functionId + "    - Variable Ref '" + name + "' was not found in any Variable database");
        }

        // save the parameter type of the base parameter
        paramValue.setParamTypeDiscrete (pType);

        // now check for brackets being applied (this may change the type for Arrays)
        paramValue = applyBracketing(paramValue);

        // now check for traits being applied (this can also change the data type returned)
        paramValue = TraitInfo.applyTraitValue (paramValue);
        
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

        if (name == null || value == null) {
            throw new ParserException(functionId + "Null input value");
        }
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

        if (name == null || value == null) {
            throw new ParserException(functionId + "Null input value");
        }
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

        if (name == null || value == null) {
            throw new ParserException(functionId + "Null input value");
        }
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

        if (name == null || value == null) {
            throw new ParserException(functionId + "Null input value");
        }
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
     * 
     * @throws ParserException
     */
    public static boolean variableDelete (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null) {
            throw new ParserException(functionId + "Null input value");
        }
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
            VarArray.removeArrayEntry(name, ParameterStruct.ParamType.StrArray);
            frame.outputInfoMsg(STATUS_VARS, "   - Deleted StrArray Variable: " + name);
        }
        return false;
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
    public static AccessType getAccessType (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null) {
            throw new ParserException(functionId + "Null input value");
        }
        for (AccessType entry : AccessType.values()) {
            if (entry.toString().contentEquals(name)) {
                return entry;
            }
        }
        return null;
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
    private static ReservedVars isReservedName (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null) {
            throw new ParserException(functionId + "Null input value");
        }
        for (ReservedVars entry : ReservedVars.values()) {
            if (entry.toString().contentEquals(name)) {
//                frame.outputInfoMsg(STATUS_DEBUG, "    Reserved name found: " + name);
                return entry;
            }
        }
        return null;
    }
    
    /**
     * this returns the type of variable found.
     * 
     * @param name - name of the variable
     * 
     * @return type of variable (UNKNOWN if not found)
     * 
     * @throws ParserException
     */
    public static VarClass getVariableClass (String name) throws ParserException {
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
        if (isReservedName (name) != null)   return VarClass.RESERVED;
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
     * determines the type of Variable by searching for the name.
     * 
     * @param name - name of the Variable
     * 
     * @return the corresponding Variable type
     * 
     * @throws ParserException
     */    
    public static ParameterStruct.ParamType getVariableTypeFromName (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (name == null) {
            throw new ParserException(functionId + "Null input value");
        }
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }
        int offTrait = name.indexOf('.');
        int offBrack = name.indexOf('[');
        if (offTrait > 0 && (offBrack > offTrait || offBrack < 0))
            name = name.substring(0, offTrait);
        else if (offBrack > 0 && (offTrait > offBrack || offTrait < 0))
            name = name.substring(0, offBrack);

        // this is the default value
        ParameterStruct.ParamType vartype = null;
        ReservedVars reserved = isReservedName (name);
        if (reserved != null) {
            switch (reserved) {
                case RESPONSE:
                    vartype = ParameterStruct.ParamType.StrArray;
                    break;
                case STATUS:
                    vartype = ParameterStruct.ParamType.Integer;
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
        else if (LoopStruct.getCurrentLoopValue(name) != null) {
            vartype = ParameterStruct.ParamType.Integer;
        } else if (longParams.containsKey(name)) {
            vartype = ParameterStruct.ParamType.Integer;
        } else if (uintParams.containsKey(name)) {
            vartype = ParameterStruct.ParamType.Unsigned;
        } else if (boolParams.containsKey(name)) {
            vartype = ParameterStruct.ParamType.Boolean;
        } else if (strParams.containsKey(name)) {
            vartype = ParameterStruct.ParamType.String;
        } else if (VarArray.isIntArray(name)) {
            vartype = ParameterStruct.ParamType.IntArray;
        } else if (VarArray.isStrArray(name)) {
            vartype = ParameterStruct.ParamType.StrArray;
        }
        if (vartype == null) {
            throw new ParserException(functionId + "Variable entry not found: " + name);
        }
        return vartype;
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
    public static Long getNumericValue (String name, TraitInfo.Trait traitVal, boolean bNoLoops) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (name == null) {
            throw new ParserException(functionId + "Null input value");
        }
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }
        Long iValue = null;
        try {
            ReservedVars reserved = isReservedName (name);
            Integer loopVal = LoopStruct.getCurrentLoopValue(name);
            if (loopVal != null) {
                iValue = (long) loopVal;
                if (bNoLoops) {
                    return null; // value is a loop var, but is not allowed
                }
            } else if (reserved != null) {
                switch (reserved) {
                    case RESPONSE:
                        String strValue = VarArray.getResponseValue().getFirst();
                        iValue = ParameterStruct.getLongOrUnsignedValue(strValue);
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
                        LocalDate currentDate = LocalDate.now();
                        iValue = TraitInfo.getTraitIntValues(traitVal, name, ParameterStruct.ParamType.String);
                        break;
                    default:
                    case TIME:
                    case OCRTEXT:
                        // these can't be converted to Integer
                        break;
                }
            }
            else if (longParams.containsKey(name)) {
                iValue = longParams.get(name);
            }
            else if (uintParams.containsKey(name)) {
                iValue = uintParams.get(name);
            }
            else if (strParams.containsKey(name)) {
                // first, check for a Trait
                iValue = TraitInfo.getTraitIntValues(traitVal, name, ParameterStruct.ParamType.String);
                if (iValue == null) {
                    String strValue = strParams.get(name);
                    iValue = ParameterStruct.getLongOrUnsignedValue(strValue);
                    frame.outputInfoMsg(STATUS_DEBUG, "Converting variable " + name + " to Integer: " + iValue);
                }
            }
            else if (boolParams.containsKey(name)) {
                frame.outputInfoMsg(STATUS_DEBUG, "Converting from Boolean to Integer: " + name);
                iValue = boolParams.get(name) ? 1L : 0;
            }
            else if (VarArray.isIntArray(name)) {
                // first, check for a Trait
                iValue = TraitInfo.getTraitIntValues(traitVal, name, ParameterStruct.ParamType.IntArray);
                if (iValue == null) {
                    iValue = VarArray.getIntArray(name).getFirst();
                    frame.outputInfoMsg(STATUS_DEBUG, "Extracted 1st entry of IntArray " + name + " to Integer: " + iValue);
                }
            }
            else if (VarArray.isStrArray(name)) {
                // first, check for a Trait
                iValue = TraitInfo.getTraitIntValues(traitVal, name, ParameterStruct.ParamType.StrArray);
                if (iValue == null) {
                    String strValue = VarArray.getStrArray(name).getFirst();
                    iValue = ParameterStruct.getLongOrUnsignedValue(strValue);
                    frame.outputInfoMsg(STATUS_DEBUG, "Extracted 1st entry of StrArray " + name + " to Integer: " + iValue);
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
     * @throws ParserException - if not valid
     */
    public static void checkValidVariable (VarCheck use, String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || use == null) {
            throw new ParserException(functionId + "Null input value");
        }
        try {
            if (name.startsWith("$") && use == VarCheck.REFERENCE) {
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
                case ALLOCATE:
                    switch (varClass) {
                        case UNKNOWN  -> { // not found in any group is good
                        }
                        case LOOP     -> errMsg = "Using Loop Variable name: " + name;
                        case RESERVED -> errMsg = "Using Reserved command name: " + name;
                        default       -> errMsg = "Variable name already in use: " + name;
                    }
                    break;

                case SET:
                    // you can only set user variables plus this one reserved variable
                    if (name.contentEquals("RANDOM")) {
                        break;
                    }
                    switch (varClass) {
                        case LOOP     -> errMsg = "Using Loop Variable name: " + name;
                        case RESERVED -> errMsg = "Using Reserved command name: " + name;
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
     */
    public static boolean verifyVariableFormat (String name) {
        if (name == null || name.isBlank()) {
            frame.outputInfoMsg(STATUS_DEBUG, "Variable name is null");
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
