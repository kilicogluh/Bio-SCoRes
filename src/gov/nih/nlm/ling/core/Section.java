package gov.nih.nlm.ling.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a section of a <code>Document</code>. <p>
 * A section consists of zero or one title span, a continuous span of section text, a
 * and potentially subsections contained within it.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Section {

	private final Span titleSpan;
	private final Span textSpan;
	private final Document document;
	private List<Section> subSections;
	
	/**
	 * Creates a <code>Section</code> object, within the given <var>document</var>.
	 * 
	 * @param titleSpan the span of the section title, if any.
	 * @param textSpan  the span of the section body text.
	 * @param document  the document enclosing the section
	 */
	public Section(Span titleSpan, Span textSpan, Document document) {
		this.titleSpan = titleSpan;
		this.textSpan = textSpan;
		this.document = document;
		this.subSections = new ArrayList<Section>();
	}

	public Span getTextSpan() {
		return textSpan;
	}

	public Document getDocument() {
		return document;
	}

	public Span getTitleSpan() {
		return titleSpan;
	}

	public List<Section> getSubSections() {
		return subSections;
	}

	public void setSubSections(List<Section> subSections) {
		this.subSections = subSections;
	}
	
	public void addSubsection(Section sub) {
		if (Span.subsume(textSpan, sub.getTextSpan()) == false ||
				(sub.getTitleSpan() != null && Span.subsume(textSpan,sub.getTitleSpan()) == false)) return;
		if (subSections == null) subSections = new ArrayList<>();
		subSections.add(sub);
			
	}
	
	/**
	 * Gets sentences enclosed in this section.
	 * 
	 * @return the list of sentences.
	 */
	public List<Sentence> getSentences() {
		List<Sentence> out = new ArrayList<>();
		for (Sentence sent: document.getSentences()) {
			if (Span.subsume(textSpan, sent.getSpan()) || 
				(titleSpan != null && Span.subsume(titleSpan,sent.getSpan()))) 
				out.add(sent);		
		}
		return out;
	}

	public String toString() {
		String text= document.getStringInSpan(textSpan);
		int newline = text.indexOf("\n");
		if (newline == -1) {
			newline = (text.length() <= 100 ? text.length(): 100);
		}
		String shortened = text.substring(0,Math.min(100, newline));
		if (shortened.length() < text.length()) shortened += "...";
		return (titleSpan == null ? "NO_TITLE" : document.getStringInSpan(titleSpan)) + "|" + textSpan.toString() + "|" + shortened;
	}
	
	/**
	 * Determines whether two sentences are in the same section of a document.
	 * 
	 * @param a the first sentence
	 * @param b the second sentence
	 * @return  true if the sentences are in the same section
	 */
	// TODO Does not consider title spans at the moment.
	public static boolean sameSection(Sentence a, Sentence b) {
		if (a.equals(b)) return true;
		if (a.getDocument().equals(b.getDocument()) == false) return false;
		Document doc = a.getDocument();
		for (Section sect : doc.getSections()) {
			if (Span.subsume(sect.getTextSpan(), a.getSpan()) && Span.subsume(sect.getTextSpan(),b.getSpan())) return true;
		}
		return false;
	}

}
