package unicasc;

import java.util.HashMap;
import java.util.Map;

public class Unicode {
	private static final Map<String, String> u2a = new HashMap<>();
	private static final Map<String, String> a2u = new HashMap<>();

	private static final String[][] table = { 
			{ "\u225C", "==" }, // ≜
			{ "\u2190", "<-" }, // ←
			{ "\u2192", "->" }, // →
			{ "\u21A6", "|->" }, // ↦

			{ "\u27E8", "<<" }, // ⟨
			{ "\u27E9", ">>" }, // ⟩
			{ "\u27E9_", ">>_" }, // ⟩

			{ "\u2200\u2200", "\\AA" }, // ∀∀
			{ "\u2203\u2203", "\\EE" }, // ∃∃
			{ "\u2610︎", "[]" }, // ☐
			{ "\u2662", "<>" }, // ♢
			{ "\u2933", "~>" }, // ⤳
			{ "\u2945", "-+->" },

			{ "\u2200", "\\A", "\\forall" }, // ∀
			{ "\u2203", "\\E", "\\exists" }, // ∃

			{ "\u00ac", "~", "\\lnot", "\\neg" }, // ¬
			{ "\u2227", "/\\", "\\land" }, // ∧
			{ "\u2228", "\\/", "\\lor" }, // ∨
			{ "\u21D2", "=>" }, // ⇒
			{ "\u2263", "<=>", "\\equiv" }, // ≣ / ⇔ \u21D4

			{ "\u2260", "#", "/=" }, // ≠

			{ "\u2208", "\\in" },       // ∈
			{ "\u2209", "\\notin" },    // ∉
			{ "\u2282", "\\subset" },   // ⊂
			{ "\u2286", "\\subseteq" }, // ⊆
			{ "\u2283", "\\supset" },   // ⊃
			{ "\u2287", "\\supseteq" }, // ⊇

			{ "\u2229", "\\cap", "\\intersect" }, // ∩
			{ "\u222A", "\\cup", "\\union" },     // ∪
			{"\u228E", "\\uplus" },               // ⊎
			
			{ "\u2264", "<=", "=<", "\\leq" }, // ≤
			{ "\u2265", ">=", "\\geq" },       // ≥
			{ "\u226A", "\\ll" },              // ≪
			{ "\u226B", "\\gg" },              // ≫

			// { "%", "\\mod" }, 
			{ "\u22C5", "\\cdot" },          // ⋅
			{ "\u00D7", "\\X", "\\times" },  // ×
			{ "\u00F7", "\\div" },           // ÷

			{ "\u2295", "(+)", "\\oplus" },    // ⊕
			{ "\u2296", "(-)", "\\ominus" },   // ⊖
			{ "\u2297", "(\\X)", "\\otimes" }, // ⊗
			{ "\u2298", "(/)", "\\oslash" },   // ⊘
			{ "\u2299", "(.)", "\\odot" },     // ⊙

			{ "\u25CB", "\\o", "\\circ" }, // ○
			{ "\u25EF", "\\bigcirc" },     // ◯
			{ "\u2022", "\\bullet" },      // •
			{ "\u2B51", "\\star" },        // ⭑

			{ "\u227A", "\\prec" },   // ≺
			{ "\u2AAF", "\\preceq" }, // ⪯
			{ "\u227B", "\\succ" },   // ≻
			{ "\u2AB0", "\\succeq"},  // ⪰

			{ "\u228F", "\\sqsubset" },  // ⊏
			{ "\u2291", "\\sqsubseteq" }, // ⊑
			{ "\u2290", "\\sqsupset" },  // ⊐
			{ "\u2292", "\\sqsupseteq" }, // ⊒
			{ "\u2293", "\\sqcap" }, // ⊓
			{ "\u2294", "\\sqcup" }, // ⊔

			{ "\u224D", "\\asymp" },  // ≍
			{ "\u2240", "\\wr" },     // ≀
			{ "\u2245", "\\cong" },   // ≅
			{ "\u221D", "\\propto" }, // ∝
			{ "\u2248", "\\approx" }, // ≈
			{ "\u2250", "\\doteq" },  // ≐
			{ "\u2243", "\\simeq" },  // ≃
			{ "\uFF5E", "\\sim" },    // ～

			{ "\u22A2", "|-" }, // ⊢
			{ "\u22A8", "|=" }, // ⊨
			{ "\u22A3", "-|" }, // ⊣
			{ "\u2AE4", "=|" }, // ⫤

			{ "\u2016", "||" }, // ‖
			// { "\u207A", "^+" }, // ⁺			
	};


	static {
		// initialize maps
		for (String[] row : table) {
			final String u = row[0]; // unicode
			u2a.put(u, row[1]);
			for (int i = 1; i < row.length; i++)
				a2u.put(row[i], u);
		}
	}

	public static String u2a(String u) {
		return u2a.get(u);
	}

	public static String a2u(String a) {
		return a2u.get(a);
	}

	public static String a2uc(String a) {
		String res = a2u.get(a);
		if (res != null && !u2a.get(res).equals(a))
			res = null;
		return res;
	}

	private Unicode() {
	}
}
