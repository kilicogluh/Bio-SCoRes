package tasks.coref.spl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.core.CoreferenceSemanticItemFactory;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.io.XMLCoreferenceChainReader;
import gov.nih.nlm.bioscores.io.XMLExpressionReader;
import gov.nih.nlm.ling.brat.Annotation;
import gov.nih.nlm.ling.brat.Reference;
import gov.nih.nlm.ling.brat.StandoffAnnotationReader;
import gov.nih.nlm.ling.brat.StandoffAnnotationWriter;
import gov.nih.nlm.ling.brat.TermAnnotation;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.io.XMLEntityReader;
import gov.nih.nlm.ling.io.XMLReader;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import gov.nih.nlm.ling.sem.Term;
import gov.nih.nlm.ling.util.FileUtils;
import nu.xom.Element;
import nu.xom.Serializer;

/**
 * Utility class to add new standoff annotations to an existing XML file. The annotations 
 * to add in our case are MetaMap annotations.
 * 
 * @author Halil Kilicoglu
 *
 */
public class AddTermAnnotationsToXML {
	private static Logger log = Logger.getLogger(AddTermAnnotationsToXML.class.getName());
	
	/**
	 * Adds term annotations to a document.
	 * 
	 * @param doc			the document to add annotations to
	 * @param annFilename	the name of the file with annotations
	 * @return	XML representation of the document after adding annotations.
	 */
	public static Element processSingleFile(Document doc, String annFilename) {
		Map<String,List<String>> lines = StandoffAnnotationReader.readAnnotationFiles(Arrays.asList(annFilename), Constants.UMLS_SEMTYPES);
		Map<Class,List<Annotation>> annotations = StandoffAnnotationReader.parseAnnotations(doc.getId(), lines, null);
		SemanticItemFactory sif = doc.getSemanticItemFactory();	
		Class[] processOrder = new Class[]{TermAnnotation.class,Reference.class};
		for (int i=0; i < processOrder.length; i++) {
			Class c = processOrder[i];
			List<Annotation> anns = annotations.get(c);
			if (anns == null) continue;
			for (Annotation ann: anns) {
				if (ann instanceof TermAnnotation) {
					Entity ent = sif.newEntity(doc, (TermAnnotation)ann);
					int maxId = doc.getMaxId(Term.class);
					ent.setId("T" + ++maxId);
				}
			}	
		}
		return doc.toXml();
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length < 4) {
			System.err.print("Usage: inputXMLDirectory inputAnnotationDirectory outputXMLDirectory outputAnnotationDirectory");
		}
		String xmlIn = args[0];
		String annIn = args[1];
		String out = args[2];
		String annOut = args[3];
		File inDir = new File(xmlIn);
		File annDir = new File(annIn);
		if (inDir.isDirectory() == false) {
			System.err.println("inputXMLDirectory is not a directory.");
			System.exit(1);
		}
		if (annDir.isDirectory() == false) {
			System.err.println("inputXMLDirectory is not a directory.");
			System.exit(1);
		}
		File outDir = new File(out);
		if (outDir.isDirectory() == false) {
			System.err.println("The directory " + out + " doesn't exist. Creating a new directory..");
			outDir.mkdir();
		}
		File annOutDir = new File(annOut);
		if (annOutDir.isDirectory() == false) {
			System.err.println("The directory " + annOut + " doesn't exist. Creating a new directory..");
			annOutDir.mkdir();
		}
		Map<Class<? extends SemanticItem>,List<String>> annTypes = new HashMap<>();
		annTypes.put(Entity.class,Constants.ENTITY_TYPES);
		annTypes.put(Expression.class, Constants.EXP_TYPES);
		annTypes.put(Relation.class, Constants.COREFERENCE_TYPES);
		XMLReader reader = new XMLReader();
		reader.addAnnotationReader(Entity.class, new XMLEntityReader());
		reader.addAnnotationReader(Expression.class,new XMLExpressionReader());
		reader.addAnnotationReader(Relation.class,new XMLCoreferenceChainReader());
			
		List<String> files = FileUtils.listFiles(xmlIn, false, "xml");
		int fileNum = 0;
		for (String filename: files) {
			String filenameNoExt = filename.replace(".xml", "");
			filenameNoExt = filenameNoExt.substring(filenameNoExt.lastIndexOf(File.separator)+1);
			log.info("Processing " + filenameNoExt + ":" + ++fileNum);
			if (filenameNoExt.startsWith("0d9")) continue;
			String annFilename = annDir.getAbsolutePath() + File.separator + filenameNoExt + ".ann";
			String outFilename = outDir.getAbsolutePath() + File.separator + filenameNoExt + ".xml";
			String annOutFilename = annOutDir.getAbsolutePath() + File.separator + filenameNoExt + ".ann";
			Document doc = reader.load(filename, true, CoreferenceSemanticItemFactory.class,annTypes, null);
			Element docEl = processSingleFile(doc, annFilename);
			nu.xom.Document xmlDoc = new nu.xom.Document(docEl);
			Serializer serializer = new Serializer(new FileOutputStream(outFilename));
			serializer.setIndent(4);
			serializer.write(xmlDoc); 
			StandoffAnnotationWriter.write(doc, annOutFilename);
		}
	}	
}
