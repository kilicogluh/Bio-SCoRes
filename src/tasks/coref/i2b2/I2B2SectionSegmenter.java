package tasks.coref.i2b2;

import java.util.ArrayList;
import java.util.List;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Section;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.process.SectionSegmenter;
import gov.nih.nlm.ling.util.StringUtils;

/**
 * This class implements a simple method to segment i2b2 discharge summaries. <p>
 * The section headers are taken as those lines that end with comma (:)
 * and have at least one token that is capitalized. No subsections are recognized.
 * 
 * @author Halil Kilicoglu
 *
 */
public class I2B2SectionSegmenter implements SectionSegmenter {
	
	@Override
	public void segment(Document document) {
		int ind = 0;
		Span titleSpan = null;
		Span sectionSpan = null;
		List<Section> sections = new ArrayList<>();
		String text = document.getText();
		String[] lines = text.split("[\n]+");
		for (int i=0;i< lines.length;i++) {
			String line = lines[i];
			int lineInd = text.indexOf(line,ind);
			Span lineSpan = new Span(lineInd,lineInd+line.length());
			List<Word> words = document.getWordsInSpan(lineSpan);
			if (words.size() == 0) {
				ind = lineSpan.getEnd();
				continue;
			}
			boolean titleLine = (words.get(words.size()-1).getText().equals(":"));
			boolean atLeastOneWord = false;
			if (titleLine) {
				for (Word w: words) {
					if (StringUtils.isAllLetters(w.getText())) {
						atLeastOneWord = true;
						if (StringUtils.isAllUpperCase(w.getText()) == false) titleLine = false;
					}	
				}
				if (atLeastOneWord && titleLine && sectionSpan != null) {
					Section sect = new Section(titleSpan,sectionSpan,document);
					sections.add(sect);
					titleSpan = lineSpan;
					sectionSpan = null;
				}
			}
			else {
				if (sectionSpan == null) 
					sectionSpan = new Span(lineSpan.getBegin(),lineSpan.getEnd());
				else 
					sectionSpan.setEnd(lineSpan.getEnd());	
			}
			ind = lineSpan.getEnd();
		}
		if (sectionSpan != null) {
			Section sect = new Section(titleSpan,sectionSpan,document);
			sections.add(sect);
		}
		document.setSections(sections);		
	}
}
