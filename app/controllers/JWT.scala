package controllers

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import play.api.libs.json.{Json, JsValue}

import scalaz.syntax.id._

final case class JWT(claims: JsValue)

object JWT extends (JsValue ⇒ JWT) {
  def isValid(token: String, key: String): Boolean = {
    token.split('.') match {
      case Array(header, payload, signature) ⇒
        val s = decode(signature)
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = new SecretKeySpec(decode(key), "HmacSHA256")
        mac.init(keySpec)
        mac.update(header.getBytes)
        mac.update('.'.toByte)
        mac.update(payload.getBytes)
        val calculatedSignature = mac.doFinal()
        java.util.Arrays.equals(calculatedSignature, s)
      case _ ⇒ false
    }
  }

  def parse(str: String): Option[JWT] = {
    str.split('.') match {
      case Array(_, encodedPayload, _) ⇒
        encodedPayload |> decode |> parseJson map JWT
      case _ ⇒ None
    }
  }

  private def decode(encoded: String): Array[Byte] = {
    import com.google.common.io.BaseEncoding
    BaseEncoding.base64Url().decode(encoded)
  }

  private def parseJson(str: Array[Byte]): Option[JsValue] = {
    import com.fasterxml.jackson.core.JsonParseException
    import com.fasterxml.jackson.databind.JsonMappingException
    import scala.util.control.Exception._

    catching(classOf[JsonParseException], classOf[JsonMappingException]) opt Json.parse(str)
  }
}
