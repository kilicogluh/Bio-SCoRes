package gov.nih.nlm.ling.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.GappedMultiWord;
import gov.nih.nlm.ling.core.MultiWord;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.core.WordLexeme;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.SemanticItem;


/**
 * A <code>DependencyTransformation</code> implementation that splits complex, hyphenated
 * adjectives that appear often in biomedical text. Such adjectives (e.g., <i>PARK7-induced</i>)
 * include both entity and predicates. The goal here is to split them such that we can
 * construct relations with the entity and the predicate. <p> 
 * Splitting existing surface elements can be problematic, so we need to proceed with caution. 
 * This transformation does not have any prerequisites.
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO This class still needs much work.
public class HyphenatedAdjectiveTransformation implements DependencyTransformation {
	private static Logger log = Logger.getLogger(HyphenatedAdjectiveTransformation.class.getName());	
	
	@Override
	public List<Class<? extends DependencyTransformation>> getPrerequisites() {
		return new ArrayList<Class<? extends DependencyTransformation>>();
	}

	/**
	 * Transforms a sentence by identifying the predicate and entity components of a complex,
	 * hyphenated adjective and splitting the adjective in such a way that relations can be 
	 * constructed later. 
	 * Creates <code>Word</code> objects when splitting, though it does not use the 
	 * {@code Document#getSurfaceElementFactory()} method. 
	 * Instead, it calls {@code Sentence#splitSurfaceElements(SurfaceElement, List)}, which is not ideal.
	 * 
	 * @param  sent  the sentence to transform
	 */
	public synchronized void transform(Sentence sent) {
		if (sent.getSurfaceElements() == null || sent.getEmbeddings() == null) {
			log.log(Level.WARNING,"No textual units or embeddings can be found for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		List<Class<? extends DependencyTransformation>> applied  = sent.getTransformations();
		if (applied.containsAll(getPrerequisites()) == false) {
			log.log(Level.WARNING,"The prerequisites for HyphenatedAdjectiveTransformation are not met for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		
		Document doc = sent.getDocument();
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		List<SynDependency> embeddings = sent.getEmbeddings();
		List<SurfaceElement> verticesForAddition = new ArrayList<>();					
		List<SynDependency> markedForRemoval = new ArrayList<>();
		List<SynDependency> markedForAddition = new ArrayList<>();
		List<String> depTypes = Arrays.asList("amod","dep");
		List<String> npTypes = Arrays.asList("amod","nn");
		for (SurfaceElement su: surfs) {
			/*
			 * It can in fact be a multi word too.
			 * e.g., RelA/NFkappaB-binding activity. 
			 */
			if (su instanceof MultiWord || su instanceof GappedMultiWord) continue;
			Word w = (Word)su;
			if (!(w.hasSemantics() && (w.isAdjectival() || w.isVerbal()))) continue; 
			// the textual unit has to have two semantic objects at least, one for the Entity and one for the Predicate
			// this may be too strict
			boolean hyphenated = false;
			if (w.getText().contains("-")) {
				if (w.getSemantics().size() > 1) {
					int hyphenIndex = w.getText().lastIndexOf('-')+ w.getSpan().getBegin();
					LinkedHashSet<SemanticItem> predicates = Document.getSemanticItemsByClassSpan(doc, Predicate.class, new SpanList(new Span(hyphenIndex+1,w.getSpan().getEnd())),false);
					LinkedHashSet<SemanticItem> entities = Document.getSemanticItemsByClassSpan(doc, Entity.class, new SpanList(new Span(w.getSpan().getBegin(),hyphenIndex)),false);
					if (predicates.size() == 0 || entities.size() == 0) continue;
					SurfaceElement entity = null;
					SurfaceElement predicate= null;
					/// NOTE: Here the indices will be screwed up. Need to update them.
					// Since we are not touching the original word list, we may be off the hook.
					// Creating new Words at this stage is generally not a good idea
					// more checks needed here possibly, to ensure that we are not screwing things up
					// ** Now uses s.splitSurfaceElements() which should handle this problem, but have not tested.
					for (SemanticItem si: entities) {
						Entity e = (Entity)si;
						Span esp = e.getSpan().getSpans().get(0);
						String et = doc.getStringInSpan(esp);
						//	entity = doc.getSurfaceElementFactory().createSurfaceElementIfNecessary(s, e.getSpan());
						entity = new Word(et,"NNS", new WordLexeme(et,"NN"), w.getIndex(), esp);
						entity.addSemantics(e);
					}
					for (SemanticItem si: predicates) {
						Predicate p = (Predicate)si;
						Span psp = p.getSpan().getSpans().get(0);
						String pt = doc.getStringInSpan(psp);
						//	entity = doc.getSurfaceElementFactory().createSurfaceElementIfNecessary(s, e.getSpan());
						String pos = (pt.endsWith("ing") ? "VBG" : "VBN");
						predicate = new Word(pt, pos, new WordLexeme(pt,"VB"), w.getIndex(), psp);
						predicate.addSemantics(p);
						LinkedHashSet<SemanticItem> rels = w.filterByRelations();
						if (rels !=null) predicate.addSemantics(rels);
					}
					if (entity != null && predicate != null) hyphenated = true;
					
					if (hyphenated) {
						Word hyph = new Word("-", "-", new WordLexeme("-","-"), w.getIndex(), new Span(hyphenIndex,hyphenIndex+1));
						boolean hasDependencies = false;

						List<SynDependency> inDeps = SynDependency.inDependencies(su, embeddings);
						if (inDeps == null) continue;
						for (SynDependency e: inDeps) {
							if (depTypes.contains(e.getType()) == false) continue; 
							hasDependencies = true;
							SurfaceElement opposite = e.getGovernor(); //g.getOpposite(v, e);
							markedForRemoval.add(e);
							log.log(Level.FINE,"Removing dependency due to hyphenated adjective transformation: {0}.",new Object[]{e.toString()});
							for (SynDependency sd: SynDependency.inDependencies(opposite, embeddings)) {
								markedForRemoval.add(sd);
								log.log(Level.FINE,"Removing dependency due to hyphenated adjective transformation: {0}.",new Object[]{sd.toString()});
								SynDependency d = new SynDependency("HYP_" + sd.getId(),sd.getType(),sd.getGovernor(),predicate);
								markedForAddition.add(d);
								log.log(Level.FINE,"Adding dependency due to hyphenated adjective transformation: {0}.",new Object[]{d.toString()});
							}
							String type1 = (((Word)predicate).getPos().equals("VBN") ? "dobj" : "nsubj");
							String type2 = (((Word)predicate).getPos().equals("VBN") ? "nsubj" : "dobj");
							SynDependency d1 = new SynDependency("HYP_" + e.getId(),type1,predicate,opposite);
							markedForAddition.add(d1);
							log.log(Level.FINE,"Adding dependency due to hyphenated adjective transformation: {0}.",new Object[]{d1.toString()});
							SynDependency d2 = new SynDependency("HYP_" + e.getId(),type2,predicate,entity);
							markedForAddition.add(d2);
							log.log(Level.FINE,"Adding dependency due to hyphenated adjective transformation: {0}.",new Object[]{d2.toString()});
						}
						if (hasDependencies) {
							verticesForAddition.add(entity);
							log.log(Level.FINE,"Adding textual unit due to hyphenated adjective transformation: {0}.",new Object[]{entity.toString()});
							verticesForAddition.add(hyph);
							log.log(Level.FINE,"Adding textual unit due to hyphenated adjective transformation: {0}.",new Object[]{hyph.toString()});
							verticesForAddition.add(predicate);
							log.log(Level.FINE,"Adding textual unit due to hyphenated adjective transformation: {0}.",new Object[]{predicate.toString()});
							sent.splitSurfaceElements(su, verticesForAddition);
						}
					}
				}
			}
			// No hyphen involved, but the structure is the same (X induced, rather than X-induced).
			// TODO Obviously doesn't take into account irregular forms. Should really get this lexical information somewhere else, such as SPECIALIST.
			if (!hyphenated && (w.getText().endsWith("ed") || w.getText().endsWith("ing"))) {
				// a regular adjectival trigger?
				for (SemanticItem so: w.getSemantics()) {
					if (so instanceof Entity) continue;
					if (so instanceof Predicate) {
						Collection<SynDependency> inDeps = SynDependency.inDependencies(su, embeddings);
						if (inDeps == null) continue;
						for (SynDependency e: inDeps) {
							if (depTypes.contains(e.getType()) == false) continue; 
							String pos = (w.getText().endsWith("ing") ? "VBG" : "VBN");
							SurfaceElement opposite = e.getGovernor(); //g.getOpposite(v, e);
							if (SpanList.atLeft(opposite.getSpan(),w.getSpan())) continue;
							markedForRemoval.add(e);
							for (SynDependency sd: SynDependency.inDependencies(opposite, embeddings)) {
								markedForRemoval.add(sd);
								log.log(Level.FINE,"Removing dependency due to hyphenated adjective transformation: {0}.",new Object[]{sd.toString()});
								SynDependency d = new SynDependency("HYP_" + sd.getId(),sd.getType(),sd.getGovernor(),w);
								markedForAddition.add(d);
								log.log(Level.FINE,"Adding dependency due to hyphenated adjective transformation: {0}.",new Object[]{d.toString()});
							}
							String type = (pos.equals("VBN") ? "amod" : "nsubj");
							for (SynDependency sd: SynDependency.outDependencies(opposite, embeddings)) {
								if (npTypes.contains(sd.getType())){
									SurfaceElement dep = sd.getDependent();
									if (dep.equals(w)) continue;
									if (dep.filterByEntities().size() > 0 && dep.getIndex() < w.getIndex()) {
										// nsubj was dobj, how can that be correct??
										String type2 = (type.equals("amod") ? "nsubj" : "amod");
										markedForRemoval.add(sd);
										log.log(Level.FINE,"Removing dependency due to hyphenated adjective transformation: {0}.",new Object[]{sd.toString()});
										SynDependency d =new SynDependency("HYP_" + sd.getId(),type2,w,dep);
										markedForAddition.add(d);
										log.log(Level.FINE,"Adding dependency due to hyphenated adjective transformation: {0}.",new Object[]{d.toString()});
									}
								}
							}
							// this is ad-hoc, X induced Y phosphorylation, here generally the link between X and induced is dep.
							for (SynDependency sd: SynDependency.outDependencies(su, embeddings)) {
								if (sd.getType().equals("dep"))  {
									SurfaceElement dep = sd.getDependent();
									if (pos.equals("VBN") && dep.filterByEntities() != null && dep.getIndex() == w.getIndex()-1) {
										markedForRemoval.add(sd);
										log.log(Level.FINE,"Removing dependency due to hyphenated adjective transformation: {0}.",new Object[]{sd.toString()});
										SynDependency d =new SynDependency("hyph_" + sd.getId(),"dobj",w,dep);
										markedForAddition.add(d);
										log.log(Level.FINE,"Adding dependency due to hyphenated adjective transformation: {0}.",new Object[]{d.toString()});

									}
								}
							}
							SynDependency d =new SynDependency("hyph_" + e.getId(),type,w,opposite);
							markedForAddition.add(d);
							log.log(Level.FINE,"Adding dependency due to hyphenated adjective transformation: {0}.",new Object[]{d.toString()});
						}									
					}
				}
			}
			
		}
		// May not be the best place, maybe do it after each surface element is processed?
		sent.synchEmbeddings(markedForRemoval, markedForAddition);
		sent.addTransformation(this.getClass());
	}

}

