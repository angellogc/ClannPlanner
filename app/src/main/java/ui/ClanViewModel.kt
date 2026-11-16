package com.angello.clannplanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.angello.clannplanner.data.AppDatabase
import com.angello.clannplanner.data.ClanRepository
import com.angello.clannplanner.data.MessageEntity
import com.angello.clannplanner.data.TaskEntity
import com.angello.clannplanner.data.UserEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ClanRepository

    init {
        val userDao = AppDatabase.getDatabase(application).userDao()
        val taskDao = AppDatabase.getDatabase(application).taskDao()
        val messageDao = AppDatabase.getDatabase(application).messageDao()
        repository = ClanRepository(userDao, taskDao, messageDao)
    }

    // Lista de usuarios reactiva para el Muro de Estados
    val users: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Lista de tareas reactiva para la Bandeja de Tareas
    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Lista de mensajes reactiva para el chat del clan
    val messages: StateFlow<List<MessageEntity>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Actualizar batería y estado de un miembro del clan (Única versión unificada)
    fun updateUserStatus(user: UserEntity, newBattery: Int, newStatus: String) {
        viewModelScope.launch {
            val updated = user.copy(bateria = newBattery, estadoTexto = newStatus)
            repository.updateUser(updated)
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    fun updateUser(user: UserEntity) {
        viewModelScope.launch {
            repository.updateUser(user)
        }
    }

    // Eliminar la base de datos
    fun resetDatabase() {
        viewModelScope.launch {
            repository.clearDatabase()
        }
    }

    // Añadir una tarea nueva a la bandeja
    fun addTask(title: String, assignedUserId: Long) {
        viewModelScope.launch {
            repository.insertTask(TaskEntity(titulo = title, asignadoAUserId = assignedUserId))
        }
    }

    // Marcar tarea como completada o pendiente
    // Marcar tarea como completada o pendiente y actualizar puntos del miembro
    fun toggleTaskCompletion(task: TaskEntity, usersList: List<UserEntity>) {
        viewModelScope.launch {
            val nuevoEstado = !task.completada
            repository.updateTask(task.copy(completada = nuevoEstado))

            // Si hay un usuario asignado, actualizamos sus puntos (+10 al completar, -10 al desmarcar)
            usersList.find { it.id == task.asignadoAUserId }?.let { user ->
                val puntosActuales = user.puntos
                val nuevosPuntos = if (nuevoEstado) puntosActuales + 10 else maxOf(0, puntosActuales - 10)
                repository.updateUser(user.copy(puntos = nuevosPuntos))
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // Enviar un mensaje nuevo en el chat
    fun sendMessage(senderId: Long, text: String) {
        if (text.isNotBlank()) {
            viewModelScope.launch {
                repository.insertMessage(
                    MessageEntity(
                        remitenteId = senderId,
                        texto = text,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun deleteMessage(message: MessageEntity) {
        viewModelScope.launch {
            repository.deleteMessage(message)
        }
    }
    // esto es lo ultimo de cargar datos de demostracion
    // En ClanViewModel.kt
    fun cargarDatosDemostracion() {
        viewModelScope.launch {
            // 1. Crear usuarios de prueba si no existen
            val user1 = UserEntity(nombre = "Profesor Ana", email = "ana@clann.com", password = "123", avatar = "👩‍🏫", puntos = 120, bateria = 100, estadoTexto = "En casa", colorHex = "#1E88E5")
            val user2 = UserEntity(nombre = "Carlos (Alumno)", email = "carlos@clann.com", password = "123", avatar = "👦", puntos = 85, bateria = 50, estadoTexto = "Trabajando", colorHex = "#D81B60")
            val user3 = UserEntity(nombre = "Lucía (Alumna)", email = "lucia@clann.com", password = "123", avatar = "👧", puntos = 95, bateria = 75, estadoTexto = "En la escuela", colorHex = "#43A047")

            // Insértalos en tu repositorio (según cómo gestiones tus inserciones)
            repository.insertUser(user1)
            repository.insertUser(user2)
            repository.insertUser(user3)

            // 2. Añadir tareas de ejemplo asociadas
            repository.insertTask(TaskEntity(titulo = "Preparar examen final de DAM", completada = false, asignadoAUserId = 1))
            repository.insertTask(TaskEntity(titulo = "Revisar documentación de la App", completada = true, asignadoAUserId = 2))

            // 3. Añadir mensajes de ejemplo al chat
            repository.insertMessage(MessageEntity(remitenteId = 1, texto = "¡Bienvenidos al panel de evaluación de ClanPlanner!", timestamp = System.currentTimeMillis()))
        }
    }
    // arriba es lo ultimo de cargar datos de demostracion

    fun registerOrLoginUser(email: String, pass: String, name: String, onResult: (UserEntity) -> Unit) {
        viewModelScope.launch {
            val currentUsers = repository.allUsers.firstOrNull() ?: emptyList()
            var user = currentUsers.find { it.email.equals(email, ignoreCase = true) }

            if (user == null) {
                // Creamos el usuario nuevo
                val newUser = UserEntity(
                    nombre = name,
                    email = email,
                    password = pass,
                    estadoTexto = "🏠 En casa",
                    bateria = 100,
                    avatar = "👤",
                    colorHex = "#6750A4"
                )
                repository.insertUser(newUser)

                // Volvemos a consultar la lista para capturar el ID real que Room acaba de generar (1, 2, etc.)
                val updatedUsers = repository.allUsers.firstOrNull() ?: emptyList()
                user = updatedUsers.find { it.email.equals(email, ignoreCase = true) } ?: newUser
            }

            onResult(user)
        }
    }
}