package route

import cats.effect.IO
import io.circe.derivation.ConfiguredDecoder
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import service.ParticipantService
import io.circe.Decoder.derivedConfigured
import io.circe.derivation.*
import io.circe.derivation.Configuration
import io.circe.{Codec, Decoder, Encoder}
import io.circe.syntax.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder

object ParticipantRoutes {
  def routes(service: ParticipantService): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*
    HttpRoutes
      .of[IO] { case req @ POST -> Root / "user" =>
        for {
          request  <- req.as[RegisterParticipantRequest]
          // response <- service.register()
          response <- Ok()
        } yield response
      }
  }

  case class RegisterParticipantRequest(
    id: Int,
    first_name: String,
    last_name: String,
    email: String,
  ) derives Decoder
}
