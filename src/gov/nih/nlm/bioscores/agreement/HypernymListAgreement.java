package gov.nih.nlm.bioscores.agreement;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import edu.stanford.nlp.util.CollectionUtils;
import gov.nih.nlm.bioscores.core.CoreferenceProperties;
import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.CoreferenceUtils;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.Conjunction;
import gov.nih.nlm.ling.sem.ConjunctionDetection;
import gov.nih.nlm.ling.sem.SemanticItem;

/** 
 * Implementation of agreement based on pre-defined hypernym lists for semantic groups.
 * This is only useful for nominal mentions and {@link CoreferenceType#Anaphora} and 
 * {@link CoreferenceType#Cataphora} cases. <p>
 * Agreement is predicted if the coreferential mention involves a hypernym for a semantic group
 * that the referent belongs to. For example, <i>this condition</i> is considered compatible
 * with <i>hypertension</i>, assuming that the hypernym list for disorders include
 * <i>condition</i> and <i>hypertension</i> belongs to disorder semantic group.
 * 
 * @author Halil Kilicoglu
 *
 */
public class HypernymListAgreement implements Agreement {
	
	public static final List<String> generalAnaphoricHypernyms = Arrays.asList("former","latter");
	public static final List<String> generalCataphoricHypernyms = Arrays.asList("following");
	
	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement candidate) {
		if ((corefType == CoreferenceType.PredicateNominative || corefType == CoreferenceType.Appositive) && 
				CoreferenceUtils.hasNonCoreferentialSemantics(candidate) == false) return false;
		if (corefType == CoreferenceType.Cataphora && Expression.filterByExpressions(candidate).size() > 0) return false;
		
		if (corefType == CoreferenceType.Cataphora && generalCataphoricHypernyms.contains(exp.getHead().getLemma())) return true;
		if (corefType == CoreferenceType.Anaphora && generalAnaphoricHypernyms.contains(exp.getHead().getLemma())) return true;
		LinkedHashSet<SemanticItem> sems = null;
		if (ConjunctionDetection.filterByConjunctions(candidate).size() > 0) {
			Conjunction conjPred = (Conjunction)ConjunctionDetection.filterByConjunctions(candidate).iterator().next();
			sems =  new LinkedHashSet<>(conjPred.getArgItems());
		} 	
		else sems = candidate.getHeadSemantics();
		if (sems == null) return false;
		for (String h: CoreferenceProperties.HYPERNYMS.keySet()) {
			List<String> hypers = CoreferenceProperties.HYPERNYMS.get(h);
			List<String> semtypes = CoreferenceProperties.SEMTYPES.get(h);
			for (String hy: hypers) {
					if (exp.containsLemma(hy)) {
						for (SemanticItem sem: sems) {
							// allowing hypernyms in candidates caused significant decline when using gold terms
							if (CollectionUtils.containsAny(semtypes,sem.getAllSemtypes()) || hypers.contains(candidate.getHead().getLemma().toLowerCase()))
//							if (CollectionUtils.containsAny(semtypes,sem.getAllSemtypes()))								
								return true;
					}
				}
			}
		}
		return false;
	}
}
