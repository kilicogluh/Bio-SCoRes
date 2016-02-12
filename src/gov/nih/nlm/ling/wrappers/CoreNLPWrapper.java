package gov.nih.nlm.ling.wrappers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.OriginalTextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.StringLabelFactory;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.core.WordLexeme;
import gov.nih.nlm.ling.util.FileUtils;

/**
 * A class that provides access to Stanford CoreNLP functionality. <p>
 * Ideally, we should be able to access each CoreNLP annotation type separately,
 * but, in general, we assume that we have token, sentence, lemma, part-of-speech,
 * parse tree and dependency information before we can do anything, so currently 
 * we are performing all these operations at once. <p> 
 * The annotation types are read from a <code>Properties</code> object, which should
 * contain <i>annotators</i> as key and a comma-delimited list of annotator types (by default,
 * we use <i>tokenize,ssplit,pos,lemma,parse</i> as the annotators).
 * 
 * @author Halil Kilicoglu
 *
 */
public class CoreNLPWrapper {
	private static Logger log = Logger.getLogger(CoreNLPWrapper.class.getName());	
	
	private static CoreNLPWrapper coreNLP = null;
	private static StanfordCoreNLP pipeline = null;
	private static TreebankLanguagePack	tlp = new PennTreebankLanguagePack();
	
	private CoreNLPWrapper(Properties props) {
		pipeline = new StanfordCoreNLP(props);
	}
	
	private CoreNLPWrapper() {
		Properties props = new Properties();
		props.put("annotators", "tokenize,ssplit,pos,lemma,parse");
//		props.put("tokenize.options", "normalizeParentheses=false,normalizeOtherBrackets=false");
		pipeline = new StanfordCoreNLP(props);
	}
	
	/**
	 * Instantiates a Core NLP pipeline with the default annotators. 
	 * The default annotators are <i>tokenize,ssplit,pos,lemma,parse</i>.
	 * 
	 * @return  a CoreNLP instance
	 */
	public static CoreNLPWrapper getInstance() {
		if (pipeline == null) {	
			log.info("Initializing a Stanford Core NLP instance with default annotators...");
			coreNLP = new CoreNLPWrapper();
		}
	    return coreNLP;
	}
	
	/**
	 * Instantiates a Core NLP pipeline from <var>props</var> settings.
	 * 
	 * @param props  the properties
	 * @return  a CoreNLP instance
	 */
	public static CoreNLPWrapper getInstance(Properties props) {
		if (pipeline == null) {	
			log.info("Initializing a Stanford Core NLP instance...");
			coreNLP = new CoreNLPWrapper(props);
		}
	    return coreNLP;
	}
	
	/**
	 * Checks whether CoreNLP is instantiated
	 * 
	 * @return  true if there is a CoreNLP instance
	 */
	public static boolean instantiated() {
		return (coreNLP != null);
	}
	
	/**
	 * Processes a file with Core NLP and writes the XML output to a file.
	 * 
	 * @param inFile  the file to process
	 * @param outFile  the file to write output to
	 * 
	 * @throws IOException	if there is file IO problem
	 */
	public void coreNLP(String inFile, String outFile)  throws IOException {
	    File file = new File(inFile);
	    log.log(Level.INFO,"Processing file with CoreNLP: {0}.", new Object[]{file.getAbsolutePath()});
	    String text = FileUtils.stringFromFileWithBytes(inFile, "UTF-8");    
	    Document doc = new Document(inFile,text);
	    coreNLP(doc);
	    FileUtils.write(doc.toXml().toXML(), new File(outFile));
	}
				
	/**
	 * Runs the Core NLP pipeline on a <code>Document</code> and populates its
	 * content. <p>
	 * It's assumed that the document's {@code Document#getText()} does not return null.
	 * 
	 * @param document  the document to process
	 */
	public static void coreNLP(Document document) {
		if (document == null || document.getText() == null) {
			log.warning("Document is null or has no text. Skipping coreNLP..");
			return;
		}
		Annotation annotation = new Annotation(document.getText());
		pipeline.annotate(annotation);
	    List<CoreMap> sentenceAnns = annotation.get(SentencesAnnotation.class);  
	    if (sentenceAnns == null || sentenceAnns.size() == 0) {
	    	log.warning("No sentence annotations were generated for the document. Skipping coreNLP..");
	    	return;
	    }
		List<Sentence> sentences = new ArrayList<>();
		int si = 0;
	    for(CoreMap sentAnn: sentenceAnns) {
	      List<Word> words = getSentenceWords(sentAnn,0);
	      Sentence sentence = getSentence(sentAnn,++si);
		  sentence.setWords(words);
		  for (Word w: words) w.setSentence(sentence);
		  sentence.setTree(getSentenceTree(sentAnn));
		  List<SynDependency> depList = getSentenceDependencies(sentAnn,words);
		  sentence.setDependencyList(depList);
		  sentence.setSurfaceElements(new ArrayList<SurfaceElement>(words));
		  sentence.setEmbeddings(new ArrayList<SynDependency>(depList));
		  sentences.add(sentence);  
	    }
	    document.setSentences(sentences);
	}
	
	/**
	 * Runs the pipeline on a piece of text. <p>
	 * It will perform sentence segmentation, unless <var>singleSentence</var> variable 
	 * is set to true.
	 * 
	 * @param text  		  the input text
	 * @param singleSentence  whether to process the input text as a single sentence
	 * @return the core NLP annotation
	 */
	public static Annotation coreNLP(String text, boolean singleSentence) {
		if (singleSentence) 		
			pipeline.getProperties().setProperty("ssplit.isOneSentence", "true");
		pipeline = new StanfordCoreNLP(pipeline.getProperties());
		Annotation annotation = new Annotation(text);
		pipeline.annotate(annotation);
		return annotation;
	}
	
	/** 
	 * Annotates a single sentence. This can be used to annotate an already
	 * segmented sentence. 
	 * 
	 * @param sent  the sentence to annotate
	 */
	public static void coreNLP(Sentence sent) {
		if (sent.getText().trim().equals("")) {
			return;
		}
		Annotation annotation = new Annotation(sent.getText());
		pipeline.annotate(annotation);
	    List<CoreMap> sentenceAnns = annotation.get(SentencesAnnotation.class);  
	    if (sentenceAnns == null || sentenceAnns.size() == 0) {
	    	log.warning("No sentence annotations were generated. Skipping coreNLP..");
	    	return;
	    }
	    List<Word> words = new ArrayList<>();
	    List<SynDependency> depList = new ArrayList<>();
	    if (sentenceAnns.size() == 1) {
	    	CoreMap sentAnn = sentenceAnns.get(0);
	    	words = getSentenceWords(sentAnn,sent.getSpan().getBegin());
	    	sent.setWords(words);
	    	for (Word w : words) w.setSentence(sent);
	    	sent.setTree(getSentenceTree(sentAnn));
	    	depList = getSentenceDependencies(sentAnn,words);
	    	sent.setDependencyList(depList); 
	    	sent.setSurfaceElements(new ArrayList<SurfaceElement>(words));
	    	sent.setEmbeddings(new ArrayList<SynDependency>(depList));
	    } 
	}
	
	/** 
	 * Determines whether a sentence has been processed. <p>
	 * A sentence is considered unprocessed, if it has no parse tree, no dependencies.
	 * 
	 * @param sentence	the sentence in consideration
	 * @return	true if the sentence has been processed
	 */
	public static boolean isProcessed(Sentence sentence) {
		if (sentence.getTree() == null || sentence.getDependencyList().size() == 0) return false;
		for (SynDependency d: sentence.getDependencyList()) {
			if (d.getType().equals("dep") == false) return true;
		}
		return false;
	}
	
	/**
	 * Creates a <code>Sentence</code> object from the annotation. 
	 * 
	 * @param sentenceAnnotation  	CoreNLP annotation for the sentence
	 * @param index  				the sentence index
	 * @return a <code>Sentence</code> object with span and text information
	 */
	public static Sentence getSentence(CoreMap sentenceAnnotation, int index) {
		int sBegin = sentenceAnnotation.get(CharacterOffsetBeginAnnotation.class);
		int sEnd = sentenceAnnotation.get(CharacterOffsetEndAnnotation.class);
		Span ssp = new Span(sBegin,sEnd);
		String st = sentenceAnnotation.get(TextAnnotation.class);
		Sentence sentence = new Sentence("S" + index,st,ssp);
		return sentence;
	}
	
	/** 
	 * Returns the parse tree from the sentence annotation.
	 * 
	 * @param sentenceAnnotation  CoreNLP annotation for the sentence
	 * @return the corresponding parse tree
	 */
	public static Tree getSentenceTree(CoreMap sentenceAnnotation) {
		return sentenceAnnotation.get(TreeAnnotation.class);
	}
	
	/**
	 * Instantiates the list of <code>Word</code> objects belonging to the sentence.<p>
	 * If the sentence has been annotated independently, <var>sentOffset</var> should be set to 
	 * the offset of first character of the sentence. Otherwise, it should set to 0.
	 * 
	 * @param sentenceAnnotation  CoreNLP annotation for the sentence
	 * @param sentOffset  the sentence offset
	 * @return the list of <code>Word</code> objects
	 */
	public static List<Word> getSentenceWords(CoreMap sentenceAnnotation, int sentOffset) {
	  List<Word> words = new ArrayList<>();
	  List<CoreLabel> tokenAnns = sentenceAnnotation.get(TokensAnnotation.class);
	  if (tokenAnns == null || tokenAnns.size() == 0) {
		  log.warning("No token annotations were generated for the sentence. Skipping coreNLP..");
		  return words;
	  }
	  int wi =0;
	  for (CoreLabel token: sentenceAnnotation.get(TokensAnnotation.class)) {
	    String str = token.get(OriginalTextAnnotation.class);
	    String pos = token.get(PartOfSpeechAnnotation.class);
	    String lemma = token.get(LemmaAnnotation.class);
	    if (StringUtils.isPunct(str) && lemma.startsWith("-") && lemma.endsWith("-")) lemma = str.toLowerCase();
		WordLexeme lex = new WordLexeme(lemma,pos);
		Word w = new Word(str,pos,lex,++wi);
		int begin = token.get(CharacterOffsetBeginAnnotation.class);
		int end = token.get(CharacterOffsetEndAnnotation.class);
		w.setSpan(new SpanList(begin+sentOffset,end+sentOffset));
		words.add(w);
	  }
	  return words;
	}
	
	/**
	 * Instantiates the list of syntactic dependency objects belonging to the sentence.
	 * 
	 * @param sentenceAnnotation  CoreNLP annotation for the sentence
	 * @param words  the list of words in the sentence
	 * @return the list of <code>SynDependency</code> objects
	 */
	public static List<SynDependency> getSentenceDependencies(CoreMap sentenceAnnotation, List<Word> words) {
		  SemanticGraph graph = sentenceAnnotation.get(CollapsedCCProcessedDependenciesAnnotation.class);
		  if (graph == null) return new ArrayList<>();
		  List<TypedDependency> dependencies = new ArrayList<>(graph.typedDependencies());
		  List<SynDependency> depList = new ArrayList<>(dependencies.size());
		  if (words == null || words.size() == 0) {
			  log.warning("No words are associated with the sentence. Skipping coreNLP..");
			  return depList;
		  }
		  int di = 0;
		  for (TypedDependency d: dependencies) {
			  Word gw = words.get(d.gov().index()-1);
			  Word dw = words.get(d.dep().index()-1);
			  String type = d.reln().toString();
	    	  SynDependency sd = new SynDependency("SD" + ++di,type,gw,dw);
	    	  depList.add(sd);
		  }
		  return depList;
	}
		
	/**
	 * Converts a <code>Tree</code> object to its string representation.
	 * 
	 * @param t  a <code>Tree</code> object 
	 * @return  its string representation
	 */
    public static String printTree(Tree t) {
	    StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		TreePrint tp = new TreePrint("penn","markHeadNodes",tlp);
	    tp.printTree(t, pw);
	    return sw.toString();
    }
    
    /**
     * Converts a string to a parse tree. <p>
     * 
     * @param s  the string representation of a parse tree
     * @return  a <code>Tree</code> object corresponding to the parse tree, null if the tree cannot be constructed.
     */
	public static Tree convertToTree(String s) {
		try  {
			PennTreeReader ptr = new PennTreeReader(new StringReader(s),new LabeledScoredTreeFactory(new StringLabelFactory()));
			Tree tr = ptr.readTree();
			ptr.close();
			return tr;
		} catch (IOException ioe) {
			log.log(Level.SEVERE,"Unable to convert the string to a parse tree: {0}. {1}", new Object[]{s,ioe.getMessage()});
			ioe.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) throws IOException {
		System.setProperty("java.util.logging.config.file", "logging.properties");
		String filename = args[0];
		String outputFilename = args[1];
		CoreNLPWrapper.getInstance().coreNLP(filename, outputFilename);
	}
}
