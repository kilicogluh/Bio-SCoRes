package gov.nih.nlm.bioscores.core;

import java.util.List;

import gov.nih.nlm.bioscores.candidate.CandidateFilter;
import gov.nih.nlm.bioscores.candidate.PostScoringCandidateFilter;
import gov.nih.nlm.bioscores.exp.ExpressionFilter;

/**
 * This class represents a coreference resolution strategy. Each resolution strategy
 * associates a coreference type (e.g., anaphora) and a mention type (e.g.,
 * definite NPs) with methods that handle various aspects of coreference resolution.
 * These methods include the following: <ul>
 * <li> Mention filtering methods: These extend {@link ExpressionFilter} interface, 
 * and filter out those mentions of the given type that are excluded from coreference 
 * resolution.
 * <li> Candidate filtering methods: These extend {@link CandidateFilter} interface,
 * and eliminate the candidate referents that are incompatible with the mention 
 * on some constraint. 
 * <li> Scoring function: Each scoring function determines how to cumulatively
 * assign a compatibility score between the coreferential mention and a candidate referent.
 * <li> Post-scoring filters: These extend {@link PostScoringCandidateFilter} interface,
 * and indicates methods to use to identify the best referent among a set of 
 * scored candidate referents.
 * </ul>
 * 
 * @author Halil Kilicoglu
 *
 */
public class Strategy {
		
	private CoreferenceType corefType;
	private ExpressionType expType;
	private List<? extends ExpressionFilter> expressionFilteringMethods;
	private List<? extends CandidateFilter> candidateFilteringMethods;
	private List<ScoringFunction> scoringFunction;
	private List<? extends PostScoringCandidateFilter> postScoringFilters;
	
	public Strategy(CoreferenceType corefType, ExpressionType expType, 
			List<ExpressionFilter> expressionFilteringMethods, 
			List<CandidateFilter> candidateFilteringMethods,
			List<ScoringFunction> scoringFunction, 
			List<PostScoringCandidateFilter> postScoringFilters) {
		this.corefType = corefType;
		this.expType = expType;
		this.expressionFilteringMethods = expressionFilteringMethods;
		this.candidateFilteringMethods = candidateFilteringMethods;
		this.scoringFunction = scoringFunction;
		this.postScoringFilters = postScoringFilters;
	}

	public CoreferenceType getCorefType() {
		return corefType;
	}

	public ExpressionType getExpType() {
		return expType;
	}
	
	public List<? extends ExpressionFilter> getExpressionFilteringMethods() {
		return expressionFilteringMethods;
	}

	public List<? extends CandidateFilter> getCandidateFilteringMethods() {
		return candidateFilteringMethods;
	}

	public List<ScoringFunction> getScoringFunction() {
		return scoringFunction;
	}

	public List<? extends PostScoringCandidateFilter> getPostScoringFilters() {
		return postScoringFilters;
	}
}
