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
    
    // this defines characteristics for a referenced Variable
    private String      name;           // name of Variable
    private ParameterStruct.ParamType type; // data type classification
    private Integer     index;          // associated index [x] for String, StrArray, IntArray params
    private Integer     indexmax;       // associated ending index for String, StrArray, IntArray params
    private VariableExtract.Trait trait;   // object after '.' demarcation
        
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

    VariableInfo (VariableExtract paramInfo) {
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
    
    public void setParamTraits (VariableExtract data) {
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
    
    public Integer getIndexStart() {
        return this.index;
    }
    
    public Integer getIndexEnd() {
        return this.indexmax;
    }
    
    public VariableExtract.Trait getTrait() {
        return this.trait;
    }

}
