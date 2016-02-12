package tasks.coref.i2b2;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.core.CoreferenceChain;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.HasPredicate;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;

/**
 * A writer that contains methods to convert coreference resolution pipeline results to
 * the format that is expected for the i2b2/VA corpus.
 * 
 * @author Halil Kilicoglu
 *
 */
public class I2B2CorefWriter {
	private static Logger log = Logger.getLogger(I2B2CorefWriter.class.getName());	
	
	/**
	 * Writes the markables (entities and coreferential mentions) in a document to an output file.
	 * 
	 * @param doc			the document with the markables
	 * @param outFile		the markable output file
	 * @throws IOException	if there is a problem with writing to <var>outFile</var>
	 */
	public static void writeMarkables(Document doc, String outFile) throws IOException {
		LinkedHashSet<SemanticItem> entitySet = Document.getSemanticItemsByClass(doc, Entity.class);
		LinkedHashSet<SemanticItem> anaSet = Document.getSemanticItemsByClass(doc, Expression.class);
		Set<SemanticItem> itemSet = new HashSet<>();
		if (entitySet != null) itemSet.addAll(entitySet);
		if (anaSet != null) itemSet.addAll(anaSet);
		PrintWriter pw;
		if (outFile == null)  
			pw = new PrintWriter(System.out);
		else pw = new PrintWriter(outFile);
		if (itemSet != null) {
			List<SemanticItem> items = new ArrayList<>(itemSet);
			for (SemanticItem si: items) {
				String mark = getTermString(si);
				String type = si.getType();
				try {
					ExpressionType.valueOf(type);
				} catch (IllegalArgumentException iae) {
					pw.println(mark + "||t=\"" + type + "\"");
				}
			}
		}
		pw.flush(); pw.close();
	}
	
	/**
	 * Writes the coreference chains in a document to an output file.
	 * 
	 * @param doc			the document with the coreference chains
	 * @param outFile		the output file
	 * @throws IOException	if there is a problem with writing to <var>outFile</var>
	 */
	public static void writeCoref(Document doc, String outFile) throws IOException {
		Set<SemanticItem> itemSet = Document.getSemanticItemsByClass(doc, CoreferenceChain.class);
		if (itemSet == null || itemSet.size() == 0) {
			PrintWriter pw;
			pw = new PrintWriter(outFile);
			pw.flush(); pw.close();
			return;
		}
		List<SemanticItem> items = new ArrayList<>(itemSet);
//		Collections.sort(items,SemanticItem.SPAN_ORDER);
		Collections.sort(items);
		writeAsChain(items,outFile);
	}
	
	/**
	 * Writes a list of coreference chains to an output file.
	 * 
	 * @param chains		the list of coreference chains
	 * @param outFile		the output file
	 * @throws IOException	if there is a problem with writing to <var>outFile</var>
	 */
	public static void writeAsChain(List<SemanticItem> chains, String outFile) throws IOException {
		List<String> seenIds = new ArrayList<>();
		PrintWriter pw;
		if (outFile == null)  
			pw = new PrintWriter(System.out);
		else pw = new PrintWriter(outFile);
		for (SemanticItem si: chains) {
			if (seenIds.contains(si.getId())) continue; 
			if (si instanceof CoreferenceChain == false) continue;
			CoreferenceChain cc = (CoreferenceChain)si;
			String chainStr = asI2B2Chain(cc);
			if (chainStr.equals("")) continue;
			pw.println(chainStr);
			seenIds.add(si.getId());
		}
		pw.flush();
		pw.close();
	}
	
	/**
	 * Gets the string representation of a coreference chain formatted 
	 * according to i2b2 format. <p>
	 *  
	 * 
	 * @param chain	the coreference chain
	 * @return	the i2b2 string representation of the chain, or an empty string if the chain is malformed
	 */
	public static String asI2B2Chain(CoreferenceChain chain) {
		StringBuffer buf = new StringBuffer();
		List<Argument> arguments = chain.getArguments();
		if (arguments == null) {
			log.log(Level.WARNING,"Malformed coreference: {0}.",chain.toString());
			return buf.toString();
		}
		Collections.sort(arguments, new Comparator<Argument>() {
				public int compare(Argument a,Argument b) {
					SemanticItem as = a.getArg();
					SemanticItem bs= b.getArg();
					return as.compareTo(bs);
				}});
		
		String type = "";
		for (Argument arg: arguments) {
			SemanticItem sem = arg.getArg();
			String argstr = getTermString(sem);
			if (argstr.equals("")) return "";
			buf.append(argstr + "||");
			if (type.equals("")) {
				try {
					ExpressionType.valueOf(sem.getType());
				} catch (IllegalArgumentException iae) {
					type = sem.getType();
				}
			}
		}
		if (type.equals("")) {
			type = getChainType(chain);
		}
		if (type.equals("pronoun")) return "";
		buf.append("t=\"coref " + type + "\"");
		return buf.toString();
	}
	
	/**
	 * Gets the i2b2 chain type to assign to a semantic object. <p>
	 * If the semantic object is an entity, it will return the type of that entity.
	 * If the semantic object is a coreference chain, it will return the type of
	 * entities involved in this chain. 
	 * 
	 * @param sem	the semantic object
	 * @return	the chain type to assign to this semantic object
	 */
	public static String getChainType(SemanticItem sem) {
		if (sem instanceof Entity) return sem.getType();
		CoreferenceChain cc = (CoreferenceChain)sem;
		Document doc = cc.getDocument();
		for (SemanticItem si : cc.getArgItems()) {
			LinkedHashSet<SemanticItem> others = 
					Document.getSemanticItemsByClassTypeSpan(doc, Entity.class, Constants.ENTITY_TYPES, si.getSpan(),false);
			for (SemanticItem oth: others) {
				if (oth.getSpan().equals(si.getSpan())) 
					return oth.getType();
			}
		}
		return null;
	}


	/**
	 * Gets the i2b2 string representation for an individual term. <p>
	 * If the input semantic object is a relation with a predicate, 
	 * the predicate is taken into account.
	 * 
	 * i2b2 format uses sentence and word indexes to represent terms.
	 * The term string has the format 
	 * <pre>{@code c="<term_string>" <sentence_index>:<first_word_index> 
	 * <sentence_index>:<last_word_index>}</pre>
	 * 
	 * @param sem	the term
	 * @return	the string representation of the term
	 */
	public static String getTermString(SemanticItem sem) {
		Document doc = sem.getDocument();
		Term ts = null;
		if (sem instanceof Term) {
			ts = (Term)sem;
		}
		else if (sem instanceof HasPredicate) {
			ts = ((HasPredicate)sem).getPredicate();
		}
		if (ts == null) return "";
		SurfaceElement st = ts.getSurfaceElement();
		Sentence sent = st.getSentence();
		List<Word> stW = sent.getWordsInSpan(ts.getSpan());
		int sentId = doc.getSentences().indexOf(sent) +1;
		Word firstWt = stW.get(0);
		Word lastWt = stW.get(stW.size()-1);
		int firstIndex  = sent.getWords().indexOf(firstWt);
		int lastIndex = sent.getWords().indexOf(lastWt);
		return "c=\"" + doc.getStringInSpan(new SpanList(firstWt.getSpan().getBegin(),lastWt.getSpan().getEnd())).toLowerCase() + "\" " + sentId +  ":" +  firstIndex + " "  + 
				   sentId +  ":" +  lastIndex;
	}
}
