package tasks.coref.bionlp;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import edu.stanford.nlp.util.StringUtils;
import gov.nih.nlm.ling.util.FileUtils;

/**
 * A simple utility program to combine a1 and a2 annotation files that come with
 * the BioNLP coreference dataset. They are combined into a file with the extension ann,
 * which can be used as the gold standard.
 * 
 * @author Halil Kilicoglu
 *
 */
public class MergeA1A2 {
	private static Logger log = Logger.getLogger(MergeA1A2.class.getName());	
	
	public static void processDirectory(String in, String out) throws IOException {
		File inDir = new File(in);
		if (inDir.isDirectory() == false) return;
		File outDir = new File(out);
		if (outDir.isDirectory() == false) return;
		int fileNum = 0;
		List<String> files = FileUtils.listFiles(in, false, "txt");
		for (String filename: files) {
			String id = filename.substring(filename.lastIndexOf(File.separator)+1).replace(".txt", "");
			log.info("Processing " + id + ":" + ++fileNum);
			String a1Filename = inDir.getAbsolutePath() + File.separator + id + ".a1";
			if (new File(a1Filename).exists() == false) continue;
			String a2Filename = inDir.getAbsolutePath() + File.separator + id + ".a2";
			String outFilename = outDir.getAbsolutePath() + File.separator + id + ".ann";
			if (new File(outFilename).exists()) continue;
			List<String> linesA1 = FileUtils.linesFromFile(a1Filename, "UTF8");
			List<String> linesA2 = FileUtils.linesFromFile(a2Filename, "UTF8");
			linesA1.addAll(linesA2);
			FileUtils.write(StringUtils.join(linesA1, "\n"),new File(outFilename));
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.print("Usage: inputDirectory outputDirectory");
		}
		String inDir = args[0];
		String outDir = args[1];
		processDirectory(inDir,outDir);
	}

}
