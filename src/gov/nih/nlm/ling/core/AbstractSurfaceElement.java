package gov.nih.nlm.ling.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.HasPredicate;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;
import gov.nih.nlm.ling.util.SemUtils;
import gov.nih.nlm.ling.wrappers.WordNetWrapper;

/**
 * The basic implementation of <code>SurfaceElement</code> interface. 
 * 
 * @see Word
 * @see MultiWord
 * @see GappedMultiWord
 * 
 * @author Halil Kilicoglu
 *
 */
public abstract class AbstractSurfaceElement implements SurfaceElement {
	protected String id;
	protected String text;
	protected int index;
	protected String pos;
	protected SpanList span;
	protected LinkedHashSet<SemanticItem> semantics;
	protected Sentence sentence;
	
	public AbstractSurfaceElement() {}
	
	public AbstractSurfaceElement(String id) {
		this.id = id;
	}
	
	public AbstractSurfaceElement(String id, String text) {
		this(id);
		this.text = text;
	}
	
	public AbstractSurfaceElement(String id, String text, SpanList span) {
		this(id,text);
		this.span = span;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public String getPos() {
		return pos;
	}
	public void setPos(String pos) {
		this.pos = pos;
	}
	public Sentence getSentence() {
		return sentence;
	}
	public void setSentence(Sentence sentence) {
		this.sentence = sentence;
	}
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
		
	public SpanList getSpan() {
		return span;
	}
	
	public void setSpan(SpanList span) {
		this.span = span;
	}
	
	public LinkedHashSet<SemanticItem> getSemantics() {
		return semantics;
	}
	
	public void setSemantics(LinkedHashSet<SemanticItem> semantics) {
		this.semantics = semantics;
	}
	
	public void addSemantics(SemanticItem so) {
		if (semantics == null) semantics = new LinkedHashSet<>();
		semantics.add(so);
	}
	
	public void addSemantics(LinkedHashSet<SemanticItem> sos) {
		if (semantics == null) semantics = new LinkedHashSet<>();
		semantics.addAll(sos);
	}
	
	public void removeSemantics(SemanticItem so) {
		if (semantics == null || semantics.size() == 0) return;
		// TODO this stopped working in Java 1.8??
//		semantics.remove(so);
		HashSet<SemanticItem> sems = new HashSet<>(semantics);
		sems.remove(so);
		semantics = new LinkedHashSet<>(sems);
	}
	
	public boolean hasSemantics() {
		if (semantics == null) return false;
		return semantics.size() > 0;
	}
	
	/**
	 * @throws IllegalStateException if the textual unit is not associated with a <code>Sentence</code>.
	 */
	public Document getDocument() {
	    if (sentence == null) {
	        throw new IllegalStateException("Null sentence for surface element " + this.toString());
	    }
	    return sentence.getDocument();
	}

	/**
	 * Returns a higher-level POS category.<p>
	 * It is used to abstract away from tense information, and such.
	 * 
	 * For example, if the POS is 'VBZ', it returns 'VB', if the POS is 'NNS', it returns 'NN'.
	 * 
	 * @return  the category of the textual unit
	 * @throws IllegalStateException if the textual unit does not have a part-of-speech
	 */
	public String getCategory() {
	    if (getPos() == null) {
	        throw new IllegalStateException("Null part-of-speech: " + this.toString());
	    }
		return (getPos().length() == 1 ? getPos(): getPos().substring(0,2));
	}
	
	public LinkedHashSet<SemanticItem> filterSemanticsByClass(Class<? extends SemanticItem> clazz) {
		return filterSemanticsByClass(semantics,clazz);
	}
	
	/**
	 * Gets the semantic items of a given class from a collection of semantic items.<p>
	 * The class of a semantic item needs to be equal to <var>clazz</var> or it should be 
	 * extending or implementing <var>clazz</var>.
	 * 
	 * @param sems the semantic item collection
	 * @param clazz  the semantic class to consider
	 * @return a set of semantic items of the given class
	 */
	public static LinkedHashSet<SemanticItem> filterSemanticsByClass(Collection<SemanticItem> sems, Class<? extends SemanticItem> clazz) {
		LinkedHashSet<SemanticItem> out = new LinkedHashSet<>();
		if (sems == null || sems.size() == 0) return out;
		for (SemanticItem sem: sems) {
			if (sem.getClass().equals(clazz)  || SemUtils.getGeneralizations(sem.getClass()).contains(clazz)) 
				out.add(sem);
		}
		return out;
	}
	
	public LinkedHashSet<SemanticItem> filterByEntities() {
		return filterSemanticsByClass(Entity.class);
	}
	
	public LinkedHashSet<SemanticItem> filterByPredicates() {
		return filterSemanticsByClass(Predicate.class);
	}
	
	public LinkedHashSet<SemanticItem> filterByTerms() {
		return filterSemanticsByClass(Term.class);
	}

	/**
	 * Gets all <code>Relation</code> objects anchored by this textual unit;
	 * i.e., the predicate is associated with this textual unit.
	 * 
	 * @return a set of <code>Relation</code> objects anchored by this textual unit, or empty set if none
	 * @throws IllegalStateException if it is an orphan textual unit (no sentence)
	 * 
	 */
	public LinkedHashSet<SemanticItem> filterByRelations() {
		if (sentence == null) 
			throw new IllegalStateException("Null sentence for surface element " + this.toString());
		LinkedHashSet<SemanticItem> rels = new LinkedHashSet<>();
		LinkedHashSet<SemanticItem> preds = filterByPredicates();
		if (preds == null || preds.size() == 0) return rels;
		for (SemanticItem pred: preds) {
			Set<Relation> relations = Document.getRelationsWithPredicate(getDocument(), (Predicate)pred);
			if (relations != null)
				rels.addAll(relations);
		}
		return rels;
	}
	
	public LinkedHashSet<SemanticItem> getHeadSemantics() {
		LinkedHashSet<SemanticItem> allHeads = new LinkedHashSet<>();
		if (hasSemantics() == false) return allHeads;
		Word head = getHead();
		for (SemanticItem si: semantics) {
			SpanList sp = si.getSpan();
			if (SpanList.overlap(sp, head.getSpan())) allHeads.add(si);
		}
		return allHeads;
	}
	
	/**
	 * Gets the most salient semantic item associated with this textual unit. <p>
	 * This item is typically associated with the head. If no semantics is associated 
	 * with the head, the item with the largest coverage is preferred.
	 * 
	 * @return  the most salient semantic item associated with this textual unit
	 */
	public SemanticItem getMostProminentSemanticItem() {
		if (hasSemantics() == false) return null;
		if (semantics.size() == 1) return semantics.iterator().next();
		Set<SemanticItem> allHeads = getHeadSemantics();
		if (allHeads.size() == 1) return allHeads.iterator().next();
		if (allHeads.size() == 0) return getSemanticItemWithMaxSpan(semantics);
		return getSemanticItemWithMaxSpan(allHeads);
	}
	
	private SemanticItem getSemanticItemWithMaxSpan(Collection<SemanticItem> sems) {
		if (sems.size() == 0) return null;
		SemanticItem max = null;
		int maxLen = Integer.MIN_VALUE;
		for (SemanticItem si: sems) {
			int len = si.getSpan().length();
			if (len > maxLen) {max = si; maxLen = len;}
		}
		return max;
	}
		
	/**
	 * Gets the most complex semantic items associated with this textual unit.<p>
	 * The order of complexity at this stage is: <code>HasPredicate</code> &gt; 
	 * <code>Relation</code> &gt; <code>Term</code>.
	 * 
	 * @return the semantic items of the most complex semantic class
	 */
	public LinkedHashSet<SemanticItem> getMostComplexSemantics() {
		if (semantics == null) return null;
		LinkedHashSet<SemanticItem> rels = filterByRelations();
		LinkedHashSet<SemanticItem> hasPred = new LinkedHashSet<>();
		if (rels.size() > 0) {
			for (SemanticItem rel: rels) {
				if (rel instanceof HasPredicate) hasPred.add(rel);
			}
			if (hasPred.size() > 0) return hasPred;
			return rels;
		}
		return filterByTerms();
	}
	
	public String writeSemantics() {
		StringBuffer buf = new StringBuffer();
		if (this.getSemantics() != null) {
			for (SemanticItem s: this.getSemantics()) {
				buf.append("|" + s.toShortString());
			}
		}
		return buf.toString().trim();
	}
	
	public boolean isAlphanumeric() {
		Pattern p = Pattern.compile("\\w");
		if (p.matcher(getText()).find()) return true;
		return false;
	}
	
	public boolean isVerbal() {
		return getHead().isVerbal();
	}
	
	public boolean isAdjectival() {
		return getHead().isAdjectival();
	}
	
	public boolean isCoordConj() {
		return getHead().isCoordConj();
	}
	
	public boolean isAdverbial() {
		return getHead().isAdverbial();
	}
	
	public boolean isDeterminer() {
		return getHead().isDeterminer();
	}
	
	public boolean isPrepositional() {
		return getHead().isPrepositional();
	}
	
	/**
	 * @return true if this textual unit is a relative pronoun
	 */
	public boolean isRelativePronoun() {
		if (toWordList().size() == 1){
			getHead().isRelativePronoun();
		}
		return false;
	}
	
	/**
	 * A textual unit is nominal if it is a NP, or has a determiner or involves NP dependencies (e.g., amod, nn).
	 * 
	 * @return true if the textual unit is a nominal.
	 */
	public boolean isNominal() {
		boolean headNominal = getHead().isNominal();
		if (headNominal) return true;
		if (toWordList().size() > 0) {
			for (Word w: toWordList()) {
				if (w.isNominal() || w.isDeterminer()) return true;
				if (SynDependency.outDependenciesWithTypes(w, sentence.getDependencyList(), SynDependency.NP_INTERNAL_DEPENDENCIES, true).size() > 0) return true;
			}
		}
		return false;
	}
	
	/** 
	 * A textual unit is a gerund if it is a nominal and the headword ends with "ing".
	 * 
	 * @return true if the unit is a gerund
	 */
	public boolean isGerund() {
		return (isNominal() && getHead().getText().toLowerCase().endsWith("ing"));
	}
	
	/**
	 * Finds whether a textual unit is a nominalized form of an adjective or a verb.<p>
	 * It uses WordNet to get this information, it assumes WordNet has already been initialized.
	 * It will return false if WordNet is not initialized properly.
	 * 
	 * @return  true if the textual unit is a nominalized form
	 */
	public boolean isNominalization() {
		return (WordNetWrapper.getDenominal(this) != null);
	}
	
	/**
	 * Currently, only verbs, nouns, and adjectives are allowed to be predicates. 
	 * 
	 * @return true if this textual unit can act as a predicate.
	 */
	// TODO This is probably too strict.
	public boolean isPredicate() {
		return isVerbal() || isNominal() || isAdjectival();
//		return isVerbal() || isNominal() || isAdjectival() || isAdverbial();
	}
	
	/**
	 * Compares two textual unit based on their spans.
	 */
	public int compareTo(SurfaceElement ase) {
		return getSpan().compareTo(ase.getSpan());
	}
	
	/**
	 * Returns true if this textual unit has semantic content overlapping
	 * with that of another textual unit. 
	 * 
	 * @param other  the textual unit to compare to
	 * 
	 * @return true if this textual unit shares semantics with <var>other</var>
	 */
	// TODO Unused
	public boolean sharesSemanticsWith(SurfaceElement other) {
		return (Document.getOntologyMatches(getDocument(), this)).contains(other);
	}
	
	/**
	 * Returns true if this textual unit has the same surface form as that of another unit.<p>
	 * Having the same surface form means having the same words (with the same lemmas) in the same order.
	 * 
	 * @param e  the textual unit to compare to
	 * 
	 * @return true if the two textual units have the same surface form
	 */
	// TODO Unused
	public boolean sameSurfaceForm(SurfaceElement e) {
		List<Word> words = toWordList();
		List<Word> eWords = e.toWordList();
		if (words == null || eWords == null || words.size() == 0 || eWords.size() == 0 ||
				words.size() != eWords.size() )  return false;
		for (int i=0; i < words.size(); i++ ) {
			String text = words.get(i).getText();
			String etext = eWords.get(i).getText();
			if (text.equalsIgnoreCase(etext)) continue;
			String lemma = words.get(i).getLemma();
			String elemma = eWords.get(i).getLemma();
			if (!(lemma.equalsIgnoreCase(elemma))) return false;
		}
		return true;
	}
	
	public boolean containsToken(String t) {
		List<Word> words = toWordList();
		if (words == null || words.size() == 0) return false;
		for (Word w: words) {
			if (w.getText().equalsIgnoreCase(t)) return true;
		}
		return false;
	}
	
	public boolean containsAnyToken(List<String> tokens) {
		for (String t: tokens) {
			if (containsToken(t)) return true;
		}
		return false;
	}
	
	public boolean containsLemma(String l) {
		List<Word> words = toWordList();
		if (words == null || words.size() == 0) return false;
		for (Word w: words) {
			if (w.getLemma().equalsIgnoreCase(l)) return true;
		}
		return false;
	}
	
	public boolean containsAnyLemma(List<String> lemmas) {
		for (String l: lemmas) {
			if (containsLemma(l)) return true;
		}
		return false;
	}
		
	abstract public Word getHead();
	abstract public List<Word> toWordList();
	abstract public List<Word> toWordList(String cat);
	abstract public boolean isPronominal();

}
