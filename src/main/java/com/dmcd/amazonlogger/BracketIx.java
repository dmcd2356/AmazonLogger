/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

/**
 *
 * @author dan
 */
public class BracketIx {
    
    private static final String CLASS_NAME = BracketIx.class.getSimpleName();

    private Integer value;              // if index was a discreet value
    private String  var;                // if index was a variable
        
    BracketIx () {
        this.value = null;
        this.var = null;
    }

    public void setValue (Integer value) {
        this.value = value;
    }
    
    public void setVariable (String value) {
        this.var = value;
    }

    public Integer getValue () {
        return this.value;
    }
    
    public String getVariable () {
        return this.var;
    }
    
}
