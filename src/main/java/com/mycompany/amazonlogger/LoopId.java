/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

/**
 *
 * @author dan
 */
public class LoopId {
    
    private static final String CLASS_NAME = LoopId.class.getSimpleName();

    // the key for loops uses both the name and the command index of the FOR statement.
    //  this way, loop names can be reused as long as they aren't nested within each other.
    private final String  name;       // name of the loop
    private final int     index;      // command index of the start of the loop
        
    LoopId (String name, int index) {
        this.name  = name;
        this.index = index;
    }

    public String getName() {
        return name;
    }
    
    public int getIndex() {
        return index;
    }
    
    public String printLoopId () {
        return "Loop " + name + " index @ " + index;
    }
    
}
