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

import vlad.text.StringReplacer;
import vlad.util.IOUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author vlad
 *
 */
public class ParallelCorpus {

	private Corpus originalCorpus;
	private Corpus translatedCorpus;
    private String name;

	public boolean showSentIndex = false;
	
	//output channel for output of parallelization process information
	private static PrintWriter protOut = null;
	
	/**
	 * Містить точки поділу тексту в такому форматі:
	 * [індекс_речення_в_originalCorpus], [точка поділу]
	 * 
	 * Так як кожне речення, як з originalCorpus так і з translatedCorpus,
	 * може бути використане лише один раз, то потрібне додаткова структура,
	 * де зберігаються дані про входження речень з translatedCorpus в 
	 * mapping. Для цього використовується reverseMapping
	 * 
	 * !!!! Зміна цього поля повинна відбуватися лише в методах: 
	 *    addSplitPointToMapping()
	 */
	private Map<Integer,SplitPoint> mapping = new TreeMap<Integer,SplitPoint>();
	
	/**
	 * Містить додаткові дані про входження речень з translatedCorpus в 
	 * mapping в такому форматі:
	 * [індекс_речення_в_translatedCorpus], [індекс_речення_в_originalCorpus]
	 * 
	 *!!!! Зміна цього поля повинна відбуватися лише в методах класу.
	 *    addSplitPointToMapping()
	 */
	private Map<Integer,Integer> reverseMapping = new TreeMap<Integer,Integer>();

	public ParallelCorpus(Corpus original, Corpus translation) {
		originalCorpus = original;
		translatedCorpus = translation;
	}
	
	public boolean addSplitPointToMapping(SplitPoint sp) {
		boolean bAdded = true;
		if (mapping.containsKey(sp.sentenceIndCorpus1) ||
				reverseMapping.containsKey(sp.sentenceIndCorpus2)) {
			bAdded = false;
			System.out.println("SplitPoint was not added ["+sp.sentenceIndCorpus1+"; "+sp.sentenceIndCorpus2 + "\t" + 
					sp.splitWf1 + "; " + sp.splitWf2+"]");
		} else {
			mapping.put(sp.sentenceIndCorpus1, sp);
			reverseMapping.put(sp.sentenceIndCorpus2, sp.sentenceIndCorpus1);
		}
		return bAdded;
	}

	public boolean removeSplitPoint(Integer ind) {
		boolean bDone = false;
		if (this.mapping.containsKey(ind)) {
		   SplitPoint sp = (SplitPoint)this.mapping.get(ind);
		   if (this.reverseMapping.containsKey(sp.sentenceIndCorpus2)) {
			  this.mapping.remove(ind);
			  this.reverseMapping.remove(sp.sentenceIndCorpus2);
			  bDone = true;
		   } else {
			  System.out.println("SplitPoint [" + sp.sentenceIndCorpus1 + "; " + sp.sentenceIndCorpus2 + "\t" + sp.splitWf1 + "; " + sp.splitWf2 + "] was not removed. No entry in reverseMapping.");
		   }
		} else {
		   System.out.println("SplitPoint was not removed. SplitPoint with key = " + ind + " not exist.");
		}
  
		return bDone;
	 }
	
	private List<String> sortBasesByWfOrderInList(Map<String,String> htBaseWfMap, List<String> listWf) {
		List<String> ret = new ArrayList<String>();
		TreeMap<Integer, String> map = new TreeMap<Integer, String>();
		for (String sBase : htBaseWfMap.keySet()) {
			String sWf = htBaseWfMap.get(sBase);
			map.put(listWf.indexOf(sWf), sBase);
		}
		for (Integer i : map.keySet()) {
			ret.add(map.get(i));
		}
		return ret;
	}

	public void align(TranslatorInterface translator, int maxSplitSteps, int stopBadSplitPointCount) {
		makeMappingWithWordsUsedOnce(translator);

		List<Integer> badSplitPoints = getBadSplitPoints();
		System.out.println("=== Quality ===");
		System.out.println("Split points: " + getMapping().size());
		System.out.println("Bad split points: " + badSplitPoints.size());
		int prevBadSplitPointsSize;
		int step = 1;
		do {
			prevBadSplitPointsSize = badSplitPoints.size();
			System.out.println("   Step "+step+". Remove bad split points. Split ones more.");
			badSplitPoints.forEach(ind -> removeSplitPoint(ind));
			makeMappingWithWordsUsedOnce(translator);
			badSplitPoints = getBadSplitPoints();
			System.out.println("   Split points: " + getMapping().size());
			System.out.println("   Bad split points: " + badSplitPoints.size());
			step++;
		} while (prevBadSplitPointsSize > badSplitPoints.size()
				&& badSplitPoints.size() > stopBadSplitPointCount
				&& step <= maxSplitSteps);
	}

	public void makeMappingWithWordsUsedOnce(TranslatorInterface translator) {

		boolean bEnd = false;
		boolean bIgnoreCase = true;
		if (protOut != null ) {
			protOut.println("== makeMappingWithWordsUsedOnce (stpos1 : "+getOriginalCorpus().getStartPosInParentCorpus()
					+ ", stpos2 : "+getTranslatedCorpus().getStartPosInParentCorpus()+") ==");
		}
		do {
			int subCorpusCount = getCorpusPairCount();
			if (subCorpusCount <= 1) { //перша розбивка цього тексту
				TreeMap<Integer,SplitPoint> newMapping = new TreeMap<Integer,SplitPoint>();
	
				// Get stats of text and extract word bases used only once
	
				// Text1
				// малі й великі букви ігноруються при створенні списку нижче
				List<String> listWfOnce1 = originalCorpus.getWordsUsedOnce(bIgnoreCase);
				//HashMap (base (key), wf (value) ) of words used once
				Map<String, String> htBaseWf1;
				try {
					htBaseWf1 = translator.getWordBasesUsedOnce(originalCorpus.getLang(), listWfOnce1);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}
				// remove all stop-words
				for (String sStopword : StopWords.getStopwords(originalCorpus.getLang())) {
					htBaseWf1.remove(sStopword);
				}

				// слова відсортовані по розміщенню відповідних словоформ в тексті
				List<String> listWords1 = sortBasesByWfOrderInList(htBaseWf1, listWfOnce1);
	
				// text2
				List<String> listWfOnce2 = translatedCorpus.getWordsUsedOnce(bIgnoreCase);
				Map<String, String> htBaseWf2;
				try {
					htBaseWf2 = translator.getWordBasesUsedOnce(translatedCorpus.getLang(), listWfOnce2);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}
				
				// remove all stop-words
				for (String sStopword : StopWords.getStopwords(translatedCorpus.getLang())) {
					htBaseWf2.remove(sStopword);
				}

				// слова відсортовані по розміщенню відповідних словоформ в тексті
				List<String> listWords2 = sortBasesByWfOrderInList(htBaseWf2, listWfOnce2);
	
	//				if (isProtocol()) {
	//					IOUtil.storeSet(getOutDir()+"wf_lang1.txt", "utf-8", hsWfOnce1);
	//					IOUtil.storeSet(getOutDir()+"wf_lang2.txt", "utf-8", hsWfOnce2);
	//					IOUtil.storeHashtable(getOutDir()+"wf_base_lang1.txt", "utf-8", htBaseWf1);
	//					IOUtil.storeHashtable(getOutDir()+"wf_base_lang2.txt", "utf-8", htBaseWf2);
	//					String sWord = "";
	//					String sWordTr = "";
	//					int trCount = 0;
	//					Vector trPairList = new Vector();
	//					Vector trGoodPairList = new Vector();
	//					for (int i = 0; i < vWords1.size(); i++) {
	//						sWord = vWords1.get(i).toString();
	//						//Word w = new Word(vWords1.get(i).toString());
	//						Vector trList = new Vector();
	//						Vector tmpTrList = learn.getTranslator().getTranslation(sWord, lang1, lang2);
	//						if (tmpTrList instanceof WordList) {
	//							//convert to Vector of Strings
	//							for (int j = 0; j<tmpTrList.size(); j++) {
	//								trList.add(((WordList)tmpTrList).getAt(j).getInf());
	//							}
	//						}
	//						//get intersection of trList with vWords2 
	//						for (int j = 0; j<trList.size(); j++ ) {
	//							if (vWords2.contains(trList.get(j))) {
	//								sWordTr = trList.get(j).toString();
	//								trPairList.add(sWord + "\t" + sWordTr);
	//							}
	//						}
	//						//check if intersection of trList with vWords2 
	//						//contains only one element and store it in sWordTr.
	//						trCount = 0;
	//						for (int j = 0; j<trList.size(); j++ ) {
	//							if (vWords2.contains(trList.get(j))) {
	//								trCount++;
	//								sWordTr = trList.get(j).toString();
	//								trPairList.add(sWord + "\t" + sWordTr);
	//							}
	//						}
	//						if (trCount == 1) {
	//							trGoodPairList.add(sWord + "\t" + sWordTr);
	//						}
	//					}
	//					IOUtil.storeVector(getOutDir()+"tr_map.txt", "utf-8", trPairList);
	//					IOUtil.storeVector(getOutDir()+"good_tr_map.txt", "utf-8", trGoodPairList);
	//				}
				
				String sWordTr = "";
				String sUniqueWord = "";
				String sUniqueWordTr = "";
                int lastIndSent1 = -1;
                int lastIndSent2 = -1;
				int trCount = 0;

				//ToDo надавати перевагу іменникам, дієсловам та прикметникам, а вже потім решту слів 
				for (String sWord : listWords1) {
	
					// find unique word with translation
					List<String> trList = new ArrayList<String>();
					List<Word> tmpTrList = translator.getTranslation(sWord, originalCorpus.getLang(), translatedCorpus.getLang());
					//convert to List of Strings
					for (Word w : tmpTrList) {
						trList.add(w.getInf());
					}
					//check if intersection of trList with vWords2 
					//contains only one element and store it in sWordTr.
					trCount = 0;
					for (int j = 0; j < trList.size() && trCount < 2; j++ ) {
						if (listWords2.contains(trList.get(j))) {
							trCount++;
							sWordTr = trList.get(j);
						}
					}
					if (trCount == 1) { //знайдено лише один переклад
						//get wordform for wordbase sWord
						sUniqueWord = htBaseWf1.get(sWord);
						sUniqueWordTr = htBaseWf2.get(sWordTr);
	
						if (isAcceptablePositionOfDividerWords(sUniqueWord, sUniqueWordTr, -1, -1)) {
							//get sentences containing sUniqueWord and sUniqueWordTr
							int indSent1 = originalCorpus.getIndexOfSentenceContainingWf(sUniqueWord, true);
							int indSent2 = translatedCorpus.getIndexOfSentenceContainingWf(sUniqueWordTr, true);
	
							//ToDo check possible sentences-translations
							//1. translations can't differ in length more then two times
	//							String sent1 = vText1.get(1).toString();
	//							String sent2 = vText2.get(1).toString();
	//							if (sent1.trim().length() == 0 &&  sent2.trim().length() == 0 ) {
	//								if (isProtocol()) {
	//									protOut.println("Продовжуєм: обидва речення порожні. ");
	//								}
	//								sUniqueWord = "";
	//								sUniqueWordTr = "";
	//								return ret;
	//							}
	//							int minLen = Math.min(sent1.length(), sent2.length());
	//							int maxLen = Math.max(sent1.length(), sent2.length());
	//							if (maxLen > minLen*2) {
	//								if (isProtocol()) {
	//									protOut.println("Continue: (maxLen > minLen*2)");
	//								}
	//								sUniqueWord = "";
	//								sUniqueWordTr = "";
	//								return ret;
	//							}
	//							//show GUI element and interact with user
	//							if (userInteraction != null) {
	//								if (!userInteraction.confirmTranslation(sent1, sent2)) {
	//									sUniqueWord = "";
	//									sUniqueWordTr = "";
	//									return ret;
	//								}
	//							}
							//додати нову точки поділу
							if (indSent2 > lastIndSent2 && indSent1 > lastIndSent1 ) {
								// кожне речення може використовуватися для поділу лише раз

                                // відстань до точок попереднього поділу не має бути надто великою
                                if (Math.abs(indSent1 - lastIndSent1) < Math.abs(indSent2 - lastIndSent2) * 10
                                        && Math.abs(indSent2 - lastIndSent2) < Math.abs(indSent1 - lastIndSent1) * 10) {
                                    SplitPoint sp = new SplitPoint(indSent1, indSent2, sUniqueWord, sUniqueWordTr);
                                    newMapping.put(indSent1, sp);
                                    lastIndSent1 = indSent1;
                                    lastIndSent2 = indSent2;
                                } else {
                                    if (protOut != null) {
                                        protOut.println("Split point ("+indSent1+", "+indSent2+") ignored. Too far from previous split point: ("+lastIndSent1+", "+lastIndSent2+")");
                                    }

                                }
							} else {
                                if (protOut != null) {
                                    protOut.println("Split point ("+indSent1+", "+indSent2+") ignored. Conflict with previous split point: ("+lastIndSent1+", "+lastIndSent2+")");
                                }
                            }
							

	//							//protocoll
	//							if (isProtocol()) {
	//								protOut.println("Sentence1: "+vText1.get(1));
	//								protOut.println("Sentence2: "+vText2.get(1));
	//							}
	//							// dictionary
	//							addToDict(sent1, sent2);
	//							addTranslation(sent1, sent2, DbManager.TRPOOL_TR_SENT);
	//							if (getSentenceCount(vText1.get(0).toString())==1 && 
	//									getSentenceCount(vText2.get(0).toString())==1) {
	//								addToDict(vText1.get(0).toString(), vText2.get(0).toString());
	//								addTranslation(vText1.get(0).toString(), vText2.get(0).toString(), DbManager.TRPOOL_POSSIBLE_TR_SENT);
	//							} else {
	//								//add to the rest of translation
	//								addTranslation(vText1.get(0).toString(), vText2.get(0).toString(), DbManager.TRPOOL_TR_TEXT);
	//							}
	//							if (getSentenceCount(vText1.get(2).toString())==1 && 
	//									getSentenceCount(vText2.get(2).toString())==1) {
	//								addToDict(vText1.get(2).toString(), vText2.get(2).toString());
	//								addTranslation(vText1.get(2).toString(), vText2.get(2).toString(), DbManager.TRPOOL_POSSIBLE_TR_SENT);
	//							} else {
	//								//add to the rest of translation
	//								addTranslation(vText1.get(2).toString(), vText2.get(2).toString(), DbManager.TRPOOL_TR_TEXT);
	//							}
						} else {
							sUniqueWord = "";
							sUniqueWordTr = "";
	//							if (isProtocol()) {
	//								protOut.println("Continue");
	//							}
						}
					}
				}//кінець циклу по словах, які вживаються лише один раз
	
				// додати новостіорений поділ newMapping до вже існуючого
				if (newMapping.size() > 0) {
					int addedCount = 0;
					for (Integer key : newMapping.keySet()) {
						if (addSplitPointToMapping(newMapping.get(key))) addedCount++;
					}
					bEnd = (addedCount == 0);
				} else {
					bEnd = true; 
				}

				
				// продовжити розбиття тексту для решти тексту
	//			List<String> vWords1Next = new ArrayList<String>(listWords1.subList(i, listWords1.size()));
	//			Map<String,String> htBaseWf1Next = new HashMap<String,String>(vWords1Next.size());
	//			for (String o : vWords1Next) {
	//				htBaseWf1Next.put(o, htBaseWf1.get(o));
	//			}
	//				spliterRet = splitTrRecursion(spliterRet.txt1, spliterRet.txt2,
	//						htBaseWf1Next, htBaseWf2Next,
	//						vWords1Next, vWords2Next, sb);
	//				if (spliterRet.bSplited) {
	//					iSplitCount++;
	//					int pos = vWords2.indexOf(spliterRet.splitTrWord);
	//					vWords2Next = new Vector(vWords2.subList(pos+1, vWords2.size()));
	//					htBaseWf2Next = new Hashtable(vWords2Next.size());
	//					for (Object o : vWords2Next) {
	//						htBaseWf2Next.put(o, htBaseWf2.get(o));
	//					}
	//				}
			} else { //текст вже має поділ - робимо розбиття частин тексту
				// recursion
				List<ParallelCorpus> pcList = new ArrayList<ParallelCorpus>();
                //dumpMapping();
				for (int i = 0; i < subCorpusCount; i++) {
					ParallelCorpus pc = getCorpusPairAt(i);
                    if (pc != null && pc.getOriginalCorpus().getSentenceList().size() > 0 &&
							pc.getTranslatedCorpus().getSentenceList().size() > 0 &&
							(pc.getOriginalCorpus().getSentenceList().size() > 1 ||
							pc.getTranslatedCorpus().getSentenceList().size() > 1)) {
                        pc.setName(this.getName()+"_"+i);
//                        try {
//                            IOUtil.storeString("/home/vlad/imex/test/"+pc.getName()+".par.html", "utf-8", pc.getAsParHTML());
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
						//обидва тексти пари непорожні і хоча б один текст має більше одного речення
						//(немає сенсу шукати відповідність якщо кожен текст містить одне речення) 
						pc.makeMappingWithWordsUsedOnce(translator);
						pcList.add(pc);
					}
				}
				// додати новостворені поділи до вже існуючого
				TreeMap<Integer,SplitPoint> newMapping = new TreeMap<Integer,SplitPoint>();
				for (ParallelCorpus pc : pcList) {
					for (SplitPoint sp : pc.getMapping().values()) {
						int delta1 = pc.getOriginalCorpus().getStartPosInParentCorpus();
						int delta2 = pc.getTranslatedCorpus().getStartPosInParentCorpus();
						if (delta1 > -1 && delta2 > -1) {
							newMapping.put(sp.sentenceIndCorpus1 + delta1,
									new SplitPoint(sp.sentenceIndCorpus1 + delta1, sp.sentenceIndCorpus2 + delta2,
											sp.splitWf1, sp.splitWf2));
						}
					}
				}

				if (newMapping.size() > 0) {
					int addedCount = 0;
					for (Integer key : newMapping.keySet()) {
						if (addSplitPointToMapping(newMapping.get(key))) addedCount++;
					}
					bEnd = (addedCount == 0);
				} else {
					bEnd = true; 
				}
			}
		} while (!bEnd);
	}

	public int getCorpusPairCount() {
		return mapping.size() + 1;
	}
	
	private ParallelCorpus getCorpusPairAt(int pos) {
		Corpus corp1 = null;
		Corpus corp2 = null;
		
		SplitPoint spBegin = null;
		SplitPoint spEnd = null;
		int corp1Begin = 0, corp1End = 0, corp2Begin = 0, corp2End = 0;
		if (mapping.size() >= pos) {
			List<Integer> lst = new ArrayList<Integer>(mapping.keySet());
			if (pos == 0) {
				spEnd = mapping.get(lst.get(pos));
				corp1Begin = 0;
				corp1End = spEnd.sentenceIndCorpus1;
				corp2Begin = 0;
				corp2End = spEnd.sentenceIndCorpus2;
			} else if (pos == mapping.size()) {
				spBegin = mapping.get(lst.get(pos-1));
				corp1Begin = spBegin.sentenceIndCorpus1+1;
				corp1End = originalCorpus.getSentenceList().size();
				corp2Begin = spBegin.sentenceIndCorpus2+1;
				corp2End = translatedCorpus.getSentenceList().size();
			} else {
				spBegin = mapping.get(lst.get(pos-1));
				spEnd = mapping.get(lst.get(pos));
				corp1Begin = spBegin.sentenceIndCorpus1+1;
				corp1End = spEnd.sentenceIndCorpus1;
				corp2Begin = spBegin.sentenceIndCorpus2+1;
				corp2End = spEnd.sentenceIndCorpus2;
			}
			if (corp1Begin > corp1End || corp2Begin > corp2End ) {
				//TODO check
                //protocol
                if (protOut != null) {
                    protOut.println("Not valid corpus pair: corp1 ("+corp1Begin+", "+corp1End+") corp2 ("+corp2Begin+", "+corp2End+"");
                    if (spBegin != null) protOut.println("\t Split point begin: "+spBegin.toString());
                    if (spEnd != null) protOut.println("\t Split point end: "+spEnd.toString());
                }
				return null;
			} else {
				corp1 = new Corpus(originalCorpus.getSentenceList().subList(corp1Begin, corp1End));
				corp1.setStartPosInParentCorpus(corp1Begin);
				corp1.setLang(originalCorpus.getLang());
				corp2 = new Corpus(translatedCorpus.getSentenceList().subList(corp2Begin, corp2End));
				corp2.setStartPosInParentCorpus(corp2Begin);
				corp2.setLang(translatedCorpus.getLang());
			}
		}
		return new ParallelCorpus(corp1, corp2);
	}

	// TODO: rewrite
	public List<Integer> getBadSplitPoints() {
		List ret = new ArrayList();
		int maxSentDiff = 3;
		int minLastGood = 15;
		SplitPoint prevSplitPoint = null;
		SplitPoint splitPoint = null;
		int lastGoodPairs = 0;
		List<Integer> maybeInvalid = new ArrayList();

		int indFrom1;
		int indFrom2;
		int indTo1;
		int indTo2;
		for (Integer splitInd : this.mapping.keySet()) {
			splitPoint = (SplitPoint) this.mapping.get(splitInd);
			indFrom1 = prevSplitPoint == null ? 0 : prevSplitPoint.sentenceIndCorpus1 + 1;
			indTo1 = splitPoint.sentenceIndCorpus1;
			indFrom2 = prevSplitPoint == null ? 0 : prevSplitPoint.sentenceIndCorpus2 + 1;
			indTo2 = splitPoint.sentenceIndCorpus2;
			if (indTo1 > indFrom1 || indTo2 > indFrom2) {
				if (Math.abs(indTo1 - indFrom1 - (indTo2 - indFrom2)) > maxSentDiff) {
					lastGoodPairs = 0;
				} else {
					++lastGoodPairs;
				}
			}

			if (lastGoodPairs < minLastGood) {
				if (lastGoodPairs > 0) {
					maybeInvalid.add(splitInd);
				} else {
					maybeInvalid.forEach(e -> ret.add(e));
					maybeInvalid.clear();
					ret.add(splitInd);
				}
			} else {
				maybeInvalid.clear();
			}

			prevSplitPoint = splitPoint;
			++lastGoodPairs;
		}

		if (splitPoint != null) {
			indFrom1 = splitPoint.sentenceIndCorpus1 + 1;
			indTo1 = this.originalCorpus.getSentenceList().size();
			indFrom2 = splitPoint.sentenceIndCorpus2 + 1;
			indTo2 = this.translatedCorpus.getSentenceList().size();
			if (indTo1 <= indFrom1 && indTo2 > indFrom2) {
			}
		}

		return ret;
	}

	private boolean isAcceptablePositionOfDividerWords(String word1, String word2, int startPos1, int startPos2) {
		boolean ret = true;
		//Check position of word
		//Characters before and after word should be dividers
		int wPos1 = originalCorpus.getIndexOfWord(word1, startPos1);
		double relPos1 = -1;
		if (originalCorpus.getLength() > 0) {
			relPos1 = (double)wPos1/(double)originalCorpus.getLength();
		}
		int wPos2 = translatedCorpus.getIndexOfWord(word2, startPos2);
		double relPos2 = -1;
		if (translatedCorpus.getLength() > 0) {
			relPos2 = (double)wPos2/(double)translatedCorpus.getLength();
		}

		double relPosDist = Math.abs(relPos1-relPos2);
		if (originalCorpus.getLength() > 10000) {
			if (relPosDist > 0.02) {
				ret = false;
			}
		} else if (originalCorpus.getLength() > 5000) {
			if (relPosDist > 0.03) {
				ret = false;
			}
		} else if (originalCorpus.getLength() > 1000) {
			if (relPosDist > 0.06) {
				ret = false;
			}
		} else if (originalCorpus.getLength() > 500) {
			if (relPosDist > 0.1) {
				ret = false;
			}
		} else if (originalCorpus.getLength() > 100) {
			if (relPosDist > 0.2) {
				ret = false;
			}
		} else if (originalCorpus.getLength() > 10) {
			if (relPosDist > 0.3) {
				ret = false;
			}
		}

		if (! ret) {
			//protocol
			if (protOut != null) {
				protOut.println(word1+"\t"+wPos1+"\t"+originalCorpus.getLength()+"\t"+relPos1);
				protOut.println(word2+"\t"+wPos2+"\t"+translatedCorpus.getLength()+"\t"+relPos2);
				protOut.println("\t isAcceptablePositionOfWords: (abs(relPos1-relPos2) = "+relPosDist+", text1 len = " + originalCorpus.getLength()+")");
			}
		}
		return ret;
	}
	
	public void makeMappingUsingWords(TranslatorInterface translator) {
		//TODO
	}
	
//	public void dumpUsageStatsForWordsNotInDict(TranslatorInterface translator) {
//		//map
//		PrintWriter out = null;
//		try {
//			CountHashtable origStats = originalCorpus.getWordUsageStats(false);
//			List<Word> wList;
//			List<String> delList = new ArrayList<String>();
//			for (Object sWf : origStats.keySet()) {
//				if (sWf != null) {
//					wList = translator.getBaseForm(sWf.toString().toLowerCase(), originalCorpus.getLang());
//					if (wList != null && wList.size() > 0) {
//						delList.add(sWf.toString());
//					}
//				}
//			}
//			for (String s : delList) {
//				origStats.remove(s);
//			}
//			IOUtil.storeVectorOfPairObjects("wfStats_en.txt","utf-8",origStats.sortByValue(false));
//			
//			CountHashtable trStats = translatedCorpus.getWordUsageStats(false);
//			delList.clear();
//			for (Object sWf : trStats.keySet()) {
//				if (sWf != null) {
//					wList = translator.getBaseForm(sWf.toString().toLowerCase(), translatedCorpus.getLang());
//					if (wList != null && wList.size() > 0) {
//						delList.add(sWf.toString());
//					}
//				}
//			}
//			for (String s : delList) {
//				trStats.remove(s);
//			}
//			IOUtil.storeVectorOfPairObjects("wfStats_uk.txt","utf-8",trStats.sortByValue(false));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	public void dumpMapping() {
		//map
		PrintWriter out = null;
		try {
			out = IOUtil.openFile("dump_map.txt", "utf-8");
			SplitPoint splitPoint;
			for (Integer ind: mapping.keySet()) {
				splitPoint = mapping.get(ind);
				out.println(ind.toString()+"; "+splitPoint.sentenceIndCorpus2 + "\t" + 
						splitPoint.splitWf1 + "; " + splitPoint.splitWf2);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				out.flush();
				out.close();
			}
		}
		
		//text 1
		try {
			out = IOUtil.openFile("dump_en.txt", "utf-8");
			for (int i = 0; i < originalCorpus.getSentenceList().size(); i++) {
				out.println("["+i+"]"+originalCorpus.getSentenceList().get(i).getContent());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				out.flush();
				out.close();
			}
		}
		
		//text 2
		try {
			out = IOUtil.openFile("dump_uk.txt", "utf-8");
			for (int i = 0; i < translatedCorpus.getSentenceList().size(); i++) {
				out.println("["+i+"]"+translatedCorpus.getSentenceList().get(i).getContent());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				out.flush();
				out.close();
			}
		}
	}
	
	public String getAsParXML() {
		StringBuilder ret = new StringBuilder();
		SplitPoint prevSplitPoint = null;
		SplitPoint splitPoint = null;
		int indFrom1, indFrom2, indTo1, indTo2;
		
		for (Integer ind: mapping.keySet()) {
			splitPoint = mapping.get(ind);
			indFrom1 = (prevSplitPoint == null) ? 0 : prevSplitPoint.sentenceIndCorpus1+1;
			indTo1 = splitPoint.sentenceIndCorpus1;

			indFrom2 = (prevSplitPoint == null) ? 0 : prevSplitPoint.sentenceIndCorpus2+1;
			indTo2 = splitPoint.sentenceIndCorpus2;

			if (indTo1 > indFrom1 || indTo2 > indFrom2) {
				//додати блок текст-переклад якщо хоча б одна з частин непорожня
				ret.append("<l1t>");
				for (int i = indFrom1; i < indTo1; i++) {
					ret.append(originalCorpus.getSentenceList().get(i).getContent());
					ret.append(IOUtil.ln);
				}
				ret.append("</l1t>");
				ret.append(IOUtil.ln);

				ret.append("<l2t>");
				for (int i = indFrom2; i < indTo2; i++) {
					ret.append(translatedCorpus.getSentenceList().get(i).getContent());
					ret.append(IOUtil.ln);
				}
				ret.append("</l2t>");
				ret.append(IOUtil.ln);
			}

			ret.append("<l1s>");
			ret.append(originalCorpus.getSentenceList().get(splitPoint.sentenceIndCorpus1).getContent());
			ret.append("</l1s>");
			ret.append(IOUtil.ln);

			ret.append("<l2s>");
			ret.append(translatedCorpus.getSentenceList().get(splitPoint.sentenceIndCorpus2).getContent());
			ret.append("</l2s>");
			ret.append(IOUtil.ln);
			
			prevSplitPoint = splitPoint;
		}
		//add last text
		if (splitPoint != null) {
			indFrom1 = splitPoint.sentenceIndCorpus1+1;
			indTo1 = originalCorpus.getSentenceList().size();

			indFrom2 = splitPoint.sentenceIndCorpus2+1;
			indTo2 = translatedCorpus.getSentenceList().size();

			if (indTo1 > indFrom1 || indTo2 > indFrom2) {
				//додати блок текст-переклад якщо хоча б одна з частин непорожня
				ret.append("<l1t>");
				for (int i = indFrom1; i < indTo1; i++) {
					ret.append(originalCorpus.getSentenceList().get(i).getContent());
					ret.append(IOUtil.ln);
				}
				ret.append("</l1t>");
				ret.append(IOUtil.ln);

				ret.append("<l2t>");
				for (int i = indFrom2; i < indTo2; i++) {
					ret.append(translatedCorpus.getSentenceList().get(i).getContent());
					ret.append(IOUtil.ln);
				}
				ret.append("</l2t>");
				ret.append(IOUtil.ln);
			}
		}
		
		return ret.toString();
	}

    /**
     * Converts a stream of plaintext into valid XML.
     * Output stream must convert stream to UTF-8 when saving to disk.
     */
    public String makeValidXML(String plaintext)
    {
        char c;
        StringBuilder out = new StringBuilder();
        String text = fixChars(plaintext);
        for (int i=0; i<text.length(); i++)
        {
            c = text.charAt(i);
            out.append(makeValidXML(c));
        }
        return out.toString();
    }

    /**
     * Converts a single char into valid XML.
     * Output stream must convert stream to UTF-8 when saving to disk.
     */
    public static String makeValidXML(char c)
    {
        switch( c )
        {
            //case '\'':
            //    return "&apos;";	// NOI18N
            case '&':
                return "&amp;";	// NOI18N
            case '>':
                return "&gt;";	// NOI18N
            case '<':
                return "&lt;";	// NOI18N
            case '"':
                return "&quot;";	// NOI18N
            default:
                return String.valueOf(c);
        }
    }
    
    /**
     * Replace invalid XML chars by spaces. See supported chars at
     * http://www.w3.org/TR/2006/REC-xml-20060816/#charsets.
     * 
     * @param str input stream
     * @return result stream
     */
    public static String fixChars(String str) {
        char[] result = new char[str.length()];
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < 0x20) {
                if (c != 0x09 && c != 0x0A && c != 0x0D) {
                    c = ' ';
                }
            } else if (c >= 0x20 && c <= 0xD7FF) {
            } else if (c >= 0xE000 && c <= 0xFFFD) {
            } else if (c >= 0x10000 && c <= 0x10FFFF) {
            } else {
                c = ' ';
            }
            result[i] = c;
        }
        return new String(result);
    }    
    
	public String getAsTMX() {
		StringBuilder ret = new StringBuilder();
		SplitPoint prevSplitPoint = null;
		SplitPoint splitPoint = null;
		int indFrom1, indFrom2, indTo1, indTo2;
		
	    Pattern tagPattern = Pattern.compile("<\\/?[a-zA-Z]+[0-9]+\\/?>");
	    String langFrom = originalCorpus.getLang().getLanguage();
	    String langTo = translatedCorpus.getLang().getLanguage();
		
	    String langAttr = "lang";
	    
	    ret.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(IOUtil.ln);
    	ret.append("<!DOCTYPE tmx SYSTEM \"tmx11.dtd\">").append(IOUtil.ln);
    	ret.append("<tmx version=\"1.1\">").append(IOUtil.ln);
        ret.append("  <header").append(IOUtil.ln);
        ret.append("    creationtool=\"AutoPar\"").append(IOUtil.ln);
        ret.append("    creationtoolversion=\"1.0\"").append(IOUtil.ln);
        ret.append("    segtype=\"sentence\"").append(IOUtil.ln);
        ret.append("    o-tmf=\"AutoPar TMX\"").append(IOUtil.ln);
        ret.append("    adminlang=\"EN-US\"").append(IOUtil.ln);
        ret.append("    srclang=\"" + langFrom + "\"").append(IOUtil.ln);
        ret.append("    datatype=\"plaintext\"").append(IOUtil.ln);
        ret.append("  >").append(IOUtil.ln);
        ret.append("  </header>").append(IOUtil.ln);
        ret.append("  <body>").append(IOUtil.ln);

        for (List<String> pairList : getAsDoubleList(false)) {
            ret.append("<tu>").append(IOUtil.ln);
            ret.append(" <tuv " + langAttr + "=\"" + langFrom + "\">").append(IOUtil.ln);
            ret.append("  <seg>");
            String val = "";
            if (pairList != null && pairList.size() > 0) {
                val = pairList.get(0);
            }
            val = tagPattern.matcher(val).replaceAll("");
            val = makeValidXML(val);
            ret.append(val).append(IOUtil.ln);
            ret.append("  </seg>").append(IOUtil.ln);
            ret.append(" </tuv>").append(IOUtil.ln);

            ret.append(" <tuv " + langAttr + "=\"" + langTo + "\">").append(IOUtil.ln);
            ret.append("  <seg>");
            val = "";
            if (pairList != null && pairList.size() > 1) {
                val = pairList.get(1);
            }
            val = tagPattern.matcher(val).replaceAll("");
            val = makeValidXML(val);
            ret.append(val).append(IOUtil.ln);
            ret.append("  </seg>").append(IOUtil.ln);
            ret.append(" </tuv>").append(IOUtil.ln);
            ret.append("</tu>").append(IOUtil.ln);
            ret.append(IOUtil.ln);
        }

        ret.append("  </body>").append(IOUtil.ln);
        ret.append("</tmx>");

		return ret.toString();
	}

    public String getAsParHTML() {
        StringBuffer ret = new StringBuffer();
        ret.append("<html><body>");
        ret.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
        ret.append(getAsHtmlTable(getAsDoubleList(true)));
        ret.append("</body></html>");
        return ret.toString();
    }

	public String getAsAnonymizedParHTML(Map<String, String> origReplacements, Map<String, String> translationReplacements) {
		StringBuffer ret = new StringBuffer();
		ret.append("<html><body>");
		ret.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
		List<List<String>> doubleList = getAsDoubleList(true);
		ret.append(getAsHtmlTable(anonymizeDoubleList(doubleList, origReplacements, translationReplacements)));
		ret.append("</body></html>");
		return ret.toString();
	}

	public String getAsHtmlTable(List<List<String>> doubleList) {
		StringBuffer ret = new StringBuffer();

		ret.append("<table border=\"1\">");

        for (List<String> pairList : doubleList) {
            ret.append("<tr><td>");
            String val = "";
            if (pairList != null && pairList.size() > 0) {
                val = pairList.get(0);
            }
            ret.append(val);
            ret.append("</td>");
            ret.append(IOUtil.ln);
            ret.append("<td>");
            val = "";
            if (pairList != null && pairList.size() > 1) {
                val = pairList.get(1);
            }
            ret.append(val);
            ret.append("</td></tr>");
            ret.append(IOUtil.ln);
        }
		ret.append("</table>");
		
		return ret.toString();
	}

	public List<List<String>> anonymizeDoubleList(List<List<String>> list, Map<String, String> origReplacements, Map<String, String> translationReplacements) {
		for (List<String> elem : list) {
			// original
			elem.set(0, StringReplacer.replaceWords(elem.get(0), origReplacements));
			// translation
			elem.set(0, StringReplacer.replaceWords(elem.get(0), translationReplacements));
		}
		return list;
	}

	public List<List<String>> getAsDoubleList(boolean bMarkSplitPoints) {
        List<List<String>> ret = new ArrayList<List<String>>();
        SplitPoint prevSplitPoint = null;
        SplitPoint splitPoint = null;

        for (Integer ind: mapping.keySet()) {
            splitPoint = mapping.get(ind);
			ArrayList<String> pairList = preparePairBetweenSplitPoints(prevSplitPoint, splitPoint);
			if (pairList.size() > 0) {
				ret.add(pairList);
			}

			// Sentence at split point
            pairList =  new ArrayList<>(2);
            //original text
			String origText = originalCorpus.getSentenceList().get(splitPoint.sentenceIndCorpus1).getContent();
            if (bMarkSplitPoints) {
				origText = originalCorpus.getSentenceList().get(splitPoint.sentenceIndCorpus1).getContentWithMarkedWord(splitPoint.splitWf1);
            }
			if (showSentIndex) {
				origText = "["+splitPoint.sentenceIndCorpus1 + "] " + origText;
			}
			pairList.add(origText);

            //translation
			String trText = translatedCorpus.getSentenceList().get(splitPoint.sentenceIndCorpus2).getContent();
            if (bMarkSplitPoints) {
				trText = translatedCorpus.getSentenceList().get(splitPoint.sentenceIndCorpus2).getContentWithMarkedWord(splitPoint.splitWf2);
            }
			if (showSentIndex) {
				trText = "["+splitPoint.sentenceIndCorpus2 + "] " + trText;
			}
			pairList.add(trText);

            ret.add(pairList);

            prevSplitPoint = splitPoint;
        }
        //add last text
        if (splitPoint != null) {
			SplitPoint spEndOfText = new SplitPoint(originalCorpus.getSentenceList().size(),
					translatedCorpus.getSentenceList().size(), "", "");
			ArrayList pairList = preparePairBetweenSplitPoints(splitPoint, spEndOfText);
            if (pairList.size() > 0) {
                ret.add(pairList);
            }
        } else { //no split points
			SplitPoint spEndOfText = new SplitPoint(originalCorpus.getSentenceList().size(),
					translatedCorpus.getSentenceList().size(), "", "");
			ArrayList pairList = preparePairBetweenSplitPoints(null, spEndOfText);
			if (pairList.size() > 0) {
				ret.add(pairList);
			}
        }

        return ret;
    }

	private ArrayList<String> preparePairBetweenSplitPoints(SplitPoint spFrom, SplitPoint spTo) {
		ArrayList pairList =  new ArrayList<String>(2);
		int indFrom1 = (spFrom == null) ? 0 : spFrom.sentenceIndCorpus1+1;
		int indTo1 = spTo.sentenceIndCorpus1;

		int indFrom2 = (spFrom == null) ? 0 : spFrom.sentenceIndCorpus2+1;
		int indTo2 = spTo.sentenceIndCorpus2;

		if (indTo1 > indFrom1 || indTo2 > indFrom2) {
			//додати блок текст-переклад якщо хоча б одна з частин непорожня
			StringBuffer cellText = new StringBuffer();
			//original text
			for (int i = indFrom1; i < indTo1; i++) {
				cellText.append(originalCorpus.getSentenceList().get(i).getContent()).append(IOUtil.ln);
			}
			pairList.add(cellText.toString());

			//translation
			cellText = new StringBuffer();
			for (int i = indFrom2; i < indTo2; i++) {
				cellText.append(translatedCorpus.getSentenceList().get(i).getContent()).append(IOUtil.ln);
			}
			pairList.add(cellText.toString());
		}
		return pairList;
	}

	private class SplitPoint {
		int sentenceIndCorpus1;
		int sentenceIndCorpus2;
		String splitWf1;
		String splitWf2;

		public SplitPoint(int ind1, int ind2, String wf1, String wf2) {
			this.sentenceIndCorpus1 = ind1;
			this.sentenceIndCorpus2 = ind2;
			this.splitWf1 = wf1;
			this.splitWf2 = wf2;
		}

        public String toString() {
            return "text1: (sent="+sentenceIndCorpus1+" ; word="+splitWf1+") test2 (sent="+sentenceIndCorpus2+" ; word="+splitWf2+")";
        }
	}

	public Map<Integer,SplitPoint> getMapping() {
		return mapping;
	}

	public Corpus getOriginalCorpus() {
		return originalCorpus;
	}

	public Corpus getTranslatedCorpus() {
		return translatedCorpus;
	}

	public static void setProtOut(PrintWriter pw) {
		protOut = pw;
	}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
