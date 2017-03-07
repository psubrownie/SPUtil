package solutions.titania.SP;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.beanutils.PropertyUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface SPListItem {

    static final SimpleDateFormat spDateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // String toFieldList();

    Number getSpId();

    void setSpId(Number id);

    @JsonIgnore
    @XmlTransient
    default String getFieldRefList() {
	StringBuilder sb = new StringBuilder();
	// sb.append("<viewFields><ViewFields>");
	for (Field field : SPUtil.getInheritedFields(this.getClass())) {
	    if (field.isAnnotationPresent(SPListColumn.class)) {
		SPListColumn column = field.getAnnotation(SPListColumn.class);
		if (!column.name().equalsIgnoreCase("ID"))
		    sb.append("<FieldRef Name='" + column.name() + "' />");
	    }
	}

	// sb.append("</ViewFields></viewFields>");
	return sb.toString();
    }

    @JsonIgnore
    @XmlTransient
    default String getFieldList() throws IntrospectionException, ReflectiveOperationException, IllegalArgumentException,
	    InvocationTargetException {
	return getFieldList(null);
    }

    @JsonIgnore
    @XmlTransient
    public static List<Field> getAllFields(List<Field> fields, Class<?> type) {
	fields.addAll(Arrays.asList(type.getDeclaredFields()));

	if (type.getSuperclass() != null) {
	    fields = getAllFields(fields, type.getSuperclass());
	}

	return fields;
    }

    @JsonIgnore
    @XmlTransient
    default String getFieldList(List<String> changedFields) throws IntrospectionException, ReflectiveOperationException,
	    IllegalArgumentException, InvocationTargetException {
	StringBuilder sb = new StringBuilder();

	List<Field> fields = getAllFields(new ArrayList<Field>(), this.getClass());

	for (Field field : fields) {
	    if (field.isAnnotationPresent(SPListColumn.class)) {
		SPListColumn column = field.getAnnotation(SPListColumn.class);
		if (!column.name().equals("ID") && column.readonly() == false && (changedFields == null
			|| (changedFields != null && changedFields.contains(field.getName())))) {
		    PropertyDescriptor pd = new PropertyDescriptor(field.getName(), this.getClass());
		    Object value = pd.getReadMethod().invoke(this);
		    if (value == null) {
			sb.append("<Field Name='" + column.name() + "'></Field>");
		    } else {
			switch (column.type()) {
			case BOOLEAN:
			    sb.append("<Field Name='" + column.name() + "'>" + ((Boolean) value ? "1" : "0")
				    + "</Field>");
			    break;
			case DATE:
			    Date date = (Date) value;
			    if (date != null) {
				sb.append("<Field Name='" + column.name() + "'>" + spDateFormater.format(date)
					+ "</Field>");
			    }
			    break;
			case MULT_LINE:
			case SINGLE_LINE:
			case LOOKUP_ADDITIONAL_FIELD:
			    sb.append("<Field Name='" + column.name() + "'>" + SoapUtil.urlEncode(value.toString())
				    + "</Field>");
			    break;
			case NUMBER:
			    Number num = (Number) value;
			    if (num.floatValue() == num.intValue()) {
				sb.append("<Field Name='" + column.name() + "'>" + num.intValue() + "</Field>");
			    } else {
				sb.append("<Field Name='" + column.name() + "'>" + num.floatValue() + "</Field>");
			    }
			    break;
			case LOOKUP:
			    if (value instanceof SPLookupItem) {
				SPLookupItem item = (SPLookupItem) value;
				if (item.getSpId() != null)
				    sb.append("<Field Name='" + column.name() + "'>" + item.getSpId() + ";#"
					    + SoapUtil.urlEncode(item.getName()) + "</Field>");
				else
				    sb.append("<Field Name='" + column.name() + "'></Field>");
			    } else {
				String lookupDisplay = column.lookupDisplayField();
				Set<Field> valueFields = findFields(value.getClass(), SPListColumn.class);
				Field spIdField = null;
				Field displayField = null;
				for (Field f2 : valueFields) {
				    if (f2.getAnnotation(SPListColumn.class).name().equals("ID"))
					spIdField = f2;
				    else if (f2.getAnnotation(SPListColumn.class).name().equals(lookupDisplay))
					displayField = f2;
				}

				if (displayField == null) {
				    throw new RuntimeException("Field Display Value unknown");
				} else {// TODO could use more testing
				    Object lookupID = PropertyUtils.getProperty(value, spIdField.getName());
				    Object lookupDisplayValue = PropertyUtils.getProperty(value,
					    displayField.getName());
				    sb.append("<Field Name='" + column.name() + "'>" + lookupID + ";#"
					    + SoapUtil.urlEncode(lookupDisplayValue.toString()) + "</Field>");
				}
			    }
			    break;
			case LOOKUP_MULT:

			    sb.append("<Field Name='" + column.name() + "'>");
			    StringBuilder sb2 = new StringBuilder();
			    Collection<Object> list = (Collection<Object>) value;
			    for (Object i : list) {
				if (i instanceof SPLookupItem) {
				    sb2.append(";#" + ((SPLookupItem) i).getSpId() + ";#"
					    + SoapUtil.urlEncode(((SPLookupItem) i).getName()));
				} else {
				    // appendDynamicFK(sb, column, value);
				    String lookupDisplay = column.lookupDisplayField();
				    Set<Field> valueFields = findFields(i.getClass(), SPListColumn.class);
				    Field spIdField = null;
				    Field displayField = null;
				    for (Field f2 : valueFields) {
					if (f2.getAnnotation(SPListColumn.class).name().equals("ID"))
					    spIdField = f2;
					else if (f2.getAnnotation(SPListColumn.class).name().equals(lookupDisplay))
					    displayField = f2;
				    }

				    if (displayField == null) {
					throw new RuntimeException("Field Display Value unknown");
				    } else {// TODO could use more testing
					Object lookupID = PropertyUtils.getProperty(i, spIdField.getName());
					Object lookupDisplayValue = PropertyUtils.getProperty(i,
						displayField.getName());
					sb2.append(";#" + lookupID + ";#"
						+ SoapUtil.urlEncode(lookupDisplayValue.toString()));
				    }
				}

			    }
			    if (sb2.length() >= 2)
				sb.append(sb2.substring(2));
			    sb.append("</Field>");
			    break;
			default:
			    break;
			}
		    }
		}
	    }
	}
	for (Method method : this.getClass().getDeclaredMethods()) {
	    if (method.isAnnotationPresent(SPListColumn.class)) {
		SPListColumn column = method.getAnnotation(SPListColumn.class);
		Object value = method.invoke(this);
		switch (column.type()) {
		case BOOLEAN:
		    sb.append("<Field Name='" + column.name() + "'>" + ((Boolean) value ? "1" : "0") + "</Field>");
		    break;
		case DATE:
		    Date date = (Date) value;
		    if (date != null) {
			sb.append("<Field Name='" + column.name() + "'>" + spDateFormater.format(date) + "</Field>");
		    }
		    sb.append("<Field Name='" + column.name() + "'></Field>");
		    break;
		case MULT_LINE:
		case SINGLE_LINE:
		case LOOKUP_ADDITIONAL_FIELD:
		    sb.append("<Field Name='" + column.name() + "'>" + SoapUtil.urlEncode((String) value) + "</Field>");
		    break;
		case NUMBER:
		    sb.append("<Field Name='" + column.name() + "'>" + (Number) value + "</Field>");
		    break;
		case LOOKUP:
		case LOOKUP_MULT:
		    SPLookupItem item = (SPLookupItem) value;
		    sb.append("<Field Name='" + column.name() + "'>" + item.getSpId() + ";#"
			    + SoapUtil.urlEncode(item.getName()) + "</Field>");
		    break;
		}

	    }
	}
	return sb.toString();
    }

    /**
     * @return null safe set
     */
    static Set<Field> findFields(Class<?> classs, Class<? extends Annotation> ann) {
	Set<Field> set = new HashSet<>();
	Class<?> c = classs;
	while (c != null) {
	    for (Field field : c.getDeclaredFields()) {
		if (field.isAnnotationPresent(ann)) {
		    set.add(field);
		}
	    }
	    c = c.getSuperclass();
	}
	return set;
    }
}
