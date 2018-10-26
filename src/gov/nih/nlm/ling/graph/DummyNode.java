package gov.nih.nlm.ling.graph;

import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.sem.SemanticItem;

import java.util.LinkedHashSet;

/**
 * Represents a dummy node, which can be used to 
 * represent units that go beyond textual units, 
 * such as documents, sentences, quotations, etc. 
 * This is expected to be mostly useful for discourse processing.
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO: Not fully implemented or used at this point
public class DummyNode implements Node {
	private String id;
	private LinkedHashSet<SemanticItem> semantics;
	private SpanList span;

	/**
	 * Constructs a <code>DummyNode</code> with an identifier.
	 * 
	 * @param id the identifier
	 */
	public DummyNode(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public LinkedHashSet<SemanticItem> getSemantics() {
		return semantics;
	}

	public void setSemantics(LinkedHashSet<SemanticItem> semantics) {
		this.semantics = semantics;
	}

	public void addSemantics(SemanticItem s) {
		if (semantics == null) semantics = new LinkedHashSet<SemanticItem>();
		semantics.add(s);
	}
	
	public void addSemantics(LinkedHashSet<SemanticItem> s) {
		if (semantics == null) semantics = new LinkedHashSet<SemanticItem>();
		semantics.addAll(s);
	}
	
	public SpanList getSpan() {
		return span;
	}

	public void setSpan(SpanList span) {
		this.span = span;
	}
	
	// TODO Not implemented.
	public boolean isDocumentNode() {
		return false;
	}
	
	// TODO Not implemented.
	public boolean isSentenceNode() {
		return false;
	}
	
	// TODO Not implemented.
	public boolean isQuotationNode() {
		return false;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		if (this.getSemantics() != null) {
			for (SemanticItem so: this.getSemantics()) {
				buf.append("_" + so.toShortString());
			}
		}
		return "DUMMY_NODE("+ id + "_" + buf.toString() + ")";
	}

}
