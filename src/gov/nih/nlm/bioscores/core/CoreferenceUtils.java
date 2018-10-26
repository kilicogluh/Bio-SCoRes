package gov.nih.nlm.bioscores.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;

/**
 * Static utility methods for coreference resolution operations.
 * 
 * @author Halil Kilicoglu
 *
 */
public class CoreferenceUtils {
	private static Logger log = Logger.getLogger(CoreferenceUtils.class.getName());

	/**
	 * Determines whether a textual unit is associated with semantics other than
	 * coreference objects (i.e., instances of {@link Expression} class).
	 * 
	 * @param surf	the textual unit
	 * @return		true if the textual unit is associated with non-coreferential semantics
	 */
	public static boolean hasNonCoreferentialSemantics(SurfaceElement surf) {
		return (getNonCoreferentialSemantics(surf).size() > 0);
	}
	
	/**
	 * Gets semantic content associated with the textual unit other than coreferential mentions.
	 * 
	 * @param surf	the textual unit
	 * @return a set of non-coreferential semantic objects, or empty set if no such object
	 */
	public static LinkedHashSet<SemanticItem> getNonCoreferentialSemantics(SurfaceElement surf) {
		LinkedHashSet<SemanticItem> nonCoref = new LinkedHashSet<>();
		if (surf.hasSemantics() == false) return nonCoref;
		LinkedHashSet<SemanticItem> exps = Expression.filterByExpressions(surf);
		nonCoref = new LinkedHashSet<>(surf.getSemantics());
		nonCoref.removeAll(exps);
		return nonCoref;
	}
	
	/**
	 * Gets semantic content associated with the head of a textual unit, other than coreferential mentions.
	 * 
	 * @param surf	the textual unit
	 * @return a set of non-coreferential semantic objects associated with the head, or empty set if no such object
	 */
	public static LinkedHashSet<SemanticItem> getNonCoreferentialHeadSemantics(SurfaceElement surf) {
		LinkedHashSet<SemanticItem> headSem = surf.getHeadSemantics();
		LinkedHashSet<SemanticItem> nonCoref = getNonCoreferentialSemantics(surf);
		headSem.retainAll(nonCoref);
		return headSem;
	}
	
	/**
	 * Gets the pronoun token (in lowercase) in a textual unit.
	 * 
	 * @param surf	the textual unit
	 * @return the pronominal token in the textual unit, or null if there are no pronouns
	 */
	public static String getPronounToken(SurfaceElement surf) {
		if (surf == null) return null;
		if (surf instanceof Word ) {
			if (surf.isPronominal())
				return surf.getText().toLowerCase();
			return null;
		}
		List<Word> words = surf.toWordList();
		for (Word w: words) {
			if (w.isPronominal()) return w.getText().toLowerCase();
		}
		return null;
	}
	
	/**
	 * Gets the determiner token (in lowercase) in a textual unit.
	 * 
	 * @param surf	the textual unit
	 * @return the determiner token in the textual unit, or null if there are no determiners
	 */
	public static String getDeterminerToken(SurfaceElement surf) {
		if (surf == null) return null;
		if (surf instanceof Word ) {
			if (surf.isDeterminer())
				return surf.getText().toLowerCase();
			return null;
		}
		List<Word> words = surf.toWordList();
		for (Word w: words) {
			if (w.isDeterminer()) return w.getText().toLowerCase();
		}
		return null;
	}
	
	/**
	 * Checks whether a noun phrase has a named entity in modifier position. <p>
	 * This method helps to filter out noun phrases such as "the TRADD protein", where TRADD is annotated as a term,
	 * as coreferential mentions. These are also called <i>rigid designators</i>.
	 * If the parameter <var>checkExps</var> is set to true, it will ensure that
	 * the semantic object in modifier position is not a coreferential mention.
	 * 
	 * @param type	the coreferential mention type of the noun phrase
	 * @param surf  the noun phrase in question
	 * @param checkExps	whether to check for coreferential mentions in modifier position
	 * 
	 * @return true if the textual unit has a term in modifier position
	 */	
	// TODO Needs more work.
	public static boolean isRigidDesignator(ExpressionType type, SurfaceElement surf, boolean checkExps) {
		if (surf == null) return false;
		Sentence sent = surf.getSentence();
		if (sent == null) return false;
		Document doc = sent.getDocument();
//		if (!(su.isNominal())) return false;
		if ( ExpressionType.NOMINAL_TYPES.contains(type) == false || 
				ExpressionType.nominalExpression(surf) == false) return false; 
		List<SynDependency> embeddings = surf.getSentence().getEmbeddings();
		if (embeddings == null || embeddings.size() == 0) return false;	
		List<SynDependency> deps = SynDependency.outDependenciesWithTypes(surf, embeddings, 
				SynDependency.NP_INTERNAL_DEPENDENCIES, true);
		LinkedHashSet<SemanticItem> terms = surf.filterByTerms();
		
		// already chunked
		if (deps.size() == 0) {
			if (checkExps) {
				SpanList hsp = surf.getHead().getSpan();
				for (SemanticItem term: terms) {
					if (SpanList.atLeft(term.getSpan(), hsp) && term instanceof Expression) continue;
					LinkedHashSet<SemanticItem> spTerms = Document.getSemanticItemsByClassSpan(doc, Expression.class, term.getSpan(), true);
					if (spTerms.size() == 0) return true;
				}
				for (Word w: surf.toWordList()) {
					if (SpanList.atLeft(w.getSpan(), hsp) && w.isNominal()) {
						for (SemanticItem term: terms) {
							if (SpanList.subsume(term.getSpan(), w.getSpan())) continue;
						}
						return true;
					}
				}
			} else {
				LinkedHashSet<SemanticItem> headSem = surf.getHeadSemantics();
				for (SemanticItem term: terms) {
					for (SemanticItem hs: headSem) {
						if (term.equals(hs)) continue;
						if (SpanList.atLeft(term.getSpan(),hs.getSpan())) {
							List<Word> tws = sent.getWordsInSpan(term.getSpan());
							for (Word tw: tws) {
								if (tw.isAdjectival() || tw.isNominal()) return true;
							}
						}
					}
				}
			}
		} else {
			for (SynDependency sd : deps) {
				SurfaceElement opposite = sd.getDependent();
				if (opposite.filterByEntities().size() > 0) return true;
			}
		}
		return  false;
	}
	
	/**
	 * Checks whether a textual unit is modified. <p>
	 * A textual unit is modified (and therefore not eligible for being a coreferential mention) if
	 * it is a rigid designator, or modified by relative clauses or prepositional phrases introduced 
	 * by <i>of</i>.
	 * 
	 * @param type	the coreferential mention type of the textual unit in consideration
	 * @param su  	the textual unit in question
	 * @param checkExps	whether to check for coreferential mentions in rigid designator check.
	 * 
	 * @return true if the textual unit has a term in modifier position
	 */	
	// TODO This is by no means perfect.
	public static boolean isModified(ExpressionType type, SurfaceElement su, boolean checkExps) {
		if (ExpressionType.PRONOMINAL_TYPES.contains(type)) return false;
		if (isRigidDesignator(type, su, checkExps)) return true;
		List<SynDependency> embeddings = su.getSentence().getEmbeddings();
		if (embeddings == null || embeddings.size() == 0) return false;
//		if (SynDependency.dependenciesWithTypes(su, embeddings, SynDependency.APPOS_DEPENDENCIES,true).size() > 0) return true;
		List<SynDependency> outDeps = SynDependency.outDependencies(su,embeddings);
		if (SynDependency.dependenciesWithTypes(outDeps,Arrays.asList("rcmod","vmod"),true).size() > 0) return true;
		return (SynDependency.dependenciesWithType(outDeps, "prep_of", true).size() >0);
	}
	
	/**
	 * Gets the appropriate mention associated with a textual unit, given the mention type.
	 * 
	 * @param expType	the coreferential mention type
	 * @param surf		the textual unit
	 * @return	the coreferential mention object with the given type, or null if no such mention exists
	 */
	public static SemanticItem getCompatibleExpression(ExpressionType expType, SurfaceElement surf) {
		if (surf.hasSemantics() == false) return null;
		for (SemanticItem sem : surf.getSemantics()) {
			try {
				if (sem instanceof Expression && expType == ExpressionType.valueOf(sem.getType())) return sem;
			} catch (IllegalArgumentException iae) {}
		}
		return null;
	}
		
	/**
	 * Checks whether the textual unit is a pleonastic-<i>it</i> (e.g., <i>It is possible that</i>). <p>
	 * The rules are adapted from Wang, Melton, and Pakhomov (2011). <ul>
	 * <li> It be Adj [for NP] to VP	(<i>It is possible for X to be .....</i>)
	 * <li> It be [Adj|Adv| verb]* that	(<i>It has been shown that ....</i>)
	 * <li> It VB [that]*				(<i>It seems that ...</i>)
	 * <li> NP [makes|finds|take] it [Adj]* [for NP]* [to VP|Ving]	(<i>The patient's angina makes it possible to ...</i>)
	 * <li> It [MD*|RB*|VB*|VP*] be NP + Clause 	(<i>But it has certainly been a pleasure to be involved...</i>)
	 * </ul>
	 * 
	 * @param surf	the textual unit in question
	 * @param deps  the syntactic dependencies for the sentence of the textual unit
	 * 
	 * @return true if the textual unit is a pleonastic-<i>it</i>
	 */
	public static boolean pleonasticIt(SurfaceElement surf, List<SynDependency> deps) {
		if (surf.getText().equalsIgnoreCase("it") == false) return false;
//		String[] passive = new String[]{"ccomp","infmod","vmod"};
//		String[] nonPassive = new String[]{"ccomp","xcomp","infmod","vmod"};
		List<SynDependency> inDeps = 
				SynDependency.inDependenciesWithTypes(surf, deps, Arrays.asList("nsubj","nsubjpass"), true);
		for (SynDependency dep: inDeps) {
			SurfaceElement se = dep.getGovernor();
//			String[] types = nonPassive;
//			if (dep.getType().equals("nsubjpass")) types = passive;
			Collection<SynDependency> outDeps = 
					SynDependency.outDependenciesWithTypes(se, deps, Arrays.asList("ccomp","xcomp","infmod","vmod"), true);
			if (outDeps.size() > 0) 
				return true;
		}
		return false;
	}
	
	/**
	 * Determines whether a textual unit with the token <i>that</i> is a complementizer.
	 * 
	 * @param su	the textual unit
	 * @return	true if the textual unit contains a complementizer <i>that</i>.
	 */
	public static boolean complementizerThat(SurfaceElement su) {
		if (su.containsLemma("that") == false) return false;
		String lex = su.toWordList().get(0).getLemma();
		if (lex.equals("that") == false) return false;
		String pos = su.toWordList().get(0).getPos();
		return (pos.equalsIgnoreCase("WDT") || 
						SynDependency.inDependenciesWithTypes(su,su.getSentence().getEmbeddings(),Arrays.asList("complm","mark"),true).size() > 0);
	}
	
	/**
	 * Determines whether a coreferential mention and a given semantic object are compatible
	 * with respect to the direction of the relation and the coreference type under consideration.<p>
	 * For example, if the mention precedes the semantic object in text and the coreference type under
	 * consideration is anaphora, they have incompatible direction.
	 * 
	 * @param exp	the coreferential mention
	 * @param sem	the semantic object
	 * @param type	the coreference type
	 * @return	true if <var>exp</var> and <var>sem</var> have incompatible direction with respect to <var>type</var>
	 */
	public static boolean incompatibleDirection(SurfaceElement exp, SemanticItem sem, CoreferenceType type) {
		if (type.getSearchDirection() == CoreferenceType.SEARCH_BOTH) return false; 
		return ((SpanList.atLeft(exp.getSpan(), sem.getSpan()) && type.getSearchDirection() == CoreferenceType.SEARCH_BACKWARD) ||
		    (SpanList.atLeft(sem.getSpan(), exp.getSpan()) && type.getSearchDirection() == CoreferenceType.SEARCH_FORWARD));
	}
	
	/**
	 * Finds a semantic object that has the same ontological mapping as another one, but is closer to 
	 * a given textual unit. This can be useful when a referent is found but proximity to the mention is 
	 * important, as is in some corpora.
	 * 
	 * @param doc	the document being analyzed
	 * @param su	the textual unit, presumably a coreference mention
	 * @param si	a semantic object, presumably indicating a referent
	 * @return	a semantic object with the same ontological mapping as <var>si</var>, but closer to <var>su</var>, 
	 * 			null if no such object exists
	 */
	// TODO This is not really coreference-specific, but was needed only in that context.
	public static SemanticItem closerWithSameOntology(Document doc, SurfaceElement su, SemanticItem si) {
		if (si instanceof Relation) {
			log.log(Level.WARNING, "Relation instance found where Term was expected: {0}.", new Object[]{si.toShortString()});
			return null;
		}
		Term t = (Term)si;
		SpanList sisp = si.getSpan();
		List<Span> sps = sisp.getSpans();
		Span last = sps.get(sps.size()-1);
		String text = t.getText().toLowerCase();
		List<SurfaceElement> surfs = doc.getSurfaceElementsInSpan(new Span(last.getEnd(),su.getSpan().getBegin()));
		SurfaceElement closer = null;
		for (SurfaceElement surf: surfs) {
			String surfText = surf.getText().toLowerCase();
			int ind = surfText.indexOf(text);
			if (ind >= 0)  {
				closer = surf;
			}
		}
		if (closer == null) return null;
		LinkedHashSet<SemanticItem> sems = closer.getSemantics();
		if (sems == null) return null;

		for (SemanticItem sem: sems) {
			if (sem.ontologyEquals(si)) return sem;
		}
		return null;
	}
}
