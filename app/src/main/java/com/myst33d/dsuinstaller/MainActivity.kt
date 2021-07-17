package com.myst33d.dsuinstaller

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.myst33d.dsuinstaller.ui.theme.BluePrimary
import com.myst33d.dsuinstaller.ui.theme.DSUInstallerTheme
import com.myst33d.dsuinstaller.ui.theme.TitleText
import kotlinx.coroutines.*
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.util.*

class MainActivity : ComponentActivity() {
    var dsuCallback: (String) -> Unit = {} // Dummy callback
    val daemonPort = 35503
    lateinit var daemon: Socket
    lateinit var inputStream: Scanner
    lateinit var outputStream: PrintWriter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filePicker =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    dsuCallback(it.data?.data.toString())
                }
            }

        setContent {
            DSUInstallerTheme {
                Surface {
                    window.statusBarColor = BluePrimary.hashCode()
                    var dataSize by rememberSaveable { mutableStateOf("8") }
                    var dsuPackage by rememberSaveable { mutableStateOf("") }
                    var showDaemonDialog by rememberSaveable { mutableStateOf(false) }

                    val dataSizeValid = dataSize.toLongOrNull() != null
                    val dsuPackageValid = dsuPackage != "" && dsuPackage.startsWith("content://")

                    dsuCallback = { dsuPackage = it }

                    initializeDaemonClient { showDaemonDialog = it }

                    if (showDaemonDialog) {
                        AlertDialog(
                            onDismissRequest = { finish() },
                            title = { Text("Oops!") },
                            text = { Text("Seems like DSUDaemon is not running, please launch the DSUDaemon and restart the app.") },
                            buttons = {
                                Row(
                                    modifier = Modifier
                                        .padding(bottom = 16.dp, end = 16.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(onClick = { finish() }) {
                                        Text("Exit")
                                    }
                                }
                            }
                        )
                    }

                    Column {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .padding(top = 16.dp, bottom = 16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                modifier = Modifier.height(64.dp),
                                painter = painterResource(R.drawable.dsu_installer),
                                contentDescription = null
                            )
                            Text("DSU Installer", style = TitleText)
                        }

                        Column(
                            modifier = Modifier
                                .padding(start = 32.dp, end = 32.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = dataSize,
                                onValueChange = { dataSize = it },
                                isError = !dataSizeValid,
                                label = {
                                    val label =
                                        if (dataSizeValid) "Data size in GB" else "Data size in GB*"
                                    Text(label)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row {
                                OutlinedTextField(
                                    value = dsuPackage,
                                    onValueChange = { dsuPackage = it },
                                    isError = !dsuPackageValid,
                                    label = {
                                        val label =
                                            if (dsuPackageValid) "DSU package" else "DSU package*"
                                        Text(label)
                                    },
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            type = "application/zip"
                                        }

                                        filePicker.launch(intent)
                                    }, modifier = Modifier
                                        .width(80.dp)
                                        .padding(top = 8.dp)
                                ) {
                                    Text("Select")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Column(
                            modifier = Modifier
                                .padding(32.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Button(onClick = {
                                if (dsuPackageValid && dataSizeValid) {
                                    if (!daemon.isConnected) {
                                        showDaemonDialog = true
                                    } else {
                                        val finalDataSize = dataSize.toLong() * 1024 * 1024 * 1024
                                        GlobalScope.launch(Dispatchers.IO) {
                                            outputStream.write("flash_dsu_package|$dsuPackage|$finalDataSize\n")
                                            outputStream.flush()
                                        }
                                    }
                                }
                            }, modifier = Modifier.width(80.dp)) {
                                Text("Install")
                            }
                        }
                    }
                }
            }
        }
    }

    fun initializeDaemonClient(callback: (Boolean) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                daemon = Socket("127.0.0.1", daemonPort)
                inputStream = Scanner(daemon.inputStream)
                outputStream = PrintWriter(daemon.outputStream)

                if (!daemon.isConnected) {
                    withContext(Dispatchers.Main) {
                        callback(true)
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    callback(true)
                }
            }
        }
    }
}