package gov.nih.nlm.ling.brat;

import gov.nih.nlm.ling.core.SpanList;

import java.util.List;

/**
 * The class to represent an event annotation. An event is essentially
 * a complex semantic object analogous to a predicate-argument structure. 
 * It extends {@link RelationAnnotation}, by requiring a predicate.
 * 
 * @author Halil Kilicoglu
 *
 */
public class EventAnnotation extends RelationAnnotation {

	private TermAnnotation predicate;
	
	/**
	 * Creates an <code>EventAnnotation</code> object. 
	 * 
	 * @param id		annotation id
	 * @param docId  	document id
	 * @param type  	semantic type of the event
	 * @param args  	typed arguments of the event
	 * @param predicate the predicate indicating the event
	 */
	public EventAnnotation(String id, String docId, String type,
			List<AnnotationArgument> args, TermAnnotation predicate) {
		super(id, docId, type, args);
		this.predicate = predicate;
	}

	public TermAnnotation getPredicate() {
		return predicate;
	}

	public void setPredicate(TermAnnotation predicate) {
		this.predicate = predicate;
	}
	
	public SpanList getSpan() {
		return predicate.getSpan();
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(id + "\t" + type + ":" + predicate.getId() + " ");
		if (arguments == null || arguments.size() == 0) return buf.toString().trim();
		for (AnnotationArgument a: arguments) {
			buf.append(a.toString() + " ");
		}
		return buf.toString().trim();
	}
	
	/**
	 * @return an easier-to-read text representation of the annotation.
	 */
	public String toResolvedString() {
		StringBuffer buf = new StringBuffer();
		buf.append(predicate.toResolvedString());
		if (arguments == null || arguments.size() == 0) return buf.toString().trim();
		for (AnnotationArgument a: arguments) {
			Annotation ann = a.getArg();
			if (ann instanceof RelationAnnotation)
				buf.append(" " + a.getRole() + ":"  + ann.getId());
			else
				buf.append(" " + a.getRole() + ":" + a.getArg().toResolvedString());
		}
		return buf.toString().trim();
	}
	
	/**
	 * Checks whether the event annotation matches exactly another annotation
	 * by checking the semantic types and the exact match of the arguments
	 * 
	 * @param a  annotation to compare with 
	 * @return true if exact match
	 */
	public boolean exactMatch(Annotation a) {
		if (a instanceof EventAnnotation == false) return false;
		EventAnnotation ae = (EventAnnotation)a;
		TermAnnotation aPred = ae.getPredicate();
		boolean hasArg = !(arguments == null || arguments.size() == 0);
		boolean hasArgA = !(ae.getArguments() == null || ae.getArguments().size() == 0);
		if (!hasArg && !hasArgA) return predicate.exactMatch(aPred);
		if (super.exactMatch((RelationAnnotation)a) == false) return false;
		return (predicate.exactMatch(aPred));
	}
	
	/**
	 * Checks whether the event annotation approximately matches another annotation.
	 * Character offset overlap rather than equality is sufficient.
	 * 
	 * @param a  annotation to compare with 
	 * @return true if approximate match
	 */
	public boolean approximateMatch(Annotation a) {
		if (a instanceof EventAnnotation == false) return false;
		EventAnnotation ae = (EventAnnotation)a;
		TermAnnotation aPred = ae.getPredicate();
		boolean hasArg = !(arguments == null || arguments.size() == 0);
		boolean hasArgA = !(ae.getArguments() == null || ae.getArguments().size() == 0);
		if (!hasArg && !hasArgA) return predicate.approximateMatch(aPred);	
		if (super.approximateMatch((RelationAnnotation)a) == false) return false;
		return (predicate.approximateMatch(aPred));
	}
	
	/**
	 * Checks whether the normalized event annotation matches another normalized annotation.
	 * Exact or approximate match can still be specified.
	 * 
	 * @param approximateMatch	whether to perform approximate match for offsets
	 * @param a  				annotation to compare with 
	 * @return true if the annotations are normalized to the same concept
	 */
	public boolean referenceMatch(boolean approximateMatch, Annotation a) {
		if (a instanceof EventAnnotation == false) return false;
		EventAnnotation ae = (EventAnnotation)a;
		TermAnnotation aPred = ae.getPredicate();
		boolean hasArg = !(arguments == null || arguments.size() == 0);
		boolean hasArgA = !(ae.getArguments() == null || ae.getArguments().size() == 0);
		if (!hasArg && !hasArgA) return predicate.referenceMatch(approximateMatch,aPred);	
		if ( super.referenceMatch(approximateMatch,(RelationAnnotation)a) == false) return false;
		return (predicate.referenceMatch(approximateMatch,aPred));
	}
	
	public int compareTo(Annotation a) {
		if (a instanceof EventModificationAnnotation) return -1;
		if (a instanceof TermAnnotation) return 1;
		if (a instanceof EventAnnotation) {
			EventAnnotation ea = (EventAnnotation)a;
			int pc = predicate.compareTo(ea.getPredicate());
			if (pc == 0) {
				int c = getSpan().compareTo(ea.getSpan());
				if (c == 0) {
					int tc = type.compareTo(ea.getType());	
					if (tc == 0) {
						int sc = getArguments().size() -ea.getArguments().size();
						if (sc == 0) {
							for (int i=0; i < getArguments().size(); i++) {
								int ac = getArguments().get(i).compareTo(ea.getArguments().get(i));
								if (ac != 0) return ac;
							}
							return id.compareTo(a.getId());
						} else return sc;
					} else return tc;
				}
			} else return pc;
		} 
		return 1;	
	}
	
	public int hashCode() {
		int code = 73 * getSpan().hashCode() + 191 * predicate.hashCode();
		for (AnnotationArgument aa: getArguments()) {
			code += 103 * aa.hashCode();
		}
		return code;
	}	
}