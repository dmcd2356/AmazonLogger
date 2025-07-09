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

    public enum KeyTyp { NONE, HELLO_D, HELLO_C, ORDER_PLACED, ORDER_DETAILS, 
                         TOTAL_COST, ORDER_NUMBER, RETURNED, REFUNDED, DELIVERED, ARRIVING, NOW_ARRIVING,
                         VENDOR_RATING, PACKAGE_LEFT, DESCRIPTION, DESCRIPTION_2, COMPLETE, GROSS_COST,
                         TAXES, SHIPPING, ITEM_COST, SELLER, QUANTITY, END_OF_RECORD };

    public enum DataTyp { NONE, INLINE, NEXTLINE };
    public enum ClipTyp { NONE, ORDERS, INVOICE };
    
    public class KeywordClipEntry {
        String  strKeyword;     // the keyword string
        KeyTyp  eKeyId;         // keyword id
        ClipTyp eClipType;      // indicates the clipboard file type
        
        public KeywordClipEntry (String keyword, KeyTyp id, ClipTyp type) {
            strKeyword = keyword;
            eKeyId = id;
            eClipType  = type;
        }
        
        public KeywordClipEntry () {
            strKeyword = "";
            eKeyId = KeyTyp.NONE;
            eClipType  = ClipTyp.NONE;
        }
    }
    
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
        putKeywordInfo ("Order placed"              , KeyTyp.ORDER_PLACED);
        putKeywordInfo ("Total"                     , KeyTyp.TOTAL_COST);
        putKeywordInfo ("Order #"                   , KeyTyp.ORDER_NUMBER);
        putKeywordInfo ("Returned"                  , KeyTyp.RETURNED);
        putKeywordInfo ("Return started"            , KeyTyp.RETURNED);
        putKeywordInfo ("Return complete"           , KeyTyp.RETURNED);
        putKeywordInfo ("Refunded"                  , KeyTyp.REFUNDED);
        putKeywordInfo ("Item refunded"             , KeyTyp.REFUNDED);
        putKeywordInfo ("Delivered"                 , KeyTyp.DELIVERED);
        putKeywordInfo ("Arriving"                  , KeyTyp.ARRIVING);
        putKeywordInfo ("Now arriving"              , KeyTyp.NOW_ARRIVING);
        putKeywordInfo ("Your package was left"     , KeyTyp.PACKAGE_LEFT);
        putKeywordInfo ("Package was left"          , KeyTyp.PACKAGE_LEFT);
        putKeywordInfo ("Grand Total:"              , KeyTyp.TOTAL_COST);
        putKeywordInfo ("Sold by:"                  , KeyTyp.SELLER);
        putKeywordInfo ("$"                         , KeyTyp.ITEM_COST);
        putKeywordInfo ("Total before tax:"         , KeyTyp.GROSS_COST);
        putKeywordInfo ("Estimated tax to be collected:", KeyTyp.TAXES);
        
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
    
    private final KeywordClipEntry [] KeywordTable_None = {
        new KeywordClipEntry ("Hello, Dan"         , KeyTyp.HELLO_D      , ClipTyp.NONE),
        new KeywordClipEntry ("Hello, Connie"      , KeyTyp.HELLO_C      , ClipTyp.NONE),
        new KeywordClipEntry ("Order placed"       , KeyTyp.ORDER_PLACED , ClipTyp.ORDERS),
        new KeywordClipEntry ("Order Details"      , KeyTyp.ORDER_DETAILS, ClipTyp.INVOICE),
    };

    public static DataTyp getDataType (ClipTyp clipType, KeyTyp keyType) {
        if (clipType == ClipTyp.ORDERS) {
            switch (keyType) {
                case ORDER_NUMBER:
                case DELIVERED:
                case ARRIVING:
                case NOW_ARRIVING:
                    return Keyword.DataTyp.INLINE;
                case ORDER_PLACED:
                case TOTAL_COST:
                    return Keyword.DataTyp.NEXTLINE;
                default:
                    break;
            }
        } else {
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
                default:
                    break;
            }
        }
        return Keyword.DataTyp.NONE;
    }
    
    /**
     * finds if the current line is one of the keywords to search for before the type of
     *   clipboard file has been determined.
     * 
     * @param line - the line read from the web page
     * 
     * @return the structure indicating the type of keyword that was found
     *         (a dummy entry is returned indicating NONE if key was not found)
     */
    public KeywordClipEntry getKeywordInit (String line) {
        int linelen = line.length();
        for (KeywordClipEntry KeywordTable1 : KeywordTable_None) {
            if (line.startsWith(KeywordTable1.strKeyword)) {
                int keylen = KeywordTable1.strKeyword.length();
                // length must match keyword length, or have a space following it
                if (keylen == linelen || (linelen > keylen && line.charAt(keylen) == ' ')) {
                    GUILogPanel.outputInfoMsg (MsgType.DEBUG,
                                            "ClipTyp." + KeywordTable1.eClipType.name() +
                                          " : KeyTyp." + KeywordTable1.eKeyId.name() +
                                          " : " + line);
                    return KeywordTable1;
                }
            }
        }
        String shortLine = line.substring(0, (line.length() > 20 ? 20 : line.length()));
        GUILogPanel.outputInfoMsg (MsgType.DEBUG,"ClipTyp.NONT : NO MATCH: "  + shortLine);
        return new KeywordClipEntry();
    }
    
    /**
     * finds if the current line is one of the keywords to search for after the type of
     *   clipboard file has been determined.
     * 
     * @param clipType - ORDER or INVOICE
     * @param line     - the line read from the web page
     * 
     * @return the structure indicating the type of keyword found, null if not found
     */
    public static KeywordInfo getKeyword (ClipTyp clipType, String line) {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        for (HashMap.Entry<String, KeywordInfo> mapEntry : Keyword_Orders.entrySet()) {
            String keyStr = mapEntry.getKey();
            KeywordInfo keyInfo = mapEntry.getValue();
            DataTyp dataType = getDataType (clipType, keyInfo.eKeyId);

            // END_OF_RECORD entries are also partial string matches
            if (dataType != DataTyp.INLINE && keyInfo.eKeyId != KeyTyp.END_OF_RECORD) {
                if (line.contentEquals(keyStr)) {
                        GUILogPanel.outputInfoMsg (MsgType.DEBUG, "EXACT : KeyTyp." + keyInfo.eKeyId +
                                                        " : Length." + keyInfo.keyLength + " : " + line);
                        return keyInfo;
                }
            } else {
                if (line.startsWith(keyStr)) {
                        GUILogPanel.outputInfoMsg (MsgType.DEBUG, "PARTIAL : KeyTyp." + keyInfo.eKeyId +
                                                        " : Length." + keyInfo.keyLength + " : " + line);
                        return keyInfo;
                }
            }
        }
        String shortLine = line.substring(0, (line.length() > 20 ? 20 : line.length()));
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
