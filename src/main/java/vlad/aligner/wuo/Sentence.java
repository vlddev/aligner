/*
#######################################################################
#
#  vlad-aligner - parallel texts aligner
#
#  Copyright (C) 2009-2010 Volodymyr Vlad
#
#  This file is part of vlad-aligner.
#
#  Foobar is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  Foobar is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
#
#######################################################################
*/

package vlad.aligner.wuo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

public class Sentence {

	private int startPosInText = 0;
	private String content;
	private String contentLoverCase;
	private Locale lang;
	private List<String> elemList = new ArrayList<String>();
	private List<String> elemListLoverCase = new ArrayList<String>();
	private List<String> dividerList = new ArrayList<String>();
	
	public Sentence(String text) {
		content = text;
		// parse sentence
		init();
	}

	private void init() {
		StringTokenizer fIn = new StringTokenizer(content, Corpus.DIVIDER_CHARS);
		if(fIn.hasMoreTokens()) {
			String strWord = "";
			do {
				strWord = fIn.nextToken();
				while (strWord.startsWith("-"))  {
					strWord = strWord.substring(1);
				}
				while (strWord.endsWith("-"))  {
					strWord = strWord.substring(0, strWord.length()-1);
				}
				if (strWord.length() > 0) {
					elemList.add(strWord);
				}
			} while(fIn.hasMoreTokens());
		}
		// init elemListLoverCase
		for (String s : elemList) {
			elemListLoverCase.add(s.toLowerCase());
		}
		contentLoverCase = content.toLowerCase();
		//TODO init dividers!!!
	}
	
	public String getNormalized() {
		StringBuffer sb = new StringBuffer(contentLoverCase);

		// remove first BOM in utf8
		if (sb.length() > 0) {
			byte[] bomArr = sb.substring(0, 1).getBytes();
			if (bomArr.length == 3 && bomArr[0] == (byte)0xEF && bomArr[1] == (byte)0xBB && bomArr[2] == (byte)0xBF) {
				//BOM in utf8
				sb.deleteCharAt(0);
			}
		}

		// remove first "char(63)"
		if (sb.length() > 0) {
			byte[] bomArr = sb.substring(0, 1).getBytes();
			if (bomArr.length == 1 && bomArr[0] == (byte)0x3F) {
				sb.deleteCharAt(0);
			}
		}
		
		//відкинути всі символи з множини Corpus.DEVIDER_CHARS на початку та в кінці тексту
		String sChar;
		while(sb.length() > 0) {
			sChar = sb.substring(0, 1);
			if (Corpus.DIVIDER_CHARS.indexOf(sChar) > -1 ) {
				sb.deleteCharAt(0);
			} else {
				break;
			}
		}
		while(sb.length() > 0) {
			sChar = sb.substring(sb.length()-1);
			if (Corpus.DIVIDER_CHARS.indexOf(sChar) > -1 ) {
				sb.deleteCharAt(sb.length()-1);
			} else {
				break;
			}
		}
		return sb.toString();
	}
	
	public boolean containsWordform(String wf, boolean ignoreCase) {
		boolean ret = false;
		if (ignoreCase) {
			String wfLoverCase = wf.toLowerCase();
			if (contentLoverCase.indexOf(wfLoverCase) > -1) {
				ret = elemListLoverCase.contains(wfLoverCase);
			}
		} else {
			if (content.indexOf(wf) > -1) {
				ret = elemList.contains(wf);
			}
		}
		return ret;
	}

	public int getWordformIndex(String wf, boolean ignoreCase) {
		int ret = -1;
		if (ignoreCase) {
			String wfLoverCase = wf.toLowerCase();
			if (contentLoverCase.indexOf(wfLoverCase) > -1) {
				ret = elemListLoverCase.indexOf(wfLoverCase);
			}
		} else {
			if (content.indexOf(wf) > -1) {
				ret = elemList.indexOf(wf);
			}
		}
		ret = ret > -1 ? ret+startPosInText : ret;
		return ret;
	}

	public String getContent() {
		return content;
	}

	public String getContentWithMarkedWord(String word) {
		String wordLoverCase = word.toLowerCase();
		StringBuffer sb = new StringBuffer(content);
		if (contentLoverCase.indexOf(wordLoverCase) == contentLoverCase.lastIndexOf(wordLoverCase)) {
			//слово зустрічається лише раз
			sb.insert(contentLoverCase.indexOf(wordLoverCase) + word.length(), "</b>");
			sb.insert(contentLoverCase.indexOf(wordLoverCase), "<b>");
		} else {
			// слово зустрічається декілька раз, потрібно виділити переше повне слово (а не частину слова)
			
			int wPos = -1;
			int resPos = -1;
			String sCharBefore;
			String sCharAfter;
			do {
				wPos = contentLoverCase.indexOf(wordLoverCase, wPos+1);
				if (wPos > -1) {
					if (wPos == 0) { //first word in text
						sCharBefore = " ";
					} else {
						sCharBefore = contentLoverCase.substring(wPos-1, wPos);
					}
					if (wPos+word.length() == contentLoverCase.length()) { //last word in text
						sCharAfter = " ";
					} else {
						sCharAfter = contentLoverCase.substring(wPos+word.length(), wPos+word.length()+1);
					}
					if (Corpus.DIVIDER_CHARS.indexOf(sCharBefore) > -1 && Corpus.DIVIDER_CHARS.indexOf(sCharAfter) > -1) {
						resPos = wPos;
					}
				}
			} while (resPos == -1 && wPos > -1);
			
			if (resPos > -1 ) {
				sb.insert(resPos + word.length(), "</b>");
				sb.insert(resPos, "<b>");
			} else {
				// слова не знайдено 
			}
		}
		return sb.toString();
	}

	public int getStartPosInText() {
		return startPosInText;
	}

	public void setStartPosInText(int startPosInText) {
		this.startPosInText = startPosInText;
	}

	public List<String> getElemList() {
		return elemList;
	}
	
}
