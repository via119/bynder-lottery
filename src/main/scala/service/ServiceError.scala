package service

enum ServiceError:
  case ValidationError(message: String)
  case UnexpectedError(message: String)
