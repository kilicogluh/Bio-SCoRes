package gov.nih.nlm.ling.sem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nih.nlm.ling.composition.EmbeddingCategorization;
import nu.xom.Attribute;
import nu.xom.Element;

/**
 * Represents an indicator sense. A word like <i>stimulate</i> may indicate different type
 * of relations (e.g., STIMULATES, AUGMENTS), depending on the context, and this class
 * encodes one of those meanings.<p>
 * A <code>Sense</code> object consists of the following elements, used in argument identification and interpretation (see 
 * Kilicoglu (2012) for further details: <ul>
 * <li><i>category</i>: the meaning type encoded by this sense (e.g., CAUSE)
 * <li><i>priorScalarValue</i>: the scalar value introduced by this sense and used in composition, by default 1.0.
 * <li><i>scopeType</i>: whether this sense has wide, narrow, or default scope reading.
 * <li><i>embeddingTypes</i>: the syntactic categories used to find the arguments of this indicator sense.
 * <li><i>inverse</i>: whether the logical subject/object arguments are reversed in semantic interpretation.
 * <li><i>probability</i>: the likelihood of this sense, generally based on some corpus analysis.
 * <li><i>additionalFeatures</i>: key-value pairs for other relevant features (hedge strength, source types, etc.)
 * </ul>
 * 
 * @author Halil Kilicoglu
 *
 */
public class Sense implements Ontology, Comparable<Sense> {
	
	public enum ScopeType {
		DEFAULT, WIDE, NARROW;
		public String toString() {
			switch (this) {
			case WIDE: return "wide";
			case NARROW: return "narrow";
			default: return "default";
			}
		}
		
		public static ScopeType fromString(String s) {
			if (s == null) return DEFAULT;
			if (s.equalsIgnoreCase("wide")) return WIDE;
			if (s.equalsIgnoreCase("narrow")) return NARROW;
			return DEFAULT;
		}
	};
	
	private String category; //embedding category or other semantic type 
	private double priorScalarValue;
	private ScopeType scopeType;
	private List<String> embeddingTypes;
	private boolean inverse;
	private double probability;
	private Map<String,Object> additionalFeatures;

	/**
	 * Constructs a <code>Sense</code> object with a meaning category and default values.
	 * 
	 * @param category	the category this sense encodes
	 */
	public Sense(String category) {
		this.category = category;
		this.probability = 1.0;
		this.scopeType = ScopeType.DEFAULT;
		this.priorScalarValue = 1.0;
		this.inverse = false;
	}
	
	/** 
	 * Constructs a <code>Sense</code> object with core member elements.
	 * 
	 * @param category			the category this sense encodes
	 * @param probability		the probability associated with this sense
	 * @param value				the prior scalar modality value
	 * @param scopeType			the scope type (wide, narrow, default) for this sense
	 * @param embeddingTypes	the syntactic classes used to identify objects for this sense
	 * @param inverse			whether logical argument inversion is done for this sense
	 */
	public Sense(String category, double probability,
			double value, ScopeType scopeType, List<String> embeddingTypes, boolean inverse) {
		this(category);
		this.probability = probability;
		this.priorScalarValue = value;
		this.scopeType = scopeType;
		this.embeddingTypes = embeddingTypes;
		this.inverse = inverse;
	}
	
	
	public Sense(Element el) {
		this(el.getAttributeValue("category"), 
			 Double.parseDouble(el.getAttributeValue("probability")),
			 Double.parseDouble(el.getAttributeValue("priorScalarValue")), 
			 ScopeType.fromString(el.getAttributeValue("scopeType")),
			 ( el.getAttributeValue("embeddingTypes") == null ? new ArrayList<String>() : Arrays.asList(el.getAttributeValue("embeddingTypes").split("[,]"))),
			 ( el.getAttributeValue("inverse") == null ? false : true)
			 );
		List<String> coreAttrs = Arrays.asList("category","probability","priorScalarValue","scopeType","embeddingTypes","inverse");
		for (int i =0; i < el.getAttributeCount(); i++) {
			Attribute att = el.getAttribute(i);
			if (coreAttrs.contains(att.getLocalName())) continue;
			String key = att.getLocalName();
			String value = att.getValue();
			if (additionalFeatures == null) additionalFeatures = new HashMap<String,Object>();
			additionalFeatures.put(key, value);
		}

	}
	
	public boolean isInverse() {
		return inverse;
	}
	
	public String getCategory() {
		return category;
	}

	public double getProbability() {
		return probability;
	}

	public double getPriorScalarValue() {
		return priorScalarValue;
	}

	public ScopeType getScopeType() {
		return scopeType;
	}
	
	public String getScopeTypeAsString() {
		return scopeType.toString();
	}
	
	public List<String> getEmbeddingTypes() {
		return embeddingTypes;
	}
	
	public Map<String, Object> getAdditionalFeatures() {
		return additionalFeatures;
	}
	
	public void addFeature(String key, Object value) {
		if (additionalFeatures == null) additionalFeatures = new HashMap<String,Object>();
		additionalFeatures.put(key,value);
	}
	
	public Object getFeature(String key){
		return additionalFeatures.get(key);
	}

	/**
	 * 
	 * @return true if this sense has a category that is part of the embedding categorization
	 */
	public boolean isEmbedding() {
		return EmbeddingCategorization.getInstance().containsVertex(category);
	}
	
	/**
	 * 
	 * @return true if this sense has a category that is part of the RELATIONAL hierarchy
	 * 		   in the embedding categorization
	 */
	public boolean isDiscourseConnective() {
		return EmbeddingCategorization.getAllDescendants("RELATIONAL").contains(category);
	}
	
	/**
	 * 
	 * @return true if this sense has a category that is not part of the embedding categorization
	 */
	public boolean isAtomic() {
		return !isEmbedding();
	}
	
	public int compareTo(Sense et1) {
		if ((this.isAtomic() && et1.isAtomic()) ||
				(this.isDiscourseConnective() && et1.isDiscourseConnective())) {
			if (this.probability < et1.getProbability()) return 1;
			else return -1;
		}		
		if (this.isEmbedding() && et1.isEmbedding()) {
			if (this.probability < et1.getProbability() &&
					this.probability > 0.0 && et1.getProbability() > 0.0) return 1;
			if (this.probability < et1.getProbability() &&
					et1.getProbability() > 0.0) return 1;
			else return -1;
		}
		if (!(this.isDiscourseConnective()) && et1.isDiscourseConnective()) return 1;
		if (this.isDiscourseConnective() && !(et1.isDiscourseConnective())) return -1;
		return -1;
		
	}
	
	public String toString() {
		return category + "_" + priorScalarValue + "_" + embeddingTypes + "_" + scopeType + "_" + probability;
	}
		
	public int hashCode() {
		int code = 0;
		if (embeddingTypes == null) code += 83;
		else {
			for (String syn: embeddingTypes) {
				code += syn.hashCode();
			}
		}
	    return (
	    		(category == null ? 37: category.hashCode()) ^
	    		(Double.valueOf(priorScalarValue) == null ? 89 : (Double.valueOf(priorScalarValue).hashCode())) ^
	    		(scopeType ==  null ? 113 :scopeType.hashCode()) ^
	    		code ^
	    		(Boolean.valueOf(inverse) ? 91 : 97) ^
	    		(Double.valueOf(probability) == null ? 119 : (Double.valueOf(probability).hashCode())) ^
	    		super.hashCode());
	}
	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		if (this == obj) return true;
		Sense ind = (Sense)obj;
		if (priorScalarValue != ind.getPriorScalarValue()) return false;
		if (scopeType.equals(ind.getScopeType()) == false) return false;
		if (probability != ind.getProbability()) return false;
		if (inverse != ind.isInverse()) return false;
		if (embeddingTypes != null) {
			for (int i=0; i < embeddingTypes.size(); i++) {
				if (embeddingTypes.get(i).equals(ind.getEmbeddingTypes().get(i))) return false;
			}
		}
		return super.equals(ind);
	}
	
	public Element toXml() {
		Element el = new Element("SemInfo");
		el.addAttribute(new Attribute("semType",category));
		el.addAttribute(new Attribute("priorScalarValue", String.valueOf(priorScalarValue)));
		el.addAttribute(new Attribute("probability",String.valueOf(probability)));
		el.addAttribute(new Attribute("inverse",String.valueOf(inverse)));
		el.addAttribute(new Attribute("scopeType",scopeType.toString()));
		el.addAttribute(new Attribute("embeddingTypes",embeddingTypes.toString()));
		if (additionalFeatures != null && additionalFeatures.size() > 0) {
			for (String k: additionalFeatures.keySet()) {
				el.addAttribute(new Attribute(k,additionalFeatures.get(k).toString()));
			}
		}
		return el;
	}
	
}
