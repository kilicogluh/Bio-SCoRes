package gov.nih.nlm.ling.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SurfaceElementFactory;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.sem.SemanticItem;

/**
 * A <code>DependencyTransformation</code> implementation that transforms a <code>Sentence</code> object
 * by making explicit the semantic dependencies involving adverbial clauses. This transformation is mostly useful
 * for discourse processing. The prerequisite is <code>DependencyDirectionReversal</code> transformation.
 * 
 * @author Halil Kilicoglu
 *
 */
public class AdverbialTransformation implements DependencyTransformation {
	private static Logger log = Logger.getLogger(AdverbialTransformation.class.getName());
	
	private final static List<String> NP_INTERNAL_DEPS = Arrays.asList("det","nn","num","amod","dep","poss","measure","quantmod");
	private final static List<String> MANDATORY_TYPES = Arrays.asList("advmod");
	
	@Override
	public List<Class<? extends DependencyTransformation>> getPrerequisites() {
		List<Class<? extends DependencyTransformation>> prereqs = 
				new ArrayList<Class<? extends DependencyTransformation>>();
		prereqs.add(DependencyDirectionReversal.class);
		return prereqs;
	}

	/**
	 * Transforms a sentence by addressing the adverbial clauses and phrases. 
	 * Simply calls {@code #transformAdvClause(Sentence)} and {@code #transformAdvPhrase(Sentence)}.
	 * 
	 * @param sent  the sentence to transform
	 */
	public synchronized void transform(Sentence sent) {
		if (sent.getSurfaceElements() == null || sent.getEmbeddings() == null) {
			log.log(Level.WARNING,"No textual units or embeddings can be found for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		List<Class<? extends DependencyTransformation>> applied  = sent.getTransformations();
		if (applied.containsAll(getPrerequisites()) == false) {
			log.log(Level.WARNING,"The prerequisites for AdverbialTransformation are not met for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		transformAdvClause(sent);
		transformAdvPhrase(sent);
		sent.addTransformation(this.getClass());
	}
	
	/**
	 * Transforms a sentence by addressing the dependencies that involve adverbial clauses. <p>
	 * It currently handles the configuration where two dependencies advcl(c,b) and SYN_DEP(a,b)
	 * exist, where SYN_DEP is either mark or advmod dependency. The result is
	 * advcl(a,c) and SYN_DEP(a,b).
	 * 
	 * @param  sent  the sentence to transform
	 */
	private synchronized void transformAdvClause(Sentence sent) {
		List<SynDependency> embeddings = sent.getEmbeddings();
		List<SynDependency> markedForAddition = new ArrayList<>();
		List<SynDependency> markedForRemoval = new ArrayList<>();
		for (SurfaceElement su: sent.getSurfaceElements()) {
			List<SynDependency> outDeps = SynDependency.outDependenciesWithType(su, embeddings, "advcl", true);
			if (outDeps.size() == 0) continue;
			// assuming at most one dependency
			SynDependency existing = outDeps.get(0);
			SurfaceElement advclObj = existing.getDependent();
			if (advclObj == null) continue;
			List<SynDependency> inDeps = 
					SynDependency.inDependenciesWithTypes(advclObj, embeddings, Arrays.asList("mark","advmod"), true);
			if (inDeps.size() == 0) continue;
			markedForRemoval.add(existing);
			SurfaceElement adverbial = null;
			for (SynDependency in: inDeps) {
				log.log(Level.FINEST,"Adverbial modifier dependency: {0}.", new Object[]{in.toString()});
				adverbial = in.getGovernor();
				markedForAddition.add(new SynDependency("ACL_" + existing.getId(), existing.getType(), adverbial,su));
				List<SynDependency> allInDeps = SynDependency.inDependencies(su, embeddings);
				if (adverbial != null) {
					for (SynDependency sd: allInDeps) {
						markedForRemoval.add(sd);
						SynDependency nD = new SynDependency("ACL_" + sd.getId(), sd.getType(), sd.getGovernor(),adverbial);
						markedForAddition.add(nD);
						log.log(Level.FINE,"Removing dependency due to adverbial clause transformation: {0}.", new Object[]{sd.toString()});
						log.log(Level.FINE,"Adding dependency due to adverbial clause transformation: {0}.", new Object[]{nD.toString()});
					}
				}
			}
		}
		sent.synchEmbeddings(markedForRemoval,markedForAddition);
	}
	
	/**
	 * Similar to {@code NPInternalTransformation.chunkWithEmbeddings(Sentence sent)}, just using a slightly larger set of dependencies.
	 * 
	 * @param sent  the sentence to transform
	 */
	private synchronized void transformAdvPhrase(Sentence sent) {
		Document doc = sent.getDocument();
		SurfaceElementFactory sef = doc.getSurfaceElementFactory();
		List<SynDependency> embeddings = sent.getEmbeddings();

		// the textual unit is one that is the adverbial modifier while also being the head of a NP. 
		for (SurfaceElement su: sent.getSurfaceElements()) {
			List<SurfaceElement> surfsForRemovalSingle = new ArrayList<>();
			if (su.hasSemantics()) continue;
			boolean adverbial = SynDependency.inDependenciesWithTypes(su, embeddings, MANDATORY_TYPES, true).size() > 0;
			if (!adverbial) continue;
			boolean maySplit = true;
			Set<SemanticItem> realHeadPredicates = null;
			List<SynDependency> npDeps = SynDependency.outDependenciesWithTypes(su, embeddings, NP_INTERNAL_DEPS, true);
			if (npDeps.size() == 0) continue;
			Collections.sort(npDeps);
			for (int i=npDeps.size(); i > 0; i--) {
				SynDependency dep = npDeps.get(i-1);
				SurfaceElement opposite = dep.getDependent();
				// potential empty head, not sure how well it works
				if (opposite.filterByPredicates().size() > 0 &&
					su.getIndex() == opposite.getIndex()+1) {
					realHeadPredicates=opposite.filterByPredicates();
				}
				else if (opposite.hasSemantics()) {maySplit = false; break;}
				surfsForRemovalSingle.add(opposite);
				List<SynDependency> subtreeNpDeps = new ArrayList<>();
				List<SynDependency> otherDeps = new ArrayList<>();
				log.log(Level.FINEST,"Checking the subtree of the textual unit {0}. ", new Object[]{opposite.toString()});
				SynDependency.getSubtreeDependenciesWithTypes(embeddings, opposite, NP_INTERNAL_DEPS,subtreeNpDeps, otherDeps);
				for (SynDependency c: subtreeNpDeps) {
					surfsForRemovalSingle.add(c.getDependent());
				}
			}
			// encountered a dependent with semantics (and no predicates to the right of the dependent), so ignore
			if (!maySplit && realHeadPredicates == null) continue;
			
			// there is a candidate for collapsing
			int min = Integer.MAX_VALUE;
			int max = 0;
			if (surfsForRemovalSingle.size() > 0){
				for (SurfaceElement so: surfsForRemovalSingle) {
					log.log(Level.FINE,"Removing textual unit due to adverbial phrase transformation: {0}.",new Object[]{so.toString()});
					if (so.getSpan().getEnd() > max) max = so.getSpan().getEnd();
					if (so.getSpan().getBegin() < min) min = so.getSpan().getBegin();
				}
				max = Math.max(max, su.getSpan().getEnd());
				min = Math.min(min, su.getSpan().getBegin());
				SurfaceElement nw = sef.createSurfaceElementIfNecessary(doc, new SpanList(min,max), false);
				log.log(Level.FINE,"Adding new textual unit due to adverbial phrase transformation: {0}.", new Object[]{nw.toString()});
				sent.synchSurfaceElements(nw);
				sent.synchEmbeddings();
			}
						
		}
	}
	
}
