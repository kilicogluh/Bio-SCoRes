package gov.nih.nlm.ling.sem;

/**
 * An interface to distinguish <code>Relation</code> objects indicated with explicit predicates
 * from those that are implicitly indicated.
 * 
 * @author Halil Kilicoglu
 *
 */
public interface HasPredicate {
	
	/**
	 * 
	 * @return the predicate of the relation
	 */
	public Predicate getPredicate();
}
