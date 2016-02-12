package tasks.coref.spl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Logger;

import gov.nih.nlm.ling.brat.Annotation;
import gov.nih.nlm.ling.brat.RelationAnnotation;
import gov.nih.nlm.ling.brat.StandoffAnnotationEvaluator;
import gov.nih.nlm.ling.brat.StandoffAnnotationFileComparator;
import gov.nih.nlm.ling.brat.TermAnnotation;
import gov.nih.nlm.ling.sem.DomainProperties;
import gov.nih.nlm.ling.util.FileUtils;

/**
 * Evaluation program for the SPL coreference resolution pipeline.
 * It takes standoff annotations in an output directory and
 * compares it to the corresponding gold standard directory. It expects
 * annotation and gold files to have ann extension.
 * 
 * Precision, recall, and F1 are calculated and results are written
 * to standard output.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SPLEvaluator {
	private static Logger log = Logger.getLogger(SPLEvaluator.class.getName());	

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usage: annotationDirectory goldDirectory " + 
						"[baseline] [approximateMatch] [usedTermMatchOnly] [printErrors] [printCorrect]");
			System.exit(1);
		}
		String annDirName = args[0];
		String goldDirName = args[1];
		if (annDirName.equals(goldDirName)) {
			System.err.println("The annotation directory and the gold directory are the same. Exiting..");
			System.exit(1);
		}
		File annDir = new File(annDirName);
		File goldDir = new File(goldDirName);
		if (annDir.isDirectory() == false  || goldDir.isDirectory() == false)  {
			System.err.println("annDirectory and/or goldDirectory is not a directory. Exiting..");
			System.exit(1);
		}
		boolean baseline = false;
		boolean approximateMatch = false;
		boolean usedTermMatchOnly = false;
		boolean printErrors = false;
		boolean printCorrect = false;
		if (args.length > 2) {
			baseline = Boolean.parseBoolean(args[2]);
		}
		if (args.length > 3) {
			approximateMatch = Boolean.parseBoolean(args[3]);
		}
		if (args.length > 4) {
			usedTermMatchOnly = Boolean.parseBoolean(args[4]);
		}
		if (args.length > 5) {
			printErrors = Boolean.parseBoolean(args[5]);
		}
		if (args.length > 6) {
			printCorrect = Boolean.parseBoolean(args[6]);
		}
		
		int fileNum = 0;
		List<String> files = FileUtils.listFiles(goldDirName, false, "ann");
		Map<String, List<Annotation>> annoTP = new TreeMap<>();
		Map<String, List<Annotation>> annoFP = new TreeMap<>();
		Map<String, List<Annotation>> annoFN = new TreeMap<>();
		Map<Class, List<String>> map = new HashMap<>();

		Properties props = FileUtils.loadPropertiesFromFile("coref_spl.properties");
		DomainProperties.init(props);
		
		List<String> termTypes =  new ArrayList<>();
		termTypes.addAll(Arrays.asList("Drug","Drug_Class","Substance"));
		termTypes.addAll(DomainProperties.parse2("annotationTypes").get("Entity"));

		List<String> expTypes = null;
		if (baseline)
			expTypes = Arrays.asList("Expression");
		else
			expTypes = Constants.EXP_TYPES;
		termTypes.addAll(expTypes);
		map.put(TermAnnotation.class,termTypes);
		List<String> corefTypes = null;
		if (baseline)
			corefTypes = Arrays.asList("Anaphora");
		else 
			corefTypes = Constants.COREFERENCE_TYPES;
		map.put(RelationAnnotation.class, corefTypes); 
		List<String> parseTypes= new ArrayList<>();
		parseTypes.addAll(map.get(TermAnnotation.class));
		parseTypes.addAll(map.get(RelationAnnotation.class));
		
		for (String filename: files) {
			String id = filename.replace(".ann", "");
			id = id.substring(id.lastIndexOf(File.separator)+1);
			log.info("Processing " + id + ":" + ++fileNum);
			String annFilename = annDir.getAbsolutePath() + File.separator + id + ".ann";
			String goldFilename = goldDir.getAbsolutePath() + File.separator + id + ".ann";
			StandoffAnnotationFileComparator.compare(id, annFilename, goldFilename, parseTypes, null, 
					approximateMatch, true, usedTermMatchOnly, false, annoTP, annoFP, annoFN);
		}
		PrintWriter pw = new PrintWriter(System.out);
		if (printErrors) StandoffAnnotationEvaluator.printDiffs(pw,map,annoFP,annoFN);
		if (printCorrect) StandoffAnnotationEvaluator.printCorrect(pw,map,annoTP);
		StandoffAnnotationEvaluator.printResults(pw,map,annoTP,annoFP,annoFN);
		pw.flush();
		pw.close();
	}

}
