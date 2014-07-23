package org.http4s.client

import org.http4s.{Uri, Method, Request}


trait RequestBuilder {

  final def GET(uri: String): Request = Request(Method.Get, parseUri(uri))

  final def PUT(uri: String): Request = Request(Method.Put, parseUri(uri))

  final def POST(uri: String): Request = Request(Method.Post, parseUri(uri))

  final def OPTIONS(uri: String): Request = Request(Method.Options, parseUri(uri))

  final def HEAD(uri: String): Request = Request(Method.Head, parseUri(uri))

  final def DELETE(uri: String): Request = Request(Method.Delete, parseUri(uri))

  private def parseUri(str: String): Uri = {
    val uri = Uri.fromString(str).getOrElse(sys.error(s"Invalid Uri: $str"))
    if (uri.authority.isEmpty) sys.error(s"Uri requires authority: $str")
    uri
  }
}
