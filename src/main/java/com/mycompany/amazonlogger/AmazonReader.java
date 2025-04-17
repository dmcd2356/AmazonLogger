/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

// Importing java input/output classes
import static com.mycompany.amazonlogger.UIFrame.STATUS_ERROR;
import java.io.IOException;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

public class AmazonReader {

    private static final String CLASS_NAME = "AmazonReader";
    
    // GLOBALS
    public  static UIFrame frame;
    public  static Keyword keyword;
    public  static PropertiesFile props;


    // Main driver method
    public static void main(String[] args)
    {
        String functionId = CLASS_NAME + "main: ";

        // check for arguments passed (non-GUI interface for testing):
        if (args.length > 0) {
            // command line version for testing
            frame = new UIFrame(false);
            props = new PropertiesFile();
            
            // set defaults from properties file
            frame.setDefaultStatus ();
         
            // run the command line arguments
            try {
                if (args[0].contentEquals("-f")) {
                    ScriptCompile fileParser = new ScriptCompile();
                    if (args.length < 2) {
                        throw new ParserException(functionId + "missing filename argument for option: -f");
                    }
                    fileParser.runFromFile (args[1]);
                } else {
                    CmdOptions cmdLine = new CmdOptions();
                    cmdLine.runCommandLine(args);
                }
            } catch (ParserException | IOException | SAXException | TikaException ex) {
                frame.outputInfoMsg (STATUS_ERROR, ex.getMessage() + "\n  -> " + functionId);
                ScriptExecute exec = new ScriptExecute();
                try {
                    exec.close();
                } catch (IOException exIO) {
                    frame.outputInfoMsg (STATUS_ERROR, exIO.getMessage() + "\n  -> " + functionId);
                }
            }
            
            // close the test output file
            frame.closeTestFile();
        } else {
            // create the user interface to control things
            frame = new UIFrame(true);
            props = new PropertiesFile();

            // enable the messages as they were from prevous run
            frame.setDefaultStatus ();
        }
    }

}
    
class ParserException extends Exception
{
    // Parameterless Constructor
    public ParserException() {}

    // Constructor that accepts a message
    public ParserException(String message)
    {
        super(message);
    }

    // Constructor that accepts a message along with the line and its line number
    public ParserException(String message, String line)
    {
        super(message + line);
    }
}

