package org.http4s

import cats._
import org.http4s.batteries._

final case class Entity[F[_]](body: EntityBody[F], length: Option[Long] = None) {
  def +(that: Entity[F]): Entity[F] =
    Entity(this.body ++ that.body, (this.length |@| that.length).map(_ + _))
}

object Entity {
  implicit def entityInstance[F[_]]: Monoid[Entity[F]] =
    new Monoid[Entity[F]] {
      def combine(a1: Entity[F], a2: Entity[F]): Entity[F] =
        a1 + a2
      val empty: Entity[F] =
        Entity.empty[F]
    }

  def empty[F[_]] = Entity(EmptyBody[F], Some(0L))
}
