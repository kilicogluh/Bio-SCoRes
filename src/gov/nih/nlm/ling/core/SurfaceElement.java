package gov.nih.nlm.ling.core;

import gov.nih.nlm.ling.graph.Node;
import gov.nih.nlm.ling.sem.SemanticItem;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * An interface to represent textual units. A textual unit may be a single word,
 * a multi-word token or a textual unit with gaps (e.g., <i>give up</i> in <i>give this up</i>).<p>
 * Each textual unit is enclosed by a <code>Sentence</code>, has an id, index, lemma, part-of-speech, 
 * syntactic category, and a head. It may be associated with a number of <code>SemanticItem</code> objects,
 * that represent the semantics of the textual unit. <p>
 * In rare cases, a textual unit (a <code>GappedMultiWord</code>) may extend beyond a sentence
 * (e.g., <i>On one hand .... On the other hand ....</i>), but even in those cases, the unit is primarily 
 * associated with one sentence only.
 * 
 * @see AbstractSurfaceElement
 * @see ContiguousSurfaceElement
 * @see Word
 * @see MultiWord
 * @see GappedMultiWord
 * 
 * @author Halil Kilicoglu
 *
 */
public interface SurfaceElement extends Node, Comparable<SurfaceElement> {
	
	/**
	 * Span-based comparison of textual units. The textual unit that appears earlier in text
	 * is given precedence.
	 */
	public static final Comparator<SurfaceElement> SPAN_ORDER = 
	        new Comparator<SurfaceElement>() {
			public int compare(SurfaceElement s1, SurfaceElement s2) {
				SpanList sp1 = s1.getSpan();
				SpanList sp2 = s2.getSpan();
				return sp1.compareTo(sp2);
			}	
		};
	
	public int getIndex();
	public void setIndex(int i);
	
	public Sentence getSentence();
	public void setSentence(Sentence s);
	
	public Document getDocument();
	
	public String getText();
	public void setText(String t);
	
	public String getLemma();
	
	public String getPos();
	public void setPos(String p);
		
	/**
	 * Returns a higher-level POS category.<p>
	 * It is used to abstract away from tense information, and such.
	 * 
	 * For example, if the POS is 'VBZ', it returns 'VB', if the POS is 'NNS', it returns 'NN'.
	 * 
	 * @return  the category of the textual unit
	 */
	public String getCategory();
	
	public boolean isVerbal();
	public boolean isNominal();
	public boolean isAdjectival();
	public boolean isCoordConj();
	public boolean isAdverbial();
	public boolean isDeterminer();
	public boolean isPrepositional();
	
	/**
	 * @return  true if the textual unit can act as a predicate
	 * 
	 */
	public boolean isPredicate();
	public boolean isPronominal();
	public boolean isRelativePronoun();
	public boolean isAlphanumeric();
	
	/**
	 * @return  the head word of this textual unit
	 */
	public Word getHead();
	/**
	 * @return  the list of <code>Word</code> objects (tokens) in this textual unit from left to right
	 */
	public List<Word> toWordList();
	
	/**
	 * 
	 * @param cat  the category to search for
	 * @return  the list of <code>Word</code> objects (tokens) in this textual unit having a specific category
	 */
	public List<Word> toWordList(String cat);
	
	public boolean hasSemantics();
	/** 
	 * @return  a String representation of the semantic objects associated with this textual unit.
	 */
	public String writeSemantics();
	
	/**
	 * Gets all semantic items of a given class associated with this textual unit. 
	 * 
	 * @param clazz  the semantic object class (<code>Entity</code>, <code>Predicate</code>, etc.) provided with their canonical class name
	 * @return  all semantic items with the given type that are associated with the textual unit, 
	 * 			null if the textual unit is semantically empty
	 */
	public LinkedHashSet<SemanticItem> filterSemanticsByClass(Class<? extends SemanticItem> clazz);
	
	/**
	 * @return  only <code>Entity</code> objects associated with the textual unit 
	 * 
	 */
	public LinkedHashSet<SemanticItem> filterByEntities();
	/**
	 * @return  only <code>Predicate</code> objects
	 */
	public LinkedHashSet<SemanticItem> filterByPredicates();
	/**
	 * Returns all atomic terms (<code>Entity</code> and <code>Predicate</code>). 
	 * @return the set of atomic terms
	 */
	public LinkedHashSet<SemanticItem> filterByTerms();

	/**
	 * @return  only <code>Relation</code> objects.
	 */
	public LinkedHashSet<SemanticItem> filterByRelations();
	
	/**
	 * Finds the semantic items associated specifically with the head of the textual unit.<p>
	 * This is usually the most prominent semantic object associated with the textual unit.
	 * 
	 * @return  the semantic objects associated with the head word, null if the textual unit 
	 * 			is semantically empty
	 */
	public LinkedHashSet<SemanticItem> getHeadSemantics();

	/**
	 * Returns true if the textual unit contains any of the tokens in a list, ignoring case.
	 * 
	 * @param tokens  the tokens to check for
	 * 
	 * @return true if the textual unit contains any of the token.
	 */
	public boolean containsAnyToken(List<String> tokens);
	/**
	 * Similar to {@link #containsAnyToken(List)} but checks lemmas, instead of tokens.
	 * 
	 * @param lemmas	the lemmas to check for
	 * 
	 * @return true if the textual unit contains any of the lemmas
	 */
	public boolean containsAnyLemma(List<String> lemmas);
	/**
	 * Returns true if the textual unit contains a specific token, ignoring case.
	 * 
	 * @param token  the string to check for 
	 * 
	 * @return true if the textual unit contains a specific token
	 */
	public boolean containsToken(String token);
	/** 
	 * Similar to {@link #containsToken(String)} but checks lemmas, instead of tokens.
	 * 
	 * @param lemma the lemma to check for
	 * 
	 * @return true if the textual unit contains a specific lemma
	 */
	public boolean containsLemma(String lemma);

}
