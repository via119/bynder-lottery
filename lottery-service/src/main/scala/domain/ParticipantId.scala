package domain

opaque type ParticipantId = Int
object ParticipantId:
  def apply(value: Int): ParticipantId =
    value

  extension (id: ParticipantId) def toInt: Int = id
