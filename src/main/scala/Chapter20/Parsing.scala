package Chapter20

object Parsing {
// topics:
    // grammars
    // combining parser operations
    // transforming parsing results
    // discarding tokens
    // generating parse trees
    // avoiding left recursion
    // more combinators
    // avoiding backtracking
    // packrat parsers
    // what exactly are parsers
    // regex parsers
    // token-based parsers
    // error handling

    // grammars
    def grammars = {
        ???
    }

    // combining parser operations
    def combiningParserOperations = {
        ???
    }

    // transforming parsing results
    def transformingParsingResults = {
        ???
    }

    // discarding tokens
    def discardingTokens = {
        ???
    }

    // generating parse trees
    def generatingParseTrees = {
        ???
    }

    // avoiding left recursion
    def avoidingLeftRecursion = {
        ???
    }

    // more combinators
    def moreCombinators = {
        ???
    }

    // avoiding backtracking
    def avoidingBacktracking = {
        ???
    }

    // packrat parsers
    def packratParsers = {
        ???
    }

    // what exactly are parsers
    def whatExactlyAreParsers = {
        ???
    }

    // regex parsers
    def regexParsers = {
        ???
    }

    // token-based parsers
    def tokenBasedParsers = {
        ???
    }

    // error handling
    def errorHandling = {
        ???
    }
}

object Parsing_Exercises {

    // 1. Add / and % operations to the arithmetic expression evaluator.
    def ex1 = {
        ???
    }

    // 2. Add a ^ operator to the arithmetic expression evaluator. As in mathematics, ^ should have a
    //higher precedence than multiplication, and it should be right-associative. That is, 4^2^3
    //should be 4^(2^3), or 65536.
    def ex2 = {
        ???
    }

    // 3. Write a parser that parses a list of integers (such as (1, 23, -79)) into a List[Int].
    def ex3 = {
        ???
    }

    // 4. Write a parser that can parse date and time expressions in ISO 8601. Your parser should return
    //a java.time.LocalDateTime object.
    def ex4 = {
        ???
    }

    // 5. Write a parser that parses a subset of XML. Handle tags of the form <ident>...
    //</ident> or <ident/>. Tags can be nested. Handle attributes inside tags. Attribute values
    //can be delimited by single or double quotes. You don’t need to deal with character data (that is,
    //text inside tags or CDATA sections). Your parser should return a Scala XML Elem value. The
    //challenge is to reject mismatched tags. Hint: into, accept.
    def ex5 = {
        ???
    }

    // 6. Assume that the parser in Section 20.5, “Generating Parse Trees,” on page 309 is completed
    //with
    //Click here to view code image
    //class ExprParser extends RegexParsers {
    //def expr: Parser[Expr] = (term ~ opt(("+" | "-") ~ expr)) ^^ {
    //case a ~ None => a
    //case a ~ Some(op ~ b) => Operator(op, a, b)
    //}
    //...
    //}
    //Unfortunately, this parser computes an incorrect expression tree—operators with the same
    //precedence are evaluated right-to-left. Modify the parser so that the expression tree is correct.
    //For example, 3-4-5 should yield an Operator("-", Operator("-", 3, 4), 5).
    def ex6 = {
        ???
    }

    // 7. Suppose in Section 20.6, “Avoiding Left Recursion,” on page 310, we first parse an expr into
    //a list of ~ with operations and values:
    //Click here to view code image
    //def expr: Parser[Int] = term ~ rep(("+" | "-") ~ term) ^^ {...}
    //To evaluate the result, we need to compute ((t 0 ± t1) ± t2) ± . . . Implement this computation as a
    //fold (see Chapter 13).
    def ex7 = {
        ???
    }

    // 8. Add variables and assignment to the calculator program. Variables are created when they are
    //first used. Uninitialized variables are zero. To print a value, assign it to the special variable
    //out.
    def ex8 = {
        ???
    }

    // 9. Extend the preceding exercise into a parser for a programming language that has variable
    //assignments, Boolean expressions, and if/else and while statements.
    def ex9 = {
        ???
    }

    // 10. Add function definitions to the programming language of the preceding exercise.
    def ex10 = {
        ???
    }

}
