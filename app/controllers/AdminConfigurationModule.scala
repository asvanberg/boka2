package controllers

import algebra.Daisy
import models.Auth0Configuration
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

class AdminConfigurationModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    val auth0Config = for {
      callbackUrl ← configuration.getString("auth0.callbackUrl")
      clientId ← configuration.getString("auth0.clientId")
      secret ← configuration.getString("auth0.clientSecret")
      domain ← configuration.getString("auth0.domain")
    } yield {
      bind[Auth0Configuration].toInstance(Auth0Configuration(callbackUrl, clientId, secret, domain))
    }

    val daisyConfig = for {
      host ← configuration.getString("daisy.api.host")
      port ← configuration.getInt("daisy.api.port")
      username ← configuration.getString("daisy.api.username")
      password ← configuration.getString("daisy.api.password")
    } yield {
      bind[Daisy.Configuration].toInstance(Daisy.Configuration(host, port, username, password))
    }

    Seq(
      auth0Config.getOrElse(sys.error("Auth0 not configured")),
      daisyConfig.getOrElse(sys.error("Daisy not configured"))
    )
  }
}
