package tasks.factuality.semrep;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.nlp.util.CollectionUtils;
import edu.stanford.nlp.util.StringUtils;
import gov.nih.nlm.ling.brat.StandoffAnnotationWriter;
import gov.nih.nlm.ling.composition.ArgumentIdentification;
import gov.nih.nlm.ling.composition.ArgumentRule;
import gov.nih.nlm.ling.composition.EmbeddingCategorization;
import gov.nih.nlm.ling.composition.ScalarModalityValueComposition;
import gov.nih.nlm.ling.composition.ScalarModalityValueComposition.ScaleShift;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.MultiWord;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.graph.DocumentGraph;
import gov.nih.nlm.ling.graph.GraphUtils;
import gov.nih.nlm.ling.graph.Node;
import gov.nih.nlm.ling.graph.SemanticGraph;
import gov.nih.nlm.ling.io.XMLEntityReader;
import gov.nih.nlm.ling.io.XMLEventReader;
import gov.nih.nlm.ling.io.XMLPredicateReader;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.process.ComponentLoader;
import gov.nih.nlm.ling.process.IndicatorAnnotator;
import gov.nih.nlm.ling.process.TermAnnotator;
import gov.nih.nlm.ling.sem.AbstractRelation;
import gov.nih.nlm.ling.sem.AbstractTerm;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Concept;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Event;
import gov.nih.nlm.ling.sem.HasPredicate;
import gov.nih.nlm.ling.sem.Indicator;
import gov.nih.nlm.ling.sem.Interval;
import gov.nih.nlm.ling.sem.Modification;
import gov.nih.nlm.ling.sem.Ontology;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.Predication;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.ScalarModalityValue;
import gov.nih.nlm.ling.sem.ScalarModalityValue.ScaleType;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import gov.nih.nlm.ling.sem.Sense;
import gov.nih.nlm.ling.transform.AdverbialTransformation;
import gov.nih.nlm.ling.transform.CoordinationTransformation;
import gov.nih.nlm.ling.transform.DependencyDirectionReversal;
import gov.nih.nlm.ling.transform.DiscourseConnectiveTransformation;
import gov.nih.nlm.ling.transform.HyphenatedAdjectiveTransformation;
import gov.nih.nlm.ling.transform.ModifierCoordinationCorrection;
import gov.nih.nlm.ling.transform.NPInternalTransformation;
import gov.nih.nlm.ling.transform.PPAttachmentCorrection;
import gov.nih.nlm.ling.transform.PhrasalVerbTransformation;
import gov.nih.nlm.ling.transform.PolarityComposition;
import gov.nih.nlm.ling.transform.ScopeTransformation;
import gov.nih.nlm.ling.transform.VerbComplexTransformation;
import gov.nih.nlm.ling.util.FileUtils;
import gov.nih.nlm.ling.wrappers.WordNetWrapper;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

/**
 * The main SemRep factuality pipeline.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SemRepFactualityPipeline {

	private static Logger log = Logger.getLogger(SemRepFactualityPipeline.class.getName());	
	
	private static int aMaxId = 0;
	private static Interval l3Interval = new Interval(1.0,1.0);
	private static Interval l2Interval = new Interval(0.65,0.99);
	private static Interval l1Interval = new Interval(0.26,0.64);
	private static Interval l0Interval = new Interval(0.0,0.0);
	private static Interval negInterval = new Interval(0.0,0.25);
	
	private static Properties properties = null;
	private static LinkedHashSet<Indicator> factualityIndicators = null;
	private static List<ArgumentRule> argumentRules = null;
	private static XMLReader reader = null;
	private static Map<Class<? extends SemanticItem>,List<String>> annotationTypes = null;
	
	
	/**
	 * Extracts the factuality value from a predication and generates the corresponding <code>Modification</code> object.
	 *  
	 * @param predication	the input predication
	 * @return	the factuality <code>Modification</code> object encapsulating the input predication 
	 */
	public static Modification convertScalarModalityValueToFactuality(Predication predication) {
		String value = convertScalarModalityValueToFactualityValue(predication);
		List<Argument> args = new ArrayList<Argument>();
		args.add(new Argument("SemanticItem",predication));
		return new Modification("A" + ++aMaxId,"Factuality",args,value);
	}
	
	/**
	 * Extracts the factuality value of a predication as a string. The value is one of the following:
	 * Fact, Probable, Possible, Doubtful, Counterfact, or Uncommitted.
	 * 
	 * @param predication	the input predication
	 * @return	the factuality value of the predication as a string 
	 */
	public static String convertScalarModalityValueToFactualityValue(Predication predication) {
		boolean non = updateForNon(predication);
		if (predication.getType().equals("ISA"))  return "Fact";
		String indicatorType = "";
		if (predication.getFeatures() != null) {
			indicatorType = (String)predication.getFeatures().get("indicatorType");
			if (indicatorType.equals("MOD/HEAD")) {
				if (non) return "Counterfact";
				return "Fact";
			}
			else if (indicatorType.equals("PREP")) return "Fact";/*{
				return handlePrepositionalPredicates(pr);
			}*/
		}
		List<ScalarModalityValue> smvs = predication.getScalarValues();
		String value = "Fact";
		for (ScalarModalityValue smv: smvs) {
			Interval smvv = smv.getValue();
			 if (smv.getScaleType() == ScaleType.POTENTIAL) {
				 if (Interval.intervalSubsume(l0Interval,smvv) ==false) {
					value="Probable";
//					value = "Fact";
				 } else value = "Counterfact";
			}
			else if (smv.getScaleType() == ScaleType.EPISTEMIC) {
				if (Interval.intervalSubsume(l3Interval, smvv)) value = "Fact";
				else if (Interval.intervalSubsume(l2Interval, smvv)) value = "Probable";
				else if (Interval.intervalSubsume(l1Interval, smvv)) value = "Possible";
				else if (Interval.intervalSubsume(negInterval, smvv) && smvv.getBegin() > 0.0 ) value = "Doubtful";
				else if (Interval.intervalSubsume(l0Interval, smvv)) value = "Counterfact";
			} else 	if (smv.getScaleType() == ScaleType.INTERROGATIVE) {
				if (Interval.intervalSubsume(l3Interval, smvv)) value = "Uncommitted";
				else if (Interval.intervalSubsume(l0Interval, smvv)) value = "Counterfact";
			} /*else 	if (smv.getScaleType() == ScaleType.INTENTIONAL) {
				if (Interval.intervalSubsume(l3Interval, smvv)) value = "Uncommitted";
				else if (Interval.intervalSubsume(l0Interval, smvv)) value = "Counterfact";
			}*/ else 	if (smv.getScaleType() == ScaleType.SUCCESS) {
				if (Interval.intervalSubsume(l3Interval, smvv)) value = "Fact";
				else if (Interval.intervalSubsume(l2Interval, smvv)) value = "Probable";
				else if (Interval.intervalSubsume(l1Interval, smvv)) value = "Possible";
				else if (Interval.intervalSubsume(negInterval, smvv) && smvv.getBegin() > 0.0 ) value = "Doubtful";
				else if (Interval.intervalSubsume(l0Interval, smvv)) value = "Counterfact";
			} else 	if (smv.getScaleType() == ScaleType.DEONTIC) {
				if (Interval.atLeft(l2Interval,smvv) ) value = "Uncommitted";
			}  /*else if (smv.getScaleType() == ScaleType.VOLITIVE) {
				if (Interval.intervalSubsume(l3Interval, smvv)) value = "Uncommitted";
				if (Interval.atLeft(l0Interval, smvv) ) value = "Counterfact";
			} */
			return value;
		}
		return value;
	}
	
	private static Predication inferenceFrom(Predication pr) {
		// if a predication is an INFER predication, get the base predication for it.
		Document doc = pr.getDocument();
		List<SemanticItem> args = pr.getArgItems();
		LinkedHashSet<SemanticItem> all = Document.getSemanticItemsByClass(doc,Predication.class);
		for (SemanticItem prss : all) {
			Predication cc = (Predication)prss;
			if (cc.equals(pr)) continue;
			if (cc.getType().equals(pr.getType()) == false) continue;
			if (CollectionUtils.containsAny(args, cc.getArgItems()) && cc.getFeatures().get("specInfer") == null) {
				return cc;
			}
		}
		return null;
	}
	
	private static boolean updateForNon(Predication pr) {
		// special treatment for lexical negation
		Predicate p = pr.getPredicate();
		SurfaceElement se = p.getSurfaceElement();
		String setext = se.getText().toLowerCase();
		String prtext = p.getText().toLowerCase();
		if (setext.equals("non" + prtext) || setext.equals("non-" + prtext)) {
			ScalarModalityValueComposition.updateScaleValues(ScaleShift.NEGATE, pr.getScalarValues());
			return true;
		}
		return false;
	}
	
	/**
	 * Writes a human-readable outout of the document, similar to SemRep's full-fielded output.
	 * 
	 * @param document				the processed document
	 * @param readableOutFileName	the output file name
	 * 
	 * @throws IOException			if there is a problem with the output file
	 */
	public static void writeReadable(Document document, String readableOutFileName) 
			throws IOException {
		PrintWriter pw;
		if (readableOutFileName == null)  pw = new PrintWriter(System.out);
		else pw = new PrintWriter(readableOutFileName);
		String docId = document.getId();
		for (Sentence s: document.getSentences()) {
			String stext = s.getText();
			pw.write(docId+ "|" + s.getId() + "|text|" + stext);
			pw.write("\n");
			LinkedHashSet<SemanticItem> spreds = Document.getSemanticItemsByClassSpan(document, Predicate.class, new SpanList(s.getSpan()),false);
			for (SemanticItem spred: spreds) {
				Predicate pred = (Predicate)spred;
				LinkedHashSet<Relation> rels = Document.getRelationsWithPredicate(document, pred);
				for (Relation rel: rels) {
					if (rel instanceof Predication) {
						Predication prsi = (Predication)rel;
						String specInfer = "";
						if (prsi.getFeatures() != null && prsi.getFeatures().get("specInfer") != null) {
							specInfer = prsi.getFeatures().get("specInfer").toString();
						}
						if (Constants.SEMREP_RELATION_TYPES.contains(prsi.getType()) == false) continue;
						StringBuffer readable = new StringBuffer();
						Entity subj = (Entity)(prsi.getArgs("Subject").get(0)).getArg();
						Concept subjConc = subj.getSense();
						Entity obj = (Entity)(prsi.getArgs("Object").get(0)).getArg();
						Concept objConc = obj.getSense();
						if (subjConc == null) {
							readable.append(docId + "|" + s.getId() + "|relation|" + subj.getId() +"|"+subj.getText()+"|"+subj.getType()+"|"+subj.getText()+"|");
						} else 
							readable.append(docId + "|" + s.getId() + "|relation|" + subjConc.getId() +"|"+subjConc.getName()+"|"+StringUtils.join(subjConc.getSemtypes(),",")+"|"+subj.getText()+"|");
						if (specInfer.equals("")) 
							readable.append(pred.getType()+"|"+pred.getText()+"|");
						else 
							readable.append(pred.getType()+ "(" + specInfer + ")" + "|"+pred.getText()+"|");
						if (objConc == null) {
							readable.append(docId + "|" + s.getId() + "|relation|" + obj.getId() +"|"+obj.getText()+"|"+obj.getType()+"|"+obj.getText()+"|");
						} else 
							readable.append(objConc.getId()+"|"+objConc.getName()+"|"+StringUtils.join(objConc.getSemtypes(),",")+"|"+obj.getText());
						Modification factualityValue = convertScalarModalityValueToFactuality(prsi);
						readable.append("|"+factualityValue.getValue());
						for (ScalarModalityValue smv: prsi.getScalarValues()) {
							readable.append("|" + smv.toString());
						}
						pw.write(readable.toString().trim());
						pw.write("\n");
					}
				}
			}
			pw.write("\n");
			pw.flush();
		}
		pw.close();
	}
	
	/**
	 * Writes standoff annotation output of the document.
	 * 
	 * @param document			the processed document
	 * @param annOutFileName	the output file name
	 * 
	 * @throws IOException		if there is a problem with the output file
	 */
	public static void writeStandoff(Document document, String annOutFileName) 
			throws IOException {
		List<Class<? extends SemanticItem>> writeTypes = new ArrayList<>(annotationTypes.keySet());
		writeTypes.add(Predication.class);
		write(document, writeTypes, annOutFileName);
/*		for (SemanticItem si: Document.getSemanticItemsByClass(doc, Predication.class)) {
			Predication pr = (Predication)si;
			boolean print = false;
			for (ScalarModalityValue sc: pr.getScalarValues()) {
				if (sc.getValue().getEnd() < 1.0) { print = true; break;}
			}
			if (print) log.warning("NON-FACTUAL:" + pr.toShortString());
		}
		for (SemanticItem si: Document.getSemanticItemsByClass(doc, Predication.class)) {
			Predication pr = (Predication)si;
			if (pr.hasDefaultSource() == false) 
				log.warning("NON-AUTHOR PREDICATION:" + pr.toShortString());
		}
		log.warning("SEM SIZE: " + doc.getAllSemanticItems().size());*/
	}
	
	private static void write(Document doc, List<Class<? extends SemanticItem>> categories, String fileName) 
			throws IOException {
		if (doc == null) return;
		// write all annotations
		if (categories == null) StandoffAnnotationWriter.write(doc, fileName);
		// write specific annotations
		else {
			Set<SemanticItem> catItems = new HashSet<SemanticItem>();
			for (Class<? extends SemanticItem> cat: categories) {
				LinkedHashSet<SemanticItem> itemSet = 
						Document.getSemanticItemsByClass(doc, cat);
				if (itemSet == null) continue;
				catItems.addAll(itemSet);
			}
			write(new ArrayList<SemanticItem>(catItems), fileName);
		}
	}
	
	private static void write(final Collection<SemanticItem> semanticItems, String fileName) throws IOException {
		if (semanticItems == null) return;
		List<String> seenIds = new ArrayList<>();
		PrintWriter pw;
		if (fileName == null) pw = new PrintWriter(System.out);
		else pw = new PrintWriter(fileName);
		List<SemanticItem> asList = new ArrayList<>();
		for (SemanticItem si : semanticItems) {
			asList.add(si);
/*			if (si instanceof Predication) {
				Predication sipr = (Predication)si;
				Predication npr = addFromPredicate(sipr);
				if (npr != null) asList.add(npr);
			}*/
		}
		for (SemanticItem si: asList) {
			if (seenIds.contains(si.getId())) continue; 
			if (si instanceof AbstractTerm) {
				if (!(seenIds.contains(si.getId()))) {
					pw.write(si.toStandoffAnnotation());
					pw.write("\n");
					seenIds.add(si.getId());
				}
			} else if (si instanceof AbstractRelation) {
				Predicate p = null;
				if (si instanceof HasPredicate) {
					p = ((HasPredicate)si).getPredicate();
				} 
				if (p != null && !(seenIds.contains(p.getId())))  {
					pw.write(p.toStandoffAnnotation());
					pw.write("\n");
					seenIds.add(p.getId());
				}	
				if (si instanceof Predication) {
					Predication prsi = (Predication)si;
					String specInfer = "";
					if (prsi.getFeatures() != null && prsi.getFeatures().get("specInfer") != null) {
						specInfer = prsi.getFeatures().get("specInfer").toString();
					}
					String type = prsi.getType();
//					if (specInfer.equals("") == false) 
//						type += "(" + specInfer + ")";
					if (Constants.SEMREP_RELATION_TYPES.contains(type) || 
							Constants.SEMREP_NEG_RELATION_TYPES.contains(type)) {
						pw.write(prsi.toStandoffAnnotation());
						pw.write("\n");
						Modification factualityLevel = convertScalarModalityValueToFactuality(prsi);
						pw.write(factualityLevel.toStandoffAnnotation());
						pw.write("\n");
					}
//					pw.println(prsi.toStandoffAnnotation());
					seenIds.add(si.getId());
				}
			}
		}
		pw.flush();
		pw.close();
	}

	/**
	 * Annotates factuality triggers in a document. 
	 * If an entity or another trigger has already been annotated to subsume the trigger, the trigger is not annotated.
	 * 
	 * @param document					the document to annotate
	 * @param indicators				the factuality triggers
	 * 
	 * @throws ClassNotFoundException	if trigger annotation class is not found
	 * @throws InstantiationException	if the trigger annotation class cannot be instantiated
	 * @throws IllegalAccessException	if there is a problem with access to trigger annotation class
	 */
	public static void annotateIndicators(Document document, LinkedHashSet<Indicator> indicators) 
			throws ClassNotFoundException, InstantiationException, IllegalAccessException{
		SemanticItemFactory sif = document.getSemanticItemFactory();
		List<TermAnnotator> termAnnotators = ComponentLoader.getTermAnnotators(properties);
		Map<SpanList,LinkedHashSet<Ontology>> annotations = new HashMap<>();
		for (TermAnnotator annotator : termAnnotators) {
			if (annotator instanceof IndicatorAnnotator) {
				((IndicatorAnnotator)annotator).setIndicators(indicators);
				String ignore = properties.getProperty("ignorePOSforIndicators");
				boolean ignorePOS = Boolean.parseBoolean(ignore == null ? "false" : ignore);
				((IndicatorAnnotator)annotator).setIgnorePOS(ignorePOS);
			}
			annotator.annotate(document, properties, annotations);
		}
		// subsumed by an entity
		for (SpanList sp: annotations.keySet()) {
			LinkedHashSet<Ontology> onts = annotations.get(sp);
			// this improved overall very slightly (mainly COUNTERFACT)
			LinkedHashSet<SemanticItem> entities = Document.getSemanticItemsByClassSpan(document, Entity.class, sp, true);
			boolean subsumed = false;
			for (SemanticItem ent: entities) {
				Entity e = (Entity)ent;
				if (SpanList.subsume(e.getSpan(), sp) && e.getSurfaceElement().isNominal()) {			
					log.log(Level.FINEST,"Removing subsumed trigger: {0}.", ent.toShortString()); subsumed = true; break;}	
			}
			if (subsumed) continue;
			Indicator ind = (Indicator)onts.iterator().next();
			Sense sense = ind.getMostProbableSense();
			Word head = MultiWord.findHeadFromCategory(document.getWordsInSpan(sp));
			sif.newPredicate(document, sp, head.getSpan(), ind, sense);
		}
		// subsumed by another predicate
		removeSubsumedPredicates(document, indicators);
	}
	
	private static void removeSubsumedPredicates(Document doc, LinkedHashSet<Indicator> indicators) {
		// remove factuality triggers subsumed by other factuality triggers ('not' vs. 'may not' for example)
		List<SemanticItem> toRemove = new ArrayList<>();
		LinkedHashSet<SemanticItem> terms = Document.getSemanticItemsByClass(doc, Predicate.class);
		for (SemanticItem term : terms) {
			Predicate t = (Predicate)term;
			Indicator ind = t.getIndicator();
			SurfaceElement su = t.getSurfaceElement();
			LinkedHashSet<SemanticItem> suTerms = su.filterByPredicates();
			for (SemanticItem suT: suTerms) {
				if (suT.equals(t) || toRemove.contains(suT)) continue;
				if (indicators.contains(ind) == false) continue;
				if (SpanList.subsume(suT.getSpan(), t.getSpan()) && suT.getSpan().length() > t.getSpan().length()) {
					toRemove.add(t);
					break;
				}
			}
		}
		for (SemanticItem rem: toRemove) {
			log.log(Level.FINEST, "Removing subsumed factuality trigger: {0}.", rem.toShortString());
			doc.removeSemanticItem(rem);
		}
	}
	
	
	private static void writeTransformation(Sentence sent, String type) {
		if (log.isLoggable(Level.FINEST) == false) return;
		for (SynDependency sd: sent.getEmbeddings()) {
			log.log(Level.FINEST,"{0} {1}", new Object[]{type,sd.toShortString()});
		}
	}
	
	private static void transformSentence(Sentence sent) {
		// transform the sentence using transformation rules
		writeTransformation(sent,"INPUT");
		new DependencyDirectionReversal().transform(sent);
		writeTransformation(sent,"DependencyReversal");

		new DiscourseConnectiveTransformation().transform(sent);
		writeTransformation(sent,"DiscourseConnective");
		new PPAttachmentCorrection().transform(sent);
		writeTransformation(sent,"PPAttachment");

		new VerbComplexTransformation().transform(sent);
		writeTransformation(sent,"VerbComplex");
		new PhrasalVerbTransformation().transform(sent);
		writeTransformation(sent,"PhrasalVerb");
		new ModifierCoordinationCorrection().transform(sent);
		writeTransformation(sent,"ModifierCoord");
		new CoordinationTransformation().transform(sent);
		writeTransformation(sent,"Coordination");

		new AdverbialTransformation().transform(sent);
		writeTransformation(sent,"Adverbial");
		
		new NPInternalTransformation().transform(sent);
		writeTransformation(sent,"NPInternal");
		new HyphenatedAdjectiveTransformation().transform(sent);
		writeTransformation(sent,"HyphenatedAdjective");
		new ScopeTransformation().transform(sent);
		writeTransformation(sent,"Scope");
		new PolarityComposition().transform(sent);
		writeTransformation(sent,"PolarityComp");
	}
	
	private static void updateInferPredications(Document doc) {
		// if it is a INFER predication, get the scalar value and source from the base predication.
		for (SemanticItem si: Document.getSemanticItemsByClass(doc, Predication.class)) {
			Predication pr = (Predication)si;
			String infer= "";
			if (pr.getFeatures() != null && pr.getFeatures().get("specInfer") != null) infer = pr.getFeatures().get("specInfer").toString();
			if (infer.equals("") == false) {
				Predication fromInfer = inferenceFrom(pr);
				if (fromInfer != null) {
					pr.setScalarValues(fromInfer.getScalarValues());
					pr.setSources(fromInfer.getSources());
				}
			}
		}
	}
	
	/**
	 * Processes a single <code>Document</code> with the Embedding Framework.
	 * 
	 * @param document		the document to process
	 * @param indicators	the set of factuality triggers to use
	 * @param rules			the list of argument identification rules to use
	 * 
	 * @throws ClassNotFoundException 	if there is a problem with indicator annotator class
	 * @throws IllegalAccessException	if there is a problem with indicator annotation
	 * @throws InstantiationException	if there is a problem with indicator annotation
	 *  
	 */
	public static void process(Document document, LinkedHashSet<Indicator> indicators, List<ArgumentRule> rules) 
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		if (document == null) {
			log.severe("Invalid document.");
			return;
		}
		aMaxId = 0;
		annotateIndicators(document,indicators);
		for (Sentence sent: document.getSentences()) {
			transformSentence(sent);
		}
		DocumentGraph docGraph = new DocumentGraph(document);
		List<Node> roots = GraphUtils.getRoots(docGraph);
		SemanticGraph semGraph = new SemanticGraph(document);
		LinkedHashSet<SemanticItem> childSems = new LinkedHashSet<>();
		for (Node root: roots) {
			ArgumentIdentification.argumentIdentification(root, docGraph, rules, semGraph, childSems, false, false);
		}
		updateInferPredications(document);
		for (SemanticItem si: document.getAllSemanticItems()) {
			log.log(Level.FINEST,"Document semantic item: {0}.", new Object[]{si.toShortString()});
		}
	}
	
	/**
	 * Processes a directory of XML files and writes the output of the pipeline
	 * as standoff annotation and as human-readable output. 
	 * 
	 * To incorporate SemRep information, it also uses as input the corresponding full-fielded SemRep output file. 
	 * 
	 * @param inDirName				the XML input directory
	 * @param annOutDirName			the standoff annotation output directory
	 * @param readableOutDirName	the human-readable output directory
	 * @param indicators			the set of factuality triggers to use
	 * @param rules					the list of argument identification rules to use
	 * 
	 * @throws IOException 				if there is a problem with file reading/writing
	 * @throws ClassNotFoundException 	if there is a problem with indicator annotator class
	 * @throws IllegalAccessException	if there is a problem with indicator annotation
	 * @throws InstantiationException	if there is a problem with indicator annotation
	 * 
	 */
	public static void processDirectory(String inDirName, String annOutDirName, String readableOutDirName, 
			LinkedHashSet<Indicator> indicators, List<ArgumentRule> rules) 
			throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		File annDir = new File(inDirName);
		if (annDir.isDirectory() == false) {
			System.err.println("First argument is required to be an input directory:" + annDir);
			return;
		}
		if (indicators == null || indicators.size() == 0) {
			log.severe("No factuality triggers are provided.");
			return;
		}
		if (rules == null || rules.size() == 0) {
			log.severe("No argument identification rules are provided.");
			return;
		}
		
		File annOutDir = new File(annOutDirName);
		if (annOutDir.isDirectory() == false) {
			System.err.println("The directory " + annOutDir + " doesn't exist. Creating a new directory..");
			annOutDir.mkdir();
		}
		File readableOutDir = new File(readableOutDirName);
		if (readableOutDir.isDirectory() == false) {
			System.err.println("The directory " + readableOutDir + " doesn't exist. Creating a new directory..");
			readableOutDir.mkdir();
		}
		
		int fileNum = 0;
		List<String> files = FileUtils.listFiles(inDirName, false, "xml");
		for (String filename: files) {
			String id = filename.substring(filename.lastIndexOf(File.separator)+1).replace(".xml", "");
			log.log(Level.INFO,"Processing {0}: {1}.", new Object[]{id,++fileNum});
//			if (filenameNoExt.contains("10638965") == false) continue;
			String annFilename = annOutDir.getAbsolutePath() + File.separator + id + ".ann";
//			if (new File(a2Filename).exists()) continue;
			String readableFilename = readableOutDir.getAbsolutePath() + File.separator + id + ".out";
			if (new File(annFilename).exists()) {
				log.log(Level.INFO, "Output XML file exists: {0}. Skipping..", annFilename);
//				continue;
			}
			Document doc = reader.load(filename, true, SemanticItemFactory.class,annotationTypes, null);
			if (doc == null) {
				log.log(Level.SEVERE,"Invalid document: {0}.", id);
				continue;
			}
			process(doc, indicators, rules);
			writeStandoff(doc,annFilename);
			writeReadable(doc,readableFilename);
		}
	}
	
	/**
	 * Loads factuality triggers from a file defined in properties.
	 * 
	 * @param filename	the name of the file with the indicators
	 * 
	 * @return the set of factuality triggers
	 * 
	 * @throws IOException				if properties file cannot be found
	 * @throws ParsingException			if indicator file cannot be parsed
	 * @throws ValidityException		if the indicator file is not valid
	 */
	public static LinkedHashSet<Indicator> loadFactualityIndicators(String filename) 
			throws IOException, ParsingException, ValidityException {
		LinkedHashSet<Indicator> indicators = Indicator.loadIndicatorsFromFile(filename,0);
/*		LinkedHashSet<Indicator> indicators = Indicator.filterIndicatorsHierarchically(
			 					Indicator.filterVerified(allIndicators), Arrays.asList("MODAL","VALENCE_SHIFTER"));*/
		log.log(Level.INFO,"Loaded {0} factuality triggers.", indicators.size());
		for (Indicator ind: indicators) {
			log.log(Level.INFO,"Factuality trigger: {0}.", ind.toString());
		}	
		return indicators;
	}
	
	/**
	 * Initializes SemRep relation definitions.
	 * 
	 */
	public static void initSemRepDefinitions() {
		Event.setDefinitions(Constants.loadSemRepDefinitions());
		Predication.addDefinitions(Event.getDefinitions());
	}
		
	/**
	 * Initializes the XML readers for SemRep semantic items.
	 * 
	 * @return an XML reader that can read relevant semantic items.
	 * 
	 */
	public static XMLReader getXMLReader() {
		XMLReader reader = new XMLReader();
		reader.addAnnotationReader(Entity.class, new XMLEntityReader());
		reader.addAnnotationReader(Predicate.class, new XMLPredicateReader());
		reader.addAnnotationReader(Event.class, new XMLEventReader());
		return reader;
	}
	
	/**
	 * Initializes the input annotation types to SemRep entity/relation types.
	 * 
	 * @return a Map of semantic item classes and the corresponding subtypes.
	 * 
	 */
	public static Map<Class<? extends SemanticItem>,List<String>> getAnnotationTypes() {
		Map<Class<? extends SemanticItem>,List<String>> annotationTypes = new HashMap<>();
		List<String>  entityTypes = new ArrayList<String>(Constants.SEMREP_ENTITY_TYPES);
		annotationTypes.put(Entity.class,entityTypes);
		annotationTypes.put(Predicate.class, Constants.SEMREP_RELATION_TYPES);
		annotationTypes.put(Event.class,Constants.SEMREP_RELATION_TYPES);
		return annotationTypes;
	}
	
	public static void main(String[] args) 
			throws Exception {
		if (args.length < 3) {
			System.err.print("Usage: xmlInputDirectory standoffOutputDirectory readableOutputDirectory");
		}
		properties = FileUtils.loadPropertiesFromFile("factuality_semrep.properties");
		String in = args[0];
		String annOut = args[1];
		String readableOut = args[2];
		
		initSemRepDefinitions();
		WordNetWrapper.getInstance(properties);
		EmbeddingCategorization.getInstance();
		argumentRules = ArgumentRule.loadRules();
		factualityIndicators = loadFactualityIndicators(properties.getProperty("indicatorFile"));
		reader = getXMLReader();
		annotationTypes = getAnnotationTypes();
		
		processDirectory(in,annOut,readableOut,factualityIndicators,argumentRules);			
	}
	
/*	private static String handlePrepositionalPredicates(Predication pr) {
		Predicate p = pr.getPredicate();
		Sentence s = p.getSurfaceElement().getSentence();
		if (s.getId().equals("S1") == false) return "Fact";
		if (pr.getType().equals("PROCESS_OF") || pr.getType().equals("PART_OF") || pr.getType().equals("LOCATION_OF")) return "Fact";
		String main = getMainVerb(s);
		if (main.equals("NONE")) return "Uncommitted";
		return "Fact";
	}*/
	
/*	public static String getMainVerb(Sentence s) {
		  List<Word> verbs = new ArrayList<>();
		  for (Word w: s.getWords()) {
			  if (w.isVerbal() || w.isAdjectival()) verbs.add(w);
		  }
		  List<Word> nodeps = new ArrayList<>();
		  for (Word w: verbs) {
			  List<SynDependency> dd = SynDependency.dependenciesWithType(w, s.getDependencyList(), "dep", true);
			  List<SynDependency> conjs = SynDependency.dependenciesWithType(w, s.getDependencyList(), "conj", false);
			  dd.addAll(conjs);
			  List<SynDependency> govs = SynDependency.outDependencies(w, s.getDependencyList());
			  List<SynDependency> deps = SynDependency.inDependencies(w, s.getDependencyList());
			  govs.removeAll(dd);
			  deps.removeAll(dd);
			  if (govs.size() > 0 && deps.size() == 0) {
				  nodeps.add(w);
			  }
		  }
		  if (nodeps.size() > 0) return nodeps.get(0).getText().toLowerCase();
		  return "NONE";
	  }*/
	
	// TODO This should really be in ArgumentIdentification. Lexical embedding??
/*	private static Predication addFromPredicate(Predication pr) {
		Predicate p = pr.getPredicate();
		SpanList sp = p.getSpan();
		Sentence sent = p.getSurfaceElement().getSentence();
		if (lexicalNegs.contains(p.getText().toLowerCase()) == false) return null;
		Document doc = sent.getDocument();
		LinkedHashSet<SemanticItem> spanPs = Document.getSemanticItemsByClassSpan(doc, Predicate.class, sp,false);
//		LinkedHashSet<SemanticItem> spanPs = p.getSurfaceElement().filterByPredicates();
		Predicate other = null;
		for (SemanticItem spanP: spanPs) {
			if (spanP.equals(p)) continue;
			if (EmbeddingCategorization.getAllDescendants(Arrays.asList("MODAL","VALENCE_SHIFTER")).
					contains(spanP.getType())) {
				other = (Predicate)spanP;
	//			LinkedHashSet<Relation> allPreds = Document.getRelationsWithPredicate(doc, p);
	//			for (Relation pred: allPreds) {
					List<Argument> args = new ArrayList<>();
					args.add(new Argument("COMP",(Predication)pr));
					Predication np = doc.getSemanticItemFactory().newPredication(doc, other, args, null,null);
					np.setSources(pr.getSources());
					np.setSource(true);
					List<ScalarModalityValue> smvs = pr.getScalarValues();
					for (ScalarModalityValue smv: smvs) {
						if (smv.getScaleType() == ScaleType.EPISTEMIC) {
							smv.setValue(new Interval(0.0,0.0));
						}
					}
					ScalarModalityValueComposition.propagateScalarModalityValues(np);
					//SourcePropagation.propagateSource(np);
	//				allOut.add(np);
	//			}
				return np;
			}
		}
		return null;
	}*/
	
	/*	private static void removeUnusedPredicates(Document doc) {
	List<SemanticItem> toRemove = new ArrayList<>();
	LinkedHashSet<SemanticItem> terms = Document.getSemanticItemsByClass(doc, Predicate.class);
	for (SemanticItem term : terms) {
		Predicate p = (Predicate)term;
		LinkedHashSet<Relation> rels = Document.getRelationsWithPredicate(doc, p);
		if (rels == null || rels.size() == 0)  {
			toRemove.add(p);
		}
	}
	for (SemanticItem rem: toRemove) {
		log.log(Level.FINEST,"Removing predicate {0}.", rem.toShortString());
		doc.removeSemanticItem(rem);
	}
}*/
}
