package gov.nih.nlm.bioscores.exp;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import edu.stanford.nlp.util.CollectionUtils;
import gov.nih.nlm.bioscores.core.CoreferenceProperties;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.core.Word;

/**
 * Recognition of zero article NP mentions.
 * It recognizes such mentions by the lack of determiners/adjectives
 * that indicate other types of coreferential mentions.
 * It also limits them to hypernymic expressions only.
 * 
 * @author Halil Kilicoglu
 *
 */
public class ZeroArticleNPOps implements ExpressionOps {

	// Concatenation of determiners/adjectives used by other Ops classes
	public static final List<String> ALL_DETERMINERS = Arrays.asList("the","these","those","this","that",
			"either","neither","both","each","no","a","an","some","many","any","another","all");
	public static final List<String> ALL_ADJECTIVES = Arrays.asList("such","other");
	
	@Override
	public boolean recognize(SurfaceElement surfaceElement) {
		if (getZeroArticleNPSpan(surfaceElement) != null) {
			List<String> allHyper = CollectionUtils.flatten(CoreferenceProperties.HYPERNYMS.values());
			for (String hyp: allHyper) {
				if (surfaceElement.containsLemma(hyp)) return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets the span of a zero article NP that subsumes a given textual unit, if any.   
	 * 
	 * @param surf	the textual unit
	 * @return the span of the zero article NP that subsumes <var>surf</var>, 
	 * 			null if it is not subsumed by a zero article NP. 
	 */
	public static SpanList getZeroArticleNPSpan(SurfaceElement surf) {
		if (surf == null || surf.isNominal() == false) return null;
		Sentence sent = surf.getSentence();
		if (sent == null) return null;
		List<SynDependency> embeddings = sent.getEmbeddings();
		if (embeddings == null || embeddings.size() == 0) return null;
		List<SynDependency> outDeps = SynDependency.outDependencies(surf,embeddings);
		if (outDeps.size() ==  0) {
			if (hasDeterminer(surf) || hasAdjective(surf)) return null;
			return surf.getSpan();
		} 
		Collection<SynDependency> dets = SynDependency.dependenciesWithTypes(outDeps, Arrays.asList("det","amod"), true);
		if (dets.size() == 0) {
			if (hasDeterminer(surf) || hasAdjective(surf)) return null;
			return surf.getSpan();
		}
		else {
			SpanList sp = surf.getSpan();
			for (SynDependency d: dets) {
				SurfaceElement dep = d.getDependent();
				if (hasDeterminer(dep) || hasAdjective(dep))  {	
					return null;
				}
			}
			return sp;
		}
	}
	
	private static boolean hasAdjective(SurfaceElement si) {
		List<Word> ws = si.toWordList("JJ");
		if (ws == null || ws.size() == 0) return false;
		for (Word w: ws) {
			if (ALL_ADJECTIVES.contains(w.getLemma().toLowerCase())) return true;
		}
		return false;
	}
	
	private static boolean hasDeterminer(SurfaceElement si) {
		List<Word> ws = si.toWordList("DT");
		if (ws == null || ws.size() == 0) return false;
		for (Word w: ws) {
			if (ALL_DETERMINERS.contains(w.getLemma().toLowerCase())) return true;
		}
		return false;
	}

}
