/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

// Importing java input/output classes
import com.mycompany.amazonlogger.GUILogPanel.MsgType;
import java.io.File;
import java.io.IOException;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

public class AmazonReader {

    private static final String CLASS_NAME = AmazonReader.class.getSimpleName();
    
    // GLOBALS
    public  static Keyword keyword;
    public  static PropertiesFile props;
    
    private static GUIMain frame;
    private static boolean bCompileOnly = false;
    private static OperatingMode opMode;
    private static String  scriptName = "";
    private static File    scriptFile = null;
    private static int     commandIndex = 0;
    private static ScriptExecute exec = null;
    
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
        GUILogPanel.reset();    // reset the GUI settings
        FileIO.init();          // reset the File settings
        PreCompile.variables.resetVariables();  // reset all variable values back to default
        Spreadsheet.init();     // reset spreadsheet params
        OpenDoc.init();         // reset the OpenDoc params
        GUIMain.elapsedTimerDisable();    // stop the timer for the timestamp
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "Resetting program index to begining");
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
            frame = new GUIMain(true);
            props = new PropertiesFile();

            // enable the messages as they were from prevous run
            frame.setDefaultStatus ();
        } else {
            // command line version for testing
            frame = new GUIMain(false);
            props = new PropertiesFile();
            
            // set defaults from properties file
            frame.setDefaultStatus ();
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
                        Integer port = null;
                        if (args.length == 2) {
                            port = Utils.getIntValue(args[1]).intValue();
                        }
                        // start the network server
                        server = new TCPServerMain (port);
                        ScriptThread.enableRun();
                    }
                    default -> {
                        setOpMode (OperatingMode.COMMAND_LINE);
                        CmdOptions cmdLine = new CmdOptions();
                        cmdLine.runCommandLine(args);
                    }
                }
            } catch (ParserException | IOException | SAXException | TikaException ex) {
                GUILogPanel.outputInfoMsg (MsgType.ERROR, ex.getMessage());
                try {
                    ScriptExecute.exit();
                } catch (IOException exIO) {
                    GUILogPanel.outputInfoMsg (MsgType.ERROR, exIO.getMessage());
                }
                GUILogPanel.closeTestFile();
            }
            
            // close the test output file
            GUILogPanel.closeTestFile();
            if (isOpModeNetwork()) {
//                server.exit();
            }
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
            TCPServerThread.sendStatus("ERROR: LOAD");
            throw new ParserException(functionId + "Invalid file - no read access: " + fname);
        }
            
        GUILogPanel.outputInfoMsg(MsgType.COMPILE, "Running from script: " + fname);
        
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
        
        if (isOpModeNetwork()) {
            TCPServerThread.sendStatus("LOADED");
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
        GUIMain.elapsedTimerEnable();
        ScriptThread.initBreakpoint();

        try {
            // do the Pre-compile operation
            GUILogPanel.outputInfoMsg(MsgType.COMPILE, "===== BEGINING PROGRAM PRE-COMPILE =====");
            PreCompile preCompile = new PreCompile();
            Variables variables = preCompile.build(scriptFile);

            // compile the program
            GUILogPanel.outputInfoMsg(MsgType.COMPILE, "\"===== BEGINING PROGRAM COMPILE =====");
            ScriptCompile compiler = new ScriptCompile(variables);
            compiler.build(scriptFile);

            if (isOpModeNetwork()) {
                variables.sendVarAlloc();
            }
        } catch (ParserException exMsg) {
            TCPServerThread.sendStatus("ERROR: COMPILE");
            throw new ParserException(exMsg.getMessage());
        }

        GUIMain.elapsedTimerDisable();
        VarReserved.putScriptNameValue(scriptName);
        VarReserved.putCurDirValue(FileIO.getCurrentFilePath());

        if (isOpModeNetwork()) {
            ScriptThread.compilerComplete();
            TCPServerThread.sendStatus("COMPILED");
        } else {
            exec = new ScriptExecute(scriptFile.getAbsolutePath(), ScriptCompile.getMaxLines());
            scriptInit();
        }
        
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
        
        int compileSize = ScriptCompile.getCompiledSize();
        if (compileSize <= 0 || exec == null) {
            throw new ParserException(functionId + "No script file has been compiled");
        }
        // enable timestamp on log messages
        GUIMain.elapsedTimerEnable();

        try {
            // execute the program by running each 'cmdList' entry
            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "===== BEGINING PROGRAM EXECUTION =====");
            while (commandIndex >= 0 && commandIndex < compileSize) {
                commandIndex = exec.executeProgramCommand (commandIndex, ScriptCompile.getExecCommand(commandIndex));
            }
        } catch (ParserException exMsg) {
            Utils.throwAddendum (exMsg.getMessage(), functionId);
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

