package gov.nih.nlm.ling.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A class that contains static utility methods for collections.
 * 
 * @author Halil Kilicoglu
 *
 */
public class CollectionUtils {
	
	/**
	 * Generates permutations from a list of collections. Each permutation contains an element
	 * from each of the collections. 
	 * 
	 * @param <T>			the generic type for object in the collections
	 * @param collections	the list of collections
	 * @return				the collection of permutations
	 */
	public static <T> Collection<List<T>> generatePermutations(List<Collection<T>> collections) {
		if (collections == null || collections.isEmpty()) {
			return Collections.emptyList();
		} else {
			Collection<List<T>> res = new LinkedList<List<T>>();
			permutationsImpl(collections, res, 0, new LinkedList<T>());
			return res;
		}
	}

	private static <T> void permutationsImpl(List<Collection<T>> ori, Collection<List<T>> res, int d, List<T> current) {
		if (d == ori.size()) {
			res.add(current);
		    return;
		  }
		// iterate from current collection and copy 'current' element N times, one for each element
		Collection<T> currentCollection = ori.get(d);
		for (T element : currentCollection) {
			List<T> copy = new LinkedList<T>();
			copy.addAll(current);
		    copy.add(element);
		    permutationsImpl(ori, res, d + 1, copy);
		}
	}
	
	/**
	 * Generates combinations of size <var>n</var> from a list of objects. 
	 * 
	 * @param <T>	the generic type for object in the list
	 * @param n		size of each combination list
	 * @param in	the list of objects to combine
	 * @param list	the list of combinations
	 */
	public static <T> void generateCombinations(int n, List<T> in, List<List<T>> list) {
	    int numLists = getCombinationCount(in.size(),n);
	    for(int i = 0; i < numLists; i++) {
	        list.add(new ArrayList<>(n));
	    }
	    for(int j = 0; j < n; j++) {
	        // This is the period with which this position changes, i.e.
	        // a period of 5 means the value changes every 5th array
	        int period = (int) Math.pow(in.size(), n - j - 1);
	        for(int i = 0; i < numLists; i++) {
	            List<T> current = list.get(i);
	            int inIndex = -1;
	            if (current.size() > 0) 
	            	inIndex = in.indexOf(current.get(current.size()-1));
	            // Get the correct item and set it
	            int index = i / period % in.size();
	            if (index > inIndex)
	            	current.add(in.get(index));
	        }
	    }
	    List<List<T>> remove = new ArrayList<>();
	    for (List<T> l: list) {
	    	if (l.size() < n) remove.add(l); 
	    }
	    list.removeAll(remove);
	}
	
	private static int getCombinationCount(int size, int n) {
		if(size < n)
	        return 0;
	    if(n == 0 || n == size)
	        return 1;
	    return getCombinationCount(size-1,n-1)+getCombinationCount(size-1,n);
	}
}
