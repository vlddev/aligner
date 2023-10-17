package vlad.aligner.cli;

import org.json.JSONObject;
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
import java.io.FileWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Command(name = "AlignerBatch", version = "AlignerBatch 1.0", mixinStandardHelpOptions = true)
public class AlignerBatch implements Runnable {

    Logger logger = LoggerFactory.getLogger(AlignerBatch.class);

    // TODO:  --additional-db  - additional data to be loaded into memory db

    @Option(names = { "-db", "--db-url" }, description = "JDBC database URL")
    String dbUrl = "jdbc:h2:./aligner.h2";

    @Option(names = { "-i", "--input" }, description = "File with data to process")
    String inputFile = "align_jobs.txt";

    @Option(names = { "-d", "--data" }, description = "additional data to be loaded into memory db")
    String dataFile = "data.txt";

    @Option(names = { "-l", "--libroot" }, description = "Library root folder")
    String libRoot = "";

    String parSentFile = "parSentFile.tsv";
    boolean storeParSentInDb = false;
    boolean storeParSentInFile = false;
    FileWriter parSentFileWriter = null;

    boolean writeProtocol = false;

    boolean overwriteSpl = false;

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
                DbTranslator translator = new DbTranslator(connectionMemDb);
                if (translator.checkDB(new Locale("en"), new Locale("uk")).size() > 0) {
                    connectionMemDb.createStatement().execute("RUNSCRIPT FROM '"+inMemoryDbScript+"'");
                }
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
        logger.info("Start AlignerBatch", dataFile);
        logger.info("  --db-url '{}'.", this.dbUrl);
        logger.info("  --libroot '{}'.", this.libRoot);
        logger.info("  --input '{}'.", this.inputFile);
        logger.info("  --data '{}'.", this.dataFile);
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
        if (this.storeParSentInFile) {
            logger.info("Store all parallel sentences in '{}'.", Path.of(parSentFile).toAbsolutePath().toString());
            this.parSentFileWriter = new FileWriter(this.parSentFile);
        }
        for (String batchJobFile : batchData) {
            doSplFile(libRoot+batchJobFile, this.overwriteSpl);
            doBatchJob(libRoot+batchJobFile);
        }
        if (this.storeParSentInFile) {
            this.parSentFileWriter.flush();
            this.parSentFileWriter.close();
        }
    }

    public void doBatchJob(String batchJobFile) {
        Path fileFrom = Path.of(batchJobFile + "_en.spl");
        String langFrom = "en";
        Path fileTo = Path.of(batchJobFile + "_uk.spl");
        String langTo = "uk";
        String outFile = batchJobFile + "_en_uk.spl";
        Path jsonParFile = Path.of(outFile + ".json");
        boolean doJob = true;
        if (!Files.exists(fileFrom)) {
            logger.warn("From file '{}' not exists.", fileFrom);
            doJob = false;
        }
        if (!Files.exists(fileTo)) {
            logger.warn("To file '{}' not exists.", fileTo);
            doJob = false;
        }
        if (Files.exists(jsonParFile)) {
            logger.warn("JSON-Par-File '{}' exists.", jsonParFile);
            doJob = false;
        }
        if (doJob) {
            try {
                logger.info("Run job '{}'.", batchJobFile);
                alignTranslations(Files.readString(fileFrom), langFrom, Files.readString(fileTo), langTo, outFile);
            } catch (Exception e) {
                logger.error("Error in job.", e);
            }
        } else {
            logger.warn("Job '{}' ignored.", batchJobFile);
        }
    }

    public void doSplFile(String batchJobFile, boolean bForceReplace) {
        doEnSplFile(batchJobFile, bForceReplace);
        doUkSplFile(batchJobFile, bForceReplace);
    }

    public void doUkSplFile(String batchJobFile, boolean overwriteOutput) {
        // convert uk
        TextToSentencesConverter splConverter = new TextToSentencesConverter();
        splConverter.overwriteOutput = overwriteOutput;
        splConverter.inputFile = batchJobFile+"_uk.txt";
        splConverter.outputFile = batchJobFile+"_uk.spl";
        Path inFile = Path.of(splConverter.inputFile);
        if (!Files.exists(inFile)) {
            logger.warn("Input file '{}' not exists.", splConverter.inputFile);
        } else {
            logger.info("Convert Txt to Spl '{}'.", batchJobFile);
            try {
                splConverter.run();
            } catch (Exception e) {
                logger.warn(" Warn: {}", e.getMessage());
            }
        }
    }

    public void doEnSplFile(String batchJobFile, boolean overwriteOutput) {
        // convert en
        TextToSentencesConverter splConverter = new TextToSentencesConverter();
        splConverter.overwriteOutput = overwriteOutput;
        splConverter.conversionMethod = "stanford";
        splConverter.inputFile = batchJobFile+"_en.txt";
        splConverter.outputFile = batchJobFile+"_en.spl";
        Path inFile = Path.of(splConverter.inputFile);
        if (!Files.exists(inFile)) {
            logger.warn("Input file '{}' not exists.", splConverter.inputFile);
        } else {
            logger.info("Convert Txt to Spl '{}'.", batchJobFile);
            try {
                splConverter.run();
            } catch (Exception e) {
                logger.warn(" Warn: {}", e.getMessage());
            }
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
        pc.showSentIndex = false;

        pc.align(translator, 5, 10);

        //store as XML
        //IOUtil.storeString(sFile+".par.xml", "utf-8", pc.getAsParXML());
        //store as TMX
        //IOUtil.storeString(sFile+".par.tmx", "utf-8", pc.getAsTMX());
        //store as HTML
        IOUtil.storeString(outFile+".par.html", "utf-8", pc.getAsParHTML());

        boolean writeJson = false;
        translator.storeListOfPairObjects(pc, outFile, this.storeParSentInDb, writeJson);

        Path jsonFile = Path.of(outFile + ".json");
        if (Files.exists(jsonFile)) {
            logger.warn("JSON file '{}' exists.", jsonFile);
        } else {
            JSONObject json = translator.getParallelCorpusWithEntitiesAsJson(pc);
            json.put("stats", pc.getStatsJson());
            IOUtil.storeString(jsonFile.toString(), StandardCharsets.UTF_8.name(), json.toString(2));
        }

        if (this.storeParSentInFile) {
            //parSentFileWriter.append(translator.getListOfPairObjectsAsTsv(pc));
            parSentFileWriter.append(translator.parallelCorpusJsonToTsv(translator.getAnonymizedParallelCorpusAsJson(pc)));
        }

        long runTime = ((new Date()).getTime() - start.getTime())/1000;
        System.out.println("=== Statistics ===");
        System.out.println("Corpus 1: language - "+ corp1.getLang().getLanguage() +
                ", sentences - " + corp1.getSentenceList().size());
        System.out.println("Corpus 2: language - "+ corp2.getLang().getLanguage() +
                ", sentences - " + corp2.getSentenceList().size());
        System.out.println("Division points - "+ pc.getCorpusPairCount());
        System.out.println("AlignerSTA\t" + outFile
                + "\t" + corp1.getSentenceList().size()
                + "\t" + corp2.getSentenceList().size()
                + "\t" + pc.getCorpusPairCount() );
        System.out.println("Done in "+ runTime + " sec.");
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AlignerBatch()).execute(args);
        System.exit(exitCode);
    }
}
