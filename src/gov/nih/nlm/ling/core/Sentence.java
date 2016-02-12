package gov.nih.nlm.ling.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.nlp.trees.Tree;
import gov.nih.nlm.ling.transform.DependencyTransformation;
import gov.nih.nlm.ling.util.XMLUtils;
import gov.nih.nlm.ling.wrappers.CoreNLPWrapper;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Text;

/**
 * Representation of a sentence, a list of which form a <code>Document</code>. 
 * A sentence consists of words (tokens) and is associated with a parse tree and syntactic dependencies 
 * that hold between its words. <p>
 * As the processing progresses, the word list is transformed into a list of textual units
 * ({@link SurfaceElement}s) and the dependency list is transformed into a list of embeddings, 
 * which are essentially semantically-informed modifications to the original
 * word list and dependencies, respectively. The original word and dependency list is maintained separately.
 * The list of transformations the sentence has gone through are kept track of.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Sentence {
	private final static Logger log = Logger.getLogger(Sentence.class.getName());
	
	private String id;
	private String text;
	private Document document;
	private Span span;
	private List<Word> words;
	private Tree tree;
	private List<SynDependency> dependencyList;
	private boolean hasTree;
	private List<SynDependency> embeddings;
	private List<SurfaceElement> surfaceElements;
	private List<Class<? extends DependencyTransformation>> transformations = new ArrayList<>();
	
	/**
	 * Creates a <code>Sentence</code> object with an identifier, the sentence string and its character offsets.
	 * 
	 * @param id  the identifier
	 * @param text  the sentence string
	 * @param span  the sentence character offsets
	 */
	public Sentence(String id, String text, Span span) {
		this.id = id;
		this.text = text;
		this.span = span;
	}
	
	/**
	 * Creates a <code>Sentence</code> with a token list only.
	 * 
	 * @param id  the identifier
	 * @param text  the sentence string
	 * @param span  the sentence character offsets
	 * @param words the word list
	 */
	public Sentence(String id, String text, Span span, List<Word> words) {
		this(id,text,span);
		this.words = words;
		this.surfaceElements = new ArrayList<SurfaceElement>(words);
		this.dependencyList = new ArrayList<>();
		this.embeddings = new ArrayList<>();
		this.tree = null;
		this.hasTree = false;
	}
	
	/**
	 * Creates a <code>Sentence</code> with a token list and syntactic information (parse tree and dependencies)
	 * 
	 * @param id  the identifier
	 * @param text  the sentence string
	 * @param span  the sentence character offsets
	 * @param words the word list
	 * @param dependencies  the syntactic dependencies of the sentence
	 * @param tree  the parse tree for the sentence
	 */
	public Sentence(String id, String text, Span span,
			List<Word> words, List<SynDependency> dependencies, Tree tree) {
		this(id,text,span,words);
		this.dependencyList = dependencies;
		this.embeddings = new ArrayList<>(dependencyList);
		this.tree = tree;
		this.hasTree = true;
	}
	
	/**
	 * Creates a <code>Sentence</code> object from an XML element.
	 * 
	 * @param el  the XML element that encapsulates the sentence information
	 */
	public Sentence(Element el) {
		this(el.getAttributeValue("id"),
			 el.getChildElements("text").get(0).getValue().trim(),
		     new Span(el.getAttributeValue("charOffset")));
		Elements tokenEls = el.getChildElements("tokens").get(0).getChildElements();
		words = new ArrayList<>();
		Map<String,Word> ids = new HashMap<>();
		for (int i=0; i < tokenEls.size(); i++) {
			Element tokenEl = tokenEls.get(i);
			Word w = new Word(tokenEl);
			words.add(w);
			w.setSentence(this);
			ids.put(w.getId(), w);
		}
		Elements depEls = el.getChildElements("dependencies").get(0).getChildElements();
		dependencyList = new ArrayList<>();
		for (int i=0; i < depEls.size(); i++) {
			Element depEl = depEls.get(i);
			SynDependency d = new SynDependency(depEl,ids);
			dependencyList.add(d);
		}
		Element treeEl = el.getChildElements("tree").get(0);
    	tree = CoreNLPWrapper.convertToTree(treeEl.getValue().trim());
		surfaceElements = new ArrayList<SurfaceElement>(words);
		embeddings = new ArrayList<>(dependencyList);
	}
	
	/**
	 * Similar to {@code #Sentence(Element)}, but allows using a particular
	 * tokenizer/parser output for sentence analysis.
	 * 
	 * @param el  the XML element encapsulating the sentence information
	 * @param tokenizer  the tokenizer name
	 * @param parser  the parser name
	 */
	public Sentence(Element el, String tokenizer, String parser) {
		this(el.getAttributeValue("id"),
				 el.getChildElements("text").get(0).getValue().trim(),
			     new Span(el.getAttributeValue("charOffset")));
		List<Element> tokensEls = XMLUtils.getChildrenWithAttributeValue(el, "tokens", "tokenizer", tokenizer);
		if (tokensEls.size() > 0) {
			Elements tokenEls = tokensEls.get(0).getChildElements();
			words = new ArrayList<Word>();
			Map<String,Word> ids = new HashMap<>();
			for (int i=0; i < tokenEls.size(); i++) {
				Element tokenEl = tokenEls.get(i);
				Word w = new Word(tokenEl);
				words.add(w);
				w.setSentence(this);
				ids.put(w.getId(), w);
			}
			List<Element> depsEls = XMLUtils.getChildrenWithAttributeValue(el, "dependencies", "parser", parser);
			if (depsEls.size() > 0) {
				Elements depEls = depsEls.get(0).getChildElements();
				dependencyList = new ArrayList<>();
				for (int i=0; i < depEls.size(); i++) {
					Element depEl = depEls.get(i);
					SynDependency d = new SynDependency(depEl,ids);
					if (SynDependency.findDependenciesBetween(dependencyList, d.getDependent(), d.getGovernor(), true).size() == 0 && 
							SynDependency.findDependenciesBetween(dependencyList, d.getGovernor(), d.getDependent(), true).size() == 0)
						dependencyList.add(d);
				}
			}
			List<Element> parseEls = XMLUtils.getChildrenWithAttributeValue(el, "tree", "parser", parser);
			if (parseEls.size() > 0) {
				Element treeEl = parseEls.get(0);
				tree = CoreNLPWrapper.convertToTree(treeEl.getValue().trim());
			}
			surfaceElements = new ArrayList<SurfaceElement>(words);
			embeddings = new ArrayList<>(dependencyList);
		}
	}
	
	public String getId() {
		return id;
	}
	public String getText() {
		return text;
	}
	public Document getDocument() {
		return document;
	}
	public void setDocument(Document document) {
		this.document = document;
	}
	public Span getSpan() {
		return span;
	}
	public List<Word> getWords() {
		return words;
	}
	public void setWords(List<Word> words) {
		this.words = words;
	}
	public Tree getTree() {
		return tree;
	}
	public void setTree(Tree tree) {
		this.tree = tree;
	}
	public boolean hasTree() {
		return hasTree;
	}
	public List<SynDependency> getDependencyList() {
		return dependencyList;
	}
	public void setDependencyList(List<SynDependency>dependencyList) {
		this.dependencyList = dependencyList;
	}
	public List<SynDependency> getEmbeddings() {
		return embeddings;
	}
	public void setEmbeddings(List<SynDependency> embeddings) {
		this.embeddings = embeddings;
	}
	public List<SurfaceElement> getSurfaceElements() {
		return surfaceElements;
	}
	public void setSurfaceElements(List<SurfaceElement> surfaceElements) {
		this.surfaceElements = surfaceElements;
	}
	public void addSurfaceElement(SurfaceElement element) {
		if (surfaceElements == null) surfaceElements = new ArrayList<>();
		surfaceElements.add(element);
	}
	public List<Class<? extends DependencyTransformation>> getTransformations() {
		return transformations;
	}
	public void setTransformations(List<Class<? extends DependencyTransformation>> transformations) {
		this.transformations = transformations;
	}
	public void addTransformation(Class<? extends DependencyTransformation> transform) {
		this.transformations.add(transform);
	}
	
	/**
	 * Resets the textual units and embeddings to the original word list and dependency list, respectively.
	 * 
	 */
	public void reset() {
		surfaceElements = new ArrayList<SurfaceElement>(words);
		embeddings = new ArrayList<SynDependency>(dependencyList);
	}
	
	/**
	 * Finds the textual units that are covered (fully or partially) by a given span
	 * 
	 * @param sp  the span that is considered
	 * @return  the list of textual units in span, empty list if no such textual units exist
	 */
	public List<SurfaceElement> getSurfaceElementsFromSpan(Span sp) {
		if (surfaceElements == null || sp == null || 
				Span.invalidSpan(sp) || Span.overlap(span, sp) == false) 
			return new ArrayList<>();
		return getSurfaceElementsFromSpan(new SpanList(sp));
	}
	
	/**
	 * Similar to {@link #getSurfaceElementsFromSpan(Span)}, takes in a span list as input.
	 * 
	 * @param sp	the span to consider
	 * @return the list of textual units in the span list, empty list if no such textual units exist
	 */
	public List<SurfaceElement> getSurfaceElementsFromSpan(SpanList sp) {
		if (surfaceElements == null || sp == null || 
				SpanList.invalidSpan(sp) || SpanList.overlap(new SpanList(span), sp) == false) 
			return new ArrayList<>();
		List<SurfaceElement> extentSurfaceEls = new ArrayList<SurfaceElement>();
		for (SurfaceElement surf: surfaceElements) {
			SpanList sps =  surf.getSpan();
			if (SpanList.overlap(sp, sps)) {
				extentSurfaceEls.add(surf);
			}
		}
		return extentSurfaceEls;
	}

	
	/**
	 * Similar to {@link #getSurfaceElementsFromSpan(Span)}, but returns individual tokens
	 * instead of textual units.
	 * 
	 * @param sp	the span to consider
	 * @return the list of words in the span, empty list if no words are found
	 */
	public List<Word> getWordsInSpan(Span sp) {
		if (words == null || sp == null || 
				Span.invalidSpan(sp) || Span.overlap(span, sp) == false)  
			return new ArrayList<>();
		return getWordsInSpan(new SpanList(sp));
	}
	
	/**
	 * Similar to {@link #getSurfaceElementsFromSpan(SpanList)}, but returns individual tokens
	 * instead of textual units.
	 * 
	 * @param sp	the span list to consider
	 * @return the list of words in the span list, empty list if no words are found
	 */
	public List<Word> getWordsInSpan(SpanList sp) {
		if (words == null || sp == null || 
				SpanList.invalidSpan(sp) || SpanList.overlap(new SpanList(span), sp) == false)  
			return new ArrayList<>();
		List<Word> wordList =  new ArrayList<>();
		for (Word w: words) {
			if (SpanList.overlap(sp, w.getSpan())) {
				wordList.add(w);
			}
		}
		return wordList;
	}
	
	
	/**
	 * Finds the text in the given span. 
	 * 
	 * @param sp  the span that is considered
	 * @return  the string in the span, empty string if the sentence does not cover the span
	 */
	public String getStringInSpan(Span sp) {
		if (sp == null || Span.invalidSpan(sp) || Span.overlap(span, sp) == false) return "";
		int sOffset = span.getBegin(); int sEnd = span.getEnd();
		int startPos = sp.getBegin();  int endPos = sp.getEnd();
		if (endPos <= startPos || endPos < sOffset || startPos > sEnd) return "";
		if (startPos-sOffset < 0 || endPos-sOffset > text.length()) return "";
		// trim() removed
		return text.substring(startPos-sOffset,endPos-sOffset);
	}
	
	/**
	 * Similar to {@link #getStringInSpan(Span)}, but it takes a span list as input.
	 * {@code SpanList#SEPARATOR} is used as the delimiter.
	 * 
	 * @param sp  the span list that is considered
	 * @return  the string in the span list, empty string if the sentence does not cover the span list
	 */
	public String getStringInSpan(SpanList sp) {
		if (sp == null ||SpanList.invalidSpan(sp) || SpanList.overlap(new SpanList(span), sp) == false) return "";
		String out = "";
		int size = sp.getSpans().size();
		for (int i=0; i< size-1; i++) {
			Span ls = sp.getSpans().get(i);
			String lsstr = getStringInSpan(ls);
			out += lsstr + SpanList.SEPARATOR;
		}
		out += getStringInSpan(sp.getSpans().get(size-1));
		// trim() removed
		return out;
	}
	
	/**
	 * @return  the list of all lexemes in this sentence.
	 */
	public List<WordLexeme> getLexemes() {
		if (words == null) return new ArrayList<>();
		List<WordLexeme> lex = new ArrayList<>();
		for (Word w: words) {
			lex.add(w.getLexeme());
		}
		return lex;
	}
	/**
	 * Finds the textual unit that fully covers the specified span list.
	 * 
	 * @param sp  the span list to consider
	 * @return  the textual unit that covers the span, if any, otherwise null.
	 */
	public SurfaceElement getSubsumingSurfaceElement(SpanList sp) {
		if (surfaceElements == null) return null;
		for (SurfaceElement surf: surfaceElements) {
			SpanList sps =  surf.getSpan();
			if (sps.equals(sp) || SpanList.subsume(sps,sp)) {
				return surf;
			}
		}
		return null;
	}
	
	/**
	 * Gets all spans of a given text in this sentence. 
	 * It does not check whether parts of an instance correspond to the same textual unit or not.
	 * 
	 * @param t  the text to check for
	 * @param allowSubMatch whether to allow matches with token substrings
	 * 
	 * @return  a <code>SpanList</code> object containing the span of each instance, null if none found
	 */
	public SpanList getSpansWithText(String t, boolean allowSubMatch) {
		if (t == null || t.equals("")) return null;
		int sentOffset = span.getBegin();
		int i = 0;
		List<Span> sps = new ArrayList<>();
		while (i < text.length()) {
			int ind = text.toLowerCase().indexOf(t,i);
			if (ind < 0) break;
			int n = ind+sentOffset;
			Span sp = new Span(n,n+t.length());
			// check whether the match is a substring of a token
			// if so, do not include
			boolean submatch = false;
			if (allowSubMatch == false) {
				for (Word w: words) {
					Span ws = w.getSingleSpan();
					if (Span.subsume(ws, sp) && ws.equals(sp) == false) submatch = true;
				}
			}
			if (!submatch) sps.add(sp);
			i += ind + t.length();
		}
		return (sps.size() == 0? null : new SpanList(sps));
	}
				
	/**
	 * Updates the textual unit information of this sentence with a new (probably newly-created)
	 * <code>SurfaceElement</code>.<p>
	 * It has no knowledge of the syntactic dependencies involved, so it's best to run this with {@code #synchEmbeddings(List, List)}
	 * with a list of dependencies to remove or add. 
	 * 
	 * 
	 * @param si  a new textual unit
	 */
	public synchronized void synchSurfaceElements(SurfaceElement si) {
		if (si == null || words == null) return;
		SpanList sisp = si.getSpan();
		List<SurfaceElement> newSurfaceItems = new ArrayList<>();
		List<SurfaceElement> existing;
		if (surfaceElements == null)  existing = new ArrayList<SurfaceElement>(words);
		else existing = surfaceElements;
		boolean added = false;
		int i = 0;
		for (int j = 0; j < existing.size(); j++) {
			SurfaceElement seit = existing.get(j);
			SpanList seitsp = seit.getSpan();
			if (SpanList.subsume(sisp, seitsp)) {
				if (added) continue;
				si.setIndex(++i);
				newSurfaceItems.add(si);
				added = true;
			} 
			else {
				added = false;
				seit.setIndex(++i);
				newSurfaceItems.add(seit);
			}
		}
		setSurfaceElements(newSurfaceItems);
	}
	
	/**
	 * Splits an existing textual unit into smaller chunks. The elements of <var>replacements</var> variable are
	 * expected to be in span order and fully subsume the textual unit. <p>
	 * It has no knowledge of the syntactic dependencies involved, so it's best to run this with {@code #synchEmbeddings(List, List)}
	 * with a list of dependencies to remove or add. 
	 * 
	 * @param si  an existing textual unit
	 * @param replacements  the replacing textual units
	 */
	public synchronized void splitSurfaceElements(SurfaceElement si, List<SurfaceElement> replacements) {
		if (si == null || words == null || replacements == null || replacements.size() == 0) return;
		List<SurfaceElement> newSurfaceItems = new ArrayList<>();
		List<SurfaceElement> existing;
		if (surfaceElements == null)  existing = new ArrayList<SurfaceElement>(words);
		else existing = surfaceElements;
		int index = si.getIndex();
		for (int j=0; j < index-1; j++) {
			newSurfaceItems.add(existing.get(j));
		}
		for (int i=0; i < replacements.size(); i++) {
			SurfaceElement r = replacements.get(i);
			r.setIndex(i+index);
			newSurfaceItems.add(r);
			r.setSentence(this);
		}
		for (int j = index; j < existing.size(); j++) {
			SurfaceElement seit = existing.get(j);
			int ind = seit.getIndex() + replacements.size() -1;
			seit.setIndex(ind);
			newSurfaceItems.add(seit);
		}
		setSurfaceElements(newSurfaceItems);
	}
	
	/**
	 * Updates embedding dependencies to make sure that they reflect the latest textual unit list.<p>
	 * If the embedding list has not been initialized, it will try to initialize it to the original dependency list.
	 * New dependencies may be added.
	 * 
	 */
	public synchronized void synchEmbeddings() {
		if (dependencyList == null || dependencyList.size() == 0) return;
		if (embeddings == null || embeddings.size() == 0) embeddings = new ArrayList<>(dependencyList);
		LinkedHashSet<SynDependency> markedForAddition = new LinkedHashSet<>();
		for (SynDependency d: embeddings) {
			SurfaceElement gov = d.getGovernor();
			SurfaceElement dep = d.getDependent();
			SurfaceElement subsumesGov = getSubsumingSurfaceElement(gov.getSpan());
			SurfaceElement subsumesDep = getSubsumingSurfaceElement(dep.getSpan());			
			if (subsumesGov == null || subsumesDep == null) {
				log.log(Level.WARNING, "Error with dependency {0} in sentence {1}.", new Object[]{d.toShortString(),id});
				if (subsumesGov == null) 
					log.log(Level.WARNING, "Null governor for the dependency {0}.", new Object[]{d.toShortString()});
				if (subsumesDep == null) 					
					log.log(Level.WARNING, "Null dependent for the dependency {0}.", new Object[]{d.toShortString()});
				continue;
			}
			// intra-semantic item dependency
			if (subsumesGov == subsumesDep)  continue;
			// words themselves correspond to semantic items, simply add
			if (subsumesGov == gov && subsumesDep == dep) {
				markedForAddition.add(d);
				continue;
			}
			// if an edge already exists, avoid creating a loop.
			if (SynDependency.findDependenciesBetween(new ArrayList<>(markedForAddition), subsumesDep, subsumesGov,false).size() > 0) 
				continue;
			// add a new dependency if no path exists 
			if (SynDependency.findDependencyPath(embeddings,subsumesDep,subsumesGov,true) == null) {
				SynDependency nSD = new SynDependency("semb_" + d.getId(),d.getType(),subsumesGov,subsumesDep);
				if (markedForAddition.contains(nSD) == false) {
					log.log(Level.FINEST, "Replacing dependency {0} with {1}.", new Object[]{d.toShortString(),nSD.toShortString()});
					markedForAddition.add(nSD);
				}
			}
		}
		setEmbeddings(new ArrayList<SynDependency>(markedForAddition));
	}
	
	/**
	 * Similar to {@link #synchEmbeddings()}, but takes as argument explicit lists of dependencies to remove/add.
	 * 
	 * @param remove  the list of dependencies to remove
	 * @param add  the list of dependencies to add
	 * 
	 */
	public synchronized void synchEmbeddings(List<SynDependency> remove, List<SynDependency> add) {
		if (dependencyList == null || dependencyList.size() == 0) return;
		if (embeddings == null || embeddings.size() == 0) embeddings = new ArrayList<>(dependencyList);
		List<SynDependency> alreadyRemoved = new ArrayList<>();
		if (remove != null) {
			for (SynDependency r: remove) {
				if (alreadyRemoved.contains(r)) continue;
				if (r != null) {
					log.log(Level.FINEST, "Removing dependency {0}.", new Object[]{r.toShortString()});
					embeddings.remove(r);
					alreadyRemoved.add(r);
				}
			}
		}
		if (add != null){
			for (SynDependency a: add) {
				// if an edge already exists, avoid creating a loop.
				if (SynDependency.findDependencyPath(embeddings, a.getDependent(), a.getGovernor(),true) == null) {
					log.log(Level.FINEST, "Adding dependency {0}.", new Object[]{a.toShortString()});
					embeddings.add(a);		
				} 
			}
		}
	}
	
	/**
	 * Creates a single <code>SpanList</code> from one that may contain individual <code>Span</code> 
	 * objects corresponding to adjacent words 
	 * 
	 * @param sp  the input span list
	 * @return  the single merged span list
	 */
	// TODO may be buggy
	public SpanList mergeSpansIfPossible(SpanList sp) {
		if (sp == null || SpanList.invalidSpan(sp) || SpanList.overlap(new SpanList(span), sp) == false) 
			return null;
		if (sp.size() == 1) return sp;
		List<Span> outSpans = new ArrayList<>();
		Span curr = null;
		int prevWordIndex = -1;
		for (Span ls: sp.getSpans()) {
			List<SurfaceElement> lws = getSurfaceElementsFromSpan(ls);
			// another sentence span, in other words, a GappedMultiWord
			if (lws.size() == 0)  { outSpans.add(ls); continue;} 
			if (curr == null) { 
				curr = new Span(ls.getBegin(),ls.getEnd()); 
				prevWordIndex = lws.get(lws.size()-1).getIndex();
			}
			else {
				int firstIndex = lws.get(0).getIndex();
				if (firstIndex == prevWordIndex + 1) {
					curr.setEnd(lws.get(lws.size()-1).getSpan().getEnd());
				} else if (firstIndex > prevWordIndex + 1) {
					outSpans.add(curr);
					curr = new Span(ls.getBegin(),ls.getEnd()); 
					prevWordIndex = lws.get(lws.size()-1).getIndex();
				}
			}
		}
		if (curr != null) outSpans.add(curr);
		return new SpanList(outSpans);
	}
	
	/**
	 * Logs transformed syntactic dependencies of this sentence.
	 * 
	 * @param type	the transformation name to write to log
	 */
	public void logTransformation(String type) {
		if (embeddings == null) return;
		for (SynDependency sd: embeddings) {
			log.log(Level.FINEST, "After transformation {0}: {1}.", new Object[]{type,sd.toShortString()});
		}
	}
	
	/**
	 * Creates an XML representation of this sentence. 
	 * 
	 * @return an XML representation of this sentence
	 */
	public Element toXml() {
		Element sent = new Element("sentence");
		sent.addAttribute(new Attribute("charOffset",span.toString()));
		sent.addAttribute(new Attribute("id",id));
		Text tex = new Text(text);
		Element textEl = new Element("text");
		textEl.addAttribute(new Attribute("xml:space", 
		          "http://www.w3.org/XML/1998/namespace", "preserve"));
		textEl.appendChild(tex);
		sent.appendChild(textEl);

		if (words != null) {
			Element wordsEl = new Element("tokens");
			int i=1;
			for (Word w: words) wordsEl.appendChild(w.toXml(i++));
			sent.appendChild(wordsEl);
		}
		if (dependencyList != null) { 
			Element depsEl = new Element("dependencies");
			int i =1;
			for (SynDependency sd: dependencyList) {
				depsEl.appendChild(sd.toXml(i++));
			}
			sent.appendChild(depsEl);
		}
		if (tree != null) {
			Element treeEl = new Element("tree");
			treeEl.appendChild(CoreNLPWrapper.printTree(tree));
		    sent.appendChild(treeEl);
		}
		return sent;
	}
	
	public String toString() {
		return document.getId() + "_" + id + "_" + text;
	}
	
}
