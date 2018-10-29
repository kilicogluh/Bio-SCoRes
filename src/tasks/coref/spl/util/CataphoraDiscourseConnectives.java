package tasks.coref.spl.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.core.CoreferenceChain;
import gov.nih.nlm.bioscores.core.CoreferenceSemanticItemFactory;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.io.XMLCoreferenceChainReader;
import gov.nih.nlm.bioscores.io.XMLExpressionReader;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.io.XMLEntityReader;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.sem.DomainProperties;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.transform.DiscourseConnectiveTransformation;
import gov.nih.nlm.ling.util.FileUtils;
import gov.nih.nlm.ling.wrappers.WordNetWrapper;
import tasks.coref.spl.Constants;

/**
 * Utility to find sentence-initial discourse connectives in sentences that involve
 * cataphora with pronominal mentions.
 * 
 * @author Halil Kilicoglu
 *
 */
public class CataphoraDiscourseConnectives {
	private static Logger log = Logger.getLogger(CataphoraDiscourseConnectives.class.getName());
	
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.print("Usage: inputXMLDirectory");
		}
		String in = args[0];
		File inDir = new File(in);
		if (inDir.isDirectory() == false)  {
			System.err.println("inDirectory is not a directory.");
			System.exit(1);
		}
		Map<Class<? extends SemanticItem>,List<String>> annTypes = new HashMap<>();
		
		annTypes.put(Entity.class,Constants.ENTITY_TYPES);
		annTypes.put(Expression.class, Arrays.asList("PersonalPronoun", "DemonstrativePronoun", "DistributivePronoun", 
				"ReciprocalPronoun", "RelativePronoun", "IndefinitePronoun","PossessivePronoun"));
		annTypes.put(Relation.class, Arrays.asList("Cataphora"));
		XMLReader reader = new XMLReader();
		reader.addAnnotationReader(Entity.class, new XMLEntityReader());
		reader.addAnnotationReader(Expression.class,new XMLExpressionReader());
		reader.addAnnotationReader(Relation.class,new XMLCoreferenceChainReader());
		List<String> files = FileUtils.listFiles(in, false, "xml");
		int fileNum = 0;
		Map<String,Set<String>> sentenceInitialConnectives = new HashMap<>();
		for (String filename: files) {
			String filenameNoExt = filename.replace(".xml", "");
			filenameNoExt = filenameNoExt.substring(filenameNoExt.lastIndexOf(File.separator)+1);
			log.info("Processing " + filenameNoExt + ":" + ++fileNum);
			Document doc = reader.load(filename, true, CoreferenceSemanticItemFactory.class,annTypes, null);
					
			LinkedHashSet<SemanticItem> corefs = Document.getSemanticItemsByClass(doc, CoreferenceChain.class);
			if (corefs != null) {
				for (SemanticItem ent: corefs) {
					CoreferenceChain cc = (CoreferenceChain)ent;
					Expression exp = (Expression)cc.getExpressions().get(0);
					SurfaceElement surf = exp.getSurfaceElement();
					Sentence sent = surf.getSentence();
					Word head = sent.getWords().get(0);
					String conn = head.getText().toLowerCase();
					Set<String> ids = sentenceInitialConnectives.get(conn);
					if (ids == null) ids = new HashSet<>();
					ids.add(doc.getId());
					sentenceInitialConnectives.put(conn, ids);
				}
			}
		}
		for (String conn: sentenceInitialConnectives.keySet()) {
			System.out.println("CONNECTIVE: " + conn);
			Set<String> docIds = sentenceInitialConnectives.get(conn);
			for (String id: docIds) 
				System.out.println("\t" + id);
		}
	}
}
