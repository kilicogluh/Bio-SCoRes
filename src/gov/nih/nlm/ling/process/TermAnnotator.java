package gov.nih.nlm.ling.process;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.sem.Ontology;

/**
 * An interface for term annotator modules. Each term annotator is provided with a
 * <code>Document</code> object, a <code>Properties</code> where term annotator properties
 * can be read from. The results are returned as a map of <code>SpanList</code>/<code>Term</code>
 * key/value pairs.
 * 
 * @author Halil Kilicoglu
 *
 */
public interface TermAnnotator {
	
	/**
	 * Annotates a given document with terms. 
	 * 
	 * @param document  the document to annotate
	 * @param props  the properties to read annotation parameters from, if any
	 * @param annotations  the resulting annotations with their span as they key
	 */
	public void annotate(Document document, Properties props, Map<SpanList,LinkedHashSet<Ontology>> annotations);
}
