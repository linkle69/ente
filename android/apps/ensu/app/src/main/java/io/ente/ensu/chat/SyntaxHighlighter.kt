package io.ente.ensu.chat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle

/** Colors used for syntax highlighting in code blocks. */
internal data class SyntaxPalette(
    val keyword: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
    val type: Color,
    val function: Color,
    val annotation: Color,
    val property: Color,
) {
    companion object {
        val Light =
            SyntaxPalette(
                keyword = Color(0xFFCF222E),
                string = Color(0xFF0A3069),
                comment = Color(0xFF6E7781),
                number = Color(0xFF0550AE),
                type = Color(0xFF953800),
                function = Color(0xFF8250DF),
                annotation = Color(0xFF116329),
                property = Color(0xFF0550AE),
            )

        val Dark =
            SyntaxPalette(
                keyword = Color(0xFFFF7B72),
                string = Color(0xFFA5D6FF),
                comment = Color(0xFF8B949E),
                number = Color(0xFF79C0FF),
                type = Color(0xFFFFA657),
                function = Color(0xFFD2A8FF),
                annotation = Color(0xFF7EE787),
                property = Color(0xFF79C0FF),
            )

        @Composable fun current(): SyntaxPalette = if (isSystemInDarkTheme()) Dark else Light
    }
}

private enum class TokenKind {
    Keyword,
    StringLit,
    Comment,
    Number,
    Constant,
    Type,
    Function,
    Annotation,
    Property,
}

private class LanguageSpec(
    keywords: String = "",
    val lineComments: List<String> = emptyList(),
    val blockComment: Pair<String, String>? = null,
    /** Characters that open a single-line string. */
    val quotes: String = "\"'",
    /** Quote characters whose strings may span lines (e.g. JS template literals). */
    val multilineQuotes: String = "",
    val tripleQuotes: Boolean = false,
    /** `'` only opens short char literals, so Rust lifetimes etc. aren't treated as strings. */
    val charLiteralSingleQuote: Boolean = false,
    val capitalizedTypes: Boolean = true,
    val caseInsensitive: Boolean = false,
    val extraIdentChars: String = "",
    /** An identifier directly followed by one of these is a key/property (YAML, CSS, TOML). */
    val keyTerminators: String = "",
    /** A string directly followed by `:` is a key (JSON). */
    val stringKeys: Boolean = false,
    val markup: Boolean = false,
    val variablePrefix: Char? = null,
    val annotations: Boolean = true,
    val macroBang: Boolean = false,
) {
    val keywords: Set<String> =
        keywords
            .split(' ')
            .filter { it.isNotBlank() }
            .map { if (caseInsensitive) it.lowercase() else it }
            .toSet()
}

private val constants =
    setOf(
        "true",
        "false",
        "null",
        "nil",
        "None",
        "True",
        "False",
        "undefined",
        "NULL",
        "nullptr",
        "self",
        "this",
        "NaN",
        "Infinity",
        "yes",
        "no",
    )

private val cStyle = listOf("//")
private val cBlock = "/*" to "*/"

private val kotlinSpec =
    LanguageSpec(
        keywords =
            "package import class interface fun val var object companion data sealed enum " +
                "annotation open abstract override private protected public internal inline " +
                "suspend return if else when for while do try catch finally throw is in as by " +
                "get set init constructor super typealias where lateinit const operator infix " +
                "reified crossinline noinline vararg out break continue value",
        lineComments = cStyle,
        blockComment = cBlock,
        quotes = "\"'",
        tripleQuotes = true,
        charLiteralSingleQuote = true,
    )

private val javaSpec =
    LanguageSpec(
        keywords =
            "abstract assert boolean break byte case catch char class const continue default " +
                "do double else enum extends final finally float for goto if implements import " +
                "instanceof int interface long native new package private protected public " +
                "return short static strictfp super switch synchronized throw throws transient " +
                "try void volatile while var record yield sealed permits",
        lineComments = cStyle,
        blockComment = cBlock,
        tripleQuotes = true,
        charLiteralSingleQuote = true,
    )

private val jsSpec =
    LanguageSpec(
        keywords =
            "break case catch class const continue debugger default delete do else export " +
                "extends finally for function if import in instanceof let new return super " +
                "switch throw try typeof var void while with yield async await of static get " +
                "set from as type interface enum implements private protected public readonly " +
                "declare namespace abstract keyof infer is satisfies any unknown never string " +
                "number boolean",
        lineComments = cStyle,
        blockComment = cBlock,
        quotes = "\"'`",
        multilineQuotes = "`",
        extraIdentChars = "$",
    )

private val pythonSpec =
    LanguageSpec(
        keywords =
            "and as assert async await break class continue def del elif else except finally " +
                "for from global if import in is lambda nonlocal not or pass raise return try " +
                "while with yield match case",
        lineComments = listOf("#"),
        tripleQuotes = true,
    )

private val rustSpec =
    LanguageSpec(
        keywords =
            "as async await break const continue crate dyn else enum extern fn for if impl in " +
                "let loop match mod move mut pub ref return static struct super trait type " +
                "unsafe use where while macro_rules i8 i16 i32 i64 i128 isize u8 u16 u32 u64 " +
                "u128 usize f32 f64 bool char str",
        lineComments = cStyle,
        blockComment = cBlock,
        charLiteralSingleQuote = true,
        macroBang = true,
    )

private val goSpec =
    LanguageSpec(
        keywords =
            "break case chan const continue default defer else fallthrough for func go goto if " +
                "import interface map package range return select struct switch type var int " +
                "int8 int16 int32 int64 uint uint8 uint16 uint32 uint64 uintptr float32 float64 " +
                "complex64 complex128 string bool byte rune error any",
        lineComments = cStyle,
        blockComment = cBlock,
        quotes = "\"'`",
        multilineQuotes = "`",
        charLiteralSingleQuote = true,
    )

private val cFamilySpec =
    LanguageSpec(
        keywords =
            "auto break case char const continue default do double else enum extern float for " +
                "goto if inline int long register restrict return short signed sizeof static " +
                "struct switch typedef union unsigned void volatile while class namespace " +
                "template typename public private protected virtual override final new delete " +
                "throw try catch using operator friend explicit constexpr noexcept static_cast " +
                "dynamic_cast reinterpret_cast const_cast bool include define ifdef ifndef " +
                "endif pragma elif undef std var string object foreach in out ref readonly " +
                "async await get set partial sealed abstract base is as lock params event " +
                "delegate decimal interface",
        lineComments = cStyle,
        blockComment = cBlock,
        charLiteralSingleQuote = true,
    )

private val swiftSpec =
    LanguageSpec(
        keywords =
            "associatedtype class deinit enum extension fileprivate func import init inout " +
                "internal let open operator private protocol public rethrows static struct " +
                "subscript typealias var break case continue default defer do else " +
                "fallthrough for guard if in repeat return switch where while as catch is " +
                "throw throws try async await some any Self super mutating override weak " +
                "lazy final",
        lineComments = cStyle,
        blockComment = cBlock,
        tripleQuotes = true,
    )

private val dartSpec =
    LanguageSpec(
        keywords =
            "abstract as assert async await break case catch class const continue covariant " +
                "default deferred do dynamic else enum export extends extension external " +
                "factory final finally for get if implements import in interface is late " +
                "library mixin new on operator part required rethrow return sealed set show " +
                "static super switch sync throw try typedef var void while with yield",
        lineComments = cStyle,
        blockComment = cBlock,
        tripleQuotes = true,
    )

private val scalaSpec =
    LanguageSpec(
        keywords =
            "abstract case catch class def do else extends final finally for forSome if " +
                "implicit import lazy match new object override package private protected " +
                "return sealed super throw trait try type val var while with yield given " +
                "using enum then",
        lineComments = cStyle,
        blockComment = cBlock,
        tripleQuotes = true,
        charLiteralSingleQuote = true,
    )

private val rubySpec =
    LanguageSpec(
        keywords =
            "alias and begin break case class def defined do else elsif end ensure for if in " +
                "module next not or redo rescue retry return super then undef unless until " +
                "when while yield require attr_accessor attr_reader puts",
        lineComments = listOf("#"),
        annotations = false,
    )

private val phpSpec =
    LanguageSpec(
        keywords =
            "abstract and array as break callable case catch class clone const continue " +
                "declare default do echo else elseif empty extends final finally fn for " +
                "foreach function global goto if implements include include_once instanceof " +
                "insteadof interface isset list match namespace new or print private " +
                "protected public readonly require require_once return static switch throw " +
                "trait try unset use var while xor yield",
        lineComments = listOf("//", "#"),
        blockComment = cBlock,
        variablePrefix = '$',
    )

private val shellSpec =
    LanguageSpec(
        keywords =
            "if then else elif fi case esac for select while until do done in function time " +
                "return exit break continue local export readonly declare unset source echo " +
                "cd sudo set shift trap eval exec alias",
        lineComments = listOf("#"),
        quotes = "\"'",
        multilineQuotes = "\"'",
        capitalizedTypes = false,
        extraIdentChars = "-",
        variablePrefix = '$',
        annotations = false,
    )

private val sqlSpec =
    LanguageSpec(
        keywords =
            "select from where and or not insert into values update set delete create table " +
                "drop alter add column index primary key foreign references join inner left " +
                "right outer full on as group by order having limit offset distinct union all " +
                "exists in is like between case when then else end view database if begin " +
                "commit rollback transaction default unique check constraint int integer " +
                "varchar text boolean date timestamp serial returning with asc desc count sum " +
                "avg min max",
        lineComments = listOf("--"),
        blockComment = cBlock,
        capitalizedTypes = false,
        caseInsensitive = true,
        annotations = false,
    )

private val luaSpec =
    LanguageSpec(
        keywords =
            "and break do else elseif end for function goto if in local not or repeat return " +
                "then until while",
        lineComments = listOf("--"),
        blockComment = "--[[" to "]]",
        annotations = false,
    )

private val jsonSpec =
    LanguageSpec(
        quotes = "\"",
        stringKeys = true,
        capitalizedTypes = false,
        annotations = false,
    )

private val yamlSpec =
    LanguageSpec(
        lineComments = listOf("#"),
        capitalizedTypes = false,
        extraIdentChars = "-.",
        keyTerminators = ":",
        annotations = false,
    )

private val tomlSpec =
    LanguageSpec(
        lineComments = listOf("#", ";"),
        capitalizedTypes = false,
        extraIdentChars = "-.",
        keyTerminators = "=",
        annotations = false,
    )

private val cssSpec =
    LanguageSpec(
        keywords = "important",
        blockComment = cBlock,
        lineComments = cStyle,
        capitalizedTypes = false,
        extraIdentChars = "-",
        keyTerminators = ":",
        variablePrefix = '$',
    )

private val markupSpec =
    LanguageSpec(
        blockComment = "<!--" to "-->",
        capitalizedTypes = false,
        extraIdentChars = "-:",
        markup = true,
        annotations = false,
    )

private val genericSpec =
    LanguageSpec(
        keywords =
            "if else for while do return function fn def func class struct enum import from " +
                "export let const var val public private protected static void new try catch " +
                "finally throw switch case break continue in of async await package interface " +
                "module use match when elif then end type impl",
        lineComments = listOf("//", "#"),
        blockComment = cBlock,
    )

private fun specFor(language: String?): LanguageSpec =
    when (language?.lowercase()?.trim()) {
        "kotlin",
        "kt",
        "kts" -> kotlinSpec
        "java" -> javaSpec
        "javascript",
        "js",
        "jsx",
        "mjs",
        "cjs",
        "typescript",
        "ts",
        "tsx",
        "node" -> jsSpec
        "python",
        "py",
        "python3",
        "py3",
        "gyp" -> pythonSpec
        "rust",
        "rs" -> rustSpec
        "go",
        "golang" -> goSpec
        "c",
        "h",
        "cpp",
        "c++",
        "cc",
        "cxx",
        "hpp",
        "objc",
        "objective-c",
        "cs",
        "c#",
        "csharp",
        "glsl",
        "hlsl",
        "cuda",
        "arduino",
        "zig" -> cFamilySpec
        "swift" -> swiftSpec
        "dart" -> dartSpec
        "scala",
        "groovy",
        "gradle" -> scalaSpec
        "ruby",
        "rb" -> rubySpec
        "php" -> phpSpec
        "bash",
        "sh",
        "shell",
        "zsh",
        "fish",
        "console",
        "shellscript",
        "powershell",
        "ps1",
        "dockerfile",
        "docker",
        "makefile",
        "make" -> shellSpec
        "sql",
        "mysql",
        "postgres",
        "postgresql",
        "sqlite",
        "plsql" -> sqlSpec
        "lua" -> luaSpec
        "json",
        "jsonc",
        "json5" -> jsonSpec
        "yaml",
        "yml" -> yamlSpec
        "toml",
        "ini",
        "conf",
        "properties",
        "env" -> tomlSpec
        "css",
        "scss",
        "sass",
        "less" -> cssSpec
        "html",
        "htm",
        "xml",
        "svg",
        "xhtml",
        "vue",
        "svelte",
        "plist" -> markupSpec
        else -> genericSpec
    }

/** Languages that should be rendered without any highlighting. */
private val plainLanguages = setOf("text", "txt", "plain", "plaintext", "output", "log")

internal object SyntaxHighlighter {
    fun highlight(code: String, language: String?, palette: SyntaxPalette): AnnotatedString {
        if (language != null && language.lowercase() in plainLanguages) {
            return AnnotatedString(code)
        }
        val spec = specFor(language)
        return buildAnnotatedString {
            append(code)
            // Guard against pathological inputs; a bug here must never break rendering.
            runCatching {
                Scanner(code, spec) { start, end, kind ->
                        addStyle(styleFor(kind, palette), start, end)
                    }
                    .run()
            }
        }
    }

    private fun styleFor(kind: TokenKind, p: SyntaxPalette): SpanStyle =
        when (kind) {
            TokenKind.Keyword -> SpanStyle(color = p.keyword)
            TokenKind.StringLit -> SpanStyle(color = p.string)
            TokenKind.Comment -> SpanStyle(color = p.comment, fontStyle = FontStyle.Italic)
            TokenKind.Number,
            TokenKind.Constant -> SpanStyle(color = p.number)
            TokenKind.Type -> SpanStyle(color = p.type)
            TokenKind.Function -> SpanStyle(color = p.function)
            TokenKind.Annotation -> SpanStyle(color = p.annotation)
            TokenKind.Property -> SpanStyle(color = p.property)
        }
}

private class Scanner(
    private val code: String,
    private val spec: LanguageSpec,
    private val sink: (Int, Int, TokenKind) -> Unit,
) {
    private val n = code.length

    private fun emit(start: Int, end: Int, kind: TokenKind) {
        if (end > start) sink(start, end.coerceAtMost(n), kind)
    }

    private fun isIdentStart(c: Char): Boolean =
        c.isLetter() || c == '_' || (c == '$' && '$' in spec.extraIdentChars)

    private fun isIdentChar(c: Char): Boolean =
        c.isLetterOrDigit() || c == '_' || c in spec.extraIdentChars

    private fun nextNonSpace(from: Int): Char? {
        var j = from
        while (j < n && (code[j] == ' ' || code[j] == '\t')) j++
        return if (j < n) code[j] else null
    }

    private fun lineEnd(from: Int): Int = code.indexOf('\n', from).let { if (it < 0) n else it }

    private fun isLineCommentAt(marker: String, i: Int): Boolean {
        if (!code.startsWith(marker, i)) return false
        if (marker == "#" || marker == ";") {
            // Avoid treating `$#`, `#fff`, `a#b` etc. as comments.
            val prev = if (i > 0) code[i - 1] else null
            if (prev != null && !prev.isWhitespace()) return false
        }
        return true
    }

    /** Returns the end index (exclusive) of a string starting at [start], or -1 if none. */
    private fun scanString(start: Int, quote: Char): Int {
        val multiline = quote in spec.multilineQuotes
        var j = start + 1
        while (j < n) {
            val ch = code[j]
            when {
                ch == '\\' -> j += 2
                ch == quote -> {
                    val end = j + 1
                    if (quote == '\'' && spec.charLiteralSingleQuote) {
                        // 'x' or an escape like '\n' / '\u{1F600}'; anything else is a
                        // lifetime/label, not a char literal.
                        val isChar =
                            end - start == 3 || (code[start + 1] == '\\' && end - start <= 12)
                        if (!isChar) return -1
                    }
                    return end
                }
                ch == '\n' && !multiline -> return -1
                else -> j++
            }
        }
        // Unterminated: only multiline strings (still streaming) extend to the end.
        return if (multiline && !(quote == '\'' && spec.charLiteralSingleQuote)) n else -1
    }

    fun run() {
        var i = 0
        var inTag = false
        while (i < n) {
            val c = code[i]

            val block = spec.blockComment
            if (block != null && code.startsWith(block.first, i)) {
                val close = code.indexOf(block.second, i + block.first.length)
                val end = if (close < 0) n else close + block.second.length
                emit(i, end, TokenKind.Comment)
                i = end
                continue
            }

            if (!spec.markup || !inTag) {
                val marker = spec.lineComments.firstOrNull { isLineCommentAt(it, i) }
                if (marker != null) {
                    val end = lineEnd(i)
                    emit(i, end, TokenKind.Comment)
                    i = end
                    continue
                }
            }

            if (spec.markup) {
                val next = if (i + 1 < n) code[i + 1] else null
                if (
                    !inTag &&
                        c == '<' &&
                        next != null &&
                        (next.isLetter() || next == '/' || next == '!' || next == '?')
                ) {
                    var j = i + 1
                    if (code[j] == '/' || code[j] == '!' || code[j] == '?') j++
                    while (j < n && (code[j].isLetterOrDigit() || code[j] in "-_:.")) j++
                    emit(i, j, TokenKind.Keyword)
                    i = j
                    inTag = true
                    continue
                }
                if (inTag && (c == '>' || ((c == '/' || c == '?') && next == '>'))) {
                    val end = if (c == '>') i + 1 else i + 2
                    emit(i, end, TokenKind.Keyword)
                    i = end
                    inTag = false
                    continue
                }
                if (!inTag) {
                    if (c == '&') {
                        val semi = code.indexOf(';', i)
                        if (semi in (i + 2)..(i + 10)) {
                            emit(i, semi + 1, TokenKind.Constant)
                            i = semi + 1
                            continue
                        }
                    }
                    i++
                    continue
                }
            }

            if (
                spec.tripleQuotes &&
                    (code.startsWith("\"\"\"", i) || code.startsWith("'''", i))
            ) {
                val delimiter = code.substring(i, i + 3)
                val close = code.indexOf(delimiter, i + 3)
                val end = if (close < 0) n else close + 3
                emit(i, end, TokenKind.StringLit)
                i = end
                continue
            }

            if (c in spec.quotes) {
                val end = scanString(i, c)
                if (end > 0) {
                    val kind =
                        if (spec.stringKeys && nextNonSpace(end) == ':') TokenKind.Property
                        else TokenKind.StringLit
                    emit(i, end, kind)
                    i = end
                    continue
                }
            }

            if (spec.annotations && c == '@' && i + 1 < n && isIdentStart(code[i + 1])) {
                var j = i + 1
                while (j < n && (isIdentChar(code[j]) || code[j] == '.')) j++
                emit(i, j, TokenKind.Annotation)
                i = j
                continue
            }

            if (spec.variablePrefix == c && i + 1 < n) {
                val next = code[i + 1]
                if (next == '{') {
                    val close = code.indexOf('}', i + 2)
                    if (close > 0 && close < lineEnd(i)) {
                        emit(i, close + 1, TokenKind.Property)
                        i = close + 1
                        continue
                    }
                } else if (next.isLetterOrDigit() || next == '_' || next in "@#?*!") {
                    var j = i + 2
                    if (next.isLetter() || next == '_') {
                        while (j < n && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                    }
                    emit(i, j, TokenKind.Property)
                    i = j
                    continue
                }
            }

            if (c.isDigit() && (i == 0 || !isIdentChar(code[i - 1]))) {
                var j = i + 1
                while (
                    j < n &&
                        (code[j].isLetterOrDigit() ||
                            code[j] == '_' ||
                            (code[j] == '.' && j + 1 < n && code[j + 1].isDigit()))
                ) j++
                emit(i, j, TokenKind.Number)
                i = j
                continue
            }

            if (isIdentStart(c)) {
                var j = i + 1
                while (j < n && isIdentChar(code[j])) j++
                // Don't let a trailing '-' or '.' (e.g. `foo-` in shell, `a.` in YAML) join.
                while (j > i + 1 && code[j - 1] in "-.") j--
                val word = code.substring(i, j)
                val lookup = if (spec.caseInsensitive) word.lowercase() else word
                val next = nextNonSpace(j)
                val kind =
                    when {
                        spec.markup -> TokenKind.Property
                        lookup in spec.keywords -> TokenKind.Keyword
                        word in constants -> TokenKind.Constant
                        next != null && next in spec.keyTerminators -> TokenKind.Property
                        next == '(' -> TokenKind.Function
                        spec.macroBang && j < n && code[j] == '!' -> TokenKind.Function
                        spec.capitalizedTypes && word[0].isUpperCase() -> TokenKind.Type
                        else -> null
                    }
                if (kind != null) emit(i, j, kind)
                i = j
                continue
            }

            i++
        }
    }
}
