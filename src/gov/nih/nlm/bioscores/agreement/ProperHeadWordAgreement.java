package gov.nih.nlm.bioscores.agreement;

import java.util.ArrayList;
import java.util.List;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.bioscores.exp.DemonstrativeNPOps;
import gov.nih.nlm.bioscores.exp.IndefiniteNPOps;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.util.StringUtils;

/**
 * Approximation of one of the precise constructs in used in Stanford DCoref sieve #8.
 * This agreement constraint can be useful for nominal coreference. It is currently
 * unused.<p>
 * 
 * Agreement is predicted if nominal mentions in question match up to their head or if
 * they are not proper nouns.
 * 
 * @author Halil Kilicoglu
 *
 */
public class ProperHeadWordAgreement implements Agreement {

	/**
	 * @return true if the nominal argument match up to their head.
	 */
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		if (exp.isNominal() == false || referent.isNominal() == false) return false;
		Word expHead = exp.getHead();
		Word refHead = referent.getHead();
		boolean expNNP = expHead.getPos().startsWith("NNP");
		boolean refNNP = refHead.getPos().startsWith("NNP");
		if (!expNNP || !refNNP) return true;
		
		List<String> refTokens = getModifiers(referent);
		List<String> expTokens = getModifiers(exp);
		
		return properNounModifierMatch(expTokens,refTokens);
	}
	
	private List<String> getModifiers(SurfaceElement surf) {
		List<String> tokens = new ArrayList<>();
		Word head = surf.getHead();
		int headInd = surf.toWordList().indexOf(head);
		for (int i=headInd-1; i >= 0; i--) {
			Word mod = surf.toWordList().get(i);
			String ws = mod.getText().toLowerCase();
			if (mod.isNominal() || mod.isAdjectival())  {
				if (DemonstrativeNPOps.DEMONSTRATIVE_ADJECTIVES.contains(ws)) continue;
				if (IndefiniteNPOps.INDEFINITE_ADJECTIVES.contains(ws)) continue;
				tokens.add(mod.getText().toLowerCase());
			}
		}
		return tokens;
	}
	
	// TODO This is probably not very accurate.
	private boolean properNounModifierMatch(List<String> expMods, List<String> refMods) {
		if (expMods.size() != refMods.size()) return false;
		List<String> numbers = new ArrayList<>();
		for (int i=0; i < expMods.size(); i++) {
			String s = expMods.get(i);
			if (StringUtils.isAllDigits(s) ) numbers.add(s);
		}
		for (int i=0; i < refMods.size(); i++) {
			String s = refMods.get(i);
			if (StringUtils.isAllDigits(s)) {
				if (numbers.contains(s) == false) return false;
			}
		}
		return true;
	}
	  
}
