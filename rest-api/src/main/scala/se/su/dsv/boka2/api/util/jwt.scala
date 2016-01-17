package se.su.dsv.boka2.api.util

import java.time.Instant

import argonaut.{DecodeJson, Json, Parse}

import scalaz.concurrent.Task
import scalaz.syntax.either._
import scalaz.syntax.std.option._
import scalaz.syntax.traverse._
import scalaz.{-\/, \/, \/-}


object jwt {
  type Base64String = String

  trait Signer {
    def sign(data: String, algorithm: Algorithm): Signer.SignResult
  }
  object Signer {
    sealed trait SignResult
    final case class Signature(signature: Array[Byte]) extends SignResult
    case object UnsupportedAlgorithm extends SignResult
  }

  implicit val i: DecodeJson[Instant] = DecodeJson.LongDecodeJson.map(Instant.ofEpochSecond)
  implicit val x: DecodeJson[ReservedClaims] = DecodeJson.jdecode2L(ReservedClaims)("exp", "sub")

  sealed trait ParseFailure
  case object MalformedToken extends ParseFailure
  case object MalformedHeader extends ParseFailure
  case object MalformedSignature extends ParseFailure
  case object MalformedPayload extends ParseFailure
  case object InvalidSignature extends ParseFailure
  case object Expired extends ParseFailure
  final case class UnsupportedAlgorithm(algorithm: Algorithm) extends ParseFailure
  final case class UnknownAlgorithm(algorithm: String) extends ParseFailure

  sealed trait Algorithm
  case object HS256 extends Algorithm

  final case class ReservedClaims(expiration: Option[Instant], subject: Option[String])
  final case class JWT(reservedClaims: ReservedClaims, claims: Json)

  private final case class RawJWT(header: Base64String, payload: Base64String, signature: Base64String)

  def parse(signer: Signer)(token: String): Task[ParseFailure \/ JWT] = {
    val jwt = for {
      raw ← split(token)
      decodedHeader ← decodeS(raw.header).toRightDisjunction(MalformedHeader)
      algorithm ← getAlgorithm(decodedHeader)
      calculatedSignature ← signer.sign(s"${raw.header}.${raw.payload}", algorithm) match {
        case Signer.Signature(signature) ⇒ signature.right
        case Signer.UnsupportedAlgorithm ⇒ UnsupportedAlgorithm(algorithm).left
      }
      tokenSignature ← decode(raw.signature).toRightDisjunction[ParseFailure](MalformedSignature)
      _ ← checkSignature(calculatedSignature, tokenSignature)
      result ← parsePayload(raw.payload)
    } yield result

    jwt.traverseM(j ⇒ expired(j).map(if (_) -\/(Expired) else \/-(j)))
  }

  private def split(str: String): ParseFailure \/ RawJWT = {
    str.split('.') match {
      case Array(header, payload, signature) ⇒
        \/-(RawJWT(header, payload, signature))
      case _ ⇒ -\/(MalformedToken)
    }
  }

  private def getAlgorithm(header: String): ParseFailure \/ Algorithm =
    Parse.parseWith(header, _.field("alg").flatMap(_.string) match {
      case Some("HS256") ⇒ \/-(HS256)
      case Some(alg) ⇒ -\/(UnknownAlgorithm(alg))
      case None ⇒ -\/(MalformedHeader)
    }, _ ⇒ -\/(MalformedHeader))

  private def checkSignature(calculated: Array[Byte], token: Array[Byte]) =
    if (java.util.Arrays.equals(calculated, token)) ().right[ParseFailure]
    else InvalidSignature.left

  private def expired(jwt: JWT): Task[Boolean] =
    jwt.reservedClaims.expiration.fold(Task.now(false)) { expiration ⇒
      Task.delay(Instant.now).map(_.isAfter(expiration))
    }

  private def parsePayload(payload: String) = for {
    decodedPayload ← decodeS(payload).toRightDisjunction(MalformedPayload)
    result ← Parse.parseWith(
      decodedPayload,
      json ⇒ {
        json.as[ReservedClaims].toDisjunction.bimap(
          _ ⇒ MalformedPayload,
          JWT(_, json)
        )
      },
      _ ⇒ -\/(MalformedPayload))
  } yield result

  import java.nio.charset.StandardCharsets
  private def decodeS(str: String) =
    decode(str).map(new String(_, StandardCharsets.UTF_8))
  private def decode(str: Base64String) = {
    import java.util.Base64

    import scala.util.control.Exception.catching
    catching(classOf[IllegalArgumentException])
      .opt(Base64.getUrlDecoder.decode(str.getBytes(StandardCharsets.UTF_8)))
  }
}

