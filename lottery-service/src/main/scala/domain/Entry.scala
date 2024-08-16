package domain

import java.time.LocalDateTime

case class Entry(participantId: ParticipantId, lotteryId: LotteryId, entryTime: LocalDateTime)
