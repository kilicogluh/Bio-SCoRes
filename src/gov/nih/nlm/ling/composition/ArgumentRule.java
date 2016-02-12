package gov.nih.nlm.ling.composition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.nlp.util.CollectionUtils;
import gov.nih.nlm.ling.core.Lexeme;
import gov.nih.nlm.ling.core.WordLexeme;


/**
 * Represents a rule that is used to identify the argument of a <code>Predicate</code>.
 * A rule consists of an embedding type, category, and resulting argument type. It can
 * have exceptions or it can be limited to certain lemmas or semantic classes. 
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO I am not sure this class is well-integrated with the embedding types from the dictionary
public class ArgumentRule {
	private static Logger log = Logger.getLogger(ArgumentRule.class.getName());	
	
	public static final List<String> SCOPE_ARGUMENT_TYPES = Arrays.asList("OBJ","COMP","Arg2");
	
	private String depType;
	private String category;
	private Set<Lexeme> exceptions;
	private Set<Lexeme> specificTo;
	private Set<String> exceptionClass;
	private Set<String> specificClass;
	private String argumentType;
	
	/**
	 * Creates a new <code>ArgumentRule</code> with no exceptions or specificities.
	 * 
	 * @param depType  the embedding type
	 * @param category the lemma category
	 * @param argType  the resulting argument type
	 */
	public ArgumentRule(String depType, String category, String argType) {
		this.depType = depType;
		this.category = category;
		this.argumentType = argType;
		this.exceptions = null;
		this.specificTo = null;
		this.exceptionClass = null;
		this.specificClass = null;
	}

	/**
	 * Creates a new <code>ArgumentRule</code> with exceptions and specificities.
	 * 
	 * @param depType  		the embedding type
	 * @param category 		the lemma category
	 * @param argType		the resulting argument type
	 * @param exceptions  	the lemmas that are exceptions to the rule
	 * @param specificTo  	the lemmas the rule is specific to 
	 * @param exceptionClass the semantic classes that are exceptions to the rule
	 * @param specificClass  the semantic classes that the rule is specific to
	 */
	public ArgumentRule(String depType, String category,String argType,
			Set<Lexeme> exceptions, Set<Lexeme> specificTo, 
			Set<String> exceptionClass,Set<String> specificClass) {
		this(depType,category,argType);
		this.exceptions = exceptions;
		this.specificTo = specificTo;
		this.exceptionClass = exceptionClass;
		this.specificClass = specificClass;
	}
	
	public String getDepType() {
		return depType;
	}

	public String getCategory() {
		return category;
	}
	
	public String getArgumentType() {
		return argumentType;
	}

	public Set<Lexeme> getExceptions() {
		return exceptions;
	}

	public Set<Lexeme> getSpecificTo() {
		return specificTo;
	}

	public Set<String> getExceptionClass() {
		return exceptionClass;
	}

	public Set<String> getSpecificClass() {
		return specificClass;
	}

	/**
	 * Filters a list of <code>ArgumentRule</code> objects by category.
	 * A prefix match is performed.
	 * 
	 * @param rules 	the list of rules
	 * @param category  the category 
	 * 
	 * @return the list of rules applying to the category
	 */
	public static List<ArgumentRule> filterByCategory(List<ArgumentRule> rules, String category) {
		List<ArgumentRule> outRules = new ArrayList<ArgumentRule>();
		if (rules == null || rules.size() == 0) {
			log.warning("No rules are provided for filtering.");
			return outRules;
		}
		if (category == null || category.trim().equals(""))  {
			log.warning("Invalid category provided for filtering.");
			return outRules;
		}
		for (ArgumentRule air: rules) {
			if (air.getCategory().startsWith(category))  outRules.add(air);
		}
		return outRules;
	}
	
	/**
	 * Finds an appropriate <code>ArgumentRule</code> from a list of such rules that applies to a given 
	 * embedding type, lemma, and category. 
	 * 
	 * @param rules  		the list of rules
	 * @param embedType  	the embedding type of the rules to filter with
	 * @param lex  			the lemma 
	 * @param cat  			the category 
	 * @param embeddingTypes	the embedding categories from a dictionary definition to filter with
	 * 
	 * @return  the rule with <var>embedType</var> embedding type applying to <var>lex</var> lemma, null if such a rule cannot be found
	 */
	public static ArgumentRule findMatchingArgumentRule(List<ArgumentRule> rules, String embedType, Lexeme lex, 
			String cat, List<String> embeddingTypes) {
		if (rules == null) {
			log.warning("No rules are provided for filtering.");
			return null;
		}
		if (embedType == null || embedType.trim().equals("")) {
			log.warning("Invalid embedding type provided for filtering.");
			return null;
		}
		if (cat == null || cat.trim().equals("")) {
			log.warning("Invalid category provided for filtering.");
			return null;
		}
		// may not be such a god idea
		if (lex == null) {
			log.warning("Invalid lemma provided for filtering.");
			return null;
		}
		List<DependencyClass> depClasses = DependencyClass.loadDepClasses();
		for (ArgumentRule air: rules) {
			List<String> corrDeps = DependencyClass.filterByDepClass(depClasses,cat, air.getDepType());
			if (corrDeps.size() == 0) {
				corrDeps = new ArrayList<>();
				corrDeps.add(air.getDepType().toUpperCase());
			}
			Set<Lexeme> specific = air.getSpecificTo();
			Set<Lexeme> exception = air.getExceptions();
			Set<String> exceptionClasses = air.getExceptionClass();
			Set<String> specificClasses = air.getSpecificClass();

			if (corrDeps.contains(embedType)) {
				if (specific == null && exception == null && specificClasses == null && exceptionClasses == null) {
					log.log(Level.FINEST,"An argument rule found: {0}.", new Object[]{air.toString()});
					return air;
				}
				if (exception != null && exception.contains(lex)) continue;
				if (exceptionClasses != null && embeddingTypes != null && CollectionUtils.containsAny(exceptionClasses,embeddingTypes)) continue;
				if (specific != null && (specific.contains(lex))) {
					log.log(Level.FINEST,"An argument rule found: {0}.", new Object[]{air.toString()});
					return air;  
				}
				List<String> specificClassDeps = DependencyClass.filterByDepClasses(depClasses, cat, specificClasses);
				// TODO this statement seems a bit fishy, need to test
				if (specific== null && 
					(specificClasses == null || 
					 (embeddingTypes != null && CollectionUtils.containsAny(specificClasses,embeddingTypes))) ||
					  (specificClassDeps.contains(embedType))) {
					log.log(Level.FINEST,"An argument rule found: {0}.", new Object[]{air.toString()});
					return air;
				}				
			}
		}
		return null;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(depType  + "_" + category + "_" + argumentType + "_EXCEPT(");
		if (exceptions != null) {
			for (Lexeme l: exceptions)
				buf.append(l.toString() + ",");
			buf.append(")");
		}
		buf.append("_SPECIFIC(");
		if (specificTo != null) {
			for (Lexeme l: specificTo)
				buf.append(l.toString() + ",");
			buf.append(")");
		}
		buf.append("EXCEPT_CLASS(");
		if (exceptionClass!= null) {
			buf.append(exceptionClass);
		}
		buf.append(")");
		buf.append("SPECIFIC_CLASS(");
		if (specificClass != null) {
			buf.append(specificClass);
		}
		buf.append(")");
		return buf.toString();
	}
	
	/**
	 * Loads the default argument identification rules.
	 * 
	 * @return  the list of default argument identification rules.
	 */
	public static List<ArgumentRule> loadRules() {
		List<ArgumentRule> rules = new ArrayList<>();
		// NN rules
		rules.add(new ArgumentRule("SUBJ","NN","SUBJ",
				null,null,new HashSet<String>(Arrays.asList("PREP_WITH","PREP_BY","PREP_VIA")),null));
		rules.add(new ArgumentRule("OBJ","NN","SUBJ",
								   null,new HashSet<Lexeme>(Arrays.asList(
										new WordLexeme("involvement","NN"),
										new WordLexeme("effect","NN"),
										new WordLexeme("role","NN"),
										new WordLexeme("influence","NN"),
										new WordLexeme("importance","NN"),
										new WordLexeme("impact","NN"))) ,
									null,null));
		rules.add(new ArgumentRule("OBJ","NN","COMP"));
		rules.add(new ArgumentRule("PREP_FOR","NN","SUBJ",null,
				new HashSet<Lexeme>(Arrays.asList(new WordLexeme("role","NN"))),null,null)); 
		
		rules.add(new ArgumentRule("SUBJ","VB","SUBJ"));
		// TODO not great, better to use bidirectional relation logic (CORRELATIVE, etc.)
		rules.add(new ArgumentRule("OBJ","VB","SUBJ",
				null,new HashSet<Lexeme>(Arrays.asList(new WordLexeme("associate","VB"))),null,null));
		rules.add(new ArgumentRule("OBJ","VB","COMP",
				// FACTUALITY-SPECIFIC!!
				new HashSet<Lexeme>(Arrays.asList(new WordLexeme("determine","VB"))),null,null,null));
		
		rules.add(new ArgumentRule("PREP_BY","JJ","SUBJ"));
		rules.add(new ArgumentRule("SUBJ","JJ","SUBJ",null,null,null,new HashSet<String>(Arrays.asList("PREP","SUBJ"))));
		rules.add(new ArgumentRule("SUBJ","JJ","COMP",null,null,new HashSet<String>(Arrays.asList("PREP","SUBJ")),null));
		
		rules.add(new ArgumentRule("PREP_IN","VB","ADJUNCT_IN",null,null,new HashSet<String>(Arrays.asList("PREP_IN")),null));
		rules.add(new ArgumentRule("PREP_IN","NN","ADJUNCT_IN",null,null,new HashSet<String>(Arrays.asList("PREP_IN")),null));
		rules.add(new ArgumentRule("PREP_IN","JJ","ADJUNCT_IN",null,null,new HashSet<String>(Arrays.asList("PREP_IN")),null));
		rules.add(new ArgumentRule("PREP_AT","VB","ADJUNCT_AT",null,null,new HashSet<String>(Arrays.asList("PREP_AT")),null));
		rules.add(new ArgumentRule("PREP_AT","NN","ADJUNCT_AT",null,null,new HashSet<String>(Arrays.asList("PREP_AT")),null));
		rules.add(new ArgumentRule("PREP_AT","JJ","ADJUNCT_AT",null,null,new HashSet<String>(Arrays.asList("PREP_AT")),null));
		rules.add(new ArgumentRule("PREP_ON","VB","ADJUNCT_ON",null,null,new HashSet<String>(Arrays.asList("PREP_ON")),null));
		rules.add(new ArgumentRule("PREP_ON","NN","ADJUNCT_ON",null,null,new HashSet<String>(Arrays.asList("PREP_ON")),null));
		rules.add(new ArgumentRule("PREP_ON","JJ","ADJUNCT_ON",null,null,new HashSet<String>(Arrays.asList("PREP_ON")),null));
		rules.add(new ArgumentRule("PURPCL","VB","ADJUNCT_TO",null,null,new HashSet<String>(Arrays.asList("PURPCL")),null));
		
		// but not
		rules.add(new ArgumentRule("NEGCC","RB","NEG_CONJ"));	
		// nor
		rules.add(new ArgumentRule("NEGCC","CC","NEG_CONJ"));	
		rules.add(new ArgumentRule("CC","CC","CONJ"));
		rules.add(new ArgumentRule("DET","DT","COMP",
				null, new HashSet<Lexeme>(Arrays.asList(new Lexeme[]{
						new WordLexeme("no","DT")
				})),null,null));
		
		rules.add(new ArgumentRule("SUBJ","RB","SUBJ"));
		rules.add(new ArgumentRule("SUBJ","IN","SUBJ"));
		
		//without
		rules.add(new ArgumentRule("POBJ","IN","OBJ"));
		
//		rules.add(new ArgumentRule("advcl","RB","ARG1"));
//		rules.add(new ArgumentRule("mark","RB","ARG2"));
//		rules.add(new ArgumentRule("advcl","IN","ARG1"));
//		rules.add(new ArgumentRule("mark","IN","ARG2"));
		return rules;
		
	}

	
}
