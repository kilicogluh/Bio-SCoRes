package gov.nih.nlm.ling.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.HasPredicate;
import gov.nih.nlm.ling.sem.Ontology;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import gov.nih.nlm.ling.sem.Term;
import gov.nih.nlm.ling.util.SemUtils;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Text;

/**
 * The representation of a basic document, consisting of sentences. <p>
 * The semantic objects, such as terms and relations, are tracked at the document level.
 * A document may also have been segmented into sections.
 * This class includes a {@link SurfaceElementFactory} object used to instantiate textual units
 * and a {@link SemanticItemFactory} to instantiate semantic objects.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Document {
	private final static Logger log = Logger.getLogger(Document.class.getName());

	protected String id;
	protected List<Sentence> sentences;
	protected String text;
	protected Tree documentTree;
	protected Map<Class<? extends SemanticItem>,LinkedHashSet<SemanticItem>> semanticItems;
	protected SurfaceElementFactory surfaceElementFactory;
	protected SemanticItemFactory semanticItemFactory;
	protected List<Section> sections;
	// TODO A generic metadata/feature map could be more appropriate
	protected List<Object> topics;
//	protected List<SpanList> quotationSpans;
	
	/**
	 * Creates a <code>Document</code> object from a piece of text.
	 * 
	 * @param id  the identifier
	 * @param text  the text of the document 
	 */
	public Document(String id, String text) {
		this.id = id;
		this.text = text;
		semanticItems = new HashMap<>();
	}
	
	/**
	 * Creates a <code>Document</code> object from a list of sentences.
	 * 
	 * @param id  the identifier
	 * @param text  the text of the document 
	 * @param sentences the list of sentences, ordered by character offsets
	 */
	public Document(String id, String text, List<Sentence> sentences) {
		this(id,text);
		this.sentences = sentences;
	}
	
	/**
	 * Creates a <code>Document</code> object with the full parse tree.
	 * 
	 * @param id  the identifier
	 * @param text  the text of the document 
	 * @param sentences the list of sentences, ordered by character offsets
	 * @param documentTree  the parse trees of the sentences combined
	 */
	public Document(String id, String text, List<Sentence> sentences, Tree documentTree) {
		this(id,text,sentences);
		this.documentTree = documentTree;
	}
	
	/**
	 * Creates a <code>Document</code> object with semantic objects. <p>
	 * Semantic objects are specified as key/value pairs, where the key is the class of semantic object
	 * (<code>Entity</code>, <code>Predicate</code>, etc.) and the value is the set of semantic objects belonging to that type in
	 * the document. 
	 * 
	 * @param id  the identifier
	 * @param text  the text of the document 
	 * @param sentences the list of sentences, ordered by character offsets
	 * @param documentTree  the parse trees of the sentences combined
	 * @param semanticItems  a Map of semantic objects.
	 */
	public Document(String id, String text, List<Sentence> sentences, Tree documentTree, 
			Map<Class<? extends SemanticItem>, LinkedHashSet<SemanticItem>> semanticItems) {
		this(id,text,sentences,documentTree);
		this.semanticItems = semanticItems;
	}
	
	/** 
	 * Creates a<code>Document</code> from an XML element. <p>
	 * The lexical and syntactic information is parsed, but not semantic annotations.
	 * 
	 * @param el  the XML element corresponding to this document
	 */
	public Document(Element el) {
		id = el.getAttributeValue("id");
		text =  el.getChildElements("text").get(0).getValue().trim();
		Elements sentEls = el.getChildElements("sentence");
		List<Sentence> extractedSentences = new ArrayList<>();
		TreeFactory factory = new LabeledScoredTreeFactory();
		Tree docTree = factory.newTreeNode("DOCROOT", new ArrayList<Tree>());
		Tree tmpTree1 = docTree;
		Tree tmpTree2;
		for (int i=0; i < sentEls.size(); i++) {
			Element sentEl = sentEls.get(i);
			Sentence sent = new Sentence(sentEl);
			sent.setDocument(this);
			extractedSentences.add(sent);
			tmpTree1.addChild(sent.getTree());
			if(i<sentEls.size()-1){ 
				tmpTree2 = factory.newTreeNode("DOCROOT", new ArrayList<Tree>());
				tmpTree1.addChild(tmpTree2);
				tmpTree1 = tmpTree2;
			}
		}
		sentences = extractedSentences;
		documentTree = docTree;
	}
	
	/**
	 * Similar to {@code #Document(Element)}, but allows selecting a particular tokenizer/parser 
	 * output for syntactic analysis of the sentence.
	 * 
	 * @param el  the XML element corresponding to this document
	 * @param tokenizer  the tokenizer name 
	 * @param parser  the parser name
	 */
	public Document(Element el, String tokenizer, String parser) {
		id = el.getAttributeValue("id");
		text =  el.getChildElements("text").get(0).getValue();
		Elements sentEls = el.getChildElements("sentence");
		List<Sentence> extractedSentences = new ArrayList<>();
		TreeFactory factory = new LabeledScoredTreeFactory();
		Tree docTree = factory.newTreeNode("DOCROOT", new ArrayList<Tree>());
		Tree tmpTree1 = docTree;
		Tree tmpTree2;
		for (int i=0; i < sentEls.size(); i++) {
			Element sentEl = sentEls.get(i);
			Sentence sent = new Sentence(sentEl, tokenizer, parser);
			sent.setDocument(this);
			extractedSentences.add(sent);
			tmpTree1.addChild(sent.getTree());
			if(i<sentEls.size()-1){ 
				tmpTree2 = factory.newTreeNode("DOCROOT", new ArrayList<Tree>());
				tmpTree1.addChild(tmpTree2);
				tmpTree1 = tmpTree2;
			}
		}
		sentences = extractedSentences;
		documentTree = docTree;
	}
	
	public String getId() {
		return id;
	}

	public Tree getDocumentTree() {
		return documentTree;
	}
	
	public Map<Class<? extends SemanticItem>, LinkedHashSet<SemanticItem>> getSemanticItems() {
		return semanticItems;
	}
	
	public void setSemanticItems(Map<Class<? extends SemanticItem>, LinkedHashSet<SemanticItem>> semanticItems) {
		this.semanticItems = semanticItems;
	}
	
	public List<Sentence> getSentences() {
		return sentences;
	}

	public void setSentences(List<Sentence> sentences) {
		this.sentences = sentences;
	}
	
	public void addSentence(Sentence sentence) {
		if (sentences == null) sentences = new ArrayList<>();
		sentences.add(sentence);
	}

	public String getText() {
		return text;
	}
	
	public List<Section> getSections() {
		return sections;
	}

	public void setSections(List<Section> sections) {
		this.sections = sections;
	}
	
	public void addSection(Section section) {
		if (sections == null) sections = new ArrayList<>();
		sections.add(section);
	}
	
	public List<? extends Object> getTopics() {
		return topics;
	}

	public void setTopics(List<Object> topics) {
		this.topics = topics;
	}
	
	public void addTopic(Object topic) {
		if (topics == null) topics = new ArrayList<>();
		topics.add(topic);
	}
	
	/**
	 * Gets the <code>SurfaceElementFactory</code> associated with this document, 
	 * or creates one if it does not exist.
	 * 
	 * @return the <code>SurfaceElementFactory</code> object for this document.
	 */
	public SurfaceElementFactory getSurfaceElementFactory() {
		if (surfaceElementFactory == null) surfaceElementFactory = new SurfaceElementFactory();
		return surfaceElementFactory;
	}
	
	/**
	 * Gets the <code>SemanticItemFactory</code> associated with this document, or 
	 * create a default one if necessary and initializes the id counter.
	 * 
	 * @return  the <code>SemanticItemFactory</code> object for this document.
	 */
	public SemanticItemFactory getSemanticItemFactory() {
		if (semanticItemFactory == null) 	{
			Map<Class<? extends SemanticItem>,Integer> counter = new HashMap<>();
			if (semanticItems != null) {
				for (Class<? extends SemanticItem> k: semanticItems.keySet()) {
					int size = semanticItems.get(k).size();
					counter.put(k, size);
				}
			}
			semanticItemFactory = new SemanticItemFactory(this,counter);
		}
		return semanticItemFactory;
	}
	
	/**
	 * Sets the semantic item factory for this document.<p>
	 * This can be used when a class that extends the generic <code>SemanticItemFactory</code>
	 * needs to be create new types of semantic items.
	 * 
	 * @param sif	a semantic item factory
	 */
	public void setSemanticItemFactory(SemanticItemFactory sif) {
		this.semanticItemFactory = sif;
	}
	
	/**
	 * Gets all textual units of this document.
	 * 
	 * @return the list of all textual units of this document in the order they appear.
	 * 
	 * @throws IllegalStateException if the document has not been split into sentences already.
	 */
	public List<SurfaceElement> getAllSurfaceElements() {
		if (sentences == null) 
			throw new IllegalStateException("The sentences of the document  " + id + " are unknown.");
		List<SurfaceElement> all = new ArrayList<>();
		for (Sentence s: sentences) {
			List<SurfaceElement> surfs = s.getSurfaceElements();
			if (surfs == null) continue;
			all.addAll(surfs);
		}
		return all;
	}
	
	/**
	 * Gets all the semantic items of this documents. 
	 * 
	 * @return the set of all semantic items of this document.
	 */
	public LinkedHashSet<SemanticItem> getAllSemanticItems() {
		LinkedHashSet<SemanticItem> out = new LinkedHashSet<>();
		if (semanticItems == null) return out;
		for (Class<? extends SemanticItem> s: semanticItems.keySet()) {
			Set<SemanticItem> sems = semanticItems.get(s);
			out.addAll(sems);
		}
		 return out;
	}
	
	/**
	 * Adds a single semantic item to this document.
	 * 
	 * @param semanticItem  the semantic item to add.
	 */
	public void addSemanticItem(SemanticItem semanticItem){
		if (semanticItems == null) { semanticItems = new HashMap<>();}
		Class<? extends SemanticItem> s = semanticItem.getClass();
		LinkedHashSet<SemanticItem> objs = this.semanticItems.get(s);
		if (objs == null) {
			objs = new LinkedHashSet<>();
		}
		objs.add(semanticItem);
		this.semanticItems.put(s,objs);
	}
	
	/**
	 * Adds multiple semantic items to this document.
	 * 
	 * @param semanticItems  the semantic item collection to add.
	 */
	public void addSemanticItems(Collection<SemanticItem> semanticItems) {
		for (SemanticItem s: semanticItems) {
			addSemanticItem(s);
		}
	}
	
	/**
	 * Removes all semantics associated with this document and resets the underlying sentences 
	 * (i.e., remove the multi-word expression information).
	 * 
	 */
	public synchronized void removeAllSemantics() {
		for (SemanticItem sem: getAllSemanticItems()) {
			removeSemanticItem(sem);
		}
		setSemanticItems(new HashMap<Class<? extends SemanticItem>,LinkedHashSet<SemanticItem>>());
		for (Sentence s: sentences) {
			s.reset();
		}
	}
	
	/**
	 * Removes a single semantic item as well as the associated semantic items from this document.<p>
	 * For example, if a <code>Predicate</code> is being removed, all the <code>Relation</code>s
	 * associated with it are also removed. The textual units associated with the removed semantic items
	 * are updated as well.
	 * 
	 * @param semanticItem  the semantic item to remove from this document
	 */
	public synchronized void removeSemanticItem(SemanticItem semanticItem) {
		if (semanticItems == null || semanticItems.size() == 0) return;
		Map<Class<? extends SemanticItem>,LinkedHashSet<SemanticItem>> toRemove = new HashMap<>();
		for (Class<? extends SemanticItem> s: semanticItems.keySet()) {
			if (s.isInstance(semanticItem) == false) continue;
			if (semanticItem instanceof Term) {
				// TODO LinkedHashSet.contains() stopped working in Java 1.8??
				Set<SemanticItem> set = new HashSet<>(semanticItems.get(s));
				if (set.contains(semanticItem)) {
					LinkedHashSet<SemanticItem> removeType = toRemove.get(s);
					if (removeType == null) removeType = new LinkedHashSet<>();
					removeType.add(semanticItem);
					toRemove.put(s, removeType);
				}
			}
			else {
				Set<SemanticItem> set = semanticItems.get(s);
				for (SemanticItem se: set) {
					// Add the predicate to removeList
					if (se instanceof HasPredicate) {
						Predicate p = ((HasPredicate)se).getPredicate();
						if (p.equals(semanticItem)) {
							LinkedHashSet<SemanticItem> removeType = toRemove.get(s);
							if (removeType == null) removeType = new LinkedHashSet<>();
							removeType.add(se);
							toRemove.put(s,removeType);
							LinkedHashSet<SemanticItem> removePred = toRemove.get(Predicate.class);
							if (removePred == null) removePred = new LinkedHashSet<>();
							removePred.add(p);
							toRemove.put(Predicate.class,removePred);
						} 
					}
					// If the semantic item to remove is an argument of a relation, add the relation to removeList
					if (se instanceof Relation) {
						Relation rel = (Relation)se;
						Argument removeArg = null;
						for (int i=0; i< rel.getArguments().size(); i++) {
							Argument arg = rel.getArguments().get(i);
							SemanticItem argSe = arg.getArg();
							if (argSe.equals(semanticItem)) {
								removeArg = arg;
								break;
							}
						}
						if (removeArg != null) rel.removeArg(removeArg);
						if (rel.equals(semanticItem)) {
							LinkedHashSet<SemanticItem> alreadyRemove = toRemove.get(s);
							if (alreadyRemove == null) alreadyRemove = new LinkedHashSet<>();
							alreadyRemove.add(rel);
							toRemove.put(s,alreadyRemove);
						}
					}
				}
			}
		}
		if (semanticItem instanceof Term) {
			AbstractSurfaceElement surf = (AbstractSurfaceElement)((Term)semanticItem).getSurfaceElement();
			surf.removeSemantics(semanticItem);
//			surf.getSemantics().remove(semanticItem);
		}
		if (semanticItem instanceof HasPredicate) {
			Predicate p = ((HasPredicate)semanticItem).getPredicate();
			AbstractSurfaceElement surf = (AbstractSurfaceElement)p.getSurfaceElement();
			surf.removeSemantics(p);
			surf.removeSemantics(semanticItem);
//			surf.getSemantics().remove(p);
//			surf.getSemantics().remove(semanticItem);
		}
		Map<Class<? extends SemanticItem>,LinkedHashSet<SemanticItem>> updatedMap = 
				new HashMap<Class<? extends SemanticItem>,LinkedHashSet<SemanticItem>>();
		for (Class<? extends SemanticItem> s: semanticItems.keySet()) {
			LinkedHashSet<SemanticItem> removeSet = toRemove.get(s);
			LinkedHashSet<SemanticItem> set = new LinkedHashSet<SemanticItem>(semanticItems.get(s));
			if (removeSet == null)  {
				updatedMap.put(s, set);
			}
			else {
				set.removeAll(removeSet);
				updatedMap.put(s, set);
			}
		}
		this.semanticItems = updatedMap;
	}
	
	/**
	 * For a given semantic class (or its subclasses), finds the max id in this document so that
	 * the counter can be updated accordingly. It assumes that ids have the format
	 * [A-Z][0-9]+ where the second half is the id. Returns 0 if there are no semantic items
	 * of the given semantic class.
	 * 
	 * @param clazz  the semantic class
	 * @return  the max id
	 * 
	 */
	public int getMaxId(Class<? extends SemanticItem> clazz) {
		if (semanticItems == null) return 0;
		LinkedHashSet<SemanticItem> semObjects = getSemanticItemsByClass(this,clazz);
		if (semObjects.size() == 0) return 0;
		int max = 0;
		for (SemanticItem sem: semObjects) {
			String id = sem.getId();
			int n = Integer.parseInt(id.substring(1));
			if (n > max) max = n;
		}
		return max;
	}
	
	/**
	 * Gets the semantic object with the given id. 
	 * Returns null if no semantic item with the given id is found. 
	 * 
	 * @param semId  the identifier
	 * @return  the semantic object with the given id
	 */
	public SemanticItem getSemanticItemById(String semId) {
		if (semanticItems == null) return null;
		for (Class<? extends SemanticItem> key: semanticItems.keySet()){
			SemanticItem obj = getSemanticItemByTypeId(key,semId);
			if (obj != null) return obj;
		}
		log.log(Level.WARNING,"Cannot find semantic item {0} in document {1}", new Object[]{semId,id});
		return null;
	}
	
	private SemanticItem getSemanticItemByTypeId(Class<? extends SemanticItem> type, String semId) {
		LinkedHashSet<SemanticItem> objs = semanticItems.get(type);
		for (SemanticItem o: objs){
			if (o.getId().equals(semId)) return o;
		}
		return null;
	}
	
	/**
	 * Gets semantic items associated with a semantic class (<code>Entity</code>, <code>Event</code>, etc.).
	 * <p>
	 * If the class name provided is an interface or superclass name, it attempts to identify 
	 * the implementing/extending classes and the instances belonging to these classes. 
	 * For example, it will return both <code>Entity</code> and <code>Predicate</code> objects
	 * when the class name provided is <code>Term</code>. 
	 * 
	 * @param doc  the document with the semantic items
	 * @param clazz  the semantic class name
	 * @return  the set of semantic items of the given class, empty set if there are none or if the key is invalid
	 */
	// TODO It is possible to make this method and other related ones below non-static.
	public static LinkedHashSet<SemanticItem> getSemanticItemsByClass(Document doc, Class<? extends SemanticItem> clazz) {
		if (doc.getSemanticItems() == null) return new LinkedHashSet<>();
		LinkedHashSet<SemanticItem> sems = doc.getSemanticItems().get(clazz);
		if (sems != null) return sems;
		sems = new LinkedHashSet<>();
		for (Class<? extends SemanticItem> k: doc.getSemanticItems().keySet()) {
			if (SemUtils.getGeneralizations(k).contains(clazz)) {
				sems.addAll(doc.getSemanticItems().get(k));
			}
		}
		return sems;
	}

	/**
	 * Finds all semantic items of a given semantic class in a given text span. <p>
	 * A specialization of {@link #getSemanticItemsByClass(Document, Class)}. 
	 * For <code>Relation</code> objects, it will only consider those whose arguments and predicate, if any,
	 * fall within the span.
	 * 
	 * @param doc  the document with the semantic items
	 * @param clazz  the semantic class
	 * @param sp  the text span subsuming the semantic items
	 * @param allowOverlap whether to include the semantic items that only overlap with the text span and are not subsumed by it
	 * 
	 * @return  the semantic items in the given span, empty set if no semantic objects with the given class 
	 * 
	 */
	public static LinkedHashSet<SemanticItem> getSemanticItemsByClassSpan(Document doc, Class<? extends SemanticItem> clazz, 
			SpanList sp, boolean allowOverlap) {
		LinkedHashSet<SemanticItem> cl = getSemanticItemsByClass(doc,clazz);
		LinkedHashSet<SemanticItem> spItems = new LinkedHashSet<>();
		for (SemanticItem si: cl) {
			SpanList sisp = si.getSpan();
			if (si instanceof Relation) {
				sisp = new SpanList(si.getSpan().asSingleSpan());
			}
			if (SpanList.subsume(sp, sisp)|| (allowOverlap && SpanList.overlap(sisp, sp))) spItems.add(si);
		}
		return spItems;
	}
	
	/**
	 * Finds all semantic items of a given semantic class and a given list of semantic types.<p>
	 * A specialization of {@link #getSemanticItemsByClass(Document, Class)}. 
	 * 
	 * @param doc  the document with the semantic items
	 * @param key  the semantic class
	 * @param types  a list of semantic types
	 * 
	 * @return  the semantic items of the given class and semantic types
	 */
	public static LinkedHashSet<SemanticItem> getSemanticItemsByClassType(Document doc, Class<? extends SemanticItem> key, List<String> types) {
		LinkedHashSet<SemanticItem> cl = getSemanticItemsByClass(doc,key);
		LinkedHashSet<SemanticItem> out = new LinkedHashSet<>();
		for (SemanticItem c: cl) {
			if (types.contains(c.getType())) out.add(c);
		}
		return out;
	}
	
	/**
	 * Similar to {@link #getSemanticItemsByClassType(Document, Class, List)}, 
	 * but also filters by text span and may allow overlap.
	 * 
	 * @param doc			the document to query
	 * @param clazz			the semantic object class
	 * @param types			the semantic types
	 * @param sp			the span to query
	 * @param allowOverlap	whether span overlap is allowed
	 * 
	 * @return  the semantic items with the given class/semantic types within a span
	 */
	public static LinkedHashSet<SemanticItem> getSemanticItemsByClassTypeSpan(Document doc, Class<? extends SemanticItem> clazz, List<String> types, 
			SpanList sp, boolean allowOverlap) {
		LinkedHashSet<SemanticItem> spItems = new LinkedHashSet<>();
		if (doc.getSemanticItems() == null) return spItems;
		LinkedHashSet<SemanticItem> classTypeItems = getSemanticItemsByClassType(doc, clazz, types);
		if (classTypeItems == null) return spItems;
		for (SemanticItem si: classTypeItems) {
			SpanList sisp = si.getSpan();
			if (si instanceof Relation) {
				sisp = new SpanList(si.getSpan().asSingleSpan());
			}
			if (SpanList.subsume(sp, sisp) || (allowOverlap && SpanList.overlap(sisp, sp))) spItems.add(si);
		}
		return spItems;
	}
	
	/**
	 * Finds all semantic items, regardless of class, within a text span.<p>
	 * For <code>Relation</code> objects, it will only consider those whose arguments and predicate, if any,
	 * fall within the span.
	 * 
	 * @param doc 			the document to query
	 * @param sp			the span to query
	 * @param allowOverlap 	whether to allow overlap with the text span
	 * 
	 * @return  all semantic items within a given span
	 */
	public static LinkedHashSet<SemanticItem> getSemanticItemsBySpan(Document doc, SpanList sp, boolean allowOverlap) {
		LinkedHashSet<SemanticItem> allItems = doc.getAllSemanticItems();
		LinkedHashSet<SemanticItem> spItems = new LinkedHashSet<>();
		for (SemanticItem si : allItems) {
			SpanList sisp = si.getSpan();
			if (si instanceof Relation) {
				sisp = new SpanList(si.getSpan().asSingleSpan());
			}
			if (SpanList.subsume(sp, sisp) || (allowOverlap && SpanList.overlap(sisp, sp))) spItems.add(si);
		}
		return spItems;
	}

	
	/**
	 * Gets all semantic relations with the given predicate in a document
	 * 
	 * @param doc  the document
	 * @param predicate  the predicate 
	 * @return  the set of <code>Relation</code>-implementing semantic items with the given predicate
	 */
	public static LinkedHashSet<Relation> getRelationsWithPredicate(Document doc, Predicate predicate) {
		LinkedHashSet<SemanticItem> rels = getSemanticItemsByClass(doc,Relation.class);
		LinkedHashSet<Relation> out = new LinkedHashSet<>();
		if (rels == null) return out;
		for (SemanticItem rel: rels) {
			Relation hp = (Relation)rel;
			if (hp instanceof HasPredicate &&
					((HasPredicate)hp).getPredicate().equals(predicate)) out.add(hp);
		}
		return out;
	}
	
	/**
	 * Gets all semantic relations that take the given semantic item as argument in a given document
	 * 
	 * @param doc  the document
	 * @param sem  the semantic item in argument role
	 * @return the set of <code>Relation</code>-implementing items with the given argument
	 */
	public static LinkedHashSet<Relation> getRelationsWithArgument(Document doc, SemanticItem sem) {
		LinkedHashSet<SemanticItem> rels = getSemanticItemsByClass(doc,Relation.class);
		LinkedHashSet<Relation> out = new LinkedHashSet<>();
		if (rels == null) return out;
		for (SemanticItem s: rels) {
			Relation rel = (Relation)s;
			List<Argument> args = rel.getArguments();
			for (Argument arg: args) {
				if (arg.getArg().equals(sem)) { 
					out.add(rel);
					break;
				}
			}
		}
		return out;
	}

	/**
	 * Returns all concepts and their counts from this document
	 * 
	 * @return  a map of concept/count key/value pairs
	 */
	public Map<Ontology,Integer> getOntologyCounts() {
		Map<Ontology,Integer> counts = new HashMap<>();
		for (Class<? extends SemanticItem> s: semanticItems.keySet()) {
			counts.putAll(getOntologyCounts(s));
		}
		return counts;
	}

	/**
	 * Returns concept mapping counts from this document for a given semantic class
	 * 
	 * @param type  the semantic class to consider 
	 * @return  a map of concept/count key/value pairs
	 */
	public Map<Ontology,Integer> getOntologyCounts(Class<? extends SemanticItem> type) {
		LinkedHashSet<SemanticItem> typed = getSemanticItemsByClass(this,type);
		List<SemanticItem> seen = new ArrayList<>();
		Map<Ontology,Integer> counts = new HashMap<>();
		for (SemanticItem si: typed) {
			if (seen.contains(si)) continue;
			int cnt = 0;
			for (SemanticItem ssi: typed) {
				if (ssi.equals(si)) continue;
				if (ssi.ontologyEquals(si)) {
					cnt++;
					seen.add(ssi);
				}
			}
			counts.put(si.getOntology(), ++cnt);
			seen.add(si);
		}
		return counts;
	}
	
	/**
	 * Groups all textual units by the concepts they map to
	 * 
	 * @return a map of concept/textual units key/value pairs
	 */
	public Map<Ontology,LinkedHashSet<SurfaceElement>> getOntologyTerms() {
		Map<Ontology,LinkedHashSet<SurfaceElement>> map = new HashMap<>();
		for (Class<? extends SemanticItem> s: semanticItems.keySet()) {
			map.putAll(getOntologyTerms(s));
		}
		return map;
	}
	
	/**
	 * Finds all the conceptually equivalent textual units within the document.
	 * This method only considers a specific semantic class.
	 * 
	 * @param clazz  the semantic item class
	 * @return  a map of concept/textual units for the given semantic item class 
	 */
	// TODO May consider making it private
	public Map<Ontology,LinkedHashSet<SurfaceElement>> getOntologyTerms(Class<? extends SemanticItem> clazz) {
		LinkedHashSet<SemanticItem> typed = getSemanticItemsByClass(this,clazz);
		List<SemanticItem> seen = new ArrayList<>();
		Map<Ontology,LinkedHashSet<SurfaceElement>> terms = new HashMap<>();
		for (SemanticItem si: typed) {
			if (seen.contains(si)) continue;
			LinkedHashSet<SurfaceElement> surfs = new LinkedHashSet<SurfaceElement>();
			for (SemanticItem ssi: typed) {
				if (ssi.equals(si)) continue;
				if (ssi.ontologyEquals(si)) {
					if (ssi instanceof Term) {
						Term tssi = (Term)ssi;
						surfs.add(tssi.getSurfaceElement());
					}
					seen.add(ssi);
				}
			}
			terms.put(si.getOntology(), surfs);
			seen.add(si);
		}
		return terms;
	}
	
	/**
	 * Given a textual unit, finds all conceptually equivalent textual units within the document
	 * 
	 * @param doc  the document of the textual unit
	 * @param surf  the input textual unit  
	 * @return  the textual units within the document that map to the same concept, null if 
	 * 			the textual unit has no semantics
	 */
	public static LinkedHashSet<SurfaceElement> getOntologyMatches(Document doc, SurfaceElement surf) {
		LinkedHashSet<SemanticItem> sems = surf.getSemantics();
		LinkedHashSet<SurfaceElement> out = new LinkedHashSet<>();
		if (sems == null) return out;
		for (SemanticItem sem: sems) {
			LinkedHashSet<SemanticItem> allMatches = getOntologyMatches(doc,sem);
			for (SemanticItem a: allMatches) {
				if (a instanceof Term == false) continue;
				Term ta = (Term)a;
				out.add(ta.getSurfaceElement());
			}
		}
		return out;
	}
	
	/**
	 * Given a semantic item, finds all conceptually equivalent semantic items within the document
	 * 
	 * @param doc  the document of the semantic item
	 * @param sem  the input semantic item
	 * @return  the semantic items within the document that are normalized to the same concept
	 */
	public static LinkedHashSet<SemanticItem> getOntologyMatches(Document doc, SemanticItem sem) {
		LinkedHashSet<SemanticItem> allSems = doc.getAllSemanticItems();
		LinkedHashSet<SemanticItem> out = new LinkedHashSet<>();
		for (SemanticItem a: allSems) {
			if (a.equals(sem)) continue;
			if (a.ontologyEquals(sem)) {
				out.add(a);
			}
		}
		return out;
	}
	
	/**
	 * Calculates the distance of two textual units within this document.
	 * 
	 * @param s1  the first textual unit
	 * @param s2  the second textual unit
	 * @return  the distance between the textual units
	 * @throws IllegalStateException if the sentences are not known
	 */
	// TODO Unused
	public int linearDistance(SurfaceElement s1, SurfaceElement s2) {
		if (sentences == null) 
			throw new IllegalStateException("The sentences of the document  " + id + " are unknown.");
		int dist = -1;
		SurfaceElement other = null;
		boolean done = false;
		for (Sentence s: sentences) {
			for (SurfaceElement su: s.getSurfaceElements()) {
				if (su.equals(s1)) { 
					if (other == null) {other = s2; dist = 0;}
					else {dist++; done = true;}
				}
				else if (su.equals(s2)) { 
					if (other == null) { other = s1; dist = 0;}
					else {dist++; done = true;}
				}
				else if (other != null) dist++;
				if (done) break;
			}
			if (done) break;
		}
		return dist;
	}
			
	/**
	 * Gets the sentence that subsumes the given span.<p>
	 * If the span partially overlaps with the sentence, the sentence is still returned.
	 * 
	 * @param sp  the span of the text 
	 * @return  the sentence that subsumes/overlaps with the span, or null if the document length is smaller than the given span
	 * @throws IllegalStateException if the sentences are not known
	 */
	public Sentence getSubsumingSentence(Span sp) {
		if (sentences == null) 
			throw new IllegalStateException("The sentences of the document  " + id + " are unknown.");
		for (Sentence s: sentences) {
//			String st = s.getStringInSpan(sp);
//			if (st != null) return s;
			if (Span.overlap(sp, s.getSpan())) {
				return s;
			}
		}
		return null;
	}
	
	/**
	 * Gets the textual units in a given span. 
	 * 
	 * @param sp  the span
	 * @return the list of textual units in the span
	 * @throws IllegalStateException if the sentences are not known
	 */
	public List<SurfaceElement> getSurfaceElementsInSpan(Span sp) {
		if (sentences == null) 
			throw new IllegalStateException("The sentences of the document  " + id + " are unknown.");
		List<SurfaceElement> out = new ArrayList<>();
		for (int i=0; i< sentences.size(); i++) {
			Sentence sent = sentences.get(i);
			if (Span.overlap(sent.getSpan(),sp)) {
				Span intersect = Span.intersection(sent.getSpan(),sp);
				out.addAll(sent.getSurfaceElementsFromSpan(intersect));
			}
		}
		return out;
	}
	
	/**
	 * Similar to {@code #getSurfaceElementsInSpan(Span)}, but takes a span list as argument.
	 * 
	 * @param sp  the spans
	 * @return  the list of textual units in the span list
     * @throws IllegalStateException if the sentences are not known
	 */
	public List<SurfaceElement> getSurfaceElementsInSpan(SpanList sp) {
		if (sentences == null) 
			throw new IllegalStateException("The sentences of the document  " + id + " are unknown.");
		List<SurfaceElement> surfs = new ArrayList<>();
		for (Span s: sp.getSpans()) {
			surfs.addAll(getSurfaceElementsInSpan(s));
		}
		return surfs;
	}
	
	/**
	 * Gets the words in a given span.
	 * 
	 * @param sp  the span
	 * @return  the list of words in the span
	 * @throws IllegalStateException if the sentences are not known
	 */
	public List<Word> getWordsInSpan(Span sp) {
		if (sentences == null) 
			throw new IllegalStateException("The sentences of the document  " + id + " are unknown.");
		List<Word> out = new ArrayList<>();
		for (int i=0; i< sentences.size(); i++) {
			Sentence sent = sentences.get(i);
			if (Span.overlap(sent.getSpan(),sp)) {
				Span intersect = Span.intersection(sent.getSpan(),sp);
				out.addAll(sent.getWordsInSpan(intersect));
			}
		}
		return out;
	}
	
	/**
	 * Similar to {@code #getWordsInSpan(Span)}, but takes a span list as argument.
	 * 
	 * @param sp  the spans
	 * @return  the list of words in the span list
	 * @throws IllegalStateException if the sentences are not known
	 */
	public List<Word> getWordsInSpan(SpanList sp) {
		if (sentences == null) 
			throw new IllegalStateException("The sentences of the document  " + id + " are unknown.");
		List<Word> words = new ArrayList<>();
		for (Span s: sp.getSpans()) {
			words.addAll(getWordsInSpan(s));
		}
		return words;
	}
	
	
	/**
	 * Returns the words in a given span. Limited to the words in the first sentence.
	 * 
	 * @param sp  the span
	 * @return  a list of words in the span
	 */
	// TODO: Call to these methods should be replaced by those to getWordsInSpan and these should go.
/*	public List<Word> getWordsInSentenceSpan(Span sp) {
		Sentence sent = getSubsumingSentence(sp);
		return sent.getWordsInSpan(sp);
	}*/
	
	/**
	 * Similar to {@code #getWordsInSentenceSpan(Span)}, but takes a span list as argument.
	 * 
	 * @param sp  the spans
	 * @return  the list of words in the span list
	 */
/*	public List<Word> getWordsInSentenceSpan(SpanList sp) {
		List<Word> words = new ArrayList<Word>();
		for (Span s: sp.getSpans()) {
			words.addAll(getWordsInSentenceSpan(s));
		}
		return words;
	}*/
	
	/**
	 * Gets the text within a given span of this document.
	 * 
	 * @param sp  the span of the text 
	 * @return  the text in the span
	 */
	public String getStringInSpan(Span sp) {
		return text.substring(sp.getBegin(), sp.getEnd());
	}
	
	/**
	 * Similar to {@code #getStringInSpan(Span)}, but takes a span list as an argument.<p>
	 * The texts will be delimited by {@code SpanList#SEPARATOR}. 
	 * 
	 * @param sp  the span of the text 
	 * @return  the text in the span
	 */
	public String getStringInSpan(SpanList sp) {
		List<Span> spans = sp.getSpans();
		String first = getStringInSpan(spans.get(0));
		if (spans.size() == 1) return first;
		StringBuffer buf = new StringBuffer();
		buf.append(first);
		for (int i=1;i<spans.size();i++) {
			buf.append(SpanList.SEPARATOR + getStringInSpan(spans.get(i)));
		}
		return buf.toString();
	}	
	
	/**
	 * Gets the individual tokens intervening between two textual units. 
	 * The order of the textual units in text is not important.
	 * 
	 * @param a the first textual unit
	 * @param b the second textual unit
	 * @return the list of tokens between the textual units, or empty list if they are adjacent
	 * 
	 */
	public List<Word> getInterveningWords(SurfaceElement a, SurfaceElement b) {
		if (a.equals(b)) return new ArrayList<>();
		SpanList as = a.getSpan();
		SpanList bs = b.getSpan();
		SpanList left = null; SpanList right = null;
		if (SpanList.atLeft(bs, as)) {left = bs; right=as;}
		else {left=as;right=bs;}
		Span between = new Span(left.getEnd(),right.getBegin());
		return getWordsInSpan(between);
	}
	
	/**
	 * Similar to {@code #getInterveningWords(SurfaceElement, SurfaceElement)}, but
	 * will get a list of textual units, rather than individual tokens.
	 * 
	 * @param a the first textual unit
	 * @param b the second textual unit
	 * @return the list of tokens between the textual units, or empty list if they are adjacent
	 * 
	 */
	public List<SurfaceElement> getInterveningSurfaceElements(SurfaceElement a, SurfaceElement b) {
		if (a.equals(b)) return new ArrayList<>();
		SpanList as = a.getSpan();
		SpanList bs = b.getSpan();
		SpanList left = null; SpanList right = null;
		if (SpanList.atLeft(bs, as)) {left = bs; right=as;}
		else {left=as;right=bs;}
		Span between = new Span(left.getEnd(),right.getBegin());
		return getSurfaceElementsInSpan(between);
	}
	
	/**
	 * Returns the XML representation of this document. <p>
	 * Semantic items are children of the document node, independent of sentences.
	 * 
	 * @return  the XML representation
	 */
	public Element toXml() {
		Element docc = new Element("document");
		docc.addAttribute(new Attribute("id",id));
		Text tex = new Text(text);
		Element textEl = new Element("text");
		textEl.addAttribute(new Attribute("xml:space", 
		          "http://www.w3.org/XML/1998/namespace", "preserve"));
		textEl.appendChild(tex);
		docc.appendChild(textEl);
		if (sentences != null) {
			for (Sentence s: sentences) {
				Element sEl = s.toXml();
				docc.appendChild(sEl);
			}
		}
		if (semanticItems != null) {
			for (Class<? extends SemanticItem> key: semanticItems.keySet()) { 
				Set<SemanticItem> objs = semanticItems.get(key);
				for (SemanticItem s: objs) {
					docc.appendChild(s.toXml());
				}
			}
		}
		return docc;
	}
	public String toString() {
		return id + "_" + text;
	}
	
/*	public List<SpanList> getQuotationSpans() {
		if (quotationSpans == null) quotationSpans = findQuotationSpans();
		return quotationSpans;
	}
	
	public void addQuotationSpan(SpanList sp) {
		if (quotationSpans == null) quotationSpans = new ArrayList<SpanList>();
		quotationSpans.add(sp);
	}

	public void setQuotationSpans(List<SpanList> quotationSpans) {
		this.quotationSpans = quotationSpans;
	}*/
	
	/* 
	 * This should also consider gapped quotations. See if there are heuristics
	 * that could be used for this.
	 */
/*	public List<SpanList> findQuotationSpans() {
		String[] doubleQuoted = text.split("[\\\"]");
		List<SpanList> spans = null;
		if (doubleQuoted.length >3) {
			int st = 0;
			spans = new ArrayList<SpanList>();
			for (int i=1; i< doubleQuoted.length; i=i+2) {
				st += doubleQuoted[i-1].length() +1;
				int end = st + doubleQuoted[i].length()+1;
				LingSpan sp = new LingSpan(st,end);
				spans.add(new SpanList(sp));
				st = end;
			}
		}
		return spans;
	}*/


}
