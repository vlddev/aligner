package vlad.aligner.wuo;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class CorpusTest {

    @Test
    public void testGetSentenceList() throws Exception {
        String text = Files.readString(Paths.get(getClass().getClassLoader().getResource("txt/test_en.txt").toURI()));
        Corpus corp = new Corpus(text);
        Assert.assertEquals(19, corp.getSentenceList().size());
        /*
        for (int i = 0; i < corp.getSentenceList().size(); i++) {
            Sentence sent = corp.getSentenceList().get(i);
            System.out.println(i + ": (" + sent.toString().length() +") "+ sent.toString());
        }*/
    }

    @Test
    public void testGetWords() throws Exception {
        String text = Files.readString(Paths.get(getClass().getClassLoader().getResource("txt/test_en.txt").toURI()));
        Corpus corp = new Corpus(text);
        List<String> words = corp.getWordsUsedOnce(true);
        words.forEach(w -> System.out.println(w));
    }
}