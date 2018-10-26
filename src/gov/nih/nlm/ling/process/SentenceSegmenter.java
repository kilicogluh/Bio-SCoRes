package gov.nih.nlm.ling.process;

import gov.nih.nlm.ling.core.Sentence;

import java.util.List;

/**
 * An interface for sentence segmentation modules.
 * 
 * @author Halil Kilicoglu
 *
 */
public interface SentenceSegmenter {
	
	/**
	 * Segments the given text into a list of <code>Sentence</code> objects.
	 * 
	 * @param text		the document text
	 * @param sentences	the list of sentences, in the order they appear in text
	 */
	public void segment(String text, List<Sentence> sentences);
}

