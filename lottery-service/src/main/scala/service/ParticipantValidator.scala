package service

import cats.data.Validated.*
import cats.data.*
import cats.syntax.all.*
import domain.Participant
import route.ParticipantRoutes.RegisterParticipantRequest

object ParticipantValidator {

  val invalidEmailErrorMessage     = "Invalid email address."
  val invalidFirstNameErrorMessage = "First name cannot contain spaces, numbers or special characters."
  val invalidLastNameErrorMessage  = "Last name cannot contain spaces, numbers or special characters."

  def validateEmail(email: String): ValidatedNel[String, String] = {
    val emailRegex = """^\w+([-+.']\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$"""

    if (email.matches(emailRegex))
      email.validNel
    else
      invalidEmailErrorMessage.invalidNel
  }

  def validateFirstName(firstName: String): ValidatedNel[String, String] =
    if (firstName.matches("^[a-zA-Z]+$"))
      firstName.validNel
    else
      invalidFirstNameErrorMessage.invalidNel

  def validateLastName(lastName: String): ValidatedNel[String, String] =
    if (lastName.matches("^[a-zA-Z]+$"))
      lastName.validNel
    else
      invalidLastNameErrorMessage.invalidNel

  def validateParticipant(registerParticipantRequest: RegisterParticipantRequest): ValidatedNel[String, Participant] = {
    (
      validateFirstName(registerParticipantRequest.firstName),
      validateLastName(registerParticipantRequest.lastName),
      validateEmail(registerParticipantRequest.email),
    )
      .mapN(Participant.apply)
  }

}
