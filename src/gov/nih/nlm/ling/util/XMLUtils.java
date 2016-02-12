package gov.nih.nlm.ling.util;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;

/**
 * A class for static XML utility methods.
 * 
 * @author Halil Kilicoglu
 *
 */
public class XMLUtils {
	/**
	 * Returns children elements of a given node that have a specific attribute/value pair.
	 * 
	 * @param node  the parent node
	 * @param childName  the child node name
	 * @param attributeName  the child attribute name
	 * @param value  the attribute value
	 * @return an <code>Elements</code> object containing the children, empty list if none is found 
	 */
	public static List<Element> getChildrenWithAttributeValue(Element node, String childName, 
			String attributeName, String value) {
		Elements els = null;
		List<Element> out = new ArrayList<Element>();
		if (childName == null)
			els = node.getChildElements();
		else 
			els = node.getChildElements(childName);
		if (els == null) return out;
		for (int i=0; i < els.size(); i++) {
			Element e = els.get(i);
			Attribute a = e.getAttribute(attributeName);
			if (a == null || (a != null && a.getValue().equals(value))) {
				out.add(e);
			}
		}
		return out;
	}

}
