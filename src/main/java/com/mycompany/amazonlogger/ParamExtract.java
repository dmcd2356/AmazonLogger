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

public class ParamExtract {
    private static final String CLASS_NAME = "ParamExtract";
    
    private String  name;           // parameter name (null if not found)
    private ParameterStruct.ParamType type; // type of parameter
    private boolean bEquals;        // true if '=' sign was found
    private Integer index;          // associated index [x] for String, StrArray, IntArray params
    private Integer indexmax;       // associated ending index for String, StrArray, IntArray params
    private Trait   trait;          // object after '.' demarcation
    private String  evaluation;     // evaluation on Right-side of '=' to set parameter to

    // traits extensions to String, StrArray and IntArray types
    public enum Trait {
        UPPER,              // String: convert all chars to uppercase
        LOWER,              // String: convert all chars to lowercase
        SIZE,               // number of chars for String, number of elements for Arrays
        ISEMPTY,            // check if item is zero-length (not null)
    }

    /**
     * extracts the parameter and any extensions from a command line.
     * 
     * Assumes the 'field' passed to this begins with a parameter name.
     * It can be either a command calculation of the form:
     *      ParamName = Calculation
     * Or it can be within the Calculation of the Right-side of the '=':
     *      $ParamName
     * The 1st case will not have a '$' infront of the parameter name and should
     *   have an '=' sign in it. It cannot have any extensions to it (brackets or
     *   Traits).
     * The 2nd form must be preceeded by a '$' character and must not have any '='
     *   character following the parameter name. It can have either brackets
     *   enclosing a numeric or 2 numerics seperated by a dash or a '.' followed
     *   by a Trait of the parameter if the parameter type supports either of these
     *   (Strings, StrArrays, and IntArrays only).
     * 
     * @param field - the command line input containing the parameter name to extract
     * 
     * @throws ParserException 
     */    
    ParamExtract (String field) throws ParserException {
        String functionId = CLASS_NAME + " (new): ";
        
        field = field.strip();
        name = null;
        index = null;
        indexmax = null;
        trait = null;
        evaluation = null;
        bEquals = false;
        
        // first, let's see if there are extraneous space characters following
        //  the potential parameter name and an '=' sign (can't have intervening
        //  spaces for either the '.' or '[' delimiters).
        // 'field' then is modified to be terminated with a space char or end-of-line.
        boolean bRightSide = field.startsWith("$");
        int offset = field.indexOf(' ');
        int equals = field.indexOf('=');
        if (offset > 0 || equals > 0) {
            if (offset < equals)
                offset = equals;
            evaluation = field.substring(offset).strip();
            field = field.substring(0, offset);
            equals = evaluation.indexOf('=');
            if (equals >= 0) {
                if (equals >= evaluation.length() - 1) {
                    throw new ParserException(functionId + "Invalid command format (missing data after '='): " + field + evaluation);
                }
                if (bRightSide) {
                    throw new ParserException(functionId + "Invalid command format ('$' can't be used on left-side of '='): " + field + evaluation);
                }
                bEquals = true;
                evaluation = evaluation.substring(equals + 1).strip();
            }
        }
        
        // extract the name of the parameter from the string.
        // it can be delimited by either a space, a '.' or a ['.
        String leftover = "";
        int offTrait  = field.indexOf('.');
        int offLeftB  = field.indexOf('[');
        int offRightB = field.indexOf(']');
        if (offTrait > 0) {
            if (! bRightSide) {
                throw new ParserException(functionId + "Traits only allowed for Right-side parameter usage: " + field + evaluation);
            }
            // we have a trait combined with the param name
            name = field.substring(0, offTrait);
            type = ParameterStruct.getParamTypeFromName (name);
            if (field.length() > offTrait + 1) {
                leftover = field.substring(offTrait + 1);
            }
            for (Trait entry : Trait.values()) {
                if (entry.toString().contentEquals(leftover)) {
                    trait = entry;
                    frame.outputInfoMsg(STATUS_PROGRAM, "Parameter trait found: " + name + "." + leftover);
                    break;
                }
            }
            if (type != ParameterStruct.ParamType.String && (trait == Trait.LOWER || trait == Trait.UPPER)) {
                throw new ParserException(functionId + "Invalid Trait for Array parameter: " + leftover);
            }
            if (trait == null) {
                throw new ParserException(functionId + "Invalid Trait for parameter: " + leftover);
            }
        } else if (offLeftB > 0) {
            if (! bRightSide) {
                throw new ParserException(functionId + "Brackets only allowed for Right-side parameter usage: " + field + evaluation);
            }
            // we have an index associated with the param
            name = field.substring(0, offLeftB);
            type = ParameterStruct.getParamTypeFromName (name);
            if (field.length() > offRightB && offRightB > offLeftB) {
                leftover = field.substring(offLeftB + 1, offRightB);
            } else {
                throw new ParserException(functionId + "Invalid bracketing of parameter: " + name + " = " + leftover);
            }
            // now see if we have a single entry or a range
            offset = leftover.indexOf('-');
            if (offset > 0) {
                index = Utils.getIntValue (leftover.substring(0, offset)).intValue();
                indexmax = Utils.getIntValue (leftover.substring(offset+1)).intValue();
                frame.outputInfoMsg(STATUS_PROGRAM, "Parameter index range found: " + name + "[" + index + "-" + indexmax + "]");
            } else {
                index = Utils.getIntValue (leftover).intValue();
                indexmax = null;
                frame.outputInfoMsg(STATUS_PROGRAM, "Parameter index entry found: " + name + "[" + index + "]");
            }
        } else {
            // no additional entries, the param name must be by itself
            name = field;
            type = ParameterStruct.getParamTypeFromName (name);
        }
    }
    
    String getName () {
        return name;
    }
    
    ParameterStruct.ParamType getType () {
        return type;
    }
    
    boolean isEquation () {
        return bEquals;
    }
    
    Integer getIndex () {
        return index;
    }
    
    Integer getIndexEnd () {
        return indexmax;
    }
    
    Trait getTrait () {
        return trait;
    }
    
    String getEvaluation () {
        return evaluation;
    }
}
    
