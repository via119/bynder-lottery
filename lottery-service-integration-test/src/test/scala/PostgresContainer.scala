import cats.effect.{IO, Resource}
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile

object PostgresContainer {

  def get(): Resource[IO, PostgreSQLContainer] = Resource.make(create())(container => close(container))

  def create(): IO[PostgreSQLContainer] = {
    IO {
      val container = new PostgreSQLContainer(Some(DockerImageName.parse("postgres:14.12")))
      container.container.withCopyFileToContainer(MountableFile.forClasspathResource("init.sql"), "/docker-entrypoint-initdb.d/")
      container.container.start()
      container
    }
  }

  def close(container: PostgreSQLContainer): IO[Unit] = {
    IO {
      container.close()
    }
  }
}
