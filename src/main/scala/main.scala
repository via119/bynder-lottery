import cats.effect.{ExitCode, IO, IOApp}
import config.SqlServerConfig
import fs2.Stream
import org.http4s.blaze.server.BlazeServerBuilder
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.*

object Main extends IOApp {
  final override def run(args: List[String]): IO[ExitCode] = for {
    config <- ConfigSource.default.loadF[IO, SqlServerConfig]()
    _       = println("hi")
    _      <- serviceStream.compile.drain
  } yield ExitCode.Success

  private val serviceStream: Stream[IO, Nothing] = {
    import org.http4s.implicits.*
    val httpApp = LotteryServiceRoute.service.orNotFound
    for {
      exitCode <- BlazeServerBuilder[IO]
                    .bindHttp(8080, "0.0.0.0")
                    .withHttpApp(httpApp)
                    .serve
    } yield exitCode
  }.drain
}
