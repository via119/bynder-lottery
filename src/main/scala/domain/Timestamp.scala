package domain

import java.time.LocalDateTime

opaque type Timestamp = LocalDateTime
object Timestamp:
  def apply(value: LocalDateTime): Timestamp                   =
    value
  def now(): Timestamp                                         =
    LocalDateTime.now()
  extension (ts: Timestamp) def toLocalDateTime: LocalDateTime = ts
