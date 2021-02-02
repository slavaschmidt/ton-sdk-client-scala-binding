package ton.sdk.client.binding

import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

/**
  * An iterator over a collection which elements can arrive at some point later.
  * BlockingIterator differs from scala streams and normal Iterator that are expected
  * to be able to return all their elements if requested.
  *
  * @tparam A - the type of the elements stored in this [[BlockingIterator]]
  */
trait BlockingIterator[+A] {

  /**
    * Returns {@code true} if the iteration has more elements available without blocking
    *
    * @return {@code true} if some elements are there
    */
  def hasNext: Boolean

  /**
    * Returns {@code true} if the all of the expected elements are stored locally
    * and no new elements are expected to arrive in the future
    *
    * @return {@code true} if there are no new elements expected to be put in this iterator in addition to these already available
    */
  def isClosed: Boolean

  /**
    * Returns {@code true} if this iterator is closed and no error happened during fetching of the elements.
    *
    * @return {@code true} if this iterator is clodes and all elements were fetched without errors
    */
  def isSuccess: Boolean

  /**
    * If there is an element available locally, this method will return it. Otherwise returns None.
    * Due to blocking nature of this iterator, an element might be put into the queue between
    * calls {@code hasNext} and {@code next()}, in this case an element will be returned from
    * seemingly empty {@code BlockingIterator}
    *
    * @return {@code Some(element)} if there is an element available locally
    */
  def next(): Option[A]

  /**
    * Awaits first available element and returns it.
    *
    * @return first element put into the queue {@code element}
    * @throws TimeoutException if there is a timeout
    */
  def getNext(timeout: Duration): A

  /**
    * Calculates number of elements known to be available locally
    *
    * @return number of elements available to be fetched from this {@code BlockingIterator}
    */
  def knownSize: Int

  /**
    * Collects all of the available elements for at most {@code timeout}.
    * Collected elements are not removed from the iterator.
    * This method should block for at most {@code timeout} to collect all possible elements.
    * This method should return immediately if this {@code BlockingIterator} is already closed.
    * This method should not wait longer than needed in the case this {@code BlockingIterator}
    * is closed earlier than the specified {@code timeout}
    *
    * @param timeout how long to wait for the elements to arrive
    * @return collected elements. The original items aren't removed from the iterator
    */
  def collect(timeout: Duration): Seq[A]
}

/**
  * An implementation of the {@code BlockingIterator} backed by java Queue and scala Future.
  *
  * @tparam A - the type of the elements stored in this [[BlockingIterator]]
  */
class QueueBackedIterator[A]() extends BlockingIterator[A] {
  private val p: Promise[Unit]    = Promise()
  override def hasNext: Boolean   = !buf.isEmpty
  override def isClosed: Boolean  = flag.isCompleted
  override def isSuccess: Boolean = isClosed && flag.value.exists(_.isSuccess)
  override def next(): Option[A]  = Option(buf.poll())
  override def knownSize: Int     = buf.size()

  override def collect(timeout: Duration): Seq[A] = {
    Try(Await.ready(flag, timeout))
    buf.iterator().asScala.toSeq
  }

  // adds element to the iterator, to be used by the callback
  protected[binding] def append(a: A): Boolean = {
    listener.fold(buf.add(a))(_.tryComplete(Success(a)))
  }

  // to be used by the callback to signal that this {@code BlockingIterator} should close with success or failure
  protected[binding] def close(ex: Option[Exception]): Boolean = p.tryComplete(ex.map(Failure(_)).getOrElse(Success(())))

  private[this] val buf  = new ConcurrentLinkedQueue[A]()
  private[this] val flag = p.future

  private var listener: Option[Promise[A]] = None

  override def getNext(timeout: Duration): A = {
    if (hasNext) {
      next().get
    } else {
      val pr = Promise[A]()
      listener = Option(pr)
      try {
        Await.result(pr.future, timeout)
      } finally {
        listener = None
      }
    }
  }
}
