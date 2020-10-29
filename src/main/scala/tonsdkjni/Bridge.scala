package tonsdkjni

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class Bridge {

  @native def tcCreateContext(config: String): String

  @native def tcDestroyContext(context: Long): Unit

  @native def tcRequest(context: Long, functionName: String, functionParamsJson: String, requestId: Long): Unit

  @native def tcRequestSync(context: Long, functionName: String, functionParamsJson: String): String

  def request(context: Long, functionName: String, functionParamsJson: String, responseHandler: Bridge.Handler): Unit = {
    val id = Bridge.counter.incrementAndGet()
    val internalHandler = new ResponseHandler {
      override def apply(requestId: Long, paramsJson: String, responseType: Long, finished: Boolean): Unit = {
        responseHandler(paramsJson, responseType, finished)
        if (finished) Bridge.mapping.remove(requestId)
      }
    }
    Bridge.mapping.put(id, internalHandler)
    tcRequest(context, functionName, functionParamsJson, id)
  }

  def handler(requestId: Long): ResponseHandler = Bridge.mapping.get(requestId)

}

object Bridge {
  type Handler = (String, Long, Boolean) => Unit
  private val mapping = new ConcurrentHashMap[Long, ResponseHandler]()
  private val counter = new AtomicLong(0)
}

abstract class ResponseHandler {
  def apply(requestId: Long, paramsJson: String, responseType: Long, finished: Boolean): Unit
}
