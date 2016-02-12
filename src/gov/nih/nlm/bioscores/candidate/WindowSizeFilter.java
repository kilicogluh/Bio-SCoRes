package gov.nih.nlm.bioscores.candidate;

import java.util.List;

import gov.nih.nlm.bioscores.core.Configuration;
import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.Section;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.util.DiscourseUtils;

/**
 * This filter eliminates referents that do not appear within a pre-defined
 * number of sentences from the coreferential mention.
 * 
 * @author Halil Kilicoglu
 *
 */
public class WindowSizeFilter implements CandidateFilter {
	private int windowSize;
	
	/**
	 * Constructs a {@code WindowSizeFilter} with a window size.
	 * 
	 * @param windowSize	the sentence window size
	 */
	public WindowSizeFilter(int windowSize) {
		this.windowSize = windowSize;
	}

	@Override
	public void filter(SurfaceElement exp, CoreferenceType type, ExpressionType expType,
			List<SurfaceElement> candidates, List<SurfaceElement> filteredCandidates) {
		if (candidates == null) return;
		Sentence suSent = exp.getSentence();
		for (SurfaceElement cand: candidates) {
			if (cand.equals(exp) || SpanList.overlap(cand.getSpan(), exp.getSpan())) continue;
			Sentence candSent = cand.getSentence();
			if (windowSize == Configuration.RESOLUTION_WINDOW_ALL || 
				(windowSize == Configuration.RESOLUTION_WINDOW_SECTION && Section.sameSection(suSent, candSent)) ||
				(windowSize == Configuration.RESOLUTION_WINDOW_SENTENCE && candSent.equals(suSent)) ||
			    DiscourseUtils.withinNSentence(suSent, candSent, windowSize)) 
				filteredCandidates.add(cand);
		}
	}
}
