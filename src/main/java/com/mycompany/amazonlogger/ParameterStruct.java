/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

/**
 * This class defines the structure of the parameters stored for the commands.
 * 
 * It allows Integer, Boolean and String data types and has a paramType that
 * corresponds to the data type.
 * 
 * @author dan
 */
public final class ParameterStruct {

    private static final String CLASS_NAME = "ParameterStruct";
    
    private String   strParam;      // value for the String  param type
    private Integer  intParam;      // value for the Integer param type
    private Boolean  boolParam;     // value for the Boolean param type
    private ArrayList<Integer> arrayParam;  // value for Integer Array param type
    private ArrayList<String>  listParam;   // value for String  List  param type
    private String   paramName;     // name of parameter if references a saved parameter
    private char     paramType;     // the corresponding data type stored
    
    // saved static parameters
    private static String  strResponse = "";    // response from last RUN command
    private static Integer intResult = 0;       // result of last CALC command
    private static final HashMap<String, String>  strParams  = new HashMap<>();
    private static final HashMap<String, Integer> intParams  = new HashMap<>();
    private static final HashMap<String, Boolean> boolParams = new HashMap<>();
    private static final HashMap<String, ArrayList<Integer>> arrayParams = new HashMap<>();
    private static final HashMap<String, ArrayList<String>>  listParams  = new HashMap<>();
    
    // for loops, the loopParams will find the loop parameter for the loop at the
    // specified command index. In order to determine if we have a nested loop
    // using the same param name, we use loopNames that contains an array of
    // all the loop params having the same name. When a name is being reused,
    // we must verify that all the occurrances of FOR loops using that name
    // are all completely defined, meaning that the ENDFOR has already been found
    // for each one. When compiling, we simply proceed through the instructions
    // sequentially, so if all current uses of the FOR parameter indicate they
    // are complete (i.e. ENDFOR has been found), we are safe to reuse the loop name.
    private static final HashMap<LoopId, LoopStruct> loopParams = new HashMap<>();
    private static final HashMap<String, ArrayList<LoopId>> loopNames = new HashMap<>();
    
    public ParameterStruct() {
        strParam = null;
        intParam = null;
        boolParam = null;
        arrayParam = null;
        listParam = null;
        paramName = "";
        paramType = '?';
    }
    
    /**
     * Creates a parameter and determines the data type.
     * This is only used in the execution phase, so if a parameter value is found,
     *  do a replacement of it.
     * Note that the data is always stored in String format.
     * 
     * I = signed integer   ( LT 0 )
     * U = unsigned integer ( GE 0 )
     * B = boolean
     * S = string (single word)
     * L = list (multi-word string)
     * 
     * @param objValue - the parameter value to use
     * 
     * @throws ParserException
     */
    public ParameterStruct (Object objValue) throws ParserException {
         String functionId = CLASS_NAME + " (new Object): ";
        
        String classType = objValue.getClass().toString();
        paramName = null;
        switch (classType) {
            case "class java.lang.Integer":
                Integer iVal = (Integer) objValue;
                strParam = iVal.toString();
                if (iVal >= 0)
                    paramType = 'U';
                else
                    paramType = 'I';
                break;
            case "class java.lang.Boolean":
                Boolean bValue = (Boolean) objValue;
                strParam = bValue.toString();
                paramType = 'B';
                break;
            case "class java.lang.String":
                strParam = (String) objValue;
                if (strParam.equalsIgnoreCase("TRUE") || strParam.equalsIgnoreCase("FALSE")) {
                    paramType = 'B';
                }
                try {
                    iVal = Utils.getIntValue (strParam);
                    if (iVal >= 0)
                        paramType = 'U';
                    else
                        paramType = 'I';
                } catch (ParserException ex) {
                    if (strParam.startsWith("$")) {
                        paramType = 'R';
                        paramName = strParam.substring(1);
                        ParameterStruct refParam = getParameterEntry (paramName);
                        switch (refParam.paramType) {
                            case 'I':
                                strParam = refParam.intParam.toString();
                                paramType = refParam.paramType;
                                break;
                            case 'B':
                                strParam = refParam.boolParam.toString();
                                paramType = refParam.paramType;
                                break;
                            case 'S':
                                strParam = refParam.strParam;
                                paramType = refParam.paramType;
                                break;
                            default:
                            case 'A':
                            case 'L':
                                throw new ParserException(functionId + "Invalid Parameter type '" + refParam.paramType + "' : " + paramName);
                        }
                    }
                    else if (strParam.indexOf(' ') > 0)
                        paramType = 'L';
                    else
                        paramType = 'S';
                }
                break;
                
            // TODO: handle ArrayList<Integer> and ArrayList<String> values
                
            default:
                throw new ParserException(functionId + "Invalid class type for param: " + classType);
        }
        
        frame.outputInfoMsg(STATUS_DEBUG, functionId + "type " + paramType + ", " + classType);
    }
        
    /**
     * Creates a parameter having the specified characteristics.
     * This is only used in the Compilation phase, so we are creating the parameter
     *   entry and verifying the type is valid, but if it is a reference parameter,
     *   don't replace the parameter with its value.
     *   That can only be done during execution phase.
     * 
     * @param strValue - the parameter value to use (can be a parameter reference)
     * @param dataType - the parameter type desired
     * 
     * @throws ParserException
     */
    public ParameterStruct (String strValue, char dataType) throws ParserException {
        String functionId = CLASS_NAME + " (new): ";
        
        paramName = null;
        strParam = strValue;
        
        if (strParam.startsWith("$")) {
            paramName = strValue.substring(1);
            paramType = 'S';
            strParam = paramName;
            if (paramName.charAt(1) == '_') {
                switch (paramName.charAt(0)) {
                    case 'I':
                    case 'B':
                    case 'S':
                    case 'A':
                    case 'L':
                        paramType = paramName.charAt(0);
                        break;
                    default:
                        break;
                }
            }
            return;
        }
        
        switch (dataType) {
            case 'B':
                if (!strParam.equalsIgnoreCase("TRUE") &&
                    !strParam.equalsIgnoreCase("FALSE") &&
                    !strParam.contentEquals("0") &&
                    !strParam.contentEquals("1") ) {
                    throw new ParserException(functionId + "Invalid value for '" + dataType + "' type param: " + strValue);
                }
                paramType = 'B';
                boolParam = strParam.equalsIgnoreCase("TRUE") || strParam.contentEquals("1");
                break;

            case 'I':
                try {
                    intParam = Utils.getIntValue (strParam);
                    paramType = 'I';
                } catch (ParserException ex) {
                    throw new ParserException(functionId + "Invalid value for '" + dataType + "' type param: " + strValue);
                }
                arrayParam = new ArrayList<>();
                arrayParam.add(intParam);
                break;
            case 'U':
                try {
                    intParam = Utils.getHexValue (strParam);
                    if (intParam == null) {
                        intParam = Utils.getIntValue (strParam);
                    }
                    if (intParam >= 0)
                        paramType = 'U';
                } catch (ParserException ex) {
                    throw new ParserException(functionId + "Invalid value for '" + dataType + "' type param: " + strValue);
                }
                arrayParam = new ArrayList<>();
                arrayParam.add(intParam);
                break;
            case 'S':
                paramType = 'S';
                listParam = new ArrayList<>();
                listParam.add(strParam);
                break;
            case 'L':
                paramType = 'L';
                break;

            // TODO: handle ArrayList<Integer> and ArrayList<String> values
                
        }
    }

    /**
     * Creates a parameter having the specified characteristics.
     * This is only used in the Compilation phase, so we are creating the parameter
     *   entry and verifying the type is valid.
     * 
     * @param listValue - the list parameter value to use
     * @param dataType  - the parameter type desired
     * 
     * @throws ParserException
     */
    public ParameterStruct (ArrayList<String> listValue, char dataType) throws ParserException {
        String functionId = CLASS_NAME + " (new list): ";

        paramName = null;
        listParam = listValue;
        strParam = listValue.getFirst();
        paramType = 'L';
        
        // remove whitespace from each entry
        ListIterator<String> itr = listParam.listIterator();
        while (itr.hasNext()) {
            itr.set(itr.next().strip());
        }
        
        // now check if all the entries are numeric. If so, convert this to an Array type
        boolean bFail = false;
        arrayParam = new ArrayList<>();
        for (int ix = 0; ix < listValue.size(); ix++) {
            try {
                Integer iVal = Utils.getHexValue (listValue.get(ix));
                if (iVal == null) {
                    iVal = Utils.getIntValue (listValue.get(ix));
                }
                arrayParam.add(iVal);
            } catch (ParserException ex) {
                arrayParam = null;
                bFail = true;
                break;
            }
        }
        if (! bFail) {
            paramType = 'A';
        }

        frame.outputInfoMsg(STATUS_DEBUG, functionId + "type " + paramType + ", " + listParam.size() + " entries");
    }
    
    /**
     * returns the parameter type (I, U, B, S, L, R)
     * 
     * @return the parameter type
     */
    public char getParamType () {
        return paramType;
    }
       
    /**
     * returns the parameter name
     * 
     * @return the parameter name (null if parameter is not a reference to a saved parameter)
     */
    public String getParamName () {
        return paramName;
    }
       
    /**
     * returns the String data value
     * 
     * @return the String value
     */
    public String getStringValue () {
        return strParam;
    }
        
    /**
     * returns the Integer data value
     * 
     * @return the Integer value
     */
    public Integer getIntegerValue () {
        return intParam;
    }
        
    /**
     * returns the Boolean data value
     * 
     * @return the Boolean value
     */
    public Boolean getBooleanValue () {
        return boolParam;
    }

    /**
     * returns the Array data element value
     * 
     * @return the number of elements in the Array
     */
    public int getArraySize () {
        if (arrayParam == null)
            return 0;
        return arrayParam.size();
    }

    public void updateFromReference () throws ParserException {
        
        ParameterStruct value = getParameterEntry (paramName);
        if (value != null) {
            this.paramType  = value.paramType;
            this.intParam   = value.intParam;
            this.boolParam  = value.boolParam;
            this.strParam   = value.strParam;
            this.arrayParam = value.arrayParam;
            this.listParam  = value.listParam;
            
            frame.outputInfoMsg(STATUS_DEBUG, "    unpacked param " + paramName + " as type '" + paramType + "' value: " + boolParam);
        }
    }
    
    /**
     * returns the Array data element value
     * 
     * @param index - the entry from the array to get
     * 
     * @return the Array element value
     */
    public Integer getArrayElement (int index) {
        if (index >= arrayParam.size()) {
            return null;
        }
        return arrayParam.get(index);
    }

    /**
     * returns the List data element value
     * 
     * @return the number of elements in the List
     */
    public int getListSize () {
        if (listParam == null)
            return 0;
        return listParam.size();
    }

    /**
     * returns the List data element value
     * 
     * @param index - the entry from the list to get
     * 
     * @return the List element value
     */
    public String getListElement (int index) {
        if (index >= listParam.size()) {
            return null;
        }
        return listParam.get(index);
    }

    /**
     * converts the current parameter value to the specified type.
     * This will replace 'strParam' with the reference parameter value if the
     *  current value is a parameter reference, and will convert the 'strParam'
     *  into Integer or Boolean value and save to 'intParam' or 'boolParam'
     *  if the dataType param specifies it.
     * 
     * @param dataType - type of data to convert the current value to
     * 
     * @throws ParserException 
     */
//    public void convertType (char dataType) throws ParserException {
//        String functionId = CLASS_NAME + ".convertType: ";
//        
//        dataType = Character.toUpperCase(dataType);
//        
//        ParameterStruct value = getParameterEntry (paramName);
//        if (value != null) {
//            //this.paramName  = value.paramName;
//            this.paramType  = value.paramType;
//            this.intParam   = value.intParam;
//            this.boolParam  = value.boolParam;
//            this.strParam   = value.strParam;
//            this.arrayParam = value.arrayParam;
//            this.listParam  = value.listParam;
//            
//            if (dataType == 'B' && value.boolParam == null) {
//                throw new ParserException(functionId + "Invalid Parameter type '" + value.paramType + "' : " + paramName);
//            }
//            frame.outputInfoMsg(STATUS_DEBUG, "    unpacked '" + paramType + "' type " + paramName + " value: " + boolParam);
//        } else {
//            switch (dataType) {
//                case 'I' :
//                    if (intParam != null) return;
//                    // convert from String
//                    intParam = Utils.getIntValue(strParam);
//                    paramType = 'I';
//                    break;
//                case 'U':
//                    if (intParam != null) return;
//                    // convert from String
//                    intParam = Utils.getIntValue(strParam);
//                    if (intParam < 0) {
//                        throw new ParserException(functionId + "Unsigned can't have a negative value: " + intParam);
//                    }
//                    paramType = 'I';
//                    break;
//                case 'B':
//                    if (boolParam != null) return;
//                    // convert from String
//                    boolParam = Utils.getBooleanValue(strParam);
//                    paramType = 'B';
//                    break;
//                case 'A':
//                    if (arrayParam != null) return;
//                    // TODO:
//                    break;
//                case 'L':
//                    if (listParam != null) return;
//                    // TODO:
//                    break;
//                case 'S':
//                    // nothing to do
//                    break;
//                default:
//                    throw new ParserException(functionId + "Invalid Data type '" + dataType);
//            }
//            frame.outputInfoMsg(STATUS_DEBUG, "    unpacked '" + dataType + "' type from '" + paramType + "' value: " + boolParam);
//        }
//    }
    
    /**
     * gets the String value of the current parameter, which may be a saved parameter
     * 
     * @return the String value, which may be a parameter reference & may require type conversion
     * 
     * @throws ParserException 
     */
//    public String unpackStringValue () throws ParserException {
//        String functionId = CLASS_NAME + ".unpackStringValue: ";
//        
//        // check for entry in params first
//        ParameterStruct value = getParameterEntry (paramName);
//        if (value != null) {
//            switch (value.paramType) {
//                case 'I':
//                case 'U':
//                    strParam = value.intParam.toString();
//                    break;
//                case 'B':
//                    strParam = value.boolParam.toString();
//                    break;
//                case 'S':
//                    strParam = value.strParam;
//                    break;
//                default:
//                case 'A':
//                case 'L':
//                    throw new ParserException(functionId + "Invalid Parameter type '" + value.paramType + "' : " + paramName);
//            }
//            frame.outputInfoMsg(STATUS_DEBUG, "    unpacked String from '" + paramType + "' " + paramName + " value: " + strParam);
//        } else {
//            // strParam should have already been set by the new ParameterStruct() call
//            frame.outputInfoMsg(STATUS_DEBUG, "    unpacked String from '" + paramType + "' value: '" + strParam + "'");
//        }
//        return strParam;
//    }
        
    /**
     * gets the Integer value of the current parameter, which may be a saved parameter
     * 
     * @return the Integer value, which may be a parameter reference & may require type conversion
     * 
     * @throws ParserException 
     */
//    public Integer unpackIntegerValue () throws ParserException {
//        String functionId = CLASS_NAME + ".unpackIntegerValue: ";
//        
//        // check for entry in params first
//        if (paramName != null) {
//            // unpack from reference parameter value
//            ParameterStruct value = getParameterEntry (paramName);
//            switch (value.paramType) {
//                case 'I':
//                case 'U':
//                    intParam = value.intParam;
//                    break;
//                case 'B':
//                    intParam = value.boolParam ? 1 : 0;
//                    break;
//                case 'S':
//                    intParam = Utils.getHexValue(value.strParam);
//                    if (intParam == null) {
//                        intParam = Utils.getIntValue(value.strParam);
//                    }
//                    break;
//                default:
//                case 'A':
//                case 'L':
//                    throw new ParserException(functionId + "Invalid Parameter type '" + value.paramType + "' : " + paramName);
//            }
//            frame.outputInfoMsg(STATUS_DEBUG, "    unpacked Integer from '" + paramType + "' " + paramName + " value: " + intParam);
//        } else if (strParam != null) {
//            // unpack from String entry
//            intParam = Utils.getHexValue(strParam);
//            if (intParam == null) {
//                intParam = Utils.getIntValue(strParam);
//            }
//            frame.outputInfoMsg(STATUS_DEBUG, "    unpacked Integer from '" + paramType + "' value: " + intParam);
//        }
//        
//        paramType = 'I';
//        return intParam;
//    }
        
    /**
     * returns a String for displaying the current param data type and value.
     * 
     * @return a String indicating the parameter type and value
     */
    public String showParam () {
        String strCommand;
        switch (paramType) {
            case 'I', 'U' -> {
                strCommand = " [" + paramType + "] " + intParam + "";
            }
            case 'B' -> {
                strCommand = " [" + paramType + "] " + boolParam + "";
            }
//            case 'A' -> {
//                strCommand = " [" + paramType + "] " + arrayParam.toString() + "";
//            }
//            case 'L' -> {
//                strCommand = " [" + paramType + "] " + listParam.toString() + "";
//            }
            default -> {
                strCommand = " [" + paramType + "] '" + strParam + "'";
            }
        }
            
        return strCommand;
    }
    
    //========================================================================
    // THESE STATIC METHODS ARE USED FOR ACCESSING THE STATIC PARAMETER VALUES
    //========================================================================

    /**
     * initializes the saved parameters
     */
    public static void initParameters () {
        strParams.clear();
        intParams.clear();
        boolParams.clear();
        arrayParams.clear();
        listParams.clear();
        loopParams.clear();
        loopNames.clear();
        strResponse = "";
        intResult = 0;
    }

    /**
     * set the value of the $RESPONSE parameter
     * 
     * @param value - value to set the response param to
     */
    public static void putResponseValue (String value) {
        strResponse = value;
    }
    
    /**
     * set the value of the $RESULT parameter
     * 
     * @param value - value to set the result param to
     */
    public static void putResultValue (Integer value) {
        intResult = value;
    }

    /**
     * creates a new entry in the Integer params table and sets the initial value.
     * Indicates if the param was already defined.
     * 
     * @param name  - parameter name
     * 
     * @throws ParserException - if name already exists
     */
    public static void allocateParameter (String name) throws ParserException {
        String functionId = CLASS_NAME + ".allocateParameter: ";

        // first, verify parameter name to make sure it is valid format and
        //  not already used.
        boolean bIsDefined = isValidParamName(name);
        if (bIsDefined) {
            throw new ParserException(functionId + "Parameter " + name + " already defined");
        }
        
        String typeName = "String";
        char pType = 'S';
        if (name.charAt(1) == '_') {
            pType = name.charAt(0);
        }
        switch (pType) {
            case 'I':  // Integer (and Unsigned) type
                typeName = "Integer";
                intParams.put(name, 0);         // default value to 0
                break;
            case 'B':  // Boolean type
                typeName = "Boolean";
                boolParams.put(name, false);    // default value to false
                break;
            case 'A':  // Integer Array type
                typeName = "Array";
                arrayParams.put(name, new ArrayList<>());
                break;
            case 'L':  // String List type
                typeName = "List";
                listParams.put(name, new ArrayList<>());
                break;
            default:    // else String type
            case 'S':
                strParams.put(name, "");        // default value to empty String
                break;
        }
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Added " + typeName + " parameter: " + name);
    }

    /**
     * returns the value of a reference parameter along with its data type.
     * 
     * @param name  - parameter name
     * 
     * @return the parameter value
     * 
     * @throws ParserException - if parameter not found
     */
    public ParameterStruct getParameterEntry (String name) throws ParserException {
        String functionId = CLASS_NAME + ".getParameterEntry: ";

        if (name == null) {
            return null;
        }
        
        ParameterStruct paramValue = new ParameterStruct();

        // this allows us to take as a name input either the name itself or with the '$' preceeding it.
        if (name.charAt(0) == '$') {
            name = name.substring(1);
        }
        char pType = 'S';
        if (name.charAt(1) == '_') {
            pType = name.charAt(0);
        }
        
        // default the param values to null
        paramValue.arrayParam = null;
        paramValue.listParam = null;
        paramValue.intParam = null;
        paramValue.boolParam = null;
        paramValue.strParam = null;
        
        switch (pType) {
            case 'I':  // Integer (and Unsigned) type
                paramValue.intParam = intParams.get(name);
                if (paramValue.intParam == null) {
                    throw new ParserException(functionId + "Parameter " + name + " not found");
                }
                if (paramValue.intParam >= 0) {
                    pType = 'U';
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.intParam);

                // always update all other values as best we can
                paramValue.strParam = paramValue.intParam.toString();
                paramValue.boolParam = paramValue.intParam != 0;
                break;
            case 'B':  // Boolean type
                paramValue.boolParam = boolParams.get(name);
                if (paramValue.boolParam == null) {
                    throw new ParserException(functionId + "Parameter " + name + " not found");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.boolParam);

                // always update all other values as best we can
                paramValue.strParam = paramValue.boolParam.toString();
                paramValue.intParam = (paramValue.boolParam) ? 1 : 0;
                break;
            case 'A':  // Integer Array type
                paramValue.arrayParam = arrayParams.get(name);
                if (paramValue.arrayParam == null) {
                    throw new ParserException(functionId + "Parameter " + name + " not found");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.arrayParam.toString());

                // always update all other values as best we can
                paramValue.strParam = paramValue.arrayParam.getFirst().toString();
                paramValue.intParam = paramValue.arrayParam.getFirst();
                paramValue.boolParam = !paramValue.arrayParam.isEmpty(); // set to true if array has an entry
                break;
            case 'L':  // String List type
                paramValue.listParam = listParams.get(name);
                if (paramValue.listParam == null) {
                    throw new ParserException(functionId + "Parameter " + name + " not found");
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.listParam.toString());

                // always update all other values as best we can
                paramValue.strParam = paramValue.listParam.getFirst();
                break;
            default:
                if (name.contentEquals("RESULT")) {
                    paramValue.intParam = intResult;
                    pType = 'I';
                    if (paramValue.intParam >= 0) {
                        pType = 'U';
                    }
                    paramValue.strParam = paramValue.intParam.toString();
                } else if (name.contentEquals("RESPONSE")) {
                    paramValue.strParam = strResponse;
                    pType = 'S';
                } else {
                    paramValue.strParam = strParams.get(name);
                    if (paramValue.strParam != null) {
                        pType = 'S';
                    } else if (loopNames.containsKey(name)) {
                        LoopStruct loopInfo = new LoopStruct();
                        paramValue.intParam = loopInfo.getCurrentLoopValue(name);
                        pType = 'I';
                        if (paramValue.intParam >= 0) {
                            pType = 'U';
                        }
                        paramValue.strParam = paramValue.intParam.toString();
                    } else {
                        throw new ParserException(functionId + "Parameter " + name + " not found");
                    }
                }
                frame.outputInfoMsg(STATUS_PROGRAM, "    Lookup Ref '" + name + "' as type " + pType + ": " + paramValue.strParam);
                
                if (pType == 'S') {
                    if (paramValue.strParam.equalsIgnoreCase("TRUE")) {
                        paramValue.boolParam = true;
                        pType = 'B';
                    }
                    else if (paramValue.strParam.equalsIgnoreCase("FALSE")) {
                        paramValue.boolParam = false;
                        pType = 'B';
                    }
                    else {
                        paramValue.intParam = Utils.getHexValue(paramValue.strParam);
                        if (paramValue.intParam != null) {
                            pType = 'U';
                            paramValue.boolParam = paramValue.intParam != 0;
                        } else {
                            try {
                                paramValue.intParam = Utils.getIntValue(paramValue.strParam);
                                pType = 'I';
                                paramValue.boolParam = paramValue.intParam != 0;
                            } catch (ParserException ex) {
                                // keep param as String type and we can't do any conversions
                            }
                        }
                    }
                } else {
                    paramValue.strParam = paramValue.intParam.toString();
                    paramValue.boolParam = paramValue.intParam != 0;
                }
                break;
        }

        paramValue.paramType = pType;
        paramValue.paramName = name;
        return paramValue;
    }

    /**
     * modifies the value of an existing entry in the String params table.
     * Indicates if the param was not found (does NOT create a new entry).
     * 
     * @param name  - parameter name
     * @param value - parameter value
     * 
     * @return true if successful, false if the parameter was not found
     */
    public static boolean modifyStringParameter (String name, String value) {
        if (strParams.containsKey(name)) {
            strParams.replace(name, value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified String param: " + name + " = " + value);
            return true;
        }
        return false;
    }

    /**
     * modifies the value of an existing entry in the Integer params table.
     * Indicates if the param was not found (does NOT create a new entry).
     * 
     * @param name  - parameter name
     * @param value - parameter value
     * 
     * @return true if successful, false if the parameter was not found
     */
    public static boolean modifyIntegerParameter (String name, Integer value) {
        if (intParams.containsKey(name)) {
            intParams.replace(name, value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified Integer param: " + name + " = " + value);
            return true;
        }
        return false;
    }

    /**
     * modifies the value of an existing entry in the Boolean params table.
     * Indicates if the param was not found (does NOT create a new entry).
     * 
     * @param name  - parameter name
     * @param value - parameter value
     * 
     * @return true if successful, false if the parameter was not found
     */
    public static boolean modifyBooleanParameter (String name, Boolean value) {
        if (boolParams.containsKey(name)) {
            boolParams.replace(name, value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified Boolean param: " + name + " = " + value);
            return true;
        }
        return false;
    }

    
    // TODO: make a mofifyArrayParameter
    // TODO: make a modifyListParameter
    
    
    /**
     * gets the LoopStruct entry corresponding to the LoopId value.
     * 
     * @param loopId - the loop name-index combo that uniquely defines a LoopStruct entry
     * 
     * @return the corresponding LoopStruct value from loopParams table
     */
    private static LoopStruct getLoopStruct (LoopId loopId) {
        if (loopParams== null || loopParams.isEmpty()) {
            return null;
        }
        // search for a LoopId match
        Iterator it = loopParams.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            LoopId mapId = (LoopId) pair.getKey();
            LoopStruct mapInfo = (LoopStruct) pair.getValue();
            if (loopId.name.contentEquals(mapId.name) && loopId.index == mapId.index) {
                return mapInfo;
            }
        }
        return null;
    }
    
    /**
     * get the current loop value for the specified loop id
     * 
     * @param loopId - the name-index ID for the currently active loop
     * 
     * @return value of the loop parameter, null if param not found
     */    
    public static Integer getLoopValue (LoopId loopId) {
        LoopStruct loopInfo = getLoopStruct (loopId);
        if (loopInfo == null) {
            return null;
        }
        return loopInfo.getLoopValue();
    }
        
    /**
     * checks if the current Loop command is at the same IF level as its corresponding FOR statement.
     * 
     * @param command - the FOR command being run
     * @param level  - current IF nest level for current FOR command
     * @param loopId - the name-index ID for the current loop
     * 
     * @throws ParserException 
     */    
    public static void checkLoopIfLevel (String command, int level, LoopId loopId) throws ParserException {
        String functionId = CLASS_NAME + ".setLoopEnd: ";
        
        LoopStruct loopInfo = getLoopStruct (loopId);
        if (loopInfo == null) {
            throw new ParserException(functionId + "FOR Loop " + loopId.name + " @ " + loopId.index + " not found");
        }
        if (! loopInfo.isLoopIfLevelValid(level)) {
            throw new ParserException(functionId + command + "exceeded bounds of enclosing IF block: IF level = " + level);
        }
    }
        
    /**
     * sets the location of the end of the loop when the ENDLOOP command is parsed
     * 
     * @param index  - current command index for ENDLOOP command
     * @param loopId - the name-index ID for the current loop
     * 
     * @throws ParserException 
     */    
    public static void setLoopEndIndex (int index, LoopId loopId) throws ParserException {
        String functionId = CLASS_NAME + ".setLoopEnd: ";
        
        LoopStruct loopInfo = getLoopStruct (loopId);
        if (loopInfo == null) {
            throw new ParserException(functionId + "FOR Loop " + loopId.name + " @ " + loopId.index + " not found");
        }
        loopInfo.setLoopEnd(index);
    }
        
    /**
     * gets the next command index based on the loop command specified for current loop.
     * This should be called by 'CommandParser.executeProgramCommand' when a FOR
     * loop is in progress and one of the loop commands was found that may change
     * the next location to execute from.
     * 
     * @param command - the loop command to execute
     * @param index   - the current command index
     * @param loopId  - the loop parameter currently running
     * 
     * @return the next command index to run
     * 
     * @throws ParserException
     */
    public static int getLoopNextIndex (String command, int index, LoopId loopId) throws ParserException {
        String functionId = CLASS_NAME + ".getLoopNextIndex: ";
        
        int nextIndex = index;
        
        LoopStruct loopInfo = getLoopStruct (loopId);
        if (loopInfo == null) {
            throw new ParserException(functionId + "FOR Loop " + loopId.name + " @ " + loopId.index + " not found");
        }
        
        String action = "";
        switch (command) {
            case "FOR":
                nextIndex = loopInfo.startLoop(index);
                action = "starting";
                break;
            case "BREAK":
                nextIndex = loopInfo.loopBreak();
                action = "exiting";
                break;
            case "NEXT":
            case "CONTINUE":
                nextIndex = loopInfo.loopNext();
                if (nextIndex < index)
                    action = "restarting";
                else
                    action = "exiting";
                break;
            default:
                break;
        }
        
        frame.outputInfoMsg(STATUS_PROGRAM, command + " command " + action + " at index: " + nextIndex);
        return nextIndex;
    }

    /**
     * adds a new entry in the Loop parameters table.
     * This should only be called by 'CommandParser.compileProgram' when stepping
     * through the commands to verify and create the compiled list of commands.
     * 
     * @param name     - loop parameter name
     * @param loopId   - loop name-index combination to uniquely identify the loop param
     * @param loopInfo - the loop parameter to add
     */
    public static void saveLoopParameter (String name, LoopId loopId, LoopStruct loopInfo) {
        String functionId = CLASS_NAME + ".saveLoopParameter: ";
        
        // create a new loop ID (name + command index) for the entry and add it
        // to the list of IDs for the loop parameter name
        ArrayList<LoopId> loopList;
        if (loopNames.isEmpty()) {
            // first loop defined, create an empty array list and add it to the list of names for this name.
            loopList = new ArrayList<>();
            loopNames.put(name, loopList);
        } else {
            loopList = loopNames.get(name);
        }
        loopList.add(loopId);
        frame.outputInfoMsg(STATUS_DEBUG, functionId + "Number of loops with name " + name + ": " + loopList.size());
        
        // now add loop entry to hashmap based on name/index ID
        frame.outputInfoMsg(STATUS_DEBUG, functionId + "loopParams [" + loopParams.size() + "] " + loopId.name + " @ " + loopId.index);
        loopParams.put(loopId, loopInfo);
    }

    /**
     * determines the type of data in a String value.
     * 
     * @param strValue - the String value to check
     * 
     * @return the data type found
     */
    public static char classifyDataType (String strValue) {
        char dataType;

        if (strValue.startsWith("$I_")) {
            dataType = 'I'; // these should always be integers (unsure if they are unsigned or not)
        }
        else if (strValue.startsWith("$")) {
            dataType = 'S'; // these can be anything, but during compile we won't know runtime values
        }
        else if (strValue.equalsIgnoreCase("TRUE") ||
                 strValue.equalsIgnoreCase("FALSE")) {
            dataType = 'B';
        } else {
            try {
                Integer iVal = Utils.getHexValue (strValue);
                if (iVal == null) {
                    iVal = Utils.getIntValue (strValue);
                }
                if (iVal >= 0)
                    dataType = 'U';
                else
                    dataType = 'I';
            } catch (ParserException ex) {
                int offset = strValue.indexOf(' ');
                if (offset > 0) {
                    dataType = 'L';
                } else {
                    dataType = 'S';
                }
            }
        }
        
        return dataType;
    }
    
    /**
     * checks if a parameter name is valid.
     *   - name must begin with an alpha character
     *   - name must be only alphanumeric or '_' chars,
     *   - cannot be a reserved param name (RESPONSE, RESULT)
     *   - cannot be a command name or an operation name
     *   - cannot be a Loop parameter name.
     *   - checks if param is already defined
     * 
     * @param name - the name to check
     * 
     * @return  true if param is already defined, false if not
     * 
     * @throws ParserException - if not valid
     */
    public static boolean isValidParamName (String name) throws ParserException {
        String functionId = CLASS_NAME + ".isValidParamName: ";
        
        if (name.startsWith("$")) {
            name = name.substring(1);
        }
        verifyParamFormat(name);
        verifyNotReservedName(name);
        if (loopNames.containsKey(name)) {
            throw new ParserException(functionId + "using Loop parameter name: " + name);
        }

        // see if its already defined
        if (intParams.containsKey(name)   ||
            strParams.containsKey(name)   ||
            boolParams.containsKey(name)  ||
            arrayParams.containsKey(name) ||
            listParams.containsKey(name)    ) {
            return true;
        }
        
        return false;
    }
    
    /**
     * checks if a Loop parameter name is valid.
     * 
     * @param name - the name to check
     *               name must be only alphanumeric or '_' chars,
     *               cannot be a reserved name (RESPONSE, RESULT)
     *               or a String or Integer parameter name.
     * @param index - the command index for the FOR command
     * 
     * @return  true if valid
     * 
     * @throws ParserException
     */
    public static boolean isValidLoopName (String name, int index) throws ParserException {
        String functionId = CLASS_NAME + ".isValidLoopName: ";
        
        if (name.startsWith("$")) {
            name = name.substring(1);
        }
        verifyParamFormat(name);
        verifyNotReservedName(name);

        // make sure its not the same as a regular parameter
        if (intParams.containsKey(name)) {
            throw new ParserException(": using Integer parameter name: " + name);
        }
        if (strParams.containsKey(name)) {
            throw new ParserException(": using String parameter name: " + name);
        }
        if (boolParams.containsKey(name)) {
            throw new ParserException(": using Boolean parameter name: " + name);
        }
        if (arrayParams.containsKey(name)) {
            throw new ParserException(": using Array parameter name: " + name);
        }
        if (listParams.containsKey(name)) {
            throw new ParserException(": using List parameter name: " + name);
        }
        
        // now check if this loop name is nested in a loop having same name
        // get the list of loops using this parameter name (if any)
        ArrayList<LoopId> loopList = loopNames.get(name);
        if (loopList != null && ! loopList.isEmpty()) {
            // we have one or more uses of the same name, check if this is nested in one
            frame.outputInfoMsg(STATUS_PROGRAM, "   - checking previous uses of FOR Loop parameter " + name + " to see if we have a nesting problem");
            for (int ix = 0; ix < loopList.size(); ix++) {
                LoopId loopEntry = loopList.get(ix);
                LoopStruct loopInfo = getLoopStruct (loopEntry);
                if (loopInfo == null || ! loopInfo.isLoopComplete()) {
                    throw new ParserException(": Loop param " + name + " @ " + index + " is nested in same name at " + loopEntry.index);
                } else {
                    frame.outputInfoMsg(STATUS_PROGRAM, "   - FOR Loop parameter " + name + " @ " + loopEntry.index + " was complete");
                }
            }
        }
        
        return true;
    }
    
    /**
     * checks if a parameter is an Integer type
     * 
     * @param name - the name to check
     * 
     * @return  true if Integer parameter
     */
    public static boolean isIntegerParam (String name) {
        return name.startsWith("I_");
    }
    
    //========================================================================
    // LOCAL METHODS
    //========================================================================

    /**
     * checks if a parameter name is valid.
     *   name must be only alphanumeric or '_' chars and start with an alpha.
     * 
     * @param name - the name to check
     * 
     * @throws ParserException - if not valid
     */
    private static void verifyParamFormat (String name) throws ParserException {
        String functionId = CLASS_NAME + ".verifyParamFormat: ";
        
        if (name.startsWith("$")) {
            name = name.substring(1);
        }
        if (! Character.isLetter(name.charAt(0))) {
            // 1st character must be a letter
            throw new ParserException(functionId + "invalid initial character in parameter name: " + name);
        }
        for (int ix = 0; ix < name.length(); ix++) {
            if (  (name.charAt(ix) != '_') && ! Character.isLetterOrDigit(name.charAt(ix)) ) {
                throw new ParserException(functionId + "invalid character '" + name.charAt(ix) + "' in parameter name: " + name);
            }
        }
    }

    /**
     * checks if a parameter name matches a reserved name.
     *   checks against command names, operation names and reserved param names.
     * 
     * @param name - the name to check
     * 
     * @throws ParserException - if not valid
     */
    private static void verifyNotReservedName (String name) throws ParserException {
        String functionId = CLASS_NAME + ".verifyNotReservedName: ";
        
        if (name.contentEquals("RESPONSE") || name.contentEquals("RESULT")) {
            throw new ParserException(functionId + "using Reserved parameter name: " + name);
        }
        if (ScriptParser.isValidCommand(name)) {
            throw new ParserException(functionId + "using Reserved command name: " + name);
        }
        // TODO: verify against operation names
    }
    
}
