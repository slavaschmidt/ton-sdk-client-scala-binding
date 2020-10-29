package tonsdkjni

class Bridge {

  @native def tcCreateContext(config: String): String

  @native def tcDestroyContext(context: Long): Unit

  @native def tcRequest(context: Long, functionName: String, functionParamsJson: String, requestId: Long, responseHandler: ResponseHandler): Unit

  @native def tcRequestSync(context: Long, functionName: String, functionParamsJson: String): String

}

abstract class ResponseHandler {
  def handle(requestId: Long, paramsJson: String, responseType: Long, finished: Boolean): Unit

  def dummy: Unit = println("WTF")
}

class PrinterHandler extends ResponseHandler {
  override def handle(requestId: Long, paramsJson: String, responseType: Long, finished: Boolean): Unit = {
    println(s"$requestId, $paramsJson, $responseType, $finished")
  }
}
