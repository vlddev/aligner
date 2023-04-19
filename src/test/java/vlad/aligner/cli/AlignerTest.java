package vlad.aligner.cli;

import org.junit.Test;
import vlad.aligner.wuo.Corpus;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class AlignerTest {

    public void alignTranslations() throws Exception {
        Aligner aligner = new Aligner();
        aligner.dbUrl = "jdbc:mariadb://localhost:3333/aligner?useUnicode=true&characterEncoding=UTF-8";
        aligner.dbUser = "app";
        aligner.dbPassword = "app";
        String enText = Files.readString(Paths.get(getClass().getClassLoader().getResource("txt/text1_en.txt").toURI()));
        String ukText = Files.readString(Paths.get(getClass().getClassLoader().getResource("txt/text1_uk.txt").toURI()));
        aligner.alignTranslations(enText, "en", ukText, "uk");
    }

    //@Test
    public void makeText() throws Exception {
        Aligner aligner = new Aligner();
        aligner.dbUrl = "jdbc:mariadb://localhost:3333/aligner?useUnicode=true&characterEncoding=UTF-8";
        aligner.dbUser = "app";
        aligner.dbPassword = "app";
        String enText = Files.readString(Paths.get(getClass().getClassLoader().getResource("txt/text1_en.txt").toURI()));
        String ukText = Files.readString(Paths.get(getClass().getClassLoader().getResource("txt/text1_uk.txt").toURI()));
        // write text to file
        try (PrintWriter out = new PrintWriter(aligner.fileFrom)) {
            out.println(enText);
        }
        try (PrintWriter out = new PrintWriter(aligner.fileTo)) {
            out.println(ukText);
        }
        aligner.alignTranslations(aligner.fileFrom, "en", aligner.fileTo, "uk");

        // delete files
        Files.deleteIfExists(Path.of(aligner.fileFrom));
        Files.deleteIfExists(Path.of(aligner.fileTo));
    }

    //@Test
    public void makeTextSpl() throws Exception {
        Aligner aligner = new Aligner();
        aligner.dbUrl = "jdbc:mariadb://localhost:3333/aligner?useUnicode=true&characterEncoding=UTF-8";
        aligner.dbUser = "app";
        aligner.dbPassword = "app";
        aligner.inputFormat = Corpus.SENTENCE_PER_LINE;
        aligner.makeText("text1_en.txt", "en", "text1_uk.txt", "uk");
    }

    //@Test
    public void makeTextH2Spl() throws Exception {
        Aligner aligner = new Aligner();
        //aligner.dbUrl = "jdbc:h2:mem:aligner;MODE=MariaDB;DATABASE_TO_LOWER=TRUE";
        aligner.inputFormat = Corpus.SENTENCE_PER_LINE;
        aligner.makeText("text1_en.txt", "en", "text1_uk.txt", "uk");
    }

    @Test
    public void makeTextH2() throws Exception {
        Aligner aligner = new Aligner();
        String enText = Files.readString(Paths.get(getClass().getClassLoader().getResource("txt/text1_en.txt").toURI()));
        String ukText = Files.readString(Paths.get(getClass().getClassLoader().getResource("txt/text1_uk.txt").toURI()));
        // write text to file
        try (PrintWriter out = new PrintWriter(aligner.fileFrom)) {
            out.println(enText);
        }
        try (PrintWriter out = new PrintWriter(aligner.fileTo)) {
            out.println(ukText);
        }
        aligner.makeText(aligner.fileFrom, "en", aligner.fileTo, "uk");

        // delete files
        Files.deleteIfExists(Path.of(aligner.fileFrom));
        Files.deleteIfExists(Path.of(aligner.fileTo));
        Files.deleteIfExists(Path.of(aligner.fileFrom+".par.html"));
    }
}