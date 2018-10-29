package gov.nih.nlm.bioscores.agreement;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.core.CoreferenceProperties;
import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.CoreferenceUtils;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.sem.Conjunction;
import gov.nih.nlm.ling.sem.ConjunctionDetection;
import gov.nih.nlm.ling.sem.SemanticItem;

/**
 * Implementation of number agreement. 
 * Valid number values are {@link NumberAgreement.Number#Singular} and  
 * {@link NumberAgreement.Number#Plural}. 
 * 
 * Agreement is predicted if the number of the coreferential mention and
 * the candidate referent match.
 * 
 * @author Halil Kilicoglu
 *
 */
public class NumberAgreement implements Agreement {
	private static Logger log = Logger.getLogger(NumberAgreement.class.getName());
		
	public static enum Number {
		Singular, Plural;
		public String toString() {
			switch(this) {
				case Singular: return "Sg";
				case Plural: return "Pl";
			}
			return null;
		}
	}
	
	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		log.log(Level.FINEST, "Mention_Referent number: {0}_{1}.", new Object[]{number(exp).toString(),number(referent).toString()});
		if (isTwo(exp)) {
			Set<SemanticItem> conjs = ConjunctionDetection.filterByConjunctions(referent);
			if (conjs.size() > 0) {
				Conjunction conjPred = (Conjunction)conjs.iterator().next();
				return (conjPred.getArgItems().size() == 2);
			} else return false;
		}
		return number(exp) == number(referent);
	}
	
	/**
	 * Get the number value for a textual unit. <p>
	 * Coordinating conjunctions, mentions associated with {@link Conjunction} relations
	 * and with collective nouns (such as <i>family</i>) are considered {@link NumberAgreement.Number#Plural}.
	 * Otherwise, the part-of-speech information is used to determine the number.
	 *
	 * @param si  the textual unit
	 * @return {@link NumberAgreement.Number#Singular} (default) or {@link NumberAgreement.Number#Plural} 
	 */
	public static Number number(SurfaceElement si) {
		if (si.isCoordConj()) return Number.Plural;
		if (Expression.filterByExpressions(si).size() > 0) {
			if (ExpressionType.getTypes(si).contains(ExpressionType.DistributivePronoun) ||
				ExpressionType.getTypes(si).contains(ExpressionType.ReciprocalPronoun) ||
				ExpressionType.getTypes(si).contains(ExpressionType.DistributiveNP))
				return Number.Plural;
		}
		if (ConjunctionDetection.filterByConjunctions(si).size() > 0) return Number.Plural;
		if (si.isPronominal()) {
			String p = CoreferenceUtils.getPronounToken(si);
			if (p.matches("^(they|them|we|us|their|ours|our|theirs|themselves|ourselves|yourselves)$")) {
				return Number.Plural;
			} else {  //if (p.matches("^(it|its|that|this|he|him|his|she|her)$")) {
				return Number.Singular;
			}
		} 
		Word headWord = si.getHead();
		String pos = headWord.getPos();
		String lemma = headWord.getLemma();
		// TODO mass nouns? very preliminary
		if (CoreferenceProperties.COLLECTIVE_NOUNS.contains(lemma)) return Number.Plural;
		if (pos.matches("^NNP?$"))  return Number.Singular;
		if (pos.matches("^NNP?S$")) return Number.Plural;
		return Number.Singular;
	}
	
	private static boolean isTwo(SurfaceElement si) {
		return (Expression.filterByExpressions(si).size() > 0 && 
			    (ExpressionType.getTypes(si).contains(ExpressionType.DistributivePronoun)) ||
				(ExpressionType.getTypes(si).contains(ExpressionType.ReciprocalPronoun)) ||
				(ExpressionType.getTypes(si).contains(ExpressionType.DistributiveNP)) ||
				si.containsLemma("two") || si.containsLemma("2"));
	}
}
