package pl.mpieciukiewicz.mpjsons.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define second subtype of a generic type.
 * <p>
 * &#64;FirstSubType(classOf[Int]) &#64;SecondSubType(classOf[Long])<br>
 * var primitiveMap:Map[Int, Long] = _
 * @author Marcin Pieciukiewicz
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SecondSubType {
    public Class<?> value();
}
