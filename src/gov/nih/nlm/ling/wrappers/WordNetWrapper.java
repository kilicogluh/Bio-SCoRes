package gov.nih.nlm.ling.wrappers;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.smu.tspell.wordnet.WordSense;
import edu.smu.tspell.wordnet.impl.file.RetrievalException;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * A wrapper for access to WordNet features.<p>
 * 
 * This class currently has very limited functionality.
 * 
 * 
 * @author Halil Kilicoglu
 *
 */
public class WordNetWrapper {
	private static Logger log = Logger.getLogger(WordNetWrapper.class.getName());	
	
	private static WordNetWrapper wordNet = null;
	private static WordNetDatabase database = null;
	
	private WordNetWrapper(Properties props)  {
		System.setProperty("wordnet.database.dir", 
				props.getProperty("wordNetDictionary","resources/dict"));
		database = WordNetDatabase.getFileInstance();
	}
	
	private WordNetWrapper() {
		database = WordNetDatabase.getFileInstance();
	}
	
	/**
	 * Instantiates the WordNetWrapper singleton. <p>
	 * It assumes the system property <i>wordnet.database.dir</i> has been set to
	 * wordNet dictionary directory.
	 * 
	 * @return  the WordNetWrapper singleton
	 */
	public static WordNetWrapper getInstance() {
		if (wordNet == null) {	
			log.info("Initializing a WordNetWrapper instance...");
			wordNet = new WordNetWrapper();
		}
	    return wordNet;
	}
	
	/**
	 * Instantiates the WordNetWrapper singleton. <p>
	 * It picks up the WordNet dictionary directory from the property <i>wordNetDictionary</i>.
	 * If this property is not set, it will try to use <i>resources/dict</i> directory. 
	 * 
	 * @param props	properties to read
	 * @return  the WordNetWrapper singleton
	 */
	public static WordNetWrapper getInstance(Properties props) {
		if (wordNet == null) {	
			log.info("Initializing a WordNetWrapper instance...");
			wordNet = new WordNetWrapper(props);
		}
	    return wordNet;
	}
	
	/**
	 * Checks whether CoreNLP is instantiated
	 * 
	 * @return  true if there is a CoreNLP instance
	 */
	public static boolean instantiated() {
		return (wordNet != null);
	}
	
	/**
	 * Finds the denominalized form of the head of a textual unit.
	 * It uses WordNet to get this information, assumes WordNet has already been initialized.
	 * 
	 * @param surf	the textual unit
	 * @return  the denominal form of the head, null if not a noun or does not have a nominal form or if the WordNet is not initialized.
	 */
	// TODO This works partially. It finds association -> associate, for example, but also proteins -> proteinaceous.
	// TODO Either use SPECIALIST Lexicon or perhaps more sophisticated WordNet features.
	public static String getDenominal(SurfaceElement surf) {
		if (surf.isNominal() == false) return null;
		String lemma = surf.getHead().getLemma();
		try {
			Synset[] synsets = database.getSynsets(lemma,SynsetType.NOUN);
			for (int i = 0; i < synsets.length; i++)
			{
				WordSense[] senses = synsets[i].getDerivationallyRelatedForms(lemma);
				for (WordSense sense : senses) {
					SynsetType  stype = sense.getSynset().getType();
					if ((stype == SynsetType.ADJECTIVE || stype == SynsetType.VERB) ) {
						log.log(Level.FINEST, "Denominal form of {0} is {1}.", new Object[]{surf.getText(),sense.getWordForm()});
						return sense.getWordForm();
					}
				}
			}
		} catch (NullPointerException ne) {
			log.log(Level.WARNING, ne.toString() + ": wordnet.database.dir system variable needs to be set for WordNet.");
		} catch (RetrievalException e) {
			log.log(Level.WARNING, e.toString() + ": wordnet.database.dir system variable needs to be set for WordNet.");
		}
		return null;
	}
}
