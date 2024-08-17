import cats.effect.*
import cats.effect.unsafe.implicits.global
import config.SqlServerConfig
import org.http4s.{Request, *}
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.implicits.*
import repository.{LotteryRepository, ParticipantRepository}
import route.LotteryRoutes
import route.LotteryRoutes.*
import service.LotteryService
import weaver.*

import java.time.LocalDate

object LotteryServiceTest extends SimpleIOSuite {
  val lotteryName             = "new-lottery"
  val lottery2Name            = "new-lottery2"
  val lotteryId               = 1
  val lottery2Id              = 2
  val entryId                 = 1
  val entryId2                = 2
  val participantId           = 1
  val createLotteryRequest    = CreateLotteryRequest(lotteryName)
  val createLottery2Request   = CreateLotteryRequest(lottery2Name)
  val entryForLottery1Request = EntryRequest(participantId, lotteryId)
  val entryForLottery2Request = EntryRequest(participantId, lottery2Id)
  val winnersRequest          = WinnersRequest(LocalDate.now())

  def checkResponse[A](actual: Response[IO], expectedStatus: Status, expectedBody: Option[A])(using EntityDecoder[IO, A]): Boolean = {
    val statusCheck = actual.status == expectedStatus
    val bodyCheck   = expectedBody.fold[Boolean](
      actual.body.compile.toVector.unsafeRunSync().isEmpty,
    )(expected => actual.as[A].unsafeRunSync() == expected)
    statusCheck && bodyCheck
  }

  test("the only entry wins the lottery") {
    PostgresContainer.get().use { postgres =>
      for {
        config                <- IO(SqlServerConfig(postgres.host, postgres.firstMappedPort, postgres.databaseName, postgres.username, postgres.password))
        lotteryRepository      = LotteryRepository(config)
        participantRepository  = ParticipantRepository(config)
        service                = LotteryService(lotteryRepository, participantRepository)
        createLotteryResponse <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/lottery/create").withEntity(createLotteryRequest))
        entryResponse         <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/entry").withEntity(entryForLottery1Request))
        closeResponse         <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/lottery/close"))
        winnerResponse        <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.GET, uri"/winner").withEntity(winnersRequest))
      } yield {
        assert(
          checkResponse[CreateLotteryResponse](createLotteryResponse, Status.Ok, Some(CreateLotteryResponse(lotteryId))) &&
            checkResponse[EntryResponse](entryResponse, Status.Ok, Some(EntryResponse(entryId))) &&
            checkResponse[CloseLotteryResponse](closeResponse, Status.Ok, Some(CloseLotteryResponse(List(WinnerResponse(lotteryId, entryId))))) &&
            checkResponse[WinnersResponse](winnerResponse, Status.Ok, Some(WinnersResponse(List(WinnerResponse(lotteryId, entryId))))),
        )
      }
    }
  }

  test("there are no winners if there were no entries") {
    PostgresContainer.get().use { postgres =>
      for {
        config                 <- IO(SqlServerConfig(postgres.host, postgres.firstMappedPort, postgres.databaseName, postgres.username, postgres.password))
        lotteryRepository       = LotteryRepository(config)
        participantRepository   = ParticipantRepository(config)
        service                 = LotteryService(lotteryRepository, participantRepository)
        createLotteryResponse  <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/lottery/create").withEntity(createLotteryRequest))
        createLottery2Response <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/lottery/create").withEntity(createLotteryRequest))
        closeResponse          <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/lottery/close"))
        winnerResponse         <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.GET, uri"/winner").withEntity(winnersRequest))
      } yield {
        assert(
          checkResponse[CreateLotteryResponse](createLotteryResponse, Status.Ok, Some(CreateLotteryResponse(lotteryId))) &&
            checkResponse[CreateLotteryResponse](createLottery2Response, Status.Ok, Some(CreateLotteryResponse(lottery2Id))) &&
            checkResponse[CloseLotteryResponse](closeResponse, Status.Ok, Some(CloseLotteryResponse(Nil))) &&
            checkResponse[WinnersResponse](winnerResponse, Status.Ok, Some(WinnersResponse(Nil))),
        )
      }
    }
  }

  test("one winner is chosen from multiple entries") {
    PostgresContainer.get().use { postgres =>
      for {
        config                <- IO(SqlServerConfig(postgres.host, postgres.firstMappedPort, postgres.databaseName, postgres.username, postgres.password))
        lotteryRepository      = LotteryRepository(config)
        participantRepository  = ParticipantRepository(config)
        service                = LotteryService(lotteryRepository, participantRepository)
        createLotteryResponse <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/lottery/create").withEntity(createLotteryRequest))
        entryResponse         <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/entry").withEntity(entryForLottery1Request))
        entry2Response        <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/entry").withEntity(entryForLottery1Request))
        closeResponse         <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/lottery/close"))
        winnersResponse       <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.GET, uri"/winner").withEntity(winnersRequest))
      } yield {
        assert(
          checkResponse[CreateLotteryResponse](createLotteryResponse, Status.Ok, Some(CreateLotteryResponse(lotteryId))) &&
            checkResponse[EntryResponse](entryResponse, Status.Ok, Some(EntryResponse(entryId))) &&
            checkResponse[EntryResponse](entry2Response, Status.Ok, Some(EntryResponse(entryId2))) &&
            closeResponse.status == Status.Ok && closeResponse.as[CloseLotteryResponse].unsafeRunSync().winners.length == 1 &&
            winnersResponse.status == Status.Ok && winnersResponse.as[WinnersResponse].unsafeRunSync().winners.length == 1,
        )
      }
    }
  }

  test("it is possible to get the lottery list") {
    PostgresContainer.get().use { postgres =>
      for {
        config                 <- IO(SqlServerConfig(postgres.host, postgres.firstMappedPort, postgres.databaseName, postgres.username, postgres.password))
        lotteryRepository       = LotteryRepository(config)
        participantRepository   = ParticipantRepository(config)
        service                 = LotteryService(lotteryRepository, participantRepository)
        createLotteryResponse  <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/lottery/create").withEntity(createLotteryRequest))
        lotteriesResponse      <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.GET, uri"/lottery").withEntity(entryForLottery1Request))
        closeResponse          <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/lottery/close"))
        createLottery2Response <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/lottery/create").withEntity(createLottery2Request))
        lotteries2Response     <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.GET, uri"/lottery").withEntity(entryForLottery1Request))
      } yield {
        assert(
          checkResponse[CreateLotteryResponse](createLotteryResponse, Status.Ok, Some(CreateLotteryResponse(lotteryId))) &&
            checkResponse[LotteriesResponse](lotteriesResponse, Status.Ok, Some(LotteriesResponse(List(LotteryResponse(lotteryId, lotteryName, true))))) &&
            checkResponse[CloseLotteryResponse](closeResponse, Status.Ok, Some(CloseLotteryResponse(Nil))) &&
            checkResponse[CreateLotteryResponse](createLottery2Response, Status.Ok, Some(CreateLotteryResponse(lottery2Id))) &&
            checkResponse[LotteriesResponse](
              lotteries2Response,
              Status.Ok,
              Some(LotteriesResponse(List(LotteryResponse(lotteryId, lotteryName, false), LotteryResponse(lottery2Id, lottery2Name, true)))),
            ),
        )
      }
    }
  }

  test("one winner is chosen from multiple entries for each lottery") {
    PostgresContainer.get().use { postgres =>
      for {
        config               <- IO(SqlServerConfig(postgres.host, postgres.firstMappedPort, postgres.databaseName, postgres.username, postgres.password))
        lotteryRepository     = LotteryRepository(config)
        participantRepository = ParticipantRepository(config)
        service               = LotteryService(lotteryRepository, participantRepository)
        _                    <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/lottery/create").withEntity(createLotteryRequest))
        _                    <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/lottery/create").withEntity(createLotteryRequest))
        _                    <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/entry").withEntity(entryForLottery1Request))
        _                    <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/entry").withEntity(entryForLottery1Request))
        _                    <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/entry").withEntity(entryForLottery1Request))
        _                    <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/entry").withEntity(entryForLottery2Request))
        _                    <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/entry").withEntity(entryForLottery2Request))
        _                    <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/entry").withEntity(entryForLottery2Request))
        closeResponse        <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.POST, uri"/lottery/close"))
        winnersResponse      <- LotteryRoutes.routes(service).orNotFound.run(Request(Method.GET, uri"/winner").withEntity(winnersRequest))
      } yield {
        assert(
          closeResponse.status == Status.Ok && closeResponse.as[CloseLotteryResponse].unsafeRunSync().winners.length == 2 &&
            winnersResponse.status == Status.Ok && winnersResponse.as[WinnersResponse].unsafeRunSync().winners.length == 2,
        )
      }
    }
  }
}
