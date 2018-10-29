package tasks.coref.bionlp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.agreement.SemanticCoercionAgreement;
import gov.nih.nlm.bioscores.core.CoreferenceSemanticItemFactory;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.io.XMLExpressionReader;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;
import gov.nih.nlm.ling.transform.NPInternalTransformation;
import gov.nih.nlm.ling.util.FileUtils;

/**
 * A simple program to determine the head words of the noun phrases with possessive pronouns.
 * The resulting list was used as the basis for {@link SemanticCoercionAgreement} constraint.
 * 
 * @author Halil Kilicoglu
 *
 */
public class BioNLPCoercionList {
	private static Logger log = Logger.getLogger(BioNLPCoercionList.class.getName());
	
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.print("Usage: inputXMLDirectory");
		}
		Map<Class<? extends SemanticItem>,List<String>> annotationTypes = new HashMap<>();
		annotationTypes.put(Expression.class,Arrays.asList("Exp"));
		XMLReader reader = new XMLReader();
		reader.addAnnotationReader(Expression.class,new XMLExpressionReader());
		
		String in = args[0];
		File inDir = new File(in);
		if (inDir.isDirectory() == false)  {
			System.err.println("inputDirectory is not a directory.");
			System.exit(1);
		}
		List<String> poss = Arrays.asList("its","their");
		Map<String,Integer> counts = new TreeMap<>();
		if (inDir.isDirectory()) {	
			List<String> files = FileUtils.listFiles(in, false, "xml");
			int fileNum = 0;
			for (String filename: files) {
				String filenameNoExt = filename.replace(".xml", "");
				filenameNoExt = filenameNoExt.substring(filenameNoExt.lastIndexOf(File.separator)+1);
				log.info("Processing " + filenameNoExt + ":" + ++fileNum);
				Document doc = reader.load(filename, true, CoreferenceSemanticItemFactory.class,annotationTypes, null);
				for (Sentence sent: doc.getSentences()) {
					new NPInternalTransformation(NPInternalTransformation.Method.Dependency).transform(sent);
				}
				LinkedHashSet<SemanticItem> terms = Document.getSemanticItemsByClass(doc, Expression.class);
				if (terms != null) {
					for (SemanticItem ent: terms) {
						SurfaceElement surf = ((Term)ent).getSurfaceElement();
						if (surf.containsAnyLemma(poss) == false) continue;
						Word head = surf.getHead();
						String headT = head.getText().toLowerCase();
						int cnt = 0;
						if (counts.containsKey(headT)) {
							cnt = counts.get(headT);
						}
						counts.put(headT, ++cnt);
					}
				}
			}
		}
		for (String s: counts.keySet()) {
			System.out.println(s + "\t" + counts.get(s));
		}
	}
}
