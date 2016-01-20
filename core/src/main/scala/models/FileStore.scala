package models

import scalaz.{Free, Inject}

final case class MetaData(name: String, contentType: String, size: Int)
final case class FileDescription(metaData: MetaData, data: Array[Byte])

sealed trait FileStore[A]
final case class StoreFile(identifier: String, fileDescription: FileDescription) extends FileStore[Unit]
final case class RetrieveFile(identifier: String) extends FileStore[Option[FileDescription]]

class Files[F[_]](implicit I: Inject[FileStore, F]) {
  def storeFile(identifier: String, fileDescription: FileDescription) =
    Free.liftFC(I.inj(StoreFile(identifier, fileDescription)))

  def retrieveFile(identifier: String) =
    Free.liftFC(I.inj(RetrieveFile(identifier)))
}

object Files {
  implicit def files[F[_]](implicit I: Inject[FileStore, F]) = new Files
}
