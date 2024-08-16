package domain

opaque type LotteryName = String
object LotteryName:
  def apply(value: String): LotteryName =
    value

  extension (name: LotteryName) def toString: String = name
