package gov.nih.nlm.ling.brat;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.sem.AbstractRelation;
import gov.nih.nlm.ling.sem.AbstractTerm;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.HasPredicate;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.SemanticItem;


/**
 * A writer class to output brat-style standoff annotations from a <code>Document</code>. 
 * The semantic class of the objects to be written can be specified.
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO Modifications (Negation, Speculation, etc.) are not written at the moment.
public class StandoffAnnotationWriter {
	private static Logger log = Logger.getLogger(StandoffAnnotationWriter.class.getName());

	/**
	 * Writes the semantic objects of a <code>Document</code> that belong to
	 * particular categories. <p>
	 * If the <var>categories</var> is set to null,
	 * all semantic classes are written. If <var>fileName</var> is null,
	 * the output is written to standard output.
	 * 
	 * @param doc  			the document with the semantic objects
	 * @param categories  	the semantic classes to write
	 * @param outFile  	the name of the file to write to
	 * @throws IOException if there is a problem with writing to <var>outFile</var>
	 */
	public static void write(Document doc, List<Class<? extends SemanticItem>> categories, String outFile) 
			throws IOException {
		if (categories == null) write(doc, outFile);
		else {
			LinkedHashSet<SemanticItem> catItems = new LinkedHashSet<>();
			for (Class<? extends SemanticItem> cat: categories) {
				LinkedHashSet<SemanticItem> itemSet = 
						Document.getSemanticItemsByClass(doc, cat);
				if (itemSet == null) continue;
				catItems.addAll(itemSet);
			}
			write(new ArrayList<SemanticItem>(catItems), outFile);
		}
	}
	
	/**
	 * Writes all semantic objects of a <code>Document</code> to a file. <p>
	 * If <var>fileName</var> is null, the output is written to standard output. 
	 * 
	 * @param doc  		the document with the semantic objects
	 * @param outFile  the name of the file to write to
	 * @throws IOException if there is a problem with writing to <var>outFile</var>
	 */
	public static void write(Document doc, String outFile) throws IOException {
		Map<Class<? extends SemanticItem>,LinkedHashSet<SemanticItem>> items = doc.getSemanticItems();
		LinkedHashSet<SemanticItem> typeItems = new LinkedHashSet<>();
		for (Class<? extends SemanticItem> s: items.keySet())
			typeItems.addAll(items.get(s));
		write(new ArrayList<SemanticItem>(typeItems),outFile);
	}
	
	/**
	 * Writes the specified semantic objects to a file. <p>
	 * If <var>fileName</var> is null, the output is written to standard output.
	 * 
	 * @param semanticItems the semantic objects to write 
	 * @param outFile  	the name of the file to write to
	 * @throws IOException if there is a problem with writing to <var>outFile</var>
	 */
	public static void write(final Collection<SemanticItem> semanticItems, String outFile) throws IOException {
		List<String> seenIds = new ArrayList<>();
		if (semanticItems == null) {
			log.log(Level.WARNING,"No semantic items to write to {0}.", new Object[]{outFile});
			return;
		}
		PrintWriter pw;
		if (outFile == null)  
			pw = new PrintWriter(System.out);
		else pw = new PrintWriter(outFile);
		int refId = 0;
		List<SemanticItem> asList = new ArrayList<>(semanticItems);
		// TODO This screws up spans of some coreference chains for some reason, check later, final does not work.
//		Collections.sort(asList);
		for (SemanticItem si: asList) {
			if (seenIds.contains(si.getId())) continue; 
			if (si instanceof AbstractTerm) {
				if (!(seenIds.contains(si.getId()))) {
					if (si instanceof Entity) {
						String standoff = ((Entity) si).toStandoffAnnotation(true,refId);
						pw.println(standoff);
						refId += standoff.split("[\\n]+").length-1;
					} else {
						pw.println(si.toStandoffAnnotation());
					}
					seenIds.add(si.getId());
				}
			} else if (si instanceof AbstractRelation) {
				Predicate p = null;
				if (si instanceof HasPredicate) {
					p = ((HasPredicate)si).getPredicate();
				} 
				if (p != null && !(seenIds.contains(p.getId())))  {
					pw.println(p.toStandoffAnnotation());
					seenIds.add(p.getId());
				}	
				List<SemanticItem> args = ((AbstractRelation) si).getArgItems();
				for (SemanticItem arg: args) {
					if (!(seenIds.contains(arg.getId()))) {
						if (arg instanceof Entity && ((Entity)arg).getConcepts() != null) {
							String standoff = ((Entity) arg).toStandoffAnnotation(true,refId);
							pw.println(standoff);
							refId += standoff.split("[\\n]+").length-1;
						} else {
							pw.println(arg.toStandoffAnnotation());
						}
						seenIds.add(arg.getId());
					}
				}
				pw.println(si.toStandoffAnnotation());
				seenIds.add(si.getId());
			}
		}
		pw.flush();
		pw.close();
	}
	
	/**
	 * Writes standoff annotation texts to file.  <p>
	 * If <var>fileName</var> is null, the output is written to standard output.
	 * 
	 * @param lines  		the standoff annotation texts
	 * @param outFile  		the name of the file to write to
	 * @throws IOException	if there is a problem with writing to <var>outFile</var>
	 */
	public static void writeLines(final Collection<String> lines, String outFile) throws IOException {
		if (lines == null) {
			log.log(Level.WARNING,"No lines to write to {0}.", new Object[]{outFile});
			return;
		}
		PrintWriter pw;
		if (outFile == null)  
			pw = new PrintWriter(System.out);
		else pw = new PrintWriter(outFile);
		for (String l : lines)
			pw.println(l);
		pw.flush();
		pw.close();
	}	
	
}
