package gov.nih.nlm.ling.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.ContiguousLexeme;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.GappedLexeme;
import gov.nih.nlm.ling.core.MultiWord;
import gov.nih.nlm.ling.core.MultiWordLexeme;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.core.WordLexeme;
import gov.nih.nlm.ling.sem.Indicator;
import gov.nih.nlm.ling.sem.Ontology;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import gov.nih.nlm.ling.sem.Sense;

/**
 * An annotator for indicators (predicates). It currently can annotate single-word, multi-word,
 * or gapped indicators.  
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO This class can potentially be made more general to include concept annotations, as well.
public class IndicatorAnnotator implements TermAnnotator {
	private static Logger log = Logger.getLogger(IndicatorAnnotator.class.getName());	
	
	private LinkedHashSet<Indicator> indicators;
	// TODO This requires a more thorough thinking. The problem is two identical strings with different POSs may exist
	// and we may choose to use only one, but the indicator information in other respects (syntactic patterns, 
	// semantic types, etc.) may not match. So, if we ignore an indicator based on POS alone, we may lose some indicators
	// we really want. 
	private boolean ignorePOS = false;
	private boolean allowMultipleAnnotations = true;
		
	public IndicatorAnnotator() {}
	
	/**
	 * Constructs an <code>IndicatorAnnotator</code> with a set of {@link Indicator} objects.
	 * 
	 * @param indicators	the set of indicators
	 */
	public IndicatorAnnotator(LinkedHashSet<Indicator> indicators) {
		this.indicators = indicators;
	}
	
	public LinkedHashSet<Indicator> getIndicators() {
		return indicators;
	}

	public void setIndicators(LinkedHashSet<Indicator> indicators) {
		this.indicators = indicators;
	}
	
	/**
	 * Returns whether to ignore part-of-speech for annotation. If the indicator set only has <i>result[VB]</i>, for example,
	 * if <code>ignorePOS</code> variable is false, this annotator will not annotate <i>result[NN]</i> tokens.
	 * 
	 * @return	true if this variable has been set.
	 */
	public boolean getIgnorePOS() {
		return ignorePOS;
	}
	
	public void setIgnorePOS(boolean ignorePOS) {
		this.ignorePOS = ignorePOS;
	}

	/**
	 * Returns whether multiple indicator annotations are allowed over the same text.
	 * 
	 * @return true if this variable has been set.
	 */
	public boolean allowMultipleAnnotations() {
		return allowMultipleAnnotations;
	}

	public void setAllowMultipleAnnotations(boolean allowMultipleAnnotations) {
		this.allowMultipleAnnotations = allowMultipleAnnotations;
	}
	
	@Override 
	public void annotate(Document document, Properties props,
			Map<SpanList,LinkedHashSet<Ontology>> annotations) {
		if (indicators == null)
			throw new IllegalStateException("No indicators have been loaded for annotation.");
		List<Indicator> indicatorList = new ArrayList<>(indicators);
		// Annotate the larger indicators first
		Collections.sort(indicatorList,Indicator.LENGTH_ORDER);
		LinkedHashSet<String> seenIndLemmas = new LinkedHashSet<>();
		for (Indicator ind: indicatorList) {
			if (ignorePOS && seenIndLemmas.contains(ind.getLexeme().getLemma())) {
				continue;
			}
			log.log(Level.FINE,"Annotating indicator: {0}", new Object[]{ind.toString()});
			if (ind.isGapped()) {
				// how many sentences are allowed in gapping, default 2.
				int window = 2;
				if (props.getProperty("gappedTermAnnotationWindow") != null)
					window = Integer.parseInt(props.getProperty("gappedTermAnnotationWindow"));
				annotateGapped(document, ind, window, annotations);
			} else if (ind.isMultiWord()) annotateMultiWord(document, ind, annotations);
			else annotateWord(document,ind,annotations);	
			if (ignorePOS) seenIndLemmas.add(ind.getLexeme().getLemma());
		}
	}

	/**
	 * Annotates a given <code>Document</code> with the loaded indicators and 
	 * creates the corresponding <code>Predicate</code> objects for the mentions, as well.
	 * 
	 * @param document	the document to annotate
	 * @param props		the properties to use for annotation
	 */
	public void annotateIndicators(Document document, Properties props) {
		Map<SpanList,LinkedHashSet<Ontology>> map = new LinkedHashMap<>();
		annotate(document,props,map);
		SemanticItemFactory sif = document.getSemanticItemFactory();
		for (SpanList sp: map.keySet()) {
			Span first = sp.getSpans().get(0);
			// determine the head
			List<Word> words = document.getWordsInSpan(first);
			SpanList headSpan = null;
			if (words.size() > 1) 
				headSpan = MultiWord.findHeadFromCategory(words).getSpan();
			else 
				headSpan = sp;
			LinkedHashSet<Ontology> inds = map.get(sp);
			for (Ontology ont: inds) {
				Indicator ind = (Indicator)ont;
				Sense sem = ind.getMostProbableSense();
				sif.newPredicate(document, sp, headSpan, ind, sem);
			}
		}
		
	}
	
	/**
	 * Annotates single-word indicators. If the word being examined is hyphenated, it attempts
	 * to annotate the indicator on part of the word, as well.
	 * 
	 * @param document  	the document
	 * @param indicator  	the indicator
	 * @param annotations	the updated annotations list
	 */
	public void annotateWord(Document document, Indicator indicator, Map<SpanList, LinkedHashSet<Ontology>> annotations) {
		if (document.getSentences() == null) return;
		WordLexeme indLex = (WordLexeme)indicator.getLexeme();
		for (Sentence sent : document.getSentences()) {
			List<Word> words = sent.getWords();
			if (words.size() == 0) {
				log.log(Level.WARNING,"Empty sentence: {0}- {1}.", new Object[]{sent.getId(),sent.getText()});
				continue;
			}
			for (Word w: words) {
				// already annotated and no multiple annotations allowed
				if (allowMultipleAnnotations == false && Document.getSemanticItemsBySpan(document, w.getSpan(),true).size() > 0) continue;
				SpanList sp = null;
				if (w.getLexeme().equals(indLex) || 
					(ignorePOS && w.getLemma().equals(indLex.getLemma()))) {
					sp = w.getSpan();
					log.log(Level.FINE,"Word {0} matches the indicator {1}.", new Object[]{w.toString(),indicator.toString()});
				} else if (w.getText().contains("-" + indLex.getLemma())) {
					// we don't want a substring to match, 'as' in 'associated' for example.
					int len = w.getText().length() - w.getText().indexOf("-")-1;
					if (indLex.getLemma().length() < len) continue;
					int begin = w.getText().indexOf("-" + indLex.getLemma())+1+w.getSpan().getBegin();
					sp  = new SpanList(begin,begin+indLex.getLemma().length());
					log.log(Level.FINE,"Word {0} partially matches the indicator {1}.", new Object[]{w.toString(),indicator.toString()});
				}
				if (sp != null) {
					LinkedHashSet<Ontology> ex = annotations.get(sp);
					if (ex == null) ex = new LinkedHashSet<Ontology>();
					ex.add(indicator);
					annotations.put(sp, ex);
				}
			}
		}
	}
	
	/**
	 * Annotates multi-word, single span indicators.
	 * 
	 * @param document  	the document
	 * @param indicator		the indicator
	 * @param annotations	the updated annotations list
	 */
	public void annotateMultiWord(Document document, Indicator indicator, Map<SpanList, LinkedHashSet<Ontology>> annotations) {
		if (document.getSentences() == null) return;
		MultiWordLexeme indLex = (MultiWordLexeme)indicator.getLexeme();
		List<WordLexeme> intWordLexes = indLex.toLexemeList();
		for (Sentence sent : document.getSentences()) {
			List<SpanList> spans = getContiguousSpans(sent,intWordLexes);
			for (SpanList sp: spans) {
				if (allowMultipleAnnotations == false && Document.getSemanticItemsBySpan(document, sp, true).size() > 0) continue;
				log.log(Level.FINE,"Multi-word {0} matches the indicator {1}.", new Object[]{document.getStringInSpan(sp),indicator.toString()});
				LinkedHashSet<Ontology> ex = annotations.get(sp);
				if (ex == null) ex = new LinkedHashSet<Ontology>();
				ex.add(indicator);
				annotations.put(sp, ex);
			}
		}
	}
	
	/**
	 * Annotates gapped indicators. It requires a sentence window as input, so that it knows how far to look ahead
	 * for different parts of the indicator. The sentence window size includes the current sentence, so a size of 2
	 * means the current sentence and the next.
	 * 
	 * @param document			the document
	 * @param indicator			the gapped indicator
	 * @param sentWindowSize	the sentence window size
	 * @param annotations  		the resulting annotations
	 */
	// TODO: Needs serious testing.
	public void annotateGapped(Document document, Indicator indicator, int sentWindowSize, Map<SpanList, LinkedHashSet<Ontology>> annotations) {
		if (document.getSentences() == null) return;
		List<ContiguousLexeme> indLex = ((GappedLexeme)indicator.getLexeme()).getLexemes();
		int indSize = indLex.size();
		List<Sentence> sentences = document.getSentences();
		for (int i=0; i < sentences.size();i++) {
			Sentence sent = sentences.get(i);
			ContiguousLexeme cont = indLex.get(0);
			List<SpanList> spans = getContiguousSpans(sent,cont.toLexemeList());
			List<List<SpanList>> lexSpans = new ArrayList<List<SpanList>>();
			if (spans.size() > 0) {
				lexSpans.add(spans);
				boolean matchSoFar = true;
				for (int j =1;j < indSize; j++) {
					ContiguousLexeme c = indLex.get(j);
					List<Sentence> windowSents = sentences.subList(i, Math.min(i+sentWindowSize,sentences.size()));
					List<SpanList> jSpans = getContiguousSpans(windowSents,c.toLexemeList()); 
					if (jSpans.size() == 0) { matchSoFar =false; break;}
					lexSpans.add(jSpans);
				}
				if (matchSoFar) {
					// TODO This prunes to the last span at the moment, 
					List<SpanList> pruned = pruneSpans(lexSpans);
					for (SpanList p: pruned) {
						if (allowMultipleAnnotations == false && Document.getSemanticItemsBySpan(document, p, true).size() > 0) continue;
/*						List<Word> headWords = doc.getWordsInSentenceSpan(
								p.getSpans().get(((GappedLexeme)ind.getLexeme()).getPrimaryIndex()));*/
//						List<Word> headWords = doc.getWordsInSentenceSpan(
//								p.getSpans().get(0));
//						SpanList headSp = MultiWord.findHeadFromCategory(headWords).getSpan();
//						Predicate pr = sif.newPredicate(doc, p, headSp, ind, sem);
//						annotations.put(p, pr);
						log.log(Level.FINE,"Gapped word {0} matches the indicator {1}.", new Object[]{document.getStringInSpan(p),indicator.toString()});
						LinkedHashSet<Ontology> ex = annotations.get(p);
						if (ex == null) ex = new LinkedHashSet<Ontology>();
						ex.add(indicator);
						annotations.put(p, ex);
					}
				}
			}
		}
	}
	
	private List<SpanList> getContiguousSpans(Sentence sent, List<WordLexeme> lexemes) {
		List<SpanList> spans = new ArrayList<SpanList>();
		List<Word> words = sent.getWords();
		if (words.size() == 0) {
			log.log(Level.WARNING,"Empty sentence: {0}- {1}.", new Object[]{sent.getId(),sent.getText()});
			return spans;
		}
		WordLexeme firstLex = lexemes.get(0);
		List<WordLexeme> sentLexemes = sent.getLexemes();
		for (int i=0; i < sentLexemes.size(); i++) {
			WordLexeme wl = sentLexemes.get(i);
			if (wl.equals(firstLex) || (ignorePOS && wl.getLemma().equals(firstLex.getLemma()))) {
				// only matching the last token of the sentence, so skip
				if (i == sentLexemes.size() -1 && lexemes.size() > 1) continue;
				// match the rest
				if (lexemes.size() == 1) { spans.add(words.get(i).getSpan()); continue;}
				Word lastToken = getLastTokenWithLexeme(words.subList(i+1, words.size()),
								lexemes.subList(1, lexemes.size()));
				if (lastToken != null) {
					SpanList sp = new SpanList(words.get(i).getSpan().getBegin(),lastToken.getSpan().getEnd());
					spans.add(sp);
					i += lexemes.size();
				} 
			}
		}
		return spans;
	}
	
	private List<SpanList> getContiguousSpans(List<Sentence> sents, List<WordLexeme> lexemes) {
		List<SpanList> spans = new ArrayList<SpanList>();
		for (Sentence sent: sents) {
			spans.addAll(getContiguousSpans(sent,lexemes));
		}
		return spans;
	}
	
	private List<SpanList> pruneSpans(List<List<SpanList>> spans) {
		List<SpanList> outSpans = new ArrayList<SpanList>();
		List<SpanList> seen= new ArrayList<SpanList>();
		List<SpanList> iSpans = spans.get(spans.size()-1);
		for (int j=0; j < iSpans.size(); j++) {
			SpanList jSpan = iSpans.get(j);
			seen.add(jSpan);
			SpanList closest = jSpan;
			getClosestPrevious(jSpan,spans.subList(0,spans.size()-1),closest, seen);
			if (closest == null) break;
			outSpans.add(closest);
		}
		return outSpans;
	}
	
	// Recursively find the closest spans of the gapped lexeme and form a SpanList, which represents
	// the extent of the gapped lexeme.
	private void getClosestPrevious(SpanList curr, List<List<SpanList>> spans, SpanList closest, List<SpanList> seen) {
		if (spans.size() == 0) return;
		for (int i=spans.size();i>0; i--) {
			List<SpanList> iSpans = spans.get(i-1);
			SpanList close = getClosest(curr,iSpans, seen);
			if (close == null) { closest = null; return;}
			seen.add(close);
			closest = SpanList.union(closest, close);
			getClosestPrevious(close,spans.subList(0, i-1),closest,seen);
		}
	}
	
	// Assumes the spans are ordered. Get the closest preceding span list.
	private SpanList getClosest(SpanList curr, List<SpanList> spans, List<SpanList> seen) {
		for (int i=spans.size();i >0; i--) {
			// no need to go further, since we don't want to cross dependencies
			if (seen.contains(spans.get(i-1))) break;
			if (SpanList.atLeft(spans.get(i-1), curr)) return spans.get(i-1);
		}
		return null;
	}
	
	private Word getLastTokenWithLexeme(List<Word> words, List<WordLexeme> lexemes) {
		if (words == null || words.size() == 0) return null;
		Word last = null;
		for (int i=0; i< lexemes.size(); i++) {
			WordLexeme lex = lexemes.get(i);
			if (i == words.size()) return null;
			if (!ignorePOS && lex.equals(words.get(i).getLexeme()) == false) return null;
			if (ignorePOS && lex.getLemma().equals(words.get(i).getLemma()) == false) return null;
			last = words.get(i);
		}
		return last;
	}
	
}
