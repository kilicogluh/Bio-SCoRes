package gov.nih.nlm.ling.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Represents a contiguous textual unit with more than one token.
 * The most prominent of the words is called a head. Most of the syntactic and semantic characteristics
 * of the multi-word expression are derived from its head.
 * 
 * @author Halil Kilicoglu
 *
 */
public class MultiWord extends AbstractSurfaceElement implements ContiguousSurfaceElement {	
	
	private MultiWordLexeme lexeme;
	private List<Word> words;
	private Word head;
		
	/**
	 * Creates a multi-word expression from a token list. The token list can be unsorted.
	 * If <var>findHead</var> is set to true, it tries to determine the head using 
	 * the categories of the tokens.
	 * 
	 * @param words  the word list
	 * @param findHead  whether to try to find the head
	 */
	// TODO: Attempts to use CollinsHeadFinder to find head has not been successful so far
	// but it probably would be a better strategy.
	public MultiWord(List<Word> words, boolean findHead) {
		Collections.sort(words);
		this.id = words.get(0).getId() + "_" + words.get(words.size()-1).getId();
		List<WordLexeme> lexemes = new ArrayList<>();
		List<String> tokens = new ArrayList<>();
		String pos = null;
		Sentence s = words.get(0).getSentence();
		for (Word w: words) {
			lexemes.add(w.getLexeme());
			tokens.add(w.getText());
			pos = w.getPos();
		}
		SpanList sp = new SpanList(words.get(0).getSpan().getBegin(),words.get(words.size()-1).getSpan().getEnd());
		this.setSpan(sp);
		this.text = s.getStringInSpan(sp);
		this.pos = pos;
		this.lexeme = new MultiWordLexeme(lexemes);
		this.words = words;
		if (findHead) {
//			this.head = ParseTreeUtils.findHeadFromTree(s, words);
			this.head = findHeadFromCategory(words);
			this.pos = head.getPos();
		}
	}
	
	/**
	 * Creates a multi-word expression and sets the head.
	 * 
	 * @param words  the word list
	 * @param head  the head word
	 */
	public MultiWord(List<Word> words, Word head) {
		this(words, false);
		this.head = head;
		this.pos = head.getPos();
	}
	
	public MultiWordLexeme getLexeme() {
		return lexeme;
	}
	public List<WordLexeme> getLexemes() {
		return lexeme.getLexemes();
	}
	public void setLexeme(MultiWordLexeme lexeme) {
		this.lexeme = lexeme;
	}
	public List<Word> getWords() {
		return words;
	}
	public void setWords(List<Word> words) {
		this.words = words;
	}
	/**
	 * Finds the head using category information of the tokens.
	 * 
	 * @return the head token
	 */
	public Word getHead() {
		if (head == null) findHeadFromCategory(words);
		return head;
	}
	public void setHead(Word head) {
		this.head = head;
	}
	
	/**
	 * Returns a lemma string with underscore as the delimiter between word lemmas.
	 * 
	 * @return the lemma representation of this multi-word
	 */
	public String getLemma() {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i < words.size()-1;i++)  {
			Word w = words.get(i);
			buf.append(w.getLemma() + "_");
		}
		buf.append(words.get(words.size()-1).getLemma());
		return buf.toString();
	}
	
	public Span getSingleSpan() {
		if (span.size() == 1) return span.getSpans().get(0);
		if (span.size() > 1) return new Span(span.getSpans().get(0).getBegin(),
												 span.getSpans().get(span.size()-1).getEnd());
		return null;
	}
	
	public boolean isPronominal() {
		for (Word w: words) 
			if (w.getLexeme().isPronominal()) return true;
		return false;	
	}
	
	public boolean isDeterminer() {
		for (Word w : words) {
			if (w.isDeterminer()) return true;
		}
		return false;
	}
	
	public List<Word> toWordList() {
		return words;
	}
	
	public List<Word> toWordList(String cat) {
		List<Word> wout = new ArrayList<Word>();
		for (Word w: words) {
			List<Word> wcat = w.toWordList(cat);
			wout.addAll(wcat);
		}
		return wout;
	}
	
	/**
	 * Identifies the head of the multi-word from the categories of its parts.
	 * Uses {@link MultiWordLexeme#findHead(List)}. 
	 * 
	 * @param words  the word list
	 * @return  the head word, or null if empty or null input
	 */
	// TODO Not ideal.
	public static Word findHeadFromCategory(List<Word> words) {
		if (words == null || words.size() == 0) return null;
		if (words.size() == 1) return words.get(0);
		List<WordLexeme> lexemes= new ArrayList<WordLexeme>();
		for (Word w: words) {
			lexemes.add(w.getLexeme());
		}
		return words.get(lexemes.indexOf(MultiWordLexeme.findHead(lexemes)));
	}
	
	public String toString() {
		return "MULTI_WORD("+  index + "_" + text + "_" + pos + "_" + lexeme.toString() + "_" + span.toString() + ")" + writeSemantics();
	}
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		MultiWord mw = (MultiWord)obj;
		return (this.getText().equals(mw.getText()) &&
		        this.getLexeme().equals(mw.getLexeme()) &&
		        this.getIndex() == mw.getIndex() &&
		        this.getSpan().equals(mw.getSpan()));
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
