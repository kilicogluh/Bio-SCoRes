package gov.nih.nlm.ling.transform;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.SemanticItem;


/**
 * A <code>DependencyTransformation</code> implementation that transforms dependencies involving
 * discourse connectives, such as <i>while</i>, <i>on the other hand</i>, etc. <p>
 * This transformation is mostly useful for discourse processing. There are no prerequisites.
 * 
 * @author Halil Kilicoglu
 *
 */
public class DiscourseConnectiveTransformation implements DependencyTransformation {
	private static Logger log = Logger.getLogger(DiscourseConnectiveTransformation.class.getName());
	
	@Override
	public List<Class<? extends DependencyTransformation>> getPrerequisites() {
		return new ArrayList<Class<? extends DependencyTransformation>>();
	}

	/**
	 * Transform dependencies involving discourse connectives. <p> 
	 * For the most part, it assumes that discourse connective semantics have already been attached to the textual units. 
	 * Two types of transformations are applied: <ul>
	 * <li> prep_* dependencies involving discourse connectives (e.g., <i>in contrast</i> is likely to be involved in a prep_in 
	 * dependency) are converted to advmod dependencies. 
	 * <li> advcl and mark dependencies are extracted from prep_* dependencies that involve discourse segment heads.
	 * </ul>
	 * 
	 * @param  sent  the sentence to transform
	 */
	public void transform(Sentence sent) {	
		if (sent.getSurfaceElements() == null || sent.getEmbeddings() == null) {
			log.log(Level.WARNING,"No textual units or embeddings can be found for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		List<Class<? extends DependencyTransformation>> applied  = sent.getTransformations();
		if (applied.containsAll(getPrerequisites()) == false) {
			log.log(Level.WARNING,"The prerequisites for DiscourseConnectiveTransformation are not met for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		transformToAdvmod(sent);
		transformToAdvclMark(sent);
		sent.addTransformation(this.getClass());
	}
	
	// Transform prep_* dependencies involving discourse connectives to advmod dependencies.
	private void transformToAdvmod(Sentence sent) {
		List<SynDependency> markedForAddition = new ArrayList<>();
		List<SynDependency> markedForRemoval = new ArrayList<>();
		List<SynDependency> embeddings = sent.getEmbeddings();
		for (SurfaceElement su: sent.getSurfaceElements()) {
			if (discourseConnective(su)) {
				List<SynDependency> preps = SynDependency.inDependenciesWithType(su, embeddings, "prep", false); 
				if (preps.size() > 0) {
					for (SynDependency prep: preps) {
						String pr = prep.getType().substring(prep.getType().indexOf("_")+1);
						if (su.containsLemma(pr)) {
							markedForRemoval.add(prep);
							log.log(Level.FINE, "Removing dependency due to discourse connective transformation: {0}", new Object[]{prep.toString()});
							SynDependency nd = new SynDependency("DCM" + prep.getId(),"advmod", prep.getDependent(),prep.getGovernor());
							markedForAddition.add(nd);
							log.log(Level.FINE, "Adding dependency due to discourse connective transformation: {0}", new Object[]{nd.toString()});
						} 
					}
				} 
			}
		}
		sent.synchEmbeddings(markedForRemoval, markedForAddition);
	}
	
	// TODO Not sure how well this works.
	private void transformToAdvclMark(Sentence sent) {
		if (sent.getTransformations().contains(CoordinationTransformation.class)) {
			log.warning("Discourse connective transformation should be applied before coordination transformation.");
			return;
		}
		List<SynDependency> markedForAddition = new ArrayList<>();
		List<SynDependency> markedForRemoval = new ArrayList<>();
		List<SynDependency> embeddings = sent.getEmbeddings();
		for (SynDependency d: embeddings) {
			if (d.getType().startsWith("prep_") || d.getType().startsWith("prepc_")) {
				String sub = d.getType().substring(d.getType().indexOf("_")+1);
				// prep_such_as
				sub = sub.replaceAll("_", " ");
				log.log(Level.FINEST,"Discourse connective transformation with {0}.", new Object[]{sub});
				SpanList spans = sent.getSpansWithText(sub,false);
				if (spans != null)  {
					SurfaceElement dependent = d.getDependent();
					SurfaceElement governor = d.getGovernor();
					for (Span sp: spans.getSpans()) {
						SurfaceElement surf = sent.getSubsumingSurfaceElement(new SpanList(sp));
						if (surf == null) continue; // TODO This should not happen, but does, need to check why. (in EPI)
						if (surf.equals(governor) || surf.equals(dependent)) continue;
						// isPrepositional is dangerous, because multi-word items may not satisfy
						if (spans.size() > 1 && surf.isPrepositional() == false) continue;
						if (surf.hasSemantics()) {
							// When the governor or dependent is coordinated, we end up losing information
							// "A case of metabolic acidosis, acute renal failure and hepatic failure following paracetamol ingestion is presented"
							// TODO Certainly need to rethink for the discourse level
							if (discourseConnective(surf)) {
								markedForRemoval.add(d);
								log.log(Level.FINE, "Removing dependency due to discourse connective clause transformation: {0}", new Object[]{d.toString()});
								SynDependency nm = new SynDependency("DCM_" + d.getId(), "mark",surf, dependent);
								markedForAddition.add(nm);
								log.log(Level.FINE, "Adding dependency due to discourse connective clause transformation: {0}", new Object[]{nm.toString()});
								SynDependency na = new SynDependency("DCA_" + d.getId(), "advcl",surf, governor);
								markedForAddition.add(na);
								log.log(Level.FINE, "Adding dependency due to discourse connective clause transformation: {0}", new Object[]{na.toString()});
								for (SynDependency sd: SynDependency.dependenciesWithType(governor, embeddings, "conj", false)) {
									SurfaceElement other = (sd.getGovernor().equals(governor) ? sd.getDependent() : sd.getGovernor());
									List<SynDependency> otherDeps = SynDependency.findDependenciesBetween(embeddings, other, dependent, false);
									if (otherDeps.size() > 0) continue;
									markedForAddition.add(new SynDependency("DCAC_" + sd.getId(),"advcl",surf,other));
								}
								for (SynDependency sd: SynDependency.dependenciesWithType(dependent, embeddings, "conj", false)) {
									SurfaceElement other = ( sd.getGovernor().equals(dependent) ? sd.getDependent() : sd.getGovernor());
									List<SynDependency> otherDeps = SynDependency.findDependenciesBetween(embeddings, other, governor, false);
									if (otherDeps.size() > 0) continue;
									SynDependency nmc = new SynDependency("DCMC_" + sd.getId(),"mark",surf,other);
									markedForAddition.add(nmc);
									log.log(Level.FINE, "Adding dependency due to discourse connective clause transformation: {0}", new Object[]{nmc.toString()});
								}
								for (SynDependency sd: SynDependency.inDependencies(governor, embeddings)) {
									if (sd.getType().startsWith("conj")) continue;
									if (sd.getGovernor().equals(surf)) continue;
									markedForRemoval.add(sd);
									log.log(Level.FINE, "Removing dependency due to discourse connective clause transformation: {0}", new Object[]{sd.toString()});
									SynDependency ncc = new SynDependency("DCC_" + sd.getId(),sd.getType(),sd.getGovernor(),surf);
									markedForAddition.add(ncc);
									log.log(Level.FINE, "Adding dependency due to discourse connective clause transformation: {0}", new Object[]{ncc.toString()});
								}
							}
							// TODO This doesn't have anything to do with discourse connectives, so maybe move from there. 
							else if ((surf.getIndex() > governor.getIndex() && surf.getIndex() < dependent.getIndex()) ||
									(surf.getIndex() < governor.getIndex() && surf.getIndex() > dependent.getIndex()))  {
									// this is for subordinating conjunctions? Despite X, Y..
//									(si.getIndex() < governor.getIndex() && si.getIndex() < dependent.getIndex())) {
									// This seems simplistic. 2/10/2015.
//									dependent.addSemantics(surf.getSemantics());
									markedForRemoval.add(d);
									markedForAddition.add(new SynDependency("DCP_" + d.getId(), 
											"pobj",surf,dependent));
									markedForAddition.add(new SynDependency("DCP_" + d.getId(), 
											"prep",governor,surf));
								}
							}
						}
					}
					
			}
		}
		for (SurfaceElement su: sent.getSurfaceElements()) {
			// TODO secondary to, maybe too adhoc?? Does it generalize?
			if (discourseConnective(su) && su.isAdjectival()) {
				List<SynDependency> adjs =SynDependency.inDependenciesWithType(su, embeddings, "amod",true); 
				if (adjs.size() > 0) {
					for (SynDependency adj: adjs) {
						if (SpanList.atLeft(adj.getGovernor().getSpan(),su.getSpan())) {
							markedForRemoval.add(adj);
							log.log(Level.FINE, "Removing dependency due to discourse connective clause transformation: {0}", new Object[]{adj.toString()});
							if (SynDependency.findDependenciesBetween(markedForAddition, adj.getDependent(), adj.getGovernor(), true).size() > 0) continue;
							SynDependency na = new SynDependency("DCA" + adj.getId(),"advcl",adj.getDependent(),adj.getGovernor());
							markedForAddition.add(na);
							log.log(Level.FINE, "Adding dependency due to discourse connective clause transformation: {0}", new Object[]{na.toString()});
						}
					}
				}
			}
		}
		sent.synchEmbeddings(markedForRemoval, markedForAddition);
	}
	
	// TODO may consider moving to AbstractSurfaceElement
	private boolean discourseConnective(SurfaceElement surf) {
		LinkedHashSet<SemanticItem> predicates = surf.filterByPredicates();
		if (predicates != null) {
			for (SemanticItem si: predicates) {
				Predicate pr = (Predicate)si;
				if (pr.getIndicator() != null && pr.getAssociatedSense().isDiscourseConnective()) {
					return true;
				}
			}
		}
		return false;
	}

}
