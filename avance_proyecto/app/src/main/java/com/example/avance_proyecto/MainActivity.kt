package com.example.avance_proyecto

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


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

// Colores del programa y estructuras de datos

data class Medicacion(
    val id: String,
    val name: String,
    val dosage: String,
    val interval: String,
    val nextDoseTime: String,
    val description: String = "Tratamiento médico",
    val startTime: String = "06:00"
)

data class EstadoMedicamentoNuevo(
    val name: String = "",
    val doseValue: String = "",
    val doseUnit: String = "mg",
    val frequency: String = "Cada 8 horas",
    val startTime: String = "14:00",
    val notes: String = ""
)

val TealPrimary = Color(0xFF00897B)
val TealVariant = Color(0xFF00796B)
val TealLightBg = Color(0xFFE0F2F1)
val GrayBackground = Color(0xFFF7F9FA)
val TextDark = Color(0xFF1E293B)
val TextGray = Color(0xFF64748B)
val InputBorderColor = Color(0xFFE2E8F0)
val InputLabelColor = Color(0xFF475569)
val InputPlaceholderColor = Color(0xFF94A3B8)


sealed class Screen {
    object Home : Screen()
    object AddMedication : Screen()
    data class MedicationDetail(val medicacion: Medicacion) : Screen()
}

// Control de pantallas
@Composable
fun MediControlApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val medications = remember { mutableStateListOf<Medicacion>() }

    Crossfade(targetState = currentScreen, label = "Transicion de pantalla") { screen ->
        when (screen) {
            is Screen.Home -> {
                MediControlHomeScreen(
                    Medicamentos = medications,
                    onAddClick = { currentScreen = Screen.AddMedication },
                    onRegisterClick = { medication -> },
                    onCardClick = { med ->
                        currentScreen = Screen.MedicationDetail(med)
                    }
                )
            }
            is Screen.AddMedication -> {
                NewMedicationScreen(
                    onBackClick = { currentScreen = Screen.Home },
                    onSaveClick = { formState ->
                        if (formState.name.isNotBlank()) {
                            val newMed = Medicacion(
                                id = System.currentTimeMillis().toString(),
                                name = formState.name,
                                dosage = "${formState.doseValue} ${formState.doseUnit}",
                                interval = formState.frequency,
                                nextDoseTime = formState.startTime,
                                startTime = formState.startTime
                            )
                            medications.add(newMed)
                        }
                        currentScreen = Screen.Home
                    }
                )
            }
            is Screen.MedicationDetail -> {
                MedicationDetailScreen(
                    medicacion = screen.medicacion,
                    onBackClick = { currentScreen = Screen.Home },
                    onRegisterClick = {
                        // AQUÍ: Tampoco removemos el medicamento al registrar la toma desde el detalle
                        currentScreen = Screen.Home
                    }
                )
            }
        }
    }
}
// Pantalla de inicio

@Composable
fun MediControlHomeScreen(
    Medicamentos: List<Medicacion>,
    onAddClick: () -> Unit,
    onRegisterClick: (Medicacion) -> Unit,
    onCardClick: (Medicacion) -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 0.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "MediControl",
                        color = TealPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                    HorizontalDivider(color = InputBorderColor, thickness = 1.dp)
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = TealVariant,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar medicamento")
            }
        },
        containerColor = GrayBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MIS MEDICAMENTOS",
                        color = InputLabelColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${Medicamentos.size} Activos",
                        color = TealPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (Medicamentos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay medicamentos registrados",
                            color = TextGray,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                items(Medicamentos, key = { it.id }) { medication ->
                    MedicationCard(
                        medicacion = medication,
                        onCardClick = { onCardClick(medication) },
                        onRegisterClick = {
                            Toast.makeText(
                                context,
                                "Toma registrada: ${medication.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                            onRegisterClick(medication)
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun MedicationCard(
    medicacion: Medicacion,
    onCardClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Área cliqueable superior (Nombre y dosis)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onCardClick)
                ) {
                    Text(
                        text = medicacion.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Medication,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = medicacion.dosage,
                            fontSize = 14.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = TealLightBg,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.clickable(onClick = onCardClick)
                    ) {
                        Text(
                            text = medicacion.interval,
                            color = TealPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    // Botón con ícono explícito para ir a la pantalla de detalle
                    IconButton(onClick = onCardClick) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Ver detalles",
                            tint = TealPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Caja de Próxima toma
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCardClick)
                    .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Próxima toma: ", color = TextGray, fontSize = 14.sp)
                Text(
                    text = medicacion.nextDoseTime,
                    color = TealPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de Registrar Toma
            Button(
                onClick = onRegisterClick,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Registrar toma", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

// ==========================================
// PANTALLA DETALLE Y HISTORIAL
// ==========================================

@Composable
fun MedicationDetailScreen(
    medicacion: Medicacion,
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextDark)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = medicacion.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                }
                IconButton(onClick = { }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opciones", tint = TextDark)
                }
            }
        },
        containerColor = GrayBackground,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = GrayBackground
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        border = BorderStroke(1.5.dp, TealPrimary)
                    ) {
                        Text(text = "Editar", color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Button(
                        onClick = {
                            Toast.makeText(context, "Toma registrada: ${medicacion.name}", Toast.LENGTH_SHORT).show()
                            onRegisterClick()
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text(text = "Registrar toma", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = TealLightBg
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Medication,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = medicacion.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = medicacion.description,
                                fontSize = 14.sp,
                                color = TextGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = InputBorderColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "DOSIS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = medicacion.dosage, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TealPrimary)

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(text = "PRÓXIMA TOMA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = medicacion.nextDoseTime, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "FRECUENCIA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = medicacion.interval, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TealPrimary)

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(text = "HORA DE INICIO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = medicacion.startTime, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                        }
                    }
                }
            }

            Text(
                text = "HISTORIAL DE TOMAS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = InputLabelColor,
                letterSpacing = 0.5.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HistoryItem(time = "Hoy, 06:02")
                HistoryItem(time = "Ayer, 22:05")
                HistoryItem(time = "Ayer, 14:00")
            }
        }
    }
}

@Composable
fun HistoryItem(time: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, InputBorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TealPrimary, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = time, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            }
            Text(text = "Registrado", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
        }
    }
}

@Composable
fun NewMedicationScreen(
    onBackClick: () -> Unit,
    onSaveClick: (EstadoMedicamentoNuevo) -> Unit
) {
    var formState by remember { mutableStateOf(EstadoMedicamentoNuevo()) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextDark)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nuevo medicamento",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealVariant
                    )
                }
                HorizontalDivider(color = InputBorderColor, thickness = 1.dp)
            }
        },
        containerColor = GrayBackground,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = GrayBackground
            ) {
                Button(
                    onClick = { onSaveClick(formState) },
                    colors = ButtonDefaults.buttonColors(containerColor = TealVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(text = "Guardar medicamento", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FormInputField(
                label = "Nombre del medicamento",
                value = formState.name,
                onValueChange = { formState = formState.copy(name = it) },
                placeholder = "Ej. Ibuprofeno"
            )

            Column {
                FormLabel(text = "Dosis")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = formState.doseValue,
                        onValueChange = { formState = formState.copy(doseValue = it) },
                        placeholder = { Text("500", color = InputPlaceholderColor) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.35f),
                        colors = getInputFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    UnitSelector(
                        units = listOf("mg", "ml", "gotas"),
                        selectedUnit = formState.doseUnit,
                        onUnitSelected = { formState = formState.copy(doseUnit = it) },
                        modifier = Modifier.weight(0.65f)
                    )
                }
            }

            Column {
                FormLabel(text = "Frecuencia")
                DropdownField(text = formState.frequency, onClick = { })
            }

            Column {
                FormLabel(text = "Hora de inicio")
                DropdownField(
                    text = formState.startTime,
                    icon = Icons.Outlined.AccessTime,
                    onClick = { }
                )
            }

            FormInputField(
                label = "Notas adicionales (Opcional)",
                value = formState.notes,
                onValueChange = { formState = formState.copy(notes = it) },
                placeholder = "Ej. Tomar después de comer",
                singleLine = false,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// Componentes auxiliares de formulario

@Composable
fun FormLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = InputLabelColor,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun getInputFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TealPrimary,
    unfocusedBorderColor = InputBorderColor,
    cursorColor = TealPrimary,
    focusedTextColor = TextDark,
    unfocusedTextColor = TextDark,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
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
        FormLabel(text = label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = InputPlaceholderColor) },
            modifier = Modifier.fillMaxWidth(),
            colors = getInputFieldColors(),
            shape = RoundedCornerShape(12.dp),
            singleLine = singleLine,
            maxLines = maxLines
        )
    }
}

@Composable
fun DropdownField(
    text: String,
    icon: ImageVector = Icons.Outlined.KeyboardArrowDown,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, InputBorderColor),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = text, color = TextDark, fontSize = 16.sp)
            Icon(imageVector = icon, contentDescription = null, tint = TextDark.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun UnitSelector(
    units: List<String>,
    selectedUnit: String,
    onUnitSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, InputBorderColor),
        color = Color.White
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            units.forEachIndexed { index, unit ->
                val isSelected = unit == selectedUnit
                val background = if (isSelected) TealVariant else Color.Transparent
                val textColor = if (isSelected) Color.White else TextDark

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(background)
                        .clickable { onUnitSelected(unit) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unit,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                if (index < units.size - 1 && !isSelected && units[index + 1] != selectedUnit) {
                    VerticalDivider(
                        color = InputBorderColor,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}