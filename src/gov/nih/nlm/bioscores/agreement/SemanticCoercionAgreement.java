package gov.nih.nlm.bioscores.agreement;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.CollectionUtils;

import gov.nih.nlm.bioscores.core.CoreferenceProperties;
import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.Conjunction;
import gov.nih.nlm.ling.sem.ConjunctionDetection;
import gov.nih.nlm.ling.sem.SemanticItem;

/** 
 * Agreement constraint that is based on the notion of semantic coercion, 
 * described in Castano and Pustejovsky (2002). It is currently applicable to possessive
 * pronouns. <p>
 * 
 * Agreement is predicted if the possesive pronoun (coreferential mention) modifies a head 
 * that indicates event trigger or a meronym of a given semantic type AND the candidate referent is 
 * associated with semantics of that semantic type. For example, <i>its</i> in the mention 
 * <i>its expression</i> can corefer with a gene name, assuming <i>expression</i> is defined
 * as a event trigger for gene terms.
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO It can be extended to personal pronouns and generalized.
public class SemanticCoercionAgreement implements Agreement {
	
	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		if (expType != ExpressionType.PossessivePronoun) return false;
		LinkedHashSet<SemanticItem> sems = null;
		if (ConjunctionDetection.filterByConjunctions(referent).size() > 0) {
			Conjunction conjPred = (Conjunction)ConjunctionDetection.filterByConjunctions(referent).iterator().next();
			sems =  new LinkedHashSet<>(conjPred.getArgItems());
		} 	
		else sems = referent.getSemantics();
		if (sems == null) return false;
		return (canBeCoerced(CoreferenceProperties.EVENT_TRIGGERS,exp,sems) || 
				canBeCoerced(CoreferenceProperties.MERONYMS,exp,sems));
	}
	
	private static boolean canBeCoerced(Map<String,List<String>> wordList, SurfaceElement exp, LinkedHashSet<SemanticItem> sems) {
		for (String h: wordList.keySet()) {
			List<String> words = wordList.get(h);
			List<String> semtypes = CoreferenceProperties.SEMTYPES.get(h);
			for (String w: words) {
				if (exp.containsLemma(w)) {
					for (SemanticItem sem: sems) {
							if (CollectionUtils.containsAny(semtypes,sem.getAllSemtypes()))								
								return true;
					}
				}
			}
		}
		return false;
	}
}
