package gov.nih.nlm.ling.brat;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;
import gov.nih.nlm.ling.util.FileUtils;

/**
 * A class to read a text file and its corresponding standoff annotation files. <p> 
 * It currently handles the type of objects brat annotation tool can create.
 * These include terms (or named entities), relations (such as coreference), 
 * events (or predicate-argument structures), event modifications (speculation, etc.),
 * and references (concept information).<p>
 * It can read multiple standoff annotation files associated with a single text file.
 * 
 * @author Halil Kilicoglu
 *
 */
public class StandoffAnnotationReader {
	private static Logger log = Logger.getLogger(StandoffAnnotationReader.class.getName());	
	
	/**
	 * An enum that associates annotation classes with the prefixes that correspond to it.
	 * 
	 * We could have used the annotation classes that implement {#link Annotation} interface directly,
	 * but for some standoff annotation types we encountered (such as Equiv, Annotator Note), we 
	 * do not currently have corresponding annotation classes, so we use this enum instead.
	 * 
	 * @author Halil Kilicoglu
	 *
	 */
	public enum AnnotationType {
		Equiv, Term, Relation, Event, Modification, Reference, AnnotatorNote;
		
		private String prefix;
		
		public static AnnotationType getDefault(String prefix) {
			if (prefix.equals("*")) return Equiv;
			else if (prefix.equals("T")) return Term;
			else if (prefix.equals("R")) return Relation;
			else if (prefix.equals("E")) return Event;
			else if (prefix.equals("A")) return Modification;
			else if (prefix.equals("N")) return Reference;
			else if (prefix.equals("#")) return AnnotatorNote;
			return null;
		}
			
		public String getPrefix() {
			return prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}
	}
	
	protected static final String DEFAULT_ENCODING = "UTF-8";
	protected static final String FIELD_DELIMITER = "[ ]+";
	
	protected static Map<SemanticItem,Set<SemanticItem>> associations; 
	protected static Map<String,Term> termSpans;
	protected static Map<String,String> equivIds;
	
	protected static Map<String,Annotation> annotationIdMap;

	/**
	 * Creates a <code>Document</code> object from a the text file associated with the standoff annotations.
	 * 
	 * @param id  			the id to give to the <code>Document</code> object being created
	 * @param txtFileName	the name of the text file 
	 * @return  			a <code>Document</code> object representing the content of the text file,
	 * 						or null if the file cannot be read
	 */
	// TODO: It seems that something is broken with how brat handles blank lines. This was especially a big 
	// issue in SPL dataset. FileUtils.stringFromFiles methods did not work properly and the tokens and 
	// the annotation offsets did not match. Ignoring blank lines like below seems to work.
	public static Document readTextFile(String id, String txtFileName) {
		try {
//			String text = FileUtils.stringFromFileWithBytes(txtFileName, DEFAULT_ENCODING);
	    	BufferedReader br = new BufferedReader(
	    			new InputStreamReader(new FileInputStream(txtFileName),DEFAULT_ENCODING));
	    	StringBuffer lineBuf = new StringBuffer();
	    	String strLine = "";
	    	while ((strLine = br.readLine()) != null)  {
	    		lineBuf.append(strLine);
	    		lineBuf.append("\n");
	    	}
	    	br.close();
	    	String text = lineBuf.toString();
			log.log(Level.INFO, "Read {0} characters from file {1}.", new Object[]{text.length(),txtFileName});
			return new Document(id,text);
		} catch (IOException ioe) {
			log.log(Level.SEVERE,"Unable to read text file: {0}.", txtFileName);
			return null;
		}
	}
	
	/**
	 * Reads a list of standoff annotation files, only considering the semantic types provided
	 * with <var>parseTypes</var> variable. <p>
	 * It returns  a <code>Map</code> object, where the keys are annotation types used by brat 
	 * (Term, Equiv, Event, Relation, Modification, Reference)
	 * and the values are the lines with the corresponding annotation type. This method does not 
	 * perform actual instantiation of semantic objects. It currently ignores N* lines (notes).
	 * If <var>parseTypes</var> is null, it will try to load all semantic types.
	 * 
	 * @param annFileNames	the list of standoff annotation files
	 * @param prefixMap		the prefix map to use for associating lines with annotation classes; if null, defaults are used
	 * @param parseTypes  	the list of semantic types to parse
	 * 
	 * @return  a <code>Map</code> of annotation class/lines key-value pairs.
	 */
	public static Map<AnnotationType,List<String>> readAnnotationFiles(List<String> annFileNames, Map<String,AnnotationType> prefixMap, List<String> parseTypes) {
		String strLine;
		List<String> termLines = new ArrayList<>();
 		List<String> eventLines = new ArrayList<>();
		List<String> relationLines = new ArrayList<>();
		List<String> equivLines = new ArrayList<>();
		List<String> modificationLines = new ArrayList<>();
		List<String> referenceLines = new ArrayList<>();
		
		Map<AnnotationType,List<String>> anns= new HashMap<>();
		if (annFileNames != null) {
			anns = new HashMap<>();
			for (String annFileName: annFileNames) {
				try {
					List<String> lines = FileUtils.linesFromFile(annFileName, DEFAULT_ENCODING);
					for (int i=0; i < lines.size(); i++) {
						// should this be trimmed?
						strLine = lines.get(i).trim();
						if (strLine.length() == 0) continue;
						AnnotationType pa = parseAs(strLine,prefixMap);
						if (pa == null || pa == AnnotationType.AnnotatorNote) continue;
						if (parseTypes != null && parseTypes.contains(getSemType(strLine)) == false) continue;
						log.log(Level.FINEST, "Read standoff annotation line: {0}.", strLine);

						if (pa == AnnotationType.Term) {
//						if (strLine.startsWith("T")) {
							termLines.add(strLine);
						} else if (pa == AnnotationType.Equiv) {
//						} else if (strLine.startsWith("*")) {
							equivLines.add(strLine);
							//event lines
						} else if (pa == AnnotationType.Event) {
//						} else if (strLine.startsWith("E")) {
							eventLines.add(strLine);
						} else if (pa == AnnotationType.Relation) {
//						} else if (strLine.startsWith("R")) {
							relationLines.add(strLine);
					    } else if (pa == AnnotationType.Modification) {
//						} else if (strLine.startsWith("M") || strLine.startsWith("A")) {
//						} else if (strLine.startsWith("M")) {							
							modificationLines.add(strLine);
					    } else if (pa == AnnotationType.Reference) {
//						} else if (strLine.startsWith("#") || strLine.startsWith("N")) {
//						} else if (strLine.startsWith("#") || strLine.startsWith("N") || strLine.startsWith("A")) {							
							referenceLines.add(strLine);
						}
					}
				} catch (IOException ioe) {
					log.log(Level.SEVERE,"Unable to read standoff annotation file {0}.", annFileName);
				}
			}
			anns.put(AnnotationType.Term, termLines);
			anns.put(AnnotationType.Equiv, equivLines);
			anns.put(AnnotationType.Relation, relationLines);
			anns.put(AnnotationType.Event, orderEventLines(eventLines));
			anns.put(AnnotationType.Modification, modificationLines);
			anns.put(AnnotationType.Reference, referenceLines);
		}
		return anns;
	}
	
	private static AnnotationType parseAs(String line, Map<String,AnnotationType> prefixMap) {
		String prefix = line.substring(0,1);
		if (prefixMap == null || prefixMap.size() == 0) {
			return AnnotationType.getDefault(prefix);
		}
		if (prefixMap.containsKey(prefix)) {
			return prefixMap.get(prefix);
		}
		return null;
	}
	
	// events can be nested, it's important to sort them such that
	// an event that subsumes another is processed after all its components
	// are processed.
	private static List<String> orderEventLines(List<String> evs) {
		if (evs== null || evs.size() == 0) return evs;
		Pattern argPat = Pattern.compile(":([E|T])(\\d+)");
		List<String> resolvedIds = new ArrayList<>();
		Map<String,String> map = new HashMap<>();
		List<String> outLines = new ArrayList<>();
		while (resolvedIds.size() < evs.size()) {
			for (String e: evs) {
				String eId = e.substring(0,e.indexOf('\t'));
				String eArgs = e.substring(e.indexOf('\t')+1);
				if (resolvedIds.contains(eId)) continue;
				Matcher m = argPat.matcher(eArgs);
				boolean resolved = true;
				while (m.find()) {
					if (m.group(1).equals("E")) {
						String argId = "E" + m.group(2);
						if (resolvedIds.contains(argId) == false) {
							resolved = false;
						}
					}
				}
				if (resolved) {
					resolvedIds.add(eId);
					map.put(eId, e);
				}
			}
		}
		for (String id: resolvedIds) {
			outLines.add(map.get(id));
		}
		return outLines;	
	}
		
	/**
	 * Parses annotation objects from their string representation. The output is a <code>
	 * Map</code> object, where the keys are class objects for annotation types and the values
	 * are lists of objects belonging to that class. <p>
	 * This method currently handles Term, Relation, Event, Modification, and Reference annotations.
	 * 
	 * @param docId  the document identifier
	 * @param lines  a <code>Map</code> of lines, returned by {@code #readAnnotationFiles(List, List)}. 
	 * @param ignoreArgTypes  the argument types to ignore, if any, for non-atomic semantic objects
	 * 
	 * @return the <code>Map</code> object of annotation class/annotation objects pairs
	 */
	public static Map<Class,List<Annotation>> parseAnnotations(String docId, Map<AnnotationType,List<String>> lines, List<String> ignoreArgTypes) {
		Map<Class,List<Annotation>> annotations = new HashMap<>();
		annotationIdMap = new HashMap<>();
		List<String> termLines = lines.get(AnnotationType.Term);
		if (termLines != null) {
			for (String term: termLines) {
				TermAnnotation t = readTermLine(docId, term);
				if (t == null) {
					log.log(Level.WARNING, "Unable to parse the term annotation line: {0}.", term);
					continue;
				}	
				List<Annotation> termAnns = annotations.get(TermAnnotation.class);
				if (termAnns == null) {
					termAnns = new ArrayList<>();
				}
				termAnns.add(t);
				annotations.put(TermAnnotation.class, termAnns);
				annotationIdMap.put(t.getId(), t);
			}
		}
		List<String> refLines = lines.get(AnnotationType.Reference);
		if (refLines != null) {
			for (String ref : refLines) {
				readReferenceLine(docId,ref);	
			}
		}
		List<String> relLines = lines.get(AnnotationType.Relation);
		if (relLines != null) {
			for (String rel: relLines) {
				RelationAnnotation r = readRelationLine(docId, rel, ignoreArgTypes);
				// one of the arguments is invalid
				if (r == null) {
					log.log(Level.WARNING, "Unable to parse the relation annotation line: {0}.", rel);
					continue;
				}	
				List<Annotation> relAnns = annotations.get(RelationAnnotation.class);
				if (relAnns == null) {
					relAnns = new ArrayList<>();
				}
				relAnns.add(r);
				annotations.put(RelationAnnotation.class, relAnns);
				annotationIdMap.put(r.getId(), r);
			}
		}
		List<String> eventLines = lines.get(AnnotationType.Event);
		if (eventLines != null) {
			for (String ev: eventLines) {
				EventAnnotation e = readEventLine(docId, ev, ignoreArgTypes);
				if (e == null) {
					log.log(Level.WARNING, "Unable to parse the event annotation line: {0}.", ev);
					continue;
				}
				List<Annotation> evAnns = annotations.get(EventAnnotation.class);
				if (evAnns == null) {
					evAnns = new ArrayList<>();
				}
				evAnns.add(e);
				annotations.put(EventAnnotation.class, evAnns);
				annotationIdMap.put(e.getId(), e);
			}
		}
		List<String> modLines = lines.get(AnnotationType.Modification);
		if (modLines != null) {
			for (String mod: modLines) {
				ModificationAnnotation e = readModificationLine(docId, mod, ignoreArgTypes);
				if (mod == null) {
					log.log(Level.WARNING, "Unable to parse the modification annotation line: {0}.", mod);
					continue;
				}	
				List<Annotation> modAnns = annotations.get(ModificationAnnotation.class);
				if (modAnns == null) {
					modAnns = new ArrayList<>();
				}
				modAnns.add(e);
				annotations.put(ModificationAnnotation.class, modAnns);
				annotationIdMap.put(e.getId(), e);
			}
		}
		return annotations;
	}
	
	private static String getSemType(String line) {
 		String[] tabbedStrs = line.split("[\t]");	
    	String[] els = tabbedStrs[1].split(FIELD_DELIMITER);
    	int ind = els[0].indexOf(':');
    	if (ind > 0) return els[0].substring(0,ind);
    	return els[0];
	}
	
	/**
	 * Instantiates a <code>TermAnnotation</code> from a standoff annotation string.
	 * 
	 * @param docId	the document id
	 * @param line  the line corresponding to the term annotation
	 * @return  a <code>TermAnnotation</code> object
	 */
	protected static TermAnnotation readTermLine(String docId, String line) {
		log.log(Level.FINEST, "Reading term line: {0}.", line);
 		String[] tabbedStrs = line.split("[\t]");	
		String id = tabbedStrs[0];
		String semSpan = tabbedStrs[1];
    	String[] els = semSpan.split(FIELD_DELIMITER);
    	String sem = els[0];
    	String spStr = semSpan.substring(semSpan.indexOf(sem)+sem.length()+1);
    	SpanList sp = new SpanList(spStr,';',' ');
		String text = tabbedStrs[2].trim();
    	TermAnnotation term = new TermAnnotation(id,docId,sem,sp,text);
    	return term;
	}
	
	 /**
	  * Creates a <code>RelationAnnotation</code> instance from the corresponding
	  * standoff annotation string.
	  * 
	  * @param docId	the document id
	  * @param line  	the relation annotation line
	  * @param ignoreArgTypes  argument types to ignore
	  * 
	  * @return  a <code>RelationAnnotation</code> object, or null if there are unresolved or invalid arguments
	  */
	 protected static RelationAnnotation readRelationLine(String docId, String line, List<String> ignoreArgTypes) {
		 log.log(Level.FINEST, "Reading relation line: {0}.", line);
		 String[] tabbedStrs = line.split("[\t]");	
		 String id = tabbedStrs[0];
		 String[] els = tabbedStrs[1].split(FIELD_DELIMITER);
		 String sem = els[0];
		 List<AnnotationArgument> args = new ArrayList<AnnotationArgument>(els.length-1);
		 for (int i=1; i < els.length; i++) {
			 String name = els[i].substring(0,els[i].indexOf(':'));
			 if (ignoreArgTypes != null && ignoreArgTypes.contains(name)) continue;
			 String argId = els[i].substring(els[i].indexOf(':')+1);
			 // no id given, skip
			 if (name.length() ==0 || argId.length() == 0) continue;
			 Annotation ann = annotationIdMap.get(argId);
			 if (ann == null) {
				 log.log(Level.SEVERE,"The relation annotation has an unresolved argument: {0}.", argId);
				 return null;
			 }
			 args.add(new AnnotationArgument(name,ann));
		 }
		 if (args.size() == 0) {
			 log.log(Level.SEVERE,"The relation annotation has no valid argument: {0}.", line);
			 return null;
		 }	
		 RelationAnnotation rel = new RelationAnnotation(id, docId, sem, args);
		 return rel;
	 }
	 
	 /**
	  * Creates a <code>EventAnnotation</code> instance from the corresponding
	  * standoff annotation string.
	  * 
	  * @param docId	the document id
	  * @param line 	the event annotation line
	  * @param ignoreArgTypes  argument types to ignore
	  * 
	  * @return the <code>EventAnnotation</code> object, or null if there are unresolved/invalid arguments
	  */
	 protected static EventAnnotation readEventLine(String docId, String line, List<String> ignoreArgTypes) {
		 log.log(Level.FINEST, "Reading event line: {0}.", new Object[]{line});
		 List<String> roleTerms = new ArrayList<>();
		 String[] tabbedStrs = line.split("[\t]");	
		 String id = tabbedStrs[0];
		 String[] els = tabbedStrs[1].split(FIELD_DELIMITER);
		 int colonIndex = els[0].indexOf(':');
		 String sem = els[0];
		 String predicateId = null;
		 if (colonIndex > 0) {
			 sem = els[0].substring(0,els[0].indexOf(':'));
			 predicateId = els[0].substring(els[0].indexOf(':')+1);
		 }
    	TermAnnotation pred  = (TermAnnotation) annotationIdMap.get(predicateId);
    	List<AnnotationArgument> args = new ArrayList<>(els.length-1);
    	for (int i=1; i < els.length; i++) {
	    	String name = els[i].substring(0,els[i].indexOf(':'));
	    	if (ignoreArgTypes != null && ignoreArgTypes.contains(name)) continue;
	    	String argId = els[i].substring(els[i].indexOf(':')+1);
	    	// a bit dangerous but sometimes for some reason Theme2 exists but not Theme.
	    	name = name.replaceAll("[0-9]*$", "");
	    	// no id given, skip
	    	if (name.length() ==0 || argId.length() == 0) continue;
//		    if (equivIds.containsKey(argId)) argId = equivIds.get(argId);
	    	String roleTerm = name + "_" + argId;
	    	if (roleTerms.contains(roleTerm)) continue;
	    	Annotation ann = annotationIdMap.get(argId);
	    	if (ann == null) {
				 log.log(Level.SEVERE,"The event annotation has an unresolved argument: {0}.", argId);
	    		return null;
	    	}
	    	args.add(new AnnotationArgument(name,ann));
	    	roleTerms.add(roleTerm);
    	}
    	if (args.size() == 0) {
			 log.log(Level.SEVERE,"The event annotation has no valid argument: {0}.", line);
    	}
    	EventAnnotation ev = new EventAnnotation(id,docId,sem,args,pred);
    	return ev;
	}
	 
	 /**
	  * Creates a <code>PredicationAnnotation</code> instance from the corresponding
	  * standoff annotation string.
	  * 
	  * @param docId	the document id
	  * @param line 	the event annotation line
	  * @param ignoreArgTypes  argument types to ignore
	  * 
	  * @return the <code>PredicationArgument</code> object, or null if there are unresolved/invalid arguments
	  */
	 // TODO Unused, untested.
	 protected static PredicationAnnotation readPredicationLine(String docId, String line, List<String> ignoreArgTypes) {
		 log.log(Level.FINEST, "Reading predication line: {0}.", new Object[]{line});
		 List<String> roleTerms = new ArrayList<>();
		 String[] tabbedStrs = line.split("[\t]");	
		 String id = tabbedStrs[0];
		 String[] els = tabbedStrs[1].split(FIELD_DELIMITER);
		 int colonIndex = els[0].indexOf(':');
		 String sem = els[0];
		 String predicateId = null;
		 if (colonIndex > 0) {
			 sem = els[0].substring(0,els[0].indexOf(':'));
			 predicateId = els[0].substring(els[0].indexOf(':')+1);
		 }
    	TermAnnotation pred  = (TermAnnotation) annotationIdMap.get(predicateId);
    	List<AnnotationArgument> args = new ArrayList<>(els.length-1);
    	for (int i=1; i < els.length; i++) {
	    	String name = els[i].substring(0,els[i].indexOf(':'));
	    	if (ignoreArgTypes != null && ignoreArgTypes.contains(name)) continue;
	    	String argId = els[i].substring(els[i].indexOf(':')+1);
	    	// a bit dangerous but sometimes for some reason Theme2 exists but not Theme.
	    	name = name.replaceAll("[0-9]*$", "");
	    	// no id given, skip
	    	if (name.length() ==0 || argId.length() == 0) continue;
//		    if (equivIds.containsKey(argId)) argId = equivIds.get(argId);
	    	String roleTerm = name + "_" + argId;
	    	if (roleTerms.contains(roleTerm)) continue;
	    	Annotation ann = annotationIdMap.get(argId);
	    	if (ann == null) {
				 log.log(Level.SEVERE,"The predication annotation has an unresolved argument: {0}.", argId);
	    		return null;
	    	}
	    	args.add(new AnnotationArgument(name,ann));
	    	roleTerms.add(roleTerm);
    	}
    	if (args.size() == 0) {
			 log.log(Level.SEVERE,"The predication annotation has no valid argument: {0}.", line);
    	}
    	PredicationAnnotation predication = new PredicationAnnotation(id,docId,sem,args,pred);
    	if (tabbedStrs.length > 2) {
    		String scale = tabbedStrs[2].substring(tabbedStrs[2].lastIndexOf(':')+1,tabbedStrs[2].lastIndexOf('_'));
    		double value = Double.parseDouble(tabbedStrs[2].substring(tabbedStrs[2].lastIndexOf('_')+1));   	
    		predication.setScale(scale); predication.setValue(value);
    	} 
    	return predication;
	}
			  

	/**
	 * Instantiates a <code>EventModificationAnnotation</code> from a standoff annotation string.<p>
	 * It currently expects [ID]tab[MOD_TYPE] [EVENT_ID] [VALUE] format, where VALUE is optional.
	 * 
	 * @param docId				the document id
	 * @param line  			the line corresponding to the modification annotation
	 * @param ignoreArgTypes	argument types to ignore if any
	 * @return  a <code>EventModificationAnnotation</code> object, or null for a line with unexpected format
	 */
	// TODO Not thoroughly tested.
	 protected static ModificationAnnotation readModificationLine(String docId, String line, List<String> ignoreArgTypes) {
		log.log(Level.FINEST, "Reading modification line: {0}.", new Object[]{line});
		String[] tabbedStrs = line.split("[\t]");	
	 	String id = tabbedStrs[0];
	 	String[] els = tabbedStrs[1].split(FIELD_DELIMITER);
//    	String[] els = tabbedStrs[1].split("[:]");
    	String sem = els[0];
    	// there should really be just one event
    	if (els.length < 2) return null;
    	String semId = els[1];
    	if (semId.length() == 0) return null;
	    Annotation ann = annotationIdMap.get(semId);
	    if (ann == null) {
		    log.log(Level.SEVERE,"The modification annotation has an unresolved argument: {0}.", semId);
		    return null;
    	}
	    String value = null;
	    if (els.length > 2) {
//	    if (tabbedStrs.length > 2) {
//	    	value = els[2];
	    	value = tabbedStrs[1].substring(tabbedStrs[1].indexOf(els[2]));
//	    	value = tabbedStrs[2];
	    } 
	    if (value == null)
	    	return new ModificationAnnotation(id,docId,sem,ann);
	    else 
	    	return new ModificationAnnotation(id,docId,sem,ann,value);
	}
	 
	/**
	 * Adds normalization information from a standoff annotation to the corresponding <code>TermAnnotation</code> object.<p>
	 * Currently, it assumes the source for normalization information is UMLS.
	 * 
	 * @param docId	the document id
	 * @param line  the line corresponding to the reference annotation
	 */
	 // TODO Not thoroughly tested.	 
	 protected static void readReferenceLine(String docId, String line) {	
		 log.log(Level.FINEST, "Reading reference line: {0}.", new Object[]{line});
		 String[] tabbedStrs = line.split("[\t]");	
//		 String id = tabbedStrs[0];
		 String[] els = tabbedStrs[1].split(FIELD_DELIMITER);
		 if (els.length < 3) return;
		 String termId = els[1];
		 if (termId.length() == 0) return;
		 Annotation ann = annotationIdMap.get(termId);
		 if (ann == null) {
			 log.log(Level.SEVERE,"The reference annotation has an unresolved term argument: {0}.", termId);
			 return;
		 }
		 String value = els[2];
		 TermAnnotation term = (TermAnnotation)ann;
		 term.addReference(new Reference("UMLS",value));
	 }
	 
	 public static void main(String[] args) {
		String id = args[0];
		String textFile = args[1];
		String annFile = args[2];
		Document doc = readTextFile(id,textFile);
		Map<AnnotationType,List<String>> annotationLines = readAnnotationFiles(Arrays.asList(annFile), null,null);
		Map<Class,List<Annotation>> annotations = parseAnnotations(doc.getId(),annotationLines,null);
		for (Class c: annotations.keySet()) {
			List<Annotation> anns = annotations.get(c);
			for (Annotation ann: anns) {
				log.log(Level.INFO,"Read {0} annotation: {1}.", new Object[]{c.getName(),ann.toString()});
			}
		}
	 }
}
