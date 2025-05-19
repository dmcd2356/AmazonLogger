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
public class VarGlobal {
    
    private static final String CLASS_NAME = VarGlobal.class.getSimpleName();
    private static final String INDENT = "     ";
    
    // user-defined global static Variables
    private static final HashMap<String, VarAccess> globals  = new HashMap<>();
    
    VarGlobal () {
    }
    
    /**
     * initializes the saved Variables
     */
    public static void initVariables () {
        globals.clear();
    }

    /**
     * makes a global allocation for the given variable specs.
     * 
     * @param varName - name of the variable
     * @param ptype   - parameter type of variable
     * @param subName - subroutine it is defined in
     * 
     * @throws ParserException 
     */
    public static void allocVar (String varName, ParameterStruct.ParamType ptype, String subName) throws ParserException {
        VarAccess var = new VarAccess(subName, varName, ptype, Variables.AccessType.GLOBAL);
        globals.put(varName, var);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "- Allocated GLOBAL " + ptype + " variable: " + varName + " in " + subName);
    }
    
    /**
     * determine if local variable exists for the current subroutine running.
     * 
     * @param varName - name of the variable
     * 
     * @return data type of variable if found
     * 
     * @throws ParserException 
     */
    public static boolean isDefined (String varName) throws ParserException {
        if (varName == null) {
            return false;
        }
        return globals.containsKey(varName);
    }
    
    // indicates if the variable has been written to since it was allocated
    public static boolean isVarInit (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (varName == null) {
            throw new ParserException(functionId + "Null input value");
        }
        VarAccess varInfo = globals.get(varName);
        if (varInfo == null) {
            throw new ParserException(functionId + "Variable " + varName + " not found");
        }
        return varInfo.isVarInit();
    }

    // returns the line number of the script that was the last writer to the variable
    public static String getWriterIndex (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (varName == null) {
            throw new ParserException(functionId + "Null input value");
        }
        VarAccess varInfo = globals.get(varName);
        if (varInfo == null) {
            throw new ParserException(functionId + "Variable " + varName + " not found");
        }
        Integer cmdIx = varInfo.getWriterIndex();
        return "" + cmdIx;
    }

    // returns the timestamp when the last writer wrote to the variable
    public static String getWriterTime (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (varName == null) {
            throw new ParserException(functionId + "Null input value");
        }
        VarAccess varInfo = globals.get(varName);
        if (varInfo == null) {
            throw new ParserException(functionId + "Variable " + varName + " not found");
        }
        return varInfo.getWriterTime();
    }
        
    /**
     * determine if local variable exists for the current subroutine running.
     * 
     * @param varName - name of the variable
     * 
     * @return data type of variable if found
     * 
     * @throws ParserException 
     */
    public static ParameterStruct.ParamType getDataType (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (varName == null) {
            throw new ParserException(functionId + "Null input value");
        }
        VarAccess var = globals.get(varName);
        if (var != null)
            return var.getType();
        return null;
    }
    
    /**
     * returns the value of the String Variable.
     * 
     * @param name  - Variable name
     * 
     * @return variable value
     * 
     * @throws ParserException
     */
    public static String getStringVariable (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null) {
            throw new ParserException(functionId + "Null input value");
        }
        VarAccess var = globals.get(name);
        if (var == null) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        return var.getValueString();
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
    public static void putStringVariable (String name, String value) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || value == null) {
            throw new ParserException(functionId + "Null input value");
        }
        if (! globals.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        VarAccess var = globals.get(name);
        var.setValueString(value);
        globals.replace(name, var);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "- Modified String param: " + name + " = " + value);
    }

    /**
     * returns the value of the Integer Variable.
     * 
     * @param name  - Variable name
     * 
     * @return variable value
     * 
     * @throws ParserException
     */
    public static Long getIntegerVariable (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null) {
            throw new ParserException(functionId + "Null input value");
        }
        VarAccess var = globals.get(name);
        if (var == null) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        return var.getValueInteger();
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
    public static void putIntegerVariable (String name, Long value) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || value == null) {
            throw new ParserException(functionId + "Null input value");
        }
        if (! globals.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        VarAccess var = globals.get(name);
        var.setValueInteger(value);
        globals.replace(name, var);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "- Modified Integer param: " + name + " = " + value);
    }

    /**
     * returns the value of the Unsigned Variable.
     * 
     * @param name  - Variable name
     * 
     * @return variable value
     * 
     * @throws ParserException
     */
    public static Long getUnsignedVariable (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null) {
            throw new ParserException(functionId + "Null input value");
        }
        VarAccess var = globals.get(name);
        if (var == null) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        return var.getValueUnsigned();
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
    public static void putUnsignedVariable (String name, Long value) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || value == null) {
            throw new ParserException(functionId + "Null input value");
        }
        if (name.contentEquals("RANDOM")) {
            VarReserved.setMaxRandom(value);
            return;
        }
        if (! globals.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        VarAccess var = globals.get(name);
        var.setValueUnsigned(value);
        globals.replace(name, var);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "- Modified Unsigned param: " + name + " = " + value);
    }

    /**
     * returns the value of the Boolean Variable.
     * 
     * @param name  - Variable name
     * 
     * @return variable value
     * 
     * @throws ParserException
     */
    public static Boolean getBooleanVariable (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null) {
            throw new ParserException(functionId + "Null input value");
        }
        VarAccess var = globals.get(name);
        if (var == null) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        return var.getValueBoolean();
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
    public static void putBooleanVariable (String name, Boolean value) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || value == null) {
            throw new ParserException(functionId + "Null input value");
        }
        if (! globals.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        VarAccess var = globals.get(name);
        var.setValueBoolean(value);
        globals.replace(name, var);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "- Modified Boolean param: " + name + " = " + value);
    }

    /**
     * returns the value of the StrArray Variable.
     * 
     * @param name  - Variable name
     * 
     * @return variable value
     * 
     * @throws ParserException
     */
    public static ArrayList<String> getStrArray (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null) {
            throw new ParserException(functionId + "Null input value");
        }
        VarAccess var = globals.get(name);
        if (var == null) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        return var.getValueStrArray();
    }
    
    /**
     * modifies the value of an existing entry in the StrArray Variable table.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param value - Variable value
     * 
     * @throws ParserException
     */
    public static void updateStrArray (String name, ArrayList<String> value) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        if (name == null || value == null) {
            throw new ParserException(functionId + "Null input value");
        }
        VarAccess var = globals.get(name);
        var.setValueStrArray(value);
        globals.replace(name, var);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "- Modified StrArray param: " + name);
    }
    
    /**
     * returns the value of the IntArray Variable.
     * 
     * @param name  - Variable name
     * 
     * @return variable value
     * 
     * @throws ParserException
     */
    public static ArrayList<Long> getIntArray (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null) {
            throw new ParserException(functionId + "Null input value");
        }
        VarAccess var = globals.get(name);
        if (var == null) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        return var.getValueIntArray();
    }

    /**
     * modifies the value of an existing entry in the IntArray Variable table.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param value - Variable value
     * 
     * @throws ParserException
     */
    public static void updateIntArray (String name, ArrayList<Long> value) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        if (name == null || value == null) {
            throw new ParserException(functionId + "Null input value");
        }
        VarAccess var = globals.get(name);
        var.setValueIntArray(value);
        globals.replace(name, var);
        frame.outputInfoMsg(STATUS_VARS, INDENT + "- Modified IntArray param: " + name);
    }

}
