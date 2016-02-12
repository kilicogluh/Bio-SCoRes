package gov.nih.nlm.ling.process;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;

/**
 * A sentence segmentation implementation based on work from BioNLP Shared Tasks.
 * It is an attempt to do a better job with molecular biology text, where 
 * the sentences often start with lowercase letters, and organism names contain 
 * periods. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class MedlineSentenceSegmenter implements SentenceSegmenter {
	private static Logger log = Logger.getLogger(MedlineSentenceSegmenter.class.getName());	
	
	private static final Pattern BOUNDARY_PATTERN = Pattern.compile("(.+?)(\\. *(\\r?\\n)+|\\? *(\\r?\\n)+|\\! *(\\r?\\n)+|\\. +|\\? +|\\! +|(\\r?\\n)+|$)");		
	private static final Pattern SENTENCE_PATTERN = Pattern.compile("\\w");
	private static final Pattern BACTERIA_PATTERN = Pattern.compile("( |^)[A-Z]+(\\.(\\r?\\n)+|\\. +|\\?(\\r?\\n)+|\\!(\\r?\\n)+|\\? +|\\! +|(\\r?\\n)+)$");
	private static final Pattern OTHER_PATTERN 	= Pattern.compile("( |^)(Fig|et al)(\\. +)$");
	
	@Override
	public void segment(String inStr, List<Sentence> sentences){
		int id = 0;
		List<String> firstPassSegments = new ArrayList<>();

		Matcher m = BOUNDARY_PATTERN.matcher(inStr);
    	while (m.find()){
    		String text = m.group(1) + m.group(2);
    		log.log(Level.FINEST,"Initial segment: {0}-{1}:{2}", new Object[]{m.start(),text.length(),text});
    		if (SENTENCE_PATTERN.matcher(text).find())  {
    			firstPassSegments.add(text);
    		}
    	}
    	List<String> mergedSegments = mergeSegments(firstPassSegments);
    	int start = 0;
    	for (String ms: mergedSegments) {
    		start = inStr.indexOf(ms,start);
    		log.log(Level.FINEST,"Merged segment: {0}-{1}", new Object[]{start, ms});
    		Sentence s = new Sentence("S" + ++id, ms, new Span(start,start+ms.length()));
			sentences.add(s);
			start += ms.length();
    	}
	}
	
	// this causes problems when the sentence parenthetical is not well-formed. Check out
	// PMC2774163-03-Discussion (S5) file in Infectious_Diseases dataset.
/*	private static int unmatchedParenthesis(String text) {
		int c =0;
		for (int i=0; i< text.length(); i++) {
			if (text.charAt(i) == '(') c++;
			else if (text.charAt(i) == ')') c--;
		}
		return c;
	}*/
	
	private static List<String> mergeSegments(List<String> segments) {
		List<String> segmentedSentences0 = new ArrayList<>();
		List<String> segmentedSentences = new ArrayList<>();
		String temp = "";
		// take care of unmatched parenthesis, 
		// this may be too strict. Sometimes, things are not so well-written
		// but hard to do it any other way.
//		int unmatched = 0;
		for (String s: segments) {
			temp += s;
//			unmatched = unmatchedParenthesis(temp);
			segmentedSentences0.add(temp); 
			temp = "";
		}
		if (segmentedSentences0.size() == 0) return segments;
		// take care of lower letters at the start, but this is not always good. 
		// Sometimes the lowercase letter at the beginning is legitimate. (10089566.S5, for example)
		segmentedSentences.add(segmentedSentences0.get(0));
		String prevSentence = segmentedSentences0.get(0);
		for (int i=1; i < segmentedSentences0.size(); i++) {
			String ss = segmentedSentences0.get(i);
			if (((ss.charAt(0) >= 'a' && ss.charAt(0) <= 'z') && 
					BACTERIA_PATTERN.matcher(prevSentence).find()) ||
					OTHER_PATTERN.matcher(prevSentence).find()){
				segmentedSentences.set(segmentedSentences.size()-1,segmentedSentences.get(segmentedSentences.size()-1) + ss);
			} else {
				segmentedSentences.add(ss);
			}
			prevSentence = ss;
		}
		return segmentedSentences;
	}

	public static void main(String[] args) {
		String txt = "\nSeven genes, previously called yqiS, yqiT, " +
				"yqiU, yqiV, bfmBAA, bfmBAB, and bfmBB and now referred to as ptb, bcd, buk, lpd, bkdA1, bkdA2, " +
				"and bkdB, are located downstream from the bkdR gene in B. subtilis. Human immunodeficiency virus " + ""
				+ "type 1 (HIV-1) can establish a persistent " +
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
		
		SentenceSegmenter segmenter = new MedlineSentenceSegmenter();
		List<Sentence> sentences = new ArrayList<Sentence>();
		segmenter.segment(txt,sentences);
		for (Sentence s: sentences) {
			log.log(Level.INFO,"SENTENCE({0}): {1}.", new Object[]{s.getSpan().toString(),s.getText()});
		}
	}

}
