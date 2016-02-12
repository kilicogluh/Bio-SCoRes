package gov.nih.nlm.ling.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.core.Word;

/**
 * A <code>DependencyTransformation</code> implementation that transforms a <code>Sentence</code> object
 * by reversing the direction of dependencies, if necessary, to capture semantic dependencies for the purpose
 * of bottom-up traversal and semantic composition. It does not have any prerequisites. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class DependencyDirectionReversal implements DependencyTransformation {
	private static Logger log = Logger.getLogger(DependencyDirectionReversal.class.getName());

	private static final List<String> NEG_DETERMINERS = Arrays.asList("no");
	
	@Override
	public List<Class<? extends DependencyTransformation>> getPrerequisites() {
		return new ArrayList<Class<? extends DependencyTransformation>>();
	}

	/**
	 * Transforms a sentence by reversing the direction of certain dependencies.<p>
	 * These dependencies include those dominated by the dependent item (such as <i>partmod</i>, <i>mark</i>, etc.)
	 * <i>advmod</i> when the adverb has semantic content (e.g., serves as a discourse connective). <p>
	 * <i>advmod</i> rule should really only apply when the adverb is non-restricting, but difficult to 
	 * determine this from syntactic dependencies, so we limit it to those with semantic content, which are
	 * likely to be non-restricting.
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
			log.log(Level.WARNING,"The prerequisites for DependencyDirectionReversal are not met for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		List<SynDependency> embeddings = sent.getEmbeddings();
		List<SynDependency> outEmbeddings = new ArrayList<>();
		for (SynDependency d: embeddings) {
			String type = d.getType();
			String parentType = type;
			if ((type.startsWith("prep") || type.startsWith("conj")) && type.indexOf('_') >= 0) 
				parentType = type.substring(0,type.indexOf('_'));
			SurfaceElement gov = d.getGovernor();
			SurfaceElement dep = d.getDependent();
			if (SynDependency.DEPENDENT_DOMINATED_DEPENDENCIES.contains(parentType) ||
				(parentType.equals("det") && dep instanceof Word  && NEG_DETERMINERS.contains(((Word)dep).getLemma())) ||
				(parentType.equals("advmod") && dep.hasSemantics())) {
				SynDependency nD = new SynDependency("DDR_" + d.getId(), d.getType(),dep,gov);
				outEmbeddings.add(nD);
				log.log(Level.FINE,"Adding new dependency due to DependencyDirectionReversal: {0}.", new Object[]{nD.toString()});
			} 
			else 
				outEmbeddings.add(d);
		}
		sent.setEmbeddings(outEmbeddings);
		sent.addTransformation(this.getClass());
	}

}
