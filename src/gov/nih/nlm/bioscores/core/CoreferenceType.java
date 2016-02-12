package gov.nih.nlm.bioscores.core;

/**
 * Enumeration of coreference relation types that are handled by the system and their parameters.<p>
 * The system currently addresses five types: 
 * <ul>
 * <li> {@link #Anaphora}: Coreferential mention refers back to a previously mentioned item
 * <li> {@link #Cataphora}: Coreferential mention refers to subsequent items
 * <li> {@link #Appositive}: Indicates an attribute relation in an appositive construction
 * <li> {@link #PredicateNominative}: Indicates an attribute relation in a copular construction
 * <li> {@link #Ontological}: Indicates an ontological equivalence relation.
 * </ul>
 * 
 * @author Halil Kilicoglu
 *
 */
public enum CoreferenceType {
	Anaphora, Cataphora, Appositive, PredicateNominative, Ontological;
		
	public static final int SEARCH_BACKWARD = 0;
	public static final int SEARCH_FORWARD = 1;
	public static final int SEARCH_BOTH = 2;
	
	/**
	 * Enumeration of argument roles associated with different types of coreference.
	 * 
	 *
	 */
	public enum Role {
		Anaphor, Cataphor, Antecedent, Consequent, Attribute, Head, Equiv
	}
		
	public int getSearchDirection() {
		switch(this) {
			case Cataphora: return SEARCH_FORWARD;
			case Appositive: return SEARCH_BOTH;
			case PredicateNominative : return SEARCH_BOTH;
			default : return SEARCH_BACKWARD;
		}
	}
	
	public Role getExpRole() {
		switch(this) {
			case Anaphora: return Role.Anaphor;
			case Cataphora: return Role.Cataphor;
			case Ontological: return Role.Equiv;
			default: return Role.Attribute;
		}
	}

	public Role getRefRole() {
		switch(this) {
			case Anaphora: return Role.Antecedent;
			case Cataphora: return Role.Consequent;
			case Ontological: return Role.Equiv;
			default: return Role.Head;
		}
	}
}
