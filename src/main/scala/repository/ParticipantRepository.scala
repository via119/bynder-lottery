package repository

import cats.effect.IO
import config.SqlServerConfig
import domain.{Participant, ParticipantId}
import doobie.Transactor
import doobie.implicits.*

trait ParticipantRepository {
  def register(participant: Participant): IO[ParticipantId]
}

object ParticipantRepository {

  def apply(config: SqlServerConfig): ParticipantRepository =
    new ParticipantRepository {
      private val transactor: Transactor[IO] =
        Transactor.fromDriverManager[IO](
          "org.postgresql.Driver",
          s"jdbc:postgresql://${config.hostname}:${config.port}/${config.database}",
          config.username,
          config.password,
        )

      def register(participant: Participant): IO[ParticipantId] = {
        val query =
          sql"INSERT INTO participant (first_name, last_name, email) VALUES (${participant.first_name}, ${participant.last_name}, ${participant.email});".update
            .withUniqueGeneratedKeys[Int]("id")
        query.transact(transactor).map(id => ParticipantId(id))
      }
    }
}
