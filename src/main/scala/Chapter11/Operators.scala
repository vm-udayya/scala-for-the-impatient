package Chapter11

object Operators {
// topics:
    // identifiers
    // infix operators
    // unary operators
    // assignment operators
    // precedence
    // associativity
    // the apply and update methods
    // extractors
    // extractors with one or no arguments
    // the unapplySeq method
    // dynamic invocation

    // implementing your own operators;
    // operators + implicit conversions = Domain Specific Language;
    // special methods: apply, update, unapply;
    // unary and binary operators are method calls;
    // operator precedence depends on the first char, associativity on the last;
    // extractors extract tuples or sequences of values; or single value or just bool;
    // types with Dynamic trait can inspect/dispatch methods names and arguments at runtime;

    // identifiers
    def identifiers = {
        // names of variables, functions, classes, etc

        // in identifiers:
        // unicode chars are allowed;
        // operator chars are allowed: the ASCII characters
        //      ! # % & * + - / : < = > ? @ \ ^ | ~
        //      that are not letters, digits, underscore,
        //      the .,; punctuation marks, parentheses () [] {}, or quotation marks ' ` "

        val √ = scala.math.sqrt _
        println(s"sqrt of 2: ${√(2)}")

        // The identifiers that reserved in the specification
        //      @ # : = _ => <- <: <% >: ⇒ ←

        // you can include just about any id in backquotes
        val `val` = 42
        val `yield`: () => Unit = java.lang.Thread.`yield`
    }

    // infix operators
    def infixOperators = {
        // binary operators
        // a id b : operator between the arguments
        // method with two parameters: implicit + explicit

        val `1to10` = 1 to 10
        val actually = 1.to(10)
        // def to(end: Int): Range.Inclusive = Range.inclusive(self, end)

        // operator chars
        val ten = 1 -> 10 // tuple
        val ten2 = 1.->(10)

        // just define a method with desired name
        class Fraction(n: Int, d: Int) {
            private val num: Int = ???
            private val den: Int = ???
            def *(other: Fraction) = new Fraction(num * other.num, den * other.den)
        }
    }

    // unary operators
    def unaryOperators = {
        // operator with one parameter: prefix, postfix

        // + - ! ~ allowed as prefix operators
        // converted into obj.unary_op
        // e.g.     -a => a.unary_-

        // postfix op can lead to parsing errors
        import scala.language.postfixOps

        // postfix operator follows its argument
        // obj op => obj.op()
        val `42` = 42 toString
        // 42.toString()
    }

    // assignment operators
    def assignmentOperators = {
        // in the form op=
        // means mutation

        // obj op= obj2     => obj = obj op obj2
        // e.g. a += b      => a = a + b

        // <=, >=, != are NOT assignment operators
        // operator starting with '=' is never an assignment op (==, ===, =/=, etc)
        // if obj has a method 'op=' that method is called directly
    }

    // precedence
    def precedence = {
        // except for assignment operators, the precedence is determined by
        // the FIRST character of the operator

        /*
        highest: an op character (# ? @ \ ~) other then those below

        * / %
        + -
        :
        < >
        ! =
        &
        ^
        |

        a char that is not an operator char (alphanumeric)
        lowest: assignment operators (op=)
         */

        // postfix operators have lower precedence than infix operators
        // a infix b postfix => (a infix b) postfix
    }

    // associativity
    def associativity = {
        // evaluated left-to-right or right-to-left

        // left-associative
        val left = 17 - 2 - 9 // => (17 - 2) - 9

        // all operators are left-associative
        // except:  'op:' // end in a colon;
        //          'op=' // assignment operators

        val right = 1 :: 2 :: Nil // => 1 :: (2 :: Nil)
        // def ::[B >: A] (x: B): List[B] = new scala.collection.immutable.::(x, this)

        // right-associative binary operator is a method of its second argument
        // 2 :: Nil =>  Nil.::(2)
    }

    // the apply and update methods
    def theApplyAndUpdateMethods = {
        ???
    }

    // extractors
    def extractors = {
        ???
    }

    // extractors with one or no arguments
    def extractorsWithOneOrNoArguments = {
        ???
    }

    // the unapplySeq method
    def theUnapplySeqMethod = {
        ???
    }

    // dynamic invocation
    def dynamicInvocation = {
        ???
    }

}

object Operators_Exercises {

    // 1. According to the precedence rules, how are 3 + 4 -> 5 and 3 -> 4 + 5 evaluated?
    def ex1 = {
        ???
    }

    // 2. The BigInt class has a pow method, not an operator. Why didn’t the Scala library designers
    //choose ** (as in Fortran) or ^ (as in Pascal) for a power operator?
    def ex2 = {
        ???
    }

    // 3. Implement the Fraction class with operations + - * /. Normalize fractions, for example,
    //turning 15/–6 into –5/2. Divide by the greatest common divisor, like this:
    //Click here to view code image
    //class Fraction(n: Int, d: Int) {
    //private val num: Int = if (d == 0) 1
    // else n * sign(d) / gcd(n, d);
    //private val den: Int = if (d == 0) 0
    // else d * sign(d) / gcd(n, d);
    //override def toString = s"$num/$den"
    //def sign(a: Int) = if (a > 0) 1 else
    // if (a < 0) -1 else 0
    //def gcd(a: Int, b: Int): Int = if (b
    // == 0) abs(a) else gcd(b, a % b)
    //...
    //}
    def ex3 = {
        ???
    }

    // 4. Implement a class Money with fields for dollars and cents. Supply +, - operators as well as
    //comparison operators == and <. For example, Money(1, 75) + Money(0, 50) ==
    //Money(2, 25) should be true. Should you also supply * and / operators? Why or why
    //not?
    def ex4 = {
        ???
    }

    // 5. Provide operators that construct an HTML table. For example,
    //Click here to view code image
    //Table() | "Java" | "Scala" || "Gosling" | "Odersky" || "JVM" | "JVM, .NET"
    //should produce
    //Click here to view code image
    //<table><tr><td>Java</td><td>Scala</td></tr><tr><td>Gosling...
    def ex5 = {
        ???
    }

    // 6. Provide a class ASCIIArt whose objects contain figures such as
    //Click here to view code image
    ///\_/\
    //( ' ' )
    //( - )
    //| | |
    //(__|__)
    //Supply operators for combining two ASCIIArt figures horizontally
    //Click here to view code image
    ///\_/\
    // -----
    //( ' ' ) / Hello \
    //( - ) < Scala |
    //| | |
    // \ Coder /
    //(__|__)
    // -----
    //or vertically. Choose operators with appropriate precedence.
    def ex6 = {
        ???
    }

    // 7. Implement a class BitSequence that stores a sequence of 64 bits packed in a Long value.
    //Supply apply and update operators to get and set an individual bit.
    def ex7 = {
        ???
    }

    // 8. Provide a class Matrix. Choose whether you want to implement 2 × 2 matrices, square
    //matrices of any size, or m × n matrices. Supply operations + and *. The latter should also work
    //with scalars, for example, mat * 2. A single element should be accessible as mat(row,
    //col).
    def ex8 = {
        ???
    }

    // 9. Define an object PathComponents with an unapply operation class that extracts the
    //directory path and file name from an java.nio.file.Path. For example, the file
    ///home/cay/readme.txt has directory path /home/cay and file name readme.txt.
    def ex9 = {
        ???
    }

    // 10. Modify the PathComponents object of the preceding exercise to instead define an
    //unapplySeq operation that extracts all path segments. For example, for the file
    ///home/cay/readme.txt, you should produce a sequence of three segments: home, cay,
    //and readme.txt.
    def ex10 = {
        ???
    }

    // 11. Improve the dynamic property selector in Section 11.11, “Dynamic Invocation,” on page 150
    //so that one doesn’t have to use underscores. For example, sysProps.java.home should
    //select the property with key "java.home". Use a helper class, also extending Dynamic,
    //that contains partially completed paths.
    def ex11 = {
        ???
    }

    // 12. Define a class XMLElement that models an XML element with a name, attributes, and child
    //elements. Using dynamic selection and method calls, make it possible to select paths such as
    //rootElement.html.body.ul(id="42").li, which should return all li elements
    //inside ul with id attribute 42 inside body inside html.
    def ex12 = {
        ???
    }

    // 13. Provide an XMLBuilder class for dynamically building XML elements, as
    //builder.ul(id="42", style="list-style: lower-alpha;"), where the
    //method name becomes the element name and the named arguments become the attributes. Come
    //up with a convenient way of building nested elements.
    def ex13 = {
        ???
    }
}
