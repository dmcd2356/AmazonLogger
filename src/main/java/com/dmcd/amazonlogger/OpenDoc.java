/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import com.dmcd.amazonlogger.GUILogPanel.MsgType;
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
    
    private static Sheet sheetSel = null;               // the current spreadsheet tab selection
    private static final ArrayList<Sheet> sheetArray = new ArrayList<>(); // the list of sheets (tabs) loaded in memory

    /**
     * initializes all the static parameters
     */
    public static void init() {
        sheetSel = null;
        sheetArray.clear();
    }

    /**
     * creates the debug text format for showing the sheet column and row.
     * 
     * @param col     - the column selection
     * @param row     - the row selection
     * @param tabName - the sheet tab name (null if use current selected sheet)
     * 
     * @return a string containing the name of the sheet tab and the column and row selection
     */
    private static String showSelectionn(int col, int row, String tabName) {
        if (tabName == null) {
            tabName = sheetSel.getName();
        }
        return "sheet '" + tabName + "' col " + col + " row " + row;
    }
    
    /**
     * determines if a sheet has been selected
     * 
     * @return true if a sheet has been selected
     */
    public static boolean isSheetSelected() {
        return sheetSel != null;
    }

    /**
     * returns the number of sheets loaded.
     * 
     * @return number of sheets loaded
     */
    public static int getNumberOfSheets() {
        return sheetArray.size();
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
        col = sheetSel.getColumnCount();
        row = sheetSel.getRowCount();
        GUILogPanel.outputInfoMsg(MsgType.SSHEET, INDENT + "sheet '" + getSheetName() + "' new size: cols " + col + " rows " + row);
    }
    
    /**
     * sets the file selection to use for the spreadsheet file.
     * 
     * @param sheetNum  - the sheet selection to use
     * 
     * @throws ParserException
     */
    public static void setSheetSelection (Integer sheetNum) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (sheetNum == null) {
            throw new ParserException(functionId + "Sheet selection is null");
        }
        if (sheetNum >= sheetArray.size()) {
            throw new ParserException(functionId + "tab index " + sheetNum + " exceeds max tabs: " + sheetArray.size());
        }

        if (sheetNum >= 0) {
            sheetSel = sheetArray.get(sheetNum);
            GUILogPanel.outputInfoMsg(MsgType.SSHEET, INDENT + "tab " + sheetNum + " selection: '" + sheetSel.getName() + "'");
        } else {
            GUILogPanel.outputInfoMsg(MsgType.SSHEET, INDENT + "tab selection disabled");
        }
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
     * get the name of the selected tab.
     * 
     * @param ix - the tab number to check (1st entry is ix of 0)
     * 
     * @return name associated with specified tab
     * 
     * @throws ParserException
     */
    public static String getSheetName (int ix) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (ix >= sheetArray.size()) {
            throw new ParserException(functionId + "tab index " + ix + " exceeds max tabs: " + sheetArray.size());
        }

        return sheetArray.get(ix).getName();
    }

    /**
     * get the name of the selected tab.
     * 
     * @param ix   - the tab number to check (1st entry is ix of 0)
     * @param name - the name to set the tab entry to
     * 
     * @throws ParserException
     */
    public static void setSheetName (int ix, String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (ix >= sheetArray.size()) {
            throw new ParserException(functionId + "tab index " + ix + " exceeds max tabs: " + sheetArray.size());
        }

        if (name == null) {
            name = "" + ix;
        }
        sheetArray.get(ix).setName(name);
    }

    /**
     * get the selected sheet.
     * 
     * @param tabName - name of the tab for the sheet
     * 
     * @return the selected sheet (null if not found)
     */
    public static Sheet getSheetByName (String tabName) {
        for (int ix = 0; ix < sheetArray.size(); ix++) {
            Sheet sheet = sheetArray.get(ix);
            if (sheet.getName().contentEquals(tabName)) {
                return sheet;
            }
        }
        return null;
    }
    
    /**
     * get the cell value of the col & row on the selected sheet.
     * 
     * @param tabName - the sheet tab to use (null if use currently selected sheet)
     * @param col - the column selection
     * @param row - the row selection
     * 
     * @return cell contents at specified location
     * 
     * @throws ParserException
     */
    private static MutableCell getCellContents (String tabName, int col, int row) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        Sheet sheet;
        if (tabName != null) {
            sheet = getSheetByName (tabName);
            if (sheet == null) {
                throw new ParserException(functionId + "sheet tab name not found: " + tabName);
            }
        } else {
            sheet = sheetSel;
            if (sheet == null) {
                throw new ParserException(functionId + "no sheet selection for spreadsheet");
            }
            tabName = sheet.getName();
        }

        // verify the column and row are valid for sheet size
        int rowSize = sheet.getRowCount();
        int colSize = sheet.getColumnCount();
        if (row >= rowSize) {
            throw new ParserException(functionId + "sheet " + tabName + " row " + row + " exceeds max: " + rowSize);
        }
        if (col >= colSize) {
            throw new ParserException(functionId + "sheet " + tabName + "col " + col + " exceeds max: " + colSize);
        }

        return sheet.getCellAt(col,row);
    }
    
    /**
     * get the selected row of selected sheet.
     * 
     * @param tabName - name of the tab for the sheet
     * @param row     - row to return data from
     * @param colNum  - number of columns to return
     * 
     * @return the list of string entries for the selected row (empty string for any empty cells)
     * 
     * @throws ParserException
     */
    public static ArrayList<String> getRowArray (String tabName, int row, int colNum) throws ParserException {
        ArrayList<String> rowList = new ArrayList<>();

        // build up the response string of entries in the row
        for (int ix = 0; ix < colNum; ix++) {
            String strVal = "";
            MutableCell cell = getCellContents(tabName, ix, row);
            if (cell != null) {
                strVal = cell.getTextValue();
                if (strVal == null) {
                    strVal = "";
                }
            }
            rowList.add(strVal);
        }
        return rowList;
    }
    
    /**
     * get the state of whether the specified cell location is not defined (null).
     * 
     * @param col - the column selection
     * @param row - the row selection
     * 
     * @return true if cell is empty
     * 
     * @throws ParserException
     */
    public static boolean isCellEmpty (int col, int row) throws ParserException {
        MutableCell cell = getCellContents(null, col, row);
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

        String strVal = "null";
        MutableCell cell = getCellContents(null, col, row);
        if (cell == null) {
            return strVal;
        }
        Object object = cell.getValue();
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
                    throw new ParserException(functionId + showSelectionn(col, row, null) + " has non-numeric cell format: " + strVal);
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
     * @return string value at specified location (empty string if cell is empty)
     * 
     * @throws ParserException
     */
    public static String getCellTextValue (int col, int row) throws ParserException {
        String strVal = "";
        MutableCell cell = getCellContents(null, col, row);
        if (cell != null) {
            strVal = cell.getTextValue();
            if (strVal == null) {
                strVal = "";
            }
        }
        GUILogPanel.outputInfoMsg(MsgType.DEBUG, INDENT + showSelectionn(col, row, null) + " <- '" + strVal + "'");
        return strVal;
    }
    
    /**
     * get the value of the col & row on the selected sheet.
     * 
     * @param tabName - the sheet tab to use
     * @param col - the column selection
     * @param row - the row selection
     * 
     * @return string value at specified location (empty string if cell is empty)
     * 
     * @throws ParserException
     */
    public static String getCellTextValue (String tabName, int col, int row) throws ParserException {
        String strVal = "";
        MutableCell cell = getCellContents(tabName, col, row);
        if (cell != null) {
            strVal = cell.getTextValue();
            if (strVal == null) {
                strVal = "";
            }
        }
        GUILogPanel.outputInfoMsg(MsgType.DEBUG, INDENT + showSelectionn(col, row, tabName) + " <- '" + strVal + "'");
        return strVal;
    }
    
    /**
     * finds the first row in the spreadsheet that contains the specified entry.
     * 
     * @param tabName  - name of the tab selection to search
     * @param col      - column in which to search
     * @param startRow - starting row to start searching
     * @param entry    - the string value to search for
     * 
     * @return the row of the 1st occurrence of the order number in the spreadsheet
     * 
     * @throws ParserException
     */
    public static int findColumnEntry (String tabName, int col, int startRow, String entry) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        Sheet sheet = getSheetByName (tabName);
        if (sheet == null) {
            throw new ParserException(functionId + "invalid selection for tab name");
        }
        int rowIx = -1;
        for (int row = startRow; row < sheet.getRowCount(); row++) {
            String cellValue = OpenDoc.getCellTextValue(tabName, col, row);
            if (cellValue.isBlank()) {
                GUILogPanel.outputInfoMsg(MsgType.SSHEET, "Order not found. Exiting at row " + row);
                break;
            }
            if (cellValue.contentEquals(entry)) {
                rowIx = row;
                break;
            }
        }
        return rowIx;
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

        Object objVal = null;
        MutableCell cell = getCellContents(null, col, row);
        if (cell != null) {
            objVal = cell.getValue();
        }
        if (objVal == null) {
            throw new ParserException(functionId + showSelectionn(col, row, null) + ": cell value is null");
        }
        BigDecimal bdValue = (BigDecimal) objVal;
        GUILogPanel.outputInfoMsg(MsgType.DEBUG, INDENT + showSelectionn(col, row, null) + " <- " + objVal.toString());
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

        MutableCell cell = getCellContents(null, col, row);
        if (cell == null) {
            throw new ParserException(functionId + showSelectionn(col, row, null) + ": cell is null");
        }
        
        if (objVal == null) {
            cell.clearValue();
            return;
        }

        String strClass = objVal.getClass().getName();
        if (! strClass.contentEquals("java.lang.String")) {
            // if value is not a String, save it as it is
            cell.setValue(objVal);
        } else {
            // if it is a String, check if it is either a Long or Integer and convert
            String text = objVal.toString();
            try {
                Long iValue = Utils.getLongOrUnsignedValue(text);
                cell.setValue(iValue);
                strClass = "java.lang.Long";
            } catch (ParserException exMsg) {
                // if not an Integer but the value is enclosed in quotes, remove them
                if (text.charAt(0) == '"' && text.charAt(text.length()-1) == '"') {
                    text = text.substring(1, text.length()-1);
                }
                cell.setValue(text);
            }
        }

        GUILogPanel.outputInfoMsg(MsgType.SSHEET, INDENT + showSelectionn(col, row, null)
                + " -> " + objVal.toString() + " (type " + strClass + ")");
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
        MutableCell cell = getCellContents(null, col, row);
        if (cell != null) {
            String hexColor = String.format("0x%06x", color.getRGB());
            String message = showSelectionn(col, row, null) + " RGB -> " + hexColor;
            try {
                cell.setBackgroundColor(color);
                GUILogPanel.outputInfoMsg(MsgType.SSHEET, INDENT + "set color " + message);
            } catch (org.jdom.IllegalDataException exMsg) {
                GUILogPanel.outputInfoMsg(MsgType.WARN, "ERROR on setCellColor: " + message);
            }
        }
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
                GUILogPanel.outputInfoMsg(MsgType.SSHEET, INDENT + "'" + sheetSel.getName() + "' = index " + ix);
                return ix;
            }
        }
        return -1; // this indicates name was not found
    }
    
    /**
     * reads the specified number of spreadsheet tabs into memory for accessing the data.
     * 
     * @param file      - the file to load from
     * @param numSheets - number of sheets (tabs) to load into memory
     *                    (0 to reload the current number of sheets selected)
     * 
     * @return true if successful
     * 
     * @throws ParserException
     */
    public static boolean loadFromFile (File file, int numSheets) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (file == null) {
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
                SpreadSheet spreadsheet = SpreadSheet.createFromFile(file);
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
        
        int rows = getRowSize();
        int cols = getColSize();
        GUILogPanel.outputInfoMsg(MsgType.INFO, INDENT + "Spreadsheet size: cols = " + cols + ", rows = " + rows);
        return true;
    }

    /**
     * saves the modified spreadsheet data written to the spreadsheet file.
     * 
     * @param file    - the file to save to
     * @param index   - index of tab to update
     * 
     * @throws ParserException
     * @throws IOException 
     */
    public static void saveToFile (File file, Integer index) throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (file == null) {
            throw new ParserException(functionId + "Spreadsheet file is not defined");
        }
        
        Sheet sheet = sheetArray.get(index);
        sheet.getSpreadSheet().saveAs(file);
        GUILogPanel.outputInfoMsg(MsgType.INFO, INDENT + "Saving sheet '" + sheet.getName() + "' to file: "
                                    + sheet.getRowCount() + " rows, "
                                    + sheet.getColumnCount() + " cols");
    }

    /**
     * creates a spreadsheet image that has the specified column header.
     * 
     * THIS VERSION ALLOWS EXPANSION AND ADDING TABS, BUT DATA IS NEVER WRITTEN
     * TO THE NEW TABS, JUST THE TAB NAME. BUT DATA IS WRITTEN TO THE INITIAL SHEET.
     * 
     * @param file     - the file to create as a spreadsheet
     * @param name     - name of the tab
     * @param headList - the header to place as 1st row in sheet (defines the column size)
     * 
     * @throws ParserException
     * @throws IOException
     */
    public static void ssImageCreate (File file, String name, ArrayList<String> headList) throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (file == null) {
            throw new ParserException(functionId + "Spreadsheet file is not defined");
        }
        if (headList == null || headList.isEmpty()) {
            throw new ParserException(functionId + "Array list is blank");
        }
        
        // create the spreadsheet image and save sheets in our memory image of the sheets (sheetArray)
        TableModel model = new DefaultTableModel(null, headList.toArray());
        SpreadSheet sSheet = SpreadSheet.createEmpty(model);
        
        // make this sheet the current selection and set its name
        sheetSel = sSheet.getSheet(0);
        sheetSel.setName(name);
        
        // set array of sheets to just this one
        sheetArray.clear();
        sheetArray.add(sheetSel);
    }
    
    /**
     * creates a spreadsheet image that has the specified column header.
     * 
     * THIS VERSION DEFINES MULTIPLE TABS CORRECTLY, BUT DOES NOT ALLOW EXPANSIOM OF
     * ROWS OR COLUMNS AND DOES NOT PLACE ANY CONTENT IN THE CELLS.
     * 
     * @param file     - the file to create as a spreadsheet
     * @param tabList  - the list of names for the tabs
     * @param headList - the header to place as 1st row in sheet (defines the column size)
     * 
     * @throws ParserException
     * @throws IOException
     */
    public static void ssImageCreate (File file, ArrayList<String> tabList, ArrayList<String> headList) throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (file == null) {
            throw new ParserException(functionId + "Spreadsheet file is not defined");
        }
        if (tabList == null || tabList.isEmpty()) {
            throw new ParserException(functionId + "Tab list is blank");
        }
        if (headList == null || headList.isEmpty()) {
            throw new ParserException(functionId + "Header list is blank");
        }
        
        // create the spreadsheet image and save sheets in our memory image of the sheets (sheetArray)
        TableModel model = new DefaultTableModel(null, headList.toArray());
        SpreadSheet sSheet = SpreadSheet.createEmpty(model);
        
        // make this sheet the current selection and set its name
        sheetSel = sSheet.getSheet(0);
        sheetSel.setName(tabList.getFirst());
        setSize (headList.size(), 1000);
        
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
    }
/*
        Sheet sheet = sheetSel;
        if (tabName != null) {
            int ix = findSheetByName (tabName);
            sheet = sheetArray.get(ix);
        }
        sheet.getSpreadSheet().saveAs(file);
        GUILogPanel.outputInfoMsg(MsgType.INFO, INDENT + "Saving sheet '" + sheet.getName() + "' to file: "
                                    + sheet.getRowCount() + " rows, "
                                    + sheet.getColumnCount() + " cols");
*/    
    /**
     * adds a new tab to the current spreadsheet file.
     * 
     * @param tabName - name to call tab selection
     * 
     * @return the index of the new tab
     * 
     * @throws ParserException
     */
    public static int addTab (String tabName) throws ParserException {
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
        return sSheet.getSheetCount() - 1;
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
