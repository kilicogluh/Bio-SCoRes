package tasks.coref.spl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Section;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.process.SectionSegmenter;

/**
 * This class implements a simple, regular expression-based method to segment SPL documents into
 * sections.<p>
 * Two levels of sections are identified. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class SPLSectionSegmenter implements SectionSegmenter {
	
	// section names used by regex, there were mined from the dataset
	private static Pattern[] sectionNames = {
		Pattern.compile("contraindications", Pattern.CASE_INSENSITIVE),
		Pattern.compile("drug interactions", Pattern.CASE_INSENSITIVE),
		Pattern.compile("precautions", Pattern.CASE_INSENSITIVE),
		Pattern.compile("warnings", Pattern.CASE_INSENSITIVE),
		Pattern.compile("general", Pattern.CASE_INSENSITIVE),
		Pattern.compile("pediatric use", Pattern.CASE_INSENSITIVE),
		Pattern.compile("dosage", Pattern.CASE_INSENSITIVE),
		Pattern.compile("information for patients", Pattern.CASE_INSENSITIVE),
//		Pattern.compile("pregnancy", Pattern.CASE_INSENSITIVE),
		Pattern.compile("geriatric use", Pattern.CASE_INSENSITIVE),
		Pattern.compile("administration", Pattern.CASE_INSENSITIVE),
		Pattern.compile("description section", Pattern.CASE_INSENSITIVE),
		Pattern.compile("contraindications section", Pattern.CASE_INSENSITIVE),
		Pattern.compile("fetal toxicity", Pattern.CASE_INSENSITIVE),
		Pattern.compile("test interactions", Pattern.CASE_INSENSITIVE),
		Pattern.compile("laboratory tests", Pattern.CASE_INSENSITIVE),
		Pattern.compile("indications", Pattern.CASE_INSENSITIVE),
		Pattern.compile("boxed warning", Pattern.CASE_INSENSITIVE),
		Pattern.compile("nursing mothers", Pattern.CASE_INSENSITIVE)
	};

	@Override
	public void segment(Document document) {
		int ind = 0;
		Span titleSpan = null;
		Span sectionSpan = null;
		List<Section> sections = new ArrayList<Section>();
		String text = document.getText();
		String[] lines = text.split("[\n]+");
		for (int i=0;i< lines.length;i++) {
			String line = lines[i];
			int lineInd = text.indexOf(line,ind);
			Span lineSpan = new Span(lineInd,lineInd+line.length());
			List<Word> words = document.getWordsInSpan(lineSpan);
			boolean titleLine = false;
			// Section titles are expected to have less than 5 tokens
			if (words.size() <=5) {
				for (Pattern snp: sectionNames) {
					Matcher m = snp.matcher(line);
					if (m.find() && line.toLowerCase().contains("see ") == false) {
						if (sectionSpan != null) {
							Section sect = new Section(titleSpan,sectionSpan,document);
							sections.add(sect);
						}
						titleSpan = lineSpan;
						sectionSpan = null;
						titleLine = true;
						break;
					} 
				}
			}
			if (titleLine == false) {
				if (sectionSpan == null) 
					sectionSpan = new Span(lineSpan.getBegin(),lineSpan.getEnd());
				else 
					sectionSpan.setEnd(lineSpan.getEnd());	
			}
			ind += lineSpan.length();
		}
		if (sectionSpan != null) {
			Section sect = new Section(titleSpan,sectionSpan,document);
			sections.add(sect);
		}
		document.setSections(sections);
		for (Section sect: sections) {
			segmentSubsections(document,sect);
		}
	}
	
	private void segmentSubsections(Document document, Section sect) {
		int ind = 0;
		Span titleSpan = null;
		Span sectionSpan = null;
		List<Section> subSections = new ArrayList<Section>();
		String text = document.getStringInSpan(sect.getTextSpan());
		int offset = sect.getTextSpan().getBegin();
		String[] lines = text.split("[\n]+");
		for (int i=0;i< lines.length;i++) {
			String line = lines[i];
			int lineInd = text.indexOf(line,ind)+offset;
			Span lineSpan = new Span(lineInd,lineInd+line.length());
			List<Word> words = document.getWordsInSpan(lineSpan);
			boolean titleLine = false;
			if (words.size() == 0) continue;
			if (words.get(words.size()-1).getText().equals(".") == false || words.get(0).getPos().equals("CD")) {
				titleLine = true;
				for (Word w: words) {
					if (w.isVerbal()) titleLine = false;
				}
				if (itemizedLine(words)) titleLine = false;
				if (titleLine) {
					if (sectionSpan != null) {
						Section subsect = new Section(titleSpan,sectionSpan,document);
						subSections.add(subsect);
					}
					titleSpan = lineSpan;
					sectionSpan = null;
					continue;
				}
			}
			if (titleLine == false) {
				if (sectionSpan == null) 
					sectionSpan = new Span(lineSpan.getBegin(),lineSpan.getEnd());
				else 
					sectionSpan.setEnd(lineSpan.getEnd());	
			} 
			ind += lineSpan.length() - offset;
		}
		if (sectionSpan != null && sectionSpan.equals(sect.getTextSpan()) == false) {
			Section subsect = new Section(titleSpan,sectionSpan,document);
			subSections.add(subsect);
		}
		sect.setSubSections(subSections);
	}
	
	private boolean itemizedLine(List<Word> words) {
		if (words.size() == 0) return false;
		return (words.size() > 1 && words.get(0).getText().equals("-"));
	}

}
