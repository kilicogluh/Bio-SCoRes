package gov.nih.nlm.ling.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.AbstractSurfaceElement;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.sem.Concept;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Predicate;

/**
 * A <code>DependencyTransformation</code> implementation that associates semantically free textual units 
 * with generic semantic objects. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class GenericSemanticsBinding implements DependencyTransformation {
	private static Logger log = Logger.getLogger(GenericSemanticsBinding.class.getName());
	
	@Override
	public List<Class<? extends DependencyTransformation>> getPrerequisites() {
		return new ArrayList<Class<? extends DependencyTransformation>>();
	}

	/**
	 * Transforms a sentence by binding generic semantic objects to certain textual units.<p>
	 * Semantically free noun leaf nodes (not nominalization or gerund) are bound to a generic entity object.
	 * Nominalizations, gerunds, non-leaf verb, adjective and adverbs are bound to generic predicate objects.
	 * 
	 * @param  sent  the sentence to transform
	 */
	public synchronized void transform(Sentence sent) {
		if (sent.getSurfaceElements() == null || sent.getEmbeddings() == null) {
			log.log(Level.WARNING,"No textual units or embeddings can be found for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		List<Class<? extends DependencyTransformation>> applied  = sent.getTransformations();
		if (applied.containsAll(getPrerequisites()) == false) {
			log.log(Level.WARNING,"The prerequisites for GenericSemanticsBinding are not met for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		List<SurfaceElement> surfs= sent.getSurfaceElements();
		Document d = sent.getDocument();
		for (SurfaceElement su: surfs) {
			if (su.getSemantics() != null && su.getSemantics().size() > 0) continue;
			if ((((AbstractSurfaceElement)su).isNominalization() || ((AbstractSurfaceElement)su).isGerund()) ||
				((su.isAdjectival() || su.isAdverbial()) && 
						SynDependency.outDependencies(su,sent.getEmbeddings()) != null) ||
				su.isVerbal()) {
			   	Predicate pr = d.getSemanticItemFactory().newPredicate(d,su.getSpan(),su.getHead().getSpan(),Predicate.GENERIC);
				if (pr != null) su.addSemantics(pr);
			}
		}
		for (SurfaceElement su: surfs) {
			if (su.getSemantics() != null && su.getSemantics().size() > 0) continue;
			if (su.isNominal()) {
				Entity ent = d.getSemanticItemFactory().newGenericEntity(d, su.getSpan(), su.getHead().getSpan(),Concept.GENERIC);
				if (ent != null) su.addSemantics(ent);
			}
		}
		sent.addTransformation(this.getClass());
	}
}
