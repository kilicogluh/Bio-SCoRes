package gov.nih.nlm.ling.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/** 
 * A collection of utility methods for dealing with file IO.
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO The same functionality can be found in an existing package, such as Apache Commons IO, probably.
public class FileUtils {
	private static Logger log = Logger.getLogger(FileUtils.class.getName());
	
	/**
	 * Loads properties from a configuration file.
	 * 
	 * @param filename  the name of the properties file
	 * @return  the properties in the file
	 * 
	 * @throws IOException if there is problem with file IO 
	 */
	public static Properties loadPropertiesFromFile(String filename) throws IOException {
		Properties props = new Properties();
		props.load(new FileInputStream(filename));
		return props;
	}
	
	/**
	 * Lists all files in a directory with a given extension.
	 * 
	 * @param dirName		the name of the directory
	 * @param isRecursive	whether subdirectories should be considered
	 * @param ext  			the file extension
	 * @return the list of file names meeting the criterion
	 * 
	 * @throws IOException if there is problem with file IO 
	 */
    public static List<String> listFiles(String dirName, boolean isRecursive, final String ext) throws IOException {
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(dirName))) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    fileNames.addAll(listFiles(path.toString(),isRecursive,ext));
                 }
                if (path.toString().endsWith("." + ext)) 
                	fileNames.add(path.toString());
            }
        } catch (IOException ex) {}
        return fileNames;
    }
		
    /**
     * Returns the text content of a file. It assumes UTF-8 as the encoding.
     * 
     * @param filename	the file name
     * @return the text content of the file
     * 
	 * @throws IOException if there is problem with file IO 
     */
	public static String stringFromFile(String filename) throws IOException {
		return stringFromFile(filename,"UTF8");
	}

	/**
	 * Similar to {@link #stringFromFile(String)}, except this method allows specifying the encoding.
	 * 
     * @param filename	the file name
     * @param encoding 	encoding to use
     * @return the text content of the file
     * 
	 * @throws IOException if there is problem with file IO 
	 * 
	 */
	public static String stringFromFile(String filename, String encoding) throws IOException {
    	BufferedReader br = new BufferedReader(
    			new InputStreamReader(new FileInputStream(filename),encoding));
    	StringBuffer lineBuf = new StringBuffer();
    	String strLine = "";
    	while ((strLine = br.readLine()) != null)  {
    		lineBuf.append(strLine);
    		lineBuf.append("\n");
    	}
    	br.close();
    	return lineBuf.toString();
    	
/*		InputStream stream = new FileInputStream(filename);
		return stringFromStream(stream,encoding);*/
	}
	
	/**
	 * Similar to {@link #stringFromFile(String,String)}, except this method takes a stream as input.
	 * 
     * @param stream	the input stream to use
     * @param encoding 	encoding to use
     * @return the text content of the stream
     * 
	 * @throws IOException if there is problem with reading the stream 
	 * 
	 */
	public static String stringFromStream(InputStream stream, String encoding) throws IOException {
		String strLine;
    	BufferedReader br = new BufferedReader(new InputStreamReader(stream,encoding));
    	StringBuffer lineBuf = new StringBuffer();
    	while ((strLine = br.readLine()) != null)  {
    		lineBuf.append(strLine);
    		lineBuf.append(System.getProperty("line.separator"));
    	}
    	br.close();
    	return lineBuf.toString();
	}
	
	/**
	 * Similar to {@link #stringFromFile(String,String)}, but this method reads the file as bytes.
	 * 
     * @param filename	the file name
     * @param encoding 	encoding to use
     * @return the text content of the file
     * 
	 * @throws IOException if there is problem with file IO 
	 */
	public static String stringFromFileWithBytes(String filename, String encoding) throws IOException {
		Path path = Paths.get(filename);
		byte[] bytes = Files.readAllBytes(path);
	    return new String(bytes,encoding);
	}
	
	/**
	 * Similar to {@link #stringFromFile(String,String)}, except this method returns the content as a list of lines.
	 * 
     * @param filename	the file name
     * @param encoding 	encoding to use
     * @return the text content of the file as a list of lines
     * 
	 * @throws IOException if there is problem with file IO 
	 * 
	 */
	public static List<String> linesFromFile(String filename, String encoding) throws IOException {
		InputStream stream = new FileInputStream(filename);
		return linesFromStream(stream,encoding);
	}
	
	/**
	 * Similar to {@link #linesFromFile(String,String)}, but this method reads in a stream.
	 * 
     * @param stream	the input stream
     * @param encoding 	encoding to use
     * @return the text content of the file as a list of lines
     * 
	 * @throws IOException if there is problem with file IO 
	 * 
	 */
	public static List<String> linesFromStream(InputStream stream, String encoding) throws IOException {
		String strLine;
		List<String> lines = new ArrayList<>();
    	BufferedReader br = new BufferedReader(new InputStreamReader(stream,encoding));
    	while ((strLine = br.readLine()) != null)  {
    		lines.add(strLine);
    	}
    	br.close();
    	return lines;
	}
	
	/**
	 * Reads an interactive line.
	 * 
	 * @return  the text on the interactive line, or null if no text can be read
	 */
	public static String readLineInteractively() {
	    String text = null;
	    try {
	    	BufferedReader br = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
	    	text =  br.readLine();
	    } catch (UnsupportedEncodingException uee) {
		    log.log(Level.SEVERE,"Encoding error trying to read interactive text: {0}.", new Object[]{uee.getMessage()});
		    uee.printStackTrace();
	    } catch (IOException ioe) {
	    	log.log(Level.SEVERE,"IO error trying to read interactive text: {0}.", new Object[]{ioe.getMessage()});
	    	ioe.printStackTrace();
	    } 
	    return text;
	}
	
	/**
	 * Writes textual content to a file.
	 * 
	 * @param outText  the textual content
	 * @param outFile  the file to write to
	 * @param encoding the encoding to use
	 * @throws IOException	if there is a problem with writing to file
	 */
	public static void write(String outText, File outFile, String encoding) throws IOException {
		PrintWriter out = new PrintWriter(outFile,encoding);
		out.write(outText);
		out.append(System.getProperty("line.separator"));
	    out.close();
	}
	
	/**
	 * Similar to {@link #write(String, File, String)}, but UTF8 encoding is assumed.
	 * 
	 * @param outText  the textual content
	 * @param outFile  the file to write to
	 * @throws IOException	if there is a problem with writing to file
	 */
	public static void write(String outText, File outFile) throws IOException {
		write(outText,outFile,"UTF-8");
	}
	

}
