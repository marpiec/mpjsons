package pl.marpiec.mpjsons.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define fourth subtype of a generic type.
 * @author Marcin Pieciukiewicz
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FourthSubType {
    public Class<?> value();
}
