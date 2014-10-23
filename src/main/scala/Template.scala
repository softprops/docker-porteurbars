package porteurbars

import com.github.jknack.handlebars.{
  Context, Handlebars, Template => HandlebarsTemplate, ValueResolver
}
import tugboat.Docker
import org.json4s.JArray
import org.json4s.native.JsonMethods._
import scala.concurrent.{ Future, ExecutionContext }

object Template {
  val resolvers: Array[ValueResolver] =
    Array(Json4sResolver, ScalaResolver) ++ ValueResolver.VALUE_RESOLVERS
  def newContext(obj: Object) =
    Context.newBuilder(obj)
      .combine("Env", sys.env)
      .resolver(resolvers:_*)      
      .build()
  val compiler: Handlebars = new Handlebars().registerHelpers(Helpers)
}

case class Template
 (docker: Docker,
  templatePath: String,
  compiler: Handlebars = Template.compiler)
 (implicit val ec: ExecutionContext) {
  lazy val template: HandlebarsTemplate = compiler.compile(templatePath)
  private[this] lazy val containers =
    docker.containers.list
  private[this] lazy val inspect =
    docker.containers.get(_)
  def render(inspected: JValue) = template(Template.newContext(inspected))
  def apply(): Future[String] = {
    containers().flatMap { up =>
      Future.sequence(up.map { c =>
        inspect(c.id)(dispatch.as.String).map(parse(_))
      })
    }.map(JArray(_)).map(render)
  }
}
