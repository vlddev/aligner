package vlad.aligner.wuo;

import org.junit.Assert;
import org.junit.Test;
import vlad.Const;
import vlad.util.IOUtil;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

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

    @Test
    public void testExtractEntities() throws Exception {
        //List<String> commonWords = Files.readAllLines(new File(Const.HOME_DIR+"/Dokumente/mydev/db/freq_lists/uk_wf_common.txt").toPath(), Charset.defaultCharset());
        String ukText = Files.readString(Paths.get(Const.HOME_DIR+"/Dokumente/lib/ua_en/c/Crouch, Blake/Dark Matter - Blake Crouch_uk.spl"));
        Corpus corpUk = new Corpus(ukText);
        corpUk.setLang(new Locale("uk"));
        List<String> commonWords = EntityProcessor.getCommonWords(corpUk.getLang());
        List<String> words = corpUk.extractEntities(commonWords, 3);
        words.forEach(w -> System.out.println(w));
    }
}