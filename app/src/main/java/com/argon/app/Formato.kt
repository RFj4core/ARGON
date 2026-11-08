package com.argon.app

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan

object Formato {

    private val reCodigo = Regex("`([^`\\n]+)`")
    private val reMath = Regex("\\$([^$\\n]+)\\$")
    private val reNegrita = Regex("\\*\\*(?!\\s)(.+?)(?<!\\s)\\*\\*")
    private val reCursiva = Regex("(?<![\\w*])\\*(?!\\s)([^*\\n]+?)(?<!\\s)\\*(?![\\w*])")
    private val reEnlace = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")
    private val reTitulo = Regex("^(#{1,4})\\s+(.*)$")
    private val reVineta = Regex("^(\\s*)[-*•]\\s+(.*)$")
    private val reNumero = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
    private val reRegla = Regex("^(-{3,}|\\*{3,}|_{3,})$")

    private const val FLAG = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

    private fun convertirLatex(latex: String): String {
        var s = latex
        
        // Símbolos griegos y operadores comunes
        s = s.replace("\\pm", "±")
        s = s.replace("\\pi", "π")
        s = s.replace("\\alpha", "α")
        s = s.replace("\\beta", "β")
        s = s.replace("\\theta", "θ")
        s = s.replace("\\lambda", "λ")
        s = s.replace("\\mu", "μ")
        s = s.replace("\\sigma", "σ")
        s = s.replace("\\omega", "ω")
        s = s.replace("\\Delta", "Δ")
        s = s.replace("\\infty", "∞")
        s = s.replace("\\approx", "≈")
        s = s.replace("\\neq", "≠")
        s = s.replace("\\leq", "≤")
        s = s.replace("\\geq", "≥")
        s = s.replace("\\cdot", "·")
        s = s.replace("\\div", "÷")
        s = s.replace("\\times", "×")

        // Fracciones \frac{a}{b} -> (a / b)
        val fracRegex = Regex("\\\\frac\\{([^}]+)\\}\\{([^}]+)\\}")
        s = fracRegex.replace(s) { "(${it.groupValues[1]} / ${it.groupValues[2]})" }

        // Raíces \sqrt{a} -> √(a)
        val sqrtRegex = Regex("\\\\sqrt\\{([^}]+)\\}")
        s = sqrtRegex.replace(s) { "√(${it.groupValues[1]})" }

        // Superíndices simples a^2 -> a² (ampliado para múltiples caracteres o números)
        val supRegex = Regex("([a-zA-Z0-9α-ωΑ-Ω)])\\^([0-9n+-]+)")
        s = supRegex.replace(s) { match ->
            val base = match.groupValues[1]
            val exp = match.groupValues[2]
            val supExp = exp.map { c ->
                when (c) {
                    '0' -> '⁰'
                    '1' -> '¹'
                    '2' -> '²'
                    '3' -> '³'
                    '4' -> '⁴'
                    '5' -> '⁵'
                    '6' -> '⁶'
                    '7' -> '⁷'
                    '8' -> '⁸'
                    '9' -> '⁹'
                    'n' -> 'ⁿ'
                    '+' -> '⁺'
                    '-' -> '⁻'
                    else -> c
                }
            }.joinToString("")
            "$base$supExp"
        }

        // Subíndices a_1 -> a₁
        val subRegex = Regex("([a-zA-Zα-ωΑ-Ω])_([0-9ni])")
        s = subRegex.replace(s) { match ->
            val base = match.groupValues[1]
            val sub = match.groupValues[2]
            val subChar = when (sub) {
                "0" -> "₀"
                "1" -> "₁"
                "2" -> "₂"
                "3" -> "₃"
                "4" -> "₄"
                "5" -> "₅"
                "6" -> "₆"
                "7" -> "₇"
                "8" -> "₈"
                "9" -> "₉"
                "n" -> "ₙ"
                "i" -> "ᵢ"
                else -> "_$sub"
            }
            "$base$subChar"
        }

        // Limpiar llaves sobrantes
        s = s.replace("{", "").replace("}", "")
        return s
    }

    private fun aplicar(sb: SpannableStringBuilder, re: Regex, delim: Int, estilos: () -> List<Any>) {
        while (true) {
            val m = re.find(sb) ?: break
            val ini = m.range.first
            val fin = m.range.last + 1
            sb.delete(fin - delim, fin)
            sb.delete(ini, ini + delim)
            for (e in estilos()) sb.setSpan(e, ini, fin - 2 * delim, FLAG)
        }
    }

    private fun linea(texto: String, fondoCodigo: Int): SpannableStringBuilder {
        val t = reEnlace.replace(texto) { it.groupValues[1] + " (" + it.groupValues[2] + ")" }
        val sb = SpannableStringBuilder(t)
        
        // Soporte para fórmulas matemáticas en línea con $...$
        aplicar(sb, reMath, 1) {
            val original = sb.subSequence(it.range.first + 1, it.range.last).toString()
            val convertido = convertirLatex(original)
            sb.replace(it.range.first, it.range.last + 1, convertido)
            listOf(
                TypefaceSpan("monospace"),
                ForegroundColorSpan(Color.parseColor("#A8D1FF"))
            )
        }

        aplicar(sb, reCodigo, 1) {
            listOf(
                TypefaceSpan("monospace"),
                BackgroundColorSpan(fondoCodigo),
                ForegroundColorSpan(Color.parseColor("#F0C9A8"))
            )
        }
        aplicar(sb, reNegrita, 2) { listOf(StyleSpan(Typeface.BOLD)) }
        aplicar(sb, reCursiva, 1) { listOf(StyleSpan(Typeface.ITALIC)) }
        return sb
    }

    fun render(md: String, fondoCodigo: Int): CharSequence {
        val out = SpannableStringBuilder()
        val lineas = md.split("\n")
        for ((i, l0) in lineas.withIndex()) {
            var l = l0.trimEnd()
            var tamano = 1f
            var negritaLinea = false
            var prefijo = ""
            val t = reTitulo.find(l)
            val v = reVineta.find(l)
            val n = reNumero.find(l)
            if (t != null) {
                val nivel = t.groupValues[1].length
                l = t.groupValues[2]
                tamano = when (nivel) {
                    1 -> 1.35f
                    2 -> 1.2f
                    else -> 1.1f
                }
                negritaLinea = true
            } else if (v != null) {
                prefijo = " ".repeat(v.groupValues[1].length) + "•  "
                l = v.groupValues[2]
            } else if (n != null) {
                prefijo = " ".repeat(n.groupValues[1].length) + n.groupValues[2] + ".  "
                l = n.groupValues[3]
            } else if (reRegla.matches(l.trim())) {
                l = "────────────"
            }
            val ini = out.length
            out.append(prefijo)
            out.append(linea(l, fondoCodigo))
            if (tamano != 1f) out.setSpan(RelativeSizeSpan(tamano), ini, out.length, FLAG)
            if (negritaLinea) out.setSpan(StyleSpan(Typeface.BOLD), ini, out.length, FLAG)
            if (i < lineas.size - 1) out.append("\n")
        }
        return out
    }
}
