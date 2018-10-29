package gov.nih.nlm.bioscores.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.agreement.Agreement;
import gov.nih.nlm.bioscores.candidate.CandidateFilter;
import gov.nih.nlm.bioscores.candidate.PostScoringCandidateFilter;
import gov.nih.nlm.bioscores.exp.ExpressionFilter;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.SemanticItem;

/**
 * The class that contains the core functionality for coreference resolution; 
 * that is, linking of coreferential mentions to their referents.
 * It assumes that the coreferential mentions have already been recognized using
 * the methods in {@link ExpressionRecognition}. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class CoreferenceResolver {
	private static Logger log = Logger.getLogger(CoreferenceResolver.class.getName());
	
	private static Configuration config = Configuration.getInstance();
			
	/**
	 * Identifies all textual units that are potential antecedents for a coreferential mention
	 * using the specified strategy. 
	 * The resulting surface elements are sorted by character offsets.
	 * 
	 * @param strategy		the resolution strategy to use
	 * @param exp  			the coreferential mention which referents are sought for
	 * @param candidates 	the list of candidates to filter
	 * 
	 * @return list of candidates that pass the filter, empty list if no candidates pass the filter
	 */
	public static List<SurfaceElement> filterCandidates(Strategy strategy, SurfaceElement exp, List<SurfaceElement> candidates) {
		if (candidates == null || candidates.size() == 0) { 
			log.log(Level.FINE,"No candidates to filter for {0}. Skipping..", exp.toString());
			return candidates;
		}
		CoreferenceType type = strategy.getCorefType();
		ExpressionType expType = strategy.getExpType();
		List<? extends CandidateFilter> methods = strategy.getCandidateFilteringMethods();
		List<SurfaceElement> outCandidates = new ArrayList<>(candidates);
		log.log(Level.FINE,"Filtering candidates for the mention {0}.", new Object[]{exp.toString()});
		for (CandidateFilter method: methods) {
			List<SurfaceElement> cands = new ArrayList<>(); 
			method.filter(exp, type, expType, outCandidates, cands);
			outCandidates.retainAll(cands);
			if (outCandidates.size() == 0) break;
			for (SurfaceElement o: outCandidates) {
				log.log(Level.FINEST,"Referent candidate passed the filter {0}: {1}.", new Object[]{method.getClass().getName(),o.toString()});
			}
		}
		Collections.sort(outCandidates,SurfaceElement.SPAN_ORDER);
		return outCandidates;
	}
		
	/**
	 * Scores a list of candidate referents based on their salience with respect to the coreferential mention.
	 * 
	 * @param strategy		resolution strategy to use
	 * @param exp 			the coreferential mention
	 * @param candidates 	the list of candidate referents to score
	 * 
	 * @return a map of candidate-score key-value pairs
	 */
	public static Map<SurfaceElement,Integer> scoreCandidates(Strategy strategy, SurfaceElement exp, List<SurfaceElement> candidates) {
		Map<SurfaceElement, Integer> scoreMap = new HashMap<>();
		if (candidates == null || candidates.size() == 0) {
			log.log(Level.FINE,"No candidates to score for {0}. Skipping..", exp.toString());
			return scoreMap;
		}
		CoreferenceType corefType = strategy.getCorefType();
		ExpressionType type = strategy.getExpType();
		List<ScoringFunction> scoringFunction = strategy.getScoringFunction();
		log.log(Level.FINE,"Scoring candidates for the mention {0}.", new Object[]{exp.toString()});
		for (SurfaceElement cand: candidates) {
			int score = calculateSalienceScore(exp,corefType,type,cand,scoringFunction);
			log.log(Level.FINE,"Salience score for {0}: {1}.", new Object[]{cand.toString(),score});
			scoreMap.put(cand,score);
		}
		return scoreMap;
	}
	
	/**
	 * Calculate the salience score between a coreferential mention and a candidate referent
	 * using a specified list of scoring functions.
	 * 
	 * @param exp  				the coreferential mention
	 * @param corefType  		the coreference type in consideration 
	 * @param expType			the mention type in consideration
	 * @param candidate  		the candidate referent
	 * @param scoringFunctions  the list of scoring functions to use
	 * 
	 * @return the agreement score between the coreferential mention and the candidate referent
	 */
	public static int calculateSalienceScore(SurfaceElement exp, CoreferenceType corefType, ExpressionType expType, SurfaceElement candidate, 
			List<ScoringFunction> scoringFunctions) {
		int score = 0;
		if (scoringFunctions == null) {
			log.warning("No scoring function is provided. Skipping..");
			return score;
		}
		log.log(Level.FINEST, "Calculating salience score between the mention {0} and the candidate referent {1}..", 
				new Object[]{exp.toString(),candidate.toString()});
		for (ScoringFunction sc: scoringFunctions) {
			Class<? extends Agreement> imp = sc.getImplementingClass();
			try {
				Agreement agr = (Agreement)imp.newInstance();
				if (agr.agree(corefType,expType,exp,candidate)) {
					score += sc.getScore();
					log.log(Level.FINEST,"Candidate compatible by measure {0}. New score: {1}.", new Object[]{agr.getClass().getName(), score}) ;
				}
				else  {
					score -= sc.getPenalty();
					log.log(Level.FINEST,"Candidate not compatible by measure {0}. New score: {1}.", new Object[]{agr.getClass().getName(), score}) ;
				}
			} catch (Exception e) {
				log.log(Level.SEVERE,"Error creating an instance of the agreement implementing class {0}.", new Object[]{imp.getName()});
				e.printStackTrace();
			}
		}
		return score;
	}
	
	/**
	 * Applies post-scoring filters associated with the current resolution strategy to the given coreferential mention
	 * 
	 * @param strategy	the resolution strategy to use
	 * @param exp		the coreferential mention in consideration
	 * @param scoreMap	the scoring map formed in previous steps
	 * @return	the updated scoring map, after filters are applied
	 */
	public static Map<SurfaceElement,Integer> applyPostScoringFilters(Strategy strategy, SurfaceElement exp, Map<SurfaceElement,Integer> scoreMap) {
		if (scoreMap == null || scoreMap.size() == 0) {
			return scoreMap;
		}
		Map<SurfaceElement, Integer> outMap = new HashMap<>(scoreMap);
		List<? extends PostScoringCandidateFilter> postFilters = strategy.getPostScoringFilters();
		for (PostScoringCandidateFilter filter: postFilters) {
			outMap = filter.postFilter(exp, outMap);
		}
		return outMap;
	}
	
	/**
	 * 
	 * @param surf		the textual unit to process for coreference
	 * @param strategy	the resolution strategy to consider
	 * 
	 * @return	a list of candidate referents that are compatible with <var>surf</var> according to <var>strategy</var>
	 */
	public static List<SurfaceElement> processSurfaceElement(SurfaceElement surf, Strategy strategy) {
		if (strategy == null || expressionToProcess(surf,strategy) == false) {
			log.log(Level.FINEST,"No appropriate resolution strategy for the mention: {0}. Skipping..", new Object[]{surf.toString()});
			return new ArrayList<>();
		}
		List<SurfaceElement> allSurf = surf.getSentence().getDocument().getAllSurfaceElements();
		List<SurfaceElement> filteredCandidates = filterCandidates(strategy, surf, allSurf);
		Map<SurfaceElement,Integer> scoreMap = scoreCandidates(strategy, surf, filteredCandidates);
		Map<SurfaceElement,Integer> bestCandidateMap = applyPostScoringFilters(strategy,surf,scoreMap);	      
		for (SurfaceElement b: bestCandidateMap.keySet()) 
			log.log(Level.FINE,"Best candidate with score {0}: {1}." , new Object[]{bestCandidateMap.get(b),b.toString()});
		return new ArrayList<>(bestCandidateMap.keySet());
	}
	
	// whether the given textual unit can be processed with the provided resolution strategy
	private static boolean expressionToProcess(SurfaceElement su, Strategy strategy) {
		CoreferenceType type = strategy.getCorefType();
		ExpressionType expType = strategy.getExpType();
		SemanticItem esem = CoreferenceUtils.getCompatibleExpression(expType, su);
		if (esem == null) return false;
		Expression exp = (Expression)esem;
		List<? extends ExpressionFilter> methods = strategy.getExpressionFilteringMethods();
		if (methods == null) return true;
		for (ExpressionFilter method: methods) {
			boolean toProcess= method.filter(type,exp);
			if (!toProcess) return false;
		}
		return true;
	}
	
	/**
	 * Processes the entire document for anaphora chains. 
	 * 
	 * @param doc 			the document to process
	 * @param anaphoraLinks	the anaphora chains that are generated by this method
	 */
	public static void processDocumentForAnaphora(Document doc, List<SurfaceElementChain> anaphoraLinks) {
		for (Sentence s: doc.getSentences()) {
			List<SurfaceElement> surfs = s.getSurfaceElements();
			for (SurfaceElement su: surfs) {
				LinkedHashSet<ExpressionType> types = ExpressionType.getTypes(su);
				for (ExpressionType type : types) {
					Strategy strategy = config.getStrategy(CoreferenceType.Anaphora, type);
					if (strategy == null) continue;
					log.log(Level.FINE," Processing anaphor: {0}.", su.toString());
					List<SurfaceElement> best = processSurfaceElement(su,strategy);
					SurfaceElementChain ch = new SurfaceElementChain(strategy,su,best);
					anaphoraLinks.add(ch);
					log.log(Level.FINE," Adding anaphora chain: {0}.", ch.toString());
				}
			}
		}
	}
	
	/**
	 * Processes the entire document for cataphora chains. 
	 * 
	 * @param doc 				the document to process
	 * @param cataphoraLinks	the cataphora chains that are generated by this method
	 */
	public static void processDocumentForCataphora(Document doc, List<SurfaceElementChain> cataphoraLinks) {
		for (Sentence s: doc.getSentences()) {
			List<SurfaceElement> surfs = s.getSurfaceElements();
			for (SurfaceElement su: surfs) {
				LinkedHashSet<ExpressionType> types = ExpressionType.getTypes(su);
				for (ExpressionType type : types) {
					Strategy strategy = config.getStrategy(CoreferenceType.Cataphora, type);
					if (strategy == null) continue;
					log.log(Level.FINE," Processing cataphor: {0}.", su.toString());
					List<SurfaceElement> best = processSurfaceElement(su,strategy);
					SurfaceElementChain ch = new SurfaceElementChain(strategy,su,best);
					cataphoraLinks.add(ch);
					log.log(Level.FINE," Adding cataphora chain: {0}.", ch.toString());
				}
			}
		}
	}
	
	/**
	 * Processes the entire document for appositive chains. 
	 * 
	 * @param doc				the document to process
	 * @param appositiveLinks	the appositive chains that are generated by this method
	 */
	public static void processDocumentForAppositive(Document doc, List<SurfaceElementChain> appositiveLinks) {
		for (Sentence s: doc.getSentences()) {
			List<SurfaceElement> surfs = s.getSurfaceElements();
			for (SurfaceElement su: surfs) {
				LinkedHashSet<ExpressionType> types = ExpressionType.getTypes(su);
				for (ExpressionType type : types) {
					Strategy strategy = config.getStrategy(CoreferenceType.Appositive, type);
					if (strategy == null) continue;
					log.log(Level.FINE," Processing appositive: {0}.", new Object[]{su.toString()});
					List<SurfaceElement> best = processSurfaceElement(su,strategy);
					List<SurfaceElement> bestFiltered = removeIdentical(appositiveLinks,su,best);
					SurfaceElementChain ch = new SurfaceElementChain(strategy,su,bestFiltered);
					appositiveLinks.add(ch);
					log.log(Level.FINE," Adding appositive chain: {0}.", new Object[]{ch.toString()});
				}
			}
		}
	}
	
	/**
	 * Processes the entire document for predicate nominative chains. 
	 * 
	 * @param doc 			the document to process
	 * @param predNomLinks	the predicate nominative chains that are generated by this method
	 */
	public static void processDocumentForPredicateNominative(Document doc, List<SurfaceElementChain> predNomLinks) {
		for (Sentence s: doc.getSentences()) {
			List<SurfaceElement> surfs = s.getSurfaceElements();
			for (SurfaceElement su: surfs) {
				LinkedHashSet<ExpressionType> types = ExpressionType.getTypes(su);
				for (ExpressionType type : types) {
					Strategy strategy = config.getStrategy(CoreferenceType.PredicateNominative, type);
					if (strategy == null) continue;
					log.log(Level.FINE," Processing predicate nominative: {0}.", new Object[]{su.toString()});
					List<SurfaceElement> best = processSurfaceElement(su,strategy);
					SurfaceElementChain ch = new SurfaceElementChain(strategy,su,best);
					predNomLinks.add(ch);
					log.log(Level.FINE," Adding predicate nominative chain: {0}.", new Object[]{ch.toString()});
				}
			}
		}
	}
	
	/**
	 * Processes the entire document for ontological coreference chains. 
	 * 
	 * @param doc			the document to process
	 * @param corefLinks	the ontological coreference chains that are generated by this method
	 */
	public static void processDocumentForOntologicalCoreference(Document doc, List<SurfaceElementChain> corefLinks) {
		for (Sentence s: doc.getSentences()) {
			List<SurfaceElement> surfs = s.getSurfaceElements();
			for (SurfaceElement su: surfs) {
				LinkedHashSet<ExpressionType> types = ExpressionType.getTypes(su);
				for (ExpressionType type : types) {
					Strategy strategy = config.getStrategy(CoreferenceType.Ontological, type);
					if (strategy == null) continue;
					log.log(Level.FINE," Processing ontological coreference: {0}.", new Object[]{su.toString()});
					List<SurfaceElement> best = processSurfaceElement(su,strategy);
					List<SurfaceElement> bestFiltered = removeIdentical(corefLinks,su,best);
					SurfaceElementChain ch = new SurfaceElementChain(strategy,su,bestFiltered);
					corefLinks.add(ch);
					log.log(Level.FINE," Adding ontological coreference chain: {0}.", new Object[]{ch.toString()});
				}
			}
		}
	}
	
	// remove best candidates already identified by another type.
	private static List<SurfaceElement> removeIdentical(List<SurfaceElementChain> existing, SurfaceElement su, List<SurfaceElement> best) {
		List<SurfaceElement> out = new ArrayList<>();
		for (SurfaceElement b: best) {
			boolean exists = false;
			for (SurfaceElementChain ex: existing) {
				if (ex.getExpression().equals(b)) {
					exists = true;
					List<SurfaceElement> exref = ex.getReferents();
					if (exref.contains(su) == false) out.add(b);
				}
			}
			if (!exists) out.add(b);
		}
		return out;
	}

}
