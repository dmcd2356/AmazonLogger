package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.AmazonReader.props;
import com.mycompany.amazonlogger.PropertiesFile.Property;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
//import org.jopendocument.dom.OOUtils;
//import org.jopendocument.dom.OOUtils;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

/**
 *
 * @author dan
 */
public class Spreadsheet {

    private static final String SKIP_AMOUNT = "-";          // the amount value to use for Total if entry is omitted
    private static final String RETURN_DATE = "RETURN";     // the date value to use for Delivered if item was returned

    private static Sheet  sheet_0 = null;           // the data from tab 0 (Dan)    of the spreadsheet
    private static Sheet  sheet_1 = null;           // the data from tab 1 (Connie) of the spreadsheet
    private static Sheet  sheetSel = null;          // the current spreadsheet tab selection
    private static File   SpreadsheetFile;          // the spreadsheet file
    private static Integer iSheetYear = null;       // the year value the spreadsheet is marked as

    // these are the names of the column headers.
    // the file must have these defined as they are here, although they may have spaces
    //  separating the words in the name and capitalization is ignored.
    private static enum Column { 
        DateOrdered,
        OrderNumber, 
        Total, 
        DateDelivered, 
        ItemIndex,
        Qty, 
        Description, 
        ItemPrice, 
        Pending, 
        Payment, 
        Refund, 
        CreditCard,
        PreTaxCost,     // (optional) 
        Tax,            // (optional) 
        Seller          // (optional) 
    };
    
    // spreadsheet access information
    private static int firstRow = -1;           // the first row following the header
    private static int lastValidColumn = 0;     // the column index of the last valid entry
    private static final HashMap<Column, Integer> hmSheetColumns = new HashMap<>();

    /**
     * returns the corresponding column index for the specified column name in the spreadsheet.
     * 
     * @param  colName - the column name to find the index of
     * 
     * @return the corresponding column index value (null if not found)
     */
    private static Integer getColumn (Column colName) {
        Set<Entry<Column, Integer>> entrySet = hmSheetColumns.entrySet();
        for (Entry<Column, Integer> entry : entrySet) {
            if (entry.getKey() == colName)
                return entry.getValue();
        }
        return null;
    }

    /**
     * converts a string to an enum type by eliminating all spaces from the name
     * 
     * @param value - the name of the string from the header in the spreadsheet
     * 
     * @return the compressed name that should match an entry from the Column enum list
     */
    private static String strToEnum (String value) {
        String strEnum = "";
        for (int ix = 0; ix < value.length(); ix++) {
            if (value.charAt(ix) != ' ') {
                strEnum = strEnum + value.charAt(ix);
            }
        }
        return strEnum;
    }

    /**
     * sets up the column hashmap 'hmSheetColumns'.
     * This sets corresponding column index values for each Column name definition
     *  (which matches the column names in the header section of the spreadsheet file).
     * 
     * @return true if successful
     */
    private static boolean setupColumns () {
        Sheet sheetHeader = sheet_0;  // use header on tab 0 (they should both be the same)
        hmSheetColumns.clear();
        
        // search the first column in the first 5 rows of the spreadsheet for
        // one of the column names (only need to search 1st 4 columns)
        int headerRow = -1;
        for (int row = 0; row < 5 && headerRow < 0; row++) {
            String strSpreadsheet = "";
            Object object = sheetHeader.getCellAt(0,row).getValue();
            if (object != null)
                strSpreadsheet = strToEnum(object.toString());
            if (! strSpreadsheet.isBlank()) {
                for (Column colEnum : Column.values()) { 
                    String strName = colEnum.name();
                    if (strSpreadsheet.equalsIgnoreCase(strName)) {
                        headerRow = row;
                        break;
                    }
                }
            }
        }
        if (headerRow < 1) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.setupColumns: Error in locating column header information");
            return false;
        }

        // the first row for data will be the line following the header
        frame.outputInfoMsg(UIFrame.STATUS_SSHEET, "Found header on row: " + headerRow);
        firstRow = headerRow + 1;
        
        // now let's run through the header columns to assign each column a value
        // (add 5 to the count in case we had some extra columns added that aren't in our list)
        int maxColValue = 0;
        for (int col = 0; col < Column.values().length + 5; col++) {
            boolean bFound = false;
            Object object = sheetHeader.getCellAt(col,headerRow).getValue();
            if (object == null || object.toString().isBlank()) {
                frame.outputInfoMsg(UIFrame.STATUS_SSHEET, "Header column " + col + " is empty");
                break;
            }
            String colHeader = strToEnum(object.toString());
            for (Column colEnum : Column.values()) { 
                String strName = colEnum.name();
                if (colHeader.equalsIgnoreCase(strName)) {
                    // check if entry already placed
                    if (hmSheetColumns.containsKey(colEnum)) {
                        frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.setupColumns: Header column duplicate entry: " + colHeader);
                        return false;
                    }
                    frame.outputInfoMsg(UIFrame.STATUS_SSHEET, "Found header column: " + colHeader);
                    hmSheetColumns.put(colEnum, col);
                    maxColValue = col;
                    bFound = true;
                    break;
                }
            }
            if (! bFound) {
                frame.outputInfoMsg(UIFrame.STATUS_WARN, "Spreadsheet.setupColumns: Ignoring unknown header column entry: " + colHeader);
            }
        }
        
        // verify we have all of the required values and no duplicates
        // it's quicker to count the entries and eliminate the optional entries to
        // to see if we have all of the required.
        int count = hmSheetColumns.size();
        lastValidColumn = maxColValue;
        frame.outputInfoMsg(UIFrame.STATUS_SSHEET, "Total entries in header: " + count);
        if (hmSheetColumns.containsKey(Column.Seller)) count--;
        if (hmSheetColumns.containsKey(Column.PreTaxCost)) count--;
        if (hmSheetColumns.containsKey(Column.Tax)) count--;
        if (count < 12) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.setupColumns: Header column missing required entry(ies): " + (12 - count));
            return false;
        }
        
        frame.outputInfoMsg(UIFrame.STATUS_SSHEET, "Header columns successfully placed in columns");
        return true;
    }

    /**
     * gets a String value for the specified column and row.
     * The Object type returned from a 'text' type cell should be a String.
     * 
     * @param colEnum - the name of the column
     * @param row     - the row in the spreadsheet
     * 
     * @return the corresponding String value from the cell (empty string if blank)
     */
    private static String getStringValue (Column colEnum, int row) {
        String strValue = "";
        Integer col = getColumn(colEnum);
        if (col != null) {
            Object object = sheetSel.getCellAt(col,row).getValue();
            if (object != null) {
                strValue = object.toString();
            }
        }
        
        return strValue;
    }
    
    /**
     * gets a Double value for the specified column and row.
     * The Object type returned from a 'number' type cell should be a BigDecimal.
     * However, we check some other possible types and do a conversion.
     * 
     * @param colEnum - the name of the column
     * @param row     - the row in the spreadsheet
     * 
     * @return the corresponding Double value from the cell (null if blank)
     */
    private static Double getDoubleValue (Column colEnum, int row) {
        Double dValue = null;
        Integer col = getColumn(colEnum);
        if (col != null) {
            Object object = sheetSel.getCellAt(col,row).getValue();
            if (object != null) {
                Class oClass = object.getClass();
                switch (oClass.toString()) {
                    case "class java.math.BigDecimal":
                        BigDecimal bdValue = (BigDecimal) object;
                        dValue = bdValue.doubleValue();
                        break;
                    case "class java.lang.Double":
                        dValue = (Double) object;
                        break;
                    case "class java.lang.Integer":
                        Integer iValue = (Integer) object;
                        dValue = iValue.doubleValue();
                        break;
                    case "class java.lang.String":
                        String strValue = object.toString();
                        try {
                            dValue = Double.valueOf(strValue);
                        } catch (NumberFormatException ex) {
                            dValue = null;
                        }   break;
                    default:
                        break;
                }
            }
        }
        
        return dValue;
    }
    
    /**
     * gets a Integer value for the specified column and row.
     * The Object type returned from a 'number' type cell should be a BigDecimal.
     * However, we check some other possible types and do a conversion.
     * 
     * @param colEnum - the name of the column
     * @param row     - the row in the spreadsheet
     * @param iDecShift - the number of decimal places to shift the data (0 to 3)
     *                  (0 = none, 1 = x10, 2 = x100, 3 = x1000)
     * 
     * @return the corresponding Integer value from the cell (0 if blank)
     */
    private static Integer getIntegerValue (Column colEnum, int row, int iDecShift) {
        Integer iValue = 0;
        Integer col = getColumn(colEnum);
        
        // get the multiplier value (if any)
        BigDecimal bdMult = BigDecimal.TEN;
        int iMult;
        switch (iDecShift) {
            default:
                frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.getIntegerValue: dec shift out of range: " + iDecShift);
                iDecShift = 0;
                // fall through...
            case 0:
                iMult = 1;
                bdMult = null;
                break;
            case 1:
                iMult = 10;
                bdMult = BigDecimal.TEN;
                break;
            case 2:
                iMult = 100;
                bdMult = bdMult.multiply(bdMult);
                break;
            case 3:
                iMult = 1000;
                bdMult = bdMult.multiply(bdMult);
                bdMult = bdMult.multiply(bdMult);
                break;
        }

        if (col != null) {
            Object object = sheetSel.getCellAt(col,row).getValue();
            if (object != null) {
                Class oClass = object.getClass();
                switch (oClass.toString()) {
                    case "class java.math.BigDecimal":
                        BigDecimal bdValue = (BigDecimal) object;
                        if (bdMult != null)
                            bdValue = bdValue.multiply(bdMult);
                        iValue = bdValue.intValue();
                        break;
                    case "class java.lang.Double":
                        Double dValue = (Double) object;
                        iValue = dValue.intValue() * iMult;
                        break;
                    case "class java.lang.Integer":
                        iValue = (Integer) object;
                        iValue *= iMult;
                        break;
                    case "class java.lang.String":
                        String strValue = object.toString();
                        try {
                            iValue = Integer.valueOf(strValue);
                            iValue *= iMult;
                        } catch (NumberFormatException ex) {
                            iValue = null;
                        }   break;
                    default:
                        break;
                }
            }
        }
        
        return iValue;
    }

    /**
     * This returns the order number for the specified row of the spreadsheet.
     * 
     * @param row - the specified row
     * 
     * @return the order number found
     */
    public static String getOrderNumber (int row) {
        return getStringValue (Column.OrderNumber, row);
    }
    
    /**
     * This returns the order date for the specified row of the spreadsheet.
     * 
     * @param row - the specified row
     * 
     * @return the order date found
     */
    public static String getDateOrdered (int row) {
        String date = getStringValue (Column.DateOrdered, row);
        if (date.length() <= 10) {
            return date;
        }
        if (date.length() != 28) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.getDateOrdered: Invalid date format found: " + date);
            return null;
        }

        // the cell may have been formatted as a date, so the string returned is in the format:
        //    Fri Jan 19 00:00:00 EST 2024
        // we want to return a value that is of the form:
        //    2024-01-19
        String strMonName = date.substring(4, 7);
        String strDay     = date.substring(8, 10);
        String strYear    = date.substring(24, 28);
        int iMonth = DateFormat.getMonthInt (strMonName);
        if (iMonth == 0 ||
            Utils.getIntegerValue(strDay , 2) < 0 ||
            Utils.getIntegerValue(strYear, 4) < 0) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.getDateOrdered: Invalid date format found: " + date);
            return null;
        }
        String strMonth = (iMonth < 10) ? "0" + Integer.toString(iMonth) : Integer.toString(iMonth);
        frame.outputInfoMsg(UIFrame.STATUS_WARN, "Spreadsheet.getDateOrdered: Cell formatted as date: " + date);
        frame.outputInfoMsg(UIFrame.STATUS_WARN, "Spreadsheet.getDateOrdered: Converted to: " + strYear + "-" + strMonth + "-" + strDay);
        return strYear + "-" + strMonth + "-" + strDay;
    }
    
    /**
     * This returns the total cost of the order for the specified row of the spreadsheet.
     * 
     * @param row - the specified row
     * 
     * @return the total cost of the order
     */
    public static Integer getTotalCost (int row) {
        return getIntegerValue(Column.Total, row, 2);
    }
    
    /**
     * This returns the amount paid for the order for the specified row of the spreadsheet.
     * 
     * @param row - the specified row
     * 
     * @return the amount paid for the order
     */
    public static Integer getPaymentAmount (int row) {
        return getIntegerValue(Column.Payment, row, 2);
    }
    
    /**
     * This returns the amount refunded for the order for the specified row of the spreadsheet.
     * 
     * @param row - the specified row
     * 
     * @return the amount refunded for the order
     */
    public static Integer getRefundAmount (int row) {
        return getIntegerValue(Column.Refund, row, 2);
    }

    /**
     * writes any of the String values to the specified spreadsheet location.
     * Will only write if the data is defined and either the cell is currently
     *  empty or the bOverwrite flag is set.
     * If optional, will skip if column not defined. Otherwise error is flagged.
     * 
     * @param row         - the row of the spreadsheet to write to
     * @param strOrdNum   - the order number associated with the entry
     * @param bOverwrite  - true if we want to update the value even if it currently has a value
     * @param bIsRequired - true if the column is a required one
     * @param colEnum     - the column to write the value to
     * @param strVal      - the string value
     * 
     * @return +1 if successful, 0 if optional and value not present, -1 if error
     */
    private static int setSpreadsheetString (int row, String strOrdNum, boolean bOverwrite, boolean bIsRequired, Column colEnum, String strVal) {
        // value not defined, just exit
        if (strVal == null)
            return 0;

        // if column not found, exit - report error if it was a required column
        Integer col = getColumn(colEnum);
        if (col == null) {
            // column not found. is it a required column?
            if (bIsRequired) {
                // yes - this should not happen since we screened for this at startup
                frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.setSpreadsheetString: # " +
                                    strOrdNum + " header column missing: " + colEnum.name());
                return -1;
            }
            // no - fugettaboutit.
            return 0;
        }

        // value passed is defined, but only write it if the spreadsheet currently has
        //  no value posted to it, or the overwrite flag was set.
        if (sheetSel.getCellAt(col, row).getValue() == null ||
            sheetSel.getCellAt(col, row).getValue().toString().isBlank() || bOverwrite) {
            sheetSel.getCellAt(col, row).setValue(strVal);
            frame.outputInfoMsg(UIFrame.STATUS_SSHEET, "  output to row " + row + ",\tcol " + colEnum.name() + ":\t" + strVal);
        }
        return 1;
    }

    /**
     * writes any of the Integer values to the specified spreadsheet location.
     * Will only write if the data is defined and either the cell is currently
     *  empty or the bOverwrite flag is set.
     * If optional, will skip if column not defined. Otherwise error is flagged.
     * 
     * @param row         - the row of the spreadsheet to write to
     * @param strOrdNum   - the order number associated with the entry
     * @param bOverwrite  - true if we want to update the value even if it currently has a value
     * @param bIsRequired - true if the column is a required one
     * @param colEnum     - the column to write the value to
     * @param iVal        - the integer value
     * 
     * @return +1 if successful, 0 if optional and value not present, -1 if error
     */
    private static int setSpreadsheetInteger (int row, String strOrdNum, boolean bOverwrite, boolean bIsRequired, Column colEnum, Integer iVal) {
        // value not defined, just exit
        if (iVal == null)
            return 0;

        // if column not found, exit - report error if it was a required column
        Integer col = getColumn(colEnum);
        if (col == null) {
            // column not found. is it a required column?
            if (bIsRequired) {
                // yes - this should not happen since we screened for this at startup
                frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.setSpreadsheetInteger: # " +
                                    strOrdNum + " header column missing: " + colEnum.name());
                return -1;
            }
            // no - fugettaboutit.
            return 0;
        }

        // value passed is defined, but only write it if the spreadsheet currently has
        //  no value posted to it, or the overwrite flag was set.
        if (sheetSel.getCellAt(col, row).getValue() == null ||
            sheetSel.getCellAt(col, row).getValue().toString().isBlank() || bOverwrite) {
            sheetSel.getCellAt(col, row).setValue(iVal);
            frame.outputInfoMsg(UIFrame.STATUS_SSHEET, "  output to row " + row + ",\tcol " + colEnum.name() + ":\t" + iVal);
        }
        return 1;
    }

    /**
     * writes any of the 'cost' numeric values to the specified spreadsheet location.
     * Will only write if the data is defined and either the cell is currently
     *  empty or the bOverwrite flag is set.
     * If optional, will skip if column not defined. Otherwise error is flagged.
     * 
     * @param row         - the row of the spreadsheet to write to
     * @param strOrdNum   - the order number associated with the entry
     * @param bOverwrite  - true if we want to update the value even if it currently has a value
     * @param bIsRequired - true if the column is a required one
     * @param colEnum     - the column to write the value to
     * @param iVal        - the cost value in cents
     * 
     * @return +1 if successful, 0 if optional and value not present, -1 if error
     */
    private static int setSpreadsheetCost (int row, String strOrdNum, boolean bOverwrite, boolean bIsRequired, Column colEnum, Integer iVal) {
        // value not defined, just exit
        if (iVal == null)
            return 0;

        // if column not found, exit - report error if it was a required column
        Integer col = getColumn(colEnum);
        if (col == null) {
            // column not found. is it a required column?
            if (bIsRequired) {
                // yes - this should not happen since we screened for this at startup
                frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.setSpreadsheetCost: # " +
                                    strOrdNum + " header column missing: " + colEnum.name());
                return -1;
            }
            // no - fugettaboutit.
            return 0;
        }

        // value passed is defined, but only write it if the spreadsheet currently has
        //  no value posted to it, or the overwrite flag was set.
        String strVal = Utils.cvtAmountToString(iVal);
        BigDecimal bdVal = BigDecimal.valueOf(iVal);
        BigDecimal bd100 = BigDecimal.valueOf(100);
        bdVal = bdVal.divide(bd100, 2, RoundingMode.HALF_UP);
        if (sheetSel.getCellAt(col, row).getValue() == null ||
            sheetSel.getCellAt(col, row).getValue().toString().isBlank() || bOverwrite) {
            sheetSel.getCellAt(col, row).setValue(bdVal);
            frame.outputInfoMsg(UIFrame.STATUS_SSHEET, "  output to row " + row + ",\tcol " + colEnum.name() + ":\t" + strVal);
        }
        return 1;
    }
    
    /**
     * writes any of the 'date' values to the specified spreadsheet location.
     * Will only write if the data is defined and either the cell is currently
     *  empty or the bOverwrite flag is set.
     * If optional, will skip if column not defined. Otherwise error is flagged.
     * 
     * @param row         - the row of the spreadsheet to write to
     * @param strOrdNum   - the order number associated with the entry
     * @param bOverwrite  - true if we want to update the value even if it currently has a value
     * @param bIsRequired - true if the column is a required one
     * @param colEnum     - the column to write the value to
     * @param date        - the date value
     * 
     * @return +1 if successful, 0 if optional and value not present, -1 if error
     */
    private static int setSpreadsheetDate (int row, String strOrdNum, boolean bOverwrite, boolean bIsRequired, Column colEnum, LocalDate date) {
        // value not defined, just exit
        if (date == null)
            return 0;

        // if column not found, exit - report error if it was a required column
        Integer col = getColumn(colEnum);
        if (col == null) {
            // column not found. is it a required column?
            if (bIsRequired) {
                // yes - this should not happen since we screened for this at startup
                frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.setSpreadsheetDate: # " +
                                    strOrdNum + " header column missing: " + colEnum.name());
                return -1;
            }
            // no - fugettaboutit.
            return 0;
        }

        // value passed is defined, but only write it if the spreadsheet currently has
        //  no value posted to it, or the overwrite flag was set.
        String strVal = DateFormat.convertDateToString(date, false);
        if (sheetSel.getCellAt(col,row).getValue() == null ||
            sheetSel.getCellAt(col,row).getValue().toString().isBlank() || bOverwrite) {
            sheetSel.getCellAt(col,row).setValue(strVal);
            frame.outputInfoMsg(UIFrame.STATUS_SSHEET, "  output to row " + row + ",\tcol " + colEnum.name() + ":\t" + strVal);
        }
        return 1;
    }
    
    /**
     * saves the AmazonOrder information into the spreadsheet.
     * It does appropriate conversions of each data type prior to storage.
     * 
     * @param startRow   - the starting spreadsheet row (there may be multiple items in the order)
     * @param order      - the structure containing the Amazon order information
     * @param bOverwrite - true if overwrite data in the cell if it is not blank
     *                     (if false, it will only write the data if the cell is blank)
     * 
     * @return the number of items (rows) written
     */
    public static int setSpreadsheetOrderInfo (int startRow, AmazonOrder order, boolean bOverwrite) {
        // check for input errors
        if (startRow < 0 || order == null || order.item == null || order.item.isEmpty()) {
            return 0;
        }
        
        // get the order number of the order to see if it is valid
        String strOrdNum = order.getOrderNumber();
        if (strOrdNum == null) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.setSpreadsheetOrderInfo: order does not have an order number");
            return 0;
        }
        
        // check if we are overwriting an entry that is already partially filled in (adding details)
        // this does a check to verify the item count matches what's defined in the spreadsheet.
        // If the number of items in this order exceeds the number of items the spreadsheet has for
        //  this order number, we might have not terminated the web page correctly and read some advertised
        //  ones as part of the order. Let's indicate the anomaly, but proceed with the truncated count.
        int iItemCount = order.item.size();
        if (! bOverwrite) {
            int iSpreadItems = getItemCount (strOrdNum);
            if (iItemCount != iSpreadItems) {
                frame.outputInfoMsg(UIFrame.STATUS_WARN, "Spreadsheet.setSpreadsheetOrderInfo: # " + strOrdNum + " number of items passed (" +
                                    order.item.size() + ") != # in spreadsheet (" + iSpreadItems + ")");
                if (iItemCount > iSpreadItems)
                    iItemCount = iSpreadItems;
            }
        }

        // output each item in the order to the spreadsheet
        frame.outputInfoMsg(UIFrame.STATUS_SSHEET, "outputting to row " + startRow + " -> " + order.item.size() + " items");
        for (int ix = 0; ix < iItemCount; ix++) {
            int row = startRow + ix;
                
            // THE ORDER INFORMATION
            if (setSpreadsheetString (row, strOrdNum, bOverwrite, true, Column.OrderNumber, order.getOrderNumber()) <= 0) return ix;
            if (setSpreadsheetDate   (row, strOrdNum, bOverwrite, true, Column.DateOrdered,   order.getOrderDate())   <= 0) return ix;
            if (ix == 0) {
                if (setSpreadsheetCost   (row, strOrdNum, bOverwrite, true,  Column.Total,      order.getTotalCost()) <= 0) return ix;
                if (setSpreadsheetCost   (row, strOrdNum, bOverwrite, false, Column.PreTaxCost, order.getGrossCost()) <  0) return ix;
                if (setSpreadsheetCost   (row, strOrdNum, bOverwrite, false, Column.Tax,        order.getTaxCost())   <  0) return ix;
            } else {
                if (setSpreadsheetString (row, strOrdNum, bOverwrite, true,  Column.Total,      SKIP_AMOUNT)  < 0) return ix;
                if (setSpreadsheetString (row, strOrdNum, bOverwrite, false, Column.PreTaxCost, SKIP_AMOUNT)  < 0) return ix;
                if (setSpreadsheetString (row, strOrdNum, bOverwrite, false, Column.Tax,        SKIP_AMOUNT)  < 0) return ix;
            }
            
            // THE ITEM INFORMATION
            AmazonItem item = order.getItem(ix);
            if (item.getReturned()) {
                if (setSpreadsheetString (row, "", bOverwrite, true, Column.DateDelivered, RETURN_DATE)          <  0) return ix;
            } else {
                if (setSpreadsheetDate   (row, "", bOverwrite, true, Column.DateDelivered, item.getDeliveryDate()) <= 0) return ix;
            }
            if (order.getItemCount() > 1) { // skip (leave blank) for single item orders
                String strVal = (ix + 1) + " of " + order.getItemCount();
                if (setSpreadsheetString (row, strOrdNum, bOverwrite, true, Column.ItemIndex, strVal) < 0)  return ix;
            }
            if (setSpreadsheetInteger (row, strOrdNum, bOverwrite, true,  Column.Qty,           item.getQuantity())    <= 0) return ix;
            if (setSpreadsheetString  (row, strOrdNum, bOverwrite, true,  Column.Description, item.getDescription()) <= 0) return ix;
            if (setSpreadsheetCost    (row, strOrdNum, bOverwrite, false, Column.ItemPrice,     item.getItemCost())    <  0) return ix;
            if (setSpreadsheetString  (row, strOrdNum, bOverwrite, false, Column.Seller,      item.getSeller())      <  0) return ix;
        }
                
        return iItemCount;
    }

    /**
     * writes the Payment amount to the specified row of the spreadsheet.
     * 
     * @param row    - the row to write to
     * @param amount - the payment amount in cents
     */
    public static void setSpreadsheetPayment (int row, int amount) {
        setSpreadsheetCost (row, "", true, true, Column.Payment, amount);
    }
    
    /**
     * writes the Refund amount to the specified row of the spreadsheet.
     * 
     * @param row    - the row to write to
     * @param amount - the refund amount in cents
     */
    public static void setSpreadsheetRefund (int row, int amount) {
        setSpreadsheetCost (row, "", true, true, Column.Refund, amount);
    }
    
    /**
     * writes the Pending amount to the specified row of the spreadsheet.
     * 
     * @param row    - the row to write to
     * @param amount - the pending amount in cents
     */
    public static void setSpreadsheetPending (int row, int amount) {
        setSpreadsheetCost (row, "", true, true, Column.Pending, amount);
    }
    
    /**
     * writes the Credit Card name-date to the specified row of the spreadsheet.
     * 
     * @param row        - the row to write to
     * @param ccFilename - the credit card filename
     */
    public static void setSpreadsheetCreditCard (int row, String ccFilename) {
        setSpreadsheetString (row, "", true, true, Column.CreditCard, ccFilename);
    }

    /**
     * highlights the specified row to indicate that it has been balanced.
     * It may leave the Refund blank if a refund is due and this is a payment,
     *  and may also leave the Pending column blank if there is still money due.
     * 
     * @param row           - the row to highlight
     * @param bPayment      - true if this is a payment
     * @param bRemaining    - true if there is a remaining balance on the refund due
     * @param colorOfMonth  - the color to use for highlighting
     */
    public static void highlightOrderInfo (int row, boolean bPayment, boolean bRemaining, Color colorOfMonth) {
        // check if item is marked as returned
        boolean bReturn = sheetSel.getCellAt(getColumn(Column.DateDelivered),row).getTextValue().contentEquals(RETURN_DATE);

        if (bPayment) {
            // for a payment entry...
            for (int col = 0; col <= lastValidColumn; col++) {
                if (col == getColumn(Column.Refund) && bReturn) {
                    // skip highlighting the Refund column if item is marked as a return.
                    // this will be filled in when the refund is found in the card ledger.
                } else if (col == getColumn(Column.Pending) && bRemaining) {
                    // skip highlighting the Pending column if the total amount did not match
                } else {
                    sheetSel.getCellAt(col,row).setBackgroundColor(colorOfMonth);
                }
            }
        } else {
            // for refunds, always mark the refund column as done
            sheetSel.getCellAt(getColumn(Column.Refund),row).setBackgroundColor(colorOfMonth);
        }
    }
    
    /**
     * returns the first empty row in the spreadsheet data can be written to.
     * 
     * @return the next available row to write to
     */
    public static int getLastRowIndex () {
        // find the last row in the current sheet
        int row = -1;
        if (sheetSel != null) {
            for (row = firstRow; ! sheetSel.getCellAt(getColumn(Column.OrderNumber),row).getValue().toString().isBlank(); row++) {}
        }
        return row;
    }

    public static boolean isSheetEmpty() {
        if (sheetSel != null) {
            return sheetSel.getCellAt(getColumn(Column.OrderNumber),firstRow).getValue().toString().isBlank();
        }
        return true;
    }
    
    /**
     * counts the number of entries (consecutive) in the spreadsheet for the specified order number.
     * 
     * @param strOrderNum - the order number to find
     * 
     * @return the number of entries found for that order number (-1 if not found)
     */
    public static int getItemCount (String strOrderNum) {
        int count = -1;
        if (sheetSel != null) {
            String ssNumber = "x";
            for (int row = firstRow; ! ssNumber.isBlank(); row++) {
                ssNumber = sheetSel.getCellAt(getColumn(Column.OrderNumber),row).getValue().toString();
                if (ssNumber.contentEquals(strOrderNum)) {
                    count = 0;
                    while (ssNumber.contentEquals(strOrderNum)) {
                        count++;
                        ssNumber = sheetSel.getCellAt(getColumn(Column.OrderNumber),row + count).getValue().toString();
                    }
                    break;
                }
            }
        }
        return count;
    }

    /**
     * finds the first row in the spreadsheet that contains the specified order number.
     * 
     * @param strOrderNum - the order number to find
     * 
     * @return the row of the 1st occurrance of the order number in the spreadsheet
     */
    public static int findItemNumber (String strOrderNum) {
        if (sheetSel != null) {
            String ssNumber = "x";
            for (int row = firstRow; ! ssNumber.isBlank(); row++) {
                ssNumber = sheetSel.getCellAt(getColumn(Column.OrderNumber),row).getValue().toString();
                if (ssNumber.contentEquals(strOrderNum)) {
                    return row;
                }
            }
        }
        return -1;
    }
    
    /**
     * finds if the selected spreadsheet contains the credit card file name/date.
     * 
     * @param sheetName  - the sheet to search
     * @param strPdfName - the credit card id (filename/date) to search for
     * 
     * @return true if the credit card entry was found
     * 
     * @throws IOException 
     */
    public static boolean findCreditCardEntry (String sheetName, String strPdfName) throws IOException {

        // load the spreadsheet sheets into memory for each account
        selectSpreadsheetTab (sheetName);

        // find the last row in each sheet
        for (int row = firstRow; !sheetSel.getCellAt(getColumn(Column.OrderNumber),row).getTextValue().isBlank(); row++) {
            String cellValue = sheetSel.getCellAt(getColumn(Column.CreditCard),row).getTextValue();
            if (cellValue != null && strPdfName.contentEquals(cellValue)) {
                frame.outputInfoMsg(UIFrame.STATUS_WARN, "Spreadsheet.findCreditCardEntry: '" + strPdfName + "' was already balanced in the spreadsheet for " + sheetName);
                return true;
            }
        }

        return false;
    }

    /**    
     * sets the spreadsheet tab selection for many of the spreadsheet functions to use.
     * 
     * @param name - the name of the spreadsheet tab
     */
    public static void selectSpreadsheetTab (String name) {
        if (name == null) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.selectSpreadsheetTab: spreadsheet sheet selection not made");
            return;
        }
        if (! name.contentEquals("Dan") && ! name.contentEquals("Connie")) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.selectSpreadsheetTab: invalid tab selection: " + name);
            return;
        }
        
        frame.outputInfoMsg(UIFrame.STATUS_SSHEET, "Spreadsheet tab selection: '" + name + "'");
        props.setPropertiesItem(Property.SpreadsheetTab, name);
        if (name.contentEquals("Dan")) {
            sheetSel = sheet_0;
        }
        else {
            sheetSel = sheet_1;
        }
    }

    /**    
     * sets the default spreadsheet selections (for test mode).
     * 
     */
    public static void setDefaultSettings () {
        String ssPath = Utils.getPathFromPropertiesFile(Property.SpreadsheetPath);
        String ssFname = props.getPropertiesItem(Property.SpreadsheetFile, "");
        if (ssPath != null && ssFname != null) {
            File ssFile = new File(ssPath + "/" + ssFname);
            loadSpreadsheet(ssFile);
        }
        selectSpreadsheetTab (props.getPropertiesItem(Property.SpreadsheetTab, "Connie"));
    }
    
    /**    
     * gets the text data at the specified column and row of the selected spreadsheet tab.
     * 
     * @param tab - the name of the spreadsheet tab
     * @param col - the column of the spreadsheet
     * @param row - the row of the spreadsheet
     * 
     * @return text value at the specified location in the spreadsheet
     */
    public static String getSpreadsheetCell (String tab, int col, int row) {
        String strVal = "";
        if (tab == null) {
            tab = props.getPropertiesItem(Property.SpreadsheetTab, "");
        }
        if (SpreadsheetFile == null) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.getSpreadsheetCell: no spreadsheet file loaded");
        } else if (tab == null) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.getSpreadsheetCell: missing tab selection value");
        } else if (tab.contentEquals("Dan")) {
            strVal = sheet_0.getCellAt(col,row).getTextValue();
        } else if (tab.contentEquals("Connie")) {
            strVal = sheet_1.getCellAt(col,row).getTextValue();
        } else {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.getSpreadsheetCell: invalid tab selection: " + tab);
        }
        return strVal;
    }

    /**    
     * gets the class of the specified column and row of the spreadsheet.
     * 
     * @param col - the column of the spreadsheet
     * @param row - the row of the spreadsheet
     * 
     * @return text value at the specified location in the spreadsheet
     */
    public static String getSpreadsheetCellClass (int col, int row) {
        String strVal = "null";
        Object object = sheetSel.getCellAt(col,row).getValue();
        if (object != null) {
            Class oClass = object.getClass();
            strVal = oClass.getName();
        }
        return strVal;
    }
    
    /**    
     * writes the text data to the specified column and row of the selected spreadsheet tab.
     * 
     * @param tab  - the name of the spreadsheet tab
     * @param col  - the column of the spreadsheet
     * @param row  - the row of the spreadsheet
     * @param strVal - the data to write to the cell (null to erase)
     * 
     * @return text value at the specified location in the spreadsheet
     */
    public static String putSpreadsheetCell (String tab, int col, int row, String strVal) {
        String oldVal = "";
        if (tab == null) {
            tab = props.getPropertiesItem(Property.SpreadsheetTab, "");
        }
        if (SpreadsheetFile == null) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.putSpreadsheetCell: no spreadsheet file loaded");
        } else if (tab == null) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.putSpreadsheetCell: missing tab selection value");
        } else if (tab.contentEquals("Dan")) {
            oldVal = sheet_0.getCellAt(col,row).getTextValue();
            if (strVal == null)
                sheet_0.getCellAt(col, row).clearValue();
            else
                sheet_0.getCellAt(col, row).setValue(strVal);
        } else if (tab.contentEquals("Connie")) {
            oldVal = sheet_1.getCellAt(col,row).getTextValue();
            if (strVal == null)
                sheet_1.getCellAt(col, row).clearValue();
            else
                sheet_1.getCellAt(col, row).setValue(strVal);
        } else {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.putSpreadsheetCell: invalid tab selection: " + tab);
        }
        try {
            saveSpreadsheetFile();
        } catch (IOException ex) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.putSpreadsheetCell: saving spreadsheet file - " + ex);
        }
        return oldVal;
    }

    /**    
     * sets the background color of the specified column and row of the selected spreadsheet tab.
     * 
     * @param tab   - the name of the spreadsheet tab
     * @param col   - the column of the spreadsheet
     * @param row   - the row of the spreadsheet
     * @param color - the color to write to cell background
     */
    public static void setSpreadsheetCellColor (String tab, int col, int row, Color color) {
        if (tab == null) {
            tab = props.getPropertiesItem(Property.SpreadsheetTab, "");
        }
        if (SpreadsheetFile == null) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.putSpreadsheetCell: no spreadsheet file loaded");
        } else if (tab == null) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.putSpreadsheetCell: missing tab selection value");
        }
        sheetSel.getCellAt(col,row).setBackgroundColor(color);
        try {
            saveSpreadsheetFile();
        } catch (IOException ex) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.putSpreadsheetCell: saving spreadsheet file - " + ex);
        }
    }

    /**
     * Returns the year associated with the spreadsheet.
     * 
     * @return the year designated for the spreadsheet (null if invalid spreadsheet)
     */
    public static Integer getSpreadsheetYear () {
        return iSheetYear;
    }
    
    /**
     * reads the spreadsheet file into memory for accessing the data.
     * 
     * @param ssFile - file to use (optional - will ask user if not supplied)
     * 
     * @return true if successful
     */
    public static boolean loadSpreadsheet(File ssFile) {
        if (ssFile != null) {
            SpreadsheetFile = ssFile;
        } else {
            // see if we have a properties file that has a previously saved spreadsheet directory
            // if so, let's start the file selection process from there
            String ssPath = Utils.getPathFromPropertiesFile(Property.SpreadsheetPath);
            if (ssPath == null) {
                // else, find the latest year directory under Amazon and default to it
                ssPath = System.getProperty("user.dir");
                String strYear = PdfReader.getLatestYearDir(ssPath);
                if (strYear.isEmpty()) {
                    frame.outputInfoMsg(UIFrame.STATUS_WARN, "Spreadsheet.loadSpreadsheet: No year directory found in: " + ssPath);
                } else {
                    frame.outputInfoMsg(UIFrame.STATUS_INFO, "Latest directory year: " + strYear);
                    String tempPath = ssPath + "/" + strYear;
                    File tempDir = new File(tempPath);
                    if (tempDir.exists() && tempDir.isDirectory() && tempDir.listFiles() != null) {
                        ssPath = tempPath;
                    }
                }
            }

            // select the Amazon list spreadsheet file to read from
            JFileChooser jfc = new JFileChooser();
            jfc.setCurrentDirectory(new File(ssPath));
            jfc.setFileFilter(new FileNameExtensionFilter("LibreOffice ODS files", "ods"));
            jfc.showDialog(null,"Select the file");
            jfc.setVisible(true);
            File filename = jfc.getSelectedFile();
            if (filename == null) {
                frame.outputInfoMsg(UIFrame.STATUS_INFO, "No file chosen");
                return false;
            }

            SpreadsheetFile = new File(filename.getAbsolutePath());
        }

        // check the properties of the spreadsheet file chosen
        frame.setSpreadsheetSelection(SpreadsheetFile.getAbsolutePath());
        String filePath  = Utils.getFilePath(SpreadsheetFile);
        String fnameRoot = Utils.getFileRootname(SpreadsheetFile);
        String fnameExt  = Utils.getFileExtension(SpreadsheetFile);

        // update the spreadsheet path in the properties file
        if (!filePath.isEmpty()) {
            props.setPropertiesItem(Property.SpreadsheetPath, filePath);
            props.setPropertiesItem(Property.SpreadsheetFile, fnameRoot + fnameExt);
            frame.outputInfoMsg(UIFrame.STATUS_INFO, "Spreadsheet Path name: " + filePath);
        } else {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.loadSpreadsheet: Invalid path: " + filePath);
            return false;
        }
            
        // get the filename and verify it is of the correct format
        if (!fnameExt.contentEquals(".ods")) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.loadSpreadsheet: Invalid filename: " + fnameRoot + fnameExt + " (must be XXX.ods)");
            return false;
        }
        frame.outputInfoMsg(UIFrame.STATUS_INFO, "Spreadsheet File name: " + fnameRoot + fnameExt);
        
        // enable the Update and Balance buttons
        frame.enableClipboardButton(true);
        frame.enableCheckBalanceButton(true);
        
        // load the 'Dan' tab of the spreadsheet
        sheet_0 = null;
        sheet_1 = null;
        try {
            sheet_0 = SpreadSheet.createFromFile(SpreadsheetFile).getSheet("Dan");
            frame.outputInfoMsg(UIFrame.STATUS_INFO, "Loaded sheet '" + sheet_0.getName() + "' into memory");
        
            // load the 'Connie' tab of the spreadsheet
            sheet_1 = SpreadSheet.createFromFile(SpreadsheetFile).getSheet("Connie");
            frame.outputInfoMsg(UIFrame.STATUS_INFO, "Loaded sheet '" + sheet_1.getName() + "' into memory");

        } catch (IOException ex) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.loadSpreadsheet: " + ex);
        }

        // check if spreadsheet header is valid and setup column selections if so
        boolean success = setupColumns();
        if (! success) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.loadSpreadsheet: Invalid spreadsheet format - missng or incorrect header");
            return false;
        }
        
        // check to see what year this spreadsheet is for (we will only add entries for that year)
        Object oYear0 = sheet_0.getCellAt(0,0).getValue();
        Object oYear1 = sheet_1.getCellAt(0,0).getValue();
        if (oYear0 == null || oYear1 == null) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.loadSpreadsheet: Invalid spreadsheet format - header missing year");
            return false;
        }
        Integer iYear0 = Utils.getIntFromString (oYear0.toString(), 0, 4);
        Integer iYear1 = Utils.getIntFromString (oYear1.toString(), 0, 4);
        if (iYear0 == null || iYear1 == null || !Objects.equals(iYear0, iYear1)) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.loadSpreadsheet: Invalid spreadsheet format - header years invalid: " + oYear0.toString() + ", " + oYear1.toString());
            return false;
        }
        if (iYear0 < 2020 || iYear0 > 2040) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.loadSpreadsheet: Invalid spreadsheet format - header year out of range: " + iYear0);
            return false;
        }

        // get the name of the file to store debug info to (if defined)
        frame.setDebugOutputFile(props.getPropertiesItem(Property.DebugFileOut, ""));
        
        iSheetYear = iYear0;
        frame.outputInfoMsg(UIFrame.STATUS_INFO, "Spreadsheet year: " + iSheetYear);
        return true;
    }

    private static void reloadSpreadsheetFile () {
        if (SpreadsheetFile == null || sheetSel == null) {
            return;
        }
        try {
            sheet_0 = SpreadSheet.createFromFile(SpreadsheetFile).getSheet("Dan");
            sheet_1 = SpreadSheet.createFromFile(SpreadsheetFile).getSheet("Connie");
            frame.outputInfoMsg(UIFrame.STATUS_INFO, "Reloaded sheets into memory");

        } catch (IOException ex) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "Spreadsheet.loadSpreadsheet: from sheet: " + sheetSel.getName() + ", " + ex);
        }
    }
    
    /**
     * saves the modified spreadsheet data written to the spreadsheet file.
     * 
     * @throws IOException 
     */
    public static void saveSpreadsheetFile() throws IOException {
        frame.outputInfoMsg(UIFrame.STATUS_INFO, "Saving sheet '" + sheetSel.getName() + "' to spreadsheet file");
        sheetSel.getSpreadSheet().saveAs(SpreadsheetFile);
        
        // reload the spreadsheet sheets into memory, or we lose the info for one of the tabs
        reloadSpreadsheetFile ();
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

    /**
     * creates a backup copy of the current spreadsheet file selection.
     * 
     * @param backupId - the addendum to add to the end of the filename for the backup file name
     * 
     * @throws IOException 
     */
    public static void makeBackupCopy(String backupId) throws IOException {
        String filePath  = Utils.getFilePath(SpreadsheetFile);
        String fnameRoot = Utils.getFileRootname(SpreadsheetFile);
        String fnameExt  = Utils.getFileExtension(SpreadsheetFile);

        // make a backup copy of the current file before saving.
        frame.outputInfoMsg(UIFrame.STATUS_INFO, "Path to spreadsheet file: " + filePath);
        frame.outputInfoMsg(UIFrame.STATUS_INFO, "Name of spreadsheet file: " + fnameRoot + fnameExt);
        frame.outputInfoMsg(UIFrame.STATUS_INFO, "Creating backup of spreadsheet as: " + fnameRoot + backupId + fnameExt);

        Path srcPath = FileSystems.getDefault().getPath(filePath, fnameRoot + fnameExt);
        Path dstPath = FileSystems.getDefault().getPath(filePath, fnameRoot + backupId + fnameExt);
        Files.copy(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
    }
    
}
