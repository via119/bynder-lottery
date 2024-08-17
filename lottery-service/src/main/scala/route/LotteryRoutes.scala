package route

import cats.effect.IO
import io.circe.derivation.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Request, Response}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import service.LotteryService
import service.ServiceError.{UnexpectedError, ValidationError}

import java.time.LocalDate

object LotteryRoutes {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def routes(service: LotteryService): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*
    HttpRoutes
      .of[IO] {

        case req @ POST -> Root / "entry" =>
          for {
            _        <- logRequest(req)
            request  <- req.as[EntryRequest]
            result   <- service.submitEntry(request).handleError(e => Left(UnexpectedError("An unexpected error occurred: " + e.getMessage)))
            response <- result match
                          case Left(ValidationError(errorMessage)) => BadRequest(errorMessage)
                          case Left(UnexpectedError(errorMessage)) => InternalServerError(errorMessage)
                          case Right(value)                        => Ok(value.asJson)
            _        <- logResponseStatus(response)
          } yield response

        case req @ GET -> Root / "lottery" =>
          for {
            _        <- logRequest(req)
            response <-
              service
                .getLotteries()
                .flatMap(response => Ok(response.asJson))
                .handleErrorWith(e => InternalServerError("An unexpected error occurred: " + e.getMessage))
            _        <- logResponseStatus(response)
          } yield response

        case req @ POST -> Root / "lottery" / "close" =>
          for {
            _        <- logRequest(req)
            response <-
              service
                .closeLotteries()
                .flatMap(response => Ok(response.asJson))
                .handleErrorWith(e => InternalServerError("An unexpected error occurred: " + e.getMessage))
            _        <- logResponseStatus(response)
          } yield response

        case req @ POST -> Root / "lottery" / "create" =>
          for {
            _        <- logRequest(req)
            request  <- req.as[CreateLotteryRequest]
            response <- service
                          .createLottery(request)
                          .flatMap(response => Ok(response.asJson))
                          .handleErrorWith(e => InternalServerError("An unexpected error occurred: " + e.getMessage))
            _        <- logResponseStatus(response)
          } yield response

        case req @ GET -> Root / "winner" =>
          for {
            _        <- logRequest(req)
            request  <- req.as[WinnersRequest]
            response <- service
                          .getWinner(request)
                          .flatMap(response => Ok(response.asJson))
                          .handleErrorWith(e => InternalServerError("An unexpected error occurred: " + e.getMessage))
            _        <- logResponseStatus(response)
          } yield response
      }
  }

  private def logRequest(req: Request[IO])          = logger.info(s"Received request: ${req.method} ${req.uri}")
  private def logResponseStatus(resp: Response[IO]) = logger.info(s"Response status: ${resp.status}")

  given Configuration = Configuration.default.withSnakeCaseMemberNames

  case class EntryRequest(
    participantId: Int,
    lotteryId: Int,
  ) derives ConfiguredEncoder, ConfiguredDecoder

  case class EntryResponse(
    entryId: Int,
  ) derives ConfiguredEncoder, ConfiguredDecoder

  case class WinnerResponse(lotteryId: Int, entryId: Int) derives ConfiguredEncoder, ConfiguredDecoder

  case class CloseLotteryResponse(winners: List[WinnerResponse]) derives ConfiguredEncoder, ConfiguredDecoder

  case class WinnersRequest(date: LocalDate) derives ConfiguredEncoder, ConfiguredDecoder
  case class WinnersResponse(winners: List[WinnerResponse]) derives ConfiguredEncoder, ConfiguredDecoder

  case class CreateLotteryRequest(name: String) derives ConfiguredEncoder, ConfiguredDecoder
  case class CreateLotteryResponse(lotteryId: Int) derives ConfiguredEncoder, ConfiguredDecoder

  case class LotteryResponse(id: Int, name: String, active: Boolean) derives ConfiguredEncoder, ConfiguredDecoder
  case class LotteriesResponse(lotteries: List[LotteryResponse]) derives ConfiguredEncoder, ConfiguredDecoder
}
