package gov.nih.nlm.ling.sem;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.trees.Tree;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.util.ParseTreeUtils;
import gov.nih.nlm.ling.util.SemUtils;

/**
 * This class contains static utility methods to find and interpret appositives and exemplifications
 * in text. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class AppositiveDetection {
	
	/**
	 * Determines whether two textual units are appositives. <p>
	 * They need to be semantically consonant and syntactically appositive.
	 * 
	 * @param a	the first textual unit
	 * @param b	the second textual unit
	 * @return	true if the textual units can be treated as appositives
	 */
	public static boolean appositive(SurfaceElement a, SurfaceElement b) {
		return (SemUtils.semTypeEquality(a, b, true) && syntacticAppositive(a,b));
	}

	/**
	 * Determines whether two textual units are in a syntactic appositive construction.<p>
	 * It uses types of dependencies that exist between the textual units, as well as the
	 * adjacency of textual units and appositive configuration in a parse tree. 
	 *  
	 * @param a	the first textual unit
	 * @param b	the second textual unit
	 * @return	true if the textual units can be treated as syntactic appositives
	 */
	public static boolean syntacticAppositive(SurfaceElement a, SurfaceElement b) {
		List<SynDependency> depsBetween =
				SynDependency.findDependenciesBetween(a.getSentence().getEmbeddings(), a, b, false);
		if (depsBetween.size() == 0) return false;
		List<SynDependency> appos = SynDependency.dependenciesWithTypes(depsBetween, 
				SynDependency.APPOS_DEPENDENCIES, true);
		if (appos.size() > 0) return true;
		return (adjacentNPsWithSemantics(a,b) || syntacticAppositiveFromTree(a,b));
	}
	
	private static boolean adjacentNPsWithSemantics(SurfaceElement a, SurfaceElement b) {
		if (a.isNominal() == false || b.isNominal() == false) return false;
		if (a.hasSemantics() == false || b.hasSemantics() == false) return false;
		Document doc = a.getSentence().getDocument();
		List<SurfaceElement> intervening = doc.getInterveningSurfaceElements(a, b);
		return (intervening.size() == 0);
	}
	
	/**
	 * Determines whether a textual unit is involved in an appositive indicating syntactic dependency.
	 * 
	 * @param surf	the textual unit
	 * @return		true if there is an appositive dependency involving the textual unit
	 */
	public static boolean hasSyntacticAppositives(SurfaceElement surf) {
		return (SynDependency.dependenciesWithTypes(surf, surf.getSentence().getEmbeddings(), 
				SynDependency.APPOS_DEPENDENCIES, true).size() > 0);
	}
	
	/**
	 * Determines whether a textual unit is involved in an exemplification indicating syntactic dependency.
	 * 
	 * @param surf	the textual unit
	 * @return		true if there is an exemplification dependency involving the textual unit
	 */
	public static boolean hasSyntacticExemplifications(SurfaceElement surf) {
		return (SynDependency.dependenciesWithTypes(surf, surf.getSentence().getEmbeddings(), 
				SynDependency.EXEMPLIFY_DEPENDENCIES, true).size() > 0);
	}
	
	/**
	 * Identifies the textual units that are in an appositive construction with the textual unit <var>surf</var>. <p>
	 * It relies only on dependency relations.
	 * 
	 * @param surf	a textual unit
	 * @return		the set of textual units in an appositive construction with 
	 */
	public static Set<SurfaceElement> identifySyntacticAppositives(SurfaceElement surf) {
		List<SynDependency> appos = SynDependency.dependenciesWithTypes(surf, surf.getSentence().getEmbeddings(), 
						SynDependency.APPOS_DEPENDENCIES, true);
		Set<SurfaceElement> allAppos = new HashSet<SurfaceElement>();
		if (appos.size() > 0) {
			for (SynDependency a: appos) {
				SurfaceElement dep = a.getDependent();
				SurfaceElement gov = a.getGovernor();
				SurfaceElement app = dep;
				if (dep.equals(surf)) app = gov;
				allAppos.add(app);
			}	
		}	
		return allAppos;
	}
	
	/**
	 * Identifies semantic items that are in an appositive construction with the semantic item <var>si</var>.<p>
	 * if <var>si</var> is a complex semantic item with a predicate, the predicate is taken as the basis
	 * for recognizing appositives. If it's a relation without a predicate, it returns an empty set. 
	 * The semantic items returned are semantically compatible with <var>si</var>.
	 * 
	 * @param si	a semantic item
	 * @return		the semantic items in an appositive construction with <var>si</var> or its predicate, empty set otherwise
	 */
	public static Set<SemanticItem> identifyAppositives(SemanticItem si) {
		Set<SemanticItem> allAppos = new HashSet<SemanticItem>();
		Term tsi = null;
		if (si instanceof Term) {
			tsi = (Term)si;
		} else if (si instanceof HasPredicate) {
			tsi = ((HasPredicate)si).getPredicate();
		} else return allAppos;
		SurfaceElement su = tsi.getSurfaceElement();
		Set<SurfaceElement> synAppos = identifySyntacticAppositives(su);
		if (synAppos.size() > 0) {
			for (SurfaceElement app: synAppos) {
				allAppos.addAll(getConsonantSemanticItems(tsi,app));
			}
		}
		return allAppos;
	}
	
	/**
	 * Determines whether two textual units are appositives, using the parse tree.
	 * 
	 * @param surf1	the first textual unit
	 * @param surf2	the second textual unit
	 * @return	true if the textual units are in an appositive construction
	 */
	public static boolean syntacticAppositiveFromTree(SurfaceElement surf1, SurfaceElement surf2) {
		Sentence sent = surf1.getSentence();
		if (sent.equals(surf2.getSentence()) == false) return false;
		Tree sentTree = sent.getTree();
		Tree at = ParseTreeUtils.getCorrespondingSubTree(sentTree, surf1);
		Tree bt = ParseTreeUtils.getCorrespondingSubTree(sentTree, surf2);
		Tree app = appositiveConstructionFromTree(at,sentTree);
		if (app == null) return false;
		if (app.equals(bt)) return true;
		return false;
	}
	
	// TODO: I think this needs more work.
	private static Tree appositiveConstructionFromTree(Tree t, Tree root) {
		if (t==null) return null;
		Tree parent = t.parent(root);
		
		if(parent.numChildren()<3){
			return null;
		}else if(!parent.getChild(1).label().value().equals(",")){
			return null;
		} else if (!parent.getChild(2).label().value().equals("NP")) {
			return null;
		}
		for(Tree sibling: parent.getChildrenAsList()){
			if(sibling.label().value().equals("CC")){
				return null;
			}
		}
		return parent.getChild(2);
	}
	
	/**
	 * Identifies semantic items that are in an exemplification relation with the semantic item <var>si</var>.<p>
	 * It uses syntactic dependencies as well as set phrase <i>e.g.</i> to identify exemplifications. 
	 * If <var>si</var> is exemplified, it returns all example instances; if it is an example, it returns all items
	 * that are exemplified by <var>si</var>. 
	 * The returned semantic items are semantically consonant with <var>si</var>.
	 * 
	 * @param si	a semantic item
	 * @return		the semantic items that are in an exemplification relation with <var>si</var> or its predicate, empty set otherwise
	 */
	public static Set<SemanticItem> identifyExemplifications(SemanticItem si) {
		Set<SemanticItem> allExemp = new HashSet<SemanticItem>();
		Term tsi = null;
		if (si instanceof Term) {
			tsi = (Term)si;
		} else if (si instanceof HasPredicate) {
			tsi = ((HasPredicate)si).getPredicate();
		} else return allExemp;
		SurfaceElement su = tsi.getSurfaceElement();
		List<SynDependency> exemp = SynDependency.dependenciesWithTypes(su, su.getSentence().getEmbeddings(), 
						SynDependency.EXEMPLIFY_DEPENDENCIES, true);
		if (exemp.size() > 0) {
			for (SynDependency a: exemp) {
				SurfaceElement dep = a.getDependent();
				SurfaceElement gov = a.getGovernor();
				SurfaceElement app = dep;
				if (dep.equals(su)) app = gov;
				allExemp.addAll(getConsonantSemanticItems(tsi,app));
			}
			// this might be applied in general, too
		} else {
			SurfaceElement eg = exempliGratia(su);
			if (eg != null) allExemp.addAll(getConsonantSemanticItems(tsi,eg));	
		}	
		return allExemp;
	}
	
	// given a semantic item t, find all semantic items associated with a textual unit su that are consonant
	// with t, i.e., semantic groups or types have overlap.
	// TODO Can be moved to SemUtils
	private static Set<SemanticItem> getConsonantSemanticItems(SemanticItem t, SurfaceElement su) {
		boolean term = true;
		Term tsi = null;
		Set<SemanticItem> allAppos = new HashSet<SemanticItem>();
		if (t instanceof Term) {
			tsi = (Term)t;
		} else if (t instanceof HasPredicate) {
			tsi = ((HasPredicate)t).getPredicate();
			term = false;
		} else return allAppos;
		if (term && su.filterByTerms() != null) {
			Set<SemanticItem> terms = su.filterByTerms();
			for (SemanticItem tt: terms) {
				if (SemUtils.matchingSemGroup(tt,tsi) == null) continue;
				allAppos.add(tt);
			}
		}
		if (!term && su.filterByRelations() != null)  {
			Set<SemanticItem> rels = su.filterByRelations();
			for (SemanticItem tt: rels) {
				if (SemUtils.matchingSemType(tt,tsi,false) == null) continue;
				allAppos.add(tt);
			}
		}
		return allAppos;
	}
	
	/**
	 * Determines whether a textual unit exemplifies some other textual unit. <p>
	 * Semantic consonance check between the exemplifier/example can be turned on/off.
	 * 
	 * @param surf	a textual unit
	 * @param semanticallyConsonant	whether semantic consonance should be considered
	 * 
	 * @return	true if <var>surf</var> exemplifies some textual unit
	 */
	public static boolean exemplifies(SurfaceElement surf, boolean semanticallyConsonant) {
		Sentence sent = surf.getSentence();
		for (SurfaceElement su: sent.getSurfaceElements()) {
			if (SpanList.atLeft(surf.getSpan(), su.getSpan())) continue;
			if (SpanList.atLeft(su.getSpan(), surf.getSpan()) && exemplifies(su,surf,semanticallyConsonant)) return true;
		}
		return false;
	}
	
	/**
	 * Determines whether a textual unit <var>a</var> exemplifies some other textual unit <var>b</var>. <p>
	 * Semantic consonance check between the exemplifier/example can be turned on/off.
	 * 
	 * @param a	a textual unit
	 * @param b another textual unit
	 * @param semanticallyConsonant	whether semantic consonance should be considered
	 * 
	 * @return	true if <var>a</var> exemplifies <var>b</var>
	 */
	public static boolean exemplifies(SurfaceElement a, SurfaceElement b, boolean semanticallyConsonant) {
		if (semanticallyConsonant) {
			boolean consonant = SemUtils.semTypeEquality(a, b, true);
			if (!consonant) return false;
		}
		if (exempliGratia(a,b)) return true;
		List<SynDependency> depsBetween = 
				SynDependency.findDependenciesBetween(a.getSentence().getEmbeddings(), a, b, false);
		if (depsBetween.size() == 0) return false;		
		List<SynDependency> appos = SynDependency.dependenciesWithTypes(depsBetween, 
				SynDependency.EXEMPLIFY_DEPENDENCIES, true);
		if (appos.size() == 0) return false;
		return true;
	}
	
	/**
	 * Determines whether there is an exempli gratia (<i>e.g.</i>) between two textual units.
	 * @param a	the first textual unit
	 * @param b	the second textual unit
	 * 
	 * @return true if an <i>(e.g.,</i> is found 
	 */
	public static boolean exempliGratia(SurfaceElement a, SurfaceElement b) {
		if (a.getSentence().equals(b.getSentence()) == false) return false;
		SurfaceElement ex = exempliGratia(a);
		if (ex == null) return false;
		if (ex.equals(b)) return true;
		LinkedHashSet<SurfaceElement> conjs = ConjunctionDetection.getConjuncts(b);
		if (conjs.contains(ex)) return true;
		LinkedHashSet<SurfaceElement> conjargs = ConjunctionDetection.getConjunctSurfaceElements(b);
		return (conjargs.contains(ex));
	}
	
	/**
	 * Finds the textual unit that <var>surf</var> is an in an <i>e.g.,</i> with. if any. <p>
	 * While not so ad hoc, it only works in specific cases, mostly seen biomedical literature.
	 * 
	 * @param surf	a textual unit
	 * @return	true if <var>surf</var> is exemplified with or exemplifies with e.g.
	 */
	public static SurfaceElement exempliGratia(SurfaceElement surf) {
		Sentence sentence = surf.getSentence();
		List<SurfaceElement> surfs = sentence.getSurfaceElements();
		int egInd = sentence.getText().indexOf("(e.g.,") + sentence.getSpan().getBegin();
		if ( egInd >= 0) {
			int suInd = surf.getIndex();
			List<SurfaceElement> intervene = sentence.getSurfaceElementsFromSpan(new Span(egInd,egInd+6));
			if (intervene == null) return null;
			int commaInd = intervene.get(intervene.size()-1).getIndex();
			int parenInd = intervene.get(0).getIndex();
			SurfaceElement other = null;
			if (suInd == parenInd -1 && surfs.size() > commaInd) other = surfs.get(commaInd);
			else if (suInd == commaInd +1 && parenInd >= 2) other = sentence.getSurfaceElements().get(parenInd-2);
			return other;
		}
		return null;
	}
}
