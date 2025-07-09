/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import com.mycompany.amazonlogger.GUILogPanel.MsgType;

/**
 *
 * @author dan
 */

public class VarExtract {

    private static final String CLASS_NAME = VarExtract.class.getSimpleName();
    
    private String  name;           // Variable name (null if not found)
    private ParameterStruct.ParamType type; // type of parameter
    private String  equalSign;      // type of '=' sign found (null if none)
    private BracketIx index;        // associated index [x] for String, StrArray, IntArray params
    private BracketIx indexmax;     // associated ending index for String, StrArray, IntArray params
    private TraitInfo.Trait trait;          // object after '.' demarcation
    private String  evaluation;     // evaluation on Right-side of '=' to set parameter to
    private boolean bRightSide;     // true if parameter is a reference and must be on right side of '='

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
     *   character following the parameter name. It can have 1 of 2 types of extensions:
     *   - Brackets: enclosing a numeric or 2 numerics separated by a dash or a '.'.
     *       The numeric value can be a discreet number (non-negative) or can be
     *       either an Integer or Unsigned reference variable (no Calculation).
     *   - Trait: a characteristic of the parameter instead of the actual parameter,
     *       Traits are only supported by Strings, StrArrays, and IntArrays and
     *       the $DATA reserved variable.
     * 
     * NOTE: this is only called during the Compilation phase, so parameters
     *       cannot be converted to their values yet. That should be done during
     *       the execution phase by Variables.getVariableInfo. This will reach into
     *       VariableInfo.getIndexStart() and End() to get the current value.
     * 
     * @param field - the command line input containing the parameter name to extract
     * 
     * @throws ParserException 
     */    
    VarExtract (String field) throws ParserException {
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
            Utils.throwAddendum (exMsg.getMessage(), functionId);
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        // check for bracketed index or trait extensions
        String field = name;
        String leftover = "";
        int offTrait  = field.indexOf('.');
        int offLeftB  = field.indexOf('[');
        int offRightB = field.indexOf(']');
        if (offRightB < offLeftB && offLeftB > 0) {
            throw new ParserException(functionId + "Invalid brackets for Variable: " + name);
        }
        if (offTrait > 0 && offLeftB > 0) {
            int nameEnd = (offTrait < offLeftB) ? offTrait : offLeftB;
            name = field.substring(0, nameEnd);
        } else if (offTrait > 0) {
            name = field.substring(0, offTrait);
        } else if (offLeftB > 0) {
            name = field.substring(0, offLeftB);
        }

        if (offLeftB > 0) {
            if (! bRightSide) {
                throw new ParserException(functionId + "Brackets only allowed for Right-side Variable usage: " + field + evaluation);
            }
            // we have an index associated with the param
            type = PreCompile.variables.getVariableTypeFromName (name);
            leftover = field.substring(offLeftB + 1, offRightB);

            // now see if we have a single entry or a range
            int offset = leftover.indexOf('-');
            if (offset > 0) {
                index    = packIndexValue (leftover.substring(0, offset));
                indexmax = packIndexValue (leftover.substring(offset+1));
                String ixStart = index.getValue() != null ? "" + index.getValue() : index.getVariable();
                String ixEnd = indexmax.getValue() != null ? "" + indexmax.getValue() : indexmax.getVariable();
                GUILogPanel.outputInfoMsg(MsgType.VARS, "Variable index range found: " + name + "[" + ixStart + "-" + ixEnd + "]");
            } else {
                index = packIndexValue(leftover);
                indexmax = null;
                String ixStr = index.getValue() != null ? "" + index.getValue() : index.getVariable();
                GUILogPanel.outputInfoMsg(MsgType.VARS, "Variable index entry found: " + name + "[" + ixStr + "]");
            }
        } else if (offTrait > 0) {
            if (! bRightSide) {
                throw new ParserException(functionId + "Traits only allowed for Right-side Variable usage: " + field + evaluation);
            }
            // we have a trait combined with the param name
            name = field.substring(0, offTrait);
            type = PreCompile.variables.getVariableTypeFromName (name);
            if (field.length() > offTrait + 1) {
                leftover = field.substring(offTrait + 1);
            }
            trait = TraitInfo.getTrait (leftover, name, type);
        } else {
            // no additional entries, the param name must be by itself
            name = field;
            type = PreCompile.variables.getVariableTypeFromName (name);
        }
        
        // verify Variable name is valid
        try {
            PreCompile.variables.checkValidVariable(Variables.VarCheck.REFERENCE, name);
        } catch (ParserException exMsg) {
            Utils.throwAddendum (exMsg.getMessage(), functionId);
        }
    }

    private BracketIx packIndexValue (String entry) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        BracketIx bIndex = new BracketIx();
        if (entry.charAt(0) == '$') {
            // must be a parameter, check if it exists
            // if it does, we don't care what type it is -that's a run-time thing
            entry = entry.substring(1);
            Variables.VarClass cls = PreCompile.variables.getVariableClass(entry);
            if (cls != Variables.VarClass.UNKNOWN) {
                bIndex.setVariable(entry);
            } else {
                throw new ParserException(functionId + "Index variable not found: " + entry);
            }
        } else {
            bIndex.setValue(Utils.getIntValue(entry).intValue());
        }
        return bIndex;
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

    /**
     * returns the current numeric value of the selected index for a Variable.
     * It is called at execution time, so this is where we can find the current
     *  numeric value for a variable that is used as an index. Only Unsigned
     *  numeric values are allowed here, so a negative value will cause an
     *  exception.
     * 
     * @return the current numeric value for the index entry
     * 
     * @throws ParserException 
     */
    public BracketIx getIndex () throws ParserException {
        return index;
    }
    
    /**
     * returns the current numeric value of the selected ending index for a Variable.
     * It is called at execution time, so this is where we can find the current
     *  numeric value for a variable that is used as an index. Only Unsigned
     *  numeric values are allowed here, so a negative value will cause an
     *  exception.
     * 
     * @return the current numeric value for the index entry
     * 
     * @throws ParserException 
     */
    public BracketIx getIndexEnd () throws ParserException {
        return indexmax;
    }
    
    public TraitInfo.Trait getTrait () {
        return trait;
    }
    
    public String getEvaluation () {
        return evaluation;
    }
}
    
