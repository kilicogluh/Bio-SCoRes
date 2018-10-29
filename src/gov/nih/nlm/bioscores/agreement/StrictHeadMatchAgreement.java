package gov.nih.nlm.bioscores.agreement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.bioscores.exp.DemonstrativeNPOps;
import gov.nih.nlm.bioscores.exp.IndefiniteNPOps;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;

/**
 * Approximation of one of the precise constructs in Stanford DCoref sieve #5.
 * It is applicable to nominal coreference and is currently unused. <p>
 * 
 * Agreement is predicted if the mention heads match and modifying nouns and adjectives
 * of the coreferential mention are included in the candidate referent.
 * 
 * @author Halil Kilicoglu
 *
 */
public class StrictHeadMatchAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		if (exp.isNominal() == false || referent.isNominal() == false) return false;
		Word expHead = exp.getHead();
		Word refHead = referent.getHead();
		// sieve 5.1: entity head match
		if (expHead.getText().equalsIgnoreCase(refHead.getText()) == false) return false;
		// sieve 5.2: word inclusion
		// require stop word list
		// sieve 5.3: limit by adjectives and nouns
		// sieve 5.4: Not i-within-i : we cover this with candidate selection
		// we are combining 5.2 and 5.3, by checking for noun and adjectives
		List<String> refTokens = getNonStopWordList(referent);
		List<String> expTokens = getNonStopWordList(exp);
		expTokens.removeAll(refTokens);
		return (expTokens.size() == 0);
		// sieve 6, removes the compatible modifiers feature (only check for noun and adjectives)
		// sieve 7, removes word inclusion in general.
	}
	
	// TODO This may not be sufficient wrt stopwords.
	private List<String> getNonStopWordList(SurfaceElement surf) {
		List<String> otherStopWords = Arrays.asList("many");
		List<String> tokens = new ArrayList<>();
		for (Word w: surf.toWordList()) {
			String ws = w.getLemma().toLowerCase();
			if (otherStopWords.contains(ws)) continue;
			if (w.isNominal() || w.isAdjectival())  {
				if (DemonstrativeNPOps.DEMONSTRATIVE_ADJECTIVES.contains(ws)) continue;
				if (IndefiniteNPOps.INDEFINITE_ADJECTIVES.contains(ws)) continue;
				tokens.add(w.getText().toLowerCase());
			}
		}
		return tokens;
	}

}
