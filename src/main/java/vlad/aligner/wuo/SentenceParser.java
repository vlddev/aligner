package vlad.aligner.wuo;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class SentenceParser {

    public static List<Token> toTokens(String sentence) {
        List<Token> ret = new ArrayList<>();
        StringTokenizer fIn = new StringTokenizer(sentence, Corpus.DIVIDER_CHARS, true);
        while (fIn.hasMoreTokens()) {
            String token = fIn.nextToken();
            if (Corpus.DIVIDER_CHARS.contains(token)) {
                ret.add(new Token(token, true));
            } else {
                ret.addAll(splitWordWithMinus(token));
            }
        }
        return mergeDelimiters(ret);
    }

    public static List<Token> splitWordWithMinus(String word) {
        List<Token> ret = new ArrayList<>();
        String delimBefore = "";
        String delimAfter = "";
        if (word.startsWith("-")) {
            int pos = -1;
            for (int j = 0; j < word.length() && word.charAt(j) == '-'; j++) pos = j;
            delimBefore = word.substring(0, pos+1);
            word = word.substring(pos+1);
        }
        if (word.endsWith("-")) {
            int pos = -1;
            for (int j = word.length()-1; word.charAt(j) == '-'; j--) pos = j;
            delimAfter = word.substring(pos);
            word = word.substring(0, pos);
        }
        if (delimBefore.length() > 0) {
            ret.add(new Token(delimBefore, true));
        }
        if (word.length() > 0) {
            ret.add(new Token(word, false));
        }
        if (delimAfter.length() > 0) {
            ret.add(new Token(delimAfter, true));
        }
        return ret;
    }

    public static List<Token> mergeDelimiters(List<Token> tokens) {
        List<Token> ret = new ArrayList<>();
        String delimiter = "";
        for(int i=0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.isDelimiter()) {
                delimiter += token.getToken();
            } else {
                if (delimiter.length() > 0) {
                    ret.add(new Token(delimiter, true));
                    delimiter = "";
                }
                ret.add(token);
            }
        }
        if (delimiter.length() > 0) {
            ret.add(new Token(delimiter, true));
        }
        return ret;
    }

}
