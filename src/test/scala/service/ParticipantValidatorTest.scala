package service

import cats.data.{Validated, ValidatedNel}
import domain.Participant
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.*
import route.ParticipantRoutes.RegisterParticipantRequest
import service.ParticipantValidator.*

class ParticipantValidatorTest extends AnyFunSuite with Matchers {

  test("should validate emails") {
    assert(validateEmail("a@b.com").isValid)
    assert(validateEmail("a@b.nl").isValid)
    assert(validateEmail("al.ma@b.nl").isValid)
    assert(validateEmail("@b.nl").isInvalid)
    assert(validateEmail("a@.nl").isInvalid)
    assert(validateEmail(".nl").isInvalid)
    assert(validateEmail("a@b").isInvalid)
    assert(validateEmail("a@b.").isInvalid)
    assert(validateEmail("!@b.com").isInvalid)
  }

  test("should validate first names") {
    assert(validateFirstName("alma").isValid)
    assert(validateFirstName("ALMA").isValid)
    assert(validateFirstName("al!MA").isInvalid)
  }

  test("should validate last names") {
    assert(validateLastName("alma").isValid)
    assert(validateLastName("ALMA").isValid)
    assert(validateLastName("al!MA").isInvalid)
  }

  test("should validate participants") {
    val validParticipantRequest = RegisterParticipantRequest(firstName = "Joe", lastName = "Smith", email = "joey.smith@gmail.com")
    assert(validateParticipant(validParticipantRequest).isValid)

    val inValidParticipantRequest                 = RegisterParticipantRequest(firstName = "Joe!", lastName = "Smith", email = "joey")
    val result: ValidatedNel[String, Participant] = validateParticipant(inValidParticipantRequest)
    result match
      case Validated.Valid(a)   => fail("Participant should not be valid.")
      case Validated.Invalid(e) =>
        e.toList should contain theSameElementsAs List(invalidFirstNameErrorMessage, invalidEmailErrorMessage)
  }
}
