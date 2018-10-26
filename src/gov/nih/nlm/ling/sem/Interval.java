package gov.nih.nlm.ling.sem;


/**
 * Represents an interval with two real-valued endpoints.<p>
 * This class is intended to be used for scalar modality value operations.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Interval implements Comparable<Interval> {	
	
	private double begin;
	private double end;

	/**
	 * Constructs an interval with two endpoints.
	 * 
	 * @param begin	the first endpoint
	 * @param end	the second endpoint
	 */
	public Interval(double begin, double end) {
		this.begin = begin;
		this.end = end;
	}
	
	/**
	 * Constructs an interval from a string (begin:end). 
	 * 
	 * @param value	the string representation
	 */
	public Interval(String value) {
		int ind = value.indexOf(':');
		if (ind > 0) {
			this.begin = Double.valueOf(value.substring(0,value.indexOf(':')));
			this.end = Double.valueOf(value.substring(value.indexOf(':')+1))+1;
		} else {
			this.begin = Double.valueOf(value);
			this.end = Double.valueOf(value);
		}
	}

	public double getBegin() {
		return begin;
	}

	public void setBegin(double begin) {
		this.begin = begin;
	}

	public double getEnd() {
		return end;
	}

	public void setEnd(double endPos) {
		this.end = endPos;
	}
	
	public String toString() {
		if (begin == end) return String.valueOf(begin);
		return begin + ":" + end;
	}
	
	public double length() {
		return end - begin;
	}
	
	public int compareTo(Interval sp) {
		if (begin < sp.getBegin()) return -1;
		if (begin > sp.getEnd()) return 1;
		return 0;
	}
	
	/**
	 * 
	 * @param s1	the first interval
	 * @param s2	the second interval
	 * @return		true if the intervals overlap
	 */
	public static boolean intervalOverlap(Interval s1, Interval s2) {
		double s1s = s1.getBegin();
		double s1e = s1.getEnd();
		double s2s = s2.getBegin();
		double s2e = s2.getEnd();
		double len1 = s1e - s1s;
		double len2 = s2e - s2s;
		return (s1.equals(s2) || (s2s <= s1s && s1s < s2e) ||
				( s2s < s1e && s1e <= s2e) || 
				(len1 > len2 && s1s <= s2s && s2e <= s1e));
	}
	
	/**
	 * 
	 * @param s1	the first interval
	 * @param s2	the second interval
	 * @return		true if the first interval subsumes the second
	 */
	public static boolean intervalSubsume(Interval s1, Interval s2) {
		double s1s = s1.getBegin();
		double s1e = s1.getEnd();
		double s2s = s2.getBegin();
		double s2e = s2.getEnd();
		double len1 = s1e - s1s;
		double len2 = s2e - s2s;
		return (s1.equals(s2) || 
				(len1 > len2 && s1s <= s2s && s2e <= s1e));
	}
	
	/**
	 * 
	 * @param s1	the first interval
	 * @param s2	the second interval
	 * @return		true if the first interval is to the left of the second
	 */
	public static boolean atLeft(Interval s1, Interval s2) {
		double s1s = s1.getBegin();
		double s1e = s1.getEnd();
		double s2s = s2.getBegin();
		double s2e = s2.getEnd();
		return (s1s <= s1e && s1e < s2s && s2s <= s2e);
	}
	
	/**
	 * 
	 * @param s1	the first interval
	 * @param s2	the second interval
	 * @return		the smallest interval that subsumes both intervals.
	 */
	public static Interval union(Interval s1, Interval s2) {
		if (s1 == null || s2 == null) return null;
		return new Interval(Math.min(s1.getBegin(),s2.getBegin()),Math.max(s1.getEnd(), s2.getEnd()));
	}
	
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		Interval s = (Interval)obj;
		return (this.getBegin() == s.getBegin() &&
				this.getEnd() == s.getEnd());
	}
	
	/**
	 * @return	true if this interval is invalid
	 */
	public boolean invalidInterval() {
		return (end< begin);
	}
	
	/**
	 * 
	 * @return	the midpoint of the interval
	 */
	public double midPoint(){
		return ((end - begin) / 2) + begin;
	}
}
