package porteurbars

import java.io.{ File, InputStream }
import java.net.URL
import java.nio.charset.Charset

@annotation.implicitNotFound(
  msg = "porteurbars.Input[T] type class instance for type ${T} not found")
trait Input[T] {
  def contents(in: T): String
}

/** definitions of various kinds of Inputs */
object Input {
  private[this] val utf8 = Charset.forName("utf-8")

  implicit val str: Input[String] =
    new Input[String] {
      def contents(in: String) = in
    }

  implicit val utf8URL: Input[URL] =
    new Input[URL] {
      def contents(in: URL) = 
        fromURL(in, utf8)
    }

  implicit val EncodedURL: Input[(URL, Charset)] =
    new Input[(URL, Charset)] {
      def contents(in: (URL, Charset)) =
        fromURL(in._1, in._2)
    }

  implicit val stream: Input[InputStream] =
    new Input[InputStream] {
      def contents(in: InputStream) =
        io.Source.fromInputStream(in).mkString
    }

  private def fromURL(url: URL, charset: Charset) =
    io.Source.fromURL(url)(io.Codec(charset)).mkString
}
