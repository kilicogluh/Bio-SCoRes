package gov.nih.nlm.bioscores.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import gov.nih.nlm.bioscores.exp.ExpressionFilter;
import gov.nih.nlm.bioscores.exp.ExpressionFilterImpl;
import gov.nih.nlm.ling.brat.Annotation;
import gov.nih.nlm.ling.brat.StandoffAnnotationReader;
import gov.nih.nlm.ling.brat.StandoffAnnotationWriter;
import gov.nih.nlm.ling.brat.TermAnnotation;
import gov.nih.nlm.ling.core.AbstractSurfaceElement;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Section;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.process.ComponentLoader;
import gov.nih.nlm.ling.process.SectionSegmenter;
import gov.nih.nlm.ling.process.SentenceSegmenter;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Conjunction;
import gov.nih.nlm.ling.sem.ConjunctionDetection;
import gov.nih.nlm.ling.sem.DomainProperties;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import gov.nih.nlm.ling.sem.Term;
import gov.nih.nlm.ling.transform.CoordinationTransformation;
import gov.nih.nlm.ling.transform.ModifierCoordinationCorrection;
import gov.nih.nlm.ling.transform.NPInternalTransformation;
import gov.nih.nlm.ling.transform.PPAttachmentCorrection;
import gov.nih.nlm.ling.transform.TermCoordinationCorrection;
import gov.nih.nlm.ling.util.FileUtils;
import gov.nih.nlm.ling.util.SemUtils;
import gov.nih.nlm.ling.wrappers.CoreNLPWrapper;
import gov.nih.nlm.ling.wrappers.WordNetWrapper;

/**
 * The generic pipeline that can be used for coreference resolution. <p>
 * 
 * This pipeline deals with all four coreference types: {@link CoreferenceType#Anaphora}, 
 * {@link CoreferenceType#Cataphora}, {@link CoreferenceType#Appositive}, and 
 * {@link CoreferenceType#PredicateNominative}. 
 * 
 * It requires a directory of text files to process and the corresponding term annotations
 * in brat standoff annotation format in the same directory. 
 * It does not perform any named entity recognition, since the framework is NER system-agnostic. 
 * The semantic type/group labels assigned by the NER system should have been
 * introduced to the framework via <code>coref.properties</code> file in the top-level directory.
 * The hypernym/event trigger word lists associated with these types/groups should also have been defined in
 * the properties file for this pipeline to have some degree of success.
 *
 * @author Halil Kilicoglu
 *
 */
public class GenericCoreferencePipeline {
	private static Logger log = Logger.getLogger(GenericCoreferencePipeline.class.getName());
	
	private static SectionSegmenter sectionSegmenter = null;
	private static SentenceSegmenter sentenceSegmenter = null;
	private static int chainId = 0;
	
	/**
	 * Loads the resolution strategies. These are the same as those used for
	 * SPL dataset.
	 * 
	 * @return	the list of resolution strategies.
	 */
	public static List<Strategy> loadStrategies() {
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
		personalPrExpFilters.add(expressionFilterImpl.new PleonasticItFilter());
		
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
	 * Segments text into sentences and performs core NLP analysis.
	 * 
	 * @param doc		the document to analyze
	 * @param segmenter	the sentence segmenter to use
	 */
	public static void analyze(Document doc, SentenceSegmenter segmenter) {
		log.info("Analyzing the document for syntax...");
		List<Sentence> sentences = new ArrayList<>();
		segmenter.segment(doc.getText(), sentences);
		doc.setSentences(sentences);
		for (int i=0; i < doc.getSentences().size(); i++) {
			Sentence sent = doc.getSentences().get(i);
			sent.setDocument(doc);
			// create word list, parse and dependencies to sentence
			CoreNLPWrapper.coreNLP(sent);
		}
	}
	
	/**
	 * Transforms document dependency graph to reflect semantic dependencies, 
	 * useful for this pipeline, and performs coordination recognition.
	 * 
	 * @param doc	the document to preprocess
	 */
	public static void linguisticPreProcessing(Document doc) {
		for (Sentence sent: doc.getSentences()) {
			new PPAttachmentCorrection().transform(sent);
			sent.logTransformation("PPAttachment");
			new ModifierCoordinationCorrection().transform(sent);
			sent.addTransformation(ModifierCoordinationCorrection.class);
			sent.logTransformation("ModifierCoordinationCorrection");
			new TermCoordinationCorrection().transform(sent);
			sent.addTransformation(TermCoordinationCorrection.class);
			sent.logTransformation("TermCoordinationCorrection");			
			new CoordinationTransformation().transform(sent);
			sent.addTransformation(CoordinationTransformation.class);
			sent.logTransformation("CoordinationTransformation");
			new NPInternalTransformation(NPInternalTransformation.Method.Dependency).transform(sent);
			sent.addTransformation(NPInternalTransformation.class);
			sent.logTransformation("NPInternalTransformation");
			ConjunctionDetection.identifyConjunctionRelationsFromTransformation(sent);
		}
	}
	
	/**
	 * Segments the document into sections, using the default section segmenter 
	 * 
	 * @param doc	the document to process
	 */
	public static void segmentSections(Document doc) {
		segmentSections(doc,sectionSegmenter);
	}
	
	/**
	 * Segments the document into sections, if a {@code SectionSegmenter} is provided. 
	 * 
	 * @param doc		the document to process
	 * @param segmenter	the section segmenter to use
	 */
	public static void segmentSections(Document doc, SectionSegmenter segmenter) {
		if (segmenter == null) {
			log.severe("Section segmenter has not been initialized. Unable to recognize sections.");
			return;
		}
		else segmenter.segment(doc);
		for (Section sect: doc.getSections()) {
			log.log(Level.FINE,"Document Section: {0}.", sect.toString());
			if (sect.getSubSections() != null) {
				for (Section sub: sect.getSubSections()) {
					log.log(Level.FINE,"\tDocument SubSection ({0}): {1}.", new Object[]{sub.getTextSpan().toString(),sub.toString()});
				}
			}
		}
	}
	
	/**
	 * Resolves coreference in a single document. 
	 * It recognizes coreferential mentions and then identifies coreference relations.
	 * 
	 * @param doc	the document to process
	 */
	public static void coreferenceResolution(Document doc) {
		log.info("Detecting mentions...");
		coreferentialMentionDetection(doc);
		log.info("Resolving coreference...");
		mentionReferentLinking(doc);
		log.info("Pruning chains...");
		pruneCoreferenceChains(doc);
	}
	
	/**
	 * Detects coreferential mentions in a document.
	 * 
	 * @param doc	the document to process
	 */
	public static void coreferentialMentionDetection(Document doc) {
		ExpressionRecognition.annotate(doc);
		for (SemanticItem si: Document.getSemanticItemsByClass(doc, Expression.class)) {
			log.log(Level.FINE,"Mention recognized: {0}.", si.toString());
		}
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
				if (CoreferenceChain.getChainsWithTerm(si).size() > 0) 
					writeLines.add(si.toStandoffAnnotation());
			}
			else if (si instanceof CoreferenceChain) {
				CoreferenceChain cc = (CoreferenceChain)si;
				writeLines.addAll(coreferenceChainToStandoffAnnotations(cc));
			} else 
				writeLines.add(si.toStandoffAnnotation());
		}
		for (String l: writeLines) {
			log.log(Level.INFO, "{0}",l);
		}
		StandoffAnnotationWriter.writeLines(writeLines,outFileName);
	}
	
	/**
	 * Converts a {@link CoreferenceChain} object to a list of standoff annotation lines.
	 * 
	 * @param cc	the coreference chain
	 * @return	a list of corresponding standoff annotation lines
	 */
	public static List<String> coreferenceChainToStandoffAnnotations(CoreferenceChain cc) {
		List<SemanticItem> exps = cc.getExpressions();
		List<SemanticItem> refs = cc.getReferents();
		CoreferenceType  ctype = CoreferenceType.valueOf(cc.getType());
		List<String> lines = new ArrayList<>();
		String expRole = ctype.getExpRole().toString();
		String refRole = ctype.getRefRole().toString();	
		for (SemanticItem exp: exps) {
			for (SemanticItem ref: refs) {
					lines.add("R" + ++chainId + "\t" + cc.getType() + " " + expRole + ":" + exp.getId() + " " + refRole + ":" + ref.getId());
			}
		}
		return lines;
	}
	
	/** 
	 * Performs coreference resolution on a single document from the associated text and annotation files
	 * 
	 * @param id		the id for the document
	 * @param textFile	the text file of the document
	 * @param annFile  	the annotation file
	 * @param outFile	the file to write the output to
	 * 
	 * @throws IOException 	if there is a problem with writing to <var>outFile</var>
	 * 
	 */
	public static void processSingleFile(String id, String textFile, String annFile, String outFile) 
			throws IOException {
		Document doc = StandoffAnnotationReader.readTextFile(id, textFile);
		SemanticItemFactory csif = new CoreferenceSemanticItemFactory(doc,new HashMap<Class<? extends SemanticItem>,Integer>());
		doc.setSemanticItemFactory(csif);
		analyze(doc,sentenceSegmenter);
		Map<String,List<String>> lines = StandoffAnnotationReader.readAnnotationFiles(Arrays.asList(annFile), null);
		Map<Class,List<Annotation>> annotations = StandoffAnnotationReader.parseAnnotations(id, lines, null);
		CoreferenceSemanticItemFactory sif = (CoreferenceSemanticItemFactory)doc.getSemanticItemFactory();	
		List<Annotation> anns = annotations.get(TermAnnotation.class);
		if (anns == null || anns.size() == 0) {
			log.log(Level.WARNING, "Unable to continue, because no term annotations are provided: {0}.", id);
			return;
		}
		for (Annotation ann: anns) {
			log.log(Level.FINE,"Reading {0}: {1}.", new Object[]{TermAnnotation.class.getName(),ann.toString()});
			sif.newEntity(doc, (TermAnnotation)ann);
		}
		LinkedHashSet<SemanticItem> terms = Document.getSemanticItemsByClass(doc, Term.class);
		if (terms != null) {
			for (SemanticItem ent: terms) 
				log.log(Level.FINE,"Loaded term: {0}." , ((Term)ent).toString());
		}
		SemUtils.removeSubsumedTerms(doc);
		
		linguisticPreProcessing(doc);
		segmentSections(doc);
		// coreference
		coreferenceResolution(doc);
		// write
		// just so that we do not repeat ids
		chainId = doc.getMaxId(Relation.class);
		List<Class<? extends SemanticItem>> writeTypes = new ArrayList<>();
		writeTypes.add(Entity.class);
		writeTypes.add(Expression.class);
		writeTypes.add(CoreferenceChain.class);
		writeStandoffAnnotations(writeTypes,outFile,doc);
	}
	
	/**
	 * Reads properties from <code>coref.properties</code> file and initializes
	 * CoreNLP, WordNet, sentence segmenter, section segmenter (if any), and 
	 * coreference-related word lists. 
	 * 
	 * @throws ClassNotFoundException	if the sentence/section segmenter class cannot be found
	 * @throws IllegalAccessException	if the sentence/section segmenter cannot be accessed
	 * @throws InstantiationException	if the sentence/section segmenter cannot be initialized
	 * @throws IOException				if <code>coref.properties</code> cannot be found
	 */
	public static void init() 
			throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Properties props = FileUtils.loadPropertiesFromFile("coref.properties");
		DomainProperties.init(props);
		WordNetWrapper.getInstance(props);
		if (props.getProperty("sentenceSegmenter") == null)
			props.put("sentenceSegmenter","gov.nih.nlm.ling.process.MedlineSentenceSegmenter");
		props.put("annotators","tokenize,ssplit,pos,lemma,parse");	
		props.put("tokenize.options","invertible=true");
		props.put("ssplit.isOneSentence","true");
		Configuration.getInstance(loadStrategies());	
		if (props.getProperty("sectionSegmenter").trim().equals("") == false) 
			sectionSegmenter = ComponentLoader.getSectionSegmenter(props);
		CoreNLPWrapper.getInstance(props);
		sentenceSegmenter = ComponentLoader.getSentenceSegmenter(props);
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException, 
						InstantiationException, IllegalAccessException {
		if (args.length < 2) {
			System.err.print("Usage: inputDirectory outputDirectory");
			System.exit(1);
		}
		String in = args[0];
		String out = args[1];
		
		File inDir = new File(in);
		if (inDir.isDirectory() == false) {
			System.err.println("First argument is required to be an input directory:" + in);
			System.exit(1);
		}
		File outDir = new File(out);
		if (outDir.isDirectory() == false) {
			System.err.println("The directory " + out + " doesn't exist. Creating a new directory..");
			outDir.mkdir();
		}
		init();			
		// iterate through files
		List<String> files = FileUtils.listFiles(in, false, "txt");
		if (files.size() == 0) 
			log.log(Level.SEVERE, "No text files found in input directory {0}.", new Object[]{in});
		int fileNum = 0;
		for (String filename: files) {
			String id = filename.replace(".txt", "");
			id = id.substring(id.lastIndexOf(File.separator)+1);
			log.log(Level.INFO,"Processing file {0}: {1}.", new Object[]{id,++fileNum});
			String annFilename = inDir.getAbsolutePath() + File.separator + id + ".ann";
			if (new File(annFilename).exists() == false) continue;
			String outFilename = outDir.getAbsolutePath() + File.separator + id + ".ann";
			processSingleFile(id,filename,annFilename,outFilename);
		}
	}
}
