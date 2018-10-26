package tasks.coref.spl.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.core.CoreferenceChain;
import gov.nih.nlm.bioscores.core.CoreferenceSemanticItemFactory;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.io.XMLCoreferenceChainReader;
import gov.nih.nlm.bioscores.io.XMLExpressionReader;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.MultiWord;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.io.XMLEntityReader;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.util.FileUtils;
import tasks.coref.spl.Constants;

/**
 * Utility class to count the number of times each coreferential mention head (potential hypernym)
 * is used to corefer to different entity types.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SPLHypernyms {
	private static Logger log = Logger.getLogger(SPLHypernyms.class.getName());
	
	private static List<String> getExpHeads(CoreferenceChain cc) {
		List<SemanticItem> exps = cc.getExpressions();
		List<String> strs = new ArrayList<>();
		for (SemanticItem exp: exps) {
			Expression e = (Expression)exp;
			Word head = MultiWord.findHeadFromCategory(e.getSurfaceElement().toWordList());
			strs.add(head.getLemma().toLowerCase());
		}
		return strs;
	}
	
	
	private static void getRefTypes(Document doc, CoreferenceChain cc, List<String> refTypes) {
		List<SemanticItem> refs = cc.getReferents();
		for (SemanticItem ref:refs) {
			if (ref instanceof Expression) {
				Expression refx = (Expression)ref;
				LinkedHashSet<CoreferenceChain> ccs = CoreferenceChain.getChainsWithAnaphor(refx);
				for (CoreferenceChain ch: ccs) {
					List<String> chTypes = new ArrayList<>();
					getRefTypes(doc,ch,chTypes);
					refTypes.addAll(chTypes);
				}
			} else {
				refTypes.add(ref.getType());
			}
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
		annTypes.put(Expression.class, Constants.EXP_TYPES);
		annTypes.put(Relation.class, Constants.COREFERENCE_TYPES);
		XMLReader reader = new XMLReader();
		reader.addAnnotationReader(Entity.class, new XMLEntityReader());
		reader.addAnnotationReader(Expression.class,new XMLExpressionReader());
		reader.addAnnotationReader(Relation.class,new XMLCoreferenceChainReader());
		
		Map<String,Integer> drugCounts = new HashMap<>();
		Map<String,Integer> drugClassCounts = new HashMap<>();
		Map<String,Integer> substanceCounts = new HashMap<>();
		
		List<String> files = FileUtils.listFiles(in, false, "xml");
		int fileNum = 0;
		for (String filename: files) {
			String filenameNoExt = filename.replace(".xml", "");
			filenameNoExt = filenameNoExt.substring(filenameNoExt.lastIndexOf(File.separator)+1);
			log.info("Processing " + filenameNoExt + ":" + ++fileNum);
			Document doc = reader.load(filename, true, CoreferenceSemanticItemFactory.class,annTypes, null);
			LinkedHashSet<SemanticItem> relations = Document.getSemanticItemsByClass(doc, Relation.class);
			if (relations != null) {
				for (SemanticItem rel:relations) {
					CoreferenceChain cc = (CoreferenceChain)rel;	
					List<String> expHeads = getExpHeads(cc);
					List<String> refTypes = new ArrayList<>();
					getRefTypes(doc,cc,refTypes);
					for (String exp: expHeads) {
						for (String refType: refTypes) {
							int i = 0;
							if (refType.equals("Drug")) {
								if (drugCounts.containsKey(exp)) {
									i = drugCounts.get(exp);
								}
								drugCounts.put(exp, ++i);
							} else if (refType.equals("Drug_Class")) {
								if (drugClassCounts.containsKey(exp)) {
									i = drugClassCounts.get(exp);
								}
								drugClassCounts.put(exp, ++i);
							} else if (refType.equals("Substance")) {
								if (substanceCounts.containsKey(exp)) {
									i = substanceCounts.get(exp);
								}
								substanceCounts.put(exp, ++i);
							}
						}
					}
				}
			}
		}
		System.out.print("HYPERNYM\tDRUG\tDRUG_CLASS\tSUBSTANCE");
		for (String d: drugCounts.keySet()) {
			System.out.print(d + "\t"+ drugCounts.get(d)+"\t" + 
							(drugClassCounts.containsKey(d) ? drugClassCounts.get(d) : 0) + "\t" + 
							(substanceCounts.containsKey(d) ? substanceCounts.get(d) : 0));
			System.out.println();
		}
		for (String d: drugClassCounts.keySet()) {
			if (drugCounts.containsKey(d)) continue;
			System.out.print(d + "\t"+ 0 +"\t" + 
							drugClassCounts.get(d)+  "\t" + 
							(substanceCounts.containsKey(d) ? substanceCounts.get(d) : 0));
			System.out.println();
		}
		for (String d: substanceCounts.keySet()) {
			if (drugCounts.containsKey(d) || drugClassCounts.containsKey(d)) continue;
			System.out.print(d + "\t"+ 0 +"\t" + 0 +  "\t" + 
						     (substanceCounts.containsKey(d) ? substanceCounts.get(d) : 0));
			System.out.println();
		}
	}

	
}
