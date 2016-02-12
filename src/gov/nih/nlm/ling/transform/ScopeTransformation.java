package gov.nih.nlm.ling.transform;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Sense;
import gov.nih.nlm.ling.sem.Sense.ScopeType;


/**
 * A <code>DependencyTransformation</code> implementation that addresses wide/narrow
 * scope associated with embedding predicates. <p>
 * Whether a predicate has narrow/wide scope is encoded in the embedding dictionary.
 * This transformation has no prerequisites, but clearly, if no embedding semantics
 * are associated with text, no operation will be performed.
 * 
 * @author Halil Kilicoglu
 *
 */
public class ScopeTransformation implements DependencyTransformation {
	private static Logger log = Logger.getLogger(ScopeTransformation.class.getName());	

	
	@Override
	public List<Class<? extends DependencyTransformation>> getPrerequisites() {
		return new ArrayList<Class<? extends DependencyTransformation>>();
	}

	/**
	 * Transforms a sentence by rearranging the dependencies according to whether the predicates 
	 * have narrow/wide scope. Narrow scope transformation is followed by wide scope transformation.<p> 
	 * For example, <i>think</i> has narrow scope. Therefore, in <i>... don't think this will affect that</i>,  
	 * NEG(n't,think), CCOMP(think,will) is rearranged to CCOMP(think,n't), NEG(n't,will). <p>
	 * The determiner <i>no</i> can have wide scope. Therefore, in <i>No meeting is scheduled because the expansion..</i>, 
	 * det(meeting,No), nsubjpass(scheduled,meeting), ADVCL(because,scheduled) are rearranged to
	 * ADVCL(because,No), NEG(No,scheduled), NSUBJPASS(scheduled,meeting).
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
			log.log(Level.WARNING,"The prerequisites for ScopeTransformation are not met for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		transformNarrowScope(sent);
		transformWideScope(sent);
		sent.addTransformation(this.getClass());
	}
	
	private synchronized void transformNarrowScope(Sentence s) {
		List<SurfaceElement> surfs = s.getSurfaceElements();
		List<SynDependency> embeddings = s.getEmbeddings();
		List<SynDependency> markedForAddition = new ArrayList<>();
		List<SynDependency> markedForRemoval = new ArrayList<>();
		for (SurfaceElement su: surfs) {
			List<SynDependency> outDeps = SynDependency.outDependencies(su, embeddings);
			if (outDeps.size() == 0) continue;
			Set<SemanticItem> predicates = su.filterByPredicates();
			if (predicates.size() == 0) continue;
			for (SemanticItem pr: predicates) {
				Predicate p = (Predicate) pr;
				if (p.getScopeType().equals(ScopeType.NARROW)) {
					SurfaceElement compObj = null;
					SynDependency compDep = null;
					for (SynDependency sd: outDeps) {
						if (sd.getType().contains("comp")) { 
							compObj = sd.getDependent();
							compDep= sd;
							break;
						}
					}
					
					LinkedHashSet<SurfaceElement> predecessors = SynDependency.getPredecessors(su, embeddings);
					SurfaceElement negObj = null;
					SynDependency negDep = null;
					for (SurfaceElement si: predecessors) {
						log.log(Level.FINEST,"Predecessor textual unit: {0}.", new Object[]{si.toString()});
						Set<SemanticItem> inPredicates = si.filterByPredicates();
						if (inPredicates == null) continue;
						for (SemanticItem r: inPredicates) {
							Predicate inR = (Predicate) r;
							Sense ss = inR.getAssociatedSense();
							if (ss == null) continue;
							if (ss.getCategory().equals("NEGATOR")) {
								negObj = si;
								negDep = SynDependency.findDependenciesBetween(embeddings, negObj, su, true).get(0);
								break;
							}
						}
						if (negObj != null) break;
					}
					if (negObj != null && compObj != null) {
						log.log(Level.FINEST,"The textual unit has narrow scope: {0}.", new Object[]{su.toString()});
						markedForRemoval.add(compDep);
						log.log(Level.FINE,"Removing dependency due to narrow scope transformation: {0}.", new Object[]{compDep.toString()});
						SynDependency d = new SynDependency("SCN_" + compDep.getId(),compDep.getType(),su,negObj);
						markedForAddition.add(d);
						log.log(Level.FINE,"Adding dependency due to narrow scope transformation: {0}.", new Object[]{d.toString()});
						markedForRemoval.add(negDep);
						log.log(Level.FINE,"Removing dependency due to narrow scope transformation: {0}.", new Object[]{negDep.toString()});
						SynDependency d1 = new SynDependency("SCN_" + negDep.getId(),negDep.getType(),negObj,compObj);
						markedForAddition.add(d1);
						log.log(Level.FINE,"Adding dependency due to narrow scope transformation: {0}.", new Object[]{d1.toString()});
						List<SynDependency> negIn = SynDependency.inDependencies(negObj, embeddings); 
						for (SynDependency sd: negIn) {
							markedForRemoval.add(sd);
							log.log(Level.FINE,"Removing dependency due to narrow scope transformation: {0}.", new Object[]{sd.toString()});
							SynDependency d2 = new SynDependency("SCN_" + sd.getId(),sd.getType(), sd.getGovernor(),su);
							markedForAddition.add(d2);
							log.log(Level.FINE,"Adding dependency due to narrow scope transformation: {0}.", new Object[]{d2.toString()});
						}
						
					}

				}
			}
		}
		s.synchEmbeddings(markedForRemoval, markedForAddition);
	}

	private synchronized void transformWideScope(Sentence sent) {
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		List<SynDependency> embeddings = sent.getEmbeddings();
		List<SynDependency> markedForAddition = new ArrayList<>();
		List<SynDependency> markedForRemoval = new ArrayList<>();
		for (SurfaceElement su: surfs) {
			List<SynDependency> outDeps = SynDependency.outDependencies(su, embeddings);
			if (outDeps.size() == 0) continue;
			Set<SemanticItem> predicates = su.filterByPredicates();
			if (predicates.size() == 0) continue;
			for (SemanticItem tr: predicates) {
				Predicate t = (Predicate) tr;
				if (t.getScopeType().equals(ScopeType.WIDE)) {
					LinkedHashSet<SurfaceElement> predecessors = SynDependency.getPredecessors(su, embeddings);
					for (SurfaceElement si: predecessors) {
						log.log(Level.FINEST,"Predecessor textual unit: {0}.", new Object[]{si.toString()});
						SynDependency sd = SynDependency.findDependenciesBetween(embeddings, si, su, true).get(0);
						markedForRemoval.add(sd);
						// why neg? Only needed for negation terms?
						markedForAddition.add(new SynDependency("wide_" + sd.getId(), "neg", su,si));
						LinkedHashSet<SurfaceElement> siPredecessors = SynDependency.getPredecessors(si, embeddings);
						for (SurfaceElement i: siPredecessors) {
							if (predecessors.contains(i)) continue;
							SynDependency iSd = SynDependency.findDependenciesBetween(embeddings, i, si, true).get(0);
							markedForRemoval.add(iSd);
							log.log(Level.FINE,"Removing dependency due to wide scope transformation: {0}.", new Object[]{iSd.toString()});
							SynDependency d = new SynDependency("SCW_" + sd.getId(), iSd.getType(), i,su);
							markedForAddition.add(d);
							log.log(Level.FINE,"Adding dependency due to wide scope transformation: {0}.", new Object[]{d.toString()});
						}
					}

				}
			}

		}
		sent.synchEmbeddings(markedForRemoval, markedForAddition);
	}

}
