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

package vlad.text;

import java.io.IOException;

public class SentenceReader2 {

    //code of unicode character '…' is \u2026
    private static String END_OF_SENTENCE = ".!?"+'\u2026';
    private static String[] NOT_THE_END_EN = {"Mrs.","Dr.","Mr.","Ms.","...",".."};
    private static String[] NOT_THE_END_UK = {"хв", "тис", "див", "св", "рр", "ст", "м", "с", "р", "с", "н", "е"};

    private StringBuffer sbText;
    private int pos = 0;

    public SentenceReader2(String text){
        sbText = new StringBuffer(text);
    }

    /** Read a sentence from text. A sentence is considered to be terminated by any one of following
     * chars: '.', '!', '?', '…', or any combination of these characters.
     * @return A String containing the contents of the sentence, including any termination characters,
     * or null if the end of the stream has been reached.
     */
    public String readSentence() throws IOException{
        StringBuffer ret = new StringBuffer();
        char ch;
        boolean bEOS = false; //End of sentence flag
        boolean bEOF = false; //End of file flag
        boolean bReadAllEOS = true; //read all subsequent EOS or '"' chars
        do{
            if (pos < sbText.length()) {
                ch = sbText.charAt(pos);
                ret.append(ch);
                if(END_OF_SENTENCE.indexOf(ch)>-1){
                    //check exceptions from NOT_THE_END
                    // TODO ignore numbers like 123.123
                    boolean bException = false;
                    String sentence = ret.toString();
                    for (String s : NOT_THE_END_EN) {
                        if (sentence.endsWith(s)) {
                            bException = true;
                            break;
                        }
                    }
                    String lastWord = getLastWord(sentence);
                    if (!bException) {
                        for (String s : NOT_THE_END_UK) {
                            if (s.equals(lastWord)) {
                                bException = true;
                                break;
                            }
                        }
                    }
                    if (!bException) {
                        // get last word, if it has one latter in uppercase -> it is not the EOS
                        if (lastWord.length() == 1 && lastWord.toUpperCase().equals(lastWord)) {
                            bException = true;
                        }
                    }
                    if (!bException) {
                        bEOS = true;
                    }
                }
                if (ch == '\n' && pos > 0 && sbText.charAt(pos-1) == '\n') { // "\n\n" == end of sentence
                    bEOS = true;
                    bReadAllEOS = false;
                }
                pos++;
            } else {
                bEOF = true;
                bEOS = true;
            }
        } while (!bEOS);
        // read all subsequent EOS or '"' chars
        while (bReadAllEOS && true) {
            if (pos < sbText.length()) {
                ch = sbText.charAt(pos);
                if(END_OF_SENTENCE.indexOf(ch)>-1 || ch == '"'){
                    ret.append((char)ch);
                } else {
                    break;
                }
                pos++;
            } else {
                break;
            }
        }

        if(bEOF && ret.length() < 1) return null;
        return ret.toString();
    }

    private String getLastWord(String text) {
        int pos = text.length()-1;
        int endPos = pos;
        int startPos = pos;
        for (int i = pos; i > -1; i--) {
            if (Character.isLetter(text.charAt(i))) {
                endPos = i;
                break;
            }
        }
        for (int i = endPos; i > -1; i--) {
            if (!Character.isLetter(text.charAt(i))) {
                startPos = i;
                break;
            }
        }
        String ret = "";
        if (startPos < endPos) ret = text.substring(startPos+1, endPos+1);
        return ret;
    }
}