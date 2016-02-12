package gov.nih.nlm.ling.sem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.nlp.util.StringUtils;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;

/**
 * A basic implementation of an abstract, ontological concept without textual grounding.
 * A concept consists of a name, a set of semantic types and a source. Instances of 
 * {@link Entity} (which are textually grounded) can be associated with multiple concepts.<p>
 * 
 * This class also can maintain a static concept hierarchy (provided such a hierarchy exists), 
 * which can be loaded from a file. The concept hierarchy structure is quite rudimentary 
 * at the moment: each hierarchical relation is encoded as a string of the 
 * form <i>[conceptId1]|[conceptId2]</i>, where <i>conceptId1</i> is a
 * descendant of <i>conceptId2</i>.
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO: Make this class fully compatible with the UMLS notion of Concept, we need to expand 
// it significantly (synonyms, atoms, etc.)
public class Concept implements Ontology {
	private static final Logger log = Logger.getLogger(Concept.class.getName());
	
	// generic attribution values
	public static final Concept GENERIC = 
			new Concept("C000","GENERIC", new LinkedHashSet<String>(Arrays.asList("GENERIC")));
	public static final Concept SOURCE_GENERIC = 
			new Concept("C001","SOURCE_GENERIC", new LinkedHashSet<String>(Arrays.asList("SOURCE_GENERIC")));
	public static final Concept SOURCE_WRITER = 
			new Concept("C002","SOURCE_WRITER", new LinkedHashSet<String>(Arrays.asList("SOURCE_WRITER")));


	private String id;
	private String name;
	private LinkedHashSet<String> semtypes;
	private String source;
	
	public static List<String> conceptHierarchy = null;
	
	/**
	 * Instantiates a <code>Concept</code> with name and semantic types.
	 * 
	 * @param id		the identifier 
	 * @param name		the preferred name
	 * @param semtypes	the semantic types for the concept
	 * 
	 */
	public Concept(String id, String name, LinkedHashSet<String> semtypes) {
		this.id = id;
		this.name = name;
		this.semtypes = semtypes;
	}
	
	/**
	 * Instantiates a <code>Concept</code> with name, semantic types and source.
	 * 
	 * @param id		the identifier 
	 * @param name		the preferred name
	 * @param semtypes	the semantic types for the concept
	 * @param source	the source name of the concept
	 */
	public Concept(String id, String name, LinkedHashSet<String> semtypes, String source) {
		this(id,name,semtypes);
		this.source = source;
	}
	
	public Concept(Element el) {
		String concId = el.getAttributeValue("id");
		// HACK: in case it has the form Metathesaurus:C0012345
		if (concId.contains(":")) concId = concId.substring(concId.indexOf(":")+1);
		String name = el.getAttributeValue("name");	
		LinkedHashSet<String> semtypes = new LinkedHashSet<String>(Arrays.asList(el.getAttributeValue("semtypes").split(",")));
		this.id= concId;
		this.name = name;
		this.semtypes = semtypes;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public LinkedHashSet<String> getSemtypes() {
		return semtypes;
	}

	public String getSource() {
		return source;
	}
	
	public void setSource(String s) {
		this.source = s;
	}	
	
	/**
	 * Gets the concept hierarchy or initializes it, if necessary.
	 * 
	 * @param filename	the file containing the hierarchical relations
	 * @return  		the concept hierarchy list
	 */
	public static List<String> getConceptHierarchy(String filename) {
		if (conceptHierarchy == null) {
			loadHierarchy(filename);
		} 
		return conceptHierarchy;
	}
	
	public static List<String> getConceptHierarchy() {
		return conceptHierarchy;
	}
	
	/**
	 * 
	 * @param id1  the first concept id
	 * @param id2  the second concept id
	 * @return true if the second concept is an ancestor of the first
	 */
	public static boolean hierarchical(String id1, String id2) {
		if (conceptHierarchy == null) {
			log.log(Level.SEVERE, "Concept hierarchy is not initialized. Unable to determine hierarchical relation.");
			return false;
		}
		return conceptHierarchy.contains(id1+"|" +id2);
	}
	
	/**
	 * Reads the hierarchical relations between concepts from a file.
	 * We currently assume each line consists of simple concatenation of two concept ids.
	 * [ID1][ID2], indicating that ID1 is a descendant of ID2. 
	 * 
	 * @param filename  the file containing the hierarchical relations
	 */
	// TODO: This is quite simplistic, need to be able to work large
	// hierarchical files, a la UMLS
	public static void loadHierarchy(String filename)  {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(filename));
			conceptHierarchy = new ArrayList<String>();
			String line = null;
			while ((line = br.readLine()) != null) {
				conceptHierarchy.add(line);	
			}
			log.log(Level.FINEST,"Number of relations: {0}", new Object[]{conceptHierarchy.size()});
			br.close();
		} catch (IOException ioe) {
			log.log(Level.WARNING,"Cannot read hierarchy file {0}: {1}", new Object[]{filename,ioe.getMessage()});
			ioe.printStackTrace();
		} 
	}
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		Concept c = (Concept)obj;
		if (c.getId().equals(id)) return true;
		return false;
	}
	
	public int hashCode() {
	    return (id == null ? 119 : id.hashCode());
	}
	
	public String toString() {
		return id + "_" + name + "_" + semtypes.toString();
	}
	
	/**
	 * Reads all concepts associated with an <code>Entity</code> from XML.
	 * 
	 * @param el		the XML element corresponding to the entity
	 * @param entity	the entity with the concepts
	 */
	public static void assignConceptsFromXml(Element el, Entity entity) {
		LinkedHashSet<Concept> concs = new LinkedHashSet<>();
		Concept sense = null;
		Elements conceptEls = el.getChildElements("Concept");
//		String modSem = entity.getType();
		if (conceptEls.size() > 0){
			for (int i =0; i < conceptEls.size(); i++) {
				Element concEl = conceptEls.get(i);
				Concept conc = new Concept(concEl);
				boolean thisSense = Boolean.parseBoolean(concEl.getAttributeValue("sense"));
				concs.add(conc);
				if (thisSense)  {
					sense = conc;
//					modSem = conc.getSemtypes().iterator().next();
				}
			} 
			if (concs.size() == 0)  {
				log.log(Level.WARNING,"Unable to parse concepts from XML: {0}.", new Object[]{el.toXML()}); 
				return;
			}
		}
		if (concs.size() > 0) {
			entity.setConcepts(concs);
			if (sense == null) 
				sense = concs.iterator().next();
			entity.setSense(sense);
		}
	}
	
	
	public Element toXml(boolean sense) {
		Element el = new Element("Concept");
		el.addAttribute(new Attribute("id",id));
		if (sense) el.addAttribute(new Attribute("sense","true"));
		else el.addAttribute(new Attribute("sense","false"));
		el.addAttribute(new Attribute("name",name));
		el.addAttribute(new Attribute("semtypes",StringUtils.join(semtypes.toArray(new String[0]),",")));
		return el;
	}
}
