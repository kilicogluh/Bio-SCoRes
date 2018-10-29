package gov.nih.nlm.ling.core;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Element;
import nu.xom.Elements;

/**
 * A representation of a lexical unit with non-contiguous parts. This is the abstract 
 * analog of {@link GappedMultiWord}.
 * 
 * @author Halil Kilicoglu
 *
 */
public class GappedLexeme extends AbstractLexeme {
	protected List<ContiguousLexeme> lexemes;
	protected int primaryIndex; 
	
	/**
	 * Creates a <code>GappedLexeme</code> from a list of consecutive lexical units.
	 * 
	 * @param lexemes  the list of lexical units
	 */
	public GappedLexeme(List<ContiguousLexeme> lexemes) {
		this.lexemes = lexemes;
	}
	
	/**
	 * Creates a <code>GappedLexeme</code> from a list of consecutive lexical units.
	 * One of the units is assigned as primary.
	 * 
	 * @param lexemes  the list of lexical units
	 * @param primaryIndex  index of the primary unit
	 */
	public GappedLexeme(List<ContiguousLexeme> lexemes, int primaryIndex) {
		this(lexemes);
		this.primaryIndex = primaryIndex;
	}
	
	/** 
	 * Creates a <code>GappedLexeme</code> from an XML element.
	 * The XML element is expected to have multiple children named Part, each of which
	 * corresponds to a contiguous lexeme. The primary Part should have the primary attribute set to true. 
	 * 
	 * @param el  the Element that contains <code>GappedLexeme</code> elements
	 */
	public GappedLexeme(Element el) {
		Elements els = el.getChildElements("Part");
		this.lexemes = new ArrayList<ContiguousLexeme>(els.size());
		for (int i =0; i < els.size(); i++) {
			Element eli = els.get(i);
			Elements lexemeEls = eli.getChildElements("Lexeme");
			List<WordLexeme> lexemes = new ArrayList<WordLexeme>(lexemeEls.size());
			if (lexemes.size() == 1) {
				this.lexemes.add(new WordLexeme(lexemeEls.get(0)));
			} else {
				for (int j=0; j < lexemeEls.size(); j++) {
					lexemes.add(new WordLexeme(lexemeEls.get(j)));
				}
				this.lexemes.add(new MultiWordLexeme(lexemes));
			}
			if (eli.getAttribute("primary") != null && eli.getAttributeValue("primary").equals("true")) this.primaryIndex = i;
		}
	}
	
	public int getPrimaryIndex() {
		return primaryIndex;
	}

	public void setPrimaryIndex(int primaryIndex) {
		this.primaryIndex = primaryIndex;
	}
	
	public List<ContiguousLexeme> getLexemes() {
		return lexemes;
	}

	public void setLexemes(List<ContiguousLexeme> lexemes) {
		this.lexemes = lexemes;
	}
	
	public String getLemma() {
		StringBuffer buf = new StringBuffer();
		for (ContiguousLexeme l: lexemes) {
			buf.append(l.getLemma() + " ");
		}
		return buf.toString().trim();
	}
	
	public List<WordLexeme> toLexemeList() {
		List<WordLexeme> lex = new ArrayList<>();
		for (ContiguousLexeme l: lexemes) {
			if (l instanceof WordLexeme) lex.add((WordLexeme)l);
			if (l instanceof MultiWordLexeme) lex.addAll(((MultiWordLexeme)l).getLexemes());
		}
		return lex;
	}
	
	public WordLexeme getHead() {
		ContiguousLexeme l = (primaryIndex >= 0 ? lexemes.get(primaryIndex) : lexemes.get(lexemes.size()-1));
		if (l instanceof WordLexeme) return ((WordLexeme)l).getHead();
		if (l instanceof MultiWordLexeme) return ((MultiWordLexeme)l).getHead();
		return null;
	}
	
	public String toString(){
		StringBuffer buf = new StringBuffer();
		buf.append("(" + lexemes.get(0));
		for (int i= 1; i < lexemes.size(); i++) {
			buf.append("," + lexemes.get(i));
		}
		buf.append(")");
		return buf.toString();
	}

	public int hashCode() {
		int lexemeHashCode = 157;
		for (ContiguousLexeme l: lexemes) {
			lexemeHashCode += l.hashCode() * 47;
		}
		return lexemeHashCode;
	}
	
	public boolean equals(Object obj) {
		if (obj == null || this.lexemes == null || getClass() != obj.getClass()) return false;
		if (this == obj) return true;
		GappedLexeme ind = (GappedLexeme)obj;
		if (ind.lexemes == null) return false;
		if (lexemes.size() != ind.getLexemes().size()) return false;
		for (int i=0; i < lexemes.size(); i++) {
			if (lexemes.get(i).equals(ind.getLexemes().get(i)) == false) return false;			
		}	
		return true;
	}
	
	public Element toXml() {
		Element gl = new Element("GappedLexeme");
		for (ContiguousLexeme cl: getLexemes()) {
			Element p = new Element("Part");
			if (cl instanceof MultiWordLexeme) {
				MultiWordLexeme mw = (MultiWordLexeme)cl;
				for (int i=0; i < mw.getLexemes().size(); i++ ) {
					p.appendChild(mw.getLexemes().get(i).toXml());
				}
			} else if (cl instanceof WordLexeme) {
				p.appendChild(((WordLexeme)cl).toXml());
			} 
			gl.appendChild(p);
		}
		return gl;
	}
}
