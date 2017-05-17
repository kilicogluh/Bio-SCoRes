package tasks.factuality.semrep;

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

import gov.nih.nlm.bioscores.core.GenericCoreferencePipeline;
import gov.nih.nlm.ling.brat.AbstractAnnotation;
import gov.nih.nlm.ling.brat.Annotation;
import gov.nih.nlm.ling.brat.EventAnnotation;
import gov.nih.nlm.ling.brat.ModificationAnnotation;
import gov.nih.nlm.ling.brat.StandoffAnnotationReader;
import gov.nih.nlm.ling.brat.TermAnnotation;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.process.ComponentLoader;
import gov.nih.nlm.ling.process.SentenceSegmenter;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Event;
import gov.nih.nlm.ling.sem.Modification;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import gov.nih.nlm.ling.util.FileUtils;
import gov.nih.nlm.ling.wrappers.CoreNLPWrapper;
import nu.xom.Element;
import nu.xom.Serializer;

/**
 * The class to read factuality standoff annotation files and corresponding SemRep output
 * and write them as XML that can be understood by {@link XMLReader} class.
 * 
 * @author Halil Kilicoglu
 *
 */
public class FactualityToXMLWriter {
	private static Logger log = Logger.getLogger(FactualityToXMLWriter.class.getName());	

	private static List<String> parseTypes = new ArrayList<String>();
	private static SentenceSegmenter segmenter = null;
		
	/** 
	 * Processes a single factuality document from the associated files (syntax and semantics)
	 * and returns its XML representation.
	 * 
	 * @param id			the id for the document
	 * @param txtFilename	the text file of the document
	 * @param annFilename  	the annotation file
	 * @param relLines		the list of relation lines from SemRep full-fielded output
	 * 
	 * @return the XML representation of the document
	 */
	public static Element processSingleFile(String id, String txtFilename, String annFilename, List<String> relLines) {
		Document doc = StandoffAnnotationReader.readTextFile(id, txtFilename);
		SemanticItemFactory sif = doc.getSemanticItemFactory();
		GenericCoreferencePipeline.analyze(doc,segmenter);
		Map<StandoffAnnotationReader.AnnotationType,List<String>> lines = StandoffAnnotationReader.readAnnotationFiles(Arrays.asList(annFilename),null,parseTypes);
		Map<Class,List<Annotation>> annotations = StandoffAnnotationReader.parseAnnotations(id, lines, null);
		for (int i=0; i < AbstractAnnotation.COMPLEXITY_ORDER.size(); i++) {
			Class c = AbstractAnnotation.COMPLEXITY_ORDER.get(i);
			List<Annotation> anns = annotations.get(c);
			if (anns == null) continue;
			for (Annotation ann: anns) {
				String type = ann.getType();
				log.log(Level.FINE,"Processing {0} annotation: {1}.", new Object[]{c.getName(),ann.toString()});
				if (ann instanceof TermAnnotation) {
					if (Constants.SEMREP_ENTITY_TYPES.contains(type)) {
						Entity ent = sif.newEntity(doc, (TermAnnotation)ann);
						log.log(Level.FINE,"Generated entity: {0}.", new Object[]{ent.toShortString()});
					} else if (Constants.SEMREP_RELATION_TYPES.contains(type) || 
							Constants.SEMREP_NEG_RELATION_TYPES.contains(type)) {
						Predicate pr = sif.newPredicate(doc, (TermAnnotation)ann);
						log.log(Level.FINE,"Generated predicate: {0}.", new Object[]{pr.toShortString()});
					}
				} else if (ann instanceof EventAnnotation && 
						   (Constants.SEMREP_RELATION_TYPES.contains(type) || 
						    Constants.SEMREP_NEG_RELATION_TYPES.contains(type))) {
					Event ev = sif.newEvent(doc, (EventAnnotation)ann);
					log.log(Level.FINE,"Generated event: {0}.", new Object[]{ev.toShortString()});
					String evid = ev.getId();
					String line = relLines.get(Integer.parseInt(evid.substring(1))-1);
					String[] lels = line.split("[|]");
					String relType = lels[22];
					String specInfer = "";
					if (relType.indexOf("(") > 0) { 
						int ind = relType.indexOf("(");
						specInfer = relType.substring(ind+1,relType.indexOf(")",ind));
						relType = relType.substring(0,ind);
					}
					String indType = lels[21];
					boolean neg = lels[23].equals("negation");
					ev.addFeature("indicatorType", indType);
					if (specInfer.equals("") == false) ev.addFeature("specInfer", specInfer);
					if (neg) {
						ev.addFeature("negation", "true");
					}
				} else if (ann instanceof ModificationAnnotation && 
						   (Constants.SEMREP_MODIFICATION_TYPES.contains(type))) { 
							Modification mod = sif.newModification(doc, (ModificationAnnotation)ann);
							log.log(Level.FINE,"Generated modification: {0}.", new Object[]{mod.toShortString()});
				}
			}
		}
		return doc.toXml();
	}
	
	private static Map<String,List<String>> readRelationLines(String filename) throws IOException {
		Map<String,List<String>> out = new HashMap<>();
		List<String> lines = FileUtils.linesFromFile(filename, "UTF-8");
		String id = "";
		int cnt = 0;
		for (String l: lines) {
			if (l.trim().equals("")) continue;
			String[] lels = l.split("[|]");

			String type = lels[5];	
			if (type.equals("text")) {
				if (lels[1].equals(id) == false)
				id = lels[1];
			} else if (type.equals("relation")) {
				List<String> outRels = out.get(id);
				if (outRels == null) outRels = new ArrayList<>();
				outRels.add(l);
				out.put(id, outRels);
			}
			cnt++;
		}
		log.log(Level.FINE,"Read {0} SemRep full-fielded output lines.", new Object[]{cnt});
		return out;
	}
	
	/**
	 * Processes a directory of factuality annotation files and writes the corresponding
	 * text and XML files to the output directory. <p>
	 * 
	 * The annotation directory is expected to have txt and ann files.
	 * To incorporate SemRep information, it also uses as input the corresponding full-fielded SemRep output file. 
	 * 
	 * @param annDirName		the input directory
	 * @param semrepFileName	the file with corresponding SemRep full-fielded output
	 * @param xmlOutDirName  	the output directory
	 * @throws IOException if there is a problem with file reading/writing
	 */
	public static void processDirectory(String annDirName, String semrepFileName, String xmlOutDirName) throws IOException {
		File annDir = new File(annDirName);
		if (annDir.isDirectory() == false) {
			System.err.println("First argument is required to be an input directory:" + annDir);
			return;
		}
		if (new File(semrepFileName).exists() == false) {
			System.err.println("Second argument is required to be an input file:" + semrepFileName);
			return;
		}
		File outDir = new File(xmlOutDirName);
		if (outDir.isDirectory() == false) {
			System.err.println("The directory " + outDir + " doesn't exist. Creating a new directory..");
			outDir.mkdir();
		}
		int fileNum = 0;
		List<String> files = FileUtils.listFiles(annDirName, false, "ann");
		Map<String,List<String>> relLines = readRelationLines(semrepFileName);
		
		for (String filename: files) {
			String id = filename.substring(filename.lastIndexOf(File.separator)+1).replace(".ann", "");
			log.log(Level.INFO,"Processing {0}: {1}.", new Object[]{id,++fileNum});
			String txtFilename = annDir.getAbsolutePath() + File.separator + id + ".txt";
			String annFilename = annDir.getAbsolutePath() + File.separator + id + ".ann";
			String xmlFilename = outDir.getAbsolutePath() + File.separator + id + ".xml";
			if (new File(xmlFilename).exists()) {
				log.log(Level.INFO, "Output XML file exists: {0}. Skipping..", xmlFilename);
				continue;
			}
			Element docEl = processSingleFile(id, txtFilename, annFilename,relLines.get(id));
			nu.xom.Document xmlDoc = new nu.xom.Document(docEl);
		    Serializer serializer = new Serializer(new FileOutputStream(xmlFilename));
		    serializer.setIndent(4);
		    serializer.write(xmlDoc);
		}
	}
	
	/**
	 * Initializes the term types to read from brat files as well as 
	 * CoreNLP, the sentence segmenter from properties.
	 * 
	 * @param properties	the properties to use for initialization
	 * 			
	 * @throws ClassNotFoundException	if the sentence segmenter class cannot be found
	 * @throws IllegalAccessException	if the sentence segmenter cannot be accessed
	 * @throws InstantiationException	if the sentence segmenter cannot be initializaed
	 */
	public static void init(Properties properties) throws InstantiationException, 
			IllegalAccessException, ClassNotFoundException {
		Map<Class, List<String>> map = new HashMap<Class, List<String>>();
		map.put(TermAnnotation.class, Constants.SEMREP_ENTITY_TYPES);
		map.put(EventAnnotation.class, Constants.SEMREP_RELATION_TYPES); 	
		map.put(ModificationAnnotation.class, Constants.SEMREP_MODIFICATION_TYPES); 	
		parseTypes.addAll(map.get(TermAnnotation.class));
		parseTypes.addAll(map.get(EventAnnotation.class));
		parseTypes.addAll(map.get(ModificationAnnotation.class));
		parseTypes.add("Reference");
		Event.setDefinitions(Constants.loadSemRepDefinitions());
		CoreNLPWrapper.getInstance(properties);
		segmenter = ComponentLoader.getSentenceSegmenter(properties);
	}

	public static void main(String[] args) throws IOException, InstantiationException, 
			IllegalAccessException, ClassNotFoundException {
		if (args.length < 3) {
			System.err.print("Usage: bratAnnotationDirectory semrepDirectory outputDirectory");
		}

		String annDirName = args[0];
		String semrep = args[1];
		String xmlDirName = args[2];
		Properties props = FileUtils.loadPropertiesFromFile("factuality_semrep.properties");
		init(props); 
		processDirectory(annDirName, semrep, xmlDirName); 
	}

}
