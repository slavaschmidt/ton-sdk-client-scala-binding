package ton.sdk.client.binding

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers._

import scala.concurrent.duration.DurationInt

class QueueBackedIteratorSpec extends AsyncFlatSpec {

  private val bit = new QueueBackedIterator[Int]()

  it should "be empty after creation" in {
    bit.hasNext shouldBe false
    bit.isClosed shouldBe false
    bit.isSuccess shouldBe false
    bit.next() shouldBe None
    bit.knownSize shouldBe 0
  }

  it should "see new elements" in {
    for { i <- 1 to 5 } bit.append(i)
    bit.hasNext shouldBe true
    bit.isClosed shouldBe false
    bit.isSuccess shouldBe false
    bit.next() shouldBe Some(1)
    bit.knownSize shouldBe 4 // to is inclusive and one is consumed
  }

  it should "wait for new elements" in {
    val t = new Thread() {
      override def run(): Unit = {
        Thread.sleep(3)
        for { i <- 1 to 500 } bit.append(i)
      }
    }
    t.start()
    val timeout = 2.seconds
    val before = System.currentTimeMillis()
    val items = bit.collect(timeout)
    val after = System.currentTimeMillis()
    t.join()
    (after - before) / 1000 shouldEqual timeout.length
    bit.hasNext shouldBe true
    bit.isClosed shouldBe false
    bit.isSuccess shouldBe false
    bit.next() shouldBe Some(2)
    bit.knownSize shouldBe 503
    items.size shouldBe 504 // because we got them before consuming "2"
  }

  it should "properly close" in {
    bit.close(None) shouldBe true
    bit.hasNext shouldBe true
    bit.isClosed shouldBe true
    bit.isSuccess shouldBe true
    bit.next() shouldBe Some(3)
    bit.knownSize shouldBe 502
  }

  it should "consume elements" in {
    var counter = 0
    while (bit.hasNext) {
      counter += 1
      bit.next()
    }
    counter shouldEqual 502
    bit.hasNext shouldBe false
    bit.next() shouldBe None
    bit.knownSize shouldBe 0
  }

  it should "not wait on closed" in {
    val timeout = 2.seconds
    val before = System.currentTimeMillis()
    val items = bit.collect(timeout)
    val after = System.currentTimeMillis()
    (after - before) / 1000 shouldEqual 0
    items.size shouldBe 0
  }

  it should "propagate error" in {
    val bit = new QueueBackedIterator[Int]()
    bit.close(Option(new Exception("Uh Oh")))
    bit.isSuccess shouldBe false
  }

}
