package vlad.aligner.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import vlad.aligner.wuo.Corpus;
import vlad.aligner.wuo.ParallelCorpus;
import vlad.aligner.wuo.db.DbTranslator;
import vlad.util.IOUtil;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

@Command(name = "Aligner", version = "Aligner 1.0")
public class Aligner implements Runnable {

    // TODO:  --in-memory-db  - load H2 to memory
    // TODO:  --additional-db  - additional data to be loaded into memory db

    // TODO:  --batch  - process lot of files using params from input file (batch file)

    @Option(names = { "-db", "--db-url" }, description = "JDBC database URL")
    String dbUrl = "";

    @Option(names = { "-u", "--db-user" }, description = "Database user")
    String dbUser = "";

    @Option(names = { "-p", "--db-password" }, description = "Password of database user")
    String dbPassword = "";

    @Option(names = { "-f", "--from" }, description = "File with original text")
    String fileFrom = "en.txt";

    @Option(names = { "-lf", "--lang-from" }, description = "Language of original text (en,de,uk)")
    String langFrom = "en";

    @Option(names = { "-t", "--to" }, description = "File with translation text")
    String fileTo = "uk.txt";

    @Option(names = { "-lt", "--lang-to" }, description = "Language of translation text (en,de,uk)")
    String langTo = "uk";

    @Option(names = { "-fmt", "--input-format" }, description = "Input file format (SPL - sentence per line; ANY - default)")
    String inputFormat = Corpus.UNFORMATTED_TEXT;

    @Option(names = { "-o", "--output" }, description = "Output file")
    String outputFile = "out.spl";

    @Option(names = { "-s", "--store-par-sent" }, description = "Store aligned sentences in the DB")
    boolean storeParSentInDb = false;

    @Option(names = { "-w", "--write-protocol" }, description = "Write protocol to file")
    boolean writeProtocol = false;

    public Aligner() {
        //Register H2 driver
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        if (this.dbUrl.length() == 0) {
            // set default H2 DB
            URL dbFile = getClass().getResource("/aligner.h2.mv.db");
            System.out.println("Use H2 DB-File "+dbFile);
            if (dbFile != null) {
                this.dbUrl = "jdbc:h2:"+dbFile.getFile().replace(".mv.db", "")
                    + ";IFEXISTS=TRUE;MODE=MySQL;DATABASE_TO_LOWER=TRUE";
            }
        }
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void run() {
        if (this.dbUrl.startsWith("jdbc:h2:") && !this.dbUrl.endsWith(";IFEXISTS=TRUE;MODE=MySQL;DATABASE_TO_LOWER=TRUE")) {
            this.dbUrl += ";IFEXISTS=TRUE;MODE=MySQL;DATABASE_TO_LOWER=TRUE";
        }
        try {
            makeText(fileFrom, langFrom, fileTo, langTo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void makeText(String sFile, String langFrom, String sTrFile, String langTo) throws Exception {
        String textFrom = IOUtil.getFileContent(sFile, "utf-8");
        String textTo = IOUtil.getFileContent(sTrFile, "utf-8");
        alignTranslations(textFrom, langFrom, textTo, langTo);
    }

    public void alignTranslations(String textFrom, String langFrom, String textTo, String langTo) throws Exception {
        Date start = new Date();
        Locale locFrom = new Locale(langFrom);
        Locale locTo = new Locale(langTo);

        //Connection ceTr = DAO.getConnection(this.dbUser, this.dbPassword, this.dbUrl, dbJdbcDriver);
        System.out.println("DB: "+this.dbUrl);
        Connection ceTr = DriverManager.getConnection(this.dbUrl, this.dbUser, this.dbPassword);
        DbTranslator translator = new DbTranslator(ceTr);

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
            ParallelCorpus.setProtOut(IOUtil.openFile(this.fileFrom+".protocol.txt", "utf-8"));
        }

        ParallelCorpus pc = new ParallelCorpus(corp1, corp2);
        pc.setName("test");

        //pc.dumpUsageStatsForWordsNotInDict(translator);

        pc.align(translator, 5, 10);

        //dump for debugging
        //pc.dumpMapping();

        //store as XML
        //IOUtil.storeString(sFile+".par.xml", "utf-8", pc.getAsParXML());
        //store as TMX
        //IOUtil.storeString(sFile+".par.tmx", "utf-8", pc.getAsTMX());
        //store as HTML
        IOUtil.storeString(this.fileFrom+".par.html", "utf-8", pc.getAsParHTML());

        boolean writeJson = true;
        translator.storeListOfPairObjects(pc, this.fileFrom, this.storeParSentInDb, writeJson);

        long runTime = ((new Date()).getTime() - start.getTime())/1000;
        System.out.println("=== Statistics ===");
        System.out.println("Corpus 1: language - "+ corp1.getLang().getLanguage() +
                ", sentences - " + corp1.getSentenceList().size());
        System.out.println("Corpus 2: language - "+ corp2.getLang().getLanguage() +
                ", sentences - " + corp2.getSentenceList().size());
        System.out.println("Division points - "+ pc.getCorpusPairCount());
        System.out.println("Done in "+ runTime + " sec.");
        ceTr.close();
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Aligner()).execute(args);
        System.exit(exitCode);
    }
}
