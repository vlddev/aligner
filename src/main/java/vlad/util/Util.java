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

}
