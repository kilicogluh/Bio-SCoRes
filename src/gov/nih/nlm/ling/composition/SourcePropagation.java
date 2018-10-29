package gov.nih.nlm.ling.composition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Predication;
import gov.nih.nlm.ling.sem.SemanticItem;

/**
 * This class contains a static method to propagate source values of predications.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SourcePropagation {
	private static Logger log = Logger.getLogger(SourcePropagation.class.getName());	
	
	/**
	 * Finds the source of a predication and propagates it to other predications
	 * in its scope
	 * 
	 * @param predication  the predication under consideration
	 */
	public static void propagateSource(Predication predication) {
		if (EmbeddingCategorization.isEpistemicScalar(predication.getPredicate())) {
			List<SemanticItem> sources = predication.getSources();
			log.log(Level.FINEST,"Predication {0} has {1} sources.", new Object[]{predication.getId(),sources.size()});
			getSourceInfluence(predication,sources, 0);
		}
	}
	
	// TODO This heuristic is likely not sufficient.
	private static boolean introducingSource(Predication pred) {
		List<SemanticItem> sources = Argument.getArgItems(pred.getArgs("SUBJ"));
		return (sources != null || 
				pred.getPredicate().getSurfaceElement().getHead().getPos().equals("VBN"));
	}
	
	private static void getSourceInfluence(Predication current, final List<SemanticItem> sources, int level) {
		if (EmbeddingCategorization.isEpistemicScalar(current.getPredicate()) == false) {
			log.log(Level.FINEST, "Predication {0} is not epistemic scalar.", new Object[]{current.getId()});
			return;
		}
		Set<SemanticItem> scopeArgs = getScopalSourceArguments(current,level);
		if (scopeArgs.size() == 0) {
			log.log(Level.FINEST, "The predication {0} does not have any predications in scope. Not propagating source.", new Object[]{current.getId()});
			return;
		}
		for (SemanticItem sa: scopeArgs) {
			log.log(Level.FINEST, "The predication {0} has semantic item {1} in its scope at distance {2}. Will propagate source.", new Object[]{current.getId(),sa.getId(),level});
		}
		Iterator<SemanticItem> sIter = scopeArgs.iterator();
		level++;
		while (sIter.hasNext())  {
			SemanticItem so = sIter.next();
			if (so instanceof Predication) {
				Predication child = (Predication)so;
				List<SemanticItem> childArgs = child.getArgItems();
				if (org.apache.commons.collections15.CollectionUtils.intersection(sources, childArgs).size() == 0) {
					if (EmbeddingCategorization.isEpistemicScalar(child.getPredicate()) && introducingSource(child)) {
						log.log(Level.FINEST,"Encountered another epistemic scalar predication {0} in scope of predication {1}. Ending propagation.", 
								new Object[]{child.getId(), current.getId()});
						continue;
					} else {
						if (sources.contains(child)) {
							List<SemanticItem> nSources = new ArrayList<>(sources);
							nSources.remove(child);
							if (nSources.size() == 0) {
								nSources.add(Entity.SOURCE_WRITER);
							}
							child.setSources(nSources);
						} else child.setSources(sources);
						getSourceInfluence(child,sources,level);
					}
				} else {
					log.log(Level.FINEST,"Common elements between sources of the predication {0} and arguments of the predication in scope {1}. Skipping propagation.",
							new Object[]{current.getId(),child.getId()});
				}
			}
		}
	}
	
	private static Set<SemanticItem> getScopalSourceArguments(Predication pred, int level) {
		if (pred.getArguments() == null) return new HashSet<>();
		List<SemanticItem> children = pred.getArgItems();
		List<String> argNames = pred.getArgNames();
		Set<SemanticItem> out = new HashSet<>();
		for (int i=0; i < children.size(); i++) {
			SemanticItem so = children.get(i);
			if (so instanceof Predication) {
				Predication child = (Predication)so;
				if (level > 0 || Arrays.asList(ArgumentRule.SCOPE_ARGUMENT_TYPES).contains(argNames.get(i))) 
					out.add(child);
			}
		}
		return out;
	}
}
