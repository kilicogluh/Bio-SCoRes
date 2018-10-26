package gov.nih.nlm.ling.brat;

/**
 * This class represents a reference from a standoff annotation to a normalized entity in an ontology
 * or terminology.
 * 
 * @author Halil Kilicoglu
 *
 */
public class Reference {

	private String source;
	private String sourceId;
	
	/**
	 * Creates a <code>Reference</code> object. 
	 * 
	 * @param source 	the source name
	 * @param sourceId 	the id corresponding to the annotation in the source
	 */
	public Reference(String source, String sourceId) {
		this.source = source;
		this.sourceId = sourceId;
	}
	
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}
	
	public String toString() {
		return source + ":" + sourceId;
	}

	public String toResolvedString() {
		return toString();
	}

	public boolean equals(Object o) {
		if (o instanceof Reference == false ) return false;
		Reference or = (Reference)o;
		return (or.getSource().equals(source) && or.getSourceId().equals(sourceId));
	}
	
	public int compareTo(Annotation a) {
		if (a instanceof Reference == false) return 1;
		Reference ea = (Reference)a;
		int vc = source.compareTo(ea.getSource());
		if (vc == 0)  {
			return sourceId.compareTo(ea.getSourceId());
		}
		return vc;
	}
	
	public int hashCode() {
		int code =  219 * source.hashCode() + 129 * sourceId.hashCode();
		return code;
	}

}
