package com.jiyi.power.app.common

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object UserProfileStorage {
    private const val PROFILE_DIRECTORY = "profile"
    private const val AVATAR_FILE_NAME = "avatar"

    fun saveAvatar(context: Context, source: Uri): File {
        val directory = File(context.filesDir, PROFILE_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create the profile directory")
        }

        val target = File(directory, AVATAR_FILE_NAME)
        val temporary = File.createTempFile("avatar_", ".tmp", directory)
        try {
            context.contentResolver.openInputStream(source)?.use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Unable to read the selected avatar")

            Files.move(
                temporary.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
            return target
        } finally {
            temporary.delete()
        }
    }

    fun deleteAvatar(context: Context) {
        File(File(context.filesDir, PROFILE_DIRECTORY), AVATAR_FILE_NAME).delete()
    }
}
