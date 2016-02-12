package gov.nih.nlm.ling.brat;

import gov.nih.nlm.ling.core.SpanList;

import java.util.Comparator;

/**
 * Represents a role-argument pair of a more complex {@link Annotation} object 
 * ({@link RelationAnnotation}, {@link EventAnnotation}, etc.). A role is 
 * the function that the argument (participant) plays in the more complex semantic
 * object, such as Theme, Agent for an <code>EventAnnotation</code>.
 * 
 * @author Halil Kilicoglu
 *
 */
public class AnnotationArgument implements Comparable<AnnotationArgument> {

	private String role;
	private Annotation arg;
	
	/**
	 * Sort <code>AnnotationArgument</code>s by character offsets
	 */
	public static final Comparator<AnnotationArgument> SPAN_ORDER = new Comparator<AnnotationArgument>() {
		public int compare(AnnotationArgument a1,AnnotationArgument a2) {
			SpanList a1Sp = a1.getArg().getSpan();
			SpanList a2Sp = a2.getArg().getSpan();
			if (SpanList.atLeft(a2Sp,a1Sp)) return 1;
			return -1;
		}	
	};
	
	/**
	 * Creates an <code>AnnotationArgument</code> object.
	 * 
	 * @param role the semantic role of the argument (Theme, Agent, Antecedent, etc.)
	 * @param arg the <code>Annotation</code> object that fulfills the specified role
	 */
	public AnnotationArgument(String role, Annotation arg) {
		this.role = role;
		this.arg = arg;
	}
	
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public Annotation getArg() {
		return arg;
	}
	public void setArg(Annotation arg) {
		this.arg = arg;
	}
	
	public String toString() {
		return role + ":" + arg.getId();
	}
	
	public int compareTo(AnnotationArgument a) {
		int rc= role.compareTo(a.getRole());
		if (rc == 0) {
			return arg.compareTo(a.getArg());
		} else return rc;
	}
	
}
