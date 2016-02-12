package gov.nih.nlm.ling.io;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.ImplicitRelation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import gov.nih.nlm.ling.sem.Term;
import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.Elements;

/**
 * A reader to parse syntactic/semantic content of a <code>Document</code> from 
 * an XML file. The XML file is expected to conform to the format used for the BioNLP shared task, below.
 * 
 * <pre>
 * {@code
 * 	<document id="D1">
 * 		<text>Involvement ... (rest of the document)</text>
 * 		<sentence id="S1" charOffset="0-100">
 * 			<text>Involvement ... (rest of the sentence)</text>
 * 			<tokens tokenizer="" parser="">
 * 				<token id="S1T1" POS="NN" charOffset="0-11" text="Involvement" lemma="involvement"/>
 * 				... (Other tokens)
 * 			</tokens> 
 * 			<dependencies>
 * 				<dependency id="STD1" t1="S1T1" t2="S1T3" type="prep_of"/>
 * 				... (Other dependencies)
 * 			</dependencies>
 * 			<tree>(ROOT ...)</tree>
 * 		</sentence>
 * 		.... (Other sentences)
 * 		<Term id="T1" type="Protein" charOffset="37-50" headOffset="45-50" text="p70(s6)-kinase"/>
 *      ... (Other terms and assorted semantic items)
 * </document>
 * }
 * </pre>
 * 
 * @author Halil Kilicoglu
 *
 */
public class XMLReader {
  private static final Logger log = Logger.getLogger(XMLReader.class.getName());

  private List<XMLSemanticItemReader> readers = new ArrayList<XMLSemanticItemReader>();
  private Map<Class<? extends SemanticItem>,XMLSemanticItemReader> readerMap = new LinkedHashMap<>();
//  private Map<SemanticItem,Set<SemanticItem>> associations = new LinkedHashMap<>();
  private Map<SpanType,Term> termSpans = new LinkedHashMap<>();
//  private Map<String,String> equivIds = new LinkedHashMap<>();
  private Map<String,SemanticItem> equivMap = new LinkedHashMap<>();
	
  private static class SpanType {
	  private SpanList span;
	  private String type;
		
	  private SpanType(SpanList sp, String t) {
		  this.span = sp;
		  this.type = t;
	  }
	  private SpanList getSpan() {
		  return span;
	  }
	  private String getType() {
		  return type;
	  }
	  public boolean equals(Object o) {
		  if (o instanceof SpanType == false) return false;
		  SpanType st = (SpanType)o;
		  return st.getSpan().equals(span) && st.getType().equals(type);
	  }
	  public int hashCode(){
		return 137 * span.hashCode() ^ 143 * type.hashCode();
	  }
	}
  
  /**
   * Creates a new <code>XMLDocumentReader</code> with no readers.
   */
  public XMLReader() {}
  
  /**
   * Creates a new <code>XMLDocumentReader</code> with the given
   * {@link XMLSemanticItemReader}s.
   * 
   *  @param readers	the semantic annotation readers to use.
   */
   public XMLReader(List<XMLSemanticItemReader> readers) {
	   readers.addAll(readers);
   }

  /**
   * Adds an {@link XMLSemanticItemReader} to this <code>XMLDocumentReader</code>.
   * 
   * @param reader	the semantic annotation reader to use.
   */
   public void addAnnotationReader(XMLSemanticItemReader reader) {
	   readers.add(reader);
   }

  /**
   * Adds an {@link XMLSemanticItemReader} for writing out the given <var>clazz</var>.
   * 
   * @param clazz		the semantic annotation class
   * @param annReader	the associated annotation reader
   */
   public void addAnnotationReader(Class<? extends SemanticItem> clazz, XMLSemanticItemReader annReader) {
	   readerMap.put(clazz, annReader);
   }
  
	/**
	 * Loads the lexical/syntactic content from an XML file. 
	 * 
	 * @param fileName   the XML file name
	 * 
	 * @return a <code>Document</code> object represented by the XML file, null if it can not be parsed
	 */
	public Document load(String fileName) {
		return load(fileName,null,null,false,null,null,null);
	}
	
  
	/**
	 * Loads the content from an XML file. It assumes the file contains
	 * one set of tokenization/parsing output. If <var>parseSemantics</var>
	 * is set to false, the <var>annotationTypes</var> variable has no effect.
	 * 
	 * @param fileName			the XML file name
	 * @param parseSemantics	whether to parse the semantic items
	 * @param sifClass			the semantic item factory class
	 * @param annotationTypes  	a <code>Map</code> with the semantic class/type information to parse
	 * @param ignoreArgTypes  	the argument types to ignore (for relations, events)
	 * 
	 * @return a <code>Document</code> object represented by the XML file, null if it can not be parsed
	 */
	public Document load(String fileName, boolean parseSemantics, Class<? extends SemanticItemFactory> sifClass,
								Map<Class<? extends SemanticItem>,List<String>> annotationTypes, 
								Set<String> ignoreArgTypes) {
		return load(fileName,null,null,parseSemantics,sifClass,annotationTypes,ignoreArgTypes);
	}
	
	/**
	 * Similar to {@code #load(String, boolean, Class, Map, Set)}, but the tokenizer/parser output to use for 
	 * further processing can be selected. <var>tokenizerName</var> is expected as the 'tokenizerName' 
	 * attribute of the 'tokens' element. <var>parserName</var> is expected as the 'parserName' 
	 * attribute of the 'tree' element.
	 * 
	 * @param fileName			the XML file name
	 * @param tokenizerName		the name of the tokenizer to use
	 * @param parserName  		the name of the parser to use
	 * @param parseSemantics  	whether to parse semantic objects
	 * @param sifClass			the semantic item factory class
	 * @param annotationTypes  	the map of semantic class/types to parse
	 * @param ignoreArgTypes  	the argument types to ignore (such as Cue)
	 * 
	 * @return  the <code>Document</code> representation, null if it can not be parsed
	 */
	public Document load(String fileName, String tokenizerName, String parserName, 
			boolean parseSemantics, Class<? extends SemanticItemFactory> sifClass, 
			Map<Class<? extends SemanticItem>,List<String>> annotationTypes, Set<String> ignoreArgTypes) {
		return load(fileName,tokenizerName,parserName,parseSemantics,sifClass,null,annotationTypes,ignoreArgTypes);
	}
	
	/**
	 * Similar to {@code #load(String, String, String, boolean, Class, String, Map, Set)}, but also the spans to process for 
	 * semantics (i.e., zones) can be specified. The portions of the document outside the zones will be ignored 
	 * for semantics. 
	 * 
	 * @param fileName			the XML file name
	 * @param tokenizerName		the name of the tokenizer to use
	 * @param parserName		the name of the parser to use
	 * @param parseSemantics	whether to parse semantic objects
	 * @param sifClass			the semantic item factory class
	 * @param spansToProcess 	the span list specifying the zones
	 * @param annotationTypes 	the map of semantic class/types to parse
	 * @param ignoreArgTypes  	the argument types to ignore (such as Cue)
	 * 
	 * @return  the <code>Document</code> representation, null if it can not be parsed
	 */
	public Document load(String fileName, String tokenizerName, String parserName, 
			boolean parseSemantics, Class<? extends SemanticItemFactory> sifClass, SpanList spansToProcess,
			Map<Class<? extends SemanticItem>,List<String>> annotationTypes, Set<String> ignoreArgTypes) {
		Document doc = null;
		try { 
			Builder builder = new Builder();
			log.info("Loading XML file " + fileName);
			nu.xom.Document xmlDoc = builder.build(new FileInputStream(fileName));
			Element docc = xmlDoc.getRootElement();
			doc = new Document(docc,tokenizerName,parserName);
			if (sifClass != null) {
				SemanticItemFactory sif = sifClass.getConstructor(Document.class,Map.class).newInstance(doc,new HashMap<>());
				doc.setSemanticItemFactory(sif);
			}
			if (parseSemantics) parseSemanticItems(doc,spansToProcess,docc,annotationTypes, ignoreArgTypes);
		}  catch (Exception e) {
			log.log(Level.SEVERE, "Unable load the XML file {0}.", new Object[]{fileName});
			e.printStackTrace();
		}
		return doc;
	}
	
	/**
	 * Similar to {@code #load(String, boolean, String, String, Map, List)}, but also the spans to process for 
	 * semantics (i.e., zones) can be specified. The portions of the document outside the zones will be ignored 
	 * for semantics. 
	 * 
	 * @param fileName			the XML file name
	 * @param spansToProcess	the span list specifying the zones
	 * @param tokenizerName 	the name of the tokenizer to use
	 * @param parserName  		the name of the parser to use
	 * @param sif				the semantic item factory 
	 * @param annotationTypes  	the map of semantic class/types to parse
	 * @param ignoreArgTypes  	the argument types to ignore (such as Cue)
	 * 
	 * @return  the <code>Document</code> representation, null if it can not be parsed
	 */
	public Document load(String fileName, SpanList spansToProcess,
			String tokenizerName, String parserName, SemanticItemFactory sif,
			Map<Class<? extends SemanticItem>,List<String>> annotationTypes, Set<String> ignoreArgTypes) {
		Document doc = null;
		try { 
			Builder builder = new Builder();
			log.info("Loading XML file " + fileName);
			nu.xom.Document xmlDoc = builder.build(new FileInputStream(fileName));
			Element docc = xmlDoc.getRootElement();
			doc = new Document(docc,tokenizerName,parserName);
			doc.setSemanticItemFactory(sif);
			parseSemanticItems(doc,spansToProcess,docc,annotationTypes, ignoreArgTypes);
		}  catch (Exception e) {
			log.log(Level.SEVERE, "Unable load the XML file {0}.", new Object[]{fileName});
			e.printStackTrace();
		}
		return doc;
	}
	
	// Returns true if the given span is in a zone that is annotated.
	// To ensure that we are processing a part of the document that has been annotated.
	// This was a hack to allow parsing only partially annotated documents.
	private static boolean inSpan(SpanList sp, SpanList spansToProcess) {
		int startInd = sp.getBegin();
		int endInd = sp.getEnd();
		for (Span pp : spansToProcess.getSpans()) {
			int pbeg = pp.getBegin();
			int pend = pp.getEnd();
	    	if (startInd >= pbeg || pend >= endInd) return true;
		}
    	return false;
	}
		
	/**
	 * Parses the semantic objects indicated in the XML file.
	 * If the document will be partially parsed, <var>offset</var> and <var>end</var> variables
	 * need to indicate the character offsets of the part that will be parsed, otherwise 0. 
	 * The <code>Document</code> is updated with the semantic items.
	 * 
	 * @param doc  				the <code>Document</code> with the semantic objects
	 * @param spansToProcess	the span list specifying the zones, if the document will be partially parsed
	 * @param docEl				the XML element representing the individual <code>Document</code> content
	 * @param annotationTypes	the <code>Map</code> of semantic types to parse for each class
	 * @param ignoreArgTypes	the relation argument roles that should be ignored (such as Cue)
	 * 
	 */
	public void parseSemanticItems(Document doc, SpanList spansToProcess, Element docEl, 
				Map<Class<? extends SemanticItem>,List<String>> annotationTypes, Set<String> ignoreArgTypes)  {		
//		associations = new LinkedHashMap<>();
		termSpans = new LinkedHashMap<>();
//		equivIds = new LinkedHashMap<>();
		equivMap = new LinkedHashMap<>();
		
		boolean parseZones = (spansToProcess != null);
		for (Class<? extends SemanticItem> cl: annotationTypes.keySet()) {
			if (readerMap.containsKey(cl) == false) {
				log.log(Level.WARNING, "Unable to find a reader for semantic class {0}, so will skip them.", new Object[]{cl.getCanonicalName()});
			}
		}
		Elements termEls = docEl.getChildElements("Term");
		if (termEls == null) return;
		for (int i=0; i < termEls.size(); i++) {
			Element termEl = termEls.get(i);
			log.log(Level.FINEST,"Term XML: {0}.", new Object[]{termEl.toXML()});
			String id = termEl.getAttributeValue("id");
			String semtype = termEl.getAttributeValue("type");
			log.log(Level.FINEST,"Term semantic type: {0}.", new Object[]{termEl.toXML()});
			SpanList sp = new SpanList(termEl.getAttributeValue("charOffset"));
			log.log(Level.FINEST,"Span : {0}.", new Object[]{sp.toString()});
			if (parseZones && !inSpan(sp,spansToProcess)) continue;
			SpanType spt = new SpanType(sp,semtype);
			// Another term with the same type on the same span, just return the existing one
			if (termSpans.containsKey(spt)) {
				Term existing = termSpans.get(spt);
				if (existing instanceof Entity) {
//					equivIds.put(id, existing.getId());
			   		equivMap.put(id, existing);
					return;
				}
			}
			XMLSemanticItemReader annReader = parseWith(semtype,annotationTypes);
			if (annReader == null) continue;
			Entity ent = (Entity)annReader.read(termEl, doc);
			if (ent != null) termSpans.put(spt, ent);
		}
		Elements relEls = docEl.getChildElements("Relation");
		for (int i=0; i < relEls.size(); i++) {
			Element relEl = relEls.get(i);
			String semtype = relEl.getAttributeValue("type");
			XMLRelationReader annReader = (XMLRelationReader)parseWith(semtype,annotationTypes);
			if (annReader == null) continue;
			annReader.read(relEl,doc,ignoreArgTypes,equivMap);
		}
		Elements evEls = docEl.getChildElements("Event");
		for (int i=0; i < evEls.size(); i++) {
			Element evEl = evEls.get(i);
			String semtype = evEl.getAttributeValue("type");
			XMLRelationReader annReader = (XMLRelationReader)parseWith(semtype,annotationTypes);
			if (annReader == null) continue;
			annReader.read(evEl,doc,ignoreArgTypes,equivMap);
		}
/*		Elements refEls = docEl.getChildElements("Reference");
		Elements equivEls = docEl.getChildElements("Equiv");
		Elements modEls = docEl.getChildElements("Modification");*/
	}
		
	// determines how a given class/semantic type pair needs to be parsed
	private  XMLSemanticItemReader parseWith(String type, 
			Map<Class<? extends SemanticItem>,List<String>> annotationTypes) {
		for (Class<? extends SemanticItem> t: annotationTypes.keySet()) {
			if (annotationTypes.get(t).contains(type)) return readerMap.get(t);
		}
		return null;
	}
	
  public static void main(String[] argv) throws Exception {
		 Map<Class<? extends SemanticItem>,List<String>> annotationTypes = new HashMap<Class<? extends SemanticItem>,List<String>>();
		 annotationTypes.put(Entity.class,Arrays.asList("Drug","Drug_Class","Substance","SPAN",
				 "DefiniteNP", "IndefiniteNP", "ZeroArticleNP", "DemonstrativeNP", "DistributiveNP", 
				 "PersonalPronoun", "PossessivePronoun","DemonstrativePronoun", "DistributivePronoun", 
				 "ReciprocalPronoun", "RelativePronoun", "IndefinitePronoun"));
		 annotationTypes.put(ImplicitRelation.class,Arrays.asList("Anaphora","Cataphora","PredicateNominative","Appositive"));
		 XMLReader reader = new XMLReader();
		 reader.addAnnotationReader(ImplicitRelation.class,new XMLImplicitRelationReader());
		 reader.addAnnotationReader(Entity.class, new XMLEntityReader());
		 Document doc = reader.load("Q:\\Projects\\QA-Watson\\Coreference\\DrugCorefAnnotations\\TRAIN_FINAL\\XML_GOLD\\" +
				 					"0a4b5599-eded-4f96-8a0b-09bf114efd3b.xml",
				 null,null,true,null,annotationTypes,null);
		 
		for (Class<? extends SemanticItem> s: doc.getSemanticItems().keySet()) {
			System.out.println("Annotation type: " + s.getCanonicalName() + "|" + doc.getSemanticItems().get(s).size());
			for (SemanticItem si: doc.getSemanticItems().get(s)) {
				System.out.println("Annotation: " +si.toString());
			}		}
  }

}
