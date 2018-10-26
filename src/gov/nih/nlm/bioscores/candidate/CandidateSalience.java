package gov.nih.nlm.bioscores.candidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.nlp.trees.Tree;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.ConjunctionDetection;
import gov.nih.nlm.ling.sem.Ontology;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.util.ParseTreeUtils;

/**
 * This class defines methods to break ties between a number of candidate referents 
 * that are equally likely to corefer with the coreferential mention. 
 * A pre-defined salience method is used as a {@link PostScoringCandidateFilter} to 
 * find the best referent. <p>
 * 
 * Currently, five salience methods are defined. These are: <ul>
 * <li> {@link Type#Proximity}: The referent closest to the mention is selected.
 * <li> {@link Type#ParseTree}: The referent closest to the mention on the parse tree is selected.
 * <li> {@link Type#FirstTerm}: The first compatible referent that appears in the document is selected.
 * <li> {@link Type#FocusTerm}: The term that is the document focus is selected.
 * <li> {@link Type#FreqCount}: The compatible term that occurs most in the document is selected.
 * </ul>
 * 
 * The implementation of {@link Type#FocusTerm} is incomplete; currently, the focus term
 * is taken to be the same as the first term.
 * 
 * {@link Type#Proximity} and {@link Type#ParseTree} have been useful; the usefulness
 * of the other methods may be very limited.
 * 
 * @author Halil Kilicoglu
 *
 */
public class CandidateSalience {
	private static Logger log = Logger.getLogger(CandidateSalience.class.getName());
	
	public static enum Type {
		Proximity, ParseTree, FreqCount, FirstTerm, FocusTerm;
		public String toString() {
			switch(this) {
				case Proximity: return "LinearDistance";
				case ParseTree: return "GraphDistance";
				case FreqCount: return "FreqCount";
				case FirstTerm: return "FirstTerm";
				case FocusTerm: return "FocusTerm";
			}
			return null;			
		}
		public void salience(SurfaceElement exp, List<SurfaceElement> candidates, List<SurfaceElement> outCandidates) {
			switch(this) {
				case Proximity: linearDistanceSalience(exp,candidates,outCandidates); break;
				case ParseTree: graphDistanceSalience(exp,candidates,outCandidates); break;
				case FreqCount: frequencyCountSalience(exp,candidates,outCandidates); break;
				case FirstTerm: firstTermSalience(exp,candidates,outCandidates); break;
				case FocusTerm: focusTermSalience(exp,candidates,outCandidates);
			}
		}
	}
	
	private Type type;
	
	/**
	 * Constructs a <code>CandidateSalience</code> object with the given <var>type</var> parameter.
	 * 
	 * @param type	the salience type
	 */
	public CandidateSalience(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}
	
	private static void linearDistanceSalience(SurfaceElement exp, List<SurfaceElement> candidates,
			List<SurfaceElement> outCandidates) {
		if (candidates == null || candidates.size() == 0) return;
		Collections.sort(candidates,SurfaceElement.SPAN_ORDER);
		SurfaceElement closest = null;
		// assuming we can tell cataphora from the position of the candidates
		boolean cataphora = (SpanList.atLeft(exp.getSpan(), candidates.get(0).getSpan()));
		if (cataphora) closest = candidates.get(0);
		else closest = candidates.get(candidates.size()-1);
		SurfaceElement subsuming = getSubsumingSurfaceElement(candidates,closest);
		outCandidates.add(subsuming);
	}
	
	private static void frequencyCountSalience(SurfaceElement exp, List<SurfaceElement> candidates,
			List<SurfaceElement> outCandidates) {
		if (candidates == null || candidates.size() ==0) return;
		Document doc = exp.getSentence().getDocument();
		Map<Ontology,Integer> counts = doc.getOntologyCounts();
		List<Integer> countList = new ArrayList<>(counts.values());
		Collections.sort(countList);
		boolean found = false;
		for (Ontology o: counts.keySet()) {
			for (SurfaceElement s: candidates) {
				Set<SemanticItem> ss = s.getSemantics();
				if (ss == null) continue;
				for (SemanticItem si : ss) {
					if (o.equals(si.getOntology())) {
						outCandidates.add(s);
						found = true;
						break;
					}
				}
				if (found) break;
			}
			if (found) break;
		}
	}
	
	private static void firstTermSalience(SurfaceElement exp, List<SurfaceElement> candidates,
			List<SurfaceElement> outCandidates) {
		if (candidates == null || candidates.size() ==0) return;
		Collections.sort(candidates,SurfaceElement.SPAN_ORDER);
		outCandidates.add(candidates.get(0));
	}
	
	private static void focusTermSalience(SurfaceElement anaphora, List<SurfaceElement> candidates,
			List<SurfaceElement> outCandidates) {
		// TODO: FocusFinder??
		firstTermSalience(anaphora,candidates,outCandidates);
	}
	
	private static void graphDistanceSalience(SurfaceElement exp, List<SurfaceElement> candidates,
			List<SurfaceElement> outCandidates) {
		Sentence expSent = exp.getSentence();
		Document doc = expSent.getDocument();
		Tree expSentTree = expSent.getTree();
		int expIndex = doc.getSentences().indexOf(expSent);
		Tree expTree = ParseTreeUtils.getCorrespondingSubTree(expSentTree, exp);
		if (expTree == null) return;
		int distanceFromRoot = expSentTree.pathNodeToNode(expSentTree, expTree).size();
 		Map<SurfaceElement,Integer> treeDistances = new HashMap<>();
 		Map<SurfaceElement,Integer> sentRootDistances = new HashMap<>();
 		if (candidates.size() == 0) return;
		for (SurfaceElement n: candidates) {
			Sentence candSent = n.getSentence();
			Tree candSentTree = candSent.getTree();
			int candIndex = doc.getSentences().indexOf(candSent);
			int graphDist = -1;
			Tree candTree = ParseTreeUtils.getCorrespondingSubTree(candSent.getTree(), n.getHead());
			if (candTree == null) continue;
			int antDistanceFromRoot = candSentTree.pathNodeToNode(candSentTree, candTree).size();
			if (candIndex == expIndex) {
				graphDist = expSentTree.pathNodeToNode(candTree,expTree).size();
			} else {
				int sentDist = Math.abs(expIndex-candIndex);
				graphDist = distanceFromRoot + antDistanceFromRoot + (sentDist*2);
			}
			sentRootDistances.put(n,antDistanceFromRoot);
			treeDistances.put(n, graphDist);
		}
		for (SurfaceElement s: treeDistances.keySet()) {
			log.log(Level.FINE,"Parse tree distance for the candidate is {0}: {1}.", 
					new Object[]{treeDistances.get(s),s.toString()});
		}
		List<Integer> distances = new ArrayList<Integer>(treeDistances.values());
		Collections.sort(distances);
		int shortest = distances.get(0);
		List<SurfaceElement> temp = new ArrayList<>();
		for (SurfaceElement s: treeDistances.keySet()) {
			if (treeDistances.get(s) == shortest)
				temp.add(s);
		}
		List<SurfaceElement> temp2 = new ArrayList<>();
		if (temp.size() > 1) {
			int shortestLenToRoot = Integer.MAX_VALUE;
			for (SurfaceElement tt: temp) {
				if (sentRootDistances.get(tt) < shortestLenToRoot) 
					shortestLenToRoot = sentRootDistances.get(tt);
			}
			for (SurfaceElement tt: temp) {
				if (sentRootDistances.get(tt) == shortestLenToRoot) temp2.add(tt);
			}
		} else { 
			outCandidates.add(temp.get(0));
			log.log(Level.FINE,"Closest referent by the parse tree: {0}.", 
					new Object[]{temp.get(0).toString()});
			return;
		}
		// get the best candidates in the closest sentence
		List<SurfaceElement> temp3 = new ArrayList<>();
		if (temp2.size() > 1) {
			List<Sentence> sentences = doc.getSentences();
			Sentence closest = sentences.get(0);
			for (SurfaceElement tt: temp2) {
				if (sentences.indexOf(tt.getSentence()) > sentences.indexOf(closest)) closest = tt.getSentence();
			}
			for (SurfaceElement tt: temp2) {
				if (tt.getSentence().equals(closest)) temp3.add(tt);
			}
		} else  { 
			outCandidates.add(temp2.get(0));
			log.log(Level.FINE,"Closest referent by the parse tree/sentence position: {0}.", 
					new Object[]{temp2.get(0).toString()});
			return;
		}
		// get the best candidates by the subject position
		if (temp3.size() > 1) {
			SurfaceElement leftmost = null;
			for (SurfaceElement tt: temp3) {
				if (leftmost == null || SpanList.atLeft(tt.getSpan(),leftmost.getSpan())) leftmost = tt;
			}
			log.log(Level.FINE,"Closest referent by the parse tree/subject position: {0}.", 
					new Object[]{leftmost.toString()});
			outCandidates.add(leftmost);
		} else {
			log.log(Level.FINE,"Closest referent by the parse tree/sentence position: {0}.", 
					new Object[]{temp3.get(0).toString()});
			outCandidates.add(temp3.get(0));
		}
	}
	
	private static SurfaceElement getSubsumingSurfaceElement(List<SurfaceElement> candidates, SurfaceElement subsumed) {
		if (candidates == null) return null;
		if (candidates.size() == 1 && subsumed.equals(candidates.get(0))) return subsumed;
		SurfaceElement subsuming = subsumed;
		for (int i=0; i < candidates.size(); i++) {
			SurfaceElement sui = candidates.get(i);
			if (sui.equals(subsuming)) continue;
			if (SpanList.subsume(sui.getSpan(),subsuming.getSpan()) || 
					ConjunctionDetection.isConjunctionArgument(sui, subsuming)) 
				subsuming = sui;
		}
		return subsuming;
	}
	
}
