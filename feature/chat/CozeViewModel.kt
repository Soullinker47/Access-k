import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CozeViewModel(
  private val api: CozeApiClient = CozeApiClient()
): ViewModel() {

  private val _conversationId = MutableStateFlow<String?>(null)
  val conversationId: StateFlow<String?> = _conversationId

  private val _output = MutableStateFlow("")
  val output: StateFlow<String> = _output

  fun ensureConversation() {
    viewModelScope.launch {
      if (_conversationId.value == null) {
        _conversationId.value = api.createConversation()
      }
    }
  }

  fun send(content: String, userId: String = "u_local") {
    ensureConversation()
    viewModelScope.launch {
      api.streamChat(
        content = content,
        userId = userId,
        conversationId = _conversationId.value
      ).collect { chunk ->
        _output.value = if (_output.value.isEmpty()) chunk else _output.value + chunk
      }
    }
  }

  fun clearOutput() { _output.value = "" }
}
