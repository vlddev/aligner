package vlad.aligner.wuo;

import junit.framework.TestCase;
import org.junit.Test;
import vlad.util.IOUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CorpusTest {

    @Test
    public void testGetSentenceList() throws Exception {
        String text = Files.readString(Paths.get(getClass().getClassLoader().getResource("test_en.txt").toURI()));
        Corpus corp = new Corpus(text);
        corp.getSentenceList().forEach(sent -> System.out.println(sent.toString()));
    }
}