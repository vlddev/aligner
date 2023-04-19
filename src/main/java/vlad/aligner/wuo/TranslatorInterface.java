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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author Vlad
 */
public interface TranslatorInterface {
	public List<String> checkDB(Locale l1, Locale l2);
	
	public List<Word> getBaseForm(String wf, Locale lang);

	public List<WordForm> getWordForms(String word, Locale lang);

	public List<Word> getTranslation(String word, Locale langFrom, Locale langTo);

	public List<Word> getTranslation(Word word, Locale langFrom, Locale langTo);
	
	public List<Word> getTranslation(List<Word> wfList, Locale langFrom, Locale langTo);

	public Map<String,String> getWordBasesUsedOnce(Locale langFrom, Set<String> wfList) throws Exception;
	public Map<String,String> getWordBasesUsedOnce(Locale langFrom, List<String> wfList) throws Exception;
}
