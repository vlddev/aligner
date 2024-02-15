package vlad.aligner.wuo;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import vlad.aligner.cli.Aligner;
import vlad.aligner.wuo.db.DbTranslator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

public class TrExtractorTest {

    Connection connection;
    TrExtractor trExtractor;

    @Before
    public void init() throws SQLException {
        vlad.aligner.cli.Aligner aligner = new Aligner();
        connection = DriverManager.getConnection(aligner.getDbUrl(), "", "");
        trExtractor = new TrExtractor(connection, new Locale("en"), new Locale("uk"));
    }

    @Test
    public void extractTranslations() {
        String ukSent = "Дверцята ліфта стулилися, і кабіна пішла вгору.";
        String enSent = "The elevator door closed and the car lifted.";
        JSONObject parSentJson = trExtractor.extractTranslations(enSent, ukSent);
        System.out.println(parSentJson.toString());
        Assert.assertTrue(parSentJson.has("matchq"));
    }

    @Test
    public void match() {
        String ukSent = "Кетрін підвела очі.";
        ukSent = "Мені сказали, що ви вимагаєте свою історію хвороби?";
        ukSent = "– Так, – відповіла Кетрін, кладучи часопис на стіл.";
        ukSent = "– Вам не до вподоби наше лікування?";
        ukSent = "поцікавилася місіс Блекмен.";
        String[] enSentList = {"Katherine looked up.",
        "I've been told you have requested your clinic records?",
        "\"That's correct,\" said Katherine, putting the magazine down.",
        "Have you been unhappy with our care?",
        "asked Ms. Blackman"};
        trExtractor.match(ukSent, List.of(enSentList));
    }
}