package gov.nih.nlm.ling.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;

/**
 * A <code>DependencyTransformation</code> implementation that transforms the dependencies 
 * that a verb is involved in as the dependent.<p> 
 * This transformation is mainly intended to make dependencies involving passive and copular constructions,
 * external and clausal complements more explicit to reflect semantic scope relations more faithfully.<p>
 * It requires that <code>DependencyDirectionReversal</code> transformation has been performed.
 * 
 * @author Halil Kilicoglu
 *
 */

public class VerbComplexTransformation implements DependencyTransformation {
	private static Logger log = Logger.getLogger(VerbComplexTransformation.class.getName());
	
	private static final List<String> SUBJ_DEPENDENCY_TYPES = Arrays.asList("nsubj","csubj","xsubj");
	
	@Override
	public List<Class<? extends DependencyTransformation>> getPrerequisites() {
		List<Class<? extends DependencyTransformation>> prereqs = 
				new ArrayList<Class<? extends DependencyTransformation>>();
		prereqs.add(DependencyDirectionReversal.class);
		return prereqs;
	}
	
	/**
	 * Transforms a sentence by addressing the verbal complexes.<p>
	 * It performs the following operations sequentially: <ul>
	 * <li> making explicit passive constructions
	 * <li> making clausal complement dependencies more specific based on the complementizer
	 * 	    and pruning redundant dependencies
	 * <li> reordering embedding relations, in which the verb is the dependent, 
	 *      into an embedding chain ending in the verb so as to reflect the semantic scope relations 
	 *      between the surface elements explicitly.
	 * <li> making the subjects of by-clauses more explicit
	 * <li> rearranging copular dependencies
	 * <li> making the subject of external complements (xcomp) more explicit
	 * </ul>
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
			log.log(Level.WARNING,"The prerequisites for VerbComplexTransformation are not met for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		transformPassive(sent);
		transformVerbComplex(sent);
		transformByClause(sent);
		transformCopula(sent);
		transformXcomp(sent);
		sent.addTransformation(this.getClass());
	}
	
	/**
	 * Creates a direct dependency between a predicate and its passive subject.
	 * 
	 * @param sent  the sentence to transform
	 */
	private synchronized void transformPassive(Sentence sent) {
		List<SynDependency> embeddings = sent.getEmbeddings();
		List<SynDependency> markedForAddition = new ArrayList<>();
		List<SynDependency> markedForRemoval = new ArrayList<>();
		for (SurfaceElement su: sent.getSurfaceElements()) {
			if (su.isVerbal()) {
				boolean auxpass = SynDependency.inDependenciesWithType(su, embeddings, "auxpass", true).size() > 0;
				if (auxpass) {
					List<SynDependency> outSubjEdges = 
							SynDependency.outDependenciesWithTypes(su, embeddings, SUBJ_DEPENDENCY_TYPES , true);
					for (SynDependency o: outSubjEdges) {
						markedForRemoval.add(o);
						log.log(Level.FINE, "Removing dependency due to passive transformation: {0}.", new Object[]{o.toString()});
						SynDependency d = new SynDependency("VPA_" + o.getId(),o.getType() + "pass",o.getGovernor(),o.getDependent());
						markedForAddition.add(d);
						log.log(Level.FINE, "Adding dependency due to passive transformation: {0}.", new Object[]{d.toString()});
					}
				}
			}
		}
		sent.synchEmbeddings(markedForRemoval,markedForAddition);
	}
	
	/**
	 * Transforms a sentence by transforming the verb complexes. 
	 * 
	 * @param  sent  the sentence to transform
	 */
	// TODO: Needs more work, too complex as it is. The original removed 'surfsForRemoval' surface elements.
	// Not sure that is necessary.
	private synchronized void transformVerbComplex(Sentence sent) {
		List<SynDependency> embeddings = sent.getEmbeddings();
		List<SynDependency> markedForRemoval = new ArrayList<>();
		List<SynDependency> markedForAddition = new ArrayList<>();
		List<SurfaceElement> surfsForRemoval = new ArrayList<>();
		List<String> maybeUnnecessaryTypes = Arrays.asList("aux","auxpass");	
			
		for (SurfaceElement su: sent.getSurfaceElements()) {
			List<SynDependency> inDeps = SynDependency.inDependencies(su, embeddings);
			if (inDeps.size() < 2) continue;
			if (isPredicative(su,embeddings)) {
				log.log(Level.FINEST,"Potential verb complex head: {0}.", new Object[]{su.toString()});
				// sort by proximity
				// previously, it was by text order, but it causes all kinds of problems.
				// one particular issue is with complements and adverbial clauses.
				// there is no easy way to figure out which scopes over the other.
				// proximity seems like a good compromise, but will probably need more refinement.
				// One example that drove this comparator is PMC-1920263-15-DISCUSSION.
				// The sentence: "In addition, we observed that the A3G promoter was not inducible by IFN-alpha 
				// "or IFN-gamma in A3.01 T cells, whereas in the hepatic cell line HepG2, a moderate induction was measured."			
				Collections.sort(inDeps, new Comparator<SynDependency>() {
					public int compare(SynDependency a, SynDependency b) {
						if (a.getDependent().equals(b.getDependent())) {
							if (a.getType().contains("comp") && !(b.getType().contains("comp"))) return -1;
							if (!(a.getType().contains("comp")) && b.getType().contains("comp")) return 1;
							int aindex = a.getGovernor().getIndex();
							int bindex = b.getGovernor().getIndex();
							// the main verb index
							int cindex = a.getDependent().getIndex();
							int diffa = Math.abs(aindex-cindex);
							int diffb = Math.abs(bindex-cindex);
							if (diffa==diffb) return 1;
							return diffb-diffa;
						}
						else return -1;
					}
				});
				
				SurfaceElement subW = null;
				SynDependency subDep = null;
				String compType = null;
				SynDependency nD = null;
				for (SynDependency e: inDeps) {
					if (e.getType().equals("dep")) continue;
					SurfaceElement opposite = e.getGovernor();
					if (maybeUnnecessaryTypes.contains(e.getType()) && 
						!(opposite.getPos().startsWith("MD")) && !(opposite.getPos().startsWith("RB"))) {
						markedForRemoval.add(e);
						surfsForRemoval.add(opposite);
						continue;
					}
					// may need to consider aux - to for xcomp
					if (e.getType().equals("complm") || e.getType().equals("mark")) {
						markedForRemoval.add(e);
						surfsForRemoval.add(opposite);
						compType = e.getGovernor().getText().toLowerCase();
						continue;
					}
					if (subW == null) {
						subW = opposite; 
						subDep = e;
						markedForRemoval.add(e);
						continue;
					}
					if (compType != null && subDep.getType().endsWith("comp")) {
						if (compType.equals("if")) compType = "whether";
						nD = new SynDependency("VCM_" + subDep.getId(),compType + "_comp",
					               subW,opposite);

						compType = null;
					} else 
						nD = new SynDependency("VCM_" + subDep.getId(),subDep.getType(),
									               subW,opposite);
					log.log(Level.FINE,"Adding dependency due to verb complex transformation: {0}.", new Object[]{nD.toString()});
					// avoid a loop
					if (SynDependency.findDependencyPath(embeddings, opposite, subW, true) == null) {
						SynDependency toRemove = SynDependency.findDependenciesBetween(embeddings, opposite, su, false).get(0);
						log.log(Level.FINE,"Removing dependency due to verb complex transformation: {0}.", new Object[]{toRemove.toString()});
						markedForRemoval.add(toRemove);
						log.log(Level.FINE,"Adding dependency due to verb complex transformation: {0}.", new Object[]{nD.toString()});
						markedForAddition.add(nD);
						subW = opposite;
						subDep = e;
					}  else 
						log.log(Level.FINE,"Did not add verb complex dependency to avoid loop: {0}.", new Object[]{nD.toString()});
				}
				if (compType != null && subDep != null && subDep.getType().endsWith("comp")) {
					if (compType.equals("if")) compType = "whether";
					nD = new SynDependency("VCM_" + subDep.getId(),compType + "_comp",
				               subW,su);
					compType = null;
				} else {
					if (subDep == null || subW == null) continue;
					// TODO Can re-create an existing dependency, which is not great
//					if (SynDependency.findDependenciesBetween(embeddings, subW, su, true) == null) 
						nD = new SynDependency("VCM_" + subDep.getId(),subDep.getType(),
				               subW,su);
				}
				log.log(Level.FINE,"Adding dependency due to verb complex dependency: {0}.", new Object[]{nD.toString()});
				
				if (nD != null && SynDependency.findDependencyPath(embeddings, su, subW, true) == null ){
					log.log(Level.FINE,"Adding final verb complex dependency: {0}.", new Object[]{nD.toString()});
					markedForAddition.add(nD);
				}
			}
		}	
		sent.synchEmbeddings(markedForRemoval, markedForAddition);
	}
	

	/**
	 * Transforms by-clauses by making their subject explicit.
	 * 
	 * @param sent  the sentence to transform
	 */
	private synchronized void transformByClause(Sentence sent) {
		List<SynDependency> embeddings = sent.getEmbeddings();
		List<SynDependency> markedForAddition = new ArrayList<>();
		for (SurfaceElement su: sent.getSurfaceElements()) {
			if (su.isVerbal() || su.isAdjectival()) {
				List<SynDependency> outDeps = SynDependency.outDependencies(su, embeddings);
				if (outDeps.size() < 2) continue;
				LinkedHashSet<SurfaceElement> prepcBys = new LinkedHashSet<>();
				LinkedHashSet<SurfaceElement> subjs = new LinkedHashSet<>();
				String id = null;
				for (SynDependency o: outDeps) {
					SurfaceElement target = o.getDependent();
					if (o.getType().equals("prepc_by") && target.isVerbal()) prepcBys.add(target);
					else if (SUBJ_DEPENDENCY_TYPES.contains(o.getType()) && target.isNominal()) {
						subjs.add(target);
						id = o.getId();
					}
				}
				if (subjs.size() > 0 && prepcBys.size() > 0) {
					int c = 0;
					for (SurfaceElement prepc: prepcBys) {
						for (SurfaceElement subj: subjs) {
							SynDependency d = new SynDependency("VBY_" + id + "_" + ++c,"subj",prepc,subj);
							markedForAddition.add(d);
							log.log(Level.FINE, "Adding dependency due to by-clause transformation: {0}.", new Object[]{d.toString()});
						}
					}
				}
			}
		}
		sent.synchEmbeddings(null,markedForAddition);
	}
	
	/**
	 * Transforms the sentence by rearranging copular dependencies.
	 * 
	 * @param s  the sentence to transform
	 */
	private synchronized void transformCopula(Sentence s) {
		List<SynDependency> embeddings = s.getEmbeddings();
		List<SynDependency> markedForAddition = new ArrayList<>();
		List<SynDependency> markedForRemoval = new ArrayList<>();
		for (SurfaceElement su: s.getSurfaceElements()) {
			if (su.isVerbal()) {
				boolean copula = su.containsLemma("be");
				if (!copula) continue;
				List<SynDependency> outDeps = SynDependency.outDependenciesWithType(su, embeddings, "cop", true);
				if (outDeps.size() == 0) continue;
				markedForRemoval.addAll(outDeps);
				LinkedHashSet<SurfaceElement> preds = SynDependency.getPredecessors(su, embeddings);
				LinkedHashSet<SurfaceElement> cops = SynDependency.getSuccessors(su, outDeps);
				if (preds.size() > 0 && cops.size() > 0) {
					for (SurfaceElement pred: preds) {
						// Assuming there is only a single dependency between two elements, but that may not be the case
						SynDependency pr = SynDependency.findDependenciesBetween(embeddings, pred, su, true).get(0);
						if (pr == null) continue;
						int n = 0;
						for (SurfaceElement c: cops) {
							if (markedForRemoval.contains(pr) == false) {
								markedForRemoval.add(pr);
								log.log(Level.FINE, "Removing dependency due to copular transformation: {0}.", new Object[]{pr.toString()});
							}
							if (SynDependency.findDependenciesBetween(embeddings, c,pred,true).size() == 0) {
								SynDependency d = new SynDependency("VCP_" + pr.getId() + "_" + ++n,pr.getType(),pred,c);
								markedForAddition.add(d);
								log.log(Level.FINE, "Adding dependency due to copular transformation: {0}.", new Object[]{d.toString()});
							}
						}
					}
				}
					
			}
		}
		s.synchEmbeddings(markedForRemoval,markedForAddition);
	}
	
	/**
	 * Transform the sentence by adding new subject dependencies where external complements are involved.
	 * 
	 * @param s  the sentence to transform
	 */
	private synchronized void transformXcomp(Sentence s) {
		List<SynDependency> embeddings = s.getEmbeddings();
		List<SynDependency> markedForAddition = new ArrayList<>();
		List<SynDependency> markedForRemoval = new ArrayList<>();
		List<String> subjTypes = Arrays.asList("nsubj","nsubjpass","csubj","csubjpass");
		for (SurfaceElement su: s.getSurfaceElements()) {
			if (su.isVerbal() || su.isAdjectival()) {
				List<SynDependency> xcompDeps = SynDependency.outDependenciesWithType(su, embeddings, "xcomp", true);
				if (xcompDeps.size() == 0) continue;
				SurfaceElement xcompTarget = xcompDeps.get(0).getDependent();
				List<SynDependency> subjDeps = SynDependency.outDependenciesWithTypes(su, embeddings, subjTypes, true);
				for (SynDependency o: subjDeps) {
					SurfaceElement targ = o.getDependent();
				    if (SynDependency.findDependenciesBetween(embeddings, xcompTarget, targ, true).size() == 0) {
				    	String type = "";
						// it is involved in...., allowing...
				    	// xcompTarget.isVerbal() check may be incorrect.. "X is suspected to be the cause of ..."
						if (su.isVerbal() && xcompTarget.isVerbal() && !(xcompTarget.getPos().contains("VBG")))  type = "x" + o.getType().substring(1);
						else type = "xsubj";
						markedForRemoval.add(o);
						log.log(Level.FINE, "Removing dependency due to external complement transformation: {0}.", new Object[]{o.toString()});
						SynDependency d = new SynDependency("VXC_" + o.getId(), type, xcompTarget,targ);
						markedForAddition.add(d);
						log.log(Level.FINE, "Adding dependency due to external complement transformation: {0}.", new Object[]{d.toString()});
				    }
					
				}
			}
		}
		s.synchEmbeddings(markedForRemoval,markedForAddition);
	}
	
	private boolean isPredicative(SurfaceElement su, List<SynDependency> deps) {
		if (su.isVerbal()) return true;
		// otherwise, make sure they are not in modifier position 
		// (Or possibly we can just check whether they're the head?)
		if (su.isAdjectival() && SynDependency.inDependenciesWithType(su,deps, "cop", true).size() > 0 &&
		  SynDependency.inDependenciesWithType(su, deps, "amod", true).size() == 0) return true;
		if (su.isNominal() && SynDependency.inDependenciesWithType(su,deps, "cop", true).size() > 0 &&
				  SynDependency.inDependenciesWithType(su, deps, "nn", true).size() == 0) return true;
		if (su.isAdverbial() && SynDependency.inDependenciesWithType(su,deps, "cop", true).size() > 0 &&
				  SynDependency.inDependenciesWithType(su, deps, "advmod", true).size() == 0) return true;
		return false;
	}
}