package vlad.text;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringReplacer {
    public static String simpleReplaceWords(String originalString, Map<String, String> replacements) {
        // Split by any non-letter, non-digit character
        String[] words = Pattern.compile("[^\\p{L}\\p{Nd}]+").split(originalString);
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (replacements.containsKey(word)) {
                sb.append(replacements.get(word)).append(" ");
            } else {
                sb.append(word).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public static String replaceWords(String originalString, Map<String, String> replacements) {
        // Match any sequence of letters not preceded or followed by another letter or digit
        String regex = "(?<!\\p{L}\\p{Nd}+)(\\p{L}+)(?!\\p{L}\\p{Nd}+)";
        Matcher matcher = Pattern.compile(regex).matcher(originalString);
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;
        while (matcher.find()) {
            String word = matcher.group(1);
            String replacement = replacements.getOrDefault(word, word);
            int startIndex = matcher.start(1);
            int endIndex = matcher.end(1);
            sb.append(originalString, lastIndex, startIndex);
            sb.append(replacement);
            lastIndex = endIndex;
        }
        sb.append(originalString, lastIndex, originalString.length());
        return sb.toString();
    }
}
