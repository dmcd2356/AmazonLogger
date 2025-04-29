/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

/**
 *
 * @author dan
 */
public class VariableInfo {
    
    private static final String CLASS_NAME = "VariableInfo";
    
    // this defines characteristics for a referenced Variable
    private String      name;               // name of Variable
    private ParameterStruct.ParamType type; // data type classification
    private BracketIx   index;              // associated index [x] for String, StrArray, IntArray params
    private BracketIx   indexmax;           // associated ending index for String, StrArray, IntArray params
    private VariableExtract.Trait trait;    // object after '.' demarcation
        
    VariableInfo () {
        this.name     = null;
        this.type     = null;
        this.index    = null;
        this.indexmax = null;
        this.trait    = null;
    }
        
    VariableInfo (VariableInfo info) {
        this.name     = info.name;
        this.type     = info.type;
        this.index    = info.index;
        this.indexmax = info.indexmax;
        this.trait    = info.trait;
    }

    VariableInfo (VariableExtract paramInfo) throws ParserException {
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
    
    public void setParamTraits (VariableExtract data) throws ParserException {
        this.index    = data.getIndex();
        this.indexmax = data.getIndexEnd();
        this.trait    = data.getTrait();
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
    
    public VariableExtract.Trait getTrait() {
        return this.trait;
    }

    private Integer getIxValue (BracketIx entry) throws ParserException {
        String functionId = CLASS_NAME + ".getIxValue: ";
       
        if (entry != null) {
            if (entry.getValue() != null)
                return entry.getValue();
            if (entry.getVariable() != null) {
                Long numValue = Variables.getNumericValue(entry.getVariable());
                if (numValue == null) {
                    throw new ParserException(functionId + "reference Variable " + entry.getVariable() + " not found");
                }
                return numValue.intValue();
            }
        }
        return null;
    }
    
}
