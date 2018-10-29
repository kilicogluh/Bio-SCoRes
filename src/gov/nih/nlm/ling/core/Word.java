package gov.nih.nlm.ling.core;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Attribute;
import nu.xom.Element;


/**
 * Representation of a single token with sentence index, part-of-speech, lemma, and span information. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class Word extends AbstractSurfaceElement implements ContiguousSurfaceElement {

	private WordLexeme lexeme;
	
	/**
	 * Constructs a <code>Word</code> object.
	 * 
	 * @param text  the token
	 * @param pos	the part-of-speech 
	 * @param lex	the lemma 
	 */
	public Word(String text, String pos, WordLexeme lex) {
		this.text = text;
		this.pos = pos;
		this.lexeme = lex;
	}
	
	/**
	 * Constructs a <code>Word</code> object with sentence index.
	 * 
	 * @param text  the token
	 * @param pos	the part-of-speech 
	 * @param lex	the lemma 
	 * @param index	 the sentence index
	 */
	public Word(String text, String pos, WordLexeme lex, int index) {
		this(text,pos, lex);
		this.index = index;
	}
	
	/**
	 * Constructs a <code>Word</code> object with a span.
	 * 
	 * @param text  the token
	 * @param pos	the part-of-speech 
	 * @param lex	the lemma 
	 * @param index	 the sentence index
	 * @param span	the span of the token
	 */	
	public Word(String text, String pos, WordLexeme lex, int index, Span span) {
		this(text,pos,lex,index);
		this.span = new SpanList(span);
	}
	
	/**
	 * Creates a <code>Word</code> object from XML. 
	 * The input parameter is expected to have the following attributes:
	 * text, POS, lemma, id, and charOffset.
	 * 
	 * @param el  the XML tag that contains the token information
	 */
	public Word(Element el) {
		this.id = el.getAttributeValue("id");
		this.text = el.getAttributeValue("text");
		this.pos = el.getAttributeValue("POS");
		String cat = (pos.length() > 1 ? pos.substring(0, 2) : pos);
		this.lexeme = new WordLexeme(el.getAttributeValue("lemma"),cat);
		this.index = Integer.parseInt(id.substring(id.indexOf("_")+1));
		this.span = new SpanList(el.getAttributeValue("charOffset"));
	}
	
	public Word getHead() {
		return this;
	}
		
	public List<Word> toWordList() {
		List<Word> words = new ArrayList<Word>();
		words.add(this);
		return words;
	}
	
	public List<Word> toWordList(String cat) {
		List<Word> words = new ArrayList<Word>();
		if (getCategory().equals(cat)) {
			words.add(this);
		} 
		return words;
	}
	
	public Span getSingleSpan() {
		return span.getSpans().get(0);
	}
	
	public WordLexeme getLexeme() {
		return lexeme;
	}

	public void setLexeme(WordLexeme lexeme) {
		this.lexeme = lexeme;
	}
	
	public String getLemma() {
		return lexeme.getLemma();
	}
	
	public boolean isVerbal() {
		return lexeme.isVerb();
	}
	
	public boolean isAdjectival() {
		return lexeme.isAdjective();
	}
	
	public boolean isAdverbial() {
		return lexeme.isAdverb();
	}
	
	public boolean isCoordConj() {
		return lexeme.isCoordinatingConjunction();
	}
	
	public boolean isNominal() {
		return lexeme.isNoun();
	}
	
	public boolean isDeterminer() {
		return lexeme.isDeterminer();
	}
	
	public boolean isPronominal() {
		return lexeme.isPronominal();
	}
	
	public boolean isPrepositional() {
		return pos.equals("IN");
	}
	
	public boolean isRelativePronoun() {
		return (pos.equalsIgnoreCase("WDT") || pos.startsWith("WRB") || pos.startsWith("WP") ||
			   (containsLemma("that") && pos.equalsIgnoreCase("IN")));
	}
	
	public String toString() {
		return "WORD(" + index + "_" + text + "_" + pos + "_" + getLemma() + "_" + span.toString() + ")" + writeSemantics();
	}
	
	/**
	 * Returns an XML element representing the object. 
	 * 
	 * @param i 	the word index to use for its id
	 * 
	 * @return  the XML element for the Word object
	 */
	public Element toXml(int i) {
		Element el = new Element("token");
		el.addAttribute(new Attribute("xml:space", 
		          "http://www.w3.org/XML/1998/namespace", "preserve"));
		el.addAttribute(new Attribute("id","stt_" + i));
		el.addAttribute(new Attribute("POS",pos));
		el.addAttribute(new Attribute("charOffset",span.toString()));
		el.addAttribute(new Attribute("text",text));
		el.addAttribute(new Attribute("lemma",getLemma()));
		return el;
	}
	
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		Word w = (Word)obj;
		return (this.getText().equals(w.getText()) &&
		        this.getLexeme().equals(w.getLexeme()) &&
		        this.getIndex() == w.getIndex() &&
		        this.getSpan().equals(w.getSpan()));
	}

	public int hashCode() {
	    return
	    ((text  == null ? 17 : text.hashCode()) ^ 
	     (pos == null ? 29 : pos.hashCode()) ^
 	     (getLexeme() == null ? 31 : getLexeme().hashCode()) ^
	     (span == null ? 47 : span.hashCode()) ^
	     (index * 59));
	}
	
}

