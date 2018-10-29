package gov.nih.nlm.bioscores.exp;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import gov.nih.nlm.bioscores.core.CoreferenceUtils;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;

/**
 * Recognition of demonstrative pronoun mentions.
 * 
 * @author Halil Kilicoglu
 *
 */
public class DemonstrativePronounOps implements ExpressionOps {
	
	public static final List<String> DEMONSTRATIVE_PRONOUNS = Arrays.asList("this","that","these","those"); 

	@Override
	public boolean recognize(SurfaceElement surfaceElement) {
		if (surfaceElement == null) return false;
		Sentence sent = surfaceElement.getSentence();
		if (sent == null) return false;
		List<SynDependency> embeddings = sent.getEmbeddings();
		if (embeddings == null) return false;
		if (surfaceElement.toWordList().size() == 1){
			String lex = surfaceElement.toWordList().get(0).getLemma();
			if (DEMONSTRATIVE_PRONOUNS.contains(lex)) {
				boolean complementizerThat = CoreferenceUtils.complementizerThat(surfaceElement);
				Collection<SynDependency> np = 
					SynDependency.inDependenciesWithTypes(surfaceElement,embeddings,SynDependency.NP_INTERNAL_DEPENDENCIES,true);	
				if (np.size() == 0 && !complementizerThat) return true;
			}
		}
		return false;
	}

}
