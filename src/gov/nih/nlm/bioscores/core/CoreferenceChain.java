package gov.nih.nlm.bioscores.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.AbstractRelation;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.Term;
import nu.xom.Attribute;
import nu.xom.Element;

/**
 * This class represents a coreference relation. Just like any other class that 
 * implements {@link Relation} interface, a coreference relation consists of a type 
 * (Anaphora, Cataphora, etc.) and a list of arguments with roles. <p>
 * This class also contains some utility methods to query coreference chains in a document.
 * 
 * @author Halil Kilicoglu
 *
 */
public class CoreferenceChain extends AbstractRelation {
	private static Logger log = Logger.getLogger(CoreferenceChain.class.getName());
		
	/**
	 * Constructs a coreference chain, given an identifier, type, and a list of elements.
	 * 
	 * @param id		the identifier of the chain
	 * @param type		the type of the chain
	 * @param elements	the argument list of the chain
	 */
	public CoreferenceChain(String id, String type, List<Argument> elements) {
		super(id,type,elements);
	}
	
	/**
	 * Gets all semantic types associated with the coreference chain.<p>
	 * It will return the semantic types of the mention arguments, if any, otherwise 
	 * the type currently associated with the chain itself.
	 * 
	 * @return the set of semantic types
	 * 
	 */
	public LinkedHashSet<String> getAllSemtypes() {
		LinkedHashSet<String> all = new LinkedHashSet<>();
		List<SemanticItem> exps = getExpressions();
		if (exps.size() == 0) {
			all.add(getType());
			return all;
		}
		for (SemanticItem exp: exps) {
			all.addAll(exp.getAllSemtypes());
		}
		return all;
	}

	/**
	 * Gets all referents associated with the coreference chain.
	 * 
	 * @return	the list of referent semantic objects of this chain
	 */
	public List<SemanticItem> getReferents() {
		List<SemanticItem> referents = new ArrayList<>();
		List<Argument> args = getArgs(CoreferenceType.valueOf(type).getRefRole().toString());
		for (Argument arg: args) {
			referents.add(arg.getArg());
		}
		return referents;
	}
		
	/**
	 * Gets all coreferential mentions associated with the coreference chain.
	 * 
	 * @return	the list of mentions in this chain
	 */
	public List<SemanticItem> getExpressions() {
		List<SemanticItem> exps = new ArrayList<>();
		List<Argument> args = getArgs(CoreferenceType.valueOf(type).getExpRole().toString());
		for (Argument arg: args) {
			exps.add(arg.getArg());
		}
		return exps;
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(id + "_" + type);
		if (arguments == null) {
			log.log(Level.WARNING,"Malformed coreference in document {0}: {1}.", new Object[]{this.getDocument().getId(),id});
			buf.append("_NOARG");
			return buf.toString();
		}
		for (int i= 0; i < arguments.size(); i++) {
			buf.append("__" + arguments.get(i).getType() + "(" + arguments.get(i).getArg().toString() + ")");
		}
		return buf.toString();
	}
	
	@Override
	public String toShortString() {
		StringBuffer buf = new StringBuffer();
		buf.append(id + "_" + type);
		if (arguments == null) {
			log.log(Level.WARNING,"Malformed coreference in document {0}: {1}.", new Object[]{this.getDocument().getId(),id});
			buf.append("_NOARG");
			return buf.toString();
		}
		for (int i= 0; i < arguments.size(); i++) {
			buf.append("__" + arguments.get(i).getType() + "(" + arguments.get(i).getArg().getId() + ")");
		}
		return buf.toString();
	}
	
	@Override
	public String toStandoffAnnotation() {
		StringBuffer buf = new StringBuffer();
		if (arguments == null) {
			log.log(Level.WARNING,"Malformed coreference in document {0}: {1}.", new Object[]{this.getDocument().getId(),id});
			return buf.toString();
		}
		buf.append(id + "\t" + type + " ");
		for (int i= 0; i < arguments.size(); i++) {
			buf.append(arguments.get(i).getType() + ":" + arguments.get(i).getArg().getId() + " ");
		}
		return buf.toString().trim();
	}
		
	@Override
	public Element toXml() {
     	Element el = new Element("Relation");
     	el.addAttribute(new Attribute("id",id));
     	el.addAttribute(new Attribute("type",type));
     	List<SemanticItem> exps = getExpressions();
     	List<SemanticItem> refs = getReferents();
		for (int i= 0; i < exps.size(); i++) {
			String t = CoreferenceType.valueOf(type).getExpRole().toString();
//			String t = "Anaphora";
//			if (i > 0) t = t + i;
			Element expEl = new Element(t);
			expEl.addAttribute(new Attribute("idref",exps.get(i).getId()));
			el.appendChild(expEl);
		}  	
		for (int i= 0; i < refs.size(); i++) {
			String t = CoreferenceType.valueOf(type).getRefRole().toString();
//			String t = "Antecedent";
//			if (i > 0) t = t + i;
			Element refEl = new Element(t);
			refEl.addAttribute(new Attribute("idref",refs.get(i).getId()));
			el.appendChild(refEl);
		} 
		return el;
	}
	
	@Override
	public int hashCode() {
		return
	    ((id == null ? 59 : id.hashCode()) ^
	     (type  == null ? 89 : type.hashCode()) ^
	     (arguments == null? 109 : arguments.hashCode()));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CoreferenceChain other = (CoreferenceChain) obj;
		if (id == null) {
			if (other.id != null) return false;
		} else if (!id.equals(other.id)) return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return argsEquals(other);
	}
	
	/**
	 * Finds the coreference chains in which the given textual unit acts as the coreferential mention
	 * 
	 * @param exp the mention textual unit
	 * 
	 * @return  a set of coreference chains meeting this criterion, or an empty set if no such chain is found
	 */
	public static LinkedHashSet<CoreferenceChain> getChainsWithMention(SurfaceElement exp) {
		LinkedHashSet<SemanticItem> expSem = Expression.filterByExpressions(exp);
		LinkedHashSet<CoreferenceChain> existings = new LinkedHashSet<>();
		if (expSem == null) return existings;
		for (SemanticItem an: expSem) {
			LinkedHashSet<CoreferenceChain> existing = getChainsWithAnaphor((Term)an);
			if (existing != null) existings.addAll(existing);
		}
		return existings;
	}
	
	/**
	 * Finds all coreference chains involving a textual unit as an argument
	 * 
	 * @param arg  the textual unit
	 * 
	 * @return   a set of coreference chains
	 */
	public static LinkedHashSet<CoreferenceChain> getChainsWithArgument(SurfaceElement arg) {
		Document doc = arg.getDocument();
		LinkedHashSet<SemanticItem> corefs = Document.getSemanticItemsByClass(doc,CoreferenceChain.class);
		LinkedHashSet<CoreferenceChain> chains = new LinkedHashSet<>();
		if (corefs == null) return chains;
		for (SemanticItem coref : corefs) {
			CoreferenceChain cc = (CoreferenceChain)coref;
			for (Argument a: cc.getArguments()) {
				SemanticItem s = a.getArg();
				if (arg.getSemantics().contains(s)) {
					chains.add(cc);
					break;
				}
			}
		}
		return chains;
	}
	
	/**
	 * Finds all coreference chains in which the given semantic object.
	 * 
	 * @param sem  	the semantic object in the coreference chain
	 * 
	 * @return a set of coreference chains with the given term
	 */
	public static LinkedHashSet<CoreferenceChain> getChainsWithTerm(SemanticItem sem) {
		Document doc = sem.getDocument();
		LinkedHashSet<SemanticItem> corefs = Document.getSemanticItemsByClass(doc,CoreferenceChain.class);
		LinkedHashSet<CoreferenceChain> chains = new LinkedHashSet<>();
		if (corefs == null) return chains;
		for (SemanticItem coref : corefs) {
			CoreferenceChain cc = (CoreferenceChain)coref;
			for (Argument a: cc.getArguments()) {
				SemanticItem s = a.getArg();
				if (sem.equals(s)) chains.add(cc);
			}
		}
		return chains;
	}
	
	/**
	 * Finds all coreference chains in which the given semantic object acts in the given role.
	 * 
	 * @param sem  	the semantic object in the coreference chain
	 * @param role  the role of the semantic object
	 * 
	 * @return a set of coreference chains meeting the criteria
	 */
	public static LinkedHashSet<CoreferenceChain> getChainsWithTerm(SemanticItem sem, String role) {
		Document doc = sem.getDocument();
		LinkedHashSet<SemanticItem> corefs = Document.getSemanticItemsByClass(doc,CoreferenceChain.class);
		LinkedHashSet<CoreferenceChain> chains = new LinkedHashSet<>();
		if (corefs == null) return chains;
		for (SemanticItem coref : corefs) {
			CoreferenceChain cc = (CoreferenceChain)coref;
			for (Argument a: cc.getArguments()) {
				SemanticItem s = a.getArg();
				if (sem.equals(s) && (role == null || role.equals(a.getType()))) chains.add(cc);
			}
		}
		return chains;
	}
	
	/**
	 * Finds the anaphora chains where the given term acts as the anaphor.
	 * 
	 * @param term   the coreferential term (anaphor)
	 * 
	 * @return a set of anaphora chains with the term in anaphor role.
	 */
	public static LinkedHashSet<CoreferenceChain> getChainsWithAnaphor(Term term) {
		return getChainsWithTerm(term,"Anaphor");
	}
	
	/**
	 * Finds the anaphora chain that includes the given term as the anaphor.<p>
	 * In case chains involving the term are not merged, it will return the first chain 
	 * in the list of chains.
	 * 
	 * @param term  the anaphor term
	 * 
	 * @return  an anaphora chain involving the term in anaphor role.
	 */
	public static CoreferenceChain getChainWithAnaphor(Term term) {
		return getChainsWithTerm(term,"Anaphor").iterator().next();
	}
	
}
