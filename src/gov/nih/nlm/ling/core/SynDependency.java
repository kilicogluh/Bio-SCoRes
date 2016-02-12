package gov.nih.nlm.ling.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.graph.Edge;
import nu.xom.Attribute;
import nu.xom.Element;

/**
 * Represents a syntactic dependency between two textual units. Syntactic dependencies are
 * generally assumed to be between two tokens, but here that notion is generalized, so 
 * dependencies can be between longer textual units. A syntactic dependency consists of 
 * an identifier, a type (the name of the dependency), a governor and a dependent, 
 * both of which are textual units. Examples of dependency types include noun compound (nn),
 * adjectival modifier (amod), etc.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SynDependency implements Edge, Comparable<SynDependency> {
	private static Logger log = Logger.getLogger(SynDependency.class.getName());	
	
	/**
	 * dependency types in which the dependent is semantically dominant to the governor.
	 */
	// TODO Not a perfect or complete list by any means.
	public static final List<String> DEPENDENT_DOMINATED_DEPENDENCIES = 
			Collections.unmodifiableList(Arrays.asList("partmod","aux","auxpass","cop",
					/*"infmod",*/"mark","neg","rcmod"/*,"advmod"*/,"complm"/*,"parataxis"*/,"vmod"));;
	/**
	 * dependency types that hold between the tokens belonging in the same noun phrase.
	 */
	public static final List<String> NP_INTERNAL_DEPENDENCIES = 
			Collections.unmodifiableList(Arrays.asList("det","amod","nn","poss","quantmod","num","measure"));
	/**
	 * dependency types that hold between appositive tokens
	 */
	public static final List<String> APPOS_DEPENDENCIES = Collections.unmodifiableList(Arrays.asList("appos","abbrev"));
	/*	public static final List<String> APPOS_DEPENDENCIES = 
	 * Collections.unmodifiableList(Arrays.asList("appos","abbrev","prep_including","prep_such_as", "prep_compared_to",
	 * "prep_compared_with","prep_versus"};*/
	/**
	 * dependency types that indicate exemplifications
	 */
	public static final List<String> EXEMPLIFY_DEPENDENCIES = Collections.unmodifiableList(Arrays.asList("prep_such_as","prep_including"));

	/**
	 * dependency types that indicate Theme relations (in general, a logical object relation)
	 */
	public static final List<String> THEME_DEPS =  Collections.unmodifiableList(
			Arrays.asList("nsubjpass"/*, "nsubj"*/,"nn", "prep_for", 
			"prep_of", "dobj", "xcomp", "acomp","prep_to", "rcmod", "prep_with","prep_regarding",
			"prep_on", "prep_for", "prep_about", "prep_from"));
	
	/**
	 * Order a set of dependencies based on their semantics of the textual units involved.
	 * Those textual units that have associated predicates are considered to be higher-level and
	 * are given precedence. This allows a bottom-up predicate-argument structure extraction 
	 * (hopefully). Another reason this is done is to make processing of syntactic dependencies 
	 * deterministic. Otherwise, different behavior may be observed depending on the order
	 * these are processed. 
	 */
	public static final Comparator<SynDependency> SEMANTICS_ORDER = 
	        new Comparator<SynDependency>() {
			public int compare(SynDependency e1,SynDependency e2) {
				SurfaceElement e1g = e1.getGovernor();
				boolean e1gp = (e1g.filterByPredicates() != null);
				SurfaceElement e1d = e1.getDependent();
				boolean e1dp = (e1d.filterByPredicates() != null);
				SurfaceElement e2g = e2.getGovernor();
				boolean e2gp = (e2g.filterByPredicates() != null);
				SurfaceElement e2d = e2.getDependent();
				boolean e2dp = (e2d.filterByPredicates() != null);
				if (!e1gp && !e1dp && (e2gp || e2dp)) return 1;
				if ((e1gp || e1dp) && !e2gp && !e2dp) return -1;
				SpanList e1min = (SpanList.atLeft(e1g.getSpan(), e1d.getSpan()) ? e1g.getSpan() : e1d.getSpan());
				SpanList e2min = (SpanList.atLeft(e2g.getSpan(), e2d.getSpan()) ? e2g.getSpan() : e2d.getSpan());
				if (SpanList.atLeft(e2min, e1min)) return 1;
				return -1;
			}	
		};
		
	private String id;
	private String type;
	private SurfaceElement governor;
	private SurfaceElement dependent;

	/**
	 * Creates a <code>SynDependency</code> object.
	 * 
	 * @param id     	the dependency id
	 * @param type		the dependency type
	 * @param governor  the dominant textual unit
	 * @param dependent the dependent textual unit
	 */
	public SynDependency(String id, String type, 
								SurfaceElement governor, 
								SurfaceElement dependent) {
		this.id = id;
		this.type = type;
		this.governor = governor;
		this.dependent = dependent;
	}
	
	public SynDependency(String id, String type, 
			Word governor, 
			Word dependent) {
		this.id = id;
		this.type = type;
		this.governor = governor;
		this.dependent = dependent;
	}
	
	/**
	 * Creates a <code>SynDependency</code> from a Stanford XML element.
	 * 
	 * @param el			the XML element containing the dependency info
	 * @param sentenceWords the word list of the corresponding sentence
	 */
	public SynDependency(Element el, List<Word> sentenceWords) {
		this(el.getAttributeValue("id"),
			 el.getAttributeValue("type"), 
			 sentenceWords.get(Integer.parseInt(el.getChildElements("governor").get(0).getAttributeValue("idx"))-1),
			 sentenceWords.get(Integer.parseInt(el.getChildElements("dependent").get(0).getAttributeValue("idx"))-1));	
	}
	
	/**
	 * Creates a <code>SynDependency</code> from internal XML representation.
	 * The XML element is expected to have the attributes: id, type, t1, and t2. 
	 * t1, t2 are the identifiers for the words.
	 * 
	 * @param el		the XML element
	 * @param wordIdMap	a map that contains words and their ids
	 */
	public SynDependency(Element el, Map<String,Word> wordIdMap) {
		this(el.getAttributeValue("id"),
			 el.getAttributeValue("type"), 
			 wordIdMap.get(el.getAttributeValue("t1")),
			 wordIdMap.get(el.getAttributeValue("t2")));	
	}

	public String getId() {
		return id;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public SurfaceElement getGovernor() {
		return governor;
	}

	public SurfaceElement getDependent() {
		return dependent;
	}
	
	public String toString(){
		return "DEP(" + id + "_" + type + "_" + governor.toString() + "_" + dependent.toString() + ")";
	}
	
	public String toShortString(){
		return type.toUpperCase() + "(" + governor.getText() + "," + dependent.getText() + ")";
	}
	
	/**
	 * Attempts to sort dependencies in a way to allow bottom-up discourse processing.
	 * 
	 * @deprecated
	 */
	public int compareTo(SynDependency sd) {
		// this is not sure
		if (type.equals("advmod")) return -1;
		else if (sd.getType().equals("advmod")) return 1;
		if (type.equals("advcl")) return -1;
		else if (sd.getType().equals("advcl")) return 1;
		if (type.equals("cc")) return -1;
		else if (sd.getType().equals("cc")) return 1;
		if (type.startsWith("conj_")) return -1;
		else if (sd.getType().startsWith("conj_")) return 1;
		SurfaceElement gov = sd.getGovernor();
		SurfaceElement dep = sd.getDependent();
		if (governor.equals(gov)) 
			return dependent.compareTo(dep);
		if (dependent.equals(dep)) 
			return governor.compareTo(gov);
		return 0;
	}
	
	/**
	 * Creates an XML element for the dependency 
	 * 	
	 * @param id  the identifier 
	 * @return  the XML representation of the dependency
	 */
	public Element toXml(int id) {
		Element el = new Element("dependency");
		el.addAttribute(new Attribute("id", "stp_" + Integer.toString(id)));
		int govInd = governor.getSentence().getWords().indexOf(governor)+1;
		int depInd = dependent.getSentence().getWords().indexOf(dependent)+1;
		el.addAttribute(new Attribute("t1","stt_" + govInd));
		el.addAttribute(new Attribute("t2","stt_" + depInd));
		el.addAttribute(new Attribute("type",type));
		return el;
	}
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		SynDependency d= (SynDependency)obj;
		return (this.getType().equals(d.getType())) &&
		        this.getGovernor().equals(d.getGovernor()) &&
		        this.getDependent().equals(d.getDependent());
	}

	public int hashCode() {
	    return
	    ((id == null ? 29 : id.hashCode()) ^
	     (type  == null ? 31 : type.hashCode()) ^ 
	     (governor == null ? 47 : governor.hashCode()) ^
 	     (dependent == null ? 59 : dependent.hashCode()));
	}
	
	private static boolean typeMatch(String type, String cat, boolean exact) {
		if (type.equals(cat)) return true;
		if (!exact && type.startsWith(cat)) return true;
		return false;
	}
	
	/**
	 * Returns the opposite element of the dependency.
	 * 
	 * @param elem  one element of the dependency
	 * @return  the opposite element, null if <var>elem</var> is not an element of the dependency
	 */
	public SurfaceElement opposite(SurfaceElement elem) {
		if (governor.equals(elem)) return dependent;
		if (dependent.equals(elem)) return governor;
		return null;
	}
	
	/**
	 * Finds all dependencies that involve a given textual unit.
	 * 
	 * @param se  the textual unit whose dependencies are being searched for
	 * @param dependencies  the dependency list to search for
	 * @return  the list of dependencies that involve the textual unit, or empty list if no dependencies 
	 */
	public static List<SynDependency> dependencies(SurfaceElement se, List<SynDependency> dependencies) {
		List<SynDependency> deps = new ArrayList<>();
		if (dependencies == null) return deps;
		List<SynDependency> outDeps = outDependencies(se, dependencies);
		List<SynDependency> inDeps = inDependencies(se, dependencies);
		deps = outDeps;
		deps.addAll(inDeps);
		return deps;
	}
	
	/**
	 * Similar to {@link #dependencies(SurfaceElement, List)}, except this method only considers those dependencies
	 * where the textual unit is in governor position.
	 * 
	 * @param se  the textual unit whose dependencies are being searched for
	 * @param dependencies  the dependency list to search for
	 * @return  the list of dependencies where the textual unit is in governor position, or empty list if no such dependency
	 */
	public static List<SynDependency> outDependencies(SurfaceElement se, List<SynDependency> dependencies) {
		List<SynDependency> outDeps = new ArrayList<>();
		for (SynDependency dep: dependencies) {
			if (dep.getGovernor().equals(se)) 
				outDeps.add(dep);
		}
		return outDeps;	
	}
	
	/**
	 * Similar to {@link #dependencies(SurfaceElement, List)}, except this method only considers those dependencies
	 * where the textual unit is in dependent position.
	 * 
	 * @param se  the textual unit whose dependencies are being searched for
	 * @param dependencies  the dependency list to search for
	 * @return  the list of dependencies where the textual unit is in dependent position, or empty list if no such dependency
	 */
	public static List<SynDependency> inDependencies(SurfaceElement se, List<SynDependency> dependencies) {
		List<SynDependency> outDeps = new ArrayList<>();
		for (SynDependency dep: dependencies) {
			if (dep.getDependent().equals(se)) 
				outDeps.add(dep);
		}
		return outDeps;	
	}
	
	/**
	 * Finds all dependencies with a certain type that involve a given textual unit.
	 * Approximate type matching is allowed. For example, setting the type to "prep" and
	 * exactMatch to false, all prepositional dependencies will be retrieved, including "prep_in", 
	 * "prep_to", "prep_of", etc.
	 * 
	 * @param se  the textual unit whose dependencies are being searched for
	 * @param dependencies  the dependency list to search for
	 * @param type  the dependency type to search for
	 * @param exactMatch whether to do exact match on the type name
	 * @return  the list of dependencies that involve the textual unit, or empty list if no such dependency
	 */
	public static List<SynDependency> dependenciesWithType(SurfaceElement se, List<SynDependency> dependencies, 
			String type, boolean exactMatch) {
		List<SynDependency> deps = new ArrayList<>();
		if (dependencies == null) return deps;
		List<SynDependency> outDeps = outDependenciesWithType(se, dependencies, type, exactMatch);
		List<SynDependency> inDeps = inDependenciesWithType(se, dependencies, type, exactMatch);
		deps = outDeps;
		deps.addAll(inDeps);
		return deps;
	}
	
	/**
	 * Similar to {@link #dependenciesWithType(SurfaceElement, List, String, boolean)}, except this method 
	 * uses a list of dependency types, instead of a single type.
	 *
	 * @param se  the textual unit whose dependencies are being searched for
	 * @param dependencies  the dependency list to search for
	 * @param types  the dependency types to search for
	 * @param exactMatch whether to do exact match on the type name
	 * @return  the list of dependencies that involve the textual unit, or empty list if no such dependency
	 */
	public static List<SynDependency> dependenciesWithTypes(SurfaceElement se, List<SynDependency> dependencies, 
			List<String> types, boolean exactMatch) {
		List<SynDependency> outDeps = new ArrayList<>();
		if (dependencies == null) return outDeps;
		for (String t: types) {
			List<SynDependency> seDeps = dependenciesWithType(se, dependencies, t, exactMatch);
			outDeps.addAll(seDeps);
		}
		return outDeps;		
	}
	
	/**
	 * Finds all dependencies within a list with a certain type.
	 * 
	 * @param dependencies  the dependency list to search for
	 * @param type  the dependency type to search for
	 * @param exactMatch whether to do exact match on the type name
	 * @return  the list of dependencies meeting the criteria, or empty list if no such dependency
	 */
	public static List<SynDependency> dependenciesWithType(List<SynDependency> dependencies, String type, boolean exactMatch) {
		List<SynDependency> outDeps = new ArrayList<>();
		if (dependencies == null) return outDeps;
		for (SynDependency dep: dependencies) {
			if (typeMatch(dep.getType(),type,exactMatch)) {
				outDeps.add(dep);
			}
		}
		return outDeps;
	}
	
	/**
	 * Similar to {@link #dependenciesWithType(List, String, boolean)}; this method uses a list of types, instead of a single type.
	 * 
	 * @param dependencies  the dependency list to search for
	 * @param types  the dependency types to search for
	 * @param exactMatch whether to do exact match on the type name
	 * @return  the list of dependencies meeting the criteria, or empty list if no such dependency
	 */
	public static List<SynDependency> dependenciesWithTypes(List<SynDependency> dependencies, List<String> types, boolean exactMatch) {
		List<SynDependency> outDeps = new ArrayList<>();
		if (dependencies == null) return outDeps;
		for (String t: types) {
			List<SynDependency> seDeps = dependenciesWithType(dependencies, t, exactMatch);
			outDeps.addAll(seDeps);
		}
		return outDeps;	
	}
	
	/**
	 * Similar to {@link #outDependencies(SurfaceElement, List)}, adds type of the dependency as a search criterion.
	 * 
	 * @param se			the textual unit whose dependencies are being searched for
	 * @param dependencies  the dependency list to search for
	 * @param type  		the dependency type to search for
	 * @param exactMatch 	whether to do exact match on the type name
	 * @return  the list of dependencies meeting the criteria, or empty list if no such dependency
	 */
	public static List<SynDependency> outDependenciesWithType(SurfaceElement se, List<SynDependency> dependencies, 
			String type, boolean exactMatch) {
		List<SynDependency> outDeps = new ArrayList<>();
		if (dependencies == null) return outDeps;
		List<SynDependency> deps = outDependencies(se, dependencies);
		for (SynDependency dep: deps) {
			if (typeMatch(dep.getType(),type,exactMatch)) {
				outDeps.add(dep);
			}
		}
		return outDeps;
	}
	
	/**
	 * Similar to {@link #outDependencies(SurfaceElement, List)}, adds a list of dependency types as a search criterion.
	 * 
	 * @param se			the textual unit whose dependencies are being searched for
	 * @param dependencies  the dependency list to search for
	 * @param types  		the dependency types to search for
	 * @param exactMatch 	whether to do exact match on the type name
	 * @return  the list of dependencies meeting the criteria, or empty list if no such dependency
	 */
	public static List<SynDependency> outDependenciesWithTypes(SurfaceElement se, List<SynDependency> dependencies, 
			List<String> types, boolean exactMatch) {
		List<SynDependency> outDeps = new ArrayList<>();
		if (dependencies == null) return outDeps;
		List<SynDependency> deps = outDependencies(se, dependencies);
		for (String t: types) {
			List<SynDependency> tDeps = dependenciesWithType(deps, t, exactMatch);
			outDeps.addAll(tDeps);
		}
		return outDeps;
	}
	
	/**
	 * Similar to {@link #inDependencies(SurfaceElement, List)}, adds type of the dependency as a search criterion.
	 * 
	 * @param se			the textual unit whose dependencies are being searched for
	 * @param dependencies  the dependency list to search for
	 * @param type  		the dependency type to search for
	 * @param exactMatch 	whether to do exact match on the type name
	 * @return  the list of dependencies meeting the criteria, or empty list if no such dependency
	 */
	public static List<SynDependency> inDependenciesWithType(SurfaceElement se, List<SynDependency> dependencies, 
			String type, boolean exactMatch) {
		List<SynDependency> outDeps = new ArrayList<>();
		if (dependencies == null) return outDeps;
		List<SynDependency> deps = inDependencies(se, dependencies);
		for (SynDependency dep: deps) {
			if (typeMatch(dep.getType(),type,exactMatch)) {
				outDeps.add(dep);
			}
		}
		return outDeps;
	}
	
	/**
	 * Similar to {@link #inDependencies(SurfaceElement, List)}, adds a list of dependency types as a search criterion.
	 *
	 * @param se			the textual unit whose dependencies are being searched for
	 * @param dependencies  the dependency list to search for
	 * @param types  		the dependency types to search for
	 * @param exactMatch 	whether to do exact match on the type name
	 * @return  the list of dependencies meeting the criteria, or empty list if no such dependency
	 */
	public static List<SynDependency> inDependenciesWithTypes(SurfaceElement se, List<SynDependency> dependencies, 
			List<String> types, boolean exactMatch) {
		List<SynDependency> outDeps = new ArrayList<>();
		if (dependencies == null) return outDeps;
		List<SynDependency> deps = inDependencies(se, dependencies);
		for (String t: types) {
			List<SynDependency> tDeps = dependenciesWithType(deps, t, exactMatch);
			outDeps.addAll(tDeps);
		}
		return outDeps;
	}
			
	/**
	 * Recursively finds the dependency path between two textual units. The chain can be one-directional 
	 * or the direction may be ignored. 
	 * 
	 * @param dependencies  the dependency list to inspect
	 * @param start  		the first textual unit
	 * @param goal  		the second textual unit
	 * @param directed 		 whether the search is unidirectional (true if so)
	 * @return  the list of dependencies that form the path between these textual unit, null if no path is found
	 */
	public static List<SynDependency> findDependencyPath(List<SynDependency> dependencies, 
			SurfaceElement start, SurfaceElement goal, boolean directed) {
		  // list of visited nodes
		  LinkedList<SurfaceElement> closedList = new LinkedList<>();
		  Map<SurfaceElement,SurfaceElement> parents = new HashMap<>(); 
		  // list of words to visit (sorted)
		  LinkedList<SurfaceElement> openList = new LinkedList<>();
		  openList.add(start);
		  parents.put(start,null);
		  
		  while (!openList.isEmpty()) {
			SurfaceElement surf = openList.removeFirst();
			if (surf.equals(goal)) {
			      return constructDependencyPath(dependencies,parents,goal,directed);
			}
		    else {
				 List<SynDependency> surfDeps = (directed ? 
						  outDependencies(surf,dependencies) :
						  dependencies(surf,dependencies));
				 closedList.add(surf);
				 if (surfDeps.size() > 0) {
					  log.log(Level.FINEST,"Searching for dependency path: Added to closed list {0}. ", new Object[]{surf.toString()});
					  List<SurfaceElement> neighbors = new ArrayList<>();
					  for (SynDependency dd : surfDeps) {
						  if (dd.getGovernor().equals(surf)) neighbors.add(dd.getDependent());
						  else if (!directed) {
							  if (dd.getDependent().equals(surf)) neighbors.add(dd.getGovernor());
						  }
	//					  if (!directed && dd.getDependent().equals(w) && !(openList.contains(dd.getGovernor())))
	//						  neighbors.add(dd.getGovernor());
					  }
			      
			         // add neighbors to the open list
			     	for (SurfaceElement n: neighbors) {
			     		if (!closedList.contains(n) &&
			     				!openList.contains(n)) 
			     		{
			     			parents.put(n,surf);
			     			openList.add(n);
			     		}
			     	}
		      }
		    }
		  }  
		  // no path found
		  log.log(Level.FINEST,"Searching for dependency path: No path between {0} and {1}. ", new Object[]{start,goal});
		  return null;
	}
	
	private static List<SynDependency> constructDependencyPath(List<SynDependency> dependencies, 
		Map<SurfaceElement,SurfaceElement> parents, SurfaceElement surf, boolean directed) {
	  LinkedList<SynDependency> path = new LinkedList<>();
	  while (parents.get(surf) != null) {
		SurfaceElement surf1 = parents.get(surf);
		List<SynDependency> ds =  findDependenciesBetween(dependencies,surf1,surf,directed);
		if (ds.size() > 0) {
			path.addFirst(ds.get(0));
		}
	    surf = surf1;
	  }
	  for (SynDependency sd: path) {
		  log.log(Level.FINEST,"Constructing dependency path: Dependency on path is {0}. ", new Object[]{sd.toShortString()});
	  }
	  return path;
	}
	
	/**
	 * Finds all direct dependencies between two textual units (path length of 1).
	 * 
	 * @param dependencies	the list of all dependencies to consider
	 * @param surf1 		the first textual unit
	 * @param surf2  		the second textual unit
	 * @param directed  	whether direction of the dependency should be considered
	 * @return  the list of direct dependencies between <var>surf1</var> and <var>surf2</var>
	 */
	public static List<SynDependency> findDependenciesBetween(List<SynDependency> dependencies, 
			SurfaceElement surf1, SurfaceElement surf2,
			boolean directed) {
		List<SynDependency> out = new ArrayList<>();
		for (SynDependency dep: dependencies) {
			if ((dep.getGovernor().equals(surf1) && dep.getDependent().equals(surf2)) ||
				(!directed && dep.getDependent().equals(surf1)) && dep.getGovernor().equals(surf2))
				out.add(dep);
		}
		return out;		
	}
	
	/**
	 * Gets all the governing textual units in a given collection of dependencies.
	 * 
	 * @param deps  the list of dependencies
	 * @return  the set of all governors
	 */
	public static LinkedHashSet<SurfaceElement> getAllGovernors(List<SynDependency> deps) {
		LinkedHashSet<SurfaceElement> out = new LinkedHashSet<>();
		if (deps == null) return out;
		for (SynDependency sd: deps) {
			out.add(sd.getGovernor());
		}
		return out;
	}
	
	/**
	 * Returns all the dependent textual units in a given collection of dependencies.
	 * 
	 * @param deps  the list of dependencies
	 * @return  the set of all dependents
	 */
	public static LinkedHashSet<SurfaceElement> getAllDependents(List<SynDependency> deps) {
		LinkedHashSet<SurfaceElement> out = new LinkedHashSet<>();
		if (deps == null) return out;
		for (SynDependency sd: deps) {
			out.add(sd.getDependent());
		}
		return out;
	}
	
	/**
	 * Returns all the predecessors of a textual unit using a list of dependencies. 
	 * A predecessor is a textual unit that dominates the other textual unit.
	 * 
	 * @param su	a textual unit
	 * @param deps  the list of dependencies
	 * @return  the set of all predecessors to <var>su</var>
	 */
	public static LinkedHashSet<SurfaceElement> getPredecessors(SurfaceElement su, List<SynDependency> deps) {
		List<SynDependency> inDeps = inDependencies(su,deps);
		return getAllGovernors(inDeps);
	}
	
	/**
	 * Returns all the successors of a textual unit using a list of dependencies. 
	 * A successor is a textual unit that is dominated by the other textual unit.
	 * 
	 * @param su	a textual unit
	 * @param deps  the list of dependencies
	 * @return  the set of all successors to <var>su</var>
	 */
	public static LinkedHashSet<SurfaceElement> getSuccessors(SurfaceElement su, List<SynDependency> deps) {
		List<SynDependency> inDeps = outDependencies(su,deps);
		return getAllDependents(inDeps);
	}
	
	/**
	 * Finds the common ancestor of a list of textual units in the dependency graph.
	 * It currently only considers a single level up from the textual units.
	 * 
	 * @param deps	the dependencies in the graph
	 * @param surfs the list of textual units
	 * @return  the common ancestor if any, null otherwise
	 */
	// TODO Extend beyond a single level.
	public static SurfaceElement getCommonAncestor(List<SynDependency> deps, List<SurfaceElement> surfs) {
		if (surfs.size() ==0) return null;
		List<SurfaceElement> aso = new ArrayList<>();
		SurfaceElement first = surfs.get(0);
		List<SynDependency> inDeps = inDependencies(first,deps);
		for (SynDependency d: inDeps) {
			// Bidirectional dependency types
			if (d.getType().startsWith("conj") || d.getType().equals("cc") || d.getType().equals("appos")) continue;
			log.log(Level.FINEST,"Adding in-dependency {0}.", new Object[]{d.toShortString()});
			aso.add(d.getGovernor());
		}
		for (int i=1; i < surfs.size(); i++) {
			Set<SurfaceElement> curr = new HashSet<>();
			inDeps = inDependencies(surfs.get(i),deps);
			if (inDeps == null) return null;
			for (SynDependency d: inDeps) {
				log.log(Level.FINEST,"Checking in-dependency {0}." + new Object[]{d.toShortString()});
				if (aso.contains(d.getGovernor())) {
					curr.add(d.getGovernor());
				}
			}
			if (curr.size() == 0) return null;
			aso.retainAll(curr);
			if (aso.size() == 0) return null;
		}
		return (aso.size() == 0 ? null :aso.get(aso.size()-1));
	}
	
	/**
	 * Finds all out-dependencies with given types from a node or one of its descendants. <var>withTypes</var>
	 * list contains syntactic dependencies with one of the given types, and <var>others</var> contains dependencies
	 * with other types.
	 * 
	 * @param dependencies  the dependencies to check 
	 * @param surf   the textual unit whose descendants to search
	 * @param depTypes  dependency types to consider
	 * @param withTypes   dependencies with the types
	 * @param others  dependencies with other types
	 */
	public static void getSubtreeDependenciesWithTypes(List<SynDependency> dependencies, SurfaceElement surf, List<String> depTypes, 
			List<SynDependency> withTypes, List<SynDependency> others) {
		List<SynDependency> outDeps = SynDependency.outDependencies(surf, dependencies);
		// leaf
		if ( outDeps.size() == 0 && SynDependency.inDependencies(surf, dependencies).size() > 0) {
			return;
		} 
		for (SynDependency e: outDeps) {
			if (depTypes.contains(e.getType()))  {
				withTypes.add(e);
				getSubtreeDependenciesWithTypes(dependencies,e.getDependent(),depTypes,withTypes,others);
			}
			else {
				others.add(e);
			}	
		}
	}
	
}
