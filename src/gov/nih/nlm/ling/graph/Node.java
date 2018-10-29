package gov.nih.nlm.ling.graph;

import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.SemanticItem;

import java.util.LinkedHashSet;

/**
 * An interface that represents a vertex in a graph. 
 * It can be used to represent textual units as well as
 * units larger than individual textual units that need to be processed
 * as a single unit in discourse processing, such as quotations, sentences, etc.
 * 
 * @see SurfaceElement
 * @see DummyNode
 * 
 * @author Halil Kilicoglu
 *
 */
public interface Node {
//	public void setId(String id);
	public String getId();
	public void addSemantics(SemanticItem si);
	public void addSemantics(LinkedHashSet<SemanticItem> sis);
	public LinkedHashSet<SemanticItem> getSemantics();
	public void setSemantics(LinkedHashSet<SemanticItem> sis);
	public SpanList getSpan();
	public void setSpan(SpanList sp);
	
}
