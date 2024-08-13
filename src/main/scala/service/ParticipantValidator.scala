package service

import domain.{Email, Participant}
import route.ParticipantRoutes.RegisterParticipantRequest
import cats.data.Validated._
import cats.data._
import cats.data.Validated._
import cats.syntax.all._

object ParticipantValidator {

  def validateEmail(email: String): ValidatedNel[String, String] = {
    val emailRegex = """^\w+([-+.']\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$"""

    if (email.matches(emailRegex))
      email.validNel
    else
      "Invalid email address.".invalidNel
  }

  def validateFirstName(firstName: String): ValidatedNel[String, String] =
    if (firstName.matches("^[a-zA-Z]+$"))
      firstName.validNel
    else
      "First name cannot contain spaces, numbers or special characters.".invalidNel

  def validateLastName(lastName: String): ValidatedNel[String, String] =
    if (lastName.matches("^[a-zA-Z]+$"))
      lastName.validNel
    else
      "Last name cannot contain spaces, numbers or special characters.".invalidNel

  def validateParticipant(registerParticipantRequest: RegisterParticipantRequest): ValidatedNel[String, Participant] = {
    (
      validateFirstName(registerParticipantRequest.firstName),
      validateLastName(registerParticipantRequest.lastName),
      validateEmail(registerParticipantRequest.email),
    )
      .mapN(Participant.apply)
  }

}
