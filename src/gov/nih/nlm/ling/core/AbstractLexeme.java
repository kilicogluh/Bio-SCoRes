package gov.nih.nlm.ling.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The basic implementation of the <code>Lexeme</code> interface. 
 * 
 * @see WordLexeme
 * @see MultiWordLexeme
 * @see GappedLexeme
 * 
 * @author Halil Kilicoglu
 *
 */

public abstract class AbstractLexeme implements Lexeme {	
	
	// TODO This list should be more consistent with AbstractSurfaceElement.isPredicate()
	public static final List<String> PREDICATIVE_CATEGORIES = 
			Collections.unmodifiableList(Arrays.asList("NN","VB","JJ","RB","PR"));
	
	protected String lemma;
	protected String category;
	
	public AbstractLexeme() {}
	
	public AbstractLexeme(String lemma, String category) {
		this.lemma = lemma;
		this.category =  (category.length() <= 2 ? category: category.substring(0,2));
	}
	
	public String getLemma() {
		return lemma;
	}

	public String getCategory() {
		return category;
	}
	
	public boolean isNoun(){
		return category.equals("NN");
	}
	
	public boolean isVerb(){
		return category.equals("VB");
	}
	
	public boolean isAdjective(){
		return category.equals("JJ");
	}
	
	public boolean isAdverb(){
		return category.equals("RB");
	}
	
	public boolean isDeterminer(){
		return category.equals("DT");
	}
	
	public boolean isNumber(){
		return category.equals("CD");
	}
	
	public boolean isPreposition(){
		return category.equals("IN");
	}
	
	// TODO Needs attention
	public boolean isPronominal(){
		return category.equals("PR") || category.equals("WP");
	}
	
	public boolean isModal(){
		return category.equals("MD");
	}
	
	public boolean isCoordinatingConjunction(){
		return category.equals("CC");
	}
	
	// TODO not entirely accurate
	public boolean isSubordinatingConjunction(){
		return category.equals("IN");
	}
	
	public boolean isPunctuation(){		
		return lemma.replaceAll("[A-Za-z]", "").equals(category);
	}
	
	public String toString() {
		return lemma + "_" + category;	
	}
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		WordLexeme lex = (WordLexeme)obj;
		if (lemma.equals(lex.getLemma()) == false) return false;
		if (category.equals(lex.getCategory()) == false) return false;	
		return true;
	}
	
	public int hashCode() {
	    return ((lemma  == null ? 79 : lemma.hashCode()) ^ 
	    		(category  == null ? 91 : category.hashCode()));
	}

	abstract public WordLexeme getHead();
	abstract public List<WordLexeme> toLexemeList();

}
