package gov.nih.nlm.bioscores.exp;

import java.util.Arrays;
import java.util.List;

import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;

/**
 * Recognition of personal pronoun mentions.
 * 
 * @author Halil Kilicoglu
 *
 */
public class PersonalPronounOps implements ExpressionOps {	
	public static final List<String> PERSONAL_PRONOUNS = 
			Arrays.asList("i","you","she","he","it","we","they","me","him","her",
			"us","them");
	@Override
	public boolean recognize(SurfaceElement surfaceElement) {
		if (isPersonalPronoun(surfaceElement) == false) return false;
		// weird cell name
		if (surfaceElement.getText().equals("iT") ||
			surfaceElement.getText().equals("IT") ||
			surfaceElement.getText().equalsIgnoreCase("s")) return false;
		return true;
	}
	
	/**
	 * Indicates whether a textual unit contains a personal pronoun
	 * 
	 * @param su  the textual unit in question
	 * @return true if the textual unit contains a personal pronoun
	 */
	public static boolean isPersonalPronoun(SurfaceElement su) {
		for (Word w: su.toWordList()) 
			if (w.getPos().equals("PRP")) return true;
		return false;
	}

}
