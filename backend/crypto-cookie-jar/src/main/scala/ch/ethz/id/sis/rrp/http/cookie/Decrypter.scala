package ch.ethz.id.sis.rrp
package http
package cookie

import java.util.Base64
import javax.crypto.Cipher

case class Decrypter(override val secret: String, override val salt: String) extends CryptoBase:

  def decrypt(value: String): Option[String] =

    val dec = Cipher.getInstance(algorithm)
    dec.init(Cipher.DECRYPT_MODE, getKey())

    try
      val cipherText = Base64.getDecoder().decode(value)
      val plainText  = dec.doFinal(cipherText)
      Some(new String(plainText))
    catch case _: Throwable => None
