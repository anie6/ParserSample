package COSC455.ParserExample;
//  ************** NOTE: REQUIRES JAVA 11 OR ABOVE! ******************
//COSC-455-102
//ASSIGNMENT:PROGRAM 1
//Anjodi Williams

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.nio.file.Files.lines;
import static java.text.MessageFormat.format;

/**
 * COSC455 Programming Languages: Implementation and Design.
 * <p>
 * ************** NOTE: REQUIRES JAVA 11 OR ABOVE! ******************
 * <p>
 * DESIGN NOTE: It's generally bad to have a bunch of "top level classes" in one giant file.
 * However, this was done here only to keep the example down to only files... One for the parser and
 * one for everything else!
 * <p>
 * This syntax analyzer implements a top-down, left-to-right, recursive-descent parser based on the
 * production rules for a simple English language provided by Weber in "Modern Programming
 * Languages".
 */
public class MAIN {

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!! Toggle to display Graphviz prompt. !!!!!!!

    // NOTE: This is set in the Parser class.
    public static boolean PROMPT_FOR_GRAPHVIZ;

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!


    public static void main(String[] args) {
        // Check for an input file argument
        if (args.length == 1) {
            final File file = new File(args[0]);

            if (file.exists() && file.isFile() && file.canRead()) {
                try {
                    String compiledCode = ScanAndParse(file);

                    // Display the graphviz test page if desired.
                    if (PROMPT_FOR_GRAPHVIZ) {
                        try {
                            GraphViewer.openWebGraphViz(compiledCode);
                        } catch (UnsupportedEncodingException ex) {
                            final String msg =
                                    format("Could not create a valid URL String!!! {0}", ex.getMessage());

                            Logger.getAnonymousLogger().log(Level.WARNING, msg);
                        }
                    }
                } catch (IOException ex) {
                    final String msg = format("Could not read the file!!! {0}", ex.getMessage());
                    Logger.getAnonymousLogger().log(Level.SEVERE, msg);
                }
            } else {
                System.err.printf("Input file not found: %s%n", file.toPath());
            }

        } else {
            System.err.println("Must Provide an input filename!!");
        }
    }

    private static String ScanAndParse(final File inputFile) throws IOException {
        final CodeGenerator codeGenerator = new CodeGenerator();
        final LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer(inputFile);

        // Compile the program from the input supplied by the lexical analyzer.
        final Parser parser = new Parser(lexicalAnalyzer, codeGenerator);
        parser.analyze();

        return codeGenerator.getGeneratedCodeBuffer().toString();
    }
}

// *********************************************************************************************************

/**
 * This is a *FAKE* (Overly simplified) Lexical Analyzer...
 * <p>
 * NOTE: This DOES NOT "lex" the input in the traditional manner on a DFA based "state machine" or
 * even host language supported regular expressions (which are compiled into "state machine" anyhow
 * in most implementations.)
 * <p>
 * Instead of using "state transitions", this is merely a quick hack to create a something that
 * BEHAVES like a traditional lexer in its FUNCTIONALITY, but it ONLY knows how to separate
 * (tokenize) lexemes delimited by SPACES. A Real Lexer would tokenize based upon far more
 * sophisticated lexical rules.
 * <p>
 * AGAIN: ALL TOKENS MUST BE WHITESPACE DELIMITED.
 */
class LexicalAnalyzer {

    // TOKENIZED input.
    private Queue<TokenString> tokenList;

    /**
     * Construct a lexer over an input string.
     *
     * @param inputString The input file as a string.
     */
    LexicalAnalyzer(final String inputString) {
        tokenize(inputString);
    }

    /**
     * Construct a Lexer over the contents of a file. Filters out lines starting with a '#' Symbol.
     * Removes EOL markers; otherwise, our grammar would have to deal with them.
     *
     * @param inputFile The file that we are reading from.
     */
    LexicalAnalyzer(final File inputFile) throws IOException {
        Path filePath = inputFile.toPath();

        try (var lines = lines(filePath)) {
            tokenize(lines // read lines
                    .map(String::trim) // map to trimmed strings
                    .filter(x -> !x.startsWith("#")) // filter out lines starting with #
                    .collect(Collectors.joining(" "))); // join lines together with spaces in between.
        }
    }

    // Simple Wrapper around current token.
    boolean isCurrentToken(final Token token) {
        return token == getCurrentToken();
    }

    /**
     * Convert the line to a series of tokens.
     */
    private void tokenize(final String line) {
        // Using Java 8's "Function Streams"
        this.tokenList = Arrays
                .stream(line.trim().split("\\s+")) // split string on spaces
                .map(TokenString::new) // map to a new Token
                .collect(Collectors.toCollection(LinkedList::new)); // collect into a new list
    }

    /**
     * Get just the lexeme part the head of the token list.
     *
     * @return the Lexeme as an Optional string since an empty list has no tokens.
     */
    public String getCurrentLexeme() {
        return (this.tokenList.isEmpty() || getCurrentToken() == Token.EOF)
                ? "EOF"
                : Objects.requireNonNull(this.tokenList.peek()).lexeme;
    }

    /**
     * Get just the token
     */
    public Token getCurrentToken() {
        return this.tokenList.isEmpty()
                ? Token.EOF
                : this.tokenList.peek().token;
    }

    /**
     * Advance to next token, making it current.
     */
    public void advanceToken() {
        if (!this.tokenList.isEmpty()) {
            this.tokenList.remove();
        }
    }

    /**
     * To string for debugging.
     */
    @Override
    public String toString() {
        return this.tokenList.toString();
    }

    /**
     * Nested class: a "Pair Tuple/Struct" for the token type and original string.
     */
    private static class TokenString {

        private final String lexeme;
        private final Token token;

        TokenString(final String lexeme) {
            this.lexeme = lexeme;
            this.token = Token.fromLexeme(lexeme);
        }

        @Override
        public String toString() {
            return String.format("{lexeme=%s, token=%s}", lexeme, token);
        }
    }
}

// *********************************************************************************************************


/**
 * This is a ***SIMULATION*** of a "code generator" that simply generates GraphViz output.
 * Technically, this would represent be the "Intermediate Code Generation" step.
 * <p> 
 * Also, Instead of building an entire tree in memory followed by a traversal the tree at the end,
 * here we are just adding "code" as we go.
 * <p>
 * (This simulates a single-pass compiler; keep in mind that most modern compilers work in several
 * passes... eg. scan for all top level identifiers, build sub-trees for each class/method/etc.,
 * generate an internal intermediate code representation, and so on).
 * <p>
 * DESIGN NOTE: From an OOP design perspective, creating instances of "utility classes" (classes
 * with no internal state) is generally bad. However, in a more elaborate example, the code
 * generator would most certainly maintain some internal state information. (Memory address offsets,
 * etc.)
 */
class CodeGenerator {

    // Buffer for generated code
    private final StringBuffer generatedCodeBuffer;

    // Constructor
    CodeGenerator() {
        this.generatedCodeBuffer = new StringBuffer();
    }

    // Write generated code to both the screen AND the buffer.
    void outputGeneratedCode(final String msg) {
        System.out.print(msg);
        this.generatedCodeBuffer.append(msg);
    }

    // Show the terminals as ovals...
    public void addTerminalToTree(final TreeNode fromNode, final String lexeme) {
        final var node = new TreeNode(lexeme);
        final var msg = String.format("\t\"%s\" -> {\"%s\" [label=\"%s\", shape=oval]};%n", fromNode, node, lexeme);

        outputGeneratedCode(msg);
    }

    /**
     * Add an "inner node" to the parse tree.
     *
     * @param newNodeName the name of the node to insert into the tree
     * @param parentNode  the parent of the node being added to the tree
     * @return the newly added node as ParseNode object.
     */
    public TreeNode addNonTerminalToParseTree(final String newNodeName, final TreeNode parentNode) {
        final var toNode = buildNode(newNodeName);

        addNonTerminalToParseTree(parentNode, toNode);
        return toNode;
    }

    /**
     * Add an "inner node" to the parse tree.
     * <p>
     * The following code employs a bit of a trick to automatically build the calling method name.
     * The "getStackTrace()" method returns information about the entire active stack. Element[0] is
     * the actual "getStackTrace()" method (it doesn't eliminate itself from the array), element[1]
     * is THIS method (since we called "getStackTrace()") and element[2] is the method that called
     * us, etc.
     *
     * @param parentNode the parent of the node being added to the tree
     * @return the newly added node as ParseNode object.
     */
    public TreeNode addNonTerminalToParseTree(final TreeNode parentNode) {
        // This uses a Java "Trick" to return the name of the function that called this method.
        final var fromMethodName = Thread.currentThread().getStackTrace()[2].getMethodName().toUpperCase();

        // Build a node name
        final var toNode = buildNode("<" + fromMethodName + ">");

        addNonTerminalToParseTree(parentNode, toNode);

        return toNode;
    }

    /**
     * Show the non-terminals as boxes...
     *
     * @param fromNode the parent node
     * @param toNode   the child node
     * @return the child node
     */
    public TreeNode addNonTerminalToParseTree(final TreeNode fromNode, final TreeNode toNode) {
        final var msg = String.format("\t\"%s\" -> {\"%s\" [label=\"%s\", shape=rect]};%n", fromNode, toNode, toNode.getNodeName());

        outputGeneratedCode(msg);
        return toNode;
    }

    /**
     * Add the "from node" to the tree and return a new "next node" object.
     *
     * @param fromNode The node to add to the tree.
     * @return the newly added node as ParseNode object.
     */
    public TreeNode addNonTerminalToParseTree(final TreeNode fromNode, final String toNodeString) {
        final var toNode = buildNode(toNodeString);
        return addNonTerminalToParseTree(fromNode, toNode);
    }

    // Show the terminals as ovals...
    public void addEmptyToTree(final TreeNode fromNode) {
        final var node = new TreeNode("EMPTY");
        final var msg = String.format("\t\"%s\" -> {\"%s\" [label=\"%s\", shape=none]};%n", fromNode, node, "&epsilon;");

        outputGeneratedCode(msg);
    }

    // Call this if a syntax error occurs...
    public void syntaxError(final String err, TreeNode fromNode) throws ParseException {
        final var msg = String.format("\t\"%s\" -> {\"%s\"};%n}%n", fromNode, err);

        outputGeneratedCode(msg);
        throw new ParseException(err);
    }

    // Build a node name, so it can be later "deconstructed" for the output.
    private TreeNode buildNode(final String name) {
        return new TreeNode(name);
    }

   
    public void writeHeader(TreeNode node) {
        // The header for the "compiled" output
        var msg = String.format("digraph ParseTree {\n"
                        + "\t\"%s\" [label=\"%s\", shape=diamond];\n",
                node, node.getNodeName());

        outputGeneratedCode(msg);
    }

    public TreeNode writeHeader(final String nodeString) {
        // The header for the "compiled" output
        final var headerNode = buildNode(nodeString);

        writeHeader(headerNode);
        return headerNode;
    }

    // Our output requires a footer as well.
    public void writeFooter() {
        final var msg = "}\n";

        outputGeneratedCode(msg);
    }

    public StringBuffer getGeneratedCodeBuffer() {
        return generatedCodeBuffer;
    }
}

// *********************************************************************************************************

/**
 * A "3-Tuple" for the node name and id number.
 */
class TreeNode {

    private static Integer currentNodeID = 0;
    private final String nodeName;
    private final Integer nodeId;

    TreeNode(final String nodeName) {
        this.nodeName = nodeName;
        this.nodeId = currentNodeID++;
    }

    public String getNodeName() {
        return nodeName;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    @Override
    public String toString() {
        return String.format("%s-%s", this.getNodeName(), this.getNodeId());
    }
}

// *********************************************************************************************************

/**
 * Code to invoke the online graph viewer.
 */
class GraphViewer {

  
    static void openWebGraphViz(final String graph) throws UnsupportedEncodingException {
        /* Online/Web versions of Graphviz
         http://www.webgraphviz.com
         http://viz-js.com
         https://dreampuf.github.io/GraphvizOnline
         */

        final var WEBGRAPHVIZ_HOME = "https://dreampuf.github.io/GraphvizOnline/";
        final var MSG = "To visualize the output you may, Copy/Paste the parser output into:\n" + WEBGRAPHVIZ_HOME + " \n";

        String encodedURL = URLEncoder.encode(graph, StandardCharsets.UTF_8).replace("+", "%20");

        if (Desktop.isDesktopSupported()) {
            // Try to set the default skin
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                     UnsupportedLookAndFeelException e) {
                // Ignore the error, but notify.
                System.out.println("Using Default Skin");
            }

            // Open the default browser with the url:
            try {
                final URL webGraphvizURI = new URL(WEBGRAPHVIZ_HOME + "#" + encodedURL);
                final Desktop desktop = Desktop.getDesktop();

                // Can we launch a browser?
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    final var response = JOptionPane.showConfirmDialog(null, String.format("%s\nOpen %s?", MSG, WEBGRAPHVIZ_HOME), "Open Web Graphviz Page", JOptionPane.YES_NO_OPTION);

                    // Open Browser?
                    if (response == JOptionPane.YES_OPTION) {
                        desktop.browse(webGraphvizURI.toURI());
                    } else {
                        System.out.println(MSG);
                    }
                }
            } catch (IOException | URISyntaxException ex) {
                java.util.logging.Logger.getAnonymousLogger().log(java.util.logging.Level.WARNING, "Could not open browser", ex);
            }
        }
    }
}

// *********************************************************************************************************

/**
 * An exception to be raised if parsing fails due to a "syntax error" in the input file.
 */
class ParseException extends RuntimeException {
    public ParseException(String errMsg) {
        super(errMsg);
    }
}

