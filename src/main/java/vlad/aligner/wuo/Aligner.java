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

import java.util.Arrays;
import java.util.Locale;

public class Aligner {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		if (args.length == 4) {
			String txt1 = args[0];
			String lang1 = args[1];
			String txt2 = args[2];
			String lang2 = args[3];
			//check input data
			String[] langList = Locale.getISOLanguages();
			
			if (Arrays.binarySearch(langList, lang1) < 0 ) {
				System.err.println("Unknown language '"+lang1+"'.");
				System.exit(1);
			}
			if (Arrays.binarySearch(langList, lang2) < 0 ) {
				System.err.println("Unknown language '"+lang2+"'.");
				System.exit(1);
			}
			
			ParallelCorpus.makeText(txt1, new Locale(lang1), txt2, new Locale(lang2));
		} else if (args.length == 3) {
			String list = args[0];
			String lang1 = args[1];
			String lang2 = args[2];

			ParallelCorpus.makeAll(list, new Locale(lang1), new Locale(lang2));
		} else {
			System.out.println("Usage: txt1 lang1 txt2 lang2");
			System.out.println("\t or list lang1 lang2");
		}
//		ParallelCorpus.makeText("Anderson Poul - The Man Who Came Early.txt", new Locale("en","EN"),
//			"Anderson Poul - The Man Who Came Early_ua.txt", new Locale("uk","UA"));

//		ParallelCorpus.makeAll("E:\\translation\\par_text_pool\\par_text_list.txt",
//		new Locale("en","EN"),
//		new Locale("uk","UA"));
		
	}

}
