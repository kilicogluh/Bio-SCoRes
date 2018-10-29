package gov.nih.nlm.ling.sem;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the abstract definition of a relation. <p>
 * For example, we can define a <i>Regulation</i> relation, with a single core 
 * argument of type <i>Theme</i> and a single optional argument of type <i>Cause</i>. 
 * Information about the arguments (such as, what semantic types
 * can fill these roles) are provided by {@link RelationArgument} objects.
 * 
 * @author Halil Kilicoglu
 *
 */
public class RelationDefinition {
	private String type;
	private String fullName;
	private Class<? extends Relation> relClass;
	private List<RelationArgument> coreArgs;
	private List<RelationArgument> optionalArgs;
	
	/**
	 * Constructs a <code>RelationDefinition</code> object, with a type, relation class,
	 * core and optional argument information.
	 * 
	 * @param type			the short name of the relation
	 * @param fullName		the full name of the relation
	 * @param relClass		the class that implements relations created by this definition
	 * @param coreArgs		the core argument definitions
	 * @param optionalArgs	the optional argument definitions
	 */
	public RelationDefinition(String type, String fullName, Class<? extends Relation> relClass, List<RelationArgument> coreArgs,
			List<RelationArgument> optionalArgs) {
		this.type = type;
		this.fullName = fullName;
		this.relClass = relClass;
		this.coreArgs = coreArgs;
		this.optionalArgs = optionalArgs;
	}

	public String getType() {
		return type;
	}
	
	public String getFullName() {
		return fullName;
	}

	/**
	 * Gets the core arguments that are required by the relation. 
	 * 
	 * @return  a list of <code>RelationArgument</code> objects corresponding to mandatory argument types
	 */
	public List<RelationArgument> getCoreArgs() {
		return coreArgs;
	}
	
	/**
	 * Gets the optional arguments that are not required 
	 * 
	 * @return  a list of <code>RelationArgument</code> objects corresponding to optional argument types
	 */
	public List<RelationArgument> getOptionalArgs() {
		return optionalArgs;
	}
	
	/**
	 * 
	 * @return the class that implements relations defined by this object
	 */
	public Class<? extends Relation> getRelClass() {
		return relClass;
	}

	/**
	 * Finds the semantic types allowed for the given role.
	 * 
	 * @param role  an argument role
	 * @return  	the semantic types that can fill the given role
	 */
	public Set<String> getArgumentSemTypes(String role) {
		Set<String> semTypes = new HashSet<>();
		if (coreArgs != null) {
			for (RelationArgument ea: coreArgs) {
				if (ea.getRole().equals(role)) {
					semTypes.addAll(ea.getAllArgumentSemTypes());
				}
			}
		}
		if (optionalArgs != null) {
			for (RelationArgument ea: optionalArgs) {
				if (ea.getRole().equals(role)) semTypes.addAll(ea.getAllArgumentSemTypes());
			}
		}
		return semTypes;
	}
	
	/**
	 * Finds whether multiple arguments are allowed for the given role
	 * 
	 * @param role  an argument role
	 * @return  true if multiple arguments for this role are allowed
	 */
	public boolean multipleArgumentsAllowed(String role) {
		if (coreArgs != null) {
			for (RelationArgument ea: coreArgs) {
				if (ea.getRole().equals(role)) return ea.isMultiple();
			}
		}
		if (optionalArgs != null) {
			for (RelationArgument ea: optionalArgs) {
				if (ea.getRole().equals(role)) return ea.isMultiple();
			}
		}
		return false;
	}
	
	/**
	 * Checks whether the argument supplied is allowed in the given role.
	 * It checks the class and allowed semantic types that the role-filling argument
	 * is supposed to have.
	 *  
	 * @param role  the role given to the argument
	 * @param arg  	the semantic item argument
	 * 
	 * @return		true if the argument is allowed
	 */
	public boolean argSatisfiesRole(String role, SemanticItem arg) {
		if (coreArgs != null) {
			for (RelationArgument ea: coreArgs) {
				if (ea.getRole().equals(role) && ea.argumentSemTypeSatisfies(arg)) 
					return true;
			}
		}
		if (optionalArgs != null) {
			for (RelationArgument ea: optionalArgs) {
				if (ea.getRole().equals(role) && ea.argumentSemTypeSatisfies(arg)) 
					return true;
			}
		}
		return false;
	}
	
	/**
	 * For a given list of arguments, gets the set of roles which do not
	 * allow multiple arguments by definition, but have multiple arguments. <p>
	 * Used for consistency checking in relation composition.
	 *
	 * @param args	the list of arguments
	 * @return		the set of roles for which the argument list does not 
	 * 				follow the multiple argument constraint
	 */
	public Set<String> argumentViolatesMultipleRoleConstraint(List<Argument> args) {
		List<String> roles = Argument.getArgNames(args);
		Set<String> out = new LinkedHashSet<>();
		for (String r: roles) {
			if (out.contains(r)) continue;
			List<Argument> roleArgs = Argument.getArgs(args,r);
			if (multipleArgumentsAllowed(r) == false && roleArgs.size() > 1) out.add(r);
		}
		return out;
	}
}
