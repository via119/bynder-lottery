package domain

opaque type Email = String
object Email:
  def apply(value: String): Email =
    value
