package COSC455.ParserExample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


//ASSIGNMENT:PROGRAM 1
//Anjodi Williams
public enum Token {
    //Tokens from statement
    // STMT("read", "write", "id", "if", "while", "do"),
    READ("read"),
    WRITE("write"),
    OPPEN_BRACKET("("),
    CLOSE_BRACKET(")"),
    ADD_OPP("+","-",""),
    IF("if"),
    THEN("then"),
    FI("fi"),
    ASSIGN(":="),
    MULTI_OPP("*","/"),
    OPP("<", ">", ">=", "<=",":=", "!="),
    WHILE("while"),
    DO("do"),
    OD("od"),
    UNTIL("until"),
    
    // THESE ARE NOT USED IN THE GRAMMAR, BUT MIGHT BE USEFUL...  :)
    ID, //[keep]
    EOF, // End of file --> [keep]
    OTHER, // Could be "ID" in a "real programming language"
    NUMBER; // A sequence of digits. [keep]

    /**
     * A list of all lexemes for each token.
     */
    private final List<String> lexemeList;

    Token(final String... tokenStrings) {
        lexemeList = new ArrayList<>(tokenStrings.length);
        lexemeList.addAll(Arrays.asList(tokenStrings));
    }

    /**
     * Get a Token object from the Lexeme string.
     *
     * @param string The String (lexeme) to convert to a Token
     * @return A Token object based on the input String (lexeme)
     */
    public static Token fromLexeme(final String string) {
        // Just to be safe...
        final var lexeme = string.trim();

        // An empty string/lexeme should mean no more tokens to process.
        if (lexeme.isEmpty()) {
            return EOF;
        }

        // Regex for one or more digits optionally followed by . and more digits.
        // (doesn't handle "-", "+" etc., only digits)
        if (lexeme.matches("\\d+(?:\\.\\d+)?")) {
            return NUMBER;
        }

        // Search through ALL lexemes looking for a match with early bailout.
        for (var token : Token.values()) {
            if (token.lexemeList.contains(lexeme)) {
                // early bailout from for loop.
                return token;
            }
        }

        // NOTE: Other could represent an ID, for example.
        return ID;
    }
}
