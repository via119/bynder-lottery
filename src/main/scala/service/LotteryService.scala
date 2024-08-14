package service

import cats.data.OptionT
import cats.effect.IO
import domain.{Entry, LotteryId, ParticipantId, Timestamp}
import repository.{LotteryRepository, ParticipantRepository}
import route.LotteryRoutes.{EntryRequest, EntryResponse}
import service.ServiceError.ValidationError

trait LotteryService {
  def submitEntry(request: EntryRequest): IO[Either[ValidationError, EntryResponse]]
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
      }
    }
}
