package gov.nih.nlm.ling.brat;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import gov.nih.nlm.ling.core.SpanList;

/**
 * The generic implementation of the {@link Annotation} interface. 
 *
 * @author Halil Kilicoglu
 */
public abstract class AbstractAnnotation implements Annotation {

	public static final List<Class<? extends AbstractAnnotation>> COMPLEXITY_ORDER = 
			Arrays.asList(TermAnnotation.class,RelationAnnotation.class,EventAnnotation.class,EventModificationAnnotation.class);
	
	public final String id;
	public final String docId;
	public final String annotationType;
	public final String type;

	/**
	 * Sorts <code>Annotation</code>s by their character offsets and complexity,
	 * annotation with lower complexity coming first.
	 */
	public static final Comparator<Annotation> SPAN_ORDER = new Comparator<Annotation>() {
		public int compare(Annotation a1,Annotation a2) {
			SpanList a1Sp = a1.getSpan();
			SpanList a2Sp = a2.getSpan();
			if (SpanList.atLeft(a2Sp,a1Sp)) return 1;
			else if (a2Sp.equals(a1Sp)) {
				int a1i = COMPLEXITY_ORDER.indexOf(a1.getClass());
				int a2i = COMPLEXITY_ORDER.indexOf(a1.getClass());
				if (a1i == a2i) {
					String a1Id = a1.getId(); String a2Id = a2.getId();
					if (a1Id.equals(a2Id)) {
						String a1Type = a1.getType(); String a2Type = a2.getType();
						if (a1Type.equals(a2.getType())) {
							return a1.hashCode() - a2.hashCode();
						} 
						else return a1Type.compareTo(a2Type);
					} else return a1Id.compareTo(a2Id);
				}
				else return a1i - a2i;
			}
			else return -1;
		}	
	};
	
	/**
	 * Creates a generic <code>Annotation</code> object. 
	 * 
	 * @param id   the annotation id
	 * @param docId  the document id
	 * @param annotationType  higher-level type as specified by the implementing class (e.g., Term, Relation)
	 * @param type  the semantic type (e.g., Drug, Disease)
	 * 
	 */
	protected AbstractAnnotation(String id, String docId, String annotationType, String type) {
		this.docId = docId;
		this.id = id;
		this.type = type;
		this.annotationType = annotationType;
	}

	public String getDocId() {
		return docId;
	}
	
	public String getId() {
		return id;
	}
	
	public String getAnnotationType() {
		return annotationType;
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return id + "\t" + annotationType + " " + type;
	}
	
	public String toResolvedString() {
		return toString();
	}
	
	public String toStandoffAnnotation() {
		return toString();
	}
	
	abstract public SpanList getSpan();
  
}