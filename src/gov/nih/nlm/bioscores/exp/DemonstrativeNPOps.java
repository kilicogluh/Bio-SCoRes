package gov.nih.nlm.bioscores.exp;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.core.Word;

public class DemonstrativeNPOps implements ExpressionOps {
	public static final List<String> DEMONSTRATIVE_ADJECTIVES = Arrays.asList("such");
	
	@Override
	public boolean recognize(SurfaceElement surfaceElement) {
		return (getDemonstrativeNPSpan(surfaceElement) != null);
	}
	
	/**
	 * Gets the span of the demonstrative NP that subsumes a given textual unit, if any.   
	 * 
	 * @param surf	the textual unit
	 * @return the span of the demonstrative NP that subsumes <var>surf</var>, 
	 * 			null if it is not subsumed by a demonstrative NP. 
	 */
	public static SpanList getDemonstrativeNPSpan(SurfaceElement surf) {
		if (surf == null || surf.isNominal() == false) return null;
		Sentence sent = surf.getSentence();
		if (sent == null) return null;
		List<SynDependency> embeddings = sent.getEmbeddings();
		if (embeddings == null || embeddings.size() == 0) return null;
		List<SynDependency> outDeps = SynDependency.outDependencies(surf,embeddings);
		if (outDeps.size() ==  0) {
			if (surf.toWordList().size() < 2) return null;
			SpanList demSpan = demonstrativeSpan(surf);
			return demSpan;
		} 
		Collection<SynDependency> dets = SynDependency.dependenciesWithTypes(outDeps, Arrays.asList("det","amod"), true);
		if (dets.size() == 0) {
			// no determiner dependencies but with determiner POS
			// in other words, already chunked as a definite NP
			SpanList demSpan = demonstrativeSpan(surf);
			return demSpan;
		}
		else {
			SpanList sp = surf.getSpan();
			for (SynDependency d: dets) {
				SurfaceElement dep = d.getDependent();
				if (startsWithDemonstrativeDeterminer(dep) || startsWithDemonstrativeAdjective(dep))  {	
					sp = SpanList.union(sp,dep.getSpan());
				}
			}
			return (sp.equals(surf.getSpan()) ? null: new SpanList(sp.asSingleSpan()));
		}
	}

	
	private static boolean startsWithDemonstrativeAdjective(SurfaceElement si) {
		return (DEMONSTRATIVE_ADJECTIVES.contains(si.toWordList().get(0).getLemma().toLowerCase()));
	}
	
	private static boolean startsWithDemonstrativeDeterminer(SurfaceElement si) {
		return (DemonstrativePronounOps.DEMONSTRATIVE_PRONOUNS.contains(si.toWordList().get(0).getLemma().toLowerCase()));
	}
	
	private static SpanList demonstrativeSpan(SurfaceElement si) {
		SpanList detSpan = demonstrativeDeterminerSpan(si);
		if (detSpan != null) return detSpan;
		SpanList adjSpan = demonstrativeAdjectiveSpan(si);
		return adjSpan;
	}
	
	private static SpanList demonstrativeDeterminerSpan(SurfaceElement si) {
		int begin = -1;
		int end = -1;
		for (Word w: si.toWordList()) {
			if (DemonstrativePronounOps.DEMONSTRATIVE_PRONOUNS.contains(w.getLemma().toLowerCase()) && 
					w.getPos().startsWith("WD") == false) {
				begin = w.getSpan().getBegin();
			} else if (w.isPrepositional() || w.getPos().startsWith("WD")) {}
			if (begin > 0) end = w.getSpan().getEnd();
		}
		if (end == -1 || begin == -1) return null;
		return new SpanList(begin,end);
	}
	
	private static SpanList demonstrativeAdjectiveSpan(SurfaceElement si) {
		int begin = -1;
		int end = -1;
		for (Word w: si.toWordList()) {
			if (DEMONSTRATIVE_ADJECTIVES.contains(w.getLemma().toLowerCase()) ) {
				begin = w.getSpan().getBegin();
			} else if (w.isPrepositional() || w.getPos().startsWith("WD")) {}
			if (begin > 0) end = w.getSpan().getEnd();
		}
		if (end == -1 || begin == -1) return null;
		return new SpanList(begin,end);
	}

}
