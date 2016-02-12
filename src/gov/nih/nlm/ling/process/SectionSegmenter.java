package gov.nih.nlm.ling.process;

import gov.nih.nlm.ling.core.Document;

/**
 * An interface for section segmentation modules. 
 * 
 * @author Halil Kilicoglu
 *
 */
public interface SectionSegmenter {
	
	/**
	 * Segments the <code>Document</code> into sections (zones) and 
	 * updates its section information.
	 * 
	 * @param document	the document to segment into sections
	 */
	public void segment(Document document);
}
