import cats.effect.IO
import fs2.Stream
import org.http4s.blaze.server.BlazeServerBuilder
import route.{LotteryRoutes, UserRoutes}
import cats.implicits.*

object Server {
  val serverStream: Stream[IO, Nothing] = {
    import org.http4s.implicits.*
    val routes =
      (UserRoutes.routes <+> LotteryRoutes.routes).orNotFound

    for {
      exitCode <- BlazeServerBuilder[IO]
                    .bindHttp(8080, "0.0.0.0")
                    .withHttpApp(routes)
                    .serve
    } yield exitCode
  }.drain
}
