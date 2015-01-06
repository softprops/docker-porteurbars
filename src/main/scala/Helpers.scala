package porteurbars

import com.github.jknack.handlebars.{ Context, Options }
import org.json4s.JValue
import org.json4s.native.JsonMethods.{ pretty, render }
import fixiegrips.{ Json4sHelpers, ScalaHelpers }

/** docker specific handlebars helpers*/
trait DockerHelpers {
  def inspect(obj: JValue): String =
    pretty(render(obj))

  def truncateId(id: String): String =
    id.take(12)

  def stripSlash(str: String): String =
    str.replaceFirst("/", "")

  /** parses port into components @port and @type (tcp or udp) */
  def portspec(str: String, options: Options): CharSequence = {
    val sb = new StringBuilder()
    if (str.isEmpty) sb.append(options.inverse()) else {
      val parent = options.context
      val (port, typ) = str.split("/", 2) match {
        case Array(port, typ) => (port, typ)
        case Array(port) => (port, "tcp")
      }
      val ctx = Context.newBuilder(parent, str)
        .combine("@port", port)
        .combine("@type", typ)
        .build()
      sb.append(options(options.fn, ctx))
      ctx.destroy()
    }
    sb
  }

  /** parses docker image string into @registry @repo and @tag components */
  def imagespec(str: String, options: Options): CharSequence = {
    val sb = new StringBuilder()
    if (str.isEmpty) sb.append(options.inverse()) else {
      val parent = options.context
      val ctxBuilder = Context.newBuilder(parent, str)
      val (reg, rest) = str.split("/", 2) match {
        case Array(reg, rest) => (reg, rest)
        case Array(rest) => ("", rest)
      }
      val (repo, tag) = rest.split(":", 2) match {
        case Array(repo, tag) => (repo, tag)
        case Array(repo) => (repo, "latest")
      }
      val ctx  = ctxBuilder
        .combine("@registry", reg)
        .combine("@repo", repo)
        .combine("@tag", tag)
        .build()
      sb.append(options(options.fn, ctx))
      ctx.destroy()
    }
    sb
  }

  def kv(combined: String, options: Options) = keyvalue(combined, options)

  def keyvalue(combined: String, options: Options): CharSequence = {
    val (key, value) = combined.split("=", 2) match {
      case Array(k)    => (k, "")
      case Array(k, v) => (k, v)
    }
    val parent = options.context
    val next = Context.newBuilder(
      parent, combined)
      .combine("@key", key)
      .combine("@value", value)
      .build()
    val out = options(options.fn, next)
    next.destroy
    out
  }
}

object Helpers extends ScalaHelpers with DockerHelpers
