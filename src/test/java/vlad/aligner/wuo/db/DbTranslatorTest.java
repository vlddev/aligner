package vlad.aligner.wuo.db;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import vlad.aligner.cli.Aligner;
import vlad.aligner.wuo.Word;
import vlad.aligner.wuo.WordForm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;

public class DbTranslatorTest {

    Connection connection;
    DbTranslator dbTranslator;

    @Before
    public void init() throws SQLException {
        Aligner aligner = new Aligner();
        connection = DriverManager.getConnection(aligner.getDbUrl(), "", "");
        dbTranslator = new DbTranslator(connection);
    }

    @After
    public void stop() throws SQLException {
        connection.close();
    }

    @Test
    public void checkDB() {
        List<String> errors = dbTranslator.checkDB(new Locale("en"), new Locale("uk"));
        Assert.assertTrue(errors.size() == 0);
    }

    @Test
    public void getBaseForm() {
        List<Word> words = dbTranslator.getBaseForm("stays", new Locale("en"));
        Assert.assertTrue(words.size() == 1);
        Assert.assertTrue(words.get(0).getInf().equals("stay"));
    }

    @Test
    public void getWordForms() {
        List<WordForm> wfs = dbTranslator.getWordForms("world", new Locale("en"));
        Assert.assertTrue(wfs.size() == 2);
    }

    @Test
    public void getTranslation() {
        List<Word> words = dbTranslator.getTranslation("world", new Locale("en"), new Locale("uk"));
        Assert.assertTrue(words.size() > 0);
    }

    @Test
    public void getWordBasesUsedOnce() {
        List<String> wfList = new ArrayList<>(Arrays.asList("world stays stays".split(" ")));
        Map<String, String> ret = dbTranslator.getWordBasesUsedOnce(new Locale("en"), wfList);
        Assert.assertTrue(ret.size() > 0);
        Assert.assertTrue(ret.containsKey("world"));
    }

    @Test
    public void getWordBasesUsedOnceForUpper() {
        List<String> wfList = new ArrayList<>(Arrays.asList("anton stays stays".split(" ")));
        Map<String, String> ret = dbTranslator.getWordBasesUsedOnce(new Locale("en"), wfList);
        Assert.assertTrue(ret.size() > 0);
        Assert.assertTrue(ret.containsKey("world"));
    }
}