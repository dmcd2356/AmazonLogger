/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import com.dmcd.amazonlogger.GUILogPanel.MsgType;
import java.util.ArrayList;

/**
 *
 * @author dan
 */
public class VarAccess {

    private static final String CLASS_NAME = VarAccess.class.getSimpleName();
    private static final String INDENT = "     ";
    
    private boolean     bUpdate;            // true when a value has been written to
    private String      varName;            // name of variable
    private ParameterStruct.ParamType varType; // the variable data type
    private Variables.AccessType access;    // type of access permitted to variable
    private String      owner;              // owner (name of function that allocated)
    private Integer     writer;             // script line number value of last writer to variable
    private String      writeTime;          // timestamp of last write
    private String      strValue;           // String   value
    private Long        intValue;           // Integer  value (or Unsigned)
    private Boolean     boolValue;          // Boolean  value
    private ArrayList<String> strArray;     // StrArray value
    private ArrayList<Long>   intArray;     // IntArray value
        
    // this is called during allocation to define the variable type and access info
    VarAccess (String owner, String varName, ParameterStruct.ParamType varType, Variables.AccessType access) {
        this.varName   = varName;
        this.varType   = varType;
        this.access    = access;
        this.owner     = owner;
        this.writer    = null;
        this.writeTime = "";
        this.bUpdate   = false;

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
        GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "LOCAL " + varType + " Variable " + varName + " allocated for subroutine: " + this.owner);
    }

    // clears the data value and writer info from the variable
    public void reset() {
        this.writer    = null;
        this.writeTime = "";

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
        GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "LOCAL " + varType + " Variable " + varName + " reset for subroutine: " + this.owner);
    }
    
    private void checkType (ParameterStruct.ParamType callType) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        if (varType != callType) {
            throw new ParserException(functionId + "Invalid data type " + callType + " instead of " + varType);
        }
    }

    public void resetUpdate() {
        this.bUpdate = false;
    }
    
    // indicates if the variable has been written to since it was allocated
    public boolean isVarChanged () {
        return this.bUpdate;
    }

    // these functions get the access info of the variable
    public ParameterStruct.ParamType getType () {
        return this.varType;
    }

    // indicates if the variable has been written to since it was allocated
    public boolean isVarInit () {
        return this.writer != null;
    }

    // returns the line number of the script that was the last writer to the variable
    public Integer getWriterIndex () {
        if (this.writer == null) {
            return 0;
        }
        return ScriptCompile.getLineNumber(this.writer);
    }

    // returns the timestamp when the last writer wrote to the variable
    public String getWriterTime () {
        return this.writeTime;
    }
        
    public String getOwner () {
        return this.owner;
    }
        
    public boolean isGlobal () {
        return this.access == Variables.AccessType.GLOBAL;
    }
        
    public void setWriteInfo () throws ParserException {
        this.bUpdate = true;
        this.writer = Subroutine.getCurrentIndex();
        this.writeTime = GUIMain.elapsedTimerGet();
    }

    // these are the functions to set the value of the variable
    public void setValueString (String value) throws ParserException {
        checkType (ParameterStruct.ParamType.String);
        this.strValue = value;
        setWriteInfo();
    }
        
    public void setValueInteger (Long value) throws ParserException {
        checkType (ParameterStruct.ParamType.Integer);
        this.intValue = value;
        setWriteInfo();
    }
        
    public void setValueUnsigned (Long value) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        checkType (ParameterStruct.ParamType.Unsigned);
        if (! ParameterStruct.isUnsignedInt(value)) {
            throw new ParserException(functionId + "Invalid value for type Unsigned: " + value);
        }
        this.intValue = value;
        setWriteInfo();
    }
        
    public void setValueBoolean (Boolean value) throws ParserException {
        checkType (ParameterStruct.ParamType.Boolean);
        this.boolValue = value;
        setWriteInfo();
    }
        
    public void setValueStrArray (ArrayList<String> value) throws ParserException {
        checkType (ParameterStruct.ParamType.StrArray);
        this.strArray = value;
        setWriteInfo();
    }
        
    public void setValueIntArray (ArrayList<Long> value) throws ParserException {
        checkType (ParameterStruct.ParamType.IntArray);
        this.intArray = value;
        setWriteInfo();
    }
        
    // these are the function to get the variable values
    public String getValueString () throws ParserException {
        checkType (ParameterStruct.ParamType.String);
        return this.strValue;
    }
        
    public Long getValueInteger () throws ParserException {
        checkType (ParameterStruct.ParamType.Integer);
        if (! isVarInit()) {
            String subName = Subroutine.getSubName();
            GUILogPanel.outputInfoMsg(MsgType.WARN, " - variable: " + varName + " in " + subName + " was not init prior to use");
        }
        return this.intValue;
    }
        
    public Long getValueUnsigned () throws ParserException {
        checkType (ParameterStruct.ParamType.Unsigned);
        if (! isVarInit()) {
            String subName = Subroutine.getSubName();
            GUILogPanel.outputInfoMsg(MsgType.WARN, " - variable: " + varName + " in " + subName + " was not init prior to use");
        }
        return this.intValue & 0xFFFFFFFFL;
    }
        
    public Boolean getValueBoolean () throws ParserException {
        checkType (ParameterStruct.ParamType.Boolean);
        if (! isVarInit()) {
            String subName = Subroutine.getSubName();
            GUILogPanel.outputInfoMsg(MsgType.WARN, " - variable: " + varName + " in " + subName + " was not init prior to use");
        }
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

