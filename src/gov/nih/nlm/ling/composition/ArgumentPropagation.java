package gov.nih.nlm.ling.composition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.graph.Edge;
import gov.nih.nlm.ling.graph.Node;
import gov.nih.nlm.ling.sem.AbstractRelation;
import gov.nih.nlm.ling.sem.Conjunction;
import gov.nih.nlm.ling.sem.SemanticItem;

/**
 * A class that includes static methods that determine whether and how to propagate semantic objects
 * that appear further down the embedding graph (and not as immediate children) as arguments for a given
 * predicate. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class ArgumentPropagation {
	private static Logger log = Logger.getLogger(ArgumentPropagation.class.getName());	

	/**
	 * Propagates arguments up the embedding graph, if necessary.
	 * 
	 * @param node			the node in the embedding graph whose arguments are being searched for
	 * @param argStructure	a <code>Map</code> of children of <var>node</var> and the labels that link them to the <var>node</var>
	 * @param composedItems	the set of composed relations so far
	 * @param children		descendant semantics to propagate
	 */
	// TODO This needs more work.
	public static void propagate(Node node, Map<Edge,LinkedHashSet<SemanticItem>> argStructure,
			LinkedHashSet<SemanticItem> composedItems, LinkedHashSet<SemanticItem> children) {
		// if we generated some relations using this node, the semantic items from the child nodes can be ignored
		if (composedItems.size() > 0) {
			log.log(Level.FINE, "There are already composed relations associated with the node {0}, will propagate these relations.", new Object[]{node.getId()});
			children.clear();
			for (SemanticItem si: composedItems) {
				if (si instanceof Conjunction) children.addAll(((Conjunction)si).getArgItems());
				else children.add(si);
			}
			// TODO this will propagate everything, which is probably not what I want
			// this includes child semantics as well, we might want to ignore them?
		} else if (node.getSemantics() != null && node.getSemantics().size() > 0) {
			// Examples to look at include E96, E129 in PMC-2806624-07
			boolean unfulfilledRel = false;
			if (node instanceof SurfaceElement) {
				SurfaceElement sN = (SurfaceElement)node;
				//unfulfilled relations?
				if (sN.filterByPredicates().size() > 0 && sN.filterByRelations().size() == 0)  unfulfilledRel = true;
			}
			if (!unfulfilledRel) {
				log.log(Level.FINE, "The node {0} has resolved relations.", new Object[]{node.getId()});
				children.clear();
				// see if the lower level element is in fact a super-element of the current node 
				// example GENIA 10064103: 'oxidative stress dependent NF-kappaB activation is evident in patients' 
				// due to the problem in parsing dependent, oxidative stress -> activation -> NF-kappa B.
				for (Edge e: argStructure.keySet()) {
					LinkedHashSet<SemanticItem> eArgs = argStructure.get(e);
					for (SemanticItem sem: eArgs) {
						if (sem instanceof AbstractRelation) {
							AbstractRelation r = (AbstractRelation)sem;
							for (SemanticItem nsem : node.getSemantics()) {
								if (r.getArgItems().contains(nsem)) children.add(r);
							}
						}
					}
				} 
				if (children.size() ==0) {
					log.log(Level.FINE, "The node {0} has children semantics, will propagate its own semantics only.", new Object[]{node.getId()});
					children.addAll(node.getSemantics());
				}
			}
		} else {
			// Blocks the propagation of SUBJ arguments
			children.clear();
			for (Edge e: argStructure.keySet()) {
				if (node instanceof SurfaceElement) {
					SurfaceElement sN = (SurfaceElement)node;
					String type = e.getType();
					List<String> subjTypes = DependencyClass.filterByDepClass(DependencyClass.loadDepClasses(), sN.getCategory(), "SUBJ");
					// argStructure.size() == 1 is when ellipsis occurs I think, //treatment with aspirin, for example.
					// So that part might need to be done separately
					if (subjTypes.contains(type) == false || argStructure.size() == 1) {
						log.log(Level.FINE, "The node {0} will propagate its children semantics.", new Object[]{node.getId()});
						children.addAll(argStructure.get(e));
					}
				} else {
					log.log(Level.FINE, "The node {0} will propagate its children semantics.", new Object[]{node.getId()});
					children.addAll(argStructure.get(e));
				}
			}
		}
	}

	

}
