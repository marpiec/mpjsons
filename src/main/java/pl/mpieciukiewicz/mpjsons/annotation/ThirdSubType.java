package pl.mpieciukiewicz.mpjsons.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define third subtype of a generic type.
 * <p>
 * &#64;ThirdSubType(classOf[Int])<br>
 * var someTuple:(String, String, Int) = _
 * @author Marcin Pieciukiewicz
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ThirdSubType {
    public Class<?> value();
}
