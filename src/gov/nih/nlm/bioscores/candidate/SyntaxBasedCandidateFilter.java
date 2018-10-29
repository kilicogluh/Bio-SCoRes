package gov.nih.nlm.bioscores.candidate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.bioscores.exp.ReflexivePronounOps;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;

/**
 * This filter eliminates candidates syntactically linked to the coreferential mention,
 * except when they are expected to be linked syntactically, such as when the coreference
 * involves {@link CoreferenceType#Appositive}, {@link CoreferenceType#PredicateNominative}
 * types or mentions are relative pronouns or reflexive pronouns.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SyntaxBasedCandidateFilter implements CandidateFilter {
	private static Logger log = Logger.getLogger(SyntaxBasedCandidateFilter.class.getName());

	@Override
	public void filter(SurfaceElement exp, CoreferenceType type, ExpressionType expType,
			List<SurfaceElement> candidates, List<SurfaceElement> filteredCandidates) {
		if (candidates == null) return;
		List<SurfaceElement> filteredOut = new ArrayList<>();
		if (type == CoreferenceType.Appositive || 
			type == CoreferenceType.PredicateNominative || 
			type == CoreferenceType.Ontological) {
			log.warning(type.toString() + " and SyntaxBasedCandidateFilter are incompatible.");
			filteredCandidates.addAll(candidates);
			return;

		}
		if (expType == ExpressionType.RelativePronoun) {
			log.warning("Relative pronouns and SyntaxBasedCandidateFilter are incompatible.");
			filteredCandidates.addAll(candidates);
			return;
		}
		if (ReflexivePronounOps.isReflexive(exp)) {
			log.warning("Reflexive pronouns and SyntaxBasedCandidateFilter are incompatible.");
			filteredCandidates.addAll(candidates);
			return;
		}
		Sentence suSent = exp.getSentence();
		for (SurfaceElement cand: candidates) {
			Sentence candSent = cand.getSentence();
			if (candSent.equals(suSent)) {
				List<SynDependency> suCandPath = SynDependency.findDependencyPath(suSent.getEmbeddings(), exp, cand,false);
//				if (suCandPath != null && log.isLoggable(Level.FINEST)) {
				if (suCandPath != null) {
					StringBuffer  buf = new StringBuffer();
					for (SynDependency sd: suCandPath) {
						buf.append(sd.toShortString() +"->");
					}
					log.log(Level.FINEST,"Path from mention to candidate: {0}", 
							new Object[]{buf.toString().trim()});
				}
				boolean syntacticallyLinked = possibleSyntacticPath(suCandPath); 
				if (syntacticallyLinked) {
					filteredOut.add(cand);
					continue;
				}
				// blocking TNF, itself a ... (appos)
//				if (!CoreferenceUtils.isReflexive(anaphora) && directDomination(dog,si,anaphora)) continue;
//				if (!CoreferenceUtils.isReflexive(anaphora) && inSubjectObjectRelationship(dog,si,anaphora)) continue;
				// To John, he was ...  (John != he)
//				if (isSubjectAndAnaphoraInAdjunctPhrase(dog,si,anaphora)) continue;
			} 
			filteredCandidates.add(cand);
		}
	}
	
	// TODO The accuracy of this and the following methods can be improved. Need to examine more examples.
	private boolean possibleSyntacticPath(List<SynDependency> path) {
		if (path == null) return false;
		if (verbalIndicatorPath(path) || nominalIndicatorPath(path)) return true;
		List<SynDependency> appos = SynDependency.dependenciesWithTypes(path,SynDependency.APPOS_DEPENDENCIES, true);
		if (path.size() > 2 || (path.size() > 1 && appos.size() == 0)) return false;
		boolean allValid = true;
		for (SynDependency p: path) {
			String t = p.getType();
			if (appos.contains(p)) continue;
			if (t.contains("obj") || t.contains("subj") || t.startsWith("prep")
					|| t.equals("cc") || Arrays.asList(SynDependency.NP_INTERNAL_DEPENDENCIES).contains(t)) continue;
			allValid = false;
			break;
		}
		return allValid;
	}
	
	private boolean verbalIndicatorPath(List<SynDependency> path) {
		boolean objDep = false;
		boolean subjDep = false;
		for (SynDependency p: path) {
			String t = p.getType();
			if (t.contains("obj") || t.contains("prep")) objDep = true;
			else if (t.contains("subj")) subjDep = true;
			else return false;
		}
		return (path.size() == 2 && objDep && subjDep);
	}
	
	private boolean nominalIndicatorPath(List<SynDependency> path) {
		int count = 0;
		for (SynDependency p: path) {
			String t = p.getType();
			if (t.contains("prep") || Arrays.asList(SynDependency.NP_INTERNAL_DEPENDENCIES).contains(t)) count++;
			else return false;
		}
		return (path.size() == 2 && count == 2);
	}

}
