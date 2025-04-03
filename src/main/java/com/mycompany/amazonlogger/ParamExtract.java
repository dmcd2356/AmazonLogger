/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

/**
 *
 * @author dan
 */

public class ParamExtract {
    private static final String CLASS_NAME = "ParamExtract";
    
    String  name;           // parameter name (null if not found)
    char    type;           // type of parameter (0 if not found)
    char    delimiter;      // delimiter char    (0 if not found)
    String  addendum;       // object after either '.' or '@' demarcation
    String  remainder;      // remaining string after extraction of above
        
    ParamExtract (String field) throws ParserException {
        String functionId = CLASS_NAME + " (new): ";
        
        field = field.strip();
        name = null;
        addendum = null;
        remainder = null;
        delimiter = 0;
        
        // first, let's see if there are extraneous space characters following
        //  the potential parameter name and an '=' sign (can't have intervening
        //  spaces for either the '.' or '@' delimiters).
        // If we find the next non-space char is the '=', remove the intervening
        //  space characters.
        int offset = field.indexOf(' ');
        int equals = field.indexOf('=');
        if (offset > 0 && equals > 0 && offset < equals) {
            String prefix = field.substring(0, offset);
            for (; field.charAt(offset) == ' ' && offset < field.length(); offset++) { }
            if (field.charAt(offset) == '=') {
                field = prefix + field.substring(offset);
            }
        }
        
        // extract the name of the parameter from the string
        // it can be delimited by any of the following: { space = . [ @ }.
        for (int ix = 0; ix < field.length(); ix++) {
            if (field.charAt(ix) == ' ' || field.charAt(ix) == '=' ||
                field.charAt(ix) == '.' || field.charAt(ix) == '[' || field.charAt(ix) == '@') {
                name = field.substring(0, ix);
                delimiter = field.charAt(ix);
                String leftover = "";
                if (field.length() > ix + 1) {
                    leftover = field.substring(ix + 1);
                }
                String strType = ParameterStruct.isParamDefined(name);
                if (strType == null) {
                    throw new ParserException(functionId + "Parameter not defined prior to assignment: " + name);
                }
                type = strType.charAt(0);
                if (delimiter == '.' || delimiter == '@') {
                    if (leftover.charAt(ix) == ' ') {
                        throw new ParserException(functionId + "Missing info for parameter: " + name + delimiter);
                    }
                    offset = leftover.indexOf(' ');
                    if (offset <= 0) {
                        addendum = leftover;
                        remainder = null;
                    } else {
                        addendum = leftover.substring(0, offset);
                        remainder = leftover.substring(offset).strip();
                    }
                } else {
                    remainder = leftover.strip();
                }
                return;
            }
        }
    }
    
    String getName () {
        return name;
    }
    
    char getType () {
        return type;
    }
    
    char getDelimiter () {
        return delimiter;
    }
    
    String getAddendum () {
        return addendum;
    }
    
    String getRemainder () {
        return remainder;
    }
}
    
