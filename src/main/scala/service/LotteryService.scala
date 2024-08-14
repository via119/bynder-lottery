package service

import cats.data.NonEmptyList
import cats.effect.IO
import domain.{Entry, LotteryId, Participant, ParticipantId, Timestamp}
import repository.{LotteryRepository, ParticipantRepository}
import route.LotteryRoutes.{EntryRequest, EntryResponse}
import service.ServiceError.ValidationError

trait LotteryService {
  def submitEntry(request: EntryRequest): IO[Either[ValidationError, EntryResponse]]
}

object LotteryService {

  def apply(lotteryRepository: LotteryRepository, participantRepository: ParticipantRepository): LotteryService =
    new LotteryService {

      def submitEntry(request: EntryRequest): IO[Either[ValidationError, EntryResponse]] = {

        val entry = Entry(ParticipantId(request.participantId), LotteryId(request.lotteryId), Timestamp.now())
        lotteryRepository.submitEntry(entry).map(id => Right(EntryResponse(id.toInt)))
      }
    }
}
