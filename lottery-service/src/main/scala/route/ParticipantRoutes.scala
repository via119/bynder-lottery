package route

import cats.effect.IO
import io.circe.Decoder
import io.circe.derivation.*
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.Http4sDsl
import service.ParticipantService
import service.ServiceError.{UnexpectedError, ValidationError}

object ParticipantRoutes {
  def routes(service: ParticipantService): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*
    HttpRoutes
      .of[IO] { case req @ POST -> Root / "user" =>
        for {
          request  <- req.as[RegisterParticipantRequest]
          result   <- service.register(request).handleError(e => Left(UnexpectedError("An unexpected error occurred: " + e.getMessage)))
          response <- result match
                        case Left(ValidationError(errorMessage)) => BadRequest(errorMessage)
                        case Left(UnexpectedError(errorMessage)) => InternalServerError(errorMessage)
                        case Right(value)                        => Ok(value.asJson)
        } yield response
      }
  }

  given Configuration = Configuration.default.withSnakeCaseMemberNames

  case class RegisterParticipantRequest(
    firstName: String,
    lastName: String,
    email: String,
  ) derives ConfiguredDecoder, ConfiguredEncoder

  case class RegisterParticipantResponse(
    id: Int,
  ) derives ConfiguredEncoder
}
