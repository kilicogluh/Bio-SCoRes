package tasks.coref.i2b2;

import java.util.Arrays;
import java.util.List;

/**
 * This class contains some type definitions used by SPL pipelines and utilities.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Constants {
	public static List<String> EXP_TYPES = 
			Arrays.asList("DefiniteNP", "IndefiniteNP", "ZeroArticleNP", "DemonstrativeNP", "DistributiveNP", 
			"PersonalPronoun", "PossessivePronoun","DemonstrativePronoun", "DistributivePronoun", "ReciprocalPronoun", "RelativePronoun", "IndefinitePronoun");
	public static List<String> ENTITY_TYPES = Arrays.asList("treatment","test","person","problem");
	public static List<String> COREFERENCE_TYPES = Arrays.asList("Anaphora","Cataphora","Appositive","PredicateNominative");
}
