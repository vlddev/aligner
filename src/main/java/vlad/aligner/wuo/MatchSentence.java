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
      Iterator var5 = sent.getElemList().iterator();

      while(var5.hasNext()) {
         String wf = (String)var5.next();
         this.matchWfList.add(new MatchWf(wf, translator.getBaseForm(wf, lang)));
      }

   }

   public List getMatchWfList() {
      return this.matchWfList;
   }

   public List findMatches(List words) {
      List ret = new ArrayList();
      Iterator var3 = this.matchWfList.iterator();

      while(var3.hasNext()) {
         MatchWf mwf = (MatchWf)var3.next();
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
    List bases;
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