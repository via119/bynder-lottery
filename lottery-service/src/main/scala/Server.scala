import cats.effect.IO
import cats.implicits.*
import config.SqlServerConfig
import fs2.Stream
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits.*
import repository.{LotteryRepository, ParticipantRepository}
import route.{LotteryRoutes, ParticipantRoutes}
import service.{LotteryService, ParticipantService}

object Server {
  def serverStream(config: SqlServerConfig): Stream[IO, Nothing] = {
    val participantRepository = ParticipantRepository(config)
    val lotteryRepository     = LotteryRepository(config)
    val participantService    = ParticipantService(participantRepository)
    val lotteryService        = LotteryService(lotteryRepository, participantRepository)
    val routes                =
      (ParticipantRoutes.routes(participantService) <+> LotteryRoutes.routes(lotteryService)).orNotFound

    for {
      exitCode <- BlazeServerBuilder[IO]
                    .bindHttp(8080, "0.0.0.0")
                    .withHttpApp(routes)
                    .serve
    } yield exitCode
  }.drain
}
