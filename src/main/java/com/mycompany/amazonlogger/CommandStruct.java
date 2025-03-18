/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import java.util.ArrayList;

/**
 *
 * @author dan
 */
public class CommandStruct {
    
    // defines the structure for file commands
    public int     line;        // source code line number for the command
    public String  command;     // the command to execute
    public ArrayList<ParameterStruct> params;   // the arguments associated with the command
        
    CommandStruct(String cmd, int linenum) {
        line    = linenum;
        command = cmd;
        params  = new ArrayList<>();
    }
        
    String showCommand () {
        String strCommand = command;
        for (int ix = 0; ix < params.size(); ix++) {
            ParameterStruct parStc = params.get(ix);
            strCommand += parStc.showParam();
        }
            
        return strCommand;
    }

}
