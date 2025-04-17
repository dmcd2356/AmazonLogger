/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;

/**
 *
 * @author dan
 */

public class VariableExtract {
    private static final String CLASS_NAME = "ParamExtract";
    
    private String  name;           // Variable name (null if not found)
    private ParameterStruct.ParamType type; // type of parameter
    private String  equalSign;      // type of '=' sign found (null if none)
    private Integer index;          // associated index [x] for String, StrArray, IntArray params
    private Integer indexmax;       // associated ending index for String, StrArray, IntArray params
    private Trait   trait;          // object after '.' demarcation
    private String  evaluation;     // evaluation on Right-side of '=' to set parameter to
    private boolean bRightSide;     // true if parameter is a reference and must be on right side of '='

    // traits extensions to String, StrArray and IntArray types
    public enum Trait {
        UPPER,              // String: convert all chars to uppercase
        LOWER,              // String: convert all chars to lowercase
        SIZE,               // number of chars for String, number of elements for Arrays
        ISEMPTY,            // check if item is zero-length (not null)
    }

    /**
     * extracts the Variable and any extensions from a command line.
     * 
     * Assumes the 'field' passed to this begins with a parameter name.
     * It can be either a command calculation of the form:
     *      VarName = Calculation
     * Or it can be within the Calculation of the Right-side of the '=':
     *      $VarName
     * The 1st case will not have a '$' in front of the parameter name and should
     *   have an '=' sign in it. It cannot have any extensions to it (brackets or
     *   Traits).
     * The 2nd form must be preceded by a '$' character and must not have any '='
     *   character following the parameter name. It can have either brackets
     *   enclosing a numeric or 2 numerics separated by a dash or a '.' followed
     *   by a Trait of the parameter if the parameter type supports either of these
     *   (Strings, StrArrays, and IntArrays only).
     * 
     * @param field - the command line input containing the parameter name to extract
     * 
     * @throws ParserException 
     */    
    VariableExtract (String field) throws ParserException {
        String functionId = CLASS_NAME + " (new): ";
        
        field = field.strip();
        
        name = field;
        index = null;
        indexmax = null;
        trait = null;
        evaluation = null;
        equalSign = null;
        bRightSide = name.startsWith("$");
        if (name.isEmpty()) {
            return;
        }
        
        // let's allow a "++" to be at the end of a param value (no intervening spaces)
        int offset = name.indexOf(' ');
        if (offset < 0) {
            // only 1 field, check if last 2 chars are ++
            int strlen = name.length();
            if (name.substring(strlen-2).contentEquals("++")) {
                // ok, then let's replace the "++" with a "+= 1"
                name = name.substring(0, strlen-2);
                equalSign = "+=";
                evaluation = "1";
                getExtensions ();
                return;
            }
        }
        
        // first, let's see if there are extraneous space characters following
        //  the potential parameter name and an '=' sign (can't have intervening
        //  spaces for either the '.' or '[' delimiters).
        // 'field' then is modified to be terminated with a space char or end-of-line.
        int equals = name.indexOf('=');
        if (equals > 0) {
            offset = equals;
            equalSign = "=";
            char signVal = name.charAt(offset-1);
            switch (signVal) {
                case '+', '-', '*', '/', '%' -> {
                    equalSign = signVal + "=";
                    offset--;
                }
                default -> {
                    int backup = offset - 4;
                    if (backup >= 0) {
                        String preEqu = name.substring(backup);
                        if (preEqu.startsWith(" AND=")) {
                            equalSign = "AND=";
                            offset = backup;
                        } else if (preEqu.startsWith(" XOR=")) {
                            equalSign = "XOR=";
                            offset = backup;
                        } else {
                            preEqu = preEqu.substring(1);
                            if (preEqu.startsWith(" OR=")) {
                                equalSign = "OR=";
                                offset = backup + 1;
                            }
                        }
                    }
                }
            }
            evaluation = name.substring(equals+1).strip();
            name = name.substring(0, offset).strip();

            if (evaluation.isEmpty()) {
                throw new ParserException(functionId + "Invalid command format (missing data after '='): " + field);
            }
            if (bRightSide) {
                throw new ParserException(functionId + "Invalid command format ('$' can't be used on left-side of '='): " + field);
            }
        }
        else if (offset > 0) {
            evaluation = name.substring(offset).strip();
            name = name.substring(0, offset).strip();
        }
        
        // extract the name of the parameter from the string.
        // it can be delimited by either a space, a '.' or a ['.
        try {
            getExtensions ();
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
    }

    /**
     * this looks at the parameter reference name and separates out any extensions.
     *  the 'name' will be set to the parameter name by itself
     *  bracket index values will set the entries 'index' and 'indexmax' (if any)
     *  traits will be saved in the entry 'trait'
     * 
     * @throws ParserException 
     */
    private void getExtensions () throws ParserException {
        String functionId = CLASS_NAME + ".getExtensions: ";
        
        // check for bracketed index or trait extensions
        String field = name;
        String leftover = "";
        int offTrait  = field.indexOf('.');
        int offLeftB  = field.indexOf('[');
        int offRightB = field.indexOf(']');
        if (offTrait > 0) {
            if (! bRightSide) {
                throw new ParserException(functionId + "Traits only allowed for Right-side Variable usage: " + field + evaluation);
            }
            // we have a trait combined with the param name
            name = field.substring(0, offTrait);
            type = ParameterStruct.getVariableTypeFromName (name);
            if (field.length() > offTrait + 1) {
                leftover = field.substring(offTrait + 1);
            }
            for (Trait entry : Trait.values()) {
                if (entry.toString().contentEquals(leftover)) {
                    trait = entry;
                    frame.outputInfoMsg(STATUS_PROGRAM, "Variable trait found: " + name + "." + leftover);
                    break;
                }
            }
            if (type != ParameterStruct.ParamType.String && (trait == Trait.LOWER || trait == Trait.UPPER)) {
                throw new ParserException(functionId + "Invalid Trait for Array Variable: " + leftover);
            }
            if (trait == null) {
                throw new ParserException(functionId + "Invalid Trait for Variable: " + leftover);
            }
        } else if (offLeftB > 0) {
            if (! bRightSide) {
                throw new ParserException(functionId + "Brackets only allowed for Right-side Variable usage: " + field + evaluation);
            }
            // we have an index associated with the param
            name = field.substring(0, offLeftB);
            type = ParameterStruct.getVariableTypeFromName (name);
            if (field.length() > offRightB && offRightB > offLeftB) {
                leftover = field.substring(offLeftB + 1, offRightB);
            } else {
                throw new ParserException(functionId + "Invalid bracketing of Variable: " + name + " = " + leftover);
            }
            // now see if we have a single entry or a range
            int offset = leftover.indexOf('-');
            if (offset > 0) {
                index = Utils.getIntValue (leftover.substring(0, offset)).intValue();
                indexmax = Utils.getIntValue (leftover.substring(offset+1)).intValue();
                frame.outputInfoMsg(STATUS_PROGRAM, "Variable index range found: " + name + "[" + index + "-" + indexmax + "]");
            } else {
                index = Utils.getIntValue (leftover).intValue();
                indexmax = null;
                frame.outputInfoMsg(STATUS_PROGRAM, "Variable index entry found: " + name + "[" + index + "]");
            }
        } else {
            // no additional entries, the param name must be by itself
            name = field;
            type = ParameterStruct.getVariableTypeFromName (name);
        }
        
        // verify Variable name is valid
        boolean bValid;
        try {
            bValid = ParameterStruct.isValidVariableName(name);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        if (! bValid) {
            throw new ParserException(functionId + "Variable not found: " + name);
        }
    }
    
    public String getName () {
        return name;
    }
    
    public ParameterStruct.ParamType getType () {
        return type;
    }
    
    public boolean isEquation () {
        return (equalSign != null);
    }
    
    public String getEquality () {
        return equalSign;
    }
    
    public Integer getIndex () {
        return index;
    }
    
    public Integer getIndexEnd () {
        return indexmax;
    }
    
    public Trait getTrait () {
        return trait;
    }
    
    public String getEvaluation () {
        return evaluation;
    }
}
    
