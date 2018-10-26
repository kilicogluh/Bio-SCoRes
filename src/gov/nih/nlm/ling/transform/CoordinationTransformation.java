package gov.nih.nlm.ling.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.sem.ConjunctionDetection;

/**
 * A <code>DependencyTransformation</code> implementation that transforms a <code>Sentence</code> object
 * by decomposing coordination instances. <p>
 * A conjunction dependency in the form of <i>conj_X(a,b)</i>, where <i>X</i> is the coordinating conjunction, is 
 * transformed into two dependencies of the form <i>CC(X,a)</i> and <i>CC(X,b)</i>, where <i>CC</i> is 
 * either <i>cc</i> or <i>negcc</i> (if negated). 
 * Serial coordination is also taken care of. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class CoordinationTransformation implements DependencyTransformation {
	private static Logger log = Logger.getLogger(CoordinationTransformation.class.getName());	

	@Override
	public List<Class<? extends DependencyTransformation>> getPrerequisites() {
		return new ArrayList<Class<? extends DependencyTransformation>>();
	}

	/**
	 * Transforms a sentence by decomposition coordination dependencies.
	 * It attempts to take care of serial coordination, as well.
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
			log.log(Level.WARNING,"The prerequisites for CoordinationTransformation are not met for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		List<SynDependency> embeddings = sent.getEmbeddings();
		List<SurfaceElement> surfsForAddition = new ArrayList<>();				
		List<SynDependency> markedForRemoval = new ArrayList<>();
		List<SynDependency> markedForAddition = new ArrayList<>();
		for (SynDependency e: embeddings) {
			if (e.getType().startsWith("conj_")) {
				SurfaceElement conj = ConjunctionDetection.conjSurfaceElement(sent, e);
				if (conj == null) {
					log.log(Level.FINE,"Skipping..Unable to find the conjunction token for the dependency: {0}. ", new Object[]{e.toString()});
					continue;
				}
				SurfaceElement gov = e.getGovernor();
				SurfaceElement dep = e.getDependent();

				surfsForAddition.add(conj);
				log.log(Level.FINE,"Adding textual unit due to coordination transformation: {0}.", new Object[]{conj.toString()});
				String type1 = (e.getType().equals("conj_nor")? "negcc" : "cc");
				SynDependency nD1 = new SynDependency("CO1_" + e.getId(),type1,conj,gov);
				markedForAddition.add(nD1);
				log.log(Level.FINE,"Adding dependency due to coordination transformation: {0}.", new Object[]{nD1.toString()});
				String type2 = (e.getType().equals("conj_negcc")  || e.getType().equals("conj_nor") ? "negcc" : "cc");
				SynDependency nD2 = new SynDependency("CO2_" + e.getId(),type2,conj,dep);
				markedForAddition.add(nD2);
				log.log(Level.FINE,"Adding dependency due to coordination transformation: {0}.", new Object[]{nD2.toString()});
				markedForRemoval.add(e);
				log.log(Level.FINE,"Removing dependency due to coordination transformation: {0}.", new Object[]{e.toString()});
				
				// rearrange the dependencies that the conjuncts are involved in
				List<SurfaceElement> surfs = new ArrayList<>(); 
				surfs.add(gov); surfs.add(dep);
				// handle nodes that are ancestor to both conjuncts
				SurfaceElement ancestor = SynDependency.getCommonAncestor(embeddings,surfs);
				if (ancestor != null) {
					log.log(Level.FINEST,"Ancestor node for {0} and {1}: {2}.", new Object[]{gov.getText(),dep.getText(),ancestor.toString()});
					SynDependency ancGov = SynDependency.findDependenciesBetween(embeddings, ancestor, gov, true).get(0);
					SynDependency ancDep = SynDependency.findDependenciesBetween(embeddings, ancestor, dep, true).get(0);
					String type = ancGov.getType();
					if (SynDependency.findDependenciesBetween(embeddings, conj, ancestor, true).size() == 0) {
						SynDependency nD = new SynDependency("COA_" + e.getId(),type,ancestor,conj);
						markedForAddition.add(nD);
						log.log(Level.FINE,"Adding dependency due to coordination transformation: {0}.", new Object[]{nD.toString()});
					}
					markedForRemoval.add(ancGov); 
					log.log(Level.FINE,"Removing dependency due to coordination transformation: {0}.", new Object[]{ancGov.toString()});
					markedForRemoval.add(ancDep);
					log.log(Level.FINE,"Removing dependency due to coordination transformation: {0}.", new Object[]{ancDep.toString()});
				}
				// TODO It's an open question how to handle nodes that are ancestors to only one of the conjuncts. 
				// The code below is an attempt, but it caused other problems, which has also to do with Stanford Parser errors.
				// Needs more testing. So far, only looked at PMC-2806624-07.xml (sentence 2) from BIONLP shared task data.
				// Maybe we can have restrictions on semantic types or at least POS of the conjuncts? This would solve the problem with the 
				// sentence mentioned above.
				List<SynDependency> govInDeps = SynDependency.inDependencies(gov, embeddings);
				List<SynDependency>	depInDeps = SynDependency.inDependencies(dep, embeddings);
				for (SynDependency g: govInDeps) {
					if (g.getType().startsWith("conj")) continue;
					if (g.getGovernor().equals(dep)) continue;
					markedForRemoval.add(g);
					log.log(Level.FINE,"Removing dependency due to coordination transformation: {0}.", new Object[]{g.toString()});
					if (SynDependency.findDependenciesBetween(embeddings, conj, g.getGovernor(), true).size() == 0) {
						SynDependency nD3 = new SynDependency("CO3_" +g.getId(),g.getType(),g.getGovernor(),conj);
						markedForAddition.add(nD3);
						log.log(Level.FINE,"Adding dependency due to coordination transformation: {0}.", new Object[]{nD3.toString()});
					}
				}
				for (SynDependency d: depInDeps) {
					if (d.getType().startsWith("conj")) continue;
					if (d.getGovernor().equals(gov)) continue;
					markedForRemoval.add(d);
					log.log(Level.FINE,"Removing dependency due to coordination transformation: {0}.", new Object[]{d.toString()});
					if (SynDependency.findDependenciesBetween(embeddings, conj, d.getGovernor(), true).size() == 0) {
						SynDependency nD4 = new SynDependency("CO4_" +d.getId(),d.getType(),d.getGovernor(),conj);
						markedForAddition.add(nD4);
						log.log(Level.FINE,"Adding dependency due to coordination transformation: {0}.", new Object[]{nD4.toString()});
					}
				} 
			}
		}		
		serialCoordination(sent,surfsForAddition,markedForAddition,markedForRemoval);
		for (SurfaceElement su: surfsForAddition) {
			sent.synchSurfaceElements(su);
		}
		sent.synchEmbeddings(markedForRemoval, markedForAddition);
		sent.addTransformation(this.getClass());
	}

	
	/*
	 * Handle some serial coordination. Most prominently, A, B, and C form should be handled
	 * assuming that the dependencies were extracted correctly.
	 * Example sentences: PMID-9166418 - S4, PMID-8617979, S10
	 */
	private static void serialCoordination(Sentence sent, List<SurfaceElement> newlyAddedSurfs,
			List<SynDependency> newlyAddedDeps, List<SynDependency> removeDeps) {
		List<SurfaceElement> surfs= sent.getSurfaceElements();
		List<SynDependency> embeddings = sent.getEmbeddings();
		for (SurfaceElement su: surfs) {
			List<SynDependency> ccDeps = 
				SynDependency.inDependenciesWithType(su, newlyAddedDeps, "cc", true);
			if (ccDeps.size() >1) {
				// n is shared conjunct.
				log.log(Level.FINEST,"The textual unit is a shared conjunct: {0}.", new Object[]{su.toString()});
				// all the conjunctions involved: for A, B, and C, they will be {comma, and}
				List<SurfaceElement> conjs = 
						new ArrayList<SurfaceElement>(SynDependency.getAllGovernors(ccDeps));
				// sort by position
				Collections.sort(conjs);
				// as as heuristic, use the final word as the head of the coordination
				SurfaceElement conjHead = conjs.get(conjs.size()-1);
				Set<SurfaceElement> conjuncts = new HashSet<>();
				for (SurfaceElement conj: conjs) {
					if (conj.equals(conjHead)) continue;
					List<SynDependency> conjDeps = 
							SynDependency.outDependenciesWithType(conj, newlyAddedDeps, "cc", true);
					if (conjDeps.size() == 0) {
						log.log(Level.WARNING, "Skipping..The conjunction is expected to have out dependencies, but doesn't: {0}.", new Object[]{conj.toString()});
						continue;
					}
					Set<SurfaceElement> currentConjuncts = SynDependency.getAllDependents(conjDeps);
					if (currentConjuncts.size() == 0) {
						log.log(Level.WARNING, "Skipping..The conjunction is expected to have dependents, but doesn't: {0}.", new Object[]{conj.toString()});
						continue;
					}
					conjuncts.addAll(currentConjuncts);
					// we are left with those conjuncts whose incoming cc edges have to redirected
					newlyAddedSurfs.remove(conj);
					Set<SynDependency> inDeps = new HashSet<>();
					for (SynDependency in: newlyAddedDeps) {
						if (in.getDependent().equals(conj))  {
							inDeps.add(in);
							log.log(Level.FINE, "Removing dependency due to serial coordination transformation: {0}", new Object[]{in.toString()});
						}
					}
					newlyAddedDeps.removeAll(inDeps);
					for (SurfaceElement cc: currentConjuncts) {
						SynDependency remove = SynDependency.findDependenciesBetween(conjDeps, conj, cc, true).get(0);
						newlyAddedDeps.remove(remove);
						log.log(Level.FINE, "Removing dependency due to serial coordination transformation: {0}", new Object[]{remove.toString()});
						if (SynDependency.findDependenciesBetween(newlyAddedDeps, conjHead, cc, true).size() == 0) {
							SynDependency nD = new SynDependency("CSE_" + remove.getId(),remove.getType(),conjHead,cc);
							newlyAddedDeps.add(nD);
							log.log(Level.FINE,"Adding dependency due to serial coordination transformation: {0}.", new Object[]{nD.toString()});
						}
					}
				}
				SurfaceElement ancestor = 
					SynDependency.getCommonAncestor(embeddings,new ArrayList<SurfaceElement>(conjuncts));
				if (ancestor != null && SynDependency.findDependenciesBetween(embeddings, conjHead, ancestor, true).size() == 0) {
					for (SurfaceElement conj: conjs) {
						if (conj.equals(conjHead)) continue;
						for (SurfaceElement cc: conjuncts) {
							SynDependency ancDep = SynDependency.findDependenciesBetween(embeddings, ancestor, cc,true).get(0);
							removeDeps.add(ancDep);
							log.log(Level.FINE, "Removing ancestor dependency due to serial coordination transformation: {0}", new Object[]{ancDep.toString()});
						}
					} 
					SynDependency nD = SynDependency.findDependenciesBetween(embeddings, ancestor, su, true).get(0);
					if (nD != null) {
						SynDependency nD2 = new SynDependency("CSE2_" + nD.getId(),nD.getType(),ancestor,conjHead);
						newlyAddedDeps.add(nD2); 
						log.log(Level.FINE,"Adding dependency due to serial coordination transformation: {0}.", new Object[]{nD2.toString()});
					}
				}
			}
		}

	}


}
