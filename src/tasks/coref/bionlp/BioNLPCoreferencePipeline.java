package tasks.coref.bionlp;

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

import gov.nih.nlm.bioscores.agreement.AdjacencyAgreement;
import gov.nih.nlm.bioscores.agreement.AnimacyAgreement;
import gov.nih.nlm.bioscores.agreement.GenderAgreement;
import gov.nih.nlm.bioscores.agreement.HypernymListAgreement;
import gov.nih.nlm.bioscores.agreement.NumberAgreement;
import gov.nih.nlm.bioscores.agreement.PersonAgreement;
import gov.nih.nlm.bioscores.agreement.SemanticCoercionAgreement;
import gov.nih.nlm.bioscores.candidate.CandidateFilter;
import gov.nih.nlm.bioscores.candidate.CandidateSalience;
import gov.nih.nlm.bioscores.candidate.DefaultCandidateFilter;
import gov.nih.nlm.bioscores.candidate.PostScoringCandidateFilter;
import gov.nih.nlm.bioscores.candidate.PostScoringCandidateFilterImpl;
import gov.nih.nlm.bioscores.candidate.PriorDiscourseFilter;
import gov.nih.nlm.bioscores.candidate.SyntaxBasedCandidateFilter;
import gov.nih.nlm.bioscores.candidate.WindowSizeFilter;
import gov.nih.nlm.bioscores.core.Configuration;
import gov.nih.nlm.bioscores.core.CoreferenceChain;
import gov.nih.nlm.bioscores.core.CoreferenceResolver;
import gov.nih.nlm.bioscores.core.CoreferenceSemanticItemFactory;
import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.CoreferenceUtils;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.bioscores.core.GenericCoreferencePipeline;
import gov.nih.nlm.bioscores.core.ScoringFunction;
import gov.nih.nlm.bioscores.core.Strategy;
import gov.nih.nlm.bioscores.core.SurfaceElementChain;
import gov.nih.nlm.bioscores.exp.ExpressionFilter;
import gov.nih.nlm.bioscores.exp.ExpressionFilterImpl;
import gov.nih.nlm.ling.brat.StandoffAnnotationWriter;
import gov.nih.nlm.ling.core.AbstractSurfaceElement;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.MultiWord;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.io.XMLEntityReader;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Conjunction;
import gov.nih.nlm.ling.sem.DomainProperties;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;
import gov.nih.nlm.ling.util.FileUtils;
import gov.nih.nlm.ling.wrappers.WordNetWrapper;

/**
 * The pipeline used to resolve coreference in BioNLP protein coreference dataset.
 * This pipeline only deals with {@link CoreferenceType#Anaphora} type.
 * 
 * @author Halil Kilicoglu
 *
 */
public class BioNLPCoreferencePipeline {
	private static Logger log = Logger.getLogger(BioNLPCoreferencePipeline.class.getName());
	private static int chainId = 0;
	
	/**
	 * Loads the resolution strategies used for the BioNLP coreference dataset.
	 * 
	 * @return	the list of resolution strategies.
	 */
	public static List<Strategy> loadBioNLPCoreferenceStrategies() {
		List<Strategy> defs = new ArrayList<>();
		
		ExpressionFilterImpl expressionFilterImpl = new ExpressionFilterImpl();
		// generic anaphoric mention filters
		List<ExpressionFilter> anaphoricExpFilters = new ArrayList<>();
		anaphoricExpFilters.add(expressionFilterImpl.new AnaphoricityFilter());
		// possessive pronoun mention filters
		List<ExpressionFilter> possPrExpFilters = new ArrayList<>();
		possPrExpFilters.add(expressionFilterImpl.new ThirdPersonPronounFilter());
		// personal pronoun mention filters
		List<ExpressionFilter> personalPrExpFilters = new ArrayList<>(possPrExpFilters);
		personalPrExpFilters.add(expressionFilterImpl.new PleonasticItFilter());
		// relative pronoun mention filters
		List<ExpressionFilter> relExpFilters = new ArrayList<>();
		relExpFilters.add(expressionFilterImpl.new NonCorefRelativePronFilter());

		// generic anaphora candidate filters
		List<CandidateFilter> anaphoraFilters = new ArrayList<>();
		anaphoraFilters.add(new PriorDiscourseFilter());
		anaphoraFilters.add(new WindowSizeFilter(2));
		anaphoraFilters.add(new SyntaxBasedCandidateFilter());
		anaphoraFilters.add(new DefaultCandidateFilter());
		// possessive pronoun anaphora candidate filters
		List<CandidateFilter> possFilters = new ArrayList<>();
		possFilters.add(new PriorDiscourseFilter());
		possFilters.add(new WindowSizeFilter(2));
		possFilters.add(new DefaultCandidateFilter());
		// relative pronoun anaphora candidate filters
		List<CandidateFilter> relFilters = new ArrayList<>();
		relFilters.add(new PriorDiscourseFilter());
		relFilters.add(new WindowSizeFilter(Configuration.RESOLUTION_WINDOW_SENTENCE));
		relFilters.add(new DefaultCandidateFilter());
		
		// generic pronoun scoring function
		List<ScoringFunction> pronounScoringFunction = new ArrayList<>();
		pronounScoringFunction.add(new ScoringFunction(AnimacyAgreement.class,1,0));
		pronounScoringFunction.add(new ScoringFunction(GenderAgreement.class,1,0));
		pronounScoringFunction.add(new ScoringFunction(NumberAgreement.class,1,0));
		pronounScoringFunction.add(new ScoringFunction(PersonAgreement.class,1,0));
		// possessive pronoun scoring function
		List<ScoringFunction> possPronounScoringFunction = new ArrayList<>(pronounScoringFunction);
		possPronounScoringFunction.add(new ScoringFunction(SemanticCoercionAgreement.class,1,0));
		// relative pronoun scoring function
		List<ScoringFunction> relPronounScoringFunction = new ArrayList<>();
		relPronounScoringFunction.add(new ScoringFunction(AdjacencyAgreement.class,1,0));
		

		//		relPronounScoringFunction.add(new ScoringFunction(RELDependencyAgreement.class,2,0));
//		relPronounScoringFunction.add(new ScoringFunction(ClosestRCMODAgreement.class,1,0));
		
		// generic post-scoring candidate filters
		PostScoringCandidateFilterImpl postScoringFilterImpl = new PostScoringCandidateFilterImpl();
		List<PostScoringCandidateFilter> postScoringFilters = new ArrayList<>();
		postScoringFilters.add(postScoringFilterImpl.new ThresholdFilter(4));
		postScoringFilters.add(postScoringFilterImpl.new TopScoreFilter());
		// post-scoring parse tree filter
		List<PostScoringCandidateFilter> parseTreeFilter = new ArrayList<>(postScoringFilters);
		parseTreeFilter.add(postScoringFilterImpl.new SalienceTypeFilter(CandidateSalience.Type.ParseTree));
		// post-scoring proximity filter
		List<PostScoringCandidateFilter> proximityFilter = new ArrayList<>(postScoringFilters);
		proximityFilter.add(postScoringFilterImpl.new SalienceTypeFilter(CandidateSalience.Type.Proximity));
		// relative pronoun post-scoring filter 
		List<PostScoringCandidateFilter> relPostScoringFilter = new ArrayList<>();
		relPostScoringFilter.add(postScoringFilterImpl.new ThresholdFilter(1));
		relPostScoringFilter.add(postScoringFilterImpl.new SalienceTypeFilter(CandidateSalience.Type.Proximity));
		
		// resolution strategies for pronouns
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.PersonalPronoun,
				personalPrExpFilters,anaphoraFilters,pronounScoringFunction,parseTreeFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.PossessivePronoun,
				possPrExpFilters,possFilters,possPronounScoringFunction,parseTreeFilter));
//		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DemonstrativePronoun,
//				null,anaphoraFilters,pronounScoringFunction,proximityFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DistributivePronoun,
				null,anaphoraFilters,pronounScoringFunction,proximityFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.ReciprocalPronoun,
				null,anaphoraFilters,pronounScoringFunction,proximityFilter));
//		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.IndefinitePronoun,
//				null,anaphoraFilters,pronounScoringFunction,proximityFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.RelativePronoun,
				relExpFilters,relFilters,relPronounScoringFunction,relPostScoringFilter));
		
		// scoring functions for nominal mentions
		List<ScoringFunction> nominalScoringFunction = new ArrayList<>();
		nominalScoringFunction.add(new ScoringFunction(NumberAgreement.class,1,1));
		nominalScoringFunction.add(new ScoringFunction(HypernymListAgreement.class,3,0));
		
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DefiniteNP,
				anaphoricExpFilters,anaphoraFilters,nominalScoringFunction,proximityFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DemonstrativeNP,
				anaphoricExpFilters,anaphoraFilters,nominalScoringFunction,proximityFilter));
		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.DistributiveNP,
				anaphoricExpFilters,anaphoraFilters,nominalScoringFunction,proximityFilter));
//		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.ZeroArticleNP,
//				anaphoricExpFilters,anaphoraFilters,nominalScoringFunction,proximityFilter));
//		defs.add(new Strategy(CoreferenceType.Anaphora,ExpressionType.IndefiniteNP,
//				anaphoricExpFilters,anaphoraFilters,nominalScoringFunction,proximityFilter));
		return defs;
	}
	
	/**
	 * Resolves coreference in a single BioNLP coreference dataset document. 
	 * It recognizes coreferential mention and then identifies coreference relations.
	 * For BioNLP dataset, this only includes anaphora relations.
	 * 
	 * @param doc	the document to process
	 */
	public static void coreferenceResolution(Document doc) {
		log.info("Detecting mentions...");
		GenericCoreferencePipeline.coreferentialMentionDetection(doc);
		log.info("Resolving coreference...");
		mentionReferentLinking(doc);
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
		for (SurfaceElementChain link : anaphoraLinks) {
			generateCoreferenceChain(doc,link);
		}
		for (SemanticItem si: Document.getSemanticItemsByClass(doc, CoreferenceChain.class)) {
			log.log(Level.FINE,"Recognized coreference relation: {0}.", si.toString());
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
		// merge if the referent is already part of another chain
		for (SurfaceElement ref: referents) {
			boolean expReferent = false;
			if (Expression.filterByExpressions(ref).size() > 0) {
				LinkedHashSet<CoreferenceChain> chains = CoreferenceChain.getChainsWithArgument(ref);
				if (chains.size() > 0) {
					for (CoreferenceChain cc: chains) {
							SemanticItem headSem = ((AbstractSurfaceElement)exp).getMostProminentSemanticItem();
							cc.addArgument(new Argument(type.getExpRole().toString(),headSem));
							log.log(Level.FINEST,"Added mention to coreference chain {0}: {1}. Returning..",
									new Object[]{cc.getId(),headSem.toShortString()});
							expReferent = true;
					}
				}
			}
			if (expReferent) return;
			// create a generic term for the referent
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
		}
		((CoreferenceSemanticItemFactory)doc.getSemanticItemFactory()).newCoreferenceChain(doc, type.toString(),args);
	}
		
	/**
	 * Converts a <code>CoreferenceChain</code> object to a list of standoff annotation lines
	 * as expected for BioNLP protein coreference dataset.
	 * 
	 * @param cc	the coreference chain
	 * @return	a list of corresponding standoff annotation lines
	 */
	public static List<String> coreferenceChainToBioNLPStandoffAnnotation(CoreferenceChain cc) {
		List<SemanticItem> exps = cc.getExpressions();
		List<SemanticItem> refs = cc.getReferents();
		CoreferenceType  ctype = CoreferenceType.valueOf(cc.getType());
		List<String> lines = new ArrayList<>();
		String expRole = ctype.getExpRole().toString();
		// because that is how it is annotated in BioNLP dataset
		if (expRole.equals("Anaphor")) expRole = "Anaphora";
		String refRole = ctype.getRefRole().toString();
		Document doc = cc.getDocument();
		int maxCoref = doc.getMaxId(Relation.class);		
		for (SemanticItem exp: exps) {
			if (refs.size() > 1) {
				Entity ent = getMultipleReferentEntity(cc.getDocument(),refs);
				lines.add(ent.toStandoffAnnotation());
				lines.add("R" + ++chainId + "\t" + "Coref" + " " + expRole + ":" + exp.getId() + " " + refRole + ":" + ent.getId());
			} else if (refs.size() == 1) {
				lines.add("R" + ++chainId + "\t" + "Coref" + " " + expRole + ":" + exp.getId() + " " + refRole + ":" + refs.get(0).getId());				
			}
		}
		return lines;
	}
	
	// to accommodate coordinated items for output, find the span that includes them all
	// and create a new entity with type SPAN with the full span.
	private static Entity getMultipleReferentEntity(Document doc, List<SemanticItem> refs) {
		List<SpanList> rspans = new ArrayList<>();
		for (SemanticItem r: refs) rspans.add(r.getSpan());
		SpanList union = SpanList.union(rspans);
		Word head = MultiWord.findHeadFromCategory(doc.getWordsInSpan(union.asSingleSpan()));
		return doc.getSemanticItemFactory().newEntity(doc, new SpanList(union.asSingleSpan()), head.getSpan(), "SPAN");
	}

	/**
	 * Reads properties from <code>coref_bionlp.properties</code> file and 
	 * initializes WordNet, resolution strategies and coreference-related word lists. 
	 * 
	 * @throws IOException	if the properties file cannot be found
	 */
	public static void init() 
			throws IOException {
		Properties props = FileUtils.loadPropertiesFromFile("coref_bionlp.properties");
		DomainProperties.init(props);
		WordNetWrapper.getInstance(props);
		Configuration.getInstance(loadBioNLPCoreferenceStrategies());
	}
	  
	/* Runs the BioNLP coreference pipeline. */
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.print("Usage: inputDirectory outputDirectory");
		}
		String in = args[0];
		String a2Out = args[1];
		
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
        
		// annotations to load
		Map<Class<? extends SemanticItem>,List<String>> annotationTypes = new HashMap<>();
		annotationTypes.put(Entity.class,Arrays.asList("Protein"));
		XMLReader reader = new XMLReader();
		reader.addAnnotationReader(Entity.class, new XMLEntityReader());
			 
		List<String> files = FileUtils.listFiles(in, false, "xml");
		if (files.size() == 0) log.log(Level.SEVERE, "No XML file found in input directory {0}.", new Object[]{in});
		int fileNum = 0;
		for (String filename: files) {
			String filenameNoExt = filename.replace(".xml", "");
			filenameNoExt = filenameNoExt.substring(filenameNoExt.lastIndexOf(File.separator)+1);
			log.log(Level.INFO,"Processing file {0}: {1}.", new Object[]{filenameNoExt,++fileNum});
			String a2Filename = a2OutDir.getAbsolutePath() + File.separator + filenameNoExt + ".ann";
			Document doc = reader.load(filename, true, CoreferenceSemanticItemFactory.class,annotationTypes, null);
			LinkedHashSet<SemanticItem> terms = Document.getSemanticItemsByClass(doc, Term.class);
			if (terms != null) {
				for (SemanticItem ent: terms) 
					log.log(Level.FINE,"Loaded term: {0}." , ((Term)ent).toString());
			}
					
			GenericCoreferencePipeline.linguisticPreProcessing(doc);
			// coreference
			coreferenceResolution(doc);
			// post-process
			chainId = doc.getMaxId(Relation.class);
			List<Class<? extends SemanticItem>> writeTypes = new ArrayList<Class<? extends SemanticItem>>(annotationTypes.keySet());
			writeTypes.add(Expression.class);
			writeTypes.add(CoreferenceChain.class);
					
			List<String> writeLines = new ArrayList<>();
			for (SemanticItem si: doc.getAllSemanticItems()) {
				if (writeTypes.contains(si.getClass()) == false) continue;
				if (si instanceof Expression) {
					// to be consistent with the gold standard, write the mention type just as 'Exp'
					writeLines.add(si.toStandoffAnnotation().replace(si.getType(), "Exp"));
				}
				else if (si instanceof CoreferenceChain) {
					CoreferenceChain cc = (CoreferenceChain)si;
					writeLines.addAll(coreferenceChainToBioNLPStandoffAnnotation(cc));
				} else
					writeLines.add(si.toStandoffAnnotation().replace("SPAN", "Exp"));
			} 
			StandoffAnnotationWriter.writeLines(writeLines,a2Filename);
		}
	}
}
