import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object LotteryServiceRoute {
  val service: HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*
    HttpRoutes
      .of[IO] { case req @ POST -> Root / "user" =>
        Ok()
      }
  }
}
