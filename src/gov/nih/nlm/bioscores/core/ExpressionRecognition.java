package gov.nih.nlm.bioscores.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.exp.DefiniteNPOps;
import gov.nih.nlm.bioscores.exp.DemonstrativeNPOps;
import gov.nih.nlm.bioscores.exp.DemonstrativePronounOps;
import gov.nih.nlm.bioscores.exp.DistributiveNPOps;
import gov.nih.nlm.bioscores.exp.DistributivePronounOps;
import gov.nih.nlm.bioscores.exp.IndefiniteNPOps;
import gov.nih.nlm.bioscores.exp.IndefinitePronounOps;
import gov.nih.nlm.bioscores.exp.PersonalPronounOps;
import gov.nih.nlm.bioscores.exp.PossessivePronounOps;
import gov.nih.nlm.bioscores.exp.ReciprocalPronounOps;
import gov.nih.nlm.bioscores.exp.RelativePronounOps;
import gov.nih.nlm.bioscores.exp.ZeroArticleNPOps;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;

/**
 * This class contains static methods to identify various types of pronouns and NPs
 * that can corefer to other entities/events in a {@link Document}.
 * 
 * @author Halil Kilicoglu
 *
 */
public class ExpressionRecognition {
	private static Logger log = Logger.getLogger(ExpressionRecognition.class.getName());
	
	private static Configuration config = Configuration.getInstance();
	
	/** 
	 * Annotates a <code>Document</code> with coreferential mentions, reads
	 * the types to consider come from the configuration.
	 * 
	 * @param doc  the document to annotate
	 */
	public static synchronized void annotate(Document doc) {
		List<Sentence> sentences = doc.getSentences();
		if (sentences == null) return;
		List<Sentence> modSentences = new ArrayList<>();
		for (int i=0; i < sentences.size(); i++) {
			Sentence sent = sentences.get(i);
			if (config.hasExpType(ExpressionType.PersonalPronoun)) 
				annotatePersonalPronouns(sent);
			if (config.hasExpType(ExpressionType.PossessivePronoun)) 
				annotatePossessivePronouns(sent);
			if (config.hasExpType(ExpressionType.RelativePronoun))
				annotateRelativePronouns(sent);			
			if (config.hasExpType(ExpressionType.DemonstrativePronoun))
				annotateDemonstrativePronouns(sent);
			if (config.hasExpType(ExpressionType.DistributivePronoun))
				annotateDistributivePronouns(sent);
			if (config.hasExpType(ExpressionType.ReciprocalPronoun)) 
				annotateReciprocalPronouns(sent);			
			if (config.hasExpType(ExpressionType.IndefinitePronoun)) 	
				annotateIndefinitePronouns(sent);		
			if (config.hasExpType(ExpressionType.DefiniteNP)) 
				annotateDefiniteNPs(sent);
			if (config.hasExpType(ExpressionType.IndefiniteNP)) 
				annotateIndefiniteNPs(sent);			
			if (config.hasExpType(ExpressionType.DemonstrativeNP)) 	
				annotateDemonstrativeNPs(sent);
			if (config.hasExpType(ExpressionType.DistributiveNP)) 
				annotateDistributiveNPs(sent);
			if (config.hasExpType(ExpressionType.ZeroArticleNP)) 
				annotateZeroArticleNPs(sent);			
			modSentences.add(sent);
			LinkedHashSet<SemanticItem> sentExps = 
					Document.getSemanticItemsByClassSpan(doc, Expression.class, new SpanList(sent.getSpan()),true);
			for (SemanticItem exp: sentExps) {
				log.log(Level.FINE,"Found coreferential mention: {0}.", new String[]{exp.toShortString()});
			}
		}
		doc.setSentences(modSentences);
	}
		
	/** 
	 * Annotates a {@link Sentence} object with personal pronouns (e.g., <i>it, they</i>). 
	 * Filters out pleonastic <i>it</i>. 
	 * Assumes that the <code>Sentence</code> object has already been pre-processed for syntax. 
	 * 
	 * @param sent  the sentence to annotate
	 */
	public static synchronized void annotatePersonalPronouns(Sentence sent) {
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		Document doc = sent.getDocument();
		CoreferenceSemanticItemFactory csif = getCoreferenceSemanticItemFactory(doc);
		if (csif == null) return;
		String type = ExpressionType.PersonalPronoun.toString();
		for (SurfaceElement se: surfs) {
			if (new PersonalPronounOps().recognize(se)) {
				Expression exp = csif.newExpression(doc,type, se.getSpan(),se.getHead().getSpan(),se.getText());
				log.log(Level.FINE,"Personal pronominal mention: {0}.", new Object[]{exp.toShortString()});
			}
		}
	}
	
	/** 
	 * Annotates a <code>Sentence</code> object with possessive pronouns (e.g., <i>its, their</i>). 
	 * Assumes that the <code>Sentence</code> object has already been pre-processed for syntax. 
	 * 
	 * @param sent  the sentence to annotate
	 */
	public static synchronized void annotatePossessivePronouns(Sentence sent) {
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		Document doc = sent.getDocument();
		CoreferenceSemanticItemFactory csif = getCoreferenceSemanticItemFactory(doc);
		if (csif == null) return;
		String type = ExpressionType.PossessivePronoun.toString();
		for (SurfaceElement se: surfs) {
			if (new PossessivePronounOps().recognize(se)) {
				Expression exp = csif.newExpression(doc,type, se.getSpan(),se.getHead().getSpan(),se.getText());
				log.log(Level.FINE,"Possessive pronominal mention: {0}.", new Object[]{exp.toShortString()});
			}
		}
	}
	
	/** 
	 * Annotates a <code>Sentence</code> object with relative pronouns (e.g., <i>which, that</i>).  
	 * 
	 * @param sent  the sentence to annotate
	 */
	public static synchronized void annotateRelativePronouns(Sentence sent) {
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		Document doc = sent.getDocument();
		CoreferenceSemanticItemFactory csif = getCoreferenceSemanticItemFactory(doc);
		if (csif == null) return;
		String type = ExpressionType.RelativePronoun.toString();
		for (SurfaceElement se: surfs) {
			if (new RelativePronounOps().recognize(se)) {
				Expression exp = csif.newExpression(doc,type, se.getSpan(),se.getHead().getSpan(),se.getText());
				log.log(Level.FINE,"Relative pronominal mention: {0}.", new Object[]{exp.toShortString()});
			}
		}
	}
	
	/** 
	 * Annotates a <code>Sentence</code> object with demonstrative pronouns (e.g., <i>these</i>, <i>that</i>).  
	 * Assumes that the <code>Sentence</code> object has already been pre-processed for syntax. 
	 * 
	 * @param sent  the sentence to annotate
	 */
	public static synchronized void annotateDemonstrativePronouns(Sentence sent) {
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		Document doc = sent.getDocument();
		CoreferenceSemanticItemFactory csif = getCoreferenceSemanticItemFactory(doc);
		if (csif == null) return;
		String type = ExpressionType.DemonstrativePronoun.toString();
		for (SurfaceElement se: surfs) {
			if (new DemonstrativePronounOps().recognize(se)) {
				Expression exp = csif.newExpression(doc,type, se.getSpan(),se.getHead().getSpan(),se.getText());
				log.log(Level.FINE,"Demonstrative pronominal mention: {0}.", new Object[]{exp.toShortString()});
			}
		}
	}
	
	/** 
	 * Annotates a <code>Sentence</code> object with distributive pronouns (e.g., <i>each, both</i>).  
	 * 
	 * @param sent  the sentence to annotate
	 */
	public static synchronized void annotateDistributivePronouns(Sentence sent) {
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		Document doc = sent.getDocument();
		CoreferenceSemanticItemFactory csif = getCoreferenceSemanticItemFactory(doc);
		if (csif == null) return;
		String type = ExpressionType.DistributivePronoun.toString();
		for (SurfaceElement se: surfs) {
			if (new DistributivePronounOps().recognize(se)) {
				Expression exp = csif.newExpression(doc,type, se.getSpan(),se.getHead().getSpan(),se.getText());
				log.log(Level.FINE,"Distributive pronominal mention: {0}.", new Object[]{exp.toShortString()});
			}
		}
	}
	
	/** 
	 * Annotates a <code>Sentence</code> object with reciprocal pronouns (e.g., <i>one another</i>).  
	 * 
	 * @param sent  the sentence to annotate
	 */
	public static synchronized void annotateReciprocalPronouns(Sentence sent) {
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		Document doc = sent.getDocument();
		CoreferenceSemanticItemFactory csif = getCoreferenceSemanticItemFactory(doc);
		if (csif == null) return;
		String type = ExpressionType.ReciprocalPronoun.toString();
		for (SurfaceElement se: surfs) {
			if (new ReciprocalPronounOps().recognize(se)) {
				SpanList sp = ReciprocalPronounOps.getReciprocalSpan(se);
				Expression exp = csif.newExpression(doc,type, sp,sp,sent.getStringInSpan(sp));
				sent.synchSurfaceElements(exp.getSurfaceElement());
				log.log(Level.FINE,"Reciprocal pronominal mention: {0}.", new Object[]{exp.toShortString()});
			}
		}
		sent.synchEmbeddings();
	}
	
	/** 
	 * Annotates a <code>Sentence</code> object with indefinite pronouns (e.g., <i>all, any</i>).  
	 * 
	 * @param sent  the sentence to annotate
	 */
	public static synchronized void annotateIndefinitePronouns(Sentence sent) {
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		Document doc = sent.getDocument();
		CoreferenceSemanticItemFactory csif = getCoreferenceSemanticItemFactory(doc);
		if (csif == null) return;
		String type = ExpressionType.IndefinitePronoun.toString();
		for (SurfaceElement se: surfs) {
			if (new IndefinitePronounOps().recognize(se)) {
				Expression exp = csif.newExpression(doc,type, se.getSpan(),se.getHead().getSpan(),se.getText());
				log.log(Level.FINE,"Indefinite pronominal mention: {0}.", new Object[]{exp.toShortString()});
			}
		}
	}
	
	/** 
	 * Annotates a <code>Sentence</code> object with definite NPs (e.g., <i>the agent</i>).  
	 * Assumes that the <code>Sentence</code> object has already been pre-processed for syntax. 
	 * It won't annotate a textual unit if it is already associated with coreferential mentions.
	 * 
	 * @param sent  the sentence to annotate
	 */
	public static synchronized void annotateDefiniteNPs(Sentence sent) {
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		Document doc = sent.getDocument();
		CoreferenceSemanticItemFactory csif = getCoreferenceSemanticItemFactory(doc);
		if (csif == null) return;
		String type = ExpressionType.DefiniteNP.toString();
		for (SurfaceElement se: surfs) {
			if (Expression.filterByExpressions(se).size() > 0) continue;
			if (new DefiniteNPOps().recognize(se)) {
				SpanList sp = DefiniteNPOps.getDefiniteNPSpan(se);
				SurfaceElement sortalNP = doc.getSurfaceElementFactory().createSurfaceElementIfNecessary(doc, sp, false);
				if (sortalNP == null) continue;
				Expression exp = csif.newExpression(doc,type, sortalNP);
				log.log(Level.FINE,"Definite NP mention: {0}.", new Object[]{exp.toShortString()});
				sent.synchSurfaceElements(sortalNP);
			}
		}
		sent.synchEmbeddings();
	}
	
	/** 
	 * Annotates a <code>Sentence</code> object with indefinite NPs (e.g., <i>a disease</i>).  
	 * Assumes that the <code>Sentence</code> object has already been pre-processed for syntax. 
	 * It won't annotate a textual unit if it is already associated with coreferential mentions.
	 * 
	 * @param sent  the sentence to annotate
	 */
	public static synchronized void annotateIndefiniteNPs(Sentence sent) {
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		Document doc = sent.getDocument();
		CoreferenceSemanticItemFactory csif = getCoreferenceSemanticItemFactory(doc);
		if (csif == null) return;
		String type = ExpressionType.IndefiniteNP.toString();
		for (SurfaceElement se: surfs) {
			if (Expression.filterByExpressions(se).size() > 0) continue;
			if (new IndefiniteNPOps().recognize(se)) {
				SpanList sp = IndefiniteNPOps.getIndefiniteNPSpan(se);
				SurfaceElement sortalNP = doc.getSurfaceElementFactory().createSurfaceElementIfNecessary(doc, sp, false);
				if (sortalNP == null) continue;
				Expression exp = csif.newExpression(doc, type, sortalNP);
				log.log(Level.FINE,"Indefinite NP mention: {0}.", new Object[]{exp.toShortString()});
				sent.synchSurfaceElements(sortalNP);
			}
		}
		sent.synchEmbeddings();
	}
	
	/** 
	 * Annotates a <code>Sentence</code> object with demonstrative NPs (e.g., <i>these medications</i>).  
	 * Assumes that the <code>Sentence</code> object has already been pre-processed for syntax. 
	 * It won't annotate a textual unit if it is already associated with coreferential mentions.
	 * 
	 * @param sent  the sentence to annotate
	 */
	public static synchronized void annotateDemonstrativeNPs(Sentence sent) {
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		Document doc = sent.getDocument();
		CoreferenceSemanticItemFactory csif = getCoreferenceSemanticItemFactory(doc);
		if (csif == null) return;
		String type = ExpressionType.DemonstrativeNP.toString();
		for (SurfaceElement se: surfs) {
			if (Expression.filterByExpressions(se).size() > 0) continue;
			if (new DemonstrativeNPOps().recognize(se)) {
				SpanList sp = DemonstrativeNPOps.getDemonstrativeNPSpan(se);
				SurfaceElement sortalNP = doc.getSurfaceElementFactory().createSurfaceElementIfNecessary(doc, sp, false);
				if (sortalNP == null) continue;
				Expression exp = csif.newExpression(doc, type, sortalNP);
				log.log(Level.FINE,"Demonstrative NP mention: {0}.", new Object[]{exp.toShortString()});
				sent.synchSurfaceElements(sortalNP);
			}
		}
		sent.synchEmbeddings();
	}
	
	/** 
	 * Annotates a <code>Sentence</code> object with distributive NPs (e.g., <i>each patient</i>).  
	 * Assumes that the <code>Sentence</code> object has already been pre-processed for syntax. 
	 * It won't annotate a textual unit if it is already associated with coreferential mentions.
	 * 
	 * @param sent  the sentence to annotate
	 */
	public static synchronized void annotateDistributiveNPs(Sentence sent) {
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		Document doc = sent.getDocument();
		CoreferenceSemanticItemFactory csif = getCoreferenceSemanticItemFactory(doc);
		if (csif == null) return;
		String type = ExpressionType.DistributiveNP.toString();
		for (SurfaceElement se: surfs) {
			if (Expression.filterByExpressions(se).size() > 0) continue;
			if (new DistributiveNPOps().recognize(se)) {
				SpanList sp = DistributiveNPOps.getDistributiveNPSpan(se);
				SurfaceElement sortalNP = doc.getSurfaceElementFactory().createSurfaceElementIfNecessary(doc, sp, false);
				if (sortalNP == null) continue;
				Expression exp = csif.newExpression(doc, type, sortalNP);
				log.log(Level.FINE,"Distributive NP mention: {0}.", new Object[]{exp.toShortString()});
				sent.synchSurfaceElements(sortalNP);
			}
		}
		sent.synchEmbeddings();
	}
	
	/** 
	 * Annotates a <code>Sentence</code> object with bare (zero-article) NPs (e.g., <i>drug</i>).  
	 * Assumes that the <code>Sentence</code> object has already been pre-processed for syntax. 
	 * It won't annotate a textual unit if it is already associated with coreferential mentions.
	 * 
	 * @param sent  the sentence to annotate
	 */
	public static synchronized void annotateZeroArticleNPs(Sentence sent) {
		List<SurfaceElement> surfs = sent.getSurfaceElements();
		Document doc = sent.getDocument();
		CoreferenceSemanticItemFactory csif = getCoreferenceSemanticItemFactory(doc);
		if (csif == null) return;
		String type = ExpressionType.ZeroArticleNP.toString();
		for (SurfaceElement se: surfs) {
			if (Expression.filterByExpressions(se).size() > 0) continue;
			if (new ZeroArticleNPOps().recognize(se)) {
				Expression exp = csif.newExpression(doc, type, se);
				log.log(Level.FINE,"Zero-article NP mention: {0}.", new Object[]{exp.toShortString()});
				sent.synchSurfaceElements(se);
			}
		}
		sent.synchEmbeddings();
	}	
	
	/**
	 * Gets a semantic item factory associated with a <code>Document</code> object, capable of generating
	 * {@link Expression} objects.
	 * 
	 * @param doc	the document for the semantic item factory
	 * @return		a semantic item factory, capable of generating coreference objects, or null if no such factory is
	 * 				associated with the document
	 */
	public static CoreferenceSemanticItemFactory getCoreferenceSemanticItemFactory(Document doc) {
		SemanticItemFactory sif = doc.getSemanticItemFactory();
		if (sif instanceof CoreferenceSemanticItemFactory == false ) {
			log.log(Level.SEVERE,"SemanticItemFactory {0} cannot generate Expression objects, skipping...", new Object[]{sif.getClass().getName()});
			return null;
		} 
		return ((CoreferenceSemanticItemFactory)sif);
	}
	
	/**
	 * Determines the mention type of a textual unit. 
	 * This is useful when using gold standard mentions, which typically won't 
	 * have the fine-grained mention typing assumed in this framework.
	 * 
	 * @param surf	the textual unit 
	 * @return	the mention type recognized, or {@code ExpressionType#ZeroArticleNP} if no type is recognized
	 */
	public static ExpressionType getMentionType(SurfaceElement surf) {
		ExpressionType type = null;
		Word first = surf.toWordList().get(0);
		if (new PersonalPronounOps().recognize(surf)) type = ExpressionType.PersonalPronoun;
		else if (new PossessivePronounOps().recognize(surf)) type = ExpressionType.PossessivePronoun;
		else if (new RelativePronounOps().recognize(surf)) type = ExpressionType.RelativePronoun;
		else if (new DemonstrativePronounOps().recognize(surf)) type = ExpressionType.DemonstrativePronoun;
		else if (new DistributivePronounOps().recognize(surf)) type = ExpressionType.DistributivePronoun;
		else if (new ReciprocalPronounOps().recognize(surf)) type = ExpressionType.ReciprocalPronoun;
		else if (new IndefinitePronounOps().recognize(surf)) type = ExpressionType.IndefinitePronoun;
		else if (new DefiniteNPOps().recognize(surf)) type = ExpressionType.DefiniteNP;
		else if (new IndefiniteNPOps().recognize(surf)) type = ExpressionType.IndefiniteNP;
		else if (new DemonstrativeNPOps().recognize(surf)) type = ExpressionType.DemonstrativeNP;
		else if (new DistributiveNPOps().recognize(surf)) type = ExpressionType.DistributiveNP;
		else if (first.containsAnyLemma(DefiniteNPOps.DEFINITE_DETERMINERS)) type = ExpressionType.DefiniteNP;
		else if (first.containsAnyLemma(DemonstrativeNPOps.DEMONSTRATIVE_ADJECTIVES))  type = ExpressionType.DemonstrativeNP;
		else if (first.containsAnyLemma(DemonstrativePronounOps.DEMONSTRATIVE_PRONOUNS))  {
			if (surf.toWordList().size() > 1 && surf.isNominal()) type = ExpressionType.DemonstrativeNP;
			else type = ExpressionType.DemonstrativePronoun;
		}
		else if (first.containsAnyLemma(DistributivePronounOps.DISTRIBUTIVE_PRONOUNS))  {
			if (surf.toWordList().size() > 1 && surf.isNominal()) type = ExpressionType.DistributiveNP;
			else type = ExpressionType.DistributivePronoun;
		}
		else if (first.containsAnyLemma(IndefiniteNPOps.INDEFINITE_ADJECTIVES))  type = ExpressionType.DemonstrativeNP;
		else if (first.containsAnyLemma(IndefinitePronounOps.INDEFINITE_PRONOUNS))  {
			if (surf.toWordList().size() > 1 && surf.isNominal()) type = ExpressionType.IndefiniteNP;
			else type = ExpressionType.IndefinitePronoun;
		}
		else if (first.containsAnyLemma(PersonalPronounOps.PERSONAL_PRONOUNS)) type = ExpressionType.PersonalPronoun;
		else if (first.containsAnyLemma(PossessivePronounOps.POSSESSIVE_PRONOUNS)) type = ExpressionType.PossessivePronoun;
		else if (first.containsAnyLemma(RelativePronounOps.WH_RELATIVE_PRONOUNS)) type = ExpressionType.RelativePronoun;
		else if (surf.containsLemma("that")) {
			if (surf.isNominal()) type = ExpressionType.DemonstrativeNP;
			else if (surf.toWordList().get(surf.toWordList().size()-1).containsLemma("that")) type = ExpressionType.DemonstrativePronoun;
			else type = ExpressionType.RelativePronoun;
		} else {
			for (String rec: ReciprocalPronounOps.RECIPROCAL_PRONOUNS) {
				if (surf.getText().toLowerCase().contains(rec)) {
					type = ExpressionType.ReciprocalPronoun;
					break;
				}
			}
		}
//		else if (new ZeroArticleNPOps().recognize(surf)) type = ExpressionType.ZeroArticleNP;
		if (type == null) {
//			log.log(Level.WARNING, "Unable to determine mention type: {0}. Will assign ZeroArticleNP.", new Object[]{surf.toString()});
			type = ExpressionType.ZeroArticleNP;
		}
		return type;
	}

}
