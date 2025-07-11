/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import com.mycompany.amazonlogger.GUILogPanel.MsgType;
import java.util.HashMap;

/**
 *
 * @author dan
 */
public class Keyword {
    
    private static final String CLASS_NAME = Keyword.class.getSimpleName();

    // these are the enums for the lines that are of interest to us in the clipboard contents
    public enum KeyTyp { NONE, HELLO_D, HELLO_C, ORDER_PLACED, ORDER_DETAILS, 
                         TOTAL_COST, ORDER_NUMBER, RETURNED, REFUNDED,
                         DELIVERED, ARRIVING, NOW_ARRIVING, PACKAGE_LEFT,
                         DESCRIPTION, DESCRIPTION_2, COMPLETE, GROSS_COST,
                         TAXES, SHIPPING, ITEM_COST, SELLER, QUANTITY,
                         END_OF_RECORD };

    // the list of how data is packaged for each command
    public enum DataTyp {
        NONE,       // no data included with the line, keyword fully defined
        PARTIAL,    // no data included with the line, keyword partially defined
        INLINE,     // the data is on the same line, following the keywords
        NEXTLINE    // the data is on the line following the keywords (keyword must be fully defined)
    };


    public class KeywordInfo {
        KeyTyp  eKeyId;         // keyword id
        int     keyLength;      // length of keyword
        
        public KeywordInfo (String keyword, KeyTyp id) {
            eKeyId = id;
            keyLength = keyword.length();
        }
        
        public KeywordInfo () {
            eKeyId = KeyTyp.NONE;
            keyLength = 0;
        }
    }

    private static final HashMap <String, KeywordInfo> Keyword_Orders = new HashMap<>();

    Keyword () {
        // these are the items we look for in the Orders and Invoice pages
        putKeywordInfo ("Hello, Dan"                , KeyTyp.HELLO_D);
        putKeywordInfo ("Hello, Connie"             , KeyTyp.HELLO_D);
        putKeywordInfo ("Order Details"             , KeyTyp.ORDER_DETAILS);
        putKeywordInfo ("Order placed"              , KeyTyp.ORDER_PLACED);     // date placed
        putKeywordInfo ("Total"                     , KeyTyp.TOTAL_COST);       // cost of order (with taxes & shipping)
        putKeywordInfo ("Order #"                   , KeyTyp.ORDER_NUMBER);     // order number
        putKeywordInfo ("Returned"                  , KeyTyp.RETURNED);
        putKeywordInfo ("Return started"            , KeyTyp.RETURNED);
        putKeywordInfo ("Return complete"           , KeyTyp.RETURNED);
        putKeywordInfo ("Refunded"                  , KeyTyp.REFUNDED);
        putKeywordInfo ("Item refunded"             , KeyTyp.REFUNDED);
        putKeywordInfo ("Delivered"                 , KeyTyp.DELIVERED);        // date delivered
        putKeywordInfo ("Arriving"                  , KeyTyp.ARRIVING);         // date arriving
        putKeywordInfo ("Now arriving"              , KeyTyp.NOW_ARRIVING);     // date arriving
        putKeywordInfo ("Your package was left"     , KeyTyp.PACKAGE_LEFT);
        putKeywordInfo ("Package was left"          , KeyTyp.PACKAGE_LEFT);
        putKeywordInfo ("Grand Total:"              , KeyTyp.TOTAL_COST);       // cost of order (with taxes & shipping)
        putKeywordInfo ("Sold by:"                  , KeyTyp.SELLER);           // seller
        putKeywordInfo ("$"                         , KeyTyp.ITEM_COST);        // cost of item
        putKeywordInfo ("Total before tax:"         , KeyTyp.GROSS_COST);       // cost of order (before taxes & shipping)
        putKeywordInfo ("Estimated tax to be collected:", KeyTyp.TAXES);        // cost of tax

        // these are items that will indicate there are no more orders in the page
        putKeywordInfo ("←Previous"                 , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Add to cart"               , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Bargain recommendations"   , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Consider these items"      , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Next set of slides"        , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Recommended based on"      , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Recommended for you"       , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Sponsored"                 , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Related to items you've viewedSee more", KeyTyp.END_OF_RECORD);
    }

    private void putKeywordInfo (String keyString, KeyTyp key) {
        Keyword_Orders.put(keyString, new KeywordInfo (keyString, key));
    }
    

    public static DataTyp getDataTypeInvoice (KeyTyp keyType) {
        switch (keyType) {
            case SELLER:
            case ITEM_COST:
            case DELIVERED:
            case ARRIVING:
            case NOW_ARRIVING:
                return Keyword.DataTyp.INLINE;
            case ORDER_NUMBER:
            case ORDER_PLACED:
            case TOTAL_COST:
            case GROSS_COST:
            case TAXES:
                return Keyword.DataTyp.NEXTLINE;
            case PACKAGE_LEFT:
            case END_OF_RECORD:
                return Keyword.DataTyp.PARTIAL;
            default:
                break;
        }
        return Keyword.DataTyp.NONE;
    }
    
    /**
     * finds keyword match to current line.
     * 
     * @param line     - the line read from the web page
     * 
     * @return the structure indicating the type of keyword found, null if not found
     */
    public static KeywordInfo getKeyword (String line) {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        for (HashMap.Entry<String, KeywordInfo> mapEntry : Keyword_Orders.entrySet()) {
            String keyStr = mapEntry.getKey();
            KeywordInfo keyInfo = mapEntry.getValue();

            DataTyp dataType = getDataTypeInvoice (keyInfo.eKeyId);
//            if (dataType == DataTyp.INLINE || dataType == DataTyp.PARTIAL) {
                // INLINE & PARTIAL entries are partial String matches
                if (line.startsWith(keyStr)) {
                        GUILogPanel.outputInfoMsg (MsgType.DEBUG, "PARTIAL : KeyTyp." + keyInfo.eKeyId +
                                                        " : Length." + keyInfo.keyLength + " : " + line);
                        return keyInfo;
                }
//            } else {
//                // NEXTLINE & NONE types should be fully defined lines in the table
//                if (line.contentEquals(keyStr)) {
//                        GUILogPanel.outputInfoMsg (MsgType.DEBUG, "EXACT : KeyTyp." + keyInfo.eKeyId +
//                                                        " : Length." + keyInfo.keyLength + " : " + line);
//                        return keyInfo;
//                }
//            }
        }
        String shortLine = line.substring(0, (line.length() > 125 ? 125 : line.length()));
        GUILogPanel.outputInfoMsg (MsgType.DEBUG, functionId + "NO MATCH: "  + shortLine);
        return null;
    }

    /**
     * finds the Keyword entry in the ORDERS table that has the specified Key type
     * 
     * @param keyId - the Key type to find
     * 
     * @return the corresponding Keyword entry, null if not found
     */
    public static KeywordInfo findOrdersKey (KeyTyp keyId) {
        for (HashMap.Entry<String, KeywordInfo> mapEntry : Keyword_Orders.entrySet()) {
            KeywordInfo keyInfo = mapEntry.getValue();
            if (keyInfo.eKeyId == keyId) {
                return keyInfo;
            }
        }
        return null;
    }

}
