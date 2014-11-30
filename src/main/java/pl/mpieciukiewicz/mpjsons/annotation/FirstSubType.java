package pl.mpieciukiewicz.mpjsons.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define first subtype of a generic type.
 * <p>
 * &#64;FirstSubType(classOf[Long]) <br>
 * var longsList: List[Long];
 * @author Marcin Pieciukiewicz
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FirstSubType {
    public Class<?> value();
}
