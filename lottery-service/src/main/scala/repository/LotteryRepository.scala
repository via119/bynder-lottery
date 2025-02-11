package repository

import cats.data.OptionT
import cats.effect.IO
import config.SqlServerConfig
import domain.*
import doobie.Transactor
import doobie.implicits.*
import doobie.implicits.javatimedrivernative.*

import java.time.LocalDate

trait LotteryRepository {
  def isLotteryActive(id: LotteryId): OptionT[IO, Unit]
  def submitEntry(entry: Entry): IO[EntryId]
  def getActiveLotteries(): IO[List[LotteryId]]
  def chooseWinner(lotteryId: LotteryId, date: LocalDate): IO[Option[Winner]]
  def closeLottery(lotteryId: LotteryId): IO[Unit]
  def saveWinner(winner: Winner, winDate: LocalDate): IO[Unit]
  def getWinners(date: LocalDate): IO[List[Winner]]
  def createLottery(lotteryName: LotteryName): IO[LotteryId]
  def getLotteries(): IO[List[Lottery]]
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

      override def isLotteryActive(id: LotteryId): OptionT[IO, Unit] = {
        val query = sql"SELECT count(*) FROM lottery WHERE id = ${id.toInt} AND active = TRUE;"
          .query[Int]
          .unique
          .map(_ > 0)

        OptionT(query.transact(transactor).map(r => Option.when(r)(())))
      }

      override def submitEntry(entry: Entry): IO[EntryId] = {
        val query =
          sql"INSERT INTO entry (participant_id, lottery_id, entry_time) VALUES (${entry.participantId.toInt},${entry.lotteryId.toInt},${entry.entryTime});".update
            .withUniqueGeneratedKeys[Int]("id")
        query.transact(transactor).map(id => EntryId(id))
      }

      override def getActiveLotteries(): IO[List[LotteryId]] = {
        val query =
          sql"SELECT id FROM lottery WHERE active = TRUE;"
            .query[Int]
            .to[List]
        query.transact(transactor).map(ids => ids.map(LotteryId.apply))
      }

      override def chooseWinner(lotteryId: LotteryId, date: LocalDate): IO[Option[Winner]] = {
        val query =
          sql"SELECT lottery_id, id FROM entry WHERE entry_time::date = $date AND lottery_id = ${lotteryId.toInt} ORDER BY random() LIMIT 1;"
            .query[(Int, Int)]
            .option
        query.transact(transactor).map(_.map { case (lotteryId, entryId) => Winner(LotteryId(lotteryId), EntryId(entryId)) })
      }

      override def closeLottery(lotteryId: LotteryId): IO[Unit] = {
        val query = sql"UPDATE lottery SET active = FALSE WHERE id = ${lotteryId.toInt};".update.run
        query.transact(transactor).map(_ => ())
      }

      override def saveWinner(winner: Winner, winDate: LocalDate): IO[Unit] = {
        val query =
          sql"INSERT INTO winner (win_date, entry_id, lottery_id) VALUES ($winDate,${winner.entryId.toInt},${winner.lotteryId.toInt});".update.run
        query.transact(transactor).map(_ => ())
      }

      override def getWinners(date: LocalDate): IO[List[Winner]] = {
        val query =
          sql"SELECT lottery_id, entry_id FROM winner WHERE win_date = $date;"
            .query[(Int, Int)]
            .to[List]
        query.transact(transactor).map(_.map { case (lotteryId, entryId) => Winner(LotteryId(lotteryId), EntryId(entryId)) })
      }

      override def createLottery(lotteryName: LotteryName): IO[LotteryId] = {
        val query =
          sql"INSERT INTO lottery (name, active) VALUES (${lotteryName.toString}, TRUE);".update.withUniqueGeneratedKeys[Int]("id")
        query.transact(transactor).map(id => LotteryId(id))
      }

      override def getLotteries(): IO[List[Lottery]] = {
        val query =
          sql"SELECT id, name, active FROM lottery;"
            .query[(Int, String, Boolean)]
            .to[List]
        query.transact(transactor).map(_.map { case (lotteryId, lotteryName, active) => Lottery(LotteryId(lotteryId), LotteryName(lotteryName), active) })
      }
    }
}
