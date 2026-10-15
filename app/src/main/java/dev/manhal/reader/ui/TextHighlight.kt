package dev.manhal.reader.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import dev.manhal.reader.data.TextLang

/** Colours for syntax highlighting; one set for light paper, one for dark. */
class SyntaxColors(val keyword: Color, val string: Color, val number: Color, val comment: Color, val key: Color, val tag: Color, val variable: Color) {
    companion object {
        val Light = SyntaxColors(Color(0xFF8A2BB2), Color(0xFF1B7F3B), Color(0xFFB4570A), Color(0xFF7A7A7A), Color(0xFF1558B0), Color(0xFFB02A4A), Color(0xFF0E7C86))
        val Dark = SyntaxColors(Color(0xFFD59BFF), Color(0xFF8FD694), Color(0xFFF2B26B), Color(0xFF8A8F98), Color(0xFF7DB4FF), Color(0xFFFF8FA8), Color(0xFF6FD3DC))
    }
}

/** A small regex highlighter: good enough to read and edit config and code, not a parser. */
object TextHighlight {
    /** Past this the text is left plain; scanning it would stall typing. */
    const val LIMIT = 300_000

    private class Rule(val regex: Regex, val pick: (SyntaxColors) -> Color)

    private fun r(pattern: String, vararg opts: RegexOption) = Regex(pattern, opts.toSet())

    private val dq = r("\"(?:\\\\.|[^\"\\\\\\n])*\"")
    private val sq = r("'(?:\\\\.|[^'\\\\\\n])*'")
    private val bt = r("`(?:\\\\.|[^`\\\\])*`")
    private val number = r("(?<![\\w.])-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?(?![\\w.])")
    private val hashComment = r("(?<![\\w\"'])#.*")
    private val slashComment = r("//.*")
    private val blockComment = r("/\\*[\\s\\S]*?\\*/")
    private val xmlComment = r("<!--[\\s\\S]*?-->")

    private fun words(vararg w: String) = r("\\b(?:${w.joinToString("|")})\\b")

    private val codeKeywords = words(
        "if", "else", "elif", "for", "while", "do", "switch", "case", "default", "break", "continue", "return", "function", "fun", "def", "class", "interface",
        "struct", "enum", "trait", "impl", "import", "from", "export", "package", "module", "use", "const", "let", "var", "val", "static", "public", "private",
        "protected", "async", "await", "try", "catch", "except", "finally", "throw", "throws", "new", "this", "self", "super", "null", "nil", "None", "true",
        "false", "True", "False", "in", "is", "not", "and", "or", "as", "with", "yield", "lambda", "match", "when", "where", "type", "fn", "mut", "pub", "mod",
        "select", "insert", "update", "delete", "create", "table", "join", "group", "order", "by", "limit", "values", "into", "set", "SELECT", "FROM", "WHERE",
        "INSERT", "UPDATE", "DELETE", "CREATE", "TABLE", "JOIN", "ORDER", "GROUP", "BY", "LIMIT", "AND", "OR", "NOT", "NULL", "AS", "ON", "IN",
    )
    private val shellKeywords = words("if", "then", "else", "elif", "fi", "for", "while", "do", "done", "case", "esac", "function", "in", "return", "exit", "export", "local", "echo", "cd", "set", "unset", "source", "alias")
    private val dockerKeywords = r("^\\s*(?:FROM|RUN|CMD|LABEL|MAINTAINER|EXPOSE|ENV|ADD|COPY|ENTRYPOINT|VOLUME|USER|WORKDIR|ARG|ONBUILD|STOPSIGNAL|HEALTHCHECK|SHELL)\\b", RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    private val literals = words("true", "false", "null", "yes", "no", "on", "off", "True", "False", "Null", "None")

    private fun rules(lang: TextLang): List<Rule> = when (lang) {
        TextLang.Json -> listOf(
            Rule(r("\"(?:\\\\.|[^\"\\\\\\n])*\"(?=\\s*:)")) { it.key },
            Rule(dq) { it.string },
            Rule(number) { it.number },
            Rule(literals) { it.keyword },
        )
        TextLang.Yaml -> listOf(
            Rule(hashComment) { it.comment },
            Rule(r("^[ \\t]*(?:-[ \\t]+)?[\\w.\\-/\"' ]+?(?=[ \\t]*:(?:\\s|$))", RegexOption.MULTILINE)) { it.key },
            Rule(dq) { it.string },
            Rule(sq) { it.string },
            Rule(r("[&*]\\w+|!{1,2}\\w+")) { it.variable },
            Rule(number) { it.number },
            Rule(literals) { it.keyword },
        )
        TextLang.Docker -> listOf(
            Rule(hashComment) { it.comment },
            Rule(dockerKeywords) { it.keyword },
            Rule(dq) { it.string },
            Rule(sq) { it.string },
            Rule(r("\\$\\{[^}]*}|\\$\\w+")) { it.variable },
            Rule(number) { it.number },
        )
        TextLang.Shell -> listOf(
            Rule(hashComment) { it.comment },
            Rule(dq) { it.string },
            Rule(sq) { it.string },
            Rule(r("\\$\\{[^}]*}|\\$\\w+|\\$\\(")) { it.variable },
            Rule(shellKeywords) { it.keyword },
            Rule(number) { it.number },
        )
        TextLang.Xml -> listOf(
            Rule(xmlComment) { it.comment },
            Rule(dq) { it.string },
            Rule(sq) { it.string },
            Rule(r("</?[\\w:.-]+|/?>")) { it.tag },
            Rule(r("[\\w:.-]+(?==)")) { it.key },
        )
        TextLang.Toml, TextLang.Ini -> listOf(
            Rule(r("^\\s*[#;].*", RegexOption.MULTILINE)) { it.comment },
            Rule(r("^\\s*\\[[^\\]\\n]+]", RegexOption.MULTILINE)) { it.tag },
            Rule(r("^\\s*[\\w.\\-]+(?=\\s*[=:])", RegexOption.MULTILINE)) { it.key },
            Rule(dq) { it.string },
            Rule(sq) { it.string },
            Rule(number) { it.number },
            Rule(literals) { it.keyword },
        )
        TextLang.Code -> listOf(
            Rule(blockComment) { it.comment },
            Rule(slashComment) { it.comment },
            Rule(hashComment) { it.comment },
            Rule(dq) { it.string },
            Rule(sq) { it.string },
            Rule(bt) { it.string },
            Rule(codeKeywords) { it.keyword },
            Rule(number) { it.number },
        )
        TextLang.Markdown -> listOf(
            Rule(r("^```.*$", RegexOption.MULTILINE)) { it.comment },
            Rule(r("^#{1,6}\\s.*$", RegexOption.MULTILINE)) { it.key },
            Rule(r("`[^`\\n]+`")) { it.string },
            Rule(r("\\*\\*[^*\\n]+\\*\\*")) { it.keyword },
            Rule(r("\\[[^\\]\\n]*]\\([^)\\n]*\\)")) { it.variable },
            Rule(r("^\\s*(?:[-*+]|\\d+\\.)\\s", RegexOption.MULTILINE)) { it.tag },
        )
        TextLang.Plain, TextLang.Csv -> emptyList()
    }

    /** The text with colour spans; unchanged text length, so it can serve as an edit field's transformation. */
    fun highlight(text: String, lang: TextLang, colors: SyntaxColors): AnnotatedString {
        val rules = rules(lang)
        if (rules.isEmpty() || text.length > LIMIT) return AnnotatedString(text)
        val taken = BooleanArray(text.length)
        return buildAnnotatedString {
            append(text)
            for (rule in rules) {
                for (m in rule.regex.findAll(text)) {
                    val range = m.range
                    if (range.isEmpty() || taken[range.first] || taken[range.last]) continue
                    for (i in range) taken[i] = true
                    val base = SpanStyle(color = rule.pick(colors))
                    val style = if (rule.pick(colors) == colors.comment) base.copy(fontStyle = FontStyle.Italic) else if (rule.pick(colors) == colors.key && lang != TextLang.Markdown) base.copy(fontWeight = FontWeight.Medium) else base
                    addStyle(style, range.first, range.last + 1)
                }
            }
        }
    }
}
