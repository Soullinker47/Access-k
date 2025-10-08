import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.*
import java.io.BufferedReader
import java.io.InputStreamReader

class CozeApiClient(
  private val client: OkHttpClient = OkHttpClient.Builder().build(),
  private val json: Json = Json { ignoreUnknownKeys = true }
) {
  private val base = BuildConfig.COZE_PROXY_BASE.trimEnd('/')

  suspend fun createConversation(botId: String = BuildConfig.COZE_BOT_ID): String? =
    withContext(Dispatchers.IO) {
      val body = RequestBody.create(
        MediaType.get("application/json"),
        json.encodeToString(CreateConvRequest.serializer(), CreateConvRequest(botId))
      )
      val req = Request.Builder()
        .url("$base/v1/conversation/create")
        .post(body)
        .build()
      client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) return@withContext null
        val txt = resp.body?.string().orEmpty()
        runCatching { json.decodeFromString(CreateConvResp.serializer(), txt).id }.getOrNull()
      }
    }

  fun streamChat(
    content: String,
    userId: String,
    conversationId: String? = null,
    botId: String = BuildConfig.COZE_BOT_ID
  ): Flow<String> = channelFlow {
    val reqObj = ChatRequest(
      bot_id = botId,
      user_id = userId,
      additional_messages = listOf(AdditionalMessage(content = content)),
      stream = true
    )
    val body = RequestBody.create(
      MediaType.get("application/json"),
      json.encodeToString(ChatRequest.serializer(), reqObj)
    )
    val url = if (conversationId.isNullOrBlank())
      "$base/v3/chat"
    else
      "$base/v3/chat?conversation_id=$conversationId"

    val req = Request.Builder()
      .url(url)
      .post(body)
      .header("Accept", "text/event-stream")
      .build()

    val call = client.newCall(req)
    val resp = call.execute()
    if (!resp.isSuccessful) {
      close(IllegalStateException("HTTP ${resp.code}"))
      return@channelFlow
    }
    val reader = BufferedReader(InputStreamReader(resp.body!!.byteStream()))
    var line: String?
    while (reader.readLine().also { line = it } != null) {
      val l = line!!
      if (!l.startsWith("data:")) continue
      val jsonStr = l.removePrefix("data:").trim()
      if (jsonStr.isBlank()) continue
      val evt = runCatching { json.decodeFromString(CozeEvent.serializer(), jsonStr) }.getOrNull()
      val piece = when (evt?.type) {
        "message" -> evt.data?.content
        "chat" -> null // 可根据 status 显示状态
        "completed" -> {
          // completed 里通常给出最终消息，若 messages 数组有多条，拼接展示
          evt.data?.messages?.joinToString(separator = "\n") { it.content.orEmpty() }
        }
        else -> null
      }
      if (!piece.isNullOrEmpty()) trySend(piece)
    }
    resp.close()
    close()
  }
}
