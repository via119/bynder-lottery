import cats.effect.IO
import fs2.Stream
import org.http4s.blaze.server.BlazeServerBuilder
import route.{LotteryRoutes, ParticipantRoutes}
import cats.implicits.*
import service.ParticipantService

object Server {
  val serverStream: Stream[IO, Nothing] = {
    import org.http4s.implicits.*

    val participantService = ParticipantService.apply
    val routes             =
      (ParticipantRoutes.routes(participantService) <+> LotteryRoutes.routes).orNotFound

    for {
      exitCode <- BlazeServerBuilder[IO]
                    .bindHttp(8080, "0.0.0.0")
                    .withHttpApp(routes)
                    .serve
    } yield exitCode
  }.drain
}
