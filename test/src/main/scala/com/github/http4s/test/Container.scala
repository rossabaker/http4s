package com.github.http4s.test

trait Container {
  protected def start(): Unit
  protected def stop(): Unit
}
