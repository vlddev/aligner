package vlad.aligner.cli;

import vlad.aligner.wuo.Corpus;

import static org.junit.Assert.*;

public class AlignerTest {

    //@Test
    public void makeText() throws Exception {
        Aligner aligner = new Aligner();
        aligner.dbUrl = "jdbc:mariadb://localhost:3333/aligner?useUnicode=true&characterEncoding=UTF-8";
        aligner.dbUser = "app";
        aligner.dbPassword = "app";
        aligner.makeText("text1_en.txt", "en", "text1_uk.txt", "uk");
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

}