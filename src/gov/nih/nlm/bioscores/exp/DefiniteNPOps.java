package gov.nih.nlm.bioscores.exp;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;

/**
 * Recognition of definite NP mentions. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class DefiniteNPOps implements ExpressionOps {

	public static final List<String> DEFINITE_DETERMINERS = Arrays.asList("the");
	
	@Override
	public boolean recognize(SurfaceElement surfaceElement) {
		return (getDefiniteNPSpan(surfaceElement) != null);
	}
	
	/**
	 * Gets the span of the definite NP that subsumes a given textual unit, if any.   
	 * 
	 * @param surf	the textual unit
	 * @return the span of the definite NP that subsumes <var>surf</var>, 
	 * 			null if it is not subsumed by a definite NP. 
	 */
	public static SpanList getDefiniteNPSpan(SurfaceElement surf) {
		if (surf == null || surf.isNominal() == false) return null;
		Sentence sent = surf.getSentence();
		if (sent == null) return null;
		List<SynDependency> embeddings = sent.getEmbeddings();
		if (embeddings == null || embeddings.size() == 0) return null;
		List<SynDependency> outDeps = SynDependency.outDependencies(surf,embeddings);
		// Is this always true? If we apply all embedding transformations, probably not.
		if (outDeps.size() ==  0) {
			if (surf.toWordList().size() < 2) return null;
			if (startsWithDefiniteDeterminer(surf)) return surf.getSpan();
			return null;
		} 
		Collection<SynDependency> dets = 
				SynDependency.dependenciesWithTypes(outDeps, Arrays.asList("det","amod"), true);
		if (dets.size() == 0) {
			// no determiner dependencies but with determiner POS
			// in other words, already chunked as a definite NP
			if (startsWithDefiniteDeterminer(surf)) return surf.getSpan();
		}
		else {
			SpanList sp = surf.getSpan();
			for (SynDependency d: dets) {
				SurfaceElement dep = d.getDependent();
				if (startsWithDefiniteDeterminer(dep)) {
					sp = SpanList.union(sp,dep.getSpan());
				}
			}
			return (sp.equals(surf.getSpan()) ? null: new SpanList(sp.asSingleSpan()));
		}
		return null;
	}
	
	private static boolean startsWithDefiniteDeterminer(SurfaceElement si) {
		return (DEFINITE_DETERMINERS.contains(si.toWordList().get(0).getLemma().toLowerCase()));
	}
	
}
