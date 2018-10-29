package gov.nih.nlm.ling.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.nlp.trees.Tree;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SurfaceElementFactory;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.sem.DomainProperties;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.util.ParseTreeUtils;
import gov.nih.nlm.ling.util.SemUtils;

/**
 * A <code>DependencyTransformation</code> implementation that collapses noun phrase-internal
 * dependencies if possible. <p> 
 * This transformation can be performed using one of three methods: <ul>
 * <li> {@link Method#Dependency}: Uses the original dependency relations of the sentence as the basis of transformation.
 * <li> {@link Method#ParseTree}: Uses parse tree of the sentence as the basis of transformation.
 * <li> {@link Method#Embedding}: Uses the transformed embedding relations. 
 * </ul>
 * The choice of method can be set with a constructor. Methods are specified as enum values. By default, 
 * Embedding relations are used. This transformation does not have any prerequisites.
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO A similar transformation, based on an external chunker, should probably also be added.
public class NPInternalTransformation implements DependencyTransformation {
	private static Logger log = Logger.getLogger(NPInternalTransformation.class.getName());	

	private Method method = null;
	
	public static enum Method {
		Dependency, ParseTree, Embedding
	}
	
	/**
	 * Constructs a default NP-internal transformation object, which uses Embedding relations.
	 */
	public NPInternalTransformation() {
		this(Method.Embedding);
	}
	
	/**
	 * Constructs a transformation object using the specified method.
	 * 
	 * @param method	the method to use
	 */
	public NPInternalTransformation(Method method) {
		this.method = method;
	}
	
	@Override
	public List<Class<? extends DependencyTransformation>> getPrerequisites() {
		return new ArrayList<Class<? extends DependencyTransformation>>();
	}
	
	public Method getMethod() {
		return method;
	}

	/**
	 * Transforms a sentence by collapsing NP-internal dependencies using the provided method.
	 * 
	 * @param  sent  the sentence to transform
	 */

	public synchronized void transform(Sentence sent) {
		if (sent.getSurfaceElements() == null || sent.getEmbeddings() == null) {
			log.log(Level.WARNING,"No textual units or embeddings can be found for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		List<Class<? extends DependencyTransformation>> applied  = sent.getTransformations();
		if (applied.containsAll(getPrerequisites()) == false) {
			log.log(Level.WARNING,"The prerequisites for NPInternalTransformation are not met for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		switch (method) {
			case Dependency: chunkUsingDependencies(sent); break;
			case Embedding:	chunkWithEmbeddings(sent); break;
			case ParseTree: chunkUsingParseTree(sent); break;
			default: chunkWithEmbeddings(sent);
		}
		sent.addTransformation(this.getClass());
	}
	
	// TODO: Not sure this is working as intended. It seems to be trying to handle NP-internal relations with empty head mechanism
	// but this is not mentioned in the dissertation, so it may not be necessary.
	private synchronized void chunkWithEmbeddings(Sentence sent) {
		Document doc = sent.getDocument();
		SurfaceElementFactory sef = doc.getSurfaceElementFactory();
		for (SurfaceElement su: sent.getSurfaceElements()) {
			List<SurfaceElement> surfsForRemovalSingle = new ArrayList<>();
			List<SynDependency> embeddings = sent.getEmbeddings();
			if (su.isNominal() == false || su.hasSemantics()) continue;
			List<SynDependency> npDeps = SynDependency.outDependenciesWithTypes(su, embeddings, SynDependency.NP_INTERNAL_DEPENDENCIES, true);
			if (npDeps.size() == 0) continue;
			boolean maySplit = true;
			Set<SemanticItem> realHeadPredicates = null;
			Collections.sort(npDeps);
			for (int i=npDeps.size(); i > 0; i--) {
				SynDependency dep = npDeps.get(i-1);
				SurfaceElement opposite = dep.getDependent();
				if (SpanList.atLeft(su.getSpan(), opposite.getSpan())) continue;
				// empty head, not sure how well it works
				if (opposite.filterByPredicates().size() > 0 &&
					su.getIndex() == opposite.getIndex()+1) {
					realHeadPredicates=opposite.filterByPredicates();
				}
				// The second boolean statement assumes coord transformation is done.
				else if (opposite.hasSemantics() || opposite.isCoordConj()) {maySplit = false;break;}

				surfsForRemovalSingle.add(opposite);
				List<SynDependency> subtreeNpDeps = new ArrayList<>();
				List<SynDependency> otherDeps = new ArrayList<>();
				log.log(Level.FINEST,"Checking the subtree of the textual unit {0}. ", new Object[]{opposite.toString()});
				SynDependency.getSubtreeDependenciesWithTypes(embeddings, opposite, SynDependency.NP_INTERNAL_DEPENDENCIES,subtreeNpDeps, otherDeps);
				for (SynDependency c: subtreeNpDeps) 
					surfsForRemovalSingle.add(c.getDependent());
			}
			// encountered a dependent with semantics (and no predicates to the right of the dependent), so ignore
			if (!maySplit && realHeadPredicates == null) continue;
			
			// there is a candidate for collapsing
			int min = Integer.MAX_VALUE;
			int max = 0;

			if (surfsForRemovalSingle.size() > 0){
				for (SurfaceElement so: surfsForRemovalSingle) {
					log.log(Level.FINE,"Removing textual unit due to chunking with embeddings: {0}.",new Object[]{so.toString()});
					if (so.getSpan().getEnd() > max) max = so.getSpan().getEnd();
					if (so.getSpan().getBegin() < min) min = so.getSpan().getBegin();
				}
				max = Math.max(max, su.getSpan().getEnd());
				min = Math.min(min, su.getSpan().getBegin());
				SurfaceElement nw = sef.createSurfaceElementIfNecessary(doc, new SpanList(min,max), false);
				log.log(Level.FINE,"Adding new textual unit due to chunking with embeddings: {0}.", new Object[]{nw.toString()});
				sent.synchSurfaceElements(nw);
				sent.synchEmbeddings();
			}
		}
	}
	
	/**
	 * Performs simple chunking by using dependency information. 
	 * It collapses the words that have a NP-internal dependency between them into a 
	 * new textual unit, and updates the sentence accordingly.  
	 * 
	 */
	private synchronized void chunkUsingDependencies(Sentence sent) {
		List<SurfaceElement> surfaceElements = sent.getSurfaceElements();
		if (surfaceElements == null || surfaceElements.size() == 0) return;
		Document doc = sent.getDocument();
		List<SynDependency> embeddings = sent.getEmbeddings();
		List<Span> npSpans = new ArrayList<>();
		List<SpanList> headSpans = new ArrayList<>();
		List<Span> addedSpans = new ArrayList<>();
		List<SynDependency> markedForAddition = new ArrayList<>();
		List<SynDependency> markedForRemoval = new ArrayList<>();
		
		for (SurfaceElement su: surfaceElements) {
			List<SynDependency> npDeps = SynDependency.outDependenciesWithTypes(su, embeddings, SynDependency.NP_INTERNAL_DEPENDENCIES, true);
			if (npDeps.size() == 0) continue;
			Collections.sort(npDeps);
			Collections.reverse(npDeps);
			int beginIndex = su.getIndex();
			int endIndex = beginIndex;
			for (int i=0; i < npDeps.size(); i++) {
				SynDependency np = npDeps.get(i);
				SurfaceElement dep = np.getDependent();
				// treat conjunctions as boundaries
				if (dep.isCoordConj()) break;
				
				// try to exclude hypernymy cases, such as  "the cation transport system inhibitors cimetidine, X, and Y"
				if (i ==0) {
					boolean hypernymModifier = false;
					Map<String,List<String>> hyps = DomainProperties.parse2("hypernym");
					Word head = dep.getHead();
					// TODO: MAke sure this does not fail
					LinkedHashSet<SemanticItem> sems = su.getHeadSemantics();
					for (SemanticItem sem: sems) {
						LinkedHashSet<String> semGroups = SemUtils.getSemGroups(sem);
						for (String g: semGroups) {
							if (hyps.get(g) == null) continue;
							if (hyps.get(g).contains(head.getLemma())) {
								hypernymModifier = true; break;
							}
						}
						if (hypernymModifier) break;
					}
					if (hypernymModifier) {
						markedForRemoval.add(np);
						for (int j=i+1;j<npDeps.size();j++) {
							SynDependency rem = npDeps.get(j);
							markedForRemoval.add(rem);
							log.log(Level.FINE,"Removing textual unit due to chunking with dependencies: {0}.",new Object[]{rem.toString()});
							SynDependency d = new SynDependency("NPD_" + rem.getId(), "appos",su,dep);
							markedForAddition.add(d); 	
							log.log(Level.FINE,"Adding dependency due to chunking with dependencies: {0}.",new Object[]{d.toString()});
							SynDependency d2 = new SynDependency("NPD_" + rem.getId(), rem.getType(),dep,rem.getDependent());
							markedForAddition.add(d2); 
							log.log(Level.FINE,"Adding dependency due to chunking with dependencies: {0}.",new Object[]{d2.toString()});
						}
						break;
					}
				}
				int depind = dep.getIndex();
				if (depind < beginIndex)  beginIndex = depind;
				else if (depind > endIndex) endIndex = depind;
			}
			if (beginIndex == su.getIndex() && endIndex == beginIndex) continue;
			if (beginIndex < endIndex) {
				Span sp = new Span(surfaceElements.get(beginIndex-1).getSpan().getBegin(),
								   surfaceElements.get(endIndex-1).getSpan().getEnd());
				SpanList headSpan = su.getHead().getSpan();
				// figure out if there are overlapping units
				// if so, extend the one that already exists in the span lists
				// and recompute the head
				// this was implemented to fix an error in SPL appositive resolution with MetaMap concepts
				// while it improved results for that specific case, overall it did not help.
/*				boolean overlapExists = false;
				int replaceIndex = 0;
				for (int i=0; i < npSpans.size(); i++) {
					Span asp = npSpans.get(i);
					if (Span.overlap(sp, asp)) {
						overlapExists = true;
						replaceIndex = i;
						sp = new Span(Math.min(asp.getBegin(), sp.getBegin()),Math.max(asp.getEnd(),sp.getEnd()));
						headSpan = MultiWord.findHeadFromCategory(sent.getWordsInSpan(sp)).getSpan();
					}
				}
				if (overlapExists) {
					npSpans.set(replaceIndex, sp); headSpans.set(replaceIndex, headSpan);
					addedSpans.set(replaceIndex, sp);
				} else {*/
					npSpans.add(sp);
					headSpans.add(headSpan);
					addedSpans.add(sp);
//				}
			}
		}
		for (int i=0; i < npSpans.size(); i++) {
			SpanList spl = new SpanList(npSpans.get(i));
			SpanList headSp = headSpans.get(i);
			SurfaceElement ns = doc.getSurfaceElementFactory().createSurfaceElementIfNecessary(sent, spl, headSp);
			LinkedHashSet<SemanticItem> spItems = Document.getSemanticItemsBySpan(doc, new SpanList(npSpans.get(i)),false);
			if (spItems != null) ns.addSemantics(spItems);
			sent.synchSurfaceElements(ns);
			log.log(Level.FINE,"Adding new textual unit due to chunking with dependencies: {0}.", new Object[]{ns.toString()});
		}
		sent.synchEmbeddings();
		sent.synchEmbeddings(markedForRemoval, markedForAddition);
	}
	
	/**
	 * Performs simple chunking by using parse tree information and tregex. 
	 */
	private synchronized void chunkUsingParseTree(Sentence sent) {
		Tree tree = sent.getTree();
		if (tree == null) return;
		Span span = sent.getSpan();
		Document doc = sent.getDocument();
		// TODO: This will return NP nodes that do not have any NP subtree or CC node.
		// Depending on how one wants to chunk, this can be done differently. More experimentation is needed.
		String[] patterns = new String[]{"@NP=np  !<<@NP & !<<CC"};
		List<Span> npSpans = new ArrayList<>();
		List<SpanList> headSpans = new ArrayList<>();
		int offset = span.getBegin();
		for (String pattern: patterns) {
			List<ParseTreeUtils.NameTreePair> patTrees = ParseTreeUtils.matchNamedNodes(tree, pattern, Arrays.asList("np"));
			if (patTrees == null) continue;
			for (ParseTreeUtils.NameTreePair ntp: patTrees) {
				if (ntp.getName().equals("np")) {
					Tree patTree = ntp.getTree();
					Span sp = ParseTreeUtils.getSpanFromTree(patTree,sent,offset);
					if (sp == null) continue;
					npSpans.add(sp);
					List<SurfaceElement> inclSurfs = sent.getSurfaceElementsFromSpan(sp);
					if (inclSurfs.size() > 0) headSpans.add(inclSurfs.get(inclSurfs.size()-1).getHead().getSpan());
					else headSpans.add(new SpanList(sp));
					offset = sp.getEnd();
				}
			}
		}
		for (int i=0; i < npSpans.size(); i++) {
			SpanList spl = new SpanList(npSpans.get(i));
			SpanList headSp = headSpans.get(i);
			SurfaceElement ns = doc.getSurfaceElementFactory().createSurfaceElementIfNecessary(sent, spl, headSp);
			LinkedHashSet<SemanticItem> spItems = Document.getSemanticItemsBySpan(doc,spl,false);
			if (spItems != null) ns.addSemantics(spItems);
			sent.synchSurfaceElements(ns);
			log.log(Level.FINE,"Adding new textual unit due to chunking with dependencies: {0}.", new Object[]{ns.toString()});
		}
		sent.synchEmbeddings();
	}

}
