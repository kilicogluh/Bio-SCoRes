package gov.nih.nlm.ling.process;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;

import java.text.BreakIterator;
import java.util.List;
import java.util.Locale;

/**
 * A simple Java-based sentence segmenter.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SimpleSentenceSegmenter implements SentenceSegmenter {

	@Override
	public void segment(String text, List<Sentence> sentences) {
		BreakIterator boundary = BreakIterator.getSentenceInstance(Locale.US);
		boundary.setText(text);
		int start = boundary.first();
		int i = 0;
		for (int end = boundary.next(); end != BreakIterator.DONE; 
				start = end, end = boundary.next()) {
			String aSent = text.substring(start,end);
			i++;
			Sentence sent = new Sentence("S" + i, aSent, new Span(start, end));
			sentences.add(sent);
		}
	}


}
