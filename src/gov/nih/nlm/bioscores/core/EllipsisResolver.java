package gov.nih.nlm.bioscores.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Event;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;

/**
 * A very preliminary ellipsis (or implicit argument) resolution mechanism
 * based on consumer health questions and developed for extracting frames
 * from such questions.<p>
 * 
 * Once it's determined that a frame has unfulfilled mandatory slots, we try 
 * to recover the value of that slot from the prior discourse. Currently, this 
 * is limited to recovering the Theme argument (which is a disorder).
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO This should be generalized to a full implicit argument recognition program.
public class EllipsisResolver {
	private static Logger log = Logger.getLogger(EllipsisResolver.class.getName());

	/**
	 * Processes the given frame with unfulfilled argument slot for ellipsis.<p>
	 * Currently, this is limited to recovering Theme argument of a question frame.
	 * 
	 * @param frame	the frame with unfulfilled argument
	 * @param sent	the sentence with the frame predicate
	 * 
	 * @return	a new frame with the recovered argument to replace the old one, or null if no ellipsis exists
	 */
	public static Event processForEllipsis(Event frame, Sentence sent) {
		Document doc = sent.getDocument();
		Predicate pred = frame.getPredicate();
		List<String> sems = new ArrayList<>(frame.getDefinition().getArgumentSemTypes("Theme"));
		if (ellipsisExists(pred,frame.getArguments(),sent.getEmbeddings(),SynDependency.THEME_DEPS) == false) return null;  
		// for now assume that we are looking for a Theme that is a disorder (and Entity).
		// we should really get this information from the EventDefinitions and predicate
		// "Entity" should be just a parameter read from the definition
		log.log(Level.FINE, "Processing frame for ellipsis: {0}.", new Object[]{frame.toString()});
		Set<SemanticItem> entitiesByType = 
				Document.getSemanticItemsByClassType(doc,Entity.class,sems);
		SemanticItem salient = mostSalient(doc,pred,entitiesByType);
		if (salient == null) return null;
		Event nev = frame;
		nev.addArgument(new Argument("Theme",salient));
		log.log(Level.FINE, "Updated frame for ellipsis: {0}.", new Object[]{nev.toString()});
		return nev;
	}
	
	// preliminary
	// probably should have a list of acceptable and not acceptable dependencies (for each role)
	// and filter accordingly
	// but really each predicate should have its selectional restrictions encoded separately
	// and this information should be obtained from those restrictions
	// otherwise we can only check for the existence of generic dependencies (as in probDeps)
	// and there is a good chance things will go wrong there.
	// An example to try is 'What can we expect for the future?'
	private static boolean ellipsisExists(Predicate pred, List<Argument> args, List<SynDependency> embeddings,
			List<String> deps) {
		if (pred == null || embeddings == null) return false;
		Collection<SynDependency> themeEmbeddings = SynDependency.outDependenciesWithTypes(pred.getSurfaceElement(), embeddings, 
						deps, true);
		if (themeEmbeddings.size() == 0) return true;
		// if the dependency already licensed an argument, do not use it to prevent ellipsis processing
		Set<SynDependency> licensing = new HashSet<>();
		for (SynDependency sd: themeEmbeddings) {
			SurfaceElement d = sd.getDependent();
			Set<SemanticItem> sem = d.getSemantics();
			if (sem == null)  continue;
			for (Argument arg: args) {
				boolean added = false;
				SemanticItem si = arg.getArg();
				// do not know why I cannot get this to work with contains. Something must be messed up in hashCode().
				for (SemanticItem ss: sem) {
					if (ss.equals(si)) {
						licensing.add(sd);
						added =true;
						break;
					}
				}
				if (added) break;
			}
		}
		// if all licensed already, there is ellipsis
		return  (themeEmbeddings.size() == licensing.size());
	}
	
	// very preliminary
	private static SemanticItem mostSalient(Document doc, Term term, Set<SemanticItem> in) {
		if (in == null) return null;
		LinkedHashSet<SemanticItem> priorSem = 
				Document.getSemanticItemsByClassSpan(doc, Term.class, new SpanList(0,term.getSpan().getBegin()),false);
//		Set<SemanticItem> priorSem = filterBySpan(term,in);
		if (priorSem.size() == 0) return null;
		if (priorSem.size() == 1) return priorSem.iterator().next();
		List<SemanticItem> priorSemList = new ArrayList<>(priorSem);
		// first mentioned is the most salient
		Collections.sort(priorSemList);
//		return priorSemList.get(priorSemList.size()-1);
		return priorSemList.get(0);
	}
}
