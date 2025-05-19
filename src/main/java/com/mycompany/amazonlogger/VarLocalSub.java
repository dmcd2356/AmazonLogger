/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This holds maps of each of the variable data types for LOCAL variables.
 *
 * @author dan
 */
public class VarLocalSub {

    private static final String CLASS_NAME = VarLocalSub.class.getSimpleName();
    
    // These are keyed by the name of the variable.
    private final HashMap<String, VarAccess> localVar;
    
    private final String owner;

    
    public VarLocalSub (String owner) {
        this.owner = owner;
        this.localVar = new HashMap<>();
    }

    private VarAccess checkLocalVar (String varName, ParameterStruct.ParamType callType) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        VarAccess var = localVar.get(varName);
        if (var == null) {
            throw new ParserException(functionId + "Local variable not found: " + varName);
        }
        if (var.getType() != callType) {
            throw new ParserException(functionId + "Local variable not correct type: " + var.getType());
        }
        return var;
    }
    

    public String getOwner (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        VarAccess var = localVar.get(varName);
        if (var == null) {
            throw new ParserException(functionId + "Local variable not found: " + varName);
        }
        return localVar.get(varName).getOwner();
    }
        
    // indicates if the variable has been written to since it was allocated
    public boolean isVarInit (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        VarAccess varInfo = localVar.get(varName);
        if (varInfo == null) {
            throw new ParserException(functionId + "Local variable not found: " + varName);
        }
        return varInfo.isVarInit();
    }

    // returns the line number of the script that was the last writer to the variable
    public String getWriterIndex (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        VarAccess varInfo = localVar.get(varName);
        if (varInfo == null) {
            throw new ParserException(functionId + "Local variable not found: " + varName);
        }
        Integer cmdIx = varInfo.getWriterIndex();
        return "" + cmdIx;
    }

    // returns the timestamp when the last writer wrote to the variable
    public String getWriterTime (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        VarAccess varInfo = localVar.get(varName);
        if (varInfo == null) {
            throw new ParserException(functionId + "Variable " + varName + " not found");
        }
        return varInfo.getWriterTime();
    }
        
    public boolean isDefined (String varName) {
        return localVar.get(varName) != null;
    }
        
    public boolean isGlobal (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        VarAccess var = localVar.get(varName);
        if (var == null) {
            throw new ParserException(functionId + "Local variable not found: " + varName);
        }
        return localVar.get(varName).isGlobal();
    }
        
    public void allocLocal (String varName, ParameterStruct.ParamType varType, Variables.AccessType access) {
        VarAccess localInfo = new VarAccess (this.owner, varName, varType, access);
        localVar.put(varName, localInfo);
    }
    
    public ParameterStruct.ParamType getDataType (String varName) throws ParserException {
        VarAccess var = localVar.get(varName);
        return (var == null) ? null : var.getType();
    }
    
    // =================== INTEGER var access ===================

    public Long getInteger (String varName) throws ParserException {
        VarAccess local = checkLocalVar (varName, ParameterStruct.ParamType.Integer);
        return local.getValueInteger();
    }
        
    public void putInteger (String varName, Long value) throws ParserException {
        VarAccess local = checkLocalVar (varName, ParameterStruct.ParamType.Integer);
        local.setValueInteger(value);
    }
        
    // =================== UNSIGNED var access ===================

    public Long getUnsigned (String varName) throws ParserException {
        VarAccess local = checkLocalVar (varName, ParameterStruct.ParamType.Unsigned);
        return local.getValueUnsigned();
    }
        
    public void putUnsigned (String varName, Long value) throws ParserException {
        VarAccess local = checkLocalVar (varName, ParameterStruct.ParamType.Unsigned);
        local.setValueUnsigned(value);
    }
        
    // =================== BOOLEAN var access ===================

    public Boolean getBoolean (String varName) throws ParserException {
        VarAccess local = checkLocalVar (varName, ParameterStruct.ParamType.Boolean);
        return local.getValueBoolean();
    }
        
    public void putBoolean (String varName, Boolean value) throws ParserException {
        VarAccess local = checkLocalVar (varName, ParameterStruct.ParamType.Boolean);
        local.setValueBoolean(value);
    }
        
    // =================== STRING var access ===================

    public String getString (String varName) throws ParserException {
        VarAccess local = checkLocalVar (varName, ParameterStruct.ParamType.String);
        return local.getValueString();
    }
        
    public void putString (String varName, String value) throws ParserException {
        VarAccess local = checkLocalVar (varName, ParameterStruct.ParamType.String);
        local.setValueString(value);
    }
        
    // =================== STRING ARRAY var access ===================

    public ArrayList<String> getStrArray (String varName) throws ParserException {
        VarAccess local = checkLocalVar (varName, ParameterStruct.ParamType.StrArray);
        return local.getValueStrArray();
    }
        
    public void updateStrArray (String varName, String subName, ArrayList<String> value) throws ParserException {
        VarAccess local = checkLocalVar (varName, ParameterStruct.ParamType.StrArray);
        local.setValueStrArray(value);
    }
    
    // =================== INT ARRAY var access ===================

    public ArrayList<Long> getIntArray (String varName) throws ParserException {
        VarAccess local = checkLocalVar (varName, ParameterStruct.ParamType.IntArray);
        return local.getValueIntArray();
    }
        
    public void updateIntArray (String varName, String subName, ArrayList<Long> value) throws ParserException {
        VarAccess local = checkLocalVar (varName, ParameterStruct.ParamType.IntArray);
        local.setValueIntArray(value);
    }
    
}
