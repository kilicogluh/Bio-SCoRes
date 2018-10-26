package gov.nih.nlm.ling.sem;

import java.util.List;

/**
 * An interface to implement for complex semantic objects (i.e, those 
 * with arguments). Arguments are role-semantic item pairs, and the semantic item
 * of each argument can be other complex semantic objects;
 * in other words, nested relations are allowed.
 * 
 * @author Halil Kilicoglu
 *
 */
public interface Relation extends SemanticItem {
	/**
	 * Returns the arguments of the relation (a role-semantic item pair).
	 * 
	 * @return the list of arguments
	 */
	public List<Argument> getArguments();
	/**
	 * Sets the arguments of the relation.
	 * 
	 * @param args	arguments for the relation
	 */
	public void setArguments(List<Argument> args);
	/**
	 * Adds new arguments to the relation.
	 * 
	 * @param args	arguments to add to the relation	
	 */
	public void addArguments(List<Argument> args);
	/**
	 * Removes an individual argument from the argument list.
	 * 
	 * @param arg	the argument to remove.
	 */
	public void removeArg(Argument arg);
	/**
	 * Removes a set of arguments from the argument list.
	 * 
	 * @param args	the arguments to remove.
	 */
	public void removeArgs(List<Argument> args);
}
