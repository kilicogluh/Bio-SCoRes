package tasks.coref.spl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.core.CoreferenceChain;
import gov.nih.nlm.bioscores.core.CoreferenceSemanticItemFactory;
import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.CoreferenceUtils;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.io.XMLExpressionReader;
import gov.nih.nlm.ling.brat.StandoffAnnotationWriter;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.io.XMLEntityReader;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;
import gov.nih.nlm.ling.transform.NPInternalTransformation;
import gov.nih.nlm.ling.util.FileUtils;

/**
 * The baseline method for coreference resolution in SPL drug coreference dataset. <p>
 * Afer chunking for noun phrases, the presence of certain pronouns/determiners is 
 * deemed sufficient for mention detection and the closest mention to the left with a 
 * drug semantic type is taken as the referent. The baseline method attempts anaphora
 * resolution only. Adding cataphora resolution yielded worse results.
 * 
 * Pronouns/determiners are those that are used in recognition of various coreferential mention types.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SPLCoreferenceBaseline {
	private static Logger log = Logger.getLogger(SPLCoreferenceBaseline.class.getName());
	// list of pronoun/determiners
	private static List<String> ANAPHORS = Arrays.asList("it","they","itself","themselves","its","their","theirs", 
			"the","this","that","these","those","such","both","each","either","neither", 
			"all","some","one","other","another","a","an","each other","one another","one to the other",
			"which","whose"
			);
	private static boolean goldExp = false;
	
	/**
	 * Identifies baseline mentions in the sentence. <p>
	 * It simply looks for the presence of certain pronouns/determiners.
	 * 
	 * @param sent	the sentence to process
	 */
	public static void mentionDetection(Sentence sent) {
		if (sent == null) return;
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		if (surfs == null) return;
		CoreferenceSemanticItemFactory sif = (CoreferenceSemanticItemFactory)sent.getDocument().getSemanticItemFactory();
		for (SurfaceElement surf : surfs) {
			List<Word> ws= surf.toWordList();
			for (Word w: ws) {
				if (ANAPHORS.contains(w.getText().toLowerCase())) {
					Expression anaphor = sif.newExpression(sent.getDocument(),"Expression", surf);
					log.log(Level.FINE,"Recognized as anaphor: {0}.", anaphor.toString());
					break;
				}
			}
		}
	}
	
	// the simple agreement criteria
	private static boolean compatible(SurfaceElement a, SurfaceElement candidate) {
		return (candidate.isNominal() && CoreferenceUtils.hasNonCoreferentialSemantics(candidate)); 
		
	}
	
	private static SurfaceElement getClosestNPLeft(SurfaceElement surf) {
		Sentence sent = surf.getSentence();
		// same sentence
		if (sent != null) {
			if (sent.getSurfaceElements().get(0).equals(surf) == false) {
				List<SurfaceElement> surfs = sent.getSurfaceElementsFromSpan(
						new Span(sent.getSpan().getBegin(),surf.getSpan().getBegin()-1));
				if (surfs != null) {
					for (int i=surfs.size()-1;i >=0; i--) {
						SurfaceElement su = surfs.get(i);
						if (compatible(surf,su)) return su;
					}
				}
			}
		}
		// previous sentences
		Document doc = sent.getDocument();
		int ind = doc.getSentences().indexOf(sent);	
		for (int i=ind-1; i >=Math.max(0,ind-3); i--) {
			Sentence se = doc.getSentences().get(i);
			List<SurfaceElement> surfs = se.getSurfaceElements();
			if (surfs == null) continue;
			for (int j=surfs.size()-1;j >=0; j--) {
				SurfaceElement su = surfs.get(j);
				if (compatible(surf,su)) return su;
			}
		}
		return null;
	}
	// used for baseline cataphora resolution but did not yield better results, so unused.
	private static SurfaceElement getClosestNPRight(SurfaceElement surf) {
		Sentence sent = surf.getSentence();
		if (sent != null) {
			if (sent.getSurfaceElements().get(sent.getSurfaceElements().size()-1).equals(surf) == false) {
				List<SurfaceElement> surfs = sent.getSurfaceElementsFromSpan(
						new Span(surf.getSpan().getEnd(),sent.getSpan().getEnd()));
				if (surfs != null) {
					for (int i=0;i<surfs.size(); i++) {
						SurfaceElement su = surfs.get(i);
						if (compatible(surf,su)) return su;
					}
				}
			}
		}
		Document doc = sent.getDocument();
		int ind = doc.getSentences().indexOf(sent);	
		for (int i=ind+1; i <Math.min(doc.getSentences().size(),ind+3); i++) {
			Sentence se = doc.getSentences().get(i);
			List<SurfaceElement> surfs = se.getSurfaceElements();
			if (surfs == null) continue;
			for (int j=0;j <surfs.size(); j++) {
				SurfaceElement su = surfs.get(j);
				if (compatible(surf,su)) return su;
			}
		}
		return null;
	}
	
	/**
	 * Links mentions to the closest NP with semantics.
	 * 
	 * @param doc	the document to process
	 */
	public static void mentionReferentLinking(Document doc) {
		log.info("Mention referent linking...");
		List<Sentence> sentences = doc.getSentences();
		if (sentences == null) return;
		CoreferenceSemanticItemFactory sif = (CoreferenceSemanticItemFactory)doc.getSemanticItemFactory();
		for (Sentence sent: sentences) {
			if (sent == null) return;
			List<SurfaceElement> surfs = sent.getSurfaceElements();
			for (SurfaceElement surf: surfs) {
				if (Expression.filterByExpressions(surf).size() > 0) {
					// if there is a problem with head semantics, continue
					try{ 
						SurfaceElement closestNP =  null;
	//					if (surf.containsLemma("following")) 
	//						SurfaceElement closestNPright = getClosestNPRight(surf);
	//					else 
						closestNP = getClosestNPLeft(surf);
						List<Argument> args = new ArrayList<>();
						Expression ana = (Expression) Expression.filterByExpressions(surf).iterator().next();
						SemanticItem ant = null;
						ant = closestNP.getHeadSemantics().iterator().next();
						args.add(new Argument(CoreferenceType.Anaphora.getExpRole().toString(),ana));
						args.add(new Argument(CoreferenceType.Anaphora.getRefRole().toString(),ant));
						CoreferenceChain cchain = sif.
								newCoreferenceChain(doc, CoreferenceType.Anaphora.toString(),args);
						log.log(Level.FINE,"Added coreference chain: {0}.",cchain.toShortString());
					} catch (Exception e) {
					}
				}
			}
		}
	}
	
	/**
	 * Baseline coreference resolution.
	 * 
	 * @param doc	the document to process.
	 */
	public static void coreferenceProcessing(Document doc) {
		List<Sentence> sentences = doc.getSentences();
		if (sentences == null) return;
		List<Sentence> modSentences = new ArrayList<>();
		// linguistic processing and mention detection
		for (int i=0; i < sentences.size(); i++) {
			Sentence sent = sentences.get(i);
			new NPInternalTransformation(NPInternalTransformation.Method.ParseTree).transform(sent);
			if (!goldExp) mentionDetection(sent);
			modSentences.add(sent);
		}
		doc.setSentences(modSentences);
		mentionReferentLinking(doc);
	}
	

	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.err.print("Usage: inputDirectory outputDirectory useGoldMentions");
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
		List<String> files = FileUtils.listFiles(in, false, "xml");
		int fileNum = 0;
		for (String filename: files) {
			String filenameNoExt = filename.replace(".xml", "");
			filenameNoExt = filenameNoExt.substring(filenameNoExt.lastIndexOf(File.separator)+1);
			log.info("Processing " + filenameNoExt + ":" + ++fileNum);
			String a2Filename = a2OutDir.getAbsolutePath() + File.separator + filenameNoExt + ".ann";
			Document doc = null;
			Map<Class<? extends SemanticItem>,List<String>> annTypes = new HashMap<>();
			annTypes.put(Entity.class,Constants.ENTITY_TYPES);
			XMLReader reader = new XMLReader();
			reader.addAnnotationReader(Entity.class, new XMLEntityReader());
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
					log.log(Level.FINE,"Loaded term:" + ((Term)ent).getSurfaceElement().toString());
			}
			coreferenceProcessing(doc);
			LinkedHashSet<SemanticItem> exps = Document.getSemanticItemsByClass(doc, Expression.class);
			if (exps != null) {
				for (SemanticItem exp: exps) {
					exp.setType("Expression");
				}
			}
			// writeResults
			List<Class<? extends SemanticItem>> writeTypes = new ArrayList<Class<? extends SemanticItem>>(annTypes.keySet());
			if (goldExp) writeTypes.add(Expression.class);
			writeTypes.add(CoreferenceChain.class);
			StandoffAnnotationWriter.write(doc, writeTypes, a2Filename);
		}
	}
}

