package porteurbars

import com.github.jknack.handlebars.ValueResolver
import com.github.jknack.handlebars.context.MapValueResolver
import scala.collection.JavaConverters._
import java.util.{ Collections => JCollections, Map => JMap, Set => JSet }

object ScalaResolver extends ValueResolver {
  def resolve(ctx: Object, name: String): Object =
    ctx match {
      case map: Map[_,_] =>
        MapValueResolver.INSTANCE.resolve(map.asJava, name)
      case _ =>
        ValueResolver.UNRESOLVED
    }

  def resolve(ctx: Object) = ctx match {
    case map: Map[_,_] =>
      MapValueResolver.INSTANCE.resolve(map.asJava)
    case _ =>
      ValueResolver.UNRESOLVED
  }

  def propertySet(ctx: Object): JSet[JMap.Entry[String, Object]] =
    ctx match {
      case map: Map[_,_] =>
        MapValueResolver.INSTANCE.propertySet(map.asJava)
      case _ =>
        JCollections.emptySet[JMap.Entry[String, Object]]
    }
}
