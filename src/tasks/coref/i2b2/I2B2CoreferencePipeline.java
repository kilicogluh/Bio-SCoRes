package tasks.coref.i2b2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.agreement.AnimacyAgreement;
import gov.nih.nlm.bioscores.agreement.ExactStringAgreement;
import gov.nih.nlm.bioscores.agreement.GenderAgreement;
import gov.nih.nlm.bioscores.agreement.HeadWordAgreement;
import gov.nih.nlm.bioscores.agreement.NonPostModifierMatchAgreement;
import gov.nih.nlm.bioscores.agreement.NumberAgreement;
import gov.nih.nlm.bioscores.agreement.PersonAgreement;
import gov.nih.nlm.bioscores.agreement.RelaxedStemAgreement;
import gov.nih.nlm.bioscores.agreement.SemanticTypeAgreement;
import gov.nih.nlm.bioscores.agreement.SyntacticAppositiveAgreement;
import gov.nih.nlm.bioscores.candidate.CandidateFilter;
import gov.nih.nlm.bioscores.candidate.CandidateSalience;
import gov.nih.nlm.bioscores.candidate.DefaultCandidateFilter;
import gov.nih.nlm.bioscores.candidate.PostScoringCandidateFilter;
import gov.nih.nlm.bioscores.candidate.PostScoringCandidateFilterImpl;
import gov.nih.nlm.bioscores.candidate.PriorDiscourseFilter;
import gov.nih.nlm.bioscores.candidate.SemanticClassFilter;
import gov.nih.nlm.bioscores.candidate.SyntaxBasedCandidateFilter;
import gov.nih.nlm.bioscores.candidate.WindowSizeFilter;
import gov.nih.nlm.bioscores.core.Configuration;
import gov.nih.nlm.bioscores.core.CoreferenceChain;
import gov.nih.nlm.bioscores.core.CoreferenceResolver;
import gov.nih.nlm.bioscores.core.CoreferenceSemanticItemFactory;
import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.CoreferenceUtils;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.core.ExpressionRecognition;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.bioscores.core.GenericCoreferencePipeline;
import gov.nih.nlm.bioscores.core.ScoringFunction;
import gov.nih.nlm.bioscores.core.Strategy;
import gov.nih.nlm.bioscores.core.SurfaceElementChain;
import gov.nih.nlm.bioscores.exp.ExpressionFilter;
import gov.nih.nlm.bioscores.exp.PossessivePronounOps;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Section;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.io.XMLEntityReader;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.process.ComponentLoader;
import gov.nih.nlm.ling.process.SectionSegmenter;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.DomainProperties;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;
import gov.nih.nlm.ling.util.FileUtils;
import gov.nih.nlm.ling.wrappers.WordNetWrapper;

/**
 * The pipeline to resolve coreference in i2b2/VA coreference corpus. <p>
 * 
 * This class focuses on Task 1C of 2011 i2b2 challenge. No specific mention 
 * detection step is performed, but the existing gold markables are preprocessed
 * to detect their fine-grained type to be used by the pipeline. 
 * 
 * Two types of coreference are handled: {@link CoreferenceType#Anaphora} and 
 * {@link CoreferenceType#Appositive}.
 * 
 * @author Halil Kilicoglu
 *
 */
public class I2B2CoreferencePipeline {
	private static Logger log = Logger.getLogger(I2B2CoreferencePipeline.class.getName());
	
	private static Configuration config = null;
	private static SectionSegmenter sectionSegmenter = null;
	private static final List<String> termTypes = Arrays.asList("pronoun","test","treatment","person","problem");
	private static final List<String> chainTypes = Arrays.asList("test","treatment","person","problem");
	
	/**
	 * Identifies the fine-grained types of existing markables in a document.
	 * 
	 * @param doc	the document to process
	 */
	public static synchronized void identifyMarkableTypes(Document doc) {
		CoreferenceSemanticItemFactory  sif = (CoreferenceSemanticItemFactory)doc.getSemanticItemFactory();
		List<Sentence> sentences = doc.getSentences();
		if (sentences == null) return;
		List<Sentence> modSentences = new ArrayList<Sentence>();
		for (int i=0; i < sentences.size(); i++) {
			Sentence sent = sentences.get(i);
			List<SurfaceElement> surfs = sent.getSurfaceElements();
			if (surfs == null) continue;
			for (SurfaceElement se: surfs) {
				LinkedHashSet<SemanticItem> exps = se.filterByEntities();
				if (exps.size() == 0) continue;
				ExpressionType type = ExpressionRecognition.getMentionType(se);
				if (type == ExpressionType.ZeroArticleNP && PossessivePronounOps.isPossessive(se))  
					type = ExpressionType.PossessivePronoun;
				if (type != null) {
					if (type == ExpressionType.PossessivePronoun) {
						Word poss = PossessivePronounOps.getPossessivePronoun(se);
						Expression exp = sif.newExpression(doc, type.toString(), poss);
						if (se.equals(poss) == false) {
							exp = sif.newExpression(doc, "DefiniteNP", se);
							log.log(Level.FINE, "Identified coreferential mention: {0}.", exp.toString());
							exp = sif.newExpression(doc, "ZeroArticleNP", se);
							log.log(Level.FINE, "Identified coreferential mention: {0}.", exp.toString());
						}
					} 
					else {
						Expression exp = sif.newExpression(doc, type.toString(), se);
						log.log(Level.FINE, "Identified coreferential mention: {0}.", exp.toString());
					}
				} 
			}
			modSentences.add(sent);
		}
		doc.setSentences(modSentences);
	}
	
	/**
	 * Loads the resolution strategies that yield the best results on the i2b2/VA corpus.
	 * 
	 * @return	the best resolution strategies for this pipeline
	 */
	public static List<Strategy> loadStrategies() {
		List<Strategy> defs = new ArrayList<>();
		
		// mention filter for anaphora
		List<ExpressionFilter> anaExpFilters = new ArrayList<ExpressionFilter>();
		anaExpFilters.add(new I2B2AnaphoricityFilter());
		
		// generic candidate filters for anaphora
		List<CandidateFilter> anaCandFilters = new ArrayList<>();
		anaCandFilters.add(new PriorDiscourseFilter());
		anaCandFilters.add(new WindowSizeFilter(Configuration.RESOLUTION_WINDOW_SECTION));
		anaCandFilters.add(new SyntaxBasedCandidateFilter());
		anaCandFilters.add(new SemanticClassFilter(DefaultCandidateFilter.DEFAULT_SEMANTIC_CLASSES));
		// candidate filters for possessive anaphors
		List<CandidateFilter> possFilters = new ArrayList<>();
		possFilters.add(new PriorDiscourseFilter());
		possFilters.add(new WindowSizeFilter(Configuration.RESOLUTION_WINDOW_SECTION));
		possFilters.add(new SemanticClassFilter(DefaultCandidateFilter.DEFAULT_SEMANTIC_CLASSES));
		// candidate filters for relative pronominal anaphors
		List<CandidateFilter> relFilters = new ArrayList<CandidateFilter>();
		relFilters.add(new PriorDiscourseFilter());
		relFilters.add(new WindowSizeFilter(Configuration.RESOLUTION_WINDOW_SENTENCE));
		relFilters.add(new SemanticClassFilter(DefaultCandidateFilter.DEFAULT_SEMANTIC_CLASSES));
		// candidate filters for zero-article anaphors
		List<CandidateFilter> zeroArticleNPFilters = new ArrayList<>();
		zeroArticleNPFilters.add(new PriorDiscourseFilter());
		zeroArticleNPFilters.add(new WindowSizeFilter(Configuration.RESOLUTION_WINDOW_ALL));
		zeroArticleNPFilters.add(new SyntaxBasedCandidateFilter());
		zeroArticleNPFilters.add(new SemanticClassFilter(DefaultCandidateFilter.DEFAULT_SEMANTIC_CLASSES));
		
		// generic pronominal agreement 
		List<ScoringFunction> pronominalScoringFunction = new ArrayList<>();
		pronominalScoringFunction.add(new ScoringFunction(AnimacyAgreement.class,1,0));
		pronominalScoringFunction.add(new ScoringFunction(GenderAgreement.class,1,0));
		pronominalScoringFunction.add(new ScoringFunction(NumberAgreement.class,1,0));
		pronominalScoringFunction.add(new ScoringFunction(PersonAgreement.class,1,0));
		// relative pronoun agreement
		List<ScoringFunction> relPronounScoringFunction = new ArrayList<>();
		relPronounScoringFunction.add(new ScoringFunction(AnimacyAgreement.class,1,0));
		// generic nominal agreement
		List<ScoringFunction> nominalScoringFunction = new ArrayList<>();
		nominalScoringFunction.add(new ScoringFunction(NumberAgreement.class,1,1));
		nominalScoringFunction.add(new ScoringFunction(SemanticTypeAgreement.class,2,2));
		nominalScoringFunction.add(new ScoringFunction(HeadWordAgreement.class,2,0));
		nominalScoringFunction.add(new ScoringFunction(ExactStringAgreement.class,2,0));
		nominalScoringFunction.add(new ScoringFunction(NonPostModifierMatchAgreement.class,2,0));
		nominalScoringFunction.add(new ScoringFunction(RelaxedStemAgreement.class,2,0));
		// zero article agreement
		List<ScoringFunction> zeroArticleNPScoringFunction = new ArrayList<>();
		zeroArticleNPScoringFunction.add(new ScoringFunction(NumberAgreement.class,1,3));
		zeroArticleNPScoringFunction.add(new ScoringFunction(ExactStringAgreement.class,4,0));
		zeroArticleNPScoringFunction.add(new ScoringFunction(NonPostModifierMatchAgreement.class,4,0));
		zeroArticleNPScoringFunction.add(new ScoringFunction(KeyValuePairAgreement.class,4,0));
		zeroArticleNPScoringFunction.add(new ScoringFunction(RelaxedStemAgreement.class,3,0));
		
		// generic post-scoring filter
		PostScoringCandidateFilterImpl postScoringFilterImpl = new PostScoringCandidateFilterImpl();
		List<PostScoringCandidateFilter> postScoringFilters = new ArrayList<>();
		postScoringFilters.add(postScoringFilterImpl.new ThresholdFilter(4));
		postScoringFilters.add(postScoringFilterImpl.new TopScoreFilter());
		postScoringFilters.add(postScoringFilterImpl.new SalienceTypeFilter(CandidateSalience.Type.Proximity));
		
		// relative pronoun post-scoring filter
		List<PostScoringCandidateFilter> relPostScoringFilters = new ArrayList<>();
		relPostScoringFilters.add(postScoringFilterImpl.new ThresholdFilter(1));
		relPostScoringFilters.add(postScoringFilterImpl.new TopScoreFilter());
		relPostScoringFilters.add(postScoringFilterImpl.new SalienceTypeFilter(CandidateSalience.Type.Proximity));
			
		// anaphora strategies
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.PersonalPronoun,
				null,anaCandFilters,pronominalScoringFunction,postScoringFilters));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.PossessivePronoun,
				null,possFilters,pronominalScoringFunction,postScoringFilters));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DistributivePronoun,
				null,anaCandFilters,pronominalScoringFunction,postScoringFilters));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.ReciprocalPronoun,
				null,anaCandFilters,pronominalScoringFunction,postScoringFilters));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.RelativePronoun,
				null,relFilters,relPronounScoringFunction,relPostScoringFilters));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DefiniteNP,
				anaExpFilters,anaCandFilters,nominalScoringFunction,postScoringFilters));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DemonstrativeNP,
				anaExpFilters,anaCandFilters,nominalScoringFunction,postScoringFilters));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DistributiveNP,
				anaExpFilters,anaCandFilters,nominalScoringFunction,postScoringFilters));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.ZeroArticleNP,
				null,zeroArticleNPFilters,zeroArticleNPScoringFunction,postScoringFilters));
				
		// appositive candidate filters
		List<CandidateFilter> apposFilters = new ArrayList<>();
		apposFilters.add(new WindowSizeFilter(Configuration.RESOLUTION_WINDOW_SENTENCE));
		apposFilters.add(new SemanticClassFilter(DefaultCandidateFilter.DEFAULT_SEMANTIC_CLASSES));
		// appositive agreement 
		List<ScoringFunction> apposScoringFunction = new ArrayList<>();
		apposScoringFunction.add(new ScoringFunction(NumberAgreement.class,1,1));
		apposScoringFunction.add(new ScoringFunction(SyntacticAppositiveAgreement.class,3,2));
		// appositive strategies
		defs.add(new Strategy(CoreferenceType.Appositive,ExpressionType.DefiniteNP,
				null,apposFilters,apposScoringFunction,postScoringFilters));
		defs.add(new Strategy(CoreferenceType.Appositive,ExpressionType.IndefiniteNP,
				null,apposFilters,apposScoringFunction,postScoringFilters));
			
		return defs;
	}
	
	/**
	 * Task-specific preprocessing step, in which document is segmented into sections.<p>
	 * The section segmenter to use is provided as an input parameter.
	 * 
	 * @param doc		the document to process
	 * @param segmenter	the section segmenter to use
	 */
	public static void domainSpecificPreProcessing(Document doc, SectionSegmenter segmenter) {
		GenericCoreferencePipeline.segmentSections(doc,segmenter);
	}
	
	/**
	 * Resolves coreference in a single i2b2/VA document. <p> 
	 * It processes the gold markables to identify their fine-grained mention types and
	 * identifies coreference relations between them.
	 * For the i2b2/VA corpus, relations considered are anaphora and appositive relations.
	 * 
	 * @param doc	the document to process
	 */
	public static void coreferenceResolution(Document doc) {
		log.info("Identifying markable types...");
		identifyMarkableTypes(doc);
		log.info("Resolving coreference...");
		mentionReferentLinking(doc);
		log.info("Post-processing...");
		postProcessing(doc);
		log.info("Completed coreference resolution...");
	}
	
	/**
	 * Links coreferential mentions to their referents and generates <code>CoreferenceChain</code>
	 * objects.
	 * 
	 * @param doc	the document to process
	 */
	public static void mentionReferentLinking(Document doc) {
		List<SurfaceElementChain> anaphoraLinks = new ArrayList<>();
		CoreferenceResolver.processDocumentForAnaphora(doc,anaphoraLinks); 

		List<SurfaceElementChain> appositives = new ArrayList<>();
		CoreferenceResolver.processDocumentForAppositive(doc,appositives);

		for (SurfaceElementChain link : anaphoraLinks) {
			generateCoreferenceChain(doc,link);
		} 
		for (SurfaceElementChain link : appositives) {
			generateCoreferenceChain(doc,link);
		}
		log.log(Level.INFO,"Extracted {0} potential coreference chains from the document.", Document.getSemanticItemsByClass(doc, CoreferenceChain.class).size());
		for (SemanticItem si: Document.getSemanticItemsByClass(doc, CoreferenceChain.class)) {
			log.log(Level.FINE,"Extracted coreference chain: {0}.",si.toString());
		}
	}
	
	/**
	 * Adds a <code>CoreferenceChain</code> object to the document, given the textual units involved in it.
	 * It performs consistency checks, merges mentions into other coreference chains, and handles chains
	 * involving coordinated items.
	 * 
	 * @param doc  	 the document being processed
	 * @param chain  the chain of textual units 
	 */
	public static void generateCoreferenceChain(Document doc, SurfaceElementChain chain) {
		Strategy strategy = chain.getStrategy();
		SurfaceElement exp = chain.getExpression();
		List<SurfaceElement> referents = chain.getReferents();
		if (strategy == null || exp == null || referents == null || referents.size() == 0) return;
		CoreferenceSemanticItemFactory sif = (CoreferenceSemanticItemFactory)doc.getSemanticItemFactory();
		
		// find the semantic item to use among those associated with the mention
		CoreferenceType type = strategy.getCorefType();	
		Entity compatibleExp = (Entity)CoreferenceUtils.getCompatibleExpression(strategy.getExpType(),exp);
		List<SemanticItem> compatibleExpSems = new ArrayList<>();
		List<String> compatibleTypes = new ArrayList<>();
		for (SemanticItem sem: Document.getSemanticItemsByClassSpan(doc,Entity.class,compatibleExp.getSpan(),false)) {
			if (sem.equals(compatibleExp) || sem instanceof Expression) continue;
			if (sem instanceof Entity) {
				if (((Entity)sem).getSpan().equals(compatibleExp.getSpan()) && termTypes.contains(sem.getType())){
					compatibleExpSems.add(sem);
					compatibleTypes.add(sem.getType());
				}
			}
		}
		// find whether any of the semantic items associated with the mention are part of another chain already
		CoreferenceChain existingExpChain = null;
		boolean foundChain = false;
		for (SemanticItem comp: compatibleExpSems) {
			
			LinkedHashSet<CoreferenceChain> ccs = CoreferenceChain.getChainsWithTerm(comp);
			if (ccs.size() > 0) { existingExpChain = ccs.iterator().next(); foundChain = true;}
			if (foundChain) break;
		}
		// on the referent side, find the semantic items that are compatible with the mentions selected
		List<Argument> args = new ArrayList<>();	
		SemanticItem expSem = null;
		for (SurfaceElement ref: referents) {
			if (ref.hasSemantics() == false) continue;
			Set<SemanticItem> compatibleRefSems = new HashSet<>();
			SemanticItem compatibleRefSem = null;
			for (SemanticItem refs: ref.getSemantics()) {
				if (refs instanceof Expression) continue;
				if (refs instanceof Entity) {
					if (compatibleTypes.contains(refs.getType())) {
							compatibleRefSem = refs;
							compatibleRefSems.add(refs);
							int index = compatibleTypes.indexOf(refs.getType());
							expSem = compatibleExpSems.get(index);
					} 	else if (compatibleTypes.contains("pronoun")) {
						if (chainTypes.contains(refs.getType()) == false) continue;			
						compatibleRefSem = refs;
						compatibleRefSems.add(refs);
						int index = compatibleTypes.indexOf("pronoun");
						expSem = compatibleExpSems.get(index);
					}
				}
			}
			// compatible semantic objects cannot be found on either element, skip
			if (compatibleRefSem == null || expSem == null) {
				log.log(Level.FINE, "Unable to identify compatible elements in mentions {0} and {1}. Skipping...",
						new Object[]{exp.toString(),ref.toString()});
				continue;
			}
				
			LinkedHashSet<CoreferenceChain> chains = CoreferenceChain.getChainsWithArgument(ref);
			if (chains.size() > 0) {
				boolean expReferent = false;
				boolean deleteExpChain = false;
				for (CoreferenceChain cc: chains) {
					// find the semantic item that was used for this chain from this referent
					SemanticItem refSem = getRelevantSemanticItem(cc,ref);
					// this is not a chain we are interested in, since a different semantic item was used for it
					if (compatibleRefSems.contains(refSem) == false) continue;
					compatibleRefSem = refSem;
					// start a new chain
					if (expSem != null && existingExpChain == null) {
						cc.addArgument(new Argument(type.getExpRole().toString(),expSem));
						expReferent = true;
					// merge to an existing chain and remove the newly formed chain
					} else if (existingExpChain != null && existingExpChain.equals(cc) == false) {
						deleteExpChain = true;
						for (Argument arg: existingExpChain.getArguments()) {
							SemanticItem argsem = arg.getArg();
							if (unwantedSemanticItem(cc,argsem)) continue;
							cc.addArgument(new Argument(arg.getType(),argsem));
						}
						expReferent = true;
					}
				}
				if (deleteExpChain) {
					doc.removeSemanticItem(existingExpChain);
				}
				// the link under consideration was merged so return
				if (expReferent) return;
			}

			// mention appears between two compatible mentions, add it to the chain, as well
			List<SemanticItem> refArgs = new ArrayList<>();
			refArgs.add(compatibleRefSem);
			List<SemanticItem> seen = new ArrayList<>();
			for (SemanticItem sem: refArgs) {
				if (seen.contains(sem)) continue;
				SemanticItem closer = CoreferenceUtils.closerWithSameOntology(doc,exp,sem);
				if (closer != null && refArgs.contains(closer) == false)
					args.add(new Argument(type.getRefRole().toString(),closer));
				args.add(new Argument(type.getRefRole().toString(),sem));
				seen.add(sem);
			}
			if (expSem != null)
				args.add(new Argument(type.getExpRole().toString(),expSem));
		}

		if (args.size() > 0) {
			if (existingExpChain == null) {
				CoreferenceChain cchain = sif.newCoreferenceChain(doc, type.toString(),args);
				log.log(Level.FINE,"New coreference chain generated: {0}.", cchain.toString());
			} else {
				// or add the arguments to the exsiting chain
				for (Argument arg: args) {
					if (unwantedSemanticItem(existingExpChain,arg.getArg())) continue;
					existingExpChain.addArgument(arg);
				}
			}
		}
	}
	// get the semantic item associated with a textual unit that takes part in a chain
	private static SemanticItem getRelevantSemanticItem(CoreferenceChain cc, SurfaceElement surf) {
		List<SemanticItem> args = cc.getArgItems();
		for (SemanticItem arg: args) {
			if (arg instanceof Term) {
				if (((Term)arg).getSurfaceElement().equals(surf)) return arg;
			}
		}
		return null;
	}
	
	// a semantic item is unwanted for a chain, if it is not a term or if it is
	// already contained in the chain
	private static boolean unwantedSemanticItem(CoreferenceChain cc, SemanticItem sem) {
		return (sem instanceof Term == false || existingSpans(cc).contains(((Term)sem).getSpan()));
	}
	
	private static LinkedHashSet<SpanList> existingSpans(CoreferenceChain cc) {
		LinkedHashSet<SpanList> spans = new LinkedHashSet<SpanList>();
		for (SemanticItem sem: cc.getArgItems()) {
			if (sem instanceof Term) {
				spans.add(((Term)sem).getSpan());
			}
		}
		return spans;
	}
	
	/**
	 * Post-processes the generated coreference chains for consistency and relevance.<p>
	 * This method merges coreference chains extracted at the section level into existing 
	 * chain, based on {@link ExactStringAgreement} constraint. All singleton mentions
	 * with the text <i>patient</i> are clustered with the existing patient chain. Clusters
	 * with second person singular (<i>you</i> etc.) are also merged with the patient chain.
	 * All remaining singletons are discarded.
	 * 
	 * @param doc	the document to process
	 */
	public static void postProcessing(Document doc) {
		LinkedHashSet<SemanticItem> chainsSoFar = new LinkedHashSet<>();
		// merge the chains formed within the section with the previous chain if possible
		for (Section sect: doc.getSections()) {
			LinkedHashSet<SemanticItem> remove = new LinkedHashSet<>();
			LinkedHashSet<SemanticItem> sectChains = Document.getSemanticItemsByClassSpan(doc, CoreferenceChain.class, new SpanList(sect.getTextSpan()), true);
			sectChains.addAll(getSingletons(doc,sect.getTextSpan()));
			if (chainsSoFar.size() == 0)  {
				chainsSoFar.addAll(sectChains); 
				continue;
			}
			for (SemanticItem ch: chainsSoFar) {
				for (SemanticItem sch: sectChains) {
					if (chainsSoFar.contains(sch)) continue;
					String schType = I2B2CorefWriter.getChainType(sch);
					if (schType == null) {remove.add(sch); continue;}
					// test chains are limited to sections
					if (schType.equals("test")) continue;
					if (compatibleMentions(ch,sch)) {
						if (ch instanceof CoreferenceChain) {
							CoreferenceChain chcc = (CoreferenceChain)ch;
							if (sch instanceof CoreferenceChain) {
								CoreferenceChain schcc = (CoreferenceChain)sch;
								remove.add(sch);
								chcc.addArguments(schcc.getArguments());
							} else {
								CoreferenceType.Role role = CoreferenceType.valueOf(chcc.getType()).getRefRole();
								chcc.addArgument(new Argument(role.toString(),sch));
							}
						} else {
							if (sch instanceof CoreferenceChain) {
								CoreferenceChain schcc = (CoreferenceChain)sch;
								CoreferenceType.Role role = CoreferenceType.valueOf(schcc.getType()).getRefRole();
								schcc.addArgument(new Argument(role.toString(),ch));
							} else {
								SurfaceElement expSurf = ((Entity)sch).getSurfaceElement();
								SurfaceElement refSurf = ((Entity)ch).getSurfaceElement();
								LinkedHashSet<ExpressionType> types = ExpressionType.getTypes(expSurf);
								ExpressionType type = (types.size() ==0 ? null: types.iterator().next());
								if (type == null) {
									log.log(Level.SEVERE,"Null cluster type: {0}.", expSurf.toString());
								}
								// we should probably not create a chain if the type is null - 01/23/16
								SurfaceElementChain nch = new SurfaceElementChain(
										config.getStrategy(CoreferenceType.Anaphora,type),
										expSurf,Arrays.asList(refSurf));
								generateCoreferenceChain(doc,nch);
							}
						}
					}
				}
				for (SemanticItem rem: remove) doc.removeSemanticItem(rem);
			}
			chainsSoFar = Document.getSemanticItemsByClassSpan(doc, CoreferenceChain.class, new SpanList(0,sect.getTextSpan().getEnd()), false);
			chainsSoFar.addAll(getSingletons(doc,new Span(0,sect.getTextSpan().getEnd())));
		}	
		postProcessForSingletonPatients(doc);
		combinePatientChains(doc);
		log.log(Level.INFO,"{0} coreference chains after post-processing from the document.", Document.getSemanticItemsByClass(doc, CoreferenceChain.class).size());
		for (SemanticItem si: Document.getSemanticItemsByClass(doc, CoreferenceChain.class)) {
			log.log(Level.FINE,"Generated coreference chain after post-processing: {0}.", si.toString());
		}
	}
	
	// get unclustered semantic items
	private static LinkedHashSet<SemanticItem> getSingletons(Document doc, Span sp) {
		LinkedHashSet<SemanticItem> singletons = new LinkedHashSet<>();
		for (SemanticItem ent: Document.getSemanticItemsByClassSpan(doc, Entity.class, new SpanList(sp), false)) {
			if (CoreferenceChain.getChainsWithArgument(((Entity)ent).getSurfaceElement()).size() == 0) singletons.add(ent);
		}
		return singletons;
	}
	
	//determine whether two semantic items are compatible with respect to coreference
	// exact match is required for the terms involved.
	private static boolean compatibleMentions(SemanticItem a, SemanticItem b) {
		CoreferenceChain ach = null;
		CoreferenceChain bch = null;
		Entity aent = null;
		Entity bent = null;
		if (a instanceof CoreferenceChain) ach = (CoreferenceChain)a;
		if (b instanceof CoreferenceChain) bch = (CoreferenceChain)b;
		if (a instanceof Entity) aent = (Entity)a;
		if (b instanceof Entity) bent = (Entity)b;
		Document doc = a.getDocument();
		if (ach == null && bch == null) {
			if (aent.getType().equals("pronoun") || bent.getType().equals("pronoun")) return false;
			if (aent.getSpan().length() != bent.getSpan().length()) return false;
			String asp = doc.getStringInSpan(aent.getSpan());
			String bsp = doc.getStringInSpan(bent.getSpan());
			return (asp.equalsIgnoreCase(bsp));
		} else if (ach == null) {
			for (SemanticItem arg: bch.getArgItems()) 
				if (compatibleMentions(a,arg)) return true;
			return false;
		} else if (bch == null) {
			for (SemanticItem arg: ach.getArgItems()) 
				if (compatibleMentions(arg,b)) return true;
			return false;
		} else {
			for (SemanticItem aarg: ach.getArgItems()) {
				for (SemanticItem barg: bch.getArgItems()) {
					if (compatibleMentions(aarg,barg)) return true;
				}
			}
		}
		return false;
	}
		
	// tries to find the patient chain and adds all mentions that contain the word 'patient' to that chain.
	// Somewhat ad hoc, and not sure it is useful outside this particular task.
	private static synchronized void postProcessForSingletonPatients(Document doc) {
		CoreferenceChain patientChain = getPatientChain(doc);
		if( patientChain == null) return;
//		LinkedHashSet<SemanticItem> corefs = Document.getSemanticItemsByClass(doc, CoreferenceChain.class);
		for (Sentence sent: doc.getSentences()) {
			for (SurfaceElement surf: sent.getSurfaceElements()) {
				if (surf.getText().toLowerCase().contains("patient")) {
					LinkedHashSet<SemanticItem> sems = surf.getSemantics();
					if (sems == null) continue;
					for (SemanticItem sem: sems) {
						if (sem instanceof Entity) {
							if (patientChain.getArgItems().contains(sem)) continue;
							Entity ent = (Entity)sem;
							if (ent.getAllSemtypes().contains("person") && ent.getText().toLowerCase().contains("patient")) {
								patientChain.addArgument(new Argument("Equiv",ent));
							}
						}
					}
				}
			}
		}
	}
	
	// adds chains with second person singular to the patient chain
	private static synchronized void combinePatientChains(Document doc) {
		CoreferenceChain patientChain = getPatientChain(doc);
		if( patientChain == null) return;
		LinkedHashSet<SemanticItem> corefs = Document.getSemanticItemsByClass(doc, CoreferenceChain.class);
		Set<SemanticItem> remove = new HashSet<SemanticItem>();
		for (SemanticItem coref: corefs) {
			if (coref.equals(patientChain)) continue;
			if (coref instanceof CoreferenceChain == false) continue;
			CoreferenceChain cc = (CoreferenceChain)coref;
			for (SemanticItem arg : cc.getArgItems()) {
				Entity ent = (Entity)arg;
				if (ent.getText().equalsIgnoreCase("you") || ent.getText().equalsIgnoreCase("your") || 
						ent.getText().equalsIgnoreCase("yourself")) { 
					remove.add(coref);
					patientChain.addArguments(cc.getArguments());
					break;
				}
			}
		}
		for (SemanticItem rem: remove)
			doc.removeSemanticItem(rem);
	}
	
	private static CoreferenceChain getPatientChain(Document doc) {
		LinkedHashSet<SemanticItem> corefs = Document.getSemanticItemsByClass(doc, CoreferenceChain.class);
		for (SemanticItem coref: corefs) {
			CoreferenceChain cc = (CoreferenceChain)coref;
			for (SemanticItem arg : cc.getArgItems()) {
				Entity ent = (Entity)arg;
				if (ent.getText().toLowerCase().equalsIgnoreCase("the patient") || ent.getText().equalsIgnoreCase("patient") || 
						ent.getText().equalsIgnoreCase("pt") || ent.getText().equalsIgnoreCase("pt.")) { 
					return cc;
				}
			}
		}
		return null;
	}
	
	/**
	 * Reads properties from <code>coref_i2b2.properties</code> file and 
	 * initializes WordNet, resolution strategies and coreference-related word lists. 
	 * It also performs section segmentation.
	 * 
	 * @throws ClassNotFoundException	if the section segmenter class cannot be found
	 * @throws IllegalAccessException	if the section segmenter cannot be accessed
	 * @throws InstantiationException	if the section segmenter cannot be initialized
	 * @throws IOException				if properties file cannot be found
	 */
	public static void init() 
			throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Properties props = FileUtils.loadPropertiesFromFile("coref_i2b2.properties");
		props.put("sectionSegmenter", "tasks.coref.i2b2.I2B2SectionSegmenter");
		DomainProperties.init(props);
		WordNetWrapper.getInstance(props);
		config = Configuration.getInstance(loadStrategies());			
		sectionSegmenter = ComponentLoader.getSectionSegmenter(props);
	}
	
	public static void main(String[] args) throws ClassNotFoundException, IOException, 
	InstantiationException, IllegalAccessException {
		if (args.length < 2) {
			System.err.print("Usage: inputXMLDirectory chainOutputDirectory");
			System.exit(1);
		}
		String in = args[0];
		String chainOut = args[1];

		File inDir = new File(in);
		if (inDir.isDirectory() == false) {
			System.err.println("First argument is required to be an input XML directory:" + in);
			System.exit(1);
		}
		File chainOutDir = new File(chainOut);
		if (chainOutDir.isDirectory() == false) {
			System.err.println("The directory " + chainOut + " doesn't exist. Creating a new directory..");
			chainOutDir.mkdir();
		}

		// initialize 
		init();

		// annotations to load
		Map<Class<? extends SemanticItem>,List<String>> annTypes = new HashMap<>();
		annTypes.put(Entity.class,termTypes);
		XMLReader reader = new XMLReader();
		reader.addAnnotationReader(Entity.class, new XMLEntityReader());

		// iterate through files
		List<String> files = FileUtils.listFiles(in, false, "xml");
		if (files.size() == 0) log.log(Level.SEVERE, "No XML file found in input directory {0}.", new Object[]{in});
		int fileNum = 0;
		for (String filename: files) {
			String filenameNoExt = filename.replace(".xml", "");
			filenameNoExt = filenameNoExt.substring(filenameNoExt.lastIndexOf(File.separator)+1);
			log.log(Level.INFO,"Processing file {0}: {1}.", new Object[]{filenameNoExt,++fileNum});
			String chainFilename = chainOutDir.getAbsolutePath() + File.separator + filenameNoExt + ".chains";
			Document doc = reader.load(filename, true, CoreferenceSemanticItemFactory.class,annTypes, null);
			LinkedHashSet<SemanticItem> terms = Document.getSemanticItemsByClass(doc, Term.class);
			if (terms != null) {
				for (SemanticItem ent: terms) 
					log.log(Level.FINE,"Loaded term: {0}." , ((Term)ent).toString());
			}
			GenericCoreferencePipeline.linguisticPreProcessing(doc);

			domainSpecificPreProcessing(doc,sectionSegmenter);
			// coreference
			coreferenceResolution(doc);
			// write
			List<Class<? extends SemanticItem>> writeTypes = new ArrayList<>(annTypes.keySet());
			writeTypes.add(Expression.class);
			writeTypes.add(CoreferenceChain.class);
			I2B2CorefWriter.writeCoref(doc,chainFilename);
		}
	}	
}
