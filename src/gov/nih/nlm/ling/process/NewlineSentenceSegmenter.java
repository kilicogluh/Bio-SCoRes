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
			String lineTrim = line.trim();
			if (lineTrim.length() == 0) continue;
			int start = text.indexOf(lineTrim,j);
//			int end = (line.length() == 0 ? start+1: start+ line.length());
			int end = start + lineTrim.length();
			Sentence sent = new Sentence("S" + k, lineTrim, new Span(start,end));
			k++;
			j = end;
			sentences.add(sent);
		}
	}

}
