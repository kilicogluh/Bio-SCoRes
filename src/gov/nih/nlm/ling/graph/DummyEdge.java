package gov.nih.nlm.ling.graph;

/**
 * Represents a dummy edge, which is not derived from a
 * <code>SynDependency</code>. 
 * A dummy edge can be used to represent relations between sentences, etc.
 * 
 * @author Halil Kilicoglu
 *
 */
public class DummyEdge implements Edge {
	public static final String INTER_SENTENCE_PREFIX = "PREV_";
	public static final String INTRA_SENTENCE_PREFIX = "TOP_";
	
	private String id;
	private String type;
	private Node governor;
	private Node dependent;

	/**
	 * Constructs a <code>DummyEdge</code> object with an id, type, and the nodes it links.
	 * 
	 * @param id	the identifier
	 * @param type	the type of the edge
	 * @param gov	the dominating node
	 * @param dep	the dependent node
	 */
	public DummyEdge(String id, String type, Node gov, Node dep) {
		this.type = type;
		this.id = id;
		this.governor = gov;
		this.dependent = dep;
	}

	public String getType() {
		return type;
	}
	
/*	public void setType(String type) {
		this.type = type;
	}*/

	public String getId() {
		return id;
	}

	public Node getGovernor() {
		return governor;
	}

	public Node getDependent() {
		return dependent;
	}
	
	public String toString(){
		return "DUMMY_EDGE(" + id + "_" + type + "," + governor.toString() + "_" + dependent.toString() + ")";
	}
	
	public boolean withinSentence() {
		return id.startsWith(INTRA_SENTENCE_PREFIX);
	}
	
	public boolean betweenSentences() {
		return id.startsWith(INTER_SENTENCE_PREFIX);
	}


}
