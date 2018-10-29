package gov.nih.nlm.bioscores.agreement;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.CoreferenceUtils;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * Implementation of agreement constraint based on grammatical person.
 * The person values are {@link PersonAgreement.Person#First},
 * {@link PersonAgreement.Person#Second}, and 
 * {@link PersonAgreement.Person#Third}. <p>
 * 
 * Agreement is predicted if the coreferential mention and the candidate
 * referent  have the same grammatical person.
 * 
 * @author Halil Kilicoglu
 *
 */
public class PersonAgreement implements Agreement {

	public static enum Person {
		First, Second, Third;
		public String toString() {
			switch(this) {
			case First: return "1";
			case Second: return "2";
			case Third: return "3";
			default: return null;
			}
		}
	}
	
	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement anaphora, SurfaceElement candidate) {
		return person(anaphora) == person(candidate);
	}
	
	/**
	 * Gets the person value of a textual unit.
	 * 
	 * @param si  the textual unit
	 * @return the person value {@link Person#First}, {@link Person#Second}, or
	 * 			{@link Person#Third} (default)
	 */
	public static Person person(SurfaceElement si) {
		String pronoun = CoreferenceUtils.getPronounToken(si);
		if (pronoun == null) return Person.Third;
		if (pronoun.matches("^(i|me|my|mine|we|our|ours|ourselves|myself|us)$")) {
			return Person.First;
		} else if (pronoun.matches("^(you|your|yours|y'all|y'alls|yinz|yourself|yourselves)$")) {
			return Person.Second;
		} 
		return Person.Third;
	}
	
}
