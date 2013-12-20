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
            "about",
            "an",
            "and",
            "are",
            "as",
            "at",
            "be",
            "but",
            "by",
            "for",
            "from",
            "how",
            "i",
            "if",
            "in",
            "into",
            "is",
            "it",
            "of",
            "on",
            "or",
            "no",
            "not",
            "that",
            "the",
            "this",
            "to",
            "was",
            "what",
            "when",
            "where",
            "who",
            "will",
            "with"};

    public static final String[] UK_STOPWORDS = {
            "а",
            "б",
            "від",
            "за",
            "на",
            "он",
            "от",
            "по",
            "що"
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
