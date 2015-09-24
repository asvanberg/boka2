package object algebra {
  type ConnectionIO[A] = scalaz.Reader[java.sql.Connection, A]
}
