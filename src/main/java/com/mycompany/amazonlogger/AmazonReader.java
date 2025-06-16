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
    
    // GLOBALS
    public  static UIFrame frame;
    public  static Keyword keyword;
    public  static PropertiesFile props;
    
    private static RunMode opMode;
    private static String  scriptName = "";
    private static File    scriptFile = null;
    private static int     commandIndex = 0;
    private static ScriptExecute exec = null;
    private static boolean pause = false;
    
    private static ArrayList<CommandStruct> cmdList = null;
    
    private static TCPServerMain server;

    public enum RunMode {
        GUI,
        COMMAND_LINE,
        NETWORK,
        COMPILE_ONLY,
        COMPILE,
        EXECUTE,
    }

    private static void setRunMode (RunMode mode) {
        opMode = mode;
    }
    
    public static boolean isRunModeGUI () {
        return opMode == RunMode.GUI;
    }
    
    public static boolean isRunModeNetwork () {
        return opMode == RunMode.NETWORK;
    }
    
    public static boolean isRunModeScript () {
        return (opMode == RunMode.EXECUTE || opMode == RunMode.COMPILE || opMode == RunMode.COMPILE_ONLY);
    }
    
    public static boolean isRunModeCommmandLine () {
        return opMode == RunMode.COMMAND_LINE;
    }
    
    public static boolean isRunModeCompile () {
        return opMode == RunMode.COMPILE;
    }
    
    public static boolean isRunModeExecute () {
        return opMode == RunMode.EXECUTE;
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
        if (args.length == 0) {
            // GUI mode:
            setRunMode (RunMode.GUI);
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
         
            // run the command line arguments
            try {
                String firstArg = args[0];
                switch (firstArg) {
                    case "-script" ->  {
                        setRunMode (RunMode.COMPILE);
                        if (args.length != 2) {
                            throw new ParserException(functionId + "missing filename argument for option: " + firstArg);
                        }
                        String fname = args[1];
                        selectScriptFile(fname);
                        compileScript();
                        runScript();
                    }
                    case "-compile" -> {
                        setRunMode (RunMode.COMPILE_ONLY);
                        if (args.length != 2) {
                            throw new ParserException(functionId + "missing filename argument for option: " + firstArg);
                        }
                        String fname = args[1];
                        selectScriptFile(fname);
                        compileScript();
                    }
                    case "-network" -> {
                        setRunMode (RunMode.NETWORK);
                        int port = 5000; // default port selection
                        if (args.length == 2) {
                            port = Utils.getIntValue(args[1]).intValue();
                        }
                        server = new TCPServerMain (port);
                    }
                    default -> {
                        setRunMode (RunMode.COMMAND_LINE);
                        CmdOptions cmdLine = new CmdOptions();
                        cmdLine.runCommandLine(args);
                    }
                }
            } catch (ParserException | IOException | SAXException | TikaException ex) {
                frame.outputInfoMsg (STATUS_ERROR, ex.getMessage() + "\n  -> " + functionId);
                setRunMode (RunMode.EXECUTE);
                try {
                    ScriptExecute.exit();
                } catch (IOException exIO) {
                    frame.outputInfoMsg (STATUS_ERROR, exIO.getMessage() + "\n  -> " + functionId);
                }
                frame.closeTestFile();
            }
            
            // close the test output file
            frame.closeTestFile();
            if (isRunModeNetwork()) {
//                server.exit();
            }
        }
    }

    /**
     * pause / resume the script.
     */
    public static void pauseScript (boolean state) {
        if (isRunModeNetwork()) {
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
     * (Also resets the command index to begining so we can re-run)
     */
    public static void stopScript () {
        if (isRunModeNetwork()) {
            pause = true;
            if (cmdList != null && ! cmdList.isEmpty()) {
                commandIndex = 0;
            }
            frame.outputInfoMsg (STATUS_PROGRAM, "Script STOPPED");
        }
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
        frame.reset();
        frame.elapsedTimerEnable();
        cmdList = null;
        int maxLines = 0;

        try {
            // do the Pre-compile operation
            frame.outputInfoMsg(STATUS_COMPILE, "===== BEGINING PROGRAM PRE-COMPILE =====");
            PreCompile preCompile = new PreCompile();
            Variables variables = preCompile.build(scriptFile);

            if (isRunModeNetwork()) {
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
        commandIndex = 0;
        pause = false;
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

//        commandIndex = 0;
        try {
            // execute the program by running each 'cmdList' entry
            frame.outputInfoMsg(STATUS_PROGRAM, "===== BEGINING PROGRAM EXECUTION =====");
            setRunMode (RunMode.EXECUTE);
            while (commandIndex >= 0 && commandIndex < cmdList.size() && !pause) {
                commandIndex = exec.executeProgramCommand (commandIndex, cmdList.get(commandIndex));
            }
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }

        if (pause && commandIndex < cmdList.size()) {
            // if we haven't completed because we are paused, just pause the timer
            frame.elapsedTimerPause();
        } else {
            // otherwise, we have completed - inform the client and stop the timer
            TCPServerThread.sendLineInfo (commandIndex);
            frame.elapsedTimerDisable();
            frame.outputInfoMsg(STATUS_PROGRAM, "Resetting program index to begining");
            commandIndex = 0;
            frame.reset();
        }
    }
    
    /**
     * runs the currently compiled script.
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

        setRunMode (RunMode.EXECUTE);
        
        // get command to run
        CommandStruct command = cmdList.get(commandIndex);
        
        // run command instruction
        try {
            commandIndex = exec.executeProgramCommand (commandIndex, command);
            TCPServerThread.sendLineInfo (commandIndex);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        
        // reset ptr to begining if we reached the end of the script
        if (commandIndex >= cmdList.size()) {
            frame.outputInfoMsg(STATUS_PROGRAM, "Resetting program index to begining");
            commandIndex = 0;
            frame.reset();
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

