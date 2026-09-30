package com.example.avance_proyecto

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


// ======================================================
// TABLA DE MEDICAMENTOS
// ======================================================

@Entity(tableName = "medicamentos")
data class MedicacionEntity(

    @PrimaryKey
    val id: String,

    val name: String,
    val dosage: String,
    val interval: String,
    val nextDoseTime: String,
    val description: String,
    val startTime: String,

    // Guardamos el historial separado por |||
    val history: String = "",

    val perfil: String
)


// ======================================================
// TABLA DE PERFILES
// ======================================================

@Entity(tableName = "perfiles")
data class PerfilEntity(

    @PrimaryKey
    val nombre: String
)


// ======================================================
// DAO
// ======================================================

@Dao
interface MediControlDao {


    // ==========================
    // MEDICAMENTOS
    // ==========================

    @Query("SELECT * FROM medicamentos")
    fun obtenerMedicamentos(): Flow<List<MedicacionEntity>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarMedicamento(
        medicamento: MedicacionEntity
    )


    @Update
    suspend fun actualizarMedicamento(
        medicamento: MedicacionEntity
    )


    @Delete
    suspend fun eliminarMedicamento(
        medicamento: MedicacionEntity
    )


    // ==========================
    // PERFILES
    // ==========================

    @Query("SELECT * FROM perfiles ORDER BY nombre ASC")
    fun obtenerPerfiles(): Flow<List<PerfilEntity>>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun guardarPerfil(
        perfil: PerfilEntity
    )


    @Delete
    suspend fun eliminarPerfil(
        perfil: PerfilEntity
    )
}



// BASE DE DATOS

@Database(
    entities = [
        MedicacionEntity::class,
        PerfilEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {


    abstract fun mediControlDao(): MediControlDao


    companion object {


        @Volatile
        private var INSTANCE: AppDatabase? = null


        fun getDatabase(context: Context): AppDatabase {


            return INSTANCE ?: synchronized(this) {


                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medicontrol_database"
                ).build()


                INSTANCE = instance


                instance
            }
        }
    }
}


// ======================================================
// CONVERTIR ROOM -> OBJETO DE LA APP
// ======================================================

fun MedicacionEntity.toMedicacion(): Medicacion {


    return Medicacion(

        id = id,

        name = name,

        dosage = dosage,

        interval = interval,

        nextDoseTime = nextDoseTime,

        description = description,

        startTime = startTime,

        history =
            if (history.isBlank()) {

                emptyList()

            } else {

                history.split("|||")
            },

        perfil = perfil
    )
}


// ======================================================
// CONVERTIR OBJETO DE LA APP -> ROOM
// ======================================================

fun Medicacion.toEntity(): MedicacionEntity {


    return MedicacionEntity(

        id = id,

        name = name,

        dosage = dosage,

        interval = interval,

        nextDoseTime = nextDoseTime,

        description = description,

        startTime = startTime,

        history = history.joinToString("|||"),

        perfil = perfil
    )
}