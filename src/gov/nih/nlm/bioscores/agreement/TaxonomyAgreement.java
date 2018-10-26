package gov.nih.nlm.bioscores.agreement;

import java.util.LinkedHashSet;
import java.util.Set;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.CoreferenceUtils;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.AbstractSurfaceElement;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.Concept;
import gov.nih.nlm.ling.sem.ConjunctionDetection;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.SemanticItem;

/** 
 * Agreement constraint based on taxonomic relations in ontology.
 * It applies to nominal coreference. Surprisingly, it was not found
 * useful, but that might have more to do with the limited nature of
 * the experiments. <p>
 * 
 * Agreement is predicted if the concept that the coreferential mention
 * maps to is an ancestor of the concept that the candidate referent 
 * maps to.
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO I simply used drug taxonomic relations from UMLS for experiments, which didn't prove useful. 
// TODO However, I believe this can be generalized to make it more useful.
public class TaxonomyAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement candidate) {
		return (hierarchical(candidate,exp));
	}
	
	private static boolean hierarchical(SurfaceElement descendant, SurfaceElement ancestor) {
		LinkedHashSet<SemanticItem> ae = AbstractSurfaceElement.filterSemanticsByClass(
				CoreferenceUtils.getNonCoreferentialHeadSemantics(descendant),Entity.class);
		LinkedHashSet<SemanticItem> be = AbstractSurfaceElement.filterSemanticsByClass(
				CoreferenceUtils.getNonCoreferentialHeadSemantics(ancestor),Entity.class);
		
		if (be.size() == 0) return false;
		// allow relation with one of the conjuncts
		if (ae.size() == 0) {
			LinkedHashSet<SemanticItem> conjs = ConjunctionDetection.getConjunctionArguments(descendant);
			if (conjs.size() == 0) return false;
			for (SemanticItem conj : conjs) {
				if (conj instanceof Entity == false || conj instanceof Expression) continue;
				if (((Entity)conj).getSurfaceElement().equals(descendant)) continue;
				if (hierarchical(((Entity)conj).getSurfaceElement(),ancestor)) return true;
			}
		}
		// check for all senses instead of only the active sense
		for (SemanticItem at: ae) {
			if (at instanceof Entity == false) continue;
			Entity att = (Entity)at;
			Set<Concept> aconcepts = att.getConcepts();
			if (aconcepts == null) return false;
			for (Concept ac : aconcepts) {
				String id1 = ac.getId();
				for (SemanticItem bt: be) {
					if (bt instanceof Entity == false) continue;
					Entity btt = (Entity)bt;
					Set<Concept> bconcepts = btt.getConcepts();
					if (bconcepts == null) return false;
					for (Concept bc: bconcepts) {
						String id2 = bc.getId();
//						if (Concept.hierarchical(id1, id2) || id1.equals(id2)) return true;	
						if (Concept.hierarchical(id1, id2)) return true;
					}
				}
			}
		}
		return false;
	}

}
