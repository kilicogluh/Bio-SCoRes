package gov.nih.nlm.bioscores.core;

import java.util.LinkedHashSet;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.SemanticItem;

/**
 * Represents a semantic object that corresponds to a coreferential mention, 
 * such as an anaphor or a cataphor.
 * 
 * 
 * @author Halil Kilicoglu
 *
 */
public class Expression extends Entity {
		
	/**
	 * Instantiates a <code>Expression</code> object from a textual unit.
	 * 
	 * @param id		the identifier to assign
	 * @param type		the mention type to assign (from {@link ExpressionType})
	 * @param sp		the mention character offsets
	 * @param headSp	the mention head offsets
	 * @param surf		the mention textual unit
	 */
	public Expression(String id, String type, SpanList sp, SpanList headSp, SurfaceElement surf) {
		super(id, type, sp, headSp, surf);	
	}
	
	/**
	 * Determines whether a mention is cataphoric. A mention is considered cataphoric
	 * if it is nominal AND (it contains the token <i>following</i> OR 
	 * it is the first textual unit of the document).
	 * 
	 * @param type	the expression type to consider
	 * @param su	the textual unit in question
	 * @return	true if the mention is cataphoric
	 */
	// TODO This does not currently take into account cataphor instances that are indicated by sentence-initial discourse connectives.
	// TODO In that case, we also need to limit the window to the current sentence
	public static boolean isCataphoric(ExpressionType type, SurfaceElement su) {
		Sentence sent = su.getSentence();
		return ((su.hasSemantics() && filterByExpressions(su).size() > 0) && 
				ExpressionType.NOMINAL_TYPES.contains(type) && 
				(su.containsToken("following") || 
				// first textual unit in the document
				 (sent.equals(sent.getDocument().getSentences().get(0))  && 
				  sent.getSurfaceElements().get(0).equals(su))));
	}
	
	/**
	 * Finds all coreferential mention semantic objects associated with the textual unit.
	 * 
	 * @param surf	the textual unit
	 * 
	 * @return	a set of semantic objects that are coreferential mentions associated with the textual unit
	 */
	public static LinkedHashSet<SemanticItem> filterByExpressions(SurfaceElement surf) {
		return surf.filterSemanticsByClass(Expression.class);
	}
}
