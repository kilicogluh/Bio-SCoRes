package gov.nih.nlm.ling.sem;

import java.util.HashMap;
import java.util.Map;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;

/** 
 * The basic implementation of an atomic semantic object, <code>Term</code>. 
 * 
 * @author Halil Kilicoglu
 *
 */
public abstract class AbstractTerm implements Term {
	
	protected String id;
	protected String type;
	protected SpanList span;
	protected SpanList headSpan;
	protected SurfaceElement surfaceElement;
	protected Map<String,Object> features;
	
	public AbstractTerm(String id) {
		this.id = id;
	}
	
	/**
	 * Instantiates a simple term with a type and span.
	 * 
	 * @param id	the term id
	 * @param type  the term type
	 * @param sp  	the term span
	 */
	public AbstractTerm(String id, String type, SpanList sp) {
		this(id);
		this.type = type;
		this.span = sp;
	}

	/**
	 * Instantiates a simple term with a type, span, and the associated textual unit.
	 * 
	 * @param id  	the term id
	 * @param type  the term type
	 * @param sp  	the term span
	 * @param se  	the textual unit
	 */
	public AbstractTerm(String id, String type, SpanList sp, SurfaceElement se) {
		this(id, type, sp);
		this.surfaceElement = se;
	}
	
	/**
	 * Instantiates a simple term with a type, span, head span, and the associated textual unit.
	 * 
	 * @param id		the term id
	 * @param type  	the term type
	 * @param sp		the term span
	 * @param headSp  	the head span
	 * @param se  		the textual unit
	 */
	public AbstractTerm(String id, String type, SpanList sp, SpanList headSp, SurfaceElement se) {
		this(id, type, sp, se);
		this.headSpan = headSp;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getType() {
		return type;
	}
	
	@Override
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Gets the string of the term mention, from its span. If the term corresponds to 
	 * a gapped textual unit, it will return the strings of these contiguous units
	 * separated by a space.
	 * 
	 * @return  the text associated with the term
	 */
	public String getText() {
		if (surfaceElement == null) return "";
		StringBuffer buf = new StringBuffer();
		Document doc = surfaceElement.getSentence().getDocument();
		for (Span sp: span.getSpans()) {
			buf.append(doc.getText().substring(sp.getBegin(),sp.getEnd()) + " ");
		}
		return buf.toString().trim();
	}
	
	/**
	 * Gets the text of the subsuming textual unit. <p>
	 * The difference between this method and {@code #getText()} is that this method will
	 * simply return the associated textual unit's text. For example, <i>the prognosis</i>
	 * may have two terms associated with it, a coreferential mention associated with the full phrase,
	 * and a predicate associated with <i>prognosis</i> only. <p>
	 * If one wants to get <i>prognosis</i> only for the predicate term, <code>getText</code>
	 * should be used instead.
	 * 
	 * @return the text associated with the textual unit that subsumes this term
	 */
	public String getSubsumingText() {
		if (surfaceElement == null) return "";
		return surfaceElement.getText();
	}
	
	@Override
	public SpanList getSpan() {
		return span;
	}

	public void setSpan(SpanList span) {
		this.span = span;
	}

	@Override
	public SpanList getHeadSpan() {
		return headSpan;
	}
	
	public void setHeadSpan(SpanList headSpan) {
		this.headSpan = headSpan;
	}
	
	@Override
	public Document getDocument() {
		return surfaceElement.getSentence().getDocument();
	}
	
	@Override
	public SurfaceElement getSurfaceElement() {
		return surfaceElement;
	}

	@Override
	public void setSurfaceElement(SurfaceElement surfaceElement) {
		this.surfaceElement = surfaceElement;
	}
	
	@Override
	public Map<String, Object> getFeatures() {
		return features;
	}

	@Override
	public void setFeatures(Map<String, Object> features) {
		this.features = features;
		
	}

	@Override
	public void addFeature(String name, Object feature) {
		if (features == null) features = new HashMap<>();
		features.put(name, feature);
	}

	public boolean isNegated() {
		return (features != null && features.containsKey("negation") && ((Boolean)features.get("negation")));
	}

	public void setNegated(boolean isNegated) {
		addFeature("Negated",isNegated);
	}

	public boolean isSpeculated() {
		return (features != null && features.containsKey("speculation") && ((Boolean)features.get("speculation")));
	}

	public void setSpeculated(boolean isSpeculated) {
		addFeature("Speculated",isSpeculated);
	}
	
	/**
	 * Returns the standoff annotation text for this term. <p>
	 * The standoff annotation for a term is (tabs after ID and before TEXT). 
	 * [ID]	[TYPE] [SPAN]	[TEXT]
	 *  
	 */
	public String toStandoffAnnotation() {
		StringBuffer buf = new StringBuffer();
		buf.append(id + "\t" + type + " " +
				   (span == null ? "": span.toStandoffAnnotation()) + "\t" + getText());
		return buf.toString().trim();
	}
	
	/**
	 * A character-offset based comparison of terms.
	 */
	public int compareTo(SemanticItem so) {
		SpanList sosp = so.getSpan();
		return span.compareTo(sosp);
	}
	
	@Override
	public int hashCode() {
		return 
	    ((id == null ? 89 : id.hashCode()) ^
	     (type  == null ? 97 : type.hashCode()) ^
	     // This was changed because CD40-TRAF2 had the same surface element, but two separate terms
//	     (surfaceElement == null? 103 : surfaceElement.hashCode()));
	     (getText() == null ? 103: getText().hashCode()) ^
	     (span == null ? 119 : span.hashCode()));
	}
	
	/**
	 * Equality on the basis of type and mention equality.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		AbstractTerm at = (AbstractTerm)obj;
		return (id.equals(at.getId()) &&
			        type.equals(at.getType()) &&
			        getText().equals(at.getText()) &&
			        span.equals(at.getSpan()));
	}

}
