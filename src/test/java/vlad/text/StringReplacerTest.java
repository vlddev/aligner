package vlad.text;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import vlad.aligner.wuo.Corpus;
import vlad.aligner.wuo.Token;

import java.util.*;

import static org.junit.Assert.*;

public class StringReplacerTest {

    @Test
    public void replaceWords() {
        String originalString = "— Або візьміть історію з Роббі1,— провадила вона далі.";
        HashMap<String, String> replacements = new HashMap<>();
        replacements.put("Роббі", "Lenni");
        replacements.put("rapide", "vif");
        replacements.put("paresseux", "fainéant");
        String replacedString = StringReplacer.replaceWords(originalString, replacements);
        System.out.println("Original String: " + originalString);
        System.out.println("Replaced String: " + replacedString);
    }

    @Test
    public void tokenize() {
        String sentence = "— 2020 Або візьміть ---істо---рію---  --- ---історію з Роббі-1 Роббі-Роббі,— провадила----- вона - далі.";
        List<Token> tokenList = toTokens(sentence);
        tokenList.forEach(System.out::println);
        System.out.println("---------------");
        tokenList = mergeDelimiters(tokenList);
        tokenList.forEach(System.out::println);
        //String[] tokens = StringUtils.splitByCharacterTypeCamelCase(sentence);
        //for(String token : tokens) {
        //    System.out.println("\""+token+"\"");
        //}
    }

    public List<Token> toTokens(String sentence) {
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
        return ret;
    }

    @Test
    public void splitWordWithMinus() {
        String word = "--------sdf--sdf---";
        List<Token> tokenList = splitWordWithMinus(word);
        tokenList.forEach(System.out::println);
    }

    public List<Token> splitWordWithMinus(String word) {
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

    public List<Token> mergeDelimiters(List<Token> tokens) {
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
        return ret;
    }

}