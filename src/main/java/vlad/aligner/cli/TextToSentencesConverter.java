package vlad.aligner.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import vlad.aligner.wuo.Corpus;
import vlad.aligner.wuo.Sentence;
import vlad.util.IOUtil;
import vlad.util.Util;

@Command(name = "TextToSentencesConverter", version = "TextToSentencesConverter 0.5", mixinStandardHelpOptions = true)
public class TextToSentencesConverter implements Runnable {

    public static final String METHOD_STANFORD = "stanford";
    @Option(names = { "-i", "--input" }, description = "Input file")
    String inputFile = "in.txt";

    @Option(names = { "-l", "--input-lang" }, description = "Language of input file (en,de,uk)")
    String inputLang = "en";

    @Option(names = { "-o", "--output" }, description = "Output file in sentence per line format")
    String outputFile = "out.spl";

    @Option(names = { "-m", "--method" }, description = "Conversion method (default / stanford)")
    String conversionMethod = "default";

    @Option(names = { "-f", "--force" }, description = "Overwrite output file")
    boolean overwriteOutput = false;

    public void run() {
        if (!overwriteOutput && Files.exists(Path.of(outputFile))) {
            throw new RuntimeException("Output file already exists.");
        }
        List<String> sentences;
        if (METHOD_STANFORD.equals(conversionMethod.toLowerCase())) {
            sentences = stanfordConverter(getInputFileContent());
        } else {
            sentences = defaultConverter(getInputFileContent());
        }
        // Write the sentences to a new file
        try (PrintWriter writer = IOUtil.openFile(outputFile, StandardCharsets.UTF_8.name())) {
            for(String sent : sentences) {
                writer.println(sent);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getInputFileContent() {
        try {
            return Files.readString(Paths.get(inputFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> defaultConverter(String text) {
        List<String> ret = new ArrayList<>();
        Corpus corpus = new Corpus(text);

        for (Sentence sentence : corpus.getSentenceList()) {
            String s = sentence.getContent().replace("\n"," ").trim();
            if (s.length() > 0) {
                ret.add(s);
            }
        }
        return ret;
    }

    public List<String> stanfordConverter(String text) {
        // Set up the CoreNLP pipeline with the sentence annotator
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit");
        //props.setProperty("ssplit.isOneSentence", "false");
        //props.setProperty("ssplit.boundaryTokenRegex", "\\\\.|[!?\u2026]+");
        //props.setProperty("ssplit.tokenPatternsToDiscard", "Mrs.,Dr.,Mr.,...,..,хв.");
        //props.setProperty("ssplit.tokenPatternsToDiscard", "\\b(?:Dr|Mr|Mrs)\\.");

        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        List<String> ret = new ArrayList<>();
        // Create an annotation object and annotate the text
        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        // Extract the sentences from the annotation
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            String s = sentence.toString().trim();
            s = s.replace("\n", "");
            if (s.length() > 0 && !Util.containsOnly(s, Corpus.DIVIDER_CHARS)) {
                ret.add(s);
            }
        }
        return ret;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new TextToSentencesConverter()).execute(args);
        System.exit(exitCode);
    }
}
