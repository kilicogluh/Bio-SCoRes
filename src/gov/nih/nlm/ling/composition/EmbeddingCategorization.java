package gov.nih.nlm.ling.composition;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.UnmodifiableDirectedGraph;

import gov.nih.nlm.ling.graph.GraphUtils;
import gov.nih.nlm.ling.sem.Predicate;

/**
 * A static graph representation of the embedding categorization.
 * 
 * @author Halil Kilicoglu
 *
 */
public class EmbeddingCategorization extends UnmodifiableDirectedGraph<String,String>{
	private static final long serialVersionUID = -2037971105908391213L;
	
	private static EmbeddingCategorization senseHierarchy;

	/**
	 * Creates an embedding categorization instance, or returns one if it already exists.
	 * 
	 * @return  the static embedding categorization instance
	 */
	public static EmbeddingCategorization getInstance() {
		if (senseHierarchy == null) {
			senseHierarchy = new EmbeddingCategorization();
		}
		return senseHierarchy;
	}
			
	private EmbeddingCategorization() {
		super(createGraph());
	}
	
	private static SimpleDirectedGraph<String,String> createGraph() {
		SimpleDirectedGraph<String,String> sdg = new SimpleDirectedGraph<String,String>(String.class);
		addVertices(sdg);
		addEdges(sdg);
		return sdg;
	}
	
	private static void addVertices(SimpleDirectedGraph<String,String> sdg) {
		sdg.addVertex("embedding"); 
		sdg.addVertex("MODAL");					sdg.addVertex("RELATIONAL");
			sdg.addVertex("EPISTEMIC");				sdg.addVertex("TEMPORAL");
				sdg.addVertex("ASSUMPTIVE");			sdg.addVertex("ASYNCHRONOUS");
				sdg.addVertex("SPECULATIVE"); 			sdg.addVertex("SYNCHRONOUS");
			sdg.addVertex("EVIDENTIAL");			sdg.addVertex("CAUSAL");
				sdg.addVertex("SENSORY"); 				sdg.addVertex("CAUSE");
				sdg.addVertex("REPORTING");				sdg.addVertex("PREVENT");
				sdg.addVertex("DEDUCTIVE");				sdg.addVertex("ENABLE");
				sdg.addVertex("DEMONSTRATIVE");		sdg.addVertex("CORRELATIVE");
			sdg.addVertex("DEONTIC");				sdg.addVertex("CONDITIONAL");
				sdg.addVertex("PERMISSIVE"); 		sdg.addVertex("COMPARATIVE");
				sdg.addVertex("OBLIGATIVE");			sdg.addVertex("CONTRAST");
				sdg.addVertex("COMMISSIVE"); 			sdg.addVertex("CONCESSION");
			sdg.addVertex("DYNAMIC");					sdg.addVertex("HIGHER_THAN");
				sdg.addVertex("POTENTIAL"); 			sdg.addVertex("LOWER_THAN");
				sdg.addVertex("VOLITIVE"); 				sdg.addVertex("SAME_AS");
			sdg.addVertex("SUCCESS");				sdg.addVertex("EXPANSION");
			sdg.addVertex("INTENTIONAL"); 				sdg.addVertex("CONJUNCTION");
			sdg.addVertex("INTERROGATIVE");				sdg.addVertex("INSTANTIATION");
			sdg.addVertex("EVALUATIVE"); 				sdg.addVertex("SPECIFICATION");
														sdg.addVertex("EQUIVALENCE");
			sdg.addVertex("PROPOSITIONAL");				sdg.addVertex("ALTERNATIVE");
				sdg.addVertex("ASPECTUAL");				sdg.addVertex("EXCEPTION");
					sdg.addVertex("INITIATE");			sdg.addVertex("LIST");
					sdg.addVertex("REINITIATE");
					sdg.addVertex("CONTINUE");		
					sdg.addVertex("CULMINATE");		sdg.addVertex("VALENCE_SHIFTER");
					sdg.addVertex("TERMINATE");			sdg.addVertex("SCALE_SHIFTER");
				sdg.addVertex("SEMANTIC_ROLE");				sdg.addVertex("DIMINISHER");
					sdg.addVertex("AGENT");					sdg.addVertex("INTENSIFIER");
					sdg.addVertex("PATIENT");				sdg.addVertex("NEGATOR");
					sdg.addVertex("BENEFICIARY");			sdg.addVertex("HEDGE");
					sdg.addVertex("EXPERIENCER");		sdg.addVertex("POLARITY_SHIFTER");
					sdg.addVertex("INSTRUMENT");			sdg.addVertex("POSITIVE_SHIFT");
					sdg.addVertex("PLACE");					sdg.addVertex("NEGATIVE_SHIFT");
					sdg.addVertex("TIME");					sdg.addVertex("NEUTRALIZE");
					sdg.addVertex("PURPOSE");
					sdg.addVertex("MANNER");		sdg.addVertex("OTHER");
	}
	
	private static void addEdges(SimpleDirectedGraph<String,String> sdg) {	
		sdg.addEdge("embedding", "MODAL", "embedding:MODAL");
			sdg.addEdge("MODAL","EPISTEMIC", "MODAL:EPISTEMIC");
				sdg.addEdge("EPISTEMIC","ASSUMPTIVE", "EPISTEMIC:ASSUMPTIVE");
				sdg.addEdge("EPISTEMIC","SPECULATIVE", "EPISTEMIC:SPECULATIVE");
		//		sdg.addEdge("EPISTEMIC:DEDUCTIVE", "EPISTEMIC","DEDUCTIVE");
			sdg.addEdge("MODAL", "EVIDENTIAL", "MODAL:EVIDENTIAL");
				sdg.addEdge("EVIDENTIAL","SENSORY", "EVIDENTIAL:SENSORY");
				sdg.addEdge("EVIDENTIAL","REPORTING", "EVIDENTIAL:REPORTING");
				sdg.addEdge("EVIDENTIAL","DEDUCTIVE", "EVIDENTIAL:DEDUCTIVE");
				sdg.addEdge("EVIDENTIAL","DEMONSTRATIVE", "EVIDENTIAL:DEMONSTRATIVE");			
			sdg.addEdge("MODAL","DEONTIC","MODAL:DEONTIC");
				sdg.addEdge("DEONTIC","PERMISSIVE","DEONTIC:PERMISSIVE");
				sdg.addEdge("DEONTIC","OBLIGATIVE","DEONTIC:OBLIGATIVE");
				sdg.addEdge("DEONTIC","COMMISSIVE","DEONTIC:COMMISSIVE");
			sdg.addEdge("MODAL","DYNAMIC","MODAL:DYNAMIC");
				sdg.addEdge("DYNAMIC","POTENTIAL","DYNAMIC:POTENTIAL");
				sdg.addEdge("DYNAMIC","VOLITIVE","DYNAMIC:VOLITIVE");
			sdg.addEdge("MODAL","SUCCESS","MODAL:SUCCESS");
			sdg.addEdge("MODAL","INTENTIONAL","MODAL:INTENTIONAL");
			sdg.addEdge("MODAL","INTERROGATIVE","MODAL:INTERROGATIVE");
			sdg.addEdge("MODAL","EVALUATIVE","MODAL:EVALUATIVE");
		
		sdg.addEdge("embedding", "RELATIONAL","embedding:RELATIONAL");
			sdg.addEdge("RELATIONAL","TEMPORAL","RELATIONAL:TEMPORAL");
				sdg.addEdge("TEMPORAL","SYNCHRONOUS","TEMPORAL:SYNCHRONOUS");
				sdg.addEdge("TEMPORAL","ASYNCHRONOUS","TEMPORAL:ASYNCHRONOUS");
			sdg.addEdge("RELATIONAL","CAUSAL","RELATIONAL:CAUSAL");
				sdg.addEdge("CAUSAL","CAUSE","CAUSAL:CAUSE");
				sdg.addEdge("CAUSAL","ENABLE","CAUSAL:ENABLE");
				sdg.addEdge("CAUSAL","PREVENT","CAUSAL:PREVENT");
			sdg.addEdge("RELATIONAL","CORRELATIVE","RELATIONAL:CORRELATIVE");
			sdg.addEdge("RELATIONAL","CONDITIONAL","RELATIONAL:CONDITIONAL");
			sdg.addEdge("RELATIONAL","COMPARATIVE","RELATIONAL:COMPARATIVE");
				sdg.addEdge("COMPARATIVE","CONTRAST","COMPARATIVE:CONTRAST");
				sdg.addEdge("COMPARATIVE","CONCESSION","COMPARATIVE:CONCESSION");
				sdg.addEdge("COMPARATIVE","HIGHER_THAN","COMPARATIVE:HIGHER_THAN");
				sdg.addEdge("COMPARATIVE","LOWER_THAN","COMPARATIVE:LOWER_THAN");
				sdg.addEdge("COMPARATIVE","SAME_AS","COMPARATIVE:SAME_AS");
			sdg.addEdge("RELATIONAL","EXPANSION","RELATIONAL:EXPANSION");
				sdg.addEdge("EXPANSION","CONJUNCTION","EXPANSION:CONJUNCTION");
				sdg.addEdge("EXPANSION","INSTANTIATION","EXPANSION:INSTANTIATION");
				sdg.addEdge("EXPANSION","SPECIFICATION","EXPANSION:SPECIFICATION");
				sdg.addEdge("EXPANSION","EQUIVALENCE","EXPANSION:EQUIVALENCE");
				sdg.addEdge("EXPANSION","ALTERNATIVE","EXPANSION:ALTERNATIVE");
				sdg.addEdge("EXPANSION","EXCEPTION","EXPANSION:EXCEPTION");
				sdg.addEdge("EXPANSION","LIST","EXPANSION:LIST");
				
		sdg.addEdge("embedding", "PROPOSITIONAL","embedding:PROPOSITIONAL");
			sdg.addEdge("PROPOSITIONAL","ASPECTUAL","PROPOSITIONAL:ASPECTUAL");
				sdg.addEdge("ASPECTUAL","INITIATE","ASPECTUAL:INITIATE");
				sdg.addEdge("ASPECTUAL","REINITIATE","ASPECTUAL:REINITIATE");
				sdg.addEdge("ASPECTUAL","CONTINUE","ASPECTUAL:CONTINUE");
				sdg.addEdge("ASPECTUAL","CULMINATE","ASPECTUAL:CULMINATE");
				sdg.addEdge("ASPECTUAL","TERMINATE","ASPECTUAL:TERMINATE");
			sdg.addEdge("PROPOSITIONAL","SEMANTIC_ROLE","PROPOSITIONAL:SEMANTIC_ROLE");
				sdg.addEdge("SEMANTIC_ROLE","AGENT","SEMANTIC_ROLE:AGENT");
				sdg.addEdge("SEMANTIC_ROLE","PATIENT","SEMANTIC_ROLE:PATIENT");
				sdg.addEdge("SEMANTIC_ROLE","BENEFICIARY","SEMANTIC_ROLE:BENEFICIARY");
				sdg.addEdge("SEMANTIC_ROLE","EXPERIENCER","SEMANTIC_ROLE:EXPERIENCER");
				sdg.addEdge("SEMANTIC_ROLE","INSTRUMENT","SEMANTIC_ROLE:INSTRUMENT");
				sdg.addEdge("SEMANTIC_ROLE","PLACE","SEMANTIC_ROLE:PLACE");
				sdg.addEdge("SEMANTIC_ROLE","TIME","SEMANTIC_ROLE:TIME");
				sdg.addEdge("SEMANTIC_ROLE","PURPOSE","SEMANTIC_ROLE:PURPOSE");
				sdg.addEdge("SEMANTIC_ROLE","MANNER","SEMANTIC_ROLE:MANNER");
		
				
		sdg.addEdge("embedding", "VALENCE_SHIFTER","embedding:VALENCE_SHIFTER");
			sdg.addEdge("VALENCE_SHIFTER","SCALE_SHIFTER","VALENCE_SHIFTER:SCALE_SHIFTER");
				sdg.addEdge("SCALE_SHIFTER","DIMINISHER","SCALE_SHIFTER:DIMINISHER");
				sdg.addEdge("SCALE_SHIFTER","INTENSIFIER","SCALE_SHIFTER:INTENSIFIER");
				sdg.addEdge("SCALE_SHIFTER","NEGATOR","SCALE_SHIFTER:NEGATOR");
				sdg.addEdge("SCALE_SHIFTER","HEDGE","SCALE_SHIFTER:HEDGE");
			sdg.addEdge("VALENCE_SHIFTER","POLARITY_SHIFTER","VALENCE_SHIFTER:POLARITY_SHIFTER");
				sdg.addEdge("POLARITY_SHIFTER","POSITIVE_SHIFT","POLARITY_SHIFTER:POSITIVE_SHIFT");
				sdg.addEdge("POLARITY_SHIFTER","NEGATIVE_SHIFT","POLARITY_SHIFTER:NEGATIVE_SHIFT");	
				sdg.addEdge("POLARITY_SHIFTER","NEUTRALIZE","POLARITY_SHIFTER:NEUTRALIZE");	
				
		sdg.addEdge("embedding", "OTHER","embedding:OTHER");
	}
	
	
	/** 
	 * Finds all subclasses of an embedding category (including itself, as well).
	 * 
	 * @param cat  an embedding category
	 * @return  the set of subclasses
	 */
	public static Set<String> getAllDescendants(String cat) {
		Set<String> descs = new LinkedHashSet<>();
		GraphUtils.getAllDescendants(senseHierarchy, cat, descs);
		return descs;
	}
	
	/** 
	 * Similar to {@code #getAllDescendants(String)} but takes as input a list of embedding categories
	 * 
	 * @param types  a list of embedding categories
	 * @return  the set of subclasses
	 */
	public static Set<String> getAllDescendants(List<String> types) {
		Set<String> descs = new LinkedHashSet<>();
		GraphUtils.getAllDescendants(senseHierarchy, types, descs);
		return descs;
	}
	
	/**
	 * Determines whether a predicate is modal.
	 * 
	 * @param pr  a predicate
	 * @return  true if the predicate type is a subclass of MODAL category.
	 */
	public static boolean isModal(Predicate pr) {
		return (getAllDescendants("MODAL").contains(pr.getType()));
	}
	
	/**
	 * Determines whether a predicate introduces an epistemic scale. 
	 * 
	 * @param pr  a predicate
	 * @return  true if the predicate introduces epistemic scale
	 */
	public static boolean isEpistemicScalar(Predicate pr) {
		return (getAllDescendants(Arrays.asList("EPISTEMIC","EVIDENTIAL")).contains(pr.getType()));
	}
	
	/**
	 * Determines whether a predicate is relational.
	 * 
	 * @param pr  a predicate
	 * @return  true if the predicate is relational
	 */
	public static boolean isRelational(Predicate pr) {
		return (getAllDescendants("RELATIONAL").contains(pr.getType()));
	}
	
	/**
	 * Determines whether a predicate is a scale shifter.
	 * 
	 * @param pr  a predicate
	 * @return  true if the predicate is a scale shifter
	 */
	public static boolean isScaleShifter(Predicate pr) {
		return (getAllDescendants("SCALE_SHIFTER").contains(pr.getType()));
	}
}
