package service

import cats.data.OptionT
import cats.effect.IO
import cats.implicits.*
import domain.{Entry, LotteryId, ParticipantId, Timestamp}
import repository.{LotteryRepository, ParticipantRepository}
import route.LotteryRoutes.*
import service.ServiceError.ValidationError

import java.time.LocalDate

trait LotteryService {
  def submitEntry(request: EntryRequest): IO[Either[ValidationError, EntryResponse]]
  def closeLotteries(): IO[CloseResponse]
  def getWinner(request: WinnersRequest): IO[WinnersResponse]
}

object LotteryService {
  val invalidEntryErrorMessage = "This entry is not valid, either participant or lottery does not exist or lottery is not active."

  def apply(lotteryRepository: LotteryRepository, participantRepository: ParticipantRepository): LotteryService =
    new LotteryService {

      def submitEntry(request: EntryRequest): IO[Either[ValidationError, EntryResponse]] = {
        val participantId = ParticipantId(request.participantId)
        val lotteryId     = LotteryId(request.lotteryId)
        val entryTime     = Timestamp.now()
        val entry         = Entry(participantId, lotteryId, entryTime)

        (for {
          _       <- participantRepository.participantExists(participantId)
          _       <- lotteryRepository.isLotteryActive(lotteryId)
          entryId <- OptionT.liftF(lotteryRepository.submitEntry(entry))
        } yield Right(EntryResponse(entryId.toInt))).getOrElse(Left(ValidationError(invalidEntryErrorMessage)))
      }

      override def closeLotteries(): IO[CloseResponse] = {
        for {
          lotteries <- lotteryRepository.getActiveLotteries()
          today      = LocalDate.now()
          winners   <- lotteries.map(lotteryId => lotteryRepository.chooseWinner(lotteryId, today)).sequence.map(_.flatten)
          _         <- winners.map(winner => lotteryRepository.saveWinner(winner, today)).sequence
          _         <- lotteries.map(lottery => lotteryRepository.closeLottery(lottery)).sequence
        } yield CloseResponse(winners.map(winner => WinnerResponse(winner.lotteryId.toInt, winner.entryId.toInt)))
      }

      override def getWinner(request: WinnersRequest): IO[WinnersResponse] = {
        for {
          winnersList <- lotteryRepository.getWinners(request.date)
        } yield WinnersResponse(winnersList.map(winner => WinnerResponse(winner.lotteryId.toInt, winner.entryId.toInt)))

      }
    }
}
