package gov.nih.nlm.ling.composition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a higher-level classification of dependencies. A <code>DependencyClass</code>
 * is a triple of a higher-level dependency name, the syntactic categories it applies to, and
 * the corresponding syntactic dependency labels. <p>
 * As syntactic dependencies are processed to create a semantic embedding graph,
 * some dependency labels are changed to capture the nature of the semantic
 * dependency more directly. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class DependencyClass {
	private static Logger log = Logger.getLogger(DependencyClass.class.getName());	
	
	private String depClass;
	private String category;
	private List<String> depTypes;
	
	/**
	 * Creates a dependency mapping. 
	 * 
	 * @param depClass  the label of the higher-level dependency class
	 * @param category  the category this mapping applies to ("*" if it applies to all)
	 * @param depTypes  the corresponding syntactic dependency labels
	 */
	public DependencyClass(String depClass, String category, List<String> depTypes) {
		this.depClass = depClass;
		this.category = category;
		this.depTypes = depTypes;
	}
	
	public String getDepClass() {
		return depClass;
	}

	public String getCategory() {
		return category;
	}

	public List<String> getDepTypes() {
		return depTypes;
	}
	
	/**
	 * Determines whether a dependency label (<var>dep</var>) belongs in one of the higher-level
	 * dependency labels specified in <var>embedTypes</var>, for a given category.
	 * 
	 * @param category  	the category to look for
	 * @param dep  			the lower-level dependency label
	 * @param embedTypes  	the higher-level classes
	 * 
	 * @return  true if <var>dep</var> belongs
	 */
	public static boolean inClass(String category,String dep,List<String> embedTypes) {
		if (embedTypes == null || embedTypes.size() == 0) return false;
		List<DependencyClass> depClasses = loadDepClasses();
		for (String em: embedTypes) {
			if (dep.equalsIgnoreCase(em)) { 			
				log.log(Level.FINEST,"Dependency {0} is in depClass {1}.", new Object[]{dep,em});
				return true;
			}
			for (DependencyClass sdm: depClasses) {
				if ((sdm.getDepClass().equals(em))  && 
					(sdm.getCategory().startsWith(category) || sdm.getCategory().equals("*"))  && 
					(sdm.getDepTypes().contains(dep))) {
					log.log(Level.FINEST,"Dependency {0} is in depClass {1}.", new Object[]{dep,em});
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Filters a list of dependency mappings by a category.
	 * 
	 * @param mappings  the mappings to filter
	 * @param category  the category to filter with
	 * @return  the mappings that are valid for the category
	 */
	public static List<DependencyClass> filterByCategory(List<DependencyClass> mappings, String category) {
		List<DependencyClass> outMappings = new ArrayList<>();
		if (mappings == null || mappings.size() == 0) return outMappings;
		for (DependencyClass sdm: mappings) {
			if (sdm.getCategory().startsWith(category) || sdm.getCategory().equals("*"))  outMappings.add(sdm);
		}
		return outMappings;
	}
	
	/**
	 * Filters a list dependency mappings by a category and high-level label.
	 * 
	 * @param mappings  the mappings to filter
	 * @param category  the category to filter with
	 * @param depClass  the high-level label
	 * @return  a list of dependency labels in the given category and high-level class, or empty list
	 */
	public static List<String> filterByDepClass(List<DependencyClass> mappings, String category, String depClass) {
		List<String> out = new ArrayList<>();
		List<DependencyClass> cl = DependencyClass.filterByCategory(mappings, category);
		for (DependencyClass sdm: cl) {
			if (sdm.getDepClass().equals(depClass)) out.addAll(sdm.getDepTypes());
		}
		return out;
	}
	
	/**
	 * Similar to {@code #filterByDepClass(List, String, String)}, but takes as input a set of high-level dependency labels
	 * instead of just one.
	 * 
	 * @param mappings		the mappings to filter
	 * @param category		the category to filter with
	 * @param depClasses	the high-level labels
	 * 
	 * @return  a list of dependency labels in the given category and high-level classes, or empty list 
	 */
	public static List<String> filterByDepClasses(List<DependencyClass> mappings, String category, Set<String> depClasses) {
		List<String> out = new ArrayList<>();
		if (depClasses == null || depClasses.size() == 0) return out;
		List<DependencyClass> cl = DependencyClass.filterByCategory(mappings, category);
		for (DependencyClass sdm: cl) {
			if (depClasses.contains(sdm.getDepClass()))  out.addAll(sdm.getDepTypes());
		}
		return out;
	}
	
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(depClass  + "_" + category + "[");
		if (depTypes != null) {
			for (String d: depTypes)
				buf.append(d.toString() + ",");
		}
		buf.append("]");
		return buf.toString();
	}
	
	/**
	 * Loads all high-level/low-level dependency mappings.
	 * 
	 * @return a list of <code>DependencyClass</code> objects
	 */
	// TODO Read from a file instead. 
	// TODO Use of universal dependencies.
	public static List<DependencyClass> loadDepClasses() {
		List<DependencyClass> mappings = new ArrayList<>();
		
		mappings.add(new DependencyClass("OBJ","NN",Arrays.asList("prep_of","prepc_of","nn","amod","quantmod","measure","poss")));
		mappings.add(new DependencyClass("OBJ","VB",Arrays.asList("dobj","nsubjpass","xsubjpass","csubjpass","partmod","vmod")));
		mappings.add(new DependencyClass("OBJ","JJ",Arrays.asList("amod","xcomp","infmod","purpcl")));
		mappings.add(new DependencyClass("OBJ","IN",Arrays.asList("pobj")));
		mappings.add(new DependencyClass("SUBJ","NN",Arrays.asList("prep_by","prepc_by","prep_with","prepc_with","prepc_via","prep_via","agent",
				"nsubj","csubj","xsubj")));
		mappings.add(new DependencyClass("SUBJ","VB",Arrays.asList("nsubj","xsubj","csubj","agent","rcmod","agent")));
		mappings.add(new DependencyClass("PURPCL","VB",Arrays.asList("xcomp","infmod","purpcl","prep_to","prepc_to")));
		mappings.add(new DependencyClass("SUBJ","JJ",Arrays.asList("nsubj","csubj","xsubj","mark")));
		mappings.add(new DependencyClass("SUBJ","RB",Arrays.asList("mark")));
		mappings.add(new DependencyClass("SUBJ","IN",Arrays.asList("mark")));
		mappings.add(new DependencyClass("SUBJ","VB",Arrays.asList("mark")));
		mappings.add(new DependencyClass("AUX","MD",Arrays.asList("aux","auxpass","dep")));
		
		mappings.add(new DependencyClass("PREP","*",Arrays.asList("prep_about","prepc_about","prep_on","prepc_on", "prep_upon","prepc_upon",
				"prep_at","prepc_at","prep_as","prepc_as","prep_between","prepc_between",
				"prep_from","prepc_from","prep_against","prepc_against",
				"prep_except","prepc_except", "prep_without", "prepc_without")));
		mappings.add(new DependencyClass("PREP_IN","*",Arrays.asList("prep_in","prepc_in")));
		mappings.add(new DependencyClass("PREP_TO","*",Arrays.asList("prep_to","prepc_to")));
		mappings.add(new DependencyClass("PREP_OF","*",Arrays.asList("prep_of","prepc_of")));
		mappings.add(new DependencyClass("PREP_FOR","*",Arrays.asList("prep_for","prepc_for")));
		mappings.add(new DependencyClass("PREP_ABOUT","*",Arrays.asList("prep_about","prepc_about")));
		mappings.add(new DependencyClass("PREP_BY","*",Arrays.asList("prep_by","prepc_by")));
		mappings.add(new DependencyClass("PREP_VIA","*",Arrays.asList("prep_via","prepc_via")));
		mappings.add(new DependencyClass("PREP_ON","*",Arrays.asList("prep_on","prepc_on", "prep_upon","prepc_upon")));
		mappings.add(new DependencyClass("PREP_WITH","*",Arrays.asList("prep_with","prepc_with")));
		mappings.add(new DependencyClass("PREP_AT","*",Arrays.asList("prep_at","prepc_at")));
		mappings.add(new DependencyClass("PREP_AS","*",Arrays.asList("prep_as","prepc_as")));
		mappings.add(new DependencyClass("PREP_BETWEEN","*",Arrays.asList("prep_between","prepc_between")));
		mappings.add(new DependencyClass("PREP_FROM","*",Arrays.asList("prep_from","prepc_from")));
		mappings.add(new DependencyClass("PREP_AGAINST","*",Arrays.asList("prep_against","prepc_against")));
		mappings.add(new DependencyClass("PREP_EXCEPT","*",Arrays.asList("prep_except","prepc_except")));
		mappings.add(new DependencyClass("THAT_COMP","*",Arrays.asList("that_comp","ccomp","parataxis")));
		mappings.add(new DependencyClass("XCOMP","*",Arrays.asList("xcomp","infmod","purpcl","prep_to","prepc_to")));

		return mappings;
		
	}

	
}
