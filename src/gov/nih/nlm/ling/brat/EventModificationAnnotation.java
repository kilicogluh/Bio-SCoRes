package gov.nih.nlm.ling.brat;

import gov.nih.nlm.ling.core.SpanList;

/**
 * The class to represent an event modification annotation, such as speculation, negation, or factuality.
 * 
 * @author Halil Kilicoglu
 *
 */
public class EventModificationAnnotation extends AbstractAnnotation {

	EventAnnotation event;
	String value= null;
	
	/**
	 * Creates a <code>TermAnnotation</code> object. 
	 * 
	 * @param id  annotation id
	 * @param docId  document id
	 * @param type  the semantic type (e.g.,Speculation, Negation)
	 * @param event  the event annotation that is being modified
	 */
	public EventModificationAnnotation(String id, String docId, String type, EventAnnotation event) {
		super(id,docId,"EventModification", type);
		this.event = event;
	}
	
	public EventModificationAnnotation(String id, String docId, String type, EventAnnotation event, String value) {
		this(id,docId,type,event);
		this.value = value;
	}
	
	public EventAnnotation getEvent() {
		return event;
	}

	public void setEvent(EventAnnotation event) {
		this.event = event;
	}
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public String toString() {
		if (value == null)			
			return (id + "\t" + type + ":" + event.getId()).trim();
		else 
			return (id + "\t" + type + ":" + event.getId() + " " + value).trim();
	}
	
	/**
	 * @return an easier-to-read text representation of the annotation.
	 */
	public String toResolvedString() {
		StringBuffer buf = new StringBuffer();
		buf.append(type);
		if (value != null) buf.append(" " + value);
		buf.append(" " + event.getId() + ":" + event.toResolvedString());
		return buf.toString().trim();
	}

	/**
	 * Checks whether the event modification annotation matches exactly another annotation
	 * by checking the type, event argument and value if possible
	 * 
	 * @param annotation1  annotation to compare with 
	 * @return true if exact match
	 */
	public boolean exactMatch(Annotation annotation1) {
		if (annotation1 instanceof EventModificationAnnotation == false) return false;
		EventModificationAnnotation ae = (EventModificationAnnotation)annotation1;
		EventAnnotation aPred = ae.getEvent();
		if (ae.getType().equals(type) == false) return false;
		if (event.exactMatch(aPred) == false) return false;
		if (value == null && ae.getValue() == null) return true;
		return (value != null && ae.getValue() != null && value.equals(ae.getValue()));
	}

	/**
	 * Same as {@link #exactMatch(Annotation)}.
	 */
	public boolean approximateMatch(Annotation annotation1) {
		return exactMatch(annotation1);
	}
	
	/**
	 * The same as {@link #exactMatch(Annotation)} or {@link #approximateMatch(Annotation)},
	 * depending on the <var>approximateMatch</var> parameter.
	 * 
	 */
	public boolean referenceMatch(boolean approximateMatch, Annotation annotation1) {
		if (approximateMatch) return approximateMatch(annotation1);
		return exactMatch(annotation1);
	}
	
	/**
	 * The same as approximate match.
	 */
	public boolean spanMatch(Annotation a) {
		return approximateMatch(a);
	}

	@Override
	public SpanList getSpan() {
		return event.getSpan();
	}
	
	public int compareTo(Annotation a) {
		if (a instanceof EventModificationAnnotation == false) return 1;
		EventModificationAnnotation ea = (EventModificationAnnotation)a;
		int ec = event.compareTo(ea.getEvent());
		if (ec == 0) {
			int tc = type.compareTo(ea.getType());	
			if (tc == 0) {
				int vc = value.compareTo(ea.getValue());
				if (vc == 0) return id.compareTo(a.getId());
				return vc;
			} else return tc;
		}
		return ec;
	}
	
	public int hashCode() {
		int code = 289 * value.hashCode() + 187 * event.hashCode() +
				   133 * type.hashCode() + 121 * id.hashCode() + 161 * value.hashCode();
		return code;
	}

}
