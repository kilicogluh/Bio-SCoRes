package tasks.coref.i2b2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.nih.nlm.bioscores.core.CoreferenceSemanticItemFactory;
import gov.nih.nlm.bioscores.core.GenericCoreferencePipeline;
import gov.nih.nlm.ling.brat.StandoffAnnotationReader;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.MultiWord;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.process.ComponentLoader;
import gov.nih.nlm.ling.process.SentenceSegmenter;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import gov.nih.nlm.ling.util.FileUtils;
import gov.nih.nlm.ling.wrappers.CoreNLPWrapper;
import nu.xom.Element;
import nu.xom.Serializer;
import tasks.coref.spl.Constants;

/**
 * The class to read i2b2/VA corpus documents from associated text, markable,
 * and coreference chain files and write them as XML that can be understood by 
 * {@link XMLReader} class.
 * 
 * @author Halil Kilicoglu
 *
 */
public class I2B2ToXMLWriter {
	private static Logger log = Logger.getLogger(I2B2ToXMLWriter.class.getName());	

	private static SentenceSegmenter segmenter = null;
	
	private static Pattern concPattern = Pattern.compile("^c=\\\"(.+?)\\\" (\\d+):(\\d+) (\\d+):(\\d+)\\|\\|t=\\\"(.+?)\\\"");
	private static Pattern chainPattern = Pattern.compile("c=\\\"(.+?)\\\" (\\d+):(\\d+) (\\d+):(\\d+)\\|\\|");
	private static Map<String,String> conceptIds = new HashMap<>();
	
	/**
	 * Adds markables from the markable file to document representation.
	 * 
	 * @param file		the markable file
	 * @param doc		the document associated with the file
	 * @throws IOException if there is problem with file reading/writing
	 */
	public static void addConcepts(String file, Document doc) throws IOException {
		List<String> lines = FileUtils.linesFromFile(file, "UTF-8");
		SemanticItemFactory sif = doc.getSemanticItemFactory();
		for (String line: lines) {
			Matcher concMatch = concPattern.matcher(line);
			if (concMatch.find()) {
				String text = concMatch.group(1);
				int  stSentId = Integer.parseInt(concMatch.group(2));
				int stTokId = Integer.parseInt(concMatch.group(3));
				int endSentId = Integer.parseInt(concMatch.group(4));
				int endTokId = Integer.parseInt(concMatch.group(5));
				String type = concMatch.group(6);
				log.log(Level.FINE,"Reading markable annotation {0}_{1}_{2}_{3}_{4}_{5}.", 
						new Object[]{text,type,stSentId,stTokId,endSentId,endTokId});
				Span sp = getConceptSpan(doc,stSentId,stTokId,endSentId,endTokId);
				List<Word> ws = doc.getWordsInSpan(sp);
				SpanList headSp = MultiWord.findHeadFromCategory(ws).getSpan();
				Entity ent = sif.newEntity(doc, new SpanList(sp), headSp, type);
				conceptIds.put(text + "_" + stSentId + "_" + stTokId + "_" + endSentId + "_" + endTokId, ent.getId());
			}
		}
	}
	
	private static Span getConceptSpan(Document doc, int ssid, int tsid, int seid, int teid) {
		Sentence ssent = doc.getSentences().get(ssid-1);
		int beg = ssent.getWords().get(tsid).getSpan().getBegin();
		Sentence esent = doc.getSentences().get(seid-1);
		int end = esent.getWords().get(teid).getSpan().getEnd();
		return new Span(beg,end);
	}
	
	/**
	 * Adds coreference chains from the chain file to document representation.<p>
	 * These are all added as instances of <code>CoreferenceType.Ontological</code> type, 
	 * since the dataset does not make a distinction between different types.
	 * 
	 * @param file		the chain file
	 * @param doc		the document associated with the file
	 * @throws IOException if there is problem with file reading/writing
	 */
	public static void addCoreferenceChains(String file, Document doc) throws IOException {
		List<String> lines = FileUtils.linesFromFile(file, "UTF-8");
		CoreferenceSemanticItemFactory sif = (CoreferenceSemanticItemFactory)doc.getSemanticItemFactory();
		for (String line: lines) {
			Matcher chainMatch = chainPattern.matcher(line);
			List<Argument> args = new ArrayList<Argument>();
			while (chainMatch.find()) {
				String text = chainMatch.group(1);
				int  stSentId = Integer.parseInt(chainMatch.group(2));
				int stTokId = Integer.parseInt(chainMatch.group(3));
				int endSentId = Integer.parseInt(chainMatch.group(4));
				int endTokId = Integer.parseInt(chainMatch.group(5));
				log.log(Level.FINE,"Reading coreference chain annotation {0}_{1}_{2}_{3}_{4}.", 
						new Object[]{text,stSentId,stTokId,endSentId,endTokId});
				String concText = text + "_" + stSentId + "_" + stTokId + "_" + endSentId + "_" + endTokId; 
				String concId = conceptIds.get(concText);
				Entity ent = (Entity)doc.getSemanticItemById(concId);
				args.add(new Argument("Equiv",ent));
			}
			sif.newCoreferenceChain(doc, "Ontological", args);
		}
	}
	
	/** 
	 * Processes a single i2b2/VA discharge summary from the associated files and returns its XML representation.
	 * 
	 * @param id			the id for the document
	 * @param txtFilename	the text file of the document
	 * @param concFilename  the markable file
	 * @param chainFilename	the coreference chain file
	 * 
	 * @return the XML representation of the document
	 * 
	 * @throws IOException if there is a problem with file reading/writing
	 */
	public static Element processSingleFile(String id, String txtFilename, String concFilename, String chainFilename) 
			throws IOException {
		Document doc = StandoffAnnotationReader.readTextFile(id, txtFilename);
		SemanticItemFactory csif = new CoreferenceSemanticItemFactory(doc,new HashMap<Class<? extends SemanticItem>,Integer>());
		doc.setSemanticItemFactory(csif);
		GenericCoreferencePipeline.analyze(doc,segmenter);
		conceptIds = new HashMap<>();
		addConcepts(concFilename,doc);
		addCoreferenceChains(chainFilename,doc);
		return doc.toXml();
	}
	
	
	/**
	 * Processes i2b2/VA annotation files from respective directories and writes the corresponding
	 * text and XML files to the output directory. The output directory should be 
	 * a valid directory. It will skip the document if the corresponding XML file already exists.
	 * 
	 * @param doc	the directory with text documents
	 * @param conc	the directory with markable files
	 * @param chain	the directory with coreference chain files
	 * @param out  	the output directory
	 * @throws IOException if there is a problem with file reading/writing
	 */
	public static void processDirectory(String doc, String conc, String chain, String out) 
			throws IOException {
		File docDir = new File(doc);
		if (docDir.isDirectory() == false) return;
		File concDir = new File(conc);
		if (concDir.isDirectory() == false) return;
		File chainDir = new File(chain);
		if (chainDir.isDirectory() == false) return;
		File outDir = new File(out);
		if (outDir.isDirectory() == false) return;
		int fileNum = 0;
		List<String> files = FileUtils.listFiles(doc, false, "txt");
		for (String filename: files) {
			String id = filename.substring(filename.lastIndexOf(File.separator)+1);
			log.info("Processing " + id + ":" + ++fileNum);
			String concFilename = concDir.getAbsolutePath() + File.separator + id + ".con";
			if (new File(concFilename).exists() == false) continue;
			String chainFilename = chainDir.getAbsolutePath() + File.separator + id + ".chains";
			if (new File(chainFilename).exists() == false) continue;
			String outFilename = outDir.getAbsolutePath() + File.separator + id + ".xml";
			if (new File(outFilename).exists()) continue;
			Element docEl = processSingleFile(id, filename, concFilename, chainFilename);
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
		if (args.length < 4) {
			System.err.print("Usage: documentDirectory markableDirectory chainDirectory outputDirectory");
		}
		String doc = args[0];
		String con = args[1];
		String chain = args[2];
		String out = args[3];
		File docDir = new File(doc);
		if (docDir.isDirectory() == false) {
			System.err.println("First argument is required to be a text file directory:" + doc);
			System.exit(1);
		}
		File conDir = new File(con);
		if (conDir.isDirectory() == false) {
			System.err.println("Second argument is required to be a markable file directory:" + con);
			System.exit(1);
		}
		File chainDir = new File(chain);
		if (chainDir.isDirectory() == false) {
			System.err.println("Third argument is required to be a text file directory:" + chain);
			System.exit(1);
		}
		File outDir = new File(out);
		if (outDir.isDirectory() == false) {
			System.err.println("The directory " + outDir + " doesn't exist. Creating a new directory..");
			outDir.mkdir();
		}
		// add processing properties
		Properties props = new Properties();
		props.put("sentenceSegmenter","gov.nih.nlm.ling.process.NewlineSentenceSegmenter");
		props.put("annotators","tokenize,ssplit,pos,lemma,parse");	
		props.put("tokenize.options","invertible=true");
		props.put("tokenize.whitespace", "true");
		props.put("ssplit.isOneSentence","true");
		init(props);
		List<String> parseTypes = new ArrayList<>();
		parseTypes.addAll(Constants.EXP_TYPES); 
		parseTypes.addAll(Constants.ENTITY_TYPES); 
		parseTypes.addAll(Constants.COREFERENCE_TYPES);
		processDirectory(doc,con,chain,out);
	}
}
