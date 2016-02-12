package gov.nih.nlm.bioscores.exp;

import java.util.Arrays;
import java.util.List;

import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;

/**
 * Recognition of possessive pronoun mentions.
 * 
 * @author Halil Kilicoglu
 *
 */
public class PossessivePronounOps implements ExpressionOps {
	public static final List<String> POSSESSIVE_PRONOUNS = 
			Arrays.asList("my","your","her","his","its","our","their", "mine","yours","hers","ours","theirs");
	
	@Override
	public boolean recognize(SurfaceElement surfaceElement) {
		if (surfaceElement.isPronominal() == false ) return false;
		return (isPossessive(surfaceElement));
	}
	
	/**
	 * Determines whether a textual unit contains a possessive pronoun
	 * 
	 * @param su  the textual unit in question
	 * @return true if the textual unit contains a possessive pronoun
	 */
	public static boolean isPossessive(SurfaceElement su) {
		return getPossessivePronoun(su) != null;
	}
	
	/**
	 * Gets the possessive pronoun in a textual unit, if any.
	 * 
	 * @param su  the textual unit in question
	 * @return the possessive pronoun in the textual unit, if any, null otherwise	
	 */
	public static Word getPossessivePronoun(SurfaceElement su) {
		for (Word w: su.toWordList()) 
			if (w.getPos().equals("PRP$"))  return w;
		return null;
	}

}
