/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

// Importing java input/output classes
import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_COMPILE;
import static com.mycompany.amazonlogger.UIFrame.STATUS_ERROR;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

public class AmazonReader {

    private static final String CLASS_NAME = AmazonReader.class.getSimpleName();
    
    // GLOBALS
    public  static UIFrame frame;
    public  static Keyword keyword;
    public  static PropertiesFile props;
    
    private static RunMode opMode;
    private static String  scriptName = "";

    public enum RunMode {
        COMPILE_ONLY,
        COMPILE,
        EXECUTE,
    }

    private static void setRunMode (RunMode mode) {
        opMode = mode;
    }
    
    public static boolean isRunModeCompile () {
        return opMode == RunMode.COMPILE;
    }
    
    public static boolean isRunModeCompileOnly () {
        return opMode == RunMode.COMPILE_ONLY;
    }
    
    public static String getScriptName() {
        return scriptName;
    }
    
    // Main driver method
    public static void main(String[] args) {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // check for arguments passed (non-GUI interface for testing):
        if (args.length > 0) {
            // command line version for testing
            frame = new UIFrame(false);
            props = new PropertiesFile();
            
            // set defaults from properties file
            frame.setDefaultStatus ();
         
            // run the command line arguments
            try {
                if (args[0].contentEquals("-f") || args[0].contentEquals("-c")) {
                    if (args.length < 2) {
                        throw new ParserException(functionId + "missing filename argument for option: -f");
                    }
                    boolean bRunExec = args[0].contentEquals("-f");
                    String fname = args[1];
                    runFromFile (fname, bRunExec);
                } else {
                    CmdOptions cmdLine = new CmdOptions();
                    cmdLine.runCommandLine(args);
                }
            } catch (ParserException | IOException | SAXException | TikaException ex) {
                frame.outputInfoMsg (STATUS_ERROR, ex.getMessage() + "\n  -> " + functionId);
                setRunMode (RunMode.EXECUTE);
                try {
                    ScriptExecute.exit();
                } catch (IOException exIO) {
                    frame.outputInfoMsg (STATUS_ERROR, exIO.getMessage() + "\n  -> " + functionId);
                }
            }
            
            // close the test output file
            frame.closeTestFile();
        } else {
            setRunMode (RunMode.EXECUTE);
            // create the user interface to control things
            frame = new UIFrame(true);
            props = new PropertiesFile();

            // enable the messages as they were from prevous run
            frame.setDefaultStatus ();
        }
    }
    
    /**
     * runs the program from command line input
     * 
     * @param fname - the name of the script file to run
     * @param bRunExec - TRUE if execute script after compile, FALSE if compile only
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException 
     */    
    /**
     * runs the program from command line input
     * 
     * @param fname - the name of the script file to run
     * @param bRunExec - TRUE if execute script after compile, FALSE if compile only
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException 
     */
    private static void runFromFile (String fname, boolean bRunExec) throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        frame.outputInfoMsg(STATUS_COMPILE, "Running from script: " + fname);
        scriptName = fname;
        int offset = scriptName.lastIndexOf('/');
        if (offset >= 0 && offset < scriptName.length()) {
            scriptName = scriptName.substring(offset + 1);
        }
        offset = scriptName.indexOf('.');
        if (offset > 0) {
            scriptName = scriptName.substring(0, offset);
        }

        // enable timestamp on log messages
        frame.elapsedTimerEnable();

        try {
            // if we are only doing a compile, set flag so we don't terminate till the end
            if (bRunExec) {
                setRunMode (RunMode.COMPILE);
            } else {
                setRunMode (RunMode.COMPILE_ONLY);
            }

            // do the Pre-compile operation
            frame.outputInfoMsg(STATUS_COMPILE, "===== BEGINING PROGRAM PRE-COMPILE =====");
            PreCompile preCompile = new PreCompile();
            Variables variables = preCompile.build(fname);
            
            // compile the program
            frame.outputInfoMsg(STATUS_COMPILE, "\"===== BEGINING PROGRAM COMPILE =====");
            ScriptCompile compiler = new ScriptCompile(variables);
            ArrayList<CommandStruct> cmdList = compiler.build(fname);

            if (bRunExec) {
                // execute the program by running each 'cmdList' entry
                frame.outputInfoMsg(STATUS_PROGRAM, "===== BEGINING PROGRAM EXECUTION =====");
                setRunMode (RunMode.EXECUTE);
                ScriptExecute exec = new ScriptExecute();
                int cmdIx = 0;
                while (cmdIx >= 0 && cmdIx < cmdList.size()) {
                    cmdIx = exec.executeProgramCommand (cmdIx, cmdList.get(cmdIx));
                }
            }
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        frame.elapsedTimerDisable();
    }

}

class ParserException extends Exception {
    
    // Parameterless Constructor
    public ParserException() {}

    // Constructor that accepts a message
    public ParserException(String message)
    {
        super(message);
    }
    
}

