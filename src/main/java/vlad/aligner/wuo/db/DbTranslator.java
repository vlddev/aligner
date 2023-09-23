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

package vlad.aligner.wuo.db;

import org.json.JSONArray;
import org.json.JSONObject;
import vlad.aligner.wuo.*;
import vlad.util.IOUtil;
import vlad.util.Util;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * This implementation of TranslatorInterface
 * use DB to find (wordForm,word) pairs and (word, translation word) pairs
 *
 * @author Vlad
 */
public class DbTranslator implements TranslatorInterface {

    private Connection ce;

    public DbTranslator(Connection c) {
        this.ce = c;
    }

    /**
     * check if database contains all needed tables for
     * two passed locales to make text alignment.
     * @param l1 - locale1
     * @param l2 - locale2
     * @return String[] - the list of found inconsistencies. If no inconsistencies found
     * the list must be empty or null.
     */
    public List<String> checkDB(Locale l1, Locale l2) {
        List<String> ret = new ArrayList<String>();
        //<lang>_inf
        String sTable = l1.getLanguage() + "_inf";
        String sql = "select * from " + sTable + " where id = -1";
        try {
            ce.createStatement().executeQuery(sql);
        } catch (SQLException e) {
            ret.add(e.getMessage());
        }

        sTable = l2.getLanguage() + "_inf";
        sql = "select * from " + sTable + " where id = -1";
        try {
            ce.createStatement().executeQuery(sql);
        } catch (SQLException e) {
            ret.add(e.getMessage());
        }

        //<lang>_wf
        sTable = l1.getLanguage() + "_wf";
        sql = "select * from " + sTable + " where id = -1";
        try {
            ce.createStatement().executeQuery(sql);
        } catch (SQLException e) {
            ret.add(e.getMessage());
        }
        sTable = l2.getLanguage() + "_wf";
        sql = "select * from " + sTable + " where id = -1";
        try {
            ce.createStatement().executeQuery(sql);
        } catch (SQLException e) {
            ret.add(e.getMessage());
        }

        //<lang1>_<lang2>
        sTable = getTrMapTable(l1, l2);
        sql = "select * from " + sTable + " where "+l1.getLanguage()+"_id = -1";
        try {
            ce.createStatement().executeQuery(sql);
        } catch (SQLException e) {
            ret.add(e.getMessage());
        }


        return ret;
    }

    /**
     * implementation of TranslatorInterface#getBaseForm
     * @see vlad.aligner.wuo.TranslatorInterface#getBaseForm(java.lang.String, java.util.Locale)
     */
    public List<Word> getBaseForm(String wf, Locale lang) {
        List<Word> ret = new ArrayList<Word>();
        String prefix = lang.getLanguage() + "_";
        try {
            ret = getBaseForm(wf,prefix);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private List<Word> getBaseForm(String wf, String prefix) throws SQLException {
        List<Word> ret = new ArrayList<Word>();
        String sql = "select distinct inf.* from "+prefix+"wf wf, "+prefix+"inf inf"
                + " where wf.wf = ? and wf.fk_inf = inf.id ";
        //MySql make case-insensitive search
        //Case will be checked in loop
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = ce.prepareStatement(sql);
            ps.setString(1, wf);
            rs = ps.executeQuery();
            long id;
            int type;
            String inf;
            while(rs.next()) {
                id = rs.getLong("id");
                inf = rs.getString("inf");
                type = rs.getInt("type");
                if (inf.charAt(0) == wf.charAt(0)) {
                    //case sensitive check
                    Word w = new Word(id, inf);
                    w.setType(type);
                    ret.add(w);
                }
            }
        } finally {
            DAO.closeResultSet(rs);
            DAO.closeStatement(ps);
        }
        return ret;
    }

    public Long insertBaseForm(String inf, int type, String prefix) throws SQLException {
        Long id = -1L;
        String sql = "INSERT INTO "+prefix+"inf (type, inf) VALUES (?,?)";
        PreparedStatement ps = ce.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, type);
        ps.setString(2, inf);
        ps.executeUpdate();
        ResultSet generatedKeys = ps.getGeneratedKeys();
        if (generatedKeys.next()) {
            id = generatedKeys.getLong(1);
        }
        return id;
    }

    public void insertWordForm(Long fkInf, String wf, String prefix) throws SQLException {
        Long id = -1L;
        String sql = "INSERT INTO "+prefix+"wf (fk_inf, wf) VALUES (?,?)";
        PreparedStatement ps = ce.prepareStatement(sql);
        ps.setLong(1, fkInf);
        ps.setString(2, wf);
        ps.executeUpdate();
    }

    public void insertToTrMap(Long fromId, Long toId, Locale langFrom, Locale langTo) throws SQLException {
        String sTrMapTable = getTrMapTable(langFrom, langTo);
        String sql = "INSERT INTO "+sTrMapTable+" ("+langFrom.getLanguage()+"_id, "+langTo.getLanguage()+"_id) VALUES (?,?)";
        PreparedStatement ps = ce.prepareStatement(sql);
        ps.setLong(1, fromId);
        ps.setLong(2, toId);
        ps.executeUpdate();
    }

    public void insertEntityWithTranslation(List<String> enWfs, List<String> ukWfs) throws SQLException {
        // insert inf
        Long enInfId = insertBaseForm(enWfs.get(0), 20, "en_");
        Long ukInfId = insertBaseForm(ukWfs.get(0), 1, "uk_");
        insertToTrMap(enInfId, ukInfId, new Locale("en"), new Locale("uk"));
        // insert wf
        for (String wf : enWfs) {
            insertWordForm(enInfId, wf, "en_");
        }
        for (String wf : ukWfs) {
            insertWordForm(ukInfId, wf, "uk_");
        }
        ce.commit();
    }

    public List<WordForm> getWordForms(String word, Locale lang) {
        List<WordForm> ret = new ArrayList<WordForm>();
        String prefix = lang.getLanguage() + "_";
        try {
            ret = getWordForms(word, prefix);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private List<WordForm> getWordForms(String word, String dbPrefix) throws SQLException {
        List<WordForm> ret = new ArrayList<WordForm>();
        String sql = "select distinct wf.wf, wf.fk_inf from "+dbPrefix+"wf wf, "+dbPrefix+"inf inf"
                + " where inf.inf = ? and wf.fk_inf = inf.id ";
        //TODO MySql make case-insensitive search
        //Case will be checked in loop
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = ce.prepareStatement(sql);
            ps.setString(1, word);
            rs = ps.executeQuery();
            long fk_inf;
            String wf;
            while(rs.next()) {
                fk_inf = rs.getLong("fk_inf");
                wf = rs.getString("wf");
                //case sensitive check
                WordForm w = new WordForm(-1, wf);
                w.setFkInf(fk_inf);
                ret.add(w);
            }
        } finally {
            DAO.closeResultSet(rs);
            DAO.closeStatement(ps);
        }
        return ret;
    }

    private String getTrMapTable(Locale l1, Locale l2) {
        String ret = "";
        String lang1 = l1.getLanguage();
        String lang2 = l2.getLanguage();
        if (lang1.compareTo(lang2) < 0) {
            ret = lang1+"_"+lang2;
        } else {
            ret = lang2+"_"+lang1;
        }
        return ret;
    }

    /**
     * implementation of TranslatorInterface#getTranslation
     * @see vlad.aligner.wuo.TranslatorInterface#getTranslation(java.util.List, java.util.Locale, java.util.Locale)
     */
    public List<Word> getTranslation(
            List<Word> wList,
            Locale langFrom,
            Locale langTo) {
        List<Word> ret = new ArrayList<Word>();
        if (wList == null) return ret;
        if (wList.size() < 1) return ret;
        String sTrMapTable = getTrMapTable(langFrom, langTo);
        String sql = "select to_inf.* "
                + " from "+langTo.getLanguage()+"_inf to_inf, " + sTrMapTable + " map "
                + " where map." + langFrom.getLanguage() + "_id = ? and "
                + " map." + langTo.getLanguage() + "_id = to_inf.id";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = ce.prepareStatement(sql);
            for (Word w : wList) {
                ps.clearParameters();
                ps.setLong(1, w.getId());
                rs = ps.executeQuery();
                while(rs.next()) {
                    Word wTr = new Word(rs.getLong("id"), rs.getString("inf"));
                    wTr.setType(rs.getInt("type"));
                    ret.add(wTr);
                }
                DAO.closeResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DAO.closeResultSet(rs);
            DAO.closeStatement(ps);
        }
        return ret;
    }

    public List<Word> getTranslation(
            Word word,
            Locale langFrom,
            Locale langTo) {
        List<Word> ret = new ArrayList<Word>();
        if (word == null) return ret;
        //Translation
        String sTrMapTable = getTrMapTable(langFrom, langTo);
        String sql = "select to_inf.* "
                + " from "+langTo.getLanguage()+"_inf to_inf, " + sTrMapTable + " map "
                + " where map." + langFrom.getLanguage() + "_id = ? and "
                + " map." + langTo.getLanguage() + "_id = to_inf.id";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = ce.prepareStatement(sql);
            ps.clearParameters();
            ps.setLong(1, word.getId());
            rs = ps.executeQuery();
            while(rs.next()) {
                Word wTr = new Word(rs.getLong("id"), rs.getString("inf"));
                wTr.setType(rs.getInt("type"));
                ret.add(wTr);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DAO.closeResultSet(rs);
            DAO.closeStatement(ps);
        }
        return ret;
    }

    public List<Word> getTranslation(
            String word,
            Locale langFrom,
            Locale langTo) {
        List<Word> ret = new ArrayList<Word>();
        if (word == null) return ret;
        //Translation
        String sTrMapTable = getTrMapTable(langFrom, langTo);
        String sql = "select distinct to_inf.* "
                + " from " + langFrom.getLanguage()+"_inf from_inf, "
                + langTo.getLanguage()+"_inf to_inf, " + sTrMapTable + " map "
                + " where from_inf.inf = ? and "
                + " map." + langFrom.getLanguage() + "_id = from_inf.id and "
                + " map." + langTo.getLanguage() + "_id = to_inf.id";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = ce.prepareStatement(sql);
            ps.clearParameters();
            ps.setString(1, word);
            rs = ps.executeQuery();
            while(rs.next()) {
                Word wTr = new Word(rs.getLong("id"), rs.getString("inf"));
                wTr.setType(rs.getInt("type"));
                ret.add(wTr);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DAO.closeResultSet(rs);
            DAO.closeStatement(ps);
        }
        return ret;
    }

    public Map<String,String> getWordBasesUsedOnce(Locale langFrom, Set<String> wfList) throws Exception {
        return getWordBasesUsedOnce(langFrom, new ArrayList<>(wfList));
    }

    /**
     * Get wordbases which used only once in text
     */
    public Map<String,String> getWordBasesUsedOnce(Locale langFrom, List<String> wfList) {
        Map<String, String> ret = new HashMap<String,String>();
        Collection<String> ignore = new HashSet<String>();
        String s = "";
        List<Word> wordList;
        for (String wf : wfList) {
            wordList = getBaseForm(wf, langFrom);
            String wfLower = wf.toLowerCase();
            //search in lower case
            if (!wfLower.equals(wf) && wordList.size() < 1) {
                wordList = getBaseForm(wfLower, langFrom);
            }
            Collection<String> hs = new HashSet<String>();
            // розглядати омоніми як одне слово
            for (Word base : wordList) {
                hs.add(base.getInf());
            }
            if (hs.size() == 1) {
                s = hs.iterator().next();
                if(!s.equals("")) {
                    if (!ignore.contains(s)) {
                        if (ret.containsKey(s)) {
                            ignore.add(s);
                            ret.remove(s);
                        } else {
                            ret.put(s, wf);
                        }
                    }
                }
            }
        }
        return ret;
    }

    public void storeListOfPairObjects(ParallelCorpus pc, String sFileName, boolean writeToDb, boolean writeToJson) throws Exception {
        if (!writeToDb && !writeToJson) return;
        int maxSentLen = 1000;
        File dbFileName = new File(sFileName);
        String textName = dbFileName.getName();
        TrExtractor trExtractor = new TrExtractor(ce, pc.getOriginalCorpus().getLang(), pc.getTranslatedCorpus().getLang());
        String sqlInsTxt = "INSERT INTO par_txt(name) VALUES (?) ";
        String sql = "INSERT INTO par_sent(txt_id, en, uk, matchq) VALUES (?, ?, ?, ?) ";
        PreparedStatement ps = null;

        try {
            ce.setAutoCommit(false);
            int txtId = -1;
            if (writeToDb) {
                ps = this.ce.prepareStatement(sqlInsTxt, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, textName);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    txtId = rs.getInt(1);
                }
                DAO.closeStatement(ps);
                ps = ce.prepareStatement(sql);
            }
            for (List<String> pair : pc.getAsDoubleList(false)) {
                String enSent = Util.cleanupSentence(pair.get(0).trim());
                String ukSent = Util.cleanupSentence(pair.get(1).trim());
                if ( (ukSent.length() > 0 || enSent.length() > 0) && ukSent.length() < maxSentLen && enSent.length() < maxSentLen) {
                    JSONObject parSentJson = trExtractor.extractTranslations(enSent, ukSent);
                    float mq = parSentJson.getFloat("matchq");
                    if (writeToDb) {
                        ps.clearParameters();
                        ps.setInt(1, txtId);
                        ps.setString(2, enSent);
                        ps.setString(3, ukSent);
                        ps.setFloat(4, mq);
                        ps.executeUpdate();
                    }

                    if (writeToJson) {
                        // TODO: use JSONObject parSentJson
                        JSONObject jsonObject = new JSONObject().put("en", enSent).put("uk", ukSent).put("matchq", mq);
                        //System.out.println(jsonObject.toString(1));
                    }
                }
            }
            ce.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
            ce.rollback();
        } finally {
            DAO.closeStatement(ps);
        }
    }

    public String getListOfPairObjectsAsTsv(ParallelCorpus pc) throws Exception {
        int maxSentLen = 1000;
        StringBuilder sbRet = new StringBuilder();
        TrExtractor trExtractor = new TrExtractor(ce, pc.getOriginalCorpus().getLang(), pc.getTranslatedCorpus().getLang());
        for (List<String> pair : pc.getAsDoubleList(false)) {
            String enSent = Util.cleanupSentence(pair.get(0).trim());
            String ukSent = Util.cleanupSentence(pair.get(1).trim());
            if ( (ukSent.length() > 0 || enSent.length() > 0) && ukSent.length() < maxSentLen && enSent.length() < maxSentLen) {
                JSONObject parSentJson = trExtractor.extractTranslations(enSent, ukSent);
                float mq = parSentJson.getFloat("matchq");
                sbRet.append(enSent).append("\t").append(ukSent).append("\t").append(mq).append("\n");
            }
        }
        return sbRet.toString();
    }

    public JSONObject getParallelCorpusWithEntitiesAsJson(ParallelCorpus pc) {
        TrExtractor trExtractor = new TrExtractor(ce, pc.getOriginalCorpus().getLang(), pc.getTranslatedCorpus().getLang());
        try {
            JSONObject jsonRoot = new JSONObject();
            JSONArray contentList = new JSONArray();
            List<List<String>> pairList = pc.getAsDoubleList(false);
            for (List<String> pair : pairList) {
                String enSent = Util.cleanupSentence(pair.get(0).trim());
                String ukSent = Util.cleanupSentence(pair.get(1).trim());
                if ( ukSent.length() > 0 || enSent.length() > 0) {
                    JSONObject parSentJson = trExtractor.extractTranslations(enSent, ukSent);
                    parSentJson.remove("analyse");
                    contentList.put(parSentJson);
                }
            }
            jsonRoot.put("content", contentList);
            JSONArray entitiesList = new JSONArray();
            for(List<String> elem : pc.getStatisticalEntityTranslations()) {
                JSONObject jsonElem = new JSONObject();
                jsonElem.put(pc.getOriginalCorpus().getLang().getLanguage(), elem.get(0));
                jsonElem.put(pc.getTranslatedCorpus().getLang().getLanguage(), elem.get(1));
                entitiesList.put(jsonElem);
            }
            jsonRoot.put("entities", entitiesList);
            return jsonRoot;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject getAnonymizedParallelCorpusAsJson(ParallelCorpus pc) {
        TrExtractor trExtractor = new TrExtractor(ce, pc.getOriginalCorpus().getLang(), pc.getTranslatedCorpus().getLang());
        try {
            List<String> origEntities = new ArrayList<>();
            List<String> trEntities = new ArrayList<>();
            for(List<String> elem : pc.getStatisticalEntityTranslations()) {
                // elem.get(0) - orig entities comma separated
                // elem.get(1) - tr entities comma separated
                origEntities.addAll(Arrays.asList(elem.get(0).split(",")));
                trEntities.addAll(Arrays.asList(elem.get(1).split(",")));
            }
            JSONObject jsonRoot = new JSONObject();
            JSONArray contentList = new JSONArray();
            List<List<String>> pairList = pc.getAsDoubleList(false);
            for (List<String> pair : pairList) {
                String enSentStr = Util.cleanupSentence(pair.get(0).trim());
                if ( enSentStr.length() > 0) {
                    Sentence sent = new Sentence(enSentStr);
                    sent.replaceEntities(origEntities, "ENTITY");
                    // TODO: rewrite replaceEntities() to replace entities in both Sentence.content
                    // and Sentence.elemList
                    enSentStr = Util.cleanupSentence(sent.toString());
                }
                String ukSentStr = Util.cleanupSentence(pair.get(1).trim());
                if ( ukSentStr.length() > 0) {
                    Sentence sent = new Sentence(ukSentStr);
                    sent.replaceEntities(trEntities, "ENTITY");
                    // TODO: rewrite replaceEntities() to replace entities in both Sentence.content
                    // and Sentence.elemList
                    ukSentStr = Util.cleanupSentence(sent.toString());
                }
                if ( ukSentStr.length() > 0 || enSentStr.length() > 0) {
                    JSONObject parSentJson = trExtractor.extractTranslations(enSentStr, ukSentStr);
                    parSentJson.remove("analyse");
                    contentList.put(parSentJson);
                }
            }
            jsonRoot.put("content", contentList);
            return jsonRoot;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
