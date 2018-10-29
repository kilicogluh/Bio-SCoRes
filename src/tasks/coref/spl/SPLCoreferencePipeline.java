package tasks.coref.spl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections15.CollectionUtils;

import gov.nih.nlm.bioscores.agreement.AnimacyAgreement;
import gov.nih.nlm.bioscores.agreement.DiscourseConnectiveAgreement;
import gov.nih.nlm.bioscores.agreement.GenderAgreement;
import gov.nih.nlm.bioscores.agreement.HypernymListAgreement;
import gov.nih.nlm.bioscores.agreement.NumberAgreement;
import gov.nih.nlm.bioscores.agreement.PersonAgreement;
import gov.nih.nlm.bioscores.agreement.PredicateNominativeAgreement;
import gov.nih.nlm.bioscores.agreement.SyntacticAppositiveAgreement;
import gov.nih.nlm.bioscores.candidate.CandidateFilter;
import gov.nih.nlm.bioscores.candidate.CandidateSalience;
import gov.nih.nlm.bioscores.candidate.DefaultCandidateFilter;
import gov.nih.nlm.bioscores.candidate.ExemplificationFilter;
import gov.nih.nlm.bioscores.candidate.PostScoringCandidateFilter;
import gov.nih.nlm.bioscores.candidate.PostScoringCandidateFilterImpl;
import gov.nih.nlm.bioscores.candidate.PriorDiscourseFilter;
import gov.nih.nlm.bioscores.candidate.SubsequentDiscourseFilter;
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
import gov.nih.nlm.bioscores.exp.ExpressionFilterImpl;
import gov.nih.nlm.bioscores.io.XMLExpressionReader;
import gov.nih.nlm.ling.brat.StandoffAnnotationWriter;
import gov.nih.nlm.ling.core.AbstractSurfaceElement;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.io.XMLEntityReader;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.process.ComponentLoader;
import gov.nih.nlm.ling.process.SectionSegmenter;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Conjunction;
import gov.nih.nlm.ling.sem.ConjunctionDetection;
import gov.nih.nlm.ling.sem.DomainProperties;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;
import gov.nih.nlm.ling.util.FileUtils;
import gov.nih.nlm.ling.util.SemUtils;
import gov.nih.nlm.ling.wrappers.WordNetWrapper;

/**
 * The pipeline used to resolve coreference in SPL drug coreference dataset.
 * This pipeline deals with all four coreference types: {@link CoreferenceType#Anaphora}, 
 * {@link CoreferenceType#Cataphora}, {@link CoreferenceType#Appositive}, and 
 * {@link CoreferenceType#PredicateNominative}.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SPLCoreferencePipeline {
	private static Logger log = Logger.getLogger(SPLCoreferencePipeline.class.getName());
	
	private static SectionSegmenter sectionSegmenter = null;
	private static boolean goldExp = false;
	private static int chainId = 0;
	
	/**
	 * Loads the resolution strategies used for the SPL coreference dataset.
	 * 
	 * @return	the list of resolution strategies.
	 */
	public static List<Strategy> loadSPLStrategies() {
		List<Strategy> defs = new ArrayList<>();
		
		// generic anaphoric mention filter
		ExpressionFilterImpl expressionFilterImpl = new ExpressionFilterImpl();
		List<ExpressionFilter> anaphoricExpFilters = new ArrayList<>();
		anaphoricExpFilters.add(expressionFilterImpl.new AnaphoricityFilter());
		// possessive pronoun filter
		List<ExpressionFilter> possPrExpFilters = new ArrayList<>();
		possPrExpFilters.add(expressionFilterImpl.new ThirdPersonPronounFilter());
		// personal pronoun filter
		List<ExpressionFilter> personalPrExpFilters = new ArrayList<>(possPrExpFilters);
		if (!goldExp) personalPrExpFilters.add(expressionFilterImpl.new PleonasticItFilter());
		
		// generic pronoun referent filters
		List<CandidateFilter> pronounFilters = new ArrayList<>();
		pronounFilters.add(new PriorDiscourseFilter());
		pronounFilters.add(new WindowSizeFilter(2));
		pronounFilters.add(new SyntaxBasedCandidateFilter());
		pronounFilters.add(new DefaultCandidateFilter());
		pronounFilters.add(new ExemplificationFilter());
		// possessive pronoun referent filter
		List<CandidateFilter> possFilters = new ArrayList<>();
		possFilters.add(new PriorDiscourseFilter());
		possFilters.add(new WindowSizeFilter(2));
		possFilters.add(new DefaultCandidateFilter());
		possFilters.add(new ExemplificationFilter());
		// nominal anaphora referent filter
		List<CandidateFilter> npFilters = new ArrayList<>();
		npFilters.add(new PriorDiscourseFilter());
		npFilters.add(new SyntaxBasedCandidateFilter());
		npFilters.add(new DefaultCandidateFilter());
		npFilters.add(new ExemplificationFilter());
		
		// pronominal agreement methods
		List<ScoringFunction> pronounScoringFunction = new ArrayList<>();
		pronounScoringFunction.add(new ScoringFunction(AnimacyAgreement.class,1,0));
		pronounScoringFunction.add(new ScoringFunction(GenderAgreement.class,1,0));
		pronounScoringFunction.add(new ScoringFunction(NumberAgreement.class,1,0));
		pronounScoringFunction.add(new ScoringFunction(PersonAgreement.class,1,0));
		// nominal agreement methods
		List<ScoringFunction> nominalScoringFunction = new ArrayList<>();
		nominalScoringFunction.add(new ScoringFunction(NumberAgreement.class,1,1));
		nominalScoringFunction.add(new ScoringFunction(HypernymListAgreement.class,3,0));
		
		// generic post-scoring filters
		PostScoringCandidateFilterImpl postScoringFilterImpl = new PostScoringCandidateFilterImpl();
		List<PostScoringCandidateFilter> postScoringFilters = new ArrayList<>();
		postScoringFilters.add(postScoringFilterImpl.new ThresholdFilter(4));
		postScoringFilters.add(postScoringFilterImpl.new TopScoreFilter());
			
		// parse tree post-scoring
		List<PostScoringCandidateFilter> parseTreeFilter = new ArrayList<>(postScoringFilters);
		parseTreeFilter.add(postScoringFilterImpl.new SalienceTypeFilter(CandidateSalience.Type.ParseTree));
		//proximity post-scoring
		List<PostScoringCandidateFilter> closenessFilter = new ArrayList<>(postScoringFilters);
		closenessFilter.add(postScoringFilterImpl.new SalienceTypeFilter(CandidateSalience.Type.Proximity));


		// anaphora resolution strategies
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.PersonalPronoun,
				personalPrExpFilters,pronounFilters,pronounScoringFunction,parseTreeFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.PossessivePronoun,
				possPrExpFilters,possFilters,pronounScoringFunction,parseTreeFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DistributivePronoun,
				null,pronounFilters,pronounScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.ReciprocalPronoun,
				null,pronounFilters,pronounScoringFunction,closenessFilter));
		
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DefiniteNP,
				anaphoricExpFilters,npFilters,nominalScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DemonstrativeNP,
				anaphoricExpFilters,npFilters,nominalScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DistributiveNP,
				anaphoricExpFilters,npFilters,nominalScoringFunction,closenessFilter));
		
		//generic cataphoric mention filter
		List<ExpressionFilter> cataphoricExpFilters = new ArrayList<>();
		cataphoricExpFilters.add(expressionFilterImpl.new CataphoricityFilter());
		// referent filters
		List<CandidateFilter> cataphoraFilters = new ArrayList<>();
		cataphoraFilters.add(new SubsequentDiscourseFilter());
		cataphoraFilters.add(new WindowSizeFilter(2));
		cataphoraFilters.add(new SyntaxBasedCandidateFilter());
		cataphoraFilters.add(new DefaultCandidateFilter());
		// possessive pronoun filters
		List<CandidateFilter> cataphoraPossFilters = new ArrayList<>();
		cataphoraPossFilters.add(new SubsequentDiscourseFilter());
		cataphoraPossFilters.add(new WindowSizeFilter(Configuration.RESOLUTION_WINDOW_SENTENCE));
		cataphoraPossFilters.add(new DefaultCandidateFilter());
		// personal pronoun filters
		List<CandidateFilter> cataphoraPersFilters = new ArrayList<>();
		cataphoraPersFilters.add(new SubsequentDiscourseFilter());
		cataphoraPersFilters.add(new WindowSizeFilter(Configuration.RESOLUTION_WINDOW_SENTENCE));
		cataphoraPersFilters.add(new SyntaxBasedCandidateFilter());
		cataphoraPersFilters.add(new DefaultCandidateFilter());
		
		List<ScoringFunction> catPronounScoringFunction = new ArrayList<ScoringFunction>(pronounScoringFunction);
		catPronounScoringFunction.add(new ScoringFunction(DiscourseConnectiveAgreement.class,1,2));
		// cataphora resolution strategies
		defs.add(new Strategy(CoreferenceType.Cataphora,ExpressionType.PersonalPronoun,
				personalPrExpFilters,cataphoraPersFilters,catPronounScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Cataphora,ExpressionType.PossessivePronoun,
				possPrExpFilters,cataphoraPossFilters,catPronounScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Cataphora,ExpressionType.DefiniteNP,
				cataphoricExpFilters,cataphoraFilters,nominalScoringFunction,closenessFilter));
		
		// appositive mention filters
		List<CandidateFilter> apposFilters = new ArrayList<>();
		apposFilters.add(new WindowSizeFilter(Configuration.RESOLUTION_WINDOW_SENTENCE));
		apposFilters.add(new DefaultCandidateFilter());
		// appositive agreeement
		List<ScoringFunction> apposScoringFunction = new ArrayList<>();
		apposScoringFunction.add(new ScoringFunction(NumberAgreement.class,1,1));
		apposScoringFunction.add(new ScoringFunction(SyntacticAppositiveAgreement.class,3,2));
		apposScoringFunction.add(new ScoringFunction(HypernymListAgreement.class,1,1));
		// appositive resolution strategies
		defs.add(new Strategy(CoreferenceType.Appositive,ExpressionType.DefiniteNP,
				null,apposFilters,apposScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Appositive,ExpressionType.IndefiniteNP,
				null,apposFilters,apposScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.Appositive,ExpressionType.ZeroArticleNP,
				null,apposFilters,apposScoringFunction,closenessFilter));

		// predicate nominative mention filters
		List<CandidateFilter> predNomFilters = new ArrayList<CandidateFilter>(apposFilters);
		predNomFilters.add(new PriorDiscourseFilter());
		predNomFilters.add(new WindowSizeFilter(Configuration.RESOLUTION_WINDOW_SENTENCE));
		predNomFilters.add(new DefaultCandidateFilter());
		
		// predicate nominative agreement
		List<ScoringFunction> predNomScoringFunction = new ArrayList<ScoringFunction>();
		predNomScoringFunction.add(new ScoringFunction(NumberAgreement.class,1,1));
		predNomScoringFunction.add(new ScoringFunction(PredicateNominativeAgreement.class,3,2));
		predNomScoringFunction.add(new ScoringFunction(HypernymListAgreement.class,1,1));
		
		// predicate nominative strategies
		defs.add(new Strategy(CoreferenceType.PredicateNominative,ExpressionType.IndefiniteNP,
				null,predNomFilters,predNomScoringFunction,closenessFilter));
		defs.add(new Strategy(CoreferenceType.PredicateNominative,ExpressionType.ZeroArticleNP,
				null,predNomFilters,predNomScoringFunction,closenessFilter));
		
		return defs;
	}
	
	/**
	 * Task-specific preprocessing step, in which document topics are recognized 
	 * and document is segmented into sections.<p>
	 * 
	 * @param doc	the document to process
	 */
	public static void domainSpecificPreProcessing(Document doc) {
		setTopics(doc);
		for (Object ont: doc.getTopics()) {
			log.log(Level.INFO,"Document Topic: {0}.", ont.toString());
		}
		GenericCoreferencePipeline.segmentSections(doc,sectionSegmenter);
	}
	
	/**
	 * Task-specific preprocessing step, in which the document is segmented into sections.<p>
	 * Section segmenter is provided as input.
	 * 
	 * @param doc	the document to process
	 * @param segmenter	the section segmenter to use
	 */
	public static void domainSpecificPreProcessing(Document doc, SectionSegmenter segmenter) {
		GenericCoreferencePipeline.segmentSections(doc,segmenter);
	}
	
	/**
	 * Identifies document topics in a very rudimentary way. Based on the domain, the document topics
	 * are assumed to be drug, drug class, or substance names. <p>
	 * 
	 * It iterates through each sentence, trying to identify named entities of drug types.
	 * If more than one exists in a sentence, it adds all of them as topics. Once topic(s) are found,
	 * the method exits.
	 * 
	 * An alternative could be to count the number of occurrence for each term, and decide accordingly.
	 * 
	 * @param doc	the document to identify the topics for
	 */
	public static void setTopics(Document doc) {
		List<String> termTypes =  new ArrayList<String>(DomainProperties.parse2("semtype").get("DRUG"));
		for (Sentence sent: doc.getSentences()) {
			List<Object> topics = new ArrayList<Object>();
			LinkedHashSet<SemanticItem> sentSem = Document.getSemanticItemsByClassTypeSpan(doc, Entity.class, termTypes, 
					new SpanList(sent.getSpan()),false);
			for (SemanticItem si: sentSem) {
				Entity ent = (Entity)si;
				if (ent.getText().toLowerCase().contains("drug") || ent.getText().toLowerCase().contains("section")) continue;
				if (ent.getConcepts() == null) {
					topics.add(ent.getText().toUpperCase());
				}
				else topics.add(ent.getConcepts().iterator().next());
			}
			if (topics.size() > 0 ) {
				doc.setTopics(topics);
				return;
			}
		}
		doc.setTopics(new ArrayList<Object>());
	}
	
	/**
	 * Resolves coreference in a single SPL drug coreference document. 
	 * It recognizes coreferential mentions and then identifies coreference relations.
	 * For the SPL dataset, this includes relations of all 4 coreference types.
	 * 
	 * @param doc	the document to process
	 */
	public static void coreferenceResolution(Document doc) {
		log.info("Detecting mentions...");
		coreferentialMentionDetection(doc);
		log.info("Resolving coreference...");
		mentionReferentLinking(doc);
		log.info("Post-processing...");
		postProcessing(doc);
	}
	
	/**
	 * Detects coreferential mentions in a document, if necessary.
	 * 
	 * @param doc	the document to process
	 */
	public static void coreferentialMentionDetection(Document doc) {
		if (goldExp) return;
		GenericCoreferencePipeline.coreferentialMentionDetection(doc);
	}
		
	/**
	 * Links coreferential mentions to their referents and generates <code>CoreferenceChain</code>
	 * objects.
	 * 
	 * @param doc	the document to process
	 */
	public static void mentionReferentLinking(Document doc) {
		List<SurfaceElementChain> appositives = new ArrayList<>();
		CoreferenceResolver.processDocumentForAppositive(doc,appositives);
		for (SurfaceElementChain link : appositives) {
			generateCoreferenceChain(doc,link);
		}
		List<SurfaceElementChain> predNomLinks = new ArrayList<>();
		CoreferenceResolver.processDocumentForPredicateNominative(doc,predNomLinks);
		for (SurfaceElementChain link: predNomLinks) {
			generateCoreferenceChain(doc,link);
		}
		List<SurfaceElementChain> anaphoraLinks = new ArrayList<>();
		CoreferenceResolver.processDocumentForAnaphora(doc,anaphoraLinks); 
		for (SurfaceElementChain link : anaphoraLinks) {
			generateCoreferenceChain(doc,link);
		}
		if (goldExp) {
			for (Sentence sent: doc.getSentences()) 
				ExpressionRecognition.annotateZeroArticleNPs(sent);
		}
		// TODO This should really be part of domainSpecificPreProcessing.
		// Treating discourse-level conjunctions as plain conjunctions causes
		// problems with the other part of the program. Until I find a way to handle that
		// this will be performed right before cataphora linking and
		// cataphora linking will be performed as the last step in
		// mention-referent linking.
		ConjunctionDetection.identifyDiscourseLevelConjunctions(doc);
		List<SurfaceElementChain> cataphoraLinks = new ArrayList<>();
		CoreferenceResolver.processDocumentForCataphora(doc,cataphoraLinks);
		for (SurfaceElementChain link : cataphoraLinks) {
			generateCoreferenceChain(doc,link);
		}
		for (SemanticItem si: Document.getSemanticItemsByClass(doc, CoreferenceChain.class)) {
			log.log(Level.FINE,"Recognized coreference relation: {0}.", si.toString());
		}
	}
	
	/**
	 * Post-processes the generated coreference chains for consistency and relevance.
	 * 
	 * @param doc	the document to process
	 */
	public static void postProcessing(Document doc) {
		pruneCoreferenceChains(doc);
	}
	
	/**
	 * Prunes anaphoric chains in which the mention is also involved in a cataphoric chain.
	 * 
	 * @param doc	the document to process
	 */
	public static void pruneCoreferenceChains(Document doc) {
		LinkedHashSet<SemanticItem> chains = Document.getSemanticItemsByClass(doc, CoreferenceChain.class);
		LinkedHashSet<SemanticItem> prune = new LinkedHashSet<>();
		for (SemanticItem c: chains) {
			CoreferenceChain ch = (CoreferenceChain)c;
			if (CoreferenceType.Cataphora == CoreferenceType.valueOf(ch.getType())) {
				List<Argument> cataphorArg = ch.getArgs(CoreferenceType.Cataphora.getExpRole().toString());
				for (Argument arg: cataphorArg) {
					Term t = (Term) arg.getArg();
					LinkedHashSet<CoreferenceChain> chs = CoreferenceChain.getChainsWithAnaphor(t);
					if (chs.size() > 0) {
						prune.addAll(chs);
					}
				}
			}
		}
		for (SemanticItem pr: prune) {
			log.log(Level.FINE,"Pruning coreference chain: {0}.", pr.toString());
			doc.removeSemanticItem(pr);
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
		CoreferenceType type = strategy.getCorefType();
		List<Argument> args = new ArrayList<>();
		for (SurfaceElement ref: referents) {
			// create a generic term for the referent
			// TODO Is this really necessary?
			if (!ref.hasSemantics()) {
				Entity ent = doc.getSemanticItemFactory().newEntity(doc, ref.getSpan(), ref.getHead().getSpan(), "SPAN");
				log.log(Level.FINEST,"Created new entity for referent: {0}.", ent.toString());
			}
			// need to create a new coreference chain
			log.log(Level.FINEST, "Creating a new coreference link between the textual units {0} and {1}.",
					new Object[]{exp.toString(),ref.toString()});
			
			// if the referent indicates a conjunction, add its conjunct items to the chain
			// as long as the position of the conjunct is compatible with the coreference type and the mention
			SemanticItem highest = ((AbstractSurfaceElement)ref).getMostProminentSemanticItem();
			Set<SemanticItem> refArgs = new LinkedHashSet<>();
			if (highest instanceof Conjunction) {
				List<SemanticItem> conjs = ((Conjunction)highest).getArgItems();
				for (SemanticItem conj : conjs) {	
					if (CoreferenceUtils.incompatibleDirection(exp,conj,type)) {
						refArgs.clear();
						break;
					}
					refArgs.add(conj);
				}
			} else refArgs.add(highest);
			
			// if there is a textual unit with the same semantics as the referent closer to the coreferential mention,
			// add that instead to the chain. 
			// This is to be consistent with the annotation guidelines, where the closest element is often chosen.
			for (SemanticItem sem: refArgs) {
				SemanticItem closer = CoreferenceUtils.closerWithSameOntology(doc,exp,sem);
				if (closer != null && refArgs.contains(closer) == false)
					args.add(new Argument(type.getRefRole().toString(),closer));
				else
					args.add(new Argument(type.getRefRole().toString(),sem));
			}
			// if no mention semantic object is annotated, take the head semantics of the mention.
			if (Expression.filterByExpressions(exp).size() == 0) {
				SemanticItem an = exp.getHeadSemantics().iterator().next();
				args.add(new Argument(type.getExpRole().toString(),an));
			} else {
				for (SemanticItem an: Expression.filterByExpressions(exp)) {
					args.add(new Argument(type.getExpRole().toString(),an));
				}
			}
			// generate a coreference chain object
			((CoreferenceSemanticItemFactory)doc.getSemanticItemFactory()).newCoreferenceChain(doc, type.toString(),args);
		}
	}
	
	/**
	 * Writes relevant semantic objects as standoff annotations to an output file.
	 * 
	 * @param writeTypes	the semantic object types to write
	 * @param outFileName	the output file to write to
	 * @param doc			the document that contains the semantic objects
	 * @throws IOException	if <var>outFileName</var> cannot be opened
	 */
	public static void writeStandoffAnnotations(List<Class<? extends SemanticItem>> writeTypes, String outFileName, Document doc)
		throws IOException {
		List<String> writeLines = new ArrayList<>();
		for (SemanticItem si: doc.getAllSemanticItems()) {
			if (writeTypes.contains(si.getClass()) == false) continue;
			if (si instanceof Expression) {
				if (toWrite((Expression)si) == false) continue; 
				writeLines.add(si.toStandoffAnnotation());
			}
			else if (si instanceof CoreferenceChain) {
				CoreferenceChain cc = (CoreferenceChain)si;
				writeLines.addAll(coreferenceChainToSPLStandoffAnnotation(cc));
			} else 
				writeLines.add(si.toStandoffAnnotation());
		}
		StandoffAnnotationWriter.writeLines(writeLines,outFileName);
	}
	
	/**
	 * Converts a <code>CoreferenceChain</code> object to a list of standoff annotation lines
	 * as expected for the SPL drug coreference dataset.
	 * 
	 * @param cc	the coreference chain
	 * @return	a list of corresponding standoff annotation lines
	 */
	public static List<String> coreferenceChainToSPLStandoffAnnotation(CoreferenceChain cc) {
		List<SemanticItem> exps = cc.getExpressions();
		List<SemanticItem> refs = cc.getReferents();
		CoreferenceType  ctype = CoreferenceType.valueOf(cc.getType());
		List<String> lines = new ArrayList<>();
		String expRole = ctype.getExpRole().toString();
		String refRole = ctype.getRefRole().toString();
		for (SemanticItem exp: exps) {
			for (SemanticItem ref: refs) {
				if (writableCorefChain(exp,ref))
					lines.add("R" + ++chainId + "\t" + cc.getType() + " " + expRole + ":" + exp.getId() + " " + refRole + ":" + ref.getId());
			}
		}
		return lines;
	}
	
	/**
	 * Determines whether the coreference chain with the arguments <var>exp</var> and <var>ref</var>
	 * are to be written. <p>
	 * If gold coreferential mentions are used, this method returns true if the 
	 * arguments are coreferential mentions or drug-related entities.
	 * If gold coreferential mentions aren't used, either the referent should be a drug-related entity
	 * OR (the coreferential mention argument should be pronominal AND the referent should include a hypernym)
	 * OR the coreferential mention should include a hypernym. 
	 * 
	 * @param exp	the coreferential mention of the chain
	 * @param ref	the referent of the chain
	 * @return	true if the coreference chain with the arguments can be written
	 */
	public static boolean writableCorefChain(SemanticItem exp,SemanticItem ref) {
		boolean typeMatch = toWrite(exp) && toWrite(ref);
		if (goldExp) return typeMatch;
		LinkedHashSet<String> types = ref.getAllSemtypes();
		if (types.contains("SPAN")) return false;
		List<String> termTypes =  new ArrayList<String>(DomainProperties.parse2("semtype").get("DRUG"));
		if (CollectionUtils.containsAny(types, termTypes)) return true;
		List<String> hypernyms =  new ArrayList<String>(DomainProperties.parse2("hypernym").get("DRUG"));
		if (exp instanceof Expression) {
//			if (ExpressionType.PRONOMINAL_TYPES.contains(exp.getType()) && ref instanceof Expression) {
			if (exp.getType().contains("Pronoun") && ref instanceof Expression) {
				Expression r = (Expression)ref;
				SurfaceElement rsu = r.getSurfaceElement();
				for (String hy: hypernyms) {
					if (rsu.containsLemma(hy)) {
						return true;
					}
				}
			} else {
				Expression e = (Expression)exp;
				SurfaceElement expsu = e.getSurfaceElement();
				for (String hy: hypernyms) {
					if (expsu.containsLemma(hy)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Determines whether a semantic object is to be written to output. <p>
	 * A semantic object is written to output, if it is a drug-related entity or 
	 * a coreferential mention.
	 * 
	 * @param sem	the semantic object
	 * 
	 * @return	true if the semantic object can be written to output
	 */
	public static boolean toWrite(SemanticItem sem) {
		if (sem instanceof Expression) return true;
		LinkedHashSet<String> types = sem.getAllSemtypes();
		List<String> termTypes =  new ArrayList<>(DomainProperties.parse2("semtype").get("DRUG"));
		return CollectionUtils.containsAny(types, termTypes);
	}
	
	/**
	 * Reads properties from properties file and initializes
	 * WordNet, section segmenter, and coreference-related word lists. 
	 * It also loads resolution strategies.
	 * 
	 * @throws ClassNotFoundException	if the section segmenter class cannot be found
	 * @throws IllegalAccessException	if the section segmenter cannot be accessed
	 * @throws InstantiationException	if the section segmenter cannot be initialized
	 * @throws IOException				if properties file cannot be found
	 */
	public static void init() 
			throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Properties props = FileUtils.loadPropertiesFromFile("coref_spl.properties");
		props.put("sectionSegmenter", "tasks.coref.spl.SPLSectionSegmenter");
		DomainProperties.init(props);
		WordNetWrapper.getInstance(props);
		Configuration.getInstance(loadSPLStrategies());			
		sectionSegmenter = ComponentLoader.getSectionSegmenter(props);
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException, 
						InstantiationException, IllegalAccessException {
		if (args.length < 3) {
			System.err.print("Usage: inputDirectory outputDirectory useGoldMentions");
			System.exit(1);
		}
		String in = args[0];
		String a2Out = args[1];
		goldExp = Boolean.parseBoolean(args[2]);
		
		File inDir = new File(in);
		if (inDir.isDirectory() == false) {
			System.err.println("First argument is required to be an input directory:" + in);
			System.exit(1);
		}
		File a2OutDir = new File(a2Out);
		if (a2OutDir.isDirectory() == false) {
			System.err.println("The directory " + a2Out + " doesn't exist. Creating a new directory..");
			a2OutDir.mkdir();
		}
		
		// initialize 
		init();

		// load annotations to use
		Map<Class<? extends SemanticItem>,List<String>> annTypes = new HashMap<>();
		annTypes.put(Entity.class,Constants.ENTITY_TYPES);
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
			String a2Filename = a2OutDir.getAbsolutePath() + File.separator + filenameNoExt + ".ann";
			Document doc = null;
			if (!goldExp)  {
				doc = reader.load(filename, true, CoreferenceSemanticItemFactory.class,annTypes, null);
			}
			else {
				annTypes.put(Expression.class, Constants.EXP_TYPES);
				reader.addAnnotationReader(Expression.class, new XMLExpressionReader());
				doc = reader.load(filename, true, CoreferenceSemanticItemFactory.class,annTypes, null);
			}
			LinkedHashSet<SemanticItem> terms = Document.getSemanticItemsByClass(doc, Term.class);
			if (terms != null) {
				for (SemanticItem ent: terms)
					log.log(Level.FINE,"Loaded term: {0}." , ((Term)ent).toString());
			}
			if (!goldExp) SemUtils.removeSubsumedTerms(doc);
			
			GenericCoreferencePipeline.linguisticPreProcessing(doc);
			domainSpecificPreProcessing(doc);
			// coreference
			coreferenceResolution(doc);
			// write	
			chainId = doc.getMaxId(Relation.class);
			List<Class<? extends SemanticItem>> writeTypes = new ArrayList<>(annTypes.keySet());
			writeTypes.add(Expression.class);
			writeTypes.add(CoreferenceChain.class);
			writeStandoffAnnotations(writeTypes,a2Filename,doc);
		}
	}
}
