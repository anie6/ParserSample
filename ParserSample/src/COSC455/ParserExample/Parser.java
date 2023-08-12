package COSC455.ParserExample;
//  ************** NOTE: REQUIRES JAVA 11 OR ABOVE! ******************
//COSC-455-102
//ASSIGNMENT:PROGRAM 1
//Anjodi Williams
import java.util.logging.Logger;
import static COSC455.ParserExample.Token.*;


/**
 * The Syntax Analyzer.
 *
 * ************** NOTE: REQUIRES JAVA 11 OR ABOVE! ******************
 */
public class Parser {

    // The lexer which will provide the tokens
    private final LexicalAnalyzer lexer;

    // the actual "code generator"
    private final CodeGenerator codeGenerator;

    /**
     * The constructor initializes the terminal literals in their vectors.
     *
     * @param lexer The Lexer Object
     */
    public Parser(LexicalAnalyzer lexer, CodeGenerator codeGenerator) {
        this.lexer = lexer;
        this.codeGenerator = codeGenerator;

        // Change this to automatically prompt to see the Open WebGraphViz dialog or not.
        MAIN.PROMPT_FOR_GRAPHVIZ = true;
    }

    /*
     * Since the "Compiler" portion of the code knows nothing about the start rule, the "analyze" method
     * must invoke the start rule.
     *
     * Begin analyzing...
     */

    void analyze() {
        try {
            // Generate header for our output
            var startNode = codeGenerator.writeHeader("PARSE TREE");

            // THIS IS OUR START RULE
            BEGIN_PARSING(startNode);

            // generate footer for our output
            codeGenerator.writeFooter();

        } catch (ParseException ex) {
            final String msg = String.format("%s\n", ex.getMessage());
            Logger.getAnonymousLogger().severe(msg);
        }
    }

    /**
     * The start rule for the grammar.
     *
     * @param parentNode The parent node for the parse tree
     * @throws ParseException If there is a syntax error
     */
    void BEGIN_PARSING(final TreeNode parentNode) throws ParseException {
        
        PROGRAM(parentNode);
    }
    //STMT as a token does work... stack overflow
    //is valid method for each stmt--> sent in token x
    
    // <PROGRAM> ::= <STMT_LIST> (ignore $$)
    void PROGRAM(final TreeNode fromNode) throws ParseException {

        final var nodeName = codeGenerator.addNonTerminalToParseTree(fromNode);

        STMT_LIST(nodeName);
    }
    

    boolean isValidStatement(Token t){
        return switch(t){
            case READ, WRITE, ID, IF, WHILE, DO -> true;
            default -> false;
        };
    }


    // <STMT_LIST> ::= <STMT> <STMT_LIST> | <EMPTY>
    void STMT_LIST(final TreeNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToParseTree(fromNode);

        if (isValidStatement(lexer.getCurrentToken())) {
            STMT(treeNode);
            STMT_LIST(treeNode);

        } else { //| <EMPTY>
            EMPTY(treeNode);
        }
    }

    //List all STMT
    // <STMT> ::= <ID> ::= <EXPR> | READ <ID> | WRITE <EXPR>
    // <STMT> ::= <IF> condition <THEN> <STMT_LIST> <FI>
    // <STMT> ::= <WHILE> condition <DO> <STMT_LIST> <OD>
    // <STMT> ::=  <DO> <STMT_LIST> until condition

    void STMT(final TreeNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToParseTree(fromNode);
        
        // <STMT> ::= <ID> ::= <EXPR> | READ <ID> | WRITE <EXPR>
        if(lexer.isCurrentToken(ID)){
            ID_KEY(treeNode);
            ASSIGN_KEY(treeNode);
            EXPR(treeNode);
            } else if(lexer.isCurrentToken(READ)) { //| READ <ID> |
                READ_KEY(treeNode);
                ID_KEY(treeNode);
            } else if(lexer.isCurrentToken(WRITE)){ //WRITE <EXPR>
                WRITE_KEY(treeNode);
                EXPR(treeNode);
            } else if(lexer.isCurrentToken(IF)){
                // <STMT> ::= <IF> condition <THEN> <STMT_LIST> <FI>
                IF_KEY(treeNode);
                THEN_KEY(treeNode);
                STMT_LIST(treeNode);
                FI_KEY(treeNode);
            } else if(lexer.isCurrentToken(WHILE)){ //<WHILE> condition <DO> <STMT_LIST> <OD>
                WHILE_KEY(treeNode);
                CONDITION(treeNode);
                DO_KEY(treeNode);
                STMT_LIST(treeNode);
                OD_KEY(treeNode);

            } else if(lexer.isCurrentToken(DO)){ //<DO> <STMT_LIST> until condition
                DO_KEY(treeNode);
                STMT_LIST(treeNode);
                UNTIL_KEY(treeNode);
                CONDITION(treeNode);
            }


        }
         
   // <EXPR> ::= <TERM> <TERM_TAIL> 
    void EXPR(final TreeNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToParseTree(fromNode);

        TERM(treeNode);
        TERM_TAIL(treeNode);
    }

    // <TERM_TAIL> ::= <ADD_OP> <TERM> <TERM_TAIL> | <<EMPTY>>
    void TERM_TAIL(final TreeNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToParseTree(fromNode);

        if (lexer.isCurrentToken(ADD_OPP)) {
            ADD_OPP_KEY(treeNode);
            TERM(treeNode);
            TERM_TAIL(treeNode);
        } else {
            EMPTY(treeNode);
        }
    }

    // <TERM> ::= <FACTOR> <FACTOR_TAIL>
    void TERM(final TreeNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToParseTree(fromNode);
            FACTOR(treeNode);
            FACTOR_TAIL(treeNode);
        
    }

    // <FACTOR_TAIL> ::= <MUL_OP> <FACTOR> <FACTOR_TAIL> | <<EMPTY>>
    void FACTOR_TAIL(final TreeNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToParseTree(fromNode);

        if (lexer.isCurrentToken(MULTI_OPP)) {
            MULTI_OPP_KEY(treeNode);
            FACTOR(treeNode);
            FACTOR_TAIL(treeNode);
        } else {
            EMPTY(treeNode);
        }
    }

    // <FACTOR> ::= (<EXPR>) <ID> | <NUMBER> 
    void FACTOR(final TreeNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToParseTree(fromNode);

        if (lexer.isCurrentToken(OPPEN_BRACKET)) {
            OPEN(treeNode);
            EXPR(treeNode);
            CLOSE(treeNode);
        } else if(lexer.isCurrentToken(ID)){
            ID_KEY(treeNode);
        } else {
            NUMBER_KEY(treeNode);

        }
    }

    //<CONDITION> ::= <EXPR> <RELATION> <EXPR>
    void CONDITION(final TreeNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToParseTree(fromNode);
        //<EXPR> <RELATION> <EXPR>
        EXPR(treeNode);
        RELATION(treeNode);
        EXPR(treeNode);
       
    }
    /////////////////////////////////////////////////////////////////////////////////////
    // For the sake of completeness, each terminal-token has its own method,
    // though they all do the same thing here.  In a "REAL" program, each terminal
    // would likely have unique code associated with it.
    /////////////////////////////////////////////////////////////////////////////////////
    void EMPTY(final TreeNode fromNode) throws ParseException {
        codeGenerator.addEmptyToTree(fromNode);
    }
    //Read Key
    void READ_KEY(final TreeNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(READ)) {
            addTerminalAndAdvanceToken(fromNode);
        } else {
            raiseException("Keyword: Read ", fromNode);
        }
    }

    //write key
    void WRITE_KEY(final TreeNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(WRITE)) {
            addTerminalAndAdvanceToken(fromNode);
        } else {
            raiseException("Keyword: Write ", fromNode);
        }
    }

//do key
void DO_KEY(final TreeNode fromNode) throws ParseException {
    if (lexer.isCurrentToken(DO)) {
        addTerminalAndAdvanceToken(fromNode);
    } else {
        raiseException("Keyword: do ", fromNode);
    }
}

//until key
void UNTIL_KEY(final TreeNode fromNode) throws ParseException {
    if (lexer.isCurrentToken(UNTIL)) {
        addTerminalAndAdvanceToken(fromNode);
    } else {
        raiseException("Keyword: until ", fromNode);
    }
}

//if key
void IF_KEY(final TreeNode fromNode) throws ParseException {
    if (lexer.isCurrentToken(IF)) {
        addTerminalAndAdvanceToken(fromNode);
    } else {
        raiseException("Keyword: if ", fromNode);
    }
}

//while key
void WHILE_KEY(final TreeNode fromNode) throws ParseException {
    if (lexer.isCurrentToken(WHILE)) {
        addTerminalAndAdvanceToken(fromNode);
    } else {
        raiseException("Keyword: while ", fromNode);
    }
}



    // <RELATION>
    void RELATION(final TreeNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(OPP)) {
            addTerminalAndAdvanceToken(fromNode);
        } else {
            raiseException("Relation Operator ", fromNode);
        }
    }

    // <ADD_OPP> + | -
    void ADD_OPP_KEY(final TreeNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(ADD_OPP)) {
            addTerminalAndAdvanceToken(fromNode);
        } else {
            raiseException("Addition Operator", fromNode);
        }
    }
    

    // <MULT_OPP> * | /
    void MULTI_OPP_KEY(final TreeNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(MULTI_OPP)) {
            addTerminalAndAdvanceToken(fromNode);
        } else {
            raiseException("Multiplication Operator", fromNode);
        }
    }

    // <ID_KEY>
    void ID_KEY(final TreeNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(ID)) {
            addTerminalAndAdvanceToken(fromNode);
        } else {
            raiseException("Identifier", fromNode);
        }
    }

    // <NUMBER_KEY>
    void NUMBER_KEY(final TreeNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(NUMBER)) {
            addTerminalAndAdvanceToken(fromNode);
        } else {
            raiseException("Number", fromNode);
        }
    }

    // <THEN_KEY>
    void THEN_KEY(final TreeNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(THEN)) {
            addTerminalAndAdvanceToken(fromNode);
        } else {
            raiseException("Keyword: then", fromNode);
        }
    }

    // <UNTIL_KEY>
    void UNTIL(final TreeNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(UNTIL)) {
            addTerminalAndAdvanceToken(fromNode);
        } else {
            raiseException("Keyword: until", fromNode);
        }
    }

    // <OD_KEY>
    void OD_KEY(final TreeNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(OD)) {
            addTerminalAndAdvanceToken(fromNode);
        } else {
            raiseException("Keyword: od", fromNode);
        }
    }

    // <FI_KEY>
    void FI_KEY(final TreeNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(FI)) {
            addTerminalAndAdvanceToken(fromNode);
        } else {
            raiseException("Keyword: fi", fromNode);
        }
    }
    //assign, open
    void ASSIGN_KEY(final TreeNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(ASSIGN)) {
            addTerminalAndAdvanceToken(fromNode);
        } else {
            raiseException("Symbol: := ", fromNode);
        }
    }

    void OPEN(final TreeNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(OPPEN_BRACKET)) {
            addTerminalAndAdvanceToken(fromNode);
        } else {
            raiseException("Symbol: ( ", fromNode);
        }
    }

    void CLOSE(final TreeNode fromNode) throws ParseException {
        if (lexer.isCurrentToken(CLOSE_BRACKET)) {
            addTerminalAndAdvanceToken(fromNode);
        } else {
            raiseException("Symbol: ) ", fromNode);
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Terminal:
    // Test its type and continue if we really have a terminal node, syntax error if fails.
    void addTerminalAndAdvanceToken(final TreeNode fromNode) throws ParseException {
        final var currentTerminal = lexer.getCurrentToken();

        String nodeLabel = String.format("<%s>", currentTerminal);
        final var terminalNode = codeGenerator.addNonTerminalToParseTree(fromNode, nodeLabel);

        codeGenerator.addTerminalToTree(terminalNode, lexer.getCurrentLexeme());
        lexer.advanceToken();
    }

    // Handle all the errors in one place for cleaner parser code.
    private void raiseException(String expected, TreeNode fromNode) throws ParseException {
        final var template = "SYNTAX ERROR: '%s' was expected but '%s' was found.";
        final var err = String.format(template, expected, lexer.getCurrentLexeme());
        codeGenerator.syntaxError(err, fromNode);
    }
   }


