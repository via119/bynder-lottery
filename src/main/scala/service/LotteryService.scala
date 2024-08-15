package service

import cats.data.OptionT
import cats.effect.IO
import domain.{Entry, LotteryId, ParticipantId, Timestamp, Winner}
import repository.{LotteryRepository, ParticipantRepository}
import route.LotteryRoutes.{CloseResponse, EntryRequest, EntryResponse, WinnerResponse}
import service.ServiceError.ValidationError
import cats.implicits.*
import java.time.LocalDate

trait LotteryService {
  def submitEntry(request: EntryRequest): IO[Either[ValidationError, EntryResponse]]
  def closeLotteries(): IO[CloseResponse]
}

object LotteryService {

  def apply(lotteryRepository: LotteryRepository, participantRepository: ParticipantRepository): LotteryService =
    new LotteryService {
      val invalidEntryErrorMessage = "This entry is not valid, either participant or lottery id does not exist."

      def submitEntry(request: EntryRequest): IO[Either[ValidationError, EntryResponse]] = {
        val participantId = ParticipantId(request.participantId)
        val lotteryId     = LotteryId(request.lotteryId)
        val entry         = Entry(participantId, lotteryId, Timestamp.now())

        (for {
          _ <- participantRepository.participantExists(participantId)
          _ <- participantRepository.lotteryExists(lotteryId)
          r <- OptionT.liftF(lotteryRepository.submitEntry(entry))
        } yield Right(EntryResponse(r.toInt))).getOrElse(Left(ValidationError(invalidEntryErrorMessage)))

      override def closeLotteries(): IO[CloseResponse] = {
        for {
          lotteries <- lotteryRepository.getLotteries()
          today      = LocalDate.now()
          winners   <- lotteries.map(lotteryId => lotteryRepository.chooseWinner(lotteryId, today)).sequence.map(_.flatten)
          _         <- winners.map(winner => lotteryRepository.saveWinner(winner, today)).sequence
        } yield CloseResponse(winners.map(winner => WinnerResponse(winner.lotteryId.toInt, winner.entryId.toInt)))
      }
    }
}
