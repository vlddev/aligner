package vlad.aligner.wuo;

import vlad.text.SentenceReader2;
import vlad.util.CountHashtable;

import java.io.IOException;
import java.util.*;

public class Corpus {
	
	public static String LINE_SEPARATOR = System.getProperty("line.separator");
	public static final String DIVIDER_CHARS = ".,:;!?§$%&/()=[]\\#+*<>{}\"—…«»“”•~^‹› \t\r\n";
	
	private int startPosInParentCorpus = -1;
	private Locale lang;
	private String text;
	private List<Sentence> sentenceList;
	
	public Corpus(String text) {
		// parse to sentences
		initSentenceList(text);
	}

	public Corpus(List<Sentence> sentenceList) {
		this.sentenceList = sentenceList;
		
		//TODO можливо, краще брати частину оригінального тексту, а не реконструювати його 
		StringBuilder sb = new StringBuilder();
		for (Sentence sent : sentenceList) {
			sb.append(sent.getContent());
			sb.append(" ");
		}
		text = sb.toString();
	}
	
	private void initSentenceList(String txt) {
		//preprocessing
		
		// remove first BOM in utf8
		if (txt.length() > 0) {
			byte[] bomArr = txt.substring(0, 1).getBytes();
			if (bomArr.length == 3 && bomArr[0] == (byte)0xEF && bomArr[1] == (byte)0xBB && bomArr[2] == (byte)0xBF) {
				//BOM in utf8
				txt = txt.substring(1);
			}
		}
		
		sentenceList = new ArrayList<Sentence>();
		
		//1. replace "..." with "…"
		txt = txt.replace("...","…");
		//2. replace "’" with "'"
		txt = txt.replace("’","'");
		txt = txt.replace("“","\"");
		txt = txt.replace("”","\"");
		this.text = txt;

		SentenceReader2 sr = new SentenceReader2(txt);
		try {
			String str = sr.readSentence();
			Sentence sent;
			int prevSentPos = 0;
			while(str!=null){
				str = str.replace(LINE_SEPARATOR," ").trim();
				if (str.length() > 0) {
					sent = new Sentence(str);
					sent.setStartPosInText(prevSentPos);
					sentenceList.add(sent);
					prevSentPos += sent.getElemList().size();
				}
				str = sr.readSentence();
				//TODO: check words count in sentence.
				// If no words, add string to the previous sentence
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
//	/**
//	 * Знаходить індекс (номер слова в тексті) першого входження слова wf в цей текст (корпус), починаючи з позиції startPos.
//	 * "Слова" - це елементи списку Sentence.elemList. 
//	 * @param wf
//	 * @param startPos
//	 * @return
//	 */
//	public int getIndexOfWordform(String wf, int startPos) {
//		int ret = -1;
//		int sentInd = getIndexOfSentenceContainingWf(wf, true);
//		if (sentInd > -1) {
//			ret = sentenceList.get(sentInd).getWordformIndex(wf, true);
//		}
//		return ret;
//	}
	
	public int getIndexOfSentenceContainingWf(String wf, boolean ignoreCase) {
		int ret = -1;
		for (int i = 0; i < sentenceList.size() && ret < 0; i++) {
			if (sentenceList.get(i).containsWordform(wf, ignoreCase)) {
				ret = i;
			}
		}
		return ret;
	}
	
	/**
	 * Search position of given word in text.
	 * "Word" means that characters before and after word should be dividers
	 * @param sWord
	 * @param startPos
	 * @return
	 */
	public int getIndexOfWord(String sWord, int startPos) {
		int ret = -1;
		int wPos = startPos;
		String sCharBefore;
		String sCharAfter;
		String text = this.text.toLowerCase();
		do {
			wPos = text.indexOf(sWord, wPos+1);
			if (wPos > -1) {
				if (wPos == 0) { //first word in text
					sCharBefore = " ";
				} else {
					sCharBefore = text.substring(wPos-1, wPos);
				}
				if (wPos+sWord.length() == text.length()) { //last word in text
					sCharAfter = " ";
				} else {
					sCharAfter = text.substring(wPos+sWord.length(), wPos+sWord.length()+1);
				}
				if (DIVIDER_CHARS.contains(sCharBefore) && DIVIDER_CHARS.contains(sCharAfter)) {
					ret = wPos;
				}
			}
		} while (ret == -1 && wPos > -1);
		return ret;
	}
	
	
	public List<String> getWordsUsedOnce(boolean bIgnoreCase) {
		List<String> ret = new ArrayList<String>();
		HashMap<String,Integer> mapWfUsedOnce = new HashMap<String,Integer>();
    	HashSet<String> ignore = new HashSet<String>();
        StringTokenizer st = new StringTokenizer(text, DIVIDER_CHARS);
        String s;
        int i = 0;
        while (st.hasMoreTokens()) {
            s = st.nextToken();
            if (bIgnoreCase) {
            	s = s.toLowerCase();
            }
            if(!s.equals("")) {
            	if (!ignore.contains(s)) {
                	if (mapWfUsedOnce.containsKey(s)) {
                		ignore.add(s);
                		mapWfUsedOnce.remove(s);
	            	} else {
	            		mapWfUsedOnce.put(s, i);
	            	}
            	}
            }
            i++;
        }
        //sort by text order
		TreeMap<Integer, String> map = new TreeMap<Integer, String>();
        for (String wf : mapWfUsedOnce.keySet()) {
			map.put(mapWfUsedOnce.get(wf), wf);
        }
		for (Integer intObj : map.keySet()) {
			ret.add(map.get(intObj));
		}
		return ret;
	}
	
	public CountHashtable<String> getWordUsageStats(boolean bIgnoreCase) {
		CountHashtable<String> ret = new CountHashtable<String>();
        StringTokenizer st = new StringTokenizer(text, DIVIDER_CHARS);
        String s;
        while (st.hasMoreTokens()) {
            s = st.nextToken();
            if (bIgnoreCase) {
            	s = s.toLowerCase();
            }
            if(!s.equals("")) {
            	ret.add(s);
            }
        }
        return ret;
	}
	
	public List<Sentence> getSentenceList() {
		return sentenceList;
	}

	public void setSentenceList(List<Sentence> sentenceList) {
		this.sentenceList = sentenceList;
	}

	public Locale getLang() {
		return lang;
	}

	public void setLang(Locale lang) {
		this.lang = lang;
	}
	
	public int getLength() {
		return text.length();
	}

	public int getStartPosInParentCorpus() {
		return startPosInParentCorpus;
	}

	public void setStartPosInParentCorpus(int startPosInParentCorpus) {
		this.startPosInParentCorpus = startPosInParentCorpus;
	}
}
