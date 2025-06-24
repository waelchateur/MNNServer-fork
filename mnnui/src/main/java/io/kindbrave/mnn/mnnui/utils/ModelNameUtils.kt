package io.kindbrave.mnn.mnnui.utils

object ModelNameUtils {
    fun isValidModelName(modelName: String): Boolean {
        if (modelName.isBlank()) {
            return false
        }
        if (modelName.length > 255) {
            return false
        }
        val invalidChars = Regex("[\\\\/:*?\"<>|]")
        if (invalidChars.containsMatchIn(modelName)) {
            return false
        }
        if (modelName.startsWith(".") || modelName.endsWith(".") ||
            modelName.startsWith(" ") || modelName.endsWith(" ")) {
            return false
        }
        return true
    }
}