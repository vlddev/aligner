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

/**
 * @author Vlad
 */
public class Word {
	long id;
	int type;
	String inf;
	String language;
	List<WordForm> wfList = null;
	
	public Word(String s){
		id = -1;
		type = -1;
		inf = s;
	}
	
	public Word(long id, String s){
		this.id = id;
		inf = s;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
		if (wfList != null) {
			for(WordForm wf : wfList) {
				wf.setFkInf(id);
			}
		}
	}

	public String getInf(){
		return inf;
	}
	
	public int getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type = type;
	}

	public List<WordForm> getWfList() {
		return wfList;
	}
	

	public void setWfList(List<WordForm> wfList) {
		this.wfList = wfList;
		if (wfList != null ) {
			for(WordForm wf : wfList) {
				wf.setFkInf(id);
				wf.setLanguage(language);
			}
		}
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
		if (wfList != null) {
			for(WordForm wf : wfList) {
				wf.setLanguage(language);
			}
		}
	}

	public boolean equals(Object obj) {
		boolean ret = false;
		if (obj instanceof Word){
			ret = getInf().equals(((Word)obj).getInf());
			if(ret) {
				ret = (getType() ==  ((Word)obj).getType());
			}
		}
		return ret;
	}

}
