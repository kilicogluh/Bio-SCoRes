package gov.nih.nlm.ling.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.wrappers.CoreNLPWrapper;


/**
 * A sentence segmentation implementation that relies on Stanford Core NLP package.
 * 
 * @author Halil Kilicoglu
 *
 */
public class StanfordCoreNLPSentenceSegmenter implements SentenceSegmenter {
	private static Logger log = Logger.getLogger(StanfordCoreNLPSentenceSegmenter.class.getName());	
	
	@Override
	public void segment(String inStr, List<Sentence> sentences){
		if (CoreNLPWrapper.instantiated() == false) {
			Properties props = new Properties();
			props.put("annotators", "tokenize,ssplit");
			CoreNLPWrapper.getInstance(props);
		}
		Annotation annotation = CoreNLPWrapper.coreNLP(inStr, false);
	    List<CoreMap> sentenceAnns = annotation.get(SentencesAnnotation.class);  
	    if (sentenceAnns == null || sentenceAnns.size() == 0) return;
		int si = 0;
	    for(CoreMap sentAnn: sentenceAnns) {
	      Sentence sentence = CoreNLPWrapper.getSentence(sentAnn,++si);
	      sentences.add(sentence);
	    }
	}
	
	public static void main(String[] args) {
		System.setProperty("java.util.logging.config.file", "logging.properties");
		String txt = "Seven genes, previously called yqiS, yqiT, " +
				"yqiU, yqiV, bfmBAA, bfmBAB, and bfmBB and now referred to as ptb, bcd, buk, lpd, bkdA1, bkdA2, " +
				"and bkdB, are located downstream from the bkdR gene in B. subtilis. Human immunodeficiency virus type 1 (HIV-1) can establish a persistent " +
				"and latent infection in CD4+ T lymphocytes (W.C.Greene, N.Engl.J. Med.324:308-317, 1991; " + 
				"S.M.Schnittman, M.C.Psallidopoulos, H.C. Lane, L.Thompson, M.Baseler, F.Massari, C.H.Fox, " + 
				"N.P.Salzman, and A.S.Fauci, Science 245:305-308, 1989). Production of HIV-1 from latently " + 
				"infected cells requires host cell activation by T-cell mitogens (T.Folks, D.M.Powell, " + 
				"M.M.Lightfoote, S.Benn, M.A. Martin, and A.S.Fauci, Science 231:600-602, 1986; D.Zagury, " + 
				"J. Bernard, R.Leonard, R.Cheynier, M.Feldman, P.S.Sarin, and R.C. Gallo, " + 
				"Science 231:850-853, 1986). This activation is mediated by the host transcription factor " + 
				"NF-kappa B [G.Nabel and D.Baltimore, Nature (London) 326:711-717, 1987]. "  +
				"We report here that the HIV-1-encoded Nef protein inhibits the induction of NF-kappa B " +
				"DNA-binding activity by T- cell mitogens. However, Nef does not affect the DNA-binding " +
				"activity of other transcription factors implicated in HIV-1 regulation, including SP-1, " +
				"USF, URS, and NF-AT. Additionally, Nef inhibits the induction of HIV-1- and interleukin " +
				"2-directed gene expression, and the effect on HIV-1 transcription depends on an intact " +
				"NF-kappa B-binding site. These results indicate that defective recruitment of NF-kappa B " +
				"may underlie Nef's negative transcriptional effects on the HIV-1 and interleukin 2 " +
				"promoters. Further evidence suggests that Nef inhibits NF-kappa B induction by interfering" +
				" with a signal derived from the T-cell receptor complex. \n";
		try {
			SentenceSegmenter segmenter = new StanfordCoreNLPSentenceSegmenter();
			List<Sentence> sentences = new ArrayList<Sentence>();
			segmenter.segment(txt,sentences);
			for (Sentence s: sentences) {
				log.info("SENTENCE(" + s.getSpan().toString() + "): " +  s.getText());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
