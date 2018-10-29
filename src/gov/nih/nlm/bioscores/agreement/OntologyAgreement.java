package gov.nih.nlm.bioscores.agreement;

import java.util.Set;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * Implementation of the agreement constraint based on mapping to the same
 * ontological concept. This can be useful for {@link CoreferenceType#Ontological}
 * type of coreference, even though it is currently unused. <p>
 * 
 * Agreement is predicted if the coreferential mention and the candidate referent map
 * to the same ontological entity (concept).
 * 
 * @author Halil Kilicoglu
 *
 */
public class OntologyAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		Document doc = exp.getSentence().getDocument();
		Set<SurfaceElement> candidates = Document.getOntologyMatches(doc, exp);
		return (candidates != null && candidates.contains(referent));
	}

}
