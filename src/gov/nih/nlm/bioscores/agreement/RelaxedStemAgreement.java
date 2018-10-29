package gov.nih.nlm.bioscores.agreement;

import java.util.HashSet;
import java.util.Set;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.bioscores.process.PorterStemmer;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.util.StringUtils;

/**
 * An agreement constraint based on mention stems. This is useful for nominal
 * coreference. It is inspired by one of the sieves reported in Rink and Harabagiu (2012). <p>
 * 
 * Agreement is predicted between two nominal mentions if their stem overlap, 
 * based on Porter stemmer, is greater than 50%.
 * 
 * @author kilicogluh
 *
 */
public class RelaxedStemAgreement implements Agreement {

	private static PorterStemmer stemmer = new PorterStemmer();
	
	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		if (ExpressionType.PRONOMINAL_TYPES.contains(expType)) return false;
		Set<String> expStems = getStems(exp);
		Set<String> refStems = getStems(referent);
		if (incompatibleNumbers(expStems,refStems)) return false;
		Set<String> allStems = new HashSet<>(expStems); allStems.addAll(refStems);
		int all = allStems.size();
		int common = 0;
		for (String st: expStems) {
			if (refStems.contains(st)) common++;
		}
		double pct = (double) common/all;
		return (pct >= 0.5);
	}
	
	/**
	 * Gets stems of a textual unit, using Porter stemmer.
	 * 
	 * @param surf	the textual unit
	 * @return	the set of textual unit stems
	 */
	public static Set<String> getStems(SurfaceElement surf) {
		Set<String> stems = new HashSet<>();
		for (Word w: surf.toWordList()) {
			if (toStem(w)) {
				for (int i=0; i <w.getText().length(); i++ ) {
					char c = w.getText().charAt(i);
					stemmer.add(c);
				}
				stemmer.stem();
				stems.add(stemmer.toString());
			}
		}
		return stems;
	}
	
	private static boolean toStem(Word w) {
		if (w.isDeterminer() || w.isPronominal() || w.isRelativePronoun() || w.isPrepositional()) return false;
		return true;
	}
	
	private static boolean incompatibleNumbers(Set<String> stem1, Set<String> stem2) {
		for (String s: stem1) {
			if (StringUtils.containsDigit(s) && stem2.contains(s) == false) return true;
		}
		for (String s: stem2) {
			if (StringUtils.containsDigit(s) && stem1.contains(s) == false) return true;
		}
		return false;
	}

}
