package org.http4s.blaze.client

import java.nio.channels.AsynchronousChannelGroup

/**
 * Created by Bryce Anderson on 6/28/14.
 */
class PooledHttp1Client(maxPooledConnections: Int = 10,
                        bufferSize: Int = 8*1024,
                        group: Option[AsynchronousChannelGroup] = None)
  extends PooledClient(maxPooledConnections, bufferSize, group) with Http1SSLSupport
