package com.example.fleetlookup.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fleetlookup.model.Vehicle
import com.example.fleetlookup.network.BustimesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FleetVm : ViewModel() {
    private val api = BustimesApi.create()
    private val _q = MutableStateFlow("")
    val q = _q.asStateFlow()
    private val _list = MutableStateFlow<List<Vehicle>>(emptyList())
    val list = _list.asStateFlow()
    private val _sel = MutableStateFlow<Vehicle?>(null)
    val sel = _sel.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    fun setQ(s: String) { _q.value = s }
    fun select(v: Vehicle?) { _sel.value = v }
    fun search() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _list.value = api.search(_q.value.trim()).results
            } catch (_: Exception) {
                _list.value = emptyList()
            }
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: FleetVm = viewModel()) {
    val q by vm.q.collectAsState()
    val list by vm.list.collectAsState()
    val sel by vm.sel.collectAsState()
    val loading by vm.loading.collectAsState()
    val ctx = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("Fleet Lookup") }) }) { pad ->
        Column(Modifier = Modifier.padding(pad).padding(16.dp)) {
            if (sel != null) {
                Detail(sel!!) {
                    vm.select(null)
                }
                Button(onClick = {
                    val term = sel!!.fleetCode ?: sel!!.reg ?: ""
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wmbusphotos.com/forum/index.php?action=search2&search=$term")))
                }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text("Search on WM Bus Photos Forum")
                }
            } else {
                OutlinedTextField(
                    value = q,
                    onValueChange = vm::setQ,
                    label = { Text("Fleet number or registration") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = vm::search) {
                            Icon(Icons.Default.Search, null)
                        }
                    }
                )
                if (loading) CircularProgressIndicator(Modifier.padding(16.dp))
                LazyColumn {
                    items(list) { v ->
                        Card(Modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { vm.select(v) }) {
                            Column(Modifier.padding(12.dp)) {
                                Text(v.title, fontWeight = FontWeight.Bold)
                                Text("${v.operator?.name ?: ""} • ${v.vehicleType?.name ?: ""}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Detail(v: Vehicle, onBack: () -> Unit) {
    Column {
        TextButton(onClick = onBack) { Text("← Back") }
        Text(v.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        RowText("Operator", v.operator?.name)
        RowText("Type", v.vehicleType?.name)
        RowText("Fuel", v.vehicleType?.fuel)
        RowText("Livery", v.livery?.name)
        RowText("Garage", v.garage?.name)
        RowText("Branding", v.branding?.takeIf { it.isNotBlank() })
        v.specialFeatures?.takeIf { it.isNotEmpty() }?.let {
            RowText("Features", it.joinToString(", "))
        }
    }
}

@Composable
fun RowText(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(90.dp))
        Text(value)
    }
}
