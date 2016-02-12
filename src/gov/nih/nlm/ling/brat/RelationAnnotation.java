package gov.nih.nlm.ling.brat;

import gov.nih.nlm.ling.core.SpanList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a n-ary relation annotation. Each argument is associated with a specific role. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class RelationAnnotation extends AbstractAnnotation {

	protected final List<AnnotationArgument> arguments;

	/**
	 * Creates a <code>RelationAnnotation</code> object 
	 * 
	 * @param id  annotation id
	 * @param docId  document id
	 * @param type  relation semantic type
	 * @param args  the typed argument list
	 */
	public RelationAnnotation(String id, String docId, String type, 
			List<AnnotationArgument> args) {
		super(id, docId, "Relation", type);
		this.arguments = args;
	}
	
	/**
	 * @return the character offsets of the leftmost argument or null if no arguments
	 */
	// TODO Selecting the leftmost argument is arbitrary, might be better off with taking all spans.
	public SpanList getSpan() {
		SpanList sp = null;
		if (arguments == null || arguments.size() == 0) return sp;
		Collections.sort(arguments,AnnotationArgument.SPAN_ORDER);
		return arguments.get(0).getArg().getSpan();
	}
	
	public List<AnnotationArgument> getArguments() {
		return arguments;
	}

	/**
	 * Gets arguments with a specific role.
	 * 
	 * @param role	the specific role to search for
	 * @return a list of <code>Annotation</code> objects with the role, or empty list if no such argument exists
	 */
	public List<Annotation> getArgumentsWithRole(String role) {
		List<Annotation> args = new ArrayList<>();
		if (arguments == null || arguments.size() == 0) return args;
		for (AnnotationArgument a: arguments) {
			if (a.getRole().equals(role)) args.add(a.getArg());
		}
		return args;
	}
	
	/**
	 * Gets the sublist of a given typed argument list having the specific role.
	 * 
	 * @param args	the typed argument list
	 * @param role	the role to search for
	 * 
	 * @return a sublist of typed arguments, or empty list if no such arguments exist
	 */
	public static List<AnnotationArgument> getArgumentsWithRole(List<AnnotationArgument> args, String role) {
		List<AnnotationArgument> ret = new ArrayList<>();
		if (args == null || args.size() == 0) return ret;
		for (AnnotationArgument a: args) {
			if (a.getRole().equals(role)) ret.add(a);
		}
		return ret;
	}
	
	/**
	 * Returns the role-argument pair with the given annotation as argument.
	 * 
	 * @param ann the annotation to search for
	 * 
	 * @return the role-argument pair, or null if no pair can be found
	 */
	public AnnotationArgument getArgumentWithAnnotation(Annotation ann) {
		if (arguments == null || arguments.size() == 0) return null;
		for (AnnotationArgument arg: arguments) {
			if (arg.getArg().equals(ann)) return arg;
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(id + "\t" + type + " ");
		if (arguments == null || arguments.size() == 0) return buf.toString().trim();
		for (AnnotationArgument a: arguments) {
			buf.append(a.toString() + " ");
		}
		return buf.toString().trim();
	}
	
	/**
	 * @return an easier-to-read text representation
	 */
	public String toResolvedString() {
		StringBuffer buf = new StringBuffer();
		buf.append(type);
		if (arguments == null || arguments.size() == 0) return buf.toString().trim();
		for (AnnotationArgument a: arguments) {
			buf.append(" " + a.getRole() + ":" + a.getArg().toResolvedString());
		}
		return buf.toString().trim();
	}
	
	/**
	 * Checks whether the relation annotation matches exactly another annotation
	 * by comparing the typed arguments
	 * 
	 * @param a  annotation to compare with 
	 * @return true if exact match
	 */	
	public boolean exactMatch(Annotation a) {
		if (a instanceof RelationAnnotation == false) return false;
		if (arguments == null || arguments.size() == 0) return false;
		RelationAnnotation at = (RelationAnnotation) a;
		if (type.equals(at.getType()) == false) return false;
		List<AnnotationArgument> atArgs = at.getArguments();
		if (atArgs == null || atArgs.size() == 0) return false;
		if (arguments.size() != atArgs.size()) return false;
		List<AnnotationArgument> seenA = new ArrayList<>();
		List<AnnotationArgument> seenB = new ArrayList<>();
		for (AnnotationArgument arg: arguments) {
			String role = arg.getRole();
			Annotation ann = arg.getArg();
			List<AnnotationArgument> matching = getArgumentsWithRole(atArgs,role);
			for (AnnotationArgument match: matching) {
				if (seenB.contains(match)) continue;
				Annotation matchAnn = match.getArg();
				if (ann.exactMatch(matchAnn))  {
					seenA.add(arg);
					seenB.add(match);
					break;
				}
			} 
			if (seenA.contains(arg) == false) return false;
		}
		return (seenB.size() == atArgs.size() && seenA.size() == arguments.size());
	}
	
	/**
	 * Checks whether the relation annotation approximately matches another annotation.
	 * Semantic type and argument match is required but the character offset overlap is sufficient.
	 * 
	 * @param a  annotation to compare with 
	 * @return true if approximate match
	 */
	public boolean approximateMatch(Annotation a) {
		if (a instanceof RelationAnnotation == false) return false;
		if (arguments == null || arguments.size() == 0) return false;
		RelationAnnotation at = (RelationAnnotation) a;
		if (type.equals(at.getType()) == false) return false;
		List<AnnotationArgument> atArgs = at.getArguments();
		if (atArgs == null || atArgs.size() == 0) return false;
		if (arguments.size() != atArgs.size()) return false;
		List<AnnotationArgument> seenA = new ArrayList<>();
		List<AnnotationArgument> seenB = new ArrayList<>();
		for (AnnotationArgument arg: arguments) {
			String role = arg.getRole();
			Annotation ann = arg.getArg();
			List<AnnotationArgument> matching = getArgumentsWithRole(atArgs,role);
			for (AnnotationArgument match: matching) {
				if (seenB.contains(match)) continue;
				Annotation matchAnn = match.getArg();
				if (ann.approximateMatch(matchAnn))  {
					seenA.add(arg);
					seenB.add(match);
					break;
				} 
			} 
			if (seenA.contains(arg) == false) return false;
		}
		return (seenB.size() == atArgs.size() && seenA.size() == arguments.size());
	}
	
	/**
	 * Checks whether the normalized relation annotation matches another normalized annotation.
	 * Exact or approximate match can still be specified.
	 * 
	 * @param approximateMatch	whether to perform approximate match for offsets
	 * @param a  annotation to compare with 
	 * @return true if normalized match
	 */
	public boolean referenceMatch(boolean approximateMatch, Annotation a) {
		if (a instanceof RelationAnnotation == false) return false;
		if (arguments == null || arguments.size() == 0) return false;
		RelationAnnotation at = (RelationAnnotation) a;
		if (type.equals(at.getType()) == false) return false;
		List<AnnotationArgument> atArgs = at.getArguments();
		if (atArgs == null || atArgs.size() == 0) return false;
		if (arguments.size() != atArgs.size()) return false;
		List<AnnotationArgument> seenA = new ArrayList<>();
		List<AnnotationArgument> seenB = new ArrayList<>();
		for (AnnotationArgument arg: arguments) {
			String role = arg.getRole();
			Annotation ann = arg.getArg();
			List<AnnotationArgument> matching = getArgumentsWithRole(atArgs,role);
			// TODO This is a terrible idea, I don't really want to use specific roles here.
			if (role.equals("Anaphor") || role.equals("Anaphora")) {
				for (AnnotationArgument match: matching) {
					if (seenB.contains(match)) continue;
					Annotation matchAnn = match.getArg();
					if ((approximateMatch && ann.approximateMatch(matchAnn)) || 
						(!approximateMatch && ann.exactMatch(matchAnn))) {
						seenA.add(arg);
						seenB.add(match);
						break;
					} 
				} 
				if (seenA.contains(arg) == false) return false;
			}
			for (AnnotationArgument match: matching) {
				if (seenB.contains(match)) continue;
				Annotation matchAnn = match.getArg();
				if (ann.referenceMatch(approximateMatch,matchAnn) || lastResortStringMatch(ann,matchAnn)) {
					seenA.add(arg);
					seenB.add(match);
					break;
				} 
			} 
			if (seenA.contains(arg) == false) return false;
		}
		return (seenB.size() == atArgs.size() && seenA.size() == arguments.size());
	}
	
	private boolean lastResortStringMatch(Annotation a, Annotation b) {
		if (a instanceof TermAnnotation == false || b instanceof TermAnnotation == false) return false;
		TermAnnotation ta = (TermAnnotation)a; TermAnnotation tb = (TermAnnotation)b;
		if (ta.getReferences().size() == 0 && tb.getReferences().size() == 0) 
			return ta.getText().equalsIgnoreCase(tb.getText());
		return false;
	}
	
	/**
	 * Checks whether the relation annotation matches another annotation based simply on spans. <p>
	 * In other words, argument spans match as well as the relation type.
	 * 
	 * @param a  annotation to compare with 
	 * @return true if span match
	 */
	public boolean spanMatch(Annotation a) {
		if (a instanceof RelationAnnotation == false) return false;
		if (arguments == null || arguments.size() == 0) return false;
		RelationAnnotation at = (RelationAnnotation) a;
		if (type.equals(at.getType()) == false) return false;
		List<AnnotationArgument> atArgs = at.getArguments();
		if (atArgs == null || atArgs.size() == 0) return false;
		if (arguments.size() != atArgs.size()) return false;
		List<AnnotationArgument> seenA = new ArrayList<>();
		List<AnnotationArgument> seenB = new ArrayList<>();
		for (AnnotationArgument arg: arguments) {
			String role = arg.getRole();
			Annotation ann = arg.getArg();
			List<AnnotationArgument> matching = getArgumentsWithRole(atArgs,role);
			for (AnnotationArgument match: matching) {
				if (seenB.contains(match)) continue;
				Annotation matchAnn = match.getArg();
				if (ann.spanMatch(matchAnn))  {
					seenA.add(arg);
					seenB.add(match);
					break;
				} 
			} 
			if (seenA.contains(arg) == false) return false;
		}
		return (seenB.size() == atArgs.size() && seenA.size() == arguments.size());
	}
	
	public int compareTo(Annotation a) {
		if (a instanceof TermAnnotation) return 1;
		if (a instanceof EventAnnotation || a instanceof EventModificationAnnotation) return -1;
		RelationAnnotation rt = (RelationAnnotation)a;
		int c = getSpan().compareTo(rt.getSpan());
		if (c == 0) {
			int tc = type.compareTo(rt.getType());	
			if (tc == 0) {
				int sc = getArguments().size() -rt.getArguments().size();
				if (sc == 0) {
					for (int i=0; i < getArguments().size(); i++) {
						int ac = getArguments().get(i).compareTo(rt.getArguments().get(i));
						if (ac != 0) return ac;
					}
					return id.compareTo(a.getId());
				} else return sc;
			} else return tc;
		}
		return c;
	}
	
	public int hashCode() {
		int code = 73 * getSpan().hashCode();
		for (AnnotationArgument aa: getArguments()) {
			code += 119 * aa.hashCode();
		}
		return code;
	}
}