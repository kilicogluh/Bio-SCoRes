package gov.nih.nlm.ling.sem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import nu.xom.Attribute;
import nu.xom.Element;

/**
 * Represents a relation argument. An argument is basically a role/semantic item pair.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Argument {
	private String type;
	private SemanticItem arg;
	
	/* This is QA-specific, does not really belong here. 
	   How would one sort arguments?
	 */
/*	public static final Comparator<Argument> SALIENCE = 
        new Comparator<Argument>() {
		public int compare(Argument a1, Argument a2) {
			String type1 = a1.getType();
			String type2 = a2.getType();
			if (type1.equals(type2))  {
				SemanticItem as1 = a1.getArg();
				SemanticItem as2 = a2.getArg();
				if (as1 instanceof Term && as2 instanceof Term) {
					String a1str = ((Term)as1).getText().toLowerCase();
					String a2str = ((Term)as2).getText().toLowerCase();
					return a1str.compareTo(a2str);
				}
			} else {
				if (type2.equalsIgnoreCase("qcue")) return 1;
				else if (type2.equalsIgnoreCase("theme")) return 1;
			}
			return -1;
		}	
	}; */
		
	/**
	 * Creates an <code>Argument</code> object with a role and semantic object.
	 * 
	 * @param type  the role
	 * @param arg  the semantic object
	 */
	public Argument(String type, SemanticItem arg) {
		this.type = type;
		this.arg = arg;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public SemanticItem getArg() {
		return arg;
	}

	public void setArg(SemanticItem arg) {
		this.arg = arg;
	}
	
	/**
	 * Finds the semantic items that are arguments (the values) in a list of <code>Argument</code> objects.
	 * 
	 * @param  arguments	a list of arguments
	 * @return 				all semantic objects that are arguments, null if the argument list is null
	 */
	public static List<SemanticItem> getArgItems(List<Argument> arguments) {
		if (arguments == null) return null;
		List<SemanticItem> args = new ArrayList<>();
		for (Argument a: arguments) {
			args.add(a.getArg());
		}
		return args;
	}
	
	/**
	 * Finds the roles of the arguments in a list of <code>Argument</code> objects.
	 * 
	 * @param arguments a list of arguments
	 * @return 			all role names, null if argument list is null
	 */
	public static List<String> getArgNames(List<Argument> arguments) {
		if (arguments == null) return null;
		List<String> names = new ArrayList<>();
		for (Argument a: arguments) {
			names.add(a.getType());
		}
		return names;
	}
	
	/**
	 * Finds the arguments with a particular role.<p>
	 * It allows approximate matches (e.g., ADJUNCT role will return ADJUNCT_TO, ADJUNCT_IN, etc.)
	 * 
	 * @param args  a list of arguments
	 * @param name  the role name
	 * @return arguments whose role begin with the name, null if the argument list is null
	 */
	public static List<Argument> getArgs(List<Argument> args, String name) {
		if (args == null) return null;
		List<Argument> sos = new ArrayList<>();
		for (int i= 0; i < args.size(); i++) {
			String s = args.get(i).getType();
			// equals causes problems with Theme2, etc.
			// but this can be dangerous, too.
			if (s.startsWith(name)) sos.add(args.get(i));
		}
		return sos;
	}
	/**
	 * Similar to {@link #getArgs(List,String)} except this method accepts a list of roles.
	 * 
	 * @param args	a list of arguments
	 * @param names the array of role names
	 * @return 		arguments with the queried roles, null if the argument list is null
	 */
	public static List<Argument> getArgs(List<Argument> args, List<String> names) {
		if (args == null) return null;
		List<Argument> sos = new ArrayList<>();
		for (String name: names) {
			List<Argument> sos1 = getArgs(args,name);
			if (sos1 != null) sos.addAll(sos1);
		}
		return sos;
	}

	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		Argument a = (Argument)obj;
		return (this.getType().equals(a.getType()) &&
		        this.getArg().equals(a.getArg()));
	}
	
	public int hashCode() {
	    return
	    ((type  == null ? 83 : type.hashCode()) ^ 
	     (arg == null ? 67 : arg.hashCode()));
	}
	
	public String toString() {
		return type + ":" + arg.toString();
	}
	
	public String toShortString() {
		return type + ":" + arg.toShortString();
	}
	
	public Element toXml() {
		Element el = new Element(type);
		el.addAttribute(new Attribute("idref",arg.getId()));
		return el;
	}
	
}
