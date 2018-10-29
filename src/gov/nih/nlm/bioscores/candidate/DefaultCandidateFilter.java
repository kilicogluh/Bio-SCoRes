package gov.nih.nlm.bioscores.candidate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.Conjunction;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Relation;

/**
 * The default candidate filtering method. It consists of applying the following
 * filtering methods sequentially: <ul>
 * <li> {@link SemanticClassFilter} with {@link Entity}, {@link Expression}, {@link Relation}, 
 * and {@link Conjunction} semantic classes.
 * <li> {@link NounPhraseFilter}
 * <li> {@link VerbPhraseFilter}
 * </ul>
 * 
 * The candidates that pass the {@code SemanticClassFilter} and {@code NounPhraseFilter} filters
 * are kept, those that pass {@code VerbPhraseFilter} are eliminated.
 * 
 * This filter is not appropriate for event anaphora, since they can be indicated by verbal phrases.
 * 
 * @author Halil Kilicoglu
 *
 */
public class DefaultCandidateFilter implements CandidateFilter {

	public static final Collection<Class> DEFAULT_SEMANTIC_CLASSES = Arrays.asList((Class)Entity.class,(Class)Expression.class,
			(Class)Relation.class,(Class)Conjunction.class);
	
	@Override
	public void filter(SurfaceElement exp, CoreferenceType type, ExpressionType expType,
			List<SurfaceElement> candidates,
			List<SurfaceElement> filteredCandidates) {
		if (candidates == null) return;
		SemanticClassFilter scf = new SemanticClassFilter(DEFAULT_SEMANTIC_CLASSES);
		NounPhraseFilter npf = new NounPhraseFilter();
		VerbPhraseFilter vpf = new VerbPhraseFilter();
		Set<SurfaceElement> allFiltered = new LinkedHashSet<>();
		List<SurfaceElement> scfCandidates = new ArrayList<>();
		List<SurfaceElement> npfCandidates = new ArrayList<>();
		List<SurfaceElement> vpfCandidates = new ArrayList<>();
		scf.filter(exp, type, expType, candidates, scfCandidates);
		npf.filter(exp, type, expType, candidates, npfCandidates);
		vpf.filter(exp, type, expType, candidates, vpfCandidates);
		allFiltered.addAll(scfCandidates);
		allFiltered.addAll(npfCandidates);
		allFiltered.removeAll(vpfCandidates);
		filteredCandidates.addAll(allFiltered);
	}

}
