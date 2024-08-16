package service

import cats.data.OptionT
import cats.effect.IO
import cats.effect.kernel.Ref
import cats.implicits.*
import domain.*
import repository.{LotteryRepository, ParticipantRepository}
import route.LotteryRoutes.{CloseLotteryResponse, EntryRequest, WinnerResponse}
import service.LotteryService.invalidEntryErrorMessage
import service.ServiceError.ValidationError
import weaver.SimpleIOSuite

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

    override def getWinners(date: LocalDate): IO[List[Winner]] = IO.pure(Nil)

    override def createLottery(lotteryName: LotteryName): IO[LotteryId] = IO.pure(lotteryId)

    override def getLotteries(): IO[List[Lottery]] = IO.pure(Nil)
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
      winnersResultList == List(winner) && closedLotteriesResultList == List(lotteryId) && result == CloseLotteryResponse(
        List(WinnerResponse(lotteryNumber, entryNumber)),
      ),
    )
  }

  test("closeLotteries should not save winner if a winner was not chosen") {
    val testParticipantRepository = TestParticipantRepository()
    for {
      winnersRef           <- Ref[IO].of(Nil: List[Winner])
      testLotteryRepository =
        TestLotteryRepository(
          lotteryList = List(lotteryId),
          chooseWinnerResult = None,
          savedWinners = Some(winnersRef),
        )
      lotteryService        = LotteryService(testLotteryRepository, testParticipantRepository)
      result               <- lotteryService.closeLotteries()
      winnersResultList    <- winnersRef.get
    } yield expect(
      winnersResultList.isEmpty && result == CloseLotteryResponse(Nil),
    )
  }

  test("closeLotteries should close lottery even if no winners were chosen") {
    val testParticipantRepository = TestParticipantRepository()
    for {
      closedLotteriesRef        <- Ref[IO].of(Nil: List[LotteryId])
      testLotteryRepository      =
        TestLotteryRepository(
          lotteryList = List(lotteryId),
          chooseWinnerResult = None,
          closedLotteries = Some(closedLotteriesRef),
        )
      lotteryService             = LotteryService(testLotteryRepository, testParticipantRepository)
      result                    <- lotteryService.closeLotteries()
      closedLotteriesResultList <- closedLotteriesRef.get
    } yield expect(
      closedLotteriesResultList == List(lotteryId) && result == CloseLotteryResponse(Nil),
    )
  }
}
