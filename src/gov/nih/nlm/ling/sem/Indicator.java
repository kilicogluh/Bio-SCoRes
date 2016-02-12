package gov.nih.nlm.ling.sem;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import gov.nih.nlm.ling.composition.EmbeddingCategorization;
import gov.nih.nlm.ling.core.ContiguousLexeme;
import gov.nih.nlm.ling.core.GappedLexeme;
import gov.nih.nlm.ling.core.Lexeme;
import gov.nih.nlm.ling.core.MultiWordLexeme;
import gov.nih.nlm.ling.core.WordLexeme;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import nu.xom.ValidityException;


/**
 * Represents an abstract, non-textually grounded counterpart to a predicate. An indicator is defined
 * by its lemma (e.g., <i>stimulate</i>, <i>on the other hand</i>) and a list of <code>Sense</code>s 
 * it encodes (e.g., STIMULATES, AUGMENTS, for <i>stimulate</i>). 
 * 
 * @author Halil Kilicoglu
 *
 */
public class Indicator implements Ontology,Comparable<Indicator> {
	private static Logger log = Logger.getLogger(Indicator.class.getName());	

	/**
	 * Order a set of indicators based on their length, so that larger spans are preferred.
	 */
	public static final Comparator<Indicator> LENGTH_ORDER = 
	        new Comparator<Indicator>() {
			public int compare(Indicator e1,Indicator e2) {
				int e1Size = e1.getLexeme().toLexemeList().size();
				int e2Size = e2.getLexeme().toLexemeList().size();
				if (e2Size > e1Size) return 1;
				if (e1Size > e2Size) return -1;
				if ((e1.isGapped() && e2.isGapped()) || 
						(e1.isMultiWord() && e2.isMultiWord()) ||
						(e1.isWord() && e2.isWord()))  {
						return e1.getString().toLowerCase().compareTo(e2.getString().toLowerCase());
					}
				if (e2.isGapped()) return 1;
				if (e2.isMultiWord() && e1.isWord()) return 1;
				if (e2.isMultiWord()) return -1;
				return -1;
			}	
		};
			
	private String string;
	private Lexeme lexeme;
	private List<Sense> senses;
	private boolean verified;
	
	private enum GapType { 
		NONE, SENTENCE, DISCOURSE;
		public String toString() {
			switch (this) {
				case DISCOURSE: return "discourse";
				case SENTENCE: return "sentence";
				default: return "none";
			}
		}
		
		public static GapType fromString(String s) {
			if (s == null) return NONE;
			if (s.equalsIgnoreCase("discourse")) return DISCOURSE;
			if (s.equalsIgnoreCase("sentence")) return SENTENCE;
			return NONE;
		}
	}
	private GapType gapType;
	
	/**
	 * Instantiates an <code>Indicator</code> object with its senses.
	 * 
	 * @param string	the lemma text
	 * @param lexemes	the list of lexemes forming the indicator
	 * @param verified	whether this is a verified indicator (internal use)
	 * @param senses	the list of senses 
	 */
	public Indicator(String string, List<ContiguousLexeme> lexemes, boolean verified, List<Sense> senses) {
		if (lexemes.size() > 1) 
			this.lexeme = new GappedLexeme(lexemes);
		else this.lexeme = lexemes.get(0);
		this.string = string;
		this.verified = verified;
		this.senses = senses;
	}
	
	/**
	 * Instantiates an <code>Indicator</code> object with its gap type (NONE, SENTENCE, DISCOURSE).
	 * Gap type is relevant when the indicator is a multi-word unit. NONE indicates that no gaps are allowed. 
	 * SENTENCE indicates that intra-sentence gaps are allowed, DISCOURSE indicates that inter-sentence
	 * gaps are allowed. 
	 * 
	 * @param string	the lemma text
	 * @param lexemes	the list of lexemes forming the indicator
	 * @param verified	whether this is a verified indicator (internal use)
	 * @param senses	the list of senses 
	 * @param gt		the gap type ({@code GapType.NONE}, {@code GapType.SENTENCE}, or {@code GapType.DISCOURSE})
	 */
	public Indicator(String string, List<ContiguousLexeme> lexemes, boolean verified, List<Sense> senses, GapType gt) {
		this(string,lexemes,verified, senses);
		this.gapType = gt;
	}
	
	/**
	 * Instantiates an <code>Indicator</code> object from an XML element.
	 * 
	 * @param el  the XML element representing the indicator
	 * 
	 * @throws IllegalArgumentException	if the element does not conform to indicator XML
	 */
	public Indicator(Element el) {
		if (el.getChildElements("GappedLexeme").size() > 0) {
			this.lexeme = new GappedLexeme(el.getFirstChildElement("GappedLexeme"));
		} else if (el.getChildElements("Lexeme").size() >0) {
			Elements lex= el.getChildElements("Lexeme");
			if (lex.size() > 1) this.lexeme = new MultiWordLexeme(el); 
			else this.lexeme = new WordLexeme(lex.get(0));
		} else
			throw new IllegalArgumentException("No Lexeme or GappedLexeme child, cannot create an Indicator: " + el.toXML());
		this.string =  el.getAttributeValue("string");
		this.verified = Boolean.parseBoolean(el.getAttributeValue("verified"));
		this.gapType = GapType.fromString(el.getAttributeValue("gapType"));
		Elements semEls = el.getChildElements("SemInfo");
		if (semEls == null) 
			throw new IllegalArgumentException("No senses available for the Indicator: " + el.toXML());
		List<Sense> sems = new ArrayList<Sense>(semEls.size());
		for (int i=0; i <semEls.size(); i++) {
			Element sEl = semEls.get(i);
			Sense si = new Sense(sEl);
			sems.add(si);
			
		}
		this.senses = sems;
	} 
	
		
	public boolean isVerified() {
		return verified;
	}
	
	public Lexeme getLexeme() {
		return lexeme;
	}

	public GapType getGapType() {
		return gapType;
	}

	public List<Sense> getSenses() {
		return senses;
	}

	public void setSenses(List<Sense> senses) {
		this.senses = senses;
	}

	public String getString() {
		return string;
	}

	/** 
	 * 
	 * @return	true if this is a single-token indicator
	 */
	public boolean isWord() {
		return (lexeme instanceof WordLexeme);
	}
	
	/**
	 * 
	 * @return true  if this consists of multiple words
	 */
	public boolean isMultiWord() {
		return (lexeme instanceof MultiWordLexeme || 
				lexeme instanceof GappedLexeme);
	}
	
	/**
	 * 
	 * @return true  if this indicator has gaps (eg, <i>On the one hand</i>...<i>on the other hand</i> is gapped)
	 */
	public boolean isGapped() {
		return (lexeme instanceof GappedLexeme);
	}
	
	/**
	 * 
	 * @return true  if this indicator not gapped
	 */
	public boolean isContiguous() {
		return !(isGapped());
	}
	
	public String toString(){
		StringBuffer buf = new StringBuffer();
		buf.append(string + " (" + lexeme.toString() + ")");
		for (Sense sem: senses) {
			buf.append('_'); buf.append(sem.toString());
		}
		return buf.toString();
	}
		
	/**
	 * 
	 * @return  the most likely sense of this indicator based on probability values, of the first sense if no probabilities
	 */
	public Sense getMostProbableSense() {
		double prob = -1.0;
		Sense isi = senses.get(0);
		for (Sense is: senses) {
			if (is.getProbability() > prob) {
				prob = is.getProbability();
				isi = is;
			}
		}
		return isi;
	}
	
	public Element toXml() {
		Element el = new Element("Indicator");
		el.addAttribute(new Attribute("string",string));
		el.addAttribute(new Attribute("gapType",gapType.toString()));
		el.addAttribute(new Attribute("verified",String.valueOf(false)));
		if (isGapped()) {
			el.appendChild(((GappedLexeme)lexeme).toXml());
		} else if (isMultiWord()) {
			el.appendChild(((MultiWordLexeme)lexeme).toXml());
		} else {
			el.appendChild(((WordLexeme)lexeme).toXml());
		} 
		for (Sense isi: senses) {
			el.appendChild(isi.toXml());
		}
		return el;
	}
	
	/**
	 * 
	 * @return true if one of the senses corresponds to an embedding category.
	 */
	public boolean mayEmbed() {
		for (Sense isi: senses) {
			if (isi.isEmbedding()) return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @return true if one of the senses corresponds to a discourse relation category.
	 */
	public boolean mayConnectDiscourse() {
		for (Sense isi: senses) {
			if (isi.isDiscourseConnective()) return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @return true if the indicator does not have an embedding sense associated with it.
	 */
	public boolean isAtomicOnly() {
		for (Sense isi: senses) {
			if (isi.isDiscourseConnective() || isi.isEmbedding()) return false;
		}
		return true;
	}
	
	/**
	 * To sort in the order of complexity, from less to more complex. 
	 * An atomic indicator precedes an embedding indicator.
	 */
	public int compareTo(Indicator et1) {
		if (this.isAtomicOnly()) {
			return -1;
		} else if (this.mayConnectDiscourse()) {
			if (et1.mayConnectDiscourse()) return -1;
			return 1;			
		} else if (this.mayEmbed()) {
			if (et1.isAtomicOnly()) return 1;
			return -1;			
		}
		return -1;
	}
	
	/**
	 * Loads indicators from an XML file. The indicators can be filtered by corpus frequency,
	 * if specified in the XML file. 
	 * 
	 * @param fileName  the indicator XML file
	 * @param count  	the corpus frequency threshold
	 * @return 			the set of indicators in the XML file
	 * 
	 * @throws FileNotFoundException	if the XML file cannot be found
	 * @throws IOException				if the XML file cannot be read
	 * @throws ParsingException			if the XML cannot be parsed
	 * @throws ValidityException		if the XML is invalid
	 */
	public static LinkedHashSet<Indicator> loadIndicatorsFromFile(String fileName, int count) 
			throws FileNotFoundException, IOException, ParsingException, ValidityException {
		LinkedHashSet<Indicator> indicators = new LinkedHashSet<Indicator>();
		Builder builder = new Builder();
		log.info("Loading indicator file " + fileName);
		nu.xom.Document xmlDoc = builder.build(new FileInputStream(fileName));
		Element docc = xmlDoc.getRootElement();
		Elements indEls = docc.getChildElements("Indicator");
		for (int i=0; i < indEls.size(); i++) {
			Element ind = indEls.get(i);
			if (ind.getAttribute("corpusCount") != null) {
				int corpusCount = Integer.parseInt(ind.getAttributeValue("corpusCount"));
				if (corpusCount < count) continue;
			}
			Indicator indicator = new Indicator(ind);
			indicators.add(indicator);
		}
		log.info("The number of all indicators loaded from the file " + fileName + " is " + indicators.size() + ".");
		return indicators;		
	}
	
	/**
	 * Selects verified indicators from a list of indicators.
	 * Verified indicators are those that have gone through some kind of quality assurance.
	 * 
	 * @param inIndicators	the input list of indicators
	 * @return  			the verified indicators
	 */
	public static LinkedHashSet<Indicator> filterVerified(LinkedHashSet<Indicator> inIndicators) {
		LinkedHashSet<Indicator> outIndicators = new LinkedHashSet<Indicator>();
		for (Indicator ind: inIndicators) {
			if (ind.isVerified()) outIndicators.add(ind);
		}
		log.info("The number of verified indicators is " + outIndicators.size() + ".");
		return outIndicators;
		
	}
	
	/**
	 * Removes multi-word indicators from a list of indicators.
	 * 
	 * @param inIndicators	the input list of indicators
	 * @return  			the single word indicators
	 */
	public static LinkedHashSet<Indicator> filterOutMultiWordIndicators(Set<Indicator> inIndicators) {
		LinkedHashSet<Indicator> outIndicators = new LinkedHashSet<Indicator>();
		for (Indicator ind: inIndicators) {
//			if (ind.isMultiWord() == false && ind.isGapped() == false) 
			if (ind.isWord())
				outIndicators.add(ind);
		}
		return outIndicators;
	}	

	
	/**
	 * Removes gapped indicators from a list of indicators.
	 * 
	 * @param inIndicators	the input list of indicators
	 * @return				the single word and contiguous multi-word indicators
	 */
	public static LinkedHashSet<Indicator> filterOutGappedIndicators(LinkedHashSet<Indicator> inIndicators) {
		LinkedHashSet<Indicator> outIndicators = new LinkedHashSet<Indicator>();
		for (Indicator ind: inIndicators) {
			if (ind.isGapped() == false) outIndicators.add(ind);
		}
		return outIndicators;
	}
		
	/**
	 * Filters out indicators that do not belong to one of the specified embedding types.
	 * 
	 * @param inIndicators		the input set of indicators
	 * @param embeddingTypes	the embedding types desired
	 * @return  				the indicators belonging to one of the <var>embeddingTypes</var>
	 */
	public static LinkedHashSet<Indicator> filterIndicatorsHierarchically(LinkedHashSet<Indicator> inIndicators, 
			Collection<String> embeddingTypes) {
		LinkedHashSet<String> allTypes = new LinkedHashSet<String>();
		for (String s: embeddingTypes) {
			Set<String> subTypes = EmbeddingCategorization.getAllDescendants(s);
			if (subTypes.size() == 0) allTypes.add(s);
			else allTypes.addAll(subTypes);
		}
		LinkedHashSet<Indicator> outIndicators = new LinkedHashSet<>();
		for (Indicator ind: inIndicators) {
			Indicator outInd = ind;
			List<Sense> outSemInfo = new ArrayList<>();
			for (Sense sem: ind.getSenses()) {
				if (allTypes.contains(sem.getCategory())) 
					outSemInfo.add(sem);
			} 
			if (outSemInfo.size() > 0) {
				outInd.setSenses(outSemInfo);
				outIndicators.add(outInd);
			} else 
				log.finest("Filtered indicator hierarchically: " + outInd.toString());
		}
		log.fine("Number of indicators filtered hierarchically: " + outIndicators.size() + ".");
		return outIndicators;
	}
	
	/**
	 * Filters out indicators with probability under a given threshold.<p>
	 * If no probability is given for an indicator (0.0), it is included in any case. 
	 * <var>thresholds</var> array keeps the thresholds for atomic, embedding, and discourse indicators, respectively.
	 * 
	 * 
	 * @param inIndicators		the input set of indicators
	 * @param thresholds		the threshold values for atomic, embedding, and discourse indicators
	 * @return  				the indicators with a higher probability then their respective threshold
	 */
	public static LinkedHashSet<Indicator> filterIndicatorsByThreshold(LinkedHashSet<Indicator> inIndicators, double[] thresholds) {
		LinkedHashSet<Indicator> outIndicators = new LinkedHashSet<>();
		for (Indicator ind: inIndicators) {
			Indicator outInd = ind;
			List<Sense> outSemInfo = new ArrayList<>();
			for (Sense sem: ind.getSenses()) {
				if (sem.getProbability() == 0.0) {outSemInfo.add(sem); continue;}
				if (sem.isAtomic() && sem.getProbability() >= thresholds[0]) outSemInfo.add(sem);
				else if (sem.isEmbedding() && sem.getProbability() >= thresholds[1]) outSemInfo.add(sem);
				else if (sem.isDiscourseConnective() && sem.getProbability() >= thresholds[2]) outSemInfo.add(sem);
			}
			if (outSemInfo.size() > 0) {
				outInd.setSenses(outSemInfo);
				outIndicators.add(outInd);
			}
		}
		log.fine("Number of indicators filtered by threshold: " + outIndicators.size() + ".");
		return outIndicators;
	}
	
	/**
	 * Filters indicators with given types in their source they are extracted from.<p>
	 *  
	 * @param inIndicators	the input set of indicators
	 * @param types			the source types desired
	 * @return  			the indicators with the source types
	 */
	public static LinkedHashSet<Indicator> filterBySourceType(Set<Indicator> inIndicators, Collection<String> types) {
		LinkedHashSet<Indicator> outIndicators = new LinkedHashSet<>();
		for (Indicator ind: inIndicators) {
			Indicator outInd = ind;
			List<Sense> outSemInfo = new ArrayList<>();
			for (Sense sem: ind.getSenses()) {
				String sourceType = (String)sem.getFeature("sourceType");
				// CAUSE appears as source type for some unverified MODALs
//				if (types.contains(sem.getCategory()) || (types.contains(sourceType) && sourceType.equals("CAUSE") == false)) {
				if (types.contains(sem.getCategory()) || types.contains(sourceType)) {
					outSemInfo.add(sem);
				}
			} 
			if (outSemInfo.size() > 0) {
				outInd.setSenses(outSemInfo);
				outIndicators.add(outInd);
			} else 
				log.finest("Filtered indicator by source type: " + outInd.toString());
		}
		log.fine("Number of indicators filtered by source type: " + outIndicators.size() + ".");
		return outIndicators;
	}
		
	// ensure that discourse connectives do not take on other roles, remove those roles
	// also make sure that those labeled MODAL are not used, since they have not been examined yet.
/*	public static Set<Indicator> filterByType(Set<Indicator> inIndicators) {
		Set<Indicator> outIndicators = new TreeSet<Indicator>();
		for (Indicator ind: inIndicators) {
//			log.debug("Generalized In:" + ind.toString());
			Indicator outInd = ind;
			List<Sense> outSemInfo = ind.getSenses();
			if (ind.mayConnectDiscourse()) {
				outSemInfo = new ArrayList<Sense>();
				for (Sense sem: ind.getSenses()) {
					if (sem.isAtomic() || sem.isEmbedding()) continue;
					outSemInfo.add(sem);
				}
			} else {
				outSemInfo = new ArrayList<Sense>();
				for (Sense sem: ind.getSenses()) {
					if (sem.getCategory() != null && sem.getCategory().equals("MODAL")) continue;
					outSemInfo.add(sem);
				}
			}
			if (outSemInfo.size() > 0) {
				outInd.setSenses(outSemInfo);
				outIndicators.add(outInd);
			} else 
				log.debug("Removed by type " + outInd.toString());
		}
		log.debug("Filtered by type to " + outIndicators.size() + " indicators.");
		return outIndicators;
	} */
	
/*	public static Set<Indicator> filterAllEmbedding(Set<Indicator> inIndicators) {
		Set<Indicator> outIndicators = new TreeSet<Indicator>();
		for (Indicator ind: inIndicators) {
//			log.debug("Generalized In:" + ind.toString());
			Indicator outInd = ind;
			List<Sense> outSemInfo = ind.getSenses();
			if (ind.isAtomicOnly()) continue;
			outSemInfo = new ArrayList<Sense>();
			for (Sense sem: ind.getSenses()) {
//				if (sem.getHierType() != null && sem.getHierType().equals("MODAL")) continue;
				outSemInfo.add(sem);
			}
			if (outSemInfo.size() > 0) {
				outInd.setSenses(outSemInfo);
				outIndicators.add(outInd);
			} else 
				log.debug("Removed by type " + outInd.toString());
		}
		log.debug("Filtered embedding predicates to " + outIndicators.size() + " indicators.");
		return outIndicators;
	}*/
	
	// ensure that discourse connectives do not take on other roles, remove those roles
	// also make sure that those labeled MODAL are not used, since they have not been examined yet.
/*	public static Set<Indicator> filterEmbeddingNonConnective(Set<Indicator> inIndicators) {
		Set<Indicator> outIndicators = new TreeSet<Indicator>();
		for (Indicator ind: inIndicators) {
//			log.debug("Generalized In:" + ind.toString());
			Indicator outInd = ind;
			List<Sense> outSemInfo = ind.getSenses();
			if (ind.mayConnectDiscourse()) continue;
			if (ind.isAtomicOnly()) continue;
			outSemInfo = new ArrayList<Sense>();
			for (Sense sem: ind.getSenses()) {
//				if (sem.getHierType() != null && sem.getHierType().equals("MODAL")) continue;
				outSemInfo.add(sem);
			}
			if (outSemInfo.size() > 0) {
				outInd.setSenses(outSemInfo);
				outIndicators.add(outInd);
			} else 
				log.debug("Removed by type " + outInd.toString());
		}
		log.debug("Filtered embedding predicates to " + outIndicators.size() + " indicators.");
		return outIndicators;
	}*/
	
/*	public static Set<Indicator> filterDiscourseConnective(Set<Indicator> inIndicators) {
		Set<Indicator> outIndicators = new TreeSet<Indicator>();
		for (Indicator ind: inIndicators) {
//			log.debug("Generalized In:" + ind.toString());
			Indicator outInd = ind;
			List<Sense> outSemInfo = ind.getSenses();
			if (ind.isAtomicOnly()) continue;
			if (ind.mayConnectDiscourse()) {
				outSemInfo = new ArrayList<Sense>();
				for (Sense sem: ind.getSenses()) {
//					if (sem.getMajorType().equals("discourseConnective"))  
					if (sem.isDiscourseConnective())  
						outSemInfo.add(sem);
				}
			}
			if (outSemInfo.size() > 0) {
				outInd.setSenses(outSemInfo);
				outIndicators.add(outInd);
			} else 
				log.debug("Removed by type " + outInd.toString());
		}
		log.debug("Filtered by discourse type to " + outIndicators.size() + " indicators.");
		return outIndicators;
	}*/
	
/*	public static Set<Indicator> filterEmbeddingDiscourse(Set<Indicator> inIndicators) {
		Set<Indicator> embedding = filterEmbeddingNonConnective(inIndicators);
		Set<Indicator> disc = filterDiscourseConnective(inIndicators);
		embedding.addAll(disc);
		return embedding;
	}*/
	
}
