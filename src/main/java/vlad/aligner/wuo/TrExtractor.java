package vlad.aligner.wuo;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
      this.translator = new DbTranslator(con);
      List errorList = this.translator.checkDB(enLoc, ukLoc);
      if (errorList != null && errorList.size() > 0) {
         Iterator var3 = errorList.iterator();

         while(var3.hasNext()) {
            String s = (String)var3.next();
            System.err.println(s);
         }

         throw new RuntimeException("ERROR: DB-inconsistencies detected.");
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
      Iterator var5 = enSent.getMatchWfList().iterator();

      while(true) {
         MatchSentence.MatchWf enWf;
         ArrayList ukTrList;
         HashMap trMap;
         Iterator var13;
         Word enWord;
         List ukMatches;
         do {
            if (!var5.hasNext()) {
               var5 = enSent.getMatchWfList().iterator();

               while(var5.hasNext()) {
                  enWf = (MatchSentence.MatchWf)var5.next();
                  if (enWf.matchingWf != null) {
                     if (this.getTrStats() != null) {
                        this.getTrStats().add("" + enWf.base.getId() + "_" + enWf.trBase.getId());
                     }

                     System.out.println(enWf.wf + " , " + enWf.base + " -> " + enWf.trBase + ", " + enWf.matchingWf.wf);
                  }
               }

               return;
            }

            enWf = (MatchSentence.MatchWf)var5.next();
            ukTrList = new ArrayList();
            trMap = new HashMap();
            Iterator var9 = enWf.bases.iterator();

            while(var9.hasNext()) {
               enWord = (Word)var9.next();
               List enWordList = new ArrayList();
               enWordList.add(enWord);
               List trList = this.translator.getTranslation(enWordList, enLoc, ukLoc);
               trMap.put(enWord, trList);
               var13 = trList.iterator();

               while(var13.hasNext()) {
                  enWord = (Word)var13.next();
                  if (!ukTrList.contains(enWord)) {
                     ukTrList.add(enWord);
                  }
               }
            }

            ukMatches = ukSent.findMatches(ukTrList);
         } while(ukMatches.size() != 1);

         MatchSentence.MatchWf ukMatch = (MatchSentence.MatchWf)ukMatches.get(0);
         enWf.matchingWf = ukMatch;
         ukMatch.matchingWf = enWf;
         Stream var10000 = ukTrList.stream();
         List var10001 = ukMatch.bases;
         var10001.getClass();
         List matchUkWordList = (List)var10000.filter(var10001::contains).collect(Collectors.toList());
         if (matchUkWordList.size() > 1) {
            System.out.println("More then one uk word for (" + enWf.wf + "," + enWf.matchingWf.wf + ")");
         }

         enWf.trBase = (Word)matchUkWordList.get(0);
         List matchEnWordList = new ArrayList();
         var13 = trMap.keySet().iterator();

         while(var13.hasNext()) {
            enWord = (Word)var13.next();
            if (((List)trMap.get(enWord)).contains(enWf.trBase)) {
               matchEnWordList.add(enWord);
            }
         }

         if (matchEnWordList.size() > 1) {
            System.out.println("More then one en word for (" + enWf.wf + "," + enWf.matchingWf.wf + ")");
         }

         enWf.base = (Word)matchEnWordList.get(0);
      }
   }
}
