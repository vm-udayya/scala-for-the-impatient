package Chapter19

import java.awt._
import java.awt.event.ActionEvent
import java.awt.geom._
import java.awt.image.BufferedImage

import javax.swing.JComponent

import scala.collection.mutable
import scala.util.Try

object AdvancedTypes {
// topics:
    // singleton types
    // type projections
    // paths
    // type aliases
    // structural types
    // compound types
    // infix types
    // existential types
    // the scala type system
    // self types
    // dependency injection
    // abstract types
    // family polymorphism
    // higher-kinded types

    // singleton types: this.type (for method chaining);
    // type projection: inner class access, Network#Member (not a path);
    // structural type: duck typing, in-place type definition;
    // existential type: X[T] forSome { type T ... }, for java wildcards;
    // self type: this: Type => ...; restriction to mixin;
    // dependency injection = self types + cake pattern;

    // singleton types
    def singletonTypes = {
        // obj.type for method chaining with class hierarchy;
        // or for 'fluent interface' api

        // this.type example, method chaining

        def compileerror = {
            class Document {
                // n.b. return type is 'Document', later we'll need it to be 'Book'
                def setTitle(title: String) = /* do stuff */ this
                def setAuthor(author: String) = /* do stuff */ this
            }
            // chain
            val article: Document = ???
            article.setTitle("").setAuthor("")

            // what about subclass? problem
            class Book extends Document {
                def addChapter(ch: String) = /* do stuff */ this
            }
            val book: Book = ???

            // compile error, Document.addChapter???
            // book.setTitle("").addChapter("")
        }

        def compileok = {
            class Document {
                // fix method return type: 'this.type', not 'Document'
                def setTitle(title: String): this.type = /* do stuff */ this
                def setAuthor(author: String) = /* do stuff */ this
            }
            // chain
            val article: Document = ???
            article.setTitle("").setAuthor("")

            // what about subclass? problem
            class Book extends Document {
                def addChapter(ch: String) = /* do stuff */ this
            }
            val book: Book = ???

            // compile OK
            book.setTitle("").addChapter("")
        }

        // singleton.type example fluent interface with object passing

        // doc set Title to "foo"
        // doc.set(Title).to("foo")
        // method 'set' is special, argument is the singleton Title

        object Title // singleton, not a type
        class Document {
            private var useNextArgAs: Any = _
            // n.b. Title.type
            def set(obj: Title.type) = { useNextArgAs = obj; this }
            def to(x: String) = useNextArgAs match {
                case Title => ???
                case _ => ???
            }
        }

    }

    // type projections
    def typeProjections = {
        // access to nested class; Network#Member
        // not a path

        // example: can't access nested class objects

        class Network {
            class Member(val name: String) { val contacts = new mutable.ArrayBuffer[Member] }
            private val members = new mutable.ArrayBuffer[Member]
            def join(name: String) = { members += new Member(name); members.last }
        }
        // each network instance has its own Member class
        val chatter = new Network
        val myface = new Network
        // you can't add a member from one network to another
        val fred = chatter.join("Fred") // chatter.Member
        val barney = myface.join("Barney") // myface.Member
        // error:
        // fred.contacts.append(barney) // compile error, type mismatch

        // you can move Member class outside the Network class,
        // to a Network companion object maybe.

        // or you can save fine-grained classes, using 'type projection'
        // Network#Member, which means 'a member of any network'
        def typeprojection = {
            class Network {
                // n.b. type projection in 'contacts' definition!
                class Member(val name: String) { val contacts = new mutable.ArrayBuffer[Network#Member] }
                private val members = new mutable.ArrayBuffer[Member]
                def join(name: String) = { members += new Member(name); members.last }
            }
            val chatter = new Network
            val myface = new Network
            val fred = chatter.join("Fred")
            val barney = myface.join("Barney")
            // works just fine:
            fred.contacts.append(barney)
        }

        // n.b. you can't import a type projection, it's not a path
    }

    // paths
    def paths = {
        // type path: com.horstmann.impatient.Network.Member
        // each component in com.horstmann.impatient.Network must be stable;
        // stable: specify a single, definite scope;
        // - package
        // - object
        // - val
        // - this, super, ...

        // path component can't be a class or 'var'
        // nested class isn't a single type; var is mutable.

        // internally, compiler translates nested type expressions to type projections
        // a.b.c -> a.b.type#c
        // any c inside singleton b.type
    }

    // type aliases
    def typeAliases = {
        // keyword 'type';
        // type alias must be nested inside a class or object;

        // type alias example
        class Book {
            type Index = mutable.HashMap[String, String]
            val idx: Index = ???
        }

        // aside: 'type' keyword is also used for 'abstract types'
        // e.g.
        abstract class Reader {
            type Contents
            def read(filename: String): Contents
        }
        // details below
    }

    // structural types
    def structuralTypes = {
        // is a specification of abstract methods, fields, types
        // that a conforming type should possess

        // e.g. parameter 'target' of structural type
        def appendLines(
                           target: { def append(str: String): Any }, // structural type
                           lines: Iterable[String]) = {
            for (x <- lines) target.append(x)
        }
        // can pass any object with 'append' method, this is more flexible than defining a Appendable trait;
        // but much more expensive because of used reflection

        // similar to duck typing: you don't have to declare obj as a Duck
        // as long as it walks and quacks like one
    }

    // compound types
    def compoundTypes = {
        // aka intersection type, `T1 with T2 with T3 ...`
        // you can use it to require some properties from type

        val image = new mutable.ArrayBuffer[java.awt.Shape with java.io.Serializable]
        // can draw, can serialize whole collection
        image += new Rectangle(5, 10, 20, 30)
        // image += new Area(rect) // compile error: is a shape but not serializable

        trait ImageShape extends Shape with java.io.Serializable
        // this means: extends the intersection type 'Shape with Serializable'

        // you can add a structural type to compound type
        type A = Shape with java.io.Serializable { def contains(p: Point): Boolean }

        // { def contains(p: Point): Boolean } transforms to
        // AnyRef { def contains(p: Point): Boolean }
        // and
        // Shape with java.io.Serializable transforms to
        // Shape with java.io.Serializable { }
    }

    // infix types
    def infixTypes = {
        // parameters of a type written in infix notation, e.g. `String Map Int`
        type StringToInt = String Map Int // Map[String, Int]

        // pairs example
        type x[A, B] = (A, B)
        type IntPair = Int x Int

        // infix type operators have the same precedence, left-associative;
        // unless ends with ':'
        type A = String x Int x Int // ((String, Int), Int)

        // can't write in infix notation:
        type B = (Int, String, Double)

        // can't use '*' as infix type name to avoid confusion with variable argument declarations T*

    }

    // existential types
    def existentialTypes = {
        // formalism added for compatibility with Java wildcards, `typeExpr[T] forSome { type T ... }`

        type A = Array[T] forSome { type T <: JComponent}
        // this is the same as
        type B = Array[_ <: JComponent]

        // scala wildcards are syntactic sugar for existential types
        type C = Array[_] // is the same as
        type D = Array[T] forSome { type T }
        // another equivalent
        type E = Map[_, _]
        type F = Map[T, U] forSome { type T; type U }

        // forSome allows complex relationships
        type J = Map[T, U] forSome { type T; type U <: T}

        // 'val' declarations for nested types

        // nested types
        class Network {
            class Member(val name: String)
            private val members = new mutable.ArrayBuffer[Member]
            def join(name: String) = { members += new Member(name); members.last }
        }

        // type projection Network#Member as member of any network, compare to
        // val declarations
        type H = n.Member forSome { val n: Network }

        // process only members from same network
        def process[M <: n.Member forSome { val n: Network }](m1: M, m2: M) = (m1, m2)
        // test
        val chatter = new Network
        val myface = new Network
        val fred = chatter.join("Fred")
        val barney = myface.join("Barney")
        // process(fred, barney) // compiler error

        // import scala.language.existentials
    }

    // the scala type system
    def theScalaTypeSystem = {
        // scala types that user can declare:

        // class, trait: class C ..., trait T ...
        // tuple: (T1, ..., Tn)
        // function: (T1, ..., Tn) => T
        // annotated type: T @A
        // parameterized type: T[A, ..., B]
        // singleton: value.type
        // type projection: A#B
        // compound type: T1 with T2, ... with Tn { declarations }
        // infix type: A T B
        // existential type: T forSome { type, val declarations }

        // also, internally there is 'method type'
        // (T1, ..., Tn)T
        // n.b. compare to function type

    }

    // self types
    def selfTypes = {
        // a trait can require that it can only be mixed into a subclass of the given type
        // using 'self type' declaration
        // this: Type => ...

        trait Logged { def log(msg: String): Unit }
        trait LoggedException extends Logged {
            this: Exception => // self type declaration
            override def log(msg: String): Unit = log(getMessage)
        }
        // LoggedException can only be mixed into Exception

        // if you require multiple types, use a compound type
        // this: T1 with T2 with ... =>

        // you can combine self type with  the 'alias for enclosing this'
        // trait Group{ outer: Network =>
        //      class Member ( ...
        // this syntax introduces a great deal of confusion

        // self types do not automatically inherit:
        trait ManagedException extends LoggedException {
            // you must to repeat the self type
            this: Exception => ???
        }

    }

    // dependency injection
    def dependencyInjection = {
        // cake pattern

        // large system: different implementations for each component;
        // need to assemble the component choices;
        // mock/real database, console/file/etc logging, ...
        // some dependency among the components: database(logging)

        // java: spring, OSGi describes component interfaces it depends on,
        // references to actual implementation are injected when app is assembled.

        // scala: simple form of dependency injection using traits and self types

        // not really good but very simple method: glue/mix-in actual classes
        def traitsMixin = {
            // define components using self types
            trait Logger { def log(msg: String): Unit }
            trait Auth { this: Logger => def login(id: String, secret: String): Boolean }
            trait App { this: Logger with Auth => ??? }

            // some implementations
            trait FileLogger extends Logger { def logfname: String; override def log(msg: String): Unit = ??? }
            trait MockAuth extends Auth { this: Logger => def dbfname: String; override def login(id: String, secret: String): Boolean = ??? }

            // assemble an app
            object ComplexApp extends App with FileLogger with MockAuth { val logfname="test.log"; val dbfname="users.db" }

            // app really isn't an authenticator and a logger, not very clever solution.
            // but simple
        }

        // more natural to use instance variables for the components;
        // cake pattern:
        // supply a component trait for each service, containing:
        // - dependent components expressed as self types;
        // - a trait describing the service interface;
        // - an abstract 'val' instantiated with a concrete service;
        // - implementations of the service interface (optionally)

        trait LoggerComponent { // no self type: not depend on anything
            trait Logger { ??? }                                    // interface
            val logger: Logger                                      // instance
            class FileLogger(fname: String) extends Logger { ??? }  // implementation
        }

        trait AuthComponent { this: LoggerComponent =>  // self type, depend on logger
            trait Auth { ??? }                          // interface
            val auth: Auth                              // instance
            class MockAuth(fname: String) extends Auth { ??? }  // implementation
        }

        // assemble an app, configure components
        object AppComponent extends LoggerComponent with AuthComponent {
            val logger = new FileLogger("test.log")
            val auth = new MockAuth("users.db")
        }

        // either approach is better than some config magic with XML:
        // compiler can verify the module dependencies are satisfied
    }

    // abstract types
    def abstractTypes = {
        // useful replacement for type parameters while creating a class hierarchy

        // class or trait can define an abstract type (with type bounds)

        def abstracttypes = {
            trait Reader {
                type Contents // abstract
                def read(fname: String): Contents
            }
            // concrete subclass needs to specify the type
            class StringReader extends Reader {
                type Contents = String
                override def read(fname: String): String = ???
            }
            class ImageReader extends Reader {
                type Contents = BufferedImage
                override def read(fname: String): BufferedImage = ???
            }
        }

        // the same effect could be achieved with a type parameter
        trait Reader[C] { def read(fname: String): C }
        class StringReader extends Reader[String] { override def read(fname: String): String = ??? }
        class ImageReader extends Reader[BufferedImage] { override def read(fname: String): BufferedImage = ??? }

        // which is better?
        // - use type parameters when the types are supplied at the class instantiation;
        // - use abstract types when building a class hierarchy;

        // abstract types can work better when there are many type dependencies/type parameters;
        // or you need to express subtle interdependencies between types

        trait Listener {
            class EventObject
            type Event <: EventObject // can't be done with type parameters: bound is an inner class
        }
        trait ActionListener extends Listener {
            class ActionEvent extends EventObject
            type Event = ActionEvent
        }

    }

    // family polymorphism
    def familyPolymorphism = {
        // families of types that vary together;
        // consider event handling for instance

        // generic types/type parameters example

        def firstApproach = {
            trait Listener[Event] { def occured(e: Event): Unit }
            trait Source[Event, L <: Listener[Event]] {
                private val listeners = new mutable.ArrayBuffer[L]
                // def add, remove
                def fire(e: Event) = { for (x <- listeners) x.occured(e) }
            }

            // consider a button with action events, good enough
            trait ActionListener extends Listener[ActionEvent]
            class Button extends Source[ActionEvent, ActionListener] {
                // but, ActionEvent source is Object, not a type-safe
                def click() = { fire(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click")) }
            }
        }

        // type-safe version
        trait Event[S] { var source: S = _ }
        trait Listener[S, E <: Event[S]] { def occured(e: E): Unit }
        trait Source[S, E <: Event[S], L <: Listener[S, E]] { this: S => // self type for source
            private val listeners = new mutable.ArrayBuffer[L]
            def fire(e: E) = {
                e.source = this // type-safe event source
                for (x <- listeners) x.occured(e)
            }
        }

        // button implementation could be:
        class ButtonEvent extends Event[Button]
        trait ButtonListener extends Listener[Button, ButtonEvent]
        class Button extends Source[Button, ButtonEvent, ButtonListener] {
            def click() = fire(new ButtonEvent)
        }
        // you can see the proliferation of the type parameters,
        // family polymorphism in action

        // lets rewrite it with abstract types (a little nicer);
        // price to pay: you need to wrap declarations in top-level object/trait/class

        trait ListenerSupport {
            type E <: Event
            type L <: Listener
            type S <: Source

            trait Event { var source: S = _ }
            trait Listener { def occured(e: E): Unit }
            trait Source { this: S =>
                private val listeners = new mutable.ArrayBuffer[L]
                def fire(e: E) = { e.source = this; for (x <- listeners) x.occured(e) }
            }
        }

        // button implementation
        object ButtonModule extends ListenerSupport {
            type E = ButtonEvent
            type L = ButtonListener
            type S = Button

            class ButtonEvent extends Event
            trait ButtonListener extends Listener
            class Button extends Source { def click() = { fire(new ButtonEvent) } }
        }

        // button usage example:
        object Main {
            val b = new ButtonModule.Button
            // b.add(new ButtonListener { def occured(e: ButtonEvent) = ??? }
            b.click()
        }

    }

    // higher-kinded types
    def higherKindedTypes = {
        // type that uses a type constructor to produce types
        // (e.g. implementing generic 'map' method)

        // import scala.language.higherKinds

        // consider: type List depends on type T (List[T]) and produces a type, say List[Int].
        // List is a type constructor.

        // where we may need to use a type constructor?
        // 'map' function (what about functor?)
        def problem = {

            trait Iterable[E] {
                def map[F](func: E => F): Iterable[F]
            }
            // if you want a generic implementation, you want to construct concrete Iterable[F] in this trait,
            // but you can't

            class Buffer[E] extends Iterable[E] {
                def map[F](f: E => F): Buffer[F] = ??? // you need to produce Buffer[F] in Iterable trait, do you?
            }
        }

        // unless you add another type parameter: type constructor:
        def add_type_constructor = {

            trait Iterable[E, C[_]] { // iterable depends on a type constructor: Iterable is a higher-kinded type
                def build[F](): C[F] = ???
                def map[F](f: E => F): C[F] = ???
            }
        }

        // to use a constructed type we need to know that it can be an appendable container
        def add_container = {
            // typical use of a higher-kinded types: an iterator depends on Container,
            // container is a mechanism for making types (type constructor)

            trait Container[E] { def +=(e: E): Unit = ??? }

            trait Iterable[E, C[F] <: Container[F]] { // type constructor bound to be a Container
                def build[F](): C[F] = ???              // result is a container, appendable
                def map[F](f: E => F): C[F] = {       // build, then append
                    val res = build[F]()
                    // res += f(iter.next())
                    res }
            }

            // generic higher-kinded type Iterable can be used in collections lib:

            // range is an iterable but not a container, can't append;
            // 'map' produce not a range but buffer
            class Range(low: Int, high: Int) extends Iterable[Int, Buffer] {
                override def build[F]() = new Buffer[F]
            }

            // buffer is an iterable and container
            class Buffer[E] extends Iterable[E, Buffer] with Container[E] {
                override def build[F]() = new Buffer[F]
                override def +=(e: E) = ???
            }

        }

        // scala in collections lib uses an implicit parameter to conjure up an object
        // for building the target collection

    }

}

object AdvancedTypes_Exercises {

    // 1. Implement a Bug class modeling a bug that moves along a horizontal line.
    // The 'move' method moves in the current direction,
    // the 'turn' method makes the bug turn around,
    // and the 'show' method prints the current position.
    // Make these methods chainable.
    // For example,
    //      bugsy.move(4).show().move(6).show().turn().move(5).show()
    // should display '4 10 5'
    def ex1 = {
        class Bug {
            private var position: Int = 0
            private var direction: Int = 1
            def move(steps: Int): this.type = { position += steps * direction; this }
            def turn(): this.type = { direction *= -1; this }
            def show(): this.type = { println(position); this }
        }

        // test
        val bugsy = new Bug
        bugsy.move(4).show().move(6).show().turn().move(5).show()
    }

    // 2. Provide a fluent interface for the Bug class of the preceding exercise,
    // so that one can write
    //      bugsy move 4 and show and then move 6 and show turn around move 5 and show
    def ex2 = {
        // compiler unhappy when I define classes inside method
        import ex2_classes._

        // test: scala> Chapter19.AdvancedTypes_Exercises.ex2
        import BugCmd._
        val bugsy = new Bug with Fluent
        bugsy move 4 and show and next move 6 and show turn around move 5 and show // 4 10 5
        // bugsy.move(4).and(show).and(next).move(6).and(show).turn(around).move(5).and(show)
    }

    object ex2_classes {
        class Bug {
            private var position: Int = 0
            private var direction: Int = 1
            def move(steps: Int): this.type = { position += steps * direction; this }
            def turn(): this.type = { direction *= -1; this }
            def show(): this.type = { println(position); this }
        }

        object BugCmd { // extra namespace because 'show' doubling
            class Command
            object show extends Command
            object next extends Command
            object around extends Command
        }

        trait Fluent { this: Bug =>
            def turn(dir: BugCmd.around.type): this.type = { turn(); this }
            def and(cmd: BugCmd.Command): this.type = cmd match {
                case a: BugCmd.show.type => this.show()
                case b: BugCmd.next.type => this
            }
        }
    }

    // 3. Complete the fluent interface in Section 19.1, “Singleton Types,” on page 280
    // so that one can call
    //      book set Title to "Scala for the Impatient" set Author to "Cay Horstmann"
    def ex3 = {
        class Property
        object Title extends Property
        object Author extends Property

        class Document {
            private var useNextArgAs: Any = _

            def set(obj: Property): this.type = { useNextArgAs = obj; this }

            def to(x: String): this.type = { useNextArgAs match {
                case a: Title.type => println(s"set title to '${x}'")
                case b: Author.type => println(s"set author to '${x}'")
                case _ => sys.error(s"unknown property ${useNextArgAs}")
            }; this }
        }

        // test
        val book = new Document
        book set Title to "Scala for the Impatient" set Author to "Cay Horstmann"
        // book.set(Title).to("Scala for the Impatient").set(Author).to("Cay Horstmann")
    }

    // 4. Implement the 'equals' method for the Member class that is nested inside the Network class
    // in Section 19.2, “Type Projections,” on page 281.
    // For two members to be equal, they need to be in the same network.
    def ex4 = {
        // you can save fine-grained classes, using 'type projection'
        // Network#Member, which means 'a member of any network'.
        // n.b. you can't import a type projection, it's not a path

        class Network { outer =>

            class Member(val name: String) {
                val contacts = new mutable.ArrayBuffer[Network#Member]

                override def equals(other: Any): Boolean = other match {
                    case that: outer.Member => { name == that.name && contacts == that.contacts }
                    case _ => { println("wrong type"); false }
                }
                final override def hashCode(): Int = (name, contacts.mkString).## // ## method is null-safe: yields 0 for null
                override def toString: String = s"Member($name) with contacts: ${contacts.map(_.name).mkString(",")}"
            }

            private val members = new mutable.ArrayBuffer[Member]
            def join(name: String): Member = { members += new Member(name); members.last }
        }

        // test
        val chatter = new Network
        val myface = new Network

        val fred =         chatter.join("Fred")
        val anotherFred =  chatter.join("Fred")
        val barney =        myface.join("Fred")
        // possible to add contacts from another network
        fred.contacts.append(barney)
        anotherFred.contacts.append(barney)

        assert(fred == anotherFred)
        assert(barney != fred)
    }

    // 5. Consider the type alias
    //      type NetworkMember = n.Member forSome { val n: Network }
    // and the function
    //      def process(m1: NetworkMember, m2: NetworkMember) = (m1, m2)
    // How does this differ from the 'process' function in
    // Section 19.8, “Existential Types,” on page 286?
    def ex5 = {
        class Network { class Member { } }

        // Section 19.8, “Existential Types,” on page 286
        def process[M <: n.Member forSome { val n: Network }](m1: M, m2: M) = (m1, m2)
        // allow processing members only from the same network

        // ex5 func
        type NetworkMember = n.Member forSome { val n: Network }
        def processEx5(m1: NetworkMember, m2: NetworkMember) = (m1, m2)
        // allow processing members from different networks

        // test
        val n1 = new Network
        val n2 = new Network
        val n1m1 = new n1.Member
        val n1m2 = new n1.Member
        val n2m1 = new n2.Member

        val p1 = process(n1m1, n1m2)
        // val p2 = process(n1m1, n2m1) // compiler error: inferred type arguments [Network#Member] do not conform to method process's type parameter bounds [M <: n.Member forSome { val n: Network }]

        val p2 = processEx5(n1m1, n1m2)
        val p3 = processEx5(n1m1, n2m1) // ok
    }

    // 6. The Either type in the Scala library can be used for algorithms
    // that return either a result or some failure information.
    // Write a function that takes two parameters:
    // a sorted array of integers and an integer value.
    // Return either the index of the value in the array or
    // the index of the element that is closest to the value.
    // Use an infix type as the return type.
    def ex6 = {
        type SearchResult = Int Either Int

        def findIndex(arr: Array[Int], value: Int): SearchResult = {
            // probably should apply binary search or
            // iterate over arr checking the iteration state:
            // (distance to prev.value, dist. to curr. value, curr idx)
            val res = arr.zipWithIndex.map { case (n, idx) => (math.abs(n - value), idx) }
            val nearest = res.sortBy(_._1).apply(0)
            if (nearest._1 == 0) Right(nearest._2)
            else Left(nearest._2)
        }

        // test
        val arr = Array(1,2,3)
        assert(findIndex(arr, 2).right.get == 1)
        assert(findIndex(arr, 4).left.get == 2)
        findIndex(arr, 0)
    }

    // 7. Implement a method that receives an object of any class that has a method
    //      def close(): Unit
    // together with a function that processes that object.
    // Call the function and invoke the 'close' method upon completion, or when any exception occurs.
    def ex7 = {

        // compound type // structural type
        type Closable = { def close(): Unit }

        def process[A <: Closable, R](obj: A, func: Closable => R): Option[R] = {
            val res = Try(func(obj))
            obj.close()
            res.toOption
        }

        // test
        val obj = new AnyRef { def close(): Unit = println(s"closing object ${this}") }
        process(obj, x => println(s"processing object ${x}"))
    }

    // 8. Write a function 'printValues' with three parameters
    // f, from, to
    // that prints all values of 'f' with inputs from the given range.
    // Here, 'f' should be any object with an apply method that consumes and yields an Int.
    // For example,
    //      printValues((x: Int) => x * x, 3, 6) // Prints 9 16 25 36
    //      printValues(Array(1, 1, 2, 3, 5, 8, 13, 21, 34, 55), 3, 6) // Prints 3 5 8 13
    def ex8 = {
        // probably, he meant Function1[Int, Int], not 'object with an apply'

        // def printValues(f: { def apply(x: Int): Int }, from: Int, to: Int): Unit = {
        def printValues(f: Int => Int, from: Int, to: Int): Unit = {
            val res = for (i <- from to `to`) yield f(i)
            println(res.mkString(" "))
        }

        // test
        printValues((x: Int) => x * x, 3, 6) // Prints 9 16 25 36
        printValues(Array(1, 1, 2, 3, 5, 8, 13, 21, 34, 55), 3, 6) // Prints 3 5 8 13
    }

    // 9. Consider this class that models a physical dimension:
    //  abstract class Dim[T](val value: Double, val name: String) {
    //      protected def create(v: Double): T
    //      def +(other: Dim[T]) = create(value + other.value)
    //      override def toString() = s"$value $name"
    //  }
    // Here is a concrete subclass:
    //  class Seconds(v: Double) extends Dim[Seconds](v, "s") {
    //      override def create(v: Double) = new Seconds(v)
    //  }
    // But now a knucklehead could define
    //  class Meters(v: Double) extends Dim[Seconds](v, "m") {
    //      override def create(v: Double) = new Seconds(v)
    //  }
    // allowing meters and seconds to be added.
    // Use a self type to prevent that.
    def ex9 = {

        abstract class Dim[T](val value: Double, val name: String) {
            // just add one line of code:
            this: T =>

            protected def create(v: Double): T
            def +(other: Dim[T]) = create(value + other.value)
            override def toString() = s"$value $name"
        }

        class Seconds(v: Double) extends Dim[Seconds](v, "s") {
            override def create(v: Double) = new Seconds(v)
        }

        // and this become illegal:
        //class Meters(v: Double) extends Dim[Seconds](v, "m") { // self-type Meters does not conform to Dim[Seconds]'s selftype Dim[Seconds] with Seconds
        //    override def create(v: Double) = new Seconds(v)
        //}

    }

    // 10. Self types can usually be replaced with traits that extend classes,
    // but there can be situations where using self types changes the
    // initialization and override orders.
    // Construct such an example.
    def ex10 = {
        def traitConstructionOrder = {

            // constructors execution order:
            //  superclass
            //  traits left-to-right
            //  within each trait, parents constructed first (each parent only once)
            //  subclass

            // e.g.
            // class SavingsAccount extends Account with FileLogger with ShortLogger
            //  Account as superclass
            //  Logger as parent of FileLogger
            //  FileLogger
            //  ShortLogger w/o Logger
            //  SavingsAccount

            // constructor ordering is the _reverse_ of the linearization

            // linearization of the class: tech spec of all superclasses, defined by rule:
            // if C extends C1 with C2 with ... Cn,
            // then lin(C) = C >> lin(Cn) >> ... >> lin(C2) >> lin(C1)
            // where '>>' means "concatenate and remove duplicates, with the right winning out"
            // e.g. lin(SavingsAccount)
            //  = SavingsAccount >> lin(ShortLogger) >> lin(FileLogger) >> lin(Account)
            //  = SavingsAcount >> (ShortLogger >> Logger) >> (FileLogger >> Logger) >> Account
            //  = SavingsAcount >> ShortLogger >> FileLogger >> Logger >> Account

            // linearization gives the order (left-to-right) in which 'super' is resolved in a _trait_

        }

        // no traits, self type
        abstract class Dim[T](val value: Double, val name: String) { this: T =>
            protected def create(v: Double): T
            override def toString: String = s"$value $name"
        }

        // lin(Seconds) = Seconds >> lin(Dim)
        // construction order: Dim(value=v, name=s), Seconds
        class Seconds(v: Double) extends Dim[Seconds](v, "s") {
            override def create(v: Double) = new Seconds(v)
        }

        // traits, no self type
        trait TDim[T] {
            def value: Double
            def name: String
            def create(v: Double): T
            override def toString: String = s"$value $name"
        }

        // lin(Seconds) = Seconds >> lin(Dim)
        // construction order: Dim(), Seconds(value, name=s)
        class TSeconds(val value: Double, val name: String = "s") extends TDim[TSeconds] {
            override def create(v: Double): TSeconds = new TSeconds(v, name)
        }
    }

}
