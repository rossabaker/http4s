package org.http4s

import cats._
import cats.arrow.Choice
import cats.data._
import cats.effect.IO
import fs2._
import org.http4s.batteries._

object Service {

  /**
    * Lifts a total function to a `Service`. The function is expected to handle
    * all requests it is given.  If `f` is a `PartialFunction`, use `apply`
    * instead.
    */
  def lift[A, B](f: A => IO[B]): Service[A, B] =
    Kleisli(f)

  /** Lifts a partial function to an `Service`.  Responds with the
    * zero of [B] for any request where `pf` is not defined.
    */
  def apply[A, B: Monoid](pf: PartialFunction[A, IO[B]]): Service[A, B] =
    lift(req => pf.applyOrElse(req, Function.const(IO.now(Monoid[B].empty))))

  /**
    * Lifts a IO into a [[Service]].
    *
    */
  def const[A, B](b: IO[B]): Service[A, B] =
    lift(_ => b)

  /**
    *  Lifts a value into a [[Service]].
    *
    */
  def constVal[A, B](b: => B): Service[A, B] =
    lift(_ => IO.delay(b))

  /** Allows Service chainig through a `scalaz.Monoid` instance. */
  def withFallback[A, B](fallback: Service[A, B])(service: Service[A, B])(implicit M: Monoid[IO[B]]): Service[A, B] =
    service |+| fallback

  /** A service that always returns the zero of B. */
  def empty[A, B: Monoid]: Service[A, B] =
    constVal(Monoid[B].empty)
}
