package vlad.aligner.wuo;

import vlad.util.CountHashtable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class EntityProcessor {
    public static Map<String, List<String>> commonWordsMap = new HashMap<>();

    public static List<String> getCommonWords(Locale lang) {
        List<String> ret = commonWordsMap.get(lang.getLanguage());
        if ( ret == null || ret.size() == 0 ) {
            //load from file
            try {
                //ret = readFromZip(lang.getLanguage()+"_wf_common.txt.zip");
                ret = readFromTxt(lang.getLanguage()+"_wf_common.txt");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            commonWordsMap.put(lang.getLanguage(), ret);
        }
        return ret;
    }

    private static List<String> readFromZip(String resource) throws Exception {
        List<String> lines = new ArrayList<>();
        Path path = Paths.get(EntityProcessor.class.getClassLoader().getResource(resource).toURI());
        try (ZipFile zipFile = new ZipFile(path.toString())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".txt")) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return lines;
    }

    private static List<String> readFromTxt(String resource) throws Exception {
        return Files.readAllLines(Paths.get(EntityProcessor.class.getClassLoader().getResource(resource).toURI()), StandardCharsets.UTF_8);
    }

    public static List<String> getGroupFor(String lemma, List<String> words) {
        List<String> result = new ArrayList<>();
        for (String word : words) {
            if (word.startsWith(lemma)) {
                result.add(word);
            }
        }
        return result;
    }

    public static Map<String, List<String>> groupUkEntities(List<String> words) {
        // Sort the list of words
        Collections.sort(words);

        // Dictionary to store the word groups
        Map<String, List<String>> wordGroups = new HashMap<>();
        List<String> groupedWords = new ArrayList<>();

        // Loop through each word in the list
        for (String word : words) {
            if (!groupedWords.contains(word) && word.length() > 3) {
                String baseWord = word;
                List<String> group0 = getGroupFor(baseWord, words);

                // TODO: define maximum possible word length difference in group
                // group "Max: Max, Maximilian, Maximilians, Maxim" not always make sense
                if (group0.size() == 1) {
                    String baseWord1 = word.substring(0, word.length() - 1);
                    List<String> group1 = getGroupFor(baseWord1, words);
                    if (group1.size() > 1) {
                        groupedWords.addAll(group1);
                        wordGroups.put(baseWord1, group1);
                    } else {
                        groupedWords.addAll(group0);
                        wordGroups.put(baseWord, group0);
                    }
                } else {
                    groupedWords.addAll(group0);
                    wordGroups.put(baseWord, group0);
                }
            }
        }
        //TODO cleanup groups which are subsets of large group
        return wordGroups;
    }

    public static Map<String, String> reverseDict(Map<String, List<String>> dict) {
        Map<String, String> ret = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : dict.entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            for (String elem : value) {
                ret.put(elem, key);
            }
        }
        return ret;
    }

    /**
     * 	remove statistically wrong translation
     * 	If there is entry "Jason_Джейсон=984" then all "Jason_*=num" and "*_Джейсон=num" with num < 984 are wrong
     * @param learnDictStats
     * @return
     */
    public static List<Map.Entry<String, Integer>> removeWrongTranslations(CountHashtable<String> learnDictStats) {
        CountHashtable<String> ret = new CountHashtable<>();
        List<Map.Entry<String, Integer>> entries = learnDictStats.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList());
        List<Map.Entry<String, Integer>> resultEntries = new ArrayList<>(entries);
        for (Map.Entry<String, Integer> entry : entries) {
            String key = entry.getKey();
            String[] str = key.split("_");
            String prefixToDel = str[0]+"_";
            String suffixToDel = "_"+str[1];
            Integer value = entry.getValue();
            List<Map.Entry<String, Integer>> doDelEntries = new ArrayList<>();
            for (Map.Entry<String, Integer> elem : resultEntries) {
                if ((elem.getKey().startsWith(prefixToDel) || elem.getKey().endsWith(suffixToDel)) && value > elem.getValue()) {
                    doDelEntries.add(elem);
                }
            }
            resultEntries.removeAll(doDelEntries);
        }
        return resultEntries;
    }

}
