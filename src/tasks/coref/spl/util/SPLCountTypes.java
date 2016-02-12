package tasks.coref.spl.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import gov.nih.nlm.ling.brat.Annotation;
import gov.nih.nlm.ling.brat.RelationAnnotation;
import gov.nih.nlm.ling.brat.StandoffAnnotationReader;
import gov.nih.nlm.ling.util.FileUtils;
import tasks.coref.spl.Constants;

/**
 * Utility to count coreference type-mention type pairs in the gold standard.
 * 
 *	@author Halil Kilicoglu
 */
public class SPLCountTypes {
	private static Logger log = Logger.getLogger(SPLCountTypes.class.getName());	

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.print("Usage: annotationDirectory");
		}
		String goldDirName = args[0];
		File goldDir = new File(goldDirName);
		if (goldDir.isDirectory() == false)  {
			System.err.println("goldDirectory is not a directory.");
			System.exit(1);
		}
		Map<String,Integer> anaphoraCounts = new HashMap<String,Integer>();
		Map<String,Integer> cataphoraCounts = new HashMap<String,Integer>();
		Map<String,Integer> apposCounts = new HashMap<String,Integer>();
		Map<String,Integer> predNomCounts = new HashMap<String,Integer>();
		int fileNum = 0;
		List<String> files = FileUtils.listFiles(goldDirName, false, "ann");
		List<String> termTypes =  new ArrayList<>();
		termTypes.addAll(Constants.EXP_TYPES);
		termTypes.addAll(Constants.ENTITY_TYPES);
		List<String> parseTypes= new ArrayList<>();
		parseTypes.addAll(termTypes);
		parseTypes.addAll(Constants.COREFERENCE_TYPES);
		for (String filename: files) {
			String id = filename.replace(".ann", "");
			id = id.substring(id.lastIndexOf(File.separator)+1);
			log.info("Processing " + id + ":" + ++fileNum);
			String goldFilename = goldDir.getAbsolutePath() + File.separator + id + ".ann";
			Map<String,List<String>> goldLines = StandoffAnnotationReader.readAnnotationFiles(Arrays.asList(goldFilename),parseTypes);
			Map<Class,List<Annotation>> gold = StandoffAnnotationReader.parseAnnotations(id,goldLines,null);
			List<Annotation> corefs = gold.get(RelationAnnotation.class);
			if (corefs == null) continue;
			for (Annotation rel: corefs) {
				RelationAnnotation coref = (RelationAnnotation)rel;
				String expType = null;
				if (coref.getType().equals("Anaphora")) {
					expType = coref.getArgumentsWithRole("Anaphor").get(0).getType();
					int cnt = 1; 
					if (anaphoraCounts.get(expType) != null) {
						cnt = anaphoraCounts.get(expType) + 1;
					} 
					anaphoraCounts.put(expType, cnt);
				} else 	if (coref.getType().equals("Cataphora")) {
					expType = coref.getArgumentsWithRole("Cataphor").get(0).getType();
					int cnt = 1; 
					if (cataphoraCounts.get(expType) != null) {
						cnt = cataphoraCounts.get(expType) + 1;
					}
					cataphoraCounts.put(expType, cnt);
				} else 	if (coref.getType().equals("Appositive")) {
					expType = coref.getArgumentsWithRole("Attribute").get(0).getType();
					int cnt = 1;
					if (apposCounts.get(expType) != null) {
						cnt = apposCounts.get(expType) + 1;
					} 
					apposCounts.put(expType, cnt);
				} else 	if (coref.getType().equals("PredicateNominative")) {
					expType = coref.getArgumentsWithRole("Attribute").get(0).getType();
					int cnt = 1;
					if (predNomCounts.get(expType) != null) {
						cnt = predNomCounts.get(expType)+ 1;
					} 
					predNomCounts.put(expType, cnt);
				}
			}
		}
		for (String t: anaphoraCounts.keySet()) 
			System.out.println("Anaphora " + t + " " + anaphoraCounts.get(t));
		for (String t: cataphoraCounts.keySet()) 
			System.out.println("Cataphora " + t + " " + cataphoraCounts.get(t));
		for (String t: apposCounts.keySet()) 
			System.out.println("Appositive " + t + " " + apposCounts.get(t));
		for (String t: predNomCounts.keySet()) 
			System.out.println("PredNominative " + t + " " + predNomCounts.get(t));
		}

}
