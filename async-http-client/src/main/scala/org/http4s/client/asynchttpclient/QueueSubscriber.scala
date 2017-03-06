package org.http4s.client.asynchttpclient

import scalaz.concurrent.Task
import scalaz.stream.async.boundedQueue
import scalaz.stream.Process
import scalaz.stream.Process.repeatEval
import org.log4s.getLogger

class QueueSubscriber[A](bufferSize: Int = 8) extends UnicastSubscriber[A] {
  private[this] val log = getLogger

  private val queue =
    boundedQueue[A](bufferSize)

  final val process: Process[Task, A] = queue.dequeue

  def whenNext(element: A): Boolean = {
    log.info(new String(element.asInstanceOf[org.asynchttpclient.HttpResponseBodyPart].getBodyPartBytes))
    queue.enqueueOne(element).run
    true
  }

  def closeQueue(): Unit = {
    log.debug("Closing queue subscriber")
    queue.close.run
  }

  def killQueue(): Unit = {
    log.debug("Killing queue subscriber")
    queue.kill.run
  }

  override def onComplete(): Unit = {
    log.debug(s"Completed queue subscriber")
    super.onComplete()
    closeQueue()
  }

  override def onError(t: Throwable): Unit = {
    super.onError(t)
    queue.fail(t).run
  }
}
