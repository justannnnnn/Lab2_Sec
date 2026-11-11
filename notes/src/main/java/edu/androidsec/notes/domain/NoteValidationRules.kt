package edu.androidsec.notes.domain

object NoteValidationRules {

    private const val TITLE_MAX_LENGTH = 100
    private const val BODY_MAX_LENGTH = 5_000
    private const val TAGS_MAX_COUNT = 5
    private const val TAG_MAX_LENGTH = 20

    private val TAGS_REGEX =
        Regex("^[a-z0-9-]{1,20}( [a-z0-9-]{1,20}){0,4} ?$")

    fun validateTitle(title: String): Boolean {
        return title.length <= TITLE_MAX_LENGTH &&
            !title.contains('\n') &&
            !title.contains('\r')
    }

    fun validateBody(body: String): Boolean {
        return body.length <= BODY_MAX_LENGTH
    }

    fun validateTags(tags: String): Boolean {
        if (tags.isEmpty()) return true

        val normalizedTags = tags.removeSuffix(" ")
        val tagList = normalizedTags.split(" ")

        if (tagList.size > TAGS_MAX_COUNT) return false

        return tagList.all { tag ->
            tag.length in 1..TAG_MAX_LENGTH &&
                tag.all { char ->
                    char in 'a'..'z' ||
                        char in '0'..'9' ||
                        char == '-'
                }
        }
    }

    fun validateNote(note: Note): Boolean {
        return validateTitle(note.title) &&
            validateBody(note.body) &&
            validateTags(note.tags)
    }
}
