package gov.nih.nlm.bioscores.agreement;

import java.util.List;
import java.util.Set;

import gov.nih.nlm.bioscores.core.CoreferenceProperties;
import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.CoreferenceUtils;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.ConjunctionDetection;
import gov.nih.nlm.ling.sem.Predication;
import gov.nih.nlm.ling.sem.SemanticItem;

/**
 * Implementation of agreement constraint based on animacy.
 * Animacy values are {@link AnimacyAgreement.Animacy#Animate}, {@link AnimacyAgreement.Animacy#NonAnimate},
 * and {@link AnimacyAgreement.Animacy#MaybeAnimate}.
 * 
 * Agreement is predicted if the coreferential mention and the candidate referent have the same
 * animacy OR one is {@link AnimacyAgreement.Animacy#MaybeAnimate} or has no animacy value.
 * 
 * @author Halil Kilicoglu
 *
 */
public class AnimacyAgreement implements Agreement {

	private static final List<String> POPULATION_SEMTYPES = CoreferenceProperties.SEMTYPES.get("POPL");

	public static enum Animacy {
		Animate, NonAnimate, MaybeAnimate;
		public String toString() {
			switch(this) {
				case Animate: return "Anim";
				case NonAnimate: return "NAnim";
				case MaybeAnimate: return "MaybeAnim";
			}
			return null;
		}
	}
	
	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		Animacy expAnim = animacy(exp);
		Animacy refAnim = animacy(referent);
		if(	(expAnim==null || expAnim==Animacy.NonAnimate || expAnim==Animacy.MaybeAnimate) && 
			(refAnim==null || refAnim==Animacy.NonAnimate || refAnim==Animacy.MaybeAnimate))
			return true;
		if(	(expAnim==Animacy.Animate || expAnim==Animacy.MaybeAnimate) && 
			(refAnim==Animacy.Animate || refAnim==Animacy.MaybeAnimate))
			return true;
		return expAnim==refAnim;
	}
	
	/**
	 * Gets the animacy value of a textual unit.<p>
	 * Uses pronoun information, if any, and whether the textual unit refers to a population.
	 * 
	 * @param si  the textual unit in consideration
	 * @return	  the animacy value of the textual unit, or null.
	 */
	public static Animacy animacy(SurfaceElement si) {
		if (si.isPronominal()) {
			String p = CoreferenceUtils.getPronounToken(si);
			if (p.matches("^(me|he|him|his|she|her|hers|we|us|our|ours|i|my|mine|you|yours|himself|herself|ourselves|myself|who|whose|whom)$")) {
				return Animacy.Animate;
			} else if (p.matches("^(it|its|itself|which|where)$")) {
				return Animacy.NonAnimate;
			}else if (p.matches("^(they|their|theirs|them|themselves)$")) {
				return Animacy.MaybeAnimate;
			}
		} else if (si.isDeterminer()) {
			String p = CoreferenceUtils.getDeterminerToken(si);
			if (p.matches("^(those|these|this|that)$")) return Animacy.MaybeAnimate;
		} 
		if (hasPopulationSemanticType(si) || hasPopulationSemanticTypes(si)) return Animacy.Animate;
		return null;
	}
	
	private static boolean hasPopulationSemanticTypes(SurfaceElement si) {
		Set<SemanticItem> conjs = ConjunctionDetection.filterByConjunctions(si);
		Predication conjPred = null;
		if (conjs.size() > 0) {
			conjPred = (Predication)conjs.iterator().next();
		} else return false;
		List<SemanticItem> args = conjPred.getArgItems();
		for (SemanticItem a: args) {
			if (!hasPopulationSemanticType(a)) return false;
		}
		return true;
	}
	
	private static boolean hasPopulationSemanticType(SurfaceElement si) {
		Set<SemanticItem> sems = si.getSemantics();
		if (sems != null) {
			for (SemanticItem s: sems) 
				if (hasPopulationSemanticType(s)) return true;
		}
		return false;
	}
	
	private static boolean hasPopulationSemanticType(SemanticItem si) {
		if (POPULATION_SEMTYPES == null) return false;
		Set<String> semtypes = si.getAllSemtypes();
		for (String semtype: semtypes) 
			if (POPULATION_SEMTYPES.contains(semtype)) return true;
		return false;
	}
}
