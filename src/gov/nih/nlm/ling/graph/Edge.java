package gov.nih.nlm.ling.graph;

import gov.nih.nlm.ling.core.SynDependency;

/**
 * An interface that represents a directed edge in a graph.
 * It can represent a syntactic dependency or a semantic one (embedding).
 * It can also represent links between <code>Sentence</code> objects 
 * or between a <code>Sentence</code> object and a <code>SurfaceElement</code>
 * it encloses. 
 * 
 * @see SynDependency
 * @see DummyEdge
 * 
 * @author Halil Kilicoglu
 *
 */
public interface Edge {
	public String getType();
//	public void setType(String type);
	public String getId();
	public Node getGovernor();
	public Node getDependent();
}
