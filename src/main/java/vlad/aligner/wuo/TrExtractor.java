package vlad.aligner.wuo;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

import org.json.JSONObject;
import vlad.aligner.wuo.db.DbTranslator;
import vlad.util.CountHashtable;

public class TrExtractor {
    private Locale origLoc = new Locale("en");
    private Locale trLoc = new Locale("uk");
    TranslatorInterface translator;
    CountHashtable<String> trStats;

    public TrExtractor(Connection con, Locale origLoc, Locale trLoc) {
        this.origLoc = origLoc;
        this.trLoc = trLoc;
        translator = new DbTranslator(con);
        List<String> errorList = translator.checkDB(this.origLoc, this.trLoc);
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

    public JSONObject extractTranslations(String origSentStr, String trSentStr) {
        MatchSentence ukSent = new MatchSentence(trSentStr, trLoc, this.translator);
        MatchSentence enSent = new MatchSentence(origSentStr, origLoc, this.translator);
        for (MatchSentence.MatchWf enWf : enSent.getMatchWfList()) {
            Map<Word, List<Word>> trMap = new HashMap();
            List<Word> ukTrList = new ArrayList();
            List<MatchSentence.MatchWf> ukMatches;
            for (Word enWord : enWf.bases) {
                List<Word> enWordList = new ArrayList();
                enWordList.add(enWord);
                List<Word> trList = translator.getTranslation(enWordList, origLoc, trLoc);
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
        jsonObject.put(origLoc.getLanguage(), origSentStr).put(trLoc.getLanguage(), trSentStr).put("matchq", mq);
        jsonObject.put("analyse", new JSONObject().put(origLoc.getLanguage(), enInfs).put(trLoc.getLanguage(), ukInfs).put("map", enUkMaps));
        return jsonObject;
    }

    public void match(String ukSentStr, List<String> enSentList) {
        for (int i = 0; i < enSentList.size(); i++) {
            String enSent = enSentList.get(i);
            JSONObject sentJson = extractTranslations(enSent, ukSentStr);
            System.out.println(i+". "+sentJson.getFloat("matchq"));
        }
    }
}
