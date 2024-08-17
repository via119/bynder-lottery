package service

import cats.data.NonEmptyList
import cats.effect.IO
import domain.Participant
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import repository.ParticipantRepository
import route.ParticipantRoutes.{RegisterParticipantRequest, RegisterParticipantResponse}
import service.ServiceError.ValidationError

trait ParticipantService {
  def register(request: RegisterParticipantRequest): IO[Either[ValidationError, RegisterParticipantResponse]]
}

object ParticipantService {

  def apply(participantRepository: ParticipantRepository): ParticipantService =
    new ParticipantService {
      given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

      override def register(request: RegisterParticipantRequest): IO[Either[ValidationError, RegisterParticipantResponse]] = {
        for {
          _               <- logger.info("Registering new user")
          validationResult = ParticipantValidator.validateParticipant(request).toEither
          result          <- validationResult match
                               case Left(errors)       =>
                                 IO.pure(
                                   Left[ValidationError, RegisterParticipantResponse](
                                     ValidationError(
                                       s"The following validation(s) failed: ${errors.toList.mkString(",")}.",
                                     ),
                                   ),
                                 )
                               case Right(participant) =>
                                 participantRepository
                                   .register(participant)
                                   .map(id => Right[ValidationError, RegisterParticipantResponse](RegisterParticipantResponse(id.toInt)))
          _               <- result match
                               case Left(_)         => logger.info("Failed to register user")
                               case Right(response) => logger.info(s"New user registered with id ${response.id}")
        } yield result
      }
    }
}
