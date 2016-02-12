package gov.nih.nlm.ling.core;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Element;
import nu.xom.Elements;

/**
 * A representation of a contiguous, multi-part lexical unit. The most prominent lexeme in the unit
 * is its head.  Most of the syntactic and semantic characteristics of the lexical unit are derived from its head.
 * 
 * @author Halil Kilicoglu
 *
 */
public class MultiWordLexeme extends AbstractLexeme implements ContiguousLexeme {
	
	private List<WordLexeme> lexemes;
	private WordLexeme head;
	
	public MultiWordLexeme(List<WordLexeme> lexemes) {
		this.lexemes = lexemes;
		this.head = findHead(lexemes);
	}
	
	public MultiWordLexeme(Element el) {
		Elements els = el.getChildElements("Lexeme");
		this.lexemes = new ArrayList<WordLexeme>(els.size());
		for (int i=0; i < els.size(); i++) {
			this.lexemes.add(new WordLexeme(els.get(i)));
		}
		this.head = findHead(lexemes);
	}
	
	public List<WordLexeme> getLexemes() {
		return lexemes;
	}

	public void setLexemes(List<WordLexeme> lexemes) {
		this.lexemes = lexemes;
	}
	
	public WordLexeme getHead() {
		return head;
	}
	
	public List<WordLexeme> toLexemeList() {
		return lexemes;
	}
	
	public String getLemma() {
		StringBuffer buf = new StringBuffer();
		for (WordLexeme l: lexemes) {
			buf.append(l.getLemma() + " ");
		}
		return buf.toString().trim();
	}
	
	/**
	 * Identifies the head of this lexeme.<p>
	 * It currently takes the last predicative lexeme preceding a preposition or a relative pronoun as the head. 
	 * Otherwise, it returns the final lexeme.
	 * 
	 * @param lex  the lexeme list
	 * @return  the head lexeme
	 */
	public static WordLexeme findHead(List<WordLexeme> lex) {
		int lastPos = lex.size();
		for (int i = 0;i <lex.size(); i++) {
			WordLexeme l = lex.get(i);
			if (l.isPreposition() || 
				l.getCategory().equals("WD") || 
				l.getCategory().equals("WP") ||
				l.getCategory().equals("WR")) {lastPos = i; break;}
		}
		for (int i = lastPos-1;i >= 0; i--) {
			WordLexeme l = lex.get(i);
			if (PREDICATIVE_CATEGORIES.contains(l.getCategory())) {
				return l;
			}
		}
		return lex.get(lex.size()-1);
	}
	
	public String toString(){
		StringBuffer buf = new StringBuffer();
		buf.append("(" + lexemes.get(0));
		for (int i= 1; i < lexemes.size(); i++) {
			buf.append("_" + lexemes.get(i));
		}
		buf.append(")");
		return buf.toString();
	}

	public int hashCode() {
		int lexemeHashCode = 157;
		for (WordLexeme l: lexemes) {
			lexemeHashCode += l.hashCode() * 47;
		}
		return lexemeHashCode;
	}
	
	public boolean equals(Object obj) {
		if (obj == null || this.lexemes == null || getClass() != obj.getClass()) return false;
		if (this == obj) return true;
		MultiWordLexeme ind = (MultiWordLexeme)obj;
		if (ind.lexemes == null) return false;
		if (lexemes.size() != ind.getLexemes().size()) return false;
		for (int i=0; i < lexemes.size(); i++) {
			if (lexemes.get(i).equals(ind.getLexemes().get(i)) ==false ) return false;			
		}	
		return true;
	}
	
	public Element toXml() {
		Element mwlEl = new Element("MultiWordLexeme");
		for (WordLexeme l: lexemes) {
			Element lEl = l.toXml();
			mwlEl.appendChild(lEl);
		}
		return mwlEl;
	}
}
