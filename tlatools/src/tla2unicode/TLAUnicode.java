// Copyright (c) 2016 Microsoft Corporation.  All rights reserved.

/***************************************************************************
*                                                                          *
* This program converts TLA+ specifications from ASCII to Unicode          * 
* representation and vice-versa.                                           *
*                                                                          *
* When converting to Unicode, symbols in comments are not converted.       *
* When converting to ASCII, symbols in comments are converted char-by-char *
* to ensure no Unicode characters are left in the file.                    *
*                                                                          *
* BuiltInSymbols.Initialize                                                *
*    Initializes tables containing information about TLA's built-in        *
*    symbols.                                                              *
*                                                                          *
* TokenizeSpec.Tokenize                                                    *
*    Reads the input file and turns it into an array of array of Token     *
*    objects `spec', where spec[i][j] is item j on line i.  Each line of   *
*    a comment is a single token.  (In the Java program, all numbering     *
*    starts from 0.  The error messages translate into the numbering       *
*    system used by most humans, in which the first object is object       *
*    number 1.)  This method does not detect PF_STEP tokens such           *
*    as "<2>3a.".                                                          *
*                                                                          *
* TokenizeSpec.FindPfStepTokens                                            *
*    Converts a sequence of tokens that represent a proof step             *
*    number into a single PF_STEP token.                                   *
*                                                                          *
* CommentToken.ProcessComments                                             *
*    Determines which comment tokens are part of a single multi-line       *
*    comment, and indicates this by setting the subtype field of           *
*    comment tokens.                                                       *
*                                                                          *
* FormatComments.Initialize                                                *
*    Initializes tables used for formatting comments.  One table           *
*    contains common English words, which are read from the file           *
*    Parameters.WordFile                                                   *
*                                                                          *
* FindAlignments.FindAlignment                                             *
*    Determines what tokens should be aligned with what other tokens.      *
*    This is indicated by setting the tokens' belowAlign and aboveAlign    *
*    fields.  It also sets the isAlignmentPoint flag for each token        *
*    whose left-hand edge is a point used by some other token for its      *
*    alignment.                                                            *
*                                                                          *
***************************************************************************/
package tla2unicode;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import tla2tex.BuiltInSymbols;
import tla2tex.CharReader;
import tla2tex.CommentToken;
import tla2tex.Debug;
import tla2tex.FileCharReader;
import tla2tex.FindAlignments;
import tla2tex.InputStreamCharReader;
import tla2tex.OutputFileWriter;
import tla2tex.TLA2TexException;
import tla2tex.Token;
import tla2tex.TokenizeSpec;
import util.ToolIO;

public class TLAUnicode {
	static final String APP = "unicasc.TLAUnicode";
	static final String VERSION = APP + " Version 1.0";
	
	static final String USAGE = "USAGE: java " + APP + " OP input.tla [output.tla]"
			+ "\nOP is -u2a | -a2u to convert from ASCII to Unicode or vice-versa, respectively."
			+ "\nIf the output file isn't specified, the conversion is printed to the standard output.";
	
  	private static boolean debug = false ; // True if the -debug option is chosen.
	
	public static String convert(boolean toU, String input) {
		final StringWriter out = new StringWriter();
    	convert(toU, new StringReader(input), out);
    	return out.toString();
	}
	
	public static void convert(boolean toU, InputStream input, OutputStream output) {
		convert(toU, new InputStreamCharReader(input), new OutputFileWriter(output, null));
	}
	
	public static TokenPosition convert(boolean toU, Reader input, Writer output) {
		return convert(toU, new InputStreamCharReader(input), new OutputFileWriter(output, null));
	}
	
	public static void convert(boolean toU, Path inFile, Path outFile) {
		final CharReader input = new FileCharReader(inFile.normalize().toAbsolutePath().toString());
		final OutputFileWriter output = new OutputFileWriter(outFile.normalize().toAbsolutePath().toString());
		convert(toU, input, output);
	}
	
	// Main entry point
	public static TokenPosition convert(boolean toU, CharReader input, OutputFileWriter output) {
		BuiltInSymbols.Initialize(); // Initialize class BuiltInSymbols

		// Read and tokenize the spec.
		final Token[][] spec = TokenizeSpec.Tokenize(input, TokenizeSpec.MODULE);

		Token.FindPfStepTokens(spec); // Convert proof-step numbers to PF_STEP tokens.
		// Debug.print2DArray(spec, "tok");

		CommentToken.ProcessComments(spec); // Process the comment tokens.
		// Debug.print2DArray(spec, "com");

		// As comments aren't converted to U, mistakenly keeping alignment with a comment
		// can break significant alignment, so we align without comments
		final Token[][] noCommentSpec = filterOutComments(spec);
		FindAlignments.FindAlignments(noCommentSpec); // Add the alignment pointers to spec.
		
		// Debug.print2DArray(spec, "align");
		
		convert(toU, spec, noCommentSpec, output); // Write output
		return new TokenPosition(toU, spec);
	}
	
	private static void convert(boolean toU, Token[][] spec, Token[][] noCommentSpec, OutputFileWriter writer) {
		// This method performs the actual conversion
		
		List<CommentToken> leftComments = null;
		// boolean inBeginModule = false;
		for (int line = 0; line < spec.length; line++) {
			final StringBuilder out = new StringBuilder();
			leftComments = new ArrayList<>(); // left comments that we may need to move to the end of the line 
			boolean onlyComments = true; // we've only encountered comment tokens on this line so far
			boolean keepLeftComments = false;
			
			for (int item = 0; item < spec[line].length; item++) {
				final Token tok = spec[line][item];
				// System.out.println("(" + line + ", " + item + "): " + tok); if (item == spec[line].length - 1) System.out.println("$$$$$$$$$");
				
				// if line ends with an open comment or a line comment and we have left comments to move, 
			    // we wait to output the comment.
				if (keepLeftComments && item == spec[line].length - 1 && tok.type == Token.COMMENT) {
					final CommentToken ctok = (CommentToken) tok;
					// append skipped last comment token
					if (ctok.rsubtype == CommentToken.BEGIN_OVERRUN || ctok.rsubtype == CommentToken.LINE)
						continue;
				}
				
				//---- Align token ----
				final int origSpace = tok.column - (item > 0 ? spec[line][item - 1].column + spec[line][item - 1].getWidth() : 0);
				int space = -1; // How much space to leave before the token
				if (tok.aboveAlign.line != -1 && tok.type != Token.COMMENT) {
					// If aligned to a token above -- try to keep alignment
					final Token align = tok.aboveAlign.toToken(noCommentSpec);
					
					// If this token isn't a comment but it's been aligned with a comment
					// try to see if it can be aligned with a higher, non-comment line
					// As comments aren't converted to U, mistakenly keeping alignment with a comment
					// can break significant alignment
//					if (tok.type != Token.COMMENT) {
//						while (align.type == Token.COMMENT && align.aboveAlign.line != -1) {
//							// System.out.println("Fixing alignment of " + tok + " from " + align + " to " + align.aboveAlign.toToken(noCommentSpec));
//							align = align.aboveAlign.toToken(noCommentSpec);
//						}
//					}
					
					if (align.column == tok.column && align.outcolumn >= 0) {
						final int column = out.length();
						space = align.outcolumn - column;
						
						// If we're the first non-comment token, we must align.
						// If we can't, we move all left comments to the end of the line. 
						// We drop them from the output line, and keep them in leftComments.
						if (space < 0 && onlyComments && tok.type != Token.COMMENT) {
							out.delete(0, out.length()); // reset line
							space = align.outcolumn;
							
							keepLeftComments = true;
							for (CommentToken ctok : leftComments)
								ctok.outcolumn = -1;
							
							if (!leftComments.isEmpty() && leftComments.get(0).rsubtype == CommentToken.END_OVERRUN) {
								out.append("*)");
								space -= 2;
							}
						}
//						else if (space == 0 && origSpace > 1) // maybe necessary
//							space = 1;
					}
				}
				
				if (space < 0) // If we don't need to or can't align, keep original spacing.
					space = origSpace;
				else if (space == 0 && origSpace > 0) // keep a minimum of one space
					space = 1;
				
				Debug.Assert(space >= 0, tok + (item > 0 ? " :: " + spec[line][item - 1] : ""));
				
				appendSpaces(out, space);
				
				if (tok.type != Token.COMMENT) {
					onlyComments = false;
					if (!keepLeftComments)
						leftComments = null;
				}
				
				tok.outcolumn = out.length();
				Debug.Assert(toU // The following invariant always holds:
						? tok.outcolumn <= tok.column  // when -> U, token moves to the left  (or not at all)
						: tok.outcolumn >= tok.column, // when -> A, token moves to the right (or not at all)
					tok.toString() + " :: column: " + tok.column + " outcolumn: " + tok.outcolumn);

				//----- Output token ----
				
				switch (tok.type) {
				case Token.BUILTIN: {
					// Here we actually convert the symbol
					String alt = toU ? Unicode.a2u(tok.string) : Unicode.u2a(tok.string);
					out.append(alt != null ? alt : tok.string);
					break;
				}
				
//				case Token.DASHES:
//				case Token.END_MODULE:
//					inBeginModule = convertDashes(toU, out, spec, line, item, inBeginModule);
//					break;
					
				case Token.STRING:
					out.append("\"" + tok.string + "\"");
					break;

				case Token.COMMENT:
					final CommentToken ctok = (CommentToken) tok; // the current comment token
					if (onlyComments && leftComments != null)
						leftComments.add(ctok);
					appendCommentToken(toU, out, ctok);
					break;

				case Token.NUMBER:
				case Token.IDENT:
				case Token.PCAL_LABEL:
				case Token.DASHES:
				case Token.END_MODULE:
				case Token.PROLOG:
				case Token.EPILOG:
				case Token.PF_STEP:
					out.append(tok.string);
					break;

				default:
					Debug.ReportBug("Bad token type found.");
					break;
				}
			}
			
			if (keepLeftComments) { // we have comments to move to the end of the line
				for (CommentToken ctok : leftComments) {
					out.append(" (*");
					appendAndConvertCommentString(toU, out, ctok.string);
					out.append("*)");
				}
				final Token last = spec[line][spec[line].length-1]; 
				if (last.type == Token.COMMENT) {
					final CommentToken ctok = (CommentToken) last;
					// append skipped last comment token
					if (ctok.rsubtype == CommentToken.BEGIN_OVERRUN || ctok.rsubtype == CommentToken.LINE) {
						out.append(" ");
						appendCommentToken(toU, out, ctok);
					}
				}
			}
			
			writer.putLine(out.toString());
		}
		writer.close();
	}
    
	private static void appendCommentToken(boolean toU, StringBuilder out, CommentToken ctok) {
		final String commentString = ctok.string;
		switch (ctok.rsubtype) {
		case CommentToken.NORMAL:
			out.append("(*");
			StringBuilder line = new StringBuilder(); 
			appendAndConvertCommentString(toU, line, commentString);
			if (ctok.subtype == CommentToken.MULTI)
				adjustWidthTo(line, commentString.length());
			out.append(line);
			out.append("*)");
			break;
		case CommentToken.LINE:
			out.append("\\*");
			appendAndConvertCommentString(toU, out, commentString);
			break;
		case CommentToken.BEGIN_OVERRUN:
			if (ctok.getWidth() > 0)
				out.append("(*");
				appendAndConvertCommentString(toU, out, commentString);
			break;
		case CommentToken.END_OVERRUN:
			appendAndConvertCommentString(toU, out, commentString);
			out.append("*)");
			break;
		case CommentToken.OVERRUN:
			appendAndConvertCommentString(toU, out, commentString);
			break;
		default:
			Debug.ReportBug("Bad CommentToken subtype found.");
		}
	}
	
	private static void appendAndConvertCommentString(boolean toU, StringBuilder out, String commentString) {
		out.append(Unicode.convert(toU, commentString));
//		if (toU)
//			out.append(commentString);
//		else {
//			// We only support BMP chars, i.e. fit in a `char`, so we can work with chars rather than codepoints
//			// Debug.Assert(isBMP(commentString), "Comment " + commentString + " contains non-BMP Unicode characters");
//			char prev = 0; // the previous character
//			for (int i = 0; i < commentString.length(); i++) { 
//				final char c = commentString.charAt(i);
//				if (Unicode.cu2a(c) != null /*!isASCII(c)*/) {
//					String s = Unicode.cu2a(c);
//					if (!Character.isWhitespace(prev))
//						out.append(' '); // add whitespace before a unicode char
//					out.append(s);
//				} else if (!isASCII(c)) {
//					Debug.Assert(false, "An unrecognized Unicode character " + c
//							+ " was found in comment " + commentString);
//				} else {
//					if (Unicode.cu2a(prev) != null)
//						out.append(' '); // add whitespace following a unicode char (or else /\x)
//					out.append(c);
//				}
//				prev = c;
//			}
//		}
	}
	
	private static boolean convertDashes(boolean toU, StringBuilder sb, Token[][] spec, int line, int item, boolean inBeginModule) {
		final Token tok = spec[line][item];
		if (tok.type == Token.END_MODULE) {
			sb.append(UnicodeConstants.UP_AND_RIGHT);
			appendMany(sb, UnicodeConstants.HORIZONTAL, tok.string.length() - 2);
			sb.append(UnicodeConstants.UP_AND_LEFT);
			return false;
		} else if (tok.type == Token.DASHES) {
			if (inBeginModule) {
				appendMany(sb, UnicodeConstants.HORIZONTAL, tok.string.length() - 1);
				sb.append(UnicodeConstants.DOWN_AND_LEFT);
				return false;
			} else if ((item + 1 < spec[line].length) && (spec[line][item+1].string.equals("MODULE"))) {
				sb.append(UnicodeConstants.DOWN_AND_RIGHT);
				appendMany(sb, UnicodeConstants.HORIZONTAL, tok.string.length() - 1);
				return true;
			} else {
				appendMany(sb, UnicodeConstants.HORIZONTAL, tok.string.length());
				return false;
			}
		} else
			throw new AssertionError("Unexpected token type: " + tok);
	}
	
	private static void appendMany(StringBuilder sb, char c, int num) {
		for (int i = 0; i< num; i++)
			sb.append(c);
	}
	
	private static void adjustWidthTo(StringBuilder sb, int n) {
		if (n > sb.length())
			appendSpaces(sb, n - sb.length());
		else if (n < sb.length())
			trimWhitespaceToWidth(sb, n);
	}
	
	private static void appendSpaces(StringBuilder sb, int n) {
		for (int i = 0; i < n; i++)
			sb.append(' ');
	}
	
	private static void trimWhitespaceToWidth(StringBuilder sb, int n) {
		int i;
		for (i = sb.length() - 1; i > n; i--) {
			if (sb.charAt(i) != ' ') {
				i++;
				break;
			}
		}
		if (i < sb.length())
			sb.delete(i, sb.length());
	}
	
	private static boolean isInPcal(int line, int item) {
		return TokenizeSpec.hasPcal 
				&& line >= TokenizeSpec.pcalStart.line && item >= TokenizeSpec.pcalStart.item
				&& (line < TokenizeSpec.pcalEnd.line 
						|| (line == TokenizeSpec.pcalEnd.line && item < TokenizeSpec.pcalStart.item));
	}
    
	private static boolean isASCII(char c) {
		return c <= 0xff;
	}
	
	private static Token[][] filterOutComments(Token[][] spec) {
		List<Token[]> lines = new ArrayList<>();
		for (int i = 0; i < spec.length; i++) {
			List<Token> line = new ArrayList<>();
			for (int j = 0; j < spec[i].length; j++) {
				Token t = spec[i][j];
				if (t.type != Token.COMMENT)
					line.add(t);
			}
			lines.add(line.toArray(new Token[0]));
		}
		return lines.toArray(new Token[0][]);
	}
	
	/*
	 * This class converts column coordinates between an original and a converted spec
	 */
	public static class TokenPosition {
		private final boolean toU;
		private final Token[][] spec;
		
		TokenPosition(boolean toU, Token[][] spec) {
			this.toU = toU;
			this.spec = spec;
		}
		
		// A coordinates to U coordinates
		public int a2u(int line, int column) {
			return convert0(toU, line, column);
		}
		
		// U coordinates to A coordinates
		public int u2a(int line, int column) {
			return convert0(!toU, line, column);
		}
		
		public int convert(boolean toU, int line, int column) {
			return convert0(toU == this.toU, line, column);
		}
		
		private int convert0(boolean orig, int line, int column) {
			final boolean from = orig;
			final boolean to = !orig;
			
			final int toki = findCandidate(from, line, column);
			if (toki < 0)
				return column;
			final Token candidate = spec[line][toki];
			
//			System.out.println("@@Candidate: " + candidate);
			
			final int col0 = col(from, candidate);
			final int col1 = col(to, candidate);
			final int width0 = width(from, candidate);
			final int width1 = width(to, candidate);

			if (column < col0) // first token
				return Math.max(0, col0 - column);
			
			if (column > col0 + width0) { // we're to the right of a token
				if (toki == spec[line].length - 1)
					return col1 + width1 + (column - (col0 + width0));
				
				return Math.min(col1 + width1 + (column - (col0 + width0)), col(to, spec[line][toki + 1]) - 1);
			}
			
			return Math.min(col1 + width1, col1 + (column - col0));
		}
		
		private int findCandidate(boolean orig, int line, int column) {
			int i;
			for (i=0; i < spec[line].length; i++) {
				if (col(orig, spec[line][i]) > column)
					break;
			}
			return i == 0 ? (spec[line].length == 0 ? -1 : 0) : i - 1; 
		}
		
		// orig - original column
		private static int col(boolean orig, Token t) {
			return !orig && t.outcolumn >= 0 ? t.outcolumn : t.column;
		}
		
		private int width(boolean orig, Token t) {
			if (!orig) {
				String converted;
				converted = toU ? Unicode.a2u(t.string) : Unicode.u2a(t.string);
				if (converted != null)
					return converted.length();
			}
			return t.getWidth();
		}
	}
	// ----------- COMMAND LINE PARSING ---------------------------------------
	
  	private static boolean toU; // True for ASCII -> Unicode, false for Unicode -> ASCII
  	private static String inputFile = null ; // The name of the input file
  	private static String outputFile = null ; // The name of the output file
  
	public static void main(String[] args) {
		// test();
		
		getArguments(args); // Get the command-line arguments.

		final CharReader input = inputFile != null ? 
				new FileCharReader(inputFile) : new InputStreamCharReader(System.in);
		final OutputFileWriter output = outputFile != null 
				? new OutputFileWriter(outputFile) : new OutputFileWriter(System.out, "STDOUT");
				
		convert(toU, input, output);
	}
	
	private static void getArguments(String[] args) {
		 // Get the command-line arguments and set the appropriate static fields.
		
		if (args.length == 0)
			commandLineError("No arguments specified");
		
		boolean hasOp = false; // Whether or not -a2u or -u2a has been encountered.
		int argi = 0; // The index of the command line argument being processed.
		loop:
		while (argi < args.length) {
			// Process all the arguments, except for the last (unless it's a "-" argument).
			final String option = args[argi];
			switch(option) {
			case "-help":
				System.err.println(USAGE);
				System.exit(0);
				break;
			case "-debug":
				debug = true;
				break;
			case "-a2u":
				if (hasOp)
					commandLineError("Only one of -a2u or -u2a must be specified");
				hasOp = true;
				toU = true;
				break;
			case "-u2a":
				if (hasOp)
					commandLineError("Only one of -a2u or -u2a must be specified");
				hasOp = true;
				toU = false;
				break;
			default:
				if (option.startsWith("-"))
					commandLineError("Unsupported option " + option);
				break loop;
			}
			argi++;
		}
		if (!hasOp)
			commandLineError("One of -a2u or -u2a must be specified");

		// Input file
		if (argi >= args.length) {
			inputFile = null;
			return;
		}
		
		inputFile = args[argi];
		
		argi++;
		
		// Output file
		if (argi >= args.length) {
			outputFile = null;
			return;
		}
		
		outputFile = args[argi];
		// Report an error if inputFile = outFile.
		if (Objects.equals(Paths.get(inputFile).normalize().toAbsolutePath(),
				Paths.get(outputFile).normalize().toAbsolutePath()))
			commandLineError("Output file is the same as the tla input file."
					+ " This would overwrite your input file, so I won't do it");
	}

	private static void commandLineError(String msg) {
		ToolIO.out.println(APP + " command-line error: " + msg + ".");
		ToolIO.out.println(USAGE);
		throw new TLA2TexException(
				APP + " command-line error: " + msg + "." + "Use -help option for more information.");
	}
	
	private static void test() {
		String x = "0123456789    ";
		String y;
		
		y = x;
		System.out.println("1: \"" + y + "\", " + y.length());
		
		y = adjustWidth(x, x.length());
		System.out.println("2: \"" + y + "\", " + y.length());

		y = adjustWidth(x, 10);
		System.out.println("3: \"" + y + "\", " + y.length());

		y = adjustWidth(x, 7);
		System.out.println("4: \"" + y + "\", " + y.length());

		y = adjustWidth(x, 20);
		System.out.println("5: \"" + y + "\", " + y.length());
		
		System.exit(0);
	}
	
	private static String adjustWidth(String x, int n) {
		StringBuilder sb = new StringBuilder(x);
		adjustWidthTo(sb, n);
		return sb.toString();
	}

}


/*
Interesting test cases:
-------------------------

------------------------------- MODULE Bar -------------------------------
\* Comment replacement in A->U:

A == x /\ y /\ z /\ w
(*a1234ssasass*) /\ k
(*a12*)(*a*)(*c*)/\ a \* foo
(*a123*)         /\ k
(*a*)(*a*)       /\ a

B == x /\ y /\ z /\ w (* asasvjhsad
a1234ssasassas*) /\ k (* saiork
kinda*)          /\ t

\* "Accidental comment anchoring" in A->U:
Foo(a, b) == /\ a \/ b
          \* /\ a => b
             /\ b => a
=============================================================================



------------------------------- MODULE Bar -------------------------------
\* Comment replacement in U->A

A ≜ abcdefgd ∧ defjhfkjkh
(*a∧b∧c∧d∧e*)∧ k
=============================================================================

 */
