package ch.ethz.id.sis.rrp
package http
package cookie

import javax.crypto.Cipher
import java.nio.charset.StandardCharsets
import java.util.Base64

case class Encrypter(override val secret: String, override val salt: String) extends CryptoBase:

  def encrypt(input: String): String =
    val enc = Cipher.getInstance(algorithm)
    enc.init(Cipher.ENCRYPT_MODE, getKey())

    val content    = input.getBytes(StandardCharsets.UTF_8)
    val cipherText = enc.doFinal(content)
    Base64.getEncoder.encodeToString(cipherText)
