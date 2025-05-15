/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_VARS;
import java.util.ArrayList;

/**
 *
 * @author dan
 */
public class LocalInfo {

    private static final String CLASS_NAME = LocalInfo.class.getSimpleName();
    private static final String indent = "     ";
    
    ParameterStruct.ParamType varType; // the variable data type
    Variables.AccessType access;    // type of access permitted to variable
    String      owner;              // owner (name of function that allocated)
    String      strValue;           // String   value
    Long        intValue;           // Integer  value (or Unsigned)
    Boolean     boolValue;          // Boolean  value
    ArrayList<String> strArray;     // StrArray value
    ArrayList<Long>   intArray;     // IntArray value
        
    // this is called during allocation to define the variable type and access info
    LocalInfo (String owner, String varName, ParameterStruct.ParamType varType, Variables.AccessType access) {
        this.varType   = varType;
        this.access    = access;
        this.owner     = owner;
        this.strValue  = null;
        this.intValue  = null;
        this.boolValue = null;
        this.strArray  = null;
        this.intArray  = null;

        // init the value of the chosen type
        switch (varType) {
            case Integer:
            case Unsigned:
                this.intValue = 0L;
                break;
            case Boolean:
                this.boolValue = false;
                break;
            case String:
                this.strValue = "";
                break;
            case StrArray:
                this.strArray = new ArrayList<>();
                break;
            case IntArray:
                this.intArray = new ArrayList<>();
                break;
        }
        frame.outputInfoMsg(STATUS_VARS, indent + "LOCAL " + varType + " Variable " + varName + " allocated for subroutine: " + this.owner);
    }

    private void checkType (ParameterStruct.ParamType callType) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        if (varType != callType) {
            throw new ParserException(functionId + "Invalid data type " + callType + " instead of " + varType);
        }
    }
        
    // these functions get the access info of the variable
    public ParameterStruct.ParamType getType () {
        return this.varType;
    }
        
    public String getOwner () {
        return this.owner;
    }
        
    public boolean isGlobal () {
        return this.access == Variables.AccessType.GLOBAL;
    }

    // these are the functions to set the value of the variable
    public void setValueString (String value) throws ParserException {
        checkType (ParameterStruct.ParamType.String);
        this.strValue = value;
    }
        
    public void setValueInteger (Long value) throws ParserException {
        checkType (ParameterStruct.ParamType.Integer);
        this.intValue = value;
    }
        
    public void setValueBoolean (Boolean value) throws ParserException {
        checkType (ParameterStruct.ParamType.Boolean);
        this.boolValue = value;
    }
        
    public void setValueStrArray (ArrayList<String> value) throws ParserException {
        checkType (ParameterStruct.ParamType.StrArray);
        this.strArray = value;
    }
        
    public void setValueIntArray (ArrayList<Long> value) throws ParserException {
        checkType (ParameterStruct.ParamType.IntArray);
        this.intArray = value;
    }
        
    // these are the function to get the variable values
    public String getValueString () throws ParserException {
        checkType (ParameterStruct.ParamType.String);
        return this.strValue;
    }
        
    public Long getValueInteger () throws ParserException {
        checkType (ParameterStruct.ParamType.Integer);
        return this.intValue;
    }
        
    public Boolean getValueBoolean () throws ParserException {
        checkType (ParameterStruct.ParamType.Boolean);
        return this.boolValue;
    }
        
    public ArrayList<String> getValueStrArray () throws ParserException {
        checkType (ParameterStruct.ParamType.StrArray);
        return this.strArray;
    }
        
    public ArrayList<Long> getValueIntArray () throws ParserException {
        checkType (ParameterStruct.ParamType.IntArray);
        return this.intArray;
    }
        
}

