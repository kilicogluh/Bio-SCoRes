package gov.nih.nlm.ling.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.DomainProperties;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;

/**
 * A collection of static utility methods for dealing with semantic information 
 * (semantic types, groups, etc.). 
 * Semantic type/group information is expected to be present in key/value pairs
 * in a domain properties file.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SemUtils {
	private static Map<String,List<String>> semGroupTypeMap = DomainProperties.parse2("semtype");
	
	/**
	 * Finds all extended superclasses and implemented interfaces for a given class.
	 * This is useful in querying the <code>semanticItems</code> of a <code>Document</code> by class.
	 * 
	 * @param clazz	the class for querying
	 * 
	 * @return  a set of classes/interfaces extended or implemented by <var>clazz</var>. 
	 */
	public static Set<Class> getGeneralizations(Class clazz) {
	    Set<Class> generalizations = new HashSet<Class>();
	    Class superClass = clazz.getSuperclass();
	    if (superClass != null) {
	    	generalizations.add(superClass);
	        generalizations.addAll(getGeneralizations(superClass));
	    }
	    Class[] superInterfaces = clazz.getInterfaces();
	    for (int i = 0; i < superInterfaces.length; i++) {
	        Class superInterface = superInterfaces[i];
	        generalizations.add(superInterface);
	        generalizations.addAll(getGeneralizations(superInterface));
	    }
	    return generalizations;
	}
	
	/**
	 * Determines whether in a pair of classes, one extends/implements the other.
	 * 
	 * @param clazz			a class
	 * @param superClazz	potential superclass/interface
	 * @return	true if <var>clazz</var> extends/implements <var>superClazz</var> 
	 */
	public static boolean isGeneralizationOf(Class clazz, Class superClazz) {
		return getGeneralizations(clazz).contains(superClazz);
	}
	
	/**
	 * Determines whether a class extends/implements another in a list of classes/interfaces.
	 * 
	 * @param clazz			a class
	 * @param superClazzes	list of potential superclasses/interfaces
	 * 
	 * @return true if <var>clazz</var> extends/implements one in <var>superClazzes</var>
	 */
	public static boolean hasGeneralizationOf(Class clazz, Collection<Class> superClazzes) {
		if (superClazzes == null || superClazzes.size() == 0) return false;
		for (Class s: superClazzes)
			if (isGeneralizationOf(clazz,s)) return true;
		return false;
	}
			
	/**
	 * Finds whether two textual units have any semantic types in common.
	 * 
	 * @param a  		the first textual unit
	 * @param b  		the second textual unit
	 * @param headOnly	whether to consider head semantics only
	 * @return  whether any semantic types in common exist
	 */
	public static boolean semTypeEquality(SurfaceElement a, SurfaceElement b, boolean headOnly) {
		if (a.hasSemantics() == false || b.hasSemantics() == false) return false;
		LinkedHashSet<SemanticItem> as = getSalientSemantics(a,headOnly);
		LinkedHashSet<SemanticItem> bs= getSalientSemantics(b,headOnly);
		for (SemanticItem s1: as) {
			for (SemanticItem s2: bs) {
				if (SemUtils.matchingSemType(s2, s1, false) != null ) return true;
			}
		}
		return false;
	}
	
	/**
	 * Finds, if any, the semantic group two textual units have in common.<p>
	 * It returns the first semantic group in iteration, if more than one such
	 * semantic group exists. Returns null if no semantic group is common.
	 *	
	 * @param a 		the first textual unit
	 * @param b  		the second textual unit
	 * @param headOnly	whether to consider head semantics only
	 * @return  whether any semantic groups in common exist
	 */
	public static String matchingSemGroup(SurfaceElement a, SurfaceElement b, boolean headOnly) {
		if (a.hasSemantics() == false || b.hasSemantics() == false) return null;
		LinkedHashSet<SemanticItem> as = getSalientSemantics(a,headOnly);
		LinkedHashSet<SemanticItem> bs = getSalientSemantics(b,headOnly);
		LinkedHashSet<String> semgroups = new LinkedHashSet<>();
		for (SemanticItem s1: as) {
			for (SemanticItem s2: bs) {
				if (s1.equals(s2)) continue;
				String match = matchingSemGroup(s1,s2);
				if (match != null) semgroups.add(match);
			}
		}
		if (semgroups.size() > 0) return semgroups.iterator().next();
		return null;
	}
	
	/**
	 * Finds whether a textual unit has any semantic type that is in the semantic type list provided.
	 * 
	 * @param candidate	the textual unit to consider
	 * @param semTypes  a list of semantic types
	 * @return  whether the textual unit has any semantic type that is in <code>semTypes</code>
	 */
	public static boolean semGroupEquality(SurfaceElement candidate, List<String> semTypes) {
		LinkedHashSet<SemanticItem> candItems = candidate.filterByEntities();
		if (candItems == null) return false;
		LinkedHashSet<SemanticItem> relItems = candidate.filterByRelations();
		if (relItems != null) candItems.addAll(candidate.filterByRelations());
		for (SemanticItem a: candItems) {
			if (SemUtils.semTypeInList(a, semTypes, true)) return true;
		}
		return false;
	}
	
	/**
	 * Finds whether any semantic object in a list has a semantic type that is in the semantic type list provided.
	 * 
	 * @param sems		the list of semantic objects
	 * @param semTypes  the list of semantic types
	 * @return  true if any semantic object has a semantic type in <code>semTypes</code>
	 */
	public static boolean conjSemTypeEquality(List<SemanticItem> sems, List<String> semTypes) {
		if (sems == null || sems.size() == 0) return false;
		for (SemanticItem sem: sems) {
			if (semTypeInList(sem, semTypes, false) == false) return false; 
		}
		return true;
	}
	
	/**
	 * Similar to {@link #conjSemTypeEquality(List, List)}, except this method considers semantic groups of the semantic objects, 
	 * rather than types.
	 * 
	 * @param sems		the list of semantic objects
	 * @param semGroups	the list of semantic groups
	 * @return  true if any semantic object has a semantic group in <code>semGroups</code>
	 */
	public static boolean conjSemGroupEquality(List<SemanticItem> sems, List<String> semGroups) {
		if (sems == null || sems.size() == 0) return false;
		for (SemanticItem sem: sems) {
			Set<String> semgroups = new HashSet<>();
			semgroups = getSemGroups(sem);
			semgroups.retainAll(semGroups);
			if (semgroups.size() == 0) return false;
		}
		return true;
	}
	
	/**
	 * Finds the semantic group that is common to two semantic objects.
	 * 
	 * @param a  the first semantic object
	 * @param b  the second semantic object
	 * @return  the semantic group common to both semantic objects, null if there is no common semantic group
	 */
	public static String matchingSemGroup(SemanticItem a, SemanticItem b) {
		LinkedHashSet<String> aSemGroups = getSemGroups(a);
		LinkedHashSet<String> bSemGroups = getSemGroups(b);
		if (aSemGroups == null || bSemGroups == null) return null;
		aSemGroups.retainAll(bSemGroups);
		return (aSemGroups.size() == 0? null: aSemGroups.iterator().next());
	}
	
	/**
	 * Finds the semantic types common to two semantic objects. <p>
	 * It takes the first semantic type in iteration, if multiple semantic types are in common. 
	 * It may update their active semantic type of the semantic objects, if specified. 
	 * 
	 * @param a				the first semantic object
	 * @param b  			the second semantic object
	 * @param updateType	whether to update the active semantic types to the common semantic type
	 * @return  the semantic type in common, or null if no common semantic type
	 */
	public static String matchingSemType(SemanticItem a, SemanticItem b, boolean updateType) {
		LinkedHashSet<String> common = semTypesInCommon(a,b);
		if (common.size() > 0) {
			String newType = common.iterator().next();
			if (updateType) {
				a.setType(newType);
				b.setType(newType);
			}
			return newType;
		}
		return null;
	}
	
	/**
	 * Finds all the semantic groups of a semantic object.
	 * 
	 * @param a  the semantic object
	 * @return a set of semantic groups the object belongs to
	 */
	public static LinkedHashSet<String> getSemGroups(SemanticItem a) {
		LinkedHashSet<String> groups = new LinkedHashSet<>();
		LinkedHashSet<String> aSemTypes = a.getAllSemtypes();
		for (String s: semGroupTypeMap.keySet()) {
			List<String> types = new ArrayList<>(semGroupTypeMap.get(s));
			types.retainAll(aSemTypes);
			if (types.size() > 0) groups.add(s);
		}
		return groups;
	}
	
	/**
	 * Finds all semantic types common to two semantic objects.
	 * 
	 * @param a  the first semantic object
	 * @param b  the second semantic object
	 * @return  a set of semantic types common to both semantic objects
	 */
	public static LinkedHashSet<String> semTypesInCommon(SemanticItem a, SemanticItem b) {
		LinkedHashSet<String> aSemTypes = a.getAllSemtypes();
		LinkedHashSet<String> bSemTypes = b.getAllSemtypes();
		aSemTypes.retainAll(bSemTypes);
		return aSemTypes;
	}
	
	/**
	 * Finds whether a semantic object has a semantic type that is in the semantic type list provided.
	 * It takes the first semantic type in iteration, if multiple semantic types are in common. 
	 * It may update their active semantic type, if specified. 
	 * 
	 * @param a				the semantic object
	 * @param semtypes  	the list of semantic types
	 * @param updateType  	whether to update the active semantic types to the common semantic type
	 * @return  true if the semantic object has a semantic type in the list
	 */
	public static boolean semTypeInList(SemanticItem a, List<String> semtypes, boolean updateType) {
		LinkedHashSet<String> aSemTypes = a.getAllSemtypes();
		aSemTypes.retainAll(semtypes);
		if (aSemTypes.size() > 0) {
			if (updateType) {
				String newType = aSemTypes.iterator().next();
				a.setType(newType);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Removes atomic semantic objects ({@link Term} instances) that are completely subsumed by others in a document.
	 * 
	 * @param doc	the document with the semantic objects
	 */
	public synchronized static void removeSubsumedTerms(Document doc) {
		List<SemanticItem> toRemove = new ArrayList<>();
		LinkedHashSet<SemanticItem> terms = Document.getSemanticItemsByClass(doc, Term.class);
		for (SemanticItem term : terms) {
			Term t = (Term)term;
			SurfaceElement su = t.getSurfaceElement();
			LinkedHashSet<SemanticItem> suTerms = su.filterByTerms();
			for (SemanticItem suT: suTerms) {
				if (suT.equals(t) || toRemove.contains(suT)) continue;
				if (SpanList.subsume(suT.getSpan(), t.getSpan()) && suT.getSpan().length() > t.getSpan().length()) {
					toRemove.add(t);
					break;
				}
			}
		}
		for (SemanticItem rem: toRemove) {
			doc.removeSemanticItem(rem);
		}
	}
	
	private static LinkedHashSet<SemanticItem> getSalientSemantics(SurfaceElement e, boolean headOnly) {
		LinkedHashSet<SemanticItem> es = new LinkedHashSet<>();
		if (headOnly) {
			es.addAll(e.getHeadSemantics());
		} else {
			es = e.getSemantics();
		}
		return es;
	}

}
