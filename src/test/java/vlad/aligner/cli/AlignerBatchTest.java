package vlad.aligner.cli;

import org.junit.Before;
import org.junit.Test;
import vlad.Const;
import vlad.aligner.wuo.Word;
import vlad.aligner.wuo.WordForm;
import vlad.aligner.wuo.db.DbTranslator;

import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

public class AlignerBatchTest {

    private AlignerBatch alignerBatch;

    @Test
    public void initAlignerBatch() {
        if (alignerBatch == null) {
            alignerBatch = new AlignerBatch();
        }
    }

    @Test
    public void doBatchJob() {
        if (alignerBatch == null) {
            alignerBatch = new AlignerBatch();
        }
        alignerBatch.doBatchJob(Const.HOME_DIR+"/Dokumente/lib/ua_en/b/Burroughs Edgar/Burroughs, Edgar Rіce - Tarzan");
    }

    @Test
    public void readData() throws Exception {
        if (alignerBatch == null) {
            alignerBatch = new AlignerBatch();
        }
        alignerBatch.readData(Const.HOME_DIR+"/Dokumente/lib/ua_en/a/Abercrombie, Joe/entities.txt");
        DbTranslator db = new DbTranslator(alignerBatch.connectionMemDb);
        System.out.println("---- en wf ----");
        List<WordForm> wfs = db.getWordForms("Dogman", new Locale("en"));
        wfs.forEach(w->System.out.println(w.getWf()));
        System.out.println("---- uk wf ----");
        List<WordForm> ukWfs = db.getWordForms("Шукач", new Locale("uk"));
        ukWfs.forEach(w->System.out.println(w.getWf()));
        System.out.println("---- tr ----");
        List<Word> words = db.getTranslation("Dogman", new Locale("en"), new Locale("uk"));
        words.forEach(w->System.out.println(w.asString()));
    }

    @Test
    public void readDataAndBatch() throws Exception {
        if (alignerBatch == null) {
            alignerBatch = new AlignerBatch();
        }
        alignerBatch.readData(Const.HOME_DIR+"/Dokumente/lib/ua_en/a/Abercrombie, Joe/entities.txt");
        alignerBatch.doBatchJob(Const.HOME_DIR+"/Dokumente/lib/ua_en/a/Abercrombie, Joe/Abercrombie, Joe - Before They Are Hanged");
    }

    @Test
    public void testDoSplFile() throws Exception {
        AlignerBatch alignerBatch = new AlignerBatch();
        alignerBatch.doUkSplFile(Const.HOME_DIR+"/Dokumente/lib/ua_en/b/Burroughs Edgar/Burroughs, Edgar Rіce - Tarzan", true);
    }

    @Test
    // Process all files in library
    public void alignerBatch() throws Exception {
        if (alignerBatch == null) {
            alignerBatch = new AlignerBatch();
        }
        //alignerBatch.storeParSentInFile = true;
        alignerBatch.libRoot = Const.HOME_DIR+"/Dokumente/lib/ua_en/";
        alignerBatch.alignerBatch(Const.HOME_DIR+"/Dokumente/lib/ua_en/bin/convert_spl.txt");
    }
}