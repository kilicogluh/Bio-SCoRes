package gov.nih.nlm.ling.process;

import java.util.Properties;

/**
 * An interface for spelling correction.
 * 
 * @author Halil Kilicoglu
 *
 */
public interface SpellCorrection {
	
	/**
	 * Spell-corrects the given text
	 * 
	 * @param text	the text to correct
	 * @param props	the properties to read spelling correction parameters from
	 * @return  	the corrected text
	 */
	public String correct(String text, Properties props);
}
