package vlad.aligner.cli;

import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class TextToSentencesConverterTest {

    @Test
    public void defaultConverterEn() throws Exception {
        TextToSentencesConverter conv = new TextToSentencesConverter();
        conv.inputLang = "en";
        String text = "Said Nancy, looking at Dr. Brown. Well, you should have told me. Do you have a question, Mr. Wells?";
        List<String> sentList =  conv.defaultConverter(text);
        Assert.assertTrue(sentList.size() == 3);
    }

    @Test
    public void defaultConverterUk() throws Exception {
        TextToSentencesConverter conv = new TextToSentencesConverter();
        conv.inputLang = "uk";
        String text = "Сніг уже вщухав. Поставте тут, будь ласка! Мати питає, чи не хочете ви чаю?";
        List<String> sentList =  conv.defaultConverter(text);
        Assert.assertTrue(sentList.size() == 3);
    }

    @Test
    public void stanfordConverterEn() {
        TextToSentencesConverter conv = new TextToSentencesConverter();
        conv.inputLang = "en";
        String text = "Said Nancy, looking at Dr. Brown. Well, you should have told me. Do you have a question, Mr. Wells?";
        List<String> sentList =  conv.stanfordConverter(text);
        Assert.assertTrue(sentList.size() == 3);
    }

    @Test
    public void stanfordConverterUk() {
        TextToSentencesConverter conv = new TextToSentencesConverter();
        conv.inputLang = "uk";
        String text = "Сніг уже вщухав. Поставте тут, будь ласка! Мати питає, чи не хочете ви чаю?";
        List<String> sentList =  conv.stanfordConverter(text);
        Assert.assertTrue(sentList.size() == 3);
    }

    @Test
    public void runDefaultConverterEn() throws Exception {
        TextToSentencesConverter conv = new TextToSentencesConverter();
        conv.inputLang = "en";
        String text = "Said Nancy, looking at Dr. Brown. Well, you should have told me. Do you have a question, Mr. Wells?";
        // write text to file
        try (PrintWriter out = new PrintWriter(conv.inputFile)) {
            out.println(text);
        }
        conv.run();
        List<String> sentList = Files.readAllLines(Paths.get(conv.outputFile));
        // delete files
        Files.deleteIfExists(Path.of(conv.inputFile));
        Files.deleteIfExists(Path.of(conv.outputFile));
        Assert.assertTrue(sentList.size() == 3);
    }

    @Test
    public void runStanfordConverterEn() throws Exception {
        TextToSentencesConverter conv = new TextToSentencesConverter();
        conv.inputLang = "en";
        conv.conversionMethod = TextToSentencesConverter.METHOD_STANFORD;
        String text = "Said Nancy, looking at Dr. Brown. Well, you should have told me. Do you have a question, Mr. Wells?";
        try (PrintWriter out = new PrintWriter(conv.inputFile)) {
            out.println(text);
        }
        conv.run();
        List<String> sentList = Files.readAllLines(Paths.get(conv.outputFile));
        // delete files
        Files.deleteIfExists(Path.of(conv.inputFile));
        Files.deleteIfExists(Path.of(conv.outputFile));
        Assert.assertTrue(sentList.size() == 3);
    }
}