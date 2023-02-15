package vlad.aligner.wuo;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONObject;
import vlad.aligner.wuo.db.DAO;
import vlad.aligner.wuo.db.DbTranslator;
import vlad.util.CountHashtable;

public class TrExtractor {
   private static final Locale enLoc = new Locale("en");
   private static final Locale ukLoc = new Locale("uk");
   TranslatorInterface translator;
   CountHashtable<String> trStats;

   public static void main(String[] args) throws Exception {
      String ukSent = "Мій дім.";
      String enSent = "My home.";
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

   public float extractTranslations(String ukSentStr, String enSentStr) {
      MatchSentence ukSent = new MatchSentence(ukSentStr, ukLoc, this.translator);
      MatchSentence enSent = new MatchSentence(enSentStr, enLoc, this.translator);
      for (MatchSentence.MatchWf enWf : enSent.getMatchWfList()) {
         Map<Word, List<Word>> trMap = new HashMap();
         List<Word> ukTrList = new ArrayList();
         List<MatchSentence.MatchWf> ukMatches;
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

         if (ukMatches.size() == 1) {
            MatchSentence.MatchWf ukMatch = ukMatches.get(0);
            enWf.matchingWf = ukMatch;
            ukMatch.matchingWf = enWf;
            List<Word> matchUkWordList = ukTrList.stream().filter(ukMatch.bases::contains).collect(Collectors.toList());
            if (matchUkWordList.size() > 1) {
               //System.out.println("More then one uk word for (" + enWf.wf + "," + enWf.matchingWf.wf + ")");
            }

            enWf.trBase = matchUkWordList.get(0);
            ukMatch.base = enWf.trBase;
            List<Word> matchEnWordList = new ArrayList();
            for (Word enWord : trMap.keySet()) {
               if ((trMap.get(enWord)).contains(enWf.trBase)) {
                  matchEnWordList.add(enWord);
               }
            }
            if (matchEnWordList.size() > 1) {
               //System.out.println("More then one en word for (" + enWf.wf + "," + enWf.matchingWf.wf + ")");
            }

            enWf.base = matchEnWordList.get(0);
            ukMatch.trBase = enWf.base;
         }
      }

      List<String> enInfs = new ArrayList<>();
      List<String> ukInfs = new ArrayList<>();
      List<String> enUkMaps = new ArrayList<>();
      int matchCount = 0;
      // fill stats
      for (MatchSentence.MatchWf enWf1 : enSent.getMatchWfList()) {
         if (enWf1.matchingWf != null) {
            enInfs.add(enWf1.wf+"|"+enWf1.base.getId()+":"+enWf1.base.getInf()+":"+enWf1.base.getType());
            enUkMaps.add(enWf1.wf+"|"+enWf1.base.getId()+":"+enWf1.base.getInf()+":"+enWf1.base.getType()
                    +"|"+enWf1.trBase.getId()+":"+enWf1.trBase.getInf()+":"+enWf1.trBase.getType()+"|"+enWf1.matchingWf.wf);
            if (this.getTrStats() != null) {
               this.getTrStats().add("" + enWf1.base.getId() + "_" + enWf1.trBase.getId());
            }
            matchCount++;
            //System.out.println(enWf1.wf + " , " + enWf1.base.asString() + " -> " + enWf1.trBase.asString() + ", " + enWf1.matchingWf.wf);
         } else {
            if (enWf1.bases.size() == 1) {
               Word base = enWf1.bases.get(0);
               enInfs.add(enWf1.wf+"|"+base.getId()+":"+base.getInf()+":"+base.getType());
            }
         }
      }
      for (MatchSentence.MatchWf ukWf1 : ukSent.getMatchWfList()) {
         if (ukWf1.matchingWf != null) {
            ukInfs.add(ukWf1.wf + "|" + ukWf1.base.getId() + ":" + ukWf1.base.getInf() + ":" + ukWf1.base.getType());
         } else {
            if (ukWf1.bases.size() == 1) {
               Word base = ukWf1.bases.get(0);
               ukInfs.add(ukWf1.wf+"|"+base.getId()+":"+base.getInf()+":"+base.getType());
            }
         }
      }

      // match quotient
      float mq = 0.0F;
      if (enSent.getMatchWfList().size() > 0 && ukSent.getMatchWfList().size() > 0) {
         mq = (float)(matchCount*2)/(float)(enSent.getMatchWfList().size() + ukSent.getMatchWfList().size());
      }

      JSONObject jsonObject = new JSONObject();
      try {
         Field changeMap = jsonObject.getClass().getDeclaredField("map");
         changeMap.setAccessible(true);
         changeMap.set(jsonObject, new LinkedHashMap<>());
         changeMap.setAccessible(false);
      } catch (IllegalAccessException | NoSuchFieldException e) {
         System.out.println(e.getMessage());
      }
      jsonObject.put("en", enSentStr).put("uk", ukSentStr).put("matchq", mq);
      jsonObject.put("analyse", new JSONObject().put("en", enInfs).put("uk", ukInfs).put("map", enUkMaps));
      System.out.println(jsonObject.toString(1));
      //System.out.println("mq = "+mq);
      return mq;


      /* old code
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

       */
   }
}
