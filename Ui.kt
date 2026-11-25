package com.ssc27.tracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private val Blue = Color(0xFF2563EB)
private val Purple = Color(0xFF7C3AED)
private val Navy = Color(0xFF0B1220)

class AppState(val prefs: Prefs) {
    var name by mutableStateOf(prefs.name)
    var exam by mutableStateOf(prefs.examDate)
    var lang by mutableStateOf(prefs.lang)
    var dark by mutableStateOf(prefs.dark)
    var cdOn by mutableStateOf(prefs.countdownOn)
    var moOn by mutableStateOf(prefs.motivationOn)
    var nightOn by mutableStateOf(prefs.nightOn)
    var interval by mutableStateOf(prefs.interval)
    var perm by mutableStateOf(false)

    fun t(k: String) = S.t(k, lang)

    fun saveName(v: String) { if (v.isNotBlank()) { name = v.trim(); prefs.name = name } }
    fun saveExam(v: LocalDate) { exam = v; prefs.examDate = v }
    fun saveLang(v: String) { lang = v; prefs.lang = v }
    fun saveDark(v: Boolean) { dark = v; prefs.dark = v }
    fun saveCd(v: Boolean) { cdOn = v; prefs.countdownOn = v }
    fun saveMo(v: Boolean) { moOn = v; prefs.motivationOn = v }
    fun saveNight(v: Boolean) { nightOn = v; prefs.nightOn = v }
    fun saveInterval(v: Int) { interval = v; prefs.interval = v }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val st = remember { AppState(Prefs(ctx)) }
    val db = remember { NotesDb(ctx) }
    var stage by remember { mutableStateOf(0) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { st.perm = it }

    LaunchedEffect(stage) {
        st.perm = Notifier.granted(ctx)
        if (stage == 2) {
            if (!st.perm && Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            Notifier.schedule(ctx)
        }
    }

    val colors = if (st.dark) darkColorScheme(
        primary = Color(0xFF60A5FA), secondary = Color(0xFFA78BFA),
        background = Navy, surface = Navy, surfaceVariant = Color(0xFF1E293B),
        onBackground = Color.White, onSurface = Color.White, onSurfaceVariant = Color(0xFFE2E8F0)
    ) else lightColorScheme(
        primary = Blue, secondary = Purple,
        background = Color.White, surface = Color.White, surfaceVariant = Color(0xFFF1F5F9),
        onBackground = Color(0xFF0F172A), onSurface = Color(0xFF0F172A), onSurfaceVariant = Color(0xFF1E293B)
    )

    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (stage) {
                0 -> Splash(st) { stage = if (st.prefs.setupDone) 2 else 1 }
                1 -> Setup(st) { st.prefs.setupDone = true; stage = 2 }
                else -> MainScreen(st, db)
            }
        }
    }
}

@Composable
fun Splash(st: AppState, done: () -> Unit) {
    LaunchedEffect(Unit) { delay(2000); done() }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎓", fontSize = 72.sp)
        Spacer(Modifier.height(12.dp))
        Text("SSC 27 Study Tracker", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(st.t("tagline"), fontSize = 15.sp)
        Spacer(Modifier.height(28.dp))
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(init: LocalDate, st: AppState, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val ps = rememberDatePickerState(
        initialSelectedDateMillis = init.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                ps.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text(st.t("ok")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(st.t("cancel")) } }
    ) { DatePicker(state = ps) }
}

@Composable
fun Setup(st: AppState, done: () -> Unit) {
    var name by remember { mutableStateOf(st.name) }
    var showDate by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎓", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(st.t("welcome"), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(st.t("welcomeSub"), fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text(st.t("yourName")) }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
            Text(st.t("examDate") + ": " + fmtDate(st.exam, st.lang))
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = st.lang == "bn", onClick = { st.saveLang("bn") }, label = { Text("বাংলা") })
            FilterChip(selected = st.lang == "en", onClick = { st.saveLang("en") }, label = { Text("English") })
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { st.saveName(name); done() },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(st.t("start"), fontSize = 17.sp) }
    }
    if (showDate) DateDialog(st.exam, st, { st.saveExam(it) }, { showDate = false })
}

@Composable
fun MainScreen(st: AppState, db: NotesDb) {
    var tab by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    var editNote by remember { mutableStateOf<Note?>(null) }
    var tick by remember { mutableStateOf(0) }

    if (editing) {
        key(editNote?.id ?: 0L) {
            Editor(st, db, editNote) { editing = false; editNote = null; tick++ }
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                        icon = { Icon(Icons.Default.Home, null) }, label = { Text(st.t("home")) })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                        icon = { Icon(Icons.Default.Create, null) }, label = { Text(st.t("notes")) })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 },
                        icon = { Icon(Icons.Default.Settings, null) }, label = { Text(st.t("settings")) })
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (tab) {
                    0 -> HomeTab(st, db, tick,
                        onNew = { editNote = null; editing = true },
                        onOpen = { editNote = it; editing = true })
                    1 -> NotesTab(st, db, tick,
                        onNew = { editNote = null; editing = true },
                        onOpen = { editNote = it; editing = true })
                    else -> SettingsTab(st, db) { tick++ }
                }
            }
        }
    }
}

@Composable
fun NoteCard(n: Note, lang: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                if (n.title.isBlank()) "—" else n.title,
                fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (n.body.isNotBlank()) {
                Text(n.body, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(fmtDateTime(n.updated, lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun HomeTab(st: AppState, db: NotesDb, tick: Int, onNew: () -> Unit, onOpen: (Note) -> Unit) {
    val ctx = LocalContext.current
    val notes = remember(tick) { db.all() }
    val days = daysLeft(st.exam)
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(st.t("greet").format(st.name), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))

        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Blue, Purple))).padding(24.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(st.t("countdown"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                when {
                    days > 0L -> {
                        Text(num(days, st.lang), color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
                        Text(st.t("daysLeft"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    days == 0L -> Text(st.t("examDay"), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    else -> Text(st.t("passed"), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Text(st.t("examDate") + ": " + fmtDate(st.exam, st.lang), color = Color.White, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            Messages.pick(st.lang, st.name, days, LocalTime.now().hour, LocalDate.now().toEpochDay()),
            fontSize = 16.sp, lineHeight = 24.sp
        )

        Spacer(Modifier.height(16.dp))
        Button(onClick = onNew, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text(st.t("newNote"), fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))
        val notifOn = (st.cdOn || st.moOn) && Notifier.granted(ctx)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), st.t("total"), num(notes.size.toLong(), st.lang))
            StatCard(
                Modifier.weight(1f), st.t("notifStatus"),
                if (notifOn) st.t("on") + " · " + num(st.interval.toLong(), st.lang) + "h" else st.t("off")
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(st.t("recent"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (notes.isEmpty()) Text(st.t("noNotes"), fontSize = 14.sp)
        notes.take(3).forEach {
            NoteCard(it, st.lang) { onOpen(it) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, value: String) {
    Card(
        modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontSize = 12.sp)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun NotesTab(st: AppState, db: NotesDb, tick: Int, onNew: () -> Unit, onOpen: (Note) -> Unit) {
    var q by remember { mutableStateOf("") }
    val notes = remember(tick, q) { db.all(q) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                label = { Text(st.t("search")) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            if (notes.isEmpty()) Text(st.t("noNotes"))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(notes) { n -> NoteCard(n, st.lang) { onOpen(n) } }
            }
        }
        FloatingActionButton(
            onClick = onNew,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Default.Add, null) }
    }
}

@Composable
fun Editor(st: AppState, db: NotesDb, note: Note?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    var id by remember { mutableStateOf(note?.id ?: 0L) }
    var title by remember { mutableStateOf(note?.title ?: "") }
    var body by remember { mutableStateOf(note?.body ?: "") }
    var confirm by remember { mutableStateOf(false) }

    fun saveNow() {
        if (title.isNotBlank() || body.isNotBlank()) id = db.save(id, title, body)
    }

    // Auto save shortly after typing stops
    LaunchedEffect(title, body) { delay(600); saveNow() }
    BackHandler { saveNow(); onClose() }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { saveNow(); onClose() }) { Icon(Icons.Default.ArrowBack, null) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                clip.setText(AnnotatedString("$title\n\n$body"))
                Toast.makeText(ctx, st.t("copied"), Toast.LENGTH_SHORT).show()
            }) { Text(st.t("copy")) }
            IconButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, "$title\n\n$body")
                }
                ctx.startActivity(Intent.createChooser(send, null))
            }) { Icon(Icons.Default.Share, null) }
            IconButton(onClick = { confirm = true }) { Icon(Icons.Default.Delete, null) }
        }
        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text(st.t("title")) }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = body, onValueChange = { body = it },
            placeholder = { Text(st.t("desc")) },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Spacer(Modifier.height(6.dp))
        val shown = note?.created ?: System.currentTimeMillis()
        Text(fmtDateTime(shown, st.lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            text = { Text(st.t("confirmDel")) },
            confirmButton = {
                TextButton(onClick = {
                    if (id != 0L) db.delete(id)
                    confirm = false
                    onClose()
                }) { Text(st.t("delete")) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(st.t("cancel")) } }
        )
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SectionTitle(text: String) {
    Spacer(Modifier.height(18.dp))
    Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
}

@Composable
fun SettingsTab(st: AppState, db: NotesDb, onNotesChanged: () -> Unit) {
    val ctx = LocalContext.current
    var nameField by remember { mutableStateOf(st.name) }
    var showDate by remember { mutableStateOf(false) }
    var confirmAll by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { st.perm = it }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(st.t("settings"), fontSize = 22.sp, fontWeight = FontWeight.Bold)

        SectionTitle(st.t("changeName"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nameField, onValueChange = { nameField = it },
                singleLine = true, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { st.saveName(nameField) }) { Text(st.t("save")) }
        }

        SectionTitle(st.t("examDate"))
        OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
            Text(st.t("changeDate") + ": " + fmtDate(st.exam, st.lang))
        }

        SectionTitle(st.t("notifStatus"))
        if (!(st.perm || Notifier.granted(ctx))) {
            Text(st.t("permMissing"), fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            Button(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text(st.t("allowNow")) }
            Spacer(Modifier.height(6.dp))
        }
        SwitchRow(st.t("cdNotif"), st.cdOn) { st.saveCd(it); Notifier.schedule(ctx) }
        SwitchRow(st.t("moNotif"), st.moOn) { st.saveMo(it); Notifier.schedule(ctx) }
        SwitchRow(st.t("nightNotif"), st.nightOn) { st.saveNight(it) }
        Spacer(Modifier.height(6.dp))
        Text(st.t("interval"), fontSize = 15.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 3, 6).forEach { h ->
                FilterChip(
                    selected = st.interval == h,
                    onClick = { st.saveInterval(h); Notifier.schedule(ctx) },
                    label = { Text(num(h.toLong(), st.lang)) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { Notifier.post(ctx, true) }, modifier = Modifier.fillMaxWidth()) {
            Text(st.t("test"))
        }
        Spacer(Modifier.height(8.dp))
        Text(st.t("battery"), fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = {
            try { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (e: Exception) {}
        }) { Text(st.t("batteryBtn")) }

        SectionTitle(st.t("language"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = st.lang == "bn", onClick = { st.saveLang("bn") }, label = { Text("বাংলা") })
            FilterChip(selected = st.lang == "en", onClick = { st.saveLang("en") }, label = { Text("English") })
        }
        Spacer(Modifier.height(6.dp))
        SwitchRow(st.t("dark"), st.dark) { st.saveDark(it) }

        SectionTitle(st.t("about"))
        Text(st.t("aboutText"), fontSize = 14.sp)
        SectionTitle(st.t("privacy"))
        Text(st.t("privacyText"), fontSize = 14.sp)
        SectionTitle(st.t("version"))
        Text("1.0", fontSize = 14.sp)

        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = { confirmAll = true }, modifier = Modifier.fillMaxWidth()) {
            Text(st.t("deleteAll"))
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDate) DateDialog(st.exam, st, { st.saveExam(it) }, { showDate = false })
    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            text = { Text(st.t("deleteAllQ")) },
            confirmButton = {
                TextButton(onClick = { db.deleteAll(); confirmAll = false; onNotesChanged() }) { Text(st.t("delete")) }
            },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text(st.t("cancel")) } }
        )
    }
}
