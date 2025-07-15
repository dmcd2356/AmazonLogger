/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

/**
 *
 * @author dan
 */
public class VarExtensions {
    
    private static final String CLASS_NAME = VarExtensions.class.getSimpleName();
    
    // this defines characteristics for a referenced Variable
    private String                  name;       // name of Variable
    private ParameterStruct.ParamType type;     // data type classification
    private final BracketIx         index;      // associated index [x] for String, StrArray, IntArray params
    private final BracketIx         indexmax;   // associated ending index for String, StrArray, IntArray params
    private final TraitInfo.Trait   trait;      // object after '.' demarcation
    
    /**
     * this is only called by ParameterStruct() to init the variableRef entry.
     */
    VarExtensions () {
        this.name     = null;
        this.type     = null;
        this.index    = null;
        this.indexmax = null;
        this.trait    = null;
    }
        
    /**
     * This is called by Variables.getVariableInfo() during the execution stage.
     * 
     * @param info 
     */
    VarExtensions (VarExtensions info) {
        this.name     = info.name;
        this.type     = info.type;
        this.index    = info.index;
        this.indexmax = info.indexmax;
        this.trait    = info.trait;
    }

    /**
     * this is called by ParameterStruct() during Compile stage only.
     * 
     * @param paramInfo - the info extracted from the parameter name
     * 
     * @throws ParserException 
     */
    VarExtensions (VarExtract paramInfo) throws ParserException {
        this.name     = paramInfo.getName();
        this.type     = paramInfo.getType();
        this.index    = paramInfo.getIndex();
        this.indexmax = paramInfo.getIndexEnd();
        this.trait    = paramInfo.getTrait();
    }
    
    public void setParamName (String name, ParameterStruct.ParamType type) {
        this.name = name;
        this.type = type;
    }
    
    public String getName() {
        return this.name;
    }
    
    public ParameterStruct.ParamType getType() {
        return this.type;
    }
    
    public Integer getIndexStart() throws ParserException {
        return getIxValue (index);
   }
    
    public Integer getIndexEnd() throws ParserException {
        return getIxValue (indexmax);
    }
    
    public TraitInfo.Trait getTrait() {
        return this.trait;
    }

    private Integer getIxValue (BracketIx entry) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
       
        if (entry != null) {
            if (entry.getValue() != null)
                return entry.getValue();
            // no numeric value present, check if it is a variable
            if (entry.getVariable() != null) {
                // yes - we don't allow Brackets for indexes (that would be Brackets inside Brackets - too messy looking!)
                // but we do allow Traits to be specified, as long as they return integer values
                // The variable value in this case will have the Trait following a '.' char after the Variable name.
                // Also, the 'false' flag in getNumericValue() indicates we do allow loop variables to be used here as well.
                String varName = entry.getVariable();
                int offset = varName.indexOf('.');
                TraitInfo.Trait traitVal = null;
                if (offset > 0 && varName.length() > offset + 1) {
                    // Trait included: split name into var name and trait name
                    String traitName = varName.substring(offset + 1);
                    varName = varName.substring(0, offset);
                    // now get corresponding trait type & validate that it is a numeric
                    ParameterStruct.ParamType ptype = Variables.getVariableTypeFromName (varName);
                    traitVal = TraitInfo.getTrait (traitName, varName, ptype);
                    ParameterStruct.ParamType traitTyp = TraitInfo.getTraitDataType (traitVal, varName, ptype);
                    switch (traitTyp) {
                        case Integer, Unsigned -> {  }
                        default -> throw new ParserException(functionId + "Specified Variable trait is not a numeric: " + traitName);
                    }
                }
                Long numValue = Variables.getNumericValue(varName, traitVal, false);
                if (numValue == null) {
                    throw new ParserException(functionId + "reference Variable " + entry.getVariable() + " not found");
                }
                return numValue.intValue();
            }
        }
        return null;
    }
    
}
