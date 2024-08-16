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
  val entryNumber   = 12
  val entryRequest  = EntryRequest(entryNumber, 1)
  val entryId       = EntryId(entryNumber)
  val lotteryNumber = 1
  val lotteryId     = LotteryId(lotteryNumber)
  val winner        = Winner(lotteryId, entryId)

  class TestLotteryRepository(
    lotteryList: List[LotteryId] = Nil,
    chooseWinnerResult: Option[Winner] = None,
    savedWinners: Option[Ref[IO, List[Winner]]] = None,
    closedLotteries: Option[Ref[IO, List[LotteryId]]] = None,
    lotteryExistsResult: OptionT[IO, Unit] = OptionT.none,
  ) extends LotteryRepository {
    override def submitEntry(entry: Entry): IO[EntryId] = IO.pure(entryId)

    override def getActiveLotteries(): IO[List[LotteryId]] = IO.pure(lotteryList)

    override def chooseWinner(lotteryId: LotteryId, date: LocalDate): IO[Option[Winner]] = IO.pure(chooseWinnerResult)

    override def closeLottery(lotteryId: LotteryId): IO[Unit] = closedLotteries.map(_.getAndUpdate(x => lotteryId :: x)).sequence.as(())

    override def saveWinner(winner: Winner, winDate: LocalDate): IO[Unit] = savedWinners.map(_.getAndUpdate(x => winner :: x)).sequence.as(())

    override def isLotteryActive(id: LotteryId): OptionT[IO, Unit] = lotteryExistsResult
  }

  class TestParticipantRepository(participantExistsResult: OptionT[IO, Unit] = OptionT.none) extends ParticipantRepository {
    override def register(participant: Participant): IO[ParticipantId] = ???

    override def participantExists(id: ParticipantId): OptionT[IO, Unit] = participantExistsResult
  }

  test("submitEntry should return ValidationError if user does not exist") {
    val testLotteryRepository     = TestLotteryRepository(lotteryExistsResult = OptionT.some(()))
    val testParticipantRepository = TestParticipantRepository(participantExistsResult = OptionT.none)
    val lotteryService            = LotteryService(testLotteryRepository, testParticipantRepository)

    for {
      result <- lotteryService.submitEntry(entryRequest)
    } yield expect(result == Left(ValidationError(invalidEntryErrorMessage)))
  }

  test("submitEntry should return ValidationError if lottery does not exist") {
    val testLotteryRepository     = TestLotteryRepository(lotteryExistsResult = OptionT.none)
    val testParticipantRepository = TestParticipantRepository(participantExistsResult = OptionT.some(()))
    val lotteryService            = LotteryService(testLotteryRepository, testParticipantRepository)

    for {
      result <- lotteryService.submitEntry(entryRequest)
    } yield expect(result == Left(ValidationError(invalidEntryErrorMessage)))
  }

  test("closeLotteries should save the chosen winners and close the lotteries") {
    val testParticipantRepository = TestParticipantRepository()
    for {
      winnersRef                <- Ref[IO].of(Nil: List[Winner])
      closedLotteriesRef        <- Ref[IO].of(Nil: List[LotteryId])
      testLotteryRepository      =
        TestLotteryRepository(
          lotteryList = List(lotteryId),
          chooseWinnerResult = Some(winner),
          savedWinners = Some(winnersRef),
          closedLotteries = Some(closedLotteriesRef),
        )
      lotteryService             = LotteryService(testLotteryRepository, testParticipantRepository)
      result                    <- lotteryService.closeLotteries()
      winnersResultList         <- winnersRef.get
      closedLotteriesResultList <- closedLotteriesRef.get
    } yield expect(
      winnersResultList == List(winner) && closedLotteriesResultList == List(lotteryId) && result == CloseResponse(
        List(WinnerResponse(lotteryNumber, entryNumber)),
      ),
    )
  }

  test("closeLotteries should not save winner and close lottery if a winner was not chosen") {
    val testParticipantRepository = TestParticipantRepository()
    for {
      winnersRef                <- Ref[IO].of(Nil: List[Winner])
      closedLotteriesRef        <- Ref[IO].of(Nil: List[LotteryId])
      testLotteryRepository      =
        TestLotteryRepository(
          lotteryList = List(lotteryId),
          chooseWinnerResult = None,
          savedWinners = Some(winnersRef),
        )
      lotteryService             = LotteryService(testLotteryRepository, testParticipantRepository)
      result                    <- lotteryService.closeLotteries()
      winnersResultList         <- winnersRef.get
      closedLotteriesResultList <- closedLotteriesRef.get
    } yield expect(
      winnersResultList.isEmpty && closedLotteriesResultList.isEmpty && result == CloseResponse(Nil),
    )
  }
}
