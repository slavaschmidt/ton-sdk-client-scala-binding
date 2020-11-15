package ton.sdk.client.binding

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

import io.circe.generic.auto._
import io.circe.syntax._
import io.circe._
import org.slf4j.LoggerFactory
import ton.sdk.client.binding.Api._
import ton.sdk.client.binding.Context._
import ton.sdk.client.jni.{Binding, Handler}

import scala.concurrent._
import scala.concurrent.duration.DurationInt
import scala.language.higherKinds
import scala.util.{Failure, Success, Try}

/**
  * This is the represenation of the SDK client context.
  * It be explicitly closed after it is not needed.
  *
  *  Please see [[https://github.com/tonlabs/TON-SDK/blob/master/docs/json_interface.md#contexts SDK documentation]] for more information
  *
  * The way the [[Context]] does calls is encoded by two parameters, [[SdkCall]] and [[Effect]] present in scope.
  * The [[SdkCall]] defines which function should be called and marshalling semantics
  * The [[Effect]] defined a way how the scala code interacts with underlying client and thus affects the type of the result
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
    * @param effect - the effect to be used to perform a call. Currently [[scala.util.Try]] and [[scala.concurrent.Future]] are available
    * @tparam P - the type of the parameter
    * @tparam R - the type of the result
    * @tparam E - the way the request should be executed
    * @return the result of the call wrapped in appropriate effect type
    */
  def request[P, R, E[_]](params: P)(implicit call: SdkCall[P, R], effect: Effect[E]): E[R] = {
    implicit val context: Context    = this
    implicit val decoder: Decoder[R] = call.decoder
    val fnName                       = call.function
    val jsonIn                       = call.toJson(params)
    effect.request(fnName, jsonPrinter.print(jsonIn))
  }

  /**
    * Message-Recieving request [[https://github.com/tonlabs/TON-SDK/blob/master/docs/json_interface.md#request]]
    *
    * @param params - the params to be passed over
    * @param streamingEvidence - this marker specifies that only this type of request can be used for message calls
    * @param call  - the call definition of the request, encodes type of the response and name of the function to be called
    * @param effect - the effect to be used to perform a call. Currently [[scala.util.Try]] and [[scala.concurrent.Future]] are available
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
    * @param eff - the effect to be used to perform a call. Currently [[scala.util.Try]] and [[scala.concurrent.Future]] are available
    * @param ctx - the context to call the function inside
    * @tparam P - the type of the parameter
    * @tparam R - the type of the result
    * @tparam E - the way the request should be executed
    * @return the result of the call wrapped in appropriate effect type
    */
  def call[P, R, E[_]](params: P)(implicit call: SdkCall[P, R], ctx: Context, eff: Effect[E]): E[R] =
    ctx.request(params)

  /**
    * Client call with messages
    *
    * @param params - the params to be passed over
    * @param call  - the call definition of the request, encodes type of the response and name of the function to be called
    * @param eff - the effect to be used to perform a call. Currently [[scala.util.Try]] and [[scala.concurrent.Future]] are available
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
    * Marker class for calls that support messages
    * @tparam T the type of the Effect this evidence is supported within
    */
  final case class StreamingEvidence[T[_]]()

  /**
    * Defining only futureSteramingEvidence makes it impossible to compile messaging calls with other [[Effect]]s (currently [[scala.util.Try]])
    */
  implicit val futureStreamingEvidence: StreamingEvidence[Future] = StreamingEvidence[Future]()

  /**
    * The definition of what an effect should be capable of
    * @tparam T the real type of the Effect, currenlty [[scala.util.Try]] and [[scala.concurrent.Future]] are available
    */
  trait Effect[T[_]] {
    /* Request without messages */
    def request[R](functionName: String, functionParams: String)(implicit c: Context, decoder: io.circe.Decoder[R]): T[R]
    /* Request with messages */
    def request[R, S](functionName: String, functionParams: String, streamingEvidence: StreamingEvidence[T])(
      implicit c: Context,
      decoders: (io.circe.Decoder[R], io.circe.Decoder[S])
    ): T[StreamingCallResult[R, S]]

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
    * Provides a possibility to call client functions wrapped as [[scala.util.Try]]. Does not support message calls.
    */
  val tryEffect: Effect[Try] = new Effect[Try] {
    override def request[R, S](functionName: String, functionParams: String, streamingEvidence: StreamingEvidence[Try])(
      implicit c: Context,
      decoders: (io.circe.Decoder[R], io.circe.Decoder[S])
    ): Try[StreamingCallResult[R, S]] = throw new NotImplementedError("Calling this method should not compile, hence no implementation")

    override def request[R](functionName: String, functionParams: String)(implicit c: Context, decoder: io.circe.Decoder[R]): Try[R] = {
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
  }

  /**
    * Provides a possibility to call client functions wrapped as [[scala.concurrent.Future]]s.
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
    override def request[R](functionName: String, functionParams: String)(implicit c: Context, d: Decoder[R]): Future[R] =
      requestFuture(functionName, functionParams)

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
          case ResponseTypeStream(code) =>
            implicit val decoder: Decoder[S] = r._2
            def tryParseResult(t: Throwable) = SdkResultOrError.fromJsonPlain[S](requestId, paramsJson).map(result.messages.append).getOrElse(false)
            val _                            = SdkResultOrError.fromJsonPlain[SdkClientError](requestId, paramsJson).fold(tryParseResult, result.errors.append)
        }
      }
      if (!c.isOpen.get()) {
        p.failure(new IllegalStateException(s"Request(async) is called on closed context ${c.id}: $functionName, $functionParams"))
      } else {
        Binding.request(c.id, functionName, functionParams, handler)
      }
      result.result.map((_, result.messages, result.errors))
    }

    /* Call without messages */
    private def requestFuture[R, S](functionName: String, functionParams: String)(implicit c: Context, r: Decoder[R]): Future[R] = {
      val p   = Promise[R]()
      val buf = StringBuilder.newBuilder
      val handler: Handler = (requestId: Long, paramsJson: String, responseType: Long, finished: Boolean) => {
        logger.trace(s"$requestId: $responseType ($finished) - $paramsJson - ${buf.result}")
        ResponseType(responseType) match {
          case ResponseTypeNop | ResponseTypeReserved(_) =>
            successIfFinished(requestId, finished, p, buf.result())

          case ResponseTypeResult =>
            buf.append(paramsJson)
            successIfFinished(requestId, finished, p, buf.result())

          case ResponseTypeError =>
            if (!finished) logger.warn(errUndefinedBehaviour)
            p.failure(SdkClientError(c, requestId, paramsJson).fold(BindingError, identity))

          // This should not happen, but just for the case let's handle it somethow meaningfully
          case ResponseTypeStream(code) =>
            logger.warn(s"Streaming in non-streaming request: $requestId: $responseType[$code]($finished) - $paramsJson")
            buf.append(paramsJson)
            successIfFinished(requestId, finished, p, buf.result())
        }
      }
      if (!c.isOpen.get()) {
        p.failure(new IllegalStateException(s"Request(async) is called on closed context ${c.id}: $functionName, $functionParams"))
      } else {
        Binding.request(c.id, functionName, functionParams, handler)
      }
      p.future
    }

    private def successIfFinished[R: Decoder](requestId: Long, finished: Boolean, p: Promise[R], buf: String): Unit = {
      val _ =
        if (finished && !p.isCompleted)
          SdkResultOrError.fromJsonPlain(requestId, buf).fold(ex => p.failure(SdkClientError.parsingError(requestId, ex.getMessage, buf.asJson)), p.success(_))
    }

    override def managed[R](config: ClientConfig)(block: Context => Future[R]): Future[R] =
      Context.fromTry(create(config)).flatMap { context =>
        val result = block(context)
        result.onComplete(_ => context.close())
        result
      }

    override def flatMap[P, R](in: Future[P])(f: P => Future[R]): Future[R]                      = in.flatMap(f)
    override def map[P, R](in: Future[P])(f: P => R): Future[R]                                  = in.map(f)
    override def recover[R, U >: R](in: Future[R])(pf: PartialFunction[Throwable, U]): Future[U] = in.recover(pf)

    override def unsafeGet[R](a: Future[R]): R = Await.result(a, 60.seconds)
  }

  // the context creation is within Try so we need to Futurize the result
  def fromTry[R](t: Try[R]): Future[R] = t match {
    case Success(r)  => Future.successful(r)
    case Failure(ex) => Future.failed(ex)
  }

}
