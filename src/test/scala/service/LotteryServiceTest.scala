package service

import cats.data.OptionT
import cats.effect.IO
import repository.{LotteryRepository, ParticipantRepository}
import service.ServiceError.ValidationError
import weaver.SimpleIOSuite
import cats.implicits.*
import domain.{Entry, EntryId, LotteryId, Participant, ParticipantId, Winner}
import route.LotteryRoutes.EntryRequest
import service.LotteryService.invalidEntryErrorMessage

import java.time.LocalDate

object LotteryServiceTest extends SimpleIOSuite {
  val entry        = 12
  val entryRequest = EntryRequest(entry, 1)

  class TestLotteryRepository(submitEntryResult: IO[EntryId] = IO.pure(EntryId(entry))) extends LotteryRepository {
    override def submitEntry(entry: Entry): IO[EntryId] = submitEntryResult

    override def getLotteries(): IO[List[LotteryId]] = ???

    override def chooseWinner(lotteryId: LotteryId, date: LocalDate): IO[Option[Winner]] = ???

    override def saveWinner(winner: Winner, winDate: LocalDate): IO[Unit] = ???
  }

  class TestParticipantRepository(participantExistsResult: OptionT[IO, Unit] = OptionT.none, lotteryExistsResult: OptionT[IO, Unit] = OptionT.none)
      extends ParticipantRepository {
    override def register(participant: Participant): IO[ParticipantId] = ???

    override def participantExists(id: ParticipantId): OptionT[IO, Unit] = participantExistsResult

    override def lotteryExists(id: LotteryId): OptionT[IO, Unit] = lotteryExistsResult
  }

  test("submitEntry should return ValidationError if user does not exist") {
    val testLotteryRepository     = TestLotteryRepository()
    val testParticipantRepository = TestParticipantRepository(participantExistsResult = OptionT.none, lotteryExistsResult = OptionT.some(()))
    val lotteryService            = LotteryService(testLotteryRepository, testParticipantRepository)

    for {
      result <- lotteryService.submitEntry(entryRequest)
    } yield expect(result == Left(ValidationError(invalidEntryErrorMessage)))
  }

  test("submitEntry should return ValidationError if lottery does not exist") {
    val testLotteryRepository     = TestLotteryRepository()
    val testParticipantRepository = TestParticipantRepository(participantExistsResult = OptionT.some(()), lotteryExistsResult = OptionT.none)
    val lotteryService            = LotteryService(testLotteryRepository, testParticipantRepository)

    for {
      result <- lotteryService.submitEntry(entryRequest)
    } yield expect(result == Left(ValidationError(invalidEntryErrorMessage)))
  }
}
