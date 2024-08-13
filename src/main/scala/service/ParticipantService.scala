package service

import cats.data.NonEmptyList
import cats.effect.IO
import domain.Participant
import repository.ParticipantRepository
import route.ParticipantRoutes.{RegisterParticipantRequest, RegisterParticipantResponse}
import service.ServiceError.ValidationError

trait ParticipantService {
  def register(request: RegisterParticipantRequest): IO[Either[ValidationError, RegisterParticipantResponse]]
}

object ParticipantService {

  def apply(participantRepository: ParticipantRepository): ParticipantService =
    new ParticipantService {

      def register(request: RegisterParticipantRequest): IO[Either[ValidationError, RegisterParticipantResponse]] = {
        val validationResult: Either[NonEmptyList[String], Participant] = ParticipantValidator.validateParticipant(request).toEither
        validationResult match
          case Left(errors)       => IO.pure(Left(ValidationError(s"The following validation(s) failed: ${errors.toList.mkString(",")}.")))
          case Right(participant) => participantRepository.register(participant).map(id => Right(RegisterParticipantResponse(id.toInt)))
      }
    }
}
