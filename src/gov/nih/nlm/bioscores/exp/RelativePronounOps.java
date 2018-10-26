package gov.nih.nlm.bioscores.exp;

import java.util.Arrays;
import java.util.List;

import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * Recognition of relative pronoun mentions.
 * 
 * @author Halil Kilicoglu
 *
 */
public class RelativePronounOps implements ExpressionOps {
	public static final List<String> WH_RELATIVE_PRONOUNS = Arrays.asList("which","whose","who","whom","where");
	
	@Override
	public boolean recognize(SurfaceElement surfaceElement) {
		return surfaceElement.isRelativePronoun();
	}

}
