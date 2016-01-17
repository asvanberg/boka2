package se.su.dsv.boka2.api.util

import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import se.su.dsv.boka2.api.util.jwt.Signer.SignResult
import se.su.dsv.boka2.api.util.jwt.{Algorithm, HS256, Signer}

final class HS256Signer(key: Array[Byte]) extends Signer {
  override def sign(data: String, algorithm: Algorithm): SignResult = {
    algorithm match {
      case HS256 =>
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = new SecretKeySpec(key, "HmacSHA256")
        mac.init(keySpec)
        mac.update(data.getBytes(StandardCharsets.UTF_8))
        Signer.Signature(mac.doFinal())
    }
  }
}
