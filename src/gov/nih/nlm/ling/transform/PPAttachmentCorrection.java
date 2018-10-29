package gov.nih.nlm.ling.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.AbstractSurfaceElement;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.core.Word;


/**
 * A <code>DependencyTransformation</code> implementation that attempts to corect PP-attachment errors
 * often made by Stanford Parser.<p>
 * It currently uses simple wordlists for different prepositions harvested from the BioNLP shared task
 * data as well as Schuman and Bergler (2006) heuristcs. 
 * The wordlists can be generalized using the syntactic restrictions in SPECIALIST Lexicon or such.
 * 
 * @author Halil Kilicoglu
 *
 */
public class PPAttachmentCorrection implements DependencyTransformation {
	private static Logger log = Logger.getLogger(PPAttachmentCorrection.class.getName());
	
	private static final List<String> RIGHT_ASSOCIATION_PREPS = Arrays.asList("of","from","for");
	private static final List<String> STRONG_NOMINALIZATION_PREPS = Arrays.asList("by","at");
	private static final List<String> WEAK_NOMINALIZATION_PREPS = Arrays.asList("with","in","as");
	
	private Map<String,List<String>> prepTypes;

	/**
	 * The default constructor. Takes in a pre-defined list of lemmas to apply
	 * pp reattachment to. The list is primarily based on the BioNLP Shared task.
	 * 
	 */
	// TODO Generalize using restrictions in SPECIALIST Lexicon or some other source.
	public PPAttachmentCorrection() {
		prepTypes = new HashMap<String,List<String>>();
		List<String> prepInTypes = Arrays.asList("alteration","block","change","decline",
				"decrease","importance","increase","involvement","reduction","role");
		List<String> prepWithTypes = Arrays.asList("association","associate","co-expression",
				"co-immunoprecipitation","coexpression","coimmunoprecipitation","cotransfection",
				"co-transfection","complex","interaction","transfection");
		List<String> prepOnTypes = Arrays.asList("influence","effect","impact");
		List<String> prepBetweenTypes = Arrays.asList("association","interaction");
		List<String> prepForTypes = Arrays.asList("role");
		prepTypes.put("prep_in",prepInTypes);
		prepTypes.put("prep_with",prepWithTypes);
		prepTypes.put("prep_on",prepOnTypes);
		prepTypes.put("prep_for",prepForTypes);
		prepTypes.put("prep_between",prepBetweenTypes);
	}
	
	
	@Override
	public List<Class<? extends DependencyTransformation>> getPrerequisites() {				
		return new ArrayList<Class<? extends DependencyTransformation>>();
	}

	/**
	 * Takes in a pre-defined map of dependency type/lemma list pairs to apply
	 * pp reattachment to. The key values should be in the 'prep_X' format, where X is a preposition (e.g., 'prep_on').
	 * Ideally <var>prepTypes</var> should be created from the syntactic information in the dictionary.
	 * 
	 * @param prepTypes	a map of dependency type/lemma lists
	 */
	public PPAttachmentCorrection(Map<String,List<String>> prepTypes) {
		this.prepTypes = prepTypes;
	}


	public Map<String, List<String>> getPrepTypes() {
		return prepTypes;
	}

	/**
	 * Transforms a sentence by re-attaching prepositional phrases.<p> 
	 * The transformation takes place in two steps: <ul>
	 * <li> Applying the PP-attachment rules defined in Schuman and Bergler (2006).
	 * <li> Re-attaching the PPs to the nearest nominalization that acts as 
	 * an object, if the nominalization is in a predefined list of nominalizations 
	 * used in BioNLP shared tasks. 
	 * </ul>
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
			log.log(Level.WARNING,"The prerequisites for PPAttachmentCorrection are not met for the sentence: {0}.", new Object[]{sent.getId()});
			return;
		}
		transformSchumanBergler(sent);
		transformInterveningDirectObject(sent);
		sent.addTransformation(this.getClass());
	}
	
	private synchronized void transformInterveningDirectObject(Sentence sent) {
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		List<SynDependency> embeddings = sent.getEmbeddings();
		List<SynDependency> markedForRemoval = new ArrayList<>();
		List<SynDependency> markedForAddition = new ArrayList<>();
		for (SurfaceElement su: surfs) {
			if (su.isVerbal() == false && su.isAdjectival() == false) continue; 
			List<SynDependency> prepDeps = SynDependency.outDependenciesWithTypes(su, embeddings, Arrays.asList("prep","prepc"), false);
			if (prepDeps.size() == 0) continue;	
			List<SynDependency> outDeps = SynDependency.outDependencies(su, embeddings);
			Map<String, List<SynDependency>> prepDependencies = new HashMap<String,List<SynDependency>>();
			SurfaceElement dobj = null;
			for (SynDependency sd: outDeps) {
				if (sd.getType().startsWith("prep_") || sd.getType().startsWith("prepc_")) { 
					String prep = "prep_" + sd.getType().substring(sd.getType().indexOf("_")+1);
					List<SynDependency> prepSet = prepDependencies.get(prep);
					if (prepSet == null) prepSet = new ArrayList<>();
					prepSet.add(sd);
					prepDependencies.put(prep, prepSet);
				} else if (sd.getType().equals("dobj")) {
					dobj = sd.getDependent();
				}
			}
			if (dobj == null) continue;
			for (String k: prepDependencies.keySet()) {
				List<String> predicates = prepTypes.get(k);
				if (predicates == null) continue;
				List<SynDependency> sds = prepDependencies.get(k);
				if (dobj instanceof Word) {
					Word dw = (Word)dobj;
					if (predicates.contains(dw.getLemma()) && dw.isNominal() && dobj.getIndex() > su.getIndex()) {
						for (SynDependency sd: sds) {
							if (sd.getDependent().getIndex() > dobj.getIndex()) {
									markedForRemoval.add(sd);
									log.log(Level.FINE, "Removing dependency due to PP-attachment correction: {0}", new Object[]{sd.toString()});
									SynDependency d = new SynDependency("PPD_" + sd.getId(),sd.getType(),dobj,sd.getDependent());
									markedForAddition.add(d);
									log.log(Level.FINE, "Adding dependency due to PP-attachment correction: {0}", new Object[]{d.toString()});
							}
						}
					}
				}
			}
		}
		sent.synchEmbeddings(markedForRemoval,markedForAddition);
	}
	
	/**
	 * Applies the PP-attachment rules defined in Schuman and Bergler (2006).
	 * 
	 * @param  sent  the sentence to transform
	 */
	private synchronized void transformSchumanBergler(Sentence sent) {
		List<SynDependency> embeddings = sent.getEmbeddings();
		List<SynDependency> markedForRemoval = new ArrayList<>();
		List<SynDependency> markedForAddition = new ArrayList<>();
		
		for (SynDependency em: embeddings) {
			String type = em.getType();
			if (type.startsWith("prep") || type.startsWith("prepc")) {
				String prep = type.substring(type.indexOf("_")+1).replaceAll("_", " ");
				SurfaceElement gov = em.getGovernor();
				SurfaceElement dep = em.getDependent();
				SpanList spans = sent.getSpansWithText(prep,false);
				List<SurfaceElement> intervening = new ArrayList<>();
//				Span prepSpan = null;
				if (spans != null)  {
					for (Span sp: spans.getSpans()) {
						if (SpanList.atLeft(gov.getSpan(), new SpanList(sp)) && 
								(SpanList.atLeft(new SpanList(sp), dep.getSpan()) || SpanList.subsume(dep.getSpan(), new SpanList(sp)))) {
//							prepSpan = sp;
							intervening = sent.getSurfaceElementsFromSpan(new Span(gov.getSpan().getEnd(),sp.getBegin()));
						}
					}
				}
				if (RIGHT_ASSOCIATION_PREPS.contains(prep)) {
					if (intervening.size() > 0) {
						SurfaceElement newHead= null;
						for (int i=intervening.size()-1; i >=0; i--) {
							SurfaceElement n = intervening.get(i);
							if (n.isNominal()) {
								newHead = n;
								break;
							}
						}
						if (newHead != null) {
							markedForRemoval.add(em);
							log.log(Level.FINE, "Removing dependency due to PP-attachment correction: {0}", new Object[]{em.toString()});
							SynDependency d = new SynDependency("PPS_" + em.getId(),em.getType(),newHead,dep);
							markedForAddition.add(d);
							log.log(Level.FINE, "Adding dependency due to PP-attachment correction: {0}", new Object[]{d.toString()});
							
						}
					}	
				} else if (STRONG_NOMINALIZATION_PREPS.contains(prep)) {
					if (intervening.size() > 0) {
						SurfaceElement newHead= null;
						for (int i=intervening.size()-1; i >=0; i--) {
							SurfaceElement n = intervening.get(i);
							if (((AbstractSurfaceElement)n).isNominalization()) 
								newHead = n;
						}
						if (newHead == null) {
							for (int i=intervening.size()-1; i >=0; i--) {
								SurfaceElement n = intervening.get(i);
								if (n.isVerbal()) newHead = n;
							}						
						}
						if (newHead != null) {
							markedForRemoval.add(em);
							log.log(Level.FINE, "Removing dependency due to PP-attachment correction: {0}", new Object[]{em.toString()});
							SynDependency d = new SynDependency("PPS_" + em.getId(),em.getType(),newHead,dep);
							markedForAddition.add(d);
							log.log(Level.FINE, "Adding dependency due to PP-attachment correction: {0}", new Object[]{d.toString()});
						}
					}
				} else if (WEAK_NOMINALIZATION_PREPS.contains(prep)) {
					if (intervening.size() > 0) {
						SurfaceElement newHead= null;
						for (int i=intervening.size()-1; i >=0; i--) {
							SurfaceElement n = intervening.get(i);
							if (((AbstractSurfaceElement)n).isNominalization()) 
								newHead = n;
						}
						if (newHead == null) {
							for (int i=intervening.size()-1; i >=0; i--) {
								SurfaceElement n = intervening.get(i);
								if (n.isNominal()) {
									if (SynDependency.inDependenciesWithTypes(n, embeddings, Arrays.asList("prep_of","prepc_of"), false).size() == 0)
										newHead = n;
								}
							}						
						}
						if (newHead != null) {
							markedForRemoval.add(em);
							log.log(Level.FINE, "Removing dependency due to PP-attachment correction: {0}", new Object[]{em.toString()});
							SynDependency d = new SynDependency("PPS_" + em.getId(),em.getType(),newHead,dep);
							markedForAddition.add(d);
							log.log(Level.FINE, "Adding dependency due to PP-attachment correction: {0}", new Object[]{d.toString()});
						}
					}
				}
			}
		}
		sent.synchEmbeddings(markedForRemoval,markedForAddition);
	}

}
