package route

import cats.effect.IO
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import cats.effect.IO
import io.circe.derivation.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.Http4sDsl
import service.LotteryService
import service.ServiceError.{UnexpectedError, ValidationError}

object LotteryRoutes {
  def routes(service: LotteryService): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*
    HttpRoutes
      .of[IO] {
        case req @ POST -> Root / "entry"            =>
          for {
            request  <- req.as[EntryRequest]
            result   <- service.submitEntry(request).handleError(e => Left(UnexpectedError("An unexpected error occurred: " + e.getMessage)))
            response <- result match
                          case Left(ValidationError(errorMessage)) => BadRequest(errorMessage)
                          case Left(UnexpectedError(errorMessage)) => InternalServerError(errorMessage)
                          case Right(value)                        => Ok(value.asJson)
          } yield response
        case req @ GET -> Root / "winner"            =>
          Ok()
        case req @ POST -> Root / "lottey" / "close" =>
          Ok()
      }
  }

  given Configuration = Configuration.default.withSnakeCaseMemberNames

  case class EntryRequest(
    participantId: Int,
    lotteryId: Int,
  ) derives ConfiguredDecoder, ConfiguredEncoder

  case class EntryResponse(
    entryId: Int,
  ) derives ConfiguredEncoder
}
