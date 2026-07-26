package com.example.fleetlookup.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    Scaffold(topBar = { TopAppBar(title = { Text("Fleet Lookup") }) }) { pad ->
        Column(modifier = Modifier.padding(pad).padding(16.dp)) {
            if (sel != null) {
                Detail(sel!!) {
                    vm.select(null)
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
                if (loading) CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                LazyColumn {
                    items(list) { v ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { vm.select(v) }) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                val liveryColor = remember(v.livery?.colour) {
                                    try {
                                        if (v.livery?.colour != null) Color(android.graphics.Color.parseColor(v.livery.colour)) else null
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                                
                                if (liveryColor != null) {
                                    Box(
                                        Modifier
                                            .width(6.dp)
                                            .height(32.dp)
                                            .background(liveryColor, RoundedCornerShape(3.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }
                                
                                Column {
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
}

@Composable
fun Detail(v: Vehicle, onBack: () -> Unit) {
    val ctx = LocalContext.current
    Column {
        TextButton(onClick = onBack) { Text("← Back") }
        Text(v.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        RowText("Operator", v.operator?.name)
        RowText("Type", v.vehicleType?.name)
        RowText("Fuel", v.vehicleType?.fuel)
        v.livery?.let { LiveryRow(it) }
        RowText("Garage", v.garage?.name)
        RowText("Branding", v.branding?.takeIf { it.isNotBlank() })
        v.specialFeatures?.takeIf { it.isNotEmpty() }?.let {
            RowText("Features", it.joinToString(", "))
        }

        Spacer(Modifier.height(16.dp))
        Text("External Links", style = MaterialTheme.typography.titleMedium)
        
        val term = v.reg ?: v.fleetCode ?: ""
        
        Button(
            onClick = {
                val url = if (v.slug != null) {
                    "https://bustimes.org/vehicles/${v.slug}"
                } else {
                    "https://bustimes.org/search?q=${Uri.encode(term)}"
                }
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("View on bustimes.org")
        }

        OutlinedButton(
            onClick = {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flickr.com/search/?text=${Uri.encode(term)}")))
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Search photos on Flickr")
        }
    }
}

@Composable
fun LiveryRow(livery: com.example.fleetlookup.model.Livery) {
    if (livery.name.isNullOrBlank()) return
    val color = remember(livery.colour) {
        try {
            if (livery.colour != null) Color(android.graphics.Color.parseColor(livery.colour)) else null
        } catch (_: Exception) {
            null
        }
    }
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Livery: ", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(90.dp))
        if (color != null) {
            Box(
                Modifier
                    .size(16.dp)
                    .background(color, RoundedCornerShape(4.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(livery.name)
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
