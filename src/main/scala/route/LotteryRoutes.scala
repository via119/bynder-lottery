package route

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object LotteryRoutes {
  val routes: HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*
    HttpRoutes
      .of[IO] { case req @ POST -> Root / "entry" =>
        Ok()
      }
  }

  case class EntryRequest(
    participant_id: Int,
    lottery_id: Int,
  )
}
