package vlad.text;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class StringReplacerTest {

    @Test
    public void replaceWords() {
        String originalString = "— Або візьміть історію з Роббі1,— провадила вона далі.";
        HashMap<String, String> replacements = new HashMap<>();
        replacements.put("Роббі", "Lenni");
        replacements.put("rapide", "vif");
        replacements.put("paresseux", "fainéant");
        String replacedString = StringReplacer.replaceWords(originalString, replacements);
        System.out.println("Original String: " + originalString);
        System.out.println("Replaced String: " + replacedString);
    }
}