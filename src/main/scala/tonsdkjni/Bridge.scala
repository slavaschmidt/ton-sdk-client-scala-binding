//package tonsdkjni
//
//import java.util.concurrent.ConcurrentHashMap
//import java.util.concurrent.atomic.AtomicLong
//
//class Bridge {
//
//  @native def tcCreateContext(config: String): String
//
//  @native def tcDestroyContext(context: Long): Unit
//
//  @native def tcRequest(context: Long, functionName: String, functionParamsJson: String, requestId: Long): Unit
//
//  @native def tcRequestSync(context: Long, functionName: String, functionParamsJson: String): String
//
//  def request(context: Long, functionName: String, functionParamsJson: String, responseHandler: Bridge.Handler): Unit = {
//    val id = Bridge.counter.incrementAndGet()
//    Bridge.mapping.put(id, responseHandler)
//    tcRequest(context, functionName, functionParamsJson, id)
//  }
//
//  def handle(requestId: Long, paramsJson: String, responseType: Long, finished: Boolean): Unit = {
//    val handler = Bridge.mapping.get(requestId)
//    // TODO add error handling
//    handler(requestId, paramsJson, responseType, finished)
//    if (finished) Bridge.mapping.remove(requestId)
//  }
//
//}
//
//object Bridge {
//  type Handler = (Long, String, Long, Boolean) => Unit
//}
