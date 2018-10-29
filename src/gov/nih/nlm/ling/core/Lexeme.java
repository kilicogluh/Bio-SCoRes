package gov.nih.nlm.ling.core;

import java.util.List;

/**
 * An interface that represents an abstract, non-inflected lexical unit. Such a unit consists of a lemma, a category
 * (which is a generalization of a part-of-speech tag), and the head word. This interface is analogous to a 
 * <code>SurfaceElement</code>, but without the textual grounding, i.e., no spans, sentences, etc. If a lexical unit
 * is ambiguous, each sense would be its own lexeme.
 * 
 * @see AbstractLexeme
 * @see ContiguousLexeme
 * @see WordLexeme
 * @see MultiWordLexeme
 * @see GappedLexeme
 * 
 * @author Halil Kilicoglu
 *
 */
public interface Lexeme {
	public String getLemma();
	public String getCategory();
	
	/**
	 * @return  the head lexeme of this lexical unit.
	 */
	public WordLexeme getHead();
	
	/**
	 * 
	 * @return  the list of individual word lexemes from left to right.
	 */
	public List<WordLexeme> toLexemeList();
	
	public boolean isNoun();
	public boolean isVerb();
	public boolean isAdjective();
	public boolean isAdverb();
	public boolean isDeterminer();
	public boolean isNumber();
	public boolean isPreposition();
	public boolean isPronominal();
	public boolean isModal();
	public boolean isCoordinatingConjunction();
	public boolean isSubordinatingConjunction();
	public boolean isPunctuation();

}
