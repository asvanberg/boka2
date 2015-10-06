package models

final case class Auth0Configuration(callbackUrl: String, clientId: String, secret: String, domain: String)
