package tasks.factuality.semrep;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import gov.nih.nlm.ling.brat.Annotation;
import gov.nih.nlm.ling.brat.EventAnnotation;
import gov.nih.nlm.ling.brat.ModificationAnnotation;
import gov.nih.nlm.ling.brat.StandoffAnnotationEvaluator;
import gov.nih.nlm.ling.brat.StandoffAnnotationFileComparator;
import gov.nih.nlm.ling.brat.TermAnnotation;
import gov.nih.nlm.ling.util.FileUtils;

/**
 * The class to compare/evaluate a directory of SemRep/factuality standoff annotation files against 
 * another directory of such files. In the case of evaluation, the second directory
 * is expected to contain the gold files. 
 * 
 * It mainly calls {@link StandoffAnnotationEvaluator}, but also prints factuality-specific evaluation
 * output.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SemRepFactualityEvaluator {
	private static Logger log = Logger.getLogger(SemRepFactualityEvaluator.class.getName());	
	
	/**
	 * Writes evaluation results for individual factuality categories (Fact, Probable, etc.)
	 * 
	 * @param pw		the writer object for the output
	 * @param annoTP	the map of true positive annotations
	 * @param annoFP	the map of false positive annotations
	 * @param annoFN	the map of false negative annotations
	 */
	public static void printSubTypeEvaluation(PrintWriter pw, Map<String, List<Annotation>> annoTP,
			Map<String, List<Annotation>> annoFP, Map<String, List<Annotation>> annoFN) { 
		int facttp =0; int factfp=0; int factfn=0;
		int prtp =0; int prfp=0; int prfn=0;
		int pstp =0; int psfp=0; int psfn=0;
		int dbttp =0; int dbtfp=0; int dbtfn=0;
		int cfacttp =0; int cfactfp=0; int cfactfn=0;
		int unctp =0; int uncfp=0; int uncfn=0;
		int condtp =0; int condfp=0; int condfn=0;
		
		for (String t: annoTP.keySet()) {
			List<Annotation> tps = annoTP.get(t);
			for (Annotation a: tps) {
				if (a instanceof ModificationAnnotation == false ) continue;
				ModificationAnnotation ea = (ModificationAnnotation)a;
				if (ea.getValue().equals("Fact")) facttp++;
				if (ea.getValue().equals("Probable")) prtp++;
				if (ea.getValue().equals("Possible")) pstp++;
				if (ea.getValue().equals("Doubtful")) dbttp++;
				if (ea.getValue().equals("Counterfact")) cfacttp++;
				if (ea.getValue().equals("Uncommitted")) unctp++;
				if (ea.getValue().equals("Conditional")) condtp++;
			}
		}
		for (String t: annoFP.keySet()) {
			List<Annotation> fps = annoFP.get(t);
			for (Annotation a: fps) {
				if (a instanceof ModificationAnnotation == false ) continue;
				ModificationAnnotation ea = (ModificationAnnotation)a;
				if (ea.getValue().equals("Fact")) factfp++;
				if (ea.getValue().equals("Probable")) prfp++;
				if (ea.getValue().equals("Possible")) psfp++;
				if (ea.getValue().equals("Doubtful")) dbtfp++;
				if (ea.getValue().equals("Counterfact")) cfactfp++;
				if (ea.getValue().equals("Uncommitted")) uncfp++;
				if (ea.getValue().equals("Conditional")) condfp++;
			}
		}
		for (String t: annoFN.keySet()) {
			List<Annotation> fns = annoFN.get(t);
			for (Annotation a: fns) {
				if (a instanceof ModificationAnnotation == false ) continue;
				ModificationAnnotation ea = (ModificationAnnotation)a;
				if (ea.getValue().equals("Fact")) factfn++;
				if (ea.getValue().equals("Probable")) prfn++;
				if (ea.getValue().equals("Possible")) psfn++;
				if (ea.getValue().equals("Doubtful")) dbtfn++;
				if (ea.getValue().equals("Counterfact")) cfactfn++;
				if (ea.getValue().equals("Uncommitted")) uncfn++;
				if (ea.getValue().equals("Conditional")) condfn++;
			}
		}
		printType(pw,"Fact", facttp,factfp,factfn);
		printType(pw,"Probable", prtp,prfp,prfn);
		printType(pw,"Possible", pstp,psfp,psfn);
		printType(pw,"Doubtful", dbttp,dbtfp,dbtfn);
		printType(pw,"Counterfact", cfacttp,cfactfp,cfactfn);
		printType(pw,"Uncommitted", unctp,uncfp,uncfn);
		printType(pw,"Conditional", condtp,condfp,condfn);
	}
	
	private static void printType(PrintWriter pw, String name, int TP, int FP, int FN) {
		double precision = 0;
		double recall = 0;
		double f_measure = 0;
		if (TP+FP > 0) { precision = (double)TP/(TP+FP); }
		if (TP+FN > 0) { recall = (double)TP/(TP+FN); }
		if ((precision+recall) > 0) { 
			f_measure = (2*precision*recall)/(double)(precision+recall); 
		}

		pw.write(name + "\t" + (TP +FP) + "(" + TP + ")" + "\t" + (TP + FN) + "(" + TP + ")"
				+ "\t" + String.format("%1.4f", precision)
				+ "\t" + String.format("%1.4f", recall)
				+ "\t" + String.format("%1.4f", f_measure)); 		
		pw.write("\n");
	}

	public static void main(String[] args) throws Exception {
			if (args.length < 2) {
				System.err.println("Usage: 'java SemRepFactualityEvaluator annDirectory goldDirectory [outputFile] [printErrors] [printCorrect]'");
				return;
			}
			String annDirName = args[0];
			String goldDirName = args[1];
			if (annDirName.equals(goldDirName)) {
				System.err.println("The annotation directory and the gold directory are the same.");
				System.exit(1);
			}
			boolean approximateMatch = false;
			boolean usedTermMatchOnly = false;
			boolean printErrors = false;
			boolean printCorrect = false;
			String outFile = "";
			if (args.length > 2) outFile = args[2];
			if (args.length > 3) printErrors = Boolean.parseBoolean(args[3]);
			if (args.length > 4) printCorrect = Boolean.parseBoolean(args[4]);
			
			File annDir = new File(annDirName);
			File goldDir = new File(goldDirName);
			if (!(annDir.isDirectory()) || !(goldDir.isDirectory()))  {
				System.err.println("annDirectory and/or goldDirectory is not a directory.");
				System.exit(1);
			}
			System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
			int fileNum = 0;
			List<String> files = FileUtils.listFiles(goldDirName, false, "ann");
			Map<String, List<Annotation>> annoTP = new TreeMap<>();
			Map<String, List<Annotation>> annoFP = new TreeMap<>();
			Map<String, List<Annotation>> annoFN = new TreeMap<>();
			Map<Class, List<String>> map = new HashMap<>();
			List<String>  entityTypes = new ArrayList<String>(Constants.SEMREP_ENTITY_TYPES);
			entityTypes.addAll(Constants.SEMREP_RELATION_TYPES);
			map.put(TermAnnotation.class,entityTypes);
			List<String> ignoreArgTypes = null;
			map.put(EventAnnotation.class,Constants.SEMREP_RELATION_TYPES);
			map.put(ModificationAnnotation.class, Arrays.asList("Factuality"));
			List<String> parseTypes= new ArrayList<String>();
			parseTypes.addAll(map.get(TermAnnotation.class));
			parseTypes.addAll(map.get(EventAnnotation.class));
			parseTypes.addAll(map.get(ModificationAnnotation.class));
			for (String filename: files) {
				String id = filename.replace(".ann", "");
				id = id.substring(id.lastIndexOf(File.separator)+1);
				log.info("Processing " + id + ":" + ++fileNum);
				String annFilename = annDir.getAbsolutePath() + File.separator + id + ".ann";
				String goldFilename = goldDir.getAbsolutePath() + File.separator + id + ".ann";
				StandoffAnnotationFileComparator.compare(id, annFilename, goldFilename, null, parseTypes, ignoreArgTypes,
						approximateMatch, false,usedTermMatchOnly, false,annoTP, annoFP, annoFN);
			}
			PrintWriter pw = null;
			if (outFile.equals(""))
				pw = new PrintWriter(System.out);
			else 
				pw = new PrintWriter(outFile);
			
			StandoffAnnotationEvaluator.printResults(pw,map,annoTP,annoFP,annoFN);
			printSubTypeEvaluation(pw,annoTP,annoFP,annoFN);
			if (printErrors) StandoffAnnotationEvaluator.printDiffs(pw, map, annoFP, annoFN);
			if (printCorrect) StandoffAnnotationEvaluator.printCorrect(pw,map,annoTP);
			pw.flush();
			pw.close();
	}

}
