package porteurbars

import com.github.jknack.handlebars.{ Context, Options }
import com.github.jknack.handlebars.helper.{ EachHelper, IfHelper }
import scala.collection.Iterable
import org.json4s.{ JArray, JValue }
import org.json4s.native.JsonMethods.{ pretty, render }

trait DockerHelpers {
  def inspect(obj: JValue): String =
    pretty(render(obj))

  def kv(combined: String, options: Options) = keyvalue(combined, options)

  def keyvalue(combined: String, options: Options): CharSequence = {
    val (key, value) = combined.split("=", 2) match {
      case Array(k)    => (k, "")
      case Array(k, v) => (k, v)
    }
    val parent = options.context
    val next = Context.newBuilder(
      parent, Map("key" -> key, "value" -> value))
     .build()
    val out = options(options.fn, next)
    next.destroy
    out
  }
}

object Helpers extends DockerHelpers {

  /** overriding default `each` helper to support scala iterable things */
  def each(obj: Object, options: Options): CharSequence =
    obj match {
      case ary: JArray =>
        eachScalaIterable(ary.arr, options)
      case  it: Iterable[_] =>
        eachScalaIterable(it, options)
      case _ =>
        EachHelper.INSTANCE(obj, options)
    }

  /** overriding default `if` helper to support scala falsy things */
  def `if`(obj: Object, options: Options): CharSequence =
    obj match {
      case JArray(ary) =>
        `if`(ary, options)
      case it: Iterable[_] =>
        if (it.isEmpty) options.inverse() else options.fn()
      case _ =>
        IfHelper.INSTANCE(obj, options)
    }

  private def eachScalaIterable(
    it: Iterable[_], options: Options): String = {
    val sb = new StringBuilder()
    if (it.isEmpty) sb.append(options.inverse()) else {
      val parent = options.context
      def append(i: Int, iter: Iterator[_]): Unit =
        if (iter.hasNext) {
          val even = i % 2 == 0
          val ctx: Context = Context.newBuilder(parent, iter.next)
            .combine("@index", i)
            .combine("@first", if (i == 0) "first" else "")
            .combine("@last", if (!iter.hasNext) "last" else "")
            .combine("@odd", if (!even) "odd" else "")
            .combine("@even", if (even) "even" else "")
            .build()
          sb.append(options(options.fn, ctx))
          ctx.destroy
          append(i + 1, iter)
        }
      append(0, it.iterator)
    }
    sb.toString()
  }
}
