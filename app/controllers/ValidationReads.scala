package controllers

import play.api.libs.json.{JsValue, Reads, JsPath}

import scalaz._
import scalaz.syntax.std.option._

trait ValidationReads[A] {
  def createReads(path: JsPath): Reads[A]
}

object ValidationReads {
  type ValidationParser[T] = JsValue ⇒ ValidationNel[(String, String), T]

  implicit def optionValidationReads[A: Reads] = new ValidationReads[Option[A]] {
    override def createReads(path: JsPath): Reads[Option[A]] = Reads.nullable(path)
  }

  implicit def validationReads[T: Reads] = new ValidationReads[T] {
    override def createReads(path: JsPath): Reads[T] = Reads.at(path)
  }

  def read[T: ValidationReads](fieldName: String, errorMessage: String): ValidationParser[T] = {
    json ⇒ {
      val result = implicitly[ValidationReads[T]].createReads(JsPath \ fieldName).reads(json)
      result.asOpt.toSuccess(fieldName → errorMessage).toValidationNel
    }
  }
}
