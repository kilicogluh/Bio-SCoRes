package gov.nih.nlm.bioscores.candidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * Implementations of various post-scoring candidate filtering methods.
 * 
 * @author Halil Kilicoglu
 *
 */
public class PostScoringCandidateFilterImpl {
	
	/**
	 * Keeps the candidate referents with the highest score.
	 *
	 */
	public class TopScoreFilter implements PostScoringCandidateFilter {
		@Override
		public Map<SurfaceElement,Integer> postFilter(SurfaceElement exp, Map<SurfaceElement, Integer> scoreMap) {
			Map<SurfaceElement,Integer> topCandidates = new HashMap<>();
			if (scoreMap ==null || scoreMap.size() == 0) return topCandidates;
			List<Integer> scores= new ArrayList<>(scoreMap.values());
			Collections.sort(scores);
			int top = scores.get(scores.size()-1);
			for (SurfaceElement surf: scoreMap.keySet()) {
				if (scoreMap.get(surf) == top) topCandidates.put(surf,top);
			}
	        return topCandidates;
		}
	}
	
	/**
	 * Keeps the candidate referents with scores over a given threshold.
	 *
	 */
	public class ThresholdFilter implements PostScoringCandidateFilter {
		private int threshold=0;
		/**
		 * Constructs a {@code ThresholdFilter} object with a threshold value.
		 * 
		 * @param threshold	the integer threshold value.
		 */
		public ThresholdFilter(int threshold) {
			this.threshold = threshold;
		}

		@Override
		public Map<SurfaceElement,Integer> postFilter(SurfaceElement exp, Map<SurfaceElement, Integer> scoreMap) {
			Map<SurfaceElement,Integer> topCandidates = new HashMap<>();
			if (scoreMap == null || scoreMap.size() == 0) return topCandidates;
			for (SurfaceElement cand: scoreMap.keySet()) {
				int score = scoreMap.get(cand);
				if (score >= threshold)
					topCandidates.put(cand,score);
			}
	        return topCandidates;
		}
	}

	/**
	 * Keeps the candidate referents most salient according to a given 
	 * salience type, defined in {@link CandidateSalience.Type}. 
	 *
	 */
	public class SalienceTypeFilter implements PostScoringCandidateFilter {
		CandidateSalience.Type type;
		/**
		 * Constructs a {@code SalienceTypeFilter} with a specific salience type.
		 * @param type	the salience type
		 */
		public SalienceTypeFilter(CandidateSalience.Type type) {
			this.type = type;
		}

		@Override
		public Map<SurfaceElement,Integer> postFilter(SurfaceElement exp, Map<SurfaceElement, Integer> scoreMap) {
			Map<SurfaceElement,Integer> topCandidates = new HashMap<>();
			if (scoreMap == null || scoreMap.size() == 0) return topCandidates;
			if (scoreMap.size() == 1) return scoreMap;
			List<SurfaceElement> best = new ArrayList<>();
	        type.salience(exp,new ArrayList<>(scoreMap.keySet()),best);
			for (SurfaceElement cand: best) {
				topCandidates.put(cand, scoreMap.get(cand));
			}
	        return topCandidates;
		}
	}
}
