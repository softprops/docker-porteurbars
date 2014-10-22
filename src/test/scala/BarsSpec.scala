package porteurbars

import org.scalatest.FunSpec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import tugboat.Docker

class BarsSpec extends FunSpec {
  describe("bars") {
    it ("should render a template") {
      val docker = Docker()
      val bars = Bars(docker, "test")
      try assert(Await.result(bars(), Duration.Inf).nonEmpty === true)
      finally docker.close()
    }
  }
}
