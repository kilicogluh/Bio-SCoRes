package gov.nih.nlm.ling.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.util.FileUtils;
import nu.xom.Serializer;

/**
 * A utility class to write the contents of a <code>Document</code> to XML.
 * 
 * @author Halil Kilicoglu
 *
 */
public class XMLWriter {
	private static Logger log = Logger.getLogger(XMLWriter.class.getName());	
	
	/**
	 * Writes the document <var>doc</var> as XML to <var>outFilename</var> file.
	 * 
	 * @param doc			the <code>Document</code> object to write
	 * @param outFilename  	the name of the file to write to
	 * @param indent  		indentation (none if &lt; 0)
	 * 
	 * @throws IOException			 if there is a problem with file IO
	 * @throws FileNotFoundException if the file specified with <var>outFilename</var> is not found
	 */
	public static void write(Document doc, String outFilename, int indent) 
			throws IOException, FileNotFoundException {
		nu.xom.Document xmlDoc = new nu.xom.Document(doc.toXml());
        Serializer serializer = new Serializer(new FileOutputStream(outFilename));
        if (indent > 0) serializer.setIndent(indent);
        serializer.write(xmlDoc); 
	}
	
	 public static void main(String[] args) throws Exception {
		String dir = args[0];
		File inDir = new File(dir);
		String dir2 = args[1];
		File outDir = new File(dir2);
		if (!(inDir.isDirectory())) System.exit(0);
		List<String> files = FileUtils.listFiles(dir, false, "xml");
		int fileNum = 0;
		// only parse entities
		Map<Class<? extends SemanticItem>,List<String>> annotationTypes = new HashMap<>();
		annotationTypes.put(Entity.class, Arrays.asList("Drug","Drug_Class","Substance"));
		XMLReader reader = new XMLReader();
		reader.addAnnotationReader(Entity.class,new XMLEntityReader());
		for (String filename: files) {
			String id = filename.replace(".xml", "");
			log.info("Processing " + id + ":" + ++fileNum);
			String inFilename = inDir.getAbsolutePath() + System.getProperty("file.separator") + filename;
			Document doc = reader.load(inFilename, true, null, annotationTypes, null);
			String outFilename = outDir.getAbsolutePath() + System.getProperty("file.separator") + id + ".xml";
			write(doc,outFilename,4);
		}
	 }

}
