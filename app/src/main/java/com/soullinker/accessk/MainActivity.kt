package com.soullinker.accessk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { Greeting("Access-k") }
      }
    }
  }
}

@Composable fun Greeting(text: String) { Text("Hello $text") }
@Preview @Composable fun PreviewGreeting() { Greeting("Preview") }
