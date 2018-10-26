package gov.nih.nlm.bioscores.core;

import java.util.List;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.graph.SemanticGraph;
import gov.nih.nlm.ling.sem.SemanticItem;

/**
 * This class extends document semantic graphs to accommodate coreference relations.
 * {@link ArgumentIdentificationWithCoreference} class can use such information to 
 * enhance relation extraction.
 * 
 * @author Halil Kilicoglu
 *
 */
public class CoreferenceSemanticGraph extends SemanticGraph {

	private static final long serialVersionUID = -1351437983804409421L;

	/**
	 * Constructs a document semantic graph enhanced with coreference.
	 * Simply calls the constructor of the superclass.
	 * 
	 * @param doc	the document to associate with the graph
	 */
	public CoreferenceSemanticGraph(Document doc) {
		super(doc);
	}
	
	@Override
	public void addSemanticNode(SemanticItem parent, SemanticItem sem, String role) {
		if (sem instanceof CoreferenceChain) {
			CoreferenceChain cc = (CoreferenceChain)sem;
			CoreferenceType ct = CoreferenceType.valueOf(cc.getType());
			List<SemanticItem> exps = cc.getExpressions();
			List<SemanticItem> refs = cc.getReferents();
			SemanticItem first = exps.get(0);
			if (containsVertex(first) == false) addVertex(first);
			// avoid loops
			if (parent != null && parent.equals(first) == false) {
				try {
					addEdge(parent,first,role + ":" + parent.getId() + ":" + first.getId());
				} catch (RuntimeException e) {}
			}
			for (int i=1; i < exps.size(); i++) {
				SemanticItem argi = exps.get(i);
				addSemanticNode(first,argi,"Coref");
			}
			for (int i=0; i < refs.size(); i++) {
				SemanticItem argi = refs.get(i);
				addSemanticNode(first,argi,ct.getRefRole().toString());
			}
		} else {
			super.addSemanticNode(parent, sem, role);
		}
	}

}
