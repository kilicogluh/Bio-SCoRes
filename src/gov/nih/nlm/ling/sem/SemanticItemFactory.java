package gov.nih.nlm.ling.sem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.brat.Annotation;
import gov.nih.nlm.ling.brat.AnnotationArgument;
import gov.nih.nlm.ling.brat.EventAnnotation;
import gov.nih.nlm.ling.brat.Reference;
import gov.nih.nlm.ling.brat.TermAnnotation;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.MultiWord;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;

/**
 * A factory class to instantiate semantic items. Semantic objects are associated with 
 * a {@link Document} object and they are stored as a map. The key of the map is a semantic class
 * (e.g., Entity.&nbsp;class) and the value for this key is the set of entities associated with the document.
 * 
 * 
 * @author Halil Kilicoglu
 *
 */
public class SemanticItemFactory {
	private static Logger log = Logger.getLogger(SemanticItemFactory.class.getName());	

	protected Map<Class<? extends SemanticItem>,Integer> counter = null;
	private Document document;
	
	public SemanticItemFactory(Document doc, Map<Class<? extends SemanticItem>,Integer> counterMap) {
		this.document = doc;
		if (counter == null) counter = new HashMap<Class<? extends SemanticItem>,Integer>();
		counter.putAll(counterMap);
	}
				
	public Document getDocument() {
		return document;
	}

	/**
	 * Gets the next available identifier for the given semantic class,
	 * by incrementing the counter.
	 * 
	 * @param type	the semantic class
	 * @return	the next available identifier
	 */
	public int getNextId(Class<? extends SemanticItem> type) {
		int max = document.getMaxId(type);
		if (counter.containsKey(type)) {
			counter.put(type, max+1);
		} 
		return max + 1;
	}
	
	/**
	 * Creates a new <code>Entity</code> from a standoff Term annotation object.<p>
	 * It attempts to attach concepts to the entity object, if {@link Reference} annotations associated with the
	 * Term annotation is found.
	 * 
	 * @param doc	the document the term is associated with
	 * @param term	the <code>TermAnnotation</code> object corresponding to the term
	 * @return  	the new <code>Entity</code> object, null if unsuccessful with creating/retrieving the
	 * 				corresponding textual unit 
	 */
	public Entity newEntity(Document doc, TermAnnotation term) {
		log.log(Level.FINEST,"Creating entity from standoff annotation: {0}.", new Object[]{term.toString()});
		Span psp = term.getSpan().first();
		// get the primary span first in the case of disjointed items
		if (term.getSpan().size() > 1) {
			boolean found = false;
			for (Span sp: term.getSpan().getSpans()) {
				List<Word> temp = doc.getWordsInSpan(sp);
				for (Word tw : temp) {
					if (tw.isNominal()) {
						psp = sp;
						found = true;
						break;
					}
				}
				if (found) break;
			}
			if (found == false) {
				for (Span sp: term.getSpan().getSpans()) {
					List<Word> temp = doc.getWordsInSpan(sp);
					for (Word tw : temp) {
						if (tw.isVerbal()) {
							psp = sp;
							break;
						}
					}
					if (found) break;
				}
			}
		} 
		List<Word> ws = doc.getWordsInSpan(psp);
		SpanList headSpan = MultiWord.findHeadFromCategory(ws).getSpan();
		if (SpanList.subsume(term.getSpan(), headSpan) == false) headSpan = SpanList.intersection(term.getSpan(),headSpan);
		log.log(Level.FINEST,"Head span for the standoff annotation {0}: {1}.", new Object[]{term.toString(),headSpan.toStandoffAnnotation()});
		Entity ent = newEntity(doc,term.getSpan(),headSpan,term.getType());
		ent.setId(term.getId());
		// attach concepts, if any
		List<Reference> refs = term.getReferences();
		if (refs != null && refs.size() > 0) {
			LinkedHashSet<Concept> concs = new LinkedHashSet<Concept>();
			for (Reference ref: refs) {
				LinkedHashSet<String> semtypes = new LinkedHashSet<String>(); semtypes.add(term.getType());
				Concept conc = new Concept(ref.getSourceId(),term.getText(),semtypes,ref.getSource());
				concs.add(conc);
			}
			ent.setConcepts(concs);
			ent.setSense(concs.iterator().next());
		}
		return ent;
	}
	
	/**
	 * Creates a new <code>Entity</code> object with the given span and ontological information.<p>
	 * It does not check whether concept, sense information is valid. 
	 * 
	 * @param doc  	 the document the term is associated with
	 * @param sp 	 the span of the term
	 * @param headSp the span of the head of the term 
	 * @param type   the semantic type of the term
	 * @param concs  the concepts associated with the term
	 * @param sense  the relevant sense 
	 * @return  the new <code>Entity</code> object, null if unsuccessful with creating/retrieving the
	 * 			corresponding textual unit
	 */
	public Entity newEntity(Document doc, SpanList sp, SpanList headSp, String type, Set<Concept> concs, Concept sense) {
		Entity e = newEntity(doc,sp,headSp,type);
		if (e == null) return null;
		e.setConcepts(concs);
		e.setSense(sense);
		return e;
	}
	
	/**
	 * Creates a new <code>Entity</code> object with the given span and type information.
	 * 
	 * @param doc		the document the term is associated with
	 * @param sp		the span of the term
	 * @param headSp	the span of the head of the term 
	 * @param type  	the semantic type of the term
	 * @return  		the new <code>Entity</code> object, null if unsuccessful with creating/retrieving the
	 * 					corresponding textual unit
	 */
	public Entity newEntity(Document doc, SpanList sp, SpanList headSp, String type) {
		log.log(Level.FINEST,"Creating entity from span {0} and head span {1}.", new Object[]{sp.toString(),headSp.toString()});
		SurfaceElement si = doc.getSurfaceElementFactory().createSurfaceElementIfNecessary(doc, sp, headSp, true);
		if (si == null) {
			log.log(Level.SEVERE,"Unable to create entity object from span {0} in doc {1}.", new Object[]{sp.toString(),doc.getId()}); 
			return null;
		}
		log.log(Level.FINEST,"The surface element corresponding to the entity span {0}: {1}.", new Object[]{sp.toString(),si.toString()});
		Entity e = new Entity("T" + getNextId(Term.class),type, sp, headSp, si);
		// TODO Things sometimes go wrong here, if the new term happens to match an existing term exactly
		// Normally, this wouldn't be a problem, but when we are reading annotations from a file, 
		// this means one term might be lost, which might lead to other problems. A better way is needed,
		// but I don't have the time right now (2/28/15), so I will look at this later.
		// TODO This should probably be done in an XML reader.
		si.addSemantics(e);
		doc.addSemanticItem(e);		
		return e;
	}
	
	/**
	 * Creates an <code>Entity</code> object with a generic concept, such as GENERIC_SOURCE.
	 * 
	 * @param doc		the document the term is associated with
	 * @param sp		the span of the term
	 * @param headSp	the span of the head of the term 
	 * @param conc		the generic concept
	 * @return 			the new <code>Entity</code> object, null if unsuccessful with creating/retrieving the
	 * 					corresponding textual unit
	 */
	public Entity newGenericEntity(Document doc, SpanList sp, SpanList headSp, Concept conc) {
		String type = conc.getSemtypes().iterator().next();
		Set<Concept> concs = new HashSet<Concept>(); concs.add(conc);
		return newEntity(doc,sp,headSp,type,concs,concs.iterator().next());
	}
	
	
	/**
	 * Creates a new <code>Predicate</code> from a Term standoff annotation.
	 * 
	 * @param doc	the document the predicate is associated with
	 * @param t		the <code>TermAnnotation</code> object corresponding to the term
	 * @return  	the new <code>Predicate</code> object, null if unsuccessful with creating/retrieving the
	 * 				corresponding textual unit 
	 */
	public Predicate newPredicate(Document doc, TermAnnotation t) {
		SpanList sp =t.getSpan();
		List<Word> ws = doc.getWordsInSpan(sp);
		SpanList headSpan = MultiWord.findHeadFromCategory(ws).getSpan();
		Predicate pr = newPredicate(doc,sp,headSpan,t.getType());
		pr.setId(t.getId());
		return pr;
	}
	
	/**
	 * Creates a new <code>Predicate</code> object with the given span and indicator information.<p>
	 * If no indicator information is provided, it still creates an object. This may not be such a
	 * great idea. In such cases, it sets the semantic type to be <i>OTHER</i>. 
	 * 
	 * @param doc   	the document the predicate is associated with
	 * @param sp  		the span of the predicate
	 * @param headSp	the span of the predicate head 
	 * @param ind		the indicator for the predicate
	 * @param sem		the relevant sense 
	 * @return  		the new <code>Predicate</code> object, null if unsuccessful with creating/retrieving the
	 * 					corresponding textual unit
	 */
	public Predicate newPredicate(Document doc, SpanList sp, SpanList headSp, Indicator ind, Sense sem) {	
		String semType = "";
		if (sem == null) {
			if (ind.getSenses() == null || ind.getSenses().size() == 0) {
				log.log(Level.WARNING,"Problematic indicator with no senses: {0}." , new Object[]{ind.toString()});
				semType = "OTHER";
			}
			else  {
				sem = ind.getMostProbableSense();
				if (sem == null) sem = ind.getSenses().get(0);
			}
		} 
		if (semType.equals("") && sem != null) {
			semType = sem.getCategory();
		}
		Predicate pr = newPredicate(doc,sp,headSp, semType);
		if (pr == null) return null;
		pr.setIndicator(ind);
		pr.setSense(sem);
		return pr;
	}
	
	/**
	 * Creates a new <code>Predicate</code> object with the given span and type information.
	 * 
	 * @param doc   	the document the predicate is associated with
	 * @param sp  		the span of the predicate
	 * @param headSp  	the span of the predicate head
	 * @param type  	the semantic type of the predicate
	 * @return  		the new <code>Predicate</code> object, null if unsuccessful with creating/retrieving the
	 * 					corresponding textual unit
	 */
	public Predicate newPredicate(Document doc, SpanList sp, SpanList headSp, String type) {	
		log.log(Level.FINEST,"Creating predicate from span {0} and head span {1}.", new Object[]{sp.toString(),headSp.toString()});
		SurfaceElement si = doc.getSurfaceElementFactory().createSurfaceElementIfNecessary(doc, sp, headSp, true);
		if (si == null) {
			log.log(Level.WARNING,"Unable to create predicate object from span {0} in doc {1}.", new Object[]{sp.toString(),doc.getId()}); 
			return null;
		}
		log.log(Level.FINEST,"The surface element corresponding to the predicate span {0}: {1}.", new Object[]{sp.toString(),si.toString()});
		Predicate p = new Predicate("T" + getNextId(Term.class),type,sp,headSp,si);
		p.setSurfaceElement(si);
		si.addSemantics(p);
		doc.addSemanticItem(p);
		return p;
	}
	
	/**
	 * Creates a new <code>Predication</code> object. 
	 * 
	 * @param doc			the document the predication is associated with
	 * @param predicate		the predicate of the predication
	 * @param args			the list of arguments
	 * @param scalarValues  the scalar modality values associated with the predication
	 * @param sources		the sources the predication is attributed to
	 * @return a new <code>Predication</code> object, null if predicate or arguments are null or invalid.
	 */
	public Predication newPredication(Document doc, Predicate predicate, List<Argument> args,
			List<ScalarModalityValue> scalarValues, List<SemanticItem> sources) {
		if (args == null || args.size() == 0 || predicate == null) return null;
		Predication pr = null;
		log.log(Level.FINEST,"Creating a new predication with the predicate {0}.", new Object[]{predicate.toShortString()});
		for (int i=0; i < args.size(); i++) {
			log.log(Level.FINEST,"\tArgument: ", new Object[]{args.get(i).toShortString()});
		}
		String id = "P" + getNextId(Predication.class);
		pr = new Predication(id, predicate,args, scalarValues, sources);
		if (pr.coreArgsResolved()) pr.setResolved(true);
		log.log(Level.FINEST,"Predication generated: {0}.", new Object[]{pr.toShortString()});
		doc.addSemanticItem(pr);
		return pr;
	}
		
	/**
	 * Creates a new <code>ImplicitRelation</code> object. 
	 * 
	 * @param doc			the document the relation is associated with
	 * @param type			the relation type
	 * @param args			the list of arguments
	 * 
	 * @return a new <code>ImplicitRelation</code> object, null if a relation cannot be instantiated.
	 */
	public ImplicitRelation newImplicitRelation(Document doc, String type, List<Argument> args) { 
		log.log(Level.FINEST,"Creating a new implicit relation with type {0}.", new Object[]{type});
		for (int i=0; i < args.size(); i++) {
			log.log(Level.FINEST,"\tArgument: ", new Object[]{args.get(i).toShortString()});
		}
		String id = "R" + getNextId(Relation.class);
		ImplicitRelation sr = new ImplicitRelation(id,type,args);
		log.log(Level.FINEST,"Implicit relation generated: {0}.", new Object[]{sr.toShortString()});
		doc.addSemanticItem(sr);
		return sr;
	}
	
	/**
	 * Creates a new <code>Event</code> object from a Event standoff annotation.
	 * 
	 * @param doc	the document the event is associated with
	 * @param ev  	the corresponding <code>EventAnnotation</code> object
	 * @return 		a new <code>Event</code> object, null if predicate is null.
	 */
	public Event newEvent(Document doc, EventAnnotation ev) {
		Predicate pr = (Predicate)doc.getSemanticItemById(ev.getPredicate().getId());
		if (pr == null) {
			pr = newPredicate(doc,ev.getPredicate());
			if (pr == null) return null;
		} 
		List<AnnotationArgument> annArgs = ev.getArguments();
		List<Argument> args = new ArrayList<Argument>();
		for (int i=0; i < annArgs.size(); i++) {
			String r = annArgs.get(i).getRole();
			Annotation ann = annArgs.get(i).getArg();
			if (ann instanceof EventAnnotation) {
				SemanticItem si  = doc.getSemanticItemById(ann.getId());
				if (si == null) {
					Event ee = newEvent(doc,(EventAnnotation)ann);
					args.add(new Argument(r,ee));
				}  else 
					args.add(new Argument(r,(Event)si));
			} else if (ann instanceof TermAnnotation) {
				SemanticItem si  = doc.getSemanticItemById(ann.getId());
				if (si == null) {
					Entity ent = newEntity(doc,(TermAnnotation)ann);
					args.add(new Argument(r,ent));
				}  else 
					args.add(new Argument(r,si));
			} else {
				log.log(Level.WARNING,"The Event argument annotation is neither event nor entity: {0}.", new Object[]{ann.toString()});
			}
		}
		Event ne = newEvent(doc,pr,args);
		ne.setId(ev.getId());
		return ne;
	}
	
	/**
	 * Creates a new <code>Event</code> object. 
	 * 
	 * @param doc  		the document the event is associated with
	 * @param predicate the predicate of the predication
	 * @param args  	the list of arguments
	 * @return a new <code>Event</code> object, null if predicate is null.
	 */
	public Event newEvent(Document doc, Predicate predicate, List<Argument> args) { 
		if (predicate == null) return null;
		log.log(Level.FINEST,"Creating a new Event with predicate {0}.", new Object[]{predicate.toShortString()});
		for (int i=0; i < args.size(); i++) {
			log.log(Level.FINEST,"\tArgument: ", new Object[]{args.get(i).toShortString()});
		}
		String id = "E" + getNextId(Event.class);
		Event sr = new Event(id,predicate,args);
		if (sr.coreArgsResolved()) sr.setResolved(true);
		log.log(Level.FINEST,"Event generated: {0}.", new Object[]{sr.toShortString()});
		doc.addSemanticItem(sr);
		return sr;
	}
		
	/**
	 * Creates a <code>Conjunction</code> object with an explicit predicate. 
	 * 
	 * @param doc  	the document 
	 * @param type  the semantic type to assign
	 * @param pred	the predicate indicator for the conjunction
	 * @param sis	the conjunct semantic objects
	 * @return  a Conjunction object
	 */
	public Conjunction newConjunction(Document doc, String type, Predicate pred, Set<SemanticItem> sis) {
		Conjunction sic = new Conjunction("C" + getNextId(Predication.class), type, pred, sis);
		pred.getSurfaceElement().addSemantics(sic);
		doc.addSemanticItem(sic);
		log.log(Level.FINEST,"Conjunction generated: {0}.", new Object[]{sic.toShortString()});
		return sic;
	}
}
