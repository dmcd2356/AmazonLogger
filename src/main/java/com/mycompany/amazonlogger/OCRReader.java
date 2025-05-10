/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author dan
 */
public class OCRReader {

    private static final String CLASS_NAME = OCRReader.class.getSimpleName();
    
    private static Metadata metadata;
    private static String   content = "";
    
    public OCRReader() {
    }
    
    /**
     * returns the entire text of the last OCR scan.
     * 
     * @return the text from the last scan
     */
    public static String getContent () {
        return content;
    }
    
    /**
     * reads in a PDF image file with text and extracts text that is in it.
     * 
     * @param fname - name of PDF file to read (uses TestPath from Properties File)
     * 
     * @throws IOException
     * @throws TikaException
     * @throws SAXException
     * @throws ParserException 
     */
    public void run (String fname) throws IOException, TikaException, SAXException, ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // check for valid file
        String path = Utils.getDefaultPath(Utils.PathType.Test);
        fname = path + "/" + fname;
        File file = new File(fname);
        if (! file.isFile()) {
            throw new ParserException(functionId + "OCR file not found: " + fname);
        }
        
        content = "";
        Parser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(Integer.MAX_VALUE);

        TesseractOCRConfig config = new TesseractOCRConfig();
        // config.setTesseractPath(tPath);
        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(true);
        pdfConfig.setExtractUniqueInlineImagesOnly(false);

        ParseContext parseContext = new ParseContext();
        parseContext.set(TesseractOCRConfig.class, config);
        parseContext.set(PDFParserConfig.class, pdfConfig);
        //need to add this to make sure recursive parsing happens!
        parseContext.set(Parser.class, parser);

        FileInputStream stream = new FileInputStream(fname);
        metadata = new Metadata();
        parser.parse(stream, handler, metadata, parseContext);
        content = handler.toString();
        frame.outputInfoMsg (STATUS_PROGRAM, "Size of OCR scanned text: " + content.length());
    }

}
