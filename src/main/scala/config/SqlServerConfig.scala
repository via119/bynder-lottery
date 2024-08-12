package config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default._

final case class SqlServerConfig(
  hostname: String,
  port: Int,
  database: String,
  username: String,
  password: String,
) derives ConfigReader
