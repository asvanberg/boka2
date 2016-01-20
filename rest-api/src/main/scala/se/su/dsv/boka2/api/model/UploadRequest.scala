package se.su.dsv.boka2.api.model

import se.su.dsv.boka2.api.util.jwt.Base64String

final case class UploadRequest(name: String, contentType: String, data: Base64String)
