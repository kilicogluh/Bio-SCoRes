package gov.nih.nlm.bioscores.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.brat.Annotation;
import gov.nih.nlm.ling.brat.AnnotationArgument;
import gov.nih.nlm.ling.brat.EventAnnotation;
import gov.nih.nlm.ling.brat.RelationAnnotation;
import gov.nih.nlm.ling.brat.TermAnnotation;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.GappedMultiWord;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Concept;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Event;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import gov.nih.nlm.ling.sem.Term;

/**
 * Class to create semantic objects specific to coreference, such as expressions and coreference chains.
 * 
 * @author Halil Kilicoglu
 *
 */
public class CoreferenceSemanticItemFactory extends SemanticItemFactory {
	private static Logger log = Logger.getLogger(CoreferenceSemanticItemFactory.class.getName());	

	public CoreferenceSemanticItemFactory(Document doc, Map<Class<? extends SemanticItem>,Integer> counterMap) {
		super(doc,counterMap);
	}
	
	/**
	 * Creates a new {@link CoreferenceChain} from an existing standoff {@link RelationAnnotation} object. <p>
	 * If the objects referenced by <var>rel</var> annotation (e.g., terms or events) do not exist yet, 
	 * it will create these objects first. 
	 * 
	 * @param doc  the document that the coreference relation is associated with
	 * @param rel  the corresponding <code>RelationAnnotation</code> object
	 * @return  	a new <code>CoreferenceChain</code> object
	 */
	public CoreferenceChain newCoreferenceChain(Document doc, RelationAnnotation rel) {	
		List<AnnotationArgument> annArgs = rel.getArguments();
		String type = rel.getType();
		List<Argument> args = new ArrayList<>();
		for (int i=0; i < annArgs.size(); i++) {
			String r = annArgs.get(i).getRole();
			Annotation ann = annArgs.get(i).getArg();
			if (ann instanceof EventAnnotation) {
				SemanticItem si  = doc.getSemanticItemById(ann.getId());
				if (si == null) {
					Event ev = newEvent(doc,(EventAnnotation)ann);
					args.add(new Argument(r,ev));
				}  else 
					args.add(new Argument(r,(Event)si));
			} else if (ann instanceof TermAnnotation) {
				SemanticItem si  = doc.getSemanticItemById(ann.getId());
				if (si == null) {
					Entity ent = newEntity(doc,(TermAnnotation)ann);
					args.add(new Argument(r,ent));
				}  else 
					args.add(new Argument(r,(Entity)si));
			} else {
				log.log(Level.WARNING,"The argument annotation is neither event nor entity: {0}.", new Object[]{ann.toString()});
			}
		}
		CoreferenceChain cc = newCoreferenceChain(doc,type,args);
		cc.setId(rel.getId());
		return cc;
	}
	
	/**
	 * Creates a new <code>CoreferenceChain</code> object from a list or arguments.
	 * 
	 * @param doc  	the document that the coreference relation is associated with
	 * @param type  the specific coreference type
	 * @param args	the arguments of the coreference relation
	 * 
	 * @return  a <code>CoreferenceChain</code> object with the given arguments and type
	 */
	public CoreferenceChain newCoreferenceChain(Document doc, String type, List<Argument> args) {	
		CoreferenceChain c = new CoreferenceChain("R" + getNextId(CoreferenceChain.class),type, args);
		doc.addSemanticItem(c);
		return c;
	}
	
	/**
	 * Creates a new {@link Expression} object from an existing standoff {@link TermAnnotation} object.
	 * 
	 * @param doc	the document that the expression is associated with
	 * @param t		the corresponding <code>TermAnnotation</code> standoff annotation
	 * @return 		a <code>Expression</code> object
	 */
	public Expression newExpression(Document doc, TermAnnotation t) {
		Expression exp = newExpression(doc, t.getType(), t.getSpan(), null, t.getText());
		exp.setId(t.getId());
		return exp;
	}
	
	/**
	 * Creates a new <code>Expression</code> object from type and span information.
	 * 
	 * @param doc		the document that the coreferential expression is associated with
	 * @param type		the specific type of the coreferential expression
	 * @param sp		the span of the term
	 * @param headSp	the head span of term
	 * @param text  	the text of the term
	 * @return   		a <code>Expression</code> object
	 */
	public Expression newExpression(Document doc, String type, SpanList sp, SpanList headSp, String text) {	
		SurfaceElement si = doc.getSurfaceElementFactory().createSurfaceElementIfNecessary(doc, sp, headSp, true);
		return newExpression(doc, type, si);
	}
	
	/**
	 * Creates a new <code>Expression</code> object from type and textual unit information.<p>
	 * If the type specified for the mention is unrecognized, it will attempt to identify the
	 * fine-grained type.
	 * 
	 * @param doc	the document that the mention is associated with
	 * @param type  the specific type of the coref. mention
	 * @param se  	the corresponding textual unit
	 * @return  	a <code>Expression</code> object
	 */
	public Expression newExpression(Document doc, String type, SurfaceElement se) {	
		ExpressionType expType = null;
		try {
			expType = ExpressionType.valueOf(type);
		} catch (IllegalArgumentException iae) {
			expType = ExpressionRecognition.getMentionType(se);
		}
		return newExpression(doc,expType,se);
	}

	/**
	 * Creates a new <code>Expression</code> object from type and textual unit information.<p>
	 * It also attempts to attach conceptual semantics attached to the textual unit <var>se</var>
	 * to the coreferential expression, so that such information can be used in coreference
	 * agreement computation. Semantics attached to the textual unit is assumed to be entity
	 * semantics.
	 * 
	 * @param doc	the document that the mention is associated with
	 * @param type  the specific type of the coref. mention
	 * @param se  	the corresponding textual unit
	 * @return  	a <code>Expression</code> object
	 */
	public Expression newExpression(Document doc, ExpressionType type, SurfaceElement se) {	
		Expression ce = new Expression("T" + getNextId(Term.class),type.toString(),se.getSpan(),se.getHead().getSpan(),se);
		Set<SemanticItem> existingEntities = se.filterByEntities();
		// attach semantics of existing entities if possible
		if (existingEntities != null) {
			Set<Concept> concepts = null;
			Concept sense = null;
			for (SemanticItem t: se.filterByEntities()) {
				if (t instanceof Entity) {
					Set<Concept> tconcs = ((Entity)t).getConcepts();
					Concept tsense = ((Entity)t).getSense();
					if (concepts == null) {
						if (tconcs != null) concepts = new LinkedHashSet<>(tconcs);
						// first sense is the selected sense
						sense = tsense;
					}
					else if (tconcs != null) {
						concepts.addAll(tconcs);
					}
				}
			}
			if (concepts != null && sense != null) {
				ce.setConcepts(concepts);
				ce.setSense(sense);
			}
		}
		// possessive issue
		SpanList spp = se.getSpan();
		if (se instanceof GappedMultiWord)  {
			GappedMultiWord gmw = (GappedMultiWord)se;
			SurfaceElement primary = gmw.getElements().get(gmw.getPrimaryItemIndex());
			spp = primary.getSpan();
		}
		SurfaceElement subsuming = se.getSentence().getSubsumingSurfaceElement(spp);
		subsuming.addSemantics(ce);
		ce.setSurfaceElement(subsuming);
		doc.addSemanticItem(ce);
		return ce;
	}
}
