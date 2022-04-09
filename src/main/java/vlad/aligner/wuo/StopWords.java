/*
#######################################################################
#
#  vlad-aligner - parallel texts aligner
#
#  Copyright (C) 2009-2013 Volodymyr Vlad
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

import java.util.Locale;

/**
 *
 * @author Volodymyr Vlad
 */
public class StopWords {
    public static final String[] EN_STOPWORDS = {
            "a",
            "an",
            "and",
            "are",
            "as",
            "at",
            "be",
            "but",
            "by",
            "do",
            "for",
            "from",
            "how",
            "i",
            "in",
            "into",
            "is",
            "it",
            "of",
            "on",
            "or",
            "no",
            "not",
            "so",
            "that",
            "the",
            "this",
            "to",
            "was",
            "what",
            "will",
            "with"};

    public static final String[] UK_STOPWORDS = {
        "а",
        "але",
        "б",
        "в",
        "від",
        "до",
        "і",
        "із",
        "й",
        "ж",
        "з",
        "за",
        "на",
        "не",
        "ні",
        "он",
        "от",
        "по",
        "по",
        "та",
        "у",
        "цей",
        "я",
        "як"
    };

    public static String[] getStopwords(Locale language) {
        if ("en".equals(language.getLanguage())) {
            return EN_STOPWORDS;
        } else if ("uk".equals(language.getLanguage())) {
            return UK_STOPWORDS;
        } else {
            return new String[]{};
        }
    }

}
