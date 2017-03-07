package solutions.titania.SP;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class SPUtil {

    static final SimpleDateFormat readSpDateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Logger log = Logger.getLogger(SPUtil.class);

    public static void setFieldList(String xmlRow, SPListItem victim, SoapUtil soapUtil)
	    throws IntrospectionException, Exception {
	String[] attrs = xmlRow.split("ows_");
	Map<String, String> attrMap = new HashMap<>();
	for (String attr : attrs) {
	    if (!attr.trim().equals("")) {
		String key = attr.substring(0, attr.indexOf('='));
		String value = attr.substring(attr.indexOf('=') + 1);
		value = value.trim();
		value = value.substring(1, value.length() - 1);
		attrMap.put(key, value);
	    }
	}
	for (Map.Entry<String, String> entry : attrMap.entrySet()) {
	    String fieldName = entry.getKey();
	    String value = entry.getValue();
	    // System.out.println(".setFieldList() fieldName = [" + fieldName +
	    // "] value = [" + value.toString() + "]");
	    setFieldValue(victim, fieldName, value, soapUtil);
	}
    }

    public static List<Field> getInheritedFields(Class<?> type) {
	List<Field> fields = new ArrayList<Field>();
	for (Class<?> c = type; c != null; c = c.getSuperclass()) {
	    fields.addAll(Arrays.asList(c.getDeclaredFields()));
	}
	return fields;
    }

    public static void setFieldValue(SPListItem victim, String fieldName, String value, SoapUtil soapUtil)
	    throws IntrospectionException, ParseException, IllegalAccessException, InvocationTargetException {
	for (Field field : getInheritedFields(victim.getClass())) {
	    if (field.isAnnotationPresent(SPListColumn.class)) {
		SPListColumn column = field.getAnnotation(SPListColumn.class);
		if (column.name().equals(fieldName)) {
		    PropertyDescriptor pd = new PropertyDescriptor(field.getName(), victim.getClass());
		    Object reflectValue = null;
		    switch (column.type()) {
		    case BOOLEAN:
			reflectValue = parseValueByClass(value, Boolean.class);
			break;
		    case DATE:
			reflectValue = parseValueByClass(value, Date.class);
			break;
		    case MULT_LINE:
		    case SINGLE_LINE:
			reflectValue = parseValueByClass(value, String.class);
			break;
		    case NUMBER:
			reflectValue = parseValueByClass(value, Number.class);
			break;
		    case LOOKUP_ADDITIONAL_FIELD:
			String[] vArr2 = value.split(";#");
			if (field.getType().equals(SPLookupItem.class)) {

			    SPLookupItem col = new SPLookupItem();
			    if (vArr2.length > 1) {
				col.setSpId(NumberFormat.getInstance().parse(vArr2[0]));
				col.setName(vArr2[1]);
				reflectValue = col;
			    }
			} else {
			    reflectValue = parseValueByClass(vArr2[1], field.getType());
			}
			break;
		    case LOOKUP_MULT:
			ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
			Class clas = (Class<?>) stringListType.getActualTypeArguments()[0];
			SPList[] annotatList = (SPList[]) clas.getAnnotationsByType(SPList.class);
			if (annotatList.length > 0) {
			    String v = value.toString();// 193;#2_193;#211;#2_211
			    if (!"".equals(v)) {
				String[] vArr = v.split(";#");
				List<Number> fkIds = new ArrayList<>();
				for (int x = 0; x < vArr.length; x = x + 2)
				    fkIds.add(NumberFormat.getInstance().parse(vArr[x]));
				List<?> lookups = new ArrayList<>();

				if (fkIds.size() > 50) {

				    List<List<Number>> parts = chopped(fkIds, 50);
				    for (List<Number> subList : parts) {
					String query = buildQueryForIds(subList);
					lookups.addAll(getListItems(soapUtil, clas, query));
				    }
				} else {
				    String query = buildQueryForIds(fkIds);

				    lookups.addAll(getListItems(soapUtil, clas, query));
				}
				reflectValue = lookups;
			    }
			}
			break;
		    case LOOKUP: // TODO untested
			// clas = (Class<?>) field.getDeclaringClass();
			clas = (Class<?>) pd.getReadMethod().getReturnType(); // field.getDeclaringClass();
			annotatList = (SPList[]) clas.getAnnotationsByType(SPList.class);
			if (annotatList.length > 0) {
			    String v = value.toString();// 193;#2_193;#211;#2_211
			    if (!"".equals(v)) {
				String[] vArr = v.split(";#");
				String query = "<Query><Where><Eq><FieldRef Name='ID' /><Value Type='Number'>" + vArr[0]
					+ "</Value></Eq></Where></Query>";
				List<? extends SPListItem> objs = getListItems(soapUtil, clas, query);
				if (!objs.isEmpty())
				    reflectValue = objs.get(0);
			    }
			}
			break;
		    }

		    pd.getWriteMethod().invoke(victim, reflectValue);
		}
	    }
	}
    }

    // chops a list into non-view sublists of length L
    static <T> List<List<T>> chopped(List<T> list, final int L) {
	List<List<T>> parts = new ArrayList<List<T>>();
	final int N = list.size();
	for (int i = 0; i < N; i += L) {
	    parts.add(new ArrayList<T>(list.subList(i, Math.min(N, i + L))));
	}
	return parts;
    }

    private static Object parseValueByClass(String value, Class c) throws ParseException {
	Object reflectValue = null;
	if (c == Number.class) {
	    reflectValue = NumberFormat.getInstance().parse(value);
	} else if (c == String.class) {
	    reflectValue = SoapUtil.urlDecode(value);
	    String reflectS = reflectValue.toString();
	    if (reflectS.startsWith("<div")) {
		reflectS = reflectS.substring(reflectS.indexOf('>') + 1, reflectS.length() - "</div>".length());
		reflectValue = reflectS;
	    }
	} else if (c == Date.class) {
	    if (value != null) {
		reflectValue = readSpDateFormater.parse(value);
	    }
	} else if (c == Boolean.class) {
	    if ("1".equals(value))
		reflectValue = new Boolean(true);
	    if ("0".equals(value))
		reflectValue = new Boolean(true);
	}
	return reflectValue;
    }

    public static <T extends SPListItem> List<T> getListItems(SoapUtil soapUtil, Class<T> listItem) {
	return getListItems(soapUtil, listItem, "");
    }

    public static <T extends SPListItem> Map<Number, T> getListItemsAsMap(SoapUtil soapUtil, Class<T> listItem) {
	return getListItemsAsMap(soapUtil, listItem, "");
    }

    public static <T extends SPListItem> List<T> getListItems(SoapUtil soapUtil, Class<T> listItem, String query) {
	List<T> items = new ArrayList<>();
	SPList spList = listItem.getAnnotation(SPList.class);
	String listName = spList.name();

	try {
	    T c = listItem.newInstance();
	    String soapRequest = soapUtil.buildGetListItemsRequest(listName, "", query, c.getFieldRefList(), "", "");
	    String resp = soapUtil.makeGetListItemsSOAPCall(soapRequest);
	    List<String> rows = new ArrayList<>(Arrays.asList(resp.split("<z:row")));
	    rows.remove(0);
	    if (!rows.isEmpty()) {
		String lastRow = rows.remove(rows.size() - 1);
		lastRow = lastRow.substring(0, lastRow.indexOf("</rs:data>"));
		rows.add(lastRow);
		for (String row : rows) {
		    c = listItem.newInstance();
		    items.add(c);

		    SPUtil.setFieldList(row, c, soapUtil);
		}
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	    log.error(e);
	}
	return items;
    }

    public static <T extends SPListItem> Map<Number, T> getListItemsAsMap(SoapUtil soapUtil, Class<T> listItem,
	    String query) {
	Map<Number, T> items = new HashMap<>();
	SPList spList = listItem.getAnnotation(SPList.class);
	String listName = spList.name();

	try {
	    T c = listItem.newInstance();
	    String soapRequest = soapUtil.buildGetListItemsRequest(listName, "", query, c.getFieldRefList(), "", "");
	    String resp = soapUtil.makeGetListItemsSOAPCall(soapRequest);
	    List<String> rows = new ArrayList<>(Arrays.asList(resp.split("<z:row")));
	    rows.remove(0);
	    if (!rows.isEmpty()) {
		String lastRow = rows.remove(rows.size() - 1);
		lastRow = lastRow.substring(0, lastRow.indexOf("</rs:data>"));
		rows.add(lastRow);
		for (String row : rows) {
		    c = listItem.newInstance();
		    SPUtil.setFieldList(row, c, soapUtil);
		    items.put(c.getSpId(), c);
		}
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	    log.error(e);
	}
	return items;
    }

    public static String getListName(Class cls) {
	Annotation[] annotations = cls.getAnnotations();
	for (Annotation a : annotations) {
	    if (a instanceof SPList) {
		SPList l = (SPList) a;
		return l.name();
	    }
	}
	return null;
    }

    public static String buildQueryForIds(List<Number> ids) {
	StringBuilder query = new StringBuilder();
	query.append("<Eq><FieldRef Name='ID' /><Value Type='Number'>" + ids.get(0) + "</Value></Eq>");
	for (int i = 1; i < ids.size(); i++) {
	    query.insert(0, "<Or>");
	    query.append("<Eq><FieldRef Name='ID' /><Value Type='Number'>" + ids.get(i) + "</Value></Eq></Or>");
	}
	query.insert(0, "<Query><Where>");
	query.append("</Where></Query>");
	return query.toString();
    }

    public static void main(String[] args) {
	List<Number> list = new ArrayList<>();
	for (int x = 0; x < 4; x++)
	    list.add(x);
	System.out.println(buildQueryForIds(list));
    }
}
