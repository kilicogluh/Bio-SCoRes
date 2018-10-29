package gov.nih.nlm.bioscores.core;

import java.util.ArrayList;
import java.util.List;

import gov.nih.nlm.bioscores.agreement.AdjacencyAgreement;
import gov.nih.nlm.bioscores.agreement.DiscourseConnectiveAgreement;
import gov.nih.nlm.bioscores.agreement.GenderAgreement;
import gov.nih.nlm.bioscores.agreement.HypernymListAgreement;
import gov.nih.nlm.bioscores.agreement.NumberAgreement;
import gov.nih.nlm.bioscores.agreement.AnimacyAgreement;
import gov.nih.nlm.bioscores.agreement.PersonAgreement;
import gov.nih.nlm.bioscores.agreement.SemanticCoercionAgreement;
import gov.nih.nlm.bioscores.candidate.CandidateFilter;
import gov.nih.nlm.bioscores.candidate.CandidateSalience;
import gov.nih.nlm.bioscores.candidate.DefaultCandidateFilter;
import gov.nih.nlm.bioscores.candidate.HasSemanticsFilter;
import gov.nih.nlm.bioscores.candidate.PostScoringCandidateFilter;
import gov.nih.nlm.bioscores.candidate.PostScoringCandidateFilterImpl;
import gov.nih.nlm.bioscores.candidate.PriorDiscourseFilter;
import gov.nih.nlm.bioscores.candidate.SubsequentDiscourseFilter;
import gov.nih.nlm.bioscores.candidate.SyntaxBasedCandidateFilter;
import gov.nih.nlm.bioscores.candidate.WindowSizeFilter;
import gov.nih.nlm.bioscores.exp.ExpressionFilter;
import gov.nih.nlm.bioscores.exp.ExpressionFilterImpl;

/**
 * Class that provides a configuration for coreference resolution. 
 * A configuration consists of a list of {@link Strategy} objects, 
 * each of which indicates a specific resolution strategy to use for a 
 * coreference mention/relation type pair. A default configuration 
 * can also be used.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Configuration {

	/**
	 * Entire document as the resolution window
	 */
	public static final int RESOLUTION_WINDOW_ALL = -2;
	/**
	 * Entire section as the resolution window 
	 */
	public static final int RESOLUTION_WINDOW_SECTION = -1;
	/**
	 * Current sentence as the resolution window
	 */
	public static final int RESOLUTION_WINDOW_SENTENCE = 0;
	
	private static List<Strategy> strategies = null;
	private static Configuration config = null;
		
	private Configuration(List<Strategy> str) {
		strategies = str;
	}
	
	/**
	 * Gets the existing configuration instance, or creates a default instance if no configuration exists.
	 * 
	 * @return	the <code>Configuration</code> singleton
	 */
	public static Configuration getInstance() {
		if (config == null) {
			strategies = loadDefaultStrategies();
			config = new Configuration(strategies);
		}
		return config;
	}
	
	/**
	 * Gets the existing configuration instance, or creates an instance with the provided list of strategies if no configuration exists.
	 * 
	 * @param strategies	the list of strategies to use
	 * @return	the <code>Configuration</code> singleton
	 */
	public static Configuration getInstance(List<Strategy> strategies) {
		if (strategies == null) {
			return getInstance();
		}
		config = new Configuration(strategies);
		return config;
	}
	
	/**
	 * Loads the default resolution strategies. <p>
	 * The default strategy handles Anaphora and Cataphora instances. 
	 * 
	 * @return 	the list of default strategies.
	 */
	public static List<Strategy> loadDefaultStrategies() {
		List<Strategy> defs = new ArrayList<>();
		ExpressionFilterImpl expressionFilterImpl = new ExpressionFilterImpl();
		
		// ANAPHORA
		// mention filters
		List<ExpressionFilter> anaphoricExpFilters = new ArrayList<>();
		anaphoricExpFilters.add(expressionFilterImpl.new AnaphoricityFilter());
		
		List<ExpressionFilter> possPrExpFilters = new ArrayList<>();
		possPrExpFilters.add(expressionFilterImpl.new ThirdPersonPronounFilter());
		
		List<ExpressionFilter> personalPrExpFilters = new ArrayList<>(possPrExpFilters);
		personalPrExpFilters.add(expressionFilterImpl.new PleonasticItFilter());
		
		List<ExpressionFilter> relExpFilters = new ArrayList<ExpressionFilter>();
		relExpFilters.add(expressionFilterImpl.new NonCorefRelativePronFilter());
		
		// candidate filters
		List<CandidateFilter> anaphoraCandFilters = new ArrayList<>();
		anaphoraCandFilters.add(new PriorDiscourseFilter());
		anaphoraCandFilters.add(new WindowSizeFilter(2));
		anaphoraCandFilters.add(new SyntaxBasedCandidateFilter());
		anaphoraCandFilters.add(new DefaultCandidateFilter());
		
		// possessive candidate filters
		List<CandidateFilter> possCandFilters = new ArrayList<>();
		possCandFilters.add(new PriorDiscourseFilter());
		possCandFilters.add(new WindowSizeFilter(2));
		possCandFilters.add(new DefaultCandidateFilter());
		
		// relative pronoun candidate filters
		List<CandidateFilter> relCandFilters = new ArrayList<>();
		relCandFilters.add(new PriorDiscourseFilter());
		relCandFilters.add(new WindowSizeFilter(Configuration.RESOLUTION_WINDOW_SENTENCE));
		relCandFilters.add(new DefaultCandidateFilter());
		
		// post-scoring filters
		PostScoringCandidateFilterImpl postScoringFilterImpl = new PostScoringCandidateFilterImpl();
		List<PostScoringCandidateFilter> postScoringFilters = new ArrayList<>();
		postScoringFilters.add(postScoringFilterImpl.new ThresholdFilter(4));
		postScoringFilters.add(postScoringFilterImpl.new TopScoreFilter());
		List<PostScoringCandidateFilter> graphBasedFilter = new ArrayList<>(postScoringFilters);
		graphBasedFilter.add(postScoringFilterImpl.new SalienceTypeFilter(CandidateSalience.Type.ParseTree));
		List<PostScoringCandidateFilter> closenessFilter = new ArrayList<>(postScoringFilters);
		closenessFilter.add(postScoringFilterImpl.new SalienceTypeFilter(CandidateSalience.Type.Proximity));
		
		// relative pronoun post-scoring
		List<PostScoringCandidateFilter> relPostScoringFilter = new ArrayList<>();
		relPostScoringFilter.add(postScoringFilterImpl.new ThresholdFilter(1));
		relPostScoringFilter.add(postScoringFilterImpl.new SalienceTypeFilter(CandidateSalience.Type.Proximity));
		
		// scoring functions
		List<ScoringFunction> pronounScoringFunction = new ArrayList<>();
		pronounScoringFunction.add(new ScoringFunction(AnimacyAgreement.class,1,0));
		pronounScoringFunction.add(new ScoringFunction(GenderAgreement.class,1,0));
		pronounScoringFunction.add(new ScoringFunction(NumberAgreement.class,1,0));
		pronounScoringFunction.add(new ScoringFunction(PersonAgreement.class,1,0));

		List<ScoringFunction> possPronounScoringFunction = new ArrayList<>(pronounScoringFunction);
		possPronounScoringFunction.add(new ScoringFunction(SemanticCoercionAgreement.class,1,0));
		
		List<ScoringFunction> relPronounScoringFunction = new ArrayList<>();
		relPronounScoringFunction.add(new ScoringFunction(AdjacencyAgreement.class,1,0));

		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.PersonalPronoun,
				personalPrExpFilters,anaphoraCandFilters,pronounScoringFunction,graphBasedFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.PossessivePronoun,
				possPrExpFilters,possCandFilters,possPronounScoringFunction,graphBasedFilter));
//		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DemonstrativePronoun,
//				null,anaphoraFilters,pronounScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DistributivePronoun,
				null,anaphoraCandFilters,pronounScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.ReciprocalPronoun,
				null,anaphoraCandFilters,pronounScoringFunction,closenessFilter));
//		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.IndefinitePronoun,
//				null,anaphoraFilters,pronounScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.RelativePronoun,
				relExpFilters,relCandFilters,relPronounScoringFunction,relPostScoringFilter));
		
		// nominal expressions
		List<ScoringFunction> nominalScoringFunction = new ArrayList<>();
		nominalScoringFunction.add(new ScoringFunction(NumberAgreement.class,1,1));
		nominalScoringFunction.add(new ScoringFunction(HypernymListAgreement.class,3,0));
		
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DefiniteNP,
				anaphoricExpFilters,anaphoraCandFilters,nominalScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DemonstrativeNP,
				anaphoricExpFilters,anaphoraCandFilters,nominalScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DistributiveNP,
				anaphoricExpFilters,anaphoraCandFilters,nominalScoringFunction,closenessFilter));
//		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.IndefiniteNP,
//				anaphoricExpFilters,anaphoraFilters,nominalScoringFunction,closenessFilter));
//		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.ZeroArticleNP,2,
//				anaphoricExpFilters,anaphoraFilters,nominalScoringFunction,closenessFilter));
		
		// CATAPHORA
		List<ExpressionFilter> cataphoricExpFilters = new ArrayList<>();
		cataphoricExpFilters.add(expressionFilterImpl.new CataphoricityFilter());
		
		// cataphora candidate filtering
		List<CandidateFilter> cataphoraCandFilters = new ArrayList<>();
		cataphoraCandFilters.add(new SubsequentDiscourseFilter());
		cataphoraCandFilters.add(new WindowSizeFilter(2));
		cataphoraCandFilters.add(new SyntaxBasedCandidateFilter());
		cataphoraCandFilters.add(new DefaultCandidateFilter());
		cataphoraCandFilters.add(new HasSemanticsFilter());
		
		// caataphora candidate filtering for pronouns
		List<CandidateFilter> cataphoraPrCandFilters = new ArrayList<>();
		cataphoraPrCandFilters.add(new SubsequentDiscourseFilter());
		cataphoraPrCandFilters.add(new WindowSizeFilter(RESOLUTION_WINDOW_SENTENCE));
		cataphoraPrCandFilters.add(new SyntaxBasedCandidateFilter());
		cataphoraPrCandFilters.add(new DefaultCandidateFilter());
		cataphoraPrCandFilters.add(new HasSemanticsFilter());
		
		// cataphora scoring augmented for pronouns
		List<ScoringFunction> catPronounScoringFunction = new ArrayList<>(pronounScoringFunction);
		catPronounScoringFunction.add(new ScoringFunction(DiscourseConnectiveAgreement.class,1,2));

		defs.add(new Strategy(CoreferenceType.Cataphora,ExpressionType.PersonalPronoun,
				cataphoricExpFilters,cataphoraPrCandFilters,catPronounScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Cataphora,ExpressionType.PossessivePronoun,
				cataphoricExpFilters,cataphoraPrCandFilters,catPronounScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Cataphora,ExpressionType.DefiniteNP,
				cataphoricExpFilters,cataphoraCandFilters,nominalScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Cataphora,ExpressionType.DemonstrativeNP,
				cataphoricExpFilters,cataphoraCandFilters,nominalScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Cataphora,ExpressionType.DistributiveNP,
				cataphoricExpFilters,cataphoraCandFilters,nominalScoringFunction,closenessFilter));
		return defs;
	}
	
	/**
	 * Checks whether the coreference resolution system is configured to handle a specific coreference type.
	 * 
	 * @param type	the coreference type in question
	 * @return		true if the system can handle the coreference type <var>type</var>
	 */
	public boolean hasCorefType(CoreferenceType type) {
		for (Strategy str: strategies){
			if (str.getCorefType() == type) return true;
		}
		return false;
	} 
	
	/**
	 * Checks whether the coreference resolution system is configured to handle a specific mention type.
	 * 
	 * @param type	the mention type in question
	 * @return		true if the system can handle the mention type <var>type</var>
	 */
	public boolean hasExpType(ExpressionType type) {
		for (Strategy str: strategies){
			if (str.getExpType() == type) return true;
		}
		return false;
	} 
	
	/**
	 * Finds the appropriate strategy to apply for a given mention and coreference type.
	 * 
	 * @param corefType	the coreference type 
	 * @param expType	the mention type
	 * @return	the <code>Strategy</code> object that is applicable for the given input parameters, or null if no such strategy exists
	 */
	public Strategy getStrategy(CoreferenceType corefType, ExpressionType expType) {
		for (Strategy str: strategies){
			if (str.getCorefType() == corefType && str.getExpType() == expType) return str;
		}
		return null;
	}
	
}
