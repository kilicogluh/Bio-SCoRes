package gov.nih.nlm.ling.core;

/**
 * Represents the character offsets of a linguistic object. 
 * It is represented with a begin and end character offset. 
 * This class also provides methods to compare the positions of two <code>Span</code> objects.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Span implements Comparable<Span> {	
	public static final char SEPARATOR = '-';
	
	private int begin;
	private int end;

	/**
	 * Creates a Span object from begin and end character offsets.
	 * 
	 * @param  begin first character offset of the object
	 * @param  end  offset of the character following the last character of the object
	 */
	public Span(int begin, int end) {
//		if (begin >= end) 
//			throw new IllegalArgumentException("Begin offset " +begin + " should be less than the end offset " + end + ".");
		this.begin = begin;
		this.end = end;
	}
	
	/**
	 * Creates a Span object from a string representation. Assumes hyphen as the delimiter (X-Y).
	 * 
	 * @param  charOffset a String representation of the Span 
	 */
	public Span(String charOffset) {
		this(charOffset,SEPARATOR);
	}
	
	/**
	 * Creates a Span object from a string representation.
	 *  
	 * @param  charOffset a String representation of the Span
	 * @param  separator the character delimiter between begin and end positions
	 */
	public Span(String charOffset, char separator) {
		int b = Integer.valueOf(charOffset.substring(0,charOffset.indexOf(separator)));
		int e = Integer.valueOf(charOffset.substring(charOffset.indexOf(separator)+1));
		if (b >= e) 
			throw new IllegalArgumentException("Begin offset " +b + " should be less than the end offset " + e + ".");
		this.begin = b;
		this.end = e;
	}

	public int getBegin() {
		return begin;
	}
	public void setBegin(int begin) {
		this.begin = begin;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(begin);
		buf.append(SEPARATOR);
		buf.append(end);
		return buf.toString();
	}
	
	/**
	 * Returns a String representation of the Span. Uses space as delimiter.
	 * 
	 * @return  the standoff annotation string
	 */
	public String toStandoffAnnotation() {
		StringBuffer buf = new StringBuffer();
		buf.append(begin);
		buf.append(" ");
		buf.append(end);
		return buf.toString();
	}
	
	public int length() {
		return end - begin;
	}
	
	/**
	 * Determines whether two Span objects overlap. 
	 * 
	 * @param  s1 the first Span object
	 * @param  s2 the second Span object
	 * @return  true if the Span objects have overlap
	 */
	public static boolean overlap(Span s1, Span s2) {
		int s1s = s1.getBegin(); int s1e = s1.getEnd();
		int s2s = s2.getBegin(); int s2e = s2.getEnd();
		int len1 = s1e - s1s; int len2 = s2e - s2s;
		return (( (s2s <= s1s && s1s < s2e) ||
				( s2s < s1e && s1e <= s2e)) || 
				(len1 > len2 && s1s <= s2s && s2e <= s1e));
	}
	
	/**
	 * Determines whether the first Span object subsumes the second completely.
	 * 
	 * @param s1  the first Span object
	 * @param s2  the second Span object
	 * @return  true if s1 subsumes s2, or s1 and s2 cover the same span.
	 */
	public static boolean subsume(Span s1, Span s2) {
		int s1s = s1.getBegin(); int s1e = s1.getEnd();
		int s2s = s2.getBegin(); int s2e = s2.getEnd();
		int len1 = s1e - s1s; int len2 = s2e - s2s;
		return (s1.equals(s2) || (len1 > len2 && s1s <= s2s && s2e <= s1e));
	}
	
	/**
	 * Determines whether the first Span object is to the left of the second in its entirety.
	 * 
	 * @param s1  the first Span object
	 * @param s2  the second Span object
	 * @return  true if s1 is to the left of s2.
	 */
	public static boolean atLeft(Span s1, Span s2) {
		int s1s = s1.getBegin(); int s1e = s1.getEnd();
		int s2s = s2.getBegin(); int s2e = s2.getEnd();
		return (s1s < s1e && s1e <= s2s && s2s < s2e);
	}
	
	/**
	 * Gets the union of two Span objects.
	 * 
	 * @param s1  the first Span object
	 * @param s2  the second Span object
	 * @return  the Span object that subsumes both s1 and s2
	 */
	public static Span union(Span s1, Span s2) {
		if (s1 == null || s2 == null) return null;
		return new Span(Math.min(s1.getBegin(),s2.getBegin()),Math.max(s1.getEnd(), s2.getEnd()));
	}
	
	/**
	 * Returns the intersection of two Span objects.
	 * 
	 * @param s1  the first Span object
	 * @param s2  the second Span object
	 * @return  the Span object that is the intersection of s1 and s2
	 */
	public static Span intersection(Span s1, Span s2) {
		if (s1 == null || s2 == null) return null;
		if (Span.overlap(s1, s2) == false) return null;
		return new Span(Math.max(s1.getBegin(),s2.getBegin()),Math.min(s1.getEnd(), s2.getEnd()));
	}
	
	/**
	 * 
	 * @param sp a Span object 
	 * @return  true if the Span object is invalid (begin position is to the right of the end position)
	 */
	public static boolean invalidSpan(Span sp) {
		return (sp.getEnd()<= sp.getBegin());
	}
	
	public int compareTo(Span sp) {
		int sps = sp.getBegin(); int spe = sp.getEnd();
		if (sps == begin) {
			if (spe == end) return 0;
			if (spe > end) return -1;
			return 1;
		}
		if (begin < sps) return -1;
		return 1;
	}
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		if (this == obj) return true;
		Span s = (Span)obj;
		return (this.getBegin() == s.getBegin() &&
				this.getEnd() == s.getEnd());
	}
	
	public int hashCode() {
	    return (begin  * 53 ^ end * 119);
	}
	
}
