package gov.nih.nlm.ling.sem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Logger;

import nu.xom.Attribute;
import nu.xom.Element;

/**
 * Represents an attribute of a semantic item. <p>
 * No assumption is made regarding the identity of the semantic item.  
 * For simplicity, it extends {@link AbstractRelation} and therefore can 
 * involve multiple semantic items as arguments, even though so far
 * it has been used as a single-argument relation, where the argument
 * is the semantic item in question.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Modification extends AbstractRelation {
	private static Logger log = Logger.getLogger(Modification.class.getName());
	
	private String value = null;

	public Modification(String id) {
		super(id);
	}

	public Modification(String id, String type) {
		super(id, type);
	}

	public Modification(String id, List<Argument> args) {
		super(id, args);
	}

	public Modification(String id, String type, List<Argument> args) {
		super(id, type, args);
	}
	
	public Modification(String id, String type, List<Argument> args, String value) {
		this(id, type, args);
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public LinkedHashSet<String> getAllSemtypes() {
		LinkedHashSet<String> sems = new LinkedHashSet<String>();
		sems.add(type);
		return sems;
	}

	@Override
	public String toShortString() {
		StringBuffer buf = new StringBuffer();
		buf.append(type);
		if (value != null) buf.append("_" + value);
		if (arguments == null) {
			log.severe("No arguments for modification.");
			buf.append(",NO_ARG");
		}
		for (Argument a: arguments) {
			buf.append("|" + a.getType() + "(" + a.getArg().toShortString() + ")");
		}	
		buf.append(")");
		return buf.toString();
	}

	@Override
	public String toStandoffAnnotation() {
		StringBuffer buf = new StringBuffer();
		if (arguments == null) {
			log.severe("Malformed modification.");
			return buf.toString();
		}
		buf.append(id + "\t");
		if (type != null)  { 
			buf.append(type + " ");
		}
		for (int i= 0; i < arguments.size(); i++) {
			buf.append(arguments.get(i).getArg().getId());
		}
		if (value != null) buf.append(" " + value);
		return buf.toString().trim();
	}

	@Override
	public Element toXml() {
		Element el = new Element("Modification");
		el.addAttribute(new Attribute("id",id));
		if (type != null) el.addAttribute(new Attribute("type",type));
		for (int i= 0; i < arguments.size(); i++) {
			Argument arg = arguments.get(i);
			el.appendChild(arg.toXml());
		}
		if (value != null) el.addAttribute(new Attribute("value",value));
		return el;
	}

}
