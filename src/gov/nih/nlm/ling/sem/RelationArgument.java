package gov.nih.nlm.ling.sem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import gov.nih.nlm.ling.util.SemUtils;


/**
 * Represents the abstract constraints for a relation argument (participant). <p>
 * It defines which role can be filled with what kind of semantic items and types and whether 
 * multiple arguments with the same role are allowed. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class RelationArgument {
	
	private String role;
	private Map<Class<? extends SemanticItem>,Set<String>> argumentMap = new HashMap<>();
	private boolean multiple;
	
	/**
	 * Constructs a <code>RelationArgument</code> object with a given role, semantic item class/type.
	 * 
	 * @param role		the role this argument takes for the relation
	 * @param argMap	the map that contains semantic item classes and corresponding semantic types appropriate for this argument
	 * @param multiple	whether multiple arguments with the same role are acceptable
	 */
	public RelationArgument(String role, Map<Class<? extends SemanticItem>,Set<String>> argMap, boolean multiple) {
		this.role = role;
		this.argumentMap = argMap;
		this.multiple = multiple;
	}
	
	/**
	 * Gets the map that contains semantic item class/semantic types allowed for this argument. <p>
	 * An example is <i>Entity.&nbsp;class</i> as a key and a set that contains <i>Protein</i> and <i>Gene</i> 
	 * semantic types as the corresponding value. 
	 * 
	 * @return	the map that contains semantic item class/semantic types allowed for this argument
	 */
	public Map<Class<? extends SemanticItem>, Set<String>> getArgumentMap() {
		return argumentMap;
	}
	
	/**
	 * 
	 * @return the role of this argument, e.g., 'Theme'
	 */
	public String getRole() {
		return role;
	}
	
	/**
	 * 
	 * @return whether multiple arguments of this type is allowed in a relation
	 */
	public boolean isMultiple() {
		return multiple;
	}
	
	/**
	 * Gets all allowed semantic types.
	 * 
	 * @return all semantic types allowed as argument
	 */
	public Set<String> getAllArgumentSemTypes() {
		Set<String> out = new HashSet<>();
		for (Class<? extends SemanticItem> cl : argumentMap.keySet()) {
			out.addAll(argumentMap.get(cl));
		}
		return out;
	}
	
	/**
	 * Gets all semantic types allowed for a given semantic class.
	 * 
	 * @param clazz	the semantic class
	 * @return		all semantic types allowed for <var>clazz</var>
	 */
	public Set<String> getArgumentSemTypes(Class<? extends SemanticItem> clazz) {
		return argumentMap.get(clazz);
	}
	
	/**
	 * Finds out if the semantic item <var>arg</var> satisfies the constraints imposed
	 * by this relation argument.
	 * 
	 * @param arg	a semantic item
	 * @return		true if the argument satisfies the constraints
	 */
	public boolean argumentSemTypeSatisfies(SemanticItem arg) {
		Class<? extends SemanticItem> clazz = arg.getClass();
		Set<Class> corresponding = SemUtils.getGeneralizations(clazz);
		if (argumentMap.containsKey(arg.getClass()) && 
				(getArgumentSemTypes(arg.getClass()).contains(arg.getType()) || getArgumentSemTypes(arg.getClass()).size() == 0))
				return true;
		for (Class c: corresponding) {
			if (argumentMap.containsKey(c) && 
					(getArgumentSemTypes(c).contains(arg.getType()) || getArgumentSemTypes(c).size() == 0)) {
				return true;
			}
		}
		return false;
	}
	
}
