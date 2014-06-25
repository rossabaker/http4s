package org.http4s.blaze.client

import org.http4s.{Header, Status}

import scala.collection.mutable.ListBuffer

/**
 * Created by Bryce Anderson on 6/25/14.
 */


trait Http1ClientReceiver { self: BlazeClientStage =>

  private var _status: Status = null
  private var _headers = new ListBuffer[Header]

  override protected def submitResponseLine(code: Int, reason: String,
                                            scheme: String,
                                            majorversion: Int, minorversion: Int): Unit = {
    _status = Status(code)
  }

  override protected def headerComplete(name: String, value: String): Boolean = {
    _headers += Header(name, value)
    false
  }


  protected def receiveResponse(cb: Callback) {
    ???
  }

}
