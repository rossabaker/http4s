package object schrodinger {
  type Deferrable[F[_]] = fs2.util.Suspendable[F]
}
