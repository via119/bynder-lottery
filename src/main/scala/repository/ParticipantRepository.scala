package repository

import cats.effect.IO
import domain.Participant

trait ParticipantRepository {
  def register(participant: Participant): IO[Int]
}

object ParticipantRepository {

  def apply: ParticipantRepository =
    new ParticipantRepository {
      def register(participant: Participant): IO[Int] = {
        IO.pure(1)
      }
    }
}
