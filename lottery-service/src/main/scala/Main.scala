import cats.effect.{ExitCode, IO, IOApp}
import config.SqlServerConfig
import fs2.Stream
import org.http4s.blaze.server.BlazeServerBuilder
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.*

object Main extends IOApp {
  final override def run(args: List[String]): IO[ExitCode] = for {
    config <- ConfigSource.default.loadF[IO, SqlServerConfig]()
    _      <- Server.serverStream(config).compile.drain
  } yield ExitCode.Success
}
