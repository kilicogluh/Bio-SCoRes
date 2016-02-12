package gov.nih.nlm.ling.transform;

import gov.nih.nlm.ling.core.Sentence;

import java.util.List;

/**
 * Interface to implement to define a new dependency transformation.<p>
 * Each dependency transformation takes in a <code>Sentence</code> object and 
 * transforms the embedding relations it has using a transformation rule.
 * After each transformation rule is applied, the embedding relations 
 * associated with the sentence is expected to reflect the semantic dependencies
 * of the sentence elements more precisely than before.<p>
 * Dependency transformations currently serve several purposes:<ul>
 * <li>Semantically enrich the sentence.
 * <li>Reflect the semantic dependencies to allow semantic composition.
 * <li>Correct dependency parsing errors. 
 * </ul>
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO In general, all transformation should be adapted to use universal dependencies as an option.
public interface DependencyTransformation {
	/**
	 * Gets the dependency transformation rules that should have been applied before
	 * applying the current one. <p>
	 * This is because the current rule assumes a dependency configuration generated
	 * by the prerequisite rule.
	 * 
	 * @return the prerequisite dependency transformation rules, or empty list if no prerequisites
	 */
	public List<Class<? extends DependencyTransformation>> getPrerequisites();
	
	/**
	 * Transforms a <code>Sentence</code> object using the transformation rule.
	 * 
	 * @param sent	the sentence to transform
	 */
	public void transform(Sentence sent);
}
