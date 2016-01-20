package se.su.dsv.boka2.api.interpreter

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import argonaut.CodecJson
import argonaut.Argonaut._
import models._

import scalaz.concurrent.Task
import scalaz.std.option._
import scalaz.syntax.apply._
import scalaz.syntax.std.boolean._
import scalaz.~>

class FileSystemStorage(directory: Path) extends (FileStore ~> Task) {
  import FileSystemStorage._

  override def apply[A](fa: FileStore[A]): Task[A] = fa match {
    case StoreFile(identifier, FileDescription(metaData, data)) ⇒
      val file = directory.resolve(identifier)
      val metaFile = directory.resolve(s"$identifier.meta")
      val writeMetaData = Task.delay {
        Files.write(metaFile, metaData.asJson.pretty(spaces2).getBytes(StandardCharsets.UTF_8))
      }
      val writeFileData = Task.delay(Files.write(file, data))
      writeMetaData *> writeFileData.void
    case RetrieveFile(identifier) ⇒
      val file = directory.resolve(identifier)
      val metaFile = directory.resolve(s"$identifier.meta")
      val readMetaData = Task.delay {
        for {
          raw ← Files.exists(metaFile).option(Files.readAllBytes(metaFile))
          str = new String(raw, StandardCharsets.UTF_8)
          json ← argonaut.Parse.parseOption(str)
          metaData ← json.as[MetaData].toOption
        } yield metaData
      }
      val readFileData = Task.delay {
        Files.exists(file).option(Files.readAllBytes(file))
      }
      ^(readMetaData, readFileData)(^(_, _)(FileDescription))
  }
}

private object FileSystemStorage {
  implicit val metaDataCodec: CodecJson[MetaData] =
    casecodec3(MetaData.apply, MetaData.unapply)("name", "contentType", "size")
}
