package domain

opaque type EntryId = Int
object EntryId:
  def apply(value: Int): EntryId =
    value

  extension (id: EntryId) def toInt: Int = id
