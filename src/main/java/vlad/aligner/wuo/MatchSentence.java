package vlad.aligner.wuo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MatchSentence {
    private Locale lang;
    private List<MatchWf> matchWfList = new ArrayList<MatchWf>();

    public MatchSentence(String sentStr, Locale lang, TranslatorInterface translator) {
        this.lang = lang;
        Sentence sent = new Sentence(sentStr);
        List<String> elemList = sent.getElemList();
        for (int i = 0; i < elemList.size(); i++) {
            String wf = elemList.get(i);
            List<Word> bases = translator.getBaseForm(wf, lang);
            if (i == 0) {
                bases.addAll(translator.getBaseForm(wf.toLowerCase(), lang));
            }
            this.matchWfList.add(new MatchWf(wf, bases));
        }
    }

    public List<MatchWf> getMatchWfList() {
        return this.matchWfList;
    }

    public List<MatchWf> findMatches(List words) {
        List<MatchWf> ret = new ArrayList();
        for (MatchWf mwf : matchWfList) {
            if (mwf.matchingWf == null && mwf.bases.stream().anyMatch((element) -> {
                return words.contains(element);
            })) {
                ret.add(mwf);
            }
        }
        return ret;
    }

    class MatchWf {
        String wf;
        List<Word> bases;
        MatchWf matchingWf;
        Word base;
        Word trBase;

        MatchWf(String wf, List bases) {
            this.matchingWf = null;
            this.wf = wf;
            this.bases = bases;
        }
    }
}