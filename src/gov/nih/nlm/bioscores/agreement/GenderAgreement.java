package gov.nih.nlm.bioscores.agreement;

import java.util.List;
import java.util.Map;

import gov.nih.nlm.bioscores.core.CoreferenceProperties;
import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.CoreferenceUtils;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;

/**
 * Implementation of gender agreement. Gender values are {@link Gender#Female} 
 * and {@link Gender#Male}. <p>
 * 
 * Agreement is predicted if the genders of the
 * candidate and the mention match or they are both unknown.
 * 
 * @author Halil Kilicoglu
 *
 */
public class GenderAgreement implements Agreement {

	public static enum Gender {
		Male, Female;
		public String toString() {
			switch(this) {
				case Male: return "Mal";
				case Female: return "Fem";
			}
			return null;
		}
	}
	
	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		Gender expGen = gender(exp);
		Gender refGen = gender(referent);
		if (genderEquals(expGen,refGen)) return true;
		if ((canBeEitherGender(referent) && (expGen == Gender.Male || expGen == Gender.Female))) return true;
		if ((canBeEitherGender(exp) && (refGen == Gender.Male || refGen == Gender.Female))) return true;
		return false;
	}
		
	/**
	 * Performs strict gender equality test
	 * 
	 * @return true if the arguments match or are both null.
	 */
	private static boolean genderEquals(Gender x, Gender y) {
		if (x==null && y==null) return true;
		return x==y;
	}
	
	/**
	 * Gets the gender value for an expression. Uses a small list of 
	 * female and male nouns if the expression is not pronominal.
	 * 
	 * @param si  the expression to get the gender for
	 * @return  {@code GenderAgreement.Gender#Male}, {@code GenderAgreement.Gender#Female} gender or null if it cannot be determined
	 */
	private static Gender gender(SurfaceElement si) {
		if (si.isPronominal()) {
			String p = CoreferenceUtils.getPronounToken(si);
			if (p.matches("^(he|him|his|himself)$")) {
				return Gender.Male;
			} else if (p.matches("^(she|her|hers|herself)$")) {
				return Gender.Female;
			} else if (p.matches("^(it|its|itself)$")) {
				return null;
			} else {
				return null;   
			}
		}
		// preliminary female, male noun lists
		Word headWord = si.getHead();
		List<String> femNouns = CoreferenceProperties.FEMALE_NOUNS;
		List<String> maleNouns = CoreferenceProperties.MALE_NOUNS;
		if (femNouns!= null && femNouns.contains(headWord.getLemma())) return Gender.Female;
		if (maleNouns!= null && maleNouns.contains(headWord.getLemma())) return Gender.Male;
		/*		Gender firstNameGender = genderByFirstNamesOrTitles(m);
		return firstNameGender;*/
		return null;

	}
	
	private static boolean canBeEitherGender(SurfaceElement surf) {
		String headWord = surf.getHead().getLemma();
		Map<String,List<String>> hypernyms = CoreferenceProperties.HYPERNYMS;
		if (hypernyms == null) return false;
		List<String> poplHypernyms = hypernyms.get("POPL");
		if (poplHypernyms == null) return false;
		return (poplHypernyms.contains(headWord));
	}
	
	/*	private static Gender genderByFirstNamesOrTitles(Mention m) {
	if (m.node()==null) return null;  // TODO we can still figure something out, right
	
	//Go through all the NNP tokens in the noun phrase and see if any of them
	//are person names.  If so, return the gender of that name.
	//Note: this will fail for ambiguous month/person names like "April"
	
	Tree head = m.node().headPreTerminal(AnalysisUtilities.getInstance().getHeadFinder());
	Tree root = m.getSentence().rootNode();
	
	for(Tree leaf : m.node().getLeaves()){
		//System.err.println(head+"\t"+leaf+"\t"+head.parent(root)+"\t"+leaf.parent(root));
		if(!leaf.parent(m.node()).label().value().equals("NNP") 
				|| leaf.parent(root).parent(root) != head.parent(root)) //must be a sibling of the head node, as in "(NP (NNP John) (POS 's))"
		{
			continue;
		}
		String genderS = FirstNames.getInstance().getGenderString(leaf.value());
		if(genderS.equals("Mal") || leaf.value().equals("Mr.")){
			return Gender.Male;
		}else if(genderS.equals("Fem") || leaf.value().equals("Mrs.") || leaf.value().equals("Ms.")){
			return Gender.Female;
		}
	}
	return null;
}*/

}
