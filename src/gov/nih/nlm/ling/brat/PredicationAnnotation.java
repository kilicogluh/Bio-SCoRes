package gov.nih.nlm.ling.brat;

import java.util.List;

import gov.nih.nlm.ling.sem.Predication;

/**
 * The class to represent a {@link Predication} annotation. <p>
 * It extends {@link EventAnnotation} with scalar modality values.
 * 
 * @author Halil Kilicoglu
 *
 */
public class PredicationAnnotation extends EventAnnotation {

	private String scale;
	private double value;

	/**
	 * Creates an <code>PredicationAnnotation</code> object. 
	 * 
	 * @param id		annotation id
	 * @param docId  	document id
	 * @param type  	semantic type of the predication
	 * @param args  	typed arguments of the predication
	 * @param predicate the predicate indicating the predication
	 */
	public PredicationAnnotation(String id, String docId, String type,
			List<AnnotationArgument> args, TermAnnotation predicate) {
		super(id, docId, type, args,predicate);
	}
	
	/**
	 * Creates an <code>PredicationAnnotation</code> object with a scalar modality value 
	 * 
	 * @param id		annotation id
	 * @param docId  	document id
	 * @param type  	semantic type of the predication
	 * @param args  	typed arguments of the predication
	 * @param predicate the predicate indicating the predication
	 * @param scale		the scale associated with the predication
	 * @param value 	value on the scale
	 */
	public PredicationAnnotation(String id, String docId, String type,
			List<AnnotationArgument> args, TermAnnotation predicate, String scale, double value) {
		this(id, docId, type, args,predicate);
		this.scale = scale;
		this.value = value;
	}
	
	public String getScale() {
		return scale;
	}

	public void setScale(String scale) {
		this.scale = scale;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(super.toString() + " ");
		buf.append(scale + ":" + value);
		return buf.toString().trim();
	}
	
	/**
	 * @return an easier-to-read text representation of the annotation.
	 */
	public String toResolvedString() {
		StringBuffer buf = new StringBuffer();
		buf.append(super.toResolvedString() + " ");
		buf.append(scale + ":" + value);
		return buf.toString().trim();
	}
	
	/**
	 * Checks whether the predication annotation matches exactly another annotation
	 * by checking the semantic types and the exact match of the arguments
	 * 
	 * @param a  annotation to compare with 
	 * @return true if exact match
	 */
	public boolean exactMatch(Annotation a) {
		if (a instanceof PredicationAnnotation == false) return false;
		if ( super.exactMatch(a) == false) return false;
		PredicationAnnotation ae = (PredicationAnnotation)a;
		return (scale.equals(ae.getScale()) && value == ae.getValue());
	}
	
	/**
	 * Checks whether the predication annotation approximately matches another annotation.
	 * Character offset overlap rather than equality is sufficient.
	 * 
	 * @param a  annotation to compare with 
	 * @return true if approximate match
	 */
	public boolean approximateMatch(Annotation a) {
		if (a instanceof PredicationAnnotation == false) return false;
		if ( super.approximateMatch(a) == false) return false;
		PredicationAnnotation ae = (PredicationAnnotation)a;
		return (scale.equals(ae.getScale()) && value == ae.getValue());
	}
	
	/**
	 * Checks whether the normalized predication annotation matches another normalized annotation.
	 * Exact or approximate match can still be specified.
	 * 
	 * @param approximateMatch	whether to perform approximate match for offsets
	 * @param a  				annotation to compare with 
	 * @return true if the annotations are normalized to the same concept
	 */
	public boolean referenceMatch(boolean approximateMatch, Annotation a) {
		if (a instanceof PredicationAnnotation == false) return false;
		if ( super.referenceMatch(approximateMatch,a) == false) return false;
		PredicationAnnotation ae = (PredicationAnnotation)a;
		return (scale.equals(ae.getScale()) && value == ae.getValue());
	}
	
	public int compareTo(Annotation a) {
		if (a instanceof PredicationAnnotation) {
			PredicationAnnotation ea = (PredicationAnnotation)a;
			int c = super.compareTo(ea);
			if (c == 0) {
				int sc = scale.compareTo(ea.getScale());
				if (sc == 0) {
					if (value - ea.getValue() >0 ) return -1;
					else return 1;
				} else return sc;
			} else return c;
		} 
		return 1;	
	}
	
	public int hashCode() {
		return super.hashCode() +  59 * scale.hashCode() + new Double(value).hashCode();
	}
	
 
	
}