package service

import cats.data.OptionT
import cats.effect.IO
import cats.implicits.*
import domain.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import repository.{LotteryRepository, ParticipantRepository}
import route.LotteryRoutes.*
import service.ServiceError.ValidationError

import java.time.{LocalDate, LocalDateTime}

trait LotteryService {
  def submitEntry(request: EntryRequest): IO[Either[ValidationError, EntryResponse]]
  def closeLotteries(): IO[CloseLotteryResponse]
  def getWinner(request: WinnersRequest): IO[WinnersResponse]
  def createLottery(request: CreateLotteryRequest): IO[CreateLotteryResponse]
  def getLotteries(): IO[LotteriesResponse]
}

object LotteryService {
  val invalidEntryErrorMessage = "This entry is not valid, either participant or lottery does not exist or lottery is not active."

  def apply(lotteryRepository: LotteryRepository, participantRepository: ParticipantRepository): LotteryService =
    new LotteryService {
      given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

      def submitEntry(request: EntryRequest): IO[Either[ValidationError, EntryResponse]] = {
        val participantId = ParticipantId(request.participantId)
        val lotteryId     = LotteryId(request.lotteryId)
        val entryTime     = LocalDateTime.now()
        val entry         = Entry(participantId, lotteryId, entryTime)

        (for {
          _       <- participantRepository.participantExists(participantId)
          _       <- lotteryRepository.isLotteryActive(lotteryId)
          _       <- OptionT.liftF(logger.info(s"Submitting entry for participant id $participantId for lottery id $lotteryId"))
          entryId <- OptionT.liftF(lotteryRepository.submitEntry(entry))
        } yield Right(EntryResponse(entryId.toInt))).getOrElse(Left(ValidationError(invalidEntryErrorMessage)))
      }

      override def closeLotteries(): IO[CloseLotteryResponse] = {
        for {
          lotteries <- lotteryRepository.getActiveLotteries()
          _         <- logger.info(s"Closing active lotteries ${lotteries.mkString(",")}")
          today      = LocalDate.now()
          winners   <- lotteries.map(lotteryId => lotteryRepository.chooseWinner(lotteryId, today)).sequence.map(_.flatten)
          _         <- logger.info(s"Winners chosen ${winners.mkString(",")}")
          _         <- winners.map(winner => lotteryRepository.saveWinner(winner, today)).sequence
          _         <- lotteries.map(lottery => lotteryRepository.closeLottery(lottery)).sequence
        } yield CloseLotteryResponse(winners.map(winner => WinnerResponse(winner.lotteryId.toInt, winner.entryId.toInt)))
      }

      override def getWinner(request: WinnersRequest): IO[WinnersResponse] = {
        for {
          winnersList <- lotteryRepository.getWinners(request.date)
          _           <- logger.info(s"Returning winners ${winnersList.mkString(",")}")
        } yield WinnersResponse(winnersList.map(winner => WinnerResponse(winner.lotteryId.toInt, winner.entryId.toInt)))
      }

      override def createLottery(request: CreateLotteryRequest): IO[CreateLotteryResponse] = {
        for {
          lotteryId <- lotteryRepository.createLottery(LotteryName(request.name))
          _         <- logger.info(s"New lottery created with id $lotteryId")
        } yield CreateLotteryResponse(lotteryId.toInt)
      }

      override def getLotteries(): IO[LotteriesResponse] = {
        for {
          lotteries <- lotteryRepository.getLotteries()
          responses  = lotteries.map(l => LotteryResponse(id = l.id.toInt, name = l.name.toString, active = l.active))
          _         <- logger.info(s"Returning lotteries ${responses.mkString(",")}")
        } yield LotteriesResponse(responses)
      }
    }
}
