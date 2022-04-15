package vlad.aligner.wuo;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vlad.aligner.wuo.db.DAO;
import vlad.aligner.wuo.db.DbTranslator;
import vlad.util.CountHashtable;

public class TrExtractor {
   private static final Locale enLoc = new Locale("en");
   private static final Locale ukLoc = new Locale("uk");
   TranslatorInterface translator;
   CountHashtable<String> trStats;

   public static void main(String[] args) throws Exception {
      String ukSent = "двері зачинилися.";
      String enSent = "the door closed.";
      String dbUser = System.getProperty("jdbc.user");
      String dbPwd = System.getProperty("jdbc.password");
      String dbUrlTr = System.getProperty("jdbc.url");
      String dbJdbcDriver = System.getProperty("jdbc.driver");
      Connection con = DAO.getConnection(dbUser, dbPwd, dbUrlTr, dbJdbcDriver);
      TrExtractor trExtractor = new TrExtractor(con);
      trExtractor.extractTranslations(ukSent, enSent);
      con.close();
   }

   public TrExtractor(Connection con) {
      translator = new DbTranslator(con);
      List<String> errorList = translator.checkDB(enLoc, ukLoc);
      if (errorList != null && errorList.size() > 0) {
         for (String s : errorList) {
            System.err.println(s);
         }
         System.err.println("ERROR: DB-inconsistencies detected.");
         return;
      }
   }

   public CountHashtable<String> getTrStats() {
      return this.trStats;
   }

   public void setTrStats(CountHashtable<String> trStats) {
      this.trStats = trStats;
   }

   public void extractTranslations(String ukSentStr, String enSentStr) {
      MatchSentence ukSent = new MatchSentence(ukSentStr, ukLoc, this.translator);
      MatchSentence enSent = new MatchSentence(enSentStr, enLoc, this.translator);
      for (MatchSentence.MatchWf enWf : enSent.getMatchWfList()) {

      }

      Iterator var5 = enSent.getMatchWfList().iterator();

      while(true) {
         MatchSentence.MatchWf enWf;
         Map<Word, List<Word>> trMap;
         List<Word> ukTrList;
         List<MatchSentence.MatchWf> ukMatches;
         do {
            if (!var5.hasNext()) {  // end of loop while(true)
               for (MatchSentence.MatchWf enWf1 : enSent.getMatchWfList()) {
                  if (enWf1.matchingWf != null) {
                     if (this.getTrStats() != null) {
                        this.getTrStats().add("" + enWf1.base.getId() + "_" + enWf1.trBase.getId());
                     }
                     System.out.println(enWf1.wf + " , " + enWf1.base + " -> " + enWf1.trBase + ", " + enWf1.matchingWf.wf);
                  }
               }
               return;
            }

            enWf = (MatchSentence.MatchWf)var5.next();
            ukTrList = new ArrayList();
            trMap = new HashMap();
            for (Word enWord : enWf.bases) {
               List<Word> enWordList = new ArrayList();
               enWordList.add(enWord);
               List<Word> trList = translator.getTranslation(enWordList, enLoc, ukLoc);
               trMap.put(enWord, trList);
               for (Word ukWord : trList) {
                  if (!ukTrList.contains(ukWord)) {
                     ukTrList.add(ukWord);
                  }
               }
            }

            ukMatches = ukSent.findMatches(ukTrList);
         } while(ukMatches.size() != 1);

         MatchSentence.MatchWf ukMatch = ukMatches.get(0);
         enWf.matchingWf = ukMatch;
         ukMatch.matchingWf = enWf;
         //Stream var10000 = ukTrList.stream();
         //List var10001 = ukMatch.bases;
         //var10001.getClass();
         List<Word> matchUkWordList = ukTrList.stream().filter(ukMatch.bases::contains).collect(Collectors.toList());
         if (matchUkWordList.size() > 1) {
            System.out.println("More then one uk word for (" + enWf.wf + "," + enWf.matchingWf.wf + ")");
         }

         enWf.trBase = matchUkWordList.get(0);
         List<Word> matchEnWordList = new ArrayList();
         for (Word enWord : trMap.keySet()) {
            if ((trMap.get(enWord)).contains(enWf.trBase)) {
               matchEnWordList.add(enWord);
            }
         }
         if (matchEnWordList.size() > 1) {
            System.out.println("More then one en word for (" + enWf.wf + "," + enWf.matchingWf.wf + ")");
         }

         enWf.base = matchEnWordList.get(0);
      }
   }
}
