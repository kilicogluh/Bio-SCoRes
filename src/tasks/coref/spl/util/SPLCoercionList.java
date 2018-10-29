package tasks.coref.spl.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.core.CoreferenceSemanticItemFactory;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.io.XMLExpressionReader;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.io.XMLEntityReader;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;
import gov.nih.nlm.ling.transform.NPInternalTransformation;
import gov.nih.nlm.ling.util.FileUtils;
import tasks.coref.spl.Constants;

/**
 * A simple utility class to determine candidate words for semantic coercion from the dataset.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SPLCoercionList {
	private static Logger log = Logger.getLogger(SPLCoercionList.class.getName());
	
	public static void linguisticPreProcessing(Document doc) {
		for (Sentence sent: doc.getSentences()) {
			new NPInternalTransformation(NPInternalTransformation.Method.Dependency).transform(sent);
			sent.addTransformation(NPInternalTransformation.class);
			sent.logTransformation("NPInternalTransformation");
		}
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.print("Usage: inputXMLDirectory");
		}
		String in = args[0];
		File inDir = new File(in);
		if (inDir.isDirectory() == false)  {
			System.err.println("inputDirectory is not a directory.");
			System.exit(1);
		}
		Map<Class<? extends SemanticItem>,List<String>> annTypes = new HashMap<>();
		annTypes.put(Entity.class,Constants.ENTITY_TYPES);
		annTypes.put(Expression.class, Arrays.asList("PossessivePronoun"));
		XMLReader reader = new XMLReader();
		reader.addAnnotationReader(Entity.class, new XMLEntityReader());
		reader.addAnnotationReader(Expression.class,new XMLExpressionReader());
		Map<String,Set<String>> coercionMap = new HashMap<>();
		
		List<String> files = FileUtils.listFiles(in, false, "xml");
		int fileNum = 0;
		for (String filename: files) {
			String filenameNoExt = filename.replace(".xml", "");
			filenameNoExt = filenameNoExt.substring(filenameNoExt.lastIndexOf(File.separator)+1);
			log.info("Processing " + filenameNoExt + ":" + ++fileNum);
			Document doc = reader.load(filename, true, CoreferenceSemanticItemFactory.class,annTypes, null);
			linguisticPreProcessing(doc);		
			LinkedHashSet<SemanticItem> terms = Document.getSemanticItemsByClass(doc, Expression.class);
			if (terms != null) {
				for (SemanticItem ent: terms) {
					SurfaceElement surf = ((Term)ent).getSurfaceElement();
					Word head = surf.getHead();
					String conn = head.getText().toLowerCase();
					Set<String> ids = coercionMap.get(conn);
					if (ids == null) ids = new HashSet<>();
					ids.add(doc.getId());
					coercionMap.put(conn, ids);
				}
			}
		}
		for (String tok: coercionMap.keySet()) {
			System.out.println("Head word: " + tok);
			Set<String> docIds = coercionMap.get(tok);
			for (String id: docIds) 
				System.out.println("\t" + id);
		}
	}
}
