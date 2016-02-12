package gov.nih.nlm.bioscores.core;

import gov.nih.nlm.bioscores.agreement.Agreement;

/**
 * Each coreference resolution strategy ({@link Strategy}) is associated with a scoring function
 * that determines the salience of a specific type of agreement between the coreferential 
 * mentions on the overall compatibility between these mentions.
 * The compatibility between coreferential expressions is then calculated as the sum 
 * of the these salience scores. <p>
 * The notion of salience score is based on the work of Castano and Pustejovsky (2002). 
 * 
 * Each function specifies a positive score and a penalty for a given agreement type.
 * Agreement type is specified with a class implementing the {@link Agreement}
 * interface.
 *
 * 
 * @author Halil Kilicoglu
 *
 */
public class ScoringFunction {

	private Class<? extends Agreement> implementingClass;
	private int score;
	private int penalty;
	
	/**
	 * Creates a <code>ScoringFunction</code> object.
	 * 
	 * @param implementingClass	the class implementing the agreement type
	 * @param score  			the positive score in case of agreement
	 * @param penalty  			the penalty in case of disagreement
	 */
	public ScoringFunction(Class<? extends Agreement> implementingClass, int score, int penalty) {
		this.implementingClass = implementingClass;
		this.score = score;
		this.penalty = penalty;
	}
	
	public Class<? extends Agreement> getImplementingClass() {
		return implementingClass;
	}

	public int getScore() {
		return score;
	}

	public int getPenalty() {
		return penalty;
	}
}
