package org.http4s
package client
package asynchttpclient

import java.util.concurrent.{Callable, Executor, Executors, ExecutorService}

import org.asynchttpclient.AsyncHandler.State
import org.asynchttpclient.request.body.generator.{InputStreamBodyGenerator, BodyGenerator}
import org.asynchttpclient.{Request => AsyncRequest, Response => _, _}
import org.asynchttpclient.handler.StreamedAsyncHandler
import org.reactivestreams.Subscription
import org.http4s.client.impl.DefaultExecutor

import org.http4s.util.threads._

import org.reactivestreams.Publisher
import scodec.bits.ByteVector

import scala.collection.JavaConverters._

import scalaz.{\/, -\/, \/-}
import scalaz.stream.io._
import scalaz.concurrent.Task

import org.log4s.getLogger

object AsyncHttpClient {
  private[this] val log = getLogger

  val defaultConfig = new DefaultAsyncHttpClientConfig.Builder()
    .setMaxConnectionsPerHost(200)
    .setMaxConnections(400)
    .setRequestTimeout(30000)
    .setThreadFactory(threadFactory(name = { i => s"http4s-async-http-client-worker-${i}" }))
    .build()

  /**
    * Create an HTTP client based on the AsyncHttpClient library
    *
    * @param config configuration for the client
    * @param bufferSize body chunks to buffer when reading the body; defaults to 8
    * @param customExecutor custom executor which must be managed externally.
    */
  def apply(config: AsyncHttpClientConfig = defaultConfig,
            bufferSize: Int = 8,
            customExecutor: Option[ExecutorService] = None): Client = {
    val client = new DefaultAsyncHttpClient(config)
    val executorService = customExecutor.getOrElse(DefaultExecutor.newClientDefaultExecutorService("async-http-client-response"))
    val close =
      if (customExecutor.isDefined)
        Task.delay { client.close() }
      else
        Task.delay {
          client.close()
          executorService.shutdown()
        }

    Client(Service.lift { req =>
      client.executeRequest(toAsyncRequest(req), asyncHandler(bufferSize, executorService))
        .toTask(executorService)
        .flatMap(_.fold(Task.fail, Task.now))
    }, close)
  }

  private implicit class ListenableFutureSyntax[A](val self: ListenableFuture[A]) extends AnyVal {
    def toTask(executor: Executor): Task[A] =
      Task.async[A] { cb =>
        self.addListener(new Runnable {
          def run() = cb(\/.fromTryCatchThrowable[A, Throwable](self.get))
        }, executor)
        ()
      }
  }

  private def asyncHandler(bufferSize: Int, executorService: ExecutorService): AsyncHandler[Throwable \/ DisposableResponse] =
    new StreamedAsyncHandler[Throwable \/ DisposableResponse] {
      var state: State = State.CONTINUE
      var result: Throwable \/ DisposableResponse =
        \/-(DisposableResponse(Response(), Task.delay { state = State.ABORT }))

      override def onStream(publisher: Publisher[HttpResponseBodyPart]): State = {
        val subscriber = new QueueSubscriber[HttpResponseBodyPart](bufferSize) {
          override def onSubscribe(s: Subscription): Unit = {
            super.onSubscribe(s)
            request(bufferSize)
          }

          override def whenNext(element: HttpResponseBodyPart): Boolean = {
            state match {
              case State.CONTINUE =>
                val more = super.whenNext(element)
                if (more) request(1)
                more
              case State.ABORT =>
                super.whenNext(element)
                closeQueue()
                false
              case State.UPGRADE =>
                super.whenNext(element)
                state = State.ABORT
                throw new IllegalStateException("UPGRADE not implemented")
            }
          }

          override protected def request(n: Int): Unit =
            state match {
              case State.CONTINUE =>
                super.request(n)
              case _ =>
                // don't request what we're not going to process
            }
        }
        val body = subscriber.process.map(part => ByteVector(part.getBodyPartBytes))
        result = result.map(dr => dr.copy(
          response = dr.response.copy(body = body),
          dispose = Task.apply({
            log.debug("Disposing")
            state = State.ABORT
            subscriber.killQueue()
          })(executorService)
        ))
        publisher.subscribe(subscriber)
        state
      }

      override def onBodyPartReceived(httpResponseBodyPart: HttpResponseBodyPart): State =
        throw org.http4s.util.bug("Expected it to call onStream instead.")

      override def onStatusReceived(status: HttpResponseStatus): State = {
        result = result.map(dr => dr.copy(
          response = dr.response.copy(status = getStatus(status))
        ))
        state
      }

      override def onHeadersReceived(headers: HttpResponseHeaders): State = {
        result = result.map(dr => dr.copy(
          response = dr.response.copy(headers = getHeaders(headers))
        ))
        state
      }

      override def onThrowable(throwable: Throwable): Unit =
        result = -\/(throwable)

      override def onCompleted(): Throwable \/ DisposableResponse =
        result
    }

  private def toAsyncRequest(request: Request): AsyncRequest =
    new RequestBuilder(request.method.toString)
      .setUrl(request.uri.toString)
      .setHeaders(request.headers
        .groupBy(_.name.toString)
        .mapValues(_.map(_.value).asJavaCollection)
        .asJava
      ).setBody(getBodyGenerator(request.body))
      .build()

  private def getBodyGenerator(body: EntityBody): BodyGenerator =
    new InputStreamBodyGenerator(toInputStream(body))

  private def getStatus(status: HttpResponseStatus): Status =
    Status.fromInt(status.getStatusCode).valueOr(throw _)

  private def getHeaders(headers: HttpResponseHeaders): Headers = {
    Headers(headers.getHeaders.iterator.asScala.map { header =>
      Header(header.getKey, header.getValue)
    }.toList)
  }
}
