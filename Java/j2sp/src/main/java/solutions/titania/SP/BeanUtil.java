package solutions.titania.SP;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.log4j.Logger;

public class BeanUtil {
    private static final Logger log = Logger.getLogger(BeanUtil.class);

    public static List<String> compareObjects(Object oldObject, Object newObject)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
	return compareObjects(oldObject, newObject, true);
    }

    public static List<String> compareObjectsShallow(Object oldObject, Object newObject)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
	return compareObjects(oldObject, newObject, false);
    }

    public static List<String> compareObjects(Object oldObject, Object newObject, boolean deep)
	    throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
	List<String> changedFields = new ArrayList<>();
	BeanMap map = new BeanMap(oldObject);

	PropertyUtilsBean propUtils = new PropertyUtilsBean();

	for (Object propNameObject : map.keySet()) {
	    String propertyName = (String) propNameObject;
	    Object property1 = propUtils.getProperty(oldObject, propertyName);
	    Object property2 = propUtils.getProperty(newObject, propertyName);

	    if (property1 == null && "".equals(property2)) {
		property1 = "";
	    }
	    if (property2 == null && "".equals(property1)) {
		property2 = "";
	    }

	    if (property1 == null && property2 == null) {
		// log.debug(" " + propertyName + " is equal");
	    } else if ((property1 == null && property2 != null) || (property1 != null && property2 == null)) {
		changedFields.add(propertyName);
		log.debug("> " + propertyName + " is different (oldValue=\"" + property1 + "\", newValue=\"" + property2
			+ "\")");
	    }
	    // else if (property1 instanceof COA && deep == false) {
	    // // skip
	    // }
	    else if (property1 instanceof Number) {
		if (((Number) property1).floatValue() - ((Number) property2).floatValue() == 0) {
		    // log.debug(" " + propertyName + " is equal");
		} else {
		    changedFields.add(propertyName);
		    log.debug("> " + propertyName + " is different (oldValue=\"" + property1 + "\", newValue=\""
			    + property2 + "\")");
		}
	    } else if (property1 instanceof List) {
		if (deep) {
		    if (cmp((List<?>) property1, (List<?>) property2)) {
			// log.debug(" " + propertyName +
			// " is equal");
		    } else {
			changedFields.add(propertyName);
			log.debug("> " + propertyName + " is different (oldValue=\"" + property1 + "\", newValue=\""
				+ property2 + "\")");
		    }
		}
	    } else if (property1.equals(property2)) {
		// log.debug(" " + propertyName + " is equal");
	    } else {
		changedFields.add(propertyName);
		log.debug("> " + propertyName + " is different (oldValue=\"" + property1 + "\", newValue=\"" + property2
			+ "\")");
	    }
	}
	return changedFields;
    }

    public static boolean cmp(List<?> l1, List<?> l2) {
	// make a copy of the list so the original list is not changed, and
	// remove() is supported
	ArrayList<?> cp = new ArrayList<>(l1);
	if (l2 != null) {
	    for (Object o : l2) {
		if (!cp.remove(o)) {
		    return false;
		}
	    }
	}
	return cp.isEmpty();
    }

    public static void main(String[] args) throws Exception {
	Integer i = Integer.parseInt("1");
	Float f = Float.valueOf("1");
	log.debug(f.equals(i));
	// COA coa = new COA();
	// coa.setOperator("");
	// COAAirspace c1 = new COAAirspace("area", "1000", "1000", "100",
	// "100",
	// "100", coa);
	// COAAirspace c2 = new COAAirspace("area", "1000", "1000", "100",
	// "100",
	// "100", coa);
	// log.debug("" + EqualsBuilder.reflectionEquals(c1, c2,
	// false));

	// compareObjects(c1, c2);
    }
}
