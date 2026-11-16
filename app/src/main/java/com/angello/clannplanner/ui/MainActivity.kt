package com.angello.clannplanner.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.angello.clannplanner.data.MessageEntity
import com.angello.clannplanner.data.TaskEntity
import com.angello.clannplanner.data.UserEntity
import com.angello.clannplanner.ui.theme.ClannPlannerTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.BatteryChargingFull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClannPlannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ClanAppNavigator()
                }
            }
        }
    }
}

// esto es lo ultimo, cargar datos de demostracion
@Composable
fun ClanAppNavigator(viewModel: ClanViewModel = viewModel()) {
    var loggedUser by remember { mutableStateOf<UserEntity?>(null) }
    var currentScreen by remember { mutableStateOf("login") }

    when (currentScreen) {
        "login" -> LoginScreen(
            onLoginSuccess = { user ->
                loggedUser = user
                currentScreen = "main"
            },
            viewModel = viewModel
        )
        "main" -> loggedUser?.let { user ->
            ClanMainScreen(
                currentUser = user,
                viewModel = viewModel,
                onNavigateToProfile = { currentScreen = "profile" },
                onLogout = { currentScreen = "login" }
            )
        }
        "profile" -> loggedUser?.let { user ->
            ProfileScreen(
                currentUser = user,
                viewModel = viewModel,
                onBack = { updatedUser ->
                    loggedUser = updatedUser
                    currentScreen = "main"
                },
                onAccountDeleted = {
                    loggedUser = null
                    currentScreen = "login"
                }
            )
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (UserEntity) -> Unit, viewModel: ClanViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    // Estado para mostrar un aviso visual cuando se carguen los datos
    var mensajeDemo by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("🏠 ClanPlanner - Acceso", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre / Familiar") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo electrónico") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            viewModel.registerOrLoginUser(email, password, if (name.isBlank()) "Familiar" else name) { user ->
                                onLoginSuccess(user)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Entrar / Registrarse")
                }

                // 👇 BOTÓN DE DATOS DE DEMOSTRACIÓN PARA LOS PROFESORES
                Button(
                    onClick = {
                        viewModel.cargarDatosDemostracion()
                        mensajeDemo = true
                        // Opcional: Rellenar automáticamente los campos para entrar rápido
                        email = "ana@clann.com"
                        password = "123"
                        name = "Profesor Ana"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("🚀 Cargar Datos de Demostración")
                }

                if (mensajeDemo) {
                    Text(
                        "✔ ¡Datos cargados con éxito! Pulsa 'Entrar' para acceder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.resetDatabase() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("🧹 Vaciar Base de Datos (Empezar de cero)")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClanMainScreen(
    currentUser: UserEntity,
    viewModel: ClanViewModel,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val users by viewModel.users.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val messages by viewModel.messages.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Inicio/Batería, 1: Tareas, 2: Chat
    var showAddDialog by remember { mutableStateOf(false) }
    var taskTitle by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf(currentUser.id) }
    var messageText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🏠", style = MaterialTheme.typography.headlineMedium)
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                "ClanPlanner",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (currentUser.avatar.startsWith("content://") || currentUser.avatar.startsWith("file://")) {
                                    Surface(
                                        shape = CircleShape,
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        coil.compose.AsyncImage(
                                            model = currentUser.avatar,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                } else {
                                    Text(
                                        text = currentUser.avatar,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text = currentUser.nombre,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(onClick = {
                            selectedTab = if (selectedTab == 0) 1 else 0
                        }) {
                            Text(if (selectedTab == 0) "📑" else "🏠", style = MaterialTheme.typography.titleLarge)
                        }
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.Person, contentDescription = "Perfil", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        TextButton(onClick = onLogout) {
                            Text("Salir", color = MaterialTheme.colorScheme.error, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "⚡ Batería Social",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTab == 0) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "📋 Tareas (${tasks.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTab == 1) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "💬 Chat (${messages.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTab == 2) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Text("➕", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                // PESTAÑA 0: BATERÍA SOCIAL
                0 -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)); Text("⚡ Muro de Estados del Clan", style = MaterialTheme.typography.titleLarge) }

                        items(users) { user ->
                            UserCard(
                                user = user,
                                currentUser = currentUser,
                                onUpdate = { bat, status -> viewModel.updateUserStatus(user, bat, status) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(40.dp)) }
                    }
                }

                // PESTAÑA 1: TAREAS Y RANKING
                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)); Text("🏆 Ranking de Puntos del Clan", style = MaterialTheme.typography.titleLarge) }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val rankingUsers = users.sortedByDescending { it.puntos }
                                    rankingUsers.forEachIndexed { index, u ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val medalla = when(index) {
                                                0 -> "🥇"
                                                1 -> "🥈"
                                                2 -> "🥉"
                                                else -> "🏅"
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(medalla, style = MaterialTheme.typography.bodyMedium)
                                                if (u.avatar.startsWith("content://") || u.avatar.startsWith("file://")) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(20.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer
                                                    ) {
                                                        coil.compose.AsyncImage(
                                                            model = u.avatar,
                                                            contentDescription = "Avatar",
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                        )
                                                    }
                                                } else {
                                                    Text(text = u.avatar, style = MaterialTheme.typography.bodyMedium)
                                                }
                                                Text(u.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                            }
                                            Text("✨ ${u.puntos} pts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(4.dp)); Text("📋 Bandeja de Tareas Rápidas", style = MaterialTheme.typography.titleLarge) }

                        if (tasks.isEmpty()) {
                            item { Text("No hay tareas pendientes en el clan.", style = MaterialTheme.typography.bodyMedium) }
                        } else {
                            items(tasks) { task ->
                                val assignedUser = users.find { it.id == task.asignadoAUserId }
                                TaskCard(
                                    task = task,
                                    userAvatar = assignedUser?.avatar ?: "👤",
                                    userName = assignedUser?.nombre ?: "Sin asignar",
                                    onToggle = { viewModel.toggleTaskCompletion(task, users) },
                                    onDelete = { viewModel.deleteTask(task) }
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }

                // PESTAÑA 2: CHAT Y AVISOS
                2 -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text("💬 Chat y Avisos del Clan", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(messages) { message ->
                                val sender = users.find { it.id == message.remitenteId }
                                MessageBubble(
                                    message = message,
                                    senderAvatar = sender?.avatar ?: "👤",
                                    senderName = sender?.nombre ?: "Desconocido",
                                    onDelete = { viewModel.deleteMessage(message) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                placeholder = { Text("Escribe un aviso...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(onClick = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendMessage(currentUser.id, messageText)
                                    messageText = ""
                                }
                            }) {
                                Text("Enviar")
                            }
                        }
                    }
                }
            }

            // Scrollbar lateral (Solo visible en la pestaña de Batería Social)
            if (selectedTab == 0) {
                val scrollProgress by remember {
                    derivedStateOf {
                        val total = listState.layoutInfo.totalItemsCount
                        if (total > 0) listState.firstVisibleItemIndex.toFloat() / total.toFloat() else 0f
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .pointerInput(listState) {
                            detectDragGestures { change, _ ->
                                val offsetY = change.position.y
                                val trackHeight = size.height.toFloat()
                                if (trackHeight > 0f) {
                                    val progress = (offsetY / trackHeight).coerceIn(0f, 1f)
                                    val totalItems = listState.layoutInfo.totalItemsCount
                                    val targetItem = (progress * totalItems).toInt().coerceIn(0, totalItems - 1)
                                    coroutineScope.launch {
                                        listState.scrollToItem(targetItem)
                                    }
                                }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(60.dp)
                            .offset(y = (scrollProgress * 500).dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                    )
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Nueva Tarea") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = taskTitle, onValueChange = { taskTitle = it }, label = { Text("Título de tarea") })
                        Text("Asignar a:")
                        users.forEach { u ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedUserId == u.id, onClick = { selectedUserId = u.id })
                                Text(u.nombre)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (taskTitle.isNotBlank()) {
                            viewModel.addTask(taskTitle, selectedUserId)
                            taskTitle = ""
                            showAddDialog = false
                        }
                    }) { Text("Guardar") }
                },
                dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } }
            )
        }
    }
}

@Composable
fun ProfileScreen(
    currentUser: UserEntity,
    viewModel: ClanViewModel,
    onBack: (UserEntity) -> Unit,
    onAccountDeleted: () -> Unit
) {
    var name by remember { mutableStateOf(currentUser.nombre) }
    var password by remember { mutableStateOf(currentUser.password) }
    var selectedAvatar by remember { mutableStateOf(currentUser.avatar) }
    var selectedColorHex by remember { mutableStateOf(currentUser.colorHex) }

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            selectedAvatar = it.toString()
        }
    }

    val colorOptions = listOf("#6750A4", "#1E88E5", "#00897B", "#D81B60", "#8E24AA", "#F57C00", "#43A047", "#546E7A")

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
            Column(
                modifier = Modifier.padding(all = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⚙️ Ajustes de Perfil", style = MaterialTheme.typography.titleLarge)

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (selectedAvatar.startsWith("content://") || selectedAvatar.startsWith("file://")) {
                            coil.compose.AsyncImage(
                                model = selectedAvatar,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(text = selectedAvatar, style = MaterialTheme.typography.displayMedium)
                        }
                    }
                }

                OutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Text("🖼️ Elegir foto de la galería")
                }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth())

                Text("Color de tu tarjeta en el Muro:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorOptions) { hex ->
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                        Surface(
                            shape = CircleShape,
                            color = color,
                            modifier = Modifier
                                .size(36.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { selectedColorHex = hex }
                                },
                            border = if (selectedColorHex == hex) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null
                        ) {}
                    }
                }

                Button(
                    onClick = {
                        val updated = currentUser.copy(
                            nombre = name,
                            password = password,
                            avatar = selectedAvatar,
                            colorHex = selectedColorHex
                        )
                        viewModel.updateUser(updated)
                        onBack(updated)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar Cambios")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.deleteUser(currentUser)
                        onAccountDeleted()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("🗑️ Eliminar mi cuenta de ClanPlanner")
                }
            }
        }
    }
}

@Composable
fun UserCard(user: UserEntity, currentUser: UserEntity, onUpdate: (Int, String) -> Unit) {
    val isMyCard = (user.email.trim().lowercase() == currentUser.email.trim().lowercase())

    val levels = listOf(0, 50, 75, 100)
    val bateriaActual = user.bateria
    val currentLevel = levels.minByOrNull { kotlin.math.abs(it - bateriaActual) } ?: 100

    val (colorState, meaning) = when (currentLevel) {
        0 -> Pair(Color.Red, "🔴 Rojo: NECESITO MI ESPACIO")
        50 -> Pair(Color(0xFFFF8C00), "🟠 Naranja: NECESITO DESCANSAR")
        75 -> Pair(Color(0xFFFFD700), "🟡 Amarillo: ESTOY DISPONIBLE")
        else -> Pair(Color(0xFF4CAF50), "🟢 Verde: ESTOY A TOPE")
    }

    val userCardColor = try {
        Color(android.graphics.Color.parseColor(user.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val locations = listOf(
        "🏠 En casa", "💪 En el gym", "🏫 En la escuela",
        "🚶 En la calle", "💼 Trabajando", "📍 Otra ubicación"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = userCardColor.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = userCardColor.copy(alpha = 0.25f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (user.avatar.startsWith("content://") || user.avatar.startsWith("file://")) {
                                coil.compose.AsyncImage(
                                    model = user.avatar,
                                    contentDescription = "Avatar de usuario",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = user.avatar,
                                    style = MaterialTheme.typography.displayMedium
                                )
                            }
                        }
                    }
                    Text(
                        text = user.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Usamos el icono oficial de Material Design que sí acepta el color dinámico
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull, // O Icons.Default.BatteryFull según prefieras
                        contentDescription = "Nivel de batería",
                        tint = colorState, // 👈 Aquí cambia automáticamente a rojo, naranja, amarillo o verde
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "$bateriaActual%",
                        color = colorState,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = colorState.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorState,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Nivel de Batería Social:", style = MaterialTheme.typography.labelMedium)

            Slider(
                value = bateriaActual.toFloat(),
                onValueChange = { newValue ->
                    if (isMyCard) {
                        val closest = levels.minByOrNull { kotlin.math.abs(it - newValue.toInt()) } ?: 100
                        onUpdate(closest, user.estadoTexto)
                    }
                },
                enabled = isMyCard,
                valueRange = 0f..100f,
                steps = 2
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text("Ubicación actual: ${user.estadoTexto}", style = MaterialTheme.typography.bodyMedium, color = userCardColor)

            if (isMyCard) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Seleccionar ubicación:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        locations.take(3).forEach { loc ->
                            Button(
                                onClick = { onUpdate(bateriaActual, loc) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (user.estadoTexto == loc) userCardColor else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (user.estadoTexto == loc) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text(loc, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        locations.drop(3).forEach { loc ->
                            Button(
                                onClick = { onUpdate(bateriaActual, loc) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (user.estadoTexto == loc) userCardColor else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (user.estadoTexto == loc) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text(loc, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(task: TaskEntity, userAvatar: String, userName: String, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completada) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = task.titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "👤 Miembro:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

                    if (userAvatar.startsWith("content://") || userAvatar.startsWith("file://")) {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            coil.compose.AsyncImage(
                                model = userAvatar,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    } else {
                        Text(text = userAvatar, style = MaterialTheme.typography.bodySmall)
                    }

                    Text(
                        text = userName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    color = if (task.completada) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFFF8C00).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (task.completada) "✔ Tarea completada" else "⏳ Tarea pendiente",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (task.completada) Color(0xFF2E7D32) else Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.completada, onCheckedChange = { onToggle() })
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar tarea", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageEntity, senderAvatar: String, senderName: String, onDelete: () -> Unit) {
    val dateString = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (senderAvatar.startsWith("content://") || senderAvatar.startsWith("file://")) {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            coil.compose.AsyncImage(
                                model = senderAvatar,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    } else {
                        Text(text = senderAvatar, style = MaterialTheme.typography.labelMedium)
                    }

                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dateString, style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar mensaje", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(message.texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}