package vlad.text;

import java.util.Scanner;

public class LineSentenceReader {

    private Scanner scanner;

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