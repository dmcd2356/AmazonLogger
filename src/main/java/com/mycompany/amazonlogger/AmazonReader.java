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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

public class AmazonReader {

    private static final String CLASS_NAME = AmazonReader.class.getSimpleName();
    
    // port to use if not found in PropertiesFile
    private static final int SERVER_PORT = 6000;

    // GLOBALS
    public  static UIFrame frame;
    public  static Keyword keyword;
    public  static PropertiesFile props;
    
    private static boolean bCompileOnly = false;
    private static OperatingMode opMode;
    private static String  scriptName = "";
    private static File    scriptFile = null;
    private static int     commandIndex = 0;
    private static ScriptExecute exec = null;
    private static boolean pause = false;
    
    private static ArrayList<CommandStruct> cmdList = null;
    
    private static TCPServerMain server;

    private enum OperatingMode {
        GUI,
        COMMAND_LINE,
        SCRIPT,
        NETWORK,
    }
    
    private static void setOpMode (OperatingMode mode) {
        opMode = mode;
    }
    
    private static void scriptInit () {
        frame.reset();          // reset the GUI settings
        FileIO.init();          // reset the File settings
        PreCompile.variables.resetVariables();  // reset all variable values back to default
        Spreadsheet.init();     // reset spreadsheet params
        OpenDoc.init();         // reset the OpenDoc params
        frame.elapsedTimerDisable();    // stop the timer for the timestamp
        frame.outputInfoMsg(STATUS_PROGRAM, "Resetting program index to begining");
        commandIndex = 0;       // reset the command pointer to the begining
    }
    
    public static boolean isOpModeGUI () {
        return opMode == OperatingMode.GUI;
    }
    
    public static boolean isOpModeNetwork () {
        return opMode == OperatingMode.NETWORK;
    }
    
    public static boolean isOpModeScript () {
        return opMode == OperatingMode.SCRIPT;
    }
    
    public static boolean isOpModeCommmandLine () {
        return opMode == OperatingMode.COMMAND_LINE;
    }
    
    public static boolean isRunModeCompileOnly () {
        return bCompileOnly;
    }
    
    public static String getScriptName() {
        return scriptName;
    }
    
    // Main driver method
    public static void main(String[] args) {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // check for arguments passed (non-GUI interface for testing):
        if (args.length == 0) {
            // GUI mode:
            setOpMode (OperatingMode.GUI);
            // create the user interface to control things
            frame = new UIFrame(true);
            props = new PropertiesFile();

            // enable the messages as they were from prevous run
            frame.setDefaultStatus ();
        } else {
            // command line version for testing
            frame = new UIFrame(false);
            props = new PropertiesFile();
            
            // set defaults from properties file
            frame.setDefaultStatus ();
            pause = false;
            bCompileOnly = false;
         
            // run the command line arguments
            try {
                String firstArg = args[0];
                switch (firstArg) {
                    case "-script" ->  {
                        setOpMode (OperatingMode.SCRIPT);
                        if (args.length != 2) {
                            throw new ParserException(functionId + "missing filename argument for option: " + firstArg);
                        }
                        String fname = args[1];
                        selectScriptFile(fname);
                        compileScript();
                        runScript();
                    }
                    case "-compile" -> {
                        bCompileOnly = true;
                        setOpMode (OperatingMode.SCRIPT);
                        if (args.length != 2) {
                            throw new ParserException(functionId + "missing filename argument for option: " + firstArg);
                        }
                        String fname = args[1];
                        selectScriptFile(fname);
                        compileScript();
                    }
                    case "-network" -> {
                        setOpMode (OperatingMode.NETWORK);
                        Integer port = props.getPropertiesItem(PropertiesFile.Property.Port, SERVER_PORT);
                        if (args.length == 2) {
                            port = Utils.getIntValue(args[1]).intValue();
                        }
                        server = new TCPServerMain (port);
                    }
                    default -> {
                        setOpMode (OperatingMode.COMMAND_LINE);
                        CmdOptions cmdLine = new CmdOptions();
                        cmdLine.runCommandLine(args);
                    }
                }
            } catch (ParserException | IOException | SAXException | TikaException ex) {
                frame.outputInfoMsg (STATUS_ERROR, ex.getMessage() + "\n  -> " + functionId);
                try {
                    ScriptExecute.exit();
                } catch (IOException exIO) {
                    frame.outputInfoMsg (STATUS_ERROR, exIO.getMessage() + "\n  -> " + functionId);
                }
                frame.closeTestFile();
            }
            
            // close the test output file
            frame.closeTestFile();
            if (isOpModeNetwork()) {
//                server.exit();
            }
        }
    }

    /**
     * pause / resume the script.
     * 
     * @param state - the pause/resume state to go to
     */
    public static void pauseScript (boolean state) {
        if (isOpModeNetwork()) {
            pause = state;
            if (pause) {
                frame.outputInfoMsg (STATUS_PROGRAM, "Script PAUSED");
            } else {
                frame.outputInfoMsg (STATUS_PROGRAM, "Script RESUMED");
            }
        }
    }

    /**
     * stop the script from running.
     * (Also resets the command index to beginning so we can re-run)
     */
    public static void stopScript () {
        if (isOpModeNetwork()) {
            pause = true;
            if (cmdList != null && ! cmdList.isEmpty()) {
                commandIndex = 0;
            }
            frame.outputInfoMsg (STATUS_PROGRAM, "Script STOPPED");
        }
    }

    /**
     * determine if the script has completed.
     * 
     * @return true if it completed
     */
    public static boolean isScriptCompleted() {
        return (commandIndex >= cmdList.size() || commandIndex < 0);
    }
    
    /**
     * selects the program to run
     * 
     * @param fname - the name of the script file to run
     * 
     * @throws ParserException
     */
    public static void selectScriptFile (String fname) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        // verify file name
        if (fname.charAt(0) != '/') {
            fname = Utils.getDefaultPath(Utils.PathType.Test) + "/" + fname;
        }
        scriptFile = new File(fname);
        fname = scriptFile.getAbsolutePath();
        if (! scriptFile.canRead()) {
            throw new ParserException(functionId + "Invalid file - no read access: " + fname);
        }
            
        frame.outputInfoMsg(STATUS_COMPILE, "Running from script: " + fname);
        
        // extract base name from script file (no path, no extension)
        scriptName = fname;
        int offset = scriptName.lastIndexOf('/');
        if (offset >= 0 && offset < scriptName.length()) {
            scriptName = scriptName.substring(offset + 1);
        }
        offset = scriptName.indexOf('.');
        if (offset > 0) {
            scriptName = scriptName.substring(0, offset);
        }
    }
    /**
     * compiles the currently selected script.
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException 
     */
    public static void compileScript () throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (scriptFile == null) {
            throw new ParserException(functionId + "No script file selected");
        }

        // enable timestamp on log messages
        frame.init();
        frame.elapsedTimerEnable();
        cmdList = null;
        int maxLines = 0;

        try {
            // do the Pre-compile operation
            frame.outputInfoMsg(STATUS_COMPILE, "===== BEGINING PROGRAM PRE-COMPILE =====");
            PreCompile preCompile = new PreCompile();
            Variables variables = preCompile.build(scriptFile);

            if (isOpModeNetwork()) {
                ArrayList<String> varAllocs = variables.getVarAlloc();
                TCPServerThread.sendAllocations(varAllocs);
            }
            
            // compile the program
            frame.outputInfoMsg(STATUS_COMPILE, "\"===== BEGINING PROGRAM COMPILE =====");
            ScriptCompile compiler = new ScriptCompile(variables);
            maxLines = compiler.getMaxLines();
            cmdList = compiler.build(scriptFile);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }

        frame.elapsedTimerDisable();
        exec = new ScriptExecute(scriptFile.getAbsolutePath(), maxLines);
        pause = false;
        scriptInit();
        VarReserved.putScriptNameValue(scriptName);
        VarReserved.putCurDirValue(FileIO.getCurrentFilePath());

        // indicate to subroutines we have completed compiling
        Subroutine.beginExecution();
    }
    
    /**
     * runs the currently compiled script.
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException 
     */
    public static void runScript () throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (cmdList == null || exec == null) {
            throw new ParserException(functionId + "No script file has been compiled");
        }
        // enable timestamp on log messages
        frame.elapsedTimerEnable();
        pause = false;

        try {
            // execute the program by running each 'cmdList' entry
            frame.outputInfoMsg(STATUS_PROGRAM, "===== BEGINING PROGRAM EXECUTION =====");
            while (commandIndex >= 0 && commandIndex < cmdList.size() && !pause) {
                commandIndex = exec.executeProgramCommand (commandIndex, cmdList.get(commandIndex));
            }
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }

        // if the user requested a pause, pause the timer
        if (pause) {
            frame.elapsedTimerPause();
        }

        // we have completed - if running from network, inform the client and stop the timer
        if (isScriptCompleted() && isOpModeNetwork()) {
            int lineNumber = ScriptCompile.getLineNumber(commandIndex);
            TCPServerThread.sendLineInfo (lineNumber);
            scriptInit();
        }
    }
    
    /**
     * runs the currently compiled script.
     * 
     * THIS IS ONLY EXECUTED WHEN RUNNING FROM NETWORK!
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException 
     */
    public static void runScriptStep () throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (cmdList == null || exec == null || commandIndex < 0 || commandIndex > cmdList.size()) {
            throw new ParserException(functionId + "No script file has been compiled");
        }

        if (commandIndex == 0) {
            frame.outputInfoMsg(STATUS_PROGRAM, "===== BEGINING PROGRAM EXECUTION =====");
        }

        // enable timestamp on log messages
        frame.elapsedTimerEnable();
        
        // get command to run
        CommandStruct command = cmdList.get(commandIndex);
        
        // run command instruction
        try {
            commandIndex = exec.executeProgramCommand (commandIndex, command);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }

        // pause the timer
        frame.elapsedTimerPause();

        // send the next line number to the cliend
        int lineNumber = ScriptCompile.getLineNumber(commandIndex);
        TCPServerThread.sendLineInfo (lineNumber);
        
        // reset ptr to begining if we reached the end of the script
        if (isScriptCompleted()) {
            scriptInit();
        }
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

