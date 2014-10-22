package porteurbars

import com.github.jknack.handlebars.{ Context, Handlebars, Template, ValueResolver }
import tugboat.Docker
import org.json4s.native.JsonMethods._
import scala.concurrent.{ Future, ExecutionContext }

object Bars {
  val resolvers = ValueResolver.VALUE_RESOLVERS ++ Array[ValueResolver](Json4sResolver)
  def newContext(obj: Object) =
    Context.newBuilder(obj)
      .resolver(resolvers:_*)
      .build()
  val compiler = new Handlebars().registerHelpers(Helpers)
}

case class Bars
 (docker: Docker, templatePath: String)
 (implicit val ec: ExecutionContext) {
  val template: Template = Bars.compiler.compile(templatePath)
  def apply(): Future[String] = {
    docker.containers.list(dispatch.as.String).map { str =>
      template.apply(Bars.newContext(parse(str)))
    }
  }
}
