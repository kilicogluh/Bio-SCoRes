package gov.nih.nlm.ling.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents the character offsets of a linguistic object. 
 * It consists of a List of <code>Span</code> objects, which are assumed to be in ascending order.
 * This class allows handling multi-part linguistic objects. 
 * This class also provides methods to compare the positions of two <code>SpanList</code> objects.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SpanList implements Comparable<SpanList> {
	
	public static final char SEPARATOR = ';';
	private List<Span> spans;

	/**
	 * Creates a SpanList object from a List of Span objects assumed to be in ascending order.
	 * 
	 * @param sp  a List of Span objects
	 */
	public SpanList(List<Span> sp) {
		this.spans = sp;
	}
	
	/**
	 * Creates a SpanList object from a single Span.
	 * 
	 * @param sp  a Span object
	 */
	public SpanList(Span sp) {
		spans = new ArrayList<Span>();
		spans.add(sp);
	}
	
	/**
	 * Creates a SpanList object from character offsets of a single Span.
	 * 
	 * @param begin  the begin offset
	 * @param end  the end offset
	 */
	public SpanList(int begin, int end) {
		spans = new ArrayList<Span>();
		spans.add(new Span(begin, end));
	}
	
	/**
	 * Creates a SpanList object from a String representation. (X-Y;W-Z) format is assumed.
	 * 
	 * @param charOffset  a String representation of a multi-part span.
	 */
	public SpanList(String charOffset) {
		this(charOffset,SEPARATOR,Span.SEPARATOR);
	}
	
	/**
	 * Creates a SpanList object from a String representation. 
	 * 
	 * @param charOffset  a String representation of a multi-part span.
	 * @param separator  delimiter for spans
	 * @param spanSeparator  delimiter for begin/end offsets for each span  
	 */
	public SpanList(String charOffset, char separator, char spanSeparator) {
		List<String> spanStr= Arrays.asList(charOffset.split("[" + separator + "]+"));
		this.spans = new ArrayList<Span>();
		for (String s :spanStr) {
			spans.add(new Span(s,spanSeparator));
		}
		Collections.sort(spans);
	}
	
	/**
	 * 
	 * @return  the first Span object
	 */
	public Span first() {
		return spans.get(0);
	}

	/**
	 * 
	 * @return  the first Span object
	 */
	public Span last() {
		return spans.get(spans.size()-1);
	}
	
	/**
	 * 
	 * @return  number of parts 
	 */
	public int size() {
		return spans.size();
	}
	
	/**
	 * 
	 * @return  the first character offset
	 */
	public int getBegin() {
		return first().getBegin();
	}
	
	/**
	 * 
	 * @return  the last character offset
	 */
	public int getEnd() {
		return last().getEnd();
	}
	
	/**
	 * 
	 * @return   a Span object that covers the full extent of the SpanList object
	 */
	public Span asSingleSpan() {
		return new Span(getBegin(), getEnd());
	}
	
	/**
	 * 
	 * @return  the number of characters
	 */
	public int length() {
		int len = 0;
		for (Span sp: spans) {
			len += sp.length();
		}
		return len;
	}
	
	public List<Span> getSpans() {
		return spans;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i < spans.size()-1; i++) {
			Span spi = spans.get(i);
			buf.append(spi.toString());
			buf.append(SEPARATOR);
		}
		buf.append(spans.get(spans.size()-1).toString());
		return buf.toString();
	}
	
	/**
	 * 
	 * @return  a String representation in (X Y;W Z) format.
	 */
	public String toStandoffAnnotation() {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i < spans.size()-1; i++) {
			Span spi = spans.get(i);
			buf.append(spi.toStandoffAnnotation());
			buf.append(SEPARATOR);
		}
		buf.append(spans.get(spans.size()-1).toStandoffAnnotation());
		return buf.toString();
	}
		
	/**
	 * Takes the intersection of two SpanList objects. The spans of the resulting SpanList
	 * are in ascending order.
	 * 
	 * @param s1  the first SpanList object 
	 * @param s2  the second 
	 * @return  the intersection of <var>s1</var> and <var>s2</var> 
	 */
	public static SpanList intersection(SpanList s1, SpanList s2) {
		if (s1 == null || s2 == null) return null;
		if (SpanList.overlap(s1, s2) == false) return null;
		List<Span> s1s = s1.getSpans();
		List<Span> s2s = s2.getSpans();
		List<SpanList> intersects = new ArrayList<>();
		for (Span a: s1s) {
			for (Span b: s2s) {
				Span intersect = Span.intersection(a, b);
				if (intersect == null) continue;
				intersects.add(new SpanList(intersect));
			}
		}
		return union(intersects);
	}
	
	/**
	 * Takes the union of two SpanList objects. The spans of the resulting SpanList
	 * are in ascending order.
	 * 
	 * @param s1  the first SpanList object 
	 * @param s2  the second 
	 * @return  the union of <var>s1</var> and <var>s2</var>
	 */
	public static SpanList union(SpanList s1, SpanList s2) {
		if (s1 == null && s2 == null) return null;
		if (s1 == null) {
			if (s2.size() > 0) return s2;
			return null;
		}
		if (s2 == null) {
			if (s1.size() > 0) return s1;
			return null;
		}
		if (s1.equals(s2)) return s1;
		SpanList spl = null;
		List<Span> s1s = s1.getSpans();
		List<Span> s2s = s2.getSpans();
		s1s.removeAll(s2s);
		int s1size = s1s.size();
		int s2size = s2s.size();
		int i = 0; int j = 0; int k = 0;
		while (k < (s1size + s2size)) {
			if (i == s1size) {
				if (spl == null) return s2; 
				else spl.getSpans().addAll(s2s.subList(j, s2s.size()));
				break;
			}
			else if ( j == s2size) {
				if (spl == null) return s1; 
				else spl.getSpans().addAll(s1s.subList(i, s1s.size()));
				break;
			}
			else if (Span.atLeft(s1s.get(i),s2s.get(j))) {
				if (spl == null) spl = new SpanList(s1s.get(i));
				else spl.getSpans().add(s1s.get(i));
				k++;
				i++;
			}
			else if (Span.atLeft(s2s.get(j), s1s.get(i))) {
				if (spl == null) spl = new SpanList(s2s.get(j));
				else spl.getSpans().add(s2s.get(j));
				k++;
				j++;
			} else {
				if (spl == null) spl = new SpanList(Span.union(s1s.get(i), s2s.get(j)));
				else spl.getSpans().add(Span.union(s1s.get(i), s2s.get(j)));
				i++; j++; k += 2;
			}
		}
		return spl;
	}
	
	/**
	 * Similar to {@link #union(SpanList, SpanList)}, but takes as input a list of <code>SpanList</code> objects.
	 * 
	 * @param spls  a list of <code>SpanList</code> objects
	 * @return  the union of all spans in the list
	 */
	public static SpanList union(List<SpanList> spls) {
		if (spls == null || spls.size() ==0) return null;
		int ind = 0;
		for (int i=0;i<spls.size();i++) {
			SpanList spi = spls.get(i);
			if (spi == null) continue;
			else {ind = i; break;}
		}
		SpanList union = spls.get(ind);
		for (int i=ind+1;i<spls.size();i++) {
			SpanList spi = spls.get(i);
			if (spi == null) continue;
			union = union(union,spi);
		}
		return union;
	}
	
	/**
	 * Determines whether two multi-part span objects overlap. 
	 * 
	 * @param  s1 the first SpanList object
	 * @param  s2 the second SpanList object
	 * @return  true if the SpanList objects have overlap
	 */
	public static boolean overlap(SpanList s1, SpanList s2) {
		if (s1.getSpans() == null || s2.getSpans() == null) return false;
		for (Iterator<Span> iter = s1.spans.iterator(); iter.hasNext();) {
            Span s = iter.next();
            Span s3 = null;
        	for (Iterator<Span> iter2 = s2.spans.iterator(); iter2.hasNext();) {
        		s3 = iter2.next();
        		if (Span.overlap(s,s3)) return true;  
            }
        }       
        return false;
	}
	
	/**
	 * Determines whether the first SpanList object subsumes the second completely.
	 * 
	 * @param s1  the first SpanList object
	 * @param s2  the second SpanList object
	 * @return  true if s1 subsumes s2.
	 */
	public static boolean subsume(SpanList s1, SpanList s2) {
		if (s1.getSpans() == null || s2.getSpans() == null) return false;
		for (Iterator<Span> iter = s2.spans.iterator(); iter.hasNext();) {
			Span s = iter.next();
			Span s3 = null;
            boolean subsumed = false;
        	for (Iterator<Span> iter2 = s1.spans.iterator(); iter2.hasNext();) {
        		s3 = iter2.next();
        		if (Span.subsume(s3,s)) {
        			subsumed = true; 
        			break;
        		}
            }
        	if (!subsumed) return false;
        } 
		return true;
	}
	
	/**
	 * Determines whether the first SpanList object is to the left of the second in its entirety.
	 * 
	 * @param s1  the first SpanList object
	 * @param s2  the second SpanList object
	 * @return  true if s1 is to the left of s2.
	 */
	public static boolean atLeft(SpanList s1, SpanList s2) {
		int s1s = s1.first().getBegin(); int s1e = s1.last().getEnd();
		int s2s = s2.first().getBegin(); int s2e = s2.last().getEnd();
//		return (s1s <= s1e && s1e < s2s && s2s <= s2e);
		return (s1s < s1e && s1e <= s2s && s2s < s2e);
	}
	
	/**
	 * 
	 * @param sp a SpanList object 
	 * @return  true if the SpanList object is invalid (for at least one of the spans, begin position is to the right of the end position)
	 */
	public static boolean invalidSpan(SpanList sp) {
		if (sp.getSpans().size() == 0)  return false;
		for (Span s: sp.getSpans()) {
			if (Span.invalidSpan(s)) return true;
		}
		return false;
	}
	
	public int compareTo(SpanList sp) {
		for (int i=0; i < spans.size(); i++) {
			Span s = spans.get(i);
			if (i >= sp.getSpans().size()) {
				return -1;
			}
            Span s2 = sp.getSpans().get(i);
            int comp = s.compareTo(s2);
            if (comp == 0) continue;
            else return comp;
		}
		return 0;
	}
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		SpanList s = (SpanList)obj;
		if (size() != s.size()) return false;
		if (this.compareTo(s) == 0) return true;
		return false;
	}
	
	public int hashCode() {
		int a = 113;
		for (Span sp: spans) {
			a ^= sp.hashCode() * 97;
		}
		return a;
	}
}
