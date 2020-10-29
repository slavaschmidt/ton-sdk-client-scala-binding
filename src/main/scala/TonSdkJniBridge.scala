class TonSdkJniBridge {
  type TcResponseHandler = (Long, String, Long, Boolean) => Unit

  @native def tcCreateContext(config: String): Array[Byte]

  @native def tcDestroyContext(context: Long): Unit

//  @native def tcRequest(context: Long, functionName: String, functionParamsJson: String, requestId: Long, responseHandler: TcResponseHandler): Unit

//  @native def tcRequestSync(context: Long, functionName: String, paramsJson: String): Array[Byte]

}

