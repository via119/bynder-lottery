package service

import cats.effect.IO

trait ParticipantService {
  def register(): IO[Either[String, Unit]]
}

object ParticipantService {

  def apply: ParticipantService =
    new ParticipantService {
      import cats.implicits._

      def register(): IO[Either[String, Unit]] = {
        val result = Left("not implemented")

        result.pure[IO]
      }
    }
}
