package gov.nih.nlm.ling.brat;

import gov.nih.nlm.ling.core.SpanList;

/**
 * The class to represent an event modification annotation, such as speculation, negation, or factuality.
 * 
 * @author Halil Kilicoglu
 *
 */
public class ModificationAnnotation extends AbstractAnnotation {

	Annotation semanticItem;
	String value= null;
	
	/**
	 * Creates a <code>ModificationAnnotation</code> object. 
	 * 
	 * @param id  			annotation id
	 * @param docId  		document id
	 * @param type  		the semantic type (e.g.,Speculation, Negation)
	 * @param semanticItem  the semantic annotation that is being modified
	 */
	public ModificationAnnotation(String id, String docId, String type, Annotation semanticItem) {
		super(id,docId,"Modification", type);
		this.semanticItem = semanticItem;
	}
	
	public ModificationAnnotation(String id, String docId, String type, Annotation semanticItem, String value) {
		this(id,docId,type,semanticItem);
		this.value = value;
	}
	
	public Annotation getSemanticItem() {
		return semanticItem;
	}

	public void setSemanticItem(Annotation semanticItem) {
		this.semanticItem = semanticItem;
	}
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public String toString() {
		if (value == null)			
			return (id + "\t" + type + " " + semanticItem.getId()).trim();
		else 
			return (id + "\t" + type + " " + semanticItem.getId() + " " + value).trim();
	}
	
	/**
	 * @return an easier-to-read text representation of the annotation.
	 */
	public String toResolvedString() {
		StringBuffer buf = new StringBuffer();
		buf.append(type);
		if (value != null) buf.append(" " + value);
		buf.append(" " + semanticItem.getId() + ":" + semanticItem.toResolvedString());
		return buf.toString().trim();
	}

	/**
	 * Checks whether the event modification annotation matches exactly another annotation
	 * by checking the type, event argument and value if possible
	 * 
	 * @param annotation1  annotation to compare with 
	 * @return true if exact match
	 */
	public boolean exactMatch(Annotation annotation1) {
		if (annotation1 instanceof ModificationAnnotation == false) return false;
		ModificationAnnotation ae = (ModificationAnnotation)annotation1;
		Annotation aPred = ae.getSemanticItem();
		if (ae.getType().equals(type) == false) return false;
		if (semanticItem.exactMatch(aPred) == false) return false;
		if (value == null && ae.getValue() == null) return true;
		return (value != null && ae.getValue() != null && value.equals(ae.getValue()));
	}

	/**
	 * Same as {@link #exactMatch(Annotation)}.
	 */
	public boolean approximateMatch(Annotation annotation1) {
		return exactMatch(annotation1);
	}
	
	/**
	 * The same as {@link #exactMatch(Annotation)} or {@link #approximateMatch(Annotation)},
	 * depending on the <var>approximateMatch</var> parameter.
	 * 
	 */
	public boolean referenceMatch(boolean approximateMatch, Annotation annotation1) {
		if (approximateMatch) return approximateMatch(annotation1);
		return exactMatch(annotation1);
	}
	
	/**
	 * The same as approximate match.
	 */
	public boolean spanMatch(Annotation a) {
		return approximateMatch(a);
	}

	@Override
	public SpanList getSpan() {
		return semanticItem.getSpan();
	}
	
	public int compareTo(Annotation a) {
		if (a instanceof ModificationAnnotation == false) return 1;
		ModificationAnnotation ea = (ModificationAnnotation)a;
		int ec = semanticItem.compareTo(ea.getSemanticItem());
		if (ec == 0) {
			int tc = type.compareTo(ea.getType());	
			if (tc == 0) {
				int vc = value.compareTo(ea.getValue());
				if (vc == 0) return id.compareTo(a.getId());
				return vc;
			} else return tc;
		}
		return ec;
	}
	
	public boolean equals(Annotation a) {
		if (a == null) return false;
		return (a instanceof ModificationAnnotation && exactMatch(a));
	}
	
	public int hashCode() {
		int code = 289 * value.hashCode() + 187 * semanticItem.hashCode() +
				   133 * type.hashCode();
		return code;
	}
}
