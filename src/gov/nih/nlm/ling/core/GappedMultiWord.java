package gov.nih.nlm.ling.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a textual unit with non-contiguous parts. It allows
 * treating such units as single entities. This, in turn, enables us to treat terms like 
 * <i>diabetes mellitus type 1 and 2</i> as two units <i>diabetes mellitus type 1</i> 
 * and <i>diabetes mellitus type 2</i>.
 * Currently, this class is not used much, but there is no reason we can't.
 * 
 * @author Halil Kilicoglu
 *
 */
public class GappedMultiWord extends AbstractSurfaceElement  {
		
	private List<ContiguousSurfaceElement> elements;
	private int primaryItemIndex;
		
	/**
	 * Creates a <code>GappedMultiWord</code> from a list of textual units.
	 * 
	 * @param ases  the list of textual units (contiguous or gapped)
	 */
	public GappedMultiWord(List<SurfaceElement> ases) {
		elements = new ArrayList<>();
		for (SurfaceElement se: ases) {
			if (se instanceof ContiguousSurfaceElement) elements.add((ContiguousSurfaceElement)se);
			else {
				GappedMultiWord gmw = (GappedMultiWord)se;
				List<ContiguousSurfaceElement> els = gmw.getElements();
				for (ContiguousSurfaceElement e: els) {
					elements.add((ContiguousSurfaceElement)e);
				}
			}
		}
	}
	
	/**
	 * Creates a <code>GappedMultiWord</code> from a list of textual units and assigns one
	 * of these units as primary. The idea is that syntactic/semantic characteristics of the
	 * multi-word expressions will be inherited from this primary unit. The primary unit is the 
	 * first one by default.
	 * 
	 * @param ases  the list of textual units (contiguous or gapped)
	 * @param primaryIndex  the index of the primary textual unit
	 */
	public GappedMultiWord(List<SurfaceElement> ases, int primaryIndex) {
		elements = new ArrayList<>();
		int i= 0;
		ContiguousSurfaceElement primaryItem = null;
		for (SurfaceElement se: ases) {
			if (se instanceof ContiguousSurfaceElement) {
				elements.add((ContiguousSurfaceElement)se);
				if (primaryIndex == i) primaryItem = (ContiguousSurfaceElement)se;
			}
			else {
				GappedMultiWord gmw = (GappedMultiWord)se;
				List<ContiguousSurfaceElement> els = gmw.getElements();
				for (ContiguousSurfaceElement e: els) {
					elements.add((ContiguousSurfaceElement)e);
				}
				if (primaryIndex == i)  primaryItem = els.get(gmw.getPrimaryItemIndex());
			}
			i++;
		}
		if (primaryItem == null) this.primaryItemIndex = 0;
		else this.primaryItemIndex = elements.indexOf(primaryItem);
	}
		
	public int getPrimaryItemIndex() {
		return primaryItemIndex;
	}

	public void setPrimaryItemIndex(int primaryItemIndex) {
		this.primaryItemIndex = primaryItemIndex;
	}

	/**
	 * @return  the list of contiguous textual units of this gapped multi-word 
	 */
	public List<String> getItemTexts() {
		List<String> toks = new ArrayList<String>();
		for (ContiguousSurfaceElement si: elements) {
			toks.add(si.getText());
		}
		return toks;
	}
	
	/**
	 * @return the String representation of this multi-word, units separated by space.
	 */
	public String getText() {
		StringBuffer sb = new StringBuffer();
		for (String s: getItemTexts()) {
			sb.append(s +  " ");
		}
		return sb.toString().trim();
	}
	
	public String getPos() {
		return elements.get(primaryItemIndex).getPos();
	}

	public List<String> getPosList() {
		List<String> pos = new ArrayList<>();
		for (ContiguousSurfaceElement si: elements) {
				pos.add(si.getPos());
		}
		return pos;
	}

	public SpanList getSpan() {
		List<Span> spans = new ArrayList<>();
		for (ContiguousSurfaceElement si: elements) {
				spans.add(si.getSingleSpan());
		}
		return new SpanList(spans);
	}

	public ContiguousSurfaceElement first() {
		return elements.get(0);
	}

	public ContiguousSurfaceElement last() {
		return elements.get(elements.size()-1);
	}
	
	public int size() {
		return elements.size();
	}
	
	public List<ContiguousSurfaceElement> getElements() {
		return elements;
	}
	
	public boolean isPronominal() {
		for (ContiguousSurfaceElement si: elements) {
			if (si instanceof Word && ((Word)si).isPronominal()) return true;
			if (si instanceof MultiWord && ((MultiWord)si).isPronominal()) return true;
		}
		return false;
	}
	
	public boolean isDeterminer() {
		return elements.get(primaryItemIndex).isDeterminer();
	}
	
	public Word getHead() {
//		ContiguousSurfaceElement s = (primaryItemIndex >= 0 ? elements.get(primaryItemIndex) : elements.get(elements.size()-1));
		ContiguousSurfaceElement s = (primaryItemIndex >= 0 ? elements.get(primaryItemIndex) : elements.get(0));		
		if (s instanceof Word) return ((Word) s).getHead();
		if (s instanceof MultiWord) return ((MultiWord) s).getHead();
		return null;
	}
	
	/**
	 * Sets the head for a gapped multi-word.<p>
	 * This essentially amounts to setting the <var>primaryItemIndex</var> and 
	 * setting the head of the contiguous unit with the primary item index.
	 * @param w the head word to set
	 */
	public void setHead(Word w) {
		for (int i=0; i < elements.size(); i++) {
			ContiguousSurfaceElement cs = elements.get(i);
			if (Span.subsume(cs.getSingleSpan(), w.getSingleSpan())) {
//				this.primaryItemIndex = i+1;
				this.primaryItemIndex = i;
			}
			if (cs instanceof MultiWord) ((MultiWord)cs).setHead(w);
		}
	}
	
	public String getLemma() {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i < elements.size()-1;i++)  {
			ContiguousSurfaceElement cse = elements.get(i);
			buf.append(cse.getLemma() + "__");
		}
		buf.append(elements.get(elements.size()-1).getLemma());
		return buf.toString();
	}
	
	public List<Word> toWordList() {
		List<Word> words = new ArrayList<>();
		for (ContiguousSurfaceElement s: elements) {
			words.addAll(s.toWordList());
		}
		return words;
	}
	
	public List<Word> toWordList(String cat) {
		List<Word> wout = new ArrayList<>();
		for (ContiguousSurfaceElement s: elements) {
			wout.addAll(s.toWordList(cat));
		}
		return (wout.size() == 0? null: wout);
	}
	
	public List<WordLexeme> getLexemeList() {
		List<WordLexeme> lex = new ArrayList<>();
		for (ContiguousSurfaceElement s: elements) {
			if (s instanceof Word) lex.add(((Word) s).getLexeme());
			if (s instanceof MultiWord) lex.addAll(((MultiWord) s).getLexemes());
		}
		return lex;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("GAPPED_MULTI_WORD(" + index + "_");
		for (ContiguousSurfaceElement s: elements) {
			  buf.append(s.toString());
			  if (!(s.equals(this.last()))) buf.append("_");
		}
		buf.append(writeSemantics());
		buf.append(")");
		return buf.toString();
	}
				
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		GappedMultiWord s = (GappedMultiWord)obj;
		if (size() != s.size()) return false;
		if (this.compareTo(s) == 0) return true;
		return false;
	}
	
}
