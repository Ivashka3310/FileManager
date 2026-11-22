package com.example.filemanager.presentation.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.shared.models.FileItem
import java.util.Locale

@Composable
fun FileCard(
    file: FileItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, description) = resolveFileIcon(file)
            Icon(
                imageVector = icon,
                contentDescription = description,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 12.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSelectionMode) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectToggle() }
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null
                    )
                }
            }
        }
    }
}

private data class FileIcon(
    val icon: ImageVector,
    val description: String
)

private fun resolveFileIcon(file: FileItem): FileIcon {
    if (file.isDirectory) {
        return FileIcon(Icons.Default.Folder, "Folder")
    }

    val extension = file.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when (extension) {
        "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "heic", "ico" ->
            FileIcon(Icons.Default.Image, "Image file")

        "mp4", "mkv", "avi", "mov", "wmv", "webm", "flv", "m4v" ->
            FileIcon(Icons.Default.VideoFile, "Video file")

        "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma" ->
            FileIcon(Icons.Default.AudioFile, "Audio file")

        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz" ->
            FileIcon(Icons.Default.Archive, "Archive file")

        "pdf" ->
            FileIcon(Icons.Default.PictureAsPdf, "PDF document")

        "doc", "docx", "odt", "rtf", "txt", "md", "xls", "xlsx", "csv", "ppt", "pptx" ->
            FileIcon(Icons.Default.Description, "Document file")

        "kt", "java", "py", "js", "ts", "html", "css", "json", "xml", "gradle", "c", "cpp", "cs", "go", "rs", "php", "rb", "sh" ->
            FileIcon(Icons.Default.Code, "Source code file")

        else -> FileIcon(Icons.Default.FilePresent, "File")
    }
}

@Preview
@Composable
fun FileCardPreview() {
    val fileTxt = FileItem(
        name = "Текстовый файл.txt",
        path = "/home/Текстовый файл.txt",
        size = 1000000000,
        isDirectory = false,
        createdAt = "",
        lastModified = ""
    )

    val fileVideo = FileItem(
        name = "Видео.mov",
        path = "/home/Videos/Видео.mov",
        size = 1000000000,
        isDirectory = false,
        createdAt = "",
        lastModified = ""
    )

    val fileAudio = FileItem(
        name = "Аудио.mp3",
        path = "/home/Audio/Аудио.mp3",
        size = 1000000000,
        isDirectory = false,
        createdAt = "",
        lastModified = ""
    )

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FileCard(fileTxt, {}, {})
        FileCard(fileVideo, {}, {})
        FileCard(fileAudio, {}, {})
        FileCard(fileAudio, {}, {}, isSelectionMode = true, isSelected = true)
        FileCard(fileAudio, {}, {}, isSelectionMode = true, isSelected = false)
    }
}