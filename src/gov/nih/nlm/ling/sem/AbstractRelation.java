package gov.nih.nlm.ling.sem;

import gov.nih.nlm.ling.composition.EmbeddingCategorization;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.SpanList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The basic implementation of the <code>Relation</code> interface.
 * It does not assume that the relation has a predicate, so this allows
 * implicit relations and coreference chains as subtypes. <p>
 * A <i>resolved</i> relation is one in which all logical argument types are
 * identified.
 * 
 * @see Predication
 * @see Event
 * @see ImplicitRelation
 * 
 * @author Halil Kilicoglu
 *
 */
public abstract class AbstractRelation implements Relation {

	protected String id;
	protected String type;
	protected boolean resolved = false;
	protected List<Argument> arguments;
	protected RelationDefinition definition;
	protected Map<String,Object> features;
	
	protected static final String GENERIC = "GENERIC_RELATION";
	
	/**
	 * the logical argument types (roles).
	 * 
	 */
	public static final List<String> RESOLVED_ARGTYPES = Arrays.asList("OBJ","COMP","SUBJ","ADJUNCT_IN",
			"ADJUNCT_TO","ADJUNCT_AT","ADJUNCT_ON","CONJ","NEG_CONJ");
	
	public AbstractRelation(String id) {
		this.id = id;
	}

	public AbstractRelation(String id, String type) {
		this(id);
		this.type = type;
	}
	
	/**
	 * Constructs a generic <code>AbstractRelation</code> object (i.e., no type).
	 * 
	 * @param id	the relation identifier
	 * @param args	the relation arguments
	 */
	public AbstractRelation(String id, List<Argument> args) {
		this(id);
		this.type = GENERIC;
		this.arguments = args;
	}
	
	public AbstractRelation(String id, String type, List<Argument> args) {
		this(id);
		this.type = type;
		this.arguments = args;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	/**
	 * Determines whether all the presupposed logical arguments for this relation 
	 * have been found.
	 * 
	 * @return	true if the relation is resolved
	 */
	public boolean isResolved() {
		return resolved;
	}

	/**
	 * Sets the <i>resolved</i> status of this relation.
	 * 
	 * @param resolved true/false
	 */
	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}
	
	/**
	 * Gets the span of this relation. <p>
	 * The span of a relation is defined as the union of all its argument spans. 
	 * 
	 * @return the span of this relation
	 */
	public SpanList getSpan() {
		SpanList sp = null;
		for (int i= 0; i < arguments.size(); i++) {
			SpanList argsp = null;
			SemanticItem si = arguments.get(i).getArg();
			if (si instanceof Event) argsp = ((Event)si).getSpan();
			else if (si instanceof Term) argsp = ((Term)si).getSpan();
			if (sp == null)  sp = argsp;
			else sp = SpanList.union(sp, argsp);
		}
		return sp;
	}
	
	@Override
	public Document getDocument() {
		for (int i= 0; i < arguments.size(); i++) {
			SemanticItem si = arguments.get(i).getArg();
			if (si instanceof Term) return ((Term)si).getDocument();
		}
		if (this instanceof HasPredicate) {
			Predicate p = ((HasPredicate)this).getPredicate();
			return p.getDocument();
		}
		return null;
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

	@Override
	public List<Argument> getArguments() {
		return arguments;
	}
	
	@Override
	public void setArguments(List<Argument> arguments) {
		this.arguments = arguments;
	}
	
	@Override
	public void addArguments(List<Argument> arguments) {
		if (this.arguments == null) {
			this.arguments = arguments;
		}
		this.arguments.addAll(arguments);	
	}
	
	@Override
	public void removeArgs(List<Argument> remove) {
		if (remove == null || remove.size() == 0) return;
		if (arguments == null || arguments.size() == 0) return;
		for (Argument a: remove) {
			removeArg(a);
		}
	}
	
	@Override
	public void removeArg(Argument remove) {
		if (remove == null) return;
		if (arguments == null || arguments.size() ==0) return;
		int ind = arguments.indexOf(remove);
		if (ind >= 0) {
			arguments.remove(ind);
		}
	}
	
	public void addArgument(Argument arg) {
		if (arguments == null) {
			arguments = new ArrayList<Argument>();
		}
		arguments.add(arg);	
	}
	
	/**
	 * Finds the <code>SemanticItem</code> objects that are arguments to this relation.
	 * 
	 * @return all semantic objects that are arguments
	 */
	public List<SemanticItem> getArgItems() {
		return Argument.getArgItems(arguments);
	}
	
	/**
	 * Finds the roles of the arguments to this relation.
	 * 
	 * @return all role names.
	 */
	public List<String> getArgNames() {
		return Argument.getArgNames(arguments);
	}
	
	/**
	 * Finds the arguments with a particular role. Allows approximate matches (
	 * e.g., ADJUNCT role will return ADJUNCT_TO, ADJUNCT_IN, etc.)
	 * 
	 * @param name  the role name
	 * @return		arguments whose role begin with the name
	 */
	public List<Argument> getArgs(String name) {
		return Argument.getArgs(arguments, name);
	}
	
	/**
	 * Similar to {@link #getArgs(String)} except this method accepts a list of argument roles.
	 * 
	 * @param names  the array of role names
	 * @return		arguments with the queried roles
	 */
	public List<Argument> getArgs(List<String> names) {
		return Argument.getArgs(arguments, names);
	}
	
	/**
	 * Finds whether the relation has a specific semantic object 
	 * as its argument in <var>role</var> slot.
	 * 
	 * @param role	the role of the participant
	 * @param si	the semantic object to search for
	 * @return  	true if <var>si</var> is in the role <var>role</var>
	 */
	public boolean hasArgWithRole(String role, SemanticItem si) {
		if (arguments == null) return false;
		for (Argument a: arguments) {
			if (a.getArg().equals(si) && a.getType().equals(role))  {				
				return true;
			}
		}
		return false;
	}
		
	/**
	 * 
	 * @return true if no type information is available for this relation
	 */
	public boolean isGeneric() {
		return (GENERIC.equals(type));
	}
	
	/**
	 * 
	 * @param args  a list of arguments
	 * @return  	true if the argument list contains a relation 
	 */
	public static boolean isNested(List<Argument> args) {
		for (Argument a: args) {
			SemanticItem si = a.getArg();
			if (si instanceof Relation) return true;
		}
		return false;		
	}
	
	/**
	 * Opposite of {@link #isNested(List)}.
	 * 
	 * @param args the argument list of the relation.
	 * 
	 * @return true if the relation is atomic based on the identity of its arguments.
	 * 
	 */
	public static boolean isAtomic(List<Argument> args) {
		return !(isNested(args));
	}

	/**
	 * 
	 * @return true if the relation is nested. 
	 */
	public boolean isNested() {
		return isNested(arguments);
	}
	
	/**
	 * Opposite of {@link #isNested()} .
	 * 
	 * @return true if the relation is atomic.
	 */
	public boolean isAtomic() {
		return !(isNested());
	}
	
	/**
	 * Finds the <code>RelationDefinition</code> associated with a given relation <var>type</var>
	 * from a set of <code>RelationDefinition</code>s.
	 * 
	 * @param defs	all relation definitions to consider
	 * @param type  the type to get the definition for
	 * @return  	the relation definition indicated by the input type, or null if no definition is found
	 */
	public static RelationDefinition getDefinition(Set<? extends RelationDefinition> defs, String type) {
		for (RelationDefinition ed: defs) {
			if (ed.getType().equals(type) || ed.getFullName().equals(type)) return ed;
		}
		for (RelationDefinition ed: defs) {
			if (EmbeddingCategorization.getAllDescendants(ed.getType()).contains(type)) return ed;
		}
		return null;
	}
	
	/**
	 * Returns the <code>RelationDefinition</code> associated with this relation.
	 * 
	 * @return the relation definition
	 */
	public RelationDefinition getDefinition() {
		return definition;
	}
	
	/**
	 * Span-based comparison.
	 */
	public int compareTo(SemanticItem so) {
		SpanList sosp = so.getSpan();
		return getSpan().compareTo(sosp);
	}
	
	/**
	 * Checks for equality with the arguments of another relation
	 * 
	 * @param rel	the relation to consider
	 * @return  	true if all the arguments are the same 
	 */
	public boolean argsEquals(AbstractRelation rel) {
		if (arguments == null) return false;
		List<Argument> relargs = rel.getArguments();
		if (relargs == null) return false;
		if (arguments.size() != relargs.size()) return false;
		for (Argument a: arguments) {
			boolean seen = false;
			for (Argument relarg: relargs) {
				if (relarg.equals(a)) { seen = true; break;}
			}
			if (!seen) return false;
		}
		return true;
	}
	
	/**
	 * Not defined for relations at the moment, returns null.
	 * 
	 */
	public Ontology getOntology() {
		return null;
	}
	
	/**
	 * @return false
	 */
	public boolean ontologyEquals(Object o) {
		return false;
	}
	
	@Override
	public int hashCode() {
		return
	    ((id == null ? 59 : id.hashCode()) ^
	     (type  == null ? 89 : type.hashCode()) ^
	     (arguments == null? 109 : arguments.hashCode()));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractRelation other = (AbstractRelation) obj;
		if (id == null) {
			if (other.id != null) return false;
		} else if (!id.equals(other.id)) return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return argsEquals(other);
	}
}
