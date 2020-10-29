object Application extends App {
  def b2s(arr: Array[Byte]) = new String(arr)

  // System.loadLibrary("ton_client")
  System.loadLibrary("tonSdkJniBridge")

  val bridge = new TonSdkJniBridge()
  val result = b2s(bridge.tcCreateContext("""{ "servers": "http://localhost:8080" }"""))
  println(result)
  val id = result.filter(_.isDigit)
  bridge.tcDestroyContext(id.toLong)

}
