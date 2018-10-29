package gov.nih.nlm.ling.util;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * A class that contains static utility methods for discourse-level processing. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class DiscourseUtils {

	/**
	 * Determines whether two textual units are in the same sentence.
	 * 
	 * @param surf1	the first textual unit
	 * @param surf2	the second textual unit
	 * @return		true if <var>surf1</var> and <var>surf2</var> are in the same sentence
	 */
	public static boolean sameSentence(SurfaceElement surf1, SurfaceElement surf2) {
		Sentence sent1 = surf1.getSentence();
		Sentence sent2 = surf2.getSentence();
		if (sent1 == null || sent2 == null) return false;
		return (sent1.equals(sent2));
	}
	
	/**
	 * Determines whether two sentences are within a predefined number of sentences
	 * from each other.
	 * 
	 * @param a  the first sentence
	 * @param b  the second sentence
	 * @param n  the max. number of acceptable sentences
	 * @return  true if distance between the sentences is less than or equal to n (in terms of sentences).
	 */
	public static boolean withinNSentence(Sentence a, Sentence b, int n) {
		if (n < 0 || a == null || b == null) return false;
		Document doc = a.getDocument();
		if (!(b.getDocument().equals(doc))) return false;
		if (n>=0 && a.equals(b)) return true;
		int aind = doc.getSentences().indexOf(a);
		int bind = doc.getSentences().indexOf(b);
		return (Math.abs(aind-bind) <= n);
	}

}
