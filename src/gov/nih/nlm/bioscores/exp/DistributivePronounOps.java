package gov.nih.nlm.bioscores.exp;

import java.util.Arrays;
import java.util.List;

import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.core.Word;

/**
 * Recognition of distributive pronoun mentions.
 * 
 * @author Halil Kilicoglu
 *
 */
public class DistributivePronounOps implements ExpressionOps {

	public static final List<String> DISTRIBUTIVE_PRONOUNS = Arrays.asList("either","neither","both","each");

	@Override
	public boolean recognize(SurfaceElement surfaceElement) {
		return (hasDistributiveConjunction(surfaceElement));	
	}
	
	/**
	 * Indicates if the textual unit involves a distributive conjunction.
	 * 
	 * @param si	the textual unit
	 * @return		true if the textual unit involves a distributive conjunction
	 */
	public static boolean hasDistributiveConjunction(SurfaceElement si) {
		if (si == null) return false;
		if (si.toWordList().size() > 1) return false;
		if (SynDependency.inDependenciesWithType(si, si.getSentence().getEmbeddings(), "preconj",true).size() > 0) return false;
		if (SynDependency.outDependenciesWithType(si, si.getSentence().getEmbeddings(), "pobj",true).size() > 0) return false;
		Word w = si.getHead();
		return ((w.isCoordConj() || w.isDeterminer()) && 
				DISTRIBUTIVE_PRONOUNS.contains(w.getLemma().toLowerCase()));
	}

}
