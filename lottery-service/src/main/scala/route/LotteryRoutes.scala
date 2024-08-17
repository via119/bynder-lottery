package route

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

import java.time.LocalDate

object LotteryRoutes {
  def routes(service: LotteryService): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*
    HttpRoutes
      .of[IO] {
        case req @ POST -> Root / "entry"              =>
          for {
            request  <- req.as[EntryRequest]
            result   <- service.submitEntry(request).handleError(e => Left(UnexpectedError("An unexpected error occurred: " + e.getMessage)))
            response <- result match
                          case Left(ValidationError(errorMessage)) => BadRequest(errorMessage)
                          case Left(UnexpectedError(errorMessage)) => InternalServerError(errorMessage)
                          case Right(value)                        => Ok(value.asJson)
          } yield response
        case req @ GET -> Root / "lottery"             =>
          service
            .getLotteries()
            .flatMap(response => Ok(response.asJson))
            .handleErrorWith(e => InternalServerError("An unexpected error occurred: " + e.getMessage))
        case req @ POST -> Root / "lottery" / "close"  =>
          service
            .closeLotteries()
            .flatMap(response => Ok(response.asJson))
            .handleErrorWith(e => InternalServerError("An unexpected error occurred: " + e.getMessage))
        case req @ POST -> Root / "lottery" / "create" =>
          (for {
            request  <- req.as[CreateLotteryRequest]
            result   <- service.createLottery(request)
            response <- Ok(result.asJson)
          } yield response).handleErrorWith(e => InternalServerError("An unexpected error occurred: " + e.getMessage))
        case req @ GET -> Root / "winner"              =>
          (for {
            request  <- req.as[WinnersRequest]
            result   <- service.getWinner(request)
            response <- Ok(result.asJson)
          } yield response).handleErrorWith(e => InternalServerError("An unexpected error occurred: " + e.getMessage))
      }
  }

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
