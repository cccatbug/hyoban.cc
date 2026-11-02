package cc.hyoban.moments.ui.things

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cc.hyoban.moments.AppContainer
import cc.hyoban.moments.R
import cc.hyoban.moments.data.local.recordTime
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
    var title by rememberSaveable { mutableStateOf(work?.title.orEmpty()) }
    var kind by rememberSaveable { mutableStateOf(work?.kind ?: "movie") }
    var rating by rememberSaveable { mutableStateOf(work?.rating?.toString().orEmpty()) }
    var note by rememberSaveable { mutableStateOf(work?.note.orEmpty()) }
    var public by rememberSaveable { mutableStateOf(work?.private != true) }
    val score = rating.toDoubleOrNull()
    val valid = title.isNotBlank() && (rating.isBlank() || (score != null && score in 0.0..5.0))
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.sheet) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter, vertical = Space.l), verticalArrangement = Arrangement.spacedBy(Space.l)) {
            Text(work?.title ?: stringResource(R.string.screen_work_add), style = AppTheme.typography.title)
            OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.screen_work_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("screen_work_name"))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                FilterChip(selected = kind == "movie", onClick = { kind = "movie" }, label = { Text(stringResource(R.string.screen_work_movie)) })
                FilterChip(selected = kind == "tv", onClick = { kind = "tv" }, label = { Text(stringResource(R.string.screen_work_tv)) })
            }
            OutlinedTextField(rating, { rating = it }, label = { Text(stringResource(R.string.screen_work_rating)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.screen_work_note)) }, modifier = Modifier.fillMaxWidth())
            SegmentedGroup(null, null, listOf({ shapes -> SwitchRow(public, AppIcons.Eye, stringResource(R.string.visibility_public), "screen_work_public", shapes, supporting = stringResource(R.string.screen_work_public_note)) { public = !public } }))
            Row(Modifier.fillMaxWidth()) {
                if (work != null) TextButton(onClick = { scope.launch { container.repository.deleteEntry(work.uid); onDismiss() } }) { Text(stringResource(R.string.action_delete), color = tokens.danger) }
                Spacer(Modifier.weight(1f))
                Button(enabled = valid, onClick = { scope.launch { container.repository.saveEntry(Things.screenWorkEntry(work?.entry, ScreenWorkEdit(title.trim(), kind, score, note.trim(), !public), recordTime())); onDismiss() } }, modifier = Modifier.testTag("screen_work_save")) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
