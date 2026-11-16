package com.angello.clannplanner.data

import kotlinx.coroutines.flow.Flow

class ClanRepository(
    private val userDao: UserDao,
    private val taskDao: TaskDao,
    private val messageDao: MessageDao
) {
    // Flujos de datos reactivos hacia la UI
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessages()
    // funcion eliminar datos
    suspend fun clearDatabase() {
        userDao.deleteAllUsers()
        taskDao.deleteAllTasks()
        messageDao.deleteAllMessages()
    }
    // Operaciones de usuarios
    suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }
    suspend fun deleteUser(user: UserEntity) = userDao.deleteUser(user)
    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    // Operaciones de tareas
    suspend fun insertTask(task: TaskEntity) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    // Operaciones de mensajes (Chat)
    suspend fun insertMessage(message: MessageEntity) {
        messageDao.insertMessage(message)
    }

    suspend fun deleteMessage(message: MessageEntity) {
        messageDao.deleteMessage(message)
    }
}