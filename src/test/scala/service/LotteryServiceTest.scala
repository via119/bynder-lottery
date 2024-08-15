package service

import cats.data.OptionT
import cats.effect.IO
import cats.effect.kernel.Ref
import repository.{LotteryRepository, ParticipantRepository}
import service.ServiceError.ValidationError
import weaver.SimpleIOSuite
import cats.implicits.*
import domain.{Entry, EntryId, LotteryId, Participant, ParticipantId, Winner}
import route.LotteryRoutes.{CloseResponse, EntryRequest, WinnerResponse}
import service.LotteryService.invalidEntryErrorMessage

import java.time.LocalDate

object LotteryServiceTest extends SimpleIOSuite {
  val entryNumber  = 12
  val entryRequest = EntryRequest(entryNumber, 1)
  val entryId      = EntryId(entryNumber)

  class TestLotteryRepository(
    lotteryList: List[LotteryId] = Nil,
    chooseWinnerResult: Option[Winner] = None,
    savedWinners: Option[Ref[IO, List[Winner]]] = None,
  ) extends LotteryRepository {
    override def submitEntry(entry: Entry): IO[EntryId] = IO.pure(entryId)

    override def getLotteries(): IO[List[LotteryId]] = IO.pure(lotteryList)

    override def chooseWinner(lotteryId: LotteryId, date: LocalDate): IO[Option[Winner]] = IO.pure(chooseWinnerResult)

    override def saveWinner(winner: Winner, winDate: LocalDate): IO[Unit] = savedWinners.map(_.getAndUpdate(x => winner :: x)).sequence.as(())
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

  test("closeLotteries should save the chosen winners") {
    val lottery1Number            = 1
    val lotteryId1                = LotteryId(lottery1Number)
    val winner                    = Winner(lotteryId1, entryId)
    val testParticipantRepository = TestParticipantRepository()
    for {
      winnersRef           <- Ref[IO].of(Nil: List[Winner])
      testLotteryRepository =
        TestLotteryRepository(
          lotteryList = List(lotteryId1),
          chooseWinnerResult = Some(winner),
          savedWinners = Some(winnersRef),
        )
      lotteryService        = LotteryService(testLotteryRepository, testParticipantRepository)
      result               <- lotteryService.closeLotteries()
      winnersResultList    <- winnersRef.get
    } yield expect(
      winnersResultList == List(winner) && result == CloseResponse(
        List(WinnerResponse(lottery1Number, entryNumber)),
      ),
    )
  }
}
