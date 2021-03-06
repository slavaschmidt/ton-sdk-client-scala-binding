package ton.sdk.client.binding

final case class ClientErrorCode(code: Long)

object ClientErrors {
  val JsonApiParsingError                 = ClientErrorCode(-1L)
  val NotImplemented                      = ClientErrorCode(1)
  val InvalidHex                          = ClientErrorCode(2)
  val InvalidBase64                       = ClientErrorCode(3)
  val InvalidAddress                      = ClientErrorCode(4)
  val CallbackParamsCantBeConvertedToJson = ClientErrorCode(5)
  val WebsocketConnectError               = ClientErrorCode(6)
  val WebsocketReceiveError               = ClientErrorCode(7)
  val WebsocketSendError                  = ClientErrorCode(8)
  val HttpClientCreateError               = ClientErrorCode(9)
  val HttpRequestCreateError              = ClientErrorCode(10)
  val HttpRequestSendError                = ClientErrorCode(11)
  val HttpRequestParseError               = ClientErrorCode(12)
  val CallbackNotRegistered               = ClientErrorCode(13)
  val NetModuleNotInit                    = ClientErrorCode(14)
  val InvalidConfig                       = ClientErrorCode(15)
  val CannotCreateRuntime                 = ClientErrorCode(16)
  val InvalidContextHandle                = ClientErrorCode(17)
  val CannotSerializeResult               = ClientErrorCode(18)
  val CannotSerializeError                = ClientErrorCode(19)
  val CannotConvertJsValueToJson          = ClientErrorCode(20)
  val CannotReceiveSpawnedResult          = ClientErrorCode(21)
  val SetTimerError                       = ClientErrorCode(22)
  val InvalidParams                       = ClientErrorCode(23)
  val ContractsAddressConversionFailed    = ClientErrorCode(24)
  val UnknownFunction                     = ClientErrorCode(25)
  val AppRequestError                     = ClientErrorCode(26)
  val NoSuchRequest                       = ClientErrorCode(27)
  val CanNotSendRequestResult             = ClientErrorCode(28)
  val CanNotReceiveRequestResult          = ClientErrorCode(29)
  val CanNotParseRequestResult            = ClientErrorCode(30)
  val UnexpectedCallbackResponse          = ClientErrorCode(31)
  val CanNotParseNumber                   = ClientErrorCode(32)
  val InternalError                       = ClientErrorCode(33)
}

object AbiErrors {
  val RequiredAddressMissingForEncodeMessage    = ClientErrorCode(301)
  val RequiredCallSetMissingForEncodeMessage    = ClientErrorCode(302)
  val InvalidJson                               = ClientErrorCode(303)
  val InvalidMessage                            = ClientErrorCode(304)
  val EncodeDeployMessageFailed                 = ClientErrorCode(305)
  val EncodeRunMessageFailed                    = ClientErrorCode(306)
  val AttachSignatureFailed                     = ClientErrorCode(307)
  val InvalidTvcImage                           = ClientErrorCode(308)
  val RequiredPublicKeyMissingForFunctionHeader = ClientErrorCode(309)
  val InvalidSigner                             = ClientErrorCode(310)
  val InvalidAbi                                = ClientErrorCode(311)
}

object BocErrors {
  val InvalidBoc         = ClientErrorCode(201)
  val SerializationError = ClientErrorCode(202)
  val InappropriateBlock = ClientErrorCode(203)
  val MissingSourceBoc   = ClientErrorCode(204)
}

object CryptoErrors {
  val InvalidPublicKey          = ClientErrorCode(100)
  val InvalidSecretKey          = ClientErrorCode(101)
  val InvalidKey                = ClientErrorCode(102)
  val InvalidFactorizeChallenge = ClientErrorCode(106)
  val InvalidBigInt             = ClientErrorCode(107)
  val ScryptFailed              = ClientErrorCode(108)
  val InvalidKeySize            = ClientErrorCode(109)
  val NaclSecretBoxFailed       = ClientErrorCode(110)
  val NaclBoxFailed             = ClientErrorCode(111)
  val NaclSignFailed            = ClientErrorCode(112)
  val Bip39InvalidEntropy       = ClientErrorCode(113)
  val Bip39InvalidPhrase        = ClientErrorCode(114)
  val Bip32InvalidKey           = ClientErrorCode(115)
  val Bip32InvalidDerivePath    = ClientErrorCode(116)
  val Bip39InvalidDictionary    = ClientErrorCode(117)
  val Bip39InvalidWordCount     = ClientErrorCode(118)
  val MnemonicGenerationFailed  = ClientErrorCode(119)
  val MnemonicFromEntropyFailed = ClientErrorCode(120)
  val SigningBoxNotRegistered   = ClientErrorCode(121)
}

object DebotErrors {
  val DebotStartFailed       = ClientErrorCode(801)
  val DebotFetchFailed       = ClientErrorCode(802)
  val DebotExecutionFailed   = ClientErrorCode(803)
  val DebotInvalidHandle     = ClientErrorCode(804)
  val DebotInvalidJsonParams = ClientErrorCode(805)
  val DebotInvalidFunctionId = ClientErrorCode(806)
  val DebotInvalidAbi        = ClientErrorCode(807)
  val DebotGetMethodFailed   = ClientErrorCode(808)
  val DebotInvalidMsg        = ClientErrorCode(809)
  val DebotExternaCallFailed = ClientErrorCode(810)
}

object NetErrors {
  val QueryFailed                 = ClientErrorCode(601L)
  val SubscribeFailed             = ClientErrorCode(602L)
  val WaitForFailed               = ClientErrorCode(603L)
  val GetSubscriptionResultFailed = ClientErrorCode(604L)
  val InvalidServerResponse       = ClientErrorCode(605L)
  val ClockOutOfSync              = ClientErrorCode(606L)
  val WaitForTimeout              = ClientErrorCode(607L)
  val GraphqlError                = ClientErrorCode(608L)
  val NetworkModuleSuspended      = ClientErrorCode(609L)
  val WebsocketDisconnected       = ClientErrorCode(610L)
  val NotSupported                = ClientErrorCode(611L)
  val NoEndpointsProvided         = ClientErrorCode(612L)
}

object ProcessingErrors {
  val MessageAlreadyExpired           = ClientErrorCode(501)
  val MessageHasNotDestinationAddress = ClientErrorCode(502)
  val CanNotBuildMessageCell          = ClientErrorCode(503)
  val FetchBlockFailed                = ClientErrorCode(504)
  val SendMessageFailed               = ClientErrorCode(505)
  val InvalidMessageBoc               = ClientErrorCode(506)
  val MessageExpired                  = ClientErrorCode(507)
  val TransactionWaitTimeout          = ClientErrorCode(508)
  val InvalidBlockReceived            = ClientErrorCode(509)
  val CanNotCheckBlockShard           = ClientErrorCode(510)
  val BlockNotFound                   = ClientErrorCode(511)
  val InvalidData                     = ClientErrorCode(512)
  val ExternalSignerMustNotBeUsed     = ClientErrorCode(513)
}

object TvmErrors {
  val CanNotReadTransaction      = ClientErrorCode(401)
  val CanNotReadBlockchainConfig = ClientErrorCode(402)
  val TransactionAborted         = ClientErrorCode(403)
  val InternalError              = ClientErrorCode(404)
  val ActionPhaseFailed          = ClientErrorCode(405)
  val AccountCodeMissing         = ClientErrorCode(406)
  val LowBalance                 = ClientErrorCode(407)
  val AccountFrozenOrDeleted     = ClientErrorCode(408)
  val AccountMissing             = ClientErrorCode(409)
  val UnknownExecutionError      = ClientErrorCode(410)
  val InvalidInputStack          = ClientErrorCode(411)
  val InvalidAccountBoc          = ClientErrorCode(412)
  val InvalidMessageType         = ClientErrorCode(413)
  val ContractExecutionError     = ClientErrorCode(414)
}
