package gov.nih.nlm.ling.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.nlp.trees.ModCollinsHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import gov.nih.nlm.ling.core.GappedMultiWord;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.wrappers.CoreNLPWrapper;

/** 
 * A collection of utility methods for manipulating parse trees.
 * 
 * @author Halil Kilicoglu
 *
 */
public class ParseTreeUtils {
	private static Logger log = Logger.getLogger(ParseTreeUtils.class.getName());
	
	public static Map<String,String> yieldConversionMap = null;
	
	static {
		yieldConversionMap = new HashMap<>();
		yieldConversionMap.put("-LRB-","(");
		yieldConversionMap.put("-RRB-", ")");
		yieldConversionMap.put("-LSB-", "[");
		yieldConversionMap.put("-RSB-", "]");
		yieldConversionMap.put("-LCB-", "{");
		yieldConversionMap.put("-RCB-", "}");
		// TODO Not sure these are still useful
		yieldConversionMap.put("``", "\"");
		yieldConversionMap.put("''","\"");
	}
	
	/** 
	 * Representation of a named subtree, to be used with tregex.
	 *
	 */
	public static class NameTreePair {
		private String name;
		private Tree tree;
		
		public NameTreePair(String name, Tree tree) {
			this.name = name;
			this.tree = tree;
		}
		
		public String getName() {
			return name;
		}

		public Tree getTree() {
			return tree;
		}	
	}
	
	/**
	 * A class to deal with creating tregex patterns.
	 * 
	 * @author kilicogluh
	 *
	 */
	private static class TregexPatternFactory {
		
		private static TregexPatternFactory instance;
		private Map<String, TregexPattern> map;
		
		private TregexPatternFactory(){
			map = new HashMap<>();
		}
		
		private Map<String, TregexPattern> getMap(){
			return map;
		}
		
		public static TregexPattern getPattern(String tregex){
			if(instance == null){
				instance = new TregexPatternFactory();
			}
			Map<String, TregexPattern> myMap = instance.getMap();
			TregexPattern pattern = myMap.get(tregex);
			if(pattern == null){
				try{
					pattern = TregexPattern.compile(tregex);
					myMap.put(tregex, pattern);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			return pattern;
		}

	}
	
	/**
	 * Matches a given tregex pattern with a parse tree. 
	 * 
	 * @param root		the top node of the parse tree
	 * @param pattern	the tregex pattern
	 * @return	list of subtrees that match the pattern, empty list if no match is found
	 */
	public static List<Tree> matchPattern(Tree root, String pattern){
		List<Tree> res = new ArrayList<>();
		if (root == null) return res;
		TregexPattern pat = TregexPatternFactory.getPattern(pattern);
		TregexMatcher matcher = pat.matcher(root);
		while (matcher.find()) {
			res.add(matcher.getMatch());
		}
		return res;
	}
	
	/** 
	 * Matches a given tregex pattern with a parse tree and extracts the nodes corresponding to a named tregex node.
	 * 
	 * @param root			the top node of the parse tree
	 * @param pattern		the tregex pattern
	 * @param namedNodes	the list of tregex nodes to extract for each matching subtree
	 * @return	list of named subtrees
	 */
	public static List<NameTreePair> matchNamedNodes(Tree root, String pattern, List<String> namedNodes){
		List<NameTreePair> res = new ArrayList<>();
		if (root == null) return res;
		TregexPattern pat = TregexPatternFactory.getPattern(pattern);
		TregexMatcher matcher = pat.matcher(root);
		while (matcher.find()) {
			for (String name: namedNodes) {
				res.add(new NameTreePair(name,matcher.getNode(name)));
			}
		}
		return res;
	}
	
	/**
	 * Given the relevant sentence and tree, determines the character offsets of subtrees matching a tregex pattern.
	 * 
	 * @param sent		the sentence
	 * @param root		the top node of a parse tree
	 * @param pattern	the tregex pattern.
	 * @return
	 */
/*	public static List<Span> findPatternSpans(Sentence sent, Tree root, String pattern) {
		List<Span> spans = new ArrayList<>();
		List<Tree> nodes = matchPattern(root,pattern);
		if (nodes == null) return spans;
		for (Tree t: nodes) {
			List<Word> tWords = new ArrayList<>();
			if (tWords.size() > 0) {
				for (Word tw: tWords) {
					log.log(Level.FINEST,"Word matching pattern {0} in {1}: {2}.", 
							new Object[]{pattern, CoreNLPWrapper.printTree(root), tw.toString()});
				}
				spans.add(new Span(tWords.get(0).getSpan().getBegin(),tWords.get(tWords.size()-1).getSpan().getEnd()));
			}
		}
		return spans;
	}*/
	
/*	public static Span findTreeSpan(Sentence s, Tree tree, Tree root, Span sp) {
		if (tree == null) return null;
		List<Word> tWords = new ArrayList<Word>();
		int leafSize = tree.getLeaves().size();
//		int index = matchWordsInTree(tree,root,root,s.getWords(),0, tWords, false);
		if (tWords.size() > 0) {
			for (Word tw: tWords) {
				log.debug("TWORD:" + tw.toString());
			}
			log.debug("LEAFS:" + leafSize + "|" +  tWords.size());
			if (tWords.size() > leafSize) {
				for (int j =0; j < tWords.size(); j+=leafSize){
					Span treeSpan = new Span(tWords.get(j).getSpan().getBegin(),tWords.get(j+leafSize-1).getSpan().getEnd());
					if (sp == null || Span.overlap(treeSpan,sp)) 
						return treeSpan;
				}
			}  else
				 return new Span(tWords.get(0).getSpan().getBegin(),tWords.get(tWords.size()-1).getSpan().getEnd());
		}
		return null;
	}*/
			
/*	public static int matchWordsInTree(Tree t, Tree t1, Tree root, List<Word> sentenceWords, int index, List<Word> words, boolean found) {
		if (t1.equals(t)) found = true;
		if (t1.isLeaf()) {
			index++;
			if (found) {
				Tree par = t1.parent(root);
				LabeledWord lbw = new LabeledWord(t1.label(),par.label());
				for (Word sw: sentenceWords) {		
					if (lbw.tag().toString().equals(sw.getPos()) && 
							sw.getIndex() == index) {
//						List<String> leftParentheses = Arrays.asList(new String[]{"(","["});
//						List<String> rightParentheses = Arrays.asList(new String[]{")","]"});
						boolean tokenEqual = lbw.word().toString().equals(sw.getText()) || 
								sw.getText().equals(invertIfNecessary(lbw.word()));
//						 ( leftParentheses.contains(sw.getText()) && lbw.word().equals("-LRB-")) ||
//						 ( rightParentheses.contains(sw.getText()) && lbw.word().equals("-RRB-"));
						if (tokenEqual) words.add(sw);
					}

    			}  			
    		}
		}
		else {
		      Tree[] kids = t1.children();
		      for (int j = 0, n = kids.length; j < n; j++) {
		        index = matchWordsInTree(t,kids[j],root,sentenceWords,index,words, found);
		      }			
		}
		return index;
	}*/
	
	/**
	 * Given the corresponding parse tree, finds the subtree associated with a textual unit. <p>
	 * If the textual unit is gapped, it will find the subtree associated with the first part of
	 * the textual unit.
	 * 
	 * @param root	the parse tree
	 * @param surf	the textual unit
	 * @return	the subtree corresponding with the textual unit, null if no subtree is found
	 */
	public static Tree getCorrespondingSubTree(Tree root, SurfaceElement surf) {
		if (surf instanceof GappedMultiWord) {
			surf = ((GappedMultiWord)surf).first();
		}
		return getCorrespondingSubTree(root,surf.toWordList());
	}
	
	private static Tree getCorrespondingSubTree(Tree root, List<Word> words) {
		if (root == null || words == null || words.size() == 0) return null;
		List<Tree> leaves = root.getLeaves();
		Word fW = words.get(0);
		Word lW = words.get(words.size()-1);
		Sentence sent = fW.getSentence();
		Word ffW = sent.getWordsInSpan(fW.getSpan()).get(0);
		List<Word> lws = sent.getWordsInSpan(lW.getSpan());
		Word llW = lws.get(lws.size()-1);
		List<Word> sentWords = sent.getWords();
		int start = sentWords.indexOf(ffW);
		int end = sentWords.indexOf(llW);
		List<Tree> surfLeaves = new ArrayList<>();
		// CoreNLP, when converting to Tree from a String, normalizes and sometimes in bad ways
		if (leaves.size() <= end || leaves.size() <= start) {
//			surfLeaves = getTreesFromIndexes(leaves,sentWords,start,end);
			return null;
		}
		else surfLeaves = leaves.subList(start, end+1);
		if (surfLeaves.size() == 0) return null;
		Tree first = surfLeaves.get(0);
		if (surfLeaves.size() == 1) return first;
		Tree out = first;
		for (int i=1; i < surfLeaves.size(); i++) {
			Tree t = surfLeaves.get(i);
			out = root.joinNode(out, t);
		}
		return out;
	}
	
	// In case the tree is not normalized in the way we expect
/*	private static List<Tree> getTreesFromIndexes(List<Tree> leaves, List<Word> sentWords,int fromIndex, int toIndex) {
		int j = 0;
		List<Tree> out = new ArrayList<>();
		for (int i=0; i < sentWords.size(); i++) {
			Word iw = sentWords.get(i);
			if (iw.getText().equals(CoreNLPWrapper.printTree(leaves.get(j)).trim())) j++;
			if (j >= fromIndex && j<=toIndex) {
				out.add(leaves.get(j));
				j++;
			}
			if (j > toIndex) break;
		}
		return out;
	}*/
	
/*	private static Tree trimLeaves(Tree root, Tree t,List<Word> words) {
		List<Tree> leaves = t.getLeaves();
		if (leaves.size() <= words.size()) return t;
		Tree nroot = root.deepCopy();
		int num = t.nodeNumber(root);
		Tree nt = nroot.getNodeNumber(num);
		List<Tree> toRemove = new ArrayList<Tree>();
		int wind = 0;
		for (int i=0; i < leaves.size();i++) {
			Tree l = nt.getLeaves().get(i);
			Tree par = l.parent(nroot);
//			Tree parpar = par.parent(nroot);
			LabeledWord lbw = new LabeledWord(l.label(),par.label());
			if (wind == words.size()) {
//				par.removeChild(0);
//				int parNum = parpar.getChildrenAsList().indexOf(par);
//				parpar.removeChild(parNum);
				toRemove.add(par);
				continue;
			}
			Word w = words.get(wind);
			if (lbw.word().equals(w.getText()) == false) {
//				int parNum = parpar.getChildrenAsList().indexOf(par);
//				parpar.removeChild(parNum);
//				par.removeChild(0);
				toRemove.add(par);
			} else {
				wind++;
			}
		}
		for (Tree r: toRemove) {
			Tree par = r.parent(nroot);
			int rnum = par.getChildrenAsList().indexOf(r);
			par.removeChild(rnum);
		}
		return nt;
	}*/
	
	/**
	 * Gets the list of words corresponding to a given tree.
	 * 
	 * @param tree			the tree 
	 * @param sentence		the sentence corresponding to the tree
	 * @param firstOffset	the first character offset of the tree
	 * @return		the list of words corresponding to the tree, empty list if no match can be found
	 */
	public static List<Word> getWordsFromTree(Tree tree, Sentence sentence, int firstOffset) {
		Span sp = getSpanFromTree(tree, sentence, firstOffset);
		if (sp == null) return new ArrayList<>();
		return sentence.getWordsInSpan(sp);
	}
	

	/**
	 * Gets the span of the yield of a tree.
	 * 
	 * @param tree			the tree corresponding to the span
	 * @param sentence		the sentence corresponding to the span
	 * @param firstOffset	the first character offset to look for the tree
	 * @return	the span corresponding to the tree
	 */
	// TODO Not sure how well this works, beginPosition(), endPosition() return -1.
	public static Span getSpanFromTree(Tree tree, Sentence sentence, int firstOffset) {
		List<edu.stanford.nlp.ling.Word> ty = tree.yieldWords();
		if (ty == null || ty.size() == 0) return null;
		int sentOffset = sentence.getSpan().getBegin();
		if (ty.get(0).beginPosition() >= 0)
			return new Span(ty.get(0).beginPosition()+sentOffset, ty.get(ty.size()-1).endPosition()+sentOffset);
		else {
			// means we can not get the positional information from the yield
			// error prone
			int beg = -1;
			int end = -1;
			boolean found = false;
			List<Word> words = sentence.getWords();
			for (int i=0; i < words.size(); i++) {
				Word w = words.get(i);		
				if (w.getSpan().getBegin() < firstOffset) continue;	
				// found the first word
				if (w.getText().equals(convertIfNecessary(ty.get(0).word()))) { found = true; beg = w.getSpan().getBegin();}
				// terribly ad hoc, but the parse tree and the word list is not aligned
				else if (w.getText().equals("etc") && ty.get(0).word().equals("etc.") && words.get(i+1).getText().equals(".")) {
					beg = w.getSpan().getBegin(); end = words.get(i+1).getSpan().getEnd();
					found = true;
				}
				if (found) {
					log.log(Level.FINEST,"Character offset of the tree {0} is {1}.", new Object[]{CoreNLPWrapper.printTree(tree),beg});
					if (ty.size() == 1)  {
						end = w.getSpan().getEnd();
						break;
					}
					else {
						// finding the other words
						Word nw = null;
						for (int j = 1; j < ty.size(); j++) {
							nw = words.get(i+j);	
							if (nw.getText().equals(convertIfNecessary(ty.get(j).word())) == false) {
								found = false;
								break;
							}
						}
						if (found && (ty.size() == 1 || nw != null)) {
							end = nw.getSpan().getEnd();
							break;
						}
					}
				}
			}
			if (beg >= 0 && end >= 0) return new Span(beg,end);
		}
		return null;
	}
	

	/**
	 * Given a word list, finds the heads associated with it.<p>
	 * Currently, it uses a modified version of <code>ModCollinsHeadFinder</code>.
	 * 
	 * @param sentence	the sentence the words belong to
	 * @param words		a word list
	 * @return	the head words
	 */
	// TODO Whether this works well or not has not been thoroughly tested.
	public static List<Word> findHeadListFromTree(Sentence sentence, List<Word> words) {
		int ind = words.size();
		for (int i=0; i < words.size(); i++) {
			Word w = words.get(i);
			if (w.getPos().equals("WDT")) {
				ind = i;
				break;
			}
		}
		words = words.subList(0, ind);
		Tree t = getCorrespondingSubTree(sentence.getTree(), words);
		int beg = words.get(0).getSpan().getBegin();
		int end = words.get(words.size()-1).getSpan().getEnd();
		Span sp = new Span(beg,end);
		String text = sentence.getStringInSpan(sp);
		List<Word> headWords = new ArrayList<>();
		try {
			if (headWords.size() == 0) {
	//			Tree headTree = t.headTerminal(new CollinsHeadFinder());
				Tree headTree = t.headTerminal(new ModCollinsHeadFinder());
				log.log(Level.FINEST,"Head tree for wordlist \"{0}\": {1}", new Object[]{text,CoreNLPWrapper.printTree(headTree)});
				headWords = getWordsFromTree(headTree, sentence, 0);
			}
		} catch (IllegalArgumentException ie) {
			log.log(Level.WARNING,"Error finding head with ModCollinsHeadFinder: {0}", new Object[]{ie.getMessage()});
		}
		return headWords;
	}
	
	/**
	 * Same as {@link #findHeadListFromTree(Sentence, List)}, but this method will return a single word as head.<p>
	 * If more than one head is found with findHeadList(), this method will return the head that appears the last in text.
	 *  
	 *  @param sentence	the sentence that the words belong to
	 *  @param words	the list of words for which the head is sought
	 *  
	 *  @return the word that is head of the list of words
	 */
	public static Word findHeadFromTree(Sentence sentence, List<Word> words) {
		if (words == null) return null;
		List<Word> headWords = findHeadListFromTree(sentence,words);
		if (headWords == null || headWords.size() == 0)
			return null;
		else if (headWords.size() == 1) 
			return headWords.get(0);
		return headWords.get(headWords.size()-1);
	}
	
	// TODO Is this still useful?
	private static String convertIfNecessary(String s) {
		if (yieldConversionMap.containsKey(s)) return yieldConversionMap.get(s);
		return s;
	}
	
}
