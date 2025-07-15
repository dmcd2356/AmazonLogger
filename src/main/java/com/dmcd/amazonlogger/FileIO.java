/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import com.dmcd.amazonlogger.GUILogPanel.MsgType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author dan
 */
public class FileIO {
    
    private static final String CLASS_NAME = FileIO.class.getSimpleName();
    private static final String INDENT = "       ";
    
    // the map of column names to column indices in the sheet
    // for now, we only allow 1 file open for reading and 1 file open for writing at a time,
    //  and the same file can't be opened for both read and write.
    private static BufferedReader fileReader = null;
    private static PrintWriter    fileWriter = null;
    private static String fileReadName = "";
    private static String fileWriteName = "";
    private static String baseDir = System.getProperty("user.dir");
    private static String fileDir = System.getProperty("user.dir");


    /**
     * initializes all the static parameters
     */
    public static void init() {
        baseDir = System.getProperty("user.dir");
        fileDir = System.getProperty("user.dir");
        try {
            exit();
        } catch (IOException exMsg) {
            GUILogPanel.outputInfoMsg(MsgType.ERROR, "Error on closing File Reader/Writers");
        }
    }
    
    /**
     * tests whether the selected path is within the base path selection.
     * 
     * @param path - the absolute path to test
     * 
     * @return true if valid path
     */
    public static boolean isWithinTestPath (String path) throws ParserException {
        int baseLen = baseDir.length();
        if (path.length() < baseLen) {
            return false;
        }
        return path.substring(0, baseLen).contentEquals(baseDir);
    }

    /**
     * backs up to previous path in the chain.
     * 
     * This is called when a ".." entry is found in the directory path, which
     *  indicates to backup to the directory above this one. Note that is will not
     *  go beyond the user home directory.
     * 
     * @param curpath - the current path
     * 
     * @return the path above the current one
     * 
     * @throws ParserException
     */
    private static String backupPath (String curpath) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // if there is a '/' char at end of path, remove it
        if (curpath.charAt(curpath.length()-1) == '/') {
            curpath = curpath.substring(0, curpath.length()-1);
        }
        // first make sure we are in the user base path
        if (! curpath.startsWith(baseDir)) {
            throw new ParserException(functionId + "Path is not within test base path: " + curpath);
        }
        int offset = curpath.lastIndexOf('/');
        if (offset <= 0) {
            throw new ParserException(functionId + "Path is invalid: " + curpath);
        }
        curpath = curpath.substring(0, offset);
        
        // make sure we are within bounds
        if (! isWithinTestPath(curpath)) {
            throw new ParserException(functionId + "Path is not within test base path: " + curpath);
        }
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "Backed up to directory above: " + curpath);
        return curpath;
    }

    /**    
     * converts the specified path into an absolute path.
     * 
     * @param path - the path (relative, absolute, relative to home or current dir)
     * 
     * @return the corresponding absolute path name
     * 
     * @throws ParserException 
     */
    public static String getAbsPath (String path) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (path.startsWith("..")) {
            String newpath = fileDir;
            while (! path.isEmpty() && path.startsWith("..")) {
                newpath = backupPath (newpath);
                path = path.substring(2);
                if (! path.isEmpty()) {
                    if (path.charAt(0) != '/') {
                        throw new ParserException(functionId + "Invalid path format following .. : " + path);
                    }
                    path = path.substring(1);
                }
            }
            if (! path.isEmpty()) {
                newpath = newpath + "/" + path;
            }
            path = newpath;
        } else if (path.contentEquals(".")) {
            // the '.' indicates the current directory
            path = fileDir;
        } else if (path.startsWith("./")) {
            // './' indicates relative to current directory
            path = fileDir + path.substring(1);
        } else if (path.startsWith("~")) {
            // a leading '~' refers to the base Test path
            if (path.length() > 1) {
                path = System.getProperty("user.home") + path.substring(1);
            } else {
                path = System.getProperty("user.home");
            }
        } else if (! path.startsWith("/")) {
            // otherwise it is relative to currently selected file path
            path = fileDir + "/" + path;
        }
        return path;
    }
    
    /**
     * sets the base Test path for all file write accesses to be contained within.
     * This is only called by PreCompile to initialize the base path we will use.
     * 
     * @param path - the path to set as the FileIO path
     * 
     * @throws ParserException 
     */    
    public static void setBaseTestPath(String path) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        path = getAbsPath(path);
        if (! isWithinTestPath(path)) {
            throw new ParserException(functionId + "Path is not within test base path: " + path);
        }

        baseDir = path;
        fileDir = path;
        GUILogPanel.outputInfoMsg(MsgType.DEBUG, "File IO base path set to: " + path);
    }
    
    /**
     * returns the current selected directory path for FileIO.
     * 
     * @return the current directory path
     */    
    public static String getCurrentFilePath() {
        return fileDir;
    }
    
    /**
     * cleanup upon exit.
     * closes the file readers and writers.
     * 
     * @throws IOException 
     */
    public static void exit() throws IOException {
        if (fileReader != null) {
            fileReader.close();
            fileReader = null;
            GUILogPanel.outputInfoMsg(MsgType.DEBUG, "File reader closed: " + fileReadName);
            fileReadName = "";
        }
        if (fileWriter != null) {
            fileWriter.flush();
            fileWriter.close();
            fileWriter = null;
            GUILogPanel.outputInfoMsg(MsgType.DEBUG, "File writer flushed & closed: " + fileWriteName);
            fileWriteName = "";
        }
    }

    /**
     * puts the file/directory names from the specified path into the $RESPONSE param.
     * 
     * @param path - the path to check
     * @param type - -f for files only, -d for directories only, blank for files and directories
     * 
     * @throws ParserException
     */
    public static void getDirFileList (String path, String type) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        java.io.File file = getFilePath(path);
        if (! file.isDirectory()) {
            throw new ParserException(functionId + "Invalid directory selection: " + file.getAbsolutePath());
        }
        java.io.File[] list = file.listFiles();
        for (java.io.File list1 : list) {
            if ((type.isEmpty() ||
                (type.contentEquals("-d")) && list1.isDirectory()) ||
                (type.contentEquals("-f")) && list1.isFile())    {
                VarReserved.putResponseValue(list1.getName());
            }
        }
    }

    /**
     * gets the file or directory specified by the given path.
     * 
     * A single '.' will represent the current directory and '~' indicates
     *  the root directory for testing: the Test default directory.
     *  If the path does not start with a '/' char, it is a relative path.
     *  Absolute paths are only supported if it resides within the User path bounds.
     * 
     * @param path - the path of the dir or file
     * 
     * @return a file of the path specified
     * 
     * @throws ParserException
     */
    public static java.io.File getFilePath (String path) throws ParserException {
        path = getAbsPath(path);
        java.io.File file = new java.io.File(path);
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "    Path selection: " + path);
        return (file);
    }
    
    /**
     * gets the file or directory specified by the given path.
     * 
     * A single '.' will represent the current directory and '~' indicates
     *  the root directory for testing: the Test default directory.
     *  If the path does not start with a '/' char, it is a relative path.
     *  Absolute paths are only supported if it resides within the User path bounds.
     * 
     * @param path - the path of the dir or file
     * 
     * @throws ParserException
     */
    public static void setFilePath (String path) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        path = getAbsPath(path);
        if (! isWithinTestPath(path)) {
            throw new ParserException(functionId + "Path is not within test base path: " + path);
        }

        fileDir = path;
        VarReserved.putCurDirValue(fileDir);
        java.io.File file = new java.io.File(fileDir);
        if (! file.exists() || ! file.isDirectory()) {
            throw new ParserException(functionId + "Invalid directory selection: " + fileDir);
        }
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "    File path changed to: " + fileDir);
    }

    /**
     * create a directory.
     * 
     * @param fname - full name of the path to create
     * 
     * @throws ParserException 
     */
    public static void createDir (String fname) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        File file = getFilePath(fname);
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new ParserException(functionId + "Directory already exists: " + fname);
            } else {
                throw new ParserException(functionId + "File already exists with that name: " + fname);
            }
        }
        String path = file.getAbsolutePath();
        if (! isWithinTestPath(path)) {
            throw new ParserException(functionId + "Path is not within test base path: " + path);
        }
        file.mkdirs();
    }

    /**
     * remove a directory.
     * 
     * @param fname - full name of the path to remove
     * @param force - true to remove even if directory is not empty
     * 
     * @throws ParserException
     * @throws IOException 
     */
    public static void removeDir (String fname, boolean force) throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        File file = FileIO.getFilePath(fname);
        if (! file.isDirectory()) {
            throw new ParserException(functionId + "Directory not found: " + fname);
        }
        
        String path = file.getAbsolutePath();
        if (! isWithinTestPath(path)) {
            throw new ParserException(functionId + "Path is not within test base path: " + path);
        }
        if (force) {
            FileUtils.deleteDirectory(file);
        } else {
            file.delete();
        }
    }

    /**
     * create a file (error if file already exists).
     * If set to open as write, only writes can be performed on the file.
     * If set to open as read,  only reads  can be performed on the file.
     * 
     * @param fname    - name of file to create
     * @param writable - true if make the file writable
     * 
     * @throws ParserException
     * @throws IOException 
     */
    public static void createFile (String fname, boolean writable) throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (fileReader != null && ! writable) {
            throw new ParserException(functionId + "File reader already open: " + fileReadName);
        }
        if (fileWriter != null && writable) {
            throw new ParserException(functionId + "File writer already open: " + fileWriteName);
        }
        if (fileReadName != null && fname.contentEquals(fileReadName)) {
            throw new ParserException(functionId + "File already open for reading: " + fname);
        }
        if (fileWriteName != null && fname.contentEquals(fileWriteName)) {
            throw new ParserException(functionId + "File already open for writing: " + fname);
        }
        
        File file = getFilePath(fname);
        if (file.exists()) {
            throw new ParserException(functionId + "File already exists: " + fname);
        }
        
        // verify path selection is valid
        String path = file.getAbsolutePath();
        if (! isWithinTestPath(path)) {
            throw new ParserException(functionId + "Path is not within test base path: " + path);
        }

        file.createNewFile();
        if (writable) {
            file.setWritable(true);
            String absname = file.getAbsolutePath();
            fileWriter = new PrintWriter(new FileWriter(absname, true));
            fileWriteName = fname;
            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, INDENT + "File writer created for: " + fname);
        } else {
            fileReader = new BufferedReader(new FileReader(file));
            fileReadName = fname;
            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, INDENT + "File reader created for: " + fname);
        }
    }

    /**
     * open an existing file (error if file does not exist).
     * If set to open as write, only writes can be performed on the file.
     * If set to open as read,  only reads  can be performed on the file.
     * 
     * @param fname    - name of file to open
     * @param writable - true to open for writing to
     * 
     * @throws ParserException
     * @throws IOException 
     */
    public static void openFile (String fname, boolean writable) throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (fileReader != null && ! writable) {
            throw new ParserException(functionId + "File reader already open: " + fileReadName);
        }
        if (fileWriter != null && writable) {
            throw new ParserException(functionId + "File writer already open: " + fileWriteName);
        }
        if (fileReadName != null && fname.contentEquals(fileReadName)) {
            throw new ParserException(functionId + "File already open for reading: " + fname);
        }
        if (fileWriteName != null && fname.contentEquals(fileWriteName)) {
            throw new ParserException(functionId + "File already open for writing: " + fname);
        }
        
        File file = getFilePath(fname);
        if (! file.isFile()) {
            throw new ParserException(functionId + "File not found: " + fname);
        }
        if (writable) {
            // verify path selection is valid
            String path = file.getAbsolutePath();
            if (! isWithinTestPath(path)) {
                throw new ParserException(functionId + "Path is not within test base path: " + path);
            }

            if (! file.canWrite()) {
                throw new ParserException(functionId + "Invalid file - no write access: " + fname);
            }
            fileWriter = new PrintWriter(new FileWriter(path, true));
            fileWriteName = fname;
            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, INDENT + "File writer opened for: " + fname);
        } else {
            if (! file.canRead()) {
                throw new ParserException(functionId + "Invalid file - no read access: " + fname);
            }
            fileReader = new BufferedReader(new FileReader(file));
            fileReadName = fname;
            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, INDENT + "File reader opened for: " + fname);
        }
    }

    /**
     * deletes a file.
     * 
     * @param fname - name of the file to delete
     * 
     * @throws ParserException 
     */
    public static void delete (String fname) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        File file = FileIO.getFilePath(fname);
        if (! file.isFile()) {
            throw new ParserException(functionId + "File not found: " + fname);
        }
        if ( (fileWriteName != null && fname.contentEquals(fileWriteName)) ||
             (fileReadName  != null && fname.contentEquals(fileReadName))    ) {
            throw new ParserException(functionId + "File is currently open: " + fname);
        }

        // verify path selection is valid
        String path = file.getAbsolutePath();
        if (! isWithinTestPath(path)) {
            throw new ParserException(functionId + "Path is not within test base path: " + path);
        }

        file.delete();
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, INDENT + "File deleted: " + file);
    }

    /**
     * closes the file and the reader/writer for it.
     * 
     * @param fname - name of the file to close
     * 
     * @throws ParserException
     * @throws IOException 
     */
    public static void close (String fname) throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (fileWriteName != null && fname.contentEquals(fileWriteName)) {
            fileWriteName = null;
            fileWriter.flush();
            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, INDENT + "File writer flushed: " + fname);
            fileWriter.close();
            fileWriter = null;
            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, INDENT + "File writer closed: " + fname);
        } else if (fileReadName != null && fname.contentEquals(fileReadName)) {
            fileReadName = null;
            fileReader.close();
            fileReader = null;
            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, INDENT + "File reader closed: " + fname);
        } else {
            throw new ParserException(functionId + "File not open: " + fname);
        }
    }

    /**
     * read contents of file into the $RESPONSE reserved variable.
     * The file must have been opened in READ mode.
     * 
     * @param count - number of lines to read (null to read entire file)
     * 
     * @throws ParserException
     * @throws IOException 
     */
    public static void read (Integer count) throws ParserException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (fileReader == null) {
            throw new ParserException(functionId + "File reader not open");
        }
        try {
            // add the lines to $RESPONSE parameter
            int ix = 0;
            String line = "";
            while (line != null) {
                if (count != null && ix >= count) {
                    break;
                }
                line = fileReader.readLine();
                VarReserved.putResponseValue(line);
                ix++;
            }
            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, INDENT + "File reader read " + count + " lines from '" + fileReadName + "'");
        } catch (IOException ex) {
            throw new IOException(functionId + ex);
        }
    }

    /**
     * write text String to file.
     * The file must have been opened in WRITE mode.
     * 
     * @param text the text string to write
     * 
     * @throws ParserException 
     */
    public static void write (String text) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (fileWriter == null) {
            throw new ParserException(functionId + "File writer not open");
        }
        fileWriter.println(text);
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, INDENT + "File writer wrote 1 line to '" + fileWriteName + "'");
    }

    /**
     * write text String to file.
     * The file must have been opened in WRITE mode.
     * 
     * @param array the array of text to write
     * 
     * @throws ParserException 
     */
    public static void write (ArrayList<String> array) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (fileWriter == null) {
            throw new ParserException(functionId + "File writer not open");
        }
        if (array == null) {
            throw new ParserException(functionId + "Text ArrayList to write is null");
        }
        for (int ix = 0; ix < array.size(); ix++) {
            String text = array.get(ix);
            fileWriter.println(text);
        }
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, INDENT + "File writer wrote " + array.size() + " lines to '" + fileWriteName + "'");
    }

    /**
     * returns the current size of a file.
     * 
     * @param fname - name of the file
     * 
     * @return the size of the file (in number of characters)
     * 
     * @throws ParserException 
     */
    public static long fileGetSize (String fname) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        File file = FileIO.getFilePath(fname);
        if (! file.isFile()) {
            throw new ParserException(functionId + "File not found: " + fname);
        }
        return file.length();
    }

    /**
     * returns the number of lines in a file.
     * 
     * @param fname - name of the file
     * 
     * @return the number of lines in the file
     * 
     * @throws ParserException 
     * @throws FileNotFoundException 
     * @throws IOException
     */
    public static int fileGetLines (String fname) throws ParserException, FileNotFoundException, IOException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        File file = FileIO.getFilePath(fname);
        if (! file.isFile()) {
            throw new ParserException(functionId + "File not found: " + fname);
        }
        BufferedReader reader = new BufferedReader(new FileReader(file));
        int count = 0;
        try {
            // add the lines to $RESPONSE parameter
            while (reader.readLine() != null) {
                count++;
            }
            reader.close();
        } catch (IOException ex) {
            throw new IOException(functionId + ex);
        }
        return count;
    }
    
}
