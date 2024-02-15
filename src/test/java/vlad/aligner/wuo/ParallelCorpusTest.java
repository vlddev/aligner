package vlad.aligner.wuo;

import org.json.JSONObject;
import org.junit.Test;
import vlad.Const;
import vlad.aligner.cli.Aligner;
import vlad.aligner.wuo.db.DbTranslator;
import vlad.util.CountHashtable;
import vlad.util.IOUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ParallelCorpusTest {

    @Test
    public void testAlignEqualBlocks() throws Exception {
        String enText = Files.readString(Paths.get(getClass().getClassLoader().getResource("txt/text1_en.spl").toURI()));
        String ukText = Files.readString(Paths.get(getClass().getClassLoader().getResource("txt/text1_uk.spl").toURI()));
        Corpus corpEn = new Corpus(enText);
        corpEn.setLang(new Locale("en"));
        //read text in second language
        Corpus corpUk = new Corpus(ukText);
        corpUk.setLang(new Locale("uk"));
        ParallelCorpus pc = new ParallelCorpus(corpEn, corpUk);
        pc.addSplitPointToMapping(new ParallelCorpus.SplitPoint(5,5,"fifteen","п'ятнадцять"));
        pc.addSplitPointToMapping(new ParallelCorpus.SplitPoint(18,18,"tragedy","трагедія"));
        int cnt = pc.getCorpusPairCount();
        pc.makeMappingForEqualSubcorpus();
        int cntAfter = pc.getCorpusPairCount();
        List<List<String>> parList = pc.getAsDoubleList(false);
        for (List<String> lst : parList) {
            lst.size();
        }
    }

    @Test
    public void testGetStatisticalEntityTranslations() throws Exception {
        String ukText = Files.readString(Paths.get(Const.HOME_DIR+"/Dokumente/lib/ua_en/b/Burroughs Edgar/Burroughs, Edgar Rіce - Tarzan_uk.spl"));
        Corpus corpUk = new Corpus(ukText);
        corpUk.setLang(new Locale("uk"));
        String enText = Files.readString(Paths.get(Const.HOME_DIR+"/Dokumente/lib/ua_en/b/Burroughs Edgar/Burroughs, Edgar Rіce - Tarzan_en.spl"));
        Corpus corpEn = new Corpus(enText);
        corpEn.setLang(new Locale("en"));
        ParallelCorpus pc = new ParallelCorpus(corpEn, corpUk);
        vlad.aligner.cli.Aligner aligner = new Aligner();
        DbTranslator translator = new DbTranslator(DriverManager.getConnection(aligner.getDbUrl(), "", ""));
        pc.align(translator, 5, 10);
        JSONObject json = translator.getParallelCorpusWithEntitiesAsJson(pc);
        IOUtil.storeString("par.json", StandardCharsets.UTF_8.name(), json.toString(2));
    }

    @Test
    public void testGetAnonymizedParallelCorpusAsJson() throws Exception {
        String ukText = Files.readString(Paths.get(Const.HOME_DIR+"/Dokumente/lib/ua_en/b/Burroughs Edgar/Burroughs, Edgar Rіce - Tarzan_uk.spl"));
        Corpus corpUk = new Corpus(ukText);
        corpUk.setLang(new Locale("uk"));
        String enText = Files.readString(Paths.get(Const.HOME_DIR+"/Dokumente/lib/ua_en/b/Burroughs Edgar/Burroughs, Edgar Rіce - Tarzan_en.spl"));
        Corpus corpEn = new Corpus(enText);
        corpEn.setLang(new Locale("en"));
        ParallelCorpus pc = new ParallelCorpus(corpEn, corpUk);
        vlad.aligner.cli.Aligner aligner = new Aligner();
        DbTranslator translator = new DbTranslator(DriverManager.getConnection(aligner.getDbUrl(), "", ""));
        pc.align(translator, 5, 10);
        JSONObject json = translator.getAnonymizedParallelCorpusAsJson(pc);
        IOUtil.storeString("anon_par.json", StandardCharsets.UTF_8.name(), json.toString(2));
    }

}