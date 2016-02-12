package gov.nih.nlm.ling.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.nlp.util.CollectionUtils;
import gov.nih.nlm.ling.composition.EmbeddingCategorization;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.SemanticItem;

/**
 * A <code>DependencyTransformation</code> implementation that composes  the meanings 
 * contributed by two textual units with respect to polarity. It assumes that a 
 * polarity-shifting predicate (such as <i>negatively</i>, <i>positive</i>, etc.) has in its 
 * scope a causal predicate. <p>
 * It collapses these textual units and their predicate semantics
 * to provide more specificity. For example, in the phrase <i>negative regulation</i>, <i>negative</i>
 * is a NEGATIVE_SHIFT predicate and <i>regulation</i> is a CAUSAL predicate, <i>negative</i> has 
 * scope over <i>regulation</i>. This transformation creates a single textual unit <i>negative regulation</i>
 * with the predicate type PREVENT.
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO Since this is a composition operation, I am not sure it is appropriate as a dependency transformation.
public class PolarityComposition implements DependencyTransformation {
	private static Logger log = Logger.getLogger(PolarityComposition.class.getName());
	
	private static final List<String> POLARITY_SHIFT_TYPES = new ArrayList<String>(EmbeddingCategorization.getAllDescendants("POLARITY_SHIFTER"));
	private static final Set<String> CAUSAL_TYPES = EmbeddingCategorization.getAllDescendants("CAUSAL");
	// TODO Not sure this covers it all.
	private static final Collection<String> SCOPE_TYPES = CollectionUtils.union(
			SynDependency.NP_INTERNAL_DEPENDENCIES, Arrays.asList("AUX","ADVMOD"));

	@Override
	public List<Class<? extends DependencyTransformation>> getPrerequisites() {
		List<Class<? extends DependencyTransformation>> prereqs = 
				new ArrayList<Class<? extends DependencyTransformation>>();
		prereqs.add(DependencyDirectionReversal.class);
		return prereqs;
	}
	
	/**
	 * Merges the meaning of a polarity shifter with a causal predicate in its scope.
	 * 
	 * @param sent  the sentence to transform
	 */
	public void transform(Sentence sent) {
		if (sent.getSurfaceElements() == null || sent.getEmbeddings() == null) {
			log.log(Level.WARNING,"No textual units or embeddings can be found for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		List<Class<? extends DependencyTransformation>> applied  = sent.getTransformations();
		if (applied.containsAll(getPrerequisites()) == false) {
			log.log(Level.WARNING,"The prerequisites for PolarityComposition are not met for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		List<SynDependency> embeddings = sent.getEmbeddings();
		Document doc = sent.getDocument();
		Predicate remove = null;
		SpanList nsp = null;
		for (SurfaceElement su: surfs) {
			LinkedHashSet<SemanticItem> polarShifters= 
					Document.getSemanticItemsByClassTypeSpan(doc, Predicate.class, POLARITY_SHIFT_TYPES, su.getSpan(), false);
			// TODO Not sure what to do when there are multiple such items, should probably not happen anyway
			if (polarShifters.size() != 1) continue;
			Predicate ps = (Predicate)polarShifters.iterator().next();
			List<SynDependency> outgoing = SynDependency.outDependencies(su, embeddings);
			for (SynDependency sd: outgoing) {
				if (SCOPE_TYPES.contains(sd.getType()) == false) continue;
				SurfaceElement target = sd.getDependent();
				LinkedHashSet<SemanticItem> causals= 
						Document.getSemanticItemsByClassTypeSpan(doc, Predicate.class, Arrays.asList("CAUSAL"), target.getSpan(), false);
				if (causals.size() == 0) continue;
				remove = ps;
				for (SemanticItem c: causals) {
					Predicate pc = (Predicate)c;
					String ntype = updatedType(ps.getType(),pc.getType());
					if (ntype == null) continue;
					if (nsp == null) {
						nsp = SpanList.union(su.getSpan(),target.getSpan());
						// TODO this presumably will collapse the two surface elements and rearrange their dependencies, but need to test
						SurfaceElement nsu = sent.getDocument().getSurfaceElementFactory().createSurfaceElementIfNecessary(doc,nsp, true);
						sent.synchSurfaceElements(nsu);
						sent.synchEmbeddings();
						log.log(Level.FINE,"Adding new textual unit due to polarity composition: {0}.", new Object[]{nsu.toString()});
					}
					pc.setSpan(nsp);
					pc.setType(ntype);
				}
			}
			log.log(Level.FINE,"Removing semantic item due to polarity composition: {0}.", new Object[]{remove.toString()});
			doc.removeSemanticItem(remove);
		}
		sent.addTransformation(this.getClass());
	}	
	
	private static String updatedType(String parentType, String childType) {
		if (childType.equals("CAUSAL")) {
			if (parentType.equals("POSITIVE_SHIFT")) {
				return "CAUSE";
			} else if (parentType.equals("NEGATIVE_SHIFT")) {
				return "PREVENT";
			} 
		} else if (parentType.equals("NEUTRALIZE")) {
			if (CAUSAL_TYPES.contains(childType)) {
				return "CAUSAL";
			}
		}
		return null;
	}

}
