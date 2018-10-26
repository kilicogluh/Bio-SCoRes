package gov.nih.nlm.bioscores.core;

import java.util.List;

import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * This class represents a textually grounded coreference chain.
 * Such a chain consists of the textual unit that corresponds to the
 * coreferential mention and a list of textual units that correspond 
 * to the referents.
 * It also keeps track of the resolution strategy that generated
 * this chain.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SurfaceElementChain {

	private Strategy strategy;
	private SurfaceElement expression;
	private List<SurfaceElement> referents;
	
	/**
	 * Constructs a <code>SurfaceElementChain</code> object.
	 * 
	 * @param strategy		the strategy that generated this chain
	 * @param expression	the coreferential mention that anchors this chain
	 * @param referents		the referents that the coreferential mention corefers with
	 */
	public SurfaceElementChain(Strategy strategy, SurfaceElement expression,
			List<SurfaceElement> referents) {
		this.strategy = strategy;
		this.expression = expression;
		this.referents = referents;
	}

	public Strategy getStrategy() {
		return strategy;
	}

	public SurfaceElement getExpression() {
		return expression;
	}

	public List<SurfaceElement> getReferents() {
		return referents;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("EXP("  + expression.toString()+")_REF(");
		for(SurfaceElement ref:referents) {
			buf.append(ref.toString()+"_");
		}
		buf.append(")");
		return buf.toString();
	}

	
}
