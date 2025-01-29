package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;

/**
 *
 * @author dan
 */
public class Keyword {
    
    public enum KeyTyp { NONE, HELLO_D, HELLO_C, ORDER_PLACED, DETAILS, INVOICE, 
                         TOTAL_COST, ORDER_NUMBER, REFUNDED, DELIVERED, ARRIVING, NOW_ARRIVING,
                         VENDOR_RATING, DESCRIPTION, DESCRIPTION_2, COMPLETE, GROSS_COST,
                         TAXES, SHIPPING, ITEM_COST, SELLER, QUANTITY, END_OF_RECORD };

    public enum DataTyp { NONE, INLINE, NEXTLINE };
    public enum ClipTyp { NONE, ORDERS, DETAILS, INVOICE };
    
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
    
    public class KeywordEntry {
        String  strKeyword;     // the keyword string
        KeyTyp  eKeyId;         // keyword id
        DataTyp eDataType;      // where to find associated data
        
        public KeywordEntry (String keyword, KeyTyp id, DataTyp data) {
            strKeyword = keyword;
            eKeyId = id;
            eDataType  = data;
        }
        
        public KeywordEntry () {
            strKeyword = "";
            eKeyId = KeyTyp.NONE;
            eDataType  = DataTyp.NONE;
        }
    }
    
    private final KeywordClipEntry [] KeywordTable_None = {
        new KeywordClipEntry ("Hello, Dan"         , KeyTyp.HELLO_D     , ClipTyp.NONE),
        new KeywordClipEntry ("Hello, Connie"      , KeyTyp.HELLO_C     , ClipTyp.NONE),
        new KeywordClipEntry ("Order placed"       , KeyTyp.ORDER_PLACED, ClipTyp.ORDERS),
        new KeywordClipEntry ("Order Details"      , KeyTyp.DETAILS     , ClipTyp.DETAILS),
        new KeywordClipEntry ("Details for Order #", KeyTyp.INVOICE     , ClipTyp.INVOICE),
    };
    
    private final KeywordEntry [] KeywordTable_Orders = {
        new KeywordEntry ("Order placed"           , KeyTyp.ORDER_PLACED   , DataTyp.NEXTLINE),
        new KeywordEntry ("Total"                  , KeyTyp.TOTAL_COST     , DataTyp.NEXTLINE),
        new KeywordEntry ("Order #"                , KeyTyp.ORDER_NUMBER   , DataTyp.INLINE),
        new KeywordEntry ("Returned"               , KeyTyp.REFUNDED       , DataTyp.NONE),
        new KeywordEntry ("Return started"         , KeyTyp.REFUNDED       , DataTyp.NONE),
        new KeywordEntry ("Return complete"        , KeyTyp.REFUNDED       , DataTyp.NONE),
        new KeywordEntry ("Refunded"               , KeyTyp.REFUNDED       , DataTyp.NONE),
        new KeywordEntry ("Item refunded"          , KeyTyp.REFUNDED       , DataTyp.NONE),
        new KeywordEntry ("Delivered"              , KeyTyp.DELIVERED      , DataTyp.INLINE),
        new KeywordEntry ("Arriving"               , KeyTyp.ARRIVING       , DataTyp.INLINE),
        new KeywordEntry ("Now arriving"           , KeyTyp.NOW_ARRIVING   , DataTyp.INLINE),
        new KeywordEntry ("A"                      , KeyTyp.VENDOR_RATING  , DataTyp.NONE),
        new KeywordEntry ("B"                      , KeyTyp.VENDOR_RATING  , DataTyp.NONE),
        new KeywordEntry ("C"                      , KeyTyp.VENDOR_RATING  , DataTyp.NONE),
        new KeywordEntry ("D"                      , KeyTyp.VENDOR_RATING  , DataTyp.NONE),
        new KeywordEntry ("F"                      , KeyTyp.VENDOR_RATING  , DataTyp.NONE),
        new KeywordEntry ("?"                      , KeyTyp.VENDOR_RATING  , DataTyp.NONE),
        new KeywordEntry ("←Previous"              , KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Add to cart"            , KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Bargain recommendations", KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Consider these items"   , KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Next set of slides"     , KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Recommended based on"   , KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Recommended for you"    , KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Sponsored"              , KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Related to items you've viewedSee more", KeyTyp.END_OF_RECORD  , DataTyp.NONE),
    };
    
    private final KeywordEntry [] KeywordTable_Details = {
        new KeywordEntry ("Ordered on"                    , KeyTyp.ORDER_PLACED   , DataTyp.INLINE),
        new KeywordEntry ("Total before tax:"             , KeyTyp.GROSS_COST     , DataTyp.NEXTLINE),
        new KeywordEntry ("Estimated tax to be collected:", KeyTyp.TAXES          , DataTyp.NEXTLINE),
        new KeywordEntry ("Grand Total:"            , KeyTyp.TOTAL_COST     , DataTyp.NEXTLINE),
        new KeywordEntry ("Sold by:"                , KeyTyp.SELLER         , DataTyp.INLINE),
        new KeywordEntry ("$"                       , KeyTyp.ITEM_COST      , DataTyp.INLINE),
        new KeywordEntry ("Returned"                , KeyTyp.REFUNDED       , DataTyp.NONE),
        new KeywordEntry ("Return started"          , KeyTyp.REFUNDED       , DataTyp.NONE),
        new KeywordEntry ("Return complete"         , KeyTyp.REFUNDED       , DataTyp.NONE),
        new KeywordEntry ("Refunded"                , KeyTyp.REFUNDED       , DataTyp.NONE),
        new KeywordEntry ("Item refunded"           , KeyTyp.REFUNDED       , DataTyp.NONE),
        new KeywordEntry ("Delivered"               , KeyTyp.DELIVERED      , DataTyp.INLINE),
        new KeywordEntry ("Arriving"                , KeyTyp.ARRIVING       , DataTyp.INLINE),
        new KeywordEntry ("Now arriving"            , KeyTyp.NOW_ARRIVING   , DataTyp.INLINE),
        new KeywordEntry ("A"                       , KeyTyp.VENDOR_RATING  , DataTyp.NONE),
        new KeywordEntry ("B"                       , KeyTyp.VENDOR_RATING  , DataTyp.NONE),
        new KeywordEntry ("C"                       , KeyTyp.VENDOR_RATING  , DataTyp.NONE),
        new KeywordEntry ("D"                       , KeyTyp.VENDOR_RATING  , DataTyp.NONE),
        new KeywordEntry ("F"                       , KeyTyp.VENDOR_RATING  , DataTyp.NONE),
        new KeywordEntry ("?"                       , KeyTyp.VENDOR_RATING  , DataTyp.NONE),
        new KeywordEntry ("←Previous"               , KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Bargain recommendations" , KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Consider these items"    , KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Next set of slides"      , KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Recommended based on"    , KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Recommended for you"     , KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Sponsored"               , KeyTyp.END_OF_RECORD  , DataTyp.NONE),
        new KeywordEntry ("Related to items you've viewedSee more", KeyTyp.END_OF_RECORD  , DataTyp.NONE),
    };

    private final KeywordEntry [] KeywordTable_Invoice = {
    };

    /**
     * finds if the current line is one of the keywords to search for before the type of
     *   clipboard file has been determined.
     * 
     * @param line - the line read from the web page
     * 
     * @return the structure indicating the type of keyword that was found
     *         (a dummy entry is returned indicating NONE if key was not found)
     */
    public KeywordClipEntry getKeywordClip (String line) {
        int linelen = line.length();
        for (KeywordClipEntry KeywordTable1 : KeywordTable_None) {
            if (line.startsWith(KeywordTable1.strKeyword)) {
                int keylen = KeywordTable1.strKeyword.length();
                // length must match keyword length, or have a space following it
                if (keylen == linelen || (linelen > keylen && line.charAt(keylen) == ' ')) {
                    frame.outputInfoMsg (UIFrame.STATUS_DEBUG,
                                            "ClipTyp." + KeywordTable1.eClipType.name() +
                                          " : KeyTyp." + KeywordTable1.eKeyId.name() +
                                          " : " + line);
                    return KeywordTable1;
                }
            }
        }
        return new KeywordClipEntry();
    }
    
    /**
     * finds if the current line is one of the keywords to search for after the type of
     *   clipboard file has been determined.
     * 
     * @param line      - the line read from the web page
     * @param eClipType - the type of clipboard file that we have determined it to be
     * 
     * @return the structure indicating the type of keyword that was found
     *         (a dummy entry is returned indicating NONE if key was not found)
     */
    public KeywordEntry getKeyword (String line, ClipTyp eClipType) {
        int linelen = line.length();
        switch (eClipType) {
            default:
            case ClipTyp.ORDERS:
                for (KeywordEntry KeywordTable1 : KeywordTable_Orders) {
                    if (line.startsWith(KeywordTable1.strKeyword)) {
                        int keylen = KeywordTable1.strKeyword.length();
                        // length must match keyword length, or the data type must have data following it
                        if (keylen == linelen || KeywordTable1.eKeyId == KeyTyp.END_OF_RECORD || KeywordTable1.eDataType == DataTyp.INLINE) {
                            frame.outputInfoMsg (UIFrame.STATUS_DEBUG,
                                                "ClipTyp." + eClipType.name() +
                                             " : KeyTyp."  + KeywordTable1.eKeyId.name() +
                                             " : DataTyp." + KeywordTable1.eDataType.name() +
                                             " : " + line);
                            return KeywordTable1;
                        }
                    }
                }
                break;
            case ClipTyp.DETAILS:
                for (KeywordEntry KeywordTable1 : KeywordTable_Details) {
                    if (line.startsWith(KeywordTable1.strKeyword)) {
                        int keylen = KeywordTable1.strKeyword.length();
                        // length must match keyword length, or the data type must have data following it
                        if (keylen == linelen || KeywordTable1.eKeyId == KeyTyp.END_OF_RECORD || KeywordTable1.eDataType == DataTyp.INLINE) {
                            frame.outputInfoMsg (UIFrame.STATUS_DEBUG,
                                                "ClipTyp." + eClipType.name() +
                                             " : KeyTyp."  + KeywordTable1.eKeyId.name() +
                                             " : DataTyp." + KeywordTable1.eDataType.name() +
                                             " : " + line);
                            return KeywordTable1;
                        } else {
                            frame.outputInfoMsg (UIFrame.STATUS_DEBUG,"   KeyTyp."  + KeywordTable1.eKeyId.name() + " -> Not full match");
                        }
                    }
                }
                break;
            case ClipTyp.INVOICE:
                for (KeywordEntry KeywordTable1 : KeywordTable_Invoice) {
                    if (line.startsWith(KeywordTable1.strKeyword)) {
                        int keylen = KeywordTable1.strKeyword.length();
                        // length must match keyword length, or the data type must have data following it
                        if (keylen == linelen || KeywordTable1.eKeyId == KeyTyp.END_OF_RECORD || KeywordTable1.eDataType == DataTyp.INLINE) {
                            frame.outputInfoMsg (UIFrame.STATUS_DEBUG,
                                                "ClipTyp." + eClipType.name() +
                                             " : KeyTyp."  + KeywordTable1.eKeyId.name() +
                                             " : DataTyp." + KeywordTable1.eDataType.name() +
                                             " : " + line);
                            return KeywordTable1;
                        }
                    }
                }
                break;
        }
        return new KeywordEntry();
    }

    /**
     * makes a Keyword structure entity that indicates NONE.
     * 
     * @return the created Keyword entity
     */
    public KeywordEntry makeKeyword() {
        return new KeywordEntry();
    }
    
    /**
     * makes a Keyword structure entity that includes the specified data.
     * 
     * @param keyword - the Keyword string value
     * @param id      - corresponding Keyword type
     * @param data    - the data type for the Keyword
     * 
     * @return the created Keyword entity
     */
    public KeywordEntry makeKeyword(String keyword, KeyTyp id, DataTyp data) {
        return new KeywordEntry(keyword, id, data);
    }
    
    /**
     * finds the Keyword entry in the ORDERS table that has the specified Key type
     * 
     * @param keyId - the Key type to find
     * 
     * @return the corresponding Keyword entry
     */
    public KeywordEntry findOrdersKey (KeyTyp keyId) {
        for (KeywordEntry KeywordTable1 : KeywordTable_Orders) {
            if (KeywordTable1.eKeyId == keyId) {
                return KeywordTable1;
            }
        }
        return new KeywordEntry();
    }

    /**
     * finds the Keyword entry in the DETAILS table that has the specified Key type
     * 
     * @param keyId - the Key type to find
     * 
     * @return the corresponding Keyword entry
     */
    public KeywordEntry findDetailsKey (KeyTyp keyId) {
        for (KeywordEntry KeywordTable1 : KeywordTable_Details) {
            if (KeywordTable1.eKeyId == keyId) {
                return KeywordTable1;
            }
        }
        return new KeywordEntry();
    }

    /**
     * finds the Keyword entry in the INVOICE table that has the specified Key type
     * 
     * @param keyId - the Key type to find
     * 
     * @return the corresponding Keyword entry
     */
    public KeywordEntry findInvoiceKey (KeyTyp keyId) {
        for (KeywordEntry KeywordTable1 : KeywordTable_Invoice) {
            if (KeywordTable1.eKeyId == keyId) {
                return KeywordTable1;
            }
        }
        return new KeywordEntry();
    }

}
