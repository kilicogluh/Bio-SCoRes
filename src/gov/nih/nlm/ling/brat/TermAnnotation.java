package gov.nih.nlm.ling.brat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;

/**
 * The class to represent a single term annotation. A term is an atomic
 * unit of meaning that should be treated as a single unit. A term
 * can potentially cover non-adjacent units, that's why the span is 
 * specified as a list.
 * 
 * @author Halil Kilicoglu
 */
public class TermAnnotation extends AbstractAnnotation {

	protected final SpanList span;
	protected final String text;
	protected List<Reference> references = new ArrayList<>();

	/**
	 * Creates a <code>TermAnnotation</code> object. 
	 * 
	 * @param id  	annotation id
	 * @param docId document id
	 * @param type  the semantic type (e.g., Drug, Disease)
	 * @param span  the character offsets for the annotations 
	 * @param text  text covered by the annotation
	 */
	public TermAnnotation(String id, String docId, String type, SpanList span, 
			String text) {
		super(id,docId,"Entity", type);
		this.span = span;
		this.text = text;
	}
	
	/**
	 * Creates a <code>TermAnnotation</code> object with a reference concept.
	 * 
	 * @param id  	annotation id
	 * @param docId document id
	 * @param type  the semantic type 
	 * @param span  the character offsets for the annotations 
	 * @param text  text covered by the annotation
	 * @param reference	the concept information
	 */
	public TermAnnotation(String id, String docId, String type, SpanList span, 
			String text, Reference reference) {
		this(id,docId,type,span,text);
		this.references = Arrays.asList(reference);
	}
	
	/**
	 * Creates a <code>TermAnnotation</code> object with a list of reference concepts.
	 * 
	 * @param id  	annotation id
	 * @param docId document id
	 * @param type  the semantic type 
	 * @param span  the character offsets for the annotations 
	 * @param text  text covered by the annotation
	 * @param references	the concepts
	 */
	public TermAnnotation(String id, String docId, String type, SpanList span, 
			String text, List<Reference> references) {
		this(id,docId,type,span,text);
		this.references = references;
	}
		
	public SpanList getSpan() {
		return span;
	}

	public String getText() {
		return text;
	}
	
	public void addReference(Reference r) {
		if (references == null) references = new ArrayList<>();
		references.add(r);
	}

	public List<Reference> getReferences() {
		return references;
	}

	public void setReferences(List<Reference> references) {
		this.references = references;
	}

	/**
	 * Get the character offsets for the <var>n</var>th unit.
	 * 
	 * @param n	the order of the span to return
	 * @return <var>n</var>th span of this annotation
	 */
	public Span getSpan(int n) {
		return span.getSpans().get(n);
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(id + "\t" + type + " " + text + "\"[" + span.toString() + "]");
		for (Reference r: references) {
			buf.append("_" + r.toResolvedString());
		}
		return buf.toString();
	}
	
	/**
	 * @return an easy-to-read representation of the annotation
	 */
	public String toResolvedString() {
		return type + "_\"" + text + "\"[" + span.toString() + "]";
	}
	
	// TODO Make reference an option
	public String toStandoffAnnotation() {
		StringBuffer buf = new StringBuffer();
		buf.append(id + "\t" + type + " " + span.toStandoffAnnotation() + "\t" + text + "\n");
/*		for (Reference r: references) {
			buf.append(r.toResolvedString());
		}*/
		return buf.toString();
	}

	/**
	 * Checks whether the term annotation matches exactly another annotation
	 * by checking the character offsets and semantic types
	 * 
	 * @param a  annotation to compare with 
	 * @return true if exact match
	 */
	public boolean exactMatch(Annotation a) {
		if (a instanceof TermAnnotation == false) return false;
		TermAnnotation at = (TermAnnotation) a;
		return (type.equals(at.getType()) && span.equals(at.getSpan()));
	}
	
	/**
	 * Checks whether the term annotation approximately matches another annotation.
	 * Semantic type match is required but the character offset overlap is sufficient.
	 * 
	 * @param a  annotation to compare with 
	 * @return true if approximate match
	 */
	public boolean approximateMatch(Annotation a) {
		if (a instanceof TermAnnotation == false) return false;
		TermAnnotation at = (TermAnnotation) a;
		return (type.equals(at.getType()) && SpanList.overlap(span, at.getSpan()));
	}
	
	/**
	 * Checks whether the term annotation matches another annotation based simply on spans.
	 * 
	 * @param a  annotation to compare with 
	 * @return true if span match
	 */
	public boolean spanMatch(Annotation a) {
		if (a instanceof TermAnnotation == false) return false;
		TermAnnotation at = (TermAnnotation) a;
		return (SpanList.overlap(span, at.getSpan()));
	}
	
	/**
	 * Checks whether the normalized term annotation matches another normalized annotation.
	 * Exact or approximate match can still be specified.
	 * 
	 * @param approximateMatch	whether to perform approximate match for offsets
	 * @param a  				annotation to compare with 
	 * @return true if the annotations are normalized to the same concept
	 */
	public boolean referenceMatch(boolean approximateMatch, Annotation a) {
		if (a instanceof TermAnnotation == false) return false;
		TermAnnotation at = (TermAnnotation) a;
		List<Reference> aRefs = at.getReferences();
		if (references.size() == 0 || aRefs.size() == 0) {
			if ((!approximateMatch && !exactMatch(a)) || 
				(approximateMatch && !approximateMatch(a))) {
				return false;
			}
		}
		for (Reference r: references) {
			for (Reference ar: aRefs) {
				if (r.equals(ar))  {
					return true;
				} 
			} 
		}
		return false;
	}
	// TODO More strict
/*	public boolean referenceMatch(boolean approximateMatch, Annotation a) {
		if (!(a instanceof TermAnnotation)) return false;
		TermAnnotation at = (TermAnnotation) a;
		if (type.equals(at.getType()) == false) return false;
		List<Reference> aRefs = at.getReferences();
		if (references.size() != aRefs.size()) return false;
		if (references.size() == 0 && aRefs.size() == 0) {
			if ((!approximateMatch && !exactMatch(a)) || 
				(approximateMatch && !approximateMatch(a))) {
				return false;
			}
		}
		List<Reference> seenA = new ArrayList<Reference>();
		List<Reference> seenB = new ArrayList<Reference>();
		for (Reference r: references) {
			for (Reference ar: aRefs) {
				if (seenB.contains(ar)) continue;
				if (r.equals(ar))  {
					seenA.add(r);
					seenB.add(ar);
//					break;
				} 
			} 
			if (!(seenA.contains(r))) return false;
		}
		if (seenB.size() == aRefs.size() && seenA.size() == references.size()) return true;
		return false;
	} */

	public boolean equals(Annotation a) {
		return (a instanceof TermAnnotation && exactMatch(a));
	}
	
	public int compareTo(Annotation a) {
		if (a instanceof TermAnnotation == false) return 1;
		TermAnnotation at = (TermAnnotation)a;
		int c = span.compareTo(at.getSpan());
		if (c == 0) {
			int tc =  type.compareTo(at.getType());
			if (tc == 0) return id.compareTo(a.getId());
			else return tc;
		}
		else return c;
	}
	
	public int hashCode() {
		return 113 ^ span.hashCode() + 127 ^ type.hashCode() + 171 * text.hashCode();
	}
}