package gov.nih.nlm.ling.sem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.nlp.util.CollectionUtils;
import gov.nih.nlm.ling.composition.EmbeddingCategorization;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.sem.ScalarModalityValue.ScaleType;
import nu.xom.Attribute;
import nu.xom.Element;

/**
 * A generalized predicate-argument structure. In addition to all elements that pertain to
 * an <code>Event</code> objects, a predication includes scalar modality values and a source,
 * as described in Kilicoglu (2012).<p>
 * 
 * @author Halil Kilicoglu
 *
 */
public class Predication extends AbstractRelation implements HasPredicate {
	private static Logger log = Logger.getLogger(Predication.class.getName());
	
	public static final String GENERIC = "GENERIC_PREDICATION";
	
	protected static Set<RelationDefinition> definitions = new HashSet<RelationDefinition>();
	protected Predicate predicate;
	private List<ScalarModalityValue> scalarValues;
	private List<SemanticItem> sources;	
	private String text;
	private boolean resolved = false;
	
	// TODO Not quite complete.
	static {
		definitions = new HashSet<RelationDefinition>();
		Map<Class<? extends SemanticItem>,Set<String>> relOnlyMap = new HashMap<>();
		Map<Class<? extends SemanticItem>,Set<String>> entRelMap = new HashMap<>();
		relOnlyMap.put(Relation.class, new HashSet<String>());
		entRelMap.put(Relation.class, new HashSet<String>());
		entRelMap.put(Entity.class, new HashSet<String>());
		definitions.add(new RelationDefinition("MODAL","Modal",Predication.class,
						Arrays.asList(new RelationArgument("COMP",relOnlyMap,false)),
						Arrays.asList(new RelationArgument("SUBJ",entRelMap,false))));
		definitions.add(new RelationDefinition("VALENCE_SHIFTER","Valence Shifter",Predication.class,
						Arrays.asList(new RelationArgument("COMP",relOnlyMap,false)),
						null));
		definitions.add(new RelationDefinition("RELATIONAL","Relational",Predication.class,
						Arrays.asList(new RelationArgument("COMP",entRelMap,false),new RelationArgument("SUBJ",entRelMap,false)),
						null));
		definitions.add(new RelationDefinition("PROPOSITIONAL","Propositional",Predication.class,
						Arrays.asList(new RelationArgument("COMP",entRelMap,false),new RelationArgument("SUBJ",entRelMap,false)),
						null));
		definitions.add(new RelationDefinition("CONJUNCTION","Conjunction",Predication.class,
						Arrays.asList(new RelationArgument("CC",entRelMap,true)),
						null));
/*		definitions.add(new RelationDefinition("CORRELATIVE","Correlative",Predication.class,
				Arrays.asList(new RelationArgument("COMP",entRelMap,true)),
				null));*/
	}
	
	public Predication(String id) {
		super(id);
	}
	
	/**
	 * Instantiates a <code>Predication</code> that is similar to an <code>Event</code> object,
	 * i.e., without source, modality scalar value.
	 * 
	 * @param id		the predication identifier
	 * @param predicate the predicate of the predication
	 * @param args   	the argument list
	 */
	public Predication(String id, Predicate predicate, List<Argument> args) {
		super(id,Predication.GENERIC);
		this.definition = getDefinition(definitions,type);
		this.predicate = predicate;
		if (predicate != null) {
				this.type = predicate.getType();
				this.definition = getDefinition(definitions,type);
		} 
		this.arguments = args;
		setDefaultScalarValues();
		setDefaultSources();
	}
	
	/**
	 * Instantiates a <code>Predication</code> object with scalar modality values and sources.
	 * 
	 * @param id			the predication identifier
	 * @param predicate  	the predicate of the predication
	 * @param args  		the argument list
	 * @param scalarValues 	scale-value pairs associated with the predication
	 * @param sources  		the sources for the predication
	 */
	public Predication(String id,Predicate predicate, List<Argument> args,
			List<ScalarModalityValue> scalarValues, List<SemanticItem> sources) {
		this(id,predicate,args);
		if (scalarValues != null) this.scalarValues = scalarValues;
		if (sources != null) this.sources = sources;
	}
	
	public Predicate getPredicate() {
		return predicate;
	}
	
	public String getSemType() {
		if (predicate == null) return null;
		String semtype = ((Predicate)predicate).getType();
		return semtype;
	}

	/** 
	 * 
	 * @return all relation definitions
	 */
	public static Set<RelationDefinition> getDefinitions() {
		return definitions;
	}
	
	@Override
	public RelationDefinition getDefinition() {
		return definition;
	}
	
	/**
	 * Sets predication definitions to use.
	 * 
	 * @param defs  the predication definitions
	 */
	public static void setDefinitions(Set<RelationDefinition> defs) {
		definitions = defs;
	}
	
	public static void addDefinition(RelationDefinition def) {
		if (definitions == null) definitions = new HashSet<>();
		definitions.add(def);
	}
	
	public static void addDefinitions(Collection<RelationDefinition> defs) {
		if (definitions == null) definitions = new HashSet<>();
		definitions.addAll(defs);
	}
	
	public LinkedHashSet<String> getAllSemtypes() {
		return predicate.getAllSemtypes();
	}
	
	public List<ScalarModalityValue> getScalarValues() {
		return scalarValues;
	}

	public void setScalarValues(List<ScalarModalityValue> scalarValues) {
		this.scalarValues = scalarValues;
	}
	
	/**
	 * Initializes the scales for this predication. <p>
	 * This creates an EPISTEMIC scale for the predication and assigns it the value of 1.0,
	 * i.e., every predication is initially a FACT.
	 * 
	 */
	public void setDefaultScalarValues() {
		List<ScalarModalityValue> scalarValues = new ArrayList<ScalarModalityValue>();
		scalarValues.add(new ScalarModalityValue(ScaleType.EPISTEMIC,1.0));
		setScalarValues(scalarValues);
	}
	
	/**
	 * Adds a new scalar modality value for this predication. 
	 * 
	 * @param scaleType	the modality scale to add
	 * @param i			the value
	 */
	public void addScalarValue(ScaleType scaleType, Interval i) {
		if (scalarValues == null) {
			scalarValues = new ArrayList<ScalarModalityValue>();
		}
		scalarValues.add(new ScalarModalityValue(scaleType,i));
	}
	
	/**
	 * Removes the modality value with the given type.
	 * 
	 * @param type	the scale type
	 */
	public void removeScalarValue(ScaleType type) {
		if (scalarValues != null) {
			ScalarModalityValue toRemove = null;
			for (ScalarModalityValue smv: scalarValues){
				if (smv.getScaleType().equals(type)) toRemove = smv;
			}
			if (toRemove != null) scalarValues.remove(toRemove);
		}
	}
	
	public List<SemanticItem> getSources() {
		return sources;
	}

	public void setSources(List<SemanticItem> sources) {
		this.sources = sources;
	}
	
	/**
	 * Gets the span covered by this predication.<p>
	 * This corresponds to the union of the predicate span and the spans of the arguments.
	 * 
	 * @return the predication span 
	 */
	public SpanList getSpan() {
//		if (span != null) return span;
		SpanList sp = 	(predicate == null? null: predicate.getSpan());
		for (int i= 0; i < arguments.size(); i++) {
			SpanList argsp = null;
			SemanticItem si = arguments.get(i).getArg();
			if (si instanceof Predication) argsp = ((Predication)si).getSpan();
			else if (si instanceof Term) argsp = ((Term)si).getSpan();
			if (sp == null)  sp = argsp;
			else sp = SpanList.union(sp, argsp);
		}
		return sp;
	}
	
	/**
	 * Initializes the source for this predication. <p>
	 * This assigns WRITER as the source for the predication.
	 * 
	 */
	public void setDefaultSources() {
		List<SemanticItem> sourceWr = new ArrayList<SemanticItem>();
		sourceWr.add(Entity.SOURCE_WRITER);
		sources = sourceWr;
	}
	
	/**
	 * 
	 * @return true if the source of this predication is GENERIC.
	 */
	public boolean hasGenericSource() {
//		return (sources.size() == 1 && sources.get(0).equals(Entity.SOURCE_GENERIC));
		return (sources.size() == 1 && sources.get(0).getOntology().equals(Concept.SOURCE_GENERIC));	
	}
	
	/**
	 * 
	 * @return true if the source is WRITER.
	 */
	public boolean hasDefaultSource() {
//		return (sources.size() == 1 && sources.get(0).equals(Entity.SOURCE_WRITER));
		return (sources.size() == 1 && sources.get(0).getOntology().equals(Concept.SOURCE_WRITER));	
	}
	
	/**
	 * Sets the source of this predication, if the predicate is an epistemic scalar predicate.<p>
	 * It first determines whether the predication has a GENERIC source (e.g., <i>It is suggested that...</i>).
	 * Otherwise, it uses arguments with the SUBJ role to identify the source. If this does not yield a source and 
	 * if GENERIC source is allowed (<var>createGeneric</var>), it tries to use dependency relations 
	 * to identify the source.
	 * 
	 * @param createGeneric	whether GENERIC source is allowed
	 */
	public void setSource(boolean createGeneric) {
		if (EmbeddingCategorization.isEpistemicScalar(predicate) == false) return;
		if (createGeneric)  {
			setGenericSourceIfPossible();
			if (hasGenericSource())
				return;
		}
		List<Argument> subjs = Argument.getArgs(arguments, "SUBJ");
		if (subjs.size() > 0) {
			List<SemanticItem> subjSources = new ArrayList<>();
			for (Argument subj: subjs) {
				subjSources.add(subj.getArg());
			}
			setSources(subjSources);
		} else if (createGeneric) {
			SurfaceElement surf = predicate.getSurfaceElement();
			List<SemanticItem> generic = new ArrayList<>();
			// TODO other POS's are possible too. Probably need to use DependencyClass?
			if (surf.isVerbal()) {
				List<SynDependency> deps = SynDependency.outDependencies(surf, surf.getSentence().getEmbeddings());
				for (SynDependency d: deps) {
					if (d.getType().endsWith("subj")) {
						SurfaceElement dep = d.getDependent();
						if (dep.filterByRelations().size() > 0) {
							generic.addAll(dep.filterByRelations());
							break;
						}
						else if (dep.hasSemantics()) { 
							generic.addAll(dep.getSemantics());
							break;
						}
						else {
							Document doc=  surf.getSentence().getDocument();
							SemanticItemFactory sif = doc.getSemanticItemFactory();
							Entity ent = sif.newGenericEntity(doc, dep.getSpan(), dep.getHead().getSpan(),Concept.GENERIC);
//							Entity ent = sif.newEntity(doc, dep.getSpan(), dep.getHead().getSpan(), Entity.GENERIC);
							generic.add(ent);
							break;
						}
					}
				}
				if (generic.size() > 0) setSources(generic);
			}
		}
	}
	
	private void setGenericSourceIfPossible() {
		SurfaceElement surf = predicate.getSurfaceElement();
		Document doc = surf.getDocument();
		SemanticItemFactory sif = doc.getSemanticItemFactory();
		if (surf.isVerbal()) {
			List<SynDependency> deps = SynDependency.outDependencies(surf, surf.getSentence().getEmbeddings());
			List<SynDependency> byDeps = SynDependency.outDependenciesWithTypes(surf, surf.getSentence().getEmbeddings(), 
					Arrays.asList("prep_by","prepc_by"), true);
			for (SynDependency d: deps) {
				if (d.getType().endsWith("subjpass") && 
						(d.getDependent().getHead().getLemma().equals("it") || 
						 byDeps.size() == 0)) {
						List<SemanticItem> generic = new ArrayList<SemanticItem>();
						generic.add(sif.newGenericEntity(doc, d.getDependent().getSpan(), 
									d.getDependent().getHead().getSpan(),Concept.SOURCE_GENERIC));
//						generic.add(Entity.SOURCE_GENERIC);
						setSources(generic);
				}
			}
		}
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(id + "_" + resolved + "_" + 
				(sources == null ? "NOSOURCE" : sources.toString()) + "_" + 
				(getSemType()  == null ? "NOSEMTYPE" : getSemType()) + "_" + 
				(predicate == null ? "NOPREDICATE" : ((Term)predicate).getId()) + "_" + 
				(predicate == null ? text : ((Term)predicate).getText()));
		if (scalarValues != null) {
			for (ScalarModalityValue sc: scalarValues) {
				buf.append("_" + sc.toString());
			}
		}
		if (arguments == null) {
			log.log(Level.WARNING,"No arguments for the predication with id {0} in document{1}.", new Object[]{id,getDocument().getId()});
			buf.append("_NOARG");
		} else {
			for (Argument a: arguments) {
				buf.append("_" + a.toString());
			}
		}
		return buf.toString();
	}
	
	public String toShortString() {
		StringBuffer buf = new StringBuffer();
		buf.append((predicate == null ? text : ((Term)predicate).getText()) + "(" + 
				   id + "," +  (predicate == null ? "NOPRED" : ((Term)predicate).getId()) + 
				   "," +  // (sources == null ? "" : sources.toString() + ",") + 
				   (getSemType()  == null ? "NOSEM" : getSemType()) + ",");
		if (scalarValues != null) {
			for (int i=0; i < scalarValues.size() -1; i++) {
				ScalarModalityValue sc = scalarValues.get(i);
				buf.append(sc.toString() + "_");
			}
			buf.append(scalarValues.get(scalarValues.size()-1));
		}
		buf.append(",");
		if (sources != null) {
			for (int i=0; i < sources.size() -1; i++) {
				SemanticItem sm = sources.get(i);
				buf.append(sm.getId() + "_");
			}
			buf.append(sources.get(sources.size()-1).getId());
		}
		buf.append(",");
		if (arguments == null) {
			log.log(Level.WARNING,"No arguments for the predication with id {0} in document{1}.", new Object[]{id,getDocument().getId()});
			buf.append("_NOARG");
		}
		else {
			for (int i=0; i < arguments.size() -1; i++) {
				Argument arg = arguments.get(i);
				buf.append(arg.toShortString() + "_");
			}
			buf.append(arguments.get(arguments.size()-1).toShortString());
		}	
		buf.append(")");
		return buf.toString();
	}
		
	/**
	 * 
	 * @return true if all the arguments have roles that are considered resolved.
	 */
	public boolean canBeResolved() {
		if (predicate == null) return false;
		if (arguments == null) return false;
		for (Argument a: arguments) {
			if (!(RESOLVED_ARGTYPES.contains(a.getType()))) return false;
		}		
		return true;
	}
	
	/**
	 * 
	 * @return  the count of arguments with resolved roles.
	 */
	public int resolvedArgCount() {
		if (predicate == null) return 0;
		if (arguments == null) return 0;
		int c = 0;
		for (Argument a: arguments) {
			if (RESOLVED_ARGTYPES.contains(a.getType())) c++;
		}		
		return c;
	}
	
	/**
	 * 
	 * @return  true if the argument list satisfies the predication definition.
	 */
	public boolean coreArgsResolved() {
		if (definition == null) return false;
		if (predicate == null) return false;
		return coreArgsResolved(arguments,definition);
	}
	
	/**
	 * Given a list of <code>Argument</code> objects and the predication definition, 
	 * finds out whether the predication definition is satisfied by the arguments.
	 * 
	 * @param arguments	a list of arguments
	 * @param relDef	a relation definition
	 * 
	 * @return  true if the argument list satisfies the predication definition.
	 */
	public static boolean coreArgsResolved(List<Argument> arguments, RelationDefinition relDef) {
		if (arguments == null || arguments.size() == 0) return false;
		List<RelationArgument> coreArgs = relDef.getCoreArgs();
		if (coreArgs == null) return true;
		List<String> argNames = Argument.getArgNames(arguments);
		for (RelationArgument pa: coreArgs) {
			log.log(Level.FINEST,"Checking for role {0}.", new Object[]{pa.getRole()});
			if (argNames.contains(pa.getRole()) == false) {
				log.log(Level.FINEST,"Unable to find role {0}.", new Object[]{pa.getRole()});
				return false;
			}
			log.log(Level.FINEST,"Found role {0}.",new Object[]{pa.getRole()});
		}
		return true;
	}

	public Element toXml() {
		Element el = new Element("Predication");
		el.addAttribute(new Attribute("id",id));
		el.addAttribute(new Attribute("type",type));
//		el.addAttribute(new Attribute("origId",id));
		if (predicate != null) el.addAttribute(new Attribute("predicate",predicate.getId()));
		if (features.size() > 0) {
			for (String s: features.keySet()) {
				el.addAttribute(new Attribute(s,features.get(s).toString()));
			}
		}
		for (int i= 0; i < arguments.size(); i++) {
			Argument arg = arguments.get(i);
			el.appendChild(arg.toXml());
		}
		return el;
	}
	
	public String toStandoffAnnotation() {
		StringBuffer buf = new StringBuffer();
		if (arguments == null) {
			log.log(Level.WARNING,"No arguments for the predication with id {0} in document{1}.", new Object[]{id,getDocument().getId()});
			return buf.toString();
		}
		buf.append(id + "\t");
		if (predicate != null) buf.append(type + ":" + predicate.getId() + " ");
		for (int i= 0; i < arguments.size(); i++) {
			buf.append(arguments.get(i).getType() + ":" + arguments.get(i).getArg().getId() + " ");
		}
		if (sources != null) {
			buf.append("\t");
			for (int i=0; i <sources.size(); i++) {
				buf.append(" Source:" + sources.get(i).getId());
			}
		}
		for (int i=0; i <scalarValues.size(); i++) {
			buf.append(" SC:" + scalarValues.get(i).getScaleType().toString() + "_" + scalarValues.get(i).getValue().toString());
		}
		return buf.toString().trim();
	}
	
	public int hashCode() {
		return
	    ((id == null ? 59 : id.hashCode()) ^
	     (predicate == null ? 79 : predicate.hashCode()) ^
	     (type  == null ? 89 : type.hashCode()) ^
	     (arguments == null? 109 : arguments.hashCode()));
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Predication other = (Predication) obj;
		if (id == null) {
			if (other.id != null) return false;
		} else if (!id.equals(other.id)) return false;
		if (predicate == null) {
			if (other.predicate != null)
				return false;
		} else if (!predicate.equals(other.predicate))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return argsEquals(other);
	}
		
	/**
	 * Finds whether a predication (<var>pred1</var>) is in the scope of another (<var>pred2</var>). 
	 * 
	 * @param pred1 the first predication
	 * @param pred2 the second predication
	 * @return  	true if <var>pred1</var> outscopes <var>pred2</var>
	 */
	public static boolean outscopes(Predication pred1, Predication pred2) {
		if (pred1.isAtomic()) return false;
		List<Argument> srargs1 = pred1.getArguments();
		List<Argument> srargs2 = pred2.getArguments();
		if (srargs1 == null || srargs2 == null) return false;
		for (Argument a1: srargs1) {
			SemanticItem si = a1.getArg();
			if (si.equals(pred2)) 
				// limits to core arguments
				// && Constants.SCOPE_ARGTYPES.contains(srargNames1.get(i))) 
				return true;
		}
		for (Argument a1: srargs1) {
			SemanticItem si = a1.getArg();
			if (si instanceof Predication) 
				if (outscopes((Predication)si,pred2) || 
						   sharedArg((Predication)si,pred2)) return true;
		}
		return false;
	}
	
	/**
	 * Finds whether two predications share arguments.
	 * 
	 * @param pred1 the first predication
	 * @param pred2 the second predication
	 * @return  	true if the predications share any argument
	 */
	public static boolean sharedArg(Predication pred1, Predication pred2) {
		List<SemanticItem> srargs1 = pred1.getArgItems();
		List<SemanticItem> srargs2 = pred2.getArgItems();
 		if (srargs1 == null || srargs2 == null) return false;
		return CollectionUtils.containsAny(srargs1, srargs2);
	}
	
}
