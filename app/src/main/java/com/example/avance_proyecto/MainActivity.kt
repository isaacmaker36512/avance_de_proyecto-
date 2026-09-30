package com.example.avance_proyecto

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MediControlApp()
            }
        }
    }
}


// ==========================================================
// ESTRUCTURAS DE DATOS
// ==========================================================

data class Medicacion(
    val id: String,
    val name: String,
    val dosage: String,
    val interval: String,
    val nextDoseTime: String,
    val description: String = "Tratamiento médico",
    val startTime: String = "06:00",
    val history: List<String> = emptyList(),
    val perfil: String = "Personal"
)


data class EstadoMedicamentoNuevo(
    val name: String = "",
    val doseValue: String = "",
    val doseUnit: String = "mg",
    val frequency: String = "Cada 8 horas",
    val startTime: String = "14:00",
    val notes: String = "",
    val perfil: String = "Personal"
)


// ==========================================================
// COLORES
// ==========================================================

val TealPrimary = Color(0xFF00897B)
val TealVariant = Color(0xFF00796B)
val TealLightBg = Color(0xFFE0F2F1)
val GrayBackground = Color(0xFFF7F9FA)
val TextDark = Color(0xFF1E293B)
val TextGray = Color(0xFF64748B)
val InputBorderColor = Color(0xFFE2E8F0)
val InputLabelColor = Color(0xFF475569)
val InputPlaceholderColor = Color(0xFF94A3B8)
val RedDeleteColor = Color(0xFFE53935)


// ==========================================================
// NAVEGACIÓN
// ==========================================================

sealed class Screen {
    object Home : Screen()
    object AddMedication : Screen()

    data class MedicationDetail(
        val medicacion: Medicacion
    ) : Screen()
}


// ==========================================================
// APP PRINCIPAL + ROOM
// ==========================================================

@Composable
fun MediControlApp() {

    val context = LocalContext.current

    val database = remember {
        AppDatabase.getDatabase(context)
    }

    val dao = database.mediControlDao()

    val scope = rememberCoroutineScope()


    // ------------------------------------------------------
    // LEER MEDICAMENTOS DESDE ROOM
    // ------------------------------------------------------

    val medicamentosEntity by dao
        .obtenerMedicamentos()
        .collectAsState(initial = emptyList())


    // ------------------------------------------------------
    // LEER PERFILES DESDE ROOM
    // ------------------------------------------------------

    val perfilesEntity by dao
        .obtenerPerfiles()
        .collectAsState(initial = emptyList())


    // Convertimos Entity -> Medicacion
    val medications = medicamentosEntity.map {
        it.toMedicacion()
    }


    // Personal siempre estará disponible.
    val perfiles = buildList {

        add("Personal")

        perfilesEntity.forEach { perfil ->

            if (
                perfil.nombre != "Personal" &&
                !contains(perfil.nombre)
            ) {
                add(perfil.nombre)
            }
        }
    }


    var currentScreen by remember {
        mutableStateOf<Screen>(Screen.Home)
    }


    Crossfade(
        targetState = currentScreen,
        label = "Transicion de pantalla"
    ) { screen ->

        when (screen) {

            // ==================================================
            // HOME
            // ==================================================

            is Screen.Home -> {

                MediControlHomeScreen(

                    medicamentos = medications,

                    perfiles = perfiles,

                    onAddClick = {

                        currentScreen = Screen.AddMedication
                    },


                    // ------------------------------------------
                    // CREAR PERFIL
                    // ------------------------------------------

                    onAddPerfil = { nuevoPerfil ->

                        val perfilLimpio = nuevoPerfil.trim()

                        if (
                            perfilLimpio.isNotBlank() &&
                            !perfiles.contains(perfilLimpio)
                        ) {

                            scope.launch {

                                dao.guardarPerfil(
                                    PerfilEntity(
                                        nombre = perfilLimpio
                                    )
                                )
                            }
                        }
                    },


                    // ------------------------------------------
                    // ACTUALIZAR MEDICAMENTO
                    // ------------------------------------------

                    onRegisterClick = { updatedMed ->

                        scope.launch {

                            dao.guardarMedicamento(
                                updatedMed.toEntity()
                            )
                        }
                    },


                    // ------------------------------------------
                    // ELIMINAR MEDICAMENTO
                    // ------------------------------------------

                    onDeleteClick = { medToDelete ->

                        scope.launch {

                            dao.eliminarMedicamento(
                                medToDelete.toEntity()
                            )
                        }
                    },


                    // ------------------------------------------
                    // ABRIR DETALLE
                    // ------------------------------------------

                    onCardClick = { med ->

                        currentScreen =
                            Screen.MedicationDetail(med)
                    }
                )
            }


            // ==================================================
            // NUEVO MEDICAMENTO
            // ==================================================

            is Screen.AddMedication -> {

                NewMedicationScreen(

                    perfilesDisponibles = perfiles,

                    onBackClick = {

                        currentScreen = Screen.Home
                    },

                    onSaveClick = { formState ->

                        if (formState.name.isNotBlank()) {

                            val newMed = Medicacion(

                                id = System.currentTimeMillis()
                                    .toString(),

                                name = formState.name.trim(),

                                dosage =
                                    if (formState.doseValue.isNotBlank()) {

                                        "${formState.doseValue} ${formState.doseUnit}"

                                    } else {

                                        formState.doseUnit
                                    },

                                interval = formState.frequency,

                                nextDoseTime = formState.startTime,

                                startTime = formState.startTime,

                                description =
                                    if (formState.notes.isNotBlank()) {

                                        formState.notes

                                    } else {

                                        "Tratamiento médico"
                                    },

                                perfil = formState.perfil
                            )


                            // GUARDAR PERMANENTEMENTE

                            scope.launch {

                                dao.guardarMedicamento(
                                    newMed.toEntity()
                                )
                            }
                        }


                        currentScreen = Screen.Home
                    }
                )
            }


            // ==================================================
            // DETALLE
            // ==================================================

            is Screen.MedicationDetail -> {

                // Buscamos siempre la versión más nueva que venga
                // de Room.

                val medicamentoActual = medications.find {

                    it.id == screen.medicacion.id

                } ?: screen.medicacion


                MedicationDetailScreen(

                    medicacion = medicamentoActual,

                    onBackClick = {

                        currentScreen = Screen.Home
                    },


                    // ------------------------------------------
                    // REGISTRAR TOMA
                    // ------------------------------------------

                    onRegisterClick = { updatedMed ->

                        scope.launch {

                            dao.guardarMedicamento(
                                updatedMed.toEntity()
                            )
                        }

                        currentScreen = Screen.Home
                    },


                    // ------------------------------------------
                    // ELIMINAR
                    // ------------------------------------------

                    onDeleteClick = { medToDelete ->

                        scope.launch {

                            dao.eliminarMedicamento(
                                medToDelete.toEntity()
                            )
                        }

                        currentScreen = Screen.Home
                    }
                )
            }
        }
    }
}


// ==========================================================
// PANTALLA PRINCIPAL
// ==========================================================

@Composable
fun MediControlHomeScreen(
    medicamentos: List<Medicacion>,
    perfiles: List<String>,
    onAddClick: () -> Unit,
    onAddPerfil: (String) -> Unit,
    onRegisterClick: (Medicacion) -> Unit,
    onDeleteClick: (Medicacion) -> Unit,
    onCardClick: (Medicacion) -> Unit
) {

    val context = LocalContext.current

    var perfilSeleccionado by remember {
        mutableStateOf("Todos")
    }

    var mostrarDialogoNuevoPerfil by remember {
        mutableStateOf(false)
    }

    var nombreNuevoPerfil by remember {
        mutableStateOf("")
    }


    val medicamentosFiltrados =
        if (perfilSeleccionado == "Todos") {

            medicamentos

        } else {

            medicamentos.filter {
                it.perfil == perfilSeleccionado
            }
        }


    Scaffold(

        // ======================================================
        // TOP BAR
        // ======================================================

        topBar = {

            Surface(
                shadowElevation = 0.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {

                Column {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 20.dp,
                                vertical = 16.dp
                            ),

                        horizontalArrangement =
                            Arrangement.SpaceBetween,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text = "MediControl",
                            color = TealPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )


                        IconButton(
                            onClick = {
                                mostrarDialogoNuevoPerfil = true
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Agregar perfil",
                                tint = TealPrimary
                            )
                        }
                    }


                    // ==========================================
                    // SELECTOR DE PERFILES
                    // ==========================================

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 20.dp,
                                vertical = 4.dp
                            ),

                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        FilterChip(

                            selected =
                                perfilSeleccionado == "Todos",

                            onClick = {
                                perfilSeleccionado = "Todos"
                            },

                            label = {
                                Text("Todos")
                            },

                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealLightBg,
                                    selectedLabelColor = TealPrimary
                                )
                        )


                        perfiles.forEach { perfil ->

                            FilterChip(

                                selected =
                                    perfilSeleccionado == perfil,

                                onClick = {
                                    perfilSeleccionado = perfil
                                },

                                label = {
                                    Text(perfil)
                                },

                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealLightBg,
                                        selectedLabelColor = TealPrimary
                                    )
                            )
                        }
                    }


                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )


                    HorizontalDivider(
                        color = InputBorderColor,
                        thickness = 1.dp
                    )
                }
            }
        },


        // ======================================================
        // BOTÓN +
        // ======================================================

        floatingActionButton = {

            FloatingActionButton(

                onClick = onAddClick,

                containerColor = TealVariant,

                contentColor = Color.White,

                shape = RoundedCornerShape(16.dp)

            ) {

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar medicamento"
                )
            }
        },

        containerColor = GrayBackground

    ) { paddingValues ->


        // ======================================================
        // CREAR PERFIL
        // ======================================================

        if (mostrarDialogoNuevoPerfil) {

            AlertDialog(

                onDismissRequest = {
                    mostrarDialogoNuevoPerfil = false
                },

                title = {
                    Text("Crear nuevo perfil")
                },

                text = {

                    OutlinedTextField(

                        value = nombreNuevoPerfil,

                        onValueChange = {
                            nombreNuevoPerfil = it
                        },

                        placeholder = {
                            Text("Ej. Hijo, Mamá...")
                        },

                        singleLine = true
                    )
                },


                confirmButton = {

                    TextButton(

                        onClick = {

                            if (nombreNuevoPerfil.isNotBlank()) {

                                onAddPerfil(
                                    nombreNuevoPerfil.trim()
                                )

                                nombreNuevoPerfil = ""

                                mostrarDialogoNuevoPerfil = false
                            }
                        }

                    ) {

                        Text(
                            "Agregar",
                            color = TealPrimary
                        )
                    }
                },


                dismissButton = {

                    TextButton(

                        onClick = {
                            mostrarDialogoNuevoPerfil = false
                        }

                    ) {

                        Text(
                            "Cancelar",
                            color = TextGray
                        )
                    }
                }
            )
        }


        // ======================================================
        // LISTA DE MEDICAMENTOS
        // ======================================================

        LazyColumn(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)

        ) {


            item {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 20.dp,
                            bottom = 4.dp
                        ),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "MEDICAMENTOS ($perfilSeleccionado)",

                        color = InputLabelColor,

                        fontSize = 14.sp,

                        fontWeight = FontWeight.Bold,

                        letterSpacing = 0.5.sp
                    )


                    Text(
                        text =
                            "${medicamentosFiltrados.size} Activos",

                        color = TealPrimary,

                        fontSize = 14.sp,

                        fontWeight = FontWeight.Bold
                    )
                }
            }


            if (medicamentosFiltrados.isEmpty()) {

                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                "No hay medicamentos para este perfil",

                            color = TextGray,

                            fontSize = 16.sp
                        )
                    }
                }

            } else {

                items(
                    medicamentosFiltrados,
                    key = { it.id }
                ) { medication ->


                    MedicationCard(

                        medicacion = medication,


                        onCardClick = {

                            onCardClick(medication)
                        },


                        // ======================================
                        // REGISTRAR TOMA
                        // ======================================

                        onRegisterClick = {

                            val currentTime =
                                SimpleDateFormat(
                                    "HH:mm",
                                    Locale.getDefault()
                                ).format(Date())


                            val newHistoryItem =
                                "Hoy, $currentTime"


                            val updatedHistory =
                                listOf(newHistoryItem) +
                                        medication.history


                            val updatedMed =
                                medication.copy(

                                    nextDoseTime =
                                        currentTime,

                                    history =
                                        updatedHistory
                                )


                            Toast.makeText(
                                context,
                                "Toma registrada: ${medication.name}",
                                Toast.LENGTH_SHORT
                            ).show()


                            onRegisterClick(
                                updatedMed
                            )
                        },


                        // ======================================
                        // ELIMINAR
                        // ======================================

                        onDeleteClick = {

                            onDeleteClick(
                                medication
                            )


                            Toast.makeText(
                                context,
                                "Medicamento eliminado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }


            item {

                Spacer(
                    modifier = Modifier.height(80.dp)
                )
            }
        }
    }
}


// ==========================================================
// TARJETA DE MEDICAMENTO
// ==========================================================

@Composable
fun MedicationCard(
    medicacion: Medicacion,
    onCardClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onDeleteClick: () -> Unit
) {

    Card(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )

    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {


            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.Top
            ) {


                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            onClick = onCardClick
                        )
                ) {


                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text = medicacion.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )


                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )


                        Surface(
                            color = TealLightBg,
                            shape = RoundedCornerShape(4.dp)
                        ) {

                            Text(
                                text = medicacion.perfil,

                                fontSize = 10.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color = TealPrimary,

                                modifier =
                                    Modifier.padding(
                                        horizontal = 6.dp,
                                        vertical = 2.dp
                                    )
                            )
                        }
                    }


                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )


                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Medication,

                            contentDescription = null,

                            tint = TealPrimary,

                            modifier =
                                Modifier.size(18.dp)
                        )


                        Spacer(
                            modifier = Modifier.width(4.dp)
                        )


                        Text(
                            text = medicacion.dosage,
                            fontSize = 14.sp,
                            color = TextGray,
                            fontWeight =
                                FontWeight.Medium
                        )
                    }
                }


                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {


                    Surface(
                        color = TealLightBg,

                        shape =
                            RoundedCornerShape(50),

                        modifier =
                            Modifier.clickable(
                                onClick = onCardClick
                            )
                    ) {

                        Text(
                            text = medicacion.interval,

                            color = TealPrimary,

                            fontSize = 12.sp,

                            fontWeight =
                                FontWeight.SemiBold,

                            modifier =
                                Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                )
                        )
                    }


                    Spacer(
                        modifier = Modifier.width(4.dp)
                    )


                    IconButton(
                        onClick = onDeleteClick
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Delete,

                            contentDescription =
                                "Eliminar medicamento",

                            tint = RedDeleteColor
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onCardClick
                    )
                    .background(
                        Color(0xFFF8FAFC),
                        shape =
                            RoundedCornerShape(12.dp)
                    )
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Icon(
                    imageVector =
                        Icons.Outlined.AccessTime,

                    contentDescription = null,

                    tint = TextGray,

                    modifier =
                        Modifier.size(20.dp)
                )


                Spacer(
                    modifier = Modifier.width(8.dp)
                )


                Text(
                    text = "Próxima toma: ",
                    color = TextGray,
                    fontSize = 14.sp
                )


                Text(
                    text = medicacion.nextDoseTime,
                    color = TealPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }


            Spacer(
                modifier = Modifier.height(16.dp)
            )


            Button(

                onClick = onRegisterClick,

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            TealPrimary
                    ),

                shape =
                    RoundedCornerShape(50),

                modifier =
                    Modifier.align(
                        Alignment.End
                    )

            ) {


                Icon(
                    imageVector =
                        Icons.Default.Check,

                    contentDescription = null,

                    modifier =
                        Modifier.size(18.dp)
                )


                Spacer(
                    modifier = Modifier.width(6.dp)
                )


                Text(
                    text = "Registrar toma",
                    fontWeight =
                        FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}


// ==========================================================
// DETALLE DEL MEDICAMENTO
// ==========================================================

@Composable
fun MedicationDetailScreen(
    medicacion: Medicacion,
    onBackClick: () -> Unit,
    onRegisterClick: (Medicacion) -> Unit,
    onDeleteClick: (Medicacion) -> Unit
) {

    val context = LocalContext.current


    Scaffold(

        topBar = {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 12.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {


                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = onBackClick
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Volver",

                            tint = TextDark
                        )
                    }


                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )


                    Text(
                        text = medicacion.name,

                        fontSize = 20.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color = TealPrimary
                    )
                }


                IconButton(

                    onClick = {

                        onDeleteClick(
                            medicacion
                        )

                        Toast.makeText(
                            context,
                            "Medicamento eliminado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Delete,

                        contentDescription =
                            "Eliminar medicamento",

                        tint = RedDeleteColor
                    )
                }
            }
        },


        containerColor = GrayBackground,


        // ==================================================
        // BOTONES INFERIORES
        // ==================================================

        bottomBar = {

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

                color = GrayBackground
            ) {


                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {


                    OutlinedButton(

                        onClick = {

                            onDeleteClick(
                                medicacion
                            )

                            Toast.makeText(
                                context,
                                "Medicamento eliminado",
                                Toast.LENGTH_SHORT
                            ).show()
                        },

                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),

                        shape =
                            RoundedCornerShape(25.dp),

                        border =
                            BorderStroke(
                                1.5.dp,
                                RedDeleteColor
                            )

                    ) {


                        Icon(
                            imageVector =
                                Icons.Default.Delete,

                            contentDescription = null,

                            tint = RedDeleteColor,

                            modifier =
                                Modifier.size(18.dp)
                        )


                        Spacer(
                            modifier =
                                Modifier.width(4.dp)
                        )


                        Text(
                            text = "Eliminar",
                            color = RedDeleteColor,
                            fontWeight =
                                FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }


                    Button(

                        onClick = {

                            val currentTime =
                                SimpleDateFormat(
                                    "HH:mm",
                                    Locale.getDefault()
                                ).format(Date())


                            val newHistoryItem =
                                "Hoy, $currentTime"


                            val updatedHistory =
                                listOf(newHistoryItem) +
                                        medicacion.history


                            val updatedMed =
                                medicacion.copy(

                                    nextDoseTime =
                                        currentTime,

                                    history =
                                        updatedHistory
                                )


                            Toast.makeText(
                                context,
                                "Toma registrada: ${medicacion.name}",
                                Toast.LENGTH_SHORT
                            ).show()


                            onRegisterClick(
                                updatedMed
                            )
                        },

                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp),

                        shape =
                            RoundedCornerShape(25.dp),

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    TealPrimary
                            )

                    ) {

                        Text(
                            text = "Registrar toma",

                            color = Color.White,

                            fontWeight =
                                FontWeight.Bold,

                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

    ) { paddingValues ->


        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 10.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(20.dp)

        ) {


            // ==================================================
            // INFORMACIÓN
            // ==================================================

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(20.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 1.dp
                    )

            ) {


                Column(
                    modifier =
                        Modifier.padding(20.dp)
                ) {


                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {


                        Surface(
                            modifier =
                                Modifier.size(48.dp),

                            shape = CircleShape,

                            color = TealLightBg
                        ) {

                            Box(
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Outlined.Medication,

                                    contentDescription = null,

                                    tint = TealPrimary,

                                    modifier =
                                        Modifier.size(28.dp)
                                )
                            }
                        }


                        Spacer(
                            modifier =
                                Modifier.width(16.dp)
                        )


                        Column {


                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {


                                Text(
                                    text =
                                        medicacion.name,

                                    fontSize = 20.sp,

                                    fontWeight =
                                        FontWeight.Bold,

                                    color = TextDark
                                )


                                Spacer(
                                    modifier =
                                        Modifier.width(8.dp)
                                )


                                Surface(
                                    color =
                                        TealLightBg,

                                    shape =
                                        RoundedCornerShape(4.dp)
                                ) {

                                    Text(
                                        text =
                                            medicacion.perfil,

                                        fontSize = 11.sp,

                                        fontWeight =
                                            FontWeight.Bold,

                                        color =
                                            TealPrimary,

                                        modifier =
                                            Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 2.dp
                                            )
                                    )
                                }
                            }


                            Text(
                                text =
                                    medicacion.description,

                                fontSize = 14.sp,

                                color = TextGray
                            )
                        }
                    }


                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )


                    HorizontalDivider(
                        color = InputBorderColor,
                        thickness = 1.dp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )


                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {


                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {


                            Text(
                                text = "DOSIS",
                                fontSize = 12.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color = TextGray
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )


                            Text(
                                text =
                                    medicacion.dosage,

                                fontSize = 15.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color = TealPrimary
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(16.dp)
                            )


                            Text(
                                text =
                                    "PRÓXIMA TOMA",

                                fontSize = 12.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color = TextGray
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )


                            Text(
                                text =
                                    medicacion.nextDoseTime,

                                fontSize = 15.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color = TealPrimary
                            )
                        }


                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {


                            Text(
                                text = "FRECUENCIA",
                                fontSize = 12.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color = TextGray
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )


                            Text(
                                text =
                                    medicacion.interval,

                                fontSize = 15.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color = TealPrimary
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(16.dp)
                            )


                            Text(
                                text =
                                    "HORA DE INICIO",

                                fontSize = 12.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color = TextGray
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )


                            Text(
                                text =
                                    medicacion.startTime,

                                fontSize = 15.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color = TealPrimary
                            )
                        }
                    }
                }
            }


            // ==================================================
            // HISTORIAL
            // ==================================================

            Text(
                text = "HISTORIAL DE TOMAS",

                fontSize = 14.sp,

                fontWeight = FontWeight.Bold,

                color = InputLabelColor,

                letterSpacing = 0.5.sp
            )


            Column(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {


                if (medicacion.history.isEmpty()) {

                    Text(
                        text =
                            "No hay tomas registradas aún",

                        fontSize = 14.sp,

                        color = TextGray
                    )

                } else {


                    medicacion.history.forEach {
                            timeString ->

                        HistoryItem(
                            time = timeString
                        )
                    }
                }
            }
        }
    }
}


// ==========================================================
// ITEM DEL HISTORIAL
// ==========================================================

@Composable
fun HistoryItem(
    time: String
) {

    Surface(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(16.dp),

        color = Color.White,

        border =
            BorderStroke(
                1.dp,
                InputBorderColor
            )

    ) {


        Row(
            modifier =
                Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                ),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            TealPrimary,
                            shape = CircleShape
                        )
                )


                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )


                Text(
                    text = time,

                    fontSize = 14.sp,

                    fontWeight =
                        FontWeight.SemiBold,

                    color = TextDark
                )
            }


            Text(
                text = "Registrado",

                fontSize = 13.sp,

                fontWeight =
                    FontWeight.Bold,

                color = TealPrimary
            )
        }
    }
}


// ==========================================================
// NUEVO MEDICAMENTO
// ==========================================================

@Composable
fun NewMedicationScreen(
    perfilesDisponibles: List<String>,
    onBackClick: () -> Unit,
    onSaveClick: (EstadoMedicamentoNuevo) -> Unit
) {

    var formState by remember {

        mutableStateOf(
            EstadoMedicamentoNuevo(
                perfil =
                    perfilesDisponibles
                        .firstOrNull()
                        ?: "Personal"
            )
        )
    }


    val scrollState =
        rememberScrollState()


    val context =
        LocalContext.current


    var expandedFrequency by remember {
        mutableStateOf(false)
    }


    var expandedPerfil by remember {
        mutableStateOf(false)
    }


    val frequencies = listOf(
        "Cada 4 horas",
        "Cada 6 horas",
        "Cada 8 horas",
        "Cada 12 horas",
        "Una vez al día"
    )


    Scaffold(

        // ======================================================
        // TOP BAR
        // ======================================================

        topBar = {

            Column {


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 8.dp,
                            vertical = 12.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {


                    IconButton(
                        onClick = onBackClick
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Volver",

                            tint = TextDark
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )


                    Text(
                        text =
                            "Nuevo medicamento",

                        fontSize = 20.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color = TealVariant
                    )
                }


                HorizontalDivider(
                    color = InputBorderColor,
                    thickness = 1.dp
                )
            }
        },


        containerColor = GrayBackground,


        // ======================================================
        // GUARDAR
        // ======================================================

        bottomBar = {

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

                color = GrayBackground
            ) {


                Button(

                    onClick = {
                        onSaveClick(formState)
                    },

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                TealVariant
                        ),

                    shape =
                        RoundedCornerShape(12.dp),

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)

                ) {

                    Text(
                        text =
                            "Guardar medicamento",

                        fontWeight =
                            FontWeight.Bold,

                        fontSize = 16.sp
                    )
                }
            }
        }

    ) { paddingValues ->


        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(
                    horizontal = 20.dp,
                    vertical = 20.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(20.dp)

        ) {


            // ==================================================
            // PERFIL
            // ==================================================

            Column {

                FormLabel(
                    text =
                        "¿Para qué perfil es?"
                )


                Box {


                    DropdownField(

                        text =
                            formState.perfil,

                        onClick = {
                            expandedPerfil = true
                        }
                    )


                    DropdownMenu(

                        expanded =
                            expandedPerfil,

                        onDismissRequest = {
                            expandedPerfil = false
                        }

                    ) {


                        perfilesDisponibles.forEach {
                                perfil ->


                            DropdownMenuItem(

                                text = {
                                    Text(perfil)
                                },

                                onClick = {

                                    formState =
                                        formState.copy(
                                            perfil = perfil
                                        )

                                    expandedPerfil =
                                        false
                                }
                            )
                        }
                    }
                }
            }


            // ==================================================
            // NOMBRE
            // ==================================================

            FormInputField(

                label =
                    "Nombre del medicamento",

                value =
                    formState.name,

                onValueChange = {

                    formState =
                        formState.copy(
                            name = it
                        )
                },

                placeholder =
                    "Ej. Ibuprofeno"
            )


            // ==================================================
            // DOSIS
            // ==================================================

            Column {


                FormLabel(
                    text = "Dosis"
                )


                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(16.dp)

                ) {


                    OutlinedTextField(

                        value =
                            formState.doseValue,

                        onValueChange = {

                            formState =
                                formState.copy(
                                    doseValue = it
                                )
                        },

                        placeholder = {

                            Text(
                                "500",
                                color =
                                    InputPlaceholderColor
                            )
                        },

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),

                        modifier =
                            Modifier.weight(0.35f),

                        colors =
                            getInputFieldColors(),

                        shape =
                            RoundedCornerShape(12.dp),

                        singleLine = true
                    )


                    UnitSelector(

                        units = listOf(
                            "mg",
                            "ml",
                            "gotas"
                        ),

                        selectedUnit =
                            formState.doseUnit,

                        onUnitSelected = {

                            formState =
                                formState.copy(
                                    doseUnit = it
                                )
                        },

                        modifier =
                            Modifier.weight(0.65f)
                    )
                }
            }


            // ==================================================
            // FRECUENCIA
            // ==================================================

            Column {


                FormLabel(
                    text = "Frecuencia"
                )


                Box {


                    DropdownField(

                        text =
                            formState.frequency,

                        onClick = {
                            expandedFrequency = true
                        }
                    )


                    DropdownMenu(

                        expanded =
                            expandedFrequency,

                        onDismissRequest = {
                            expandedFrequency = false
                        }

                    ) {


                        frequencies.forEach {
                                freq ->


                            DropdownMenuItem(

                                text = {
                                    Text(freq)
                                },

                                onClick = {

                                    formState =
                                        formState.copy(
                                            frequency = freq
                                        )

                                    expandedFrequency =
                                        false
                                }
                            )
                        }
                    }
                }
            }


            // ==================================================
            // HORA
            // ==================================================

            Column {


                FormLabel(
                    text = "Hora de inicio"
                )


                DropdownField(

                    text =
                        formState.startTime,

                    icon =
                        Icons.Outlined.AccessTime,

                    onClick = {


                        val parts =
                            formState.startTime
                                .split(":")


                        val initialHour =
                            parts
                                .getOrNull(0)
                                ?.toIntOrNull()
                                ?: 14


                        val initialMinute =
                            parts
                                .getOrNull(1)
                                ?.toIntOrNull()
                                ?: 0


                        TimePickerDialog(

                            context,

                            { _, hour: Int,
                              minute: Int ->


                                val formattedTime =
                                    String.format(
                                        "%02d:%02d",
                                        hour,
                                        minute
                                    )


                                formState =
                                    formState.copy(
                                        startTime =
                                            formattedTime
                                    )
                            },

                            initialHour,

                            initialMinute,

                            true

                        ).show()
                    }
                )
            }


            // ==================================================
            // NOTAS
            // ==================================================

            FormInputField(

                label =
                    "Notas adicionales (Opcional)",

                value =
                    formState.notes,

                onValueChange = {

                    formState =
                        formState.copy(
                            notes = it
                        )
                },

                placeholder =
                    "Ej. Tomar después de comer",

                singleLine = false,

                maxLines = 3
            )


            Spacer(
                modifier =
                    Modifier.height(60.dp)
            )
        }
    }
}


// ==========================================================
// COMPONENTES DE FORMULARIO
// ==========================================================

@Composable
fun FormLabel(
    text: String
) {

    Text(
        text = text,

        fontSize = 14.sp,

        fontWeight =
            FontWeight.Bold,

        color =
            InputLabelColor,

        modifier =
            Modifier.padding(
                bottom = 8.dp
            )
    )
}


@Composable
fun getInputFieldColors() =
    OutlinedTextFieldDefaults.colors(

        focusedBorderColor =
            TealPrimary,

        unfocusedBorderColor =
            InputBorderColor,

        cursorColor =
            TealPrimary,

        focusedTextColor =
            TextDark,

        unfocusedTextColor =
            TextDark,

        focusedContainerColor =
            Color.White,

        unfocusedContainerColor =
            Color.White
    )


@Composable
fun FormInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {

    Column {


        FormLabel(
            text = label
        )


        OutlinedTextField(

            value = value,

            onValueChange =
                onValueChange,

            placeholder = {

                Text(
                    placeholder,
                    color =
                        InputPlaceholderColor
                )
            },

            modifier =
                Modifier.fillMaxWidth(),

            colors =
                getInputFieldColors(),

            shape =
                RoundedCornerShape(12.dp),

            singleLine =
                singleLine,

            maxLines =
                maxLines
        )
    }
}


// ==========================================================
// DROPDOWN
// ==========================================================

@Composable
fun DropdownField(
    text: String,
    icon: ImageVector =
        Icons.Outlined.KeyboardArrowDown,
    onClick: () -> Unit
) {

    Surface(

        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                onClick = onClick
            ),

        shape =
            RoundedCornerShape(12.dp),

        border =
            BorderStroke(
                1.dp,
                InputBorderColor
            ),

        color = Color.White

    ) {


        Row(

            modifier =
                Modifier.padding(
                    horizontal = 16.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.SpaceBetween

        ) {


            Text(
                text = text,
                color = TextDark,
                fontSize = 16.sp
            )


            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    TextDark.copy(
                        alpha = 0.6f
                    )
            )
        }
    }
}


// ==========================================================
// SELECTOR DE UNIDADES
// ==========================================================

@Composable
fun UnitSelector(
    units: List<String>,
    selectedUnit: String,
    onUnitSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(

        modifier =
            modifier.height(56.dp),

        shape =
            RoundedCornerShape(12.dp),

        border =
            BorderStroke(
                1.dp,
                InputBorderColor
            ),

        color = Color.White

    ) {


        Row(
            modifier =
                Modifier.fillMaxSize()
        ) {


            units.forEachIndexed {
                    index,
                    unit ->


                val isSelected =
                    unit == selectedUnit


                val background =
                    if (isSelected) {

                        TealVariant

                    } else {

                        Color.Transparent
                    }


                val textColor =
                    if (isSelected) {

                        Color.White

                    } else {

                        TextDark
                    }


                Box(

                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            background
                        )
                        .clickable {

                            onUnitSelected(
                                unit
                            )
                        },

                    contentAlignment =
                        Alignment.Center

                ) {


                    Text(
                        text = unit,

                        color =
                            textColor,

                        fontWeight =
                            if (isSelected) {

                                FontWeight.Bold

                            } else {

                                FontWeight.Medium
                            },

                        fontSize = 14.sp
                    )
                }


                if (
                    index < units.size - 1 &&
                    !isSelected &&
                    units[index + 1] != selectedUnit
                ) {


                    VerticalDivider(

                        color =
                            InputBorderColor,

                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .padding(
                                vertical = 12.dp
                            )
                    )
                }
            }
        }
    }
}