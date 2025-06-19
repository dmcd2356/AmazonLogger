/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_VARS;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author dan
 */
public class VarLocal {
    
    private static final String CLASS_NAME = VarLocal.class.getSimpleName();
    private static final String INDENT = "     ";
    
    // map of local variables for each subroutines defined
    // they are keyed by the name of the subroutine
    private static final HashMap<String, VarLocalSub> locals = new HashMap<>();

    public VarLocal() {
    }

    public static void initVariables () {
        locals.clear();
    }
    
    /**
     * returns a list of all local variables.
     * 
     * @return a list of the defined variables for local definitions in all subroutines
     */
    public ArrayList<String> getVarAlloc () {
        ArrayList<String> response = new ArrayList<>();
        ArrayList<String> subNameList = Subroutine.getSubNames();
        for (int ix = 0; ix < subNameList.size(); ix++) {
            String subName = subNameList.get(ix);
            if (locals.containsKey(subName)) {
                VarLocalSub subInfo = locals.get(subName);
                response.addAll(subInfo.getVarAlloc());
            }
        }
        return response;
    }
    
    /**
     * returns a list of the variables defined here.
     * 
     * @param subName - name of the subroutine of interest
     * @param varName - name of the variable to look up
     * 
     * @return a string containing the name, value and other info for the variable
     * 
     * @throws ParserException
     */
    public String getVarInfo (String subName, String varName) throws ParserException {
        String response = "";
        VarLocalSub varInfo = locals.get(subName);
        if (varInfo != null) {
            response = varInfo.getVarInfo(varName);
        }
        return response;
    }
    
    /**
     * this allocates a block for adding local variables for a subroutine.
     * This should be called when a subroutine is defined during COMPILE.
     * 
     * @param subName - name of the subroutine
     * 
     * @throws ParserException 
     */
    public void allocSubroutine (String subName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (locals.containsKey(subName)) {
            throw new ParserException(functionId + "Subroutine allocations already defined: " + subName);
        }
        locals.put(subName, new VarLocalSub(subName));
        frame.outputInfoMsg(STATUS_VARS, INDENT + "Local Variable allocation created for subroutine: " + subName);
    }
    
    /**
     * determine if local variable exists for the current subroutine running.
     * 
     * @param subName - name of the subroutine
     * @param varName - name of the variable
     * 
     * @return true if variable found in selected subroutine
     * 
     * @throws ParserException 
     */
    public boolean isDefined (String subName, String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (! locals.containsKey(subName)) {
            throw new ParserException(functionId + "Subroutine allocations not found: " + subName);
        }
        return locals.get(subName).isDefined(varName);
    }

    // indicates if the variable has been written to since it was allocated
    public boolean isVarInit (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        String subName = Subroutine.getSubName();
        VarLocalSub localSub = locals.get(subName);
        if (localSub == null) {
            throw new ParserException(functionId + "Subroutine allocations not found: " + subName);
        }
        if (! localSub.isDefined(varName)) {
            throw new ParserException(functionId + "Variable " + varName + " not found");
        }
        return localSub.isVarInit(varName);
    }

    // saves the time and script line when the variable was written.
    public static void setVarWriter (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        String subName = Subroutine.getSubName();
        VarLocalSub localSub = locals.get(subName);
        if (localSub == null) {
            throw new ParserException(functionId + "Subroutine allocations not found: " + subName);
        }
        if (! localSub.isDefined(varName)) {
            throw new ParserException(functionId + "Variable " + varName + " not found");
        }
        localSub.setVarWriter(varName);
    }
    
    // returns the line number of the script that was the last writer to the variable
    public String getWriterIndex (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        String subName = Subroutine.getSubName();
        VarLocalSub localSub = locals.get(subName);
        if (localSub == null) {
            throw new ParserException(functionId + "Subroutine allocations not found: " + subName);
        }
        if (! localSub.isDefined(varName)) {
            throw new ParserException(functionId + "Variable " + varName + " not found");
        }
        return localSub.getWriterIndex(varName);
    }

    // returns the timestamp when the last writer wrote to the variable
    public String getWriterTime (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        String subName = Subroutine.getSubName();
        VarLocalSub localSub = locals.get(subName);
        if (localSub == null) {
            throw new ParserException(functionId + "Subroutine allocations not found: " + subName);
        }
        if (! localSub.isDefined(varName)) {
            throw new ParserException(functionId + "Variable " + varName + " not found");
        }
        return localSub.getWriterTime(varName);
    }
        
    /**
     * determine if local variable exists for the current subroutine running.
     * 
     * @param varName - name of the variable
     * @param subName - name of the subroutine (null if use current)
     * 
     * @return data type of variable if found (null if var not found)
     * 
     * @throws ParserException 
     */
    public ParameterStruct.ParamType getDataType (String varName, String subName) throws ParserException {
        if (subName == null) {
            subName = Subroutine.getSubName();
        }
        
        if (! locals.containsKey(subName)) {
            return null;
        }
        return locals.get(subName).getDataType(varName);
    }

    /**
     * this makes an local variable allocation for the current function.
     * This should be called when a local allocation is defined in a subroutine
     *   during EXECUTION.
     * 
     * @param varName - the variable name
     * @param varType - the variable data type
     * 
     * @throws ParserException 
     */
    public void allocVar (String varName, ParameterStruct.ParamType varType) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        String subName = Subroutine.getSubName();
        if (! locals.containsKey(subName)) {
            allocSubroutine (subName);
        }
        if (isAllocated (subName, varName)) {
            throw new ParserException(functionId + "Variable " + varName + " already defined for subroutine: " + subName);
        }
        VarLocalSub vars = locals.get(subName);
        vars.allocLocal(varName, varType, Variables.AccessType.LOCAL);
    }
    
    public void putInteger (String varName, Long value) throws ParserException {
        ParameterStruct.ParamType type = ParameterStruct.ParamType.Integer;
        String subName = Subroutine.getSubName();
        VarLocalSub vars = getSubLocals(subName, varName, type);
        vars.putInteger(varName, value);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "LOCAL " + type + " Variable " + varName + " for subroutine " + subName + " set to: " + value);
    }
    
    public Long getInteger (String varName) throws ParserException {
        ParameterStruct.ParamType type = ParameterStruct.ParamType.Integer;
        String subName = Subroutine.getSubName();
        VarLocalSub vars = getSubLocals(subName, varName, type);
        Long value = vars.getInteger(varName);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "LOCAL " + type + " Variable " + varName + " for subroutine " + subName + " = " + value);
        return value;
    }
    
    public void putUnsigned (String varName, Long value) throws ParserException {
        ParameterStruct.ParamType type = ParameterStruct.ParamType.Unsigned;
        String subName = Subroutine.getSubName();
        VarLocalSub vars = getSubLocals(subName, varName, type);
        vars.putUnsigned(varName, value);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "LOCAL " + type + " Variable " + varName + " for subroutine " + subName + " set to: " + value);
    }
    
    public Long getUnsigned (String varName) throws ParserException {
        ParameterStruct.ParamType type = ParameterStruct.ParamType.Unsigned;
        String subName = Subroutine.getSubName();
        VarLocalSub vars = getSubLocals(subName, varName, type);
        Long value = vars.getUnsigned(varName);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "LOCAL " + type + " Variable " + varName + " for subroutine " + subName + " = " + value);
        return value;
    }
    
    public void putBoolean (String varName, Boolean value) throws ParserException {
        ParameterStruct.ParamType type = ParameterStruct.ParamType.Boolean;
        String subName = Subroutine.getSubName();
        VarLocalSub vars = getSubLocals(subName, varName, type);
        vars.putBoolean(varName, value);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "LOCAL " + type + " Variable " + varName + " for subroutine " + subName + " set to: " + value);
    }
    
    public Boolean getBoolean (String varName) throws ParserException {
        ParameterStruct.ParamType type = ParameterStruct.ParamType.Boolean;
        String subName = Subroutine.getSubName();
        VarLocalSub vars = getSubLocals(subName, varName, type);
        Boolean value = vars.getBoolean(varName);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "LOCAL " + type + " Variable " + varName + " for subroutine " + subName + " = " + value);
        return value;
    }
    
    public void putString (String varName, String value) throws ParserException {
        ParameterStruct.ParamType type = ParameterStruct.ParamType.String;
        String subName = Subroutine.getSubName();
        VarLocalSub vars = getSubLocals(subName, varName, type);
        vars.putString(varName, value);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "LOCAL " + type + " Variable " + varName + " for subroutine " + subName + " set to: " + value);
    }
    
    public String getString (String varName) throws ParserException {
        ParameterStruct.ParamType type = ParameterStruct.ParamType.String;
        String subName = Subroutine.getSubName();
        VarLocalSub vars = getSubLocals(subName, varName, type);
        String value = vars.getString(varName);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "LOCAL " + type + " Variable " + varName + " for subroutine " + subName + " = " + value);
        return value;
    }
    
    //==========================================    
    // Array functions
    //==========================================
    
    public void updateStrArray (String varName, ArrayList<String> value) throws ParserException {
        ParameterStruct.ParamType type = ParameterStruct.ParamType.StrArray;
        String subName = Subroutine.getSubName();
        VarLocalSub vars = getSubLocals(subName, varName, type);
        vars.updateStrArray(varName, subName, value);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "LOCAL " + type + " Variable " + varName + " updated for subroutine: " + subName);
    }
    
    public void updateIntArray (String varName, ArrayList<Long> value) throws ParserException {
        ParameterStruct.ParamType type = ParameterStruct.ParamType.IntArray;
        String subName = Subroutine.getSubName();
        VarLocalSub vars = getSubLocals(subName, varName, type);
        vars.updateIntArray(varName, subName, value);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "LOCAL " + type + " Variable " + varName + " updated for subroutine: " + subName);
    }

    public boolean isStrArray (String varName) {
        ParameterStruct.ParamType type = ParameterStruct.ParamType.StrArray;
        String subName = Subroutine.getSubName();
        try {
            VarLocalSub vars = getSubLocals(subName, varName, type);
        } catch (ParserException exMsg) {
            return false;
        }
        return true;
    }
    
    public boolean isIntArray (String varName) {
        ParameterStruct.ParamType type = ParameterStruct.ParamType.IntArray;
        String subName = Subroutine.getSubName();
        try {
            VarLocalSub vars = getSubLocals(subName, varName, type);
        } catch (ParserException exMsg) {
            return false;
        }
        return true;
    }
    
    public ArrayList<String> getStrArray (String varName) throws ParserException {
        ParameterStruct.ParamType type = ParameterStruct.ParamType.StrArray;
        String subName = Subroutine.getSubName();
        VarLocalSub vars = getSubLocals(subName, varName, type);
        return vars.getStrArray(varName);
    }
    
    public ArrayList<Long> getIntArray (String varName) throws ParserException {
        ParameterStruct.ParamType type = ParameterStruct.ParamType.IntArray;
        String subName = Subroutine.getSubName();
        VarLocalSub vars = getSubLocals(subName, varName, type);
        return vars.getIntArray(varName);
    }

    // PRIVATE METHODS
    
    private VarLocalSub getSubLocals (String subName, String varName, ParameterStruct.ParamType type) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        VarLocalSub vars = locals.get(subName);
        if (vars == null) {
            throw new ParserException(functionId + "Variables not defined for subroutine: " + subName);
        }
        ParameterStruct.ParamType varType = vars.getDataType(varName);
        if (varType == null || varType != type) {
            throw new ParserException(functionId + "Local variable type " + varType + " not same type " + type + " as function " + subName);
        }

        frame.outputInfoMsg(STATUS_VARS, INDENT + "Local Variable " + varName + " found for subroutine: " + subName);
        return vars;
    }
    
    private boolean isAllocated (String subName, String varName) throws ParserException {
        boolean bAllocated = false;
        VarLocalSub vars = locals.get(subName);
        if (vars != null) {
            ParameterStruct.ParamType varType = vars.getDataType(varName);
            bAllocated = varType != null;
        }
        return bAllocated;
    }
    
}
