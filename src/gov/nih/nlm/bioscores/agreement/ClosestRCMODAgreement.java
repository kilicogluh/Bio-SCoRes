package gov.nih.nlm.bioscores.agreement;

import java.util.LinkedHashSet;
import java.util.List;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.sem.ConjunctionDetection;

/**
 * The agreement based on relative clause dependency relation
 * between the coreferential mention and the candidate referent. 
 * It can be used for relative pronoun anaphora. <p>
 * 
 * Agreement is predicted if the candidate referent is the head of a 
 * relative clause whose dependent is to the right of the relativizer (the anaphor). 
 * 
 * @author Halil Kilicoglu
 *
 */
public class ClosestRCMODAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		Sentence sent = exp.getSentence();
		List<SynDependency> embeddings = sent.getEmbeddings();
		List<SynDependency> allRcmodDeps = SynDependency.dependenciesWithType(embeddings, "rcmod", true);
		if (allRcmodDeps.size() > 0) {
			SurfaceElement closest = null;
			for (SynDependency r: allRcmodDeps) {
				SurfaceElement d = r.getDependent();
				SurfaceElement g = r.getGovernor();
				if (SpanList.atLeft(d.getSpan(),exp.getSpan())) continue;
				if (SpanList.atLeft(exp.getSpan(),g.getSpan())) continue;
				if (closest == null || SpanList.atLeft(closest.getSpan(), g.getSpan())) {
					closest = g;
				}
			}
			if (closest == null) return false;
			if (closest.equals(referent)) return true;
			// handle conjunctions
			if (ConjunctionDetection.isConjunctionArgument(referent, closest))  {
				LinkedHashSet<SurfaceElement> conjArgs = ConjunctionDetection.getConjunctSurfaceElements(referent);
				if (conjArgs.size() < 2) return false;
				for (SurfaceElement arg: conjArgs) {
					if (arg.equals(closest)) continue;
					if (SpanList.atLeft(exp.getSpan(), arg.getSpan())) return false;
					List<SynDependency> outRcMods = SynDependency.outDependencies(arg, allRcmodDeps);
					for (SynDependency o: outRcMods) {
						if (SpanList.atLeft(o.getDependent().getSpan(),exp.getSpan())) return false;
					}
				}
				return true;
			} 
		}
		return false;
	}
}
