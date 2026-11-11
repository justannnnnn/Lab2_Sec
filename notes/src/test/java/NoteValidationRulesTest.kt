package edu.androidsec.notes.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteValidationRulesTest {

    // TITLE

    @Test
    fun `title with 100 characters is valid`() {
        val title = "A".repeat(100)

        assertTrue(NoteValidationRules.validateTitle(title))
    }

    @Test
    fun `title with 101 characters is invalid`() {
        val title = "A".repeat(101)

        assertFalse(NoteValidationRules.validateTitle(title))
    }

    @Test
    fun `title with newline is invalid`() {
        val title = "First\nSecond"

        assertFalse(NoteValidationRules.validateTitle(title))
    }

    @Test
    fun `title with carriage return is invalid`() {
        val title = "First\rSecond"

        assertFalse(NoteValidationRules.validateTitle(title))
    }

    @Test
    fun `empty title is valid`() {
        assertTrue(NoteValidationRules.validateTitle(""))
    }

    // BODY

    @Test
    fun `body with 5000 characters is valid`() {
        val body = "A".repeat(5000)

        assertTrue(NoteValidationRules.validateBody(body))
    }

    @Test
    fun `body with 5001 characters is invalid`() {
        val body = "A".repeat(5001)

        assertFalse(NoteValidationRules.validateBody(body))
    }

    @Test
    fun `empty body is valid`() {
        assertTrue(NoteValidationRules.validateBody(""))
    }

    // TAGS

    @Test
    fun `five valid tags are valid`() {
        assertTrue(
            NoteValidationRules.validateTags(
                "android kotlin security notes mobile"
            )
        )
    }

    @Test
    fun `six tags are invalid`() {
        assertFalse(
            NoteValidationRules.validateTags(
                "android kotlin security notes mobile java"
            )
        )
    }

    @Test
    fun `tag with 20 characters is valid`() {
        assertTrue(
            NoteValidationRules.validateTags("a".repeat(20))
        )
    }

    @Test
    fun `tag with 21 characters is invalid`() {
        assertFalse(
            NoteValidationRules.validateTags("a".repeat(21))
        )
    }

    @Test
    fun `uppercase letters in tags are invalid`() {
        assertFalse(
            NoteValidationRules.validateTags("Android")
        )
    }

    @Test
    fun `underscore in tags is invalid`() {
        assertFalse(
            NoteValidationRules.validateTags("android_security")
        )
    }

    @Test
    fun `empty tags are valid`() {
        assertTrue(
            NoteValidationRules.validateTags("")
        )
    }

    @Test
    fun `trailing space after tag is valid`() {
        assertTrue(
            NoteValidationRules.validateTags("android ")
        )
    }

    @Test
    fun `multiple spaces between tags are invalid`() {
        assertFalse(
            NoteValidationRules.validateTags("android  kotlin")
        )
    }

    // NOTE

    @Test
    fun `note with valid fields is valid`() {
        val note = Note(
            title = "My note",
            body = "Some text"
        )

        assertTrue(NoteValidationRules.validateNote(note))
    }

    @Test
    fun `note with invalid title is invalid`() {
        val note = Note(
            title = "A".repeat(101),
            body = "Some text"
        )

        assertFalse(NoteValidationRules.validateNote(note))
    }

    @Test
    fun `note with invalid body is invalid`() {
        val note = Note(
            title = "My note",
            body = "A".repeat(5001)
        )

        assertFalse(NoteValidationRules.validateNote(note))
    }
}
