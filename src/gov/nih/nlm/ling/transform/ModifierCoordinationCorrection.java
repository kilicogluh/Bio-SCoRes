package gov.nih.nlm.ling.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.sem.SemanticItem;

/**
 * A <code>DependencyTransformation</code> implementation that attempts to fix
 * problems with modifier coordination-related dependencies, which are often misidentified by the Stanford Parser. <p>
 * This transformation does not have any prerequisites.
 * 
 * @author Halil Kilicoglu
 *
 */
public class ModifierCoordinationCorrection implements DependencyTransformation {
	private static Logger log = Logger.getLogger(ModifierCoordinationCorrection.class.getName());	

	private List<String> npInternalDependencies = null;
	
	public ModifierCoordinationCorrection() {}
	
	@Override
	public List<Class<? extends DependencyTransformation>> getPrerequisites() {
		return new ArrayList<Class<? extends DependencyTransformation>>();
	}
	
	public ModifierCoordinationCorrection(List<String> npDependencies) {
		npInternalDependencies = npDependencies;
	}
	
	public List<String> getNpInternalDependencies() {
		return npInternalDependencies;
	}
	
	/**
	 * Transforms a sentence by rearranging the dependencies involved in modifier coordination. <p>
	 * It requires that both modifiers in consideration have semantic objects of the same class, i.e, both predicates or entities.
	 * For example, dependencies for <i>adenylate cyclase and p70(S6)-kinase activation</i> are re-arranged as follows: <ul>
	 * <li> <i>conj and(adenylate cyclase,activation), amod(activation,p70(S6)-kinase) </i>  to
	 * <li> <i>amod(activation,and), cc(and,adenylate cyclase), cc(and,p70(S6)-kinase) </i>
	 * </ul>
	 * It checks <var>npInternalDependencies</var> member variable for the list of NP-internal dependencies to use.
	 * If null, it uses the default list defined in {@link SynDependency}.
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
			log.log(Level.WARNING,"The prerequisites for ModifierCoordinationCorrection are not met for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		if (npInternalDependencies == null) npInternalDependencies = SynDependency.NP_INTERNAL_DEPENDENCIES;
		List<SynDependency> embeddings = sent.getEmbeddings();
		List<SynDependency> add = new ArrayList<>();
		List<SynDependency> remove = new ArrayList<>();	
		for (SynDependency sd: embeddings) {
			// with conj dependencies, gov is the token on the left
			SurfaceElement gov = sd.getGovernor();
			SurfaceElement dep = sd.getDependent();
			if (sd.getType().startsWith("conj_")) {
				Set<SemanticItem> govEntities= gov.filterByEntities();
				Set<SemanticItem> depEntities = dep.filterByEntities();
				Set<SemanticItem> govPreds= gov.filterByPredicates();
				Set<SemanticItem> depPreds = dep.filterByPredicates();
				if ((govEntities.size() > 0 && depEntities.size() >0) || (govPreds.size() >0 && depPreds.size() >0)) continue;
				
				boolean entityConj = govEntities.size() > 0 && depEntities.size() ==0;
				boolean predConj = govPreds.size() > 0 && depPreds.size() ==0;
				if (entityConj || predConj) {
					List<SynDependency> nnDeps = SynDependency.dependenciesWithTypes(dep, embeddings, npInternalDependencies, true);
					if (nnDeps.size() == 0) continue;
					for (SynDependency dd: nnDeps) {
						// dep must be head (activation)
						if (dd.getDependent().equals(dep)) continue;
						SurfaceElement possibleConjunct = dd.getDependent();
						if ( possibleConjunct.getIndex() < dep.getIndex() && possibleConjunct.getIndex() > gov.getIndex() &&
							( (entityConj && possibleConjunct.filterByEntities().size() > 0) ||
							  (predConj && possibleConjunct.filterByPredicates().size() > 0)))	{
							// the original sd is incorrect. remove
							SurfaceElement anc = SynDependency.getCommonAncestor(embeddings, Arrays.asList(gov,dep));
							if (anc != null) {
								List<SynDependency> between = SynDependency.findDependenciesBetween(embeddings, anc, gov, true);
								for (SynDependency b: between) {
									log.log(Level.FINE, "Removing dependency due to modifier coordination correction: {0}.", new Object[]{b.toString()});
									remove.add(b);
								}
							}
							remove.add(sd);
							log.log(Level.FINE, "Removing dependency due to modifier coordination correction: {0}.", new Object[]{sd.toString()});
							SynDependency nsd = new SynDependency("MCO_" + sd.getId(),dd.getType(),dep,gov);
							SynDependency nsd1 = new SynDependency("MCO1_" + sd.getId(),sd.getType(),gov,possibleConjunct);
							add.add(nsd); 
							log.log(Level.FINE, "Adding dependency due to modifier coordination correction: {0}.", new Object[]{nsd.toString()});
							add.add(nsd1);
							log.log(Level.FINE, "Adding dependency due to modifier coordination correction: {0}.", new Object[]{nsd1.toString()});
						}
					}
					// Remove other conj dependencies that involve gov or dep
					List<SynDependency> depConjs = SynDependency.dependenciesWithType(dep, embeddings, sd.getType(), true);
					List<SynDependency> govConjs = SynDependency.dependenciesWithType(gov, embeddings, sd.getType(), true);
					for (SynDependency gc: govConjs) {
						SurfaceElement other = ( gc.getDependent().equals(gov) ? gc.getGovernor() : gc.getDependent());
						for (SynDependency dc: depConjs) {
							SurfaceElement otherDep = ( dc.getDependent().equals(dep) ? dc.getGovernor() : dc.getDependent());
							if (other.equals(otherDep)) {
								remove.add(gc);
								log.log(Level.FINE, "Removing dependency due to modifier coordination correction: {0}.", new Object[]{gc.toString()});
							}
						}
					}
					
				}
			} 
		}
		sent.synchEmbeddings(remove, add);
		sent.addTransformation(this.getClass());
	}
}
