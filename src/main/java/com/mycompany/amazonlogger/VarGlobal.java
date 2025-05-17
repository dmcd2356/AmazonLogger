/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_VARS;
import java.util.HashMap;

/**
 *
 * @author dan
 */
public class VarGlobal {
    
    private static final String CLASS_NAME = VarGlobal.class.getSimpleName();
    private static final String INDENT = "     ";
    
    // user-defined global static Variables
    private static final HashMap<String, String>  strParams  = new HashMap<>();
    private static final HashMap<String, Long>    longParams = new HashMap<>();
    private static final HashMap<String, Long>    uintParams = new HashMap<>();
    private static final HashMap<String, Boolean> boolParams = new HashMap<>();

    VarGlobal () {
    }
    
    /**
     * initializes the saved Variables
     */
    public static void initVariables () {
        strParams.clear();
        longParams.clear();
        uintParams.clear();
        boolParams.clear();
    }

    /**
     * returns the value of the String Variables.
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
        if (! strParams.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        return strParams.get(name);
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
        if (strParams.containsKey(name)) {
            strParams.replace(name, value);
            frame.outputInfoMsg(STATUS_VARS, INDENT + "- Modified String param: " + name + " = " + value);
        } else {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
    }

    /**
     * returns the value of the Integer Variables.
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
        if (! longParams.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        return longParams.get(name);
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
        if (longParams.containsKey(name)) {
            longParams.replace(name, value);
            frame.outputInfoMsg(STATUS_VARS, INDENT + "- Modified Integer param: " + name + " = " + value);
        } else {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
    }

    /**
     * returns the value of the Unsigned Variablee.
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
        if (! uintParams.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        return uintParams.get(name);
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
        if (! ParameterStruct.isUnsignedInt(value)) {
            throw new ParserException(functionId + "value for Variable " + name + " exceeds limits for Unsigned: " + value);
        }
        if (name.contentEquals("RANDOM")) {
            VarReserved.setMaxRandom(value);
        } else if (uintParams.containsKey(name)) {
            uintParams.replace(name, value);
            frame.outputInfoMsg(STATUS_VARS, INDENT + "- Modified Unsigned param: " + name + " = " + value);
        } else {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
    }

    /**
     * returns the value of the Boolean Variablee.
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
        if (! boolParams.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        return boolParams.get(name);
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
        if (boolParams.containsKey(name)) {
            boolParams.replace(name, value);
            frame.outputInfoMsg(STATUS_VARS, INDENT + "- Modified Boolean param: " + name + " = " + value);
        } else {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
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
    public boolean isDefined (String varName) throws ParserException {
        if (varName == null) {
            return false;
        }
        if (longParams.containsKey(varName)) return true;
        if (uintParams.containsKey(varName)) return true;
        if (strParams.containsKey(varName))  return true;
        if (boolParams.containsKey(varName)) return true;
//        if (varArray.isIntArray(varName))   return true;
//        if (varArray.isStrArray(varName))   return true;
        return false;
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
    public ParameterStruct.ParamType getDataType (String varName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (varName == null) {
            throw new ParserException(functionId + "Null input value");
        }
        if (longParams.containsKey(varName)) return ParameterStruct.ParamType.Integer;
        if (uintParams.containsKey(varName)) return ParameterStruct.ParamType.Unsigned;
        if (strParams.containsKey(varName))  return ParameterStruct.ParamType.String;
        if (boolParams.containsKey(varName)) return ParameterStruct.ParamType.Boolean;
//        if (varArray.isIntArray(varName))   return ParameterStruct.ParamType.IntArray;
//        if (varArray.isStrArray(varName))   return ParameterStruct.ParamType.StrArray;
        return null;
    }
    
    /**
     * makes a global allocation for the given variable specs.
     * 
     * @param varName - name of the variable
     * @param subName - subroutine it is defined in
     * @param ptype   - parameter type of variable
     * 
     * @throws ParserException 
     */
    public void allocVar (String varName, String subName, ParameterStruct.ParamType ptype) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

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
//            case IntArray:
//            case StrArray:
//                varArray.allocateVariable(varName, ptype);
//                break;
            default:
                throw new ParserException(functionId + "Invalid variable type: " + ptype);
        }
        frame.outputInfoMsg(STATUS_VARS, INDENT + "- Allocated GLOBAL " + ptype + " variable: " + varName + " in " + subName);
    }
    
}
