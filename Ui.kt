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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private val Palette: List<Color> = run {
    val out = ArrayList<Color>()
    val hues = floatArrayOf(0f, 25f, 45f, 60f, 90f, 130f, 165f, 190f, 215f, 250f, 280f, 320f)
    val combos = listOf(0.35f to 1f, 0.7f to 1f, 1f to 0.9f, 1f to 0.6f)
    for (c in combos) for (h in hues) out.add(Color.hsv(h, c.first, c.second))
    for (g in 0..11) out.add(Color.hsv(0f, 0f, g / 11f))
    out
}

data class EditorReq(val note: Note?, val checklist: Boolean, val folder: String)
data class CItem(val done: Boolean, val text: String)

fun parseItems(body: String): List<CItem> =
    body.lines().filter { it.isNotEmpty() }.map {
        if (it.startsWith("[x] ")) CItem(true, it.drop(4))
        else if (it.startsWith("[ ] ")) CItem(false, it.drop(4))
        else CItem(false, it)
    }

fun serializeItems(l: List<CItem>): String =
    l.joinToString("\n") { (if (it.done) "[x] " else "[ ] ") + it.text }

fun plainBody(n: Note): String =
    if (n.checklist) parseItems(n.body).joinToString("\n") { (if (it.done) "☑ " else "☐ ") + it.text } else n.body

fun previewOf(n: Note): String =
    if (n.checklist) parseItems(n.body).take(3).joinToString("   ") { (if (it.done) "☑ " else "☐ ") + it.text } else n.body

fun sortNotes(l: List<Note>, s: Int): List<Note> = when (s) {
    0 -> l.sortedBy { it.title.lowercase() }
    1 -> l.sortedByDescending { it.updated }
    2 -> l.sortedBy { it.updated }
    3 -> l.sortedByDescending { it.created }
    4 -> l.sortedBy { it.created }
    else -> l.sortedBy { it.color }
}

fun exportAll(ctx: Context, db: NotesDb, st: AppState) {
    val text = db.all().joinToString("\n\n----------\n\n") { n ->
        n.title + "\n" + plainBody(n) + "\n" + fmtDateTime(n.updated, st.lang)
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(send, null))
}

class AppState(val prefs: Prefs) {
    var name by mutableStateOf(prefs.name)
    var exam by mutableStateOf(prefs.examDate)
    var lang by mutableStateOf(prefs.lang)
    var dark by mutableStateOf(prefs.dark)
    var cdOn by mutableStateOf(prefs.countdownOn)
    var moOn by mutableStateOf(prefs.motivationOn)
    var nightOn by mutableStateOf(prefs.nightOn)
    var interval by mutableStateOf(prefs.interval)
    var accent by mutableStateOf(prefs.accent)
    var fontSize by mutableStateOf(prefs.fontSize)
    var sort by mutableStateOf(prefs.sort)
    var titleOnly by mutableStateOf(prefs.titleOnly)
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
    fun saveAccent(v: Int) { accent = v; prefs.accent = v }
    fun saveFont(v: Int) { val x = v.coerceIn(12, 30); fontSize = x; prefs.fontSize = x }
    fun saveSort(v: Int) { sort = v; prefs.sort = v }
    fun saveTitleOnly(v: Boolean) { titleOnly = v; prefs.titleOnly = v }
    fun revert() {
        prefs.resetLook()
        accent = prefs.accent
        fontSize = prefs.fontSize
        sort = prefs.sort
        titleOnly = prefs.titleOnly
        dark = prefs.dark
    }
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

    val accent = Color(st.accent)
    val colors = if (st.dark) darkColorScheme(
        primary = accent, secondary = accent, onPrimary = Color.White,
        background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF1A1A1A),
        onBackground = Color.White, onSurface = Color.White, onSurfaceVariant = Color(0xFFE0E0E0)
    ) else lightColorScheme(
        primary = accent, secondary = accent, onPrimary = Color.White,
        background = Color.White, surface = Color.White, surfaceVariant = Color(0xFFF2F2F2),
        onBackground = Color(0xFF111111), onSurface = Color(0xFF111111), onSurfaceVariant = Color(0xFF222222)
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
fun TopBar(
    title: String,
    nav: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        nav()
        Text(
            title, color = Color.White, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        actions()
    }
}

@Composable
fun Divider1() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.primary))
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
fun ColorDialog(st: AppState, showNone: Boolean, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(st.t("selectColor")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (showNone) {
                    TextButton(onClick = { onPick(0); onDismiss() }) { Text(st.t("noColor")) }
                }
                Palette.chunked(6).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { c ->
                            Box(
                                Modifier.padding(4.dp).size(40.dp).background(c)
                                    .clickable { onPick(c.toArgb()); onDismiss() }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(st.t("cancel")) } }
    )
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
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var editor by remember { mutableStateOf<EditorReq?>(null) }
    var trashOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) }
    val ed = editor

    if (ed != null) {
        key(ed.note?.id ?: 0L, ed.checklist) {
            Editor(st, db, ed) { editor = null; tick++ }
        }
    } else if (trashOpen) {
        TrashScreen(st, db) { trashOpen = false; tick++ }
    } else {
        val navColors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            selectedIconColor = MaterialTheme.colorScheme.onSurface,
            selectedTextColor = MaterialTheme.colorScheme.onSurface
        )
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, colors = navColors,
                        icon = { Icon(Icons.Default.Home, null) }, label = { Text(st.t("home")) })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, colors = navColors,
                        icon = { Icon(Icons.Default.Create, null) }, label = { Text(st.t("notes")) })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, colors = navColors,
                        icon = { Icon(Icons.Default.Settings, null) }, label = { Text(st.t("settings")) })
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (tab) {
                    0 -> HomeTab(st, db, tick,
                        onNew = { editor = EditorReq(null, false, "") },
                        onOpen = { editor = EditorReq(it, it.checklist, it.folder) })
                    1 -> NotesTab(st, db, tick,
                        onMenu = { menuOpen = true },
                        onNew = { check, folder -> editor = EditorReq(null, check, folder) },
                        onOpen = { editor = EditorReq(it, it.checklist, it.folder) })
                    else -> SettingsTab(st, db, onBin = { trashOpen = true }) { tick++ }
                }
            }
        }
        if (menuOpen) {
            AlertDialog(
                onDismissRequest = { menuOpen = false },
                confirmButton = {},
                text = {
                    Column {
                        TextButton(onClick = { menuOpen = false; tab = 2 }) { Text(st.t("settings")) }
                        TextButton(onClick = { menuOpen = false; trashOpen = true }) { Text(st.t("bin")) }
                        TextButton(onClick = { menuOpen = false; exportAll(ctx, db, st) }) { Text(st.t("export")) }
                    }
                }
            )
        }
    }
}

@Composable
fun NoteRow(st: AppState, n: Note, onClick: () -> Unit) {
    val base = MaterialTheme.colorScheme.onSurface
    val titleColor = if (n.color != 0) Color(n.color) else base
    val pv = previewOf(n)
    val head = if (n.title.isNotBlank()) n.title else (pv.lines().firstOrNull() ?: "")
    Column(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    head, color = titleColor, fontSize = st.fontSize.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (!st.titleOnly && n.title.isNotBlank() && pv.isNotBlank()) {
                    Text(
                        pv, color = base.copy(alpha = 0.7f), fontSize = (st.fontSize - 3).sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(relTime(n.updated, st.lang), color = base.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        Divider1()
    }
}

@Composable
fun FolderRow(st: AppState, name: String, count: Int, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📁", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Text(name, fontSize = st.fontSize.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(num(count.toLong(), st.lang))
        }
        Divider1()
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, value: String) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp)
    ) {
        Text(label, fontSize = 12.sp)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun HomeTab(st: AppState, db: NotesDb, tick: Int, onNew: () -> Unit, onOpen: (Note) -> Unit) {
    val ctx = LocalContext.current
    val notes = remember(tick) { sortNotes(db.all(), 1) }
    val days = daysLeft(st.exam)
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxSize()) {
        TopBar("SSC 27 Study Tracker")
        Column(Modifier.fillMaxSize().padding(top = 16.dp).verticalScroll(rememberScrollState())) {
            Text(
                st.t("greet").format(st.name), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(14.dp))

            Box(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(accent, lerp(accent, Color.Black, 0.5f)))).padding(24.dp)
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
                fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onNew,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(st.t("newNote"), fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))
            val notifOn = (st.cdOn || st.moOn) && Notifier.granted(ctx)
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), st.t("total"), num(notes.size.toLong(), st.lang))
                StatCard(
                    Modifier.weight(1f), st.t("notifStatus"),
                    if (notifOn) st.t("on") + " · " + num(st.interval.toLong(), st.lang) + "h" else st.t("off")
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                st.t("recent"), fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(4.dp))
            if (notes.isEmpty()) Text(st.t("noNotes"), fontSize = 14.sp, modifier = Modifier.padding(16.dp))
            notes.take(3).forEach { NoteRow(st, it) { onOpen(it) } }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun NotesTab(
    st: AppState, db: NotesDb, tick: Int,
    onMenu: () -> Unit, onNew: (Boolean, String) -> Unit, onOpen: (Note) -> Unit
) {
    var q by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var confirmFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var folderTick by remember { mutableStateOf(0) }
    var cur by remember { mutableStateOf<String?>(null) }

    val all = remember(tick, q, folderTick) { db.all(q) }
    val folders = remember(tick, folderTick) { db.folders() }
    val shown = sortNotes(all.filter { if (q.isNotBlank()) true else it.folder == (cur ?: "") }, st.sort)
    val current = cur

    BackHandler(enabled = current != null || searching) {
        if (searching) { searching = false; q = "" } else cur = null
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = current ?: st.t("notes"),
            nav = {
                if (current != null) {
                    IconButton(onClick = { cur = null }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                } else {
                    IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, null, tint = Color.White) }
                }
            },
            actions = {
                IconButton(onClick = { searching = !searching; if (!searching) q = "" }) {
                    Icon(Icons.Default.Search, null, tint = Color.White)
                }
                IconButton(onClick = { showSort = true }) { Text("⇅", color = Color.White, fontSize = 22.sp) }
                Box {
                    IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null, tint = Color.White) }
                    DropdownMenu(expanded = showAdd, onDismissRequest = { showAdd = false }) {
                        DropdownMenuItem(text = { Text(st.t("note")) }, onClick = { showAdd = false; onNew(false, current ?: "") })
                        DropdownMenuItem(text = { Text(st.t("checklist")) }, onClick = { showAdd = false; onNew(true, current ?: "") })
                        if (current == null) {
                            DropdownMenuItem(text = { Text(st.t("folder")) }, onClick = { showAdd = false; newFolderName = ""; showNewFolder = true })
                        } else {
                            DropdownMenuItem(text = { Text(st.t("deleteFolder")) }, onClick = { showAdd = false; confirmFolder = true })
                        }
                    }
                }
            }
        )
        if (searching) {
            OutlinedTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                label = { Text(st.t("search")) },
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
        }
        if (shown.isEmpty() && (current != null || folders.isEmpty() || q.isNotBlank())) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(st.t("noNotes")) }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                if (current == null && q.isBlank()) {
                    items(folders) { f ->
                        FolderRow(st, f, all.count { it.folder == f }) { cur = f }
                    }
                }
                items(shown) { n -> NoteRow(st, n) { onOpen(n) } }
            }
        }
    }

    if (showSort) {
        AlertDialog(
            onDismissRequest = { showSort = false },
            confirmButton = {},
            text = {
                Column {
                    listOf("sTitle", "sModNew", "sModOld", "sCreNew", "sCreOld", "sColor").forEachIndexed { i, k ->
                        Row(
                            Modifier.fillMaxWidth().clickable { st.saveSort(i); showSort = false }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = st.sort == i, onClick = { st.saveSort(i); showSort = false })
                            Text(st.t(k))
                        }
                    }
                }
            }
        )
    }
    if (showNewFolder) {
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text(st.t("newFolder")) },
            text = {
                OutlinedTextField(
                    value = newFolderName, onValueChange = { newFolderName = it },
                    singleLine = true, label = { Text(st.t("folderName")) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) { db.addFolder(newFolderName.trim()); folderTick++ }
                    showNewFolder = false
                }) { Text(st.t("ok")) }
            },
            dismissButton = { TextButton(onClick = { showNewFolder = false }) { Text(st.t("cancel")) } }
        )
    }
    if (confirmFolder) {
        AlertDialog(
            onDismissRequest = { confirmFolder = false },
            text = { Text(st.t("deleteFolderQ")) },
            confirmButton = {
                TextButton(onClick = {
                    current?.let { db.deleteFolder(it) }
                    cur = null
                    folderTick++
                    confirmFolder = false
                }) { Text(st.t("delete")) }
            },
            dismissButton = { TextButton(onClick = { confirmFolder = false }) { Text(st.t("cancel")) } }
        )
    }
}

@Composable
fun Editor(st: AppState, db: NotesDb, req: EditorReq, onClose: () -> Unit) {
    val note = req.note
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val textColor = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val isCheck = note?.checklist ?: req.checklist

    var id by remember { mutableStateOf(note?.id ?: 0L) }
    var title by remember { mutableStateOf(note?.title ?: "") }
    var body by remember { mutableStateOf(if (isCheck) "" else (note?.body ?: "")) }
    val checks = remember {
        mutableStateListOf<CItem>().apply {
            if (isCheck) {
                addAll(parseItems(note?.body ?: ""))
                if (isEmpty()) add(CItem(false, ""))
            }
        }
    }
    var color by remember { mutableStateOf(note?.color ?: 0) }
    var folder by remember { mutableStateOf(note?.folder ?: req.folder) }
    var showColor by remember { mutableStateOf(false) }
    var showFolder by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }

    fun outBody(): String = if (isCheck) serializeItems(checks) else body
    fun plain(): String = title + "\n\n" + (if (isCheck) checks.joinToString("\n") { (if (it.done) "☑ " else "☐ ") + it.text } else body)
    fun hasContent(): Boolean =
        title.isNotBlank() || (if (isCheck) checks.any { it.text.isNotBlank() } else body.isNotBlank())
    fun saveNow() {
        if (hasContent()) id = db.save(id, title, outBody(), color, folder, isCheck)
    }

    LaunchedEffect(title, body, color, folder, checks.toList()) { delay(600); saveNow() }
    BackHandler { saveNow(); onClose() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(accent).height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { saveNow(); onClose() }) { Text("✓", color = Color.White, fontSize = 24.sp) }
            BasicTextField(
                value = title, onValueChange = { title = it }, singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) Text(st.t("title"), color = Color.White.copy(alpha = 0.6f), fontSize = 20.sp)
                        inner()
                    }
                }
            )
            IconButton(onClick = { showColor = true }) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape)
                        .background(if (color != 0) Color(color) else Color.White.copy(alpha = 0.35f))
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Text("⋮", color = Color.White, fontSize = 24.sp) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(st.t("copy")) }, onClick = {
                        menu = false
                        clip.setText(AnnotatedString(plain()))
                        Toast.makeText(ctx, st.t("copied"), Toast.LENGTH_SHORT).show()
                    })
                    DropdownMenuItem(text = { Text(st.t("share")) }, onClick = {
                        menu = false
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, title)
                            putExtra(Intent.EXTRA_TEXT, plain())
                        }
                        ctx.startActivity(Intent.createChooser(send, null))
                    })
                    DropdownMenuItem(text = { Text(st.t("folder")) }, onClick = { menu = false; showFolder = true })
                    DropdownMenuItem(text = { Text(st.t("delete")) }, onClick = { menu = false; confirm = true })
                }
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (folder.isNotEmpty()) {
                Text("📁 $folder", color = accent, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
            }
            if (isCheck) {
                checks.forEachIndexed { i, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.done, onCheckedChange = { checks[i] = item.copy(done = it) })
                        BasicTextField(
                            value = item.text, onValueChange = { checks[i] = item.copy(text = it) }, singleLine = true,
                            textStyle = TextStyle(
                                color = textColor, fontSize = st.fontSize.sp,
                                textDecoration = if (item.done) TextDecoration.LineThrough else null
                            ),
                            cursorBrush = SolidColor(accent),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            checks.removeAt(i)
                            if (checks.isEmpty()) checks.add(CItem(false, ""))
                        }) { Text("✕") }
                    }
                }
                TextButton(onClick = { checks.add(CItem(false, "")) }) { Text("+ " + st.t("addItem")) }
            } else {
                BasicTextField(
                    value = body, onValueChange = { body = it },
                    textStyle = TextStyle(color = textColor, fontSize = st.fontSize.sp),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 300.dp),
                    decorationBox = { inner ->
                        Box {
                            if (body.isEmpty()) Text(st.t("desc"), color = textColor.copy(alpha = 0.5f), fontSize = st.fontSize.sp)
                            inner()
                        }
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                fmtDateTime(note?.created ?: System.currentTimeMillis(), st.lang),
                fontSize = 12.sp, color = textColor.copy(alpha = 0.6f)
            )
        }
    }

    if (showColor) ColorDialog(st, true, { color = it }, { showColor = false })
    if (showFolder) {
        val list = remember { db.folders() }
        AlertDialog(
            onDismissRequest = { showFolder = false },
            confirmButton = { TextButton(onClick = { showFolder = false }) { Text(st.t("cancel")) } },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TextButton(onClick = { folder = ""; showFolder = false }) { Text(st.t("noFolder")) }
                    list.forEach { f ->
                        TextButton(onClick = { folder = f; showFolder = false }) { Text("📁 $f") }
                    }
                }
            }
        )
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            text = { Text(st.t("confirmDel")) },
            confirmButton = {
                TextButton(onClick = {
                    if (id != 0L) db.trash(id)
                    confirm = false
                    onClose()
                }) { Text(st.t("delete")) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(st.t("cancel")) } }
        )
    }
}

@Composable
fun TrashScreen(st: AppState, db: NotesDb, onBack: () -> Unit) {
    var tick by remember { mutableStateOf(0) }
    val notes = remember(tick) { db.all("", true) }
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmOne by remember { mutableStateOf<Note?>(null) }
    BackHandler { onBack() }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = st.t("bin"),
            nav = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
            actions = { IconButton(onClick = { confirmEmpty = true }) { Icon(Icons.Default.Delete, null, tint = Color.White) } }
        )
        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(st.t("binEmpty")) }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(notes) { n ->
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (n.title.isNotBlank()) n.title else previewOf(n),
                                fontSize = st.fontSize.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { db.restore(n.id); tick++ }) { Text(st.t("restore")) }
                            TextButton(onClick = { confirmOne = n }) { Text("✕") }
                        }
                        Divider1()
                    }
                }
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            text = { Text(st.t("emptyBinQ")) },
            confirmButton = { TextButton(onClick = { db.emptyTrash(); confirmEmpty = false; tick++ }) { Text(st.t("delete")) } },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text(st.t("cancel")) } }
        )
    }
    val one = confirmOne
    if (one != null) {
        AlertDialog(
            onDismissRequest = { confirmOne = null },
            text = { Text(st.t("foreverQ")) },
            confirmButton = { TextButton(onClick = { db.delete(one.id); confirmOne = null; tick++ }) { Text(st.t("delete")) } },
            dismissButton = { TextButton(onClick = { confirmOne = null }) { Text(st.t("cancel")) } }
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
fun SettingsTab(st: AppState, db: NotesDb, onBin: () -> Unit, onNotesChanged: () -> Unit) {
    val ctx = LocalContext.current
    var nameField by remember { mutableStateOf(st.name) }
    var showDate by remember { mutableStateOf(false) }
    var showAccent by remember { mutableStateOf(false) }
    var confirmAll by remember { mutableStateOf(false) }
    var confirmRevert by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { st.perm = it }

    Column(Modifier.fillMaxSize()) {
        TopBar(st.t("settings"))
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
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

            SectionTitle(st.t("dark"))
            SwitchRow(st.t("dark"), st.dark) { st.saveDark(it) }
            Row(
                Modifier.fillMaxWidth().clickable { showAccent = true }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(st.t("colorScheme"), modifier = Modifier.weight(1f), fontSize = 15.sp)
                Box(Modifier.size(36.dp).background(Color(st.accent)))
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(st.t("fontSize"), modifier = Modifier.weight(1f), fontSize = 15.sp)
                TextButton(onClick = { st.saveFont(st.fontSize - 1) }) { Text("−", fontSize = 22.sp) }
                Text(num(st.fontSize.toLong(), st.lang), fontSize = 16.sp)
                TextButton(onClick = { st.saveFont(st.fontSize + 1) }) { Text("+", fontSize = 22.sp) }
            }
            SwitchRow(st.t("titleOnly"), st.titleOnly) { st.saveTitleOnly(it) }
            OutlinedButton(onClick = { confirmRevert = true }, modifier = Modifier.fillMaxWidth()) {
                Text(st.t("revert"))
            }

            SectionTitle(st.t("notes"))
            OutlinedButton(onClick = onBin, modifier = Modifier.fillMaxWidth()) { Text(st.t("bin")) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { exportAll(ctx, db, st) }, modifier = Modifier.fillMaxWidth()) {
                Text(st.t("export"))
            }

            SectionTitle(st.t("about"))
            Text(st.t("aboutText"), fontSize = 14.sp)
            SectionTitle(st.t("privacy"))
            Text(st.t("privacyText"), fontSize = 14.sp)
            SectionTitle(st.t("version"))
            Text("1.1", fontSize = 14.sp)

            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = { confirmAll = true }, modifier = Modifier.fillMaxWidth()) {
                Text(st.t("deleteAll"))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDate) DateDialog(st.exam, st, { st.saveExam(it) }, { showDate = false })
    if (showAccent) ColorDialog(st, false, { st.saveAccent(it) }, { showAccent = false })
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
    if (confirmRevert) {
        AlertDialog(
            onDismissRequest = { confirmRevert = false },
            text = { Text(st.t("revertQ")) },
            confirmButton = { TextButton(onClick = { st.revert(); confirmRevert = false }) { Text(st.t("yes")) } },
            dismissButton = { TextButton(onClick = { confirmRevert = false }) { Text(st.t("no")) } }
        )
    }
}
