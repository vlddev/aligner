package vlad.text;

import java.util.Scanner;

public class LineSentenceReader {

	//code of unicode character '…' is \u2026
	private static String END_OF_SENTENCE = ".!?"+'\u2026';
	private static String[] NOT_THE_END = {"Mrs.","Dr.","Mr.","...","..","хв."};

    private Scanner scanner;
	private int pos = 0;

	public LineSentenceReader(String text){
		scanner = new Scanner(text);
	}

    /** Read next sentence (line) from text.
	 * @return A String containing the contents of the sentence
	 *   or null if the end reached
	 */
	public String readSentence() {
		String ret = null;
		if (scanner.hasNextLine()) {
			ret = scanner.nextLine();
		}
		return ret;
	}
}