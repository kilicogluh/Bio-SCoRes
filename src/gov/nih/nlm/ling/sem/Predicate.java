package gov.nih.nlm.ling.sem;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.Sense.ScopeType;
import nu.xom.Attribute;
import nu.xom.Element;

/**
 * Implementation of an atomic term that corresponds to a relation indicator. <p> 
 * It is similar to {@link Entity}, but its ontological status is different from an entity term,
 * in that most ontologies would not encode predicates. The relation between a <code>Predicate</code>
 * and {@link Indicator} is similar to one between an <code>Entity</code> and a set of <code>Concept</code>s. <p>
 * Predicates are generally taken to be verbs, nominalizations, and adjectives, 
 * but we are trying to be more general here, even though specific pieces of the code may make 
 * different assumptions about predicates.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Predicate extends AbstractTerm {
	
	public static final String GENERIC = "GENERIC_PREDICATE";
	
	private Indicator indicator;
	private Sense sense;
	
	public Predicate(String id, String type) {
		super(id);
		this.type = type;
	}
	
	public Predicate(String id, String type, SpanList sp, SurfaceElement se) {
		super(id, type, sp, se);	
	}
	
	public Predicate(String id, String type, SpanList sp, SpanList headSp, SurfaceElement se) {
		super(id, type, sp, headSp, se);	
	}
		
	/**
	 * Constructs a <code>Predicate</code> object with an abstract indicator.
	 * 
	 * @param id		the predicate identifier
	 * @param sp		the predicate span
	 * @param type		the predicate type
	 * @param indicator	the abstract indicator that the predicate maps to
	 */
	public Predicate(String id, SpanList sp, String type, Indicator indicator) {
		super(id,type,sp);
		this.indicator = indicator;
	}
	
	/**
	 * Constructs a <code>Predicate</code> object with an abstract indicator and a selected sense.
	 * 
	 * @param id		the predicate identifier
	 * @param sp		the predicate span
	 * @param type		the predicate type
	 * @param indicator	the abstract indicator that the predicate maps to
	 * @param sense		the sense of the indicator selected
	 */
	public Predicate(String id, SpanList sp, String type, Indicator indicator, Sense sense ) {
		this(id,sp,type,indicator);
		this.sense = sense;
		
	}
	
	/**
	 * 
	 * @return  the indicator associated with the predicate, or null if none is set
	 */
	public Indicator getIndicator() {
		return indicator;
	}

	public void setIndicator(Indicator indicator) {
		this.indicator = indicator;
	}
	
	/**
	 * @return  the active indicator sense, or null if none is set
	 */
	public Sense getSense() {
		return sense;
	}

	public void setSense(Sense sense) {
		this.sense = sense;
	}
	
	
	/**
	 * Finds the active semantic type (i.e., associated with the "sense"). <p>
	 * Returns the term type if no sense is selected. 
	 * Returns a set to conform to interface definition.
	 * 
	 * @return  the active semantic type
	 */
	public Set<String> getSemtypes() {
		Set<String> semtypes = new HashSet<String>();
		if (sense != null )
			semtypes.add(sense.getCategory());
		else semtypes.add(type);
		return semtypes;
	}
	
	/**
	 * Finds all semantic types associated with the indicator. Returns the term type if
	 * no indicator is associated with the term.
	 * 
	 * @return  all semantic types
	 */
	public LinkedHashSet<String> getAllSemtypes() {
		LinkedHashSet<String> semtypes = new LinkedHashSet<String>();
		if (indicator != null) {
			List<Sense> senses = indicator.getSenses();
			for (Sense s: senses) {
				semtypes.add(s.getCategory());
			}
		}
		else semtypes.add(type);
		return semtypes;
	}
	
	/**
	 * Finds the active sense of the predicate. <p> 
	 * If no indicator is associated with this predicate, the method will return null.
	 * If there is an indicator but no sense is selected, it will attempt to find the sense
	 * using the type.
	 * 
	 * @return the active sense, null if no indicator is associated with the term
	 */
	public Sense getAssociatedSense() {
		if (sense != null) return sense;
		if (indicator == null) return null;
		for (Sense sem: indicator.getSenses()) {
			if (sem.getCategory().equals(type))
				return sem;
		}
		return null;
	}
	
	/**
	 * Gets the scope type associated with the current sense.
	 * 
	 * @return	the scope type (NARROW, WIDE, or DEFAULT).
	 */
	public ScopeType getScopeType() {
		if (getAssociatedSense() == null) return ScopeType.DEFAULT;
		return sense.getScopeType();
	}

	public String toString() {
		return "PREDICATE(" + id + "_" + type +  "_" + 
		   (span == null ? "": span.toString()) + "," +
		   "_" + getText() + (sense == null ? "" : "_" + sense.toString() + ")");	
	}
	
	public String toShortString() {
		StringBuffer buf = new StringBuffer();
		buf.append(getText() + "(" + id + "_" + type + "_" + 
				   (span == null ? "": span.toString()) + ")");
		return buf.toString();
	}
	
	public Element toXml() {
		Element el = new Element("Term");
		el.addAttribute(new Attribute("xml:space", 
		          "http://www.w3.org/XML/1998/namespace", "preserve"));
		el.addAttribute(new Attribute("charOffset",span.toString()));
		if (headSpan == null) {
			el.addAttribute(new Attribute("headOffset",surfaceElement.getHead().getSpan().toString()));
		} else {
			el.addAttribute(new Attribute("headOffset",headSpan.toString()));
		}
		el.addAttribute(new Attribute("id",id));
		//el.addAttribute(new Attribute("origId",id));
		//el.addAttribute(new Attribute("isName","False"));
		if (features.size() > 0) {
			for (String s: features.keySet()) {
				el.addAttribute(new Attribute(s,features.get(s).toString()));
			}
		}
//		el.addAttribute(new Attribute("negation",Boolean.toString(isNegated())));	
//		el.addAttribute(new Attribute("speculation",Boolean.toString(isSpeculated())));
		el.addAttribute(new Attribute("text",getText()));
		el.addAttribute(new Attribute("type",type));
		return el;
	}
	
	@Override
	public boolean ontologyEquals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		Predicate p = (Predicate)obj;
		if (!(p.getType().equals(type))) return false;
		if (p.getOntology() == null || this.getOntology() == null) return false;
		if (!(p.getOntology().equals(this.getOntology()))) return false;
		return true;
	}
		
	@Override
	public Ontology getOntology() {
		return sense;
	}
	
	/**
	 * Selects a new sense for the term. 
	 * 
	 * @param semtype  the semantic type of the sense 
	 */
	public void modifySense(String semtype) {
		List<Sense> senses = indicator.getSenses();
		for (Sense s: senses) {
			if (s.getCategory().equals(semtype)) {
				setSense(s);
				setType(semtype);
				return;
			}
		}
	}
	
	/**
	 * @deprecated  the same method as the one in {@link AbstractTerm}.
	 */
/*	public int hashCode() {
		return 
	    ((id == null ? 89 : id.hashCode()) ^
	     (type  == null ? 97 : type.hashCode()) ^
	     // This was changed because CD40-TRAF2 had the same surface element, but two separate terms
//	     (surfaceElement == null? 103 : surfaceElement.hashCode()));
	     (getText() == null ? 103: getText().hashCode()) ^
	     (span == null ? 119 : span.hashCode()));
	}*/
	
	/**
	 * Equality on the basis of type and textual unit equality.
	 * 
	 * @deprecated  the same method as the one in {@link AbstractTerm}.
	 */
/*	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		Predicate at = (Predicate)obj;
		return (id.equals(at.getId()) &&
			        type.equals(at.getType()) &&
			        getText().equals(at.getText()) &&
			        span.equals(at.getSpan()));
	}*/
	
}
