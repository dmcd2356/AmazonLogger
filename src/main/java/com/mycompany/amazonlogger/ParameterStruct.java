package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PARSER;

/**
 *
 * @author dan
 */
public class ParameterStruct {

    private static final String CLASS_NAME = "CommandParser";
    
    private String   strParam;
    private Integer  intParam;
    private Boolean  boolParam;
    private char     paramType;
        
    /**
     * Creates a parameter having the specified characteristics
     * 
     * @param objParam - the parameter value to use
     * @param dataType - the parameter type desired
     * 
     * @throws ParserException
     */
    public ParameterStruct (Object objParam, char dataType) throws ParserException {
        switch (objParam.getClass().toString()) {
            case "class java.lang.Integer":
                setIntegerValue ((Integer) objParam, dataType);
                break;
            case "class java.lang.Boolean":
                setBooleanValue ((Boolean) objParam, dataType);
                break;
            case "class java.lang.String":
            default:
                setStringValue ((String) objParam, dataType);
                break;
        }
    }
        
    public char getParamType () {
        return paramType;
    }
       
    public String getStringValue () {
        return strParam;
    }
        
    public Integer getIntegerValue () {
        return intParam;
    }
        
    public Boolean getBooleanValue () {
        return boolParam;
    }
        
    public void setStringValue (String strVal, char dataType) throws ParserException {
        String functionId = CLASS_NAME + ".setStringValue: ";
        
        intParam = null;
        boolParam = null;
        strParam = strVal;

        // if it is a parameter, save the param name as a string and allow it
        if (strVal.startsWith("$")) {
            frame.outputInfoMsg(STATUS_PARSER, "     'S' param: " + strParam);
            paramType = 'S';
            return;
        }
        try {
            switch (dataType) {
                case 'I':
                    intParam = Utils.getIntValue(strVal);
                    break;
                case 'U':
                    intParam = Utils.getHexValue (strVal);
                    if (intParam == null) {
                        intParam = Utils.getIntValue(strVal);
                        if (intParam < 0) {
                            throw new NumberFormatException("");
                        }
                    }
                    break;
                case 'B':
                    boolParam = Utils.getBooleanValue(strVal);
                    break;
                default:
                    // all string types
                    break;
            }
        } catch (NumberFormatException ex) {
            throw new ParserException(functionId + "Invalid param data for dataType " + dataType + ": param: " + strVal);
        }
            
        frame.outputInfoMsg(STATUS_PARSER, "     '" + dataType + "' param: " + strParam);
        paramType = dataType;
    }
        
    public void setIntegerValue (Integer intVal, char dataType) throws ParserException {
        String functionId = CLASS_NAME + ".setIntegerValue: ";
        
        strParam = null;
        boolParam = null;
        intParam = intVal;

        switch (dataType) {
            case 'I':
                break;
            case 'U':
                if (intParam < 0) {
                    throw new ParserException(functionId + "Invalid param data for dataType " + dataType + ": param: " + intVal);
                }
                break;
            case 'B':
                boolParam = intParam != 0;
                break;
            default:
                // all string types
                strParam = intParam.toString();
                break;
        }
            
        paramType = dataType;
        frame.outputInfoMsg(STATUS_PARSER, "     '" + paramType + "' param: " + intParam);
    }
        
    public void setBooleanValue (Boolean boolVal, char dataType) {
        
        intParam = null;
        strParam = null;
        boolParam = boolVal;

        switch (dataType) {
            case 'I':
            case 'U':
                intParam = boolVal ? 1 : 0;
                break;
            case 'B':
                break;
            default:
                // all string types
                strParam = boolParam.toString();
                break;
        }
            
        paramType = dataType;
        frame.outputInfoMsg(STATUS_PARSER, "     '" + paramType + "' param: " + boolParam);
    }
        
    public String showParam () {
        String strCommand = " [" + paramType + "]";
        switch (paramType) {
            case 'I':
            case 'U':
                if (intParam == null)
                    strCommand += " (null)";
                else
                    strCommand += " " + intParam.toString();
                break;
            case 'B':
                if (boolParam == null)
                    strCommand += " (null)";
                else
                    strCommand += " " + boolParam.toString();
                break;
            default:
                if (strParam == null)
                    strCommand += " (null)";
                else
                    strCommand += " '" + strParam + "'";
                break;
        }
            
        return strCommand;
    }
    
}
