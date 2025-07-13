/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.amazonlogger;

import com.amazonlogger.GUILogPanel.MsgType;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.jopendocument.dom.spreadsheet.MutableCell;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

/**
 *
 * @author dan
 */
public class OpenDoc {

    private static final String CLASS_NAME = OpenDoc.class.getSimpleName();
    private static final String INDENT = "     ";
    
    private static File  spreadsheetFile;                // the spreadsheet file
    private static Sheet sheetSel = null;                // the current spreadsheet tab selection
    private static final ArrayList<Sheet> sheetArray = new ArrayList<>(); // the list of sheets (tabs) loaded in memory

    /**
     * initializes all the static parameters
     */
    public static void init() {
        spreadsheetFile = null;
        sheetSel = null;
        sheetArray.clear();
    }
    
    /**
     * returns the current number of columns defined for the selected sheet.
     * 
     * @return the number of valid columns
     * 
     * @throws ParserException 
     */    
    public static int getColSize () throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (sheetSel == null) {
            throw new ParserException(functionId + "no sheet selected");
        }
        return sheetSel.getColumnCount();
    }
    
    /**
     * returns the current number of rows defined for the selected sheet.
     * 
     * @return the number of valid rows
     * 
     * @throws ParserException 
     */    
    public static int getRowSize () throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (sheetSel == null) {
            throw new ParserException(functionId + "no sheet selected");
        }
        return sheetSel.getRowCount();
    }

    /**
     * resizes the loaded sheet to the specified size.
     * If the new size if larger than the previous, it will add new empty cells.
     * 
     * @param col
     * @param row
     * 
     * @throws ParserException
     * @throws IOException
     */    
    public static void setSize (int col, int row) throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (sheetSel == null) {
            throw new ParserException(functionId + "no sheet selected");
        }
        sheetSel.setColumnCount(col, -1, true);
        sheetSel.setRowCount(row, -1);
        sheetSel.ensureColumnCount(col);
        sheetSel.ensureRowCount(row);
        GUILogPanel.outputInfoMsg(MsgType.SSHEET, INDENT + "sheet '" + getSheetName() + "' new size: cols " + col + " rows " + row);
    }
    
    /**
     * sets the file selection to use for the spreadsheet file.
     * 
     * @param file  - the spreadsheet file to read from
     * 
     * @throws ParserException
     */
    public static void setFileSelection (File file) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (file == null) {
            throw new ParserException(functionId + "Spreadsheet file is not defined");
        }
        spreadsheetFile = file;
    }

    /**
     * sets the file selection to use for the spreadsheet file.
     * 
     * @param sheetNum  - the sheet selection to use
     * 
     * @throws ParserException
     */
    public static void setSheetSelection (int sheetNum) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (sheetNum >= sheetArray.size()) {
            throw new ParserException(functionId + "tab index " + sheetNum + " exceeds max tabs: " + sheetArray.size());
        }

        sheetSel = sheetArray.get(sheetNum);
//        if (sheetSel.getName() == null || sheetSel.getName().isEmpty()) {
//            sheetSel.setName("" + sheetNum);
//        }
        GUILogPanel.outputInfoMsg(MsgType.SSHEET, INDENT + "tab " + sheetNum + " selection: '" + sheetSel.getName() + "'");
    }

    /**
     * gets the file selection to use for the spreadsheet file.
     * 
     * @return the current spreadsheet file selection
     */
    public static File getFileSelection () {
        return spreadsheetFile;
    }

    /**
     * get the name of the current tab selection.
     * 
     * @return name associated with current tab
     */
    public static String getSheetName () {
        return sheetSel.getName();
    }

    /**
     * get the state of whether the specified cell location is not defined (null).
     * 
     * @param col - the column selection
     * @param row - the row selection
     * 
     * @return true if cell is empty
     */
    public static boolean isCellEmpty (int col, int row) {
        MutableCell cell = sheetSel.getCellAt(col,row);
        return cell == null;
    }
    
    /**
     * get the object type of the col & row on the current sheet.
     * 
     * @param col - the column selection
     * @param row - the row selection
     * 
     * @return object type at specified location (BigDecimal or String)
     * 
     * @throws ParserException
     */
    public static String getCellObjectType (int col, int row) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (sheetSel == null) {
            throw new ParserException(functionId + "no sheet selection for spreadsheet");
        }
        int rowSize = sheetSel.getRowCount();
        int colSize = sheetSel.getColumnCount();
        if (row >= rowSize) {
            throw new ParserException(functionId + "row " + row + " exceeds max: " + rowSize);
        }
        if (col >= colSize) {
            throw new ParserException(functionId + "col " + col + " exceeds max: " + colSize);
        }

        String strVal = "null";
        Object object = sheetSel.getCellAt(col,row).getValue();
        if (object != null) {
            Class oClass = object.getClass();
            strVal = oClass.getName();
            switch (strVal) {
                case "class java.math.BigDecimal":
                case "java.math.BigDecimal":
                    strVal = "BigDecimal";
                    break;
                case "class java.lang.String":
                case "java.lang.String":
                    strVal = "String";
                    break;
                default:
                    throw new ParserException(functionId + "col " + col + " row " + row + " has non-numeric cell format: " + strVal);
            }
        }
        return strVal;
    }
    
    /**
     * get the value of the col & row on the current sheet.
     * 
     * @param col - the column selection
     * @param row - the row selection
     * 
     * @return string value at specified location
     * 
     * @throws ParserException
     */
    public static String getCellTextValue (int col, int row) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (sheetSel == null) {
            throw new ParserException(functionId + "no sheet selection for spreadsheet");
        }
        int rowSize = sheetSel.getRowCount();
        int colSize = sheetSel.getColumnCount();
        if (row >= rowSize) {
            throw new ParserException(functionId + "row " + row + " exceeds max: " + rowSize);
        }
        if (col >= colSize) {
            throw new ParserException(functionId + "col " + col + " exceeds max: " + colSize);
        }

        String strVal = sheetSel.getCellAt(col,row).getTextValue();
        GUILogPanel.outputInfoMsg(MsgType.SSHEET, INDENT + "read  tab " + sheetSel.getName() + " row " + row + " col " + col + " <- " + strVal);
        return strVal;
    }
    
    /**
     * get the integer value of the col & row on the current sheet.
     * 
     * @param col - the column selection
     * @param row - the row selection
     * 
     * @return Integer value at specified location
     * 
     * @throws ParserException
     */
    public static BigDecimal getCellNumericValue (int col, int row) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (sheetSel == null) {
            throw new ParserException(functionId + "no sheet selection for spreadsheet");
        }
        int rowSize = sheetSel.getRowCount();
        int colSize = sheetSel.getColumnCount();
        if (row >= rowSize) {
            throw new ParserException(functionId + "row " + row + " exceeds max: " + rowSize);
        }
        if (col >= colSize) {
            throw new ParserException(functionId + "col " + col + " exceeds max: " + colSize);
        }

        Object objVal = sheetSel.getCellAt(col,row).getValue();
        if (objVal == null) {
            throw new ParserException(functionId + "col " + col + " row " + row + " cell value is null");
        }
        BigDecimal bdValue = (BigDecimal) objVal;
        GUILogPanel.outputInfoMsg(MsgType.SSHEET, INDENT + "read  tab " + sheetSel.getName() + " row " + row + " col " + col + " <- " + objVal.toString());
        return bdValue;
    }
    
    /**
     * sets the value of the col & row on the current sheet.
     * 
     * NOTE: if the value is a numeric, it will be saved to the cell as a numeric.
     * 
     * @param col - the column selection
     * @param row - the row selection
     * @param objVal - the value to write to the cell (null to clear the entry)
     * 
     * @throws ParserException
     */
    public static void setCellValue (int col, int row, Object objVal) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (sheetSel == null) {
            throw new ParserException(functionId + "no sheet selection for spreadsheet");
        }
        int rowSize = sheetSel.getRowCount();
        int colSize = sheetSel.getColumnCount();
        if (row >= rowSize) {
            throw new ParserException(functionId + "row " + row + " exceeds max: " + rowSize);
        }
        if (col >= colSize) {
            throw new ParserException(functionId + "col " + col + " exceeds max: " + colSize);
        }

        if (objVal == null) {
            sheetSel.getCellAt(col,row).clearValue();
            return;
        }

        String strClass = objVal.getClass().getName();
        if (! strClass.contentEquals("java.lang.String")) {
            // if value is not a String, save it as it is
            sheetSel.getCellAt(col, row).setValue(objVal);
        } else {
            // if it is a String, check if it is either a Long or Integer and convert
            String text = objVal.toString();
            try {
                Long iValue = Utils.getLongOrUnsignedValue(text);
                sheetSel.getCellAt(col, row).setValue(iValue);
                strClass = "java.lang.Long";
            } catch (ParserException exMsg) {
                // if not an Integer but the value is enclosed in quotes, remove them
                if (text.charAt(0) == '"' && text.charAt(text.length()-1) == '"') {
                    text = text.substring(1, text.length()-1);
                }
                sheetSel.getCellAt(col, row).setValue(text);
            }
        }

        GUILogPanel.outputInfoMsg(MsgType.SSHEET, INDENT + "write tab " + OpenDoc.getSheetName()
                + " row " + row + " col " + col + " -> " + objVal.toString() + " (type " + strClass + ")");
    }
    
    /**
     * sets the background color of the cell at the specified col & row on the current sheet.
     * 
     * @param col - the column selection
     * @param row - the row selection
     * @param color - the color to set the background to
     * 
     * @throws ParserException
     */
    public static void setCellColor (int col, int row, Color color) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (sheetSel == null) {
            throw new ParserException(functionId + "no sheet selection for spreadsheet");
        }
        int rowSize = sheetSel.getRowCount();
        int colSize = sheetSel.getColumnCount();
        if (row >= rowSize) {
            throw new ParserException(functionId + "row " + row + " exceeds max: " + rowSize);
        }
        if (col >= colSize) {
            throw new ParserException(functionId + "col " + col + " exceeds max: " + colSize);
        }

        sheetSel.getCellAt(col,row).setBackgroundColor(color);

        String hexColor = String.format("0x%06x", color.getRGB());
        GUILogPanel.outputInfoMsg(MsgType.SSHEET, INDENT + "set color " + OpenDoc.getSheetName() + " row " + row + " col " + col + " RGB -> " + hexColor);
    }
    
    /**    
     * finds the spreadsheet tab selection.
     * 
     * @param name - the name (or number) of the spreadsheet tab
     * 
     * @return the index of the sheet selection
     * 
     * @throws ParserException
     */
    public static int findSheetByName (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null) {
            throw new ParserException(functionId + "spreadsheet tab selection is null");
        }
        if (sheetArray.isEmpty()) {
            throw new ParserException(functionId + "no sheet tabs loaded from spreadsheet");
        }
        // search for tab name
        for (int ix = 0; ix < sheetArray.size(); ix++) {
            if (name.contentEquals(sheetArray.get(ix).getName())) {
                return ix;
            }
        }
        return -1; // this indicates name was not found
    }
    
    /**
     * reads the specified number of spreadsheet tabs into memory for accessing the data.
     * 
     * @param numSheets    - number of sheets (tabs) to load into memory
     *                       (0 to reload the current number of sheets selected)
     * 
     * @return true if successful
     * 
     * @throws ParserException
     */
    public static boolean loadFromFile (int numSheets) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (spreadsheetFile == null) {
            throw new ParserException(functionId + "Spreadsheet file is not defined");
        }

        // if no spreadsheet count is defined, this is a reload, so use the current number of sheets defined
        String loadType = "Loaded";
        if (numSheets == 0) {
            if (sheetArray.isEmpty()) {
                GUILogPanel.outputInfoMsg(MsgType.WARN, INDENT + loadType + "No sheets were defined for a reload!");
                return true;
            }
            numSheets = sheetArray.size();
            loadType = "Reloaded";
        }

        // load the specified number of tabs of the spreadsheet into memory
        sheetArray.clear();
        for (int ix = 0; ix < numSheets; ix++) {
            Sheet sheet;
            try {
                SpreadSheet spreadsheet = SpreadSheet.createFromFile(spreadsheetFile);
                sheet = spreadsheet.getSheet(ix);
                sheetArray.add(sheet);
            } catch (IOException ex) {
                GUILogPanel.outputInfoMsg(MsgType.WARN, INDENT + "tab " + ix + " was unable to be loaded");
                return false;
            }
            GUILogPanel.outputInfoMsg(MsgType.INFO, INDENT + loadType + " sheet " + ix + " '" + sheet.getName() + "' into memory: "
                                        + sheet.getRowCount() + " rows, "
                                        + sheet.getColumnCount() + " cols");
        }
        
        // init tab selection to first sheet
        setSheetSelection(0);
        return true;
    }

    /**
     * saves the modified spreadsheet data written to the spreadsheet file.
     * 
     * @throws ParserException
     * @throws IOException 
     */
    public static void saveToFile () throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (spreadsheetFile == null) {
            throw new ParserException(functionId + "Spreadsheet file is not defined");
        }
        
        for (int ix = 0; ix < sheetArray.size(); ix++) {
            Sheet sheet = sheetArray.get(ix);
            sheet.getSpreadSheet().saveAs(spreadsheetFile);
            GUILogPanel.outputInfoMsg(MsgType.INFO, INDENT + "Saving sheet " + ix + " '" + sheet.getName() + "' to file: "
                                        + sheet.getRowCount() + " rows, "
                                        + sheet.getColumnCount() + " cols");
        }
        
        // reload the spreadsheet sheets into memory, or we lose the info for one of the tabs
        loadFromFile (0);
    }

    /**
     * creates a spreadsheet file that has the specified column header.
     * 
     * THIS VERSION ALLOWS EXPANSION AND ADDING TABS, BUT DATA IS NEVER WRITTEN
     * TO THE NEW TABS, JUST THE TAB NAME. BUT DATA IS WRITTEN TO THE INITIAL SHEET.
     * 
     * @param file    - the file to create as a spreadsheet
     * @param name    - name of the tab
     * @param arrList - the header to place as 1st row in sheet (defines the column size)
     * 
     * @throws ParserException
     * @throws IOException
     */
    public static void fileCreate (File file, String name, ArrayList<String> arrList) throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (file == null) {
            throw new ParserException(functionId + "Spreadsheet file is not defined");
        }
        if (arrList == null || arrList.isEmpty()) {
            throw new ParserException(functionId + "Array list is blank");
        }
        
        // save the file selection
        setFileSelection (file);
        
        // create the spreadsheet image and save sheets in our memory image of the sheets (sheetArray)
        TableModel model = new DefaultTableModel(null, arrList.toArray());
        SpreadSheet sSheet = SpreadSheet.createEmpty(model);
        
        // make this sheet the current selection and set its name
        sheetSel = sSheet.getSheet(0);
        sheetSel.setName(name);
        
        // set array of sheets to just this one
        sheetArray.clear();
        sheetArray.add(sheetSel);

        // save the initial spreadsheet file
        saveToFile();
        
        int rows = getRowSize();
        int cols = getColSize();
        GUILogPanel.outputInfoMsg(MsgType.INFO, INDENT + "Spreadsheet size: cols = " + cols + ", rows = " + rows);
    }
    
    /**
     * creates a spreadsheet file that has the specified column header.
     * 
     * THIS VERSION DEFINES MULTIPLE TABS CORRECTLY, BUT DOES NOT ALLOW EXPANSIOM OF
     * ROWS OR COLUMNS AND DOES NOT PLACE ANY CONTENT IN THE CELLS.
     * 
     * @param file       - the file to create as a spreadsheet
     * @param tabList    - the list of names for the tabs
     * @param headerList - the header to place as 1st row in sheet (defines the column size)
     * 
     * @throws ParserException
     * @throws IOException
     */
    public static void fileCreate (File file, ArrayList<String> tabList, ArrayList<String> headerList) throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (file == null) {
            throw new ParserException(functionId + "Spreadsheet file is not defined");
        }
        if (tabList == null || tabList.isEmpty()) {
            throw new ParserException(functionId + "Tab list is blank");
        }
        if (headerList == null || headerList.isEmpty()) {
            throw new ParserException(functionId + "Header list is blank");
        }
        
        // save the file selection
        setFileSelection (file);
        
        // create the spreadsheet image and save sheets in our memory image of the sheets (sheetArray)
        TableModel model = new DefaultTableModel(null, headerList.toArray());
        SpreadSheet sSheet = SpreadSheet.createEmpty(model);
        
        // make this sheet the current selection and set its name
        sheetSel = sSheet.getSheet(0);
        sheetSel.setName(tabList.getFirst());
        setSize (headerList.size(), 1000);
        
        // save the array of sheets
        Sheet nextSheet;
        sheetArray.clear();
        for (int ix = 0; ix < tabList.size(); ix++) {
            if (ix == 0) {
                nextSheet = sheetSel;
            } else {
                nextSheet = sheetSel.copy(ix, tabList.get(ix));
            }
            sheetArray.add(nextSheet);
        
            int rows = getRowSize();
            int cols = getColSize();
            GUILogPanel.outputInfoMsg(MsgType.INFO, INDENT + "sheet[" + ix + "] '" + nextSheet.getName() + "' size: cols = " + cols + ", rows = " + rows);
        }
        
        // save the initial spreadsheet file
        saveToFile();
    }
    
    /**
     * adds a new tab to the current spreadsheet file.
     * 
     * @param tabName - name to call tab selection
     * 
     * @throws ParserException
     */
    public static void addTab (String tabName) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (tabName == null || tabName.isBlank()) {
            throw new ParserException(functionId + "Tab name is blank");
        }

        // create a new tab for the current spreadsheet image
        SpreadSheet sSheet = sheetSel.getSpreadSheet();
        sheetSel = sSheet.addSheet(tabName);
        sheetSel.setName(tabName);
        
        // save the entry in our array of sheets
        sheetArray.add(sheetSel);
    }
    
//    /**
//     * saves the modified spreadsheet to the file and then opens the file.
//     */
//    public static void updateSpreadsheet() throws IOException {
//        OOUtils.open(sheetSel.getSpreadSheet().saveAs(SpreadsheetFile));
//    }

//    /**
//     * opens the spreadsheet file.
//     * 
//     * @throws IOException 
//     */
//    public static void openSpreadsheetFile() throws IOException {
//        OOUtils.open(SpreadsheetFile);
//    }
}
