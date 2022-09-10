package ton.sdk.client.binding

import io.circe
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.slf4j.LoggerFactory
import ton.sdk.client.binding.Api._
import ton.sdk.client.binding.Context._
import ton.sdk.client.jni.{Binding, Callback, Handler}
import ton.sdk.client.modules.Client.AppRequestResult
import ton.sdk.client.modules.Client.Request.ResolveAppRequest

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable
import scala.concurrent._
import scala.concurrent.duration.DurationInt
import scala.language.higherKinds
import scala.util.{Failure, Success, Try}

/**
  * This is the representation of the SDK client context.
  * It be explicitly closed after it is not needed.
  *
  *  Please see [[https://github.com/tonlabs/TON-SDK/blob/master/docs/json_interface.md#contexts SDK documentation]] for more information
  *
  * The way the [[Context]] does calls is encoded by two parameters, [[Api.SdkCall]] and `Effect` present in scope.
  * The [[Api.SdkCall]] defines which function should be called and marshalling semantics
  * The `Effect` defined a way how the scala code interacts with underlying client and thus affects the type of the result
  */
final case class Context private (id: Long) extends Closeable {
  val isOpen = new AtomicBoolean(true)

  /**
    * Closes underlying client context
    */
  override def close(): Unit = { val _ = Try(if (isOpen.getAndSet(false)) Binding.tcDestroyContext(id)) }

  /**
    * Double-check that the context is closed if it is garbage-collected
    */
  override def finalize(): Unit = if (isOpen.get()) {
    logger.warn(s"Auto-closing Context($id) because it was not closed as expected. Probably this is a programming mistake.")
    Binding.tcDestroyContext(id)
  }

  /**
    * Normal request [[https://github.com/tonlabs/TON-SDK/blob/master/docs/json_interface.md#request]]
    *
    * @param params - the params to be passed over
    * @param call  - the call definition of the request, encodes type of the response and name of the function to be called
    * @param effect - the effect to be used to perform a call. Currently `Try` and `Future` are available
    * @tparam P - the type of the parameter
    * @tparam R - the type of the result
    * @tparam E - the way the request should be executed
    * @return the result of the call wrapped in appropriate effect type
    */
  def request[P, R, E[_]](params: P, callback: Option[Callback] = None)(implicit call: SdkCall[P, R], effect: Effect[E]): E[R] = {
    implicit val context: Context    = this
    implicit val decoder: Decoder[R] = call.decoder
    val fnName                       = call.function
    val jsonIn                       = call.toJson(params)
    effect.request(fnName, jsonPrinter.print(jsonIn), callback)
  }

  /**
    * Message-Recieving request [[https://github.com/tonlabs/TON-SDK/blob/master/docs/json_interface.md#request]]
    *
    * @param params - the params to be passed over
    * @param streamingEvidence - this marker specifies that only this type of request can be used for message calls
    * @param call  - the call definition of the request, encodes type of the response and name of the function to be called
    * @param effect - the effect to be used to perform a call. Currently `Try` and `Future` are available
    * @tparam P - the type of the parameter
    * @tparam R - the type of the result
    * @tparam E - the way the request should be executed
    * @tparam S - the type of messages
    * @return the result of the call wrapped in appropriate effect type, including messages and errors
    */
  def request[P, R, S, E[_]](params: P, streamingEvidence: StreamingEvidence[E])(implicit call: StreamingSdkCall[P, R, S], effect: Effect[E]): E[StreamingCallResult[R, S]] = {
    implicit val context: Context                   = this
    implicit val decoders: (Decoder[R], Decoder[S]) = call.decoders
    val fnName: String                              = call.function
    val jsonIn: Json                                = call.toJson(params)
    val result                                      = effect.request(fnName, jsonPrinter.print(jsonIn), streamingEvidence)
    result
  }

  /**
    * Debot request [[https://github.com/tonlabs/TON-SDK/blob/master/docs/json_interface.md#request]]
    *
    * @param params - the params to be passed over
    * @param call  - the call definition of the request, encodes type of the response and name of the function to be called
    * @param effect - the effect to be used to perform a call. Currently `Try` and `Future` are available
    * @tparam P - the type of the parameter
    * @tparam R - the type of the result
    * @tparam E - the way the request should be executed
    * @return the result of the call wrapped in appropriate effect type
    */
  def request[P, R, E[_]](params: P, callback: DebotCallback)(implicit call: DebotCall[P, R], effect: Effect[E], d: Decoder[R]): E[R] = {
    implicit val context: Context = this
    val fnName                    = call.function
    val jsonIn                    = call.toJson(params)
    effect.requestDebot(fnName, jsonPrinter.print(jsonIn), callback)
  }
}

/**
  * Concrete implementation of the [[Context]]
  */
object Context {
  type StreamingCallResult[R, S] = (R, BlockingIterator[S], BlockingIterator[SdkClientError])

  private val jsonPrinter = Printer.noSpaces.copy(dropNullValues = true)

  private val errUndefinedBehaviour =
    "Got unfinished error response, the expected behaviour is not clear. Current implementation will continue to consume data but the result will be the first error"
  private val logger = LoggerFactory.getLogger(getClass)

  /**
    * Creates a context for given config
    * @param config the config to craete context for
    * @return Context or Failure if something went wrong
    */
  def create(config: ClientConfig): Try[Context] = this.synchronized {
    val json = Binding.tcCreateContext(jsonPrinter.print(config.asJson))
    SdkResultOrError.fromJsonWrapped[Long](json).map(Context.apply)
  }

  // some predefined managed contexts for known servers
  def local[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T]   = effect.managed(ClientConfig.LOCAL)(block)
  def mainNet[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T] = effect.managed(ClientConfig.MAIN_NET)(block)
  def devNet[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T]  = effect.managed(ClientConfig.DEV_NET)(block)
  def testNet[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T] = effect.managed(ClientConfig.TEST_NET)(block)

  /**
    * Normal client call
    *
    * @param params - the params to be passed over
    * @param call  - the call definition of the request, encodes type of the response and name of the function to be called
    * @param eff - the effect to be used to perform a call. Currently `Try` and `Future` are available
    * @param ctx - the context to call the function inside
    * @tparam P - the type of the parameter
    * @tparam R - the type of the result
    * @tparam E - the way the request should be executed
    * @return the result of the call wrapped in appropriate effect type
    */
  def call[P, R, E[_]](params: P, callback: Option[Callback] = None)(implicit call: SdkCall[P, R], ctx: Context, eff: Effect[E]): E[R] =
    ctx.request(params, callback)

  /**
    * Client call with messages
    *
    * @param params - the params to be passed over
    * @param call  - the call definition of the request, encodes type of the response and name of the function to be called
    * @param eff - the effect to be used to perform a call. Currently `Try` and `Future` are available
    * @param ctx - the context to call the function inside
    * @tparam P - the type of the parameter
    * @tparam R - the type of the result
    * @tparam E - the way the request should be executed
    * @tparam S - the type of messages
    * @param streamingEvidence - this marker specifies that only this type of request can be used for message calls
    * @return the result of the call wrapped in appropriate effect type, including messages and errors
    */
  def callS[P, R, S, E[_]](
    params: P
  )(implicit call: StreamingSdkCall[P, R, S], ctx: Context, eff: Effect[E], streamingEvidence: StreamingEvidence[E]): E[StreamingCallResult[R, S]] =
    ctx.request(params, streamingEvidence)

  /**
    * Debot client call
    *
    * @param params - the params to be passed over
    * @param call  - the call definition of the request, encodes type of the response and name of the function to be called
    * @param eff - the effect to be used to perform a call. Currently `Try` and `Future` are available
    * @param ctx - the context to call the function inside
    * @tparam P - the type of the parameter
    * @tparam R - the type of the result
    * @tparam E - the way the request should be executed
    * @return the result of the call wrapped in appropriate effect type
    */
  def callD[P, R, E[_]](params: P, callback: DebotCallback)(implicit call: DebotCall[P, R], ctx: Context, eff: Effect[E], d: Decoder[R]): E[R] =
    ctx.request(params, callback)

  /**
    * Marker class for calls that support messages
    * @tparam T the type of the Effect this evidence is supported within
    */
  final case class StreamingEvidence[T[_]]()

  /**
    * Defining only futureSteramingEvidence makes it impossible to compile messaging calls with other [[Effect]]s (currently `Try`)
    */
  implicit val futureStreamingEvidence: StreamingEvidence[Future] = StreamingEvidence[Future]()

  /**
    * The definition of what an effect should be capable of
    * @tparam T the real type of the Effect, currenlty `Try` and `Future` are available
    */
  trait Effect[T[_]] {
    /* Request without messages */
    def request[R](functionName: String, functionParams: String, callback: Option[Callback])(implicit c: Context, decoder: io.circe.Decoder[R]): T[R]

    /* Request with messages */
    def request[R, S](functionName: String, functionParams: String, streamingEvidence: StreamingEvidence[T])(
      implicit c: Context,
      decoders: (io.circe.Decoder[R], io.circe.Decoder[S])
    ): T[StreamingCallResult[R, S]]

    /* Request for debot */
    def requestDebot[R](functionName: String, functionParams: String, debotCallback: DebotCallback)(implicit c: Context, decoder: io.circe.Decoder[R]): T[R]

    /**
      * Provides a possibility to execute block of code typed as this effect within a context.
      * The context is created and closed automatically.
      *
      * @param config - the config to be used for context creation
      * @param block - the block to execute
      * @tparam R - the type of expected result
      * @return
      */
    def managed[R](config: ClientConfig)(block: Context => T[R]): T[R]

    // following four are just a helper methods for effect-independent test definition and should not be used in production code
    def flatMap[P, R](in: T[P])(f: P => T[R]): T[R]
    def map[P, R](in: T[P])(f: P => R): T[R]
    def recover[R, U >: R](in: T[R])(pf: PartialFunction[Throwable, U]): T[U]
    def unsafeGet[R](a: T[R]): R
  }

  /**
    * Provides a possibility to call client functions wrapped as `Try`. Does not support message calls.
    */
  val tryEffect: Effect[Try] = new Effect[Try] {
    override def request[R, S](functionName: String, functionParams: String, streamingEvidence: StreamingEvidence[Try])(
      implicit c: Context,
      decoders: (io.circe.Decoder[R], io.circe.Decoder[S])
    ): Try[StreamingCallResult[R, S]] = throw new NotImplementedError("Calling this method should not compile, hence no implementation")

    override def request[R](functionName: String, functionParams: String, callback: Option[Callback])(implicit c: Context, decoder: io.circe.Decoder[R]): Try[R] = {
      if (!c.isOpen.get()) {
        Failure(new IllegalStateException(s"Request(sync) is called on closed context ${c.id}: $functionName, $functionParams"))
      } else {
        Try {
          val json = Binding.tcRequestSync(c.id, functionName, functionParams)
          SdkResultOrError.fromJsonWrapped(json)
        }.flatten
      }
    }
    override def managed[R](config: ClientConfig)(block: Context => Try[R]): Try[R] = {
      create(config).flatMap { c =>
        val result = Try(block(c)).flatten
        Try(c.close())
        result
      }
    }

    override def flatMap[P, R](in: Try[P])(f: P => Try[R]): Try[R]                         = in.flatMap(f)
    override def map[P, R](in: Try[P])(f: P => R): Try[R]                                  = in.map(f)
    override def unsafeGet[R](a: Try[R]): R                                                = a.get
    override def recover[R, U >: R](in: Try[R])(pf: PartialFunction[Throwable, U]): Try[U] = in.recover(pf)

    override def requestDebot[R](functionName: String, functionParams: String, debotCallback: DebotCallback)(implicit c: Context, decoder: io.circe.Decoder[R]): Try[R] =
      throw new NotImplementedError("Debot support is not implemented for the sync client calls")
  }

  /**
    * Provides a possibility to call client functions wrapped as `Future`s.
    * @param ec execution context to make asyncronous execution configurable
    * @return
    */
  def futureEffect(implicit ec: ExecutionContext): Effect[Future] = new Effect[Future] {
    /* Call with messages */
    override def request[R, S](
      functionName: String,
      functionParams: String,
      streamingEvidence: StreamingEvidence[Future]
    )(implicit c: Context, r: (Decoder[R], Decoder[S])): Future[StreamingCallResult[R, S]] =
      requestStreamingFuture(functionName, functionParams)

    /* Call without messages */
    override def request[R](functionName: String, functionParams: String, callback: Option[Callback])(implicit c: Context, d: Decoder[R]): Future[R] =
      requestFuture(functionName, functionParams, callback)

    /* Call with messages */
    private def requestStreamingFuture[R, S](
      functionName: String,
      functionParams: String
    )(implicit c: Context, r: (Decoder[R], Decoder[S])): Future[StreamingCallResult[R, S]] = {
      val p      = Promise[R]()
      val result = new AsyncCallResult[R, S](p.future)
      val buf    = StringBuilder.newBuilder
      val handler: Handler = (requestId: Long, paramsJson: String, responseType: Long, finished: Boolean) => {
        logger.trace(s"Streaming $requestId: $responseType ($finished) - $paramsJson")
        if (finished) {
          result.messages.close(None)
          result.errors.close(None)
        }
        ResponseType(responseType) match {
          case ResponseTypeNop | ResponseTypeReserved(_) =>
            implicit val decoder: Decoder[R] = r._1
            successIfFinished(requestId, finished, p, buf.result())
          case ResponseTypeResult =>
            buf.append(paramsJson)
            implicit val decoder: Decoder[R] = r._1
            val finishAfterFirstResult       = true
            successIfFinished(requestId, finished || finishAfterFirstResult, p, buf.result())
          case ResponseTypeError =>
            p.failure(SdkClientError(c, requestId, paramsJson).fold(BindingError, identity))
          case ResponseTypeAppNotify | ResponseTypeAppRequest =>
            logger.warn(s"GOT ${ResponseType(responseType)} in streaming request with messages: $paramsJson")
          case ResponseTypeStream(code) =>
            implicit val decoder: Decoder[S] = r._2
            def tryParseResult(t: Throwable) = SdkResultOrError.fromJsonPlain[S](requestId, paramsJson).map(result.messages.append).getOrElse(false)
            val _                            = SdkResultOrError.fromJsonPlain[SdkClientError](requestId, paramsJson).fold(tryParseResult, result.errors.append)
        }
      }
      if (!c.isOpen.get()) {
        p.failure(new IllegalStateException(s"Request(async streaming) is called on closed context ${c.id}: $functionName, $functionParams"))
      } else {
        Binding.request(c.id, functionName, functionParams, handler, null, null)
      }
      result.result.map((_, result.messages, result.errors))
    }

    val noopHandler: Handler = (requestId, paramsJson, responseType, finished) => logger.trace(s"$requestId, $paramsJson, $responseType, $finished")

    /* Call without messages */
    private def requestFuture[R, S](functionName: String, functionParams: String, callback: Option[Callback])(implicit c: Context, r: Decoder[R]): Future[R] = {
      val p   = Promise[R]()
      val buf = mutable.StringBuilder.newBuilder
      val handler: Handler = (requestId: Long, paramsJson: String, responseType: Long, finished: Boolean) => {
        logger.trace(s"$requestId: $responseType ($finished) - $paramsJson - ${buf.result}")
        ResponseType(responseType) match {
          case ResponseTypeNop | ResponseTypeReserved(_) =>
            successIfFinished(requestId, finished, p, buf.result())

          case ResponseTypeResult =>
            buf.append(paramsJson)
            successIfFinished(requestId, true, p, buf.result())

          case ResponseTypeError =>
            if (!finished) logger.warn(errUndefinedBehaviour)
            p.failure(SdkClientError(c, requestId, paramsJson).fold(BindingError, identity))

          // This should not happen, but just for the case let's handle it somehow meaningfully
          case ResponseTypeStream(code) =>
            logger.warn(s"Streaming in non-streaming request: $requestId: $responseType[$code]($finished) - $paramsJson")
            buf.append(paramsJson)
            successIfFinished(requestId, finished, p, buf.result())

          case ResponseTypeAppNotify =>
            buf.append(paramsJson)
            successIfFinished(requestId, finished, p, buf.result())

          case ResponseTypeAppRequest =>
            logger.info(s"ResponseTypeAppRequest: $requestId: $responseType($finished) - $paramsJson")
            val callback = Binding.callbacks.get(requestId)
            if (callback != null) {
              SdkResultOrError
                .fromJsonPlain[RequestData](requestId, paramsJson)
                .fold(
                  f => sys.error(f.getMessage),
                  json => {
                    val result = callback.call(json.request_data)
                    val ok     = ResolveAppRequest(json.app_request_id, AppRequestResult("Ok", None, Option(result))).asJson.noSpaces
                    Binding.request(c.id, "client.resolve_app_request", ok, noopHandler, null, json.app_request_id)
                  }
                )
            }
            successIfFinished(requestId, finished, p, buf.result())
        }
      }
      if (!c.isOpen.get()) {
        p.failure(new IllegalStateException(s"Request(async) is called on closed context ${c.id}: $functionName, $functionParams"))
      } else {
        Binding.request(c.id, functionName, functionParams, handler, callback.orNull, null)
      }
      p.future
    }

    private def successIfFinished[R: Decoder](requestId: Long, finished: Boolean, p: Promise[R], buf: String): Unit = {
      val _ =
        if (finished && !p.isCompleted) {
          Binding.callbacks.remove(requestId)
          Binding.handlers.remove(requestId)
          SdkResultOrError.fromJsonPlain(requestId, buf).fold(ex => p.failure(exToSdkError(requestId, buf, ex)), p.success(_))
        }
    }

    private def exToSdkError(requestId: Long, buf: String, ex: Throwable) = ex match {
      case err: SdkClientError => err
      case _                   => SdkClientError.parsingError(requestId, ex.getMessage, buf.asJson)
    }

    /**
      * Creates context, executes the provided block and then closes the context
      *
      * @param config - the config to be used for context creation
      * @param block - the block to execute
      * @tparam R - the type of expected result
      *  @return the result of the execution
      */
    override def managed[R](config: ClientConfig)(block: Context => Future[R]): Future[R] =
      Context.fromTry(create(config)).flatMap { context =>
        val result = block(context)
        result.onComplete(_ => context.close())
        result
      }

    override def flatMap[P, R](in: Future[P])(f: P => Future[R]): Future[R]                      = in.flatMap(f)
    override def map[P, R](in: Future[P])(f: P => R): Future[R]                                  = in.map(f)
    override def recover[R, U >: R](in: Future[R])(pf: PartialFunction[Throwable, U]): Future[U] = in.recover(pf)

    private val unsafeGetTimeout               = 300.seconds
    override def unsafeGet[R](a: Future[R]): R = Await.result(a, unsafeGetTimeout)

    override def requestDebot[R](functionName: String, functionParams: String, debotCallback: DebotCallback)(implicit c: Context, d: Decoder[R]): Future[R] = {
      val p   = Promise[R]()
      val buf = mutable.StringBuilder.newBuilder
      val handler: Handler = (requestId: Long, paramsJson: String, responseType: Long, finished: Boolean) => {
        logger.trace(s"$requestId: $responseType ($finished) - $paramsJson")
        ResponseType(responseType) match {

          case tpe @ (ResponseTypeAppNotify | ResponseTypeAppRequest) =>
            circe.parser.parse(paramsJson) match {
              case Right(json) =>
                debotCallback(tpe, json)
              case Left(ex) =>
                logger.warn(s"Failed to parse debot response: $requestId: $responseType($finished) - $paramsJson")
                p.failure(ex)
            }

          case ResponseTypeNop | ResponseTypeReserved(_) =>
            successIfFinished(requestId, finished, p, buf.result())

          case ResponseTypeResult =>
            buf.append(paramsJson)
            successIfFinished(requestId, finished = true, p, buf.result())

          case ResponseTypeError =>
            if (!finished) logger.warn(errUndefinedBehaviour)
            p.failure(SdkClientError(c, requestId, paramsJson).fold(BindingError, identity))

          // This should not happen, but just for the case let's handle it somehow meaningfully
          case ResponseTypeStream(code) =>
            logger.warn(s"Streaming in non-streaming request: $requestId: $responseType[$code]($finished) - $paramsJson")
            buf.append(paramsJson)
            successIfFinished(requestId, finished, p, buf.result())
        }
      }
      if (!c.isOpen.get()) {
        p.failure(new IllegalStateException(s"Request(debot) is called on closed context ${c.id}: $functionName, $functionParams"))
      } else {
        Binding.request(c.id, functionName, functionParams, handler, null, null)
      }
      p.future
    }
  }

  // the context creation is within Try so we need to "Futurize" the result
  def fromTry[R](t: Try[R]): Future[R] = t match {
    case Success(r)  => Future.successful(r)
    case Failure(ex) => Future.failed(ex)
  }

}
