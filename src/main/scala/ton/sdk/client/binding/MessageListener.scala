package ton.sdk.client.binding

import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

trait BlockingIterator[+A] {
  def hasNext: Boolean
  def isClosed: Boolean
  def isSuccess: Boolean
  def next(): Option[A]
  def knownSize: Int
  def collect(timeout: Duration): Seq[A]
}

class QueueBackedIterator[A](p: Promise[Unit]) extends BlockingIterator[A] {
  override def hasNext: Boolean   = !buf.isEmpty
  override def isClosed: Boolean  = flag.isCompleted
  override def isSuccess: Boolean = isClosed && flag.value.exists(_.isSuccess)

  override def next(): Option[A] = Option(buf.poll())

  override def knownSize: Int = buf.size()

  // Returns a copy and preserves elements in the iterator
  override def collect(timeout: Duration): Seq[A] = {
    Await.ready(flag, timeout)
    buf.iterator().asScala.toSeq
  }

  private[this] val buf  = new ConcurrentLinkedQueue[A]()
  private[this] val flag = p.future

  protected[binding] def append(a: A): Boolean                 = buf.add(a)
  protected[binding] def close(ex: Option[Exception]): Boolean = p.tryComplete(ex.map(Failure(_)).getOrElse(Success(())))
}
