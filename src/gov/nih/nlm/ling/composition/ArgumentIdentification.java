package gov.nih.nlm.ling.composition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Lexeme;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.graph.DocumentGraph;
import gov.nih.nlm.ling.graph.Edge;
import gov.nih.nlm.ling.graph.GraphUtils;
import gov.nih.nlm.ling.graph.Node;
import gov.nih.nlm.ling.graph.SemanticGraph;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.Predication;
import gov.nih.nlm.ling.sem.RelationDefinition;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import gov.nih.nlm.ling.sem.Sense;
import gov.nih.nlm.ling.util.CollectionUtils;

/**
 * This class contains static methods to process a <code>DocumentGraph</code> recursively to identify
 * arguments of predicates and create a <code>SemanticGraph</code> object that contains the full
 * semantics of the <code>Document</code>. <p>
 * The identification process is aided by a list of argument identification rules, which can 
 * be specified per lexeme in a dictionary. Otherwise, default rules defined in <code>ArgumentRule</code> are used.
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO Needs more work.
public class ArgumentIdentification {
	private static Logger log = Logger.getLogger(ArgumentIdentification.class.getName());	
	
	/**
	 * Recursively identifies arguments for a given <code>Node</code> in a <code>DocumentGraph</code>
	 * using <code>ArgumentRule</code>s, progressively building a <code>SemanticGraph</code> object. 
	 * At each step, returns all the semantic objects associated with the descendant nodes.
	 * 
	 * @param node				the current node in the <var>docGraph</var> to process
	 * @param docGraph  		the embedding graph of the document
	 * @param rules 			the argument identification rules
	 * @param semGraph  		semantic graph of the document  
	 * @param allChildSemantics all semantic objects associated with the children
	 * @param createGeneric  	whether creating generic predicates are allowed at this step 
	 * @param createIfExists  	whether new predications are allowed if there are existing ones associated with the node
	 */
	// TODO: Not sure allChildSemantics are sufficient for propagation
	// TODO: Should we not handle generic objects with GenericSemanticsBinding transformation? In that case createGeneric may be redundant.
	// TODO: Originally, this included coreference processing, but probably do this beforehand. 
	public static void argumentIdentification(Node node, DocumentGraph docGraph, List<ArgumentRule> rules, 
			SemanticGraph semGraph, LinkedHashSet<SemanticItem> allChildSemantics, boolean createGeneric, boolean createIfExists) {
		log.log(Level.FINE, "Examining node for argument identification: {0}.", new Object[]{node.toString()});
		if (GraphUtils.isLeaf(docGraph,node) ) {
			LinkedHashSet<SemanticItem> sems = node.getSemantics();
			if (sems == null || sems.size() == 0) {
				log.log(Level.FINEST, "Leaf node {0} has no semantics. Skipping..", new Object[]{node.getId()});
				return;
			}
			LinkedHashSet<SemanticItem> returnSems = new LinkedHashSet<>();
			for (SemanticItem s: sems) {
				if (s instanceof Predicate) {
					// possible ellipsis
					// May need to create Predications on triggers even when they correspond to leaves.
					// The problem is with ATTRIBUTIVE
					// X undergoes a restructuring, restructuring is only a Predicate, and no Predication is created.
					// Maybe handle it as an inferential process.
					if (node instanceof SurfaceElement) {
						SurfaceElement leafSurf = (SurfaceElement)node;
						LinkedHashSet<SemanticItem> rels = leafSurf.filterByRelations();
						if (!createIfExists && rels.size() > 0) {
							for (SemanticItem r: rels) {
								semGraph.addSemanticNode(null, r, "");
								log.log(Level.FINEST, "Leaf node {0} is associated with relation {1}, adding it to the semantic graph. ", new Object[]{node.getId(),r.getId()});
								returnSems.add(r);
							}
						}
					}
				}  else if (s instanceof Entity) {
					semGraph.addSemanticNode(null, s, "");
					log.log(Level.FINEST, "Leaf node {0} is associated with entity {1}, adding it to the semantic graph. ", new Object[]{node.getId(),s.getId()});
					returnSems.add(s);
				} 
			}
			allChildSemantics.addAll(returnSems);
			return;
		}
		log.log(Level.FINE, "Traversing node {0}.", new Object[]{node.getId()});
		LinkedHashSet<Edge> edges = new LinkedHashSet<>(docGraph.outgoingEdgesOf(node));
		Map<Edge,LinkedHashSet<SemanticItem>> argStructure  = new HashMap<>();
		for (Edge e: edges) {
			log.log(Level.FINE,"Examining edge {0}", new Object[]{e.toString()});
			Node eChild = e.getDependent();
			// Should not happen
			if (eChild == null) {
				log.log(Level.WARNING, "Edge {0} to invalid node. Skipping.", new Object[]{e.getId()});
				continue;
			}
			LinkedHashSet<SemanticItem> childSemantics = new LinkedHashSet<>();	
			argumentIdentification(eChild,docGraph,rules,semGraph,childSemantics,createGeneric,createIfExists);
			if (childSemantics == null || childSemantics.size() == 0) {
				log.log(Level.FINE, "No semantics found via edge {0} from node {1}.", new Object[]{eChild.getId(),node.getId()});
				continue;
			}
			if (childSemantics.contains(node)) {
				log.log(Level.SEVERE,"Circularity detected from node {0} via edge {1}.", new Object[]{node.getId(), eChild.getId()});
			}
			argStructure.put(e, childSemantics);
			allChildSemantics.addAll(childSemantics);
		}
		// return the most relevant, prominent semantic items from composition
		LinkedHashSet<SemanticItem> composedItems = new LinkedHashSet<>();
		compose(node,argStructure,rules,semGraph,composedItems,createGeneric,createIfExists);
		for (SemanticItem c: composedItems) {
			log.log(Level.FINE, "Composed semantic object from node {0}: {1}.", new Object[]{node.getId(),c.toString()});
		}
		ArgumentPropagation.propagate(node,argStructure,composedItems,allChildSemantics);
	}
		
	private static void compose(Node node, Map<Edge,LinkedHashSet<SemanticItem>> argStructure, List<ArgumentRule> rules, SemanticGraph d, 
			LinkedHashSet<SemanticItem> composedItems, boolean createGeneric, boolean createIfExists) {
		if (node instanceof SurfaceElement == false) {
			log.log(Level.FINEST, "Node {0} is not a textual unit. Skipping composition.", new Object[]{node.getId()});
			return;
		}
		SurfaceElement surf = (SurfaceElement)node;
		String category = surf.getCategory();
		LinkedHashSet<SemanticItem> semantics = surf.getSemantics();
		Document doc = d.getDocument();
		SemanticItemFactory sif = doc.getSemanticItemFactory();

		List<ArgumentRule> applyRules = ArgumentRule.filterByCategory(rules,category);
		if (semantics == null || semantics.size() == 0 ) {
			if (createGeneric) {
				if (argStructure.size() == 0) return;
				Predicate pr = sif.newPredicate(doc, surf.getSpan(), surf.getHead().getSpan(), Predicate.GENERIC);
				LinkedHashSet<Predication> preds = composePredications(surf,pr,sif,argStructure,applyRules,d);
				if (preds.size() == 0) return;
				for (Predication pred: preds) {
					d.addSemanticNode(null, pred,"");
					log.log(Level.FINEST, "Node {0} with generic predicate is associated with predication {1}, adding it to the semantic graph.", 
							new Object[]{node.getId(),pred.getId()});
				}
				composedItems.addAll(preds);
			}
		} else {
			LinkedHashSet<SemanticItem> rels = surf.filterByRelations();
			if (!createIfExists && rels.size() > 0) {
				for (SemanticItem r: rels) {
					d.addSemanticNode(null, r, "");
					composedItems.add(r);
					log.log(Level.FINEST, "Node {0} is associated with relation {1}, adding it to the semantic graph.", 
							new Object[]{node.getId(),r.getId()});
					LinkedHashSet<Predication> updated = handleNegatedArguments(r,argStructure);
					for (Predication u: updated) {
						log.log(Level.FINEST, "Node {0} is associated with predication {1}, adding it to the semantic graph.", 
								new Object[]{node.getId(),u.getId()});
					}
					composedItems.addAll(updated);
				}
				return;
			}
			Predicate pr = null;
			if (argStructure.size() == 0) return;

			// if the larger spanning predicate has already been used, do not use smaller ones.
			// example: if cannot was used, you don't want to use 'can' or 'not' again.
			Set<SpanList> covered = new HashSet<>(); 
			for (SemanticItem so: semantics) {
				if (so == null || so instanceof Predicate == false) continue;
				pr = (Predicate)so;
				boolean toProcess = true;
				for (SpanList cov: covered) {
					if (SpanList.subsume(cov, pr.getSpan())) {
						toProcess = false;
						break;
					}
				}
				if (!toProcess)  {
					log.log(Level.WARNING, "Skipping predicate {0} in composition, because it's subsumed by a larger unit.", 
							new Object[]{pr.getId()});
					continue;
				}
				LinkedHashSet<Predication> preds = composePredications(surf, pr,sif,argStructure,applyRules,d);
				// may want to add Span to semantic relation here. also may want to remove SR if it is not fulfilled with 
				if (preds.size() == 0) continue;
				for (Predication pred: preds) {
					d.addSemanticNode(null,pred,"");
					log.log(Level.FINEST, "Node {0} is associated with predication {1}, adding it to the semantic graph.", 
							new Object[]{node.getId(),pred.getId()});
				}
				composedItems.addAll(preds);
				covered.add(pr.getSpan());
			}
		}
//		return resolveAmbiguityIfNecessary(surf);
	}
	
	
	private static LinkedHashSet<Predication> composePredications(SurfaceElement surf, Predicate pr, SemanticItemFactory sif, 
			Map<Edge,LinkedHashSet<SemanticItem>> argStructure, List<ArgumentRule> rules, SemanticGraph d) {
		Lexeme lex = surf.getHead().getLexeme();
		String category = surf.getCategory();
		LinkedHashSet<Predication> predications = new LinkedHashSet<>();
		RelationDefinition relDef = Predication.getDefinition(Predication.getDefinitions(), pr.getType());
		if (relDef == null) return predications;
		List<Argument> arguments = new ArrayList<>();
		boolean inverse = false;
		Sense sense = null;
		List<String> embeddingTypes = null;
		if (pr != null) {
			sense = pr.getAssociatedSense();
			if (sense != null) embeddingTypes = sense.getEmbeddingTypes();
		}
		for (Edge e: argStructure.keySet()) {
			if (!(e instanceof SynDependency)) continue;
			SynDependency sd = (SynDependency)e;
			log.log(Level.FINEST,"Searching for embedding type {0} with the lemma {1}.", new Object[]{sd.getType().toString(),lex.getLemma()});
			// if the embedding types can be determined from the dictionary, use them
			String argName = (DependencyClass.inClass(category,sd.getType(),embeddingTypes) ? "COMP": null);
			ArgumentRule air = null;
			// if cannot be determined from the dictionary, try using default ones from ArgumentRule
			if (argName == null) {
				air = ArgumentRule.findMatchingArgumentRule(rules,sd.getType(),lex,category,embeddingTypes);
				if (air == null) 
					log.log(Level.FINEST,"No appropriate composition rule found for lemma_category_type: {0}_{1}_{2}.", 
							new Object[]{lex.getLemma(),category,sd.getType()});
				else 
					log.log(Level.FINEST,"Default composition rule {0} will be applied for lemma_category_type: {1}_{2}_{3}.",
							new Object[]{air.toString(),lex.getLemma(),category,sd.getType()});
			} else {
				log.log(Level.FINEST,"Dictionary composition rule {0} will be applied for lemma_category_type: {1}_{2}_{3}.",
							new Object[]{argName, lex.getLemma(),category,sd.getType()});
				if (sense!= null && sense.isInverse()) inverse = true;
			}
			Set<SemanticItem> sos = argStructure.get(sd);
			// should not happen
			if (sos == null || sos.size() == 0) {
				log.log(Level.WARNING,"No associated semantic object with the nodes via edge {0}. Skipping..", new Object[]{sd.getType()}); 
				continue;
			}
			for (SemanticItem sem: sos) {
				// rule determined from dictionary
				if (argName != null) {
					if (relDef.argSatisfiesRole(argName, sem) == false) continue;
					arguments.add(new Argument(argName,sem));
					log.log(Level.FINEST,"Adding (with dictionary rule) semantic object {0} as argument with the type {1}.", 
							new Object[]{sem.getId(),argName});
				}
				else if (air == null) {
					// this will, I think, always be true. So, do we want to keep it or not include this arg at all?
					// Keep for now, since it may prompt us to add new rules, but useless otherwise probably
					if (relDef.argSatisfiesRole(sd.getType(), sem) == false) continue;
					arguments.add(new Argument(sd.getType(),sem));
					log.log(Level.FINEST,"Adding (by default) semantic object {0} as argument with the type {1}.", 
							new Object[]{sem.getId(),sd.getType()});

				}
				// argument from general argument rules
				else {
					if (relDef.argSatisfiesRole(air.getArgumentType(), sem) == false) continue;
					arguments.add(new Argument(air.getArgumentType(),sem));
					log.log(Level.FINEST,"Adding (with general argument rule) semantic object {0} as argument with the type {1}.", 
							new Object[]{sem.getId(),air.getArgumentType()});
					
				}
			}
		}

		// Don't go further if we don't have the right arguments.
		if (arguments.size() == 0 ) {
			log.log(Level.FINE, "No argument found for composition for textual unit: {0}. Skipping.", new Object[]{surf.toString()});
			return predications;
		}
		if (inverse) {
			invert(arguments);
		}
		
		// TODO This part that has to do with determining multiple constraint and creating predications accordingly.
		// It can have its own method.
		Set<String> multipleViolatingRoles = relDef.argumentViolatesMultipleRoleConstraint(arguments);
		Document doc = sif.getDocument();

		if (multipleViolatingRoles.size() == 0) {
			if (Predication.coreArgsResolved(arguments,relDef)) {
				Predication pred = sif.newPredication(doc, pr, arguments, null, null);
				pred.setSource(true);
				ScalarModalityValueComposition.propagateScalarModalityValues(pred);
				SourcePropagation.propagateSource(pred);
				predications.add(pred);
				predications.addAll(handleNegatedArguments(pred,argStructure));
			}
		}
		else { 
			List<Collection<Argument>> violatingArgGroups = new ArrayList<>();
			List<Argument> others = new ArrayList<>(arguments);
			for (String v: multipleViolatingRoles) {
				List<Argument> vArgs = Argument.getArgs(arguments,v);
				violatingArgGroups.add((Collection<Argument>)vArgs);
				others.removeAll(vArgs);	
			}
			Collection<List<Argument>> permutations = CollectionUtils.generatePermutations(violatingArgGroups);
			for (List<Argument> perm: permutations) {
				List<Argument> nperm = new ArrayList<>(perm);
				nperm.addAll(others);
				if (Predication.coreArgsResolved(nperm, relDef)) {
					Predication pred = sif.newPredication(doc, pr, nperm, null, null);
					pred.setSource(true);					
					ScalarModalityValueComposition.propagateScalarModalityValues(pred);
					SourcePropagation.propagateSource(pred);
					predications.add(pred);
					predications.addAll(handleNegatedArguments(pred,argStructure));
				}
			}
		}
		return predications;
	}
	
	/**
	 * Inverts the roles of the arguments associated with a predication.
	 * An argument with the role SUBJ will get the role COMP, for example, and
	 * vice versa.
	 * 
	 * @param args	the arguments whose roles to invert.
	 */
	protected static void invert(List<Argument> args) {
		List<String> argNames = Argument.getArgNames(args);
		int si = argNames.indexOf("SUBJ");
		int ci = argNames.indexOf("COMP");
		if (si >= 0) argNames.set(si, "COMP");
		if (ci >= 0) argNames.set(ci, "SUBJ");
	}
		
	/**
	 * Reflects the negation of the argument to the predication level.
	 * 
	 * @param rel			the predication
	 * @param argStructure	the argument structure of this predication
	 * 
	 * @return	updated predications
	 */
	protected static LinkedHashSet<Predication> handleNegatedArguments(SemanticItem rel, Map<Edge,LinkedHashSet<SemanticItem>> argStructure) {
		LinkedHashSet<Predication> negPreds = new LinkedHashSet<>();
		if (rel instanceof Predication == false) return negPreds;
		Predication r = (Predication)rel;
		List<SemanticItem> args= r.getArgItems();
		SemanticItem neg = null;
		for (Edge s: argStructure.keySet()) {
			if (s instanceof SynDependency == false) continue;
			SynDependency sd = (SynDependency)s;
			LinkedHashSet<SemanticItem> children = sd.getDependent().filterByPredicates();
			for (SemanticItem si: children) {
				if (si.getType().equals("NEGATOR")) {
					neg = si;
					break;
				}
			}
			if (neg instanceof Predicate) {
				Predicate negPr = (Predicate)neg;
				SurfaceElement negS = negPr.getSurfaceElement();
				Document doc = negS.getSentence().getDocument();
				LinkedHashSet<SurfaceElement> succs = SynDependency.getSuccessors(negS, negS.getSentence().getEmbeddings());
				for (SurfaceElement succ : succs) {
					LinkedHashSet<SemanticItem> semantics = succ.getSemantics();
					if (semantics != null && org.apache.commons.collections15.CollectionUtils.containsAny(args,semantics)) {
						List<Argument> newArgs = new ArrayList<Argument>();
						newArgs.add(new Argument("COMP",rel));
						Predication newPr = doc.getSemanticItemFactory().newPredication(doc, negPr, newArgs, null, null);
						newPr.setSource(true);					
						ScalarModalityValueComposition.propagateScalarModalityValues(newPr);
						SourcePropagation.propagateSource(newPr);
						negPreds.add(newPr);
					}
				}
			}
		}
		return negPreds;
	}
	
	// UNUSED from AmbiguityResolution, which is removed.
/*	public static Set<SemanticItem> extractSemanticAnnotations(Node node, DocumentGraph g) {
		if (GraphUtils.isLeaf(g, node) && node instanceof SurfaceElement) return (((SurfaceElement)node).getSemantics());
		LinkedHashSet<Edge> edges = new LinkedHashSet<Edge>(g.outgoingEdgesOf(node));
		log.debug("Extracting node " + node.toString());
		Set<SemanticItem> annotations = new HashSet<SemanticItem>();
		for (Edge sd: edges) {
			Node sdChild = g.getEdgeSource(sd);
			if (sdChild.equals(node)) sdChild = g.getEdgeTarget(sd);
			Set<SemanticItem> childAnns = extractSemanticAnnotations(sdChild,g);
			if (childAnns == null)  continue;
			annotations.addAll(childAnns);
		}
		if (node instanceof DummyNode) return annotations;
		Set<SemanticItem> currAnns = ((SurfaceElement)node).getSemantics();
		// when null is returned, seems like we are not propagating up valid annotations?
		if (currAnns == null || currAnns.size() == 0)  //return null;
			return annotations;
		annotations.addAll(currAnns);
		for (SemanticItem si: annotations) {
			log.debug("Extracted node annotation: "+ node.toString());
			log.debug("SI:" + si.toString());
		}
		return annotations;
	}*/
	
	// UNUSED from AmbiguityResolution
	// If there are resolved semantic relations on a surface item and unresolved ones as well
	// only return resolved ones (The logic is based on core args, but might change)
/*	private static Set<SemanticItem> resolveAmbiguityIfNecessary(SurfaceElement si) {
		log.debug("Resolving ambiguity on " + si.toString());
		Set<SemanticItem> existingRels = si.filterByRelations();
		log.debug("Number of relations " + (existingRels == null ? "null" : existingRels.size()));
		if (existingRels == null || existingRels.size() < 2) return si.getSemantics();
		Set<SemanticItem> out = si.getSemantics();
		Set<SemanticItem> resolved = new HashSet<SemanticItem>();
		for (SemanticItem rel: existingRels) {
			Predication sr = (Predication)rel;
			if (sr.isResolved()) resolved.add(rel);
		}
		if (resolved.size() > 0) {
			for (SemanticItem rel: existingRels) {
				Predication sr = (Predication)rel;
				if (!(resolved.contains(rel))) {
					Predicate t = sr.getPredicate();
					if (t != null) out.remove(t);
					out.remove(rel);
				}
			}
			return out;
		} else {
			return si.getSemantics();
		}
	}*/
		
}
