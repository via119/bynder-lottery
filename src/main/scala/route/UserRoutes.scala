package route

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object UserRoutes {
  val routes: HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*
    HttpRoutes
      .of[IO] { case req @ POST -> Root / "user" =>
        Ok()
      }
  }

  case class RegisterParticipantRequest(
    id: Int,
    first_name: String,
    last_name: String,
    email: String,
  )
}
