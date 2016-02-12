package gov.nih.nlm.bioscores.core;

import java.util.List;
import java.util.Map;

import gov.nih.nlm.ling.sem.DomainProperties;

/**
 * Defines simple public static lists or key-value pairs for domain knowledge that is used for coreference resolution.
 * They are loaded using <code>DomainProperties</code> parse methods. 
 * 
 * Currently, these lists include semantic type list, hypernym/hyponym/meronym lists, event trigger lists, as well as
 * collective noun, female/male noun lists. If some of these lists are not defined, methods that rely on them
 * may not work well. For example, if female/male noun lists are not defined, pronominal coreference machinery
 * may not give expected results. See <i>coref.properties</i> file for how these lists were defined for particular
 * coreference resolution pipelines.
 * 
 * @author Halil Kilicoglu
 *
 */
public class CoreferenceProperties {
	public static final Map<String,List<String>> SEMTYPES = DomainProperties.parse2("semtype");
	
	public static final Map<String,List<String>> HYPERNYMS = DomainProperties.parse2("hypernym");
	public static final Map<String,List<String>> HYPONYMS = DomainProperties.parse2("hyponym");
	public static final Map<String,List<String>> MERONYMS = DomainProperties.parse2("meronym");
	public static final Map<String,List<String>> EVENT_TRIGGERS = DomainProperties.parse2("eventTrigger");

	public static final List<String> COLLECTIVE_NOUNS = DomainProperties.parse1("collectiveNoun");
	public static final List<String> FEMALE_NOUNS = DomainProperties.parse1("femaleNoun");
	public static final List<String> MALE_NOUNS = DomainProperties.parse1("maleNoun");
}
