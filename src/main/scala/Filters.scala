package porteurbars

import org.json4s.{ JArray, JNothing, JObject, JValue }

trait Filters { self: Template =>
  /** filters only those containers that have exposed ports */
  def exposed =
    filter { inspected: JValue =>
      (inspected \ "NetworkSettings" \ "Ports") match {
        case JNothing => false
        case _ => true
      }
    }

  /** filters only those containers that have published ports */
  def published =
    filter { inspected: JValue =>
      (inspected \ "NetworkSettings" \ "Ports") match {
        case JObject(fields) => 
          (for {
            (_, JArray(xs)) <- fields
            if xs.nonEmpty
          } yield true).nonEmpty
        case _ => false
      }
    }
}
