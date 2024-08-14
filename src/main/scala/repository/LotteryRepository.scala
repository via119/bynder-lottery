package repository

import cats.effect.IO
import config.SqlServerConfig
import domain.{Entry, EntryId}
import doobie.Transactor
import doobie.implicits.*
import doobie.implicits.javatimedrivernative.*

trait LotteryRepository {
  def submitEntry(entry: Entry): IO[EntryId]
}

object LotteryRepository {

  def apply(config: SqlServerConfig): LotteryRepository =
    new LotteryRepository {
      private val transactor: Transactor[IO] =
        Transactor.fromDriverManager[IO](
          "org.postgresql.Driver",
          s"jdbc:postgresql://${config.hostname}:${config.port}/${config.database}",
          config.username,
          config.password,
        )

      def submitEntry(entry: Entry): IO[EntryId] = {
        val query =
          sql"INSERT INTO entry (participant_id, lottery_id, entry_time) VALUES (${entry.participantId.toInt},${entry.lotteryId.toInt},${entry.entryTime.toLocalDateTime});".update
            .withUniqueGeneratedKeys[Int]("id")
        query.transact(transactor).map(id => EntryId(id))
      }
    }
}
