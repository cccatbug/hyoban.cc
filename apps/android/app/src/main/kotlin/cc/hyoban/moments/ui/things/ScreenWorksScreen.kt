package cc.hyoban.moments.ui.things

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cc.hyoban.moments.AppContainer
import cc.hyoban.moments.R
import cc.hyoban.moments.data.local.recordTime
import cc.hyoban.moments.data.local.Photos
import cc.hyoban.moments.data.local.Ulid
import cc.hyoban.moments.data.spec.MomentMedia
import cc.hyoban.moments.data.spec.LocalMedia
import cc.hyoban.moments.data.spec.MediaType
import cc.hyoban.moments.data.things.ScreenWork
import cc.hyoban.moments.data.things.ScreenWorkEdit
import cc.hyoban.moments.data.things.Things
import cc.hyoban.moments.ui.components.AppColors
import cc.hyoban.moments.ui.components.SubPage
import cc.hyoban.moments.ui.design.*
import cc.hyoban.moments.ui.settings.SegmentedGroup
import cc.hyoban.moments.ui.settings.SwitchRow
import cc.hyoban.moments.ui.theme.Space
import cc.hyoban.moments.ui.theme.tokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ScreenWorksScreen(container: AppContainer, onBack: () -> Unit, open: String? = null) {
    val entries by container.repository.entries.collectAsStateWithLifecycle()
    val works = remember(entries) { Things.screenWorks(entries.values) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf(works.firstOrNull { it.uid == open }) }
    SubPage(stringResource(R.string.screen_works_title), onBack, Modifier.background(AppColors.page), actions = {
        IconButton(onClick = { adding = true }, modifier = Modifier.testTag("screen_work_add")) {
            Icon(AppIcons.Plus, contentDescription = stringResource(R.string.screen_work_add))
        }
    }) {
        PageColumn {
            if (works.isEmpty()) Text(stringResource(R.string.screen_work_empty), color = tokens.tx2, modifier = Modifier.padding(Space.gutter))
            works.forEach { work ->
                ListItem(
                    headlineContent = { Text(work.title) },
                    supportingContent = { Text(listOfNotNull(stringResource(if (work.kind == "tv") R.string.screen_work_tv else R.string.screen_work_movie), work.rating?.toString(), work.note).joinToString(APART)) },
                    modifier = Modifier.fillMaxWidth().clickable { editing = work }.testTag("screen_work_${work.uid}"),
                )
            }
        }
    }
    if (adding || editing != null) ScreenWorkSheet(container, editing) { adding = false; editing = null }
}

@Composable
private fun ScreenWorkSheet(container: AppContainer, work: ScreenWork?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val kept = remember(work) { mutableStateListOf<MomentMedia>().apply { addAll(work?.entry?.let(Things::screenWorkMedia).orEmpty()) } }
    val picked = remember(work) { mutableStateListOf<Uri>() }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        picked.addAll(uris.filter { it !in picked }.take((9 - kept.size - picked.size).coerceAtLeast(0)))
    }
    var title by rememberSaveable { mutableStateOf(work?.title.orEmpty()) }
    var kind by rememberSaveable { mutableStateOf(work?.kind ?: "movie") }
    var score by rememberSaveable { mutableStateOf(work?.rating) }
    var note by rememberSaveable { mutableStateOf(work?.note.orEmpty()) }
    var public by rememberSaveable { mutableStateOf(work?.private != true) }
    val valid = title.isNotBlank()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.sheet) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter, vertical = Space.l), verticalArrangement = Arrangement.spacedBy(Space.l)) {
            Text(work?.title ?: stringResource(R.string.screen_work_add), style = AppTheme.typography.title)
            OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.screen_work_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("screen_work_name"))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                FilterChip(selected = kind == "movie", onClick = { kind = "movie" }, label = { Text(stringResource(R.string.screen_work_movie)) })
                FilterChip(selected = kind == "tv", onClick = { kind = "tv" }, label = { Text(stringResource(R.string.screen_work_tv)) })
            }
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.screen_work_rating), style = AppTheme.typography.label, color = tokens.tx2, modifier = Modifier.weight(1f))
                    if (score != null) TextButton(onClick = { score = null }) { Text(stringResource(R.string.action_clear)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    (1..5).forEach { value ->
                        val description = stringResource(R.string.screen_work_rate_stars, value)
                        Box(
                            modifier = Modifier.size(48.dp).clickable { score = value.toDouble() }
                                .semantics { contentDescription = description }.testTag("screen_work_star_$value"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(if ((score ?: 0.0) >= value) AppIcons.StarFill else AppIcons.Star, contentDescription = null,
                                modifier = Modifier.size(26.dp), tint = if ((score ?: 0.0) >= value) tokens.accent else tokens.tx3)
                        }
                    }
                }
                if (score != null) Text(stringResource(R.string.screen_work_score, score!!), style = AppTheme.typography.caption, color = tokens.tx2)
            }
            OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.screen_work_note)) }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { picker.launch(arrayOf("image/*")) }, enabled = kept.size + picked.size < 9 && !saving) { Text(stringResource(R.string.screen_work_add_photos)) }
            kept.toList().forEach { image ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    AsyncImage(model = container.mediaUrls.still(image, 480), contentDescription = stringResource(R.string.screen_work_photo), contentScale = ContentScale.Crop, modifier = Modifier.size(80.dp))
                    TextButton(onClick = { kept.remove(image) }) { Text(stringResource(R.string.screen_work_remove_photo)) }
                }
            }
            picked.toList().forEach { uri ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    AsyncImage(model = uri, contentDescription = stringResource(R.string.screen_work_photo), contentScale = ContentScale.Crop, modifier = Modifier.size(80.dp))
                    TextButton(onClick = { picked.remove(uri) }) { Text(stringResource(R.string.screen_work_remove_photo)) }
                }
            }
            if (failed) Text(stringResource(R.string.screen_work_photo_error), color = tokens.danger)
            SegmentedGroup(null, null, listOf({ shapes -> SwitchRow(public, AppIcons.Eye, stringResource(R.string.visibility_public), "screen_work_public", shapes, supporting = stringResource(R.string.screen_work_public_note)) { public = !public } }))
            Row(Modifier.fillMaxWidth()) {
                if (work != null) TextButton(onClick = { scope.launch { container.repository.deleteEntry(work.uid); onDismiss() } }) { Text(stringResource(R.string.action_delete), color = tokens.danger) }
                Spacer(Modifier.weight(1f))
                Button(enabled = valid && !saving, onClick = {
                    saving = true
                    failed = false
                    scope.launch {
                        val copies = mutableListOf<File>()
                        try {
                            val added = picked.map { uri ->
                                val info = Photos.read(context, uri)
                                val width = requireNotNull(info.width?.takeIf { it > 0 })
                                val height = requireNotNull(info.height?.takeIf { it > 0 })
                                require(!info.video && (info.type == null || info.type.startsWith("image/")))
                                val id = Ulid.next()
                                val copy = Photos.copy(context, uri, File(container.localStore.root, "media"), id)
                                copies.add(copy.file)
                                MomentMedia(uid = id, type = MediaType.IMAGE, width = width, height = height, local = LocalMedia(uri = Uri.fromFile(copy.file).toString(), sha256 = copy.sha256, copied = true))
                            }
                            container.repository.saveEntry(Things.screenWorkEntry(work?.entry, ScreenWorkEdit(title.trim(), kind, score, note.trim(), !public), recordTime(), kept.toList() + added))
                            onDismiss()
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            copies.forEach(File::delete)
                            failed = true
                        } finally { saving = false }
                    }
                }, modifier = Modifier.testTag("screen_work_save")) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
