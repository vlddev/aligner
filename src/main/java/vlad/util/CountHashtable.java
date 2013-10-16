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

import java.util.Collection;
import java.util.Hashtable;

/** Hashtable which counts how many times each element was inserted.
 */
public class CountHashtable<K> extends Hashtable<K,Integer> {

	private static final long serialVersionUID = -211022515302464879L;

	public CountHashtable(){
        super();
    }
    
    @Override
    public synchronized Integer put(K obj, Integer count){
        Integer wCount = null;
        try{
            wCount = get(obj);
            if(wCount!=null)
                wCount = wCount.intValue() + count.intValue();
            else
                wCount = count;
        }catch(Exception e){}
        if(wCount!=null)
            super.put(obj, wCount);
        else{
            super.put(obj, new Integer(1));
        }
        return wCount;
	}

    /** Adds object.
     *@return true if this object added first time (wasn't in hash). 
     */
	public synchronized boolean add(K obj){
	    boolean ret = false;
        Integer wCount = null;
        try{
            wCount = (Integer)get(obj);
        }catch(Exception e){}
        if(wCount!=null)
            super.put(obj, new Integer(wCount.intValue()+1));
        else{
            ret = true;
            super.put(obj, new Integer(1));
        }
        return ret;
	}
	
    /** Add all elements of the Collection object.
     *@return true if one or more objects was inserted to this hash. 
     */
	public synchronized boolean addAll(Collection<K> c){
	    boolean ret = false;
	    for(K k : c){
	    	if(this.add(k) && !ret) ret = true;
	    }
	    return ret;
	}
}
