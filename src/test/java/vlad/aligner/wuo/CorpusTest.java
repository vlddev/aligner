package vlad.aligner.wuo;

import junit.framework.TestCase;
import vlad.util.IOUtil;

import java.io.IOException;

public class CorpusTest extends TestCase {

    public void testGetSentenceList() throws IOException {
        Corpus corp = new Corpus(IOUtil.getFileContent("/home/volodymrvlod/Dokumente/lib/aligner_test_uk.txt","utf-8"));
        corp.getSentenceList().forEach(sent -> System.out.println(sent.toString()));
    }
}