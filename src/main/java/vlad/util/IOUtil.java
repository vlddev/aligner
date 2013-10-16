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

package vlad.util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Owner
 *
 */
public class IOUtil {

    public static final String ln = System.getProperty("line.separator");

    public static String getFileContent(String file) throws IOException{
        String encoding = System.getProperty("file.encoding");
        return getFileContent(file, encoding);
        
    }

    public static String getFileContent(String file, String encoding) throws IOException{
        StringBuffer ret = new StringBuffer();
        String ln = System.getProperty("line.separator");
		BufferedReader fIn = new BufferedReader(new InputStreamReader(
					new FileInputStream(file), encoding)
		);
		String str = fIn.readLine();
		while(str!=null){
		    ret.append(str);
		    ret.append(ln);
			str = fIn.readLine();
		}

		fIn.close();
		return ret.toString();
    }

	/**
	 * Load hashtable from file 
	 * <key>\t<val>
	 * 
	 * @param file
	 * @param encoding
	 * @param bCase true - case sencitive, false - case insencitive (load in lower case)
	 * @return
	 */
	public static Map<String, String> getFileContentAsMap(String file, String encoding, 
					boolean bCase, Map<String, String> initHm) throws Exception{
		return getFileContentAsMap(file, encoding, bCase, initHm, false);
	}
					
	/**
	 * Load hashtable from file 
	 * <key>\t<val>
	 * 
	 * @param file
	 * @param encoding
	 * @param bCase true - key is case sencitive, false - key is case insencitive (load in lower case), value will be read as is
	 * @param initHm - initial hash map (all new values will be added to this map) 
	 * @param bComments true - ignore lines starting from '#'
	 * @return
	 */
	public static Map<String, String> getFileContentAsMap(String file, String encoding, 
					boolean bCase, Map<String, String> initHm, boolean bComments) throws Exception{
		Map<String, String> hm = new HashMap<String, String>();
		if(initHm != null){
			hm = initHm; 
		}
		String str = null; 
		try{
			BufferedReader fIn = new BufferedReader(
				new InputStreamReader(new FileInputStream(file),encoding));
            
			str = fIn.readLine();
			int pos=0;
			String key = null;
			String val = null;
			while(str!=null){
				if(!str.startsWith("#")){ //ignore commented lines
					pos = str.indexOf('\t');
					if(pos>-1){
						if(str.length()>pos){
							key = str.substring(0, pos).trim();
							val = str.substring(pos+1).trim();
							if(!bCase){
								key = key.toLowerCase();
								//val = val.toLowerCase();
							}
							hm.put(key, val);
						}
					}
				}
				str = fIn.readLine();
			}
			fIn.close();
		}catch(Exception e){
            throw e;
		}
		return hm;
	}

    public static PrintWriter openFile(String file, String encoding) throws IOException{
        return new PrintWriter(
			new OutputStreamWriter(new FileOutputStream(file),encoding),true);

    }
    
    public static BufferedReader openFileToRead(String file, String encoding) throws IOException{
        return new BufferedReader(new InputStreamReader(
					new FileInputStream(file), encoding));


    }

	public static void storeString(String file, String encoding, String content) throws IOException{
		PrintWriter pw = new PrintWriter(
			new OutputStreamWriter(new FileOutputStream(file),encoding),true);
		pw.print(content);
		pw.flush();
		pw.close();
	}
}