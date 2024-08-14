package domain

opaque type LotteryId = Int
object LotteryId:
  def apply(value: Int): LotteryId =
    value

  extension (id: LotteryId) def toInt: Int = id
