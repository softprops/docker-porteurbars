package porteurbars

import com.github.jknack.handlebars.ValueResolver
import org.json4s._
import scala.collection.JavaConverters._
import java.util.{ Collections => JCollections, Map => JMap, Set => JSet }

object Json4sResolver extends ValueResolver {
  private[this] val Digit = """(\d+)""".r

  def resolve(ctx: Object, name: String): Object =
    (ctx, name) match {
      case (JArray(xs), Digit(i)) =>
        val index = i.toInt
        if (index < xs.size) resolve(xs(index))
        else ValueResolver.UNRESOLVED
      case (JObject(fields), key) =>
        fields
          .collectFirst {
            case (k, v) if k == key =>
              resolve(v)
          }
          .getOrElse {
            ValueResolver.UNRESOLVED
          }
      case _ =>
        ValueResolver.UNRESOLVED
    }

  def resolve(ctx: Object): Object =
    ctx match {
      case jv: JValue =>
        resolveJValue(jv)
      case _ =>
        ValueResolver.UNRESOLVED
    }

  private def resolveJValue(ctx: JValue): Object =
    ctx match {
      case JBool(bool) =>
        java.lang.Boolean.valueOf(bool)
      case JNull | JNothing =>
        null
      case JDouble(dub) =>
        java.lang.Double.valueOf(dub)
      case JDecimal(dec) =>
        dec
      case JInt(int) =>
        java.lang.Integer.valueOf(int.toInt)
      case JString(str) =>
        str
      case _ =>
        ctx
    }

  def propertySet(ctx: Object): JSet[JMap.Entry[String, Object]] =
    ctx match {
      case obj @ JObject(fields) =>
        (for {
          (key, _) <- fields
        } yield (key, resolve(obj, key))).toMap.asJava.entrySet()
      case _ =>
        JCollections.emptySet[JMap.Entry[String, Object]]
    }
}
