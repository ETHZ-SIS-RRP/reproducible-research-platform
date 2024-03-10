package ch.ethz.id.sis.rrp
package http
package cookie

import ch.ethz.id.sis.rrp.json.given
import io.circe.parser.decode
import io.circe.syntax._

case class CryptoCookieJar(secret: String) extends CookieJar {

  def storeSession(info: SessionInfo): String = {
    val enc = Encrypter(secret, "salt!")
    enc.encrypt(info.asJson.spaces2)
  }

  def deleteSession(id: String): Unit = ()

  def getSession(value: String): Option[SessionInfo] = {
    try {
      val dec = Decrypter(secret, "salt!")
      for
        plainText <- dec.decrypt(value)
        session   <- decode[SessionInfo](plainText).toOption
      yield session
    } catch {
      case _: Throwable => None
    }
  }
}
