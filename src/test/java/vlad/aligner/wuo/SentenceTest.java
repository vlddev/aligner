package vlad.aligner.wuo;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SentenceTest {

    @Test
    public void tokenize() {
        //Sentence sentence = new Sentence("— 2020 Або візьміть ---істо---рію---  --- ---історію з Роббі-1 Роббі-Роббі,— провадила----- вона - далі.");
        Sentence sentence = new Sentence("");
        System.out.println(sentence.getContent());
        System.out.println(sentence.toString());
    }

}