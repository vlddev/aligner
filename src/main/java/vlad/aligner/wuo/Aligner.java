/*
#######################################################################
#
#  vlad-aligner - parallel texts aligner
#
#  Copyright (C) 2009-2010 Volodymyr Vlad
#
#  This file is part of vlad-aligner.
#
#  Foobar is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  Foobar is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
#
#######################################################################
*/

package vlad.aligner.wuo;

import vlad.aligner.wuo.db.DAO;
import vlad.aligner.wuo.db.DbTranslator;
import vlad.util.CountHashtable;
import vlad.util.IOUtil;

import java.io.PrintWriter;
import java.sql.Connection;
import java.util.*;

public class Aligner {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		if (args.length == 4) {
			String txt1 = args[0];
			String lang1 = args[1];
			String txt2 = args[2];
			String lang2 = args[3];
			//check input data
			String[] langList = Locale.getISOLanguages();
			
			if (Arrays.binarySearch(langList, lang1) < 0 ) {
				System.err.println("Unknown language '"+lang1+"'.");
				System.exit(1);
			}
			if (Arrays.binarySearch(langList, lang2) < 0 ) {
				System.err.println("Unknown language '"+lang2+"'.");
				System.exit(1);
			}
			
			makeText(txt1, new Locale(lang1), txt2, new Locale(lang2));
		} else if (args.length == 3) {
			String list = args[0];
			String lang1 = args[1];
			String lang2 = args[2];

			makeAll(list, new Locale(lang1), new Locale(lang2));
		} else {
			System.out.println("Usage: txt1 lang1 txt2 lang2");
			System.out.println("\t or list lang1 lang2");
		}
//		ParallelCorpus.makeText("Anderson Poul - The Man Who Came Early.txt", new Locale("en","EN"),
//			"Anderson Poul - The Man Who Came Early_ua.txt", new Locale("uk","UA"));

//		ParallelCorpus.makeAll("E:\\translation\\par_text_pool\\par_text_list.txt",
//		new Locale("en","EN"),
//		new Locale("uk","UA"));
		
	}

    public static String alignTextsAsHtmlTable(String sTextFrom, Locale locFrom, String sTextTo, Locale locTo, Connection c) throws Exception {
        return alignTexts(sTextFrom, locFrom, sTextTo, locTo, c).getAsHtmlTable();
    }

    public static List<List<String>> alignTextsAsDoubleList(String sTextFrom, Locale locFrom, String sTextTo, Locale locTo, Connection c) throws Exception {
        return alignTexts(sTextFrom, locFrom, sTextTo, locTo, c).getAsDoubleList(true);
    }

    public static ParallelCorpus alignTexts(String sTextFrom, Locale locFrom, String sTextTo, Locale locTo, Connection c) throws Exception {
        Date start = new Date();
        TranslatorInterface translator = new DbTranslator(c);

        List<String> errorList = translator.checkDB(locFrom, locTo);
        if (errorList != null && errorList.size() > 0) {
            for (String s : errorList) {
                System.err.println(s);
            }
            System.err.println("ERROR: DB-inconsistencies detected.");
            throw new Exception("ERROR: DB-inconsistencies detected.");
        }

        //read text in first language
        Corpus corp1 = new Corpus(sTextFrom);
        corp1.setLang(locFrom); //new Locale("en","EN")
        //read text in second language
        Corpus corp2 = new Corpus(sTextTo);
        corp2.setLang(locTo); //new Locale("uk","UA")

        //
        ParallelCorpus.setProtOut(new PrintWriter(System.out));

        ParallelCorpus pc = new ParallelCorpus(corp1, corp2);
        pc.setName("test");

        pc.makeMappingWithWordsUsedOnce(translator);

        long runTime = ((new Date()).getTime() - start.getTime())/1000;
        System.out.println("=== Statistics ===");
        System.out.println("Corpus 1: language - "+ corp1.getLang().getLanguage() +
                ", sentences - " + corp1.getSentenceList().size());
        System.out.println("Corpus 2: language - "+ corp2.getLang().getLanguage() +
                ", sentences - " + corp2.getSentenceList().size());
        System.out.println("Division points - "+ pc.getCorpusPairCount());
        System.out.println("Done in "+ runTime + " sec.");

        return pc;
    }


    public static void makeText(String sFile, Locale locFrom, String sTrFile, Locale locTo) throws Exception {
        Date start = new Date();
//		String dbUser = "root";
//		String dbPwd = "root";
//		String dbUrlTr = "jdbc:mysql://localhost/ua_de_dict?useUnicode=true&characterEncoding=UTF-8";
//		String dbJdbcDriver = "com.mysql.jdbc.Driver";

        String dbUser = System.getProperty("jdbc.user");
        String dbPwd = System.getProperty("jdbc.password");
        String dbUrlTr = System.getProperty("jdbc.url");
        String dbJdbcDriver = System.getProperty("jdbc.driver");

        Connection ceTr = DAO.getConnection(dbUser, dbPwd, dbUrlTr, dbJdbcDriver);
        TranslatorInterface translator = new DbTranslator(ceTr);

        List<String> errorList = translator.checkDB(locFrom, locTo);
        if (errorList != null && errorList.size() > 0) {
            for (String s : errorList) {
                System.err.println(s);
            }
            System.err.println("ERROR: DB-inconsistencies detected.");
            return;
        }

        //read text in first language
        Corpus corp1 = new Corpus(IOUtil.getFileContent(sFile, "utf-8"));
        corp1.setLang(locFrom); //new Locale("en","EN")
        //read text in second language
        Corpus corp2 = new Corpus(IOUtil.getFileContent(sTrFile,"utf-8"));
        corp2.setLang(locTo); //new Locale("uk","UA")

        ParallelCorpus.setProtOut(IOUtil.openFile(sFile+".protocol.txt", "utf-8"));

        ParallelCorpus pc = new ParallelCorpus(corp1, corp2);
        pc.setName("test");

        //pc.dumpUsageStatsForWordsNotInDict(translator);

        pc.makeMappingWithWordsUsedOnce(translator);

        //dump for debugging
        //pc.dumpMapping();

        List<Integer> badSplitPoints = pc.getBadSplitPoints();
        System.out.println("=== Quality ===");
        System.out.println("Split points: " + pc.getMapping().size());
        System.out.println("Bad split points: " + badSplitPoints.size());
        if (pc.getMapping().size() > badSplitPoints.size() * 5) {
           System.out.println("   Remove bad split points. Split ones more.");
           badSplitPoints.forEach((ind) -> {
              pc.removeSplitPoint(ind);
           });
           pc.makeMappingWithWordsUsedOnce(translator);
           badSplitPoints = pc.getBadSplitPoints();
           System.out.println("   Split points: " + pc.getMapping().size());
           System.out.println("   Bad split points: " + badSplitPoints.size());
        }
                
        //store as XML
        //IOUtil.storeString(sFile+".par.xml", "utf-8", pc.getAsParXML());
        //store as TMX
        //IOUtil.storeString(sFile+".par.tmx", "utf-8", pc.getAsTMX());
        //store as HTML
        IOUtil.storeString(sFile+".par.html", "utf-8", pc.getAsParHTML());

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

    public static void makeAll(String listFile, Locale locFrom, Locale locTo) throws Exception {
//		String dbUser = "root";
//		String dbPwd = "root";
//		String dbUrlTr = "jdbc:mysql://localhost/ua_de_dict?useUnicode=true&characterEncoding=UTF-8";
//		String dbJdbcDriver = "com.mysql.jdbc.Driver";

        String dbUser = System.getProperty("jdbc.user");
        String dbPwd = System.getProperty("jdbc.password");
        String dbUrlTr = System.getProperty("jdbc.url");
        String dbJdbcDriver = System.getProperty("jdbc.driver");

        Connection ceTr = DAO.getConnection(dbUser, dbPwd, dbUrlTr, dbJdbcDriver);
        TranslatorInterface translator = new DbTranslator(ceTr);

        List<String> errorList = translator.checkDB(locFrom, locTo);
        if (errorList != null && errorList.size() > 0) {
            for (String s : errorList) {
                System.err.println(s);
            }
            System.err.println("ERROR: DB-inconsistencies detected.");
            return;
        }

        Map<String, String> textMap = new HashMap<String, String>();
        // read data to textMap
        textMap = IOUtil.getFileContentAsMap(listFile, "utf-8", true, null, true);

        String sTrFile;
        for (String sFile : textMap.keySet()) {
            System.out.println("Make text '"+ sFile + "'");
            Date start = new Date();
            sTrFile = textMap.get(sFile);
            //read text in first language
            Corpus corp1 = new Corpus(IOUtil.getFileContent(sFile,"utf-8"));
            corp1.setLang(locFrom);
            //read text in second language
            Corpus corp2 = new Corpus(IOUtil.getFileContent(sTrFile,"utf-8"));
            corp2.setLang(locTo);

            //ParallelCorpus.setProtOut(IOUtil.openFile(sFile+".protocol.txt", "utf-8"));

            ParallelCorpus pc = new ParallelCorpus(corp1, corp2);
            pc.setName("test");

            pc.makeMappingWithWordsUsedOnce(translator);

            //store as XML
            IOUtil.storeString(sFile+".par.xml", "utf-8", pc.getAsParXML());
            //store as TMX
            IOUtil.storeString(sFile+".par.tmx", "utf-8", pc.getAsTMX());
            //store as HTML
            IOUtil.storeString(sFile+".par.html", "utf-8", pc.getAsParHTML());
            long runTime = ((new Date()).getTime() - start.getTime())/1000;
            System.out.println("=== Statistics ===");
            System.out.println("Corpus 1: language - "+ corp1.getLang().getLanguage() +
                    ", sentences - " + corp1.getSentenceList().size());
            System.out.println("Corpus 2: language - "+ corp2.getLang().getLanguage() +
                    ", sentences - " + corp2.getSentenceList().size());
            System.out.println("Division points - "+ pc.getCorpusPairCount());
            System.out.println("Done in "+ runTime + " sec.");
        }

        ceTr.close();
    }

    public static void extractTr(Connection con, ParallelCorpus pc, CountHashtable<String> trStats) {
        TrExtractor trExtractor = new TrExtractor(con);
        trExtractor.setTrStats(trStats);
        for (List<String> pairList : pc.getAsDoubleList(false)) {
           String enSent = pairList.get(0);
           String ukSent = pairList.get(1);
           System.out.println("--------------");
           System.out.println("en:   " + enSent);
           System.out.println("uk:   " + ukSent);
           System.out.println("--------------");
           trExtractor.extractTranslations(ukSent, enSent);
        }
  
    }
}
