package gov.nih.nlm.bioscores.agreement;

import java.util.LinkedHashSet;
import java.util.Set;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.ConjunctionDetection;
import gov.nih.nlm.ling.sem.Ontology;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;

/**
 * Agreement implementation that mainly applies to indefinite NPs. We assume that 
 * an indefinite NP mention cannot refer to the topic of the document. 
 * For all other types of mention, agreement is predicted. <p>
 * This agreement measure is currently unused.
 *
 * @author Halil Kilicoglu
 *
 */
// TODO It needs more work to be useful.
public class DocumentTopicAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		if (expType != ExpressionType.IndefiniteNP) return true;
		Set<SemanticItem> conjs = ConjunctionDetection.filterByConjunctions(referent);
		if (conjs.size() > 0) {
			LinkedHashSet<SurfaceElement> surfs = ConjunctionDetection.getConjunctSurfaceElements(referent);
			for (SurfaceElement su: surfs) {
				if (hasConceptTopic(su) || hasStringTopic(su)) return false;
			}
			return true;
		} 
		return (hasConceptTopic(referent) == false && hasStringTopic(referent) == false);
	}
	
	private boolean hasConceptTopic(SurfaceElement surf) {
		Document doc = surf.getSentence().getDocument();
		LinkedHashSet<SemanticItem> sems = surf.getSemantics();
		if (sems == null) return false;
		for (SemanticItem sem: sems) {
			Ontology ont = sem.getOntology();
			if (doc.getTopics() != null && doc.getTopics().contains(ont)) return true;
		}
		return false;
	}

	private boolean hasStringTopic(SurfaceElement surf) {
		Document doc = surf.getSentence().getDocument();
		LinkedHashSet<SemanticItem> sems = surf.getSemantics();
		if (sems == null) return false;
		for (SemanticItem sem: sems) {
			if (sem instanceof Term == false) continue;
			Term term = (Term)sem;
			if (doc.getTopics() != null && doc.getTopics().contains(term.getText().toUpperCase())) return true;
		}
		return false;
	}
	
}
