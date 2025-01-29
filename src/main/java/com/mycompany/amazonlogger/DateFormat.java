package com.mycompany.amazonlogger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 *
 * @author dan
 */
public class DateFormat {
    
    public static final String DATE_SEP = "-";  // the separator char used in dates

    /*********************************************************************
    ** returns the number of days in the specified month.
    * 
    *  @param year - the year in question (for determining if leap year)
    *  @param mon  - the month
    * 
    *  @return number of days in month (1 - 31)
    */
    public static int getDaysInMonth (int year, int mon) {
        switch (mon) {
            default:
            case 1:  return 31;
            case 2:  if (year % 4 == 0) return 29; else return 28;
            case 3:  return 31;
            case 4:  return 30;
            case 5:  return 31;
            case 6:  return 30;
            case 7:  return 31;
            case 8:  return 31;
            case 9:  return 30;
            case 10: return 31;
            case 11: return 30;
            case 12: return 31;
        }
    }
    
    /*********************************************************************
    ** returns the integer month value corresponding to the month name.
    * 
    *  @param monthName - the name of the month (either full name or 3 character shorthand)
    *                     (case insensitive)
    * 
    *  @return number of the month (1 - 12, or 0 if not found)
    */
    public static int getMonthInt (String monthName) {
        int monValue = switch (monthName.toLowerCase()) {
            case "jan"       -> 1;
            case "january"   -> 1;
            case "feb"       -> 2;
            case "february"  -> 2;
            case "mar"       -> 3;
            case "march"     -> 3;
            case "apr"       -> 4;
            case "april"     -> 4;
            case "may"       -> 5;
            case "jun"       -> 6;
            case "june"      -> 6;
            case "jul"       -> 7;
            case "july"      -> 7;
            case "aug"       -> 8;
            case "august"    -> 8;
            case "sep"       -> 9;
            case "september" -> 9;
            case "oct"       -> 10;
            case "october"   -> 10;
            case "nov"       -> 11;
            case "november"  -> 11;
            case "dec"       -> 12;
            case "december"  -> 12;
                default -> 0;
            };
        
        return monValue;
    }

    /*********************************************************************
    ** returns the integer day of week corresponding to the weekday name.
    * 
    *  @param dayName - the name of the weekday (either full name or 3 character shorthand)
    *                   (case insensitive)
    * 
    *  @return number of the day (1 - 7, or -1 if not found)
    */
    public static int getDayOfWeekIndex (String dayName) {
        int dayValue = switch (dayName.toLowerCase()) {
            case "mon"       -> 1;
            case "monday"    -> 1;
            case "tue"       -> 2;
            case "tuesday"   -> 2;
            case "wed"       -> 3;
            case "wednesday" -> 3;
            case "thu"       -> 4;
            case "thursday"  -> 4;
            case "fri"       -> 5;
            case "friday"    -> 5;
            case "sat"       -> 6;
            case "saturday"  -> 6;
            case "sun"       -> 7;
            case "sunday"    -> 7;
                
            default -> -1;
        };
        
        return dayValue;
    }
    
    /*********************************************************************
    ** returns a date string (MM-DD) for a given Date value.
    * 
    *  @param date - the date to convert
    * 
    *  @return the corresponding Date formatted as: MM-DD
    */
    public static String convertDateToString (LocalDate date, boolean bInclYear) {
        if (date == null)
            return null;
        
        int iMonth = date.getMonthValue();
        int iDay = date.getDayOfMonth();

        String strDate = "";
        if (bInclYear) {
            int iYear = date.getYear();
            strDate += iYear + DATE_SEP;
        }
        if (iMonth < 10) strDate += "0";
        strDate += String.valueOf(iMonth) + DATE_SEP;
        if (iDay < 10)   strDate += "0";
        strDate += String.valueOf(iDay);
        return strDate;
    }

    /*********************************************************************
    ** returns an integer representation of a Date value.
    * 
    * @param date      - the date to convert
    * @param bInclYear - true if you want the integer result to include the year
    *                    e.g. 20251005 for a date of 10-05-2025
    * 
    *  @return the corresponding integer value of the date
    */
    public static int convertDateToInteger (LocalDate date, boolean bInclYear) {
        if (date == null)
            return -1;
        
        int iDate = (100 * date.getMonthValue()) + date.getDayOfMonth();
        if (bInclYear) {
            iDate += 10000 * date.getYear();
        }

        return iDate;
    }

    /*********************************************************************
    ** returns an integer representation of a date from the spreadsheet file.
    * 
    * @param strDate   - the string date from the spreadsheet to convert
    *                    MM-DD, MM/DD, MM-DD-YYYY, MM/DD/YYYY
    * @param bInclYear - true if you want the integer result to include the year
    *                    e.g. 20251005 for a date of 10-05-2025
    * 
    *  @return an integer value where the month is represented as
    *           hundreds value and day as ones, so "04-13" is value 413
    *           (and optionally the year is represented as 10000s value)
    * 
    *  @throws ParserException - if invalid date format
    */
    public static Integer cvtSSDateToInteger (String strDate, boolean bInclYear) throws ParserException {
        Integer iDate = null;
        
        if (strDate == null || strDate.isBlank() || strDate.length() < 5)
            return null;

        // this eliminates the leading ' char OpenOffice puts in if the cell format
        //  was set to date and a text entry was placed in it (which is what will be written by this program)
        if (strDate.charAt(0) == '\'' && strDate.length() >= 6)
            strDate = strDate.substring(1);
        
        // determine format of date:
        // is it MM-DD-YYYY, MM-DD, MM/DD/YYYY or MM/DD/YYYY style?
        if (strDate.charAt(2) == '-' || strDate.charAt(2) == '/') {
            Integer iMonth = Utils.getIntFromString (strDate, 0, 2);
            Integer iDay   = Utils.getIntFromString (strDate, 3, 2);
            Integer iYear  = null;
            if (strDate.length() >= 10)
                iYear  = Utils.getIntFromString (strDate, 6, 4);
            if (iMonth == null || iDay == null) {
                return null;
            }
            iDate = (100 * iMonth) + iDay;
            if (bInclYear && iYear != null) {
                    iDate = (10000 * iYear) + iDate;
            }
        }
        return iDate;
    }
    /*********************************************************************
    ** returns a Date value for the given date string.
    * 
    *  @param dateName - the date as an English language description
    *  @param past     - true if date was in the past (delivered) as opposed to the future (arriving on)
    * 
    *  @return the corresponding Date format value
    *
    *  @throws ParserException - if date was not in a form we understand
    */
    public static LocalDate getFormattedDate (String dateName, Boolean past) throws ParserException {

        // get current month, day and day of week
        LocalDateTime curDate = LocalDateTime.now();
        int iCurDOW = curDate.getDayOfWeek().getValue();
        int iCurDay  = curDate.getDayOfMonth();
        int iCurMon  = curDate.getMonth().getValue();
        int iCurYear = curDate.getYear();
        
        // init selected date values to current date
        int iThisYear = iCurYear;
        int iThisMon = iCurMon;
        int iThisDay = iCurDay;
        
        // skip any leading spaces
        int offset;
        for (offset = 0; offset < dateName.length() && dateName.charAt(offset) == ' '; offset++) { }
        dateName = dateName.substring(offset);

        String strMonNum = "??";
        String strDayNum = "??";
        int moLength = dateName.indexOf(' ');
        if (moLength >= 1 && dateName.length() >= moLength + 2) {
            // get the day of the month and convert it to 2 digits if < 10
            strDayNum = dateName.substring(moLength + 1);
            if (strDayNum.length() >= 2 && strDayNum.charAt(1) >= '0' && strDayNum.charAt(1) <= '9')
                strDayNum = strDayNum.substring(0,2);
            else
                strDayNum = "0" + strDayNum.charAt(0);

            // convert the month name to 2 digits
            dateName = dateName.substring(0,moLength);
            int iMonth = getMonthInt(dateName);
            if (iMonth > 0) {
                // if it is a past date and the date reflects the future, rewind it to last year
                if (past && iThisYear >= iCurYear && iMonth > iCurMon) {
                    iThisYear--;
                }

                strMonNum = String.valueOf(iMonth);
                if (strMonNum.length() == 1)
                    strMonNum = "0" + strMonNum;
            }
        }
        
        // if month was not given, see if they gave a day of the week instead
        if (strMonNum.equals("??")) {
            int relDOW = getDayOfWeekIndex(dateName);

            int iDayAdjust;
            if (relDOW > 0) {
                // day of week was referenced - need to know if past (Delivered) or future (Arriving)
                // determine how many days ago or how many days in the future
                iDayAdjust = relDOW - iCurDOW;
                if (past) {
                    if (iDayAdjust > 0) iDayAdjust -= 7;
                } else {
                    if (iDayAdjust < 0) iDayAdjust += 7;
                }
            } else {
                // else, maybe it was just a relative reference from today
                switch (dateName.toLowerCase()) {
                    case "overnight" -> iDayAdjust = 0;
                    case "today"     -> iDayAdjust = 0;
                    case "yesterday" -> iDayAdjust = -1;
                    case "tomorrow"  -> iDayAdjust = 1;

                    default -> // don't know what was given, but it wasn't valid.
                    throw new ParserException("getFormattedDate: invalid day name: " + dateName);
                }
            }

            // adjust the date accordingly
            if (iDayAdjust != 0) {
                iThisDay += iDayAdjust;
                if (iThisDay < 1) {
                    iThisMon -= 1;
                    if (iThisMon < 1) { iThisMon = 12; iThisYear--; }
                    iThisDay = getDaysInMonth(iThisYear, iThisMon) + iThisDay;
                } else {
                    int daysInMonth = getDaysInMonth(iThisYear, iThisMon);
                    if (iThisDay > daysInMonth) {
                        iThisDay -= daysInMonth;
                        iThisMon += 1;
                        if (iThisMon > 12) { iThisMon = 1; iThisYear++; }
                    }
                }
            }

            // if it is a past date and the date reflects the future, rewind it to last year
            if (past && iThisYear >= iCurYear && iThisMon > iCurMon) {
                iThisYear--;
            }
        
            strMonNum = String.valueOf(iThisMon);
            if (iThisMon < 10) strMonNum = "0" + strMonNum;
            strDayNum = String.valueOf(iThisDay);
            if (iThisDay < 10) strDayNum = "0" + strDayNum;
        }
        
        String strDate = String.valueOf(iThisYear) + DATE_SEP + strMonNum + DATE_SEP + strDayNum;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
        LocalDate date = LocalDate.parse(strDate, formatter);
        return date;
    }

}
