package gov.nih.nlm.ling.sem;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import gov.nih.nlm.ling.core.SpanList;
import nu.xom.Attribute;
import nu.xom.Element;

/**
 * Represents a multiplicity of semantic items. <p>
 * No assumption is made regarding the identity of the semantic items, 
 * it is not assumed that they are even of the same class. Currently,
 * we assume that there is an explicit predicate that indicates the 
 * conjunction, and therefore it extends {@link Predication}.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Conjunction extends Predication {
	private SpanList span;
	
	/**
	 * Creates a <code>Conjunction</code> object from a set of semantic items and an explicit predicate.
	 * 
	 * @param cid  	the identifier
	 * @param ctype the conjunction type 
	 * @param pred	the predicate for the conjunction
	 * @param sis	the set of semantic objects
	 *
	 */
	public Conjunction(String cid, String ctype, Predicate pred, Set<SemanticItem> sis) {
		super(cid);
		type = ctype;
		predicate = pred;
		arguments = new ArrayList<>();
		for (SemanticItem si: sis) {
			arguments.add(new Argument("CC",si));
			if (span == null) span = si.getSpan();
			else span = SpanList.union(span, si.getSpan());
		}
		if (predicate != null) span = SpanList.union(span,predicate.getSpan());
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getType() {
		return type;
	}
	
	/**
	 * 
	 * @return the conjunct items of this conjunction
	 */
	public List<SemanticItem> getSemanticItems() {
		return getArgItems();
	}
	
	/**
	 * Adds a new conjunct item. 
	 * 
	 * @param conjItem	the conjunct item to add.
	 */
	public void addSemanticItem(SemanticItem conjItem) {
		super.addArgument(new Argument("CC",conjItem));
		if (span == null) span = conjItem.getSpan();
		else span = SpanList.union(span, conjItem.getSpan());
	}
	
	/**
	 * Gets all semantic types of the conjoined items.
	 * 
	 * @return the set of all semantic types
	 */
	public LinkedHashSet<String> getAllSemtypes() {
		LinkedHashSet<String> all = new LinkedHashSet<>();
		for (SemanticItem si: getArgItems()) {
			all.addAll(si.getAllSemtypes());
		}
		return all;
	}
	
	/**
	 * Returns false.
	 */
	public boolean ontologyEquals(Object o) {
		return false;
	}
	
	/**
	 * Returns null.
	 */
	public Ontology getOntology() {
		return null;
	}
	
	public String toStandoffAnnotation() {
		StringBuffer buf = new StringBuffer();
		buf.append(id + "\t" + type + " " +
				   (span == null ? "": span.toStandoffAnnotation()) + "\t" + getText());
		return buf.toString().trim();
	}
	
	public String getText() {
		return getDocument().getStringInSpan(span);
	}
	
	public String toShortString() {
		StringBuffer buf = new StringBuffer();
		for (SemanticItem si: getArgItems()) {
			buf.append(si.toShortString() + "_");
		}
		String str = buf.toString();
		return str.substring(0,str.length()-1);
	}
	
	public Element toXml() {
		Element el = new Element("Conjunction");
		if (id != null) el.addAttribute(new Attribute("id",id));
		if (type != null) el.addAttribute(new Attribute("type",type));
		if (predicate != null) el.addAttribute(new Attribute("predicate",predicate.getId()));
		for (SemanticItem si: getArgItems()) {
			Element siEl = si.toXml();
			el.appendChild(siEl);
		}
		return el;
	}
		
	public int hashCode() {
		return
	    ((id == null ? 59 : id.hashCode()) ^
	     (predicate == null ? 79 : predicate.hashCode()) ^
	     (type  == null ? 89 : type.hashCode()) ^
	     (arguments == null? 109 : arguments.hashCode()));
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Conjunction other = (Conjunction) obj;
		if (id == null) {
			if (other.id != null) return false;
		} else if (!id.equals(other.id)) return false;
		if (predicate == null) {
			if (other.predicate != null)
				return false;
		} else if (!predicate.equals(other.predicate))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return argsEquals(other);
	}
	
}
