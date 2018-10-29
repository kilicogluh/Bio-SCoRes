package gov.nih.nlm.ling.graph;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.HasPredicate;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;

/**
 * Represents the semantic content of a <code>Document</code> as a directed graph
 * where the nodes are <code>Term</code> objects and the edges between the nodes
 * represent roles. <p>
 * A <code>SemanticGraph</code> is constructed as a result of bottom-up semantic composition.
 * However, a cleanup, constraint-checking is still probably necessary after it's built up.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SemanticGraph extends DirectedAcyclicGraph<SemanticItem,String> {
	private static final long serialVersionUID = -3029070503755457048L;

	private static Logger logger = Logger.getLogger(SemanticGraph.class.getName());

	private Document document;

	/**
	 * Instantiates an empty <code>SemanticGraph</code> object associated 
	 * with a <code>Document</code>.
	 * 
	 * @param doc  the corresponding document
	 */
	public SemanticGraph(Document doc) {
		super(String.class);
		this.document = doc;
	} 

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document doc) {
		this.document = doc;
	}
	
	/**
	 * Adds the semantic item <var>sem</var> to the graph. If the <var>parent</var>
	 * is null, <var>sem</var> is only added to the graph, without linking it to 
	 * any other semantic item. Otherwise, an edge is created between the parent
	 * and the sem. The id of the edge has the form <var>role</var>:pId:sId, 
	 * where pId is the id of the parent and sId is the id of the sem.
	 * 
	 * @param parent  the parent of the semantic object, can be null
	 * @param sem  the semantic object to add
	 * @param role  the relation type between the parent and the object, can be an empty string
	 */
	public void addSemanticNode(SemanticItem parent, SemanticItem sem, String role) {
		if (sem == null) 
			throw new NullPointerException("Cannot add a null SemanticItem to a SemanticGraph.");
		if (sem instanceof Term) {
			if (containsVertex(sem) == false) addVertex(sem);
			if (parent != null) 
				addEdge(parent,sem,role + ":" + parent.getId() + ":" + sem.getId());
		}
		else if (sem instanceof Relation) {
			if (sem instanceof HasPredicate) {
				Predicate pr = ((HasPredicate)sem).getPredicate();
				if (containsVertex(pr) == false) addVertex(pr);
				// avoid loops
				if (parent != null && parent.equals(pr) == false) {
					try {
						addEdge(parent,pr,role + ":" + parent.getId() + ":" + pr.getId());
					} catch (RuntimeException e) {
						logger.log(Level.WARNING, "Runtime exception when creating an edge from {0} to {1}: {2}", 
								new Object[]{parent.getId(),pr.getId(),e.getMessage()});
					}
				}
				for (Argument arg: ((Relation) sem).getArguments()) {
					SemanticItem argItem = arg.getArg();
					addSemanticNode(pr,argItem,arg.getType());
				}
			} 
			else {
				List<Argument> args = ((Relation) sem).getArguments();
				// TODO need to find a better way to handle relations without an explicit predicate
				// TODO Currently, we just link all the rest of the arguments to the first one.
				Argument first = args.get(0);
				if (containsVertex(first.getArg()) == false) addVertex(first.getArg());
				// avoid loops
				if (parent != null && parent.equals(first.getArg()) == false) {
					try {
						addEdge(parent,first.getArg(),role + ":" + parent.getId() + ":" + first.getArg().getId());
					} catch (RuntimeException e) {
						logger.log(Level.WARNING, "Runtime exception when creating an edge from {0} to {1}: {2}", 
								new Object[]{parent.getId(),first.getArg().getId(),e.getMessage()});
					}
				}
				for (int i=1; i < args.size(); i++) {
					SemanticItem argi = args.get(i).getArg();
					addSemanticNode(first.getArg(),argi,args.get(i).getType());
				}
			}

		}
	}
}
