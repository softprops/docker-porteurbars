package porteurbars

import com.github.jknack.handlebars.{
  Context, Handlebars, Template => HandlebarsTemplate, ValueResolver
}
import com.github.jknack.handlebars.io.FileTemplateLoader
import org.json4s.{ JArray, JValue }
import org.json4s.native.JsonMethods._
import scala.concurrent.{ Future, ExecutionContext }
import tugboat.Docker

object Template {
  val compiler: Handlebars =
    new Handlebars(new FileTemplateLoader("")).registerHelpers(Helpers)

  val resolvers: Array[ValueResolver] =
    Array(Json4sResolver, ScalaResolver) ++ ValueResolver.VALUE_RESOLVERS

  def apply[T: Input]
   (in: T)
   (implicit ec: ExecutionContext): Template =
    new Template(implicitly[Input[T]].contents(in), Docker())

  def apply[T: Input]
   (in: T, docker: Docker)
   (implicit ec: ExecutionContext): Template =
    Template(implicitly[Input[T]].contents(in), docker)

  def newContext(obj: Object) =
    Context.newBuilder(obj)
      .combine("Env", sys.env)
      .resolver(resolvers:_*)
      .build()
}

case class Template
 (contents: String,
  docker: Docker,
  compiler: Handlebars = Template.compiler,
  filter: Option[JValue => Boolean] = None)
 (implicit val ec: ExecutionContext) extends Filters {
  lazy val template: HandlebarsTemplate =
    compiler.compileInline(contents)
  private[this] lazy val containers =
    docker.containers.list
  private[this] lazy val inspect =
    docker.containers.get(_)

  /** @return a rendered template applied with container information from all
   *          running containers */
  def apply(): Future[String] = {
    containers().flatMap { up =>
      Future.sequence(up.map { c =>
        inspect(c.id)(dispatch.as.String).map(parse(_))
      })
    }.map(JArray(_)).map(render)
  }

  /** @return a new template instance configured to use an updated
   *          handlebars compiler */
  def configure(update: Handlebars => Handlebars) =
    copy(compiler = update(compiler))

  /** filters each running container's json information to determine which
   *  running containers will be applied to the template */
  def filter(pred: JValue => Boolean) =
    copy(filter = filter.map(filt => { jv: JValue => filt(jv) && pred(jv) })
                        .orElse(Some(pred)))

  /** release underlying docker connection resources. this should only
   *  be called once for disposal */
  def close() = docker.close()

  /** renders a template directly from a json ast */
  def render(inspected: JValue) =
    template(Template.newContext(inspected))
}
