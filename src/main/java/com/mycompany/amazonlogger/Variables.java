/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_VARS;
import java.util.ArrayList;

/**
 *
 * @author dan
 */
public class Variables {
    
    private static final String CLASS_NAME = Variables.class.getSimpleName();
    private static final String INDENT = "     ";
    
    static final int NAME_MAXLEN = 20;  // the max # chars in a param name

            
    public final VarGlobal   varGlobal   = new VarGlobal();
    public final VarLocal    varLocal    = new VarLocal();
    public final VarArray    varArray    = new VarArray();


    public enum AccessType {
        GLOBAL,     // global access for Read & Write (default)
        LOCAL,      // only local function can R/W
//        READONLY,   // only only local function can Write, local & called functions can Read
     }
    
    public enum VarClass {
        UNKNOWN,            // not a valid variable
        GLOBAL,             // a global user defined variable
        LOCAL,              // a local  user defined variable
        LOOP,               // a Loop     variable
        RESERVED,           // a Reserved variable
    }

    public enum VarCheck {
        ALLOCATE,       // defining a Variable or loop parameter
        SET,            // setting a standard Variable value (left side of =)
        REFERENCE,      // referencing a Variable (standard, reserved or loop)
    }

    Variables () {
    }
    
    /**
     * initializes the saved Variables
     */
    public static void initVariables () {
        VarReserved.initVariables();
        VarGlobal.initVariables();
        VarArray.initVariables();
        LoopParam.initVariables();
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
    public void modifyStringVariable (String name, String value) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                varLocal.putString(name, value);
                break;
            default:
            case GLOBAL:
                VarGlobal.putStringVariable(name, value);
                break;
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
    public void modifyIntegerVariable (String name, Long value) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                varLocal.putInteger(name, value);
                break;
            default:
            case GLOBAL:
                VarGlobal.putIntegerVariable(name, value);
                break;
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
    public void modifyUnsignedVariable (String name, Long value) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                varLocal.putUnsigned(name, value);
                break;
            default:
            case GLOBAL:
                VarGlobal.putUnsignedVariable(name, value);
                break;
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
    public void modifyBooleanVariable (String name, Boolean value) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                varLocal.putBoolean(name, value);
                break;
            default:
            case GLOBAL:
                VarGlobal.putBooleanVariable(name, value);
                break;
        }
    }

    public String getStringValue (String name) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                return varLocal.getString(name);
            default:
            case GLOBAL:
                return VarGlobal.getStringVariable(name);
        }
    }
    
    public Long getIntegerValue (String name) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                return varLocal.getInteger(name);
            default:
            case GLOBAL:
                return VarGlobal.getIntegerVariable(name);
        }
    }
    
    public Long getUnsignedValue (String name) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                return varLocal.getUnsigned(name);
            default:
            case GLOBAL:
                return VarGlobal.getUnsignedVariable(name);
        }
    }
    
    public Boolean getBooleanValue (String name) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                return varLocal.getBoolean(name);
            default:
            case GLOBAL:
                return VarGlobal.getBooleanVariable(name);
        }
    }
    
    public void allocStrArray (String name) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                varLocal.allocVar(name, ParameterStruct.ParamType.StrArray);
                break;
            default:
            case GLOBAL:
                varArray.allocStrArray(name);
                break;
        }
    }
    
    public void allocIntArray (String name) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                varLocal.allocVar(name, ParameterStruct.ParamType.IntArray);
                break;
            default:
            case GLOBAL:
                varArray.allocIntArray(name);
                break;
        }
    }

    public void updateStrArray (String name, ArrayList<String> value) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                varLocal.updateStrArray(name, value);
                break;
            default:
            case GLOBAL:
                varArray.updateStrArray(name, value);
                break;
        }
    }
    
    public void updateIntArray (String name, ArrayList<Long> value) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                varLocal.updateIntArray(name, value);
                break;
            default:
            case GLOBAL:
                varArray.updateIntArray(name, value);
                break;
        }
    }

    public boolean isIntArray (String name) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                return varLocal.isIntArray(name);
            default:
            case GLOBAL:
                return varArray.isIntArray(name);
        }
    }
    
    public boolean isStrArray (String name) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                return varLocal.isStrArray(name);
            default:
            case GLOBAL:
                return varArray.isStrArray(name);
        }
    }
    
    public ArrayList<String> getStrArray (String name) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                return varLocal.getStrArray(name);
            default:
            case GLOBAL:
                return varArray.getStrArray(name);
        }
    }
    
    public ArrayList<Long> getIntArray (String name) throws ParserException {
        switch (getVariableClass (name)) {
            case LOCAL:
                return varLocal.getIntArray(name);
            default:
            case GLOBAL:
                return varArray.getIntArray(name);
        }
    }

    /**
     * checks whether specified variable has Read permission for current function.
     * 
     * @param varName - name of variable to check
     * 
     * @throws ParserException 
     */
    public void checkReadAccess (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // MAIN function variables are GLOBAL, so read from anywhere
        if (Subroutine.isMainFunction()) {
            return;
        }
        
        String curSub = Subroutine.getSubName();
        boolean bExists;
        bExists = varGlobal.isDefined(varName);
        if (bExists) {
            return;
        }
        bExists = varArray.isIntArray(varName);
        if (bExists) {
            return;
        }
        bExists = varArray.isStrArray(varName);
        if (bExists) {
            return;
        }
        bExists= varLocal.isDefined(curSub, varName);
        if (bExists) {
            return;
        }
        throw new ParserException(functionId + "No Read access to variable " + varName);
    }
    
    /**
     * checks whether specified variable has Write permission for current function.
     * 
     * @param varName - name of variable to check
     * 
     * @throws ParserException 
     */
    public void checkWriteAccess (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // MAIN function variables are GLOBAL, so read from anywhere
        if (Subroutine.isMainFunction()) {
            return;
        }
        
        String curSub = Subroutine.getSubName();
        boolean bExists;

        bExists = varGlobal.isDefined(varName);
        if (bExists) {
            return;
        }
        bExists = varArray.isIntArray(varName);
        if (bExists) {
            return;
        }
        bExists = varArray.isStrArray(varName);
        if (bExists) {
            return;
        }
        bExists = varLocal.isDefined(curSub, varName);
        if (bExists) {
            return;
        }
        throw new ParserException(functionId + "No Write access to variable " + varName);
    }

    /**
     * creates a new entry in the Variable table and sets the initial value.
     * 
     * @param accStr   - the access type for the variable (GLOBAL, LOCAL)
     * @param dataType - the data type to allocate
     * @param varName  - Variable name
     * @param subName  - the function the variable is defined in
     * 
     * @throws ParserException - if Variable was already defined
     */
    public void allocateVariable (String accStr, String dataType, String varName, String subName) throws ParserException {
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
            throw new ParserException(functionId + "Invalid variable type for allocation of " + varName + ": " + dataType);
        }
        
        // allocate and set default value for the variable
        AccessType access;
        switch (accStr) {
            case "GLOBAL": access = AccessType.GLOBAL;  break;
            case "LOCAL":  access = AccessType.LOCAL;   break;
            default:
                throw new ParserException(functionId + "Invalid access type for allocation of " + varName + ": " + accStr);
        }
        if (access == AccessType.LOCAL) {
            varLocal.allocVar (varName, ptype);
        } else {
            switch (ptype) {
                case IntArray:
                case StrArray:
                    varArray.allocateVariable(varName, ptype);
                    break;
                default:
                    varGlobal.allocVar (varName, subName, ptype);
                    break;
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
    public int getStringSize (String name) throws ParserException {
        String strValue = getStringValue(name);
        if (strValue != null) {
            return strValue.length();
        }
        return 0;
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

        VarExtensions paramInfo = paramValue.getVariableRef();
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
                case IntArray:
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
                        frame.outputInfoMsg(STATUS_VARS, INDENT + name + "[" + iStart + "] as type " + pType + ": " + paramValue.getIntegerValue());
                    } else {
                        throw new ParserException(functionId + "Parameter " + name + " index " + iStart + " exceeds array");
                    }
                    break;
                case StrArray:
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
                        frame.outputInfoMsg(STATUS_VARS, INDENT + name + "[" + iStart + "] as type " + pType + ": " + paramValue.getStringValue());
                    } else {
                        throw new ParserException(functionId + "Parameter " + name + " index " + iStart + " exceeds array");
                    }
                    break;
                case String:
                    if (iStart >= 0 && iEnd < paramValue.getStrArraySize()) {
                        paramValue.setStringValue(paramValue.getStringValue().substring(iStart, iEnd + 1));
                        frame.outputInfoMsg(STATUS_VARS, INDENT + name + "index" + ixRange + " as type " + pType + ": " + paramValue.getStringValue());
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
    public ParameterStruct getVariableInfo (VarExtensions paramInfo) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (paramInfo == null || paramInfo.getName() == null || paramInfo.getType() == null) {
            return null;
        }
        
        // create a new parameter with all null entries
        ParameterStruct paramValue = new ParameterStruct();

        String name = paramInfo.getName();
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }
        
        ParameterStruct.ParamType pType = paramInfo.getType();
        Long varValue;
        switch (getVariableClass (name)) {
            // check the reserved params list
            case RESERVED:
                paramValue = VarReserved.getVariableInfo (paramInfo, name, pType);
                break;
            // next check for loop variables for name match
            case LOOP:
                varValue = LoopParam.getLoopCurValue(name);
                pType = ParameterStruct.ParamType.Integer;
                paramValue.setIntegerValue(varValue);
                frame.outputInfoMsg(STATUS_VARS, INDENT + "- Lookup Ref '" + name + "' as type " + pType + ": " + varValue);
                break;
            // otherwise, let's check for standard variables for name match
            default:
            case GLOBAL:
            case LOCAL:
                paramValue = getReferenceValue (name);
                paramValue.setVariableRef(new VarExtensions (paramInfo));
                break;
            case UNKNOWN:
                throw new ParserException(functionId + "- Variable Ref '" + name + "' was not found in any Variable database");
        }

        // save the variable info passed that contains Trait and Bracketing info
        paramValue.setVariableRef(new VarExtensions (paramInfo));
        
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
     * this returns the type of variable found.
     * 
     * @param name - name of the variable
     * 
     * @return type of variable (UNKNOWN if not found)
     * 
     * @throws ParserException
     */
    public VarClass getVariableClass (String name) throws ParserException {
        VarClass varClass = VarClass.UNKNOWN;
        if (name == null || name.isEmpty()) {
            return varClass;
        }
        // if name contains brackets or a Trait, eliminate that portion of the name for comparing type
        int offset = name.indexOf('.');
        if (offset > 0) name = name.substring(0, offset);
        else {
            offset = name.indexOf('[');
            if (offset > 0) name = name.substring(0, offset);
        }
        
        // classify the type
        if (LoopStruct.getCurrentLoopValue(name) != null) {
            varClass = VarClass.LOOP;
        }
        else if (VarReserved.isReservedName (name) != null) {
            varClass = VarClass.RESERVED;
        }
        else if (varGlobal.getDataType ( name) != null) {
            varClass = VarClass.GLOBAL;
        }
        else if (varArray.isIntArray(name))
            varClass = VarClass.GLOBAL;
        else if (varArray.isStrArray(name))
            varClass = VarClass.GLOBAL;
        else if (varLocal.getDataType (name, null) != null) {
            varClass = VarClass.LOCAL;
        }
        
        return varClass;
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
    public ParameterStruct.ParamType getVariableTypeFromName (String name) throws ParserException {
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
        if (LoopStruct.getCurrentLoopValue(name) != null) {
            vartype = ParameterStruct.ParamType.Integer;
        }
        if (vartype == null) {
            vartype = VarReserved.getVariableTypeFromName(name);
        }
        if (vartype == null) {
            vartype = varGlobal.getDataType (name);
        }
        if (vartype == null) {
            if (varArray.isIntArray(name))
                vartype = ParameterStruct.ParamType.IntArray;
            else if (varArray.isStrArray(name))
                vartype = ParameterStruct.ParamType.StrArray;
        }
        if (vartype == null) {
            vartype = varLocal.getDataType (name, null);
        }
        if (vartype == null) {
            throw new ParserException(functionId + "Variable entry not found: " + name);
        }
        return vartype;
    }

    /**
     * returns the current value for a user defined variable (LOCAL or GLOBAL).
     * This is used in the EXECUTION stage.
     * NOTE: This does NOT check Traits and doesn't check Reserved or Loop variables.
     * 
     * @param varName - name of the variable to evaluate
     * 
     * @return the variable value
     * 
     * @throws ParserException 
     */
    public ParameterStruct getReferenceValue (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (varName == null) {
            throw new ParserException(functionId + "Null input value");
        }
        if (varName.charAt(0) == '$') {
            varName = varName.substring(1);
        }
        
        // first, check for LOCAL parameters
        ParameterStruct refValue;
        ParameterStruct.ParamType varType;
        String subName = Subroutine.getSubName();
        try {
            varType = varLocal.getDataType(varName, subName);
            if (varType != null) {
                refValue = new ParameterStruct();
                refValue.setParamTypeDiscrete(varType);
                switch (varType) {
                    case Integer:
                        refValue.setIntegerValue(varLocal.getInteger(varName));
                        break;
                    case Unsigned:
                        refValue.setIntegerValue(varLocal.getUnsigned(varName));
                        break;
                    case Boolean:
                        refValue.setBooleanValue(varLocal.getBoolean(varName));
                        break;
                    case String:
                        refValue.setStringValue(varLocal.getString(varName));
                        break;
                    case StrArray:
                        refValue.setStrArray(varLocal.getStrArray(varName));
                        break;
                    case IntArray:
                        refValue.setIntArray(varLocal.getIntArray(varName));
                        break;
                }
                String value = refValue.getStringValue();
                frame.outputInfoMsg(STATUS_VARS, INDENT + "LOCAL " + varType + " Variable " + varName + ": " + value);
                return refValue;
            }
        } catch (ParserException exMsg) {
            // just continue on to check GLOBALS
        }
        // not a LOCAL var, now check GLOBALS
        varType = getVariableTypeFromName (varName);
        refValue = new ParameterStruct();
        refValue.setParamTypeDiscrete(varType);
        switch (varType) {
            case Integer:
                refValue.setIntegerValue(VarGlobal.getIntegerVariable(varName));
                break;
            case Unsigned:
                refValue.setIntegerValue(VarGlobal.getUnsignedVariable(varName));
                break;
            case Boolean:
                refValue.setBooleanValue(VarGlobal.getBooleanVariable(varName));
                break;
            case String:
                refValue.setStringValue(VarGlobal.getStringVariable(varName));
                break;
            case StrArray:
                refValue.setStrArray(varArray.getStrArray(varName));
                break;
            case IntArray:
                refValue.setIntArray(varArray.getIntArray(varName));
                break;
        }
        String value = refValue.getStringValue();
        frame.outputInfoMsg(STATUS_VARS, INDENT + "GLOBAL " + varType + " Variable " + varName + ": " + value);
        return refValue;
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
    public Long getNumericValue (String name, TraitInfo.Trait traitVal, boolean bNoLoops) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (name == null) {
            throw new ParserException(functionId + "Null input value");
        }
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }
        Long iValue = null;
        try {
            // first, check reserved values
            iValue = VarReserved.getNumericValue (name, traitVal);
            if (iValue != null) {
                return iValue;
            }
            // now check for loop values
            Integer loopVal = LoopStruct.getCurrentLoopValue(name);
            if (loopVal != null) {
                iValue = (long) loopVal;
                if (bNoLoops) {
                    return null; // value is a loop var, but is not allowed
                }
            }
            else {
                // check both LOCAL and GLOBAL params
                ParameterStruct param = getReferenceValue (name);
                switch (param.getParamType()) {
                    case Integer:
                    case Unsigned:
                        iValue = param.getIntegerValue();
                        break;
                    case Boolean:
                        frame.outputInfoMsg(STATUS_DEBUG, INDENT + "Converting from Boolean to Integer: " + name);
                        iValue = param.getBooleanValue() ? 1L : 0;
                        break;
                    case String:
                        // first, check for a Trait
                        iValue = TraitInfo.getTraitIntValues(traitVal, name, ParameterStruct.ParamType.String);
                        if (iValue == null) {
                            String strValue = param.getStringValue();
                            iValue = ParameterStruct.getLongOrUnsignedValue(strValue);
                            frame.outputInfoMsg(STATUS_DEBUG, INDENT + "Converting variable " + name + " to Integer: " + iValue);
                        }
                        break;
                    case StrArray:
                        // first, check for a Trait
                        iValue = TraitInfo.getTraitIntValues(traitVal, name, ParameterStruct.ParamType.StrArray);
                        if (iValue == null) {
                            String strValue = param.getStrArray().getFirst();
                            iValue = ParameterStruct.getLongOrUnsignedValue(strValue);
                            frame.outputInfoMsg(STATUS_DEBUG, INDENT + "Extracted 1st entry of StrArray " + name + " to Integer: " + iValue);
                        }
                        break;
                    case IntArray:
                        // first, check for a Trait
                        iValue = TraitInfo.getTraitIntValues(traitVal, name, ParameterStruct.ParamType.IntArray);
                        if (iValue == null) {
                            iValue = param.getIntArray().getFirst();
                            frame.outputInfoMsg(STATUS_DEBUG, INDENT + "Extracted 1st entry of IntArray " + name + " to Integer: " + iValue);
                        }
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
     * @throws ParserException - if not valid
     */
    public void checkValidVariable (VarCheck use, String name) throws ParserException {
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

            // get the access info for the specified variable and determine what function we are in
//            checkWriteAccess (name);
            String curSub = Subroutine.getSubName();
            
            // check for variable lists
            VarClass varClass;
            try {
                varClass = getVariableClass (name);
            } catch (ParserException exMsg) {
                varClass = VarClass.UNKNOWN;
            }
            String errMsg = null;
            switch (use) {
                default:
                case ALLOCATE:
                    switch (varClass) {
                        case UNKNOWN  -> { } // not found in any group is good
                        case LOOP     -> errMsg = "Using Loop Variable name: " + name;
                        case RESERVED -> errMsg = "Using Reserved command name: " + name;
                        case GLOBAL   -> errMsg = "Variable name already in use: " + name;
                        case LOCAL    -> {  } // this is acceptable
                    }
                    break;

                case SET:
                    // you can only set user variables plus this one reserved variable
                    if (name.contentEquals("RANDOM")) {
                        break;
                    }
                    switch (varClass) {
                        case UNKNOWN  -> errMsg = "Variable name not found: " + name;
                        case LOOP     -> errMsg = "Using Loop Variable name: " + name;
                        case RESERVED -> errMsg = "Using Reserved command name: " + name;
                        case GLOBAL   -> {  } // this is acceptable
                        case LOCAL    -> {  } // this is acceptable
                    }
                    break;

                case REFERENCE:
                    switch (varClass) {
                        case UNKNOWN  -> errMsg = "Variable name not found: " + name;
                        case LOOP     -> { } // these are always valid
                        case RESERVED -> { } // these are always valid
                        case GLOBAL   -> {  } // this is acceptable
                        case LOCAL    -> {  } // this is acceptable
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
            frame.outputInfoMsg(STATUS_DEBUG, INDENT + "Variable name is null");
            return false;
        }
        boolean bRighthand = false;
        if (name.startsWith("$")) {
            name = name.substring(1);
            bRighthand = true;
        }
        if (! Character.isLetter(name.charAt(0))) {
            // 1st character must be a letter
            frame.outputInfoMsg(STATUS_DEBUG, INDENT + "invalid initial character in Variable name: " + name);
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
                    frame.outputInfoMsg(STATUS_DEBUG, INDENT + "Variable name too long (max len " + NAME_MAXLEN + ") in name: " + name.substring(0, ix));
                    return false;
                }
            } else {
                // this will terminate the Variable search
                if (curch == ' ' || curch == '=') {
                    break;
                }
                if (!bRighthand) {
                    frame.outputInfoMsg(STATUS_DEBUG, INDENT + "Variable assignment should not include '$': " + name);
                    return false;
                }
                // check for bracket index on Array, List and String Variable
                switch (curch) {
                    case '[':
                        int offset = name.indexOf(']');
                        if (offset <= 0 || offset >= name.length() - 1) {
                            frame.outputInfoMsg(STATUS_DEBUG, INDENT + "missing end bracket in Variable name: " + name);
                            return false;
                        }
                        try {
                            indexStart = Utils.getIntValue(name.substring(ix+1)).intValue();
                        } catch (ParserException exMsg) {
                            frame.outputInfoMsg(STATUS_DEBUG, INDENT + "invalid numeric in brackets");
                            return false;
                        }
                        // TODO: evaluate indexEnd
                        break;
                    case '.':
                        // TODO:
                        break;
                    default:
                        frame.outputInfoMsg(STATUS_DEBUG, INDENT + "invalid character '" + curch + "' in Variable name: " + name);
                        return false;
                }
            }
        }
        if (indexStart == 0 && name.length() > NAME_MAXLEN) {
            frame.outputInfoMsg(STATUS_DEBUG, INDENT + "Variable name too long (max len " + NAME_MAXLEN + ") in name: " + name);
            return false;
        }
        if (indexStart > 0 && indexEnd == 0) {
            frame.outputInfoMsg(STATUS_DEBUG, INDENT + "Variable name index missing ending bracket: " + name);
            return false;
        }
            
        return true;
    }
    
}
