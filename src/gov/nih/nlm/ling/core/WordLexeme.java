package gov.nih.nlm.ling.core;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Attribute;
import nu.xom.Element;

/**
 * Representation of a single lexeme, manifested in text as a <code>Word</code>. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class WordLexeme extends AbstractLexeme implements ContiguousLexeme {
	
	public WordLexeme(String lemma, String category) {
		super(lemma, category);
	}
	
	/**
	 * Creates a <code>WordLexeme</code> object from XML. 
	 * The input parameter is expected to have the following attributes:
	 * lemma and pos.
	 * 
	 * @param el  the XML tag that contains the lexeme information
	 */
	public WordLexeme(Element el) {
		this(el.getAttributeValue("lemma"),el.getAttributeValue("pos"));
	}
	
	public WordLexeme getHead() {
		return this;
	}
	
	public List<WordLexeme> toLexemeList() {
		List<WordLexeme> lex = new ArrayList<WordLexeme>();
		lex.add(this);
		return lex;
	}
	
	public Element toXml() {	
		Element lEl = new Element("Lexeme");
		lEl.addAttribute(new Attribute("lemma",lemma));
		lEl.addAttribute(new Attribute("pos",category));
		return lEl;
	}

}
