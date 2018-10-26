package gov.nih.nlm.bioscores.exp;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;

/**
 * Recognition of indefinite NP mentions.
 * 
 * @author Halil Kilicoglu
 *
 */
public class IndefiniteNPOps implements ExpressionOps {

	public static final List<String> INDEFINITE_ADJECTIVES = Arrays.asList("other");
	
	@Override
	public boolean recognize(SurfaceElement surfaceElement) {
		return (getIndefiniteNPSpan(surfaceElement) != null);
	}
	
	/**
	 * Gets the span of the indefinite NP that subsumes a given textual unit, if any.   
	 * 
	 * @param surf	the textual unit
	 * @return the span of the indefinite NP that subsumes <var>surf</var>, 
	 * 			null if it is not subsumed by an indefinite NP. 
	 */
	public static SpanList getIndefiniteNPSpan(SurfaceElement surf) {
		if (surf == null || surf.isNominal() == false) return null;
		Sentence sent = surf.getSentence();
		if (sent == null) return null;
		List<SynDependency> embeddings = sent.getEmbeddings();
		if (embeddings == null || embeddings.size() == 0) return null;
		List<SynDependency> outDeps = SynDependency.outDependencies(surf,embeddings);
		if (outDeps.size() ==  0) {
			if (surf.toWordList().size() < 2) return null;
			if (startsWithIndefiniteDeterminer(surf) || startsWithIndefiniteAdjective(surf)) return surf.getSpan();
			return null;
		} 
		Collection<SynDependency> dets = SynDependency.dependenciesWithTypes(outDeps, Arrays.asList("det","amod"), true);
		if (dets.size() == 0) {
			if (startsWithIndefiniteDeterminer(surf) || startsWithIndefiniteAdjective(surf)) return surf.getSpan();
		}
		else {
			SpanList sp = surf.getSpan();
			for (SynDependency d: dets) {
				SurfaceElement dep = d.getDependent();
				if (startsWithIndefiniteDeterminer(dep) || startsWithIndefiniteAdjective(dep))  {	
					sp = SpanList.union(sp,dep.getSpan());
				}
			}
			return (sp.equals(surf.getSpan()) ? null: new SpanList(sp.asSingleSpan()));
		}
		return null;
	}
	
	private static boolean startsWithIndefiniteAdjective(SurfaceElement si) {
		return (INDEFINITE_ADJECTIVES.contains(si.toWordList().get(0).getLemma().toLowerCase()));
	}
	
	private static boolean startsWithIndefiniteDeterminer(SurfaceElement si) {
		return (IndefinitePronounOps.INDEFINITE_PRONOUNS.contains(si.toWordList().get(0).getLemma().toLowerCase()));
	}
}
