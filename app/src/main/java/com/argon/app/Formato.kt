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
        
        // Símbolos griegos completos (minúsculas y mayúsculas)
        s = s.replace("\\alpha", "α")
        s = s.replace("\\beta", "β")
        s = s.replace("\\gamma", "γ")
        s = s.replace("\\delta", "δ")
        s = s.replace("\\epsilon", "ε")
        s = s.replace("\\zeta", "ζ")
        s = s.replace("\\eta", "η")
        s = s.replace("\\theta", "θ")
        s = s.replace("\\iota", "ι")
        s = s.replace("\\kappa", "κ")
        s = s.replace("\\lambda", "λ")
        s = s.replace("\\mu", "μ")
        s = s.replace("\\nu", "ν")
        s = s.replace("\\xi", "ξ")
        s = s.replace("\\pi", "π")
        s = s.replace("\\rho", "ρ")
        s = s.replace("\\sigma", "σ")
        s = s.replace("\\tau", "τ")
        s = s.replace("\\upsilon", "υ")
        s = s.replace("\\phi", "ϕ")
        s = s.replace("\\chi", "χ")
        s = s.replace("\\psi", "ψ")
        s = s.replace("\\omega", "ω")

        s = s.replace("\\Gamma", "Γ")
        s = s.replace("\\Delta", "Δ")
        s = s.replace("\\Theta", "Θ")
        s = s.replace("\\Lambda", "Λ")
        s = s.replace("\\Xi", "Ξ")
        s = s.replace("\\Pi", "Π")
        s = s.replace("\\Sigma", "Σ")
        s = s.replace("\\Upsilon", "Υ")
        s = s.replace("\\Phi", "Φ")
        s = s.replace("\\Psi", "Ψ")
        s = s.replace("\\Omega", "Ω")

        // Símbolos matemáticos y operadores avanzados
        s = s.replace("\\pm", "±")
        s = s.replace("\\mp", "∓")
        s = s.replace("\\times", "×")
        s = s.replace("\\div", "÷")
        s = s.replace("\\cdot", "·")
        s = s.replace("\\ast", "∗")
        s = s.replace("\\star", "⋆")
        s = s.replace("\\circ", "∘")
        s = s.replace("\\bullet", "•")
        s = s.replace("\\oplus", "⊕")
        s = s.replace("\\otimes", "⊗")
        s = s.replace("\\odot", "⊙")
        s = s.replace("\\infinity", "∞")
        s = s.replace("\\infty", "∞")
        s = s.replace("\\approx", "≈")
        s = s.replace("\\sim", "∼")
        s = s.replace("\\simeq", "≃")
        s = s.replace("\\cong", "≅")
        s = s.replace("\\equiv", "≡")
        s = s.replace("\\ne", "≠")
        s = s.replace("\\neq", "≠")
        s = s.replace("\\le", "≤")
        s = s.replace("\\leq", "≤")
        s = s.replace("\\ge", "≥")
        s = s.replace("\\geq", "≥")
        s = s.replace("\\ll", "≪")
        s = s.replace("\\gg", "≫")
        s = s.replace("\\subset", "⊂")
        s = s.replace("\\supset", "⊃")
        s = s.replace("\\subseteq", "⊆")
        s = s.replace("\\supseteq", "⊇")
        s = s.replace("\\in", "∈")
        s = s.replace("\\notin", "∉")
        s = s.replace("\\ni", "∋")
        s = s.replace("\\cup", "∪")
        s = s.replace("\\cap", "∩")
        s = s.replace("\\setminus", "∖")
        s = s.replace("\\emptyset", "∅")
        s = s.replace("\\forall", "∀")
        s = s.replace("\\exists", "∃")
        s = s.replace("\\nexists", "∄")
        s = s.replace("\\prop", "∝")
        s = s.replace("\\propto", "∝")
        s = s.replace("\\partial", "∂")
        s = s.replace("\\nabla", "∇")
        s = s.replace("\\therefore", "∴")
        s = s.replace("\\because", "∵")
        s = s.replace("\\angle", "∠")
        s = s.replace("\\perp", "⊥")
        s = s.replace("\\parallel", "∥")
        s = s.replace("\\to", "→")
        s = s.replace("\\gets", "←")
        s = s.replace("\\rightarrow", "→")
        s = s.replace("\\leftarrow", "←")
        s = s.replace("\\Rightarrow", "⇒")
        s = s.replace("\\Leftarrow", "⇐")
        s = s.replace("\\leftrightarrow", "↔")
        s = s.replace("\\Leftrightarrow", "⇔")
        s = s.replace("\\mapsto", "↦")

        // Operadores grandes
        s = s.replace("\\sum", "∑")
        s = s.replace("\\prod", "∏")
        s = s.replace("\\coprod", "∐")
        s = s.replace("\\int", "∫")
        s = s.replace("\\iint", "∬")
        s = s.replace("\\iiint", "∭")
        s = s.replace("\\oint", "∮")

        // Funciones y operadores matemáticos
        s = s.replace("\\sin", "sen")
        s = s.replace("\\cos", "cos")
        s = s.replace("\\tan", "tan")
        s = s.replace("\\csc", "csc")
        s = s.replace("\\sec", "sec")
        s = s.replace("\\cot", "cot")
        s = s.replace("\\arcsin", "arcsen")
        s = s.replace("\\arccos", "arccos")
        s = s.replace("\\arctan", "arctan")
        s = s.replace("\\sinh", "senh")
        s = s.replace("\\cosh", "cosh")
        s = s.replace("\\tanh", "tanh")
        s = s.replace("\\log", "log")
        s = s.replace("\\ln", "ln")
        s = s.replace("\\lg", "lg")
        s = s.replace("\\lim", "lim")
        s = s.replace("\\max", "max")
        s = s.replace("\\min", "min")
        s = s.replace("\\sup", "sup")
        s = s.replace("\\inf", "inf")
        s = s.replace("\\det", "det")
        s = s.replace("\\dim", "dim")
        s = s.replace("\\ker", "ker")
        s = s.replace("\\deg", "deg")
        s = s.replace("\\gcd", "mcd")

        // Fracciones
        val fracRegex = Regex("\\\\frac\\{([^}]+)\\}\\{([^}]+)\\}")
        s = fracRegex.replace(s) { match -> "(${match.groupValues[1]} / ${match.groupValues[2]})" }

        // Raíces enésimas y cuadradas
        val sqrtNRegex = Regex("\\\\sqrt\\[([^\\]]+)\\]\\{([^}]+)\\}")
        s = sqrtNRegex.replace(s) { match -> "(${match.groupValues[1]})√(${match.groupValues[2]})" }

        val sqrtRegex = Regex("\\\\sqrt\\{([^}]+)\\}")
        s = sqrtRegex.replace(s) { match -> "√(${match.groupValues[1]})" }

        // Superíndices
        val supRegex = Regex("([a-zA-Z0-9α-ωΑ-Ωεϕψρφ)])\\^([0-9n+-]+)")
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

        // Subíndices
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
        val t = reEnlace.replace(texto) { match -> match.groupValues[1] + " (" + match.groupValues[2] + ")" }
        val sb = SpannableStringBuilder(t)
        
        while (true) {
            val m = reMath.find(sb) ?: break
            val original = m.groupValues[1]
            val convertido = convertirLatex(original)
            val ini = m.range.first
            val fin = m.range.last + 1
            sb.delete(fin - 1, fin)
            sb.delete(ini, ini + 1)
            val realFin = ini + convertido.length
            sb.replace(ini, realFin, convertido)
            sb.setSpan(TypefaceSpan("monospace"), ini, realFin, FLAG)
            sb.setSpan(ForegroundColorSpan(Color.parseColor("#A8D1FF")), ini, realFin, FLAG)
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
