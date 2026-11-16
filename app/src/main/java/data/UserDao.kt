package com.angello.clannplanner.data

import androidx.room.Dao
import androidx.room.Delete // 👈 ¡Faltaba este import obligatorio!
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // Obtener todos los usuarios de forma reactiva (ideal para Jetpack Compose)
    @Query("SELECT * FROM usuarios")
    fun getAllUsers(): Flow<List<UserEntity>>

    // Método síncrono auxiliar para comprobar si la tabla tiene datos
    @Query("SELECT * FROM usuarios")
    suspend fun getUsersSync(): List<UserEntity>

    // Insertar un usuario nuevo
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Actualizar el estado o la batería del usuario en el Muro
    @Update
    suspend fun updateUser(user: UserEntity)

    // Borrar todos los usuarios
    @Query("DELETE FROM usuarios")
    suspend fun deleteAllUsers()

    // Método para borrar un usuario específico
    @Delete
    suspend fun deleteUser(user: UserEntity)
}