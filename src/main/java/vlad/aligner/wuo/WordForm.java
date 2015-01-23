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

/**
 * @author Vlad
 * Represents FordForm
 */
public class WordForm {
	long id;
	long fkInf;
	String wf;
	String language;
	//Form ID
	String fid;
	
	public WordForm(String s){
		wf = s;
	}
	
	public WordForm(long id, String s){
		this.id = id;
		wf = s;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String get(){
		return wf;
	}
	
	public String getFid() {
		return fid;
	}

	public void setFid(String fid) {
		this.fid = fid;
	}

	public boolean equals(Object obj) {
		boolean ret = false;
		if (obj instanceof WordForm){
			ret = get().equals(((WordForm)obj).get());
		}
		return ret;
	}

	/**
	 * @return
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * @param language
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * @return
	 */
	public long getFkInf() {
		return fkInf;
	}

	/**
	 * @param fkInf
	 */
	public void setFkInf(long fkInf) {
		this.fkInf = fkInf;
	}

	/**
	 * @return
	 */
	public String getWf() {
		return wf;
	}

	/**
	 * @param wf
	 */
	public void setWf(String wf) {
		this.wf = wf;
	}

}
