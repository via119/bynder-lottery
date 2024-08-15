package repository

import cats.data.OptionT
import cats.effect.IO
import config.SqlServerConfig
import domain.{LotteryId, Participant, ParticipantId}
import doobie.Transactor
import doobie.implicits.*

trait ParticipantRepository {
  def register(participant: Participant): IO[ParticipantId]
  def participantExists(id: ParticipantId): OptionT[IO, Unit]
  def lotteryExists(id: LotteryId): OptionT[IO, Unit]
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

      override def register(participant: Participant): IO[ParticipantId] = {
        val query =
          sql"INSERT INTO participant (first_name, last_name, email) VALUES (${participant.first_name}, ${participant.last_name}, ${participant.email});".update
            .withUniqueGeneratedKeys[Int]("id")
        query.transact(transactor).map(id => ParticipantId(id))
      }

      override def participantExists(id: ParticipantId): OptionT[IO, Unit] = {
        val query = sql"SELECT count(*) FROM participant WHERE id = ${id.toInt};"
          .query[Int]
          .unique
          .map(_ > 0)

        OptionT(query.transact(transactor).map { r => Option.when(r)(()) })
      }

      override def lotteryExists(id: LotteryId): OptionT[IO, Unit] = {
        val query = sql"SELECT count(*) FROM lottery WHERE id = ${id.toInt};"
          .query[Int]
          .unique
          .map(_ > 0)

        OptionT(query.transact(transactor).map(r => Option.when(r)(())))
      }
    }
}
