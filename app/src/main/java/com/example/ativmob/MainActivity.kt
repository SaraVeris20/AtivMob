@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ativmob

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ativmob.ui.theme.AtivMobTheme
import com.google.android.gms.location.LocationServices
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// Accompanist permissions (Compose) - adicione a dependência no build.gradle:
// implementation "com.google.accompanist:accompanist-permissions:<version>"
import com.google.accompanist.permissions.*

/** ---------- Modelos de Dados ---------- **/
@Serializable
data class Item(
    val id: Int,
    val name: String,
    val description: String
)

@Serializable
data class Estado(
    val id: Int,
    val sigla: String,
    val nome: String,
    val regiao: RegiaoEstado
)

@Serializable
data class RegiaoEstado(
    val id: Int,
    val sigla: String,
    val nome: String
)

/** ---------- UiState ---------- **/
data class UiState(
    val greeting: String = "Visitante",
    val location: Location? = null,
    val estados: List<Estado> = emptyList(),
    val isLoadingGreeting: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val isLoadingStates: Boolean = false,
    val error: String? = null
)

/** ---------- ViewModel ---------- **/
class MainViewModel : ViewModel() {
    private val _uiState = MutableLiveData(UiState())
    val uiState: LiveData<UiState> = _uiState

    // Cliente Ktor para chamadas de API
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json()
        }
    }

    // Dados de exemplo para a lista embutida
    val allItems: List<Item> = List(20) {
        Item(it + 1, "Produto ${it + 1}", "Descrição do produto número ${it + 1}")
    }

    fun updateGreeting(name: String) {
        _uiState.value = _uiState.value?.copy(greeting = name)
    }

    fun fetchStatesData() {
        if (_uiState.value?.isLoadingStates == true || _uiState.value?.estados?.isNotEmpty() == true) return

        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoadingStates = true, error = null)
            try {
                val states: List<Estado> = client.get("https://servicodados.ibge.gov.br/api/v1/localidades/estados").body()
                _uiState.value = _uiState.value?.copy(estados = states.sortedBy { it.nome }, isLoadingStates = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(error = "Falha ao carregar estados: ${e.message}", isLoadingStates = false)
            }
        }
    }

    @SuppressLint("MissingPermission") // A permissão é verificada no Composable antes de chamar
    fun updateLocation(location: Location?) {
        _uiState.value = _uiState.value?.copy(location = location, isLoadingLocation = false)
    }

    fun setLoadingLocation(isLoading: Boolean) {
        _uiState.value = _uiState.value?.copy(isLoadingLocation = isLoading)
    }
}

/** ---------- Activity Principal ---------- **/
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AtivMobTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel, allItems = viewModel.allItems)
                }
            }
        }
    }
}

/** ---------- Navegação ---------- **/
@Composable
fun AppNavigation(viewModel: MainViewModel, allItems: List<Item>) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(viewModel = viewModel, navController = navController)
        }
        composable("location") {
            LocationScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable("estados") {
            EstadosScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable("lista_embutida") {
            ListaEmbutidaScreen(allItems = allItems, onNavigateBack = { navController.popBackStack() })
        }
    }
}

/** ---------- Telas (Composables) ---------- **/
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.observeAsState(initial = UiState())
    var nameInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Olá, ${uiState.greeting}!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bem-vindo ao AtivMob",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Digite seu nome") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                if (nameInput.isNotBlank()) {
                    viewModel.updateGreeting(nameInput)
                    nameInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Atualizar Saudação")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Funcionalidades Disponíveis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(onClick = { navController.navigate("location") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Ver Localização GPS", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver Localização GPS")
                }

                Button(
                    onClick = { navController.navigate("estados") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Estados Brasileiros (IBGE)")
                }

                Button(onClick = { navController.navigate("lista_embutida") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.List, contentDescription = "Lista de Produtos", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lista de Produtos")
                }
            }
        }

        if (uiState.isLoadingStates || uiState.estados.isNotEmpty() || uiState.error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        uiState.isLoadingStates -> {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Carregando dados do IBGE...")
                        }
                        uiState.error != null && uiState.estados.isEmpty() -> {
                            Text("⚠️ Erro ao carregar dados: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                        }
                        uiState.estados.isNotEmpty() -> {
                            Text("✅ ${uiState.estados.size} estados carregados para consulta.")
                        }
                    }
                }
            }
        }
    }
}

/** ---------- Localização: usa accompanist-permissions ---------- **/
@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun LocationScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.observeAsState(initial = UiState())
    val context = LocalContext.current

    // permissions state via accompanist
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun requestLocationOnce() {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.setLoadingLocation(true)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    viewModel.updateLocation(location)
                }
                .addOnFailureListener {
                    viewModel.updateLocation(null)
                }
        }
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            requestLocationOnce()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Localização Atual") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoadingLocation) {
                CircularProgressIndicator()
                Text("Obtendo localização...", modifier = Modifier.padding(top = 8.dp))
            } else {
                if (locationPermissionsState.allPermissionsGranted) {
                    Text(
                        text = if (uiState.location != null)
                            "Latitude: ${uiState.location.latitude}\nLongitude: ${uiState.location.longitude}"
                        else
                            "Localização não disponível.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { requestLocationOnce() }) {
                        Text("Atualizar Localização")
                    }
                } else {
                    Text(
                        "Permissão de localização necessária para mostrar sua posição.",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { locationPermissionsState.launchMultiplePermissionRequest() }) {
                        Text("Conceder Permissão")
                    }
                }
            }
        }
    }
}

/** ---------- Estados Screen ---------- **/
@Composable
fun EstadosScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.observeAsState(initial = UiState())

    LaunchedEffect(Unit) {
        if (uiState.estados.isEmpty() && !uiState.isLoadingStates) {
            viewModel.fetchStatesData()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estados Brasileiros (IBGE)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when {
                uiState.isLoadingStates -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text("Carregando estados...", modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
                uiState.error != null && uiState.estados.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Erro: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                uiState.estados.isNotEmpty() -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = uiState.estados, key = { it.id }) { estado ->
                            EstadoItem(estado = estado)
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum estado para exibir.", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun EstadoItem(estado: Estado) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "${estado.sigla} - ${estado.nome}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Região: ${estado.regiao.nome}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** ---------- Lista Embutida ---------- **/
@Composable
fun ListaEmbutidaScreen(allItems: List<Item>, onNavigateBack: () -> Unit) {
    var filterText by remember { mutableStateOf("") }
    val displayedItems = remember(allItems, filterText) {
        if (filterText.isBlank()) {
            allItems
        } else {
            allItems.filter {
                it.name.contains(filterText, ignoreCase = true) ||
                        it.description.contains(filterText, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Produtos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = filterText,
                onValueChange = { filterText = it },
                label = { Text("Filtrar por nome ou descrição") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = "Mostrando ${displayedItems.size} de ${allItems.size} produtos",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (displayedItems.isEmpty() && filterText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhum produto encontrado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tente uma busca diferente para \"$filterText\"",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (displayedItems.isEmpty() && filterText.isBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "ℹ️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhum produto na lista",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = displayedItems, key = { it.id }) { item ->
                        ProdutoItem(item = item)
                    }
                }
            }
        }
    }
}

@Composable
fun ProdutoItem(item: Item) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.id.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** ---------- Previews ---------- **/
@Preview(showBackground = true, name = "Home Screen Preview")
@Composable
fun HomeScreenPreview() {
    // Preview com UiState fixo (não instanciaremos o ViewModel real no preview)
    val previewUiState = UiState(greeting = "Preview User")
    val fakeItems = List(5) { Item(it + 1, "Produto ${it + 1}", "Descrição de exemplo ${it + 1}") }

    AtivMobTheme {
        // Usamos um NavController de preview mínimo
        HomeScreenForPreview(uiState = previewUiState, allItems = fakeItems)
    }
}

@Composable
private fun HomeScreenForPreview(uiState: UiState, allItems: List<Item>) {
    // Uma versão simplificada do HomeScreen apenas para preview visual
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Olá, ${uiState.greeting}!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Bem-vindo ao AtivMob", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview(showBackground = true, name = "Lista Embutida Preview")
@Composable
fun ListaEmbutidaScreenPreview() {
    val sampleItems = List(5) { Item(it + 1, "Produto de Exemplo ${it + 1}", "Esta é uma descrição de exemplo.") }
    AtivMobTheme {
        ListaEmbutidaScreen(allItems = sampleItems, onNavigateBack = {})
    }
}

@Preview(showBackground = true, name = "Estados Screen Preview (Loading)")
@Composable
fun EstadosScreenLoadingPreview() {
    // Preview visual simples
    val loadingUiState = UiState(isLoadingStates = true)
    AtivMobTheme {
        // Mostramos apenas um box indicativo
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Carregando estados...")
            }
        }
    }
}
