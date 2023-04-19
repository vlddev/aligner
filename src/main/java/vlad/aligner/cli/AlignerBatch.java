package vlad.aligner.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import vlad.aligner.wuo.Corpus;
import vlad.aligner.wuo.ParallelCorpus;
import vlad.aligner.wuo.db.DbTranslator;
import vlad.util.IOUtil;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Command(name = "AlignerBatch", version = "AlignerBatch 1.0")
public class AlignerBatch implements Runnable {

    Logger logger = LoggerFactory.getLogger(AlignerBatch.class);

    // TODO:  --additional-db  - additional data to be loaded into memory db

    @Option(names = { "-db", "--db-url" }, description = "JDBC database URL")
    String dbUrl = "jdbc:h2:./aligner.h2";

    @Option(names = { "-i", "--input" }, description = "File with data to process")
    String inputFile = "align_jobs.txt";

    @Option(names = { "-d", "--data" }, description = "additional data to be loaded into memory db")
    String dataFile = "data.txt";

    boolean storeParSentInDb = false;

    boolean writeProtocol = false;

    private String inMemoryDb = "jdbc:h2:mem:default;MODE=MySQL;DATABASE_TO_LOWER=TRUE"; // ;IGNORECASE=TRUE
    private String inMemoryDbScript = "aligner-script.sql";

    protected Connection connectionMemDb;

    public AlignerBatch() {
        //Register H2 driver
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        if (this.dbUrl.length() > 0) {
            if (this.dbUrl.startsWith("jdbc:h2:") && !this.dbUrl.endsWith(";IFEXISTS=TRUE;MODE=MySQL;DATABASE_TO_LOWER=TRUE")) {
                this.dbUrl += ";IFEXISTS=TRUE;MODE=MySQL;DATABASE_TO_LOWER=TRUE";
            }
            try {
                Date start = new Date();
                if (!Files.exists(Path.of(inMemoryDbScript))) {
                    Connection connection = DriverManager.getConnection(this.dbUrl, "", "");
                    connection.createStatement().execute("SCRIPT TO '"+inMemoryDbScript+"'");
                    connection.close();
                }
                this.connectionMemDb = DriverManager.getConnection(this.inMemoryDb, "", "");
                connectionMemDb.createStatement().execute("RUNSCRIPT FROM '"+inMemoryDbScript+"'");
                long runTime = ((new Date()).getTime() - start.getTime())/1000;
                logger.info("InMemory DB inited in {} sec.", runTime);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void run() {
        try {
            if (Files.exists(Path.of(dataFile))) {
                readData(dataFile);
            }
            alignerBatch(inputFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void readData(String dataFile) throws Exception {
        logger.info("Read additional data from '{}'.", dataFile);
        DbTranslator translator = new DbTranslator(connectionMemDb);
        List<String> lines = Files.readAllLines(new File(dataFile).toPath(), Charset.defaultCharset());
        for (String data : lines) {
            String[] lst = data.split("\t");
            translator.insertEntityWithTranslation(List.of(lst[0].split(",")), List.of(lst[1].split(",")));
        }
    }

    public void alignerBatch(String inputFile) throws Exception {
        List<String> batchData = Files.readAllLines(new File(inputFile).toPath(), Charset.defaultCharset());
        for (String batchJob : batchData) {
            doBatchJob(batchJob);
        }
    }

    public void doBatchJob(String batchJob) {
        Path fileFrom = Path.of(batchJob + "_en.spl");
        String langFrom = "en";
        Path fileTo = Path.of(batchJob + "_uk.spl");
        String langTo = "uk";
        String outFile = batchJob + "_en_uk.spl";
        boolean doJob = true;
        if (!Files.exists(fileFrom)) {
            logger.warn("From file '{}' not exists.", fileFrom);
            doJob = false;
        }
        if (!Files.exists(fileTo)) {
            logger.warn("To file '{}' not exists.", fileTo);
            doJob = false;
        }
        if (doJob) {
            try {
                logger.info("Run job '{}'.", batchJob);
                alignTranslations(Files.readString(fileFrom), langFrom, Files.readString(fileTo), langTo, outFile);
            } catch (Exception e) {
                logger.error("Error in job.", e);
            }
        } else {
            logger.warn("Job '{}' ignored.", batchJob);
        }
    }

    public void alignTranslations(String textFrom, String langFrom, String textTo, String langTo, String outFile) throws Exception {
        Date start = new Date();
        Locale locFrom = new Locale(langFrom);
        Locale locTo = new Locale(langTo);

        DbTranslator translator = new DbTranslator(connectionMemDb);

        List<String> errorList = translator.checkDB(locFrom, locTo);
        if (errorList != null && errorList.size() > 0) {
            for (String s : errorList) {
                System.err.println(s);
            }
            System.err.println("ERROR: DB-inconsistencies detected.");
            return;
        }

        //read text in first language
        Corpus corp1 = new Corpus(textFrom);
        corp1.setLang(locFrom);
        //read text in second language
        Corpus corp2 = new Corpus(textTo);
        corp2.setLang(locTo);

        if (this.writeProtocol) {
            ParallelCorpus.setProtOut(IOUtil.openFile(outFile+".protocol.txt", "utf-8"));
        }

        ParallelCorpus pc = new ParallelCorpus(corp1, corp2);
        pc.setName("test");

        pc.makeMappingWithWordsUsedOnce(translator);

        List<Integer> badSplitPoints = pc.getBadSplitPoints();
        System.out.println("=== Quality ===");
        System.out.println("Split points: " + pc.getMapping().size());
        System.out.println("Bad split points: " + badSplitPoints.size());
        int prevBadSplitPointsSize = badSplitPoints.size();
        int step = 1;
        do {
            prevBadSplitPointsSize = badSplitPoints.size();
            System.out.println("   Step "+step+". Remove bad split points. Split ones more.");
            badSplitPoints.forEach(ind -> pc.removeSplitPoint(ind));
            pc.makeMappingWithWordsUsedOnce(translator);
            badSplitPoints = pc.getBadSplitPoints();
            System.out.println("   Split points: " + pc.getMapping().size());
            System.out.println("   Bad split points: " + badSplitPoints.size());
            step++;
        } while (prevBadSplitPointsSize > badSplitPoints.size()
                && badSplitPoints.size() > 10
                && step < 6);

        System.out.println("==== Store results ====");
        //store as XML
        //IOUtil.storeString(sFile+".par.xml", "utf-8", pc.getAsParXML());
        //store as TMX
        //IOUtil.storeString(sFile+".par.tmx", "utf-8", pc.getAsTMX());
        //store as HTML
        IOUtil.storeString(outFile+".par.html", "utf-8", pc.getAsParHTML());

        boolean writeJson = false;
        translator.storeListOfPairObjects(pc.getAsDoubleList(false), outFile, this.storeParSentInDb, writeJson);

        long runTime = ((new Date()).getTime() - start.getTime())/1000;
        System.out.println("=== Statistics ===");
        System.out.println("Corpus 1: language - "+ corp1.getLang().getLanguage() +
                ", sentences - " + corp1.getSentenceList().size());
        System.out.println("Corpus 2: language - "+ corp2.getLang().getLanguage() +
                ", sentences - " + corp2.getSentenceList().size());
        System.out.println("Division points - "+ pc.getCorpusPairCount());
        System.out.println("Done in "+ runTime + " sec.");
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AlignerBatch()).execute(args);
        System.exit(exitCode);
    }
}
