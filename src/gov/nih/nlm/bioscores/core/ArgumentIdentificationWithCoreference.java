package gov.nih.nlm.bioscores.core;

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

import gov.nih.nlm.ling.composition.ArgumentIdentification;
import gov.nih.nlm.ling.composition.ArgumentPropagation;
import gov.nih.nlm.ling.composition.ArgumentRule;
import gov.nih.nlm.ling.composition.DependencyClass;
import gov.nih.nlm.ling.composition.ScalarModalityValueComposition;
import gov.nih.nlm.ling.composition.SourcePropagation;
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
import gov.nih.nlm.ling.sem.Term;
import gov.nih.nlm.ling.util.CollectionUtils;

/**
 * This class extends {@link ArgumentIdentification} class by specifying mechanisms
 * to incorporate coreference resolution into compositional argument identification
 * process. <p>
 *  
 * @author Halil Kilicoglu
 *
 */
// TODO This needs to be refactored better.
// TODO It also needs to be tested more thoroughly.
public class ArgumentIdentificationWithCoreference extends ArgumentIdentification {
	private static Logger log = Logger.getLogger(ArgumentIdentificationWithCoreference.class.getName());	
		
	/**
	 * Same as {@link ArgumentIdentification#argumentIdentification(Node, DocumentGraph, List, SemanticGraph, LinkedHashSet, boolean, boolean)}
	 * but incorporates coreference resolution into the processing.
	 * <var>semGraph</var> parameter is expected to be an instance of {@link CoreferenceSemanticGraph} class.
	 * 
	 * @param node				the current node in the <var>docGraph</var> to process
	 * @param docGraph  		the embedding graph of the document
	 * @param rules 			the argument identification rules
	 * @param semGraph  		semantic graph of the document  
	 * @param allChildSemantics all semantic objects associated with the children
	 * @param createGeneric  	whether creating generic predicates are allowed at this step 
	 * @param createIfExists  	whether new predications are allowed if there are existing ones associated with the node
	 */
	// TODO Currently, we can only handle anaphora relations
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
				} else if (s instanceof Expression) {
					semGraph.addSemanticNode(null, s, "");
					CoreferenceChain cc = CoreferenceChain.getChainWithAnaphor((Expression)s);
					if (semGraph instanceof CoreferenceSemanticGraph == false) {
						log.severe("The document semantic graph is expected to be an instance of CoreferenceSemanticGraph, but is not.");
					}
					if (cc != null) {
						semGraph.addSemanticNode(null, cc, "");
					}
					returnSems.add(s);
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
		if (node instanceof SurfaceElement) {
			LinkedHashSet<SemanticItem> sems = node.getSemantics();
			if (sems != null && sems.size() > 0) {
				for (SemanticItem s: sems) {
					if (s instanceof Expression) {
						if (semGraph instanceof CoreferenceSemanticGraph == false) {
							log.severe("The document semantic graph is expected to be an instance of CoreferenceSemanticGraph, but is not.");
						}
						semGraph.addSemanticNode(null, s, "");
						CoreferenceChain cc = CoreferenceChain.getChainWithAnaphor((Expression)s);
						if (cc != null) {
							semGraph.addSemanticNode(null, cc, "");
						}
					}
				}
			}
		}
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
	
	// TODO It's the same as compose method in ArgumentIdentification. With some refactoring, it could be possible t do without it.
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
				SemanticItem arg = getArgumentFromCoreferenceIfNecessary(d,sem);
				// rule determined from dictionary
				if (argName != null) {
					if (relDef.argSatisfiesRole(argName, arg) == false) continue;
					arguments.add(new Argument(argName,arg));
					log.log(Level.FINEST,"Adding (with dictionary rule) semantic object {0} as argument with the type {1}.", 
							new Object[]{arg.getId(),argName});
				}
				else if (air == null) {
					// this will, I think, always be true. So, do we want to keep it or not include this arg at all?
					// Keep for now, since it may prompt us to add new rules, but useless otherwise probably
					if (relDef.argSatisfiesRole(sd.getType(), arg) == false) continue;
					arguments.add(new Argument(sd.getType(),arg));
					log.log(Level.FINEST,"Adding (by default) semantic object {0} as argument with the type {1}.", 
							new Object[]{arg.getId(),sd.getType()});

				}
				// argument from general argument rules
				else {
					if (relDef.argSatisfiesRole(air.getArgumentType(), arg) == false) continue;
					arguments.add(new Argument(air.getArgumentType(),arg));
					log.log(Level.FINEST,"Adding (with general argument rule) semantic object {0} as argument with the type {1}.", 
							new Object[]{arg.getId(),air.getArgumentType()});
					
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
			
	// TODO Need to refactor
	// TODO Need to make it consistent with the types of coreference incorporated in argumentIdentification() method
	private static SemanticItem getArgumentFromCoreferenceIfNecessary(SemanticGraph d, SemanticItem sem) {
		if (sem == null || sem instanceof Expression == false) return sem;
		// do not process singletons
		if (CoreferenceChain.getChainsWithAnaphor((Term)sem).size() == 0) return sem;
		SemanticItem arg = null;		
		for (String o: d.outgoingEdgesOf(sem)) {
			if (o.contains(CoreferenceType.Anaphora.getRefRole().toString()) /*|| 
					o.contains(CoreferenceType.Cataphora.getRefRole().toString())*/)  {
				arg = d.getEdgeTarget(o);
				break;
			}
		}
		if (arg == null) {
			SemanticItem equiv = null;
			for (String in: d.incomingEdgesOf(sem)) {
				if (in.contains(CoreferenceType.Ontological.getRefRole().toString()))  {
					equiv = d.getEdgeSource(in);
					break;
				}
			}
			if (equiv != null) {
				for (String o: d.outgoingEdgesOf(equiv)) {
					if (o.contains(CoreferenceType.Anaphora.getRefRole().toString()) /*|| 
							o.contains(CoreferenceType.Cataphora.getRefRole().toString())*/)  {
						arg = d.getEdgeTarget(o);
						break;
					}
				}
			}
		}
		if (arg == null)
			arg = sem;
		return arg;
	}		
}
