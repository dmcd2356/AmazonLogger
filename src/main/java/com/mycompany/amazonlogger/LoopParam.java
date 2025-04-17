/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

/**
 *
 * @author dan
 */
public class LoopParam {
    
    private static final String CLASS_NAME = "LoopParam";
    
    Integer value;          // the current value of the loop parameter
    String  paramName;      // the name of the reference Variable (null if no ref param)
        
    public LoopParam (Integer value) {
        this.value = value;
        this.paramName = null;
    }
        
    public LoopParam (String name) throws ParserException {
        String functionId = CLASS_NAME + " (new): ";
       
        try {
            Integer iVal = Utils.getHexValue(name);
            if (iVal == null) {
                iVal = Utils.getIntValue(name).intValue();
            }
            this.value = iVal;
            this.paramName = null;
        } catch (ParserException ex) {
            boolean bValid;
            try {
                bValid = ParameterStruct.isValidVariableName(name);
            } catch (ParserException exMsg) {
                throw new ParserException(exMsg + "\n  -> " + functionId);
            }
            if (!name.startsWith("$") || ! bValid) {
                throw new ParserException(functionId + "reference Variable " + this.paramName + " is not valid Variable name");
            }
            this.value = null;
            this.paramName = name;
        }
    }
        
    public void update (Integer value) {
        this.value = value;
    }
        
    public Integer getIntValue () throws ParserException {
        String functionId = CLASS_NAME + ".getIntValue: ";
       
//        if (this.paramName != null) {
//            // it is a Variable, get the current value
//            ParameterStruct parStc = new ParameterStruct();
//            parStc = parStc.getParameterEntry (this.paramName);
//            this.value = parStc.getIntegerValue();
//            if (this.value == null) {
//                throw new ParserException(functionId + "reference Variable " + this.paramName + " is not an Integer: " + parStc.getStringValue());
//            }
//        }

        return this.value;
    }
    
}
