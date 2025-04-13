/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.ParameterStruct.getParamTypeFromName;

/**
 *
 * @author dan
 */
public class ParamContents {
    
    // this defines characteristics for a referenced parameter
    private String      name;           // name of parameter
    private ParameterStruct.ParamType   type;           // parameter classification
    private Integer     index;          // associated index [x] for String, StrArray, IntArray params
    private Integer     indexmax;       // associated ending index for String, StrArray, IntArray params
    private ParamExtract.Trait trait;   // object after '.' demarcation
        
    ParamContents () {
        this.name = null;
        this.type = null;
        this.index = null;
        this.indexmax = null;
        this.trait = null;
    }
        
    ParamContents (String name) {
        this.name = name;
        this.type = getParamTypeFromName (name);
        this.index = null;
        this.indexmax = null;
        this.trait = null;
    }

    ParamContents (ParamExtract paramInfo) {
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
    
    public void setParamTraits (ParamExtract data) {
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
    
    public ParamExtract.Trait getTrait() {
        return this.trait;
    }

}
