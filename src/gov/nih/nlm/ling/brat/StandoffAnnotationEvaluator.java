package gov.nih.nlm.ling.brat;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.util.FileUtils;

/**
 * The class to compare/evaluate a directory of standoff annotation files against 
 * another directory of such files. <p>
 * In the case of evaluation, the second directory is expected to contain the gold files.
 * 
 * 
 * @author Halil Kilicoglu
 *
 */
public class StandoffAnnotationEvaluator {
	private static Logger log = Logger.getLogger(StandoffAnnotationEvaluator.class.getName());	

	/**
	 * Writes comparison results to a file or stream, reports Precision, Recall, and F1 measures
	 * at the semantic type, annotation type, and global levels. 
	 * 
	 * @param pw 		the writer to print the results to
	 * @param typeMap 	annotation type/semantic type map for which comparison will be performed.
	 * @param annoTP 	true positives
	 * @param annoFP	false positives
	 * @param annoFN 	false negatives
	 */
	public static void printResults(PrintWriter pw, Map<Class,List<String>> typeMap, 
			Map<String,List<Annotation>> annoTP, 
			Map<String,List<Annotation>> annoFP, 
			Map<String,List<Annotation>> annoFN) {
		pw.write("\n");
		pw.write("Summary\tAnswer\tGold\tPrec\tRec\tF1\n");
		List<Class> sortedKeys=new ArrayList<>(typeMap.keySet());
		Collections.sort(sortedKeys, new Comparator<Class>() {
			public int compare(Class a, Class b) {
				int inda = AbstractAnnotation.COMPLEXITY_ORDER.indexOf(a);
				int indb = AbstractAnnotation.COMPLEXITY_ORDER.indexOf(b);
				if (indb < inda) return 1;
				return -1;
			}
		});
		int gTP = 0, gFP = 0, gFN = 0;
		for (Class c : sortedKeys){
			List<String> types = typeMap.get(c);
			int cTP = 0, cFP = 0, cFN = 0;
			for (String a: types) {
				int TP = (annoTP.get(a) == null ? 0 : annoTP.get(a).size());
				int FP = (annoFP.get(a) == null ? 0 : annoFP.get(a).size());
				int FN = (annoFN.get(a) == null ? 0 : annoFN.get(a).size());
				cTP += TP;
				cFP += FP;
				cFN += FN;
				gTP += TP;
				gFP += FP;
				gFN += FN;
				double precision = 0;
				double recall = 0;
				double f_measure = 0;
				if (TP+FP > 0) { precision = (double)TP/(TP+FP); }
				if (TP+FN > 0) { recall = (double)TP/(TP+FN); }
				if ((precision+recall) > 0) { 
					f_measure = (2*precision*recall)/(double)(precision+recall); 
				}

				pw.write(a + "\t" + (TP +FP) + "(" + TP + ")" + "\t" + (TP + FN) + "(" + TP + ")"
						+ "\t" + String.format("%1.4f", precision)
						+ "\t" + String.format("%1.4f", recall)
						+ "\t" + String.format("%1.4f", f_measure)); 		
				pw.write("\n");
			}
			double cprecision = 0;
			double crecall = 0;
			double cf_measure = 0;

			if (cTP+cFP > 0) { cprecision = (double)cTP/(cTP+cFP); }

			if (cTP+cFN > 0) { crecall = (double)cTP/(cTP+cFN); }

			if ((cprecision+crecall) > 0)
			{ cf_measure = (2*cprecision*crecall)/(double)(cprecision+crecall); }
			pw.write("TOTAL [" + c.getSimpleName() + "] " + (cTP+cFP) +"(" + cTP + ")" + "\t" + (cTP+cFN) + "(" + cTP + ")" + "\n");
			pw.write("Class [" + c.getSimpleName() + "] Precision: " + String.format("%1.4f", cprecision)); pw.write("\n");
			pw.write("Class [" + c.getSimpleName() + "] Recall   : " + String.format("%1.4f", crecall)); pw.write("\n");
			pw.write("Class [" + c.getSimpleName() + "] F1       : " + String.format("%1.4f", cf_measure)); pw.write("\n");
			pw.write("\n");
		}

		double gprecision = 0;
		double grecall = 0;
		double gf_measure = 0;

		if (gTP+gFP > 0) { gprecision = (double)gTP/(gTP+gFP); }
		if (gTP+gFN > 0) { grecall = (double)gTP/(gTP+gFN); }

		if ((gprecision+grecall) > 0) gf_measure = (2*gprecision*grecall)/(double)(gprecision+grecall); 

		pw.write("Global Precision: " + String.format("%1.4f", gprecision)); pw.write("\n");
		pw.write("Global Recall:    " + String.format("%1.4f", grecall)); pw.write("\n");
		pw.write("Global F1:        " + String.format("%1.4f", gf_measure)); pw.write("\n");
		pw.write("\n");
	}

	/**
	 * Writes differences to a file or stream. 
	 * 
	 * @param pw  		the writer to print the errors to
	 * @param typeMap  	annotation type/semantic type map for which comparison will be performed.
	 * @param annoFP  	differences of the first annotation set from the second (or false positives)
	 * @param annoFN  	differences of the second set from the first (or false negatives)
	 */
	public static void printDiffs(PrintWriter pw, Map<Class,List<String>> typeMap, 
			Map<String,List<Annotation>> annoFP, Map<String,List<Annotation>> annoFN) {
		Set<String> ids = new HashSet<>();
		if (annoFP.size() > 0) {
			for (String fp: annoFP.keySet()) {
				List<Annotation> anns = annoFP.get(fp);
				for (Annotation ann: anns) {
					ids.add(ann.getDocId());
				}
			}
		}
		if (annoFN.size() > 0) {
			for (String fn: annoFN.keySet()) {
				List<Annotation> anns = annoFN.get(fn);
				for (Annotation ann: anns) {
					ids.add(ann.getDocId());
				}
			}
		}
		pw.write("\n");
		List<String> idList = new ArrayList<String>(ids);
		Collections.sort(idList);
		for (String id : idList) {
			List<Annotation> idFPs = getIdAnnotations(id,annoFP,typeMap);
			List<Annotation> idFNs = getIdAnnotations(id,annoFN,typeMap);
			List<Annotation> allIdAnnotations = new ArrayList<>(idFPs);
			allIdAnnotations.addAll(idFNs);
			for (Annotation a: allIdAnnotations) {
				if (idFPs.contains(a)) pw.write("[FP] " + a.getDocId() + "#" + a.getId() + "\t" + a.toResolvedString() + "\n");
				else if (idFNs.contains(a)) pw.write("[FN] " + a.getDocId() + "#" + a.getId() + "\t" + a.toResolvedString() + "\n");
			}
			pw.write("\n");
		}
		pw.write("\n");
	}
	
	/**
	 * Writes true positive annotations to a file or stream. 
	 * 
	 * @param pw		the writer to print the errors to
	 * @param typeMap  	annotation type/semantic type map for which comparison will be performed.
	 * @param annoTP  	matches between the output and the gold standard
	 */
	public static void printCorrect(PrintWriter pw, Map<Class,List<String>> typeMap, Map<String,List<Annotation>> annoTP) {
		Set<String> ids = new HashSet<>();
		if (annoTP.size() > 0) {
			for (String tp: annoTP.keySet()) {
				List<Annotation> anns = annoTP.get(tp);
				for (Annotation ann: anns) {
					ids.add(ann.getDocId());
				}
			}
		}
		pw.write("\n");
		List<String> idList = new ArrayList<>(ids);
		Collections.sort(idList);
		for (String id : idList) {
			List<Annotation> idTPs = getIdAnnotations(id,annoTP,typeMap);
			for (Annotation a: idTPs) {
				pw.write("[TP] " + a.getDocId() + "#" + a.getId() + "\t" + a.toResolvedString() + "\n");			}
			pw.write("\n");
		}
		pw.write("\n");
	}
	
	private static List<Annotation> getIdAnnotations(String id, Map<String,List<Annotation>> anns, Map<Class,List<String>> typeMap) {
		List<Class> sortedKeys=new ArrayList<>(typeMap.keySet());
		List<Annotation> out = new ArrayList<>();
		Collections.sort(sortedKeys, new Comparator<Class>() {
			public int compare(Class a, Class b) {
				int inda = AbstractAnnotation.COMPLEXITY_ORDER.indexOf(a);
				int indb = AbstractAnnotation.COMPLEXITY_ORDER.indexOf(b);
				if (indb < inda) return 1;
				return -1;
			}
		});	
		for (Class c : sortedKeys){
			List<String> types = typeMap.get(c);
			for (String a: types) {
				List<Annotation> ann = anns.get(a);
				if (ann == null) continue;
				for (Annotation an: ann) {
					if (an.getDocId().equals(id))  out.add(an);
				}
			}
		}
		return out;
	}

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usage: 'java StandoffAnnotationEvaluator annDirectory goldDirectory " +
					"[approximateMatch] [useReference] [usedTermMatchOnly] [evaluateSPAN] [printErrors] [printCorrect]'");
			return;
		}
		String annDirName = args[0];
		String goldDirName = args[1];
		if (annDirName.equals(goldDirName)) {
			log.log(Level.SEVERE,"The annotation directory and the gold standard directory are the same: {0}.", new Object[]{annDirName});
			System.exit(1);
		}
		File annDir = new File(annDirName);
		File goldDir = new File(goldDirName);
		if (annDir.isDirectory() == false)  {
			log.log(Level.SEVERE,"annDirectory is not a directory: {0}.", new Object[]{annDirName});
			System.exit(1);
		}
		if (goldDir.isDirectory() == false)  {
			log.log(Level.SEVERE,"goldDirectory is not a directory: {0}.", new Object[]{goldDirName});
			System.exit(1);
		}
		
		boolean approximateMatch = false;
		boolean useReference = false;
		boolean usedTermMatchOnly = false;
		boolean evaluateSPAN = false;
		boolean printErrors = false;
		boolean printCorrect = false;

		if (args.length > 2) approximateMatch = Boolean.parseBoolean(args[2]);
		if (args.length > 3) useReference = Boolean.parseBoolean(args[3]);
		if (args.length > 4) usedTermMatchOnly = Boolean.parseBoolean(args[4]);
		if (args.length > 5) evaluateSPAN = Boolean.parseBoolean(args[5]);
		if (args.length > 6) printErrors = Boolean.parseBoolean(args[6]);
		if (args.length > 7) printCorrect = Boolean.parseBoolean(args[7]);
		
		Map<String, List<Annotation>> annoTP = new TreeMap<>();
		Map<String, List<Annotation>> annoFP = new TreeMap<>();
		Map<String, List<Annotation>> annoFN = new TreeMap<>();
		Map<Class, List<String>> map = new HashMap<>();
		map.put(TermAnnotation.class, Arrays.asList("SPAN",
				"SortalAnaphor", "AcquiredAbnormality","Activity","AgeGroup","Alga","AminoAcidSequence",
				"AminoAcidPeptideOrProtein","Amphibian","AnatomicalAbnormality","AnatomicalStructure","Animal",
				"Antibiotic","Archaeon","Bacterium","Behavior","BiologicFunction","BiologicallyActiveSubstance",
				"BiomedicalOccupationOrDiscipline","BiomedicalOrDentalMaterial","Bird","BodyLocationOrRegion",
				"BodyPartOrganOrOrganComponent","BodySpaceOrJunction","BodySubstance","BodySystem","Carbohydrate",
				"CarbohydrateSequence","Cell","CellComponent","CellFunction","CellOrMolecularDysfunction","Chemical",
				"ChemicalViewedFunctionally","ChemicalViewedStructurally","Classification","ClinicalAttribute",
				"ClinicalDrug","ConceptualEntity","CongenitalAbnormality","DailyOrRecreationalActivity","DiagnosticProcedure",
				"DiseaseOrSyndrome","DrugDeliveryDevice","EducationalActivity","Eicosanoid","ElementIonOrIsotope",
				"EmbryonicStructure","Entity","EnvironmentalEffectOfHumans","Enzyme","Event","ExperimentalModelOfDisease",
				"FamilyGroup","Finding","Fish","Food","FullyFormedAnatomicalStructure","FunctionalConcept","Fungus",
				"GeneOrGeneProduct","GeneOrGenome","GeneticFunction","GeographicArea","GovernmentalOrRegulatoryActivity",
				"Group","GroupAttribute","HazardousOrPoisonousSubstance","HealthCareActivity","HealthCareRelatedOrganization",
				"Hormone","Human","Human-causedPhenomenonOrProcess","IdeaOrConcept","ImmunologicFactor",
				"IndicatorReagentOrDiagnosticAid","IndividualBehavior","InjuryOrPoisoning","InorganicChemical",
				"IntellectualProduct","Invertebrate","LaboratoryProcedure","LaboratoryOrTestResult","Language","Lipid",
				"MachineActivity","Mammal","ManufacturedObject","MedicalDevice","MentalProcess","MentalOrBehavioralDysfunction",
				"MolecularBiologyResearchTechnique","MolecularFunction","MolecularSequence","NaturalPhenomenonOrProcess",
				"NeoplasticProcess","NeuroreactiveSubstanceOrBiogenicAmine","NucleicAcidNucleosideOrNucleotide",
				"NucleotideSequence","Object","OccupationOrDiscipline","OccupationalActivity","OrganOrTissueFunction",
				"OrganicChemical","Organism","OrganismAttribute","OrganismFunction","Organization","OrganophosphorusCompound",
				"PathologicFunction","PatientOrDisabledGroup","PharmacologicSubstance","PhenomenonOrProcess","PhysicalObject",
				"PhysiologicFunction","Plant","PopulationGroup","ProfessionalSociety","ProfessionalOrOccupationalGroup",
				"QualitativeConcept","QuantitativeConcept","Receptor","RegulationOrLaw","Reptile","ResearchActivity",
				"ResearchDevice","RickettsiaOrChlamydia","Self-helpOrReliefOrganization","SignOrSymptom","SocialBehavior",
				"SpatialConcept","Steroid","Substance","TemporalConcept","TherapeuticOrPreventiveProcedure","Tissue",
				"Vertebrate","Virus","Vitamin"));
		map.put(RelationAnnotation.class, Arrays.asList("Coreference")); 
		List<String> parseTypes= new ArrayList<>();
		parseTypes.addAll(map.get(TermAnnotation.class));
		parseTypes.addAll(map.get(RelationAnnotation.class));
		if (useReference) parseTypes.add("Reference");
		
		int fileNum = 0;
		List<String> files = FileUtils.listFiles(goldDirName, false, "ann");
		for (String filename: files) {
			String id = filename.substring(filename.lastIndexOf(File.separator)+1).replace(".ann", "");
			log.log(Level.INFO,"Processing {0}: {1}", new Object[]{id,++fileNum});
			String annFilename = annDir.getAbsolutePath() + File.separator + id + ".ann";
			String goldFilename = goldDir.getAbsolutePath() + File.separator + id + ".ann";
			StandoffAnnotationFileComparator.compare(id, annFilename, goldFilename, parseTypes, null,
					approximateMatch, useReference, usedTermMatchOnly, evaluateSPAN, annoTP, annoFP, annoFN);
		}
		PrintWriter pw = new PrintWriter(System.out);
		printResults(pw,map,annoTP,annoFP,annoFN);
		if (printErrors) printDiffs(pw, map, annoFP, annoFN);
		if (printCorrect) printCorrect(pw, map, annoTP);
		pw.flush();
		pw.close();
	}

}
