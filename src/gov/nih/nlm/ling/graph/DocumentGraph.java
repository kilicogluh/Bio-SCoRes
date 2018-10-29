package gov.nih.nlm.ling.graph;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;

/**
 * Represents a <code>Document</code> content as a directed graph
 * where the nodes are <code>SurfaceElement</code> objects and the edges between the nodes
 * are <code>SynDependency</code> objects. The top-level sentence content is captured with 
 * <code>DummyNode</code> objects and the adjacency of sentences is captured
 * with <code>DummyEdge</code> objects between these nodes. <p>
 * This class is used to provide the basis for bottom-up semantic composition.
 * 
 * @author Halil Kilicoglu
 *
 */
public class DocumentGraph extends DirectedAcyclicGraph<Node,Edge> {
	private static final long serialVersionUID = 8076536588872092561L;

	private static Logger log = Logger.getLogger(DocumentGraph.class.getName());


	private Document document;

	/**
	 * Instantiates a <code>DocumentGraph</code> object from the contents 
	 * of a <code>Document</code>. Nodes corresponding to sentences are prefixed 
	 * with 'TOP' and the edges linking adjacent sentences are prefixed with
	 * 'PREV'.
	 * 
	 * @param doc  the corresponding document
	 */
	public DocumentGraph(Document doc) {
		super(Edge.class);
		if (doc == null) throw new NullPointerException("Unable to create DocumentGraph for null Document.");
		if (doc.getSentences() == null)
			throw new IllegalStateException("Document " + doc.getId() + " has no sentences.");
		Node docNode = new DummyNode(doc.getId());
		addVertex(docNode);
		Node prevSentenceNode = docNode;
		for (Sentence s: doc.getSentences()) {
			Node sentNode = new DummyNode(s.getId()); 
			addVertex(sentNode);			
			log.log(Level.FINEST, "Added sentence node: {0}.", new Object[]{sentNode.getId()});
			addEdge(prevSentenceNode,sentNode,
					new DummyEdge(DummyEdge.INTER_SENTENCE_PREFIX + prevSentenceNode.getId() + "_" + sentNode.getId(),
							DummyEdge.INTER_SENTENCE_PREFIX,prevSentenceNode,sentNode));
			log.log(Level.FINEST, "Added inter-sentence edge: {0}-{1}.", new Object[]{prevSentenceNode.getId(),sentNode.getId()});
			SentenceGraph sentGraph = new SentenceGraph(s);
			List<Node> roots = GraphUtils.getRoots(sentGraph);
			for (Node no: roots) {
				if (this.containsVertex(no) == false) addVertex(no);
				if (GraphUtils.getSuccessors(sentGraph, no).size() == 0) continue;
				addEdge(sentNode,no,
						new DummyEdge(DummyEdge.INTRA_SENTENCE_PREFIX + sentNode.getId() + "_" + no.getId(), 
								DummyEdge.INTRA_SENTENCE_PREFIX,sentNode,no));
				log.log(Level.FINEST, "Added intra-sentence top edge: {0}-{1}.", new Object[]{sentNode.getId(),no.getId()});
			}
			for (Edge sd: sentGraph.edgeSet()) {
				Node gov = (Node)sd.getGovernor();
				Node dep = (Node)sd.getDependent();
				if (containsVertex(gov) == false) addVertex(gov);
				if (containsVertex(dep) == false) addVertex(dep);
				// could check for circularity here
				this.addEdge(gov, dep, (Edge)sd);
				log.log(Level.FINEST, "Added intra-sentence edge: {0}-{1}.", new Object[]{gov.getId(),dep.getId()});
			}
			prevSentenceNode = sentNode;
		}
		this.document = doc;
	} 
	
	public Document getDocument() {
		return document;
	}

	public void setDocument(Document doc) {
		this.document = doc;
	}
}
