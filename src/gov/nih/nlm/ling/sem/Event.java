package gov.nih.nlm.ling.sem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.SpanList;
import nu.xom.Attribute;
import nu.xom.Element;


/**
 * Implementation of an event, essentially a predicate-argument structure
 * where arguments (participants) have semantic roles. An event is indicated by
 * a {@link Predicate} object.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Event extends AbstractRelation implements HasPredicate {
	private static Logger log = Logger.getLogger(Event.class.getName());
	
	/**
	 * The set of event definitions to use in the current invoking of the program
	 */
	protected static Set<RelationDefinition> definitions = new HashSet<RelationDefinition>();
	private Predicate predicate;

	/**
	 * Constructs an <code>Event</code> object with a given id and type.
	 * 
	 * @param id	the identifier
	 * @param type	the event type
	 */
	public Event(String id, String type) {
		super(id, type);
		this.definition = getDefinition(definitions,type);
	}
	
	/**
	 * Constructs an <code>Event</code> with its predicate and argument list.
	 * 
	 * @param id		the identifier
	 * @param predicate	the event predicate
	 * @param args		the event arguments
	 * 
	 */
	public Event(String id, Predicate predicate, List<Argument> args) {
		super(id,args);
		this.predicate = predicate;
		if (predicate != null) {
				this.type = predicate.getType();
				this.definition = getDefinition(definitions,type);
		} 
	}

	@Override
	public Predicate getPredicate() {
		return predicate;
	}
		
	@Override
	public RelationDefinition getDefinition() {
		return definition;
	}
	
	/**
	 * Gets all event definitions.
	 * 
	 * @return the set of event definitions
	 */
	public static Set<RelationDefinition> getDefinitions() {
		return definitions;
	}
	
	/**
	 * Sets all event definitions.
	 * 
	 * @param defs  the event definitions
	 */
	public static void setDefinitions(Set<RelationDefinition> defs) {
		definitions = defs;
	}

	@Override
	public LinkedHashSet<String> getAllSemtypes() {
		return predicate.getAllSemtypes();
	}
	
	/**
	 * Updates argument roles when multiple arguments have the same role.
	 * (e.g., Theme -&gt; Theme1, Theme2, etc.)
	 * 
	 */
	public void updateArgNames() {
		Map<String,Integer> cnt= new HashMap<String,Integer>();
		for (Argument a: arguments) {
			String name = a.getType();
			if (cnt.containsKey(name)) {
				a.setType(name + cnt.get(name));
				int c = cnt.get(name) + 1;
				cnt.put(name,c);
			} else {
				cnt.put(name,2);
			}	
		}
	}
	
	public boolean isSpeculated() {
		if (predicate == null) return false;
		return predicate.isSpeculated();
	}
	
	public boolean isNegated() {
		if (predicate == null) return false;
		return predicate.isNegated();
	}

	/**
	 * @return  the union of argument and predicate spans
	 */
	public SpanList getSpan() {
		SpanList fromArgs = super.getSpan();
		return (predicate == null ? fromArgs : SpanList.union(fromArgs, predicate.getSpan()));
	}
		
	/**
	 * Checks whether the event has the argument in the given role
	 * 
	 * @param role  the role to search for
	 * @param si  the argument to search for
	 * @return  true if such an argument exists
	 */
	public boolean hasArgWithRole(String role, SemanticItem si) {
		if (arguments == null) return false;
		for (Argument a: arguments) {
			if (a.getArg().equals(si) && a.getType().equals(role))  {
				log.log(Level.FINEST,"Argument with role {0} is {1}.", new Object[]{role,a.getArg().toShortString()});
				return true;
			}
		}
		return false;
	}	
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(id + "_" + resolved + "_" + type + "_" + 
				  (predicate == null ? "NOPRED" : predicate.getId()) + "_" + predicate.getText());
		if (arguments == null) {
			log.warning("No arguments for semantic relation.");
			buf.append("_NOARG");
		}
		for (Argument a: arguments) {
			buf.append("_" + a.getType() + "(" + a.getArg().toString() + ")");
		}
		return buf.toString();
	}
	
	public String toShortString() {
		StringBuffer buf = new StringBuffer();
		buf.append((predicate == null ? "NO_PRED" : predicate.getText()) + "(" + 
				   id + "_" +  (predicate == null ? "" : predicate.getId()) + 
				   "_" +  type + "_");
		if (arguments == null) {
			log.warning("No arguments for semantic relation.");
			buf.append("_NOARG");
		}
		for (Argument a: arguments) {
			buf.append("_" + a.getType() + "(" + a.getArg().toShortString() + ")");
		}	
		buf.append(")");
		return buf.toString();
	}
		
	/**
	 * 
	 * @return  the count of all resolved logical arguments
	 */
	public int resolvedArgCount() {
		if (predicate == null) return 0;
		if (arguments == null) return 0;
		int c = 0;
		for (Argument a: arguments) {
			if (RESOLVED_ARGTYPES.contains(a.getType())) c++;
		}		
		return c;
	}
	
	/**
	 * 
	 * @return true if all the core arguments have been resolved
	 */
	public boolean coreArgsResolved() {
		if (predicate == null) return false;
		if (arguments == null || arguments.size() == 0) return false;
		List<RelationArgument> coreArgs = definition.getCoreArgs();
		if (coreArgs == null) return true;
		List<String> argNames = getArgNames();
		for (RelationArgument pa: coreArgs) {
			log.log(Level.FINER,"Checking for role {0}.", new Object[]{pa.getRole()});
			if (argNames.contains(pa.getRole()) == false) return false;
			log.log(Level.FINER,"Core arg with type {0} is resolved.", new Object[]{pa.getRole()});
		}
		return true;
	}

	public Element toXml() {
		Element el = new Element("Event");
		el.addAttribute(new Attribute("id",id));
		if (type != null) el.addAttribute(new Attribute("type",type));
		if (predicate != null) el.addAttribute(new Attribute("predicate",predicate.getId()));
		if (features.size() > 0) {
			for (String s: features.keySet()) {
				el.addAttribute(new Attribute(s,features.get(s).toString()));
			}
		}
		for (int i= 0; i < arguments.size(); i++) {
			Argument arg = arguments.get(i);
			el.appendChild(arg.toXml());
		}
		return el;
	}
	
	/**
	 * Gets the standoff annotation format text for the event.<p>
	 * The format for an event is the following (tab after [ID])
	 * [ID]	[TYPE]:[PREDICATE_ID] [ARG1_ROLE]:[ARG1_ID] [ARG2_ROLE]:[ARG2:ID] ...
	 * 
	 */
	public String toStandoffAnnotation() {
		StringBuffer buf = new StringBuffer();
		if (arguments == null) {
			log.warning("Malformed event.");
			return buf.toString();
		}
		buf.append(id + "\t");
		if (type != null)  { 
			buf.append(type);
			if (predicate != null) buf.append(":" + predicate.getId() + " ");
		}
		for (int i= 0; i < arguments.size(); i++) {
			buf.append(arguments.get(i).getType() + ":" + arguments.get(i).getArg().getId() + " ");
		}
		return buf.toString().trim();
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
		Event other = (Event) obj;
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
