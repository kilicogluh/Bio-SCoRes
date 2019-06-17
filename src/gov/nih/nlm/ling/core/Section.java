package gov.nih.nlm.ling.core;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;

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
	 * @param textSpan  the span of the section body text
	 * @param document  the document enclosing the section
	 */
	public Section(Span titleSpan, Span textSpan, Document document) {
		this.titleSpan = titleSpan;
		this.textSpan = textSpan;
		this.document = document;
		this.subSections = new ArrayList<Section>();
	}

	/**
	 * Creates a <code>Section</code> object from given XML representation.
	 * 
	 * @param el		the XML element corresponding to the section
	 * @param document  the document enclosing the section
	 */
	public Section(Element el, Document document) {
		this(el.getAttributeValue("titleSpan") == null ? null : new Span(el.getAttributeValue("titleSpan")),
			 el.getAttributeValue("textSpan") == null ? null : 	new Span(el.getAttributeValue("textSpan")),
			 document);
		Elements subsects = el.getChildElements("section");
		for (int i=0; i < subsects.size(); i++) {
			Element sub = subsects.get(i);
			Section subsec = new Section(sub,document);
			this.addSubsection(subsec);
		}
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
	 * Returns the title of the section. 
	 * 
	 * @return	the title string for the section or null if the title span is unspecified.
	 */
	public String getTitle() {
		if (titleSpan == null) return null;
		return document.getStringInSpan(titleSpan);
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
	 * Returns the XML representation of this section. <p>
	 * 
	 * @return  the XML representation
	 */
	public Element toXml() {
		Element sect = new Element("section");
		if (textSpan != null) 
			sect.addAttribute(new Attribute("textSpan",textSpan.toString()));
		if (titleSpan != null) {
			sect.addAttribute(new Attribute("titleSpan",titleSpan.toString()));
			sect.addAttribute(new Attribute("title",document.getStringInSpan(titleSpan)));
		}
		if (subSections != null) {
			for (Section sub: subSections) {
				sect.appendChild(sub.toXml());
			}
		}
		return sect;
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
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		Section s = (Section)obj;
		return (this.document.equals(s.getDocument()) &&
		        this.titleSpan.equals(s.getTitleSpan()) &&
		        this.textSpan.equals(s.getTextSpan()));
	}

	public int hashCode() {
	    return
	    ((document  == null ? 187 : document.hashCode()) ^ 
	     (titleSpan == null ? 131 : titleSpan.hashCode()) ^
 	     (textSpan == null ? 139 : textSpan.hashCode()));
	}

}
