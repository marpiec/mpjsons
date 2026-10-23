package io.mpjsons.fuzz

import java.lang.reflect.Modifier

/**
 * Structural, class-aware representation of a deserialized value, used to compare results of two implementations.
 * Includes runtime classes so that e.g. a Seq returned as ArrayBuffer vs List is reported as a difference.
 */
object Deep {

  def repr(value: Any): String = value match {
    case null => "null"
    case s: String => "\"" + s.flatMap(c => if (c < 32 || c > 126 || c == '"' || c == '\\') "\\u%04x".format(c.toInt) else c.toString) + "\""
    case d: java.lang.Double => "D" + java.lang.Double.doubleToRawLongBits(d)
    case f: java.lang.Float => "F" + java.lang.Float.floatToRawIntBits(f)
    case c: java.lang.Character => "C" + c.charValue().toInt
    case n: java.lang.Number => n.getClass.getSimpleName + "(" + n + ")"
    case b: java.lang.Boolean => b.toString
    case a: Array[_] => a.getClass.getSimpleName + a.map(repr).mkString("[", ",", "]")
    case s: scala.collection.immutable.ListSet[_] => cls(s) + s.toList.map(repr).mkString("(", ",", ")")
    case m: scala.collection.immutable.ListMap[_, _] => cls(m) + m.toList.map(kv => repr(kv._1) + "->" + repr(kv._2)).mkString("(", ",", ")")
    case s: scala.collection.Set[_] => cls(s) + s.toList.map(repr).sorted.mkString("(", ",", ")")
    case m: scala.collection.Map[_, _] => cls(m) + m.toList.map(kv => repr(kv._1) + "->" + repr(kv._2)).sorted.mkString("(", ",", ")")
    case s: scala.collection.Iterable[_] => cls(s) + s.toList.map(repr).mkString("(", ",", ")")
    case p: Product => cls(p) + p.productIterator.map(repr).mkString("(", ",", ")")
    case o: AnyRef => cls(o) + fields(o.getClass).map { f =>
      f.setAccessible(true)
      f.getName + "=" + repr(f.get(o))
    }.mkString("{", ",", "}")
  }

  private def cls(o: Any): String = o.getClass.getName

  private def fields(c: Class[_]): List[java.lang.reflect.Field] =
    if (c == null || c == classOf[Object]) Nil
    else c.getDeclaredFields.toList.filterNot(f => Modifier.isStatic(f.getModifiers)).sortBy(_.getName) ++ fields(c.getSuperclass)
}
