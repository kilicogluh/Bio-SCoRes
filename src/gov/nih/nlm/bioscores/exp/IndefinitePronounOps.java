package gov.nih.nlm.bioscores.exp;

import java.util.Arrays;
import java.util.List;

import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;

/**
 * Recognition of indefinite pronoun mentions.
 * 
 * @author Halil Kilicoglu
 *
 */
public class IndefinitePronounOps implements ExpressionOps {
	
	public static final List<String> INDEFINITE_PRONOUNS= Arrays.asList("a","an","some","any","another","all");
	
	@Override
	public boolean recognize(SurfaceElement surfaceElement) {
		if (surfaceElement.isPronominal() == false) return false;
		return (hasIndefinitePronoun(surfaceElement));
	}
	
	/**
	 * Indicates if the textual unit involves an indefinite pronoun, such as <i>any</i>.
	 * 
	 * @param si	the textual unit
	 * @return		true if the textual unit is an indefinite pronoun.
	 */
	public static boolean hasIndefinitePronoun(SurfaceElement si) {
		List<Word> ws = si.toWordList("DT");
		if (ws == null || ws.size() == 0) return false;
		for (Word w: ws) {
			if (INDEFINITE_PRONOUNS.contains(w.getLemma().toLowerCase())) return true;
		}
		return false;
	}
}
