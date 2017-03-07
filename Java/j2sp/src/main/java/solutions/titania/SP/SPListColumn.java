package solutions.titania.SP;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SPListColumn {
	String name();

	Type type() default Type.SINGLE_LINE;
	
	boolean readonly() default false;
	
	String lookupDisplayField() default "Title";

	public enum Type {
		SINGLE_LINE, MULT_LINE, DATE, NUMBER, BOOLEAN,LOOKUP,LOOKUP_MULT, LOOKUP_ADDITIONAL_FIELD;
	}
}
