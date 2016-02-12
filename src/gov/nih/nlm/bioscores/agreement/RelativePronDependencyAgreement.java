package gov.nih.nlm.bioscores.agreement;

import java.util.Arrays;
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
 * Agreement constraint for relative pronoun anaphora based on dependency relations.
 * While it can lead to good precision, recall may not be so good, so it is currently unused.<p>
 * 
 * Agreement is predicted if the coreferential mention is a relative pronoun and the candidate 
 * referent is indicated by the relative clause head.
 * 
 * @author Halil Kilicoglu
 *
 */
public class RelativePronDependencyAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		if (expType != ExpressionType.RelativePronoun) return false;
		Sentence sent = exp.getSentence();
		List<SynDependency> embeddings = sent.getEmbeddings();

		// used to be that the dependency required was 'rel'. Now, it is subject dependencies.
//		List<SynDependency> relDeps = SynDependency.inDependenciesWithType(anaphora, embeddings,"rel", true);
		List<SynDependency> relDeps = SynDependency.inDependenciesWithTypes(exp, embeddings, Arrays.asList("nsubj","csubj"), false);
		if (relDeps.size() > 0) {
			for (SynDependency r: relDeps) {
				SurfaceElement se = r.getGovernor();
				List<SynDependency> rcmodDeps = SynDependency.inDependenciesWithType(se, embeddings, "rcmod", true); 
				if (rcmodDeps.size() > 0 && SynDependency.outDependenciesWithType(referent, rcmodDeps, "rcmod", true).size()> 0) 
					return true;

				// handle conjunctions
				for (SynDependency rc: rcmodDeps) {
					SurfaceElement gov = rc.getGovernor();
					if (ConjunctionDetection.isConjunctionArgument(referent, gov))  {
						LinkedHashSet<SurfaceElement> conjArgs = ConjunctionDetection.getConjunctSurfaceElements(referent);
						if (conjArgs.size() < 2) return false;
						for (SurfaceElement arg: conjArgs) {
							if (arg.equals(gov)) continue;
							if (SpanList.atLeft(exp.getSpan(), arg.getSpan())) return false;
							List<SynDependency> outRcMods = SynDependency.outDependencies(arg, rcmodDeps);
							for (SynDependency o: outRcMods) {
								if (SpanList.atLeft(o.getDependent().getSpan(),exp.getSpan())) return false;
							}
						}
						return true;
					}
				} 
			}
		}
		return false;
	}
}
