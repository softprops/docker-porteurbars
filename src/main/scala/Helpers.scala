package porteurbars

import com.github.jknack.handlebars.{ Context, Options }
import com.github.jknack.handlebars.helper.{ EachHelper, IfHelper }
import scala.collection.Iterable
import org.json4s.{ JArray, JObject, JValue }
import org.json4s.native.JsonMethods.{ pretty, render }

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

/** scala specific handlebar helpers */
trait ScalaHelpers {

  /** overriding default `each` helper to support scala iterable things */
  def each(obj: Object, options: Options): CharSequence =
    obj match {
      case  it: Iterable[_] =>
        eachIterable(it, options)
      case _ =>
        EachHelper.INSTANCE(obj, options)
    }

  /** overriding default `if` helper to support scala falsy things */
  def `if`(obj: Object, options: Options): CharSequence =
    obj match {
      case it: Iterable[_] =>
        if (it.isEmpty) options.inverse() else options.fn()
      case _ =>
        IfHelper.INSTANCE(obj, options)
    }

  protected def eachNamed(
    named: Iterable[(String, _)], options: Options): String = {
    val sb = new StringBuilder()
    if (named.isEmpty) sb.append(options.inverse()) else {
      val parent = options.context
      for ((key, value) <- named) {
        val ctx = Context.newBuilder(parent, value)
          .combine("@key", key)
          .build()
        sb.append(options(options.fn, ctx))
        ctx.destroy()
      }
    }
    sb.toString
  }

  protected def eachIterable(
    it: Iterable[_], options: Options): String = {
    val sb = new StringBuilder()
    if (it.isEmpty) sb.append(options.inverse()) else {
      val parent = options.context
      def append(i: Int, iter: Iterator[_]): Unit =
        if (iter.hasNext) {
          val even = i % 2 == 0
          val ctx = Context.newBuilder(parent, iter.next)
            .combine("@index", i)
            .combine("@first", if (i == 0) "first" else "")
            .combine("@last", if (!iter.hasNext) "last" else "")
            .combine("@odd", if (!even) "odd" else "")
            .combine("@even", if (even) "even" else "")
            .build()
          sb.append(options(options.fn, ctx))
          ctx.destroy()
          append(i + 1, iter)
        }
      append(0, it.iterator)
    }
    sb.toString()
  }
}

/** json4s specific handlebar helpers */
trait Json4sHelpers extends ScalaHelpers {
  override def each(obj: Object, options: Options): CharSequence =
    obj match {
      case ary: JArray =>
        eachIterable(ary.arr, options)
      case JObject(fields) =>
        eachNamed(fields, options)
      case _ =>
        super.each(obj, options)
    }

  /** overriding default `if` helper to support scala falsy things */
  override def `if`(obj: Object, options: Options): CharSequence =
    obj match {
      case JArray(ary) =>
        `if`(ary, options)
      case _ =>
        super.`if`(obj, options)
    }
}

object Helpers extends Json4sHelpers with DockerHelpers
