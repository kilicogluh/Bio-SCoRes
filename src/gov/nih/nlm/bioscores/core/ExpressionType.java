package gov.nih.nlm.bioscores.core;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.SemanticItem;

/**
 * Enumeration of coreferential mention types that are handled by the system and their parameters.<p>
 * The system currently addresses 12 types: 
 * <ul>
 * <li> {@link #PersonalPronoun}: Personal pronominal mention (e.g., <i>they, it</i>)
 * <li> {@link #PossessivePronoun}: Possessive pronominal mention (e.g., <i>its, their</i>)
 * <li> {@link #DemonstrativePronoun}: Demonstrative pronominal mention (e.g., <i>that, these</i>)
 * <li> {@link #DistributivePronoun}: Distributive pronominal mention (e.g., <i>each, both</i>)
 * <li> {@link #ReciprocalPronoun}: Reciprocal pronominal mention (e.g., <i>one another, each other</i>)
 * <li> {@link #IndefinitePronoun}: Indefinite pronominal mention (e.g., <i>all, any</i>)
 * <li> {@link #RelativePronoun}: Relative pronominal mention (e.g., <i>which, that</i>)
 * <li> {@link #DefiniteNP}: Definite NP mention (e.g., <i>the disease</i>)
 * <li> {@link #DemonstrativeNP}: Demonstrative NP mention (e.g., <i>this drug</i>)
 * <li> {@link #DistributiveNP}: Distributive NP mention (e.g., <i>both genes</i>)
 * <li> {@link #IndefiniteNP}: Indefinite NP mention (e.g., <i>a problem</i>)
 * <li> {@link #ZeroArticleNP}: Zero-article NP mention (e.g., <i>drug</i>)
 * </ul> 
 *  
 * @author Halil Kilicoglu
 *
 */
public enum ExpressionType {
	PersonalPronoun, PossessivePronoun, DemonstrativePronoun, DistributivePronoun, 
	ReciprocalPronoun, IndefinitePronoun, RelativePronoun, 
	DefiniteNP, IndefiniteNP, ZeroArticleNP, DemonstrativeNP, DistributiveNP;
		
	public static final List<ExpressionType> PRONOMINAL_TYPES = Arrays.asList(PersonalPronoun, PossessivePronoun,
			DemonstrativePronoun, DistributivePronoun, 
			ReciprocalPronoun, RelativePronoun, IndefinitePronoun);
	
	public static final List<ExpressionType> NOMINAL_TYPES = Arrays.asList(DefiniteNP, IndefiniteNP, ZeroArticleNP, 
			DemonstrativeNP, DistributiveNP);
	
	/**
	 * Gets the coreference mention types associated with a textual unit.
	 * 
	 * @param su	the textual unit in question
	 * @return the mention types of a given textual unit, or an empty set if non-coreferential
	 */
	public static LinkedHashSet<ExpressionType> getTypes(SurfaceElement su) {
		LinkedHashSet<SemanticItem> sis = Expression.filterByExpressions(su);
		LinkedHashSet<ExpressionType> types = new LinkedHashSet<>();
		if (sis == null) return types;
		for (SemanticItem si : sis) {
			ExpressionType type = ExpressionType.valueOf(si.getType());
			if (type != null) types.add(type);
		}
		return types;
	}
	
	/**
	 * Determines whether the mention is nominal.
	 * 
	 * @param su	the coreferential mention
	 * @return	true if <var>su</var> is a nominal mention
	 */
	public static boolean nominalExpression(SurfaceElement su) {
		LinkedHashSet<ExpressionType> types = getTypes(su);
		return (types.contains(ExpressionType.DefiniteNP) || types.contains(ExpressionType.IndefiniteNP) || 
				types.contains(ExpressionType.ZeroArticleNP) || types.contains(ExpressionType.DemonstrativeNP) || 
				types.contains(ExpressionType.DistributiveNP));
	}
	
	/**
	 * Determines whether the mention is pronominal.
	 * 
	 * @param su	the coreferential mention
	 * @return	true if <var>su</var> is a pronominal mention
	 */
	public static boolean pronominalExpression(SurfaceElement su) {
		LinkedHashSet<ExpressionType> types = getTypes(su);
		return (types.contains(ExpressionType.PersonalPronoun) || types.contains(ExpressionType.PossessivePronoun) || types.contains(ExpressionType.IndefinitePronoun) || 
				types.contains(ExpressionType.ReciprocalPronoun) || types.contains(ExpressionType.DemonstrativePronoun) || 
				types.contains(ExpressionType.DistributivePronoun) || types.contains(ExpressionType.RelativePronoun));
	}
}
