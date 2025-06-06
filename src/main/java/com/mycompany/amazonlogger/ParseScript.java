/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.ParameterStruct.ParamType.Boolean;
import static com.mycompany.amazonlogger.ParameterStruct.ParamType.IntArray;
import static com.mycompany.amazonlogger.ParameterStruct.ParamType.Integer;
import static com.mycompany.amazonlogger.ParameterStruct.ParamType.StrArray;
import static com.mycompany.amazonlogger.ParameterStruct.ParamType.Unsigned;
import static com.mycompany.amazonlogger.UIFrame.STATUS_COMPILE;
import java.util.ArrayList;

/**
 *
 * @author dan
 */
public class ParseScript {
    
    private static final String CLASS_NAME = ParseScript.class.getSimpleName();

    /**
     * verifies that the argument data type is valid for the expected type.
     * 
     * @param expType - the expected data type of the argument
     * @param arg     - the argument in question
     * 
     * @throws ParserException 
     */
    private static void verifyArgDataType (ParameterStruct.ParamType expType, ParameterStruct arg) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        boolean bValid = true;
        ParameterStruct.ParamType argtype = ParameterStruct.classifyDataType(arg.getStringValue());
        if (! arg.isVariableRef()) {
            // only verify type if not a variable reference, since we allow type conversion
            switch (expType) {
                case Integer:
                case Unsigned:
                    switch (argtype) {
                        // these are not allowed
                        case StrArray, IntArray -> bValid = false;
                    }
                    break;

                case Boolean:
                    switch (argtype) {
                        // these are not allowed
                        case StrArray, IntArray, String -> bValid = false;
                    }
                    break;

                case IntArray:
                    switch (argtype) {
                        // these are not allowed
                        case Boolean -> bValid = false;
                    }
                    break;

                case String:
                case StrArray:
                    // allow anything
                    break;

                default:
                    throw new ParserException(functionId + "Undefined expected type: " + expType);
            }
            if (! bValid) {
                throw new ParserException(functionId + "Mismatched data type for reference Variable. Expected " + expType + ", got: " + argtype);
            }
        }
    }

    /**
     * checks the max args are not exceeded for the command.
     * 
     * @param count     - the max args allowed
     * @param cmdStruct - the command list
     * 
     * @throws ParserException 
     */
    public static void checkMaxArgs (int count, CommandStruct cmdStruct) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (cmdStruct.params.size() > count) {
            if (count == 0) {
                throw new ParserException(functionId + "No arguments permitted for this command");
            } else {
                throw new ParserException(functionId + "Too many arguments for this command. (max = " + count + ", found " + cmdStruct.params.size() + ")");
            }
        }
    }
    
    /**
     * checks the min args are met for the command.
     * 
     * @param count     - the min args allowed
     * @param cmdStruct - the command list
     * 
     * @throws ParserException 
     */
    public static void checkMinArgs (int count, CommandStruct cmdStruct) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (cmdStruct.params.size() < count) {
            throw new ParserException(functionId + "Missing arguments for this command. (min = " + count + ", found " + cmdStruct.params.size() + ")");
        }
    }
    
    /**
     * checks if the specified argument in the arg list is a valid variable name for assignment to.
     * 
     * @param index    - the index of the argument in the arg list
     * @param parmList - the list of args
     * 
     * @return the data type of the variable
     * 
     * @throws ParserException 
     */    
    public static ParameterStruct.ParamType checkVariableName (int index, ArrayList<ParameterStruct> parmList) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (parmList == null) {
            throw new ParserException(functionId + "Null parameter list");
        }
        if (parmList.size() <= index) {
            throw new ParserException(functionId + "Missing arguments in list - number found: " + parmList.size());
        }
        String varName = parmList.get(index).getStringValue();
        if (varName == null || varName.isBlank()) {
            throw new ParserException(functionId + "Variable name is null value");
        }

        // verify the parameter name is valid for assigning a value to
        PreCompile.variables.checkValidVariable(Variables.VarCheck.SET, varName);
        
        // return the datatype
        ParameterStruct.ParamType vtype = PreCompile.variables.getVariableTypeFromName(varName);
        return vtype;
    }

    /**
     * checks if the specified argument(s) comply for an Integer or Calculation value.
     * 
     * @param index       - the index of the argument in the arg list
     * @param vartype     - the type of variable it is being assigned to
     * @param parmList    - the list of args
     * @param parmString  - the parameter list as a String (for repacking)
     * 
     * @return the parameter list (may be replaced with Calculation params)
     * 
     * @throws ParserException 
     */    
    public ArrayList<ParameterStruct> checkArgIntOrCalc (int index, ParameterStruct.ParamType vartype,
            ArrayList<ParameterStruct> parmList, String parmString) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (parmList == null) {
            throw new ParserException(functionId + "Null parameter list");
        }
        if (parmList.size() <= index) {
            throw new ParserException(functionId + "Missing arguments in list - number found: " + parmList.size());
        }
        if (parmList.size() == index + 1) {
            verifyArgDataType (ParameterStruct.ParamType.Integer, parmList.get(index));
        } else if (vartype != null) {
            // re-pack param list as as calculation entry
            parmList = packCalculation (parmString, vartype);
        } else {
            throw new ParserException(functionId + "Too many arguments in list - number found: " + parmList.size());
        }
        return parmList;
    }

    /**
     * checks if the specified argument(s) comply for a String or String Concatenation value.
     * 
     * @param index      - the index of the argument in the arg list
     * @param parmList   - the list of args
     * 
     * @return the parameter list (may be replaced with Concatenation params)
     * 
     * @throws ParserException 
     */    
    public ArrayList<ParameterStruct> checkArgStringOrConcat (int index, ArrayList<ParameterStruct> parmList) throws ParserException {
         String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (parmList == null) {
            throw new ParserException(functionId + "Null parameter list");
        }
        if (parmList.size() <= index) {
            throw new ParserException(functionId + "Missing arguments in list - number found: " + parmList.size());
        }
        if (parmList.size() == index + 1) {
            verifyArgDataType (ParameterStruct.ParamType.String, parmList.get(index));
        } else {
            parmList = packStringConcat (parmList, 1);
        }
        return parmList;
    }

    /**
     * checks if the specified argument compiles with the specified data type.
     * 
     * @param index      - the index of the argument in the arg list
     * @param expType    - the type of variable the command is expecting
     * @param parmList   - the list of args
     * 
     * @throws ParserException 
     */    
    public static void checkArgType (int index, ParameterStruct.ParamType expType, ArrayList<ParameterStruct> parmList) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (parmList == null) {
            throw new ParserException(functionId + "Null parameter list");
        }
        if (parmList.size() <= index) {
            throw new ParserException(functionId + "Missing arguments in list - number found: " + parmList.size());
        }
        verifyArgDataType (expType, parmList.get(index));
    }

    /**
     * checks if the specified argument is a String and returns the value.
     * 
     * @param index      - the index of the argument in the arg list
     * @param parmList   - the list of args
     * 
     * @return the value of the String argument (it should always be valid)
     * 
     * @throws ParserException 
     */    
    public static String checkArgTypeString (int index, ArrayList<ParameterStruct> parmList) throws ParserException {
        checkArgType (index, ParameterStruct.ParamType.String, parmList);
        return parmList.get(index).getStringValue();
    }

    /**
     * checks if the specified argument is an Integer (or Unsigned) and returns the value.
     * 
     * @param index      - the index of the argument in the arg list
     * @param parmList   - the list of args
     * 
     * @return the value of the Integer argument (it should always be valid)
     * 
     * @throws ParserException 
     */    
    public static Long checkArgTypeInteger (int index, ArrayList<ParameterStruct> parmList) throws ParserException {
        checkArgType (index, ParameterStruct.ParamType.Integer, parmList);
        return parmList.get(index).getIntegerValue();
    }

    /**
     * checks if the specified argument is a Boolean and returns the value.
     * 
     * @param index      - the index of the argument in the arg list
     * @param parmList   - the list of args
     * 
     * @return the value of the Boolean argument (it should always be valid)
     * 
     * @throws ParserException 
     */    
    public static Boolean checkArgTypeBoolean (int index, ArrayList<ParameterStruct> parmList) throws ParserException {
        checkArgType (index, ParameterStruct.ParamType.Boolean, parmList);
        return parmList.get(index).getBooleanValue();
    }

    /**
     * checks if the specified argument is a StrArray and returns the value.
     * 
     * @param index      - the index of the argument in the arg list
     * @param parmList   - the list of args
     * 
     * @return the value of the StrArray argument (it should always be valid)
     * 
     * @throws ParserException 
     */    
    public static ArrayList<String> checkArgTypeStrArray (int index, ArrayList<ParameterStruct> parmList) throws ParserException {
        checkArgType (index, ParameterStruct.ParamType.StrArray, parmList);
        return parmList.get(index).getStrArray();
    }
    
    /**
     * checks if the specified argument is a IntArray and returns the value.
     * 
     * @param index      - the index of the argument in the arg list
     * @param parmList   - the list of args
     * 
     * @return the value of the IntArray argument (it should always be valid)
     * 
     * @throws ParserException 
     */    
    public static ArrayList<Long> checkArgTypeIntArray (int index, ArrayList<ParameterStruct> parmList) throws ParserException {
        checkArgType (index, ParameterStruct.ParamType.IntArray, parmList);
        return parmList.get(index).getIntArray();
    }
    
    /**
     * checks if the specified argument compiles with the Array Filter type.
     * 
     * @param index      - the index of the argument in the arg list
     * @param vartype    - the type of variable it is being assigned to (Array types only)
     * @param parmList   - the list of args
     * 
     * @throws ParserException 
     */    
    public void checkArgFilterValue (int index, ParameterStruct.ParamType vartype, ArrayList<ParameterStruct> parmList) throws ParserException {
         String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (parmList == null) {
            throw new ParserException(functionId + "Null parameter list");
        }
        if (parmList.size() <= index) {
            throw new ParserException(functionId + "Missing arguments in list - number found: " + parmList.size());
        }

        ParameterStruct arg = parmList.get(index);
        String argValue = arg.getStringValue();
        switch (vartype) {
            case StrArray:
                // String array just requires whether we are filtering left or right side and if inverting the filter
                if (arg.isVariableRef()) {
                    ParameterStruct.ParamType vtype = PreCompile.variables.getVariableTypeFromName(argValue);
                    if (vtype == null || (vtype != ParameterStruct.ParamType.String && vtype != ParameterStruct.ParamType.StrArray)) {
                        throw new ParserException(functionId + "Invalid variable type " + vtype + " for " + vartype + " Filter argument: " + argValue);
                    }
                } else {
                    switch (argValue) {
                        case "!":
                        case "!LEFT":
                        case "!RIGHT":
                        case "LEFT":
                        case "RIGHT":
                            break;
                        default:
                            throw new ParserException(functionId + "Invalid Filter argument for " + vartype + " type: " + argValue);
                    }
                }
                break;
            case IntArray:
                // Int Array should have 2 arguments
                if (parmList.size() <= index + 1) {
                    throw new ParserException(functionId + "Missing arguments in list - number found: " + parmList.size());
                }
                
                // this 1st arg is the comparison type to use for filtering
                if (arg.isVariableRef()) {
                    ParameterStruct.ParamType vtype = PreCompile.variables.getVariableTypeFromName(argValue);
                    if (vtype == null || (vtype != ParameterStruct.ParamType.String && vtype != ParameterStruct.ParamType.StrArray)) {
                        throw new ParserException(functionId + "Invalid Filter argument for " + vartype + " type: " + argValue);
                    }
                } else {
                    switch (argValue) {
                        case "==":
                        case "!=":
                        case ">=":
                        case "<=":
                        case ">":
                        case "<":
                            break;
                        default:
                            throw new ParserException(functionId + "Invalid Filter argument for " + vartype + " type: " + argValue);
                    }
                }
                
                // now verify we have an integer value for the filter data
                checkArgType (index + 1, ParameterStruct.ParamType.Integer, parmList);
                break;
            default:
                throw new ParserException(functionId + "Invalid variable type (must be an Array type): " + vartype);
        }
    }
    
    /**
     * This takes a command line and extracts the parameter list from it.
     * This simply separates the String of arguments into separate parameter
     *   values based on where it finds commas and quotes.
     * 
     * @param line - the string of parameters to separate and classify
     * @param bParamAssign - true if parameter assignment, so 1st value is a parameter
     * 
     * @return the ArrayList of arguments for the command
     * 
     * @throws ParserException 
     */
    public static ArrayList<ParameterStruct> packParameters (String line, boolean bParamAssign) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        ArrayList<ParameterStruct> params = new ArrayList<>();
        ParameterStruct arg;
        String paramName = null;

        try {
        for (int ix = 0; ! line.isEmpty(); ix++) {
            // read next entry
            ParameterStruct.ParamType paramType = ParameterStruct.ParamType.String;
            line = line.strip();
            String nextArg = getNextWord (line);
            line = line.substring(nextArg.length());
            nextArg = nextArg.strip();

            // if this is the 1st parameter of a parameter assignment, the 1st param
            // must be the parameter name. verify it is valid.
            if (bParamAssign) {
                if (ix == 0) {
                    PreCompile.variables.checkValidVariable(Variables.VarCheck.SET, nextArg);
                    if (line.isEmpty()) {
                        throw new ParserException(functionId + "no arguments following Variable name: " + nextArg);
                    }
                    // check if this is a String parameter (we may have extra stuff to do here)
                    if (PreCompile.variables.getVariableTypeFromName(nextArg) == ParameterStruct.ParamType.String) {
                        paramName = "$" + nextArg;
                    }
                } else if (ix == 1 && paramName != null && nextArg.contentEquals("+=")) {
                    // if it is a String parameter and the 2nd entry is a "+=", then we need to
                    //  insert the current parameter value at the begining of the list of Strings to add
                    line = line.strip();
                    nextArg = "=";
                    arg = new ParameterStruct (nextArg, ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.String);
                    frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + paramType + " value: " + nextArg);
                    params.add(arg);
                    arg = new ParameterStruct (paramName, ParameterStruct.ParamClass.Reference, ParameterStruct.ParamType.String);
                    frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + paramType + " value: " + paramName);
                    params.add(arg);
                    nextArg = "+";
                    arg = new ParameterStruct (nextArg, ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.String);
                    frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + paramType + " value: " + nextArg);
                    params.add(arg);
                    continue;
                }
            }
            
            // determine if we have a series of Strings or Integers
            if (nextArg.charAt(0) == '{') {
                // check if matching brace
                line = line.strip();
                int offset = line.indexOf('}');
                if (offset >= 0) {
                    // matching brace found...
                    // remove the begining brace and copy the characters up to
                    // the end brace into the arg parameter
                    nextArg = nextArg.substring(1);
                    nextArg += line.substring(0, offset-1);

                    // now remove the rest of the list from the line
                    if (offset == line.length() - 1) {
                        line = "";
                    } else {
                        line = line.substring(offset + 1);
                    }
                    
                    // place them in the proper list structure
                    paramType = ParameterStruct.ParamType.StrArray;
                    arg = new ParameterStruct (nextArg, ParameterStruct.ParamClass.Discrete, paramType);
                    frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + paramType + " value: [ " + nextArg + " ]");
                    params.add(arg);
                    continue;
                }
            }
            
            // determine if we have a quoted String
            if (nextArg.charAt(0) == '"') {
                // check if matching quote
                int offset = line.indexOf('"');
                if (offset >= 0) {
                    // matching quote found...
                    // remove the begining quote and copy the characters up to
                    // the quote into the arg parameter
                    nextArg = nextArg.substring(1);
                    nextArg += line.substring(0, offset);

                    // now remove the rest of the quoted string from the line
                    if (offset == line.length() - 1) {
                        line = "";
                    } else {
                        line = line.substring(offset + 1);
                    }
                }
            }
            line = line.strip();
            
            // determine the data type
            if (paramType == ParameterStruct.ParamType.String) {
                if (nextArg.equalsIgnoreCase("TRUE") ||
                    nextArg.equalsIgnoreCase("FALSE")) {
                    paramType = ParameterStruct.ParamType.Boolean;
                } else {
                    try {
                        Long longVal = ParameterStruct.getLongOrUnsignedValue(nextArg);
                        if (ParameterStruct.isUnsignedInt(longVal))
                            paramType = ParameterStruct.ParamType.Unsigned;
                        else
                            paramType = ParameterStruct.ParamType.Integer;
                    } catch (ParserException ex) {
                        paramType = ParameterStruct.ParamType.String;
                    }
                }
            }

            // create the parameter entry and add it to the list of parameters
            ParameterStruct.ParamClass pClass = ParameterStruct.ParamClass.Discrete;
            if (nextArg.startsWith("$") || (bParamAssign && params.isEmpty())) {
                pClass = ParameterStruct.ParamClass.Reference;
                paramType = PreCompile.variables.getVariableTypeFromName(nextArg);
            }
            
            arg = new ParameterStruct(nextArg, pClass, paramType);
            frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + paramType + " value: " + nextArg);
            params.add(arg);
        }
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId + " bParamAssign = " + bParamAssign);
        }
        
        return params;
    }

    /**
     * Converts a packed list of arguments into a simple list of String entries.
     * This takes an already packed list of arguments of the string concatenation
     *  and converts it into a simple list of string entries to be combined.
     *  We start at the end of the already packed string list so we can eliminate
     *  entries without screwing up the indexing.
     * Since the list is assumed to be: { value + value + value ... + value },
     *  we simply start at the last + entry and work to the first, verifying
     *  it is a + sign and eliminating it.
     * 
     * @param params - the list of string arguments having '+' signs separating the values
     * @param offset - the number of arguments in the list that preceed the string arguments
     *                 (such as the name of the param being assigned and the = sign)
     * 
     * @return a list of the strings without the + entries
     */
    public ArrayList<ParameterStruct> packStringConcat (ArrayList<ParameterStruct> params, int offset) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // start at the end of the list with the last + entry and work backwards.
        // The last  + will be the 2nd to the last entry at params.size() - 2.
        // The first + will be the 2nd entry
        for (int ix = params.size() - 2; ix > 0 + offset; ix-=2) {
            String sign = params.get(ix).getStringValue();
            if (sign.contentEquals("+")) {
                params.remove(ix);
            }
            else {
                throw new ParserException(functionId + "Invalid String concatenation");
            }
        }
        return params;
    }
    
    /**
     * This takes a command line and extracts the calculation parameter list from it.
     * For Integers and Unsigneds this will be in the form:
     *      VarName = Calculation     (+=, -=, *=, ... also allowed in place of =)
     * For Booleans, it will be:
     *      VarName = Calculation compSign Calculation  (compSign: ==, !=, >=, ...}
     * For IntArrays and StringArrays it will be:
     *      Command VarName Calculation
     * 
     * where: Calculation will be a string or one or more values/variables with
     *        associated parenthesis and operations.
     * 
     * @param line  - the string of parameters to separate and classify
     * @param ptype - the type of parameter being set
     * 
     * @return the ArrayList of arguments for the command
     * 
     * @throws ParserException 
     */
    public ArrayList<ParameterStruct> packCalculation (String line, ParameterStruct.ParamType ptype) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (ptype == ParameterStruct.ParamType.String) {
            throw new ParserException(functionId + "Assignment command not allowed for type: " + ptype);
        }
        
        ArrayList<ParameterStruct> params = new ArrayList<>();
        ParameterStruct parm;
        
        // 1st entry should be the parameter name
        String paramName = getParamName (line);
        try {
            PreCompile.variables.checkValidVariable(Variables.VarCheck.SET, paramName);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        if (line.contentEquals(paramName)) {
            throw new ParserException(functionId + "no arguments following parameter name: " + line);
        }

        frame.outputInfoMsg(STATUS_COMPILE, "     * Repacking parameters for Calculation");

        try {
            // the 1st argument of a SET command is the parameter name to assign the value to
            line = line.substring(paramName.length()).strip();
            parm = new ParameterStruct(paramName, ParameterStruct.ParamClass.Reference, ptype);
            frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + paramName);
            params.add(parm);

            // next entry should be the equality sign (except for Arrays)
            switch (ptype) {
                case Integer:
                case Unsigned:
                case Boolean:
                    String nextArg = getNextWord (line);
                    line = line.substring(nextArg.length()).strip();
                    String newOp = nextArg.substring(0, nextArg.length() - 1);
                    nextArg = nextArg.strip();
                    switch (nextArg) {
                        case "=", "+=", "-=", "*=", "/=", "%=", "AND=", "OR=", "XOR=" -> {
                        }
                        default -> throw new ParserException(functionId + "invalid equality sign: " + nextArg);
                    }

                    // bitwise ops are only allowed for Unsigned type
                    if (ptype != ParameterStruct.ParamType.Unsigned) {
                        if (newOp.equals("AND") || newOp.equals("OR") || newOp.equals("XOR")) {
                            throw new ParserException(functionId + "Bitwise assignments not allowed for type: " + ptype + ": " + newOp);
                        }
                    } else if (ptype == ParameterStruct.ParamType.Boolean && ! newOp.equals("=")) {
                        throw new ParserException(functionId + "No modifiers in equals allowed for type: " + ptype + ": " + newOp);
                    }

                    // this will pack the "=" sign
                    ParameterStruct.ParamType newParam = ParameterStruct.ParamType.String;
                    parm = new ParameterStruct("=", ParameterStruct.ParamClass.Discrete, newParam);
                    frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + newParam + " value: =");
                    params.add(parm);

                    // if there was an operation preceeding the "=" sign, let's sneek the operation in here
                    //  by adding it to the rest of the line
                    if (!newOp.isEmpty()) {
                        // first we add the parameter name that has the $ attached, so it becomes a reference value,
                        // followed by the operation to perform, followed by the opening parenthesis and then
                        // the remainder of the calculation, then we end it with the closing parenthesis.
                        // (the parenthesis are included to assure that the newOp operation is performed last.
                        line = "$" + paramName + " " + newOp + " (" + line + ")";
                    }
                    break;

                default:
                    break;
            }

            // check if Boolean type, which must have a comparison of 2 calculations
            if (ptype == ParameterStruct.ParamType.Boolean) {
                ArrayList<ParameterStruct> compParams = packComparison(line);
                params.addAll(compParams);
            } else {
                // else, numeric type: remaining data is a single Calculation
                parm = new ParameterStruct(line, ParameterStruct.ParamClass.Calculation, ptype);
                frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + ptype + " value: " + line);
                params.add(parm);
            }
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId );
        }
        
        return params;
    }
    
    /**
     * This extracts a comparison parameter list from it.
     * This is defines as the following format:
     *      Calculation compSign Calculation
     * 
     * Where: compSign: { ==, !=, >=, <=, >, < }
     * 
     *        Calculation is a string or one or more values/variables with
     *        associated parenthesis and operations.
     * 
     * @param line  - the string of parameters to separate and classify
     * 
     * @return the ArrayList of arguments for the command
     * 
     * @throws ParserException 
     */
    public ArrayList<ParameterStruct> packComparison (String line) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        ArrayList<ParameterStruct> params = new ArrayList<>();
        ParameterStruct parm;
        ParameterStruct.ParamType ptype1, ptype2;
        ParameterStruct.ParamClass pclass1, pclass2;
        
        frame.outputInfoMsg(STATUS_COMPILE, "     * Repacking parameters for Comparison");
        
        // check for 'NOT' character
        boolean bNot = false;
        line = line.strip();
        if (line.startsWith("!")) {
            bNot = true;
            line = line.substring(1).strip();
        } else if (line.startsWith("NOT")) {
            bNot = true;
            line = line.substring(1).strip();
        }

        // search for required comparison sign
        String compSign = "==";
        int offset = line.indexOf(compSign);
        if (offset <= 0) {
            compSign = "!=";
            offset = line.indexOf(compSign);
        }
        if (offset <= 0) {
            compSign = ">=";
            offset = line.indexOf(compSign);
        }
        if (offset <= 0) {
            compSign = "<=";
            offset = line.indexOf(compSign);
        }
        if (offset <= 0) {
            compSign = ">";
            offset = line.indexOf(compSign);
        }
        if (offset <= 0) {
            compSign = "<";
            offset = line.indexOf(compSign);
        }
        if (offset <= 0) {
            // this is a single boolean entry rather than a comparison
            pclass1 = (line.startsWith("$") ? ParameterStruct.ParamClass.Reference : ParameterStruct.ParamClass.Discrete);
            ptype1 = ParameterStruct.ParamType.Boolean;
            parm = new ParameterStruct(line, pclass1, ptype1);
            frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + ptype1 + " value: " + line);
            params.add(parm);
            
            if (bNot) {
                String value = "!";
                pclass2 = ParameterStruct.ParamClass.Discrete;
                ptype2 = ParameterStruct.ParamType.String;
                parm = new ParameterStruct(value, pclass2, ptype2);
                frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + ptype2 + " value: " + value);
                params.add(parm);
            }
            return params;
        }
        if (bNot) {
            throw new ParserException(functionId + "Can't use '!' modifier on comparison: ! " + compSign);
        }

        String prefix = line.substring(0, offset).strip();
        line = line.substring(offset + compSign.length()).strip();

        // Need to determine if the each comparison entry is a single word and
        //   neither is a numeric. That would make this a String comparison.
        // So if either entry is more than 1 word, we must assume a numeric comparison.
        // Otherwise, if either comparison is in quotes, we must assume a String comparison.
        // Otherwise, if either side is a numeric (value or Variable), assume numeric comparison.
        // Otherwise, do String comparison.
        boolean bCalc = false;
        boolean bQuote = false;
        // if we have more than 1 entry on either side, it must be a numeric calculation
//        if (countArgs(prefix) > 1 || countArgs(line) > 1) {
//            bCalc = true;
//        } else {
            // or, if either entry is a quoted string, remove the quotes from it
            bQuote = (prefix.charAt(0) == '\"' || line.charAt(0) == '\"');
            prefix = extractQuotedString (prefix);
            line   = extractQuotedString (line);
//        }
            
        if (! bCalc && ! bQuote) {
            // if neither of the above, let's determine if the entries are numeric or not.
            ParameterStruct.ParamType ctype1, ctype2;
            ctype1 = ParameterStruct.classifyDataType(prefix);
            ctype2 = ParameterStruct.classifyDataType(line);
            if (ctype1 == ParameterStruct.ParamType.Integer  ||
                ctype1 == ParameterStruct.ParamType.Unsigned ||
                ctype2 == ParameterStruct.ParamType.Integer  ||
                ctype2 == ParameterStruct.ParamType.Unsigned) {
                bCalc = true;
            }
        }
            
        // now set the type of comparison we are doing: Integer or String.
        if (bCalc) {
            ptype1 = ParameterStruct.ParamType.Integer;
            ptype2 = ParameterStruct.ParamType.Integer;
            pclass1 = ParameterStruct.ParamClass.Calculation;
            pclass2 = ParameterStruct.ParamClass.Calculation;
        } else {
            ptype1 = ParameterStruct.ParamType.String;
            ptype2 = ParameterStruct.classifyDataType(line);
            pclass1 = (prefix.startsWith("$") ? ParameterStruct.ParamClass.Reference : ParameterStruct.ParamClass.Discrete);
            pclass2 = (line.startsWith("$") ? ParameterStruct.ParamClass.Reference : ParameterStruct.ParamClass.Discrete);
        }

        // first add the initial value, which will usually be a Variable or a Discrete value.
        parm = new ParameterStruct(prefix, pclass1, ptype1);
        frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + ptype1 + " value: " + prefix);
        params.add(parm);
        
        // now add the comparison sign
        parm = new ParameterStruct(compSign, ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.String);
        frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type String value: " + compSign);
        params.add(parm);
            
        // remaining data is the Calculation, which may be a single value or a complex formula
        parm = new ParameterStruct(line, pclass2, ptype2);
        frame.outputInfoMsg(STATUS_COMPILE, "     packed entry [" + params.size() + "]: type " + ptype2 + " value: " + line);
        params.add(parm);

        return params;
    }

    /**
     * determine the number of arguments in the string.
     * 
     * @param line - the string containing 0 or more words
     * 
     * @return the number of arguments found
     * 
     * @throws ParserException
     */
    public static int countArgs (String line) throws ParserException {
        line = line.strip();
        if (line.isBlank()) {
            return 0;
        }
        if (line.charAt(0) == '\"' && line.charAt(line.length()-1) == '\"') {
            return 1;
        }

        int count = 1;
        for (int ix = 0; ix < line.length(); ix++) {
            if (line.charAt(ix) != ' ') {
                int offset = line.indexOf(" ", ix);
                if (offset < 0) {
                    break;
                }
                ix = offset;
                count++;
            }
        }
        return count;
    }

    /**
     * extracts the contents within a quoted string.
     * 
     * @param line - the string to extract the data from
     * 
     * @return the string value between the quotes (original string if it is not a quoted string)
     * 
     * @throws ParserException
     */
    public static String extractQuotedString (String line) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (line == null || line.isBlank() || line.charAt(0) != '\"') {
            return line;
        }
        int offset = line.indexOf('\"', 1);
        if (offset < 0) {
            throw new ParserException(functionId + "Missing ending quote for String");
        }
        return line.substring(1, offset);
    }

    /**
     * get the next word in a string of words.
     * 
     * @param line - the string containing 0 or more words
     * 
     * @return the next word in the line (empty string if no more words)
     */
    private static String getNextWord (String line) {
        if (line.isBlank()) {
            return "";
        }
        int offset = line.indexOf(" ");
        if (offset <= 0) {
            return line;
        }
        return line.substring(0, offset);
    }

    /**
     * extracts the parameter name from the string.
     * It searches only for the valid chars in a parameter name and stops on the first value
     * that i not valid, so that it doesn't depend on any particular delimiter, such as whitespace.
     * Therefore, if the parameter name has an extension added to the end of it, this
     * will only return the parameter name portion of it.
     * 
     * @param line - the string to extract the parameter name from
     * 
     * @return the parameter name (if one existed)
     */
    public static String getParamName (String line) {
        for (int ix = 0; ix < line.length(); ix++) {
            if (! Character.isLetterOrDigit(line.charAt(ix)) && line.charAt(ix) != '_')
                return line.substring(0, ix);
        }
        return line;
    }
    
}
