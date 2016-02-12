package gov.nih.nlm.ling.util;

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

}
