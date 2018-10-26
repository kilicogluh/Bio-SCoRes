package gov.nih.nlm.bioscores.exp;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.transform.CoordinationTransformation;

/**
 * Recognition of distributive NP mentions.
 * 
 * @author Halil Kilicoglu
 *
 */
public class DistributiveNPOps implements ExpressionOps {
	
	@Override
	public boolean recognize(SurfaceElement surfaceElement) {
		return (getDistributiveNPSpan(surfaceElement) != null);
	}
	
	/**
	 * Gets the span of the distributive NP that subsumes a given textual unit, if any.   
	 * 
	 * @param surf	the textual unit
	 * @return the span of the distributive NP that subsumes <var>surf</var>, 
	 * 			null if it is not subsumed by a distributive NP. 
	 */
	public static SpanList getDistributiveNPSpan(SurfaceElement surf) {
		if (surf == null || surf.isNominal() == false) return null;
		Sentence sent = surf.getSentence();
		if (sent == null) return null;
		List<SynDependency> embeddings = sent.getEmbeddings();
		if (embeddings == null || embeddings.size() == 0) return null;
		List<SynDependency> outDeps = SynDependency.outDependencies(surf,embeddings);
		// TODO This assumes coordination transformation is done. 
		if (sent.getTransformations().contains(CoordinationTransformation.class)) {
			List<SynDependency> conjDeps = SynDependency.inDependenciesWithType(surf, embeddings, "cc", true);
			if (conjDeps.size() > 0) return null;
		}
		int ind = sent.getSurfaceElements().indexOf(surf);
		if (ind > 0) {
			SurfaceElement previous = sent.getSurfaceElements().get(sent.getSurfaceElements().indexOf(surf)-1);
			// dependencies do not work well for these
			if (previous.containsAnyLemma(Arrays.asList("either","neither"))) { 
				return new SpanList(previous.getSpan().getBegin(),surf.getSpan().getEnd());
			}
		}
		if (outDeps.size() ==  0) {
			if (surf.toWordList().size() < 2) return null;
			if (startsWithDistributiveDeterminer(surf)) return surf.getSpan();
			return null;
		} 
		Collection<SynDependency> dets = SynDependency.dependenciesWithTypes(outDeps, Arrays.asList("det","amod"), true);
		if (dets.size() == 0) {
			if (startsWithDistributiveDeterminer(surf)) return surf.getSpan();
		}
		else {
			SpanList sp = surf.getSpan();
			for (SynDependency d: dets) {
				SurfaceElement dep = d.getDependent();
				if (startsWithDistributiveDeterminer(dep)) {
					sp = SpanList.union(sp,dep.getSpan());
				}
			}
			return (sp.equals(surf.getSpan()) ? null: new SpanList(sp.asSingleSpan()));
		}
		return null;
	}
	
	private static boolean startsWithDistributiveDeterminer(SurfaceElement si) {
		return (DistributivePronounOps.DISTRIBUTIVE_PRONOUNS.contains(si.toWordList().get(0).getLemma().toLowerCase()));
	}


}
