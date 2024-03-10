package ch.ethz.id.sis.rrp
package http
package cookie

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

trait CryptoBase:
  val algorithm = "AES"
  val secret: String
  val salt: String

  def getKey() =
    val keySpec  = new PBEKeySpec(secret.toCharArray(), salt.getBytes(), 65536, 256) // AES-256
    val f        = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val keyBytes = f.generateSecret(keySpec).getEncoded()
    new SecretKeySpec(keyBytes, algorithm)
