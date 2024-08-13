package route

import cats.effect.IO
import io.circe.derivation.ConfiguredDecoder
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import service.ParticipantService
import io.circe.derivation.*
import io.circe.derivation.Configuration
import io.circe.{Decoder, Encoder}
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
          result   <- service.register(request)
          response <- result match
                        case Left(value)  => BadRequest(value)
                        case Right(value) => Ok(value.asJson)
        } yield response
      }
  }

  given Configuration = Configuration.default.withSnakeCaseMemberNames

  case class RegisterParticipantRequest(
    firstName: String,
    lastName: String,
    email: String,
  ) derives ConfiguredDecoder

  case class RegisterParticipantResponse(
    id: Int,
  ) derives Encoder
}
