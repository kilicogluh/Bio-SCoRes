package gov.nih.nlm.ling.sem;

import java.util.LinkedHashSet;
import java.util.List;

import nu.xom.Attribute;
import nu.xom.Element;

/**
 * Implementation of a relation that is implicit; in other words, there are no predicates 
 * that indicate the relation. Implicit discourse relations between sentences is one example of
 * such relations.
 * 
 * @author Halil Kilicoglu
 *
 */
public class ImplicitRelation extends AbstractRelation {
	
	public ImplicitRelation(String id) {
		super(id);
	}

	public ImplicitRelation(String id, String type) {
		super(id, type);
	}

	public ImplicitRelation(String id, List<Argument> args) {
		super(id, args);
	}

	public ImplicitRelation(String id, String type, List<Argument> args) {
		super(id, type, args);
	}

	@Override
	public LinkedHashSet<String> getAllSemtypes() {
		LinkedHashSet<String> types = new LinkedHashSet<String>();
		types.add(type);
		return types;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(type + "(" + id + "_");
		for (int i= 0; i < arguments.size(); i++) {
			buf.append("_" + arguments.get(i).getType() + "(" + arguments.get(i).getArg().toString() + ")");
		}
		return buf.toString();
	}
	
	@Override
	public String toShortString() {
		return toString();
	}

	/**
	 * Gets the standoff annotation text for the implicit relation.<p>
	 * The standoff annotation format for an implicit relation is the following (tab after [ID])
	 * [ID]	[TYPE] [ARG1_ROLE]:[ARG1_ID] [ARG2_ROLE]:[ARG2:ID] ...
	 * 
	 */
	public String toStandoffAnnotation() {
		StringBuffer buf = new StringBuffer();
		buf.append(id + "\t" + type + " ");
		for (int i= 0; i < arguments.size(); i++) {
			buf.append(arguments.get(i).getType() + ":" + arguments.get(i).getArg().getId() + " ");
		}
		return buf.toString().trim();
	}

	@Override
	public Element toXml() {
     	Element el = new Element("Relation");
     	el.addAttribute(new Attribute("id",id));
     	el.addAttribute(new Attribute("type",type));
		if (features.size() > 0) {
			for (String s: features.keySet()) {
				el.addAttribute(new Attribute(s,features.get(s).toString()));
			}
		}
		for (int i= 0; i < arguments.size(); i++) {
			String t = arguments.get(i).getType();
			SemanticItem sem = arguments.get(i).getArg();
			Element argEl = new Element(t);
			argEl.addAttribute(new Attribute("idref",sem.getId()));
			el.appendChild(argEl);
		}  	
		return el;
	}

}
