import cats.effect.{ExitCode, IO, IOApp}
import config.SqlServerConfig
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.*

object Main extends IOApp {
  final override def run(args: List[String]): IO[ExitCode] = for {
    config <- ConfigSource.default.loadF[IO, SqlServerConfig]()
    _      <- Server.serverStream.compile.drain
  } yield ExitCode.Success
}
