package gov.nih.nlm.bioscores.exp;

import java.util.Arrays;
import java.util.List;

import edu.stanford.nlp.util.CollectionUtils;
import gov.nih.nlm.bioscores.agreement.PersonAgreement;
import gov.nih.nlm.bioscores.core.CoreferenceProperties;
import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.CoreferenceUtils;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.AppositiveDetection;

/**
 * Contains implementations of coreferential mention filtering methods.
 * 
 * @author Halil Kilicoglu
 *
 */
public class ExpressionFilterImpl {
	
	/**
	 * Eliminates rigid designators, noun phrases in appositive 
	 * constructions, pronominal mentions, and cataphoric mentions.
	 *
	 */
	public class AnaphoricityFilter implements ExpressionFilter {
		@Override
		public boolean filter(CoreferenceType corefType, Expression exp) {
			ExpressionType type = ExpressionType.valueOf(exp.getType());
			if (ExpressionType.NOMINAL_TYPES.contains(type) == false) return false;
			SurfaceElement su = exp.getSurfaceElement();
			if (Expression.isCataphoric(type,su)) return false;
			if (CoreferenceUtils.isModified(type,su,true)) return false;
			return (AppositiveDetection.hasSyntacticAppositives(su) == false);
		}
	 }
	
	/**
	 * Keeps nominal cataphoric mentions only.
	 *
	 */
	public class CataphoricityFilter implements ExpressionFilter {

		@Override
		public boolean filter(CoreferenceType corefType, Expression exp) {
			ExpressionType type = ExpressionType.valueOf(exp.getType());
			if (ExpressionType.NOMINAL_TYPES.contains(type) == false) return false;
			SurfaceElement su = exp.getSurfaceElement();
			if (Expression.isCataphoric(type,su) == false) return false;
			if (CoreferenceUtils.isModified(type,su,true)) return false;
			return (AppositiveDetection.hasSyntacticAppositives(su) == false);
		}
	 }
	
	/**
	 * Keeps third person personal and possessive pronoun only.
	 *
	 */
	public class ThirdPersonPronounFilter implements ExpressionFilter {
		@Override
		public boolean filter(CoreferenceType corefType, Expression exp) {
			ExpressionType type = ExpressionType.valueOf(exp.getType());
			SurfaceElement su = exp.getSurfaceElement();
			if (ExpressionType.PersonalPronoun == type ||
				ExpressionType.PossessivePronoun == type) {
				return (PersonAgreement.person(su) == PersonAgreement.Person.Third);
			} 
			return false;
		}
	 }
	
	/**
	 * Eliminates pleonastic <i>it</i> instances.
	 *
	 */
	public class PleonasticItFilter implements ExpressionFilter {

		@Override
		public boolean filter(CoreferenceType corefType, Expression exp) {
			SurfaceElement su = exp.getSurfaceElement();
			Sentence sent= su.getSentence();
			return (CoreferenceUtils.pleonasticIt(su, sent.getEmbeddings()) == false);
		}
	 }
	
	/**
	 * Eliminates relative pronouns that are not coreferential. These are
	 * <i>when</i>, <i>why</i>, <i>how</i>, and <i>what</i>.
	 * 
	 *
	 */
	public class NonCorefRelativePronFilter implements ExpressionFilter {

		@Override
		public boolean filter(CoreferenceType corefType, Expression exp) {
			List<String> nonCorefRelativePronouns = Arrays.asList("when","why","how","what");
			SurfaceElement su = exp.getSurfaceElement();
			if (su.isRelativePronoun() == false) return false;
			return (su.containsAnyLemma(nonCorefRelativePronouns)) == false;
		}
	 }
		
	/**
	 * Eliminates all mentions that do not include a hypernym.
	 * This is currently unused, but could be useful.
	 * 
	 *
	 */
	public class HypernymOnlyFilter implements ExpressionFilter {
		@Override
		public boolean filter(CoreferenceType corefType, Expression exp) {
			SurfaceElement su = exp.getSurfaceElement();
			List<String> allHyper = CollectionUtils.flatten(CoreferenceProperties.HYPERNYMS.values());
			for (String hyp: allHyper) {
				if (su.containsLemma(hyp)) return true;
			}
			return false;
		}
	 }
}
