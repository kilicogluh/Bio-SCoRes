package gov.nih.nlm.bioscores.exp;

import java.util.Arrays;
import java.util.List;

import gov.nih.nlm.bioscores.agreement.PersonAgreement;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * Recognition of reflexive pronoun mentions. 
 * It is limited to third person pronouns.
 * 
 * @author Halil Kilicoglu
 *
 */
public class ReflexivePronounOps implements ExpressionOps {
	public static final List<String> REFLEXIVE_PRONOUNS = 
			Arrays.asList("myself","yourself","herself","himself","itself","ourselves","yourselves","themselves");
	
	@Override
	public boolean recognize(SurfaceElement surfaceElement) {
		if (surfaceElement.isPronominal() == false) return false;
		if (PersonAgreement.person(surfaceElement) != PersonAgreement.Person.Third) return false;
		return (isReflexive(surfaceElement));
	}
	
	/**
	 * Indicates whether a textual unit is a reflexive pronoun.
	 * It considers reflexive pronouns of all persons (first, second, and third).
	 * 
	 * @param su  the textual unit in question
	 * @return true if the textual unit contains a reflexive pronoun
	 */
	public static boolean isReflexive(SurfaceElement su) {
		return su.getHead().getText().matches("^(myself|yourself|itself|himself|herself|ourselves|yourselves|themselves)$");
	}

}
