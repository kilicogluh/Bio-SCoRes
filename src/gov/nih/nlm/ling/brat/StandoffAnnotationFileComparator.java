package gov.nih.nlm.ling.brat;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.util.CollectionUtils;

/**
 * The class to compare two standoff annotation files for evaluation purposes.
 * 
 * @author Halil Kilicoglu
 *
 */
public class StandoffAnnotationFileComparator {
	private static Logger log = Logger.getLogger(StandoffAnnotationFileComparator.class.getName());	
	
	private static Map<Annotation,LinkedHashSet<Annotation>> partialSpanAnnotations = new HashMap<>();
	
	/**
	 * Compares two annotation files (only items with the types specified in <var>parseTypes</var>)
	 * and reports true positive, false positive and false negative annotations.
	 * 
	 * @param id 				document id, presumably compared files have the same id
	 * @param annFilename 		the first file to compare
	 * @param goldFilename 		the second file to compare (or the gold file in the case of evaluation)
	 * @param parseTypes 		the semantic types to compare
	 * @param ignoreArgTypes  	the relation argument types to ignore in comparison (Cue, etc.), if any
	 * @param approximateMatch 	whether to perform approximate span matching  
	 * @param useReference  	true if the term normalizations (CUIs, etc.) are to be used in matching terms
	 * @param matchUsedTermsOnly true for only comparing <code>TermAnnotation</code> objects used in higher-level annotations
	 * @param evaluateSPAN  	true if the unspecific SPAN term annotations can be used for partial term matching
	 * @param annoTP returned true positive annotations keyed by the annotation type
	 * @param annoFP returned false positive annotations 
	 * @param annoFN returned false negative annotations
	 * 
	 * @throws IOException	if there is a problem with reading annotation or gold standard files 
	 */
	public static void compare(String id, String annFilename, String goldFilename, List<String> parseTypes, List<String> ignoreArgTypes,
							   boolean approximateMatch, boolean useReference, boolean matchUsedTermsOnly, boolean evaluateSPAN,
							   Map<String,List<Annotation>> annoTP, 
							   Map<String,List<Annotation>> annoFP, 
							   Map<String,List<Annotation>> annoFN) 
								throws IOException {
		Map<String,List<String>> annLines = StandoffAnnotationReader.readAnnotationFiles(Arrays.asList(annFilename),parseTypes);
		Map<String,List<String>> goldLines = StandoffAnnotationReader.readAnnotationFiles(Arrays.asList(goldFilename),parseTypes);
		Map<Class,List<Annotation>> anns = StandoffAnnotationReader.parseAnnotations(id,annLines,ignoreArgTypes);
		Map<Class,List<Annotation>> gold = StandoffAnnotationReader.parseAnnotations(id,goldLines,ignoreArgTypes);
		if (evaluateSPAN) {
			partialSpanAnnotations = new HashMap<>();
			mapSPANTerms(gold.get(TermAnnotation.class));
		}
		compareTerms(anns, gold, approximateMatch, matchUsedTermsOnly, annoTP, annoFP, annoFN);
		compareRelations(anns, gold, approximateMatch, useReference, evaluateSPAN, annoTP, annoFP, annoFN);
		compareEvents(anns, gold, approximateMatch, useReference, evaluateSPAN, annoTP, annoFP, annoFN);	
		compareEventModifications(anns, gold, approximateMatch, annoTP, annoFP, annoFN);
	}
		
	private static boolean isUsed(TermAnnotation t, Map<Class,List<Annotation>> anns1, Map<Class,List<Annotation>> anns2) {
		return (isUsed(t,anns1) || isUsed(t,anns2));
	}
	
	private static boolean isUsed(TermAnnotation t, Map<Class,List<Annotation>> anns) {
		List<Annotation> relAnns = anns.get(RelationAnnotation.class);
		if (relAnns != null) {
			for (Annotation ann: relAnns) {
				RelationAnnotation rel = (RelationAnnotation)ann;
				if (rel.getArgumentWithAnnotation(t) != null) return true;
			}
		}
		List<Annotation> evAnns = anns.get(EventAnnotation.class);
		if (evAnns != null) {
			for (Annotation ann: evAnns) {
				EventAnnotation rel = (EventAnnotation)ann;
				if (rel.getPredicate().equals(t)) return true;
				if (rel.getArgumentWithAnnotation(t) != null) return true;
			}
		}
		return false;
	}
	
	// This is needed because in cases where useMatchedTermsOnly is used,
	// even though the system might have identified a Term, it may not be taken into account
	// because it is not used. However, if the Term is in the gold standard, we still want to keep it.
	private static boolean isUsed(TermAnnotation t, Set<TermAnnotation> anns) {
		boolean used = false;
		for (Annotation a: anns) {
			TermAnnotation ta = (TermAnnotation)a;
			if (ta.equals(t)) used= true;
		}
		return used;
	}
	
	private static void addEvaluation(boolean evaluateSPAN, Annotation ann, Map<String,List<Annotation>> evals) {
		boolean add = true;
		if (evaluateSPAN) {
			RelationAnnotation ra = (RelationAnnotation)ann;
			if (hasSPANAnnotation(ra.getArguments())) add = false;
		} 
		if (add == false) return;
		List<Annotation> vals = evals.get(ann.getType());
		if (vals == null) { 
			vals = new ArrayList<>();
		}
		vals.add(ann);
		evals.put(ann.getType(), vals);
	}
	
	/**
	 * Compares {@link TermAnnotation} objects
	 * 
	 * @param annotations1  	all annotations from the first file
	 * @param annotations2  	all annotations from the second file
	 * @param approximateMatch  whether to use approximate matching  
	 * @param matchUsedTermsOnly  true for only comparing <code>TermAnnotation</code> objects used in higher-level annotations
	 * @param annoTP  returned true positive annotations keyed by the annotation type
	 * @param annoFP  returned false positive annotations 
	 * @param annoFN  returned false negative annotations
	 * 
	 */
	public static void compareTerms(Map<Class,List<Annotation>> annotations1, Map<Class,List<Annotation>> annotations2, 
									   boolean approximateMatch, boolean matchUsedTermsOnly, 
									   Map<String,List<Annotation>> annoTP, 
									   Map<String,List<Annotation>> annoFP, 
									   Map<String,List<Annotation>> annoFN)  {
		List<Annotation> anns1 = annotations1.get(TermAnnotation.class);
		List<Annotation> anns2 = annotations2.get(TermAnnotation.class);
		// find all the terms used in Relations or Events to ignore the rest
		if (matchUsedTermsOnly) {
			Set<TermAnnotation> usedAnnotations = new HashSet<>();
			if (anns1 != null) {
				for (Annotation ann: anns1) {
					TermAnnotation t = (TermAnnotation)ann;
					if (isUsed(t,annotations1,annotations2)) {
						usedAnnotations.add(t);
					}
				}
			}
			if (anns2 != null) {
				for (Annotation ann: anns2) {
					TermAnnotation t = (TermAnnotation)ann;
					if (isUsed(t,annotations1,annotations2)) {
						usedAnnotations.add(t);
					}
				}
			}
			List<Annotation> unused = new ArrayList<>();
			if (anns1 != null) {
				for (Annotation ann: anns1) {
					TermAnnotation t = (TermAnnotation)ann;
					if (!(isUsed(t,usedAnnotations)))
						unused.add(t);
				}
				anns1.removeAll(unused);
			}
			unused.clear();
			if (anns2 != null) {
				for (Annotation ann: anns2) {
					TermAnnotation t = (TermAnnotation)ann;
					if (!(isUsed(t,usedAnnotations))) 
						unused.add(t);
				}
				anns2.removeAll(unused);
			}
		}
		
		
		if (anns1 == null && anns2 == null) return;
		if (anns1 == null) {
			for (Annotation b: anns2) addEvaluation(false,b,annoFN);
		} else if (anns2 == null) {
			for (Annotation a: anns1) addEvaluation(false,a,annoFP);
		}
		else  {
			Set<Annotation> ann1Pairs = new HashSet<>();
			Set<Annotation> ann2Pairs = new HashSet<>();
			for (Annotation a : anns1) {
				TermAnnotation ta = (TermAnnotation)a;
				for (Annotation b : anns2) {
					TermAnnotation tb = (TermAnnotation)b;
					if (!(ann1Pairs.contains(ta)) && !(ann2Pairs.contains(tb))) {
						if ((!approximateMatch && ta.exactMatch(tb)) ||
							(approximateMatch && ta.approximateMatch(tb))) {
							ann1Pairs.add(ta);
							ann2Pairs.add(tb);
							addEvaluation(false,a,annoTP);
							break;
						}
					}
				}
			}
			for (Annotation a : anns1) {
				if (ann1Pairs.contains(a) == false) addEvaluation(false,a,annoFP);
			}
			for (Annotation b : anns2) {
				if (ann2Pairs.contains(b) == false) addEvaluation(false,b,annoFN);
			}
		}
	}
	
	/**
	 * Compares {@link RelationAnnotation} objects
	 * 
	 * @param annotations1  	all annotations from the first file
	 * @param annotations2  	all annotations from the second file
	 * @param approximateMatch  whether to use approximate span matching  
	 * @param useReference  	true if term reference mappings are to be used in matching terms
	 * @param evaluateSPAN  	true if sub-mentions of SPAN annotations are to be used in matching arguments
	 * @param annoTP  returned true positive annotations keyed by the annotation type
	 * @param annoFP  returned false positive annotations 
	 * @param annoFN  returned false negative annotations
	 * 
	 */
	public static void compareRelations(Map<Class,List<Annotation>> annotations1, Map<Class,List<Annotation>> annotations2, 
										boolean approximateMatch,  boolean useReference, boolean evaluateSPAN,
										Map<String,List<Annotation>> annoTP,  Map<String,List<Annotation>> annoFP, 
										Map<String,List<Annotation>> annoFN) {
		List<Annotation> anns1 = annotations1.get(RelationAnnotation.class);
		List<Annotation> anns2 = annotations2.get(RelationAnnotation.class);
		if (anns1 == null && anns2 == null) return;
		if (anns1 == null) {
			for (Annotation b: anns2) addEvaluation(evaluateSPAN,b,annoFN);
		} else if (anns2 == null) {
			for (Annotation a: anns1) addEvaluation(evaluateSPAN,a,annoFP);
		}
		else  {
			Set<Annotation> ann1Pairs = new HashSet<>();
			Set<Annotation> ann2Pairs = new HashSet<>();
			for (Annotation a : anns1) {
				RelationAnnotation ra = (RelationAnnotation)a;
				for (Annotation b : anns2) {
					RelationAnnotation rb = (RelationAnnotation)b;
					if (!(ann1Pairs.contains(ra)) && !(ann2Pairs.contains(rb))) {
						if ((!approximateMatch && ra.exactMatch(rb)) || 
							(approximateMatch && (ra.approximateMatch(rb) || ra.spanMatch(rb))) || 
							(evaluateSPAN && modifiedSPANMatch(approximateMatch,ra,rb)) ||
							(useReference && ra.referenceMatch(approximateMatch,rb))) {
							ann1Pairs.add(ra);
							ann2Pairs.add(rb);
							addEvaluation(false,a,annoTP);
							break;
						} 
					}
				}
			}
			for (Annotation a : anns1) {
				if (ann1Pairs.contains(a) == false) addEvaluation(evaluateSPAN,a,annoFP);
			}
			for (Annotation b : anns2) {
				if (ann2Pairs.contains(b) == false) addEvaluation(evaluateSPAN,b,annoFN);
			}
		}
	}

	/**
	 * Compares {@link EventAnnotation} objects
	 * 
	 * @param annotations1  	all annotations from the first file
	 * @param annotations2  	all annotations from the second file
	 * @param approximateMatch  whether to use approximate span matching  
	 * @param useReference  	true if term reference mappings are to be used in matching terms
	 * @param evaluateSPAN  	true if sub-mentions of SPAN annotations are to be used in matching arguments
	 * @param annoTP  returned true positive annotations keyed by the annotation type
	 * @param annoFP  returned false positive annotations 
	 * @param annoFN  returned false negative annotations
	 * 
	 */
	public static void compareEvents(Map<Class,List<Annotation>> annotations1, Map<Class,List<Annotation>> annotations2, 
			   						 boolean approximateMatch, boolean useReference, boolean evaluateSPAN,
			   						 Map<String,List<Annotation>> annoTP, Map<String,List<Annotation>> annoFP, 
			   						 Map<String,List<Annotation>> annoFN) {
		List<Annotation> anns1 = annotations1.get(EventAnnotation.class);
		List<Annotation> anns2 = annotations2.get(EventAnnotation.class);
		if (anns1 == null && anns2 == null) return;
		if (anns1 == null) {
			for (Annotation b: anns2) addEvaluation(evaluateSPAN,b,annoFN);
		} else if (anns2 == null) {
			for (Annotation a: anns1) addEvaluation(evaluateSPAN,a,annoFP);
		}
		else  {
			Set<Annotation> ann1Pairs = new HashSet<>();
			Set<Annotation> ann2Pairs = new HashSet<>();
			for (Annotation a : anns1) {
				EventAnnotation ea = (EventAnnotation)a;
				for (Annotation b : anns2) {
					EventAnnotation eb = (EventAnnotation)b;
					if (!(ann1Pairs.contains(ea)) && !(ann2Pairs.contains(eb))) {
						if ((!approximateMatch && ea.exactMatch(eb)) ||
							(approximateMatch && ea.approximateMatch(eb)) ||
							(evaluateSPAN && modifiedSPANMatch(approximateMatch,ea,eb)) ||
							(useReference && ea.referenceMatch(approximateMatch,eb))) {
							ann1Pairs.add(ea);
							ann2Pairs.add(eb);
							addEvaluation(evaluateSPAN,a,annoTP);
							break;
						} 
					} 
				}
			}
			for (Annotation a : anns1) {
				if (ann1Pairs.contains(a) == false) addEvaluation(evaluateSPAN,a,annoFP);
			}
			for (Annotation b : anns2) {
				if (ann2Pairs.contains(b) == false) {
					EventAnnotation eb = (EventAnnotation)b;
					if (hasSPANAnnotation(eb.getArguments())) continue;
					addEvaluation(evaluateSPAN,b,annoFN);
				}
			}
		}
	}
	
	/**
	 * Compares {@link EventModificationAnnotation} objects
	 * 
	 * @param annotations1  all annotations from the first file
	 * @param annotations2  all annotations from the second file
	 * @param approximateMatch  whether to use approximate span matching  
	 * @param annoTP  returned true positive annotations keyed by the annotation type
	 * @param annoFP  returned false positive annotations 
	 * @param annoFN  returned false negative annotations
	 * 
	 */
	public static void compareEventModifications(Map<Class,List<Annotation>> annotations1, Map<Class,List<Annotation>> annotations2, 
			   									 boolean approximateMatch,  
			   									 Map<String,List<Annotation>> annoTP, Map<String,List<Annotation>> annoFP, 
			   									 Map<String,List<Annotation>> annoFN) {
		List<Annotation> anns1 = annotations1.get(EventModificationAnnotation.class);
		List<Annotation> anns2 = annotations2.get(EventModificationAnnotation.class);
		if (anns1 == null && anns2 == null) return;
		if (anns1 == null) {
			for (Annotation b: anns2) addEvaluation(false,b,annoFN);
		} else if (anns2 == null) {
			for (Annotation a: anns1) addEvaluation(false,a,annoFP);
		}
		else  {
			Set<Annotation> ann1Pairs = new HashSet<>();
			Set<Annotation> ann2Pairs = new HashSet<>();
			for (Annotation a : anns1) {
				EventModificationAnnotation ea = (EventModificationAnnotation)a;
				for (Annotation b : anns2) {
					EventModificationAnnotation eb = (EventModificationAnnotation)b;
					if (!(ann1Pairs.contains(ea)) && !(ann2Pairs.contains(eb))) {
						if ((!approximateMatch && ea.exactMatch(eb)) ||
							(approximateMatch && ea.approximateMatch(eb))) {
							ann1Pairs.add(ea);
							ann2Pairs.add(eb);
							addEvaluation(false,a,annoTP);
							break;
						}
					}
				}
			}
			for (Annotation a : anns1) {
				if (ann1Pairs.contains(a) == false) addEvaluation(false,a,annoFP);
			}
			for (Annotation b : anns2) {
				if (ann2Pairs.contains(b) == false) addEvaluation(false,b,annoFN);
			}
		}
	}
		
	private static void mapSPANTerms(List<Annotation> goldTerms) {
		for (Annotation g: goldTerms) {
			if (isSPANAnnotation(g)) {
				LinkedHashSet<Annotation> subsumed = new LinkedHashSet<>();
				for (Annotation o: goldTerms) {
					if (o.equals(g)) continue;
					if (!isSPANAnnotation(o) && SpanList.subsume(g.getSpan(), o.getSpan())) {
						subsumed.add(o);
					}
				}
				partialSpanAnnotations.put(g,subsumed);
			}
		}
	}
	
	private static boolean isSPANAnnotation(Annotation ann) {
		return (ann.getType().equals("SPAN"));
	}
	
	private static boolean hasSPANAnnotation(List<AnnotationArgument> args) {
		for (AnnotationArgument arg: args) {
			if (isSPANAnnotation(arg.getArg())) return true;
		}
		return false;
	}
	
	
	private static boolean modifiedSPANMatch(boolean approxMatch, Annotation a, Annotation b){
		List<Annotation> sra = modifySPAN(a);
		List<Annotation> srb = modifySPAN(b);
		for (Annotation aa: sra) {
			for (Annotation ba: srb) {
				if ((!approxMatch && aa.exactMatch(ba)) ||
						(approxMatch && aa.approximateMatch(ba))) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static List<Annotation> modifySPAN(Annotation annotation) {
		List<Annotation> out = new ArrayList<>();
		if (annotation instanceof TermAnnotation) return out;
		List<AnnotationArgument> args = null;
		if (annotation instanceof RelationAnnotation) {
			args = ((RelationAnnotation)annotation).getArguments();
		}
		if (hasSPANAnnotation(args) == false) {
			out.add(annotation);
			return out;
		}
		List<Collection<AnnotationArgument>> spanArgs = new ArrayList<>();
		List<AnnotationArgument> others = new ArrayList<>(args);
		for (AnnotationArgument arg: args) {
			if (isSPANAnnotation(arg.getArg())) {
				LinkedHashSet<Annotation> replacements = partialSpanAnnotations.get(arg.getArg());
				if (replacements == null) continue;
				LinkedHashSet<AnnotationArgument> replaceArgs = new LinkedHashSet<>();
				for (Annotation r: replacements) {
					replaceArgs.add(new AnnotationArgument(arg.getRole(),r));
				}
				spanArgs.add(replaceArgs);
				others.remove(arg);
			}
		}
		Collection<List<AnnotationArgument>> permutations = CollectionUtils.generatePermutations(spanArgs);
		for (List<AnnotationArgument> perm: permutations) {
			List<AnnotationArgument> nperm = new ArrayList<>(perm);
			nperm.addAll(others);
			Annotation nAnn = null;
			if (annotation instanceof RelationAnnotation) {
				nAnn = new RelationAnnotation(annotation.getId(),annotation.getDocId(),annotation.getType(),
						nperm);
			} else if (annotation instanceof EventAnnotation) {
					nAnn = new EventAnnotation(annotation.getId(),annotation.getDocId(),annotation.getType(),
							nperm, ((EventAnnotation) annotation).getPredicate());
			}
			if (nAnn != null) out.add(nAnn);
		}
		return out;
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usage: 'java StandoffAnnotationFileComparator annFile goldFile " +
							   "[approximateMatch] [useReference] [usedTermMatchOnly] [evaluateSPAN] [printErrors] [printCorrect]'");
			return;
		}
		String annFile = args[0];
		String goldFile = args[1];
		if (annFile.equals(goldFile)) {
			log.log(Level.WARNING,"The annotation file and the gold file are the same: {0}.", new Object[]{annFile});
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
		String filenameNoExt = goldFile.substring(0,goldFile.lastIndexOf('.'));
		Map<Class, List<String>> map = new HashMap<>();
		
		// SPL interactions
		map.put(TermAnnotation.class, Arrays.asList("Drug","Drug_Class","Substance",
				"Context_for_Interaction","Severity_for_Interaction","SPAN","Strength_of_Evidence",
				"Caution_Interaction","Increase_Interaction","Decrease_Interaction","Specific_Interaction",
				"Expression"));
		map.put(RelationAnnotation.class, Arrays.asList("hasDrugMember","hasClassMember","hasFragment","hasSubstanceMember",
				"Coreference"));
		map.put(EventAnnotation.class, Arrays.asList("Specific_Interaction","Caution_Interaction","Decrease_Interaction","Increase_Interaction"));
		List<String> parseTypes= new ArrayList<>();
		parseTypes.addAll(map.get(TermAnnotation.class));
		parseTypes.addAll(map.get(RelationAnnotation.class));
		parseTypes.addAll(map.get(EventAnnotation.class));
		compare(filenameNoExt,annFile,goldFile,parseTypes,null,
				approximateMatch,useReference,usedTermMatchOnly,evaluateSPAN,
				annoTP,annoFP,annoFN);
		PrintWriter pw = new PrintWriter(System.out);
		StandoffAnnotationEvaluator.printResults(pw, map, annoTP, annoFP, annoFN);
		if (printErrors) StandoffAnnotationEvaluator.printDiffs(pw, map, annoFP, annoFN);
		if (printCorrect) StandoffAnnotationEvaluator.printCorrect(pw, map, annoTP);
		pw.flush();
		pw.close();
	}

}
