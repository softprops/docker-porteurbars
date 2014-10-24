package porteurbars

import com.github.jknack.handlebars.{
  Context, Handlebars, Template => HandlebarsTemplate, ValueResolver
}
import tugboat.Docker
import org.json4s.{ JArray, JValue }
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
  def apply(templatePath: String)(implicit ec: ExecutionContext): Template =
    Template(templatePath, Docker())
}

case class Template
  (templatePath: String,
  docker: Docker,
  compiler: Handlebars = Template.compiler)
 (implicit val ec: ExecutionContext) {
  lazy val template: HandlebarsTemplate = compiler.compile(templatePath)
  private[this] lazy val containers =
    docker.containers.list
  private[this] lazy val inspect =
    docker.containers.get(_)

  def configure(update: Handlebars => Handlebars) =
    copy(compiler = update(compiler))

  def apply(): Future[String] = {
    containers().flatMap { up =>
      Future.sequence(up.map { c =>
        inspect(c.id)(dispatch.as.String).map(parse(_))
      })
    }.map(JArray(_)).map(render)
  }

  def close() = docker.close()

  private def render(inspected: JValue) =
    template(Template.newContext(inspected))
}
