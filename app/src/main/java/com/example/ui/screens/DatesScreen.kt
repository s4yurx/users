package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CountdownBreakdown
import com.example.data.CountdownCalculator
import com.example.data.SpecialDateEvent
import com.example.ui.DuetUiState
import com.example.ui.components.FeatureDisabledCard
import com.example.ui.components.exclusiveAnimatedBorder
import com.example.ui.components.glossyPressEffect
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalDuetThemePreset
import com.example.ui.theme.SoftGoldGlow
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.delay

private enum class DatePickerTarget {
    ANNIVERSARY,
    PARTNER_BIRTHDAY,
    MY_BIRTHDAY,
    NEW_EVENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatesScreen(
    state: DuetUiState,
    externalAddEventTrigger: Int = 0,
    showInternalFab: Boolean = true,
    featureEnabled: Boolean = true,
    onUpdateAnniversary: (year: Int, month: Int, day: Int) -> Unit,
    onUpdateBirthday: (forPartner: Boolean, month: Int, day: Int) -> Unit,
    onAddSpecialEvent: (title: String, category: String, year: Int, month: Int, day: Int, repeatsYearly: Boolean) -> Unit,
    onDeleteSpecialEvent: (String) -> Unit
) {
    val couple = state.couple ?: return
    val myUid = state.currentUserId
    val partnerName = couple.partnerDisplayName(myUid)

    // Live 1-second ticker for real-time countdown timers
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    var activePicker by remember { mutableStateOf<DatePickerTarget?>(null) }
    var showAddEventSheet by remember { mutableStateOf(false) }
    LaunchedEffect(externalAddEventTrigger, featureEnabled) {
        if (externalAddEventTrigger > 0 && featureEnabled) {
            showAddEventSheet = true
        }
    }
    var newEventTitle by remember { mutableStateOf("") }
    var newEventCategory by remember { mutableStateOf("DATE_NIGHT") }
    var newEventRepeats by remember { mutableStateOf(true) }
    var newEventYmd by remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }
        mutableStateOf(
            Triple(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        )
    }

    activePicker?.let { target ->
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { activePicker = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = pickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = selectedMillis
                            }
                            val y = utcCal.get(Calendar.YEAR)
                            val m = utcCal.get(Calendar.MONTH) + 1
                            val d = utcCal.get(Calendar.DAY_OF_MONTH)
                            when (target) {
                                DatePickerTarget.ANNIVERSARY -> onUpdateAnniversary(y, m, d)
                                DatePickerTarget.PARTNER_BIRTHDAY -> onUpdateBirthday(true, m, d)
                                DatePickerTarget.MY_BIRTHDAY -> onUpdateBirthday(false, m, d)
                                DatePickerTarget.NEW_EVENT -> newEventYmd = Triple(y, m, d)
                            }
                        }
                        activePicker = null
                    }
                ) {
                    Text("Save Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { activePicker = null }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showAddEventSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddEventSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Add Special Countdown",
                    style = MaterialTheme.typography.headlineSmall
                )
                OutlinedTextField(
                    value = newEventTitle,
                    onValueChange = { if (it.length <= 50) newEventTitle = it },
                    label = { Text("Event name") },
                    placeholder = { Text("Paris Weekend, First Kiss Day…") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "DATE_NIGHT" to "Date",
                        "TRIP" to "Trip",
                        "MILESTONE" to "Milestone"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = newEventCategory == key,
                            onClick = { newEventCategory = key },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedButton(
                    onClick = { activePicker = DatePickerTarget.NEW_EVENT },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .glossyPressEffect(cornerRadius = 14.dp)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Date: ${newEventYmd.first}-${newEventYmd.second.toString().padStart(2, '0')}-${newEventYmd.third.toString().padStart(2, '0')}")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Repeat every year", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = newEventRepeats,
                        onCheckedChange = { newEventRepeats = it }
                    )
                }
                Button(
                    onClick = {
                        onAddSpecialEvent(
                            newEventTitle,
                            newEventCategory,
                            newEventYmd.first,
                            newEventYmd.second,
                            newEventYmd.third,
                            newEventRepeats
                        )
                        newEventTitle = ""
                        showAddEventSheet = false
                    },
                    enabled = newEventTitle.trim().isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .glossyPressEffect(cornerRadius = 16.dp)
                        .testTag("save_special_event_button")
                ) {
                    Text("Save Countdown")
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val activeTheme = LocalDuetThemePreset.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.dates_title),
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "Live anniversary, birthday, and milestone timers synced for two",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!featureEnabled) {
                item {
                    FeatureDisabledCard(
                        title = "Date Planner Paused",
                        subtitle = "Countdowns, anniversaries, birthdays & special dates are currently turned off by Bondly Admin.",
                        testTag = "feature_disabled_date_planner"
                    )
                }
            } else {
            // 1. Anniversary Hero Live Timer Card
            item {
                val annYear = couple.anniversaryYear
                val annMonth = couple.anniversaryMonth
                val annDay = couple.anniversaryDay

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(activeTheme.heroStart, activeTheme.heroEnd)
                            )
                        )
                        .exclusiveAnimatedBorder(cornerRadius = 28.dp, strokeWidth = 1.8.dp)
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = SoftGoldGlow
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Anniversary Timer",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SoftGoldGlow
                                )
                            }
                            IconButton(
                                onClick = { activePicker = DatePickerTarget.ANNIVERSARY },
                                modifier = Modifier
                                    .glossyPressEffect(cornerRadius = 24.dp)
                                    .testTag("edit_anniversary_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit anniversary date",
                                    tint = Color.White
                                )
                            }
                        }

                        if (annYear != null && annMonth != null && annDay != null) {
                            val daysCount = CountdownCalculator.daysTogether(annYear, annMonth, annDay, nowMillis)
                            val breakdown = CountdownCalculator.computeYearlyCountdown(annMonth, annDay, nowMillis)

                            Text(
                                text = "$daysCount Days Together",
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White
                            )
                            Text(
                                text = if (breakdown.isToday) "Happy Anniversary Today!"
                                else "Next anniversary on ${breakdown.nextDateLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )

                            LiveCountdownTileRow(breakdown = breakdown, darkSurface = true)

                            LinearProgressIndicator(
                                progress = { breakdown.progressToNext },
                                color = SoftGoldGlow,
                                trackColor = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        } else {
                            Text(
                                text = "When did your story begin?",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Text(
                                text = "Set your anniversary date to track every day together and count down to your next celebration.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Button(
                                onClick = { activePicker = DatePickerTarget.ANNIVERSARY },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .glossyPressEffect(cornerRadius = 16.dp)
                                    .testTag("set_anniversary_button")
                            ) {
                                Text("Set Anniversary Date")
                            }
                        }
                    }
                }
            }

            // 2. Partner Birthday Timer Card
            item {
                val partnerBday = couple.partnerBirthday(myUid)
                BirthdayTimerCard(
                    title = "$partnerName's Birthday",
                    monthDay = partnerBday,
                    nowMillis = nowMillis,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    testTag = "edit_partner_birthday_button",
                    onEdit = { activePicker = DatePickerTarget.PARTNER_BIRTHDAY }
                )
            }

            // 3. Your Birthday Timer Card
            item {
                val myBday = couple.myBirthday(myUid)
                BirthdayTimerCard(
                    title = "Your Birthday",
                    monthDay = myBday,
                    nowMillis = nowMillis,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    testTag = "edit_my_birthday_button",
                    onEdit = { activePicker = DatePickerTarget.MY_BIRTHDAY }
                )
            }

            // 4. Custom Shared Milestones
            if (state.events.isNotEmpty()) {
                item {
                    Text(
                        text = "Special Dates Together",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(state.events, key = { it.id }) { evt ->
                    SpecialEventCard(
                        event = evt,
                        nowMillis = nowMillis,
                        onDelete = { onDeleteSpecialEvent(evt.id) }
                    )
                }
            }
            }
        }

        if (showInternalFab && featureEnabled) {
            ExtendedFloatingActionButton(
                onClick = { showAddEventSheet = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.dates_add_event)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .glossyPressEffect(cornerRadius = 16.dp)
                    .testTag("add_special_date_fab")
            )
        }
    }
}

@Composable
private fun BirthdayTimerCard(
    title: String,
    monthDay: Pair<Int, Int>?,
    nowMillis: Long,
    containerColor: Color,
    testTag: String,
    onEdit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .exclusiveAnimatedBorder(cornerRadius = 24.dp, strokeWidth = 1.6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Cake,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                FilledTonalButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .glossyPressEffect(cornerRadius = 12.dp)
                        .testTag(testTag)
                ) {
                    Text(if (monthDay == null) "Set Date" else "Edit")
                }
            }

            if (monthDay != null) {
                val breakdown = CountdownCalculator.computeYearlyCountdown(
                    month = monthDay.first,
                    day = monthDay.second,
                    nowMillis = nowMillis
                )
                Text(
                    text = if (breakdown.isToday) "Happy Birthday Today!"
                    else "Celebrating on ${breakdown.nextDateLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LiveCountdownTileRow(breakdown = breakdown, darkSurface = false)
            } else {
                Text(
                    text = "Add this birthday so neither of you ever misses the live countdown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LiveCountdownTileRow(
    breakdown: CountdownBreakdown,
    darkSurface: Boolean
) {
    val tileBg = if (darkSurface) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
    val valueColor = if (darkSurface) Color.White else MaterialTheme.colorScheme.onSurface
    val labelColor = if (darkSurface) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            breakdown.days.toString().padStart(2, '0') to "DAYS",
            breakdown.hours.toString().padStart(2, '0') to "HRS",
            breakdown.minutes.toString().padStart(2, '0') to "MIN",
            breakdown.seconds.toString().padStart(2, '0') to "SEC"
        ).forEach { (num, unit) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(tileBg)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = num,
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
            }
        }
    }
}

@Composable
private fun SpecialEventCard(
    event: SpecialDateEvent,
    nowMillis: Long,
    onDelete: () -> Unit
) {
    val breakdown = CountdownCalculator.computeEventCountdown(
        year = event.year.toInt(),
        month = event.month.toInt(),
        day = event.day.toInt(),
        repeatsYearly = event.repeatsYearly,
        nowMillis = nowMillis
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .exclusiveAnimatedBorder(cornerRadius = 22.dp, strokeWidth = 1.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (event.category == "TRIP") Icons.Rounded.FlightTakeoff
                        else Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = breakdown.nextDateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.glossyPressEffect(cornerRadius = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete countdown"
                    )
                }
            }
            LiveCountdownTileRow(breakdown = breakdown, darkSurface = false)
        }
    }
}
