package porteurbars

import org.scalatest.FunSpec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import tugboat.Docker

class TemplateSpec extends FunSpec {
  describe("template") {
    it ("should render") {
      val docker = Docker()
      val template = Template(docker, "test")
      val eval = template()
      try {
        val out = Await.result(eval, Duration.Inf)
        println(out)
        assert(out.nonEmpty === true)
      } finally docker.close()
    }
  }
}
