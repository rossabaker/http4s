package cats.effect

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

final class IO[+A] extends IONeeds[A] {

  def map[B](f: A => B): IO[B] = ???
  def flatMap[B](f: A => IO[B]): IO[B] = ???

  def attempt: IO[Either[Throwable, A]] = ???

  def unsafeRunSync(): A = ???
  def unsafeRunAsync(cb: Either[Throwable, A] => Unit): Unit = ???
  def unsafeRunTimed(limit: FiniteDuration): A = ???
}

object IO extends IOCompanionNeeds {
  def now[A](a: A): IO[A] = ???
  def delay[A](a: => A): IO[A] = ???
  def async[A](cb: (Either[Throwable, A] => Unit) => Unit): IO[A] = ???
  def fail(t: Throwable): IO[Nothing] = ???
}

trait IONeeds[+A] {
  def race[B](t: IO[B])(implicit S: fs2.Strategy): IO[Either[A,B]] = ???

  def handle[B>:A](f: PartialFunction[Throwable,B]): IO[B] = ???
  def handleWith[B>:A](f: PartialFunction[Throwable, IO[B]]): IO[B] = ???
}

trait IOCompanionNeeds {
  def fromFuture[A](f: => Future[A])(implicit ec: ExecutionContext): IO[A] = ???
  implicit def schrodingersInstances: fs2.util.Catchable[IO] with fs2.util.Suspendable[IO] = ???
  implicit def fs2Instances: fs2.util.Async[IO] = ???
  implicit def catsInstances: cats.Monad[IO] = ???
  implicit def monoidInstance[A: cats.Monoid]: cats.Monoid[IO[A]] = ???

  def apply[A](a: => A)(implicit S: fs2.Strategy): IO[A] = ???
  def start[A](io: IO[A])(implicit S: fs2.Strategy): IO[IO[A]] = ???
  def forkedAsync[A](cb: (Either[Throwable, A] => Unit) => Unit)(implicit S: fs2.Strategy): IO[A] = ???
  def suspend[A](a: => IO[A]): IO[A] = ???
}
