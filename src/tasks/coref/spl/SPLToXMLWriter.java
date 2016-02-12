package tasks.coref.spl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.core.CoreferenceSemanticItemFactory;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.core.GenericCoreferencePipeline;
import gov.nih.nlm.ling.brat.Annotation;
import gov.nih.nlm.ling.brat.EventAnnotation;
import gov.nih.nlm.ling.brat.EventModificationAnnotation;
import gov.nih.nlm.ling.brat.RelationAnnotation;
import gov.nih.nlm.ling.brat.StandoffAnnotationReader;
import gov.nih.nlm.ling.brat.TermAnnotation;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.process.ComponentLoader;
import gov.nih.nlm.ling.process.SentenceSegmenter;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import gov.nih.nlm.ling.util.FileUtils;
import gov.nih.nlm.ling.wrappers.CoreNLPWrapper;
import nu.xom.Element;
import nu.xom.Serializer;

/**
 * The class to read SPL standoff annotation files
 * and write them as XML that can be understood by {@link XMLReader} class.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SPLToXMLWriter {
	private static Logger log = Logger.getLogger(SPLToXMLWriter.class.getName());	
	
	private static List<String> parseTypes = new ArrayList<>();
	private static SentenceSegmenter segmenter = null;
	
	/** 
	 * Processes a single SPL drug coreference document from the associated files and returns its XML representation.
	 * 
	 * @param id			the id for the document
	 * @param txtFilename	the text file of the document
	 * @param annFilename  the annotation file
	 * 
	 * @return the XML representation of the document
	 * @throws IOException if there is a problem with file reading/writing
	 */
	public static Element processSingleFile(String id, String txtFilename, String annFilename) throws IOException {
		Document doc = StandoffAnnotationReader.readTextFile(id, txtFilename);
		SemanticItemFactory csif = new CoreferenceSemanticItemFactory(doc,new HashMap<Class<? extends SemanticItem>,Integer>());
		doc.setSemanticItemFactory(csif);
		GenericCoreferencePipeline.analyze(doc,segmenter);
		Map<String,List<String>> lines = StandoffAnnotationReader.readAnnotationFiles(Arrays.asList(annFilename), parseTypes);
		Map<Class,List<Annotation>> annotations = StandoffAnnotationReader.parseAnnotations(id, lines, null);
		CoreferenceSemanticItemFactory sif = (CoreferenceSemanticItemFactory)doc.getSemanticItemFactory();	
		Class[] processOrder = new Class[]{TermAnnotation.class,RelationAnnotation.class,
				EventAnnotation.class,EventModificationAnnotation.class};
		for (int i=0; i < processOrder.length; i++) {
			Class c = processOrder[i];
			List<Annotation> anns = annotations.get(c);
			if (anns == null) continue;
			for (Annotation ann: anns) {
				String type = ann.getType();
				log.log(Level.FINE,"Reading {0}: {1}.", new Object[]{c.getName(),ann.toString()});
				if (ann instanceof TermAnnotation) {
					if (Constants.EXP_TYPES.contains(type)) {
						Expression exp = sif.newExpression(doc, (TermAnnotation)ann);
						if (type.equals("Expression")) exp.setType("ZeroArticleNP");
					} else  {
						sif.newEntity(doc, (TermAnnotation)ann);
					}  
				} else if (ann instanceof RelationAnnotation && Constants.COREFERENCE_TYPES.contains(type)) {
					sif.newCoreferenceChain(doc,(RelationAnnotation)ann);
				} 
			}
		}
		return doc.toXml();
	}
	
		
	/**
	 * Processes a directory of SPL annotation files and writes the corresponding
	 * text and SPL XML files to the output directory. The output directory should be 
	 * a valid directory. SPL annotation directory is expected to have txt, a1, and a2
	 * files.
	 * 
	 * @param in	the input directory
	 * @param out  	the output directory
	 * @throws IOException if there is a problem with file reading/writing
	 */
	public static void processDirectory(String in, String out) 
			throws IOException {
		File inDir = new File(in);
		if (inDir.isDirectory() == false) return;
		File outDir = new File(out);
		if (outDir.isDirectory() == false) return;
		int fileNum = 0;
		List<String> files = FileUtils.listFiles(in, false, "txt");
		for (String filename: files) {
			String id = filename.substring(filename.lastIndexOf(File.separator)+1).replace(".txt", "");
			log.log(Level.INFO,"Processing {0}: {1}.", new Object[]{id,++fileNum});
			String annFilename = inDir.getAbsolutePath() + File.separator + id + ".ann";
			if (new File(annFilename).exists() == false) continue;
			String outFilename = outDir.getAbsolutePath() + File.separator + id + ".xml";
			if (new File(outFilename).exists()) {
				log.log(Level.INFO, "Output XML file exists: {0}. Skipping..", outFilename);
//				continue;
			}
			Element docEl = processSingleFile(id, filename, annFilename);
			nu.xom.Document xmlDoc = new nu.xom.Document(docEl);
		    Serializer serializer = new Serializer(new FileOutputStream(outFilename));
		    serializer.setIndent(4);
		    serializer.write(xmlDoc);  
		}
	}
	
	/**
	 * Initializes CoreNLP and the sentence segmenter from properties.
	 * 
	 * @param props	the properties to use for initialization
	 * 
	 * @throws ClassNotFoundException	if the sentence segmenter class cannot be found
	 * @throws IllegalAccessException	if the sentence segmenter cannot be accessed
	 * @throws InstantiationException	if the sentence segmenter cannot be initializaed
	 */
	public static void init(Properties props) 
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		CoreNLPWrapper.getInstance(props);
		segmenter = ComponentLoader.getSentenceSegmenter(props);
	}
		 
	public static void main(String[] args) 
			throws IOException, InstantiationException, 
			IllegalAccessException, ClassNotFoundException {
		if (args.length < 2) {
			System.err.print("Usage: inputDirectory outputDirectory");
		}
		String in = args[0];
		String out = args[1];
		File inDir = new File(in);
		if (inDir.isDirectory() == false) {
			System.err.println("First argument is required to be an input directory:" + in);
			System.exit(1);
		}
		File outDir = new File(out);
		if (outDir.isDirectory() == false) {
			System.err.println("The directory " + outDir + " doesn't exist. Creating a new directory..");
			outDir.mkdir();
		}
		// add processing properties
		Properties props = new Properties();
		props.put("sentenceSegmenter","gov.nih.nlm.ling.process.MedlineSentenceSegmenter");
		props.put("annotators","tokenize,ssplit,pos,lemma,parse");	
		props.put("tokenize.options","invertible=true");
		props.put("ssplit.isOneSentence","true");
		init(props);
		parseTypes.addAll(Constants.EXP_TYPES); 
		parseTypes.addAll(Constants.ENTITY_TYPES); 
		parseTypes.addAll(Constants.COREFERENCE_TYPES);
		processDirectory(in,out);
	}
}
