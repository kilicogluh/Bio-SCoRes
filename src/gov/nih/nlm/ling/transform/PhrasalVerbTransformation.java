package gov.nih.nlm.ling.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;

/**
 * A <code>DependencyTransformation</code> implementation that transforms a <code>Sentence</code> object
 * by collapsing the phrasal verbs (verb + particles).
 * 
 * @author Halil Kilicoglu
 *
 */
public class PhrasalVerbTransformation implements DependencyTransformation {
	private static Logger log = Logger.getLogger(PhrasalVerbTransformation.class.getName());
	
	@Override
	public List<Class<? extends DependencyTransformation>> getPrerequisites() {
		return new ArrayList<Class<? extends DependencyTransformation>>();
	}

	/**
	 * Collapses a verb and its particles if any. <p>
	 * It may create <code>GappedMultiWord</code> objects as a result.
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
			log.log(Level.WARNING,"The prerequisites for PhrasalVerbTransformation are not met for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		List<SynDependency> embeddings = sent.getEmbeddings();
		Document doc = sent.getDocument();
		for (SurfaceElement su: sent.getSurfaceElements()) {
			if (su.isVerbal()) {
				List<SynDependency> outDeps = SynDependency.outDependenciesWithType(su, embeddings, "prt", true);
				if (outDeps.size() == 0) continue;
				SurfaceElement prtObj= outDeps.get(0).getDependent();
				if (prtObj == null) continue;
				SpanList ns = SpanList.union(su.getSpan(), prtObj.getSpan());
				SurfaceElement rep = doc.getSurfaceElementFactory().createSurfaceElementIfNecessary(doc, ns, true);
				sent.synchSurfaceElements(rep);	
				log.log(Level.FINE,"Adding new textual unit due to phrasal verb transformation: {0}.", new Object[]{rep.toString()});
			}
		}
		sent.synchEmbeddings();
		sent.addTransformation(this.getClass());
	}

}
