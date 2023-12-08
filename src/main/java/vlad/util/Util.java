package vlad.util;

public class Util {

    public static String cleanupSentence(String sent) {
        String ret = sent.replace("\n", " ").replace("  ", " ");
        int firstLetterPos = 0;
        for (int i = 0; i < ret.length(); i++) {
            if (Character.isLetterOrDigit(ret.charAt(i))) {
                firstLetterPos = i;
                break;
            }
        }
        if (firstLetterPos > 0) {
            ret = ret.substring(firstLetterPos);
        }
        return ret;
    }

    public static boolean containsOnly(String str, String possibleChars) {
        boolean ret = true;
        for (int i = 0; i < str.length();) {
            int cp = str.codePointAt(i);
            if (!possibleChars.contains(Character.toString(cp))) {
                ret = false;
                break;
            }
            i += Character.isSupplementaryCodePoint(cp) ? 2 : 1;
        }
        return ret;
    }
}
