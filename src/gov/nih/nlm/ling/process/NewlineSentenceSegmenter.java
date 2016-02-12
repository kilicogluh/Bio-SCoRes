package gov.nih.nlm.ling.process;

import java.util.List;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;

/**
 * A simple sentence segmenter that segments at newline characters.
 * 
 * @author Halil Kilicoglu
 *
 */
public class NewlineSentenceSegmenter implements SentenceSegmenter {

	@Override
	public void segment(String text, List<Sentence> sentences) {
		String[] lines = text.split("\n+");
		int j=0;
		int k=0;
		for (int i=0; i < lines.length; i++) {
			String line = lines[i];
			int start = text.indexOf(line,j);
			int end = (line.length() == 0 ? start+1: start+ line.length());
			Sentence sent = new Sentence("S" + k, line, new Span(start,end));
			k++;
			j = end;
			sentences.add(sent);
		}
	}

}
