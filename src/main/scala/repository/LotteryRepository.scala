package repository

import cats.effect.IO
import config.SqlServerConfig
import domain.{Entry, EntryId, LotteryId, Winner}
import doobie.Transactor
import doobie.implicits.*
import doobie.implicits.javatimedrivernative.*

import java.time.LocalDate

trait LotteryRepository {
  def submitEntry(entry: Entry): IO[EntryId]
  def getLotteries(): IO[List[LotteryId]]
  def chooseWinner(lotteryId: LotteryId, date: LocalDate): IO[Option[Winner]]
  def saveWinner(winner: Winner, winDate: LocalDate): IO[Unit]
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

      override def getLotteries(): IO[List[LotteryId]] = {
        val query =
          sql"SELECT id FROM lottery;"
            .query[Int]
            .to[List]
        query.transact(transactor).map(ids => ids.map(LotteryId.apply))
      }

      override def chooseWinner(lotteryId: LotteryId, date: LocalDate): IO[Option[Winner]] = {
        val query =
          sql"SELECT lottery_id, id FROM entry WHERE entry_time::date = $date ORDER BY random() LIMIT 1;"
            .query[(Int, Int)]
            .option
        query.transact(transactor).map(_.map { case (lotteryId, entryId) => Winner(LotteryId(lotteryId), EntryId(entryId)) })
      }

      override def saveWinner(winner: Winner, winDate: LocalDate): IO[Unit] = {
        val query =
          sql"INSERT INTO winner (win_date, entry_id, lottery_id) VALUES ($winDate,${winner.entryId.toInt},${winner.lotteryId.toInt});".update.run
        query.transact(transactor).map(id => EntryId(id))
      }
    }
}
