package com.gonodono.smssender

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gonodono.smssender.sms.SmsPermissions
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URI

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val defaultHost = "http://95.85.121.153:5010"
    private val eventName = "onMessage"
    private var mSocket: Socket? = null

    private val defaultHost2 = "http://95.85.121.153:5010"
    private val eventName2 = "onMessage"
    private var mSocket2: Socket? = null

    // This will just end up blank if the permissions aren't granted.
    override fun onCreate(savedInstanceState: Bundle?) {
        val request =
            registerForActivityResult(RequestMultiplePermissions()) { grants ->
                if (grants.all { it.value }) setUpUi()
            }

        super.onCreate(savedInstanceState)

        val permissions = SmsPermissions
        if (permissions.any { checkSelfPermission(it) != PERMISSION_GRANTED }) {
            request.launch(permissions)
        } else {
            createSocket()
            connectWebSocket()
            setUpUi()
        }
    }

    private fun createSocket() {
        try {
            val options = IO.Options()
            options.forceNew = false
            options.multiplex = true
            options.transports = arrayOf(WebSocket.NAME)
            options.upgrade = true
            options.rememberUpgrade = false
            options.query = null
            options.reconnection = true
            options.reconnectionAttempts = Int.MAX_VALUE
            options.reconnectionDelay = 1000
            options.reconnectionDelayMax = 5000
            options.randomizationFactor = 0.5
            options.timeout = 20000


            mSocket = IO.socket(URI.create(defaultHost), options)
            mSocket2 = IO.socket(URI.create(defaultHost2), options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun disconnect() {
        mSocket!!.disconnect()
        mSocket!!.off(Socket.EVENT_CONNECT_ERROR, onConnectError)
        mSocket!!.off(Socket.EVENT_CONNECT, onConnect)
        mSocket!!.off(Socket.EVENT_DISCONNECT, onDisconnect)
        mSocket!!.off(eventName, onNewMessage)

        mSocket2!!.disconnect()
        mSocket2!!.off(Socket.EVENT_CONNECT_ERROR, onConnectError)
        mSocket2!!.off(Socket.EVENT_CONNECT, onConnect)
        mSocket2!!.off(Socket.EVENT_DISCONNECT, onDisconnect)
        mSocket2!!.off(eventName2, onNewMessage2)
    }

    private fun connectWebSocket() {
        mSocket!!.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
        mSocket!!.on(Socket.EVENT_CONNECT, onConnect)
        mSocket!!.on(Socket.EVENT_DISCONNECT, onDisconnect)
        mSocket!!.on(eventName, onNewMessage)
        mSocket!!.connect()

        mSocket2!!.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
        mSocket2!!.on(Socket.EVENT_CONNECT, onConnect)
        mSocket2!!.on(Socket.EVENT_DISCONNECT, onDisconnect)
        mSocket2!!.on(eventName2, onNewMessage2)
        mSocket2!!.connect()
    }

    var onConnect = Emitter.Listener {
        Log.d("TAG", "Socket Connected!")
        try {
            mSocket!!.off(eventName, onNewMessage)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        mSocket!!.emit("joinRoom", "otp_room")
        mSocket!!.on(eventName, onNewMessage)
        setStatus("Connected")
    }

    private val onConnectError =
        Emitter.Listener { args ->
            setStatus("Connection error!")
        }
    private val onDisconnect = Emitter.Listener {
        setStatus("Disconnected")
    }

    private fun setStatus(status: String) {
        val viewModel: MainViewModel by viewModels()
        Log.e("Status", status)
        viewModel.changeStatus(status)
    }

    private val onNewMessage =
        Emitter.Listener { args ->
            try {
                val viewModel: MainViewModel by viewModels()
//                val s = args[0].toString()

                Log.e("VALUE", args[0].toString())
                val obj = args[0] as JSONObject
//                val gson = Gson()
//                                String res= (String) args[0];
//                                String[] s=res.split(",");
                viewModel.sendSms(obj.getString("number"), obj.getString("text"))
//                                JSONObject obj = (JSONObject) args[0];
//                Gson gson = new Gson();
//                String res= (String) args[0];
//                val ss: Array<String> =
//                    s.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//                viewModel.sendSms(ss[0], ss[1])
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }


    private val onNewMessage2 =
        Emitter.Listener { args ->
            try {
                val viewModel: MainViewModel by viewModels()
//                val s = args[0].toString()

                val obj = args[0] as JSONObject
//                val gson = Gson()
//                                String res= (String) args[0];
//                                String[] s=res.split(",");
                viewModel.sendSms(obj.getString("phoneNumber"), obj.getString("code"))
//                                JSONObject obj = (JSONObject) args[0];
//                Gson gson = new Gson();
//                String res= (String) args[0];
//                val ss: Array<String> =
//                    s.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//                viewModel.sendSms(ss[0], ss[1])
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

    private fun setUpUi() {
        val viewModel: MainViewModel by viewModels()
        var uiState: UiState by mutableStateOf(UiState.Loading)
        lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(lifecycle)
                .onEach { uiState = it }
                .collect()

        }

//        lifecycleScope.launch {
//            viewModel.status.flowWithLifecycle(lifecycle).onEach { status.value = it }.collect()
//        }
        setContent {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                val status = viewModel.status.collectAsState()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "OTP")
                    Text(text = status.value)
                }

                when (val state = uiState) {

                    is UiState.Active -> {
                        val msgs = state.messages.split("\n\n")
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(msgs.size) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF03045e), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(msgs[it], color = Color.White)
                                }
                            }
                        }

                        val info = when {
                            state.isSending -> "Sending…"
                            else -> state.lastError
                        }

                        if (info != null) {
                            Text(info, Modifier.padding(10.dp), Color.Red)
                        }

//                        TextButton({ viewModel.queueDemoMessagesAndSend() }) {
//                            Text("Queue messages & send")
//                        }

//                        TextButton({ viewModel.resetFailedAndRetry() }) {
//                            Text("Reset failed & retry")
//                        }
                    }

                    else -> Text("Loading…")
                }
                
//                Row(modifier = Modifier
//                    .fillMaxWidth()
//                    .background(Color.DarkGray)
//                    .padding(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center){
//                    Text(text = "Developed By: @shageldi-dev", color = Color.White, style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold))
//                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }
}