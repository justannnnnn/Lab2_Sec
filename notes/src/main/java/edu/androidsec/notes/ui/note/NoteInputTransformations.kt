package edu.androidsec.notes.ui.note

import androidx.compose.foundation.text.input.InputTransformation

private const val TITLE_MAX_LENGTH = 100
private const val BODY_MAX_LENGTH = 5_000
private const val TAG_MAX_COUNT = 5
private const val TAG_MAX_LENGTH = 20

private const val TAG_ALLOWED_CHARACTERS = "a-z0-9-"

private val TAGS_REGEX = Regex(
    pattern = "^[${TAG_ALLOWED_CHARACTERS}]{1,$TAG_MAX_LENGTH}( [${TAG_ALLOWED_CHARACTERS}]{1,$TAG_MAX_LENGTH}){0,${TAG_MAX_COUNT - 1}} ?$"
)

val TitleInputTransformation = InputTransformation {
    val input = asCharSequence()

    if (
        input.length > TITLE_MAX_LENGTH ||
        input.contains('\n') ||
        input.contains('\r')
    ) {
        revertAllChanges()
    }
}

val BodyInputTransformation = InputTransformation {
    if (length > BODY_MAX_LENGTH) {
        revertAllChanges()
    }
}

val TagsInputTransformation = InputTransformation {
    val input = asCharSequence().toString()

    val isValid = input.isEmpty() || TAGS_REGEX.matches(input)

    if (!isValid) {
        revertAllChanges()
    }
}

