package route

import cats.effect.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.implicits.*
import route.ParticipantRoutes.*
import service.ServiceError.*
import service.{ParticipantService, ServiceError}
import weaver.*

object ParticipantRoutesTest extends SimpleIOSuite {

  class TestService(registerResult: IO[Either[ValidationError, RegisterParticipantResponse]]) extends ParticipantService {
    override def register(
      request: ParticipantRoutes.RegisterParticipantRequest,
    ): IO[Either[ValidationError, RegisterParticipantResponse]] = registerResult
  }

  val registerParticipantRequest = RegisterParticipantRequest("Joe", "Smith", "joe.smith@gmail.com")

  test("should return OK for correct request") {
    for {
      status <- ParticipantRoutes
                  .routes(new TestService(IO.pure(Right(RegisterParticipantResponse(1)))))
                  .run(Request[IO](Method.POST, uri"/user").withEntity(registerParticipantRequest))
                  .map(_.status)
                  .value
    } yield expect(status == Some(Status.Ok))
  }

  test("should return BadRequest for malformed request") {
    for {
      status <- ParticipantRoutes
                  .routes(new TestService(IO.pure(Left(ValidationError("error")))))
                  .run(Request[IO](Method.POST, uri"/user").withEntity(registerParticipantRequest))
                  .map(_.status)
                  .value
    } yield expect(status == Some(Status.BadRequest))
  }

  test("should return Internal Server Error for unexpected failures") {
    for {
      status <- ParticipantRoutes
                  .routes(new TestService(IO.raiseError(new Throwable("error"))))
                  .run(Request[IO](Method.POST, uri"/user").withEntity(registerParticipantRequest))
                  .map(_.status)
                  .value
    } yield expect(status == Some(Status.InternalServerError))
  }
}
