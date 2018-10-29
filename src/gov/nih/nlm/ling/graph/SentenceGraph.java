package gov.nih.nlm.ling.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;

/**
 * Represents a sentence-bound portion of a <code>DocumentGraph</code>.
 * It is generated from the textual units of a sentence and its dependencies.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SentenceGraph extends DirectedAcyclicGraph<Node,Edge>{
	private static final long serialVersionUID = 6559341509836786217L;

	private static Logger logger = Logger.getLogger(SentenceGraph.class.getName());

	private Sentence sentence;
	
	/**
	 * Instantiates a <code>SentenceGraph</code> from the textual units 
	 * and the dependencies between them in a sentence.
	 * 
	 * @param s  the sentence associated with the graph
	 */
	public SentenceGraph(Sentence s) {
		super(Edge.class);
		List<Node> seen = new ArrayList<Node>();
		List<SynDependency> dependencies = s.getEmbeddings();
		for (Edge d: dependencies) {
			Node governor = d.getGovernor();
			Node dependent = d.getDependent();
			if (governor.equals(dependent)) continue;
			if (seen.contains(governor) == false) this.addVertex(governor);
			if (seen.contains(dependent) == false) this.addVertex(dependent);
			if (SynDependency.findDependencyPath(
					dependencies, (SurfaceElement)dependent, (SurfaceElement)governor, true) != null) 
				continue;
			try {
				if (this.getEdge(dependent,governor) == null) 
					this.addDagEdge(governor, dependent,d);
			} catch (CycleFoundException cf) {
				logger.log(Level.WARNING,"Cycle detected when trying to add an edge, not adding {0}." + d.toString());
			}
			seen.add(governor);
			seen.add(dependent);
		}
		for (Node n: s.getSurfaceElements()) {
			if (seen.contains(n)) continue;
			this.addVertex(n);
		}

		this.sentence = s;
	} 

	public Sentence getSentence() {
		return sentence;
	}

}
