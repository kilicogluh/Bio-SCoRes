package gov.nih.nlm.bioscores.exp;

import java.util.Arrays;
import java.util.List;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * Recognition of reciprocal pronoun mentions.
 * 
 * @author Halil Kilicoglu
 *
 */
public class ReciprocalPronounOps implements ExpressionOps {	
	
	public static final List<String> RECIPROCAL_PRONOUNS = Arrays.asList("each other","one another","one to the other");
	
	@Override
	public boolean recognize(SurfaceElement surfaceElement) {
		return (getReciprocalSpan(surfaceElement) != null);
	}

	/**
	 * Gets the span of the reciprocal pronoun that subsumes a given textual unit, if any.   
	 * 
	 * @param surf	the textual unit
	 * @return the span of the reciprocal pronoun that subsumes <var>surf</var>, 
	 * 			null if it is not subsumed by a reciprocal pronoun. 
	 */
	public static SpanList getReciprocalSpan(SurfaceElement surf) {
		if (surf == null) return null;
		for (String pr: RECIPROCAL_PRONOUNS) {
			if (pr.equalsIgnoreCase(surf.getText().toLowerCase())) 
				return surf.getSpan();
		}
		Sentence sent = surf.getSentence();
		int siOffset = surf.getSpan().getBegin();
		if (sent == null) return null;
		for (String pr: RECIPROCAL_PRONOUNS) {
			Span maybeSp = new Span(siOffset,siOffset+pr.length());
			if (Span.subsume(sent.getSpan(), maybeSp) == false) continue;
			String spStr = sent.getStringInSpan(maybeSp);			
			if (spStr != null && spStr.equalsIgnoreCase(pr)) {
				return new SpanList(maybeSp);
			}
		}
		return null;
	}
}