package com.example.server.data

import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.name

object PathFilterParser {

    private val CONDITION_REGEX = Regex("""^\s*([a-zA-Z_]+)\s*(=|!=|>=|<=|>|<|:)\s*(.+?)\s*$""")

    /**
     * Возвращает предикат (Path) -> Boolean.
     *
     * @param filter Строка фильтра в стиле Google AIP-160 (например: "hidden=false AND ext=pdf").
     * @param hideDotFilesByDefault Если true, скрывает файлы и папки с точкой в начале имени,
     *                              если в самом фильтре явно не передано условие по полю 'hidden'.
     */
    fun parse(filter: String?, hideDotFilesByDefault: Boolean = false): (Path) -> Boolean {
        val predicates = mutableListOf<(Path) -> Boolean>()
        var hasExplicitHiddenRule = false

        if (!filter.isNullOrBlank()) {
            val conditions = filter.split(Regex("""\s+AND\s+"""))
            for (condition in conditions) {
                val match = CONDITION_REGEX.matchEntire(condition)
                    ?: throw IllegalArgumentException(
                        "Invalid filter condition: '$condition'. Expected 'field operator value'"
                    )

                val (_, fieldRaw, operator, valueRaw) = match.groupValues
                val field = fieldRaw.lowercase()
                val cleanValue = valueRaw.removeSurrounding("\"").removeSurrounding("'")

                if (field == "hidden") {
                    hasExplicitHiddenRule = true
                }

                predicates.add(buildPredicate(field, operator, cleanValue))
            }
        }

        if (hideDotFilesByDefault && !hasExplicitHiddenRule) {
            predicates.add { path -> !path.isDotHidden() }
        }

        return if (predicates.isEmpty()) {
            { true }
        } else {
            { path -> predicates.all { predicate -> predicate(path) } }
        }
    }

    fun Path.isDotHidden(): Boolean = this.name.startsWith(".")

    private fun buildPredicate(field: String, operator: String, value: String): (Path) -> Boolean {
        return when (field) {
            "name" -> buildStringPredicate(operator, value) { it.name }
            "ext" -> buildStringPredicate(operator, value.removePrefix(".")) { it.extension }
            "type" -> buildTypePredicate(operator, value)
            "hidden" -> buildBooleanPredicate(operator, value) { it.isDotHidden() }
            "size" -> buildNumericPredicate(operator, value) { path ->
                if (path.isDirectory()) 0L else path.fileSize()
            }
            "modified" -> buildNumericPredicate(operator, value) { path ->
                path.getLastModifiedTime().toMillis()
            }
            else -> throw IllegalArgumentException("Unsupported filter field: '$field'")
        }
    }

    private fun buildBooleanPredicate(
        operator: String,
        target: String,
        extractor: (Path) -> Boolean
    ): (Path) -> Boolean {
        if (operator != "=" && operator != "!=") {
            throw IllegalArgumentException("Operator '$operator' is not supported for boolean fields")
        }
        val expected = when (target.lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> throw IllegalArgumentException("Invalid boolean value: '$target'. Use 'true' or 'false'")
        }
        return { path ->
            val actual = extractor(path)
            if (operator == "=") actual == expected else actual != expected
        }
    }

    private fun buildStringPredicate(
        operator: String,
        target: String,
        extractor: (Path) -> String
    ): (Path) -> Boolean {
        return when (operator) {
            "=" -> { path -> extractor(path).equals(target, ignoreCase = true) }
            "!=" -> { path -> !extractor(path).equals(target, ignoreCase = true) }
            ":" -> { path -> extractor(path).contains(target, ignoreCase = true) }
            else -> throw IllegalArgumentException("Operator '$operator' is not supported for text fields")
        }
    }

    private fun buildTypePredicate(operator: String, target: String): (Path) -> Boolean {
        if (operator != "=" && operator != "!=") {
            throw IllegalArgumentException("Operator '$operator' is not supported for 'type'")
        }
        val expectDir = when (target.lowercase()) {
            "dir", "directory", "folder" -> true
            "file" -> false
            else -> throw IllegalArgumentException("Invalid type value: '$target'. Use 'file' or 'dir'")
        }
        return { path ->
            val matches = path.isDirectory() == expectDir
            if (operator == "=") matches else !matches
        }
    }

    private fun buildNumericPredicate(
        operator: String,
        target: String,
        extractor: (Path) -> Long
    ): (Path) -> Boolean {
        val number = target.toLongOrNull()
            ?: throw IllegalArgumentException("Value '$target' must be a valid integer")

        return when (operator) {
            "=" -> { path -> extractor(path) == number }
            "!=" -> { path -> extractor(path) != number }
            ">" -> { path -> extractor(path) > number }
            "<" -> { path -> extractor(path) < number }
            ">=" -> { path -> extractor(path) >= number }
            "<=" -> { path -> extractor(path) <= number }
            else -> throw IllegalArgumentException("Operator '$operator' is not supported for numeric fields")
        }
    }
}
