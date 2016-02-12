package gov.nih.nlm.ling.sem;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import nu.xom.Attribute;
import nu.xom.Element;

/**
 * Implementation of a basic, textually grounded named entity, with potential mappings to an ontology
 * (i.e., instances of {@link Concept} class).
 * 
 * 
 * @author Halil Kilicoglu
 *
 */
public class Entity extends AbstractTerm {
	
	public static final Entity SOURCE_WRITER = new Entity("T001","WRITER",new LinkedHashSet<Concept>(Arrays.asList(Concept.SOURCE_WRITER)));

	private Set<Concept> concepts;
	private Concept sense;
	
	public Entity(String id, String type) {
		super(id);
		this.type = type;
	}
	
	/**
	 * Constructs an <code>Entity</code> object, with a set of ontological mappings
	 *   
	 * @param id		the identifier of this entity
	 * @param type		the semantic type of this entity
	 * @param concepts	the concepts associated with this entity
	 */
	public Entity(String id, String type, Set<Concept> concepts) {
		this(id,type);
		this.concepts = concepts;
	}

	public Entity(String id, String type, SpanList sp) {
		super(id, type, sp);
	}
	
	public Entity(String id, String type, SpanList sp, SurfaceElement se) {
		super(id, type, sp, se);	
	}
	
	public Entity(String id, String type, SpanList sp, SpanList headSp, SurfaceElement se) {
		super(id, type, sp, headSp, se);	
	}
	
	/**
	 * A concept refers to the ontological information associated with the entity term.
	 * It can be thought of as analogous to a UMLS concept.
	 * 
	 * @return  the set of concepts associated with the entity term
	 */
	public Set<Concept> getConcepts() {
		return concepts;
	}

	public void setConcepts(Set<Concept> concepts) {
		this.concepts = concepts;
	}
	
	/**
	 * An entity term may correspond to multiple concepts (polysemy), but for processing purposes,
	 * only one of them is active at a time. The active concept is called the <i>sense</i>.
	 * In the course of interpretation, the term sense can be changed. 
	 * 
	 * @return  the sense associated with the term
	 */
	public Concept getSense() {
		return sense;
	}

	public void setSense(Concept sense) {
		this.sense = sense;
	}

	/**
	 * Finds the active semantic types (i.e., those associated with the <i>sense</i>). Returns the term type if
	 * no sense is selected.
	 * 
	 * @return  the active semantic types
	 */
	public LinkedHashSet<String> getSemtypes() {
		if (sense == null) {
			LinkedHashSet<String> semtypes = new LinkedHashSet<String>();
			semtypes.add(type);
			return semtypes;
		}
		return sense.getSemtypes();
	}
	
	/**
	 * Finds all semantic types (i.e., those associated with the concepts). Returns the term type if
	 * no concept is associated with the term
	 * 
	 * @return  all semantic types
	 */
	public LinkedHashSet<String> getAllSemtypes() {
		LinkedHashSet<String> semtypes = new LinkedHashSet<String>();
		if (concepts == null || concepts.size() == 0) return getSemtypes();
		for (Concept c: concepts) {
			semtypes.addAll(c.getSemtypes());
		}
		return semtypes;
	}
		
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("TERM(" + id + "_"  + type + "_" +
				   (span == null ? "": span.toString()) + "_" + getText());
		if (concepts != null) {
			for (Concept c: concepts) {
				buf.append("_" + c.toString());
			}
		}
		buf.append(")");
		return buf.toString();
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
		el.addAttribute(new Attribute("id",id));
		el.addAttribute(new Attribute("type",type));
		el.addAttribute(new Attribute("charOffset",span.toString()));
		if (headSpan == null) {
			el.addAttribute(new Attribute("headOffset",surfaceElement.getHead().getSpan().toString()));
		} else {
			el.addAttribute(new Attribute("headOffset",headSpan.toString()));
		}
		if (features != null) {
			for (String s: features.keySet()) {
				el.addAttribute(new Attribute(s,features.get(s).toString()));
			}
		}
//		el.addAttribute(new Attribute("negation",Boolean.toString(isNegated())));	
//		el.addAttribute(new Attribute("speculation",Boolean.toString(isSpeculated())));
		el.addAttribute(new Attribute("text",getText()));
		if (concepts != null) {
			for (Concept c: concepts) {
				el.appendChild(c.toXml(sense.equals(c) ? true : false));
			}	
		}
		return el;
	}
	
	/**
	 * Returns standoff annotation for this entity. This method allows adding concept information 
	 * to standoff annotation as well. Concept information will be added as a reference item.
	 * 
	 * @param withConcepts	whether to include concept information
	 * @param refId			the beginning identifier to use for the concepts in standoff format
	 * @return	the text representation of this entity in standoff annotation format
	 */
	public String toStandoffAnnotation(boolean withConcepts, int refId) {
		String standoff = toStandoffAnnotation();
		if (!withConcepts || concepts == null) return standoff;
		StringBuffer buf = new StringBuffer();
		buf.append(standoff);
		buf.append("\n");
		Concept s = sense;
	
		if (s == null) s = concepts.iterator().next();
		if (s != null)
			buf.append("N" + ++refId + "\t" + "CUI " + id + " " + s.getId() + "\t" + s.getName() + "\n");
		for (Concept c: concepts) {
			if (c.equals(s)) continue;
			buf.append("N" + ++refId + "\t" + "CUI " + id + " " + c.getId() + "\t" +  c.getName() + "\n");
		}
		return buf.toString().trim();
	}
	
	@Override
	public boolean ontologyEquals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		Entity e = (Entity)obj;
		if (!(e.getType().equals(type))) return false;
		if (e.getOntology() == null && this.getOntology() == null && e.getText().equalsIgnoreCase(getText())) return true;
		if (e.getOntology() == null || this.getOntology() == null) return false;
		if (!(e.getOntology().equals(this.getOntology()))) return false;
		return true;
	}
	
	@Override
	public Ontology getOntology() {
		return sense;
	}

	/**
	 * Selects a new sense for the term. 
	 * 
	 * @param semtype  the semantic type of the sense to associate
	 */
	public void modifySense(String semtype) {
		if (concepts == null || concepts.size() == 0) return;
		for (Concept c:concepts) {
			if (c.getSemtypes().contains(semtype)) {
				setSense(c);
				setType(semtype);
				return;
			}
		}
	}
	
}
