package porteurbars

import java.io.{ File, FileInputStream, InputStream }
import java.net.URL
import java.nio.charset.Charset

@annotation.implicitNotFound(
  msg = "porteurbars.Source[T] type class instance for type ${T} not found")
trait Source[T] {
  def contents(in: T): String
}

/** definitions of various kinds of Inputs */
object Source {
  private[this] val utf8 = Charset.forName("utf-8")

  implicit val str: Source[String] =
    new Source[String] {
      def contents(in: String): String = in
    }

  implicit val utf8url: Source[URL] =
    new Source[URL] {
      def contents(in: URL): String = 
        url.contents(in, utf8)
    }

  implicit val url: Source[(URL, Charset)] =
    new Source[(URL, Charset)] {
      def contents(in: (URL, Charset)): String =
        io.Source.fromURL(in._1)(io.Codec(in._2)).mkString
    }

  implicit val utf8file: Source[File] =
    new Source[File] {
      def contents(in: File): String =
        file.contents(in, utf8)
    }

  implicit val file: Source[(File, Charset)] =
    new Source[(File, Charset)] {
      def contents(in: (File, Charset)): String =
        stream.contents(new FileInputStream(in._1), in._2)
    }

  implicit val utf8stream: Source[InputStream] =
    new Source[InputStream] {
      def contents(in: InputStream): String =
        stream.contents(in, utf8)
    }

  implicit val stream: Source[(InputStream, Charset)] =
    new Source[(InputStream, Charset)] {
      def contents(in: (InputStream, Charset)): String =
        io.Source.fromInputStream(in._1, in._2.name).mkString
    }


  def apply[T: Source] = implicitly[Source[T]]
}
