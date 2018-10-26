package gov.nih.nlm.ling.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;

/**
 * A factory class to create textual units, from character offsets or a list of smaller textual units.
 * This class currently does not allow overlapping textual units; in other words, a sentence is a contiguous list of textual units.
 * However, it allows associating multiple overlapping or non-overlapping semantic items with a single textual unit. For example, 
 * the textual unit <i>leg pain</i> can have semantic items for <i>leg</i>, <i>pain</i>, and <i>leg pain</i> associated with it.
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO A shortcoming of this class is that it does not allow overlapping textual units. This was particularly a 
// TODO problem in handling overlapping NE annotations in the i2b2 coreference corpus. 
public class SurfaceElementFactory {
	private static Logger log = Logger.getLogger(SurfaceElementFactory.class.getName());	

//	private int counter;
	
	public SurfaceElementFactory() {
//		counter = 0;
	}
	
/*	private int getNextId() {
		return ++counter;
	}*/
	
	/**
	 * Instantiates a new <code>SurfaceElement</code> from a span in a sentence, if necessary. <p>
	 * The span is expected to belong to the sentence. 
	 * 	
	 * @param sentence  the sentence to which the textual unit will belong to
	 * @param sp  the span of the textual unit
	 * @param headSpan  the span of the head of the textual unit 
	 * @return  a new textual unit, or null if the span does not belong to the sentence or either of them is invalid
	 * 
	 * @throws NullPointerException if the sentence or span is null.
	 */
	public SurfaceElement createSurfaceElementIfNecessary(Sentence sentence, SpanList sp, SpanList headSpan) {
		if (sentence == null || sp == null) 
			throw new NullPointerException("Unable to create new textual unit: sentence or span is null.");
		return newSurfaceElement(sentence,sp, headSpan);
	}
	
	/**
	 * Similar to {@link #createSurfaceElementIfNecessary(Sentence, SpanList, SpanList)}, but computes the head
	 * as well.
	 * 
	 * @param sentence  the sentence to which the textual unit will belong to
	 * @param sp  the span of the textual unit
	 * @return  a new textual unit, or null if the span does not belong to the sentence or either of them is invalid
	 * 
	 * @throws NullPointerException if the sentence or span is null.
	 * 
	 */
	public SurfaceElement createSurfaceElementIfNecessary(Sentence sentence, SpanList sp) {
		if (sentence == null || sp == null) 
			throw new NullPointerException("Unable to create new textual unit: sentence or span is null.");
/*		List<SurfaceElement> surfs = sentence.getSurfaceElementsFromSpan(sp);
		SpanList headSpan = sp;
		if (surfs.size() > 0) {
			// TODO This assumes head is associated with the last textual unit, but we don't know that.
			// TODO Should check whether the span list is gapped, and if so, should take the one considered the primary index.
			headSpan = surfs.get(surfs.size()-1).getHead().getSpan();
		}*/
		SpanList headSpan = sp;
		List<Word> surfs = sentence.getWordsInSpan(sp);
		if (surfs.size() > 0) {
			Word head = MultiWord.findHeadFromCategory(surfs);
			headSpan = head.getSpan();
		}
		return newSurfaceElement(sentence,sp, headSpan);
	}
	
	/**
	 * Similar to {@link #createSurfaceElementIfNecessary(Sentence, SpanList, SpanList)}, but allows specifying gapping.
	 * 
	 * @param doc  the document with the textual unit
	 * @param sp  the character offset list of the textual unit
	 * @param headSpan  the character offset list of the head of the textual unit 
	 * @param gapped  whether gaps are allowed in the textual unit
	 * @return   a new textual unit, or null if one cannot be instantiated with the given input
	 * 
	 * @throws NullPointerException if the document or span is null.
	 */
	public SurfaceElement createSurfaceElementIfNecessary(Document doc, SpanList sp, SpanList headSpan, boolean gapped) {
		if (doc == null || sp == null) 
			throw new NullPointerException("Unable to create new textual unit: document or span is null.");
		Sentence sent = null;
		for (Span s: sp.getSpans()) {
			sent = doc.getSubsumingSentence(s);
			// We are assuming that the first span is in the sentence to associate with the textual unit
			if (sent != null) break; 
		}
		if (sent == null) 			
			throw new NullPointerException("Unable to create new textual unit: no sentence associated with the spans.");
		// whether or not SpanList object can be treated as a single span 
		log.log(Level.FINEST,"Possibly merging spans {0}.", new Object[]{sp.toString()});
		SpanList spu = sent.mergeSpansIfPossible(sp);
		if (!gapped && spu.getSpans().size() > 1) {
			log.log(Level.WARNING,"No gapping is allowed but the spans ({0}) correspond to a gapped textual unit. Returning null.", 
					new Object[]{spu.toString()});
			return null;
		}
		log.log(Level.FINEST,"Merged spans are {0}.", new Object[]{spu.toString()});
		return newSurfaceElement(sent, spu, headSpan);
	}
	
	/**
	 * Same as  {@link #createSurfaceElementIfNecessary(Document, SpanList, SpanList, boolean)}, except it computes the head as well.
	 * 
	 * @param doc  the document with the textual unit
	 * @param sp  the character offset list of the textual unit
	 * @param gapped  whether gaps are allowed in the textual unit
	 * @return   a new textual unit, or null if one cannot be instantiated with the given input
	 * 
	 * @throws NullPointerException if the document or span is null.
	 */
	public SurfaceElement createSurfaceElementIfNecessary(Document doc, SpanList sp, boolean gapped) {
		if (doc == null || sp == null) 			
			throw new NullPointerException("Unable to create new textual unit: document or span is null.");
		Sentence sent = null;
		for (Span s: sp.getSpans()) {
			sent = doc.getSubsumingSentence(s);
			if (sent != null) break; // We are assuming that the first span is in the sentence to associate with the surfaceElement
		}
		if (sent == null) 			
			throw new NullPointerException("Unable to create new textual unit: no sentence associated with the spans.");
		// whether or not SpanList object can be treated as a single span 
		log.log(Level.FINEST,"Possibly merging spans {0}.", new Object[]{sp.toString()});
		SpanList spu = sent.mergeSpansIfPossible(sp);
		if (!gapped && spu.getSpans().size() > 1) {
			log.log(Level.WARNING,"No gapping is allowed but the spans ({0}) correspond to a gapped textual unit. Returning null.", 
					new Object[]{spu.toString()});
			return null;
		}
		log.log(Level.FINEST,"Merged spans are {0}.", new Object[]{spu.toString()});
		return createSurfaceElementIfNecessary(sent, spu);
	}
	
	private SurfaceElement newSurfaceElement(Sentence sent, SpanList sp, SpanList headSp) {
		// do we need to consider splitting, if the span is smaller than the larger unit that already exists?
		SurfaceElement existing = sent.getSubsumingSurfaceElement(sp);
		if (existing != null) {
			existing.setSentence(sent);
			return existing;
		}
		if (sp.size() == 1) {
			return newSurfaceElement(sent, sp.getSpans().get(0),headSp);
		}
		List<SurfaceElement> surfaceItems = new ArrayList<SurfaceElement>();
		for (Span s: sp.getSpans()) {
			SurfaceElement surf = newSurfaceElement(sent,s,headSp);
			if (surf == null) surf = newSurfaceElement(sent.getDocument(),new SpanList(s),headSp);
			surfaceItems.add(surf);
		}
		GappedMultiWord gmw = new GappedMultiWord(surfaceItems); 
		gmw.setSentence(sent);
		LinkedHashSet<SemanticItem> existingSem = existingSemantics(surfaceItems,sp);
		updateSurfaceElement(existingSem,gmw);
		gmw.setSemantics(existingSem);
		return gmw;
	}
	
	/**
	 * Instantiates a new <code>SurfaceElement</code> from a list of character offsets. <p>
	 * 
	 * It does not assume that spans belong to the same sentence. 
	 * It takes the sentence that the first span belongs to as the primary sentence. 
	 * If a new textual unit is created from shorter units, the semantics of the shorter units are inherited.
	 * 	
	 * @TODO: This is a bit dangerous. Things like discourseConnectives can span multiple sentences presumably.
	 * 'On one hand .... On the other hand..' 
	 * This means we may need to treat fragments from different sentences as one unit, but that also may
	 * break the integrity of the sentence unit, which I do not know is a good idea. 
	 * 
	 * @param sent  the sentence to which the textual unit will belong to
	 * @param sp  the span of the textual unit
	 * @param headSp  the span of the head of the textual unit
	 * @return  a new textual unit, or null if the span does not belong to the sentence
	 */
	private SurfaceElement newSurfaceElement(Document d, SpanList sp, SpanList headSp) {
		List<SurfaceElement> surfaceItems = new ArrayList<SurfaceElement>();
		// The sentence that the surface element is associated with. 
		// Currently taken to be the first sentence.
		Sentence surfSentence = null;
		int i = 0;
		for (Span s: sp.getSpans()) {
			Sentence sent = d.getSubsumingSentence(s);
			if (sent == null) continue; // should not happen, but could if something went wrong earlier
			if (i == 0) surfSentence = sent; 
			i++;			
			surfaceItems.add(newSurfaceElement(sent,s,headSp));
		}
		GappedMultiWord gmw = new GappedMultiWord(surfaceItems); 
		// gmw.setDocument(d);
		gmw.setSentence(surfSentence);
		LinkedHashSet<SemanticItem> existing = existingSemantics(surfaceItems,sp);
		gmw.setSemantics(existing);
		updateSurfaceElement(existing,gmw);
		return gmw;
	}
	
	private SurfaceElement newSurfaceElement(Sentence sent, Span sp, SpanList headSp) {
		/**
		 *  return the existing unit if it already subsumes the span. 
		 *  This was added to make it consistent with newSurfaceElement(Sentence,SpanList) below. 
		 */
		SurfaceElement existing = sent.getSubsumingSurfaceElement(new SpanList(sp));
		if (existing != null) {
			existing.setSentence(sent);
			return existing;
		}
		List<Word> words = sent.getWordsInSpan(sp);
		Word head = null;
		if (headSp != null) {
			Sentence headSent = sent.getDocument().getSubsumingSentence(headSp.asSingleSpan());
			List<Word> headWords = headSent.getWordsInSpan(headSp.asSingleSpan());
			if (headWords.size() > 0) head = headWords.get(headWords.size()-1);
		}
		return newSurfaceElement(sent, words, head);
	}
	
	
	private SurfaceElement newSurfaceElement(Sentence s, List<Word> words, Word head) {
		if (words == null) return null;
		if (words.size() == 1) { words.get(0).setSentence(s); return words.get(0);}
		else if (words.size() > 1) {
			SpanList sp =new SpanList(words.get(0).getSpan().getBegin(),words.get(words.size()-1).getSpan().getEnd()); 
			LinkedHashSet<SemanticItem> existingSem = existingSemantics(s.getSurfaceElementsFromSpan(sp),sp);
			MultiWord mw = null;
			if (head == null) mw = new MultiWord(words,true); 
			else mw = new MultiWord(words,head);
			mw.setSentence(s); 
			updateSurfaceElement(existingSem,mw);
			mw.setSemantics(existingSem);
			s.synchSurfaceElements(mw);
			return mw;
		} 
		return null;
	}
	
	// simply adds semantics associated with smaller units to the semantics of the larger unit
	// gets complicated with partially overlapping terms	
	private LinkedHashSet<SemanticItem> existingSemantics(List<SurfaceElement> sis, SpanList sp) {
		if (sis == null) return null;
		LinkedHashSet<SemanticItem> existingSem = new LinkedHashSet<SemanticItem>();
		for (SurfaceElement si: sis) {
			if (si.hasSemantics()) {
				LinkedHashSet<SemanticItem> sems = si.getSemantics();
				for (SemanticItem sem : sems) {
					if (SpanList.subsume(sp, sem.getSpan()))
							existingSem.add(sem);
				}
			}
		}
		return existingSem;
	}
	
	// when a textual unit is created from smaller chunks, we need to make sure that the semantic items
	// associated with these smaller chunks are updated to have the new SurfaceElement as their surface element
	// Works in tandem with existingSemantics method above
	private void updateSurfaceElement(Set<SemanticItem> sem, SurfaceElement se) {
		for (SemanticItem s: sem) {
			if (s instanceof Term) {
				Term t = (Term)s;
				if (SpanList.subsume(se.getSpan(), t.getSpan()))
					((Term)s).setSurfaceElement(se);
			}
		}
	}
	
	/**
	 * Splits a textual unit into the units specified with <var>split</var>.
	 * 
	 * @param doc  the document with the textual unit to split
	 * @param surf  the textual unit
	 * @param split  a list of <code>SpanList</code> that indicate how the splitting should be done

	 * @return  a list of resulting surface elements
	 * 
	 */
/*	public List<SurfaceElement> splitSurfaceElementIfNecessary(Document doc, SurfaceElement surf, List<SpanList> split) {
		if (doc == null || surf == null) return null;
		List<SurfaceElement> out = new ArrayList<SurfaceElement>();
		if (split == null || split.size() < 2) { out.add(surf); return out;}
		
		Sentence sent = null;
		for (Span s: sp.getSpans()) {
			sent = doc.getSubsumingSentence(s);
			if (sent != null) break; // We are assuming that the first span is in the sentence to associate with the surfaceElement
		}
		if (sent == null) return null;
		// whether or not SpanList object can be treated as a single span 
		log.debug("Possibly merging spans " + sp.toString());
		SpanList spu = sent.mergeSpansIfPossible(sp);
		if (!gapped && spu.getSpans().size() > 1) return null;
		log.debug("Merged spans " + spu.toString());
		return createSurfaceElementIfNecessary(sent, spu);
	}*/
}
