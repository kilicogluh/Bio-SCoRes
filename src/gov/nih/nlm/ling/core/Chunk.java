package gov.nih.nlm.ling.core;

import java.util.List;

public class Chunk {
	
	List<SurfaceElement> seList;
	String chunkType;
	
	public Chunk(List<SurfaceElement> seList, String chunkType) {
		this.seList = seList;
		this.chunkType = chunkType;
	}
	
	/**
	 * Convert chunk object into string representation
	 * 
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		SurfaceElement se;
		List<Word> wordList;
		for(int i = 0; i < seList.size(); i++) {
			se = seList.get(i);
			wordList = se.toWordList();
			for(int j = 0; j < wordList.size(); j++) {
				sb.append(wordToString(wordList.get(j)));
			}
		}
		return "[ " + sb.toString() + " {" + this.chunkType + "} ]";
	}
	
	public String wordToString(Word w) {
		return w.getText() + " (" + w.getPos() + ", " + w.getLemma() + ") ";
	}
	
	public String getChunkType() {
		return this.chunkType;
	}
	
	public void setChunkType(String chunkType) {
		this.chunkType = chunkType;
	}
	
	public List<SurfaceElement> getSurfaceElementList() {
		return this.seList;
	}
	
	public void setSurfaceElementList(List<SurfaceElement> seList) {
		this.seList = seList;
	}

}
