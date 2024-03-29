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
    private String contentLowerCase;
    private Locale lang;
    private List<String> elemList = new ArrayList<String>();
    private List<String> elemListLowerCase = new ArrayList<String>();
    private List<String> delimiterList = new ArrayList<String>();

    public Sentence(String text) {
        content = text;
        // parse sentence
        init();
    }

    private void init() {
        List<Token> tokens = SentenceParser.toTokens(content);
        if (tokens.size() > 0) {
            // first delimiter
            if (!tokens.get(0).isDelimiter()) {
                delimiterList.add("");
            }
            for (Token token : SentenceParser.toTokens(content)) {
                if (token.isDelimiter()) {
                    delimiterList.add(token.getToken());
                } else {
                    elemList.add(token.getToken());
                }
            }
            // last delimiter
            if (!tokens.get(tokens.size()-1).isDelimiter()) {
                delimiterList.add("");
            }
            // init elemListLowerCase
            for (String s : elemList) {
                elemListLowerCase.add(s.toLowerCase());
            }
        } else {
            delimiterList.add("");
        }
        contentLowerCase = content.toLowerCase();
    }

    private void initOld() {
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
        // init elemListLowerCase
        for (String s : elemList) {
            elemListLowerCase.add(s.toLowerCase());
        }
        contentLowerCase = content.toLowerCase();
        //TODO init dividers!!!
    }

    public String getNormalized() {
        StringBuffer sb = new StringBuffer(contentLowerCase);

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
            if (contentLowerCase.indexOf(wfLoverCase) > -1) {
                ret = elemListLowerCase.contains(wfLoverCase);
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
            if (contentLowerCase.indexOf(wfLoverCase) > -1) {
                ret = elemListLowerCase.indexOf(wfLoverCase);
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
        StringBuffer sb = new StringBuffer(content);
        if (word.length() > 0) {
            String wordLowerCase = word.toLowerCase();
            if (contentLowerCase.indexOf(wordLowerCase) == contentLowerCase.lastIndexOf(wordLowerCase)) {
                //слово зустрічається лише раз
                sb.insert(contentLowerCase.indexOf(wordLowerCase) + word.length(), "</b>");
                sb.insert(contentLowerCase.indexOf(wordLowerCase), "<b>");
            } else {
                // слово зустрічається декілька раз, потрібно виділити переше повне слово (а не частину слова)
                int wPos = -1;
                int resPos = -1;
                String sCharBefore;
                String sCharAfter;
                do {
                    wPos = contentLowerCase.indexOf(wordLowerCase, wPos+1);
                    if (wPos > -1) {
                        if (wPos == 0) { //first word in text
                            sCharBefore = " ";
                        } else {
                            sCharBefore = contentLowerCase.substring(wPos-1, wPos);
                        }
                        if (wPos+word.length() == contentLowerCase.length()) { //last word in text
                            sCharAfter = " ";
                        } else {
                            sCharAfter = contentLowerCase.substring(wPos+word.length(), wPos+word.length()+1);
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
        }
        return sb.toString();
    }

    public void replaceEntities(List<String> entities, String replacement) {
        for (int i = 0; i < this.elemList.size(); i++) {
            String elem = this.elemList.get(i);
            if (entities.contains(elem)) {
                this.elemList.set(i, replacement);
            }
        }
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<elemList.size(); i++) {
            sb.append(delimiterList.get(i)).append(elemList.get(i));
        }
        sb.append(delimiterList.get(elemList.size()));
        return sb.toString();
    }

}
