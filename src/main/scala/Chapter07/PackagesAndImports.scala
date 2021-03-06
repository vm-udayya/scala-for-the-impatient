package Chapter07

object PackagesAndImports {
// topics:
    // packages
    // scope rules
    // chained package clauses
    // top-of-file notation
    // package objects
    // package visibility
    // imports
    // imports can be anywhere
    // renaming and hiding members
    // implicit imports

    // packages manage names;
    // packages nest, just like inner classes;
    // but, unlike classes, may be defined in separate files;
    // packages paths are not absolute;
    // a chained package a.b.c leaves packages a and b invisible;
    // import statements can be anywhere;

    // packages
    def packages = {

        // same name, different packages
        val map1 = scala.collection.immutable.Map.empty[String, String]
        val map2 = scala.collection.mutable.Map.empty[String, String]

        // package statement (must be on top level):
//        package a {
//            package b {
//                package c {
//                    class Employee
//                }
//            }
//        }

        // in another file you can define
//        package a {
//            package b {
//                package c {
//                    class Manager
//                }
//            }
//        }

        // package source can be in any directory / directories

        // conversely, one file can contain a few packages

    }

    // scope rules
    def scopeRules = {
        // you can access names from the enclosing package

//        package a {
//            package b {
//                object Utils {
//                    def percentOf(value: Double, rate: Double) = value * rate / 100.0
//                }
//                package c {
//                    // members of a and b are visible here
//                    class Employee(var salary: Double) {
//                        def giveRaise(rate: Double): Unit = {
//                            // accessible from parent package
//                            salary += Utils.percentOf(salary, rate)
//                        }
//                    }
//                }
//            }
//        }

        // fly in the ointment: somebody can define already used package name
        // e.g. collection

//        package a {
//            package b {
//                package c {
//                    class Manager {
//                        // collection is scala.collection, from pre-loaded scala package
//                        val subordinates = new collection.mutable.ArrayBuffer[Int]()
//                    }
//                }
//            }
//        }

        // and someone add to project:

//        package a {
//            package b {
//                // oops
//                package collection {
//                    ???
//                }
//            }
//        }

        // and project won't compile anymore, or, worse, compile but use code from another class

        // in java it can't happen, package names are absolute;
        // in scala package names are relative, and package code may be in a few files;

        // one solution: use absolute package names, _root_.scala.collection.bla

        // another approach: use chained package: package a.b.c
    }

    // chained package clauses
    def chainedPackageClauses = {
        // package clause can contain a chain:

//        package a.b.c {
//            // members of a and b are not visible here!
//            ???
//        }

    }

    // top-of-file notation
    def topOfFileNotation = {
        // nested notation are not the best choice;
        // package clause at the top of the file are better

//        package a.b.c
//        package d
//        ???

        // is equivalent to

//        package a.b.c {
//            package d {
//                ???
//            }
//        }

        // n.b. package a.b.c also been opened
    }

    // package objects
    def packageObjects = {
        // you can't place functions or variables to package, it's a jvm limitation

        // a trick to place static members to package namespace,
        // like a.b.c.doStuff() instead of a.b.c.utils.doStuff()

        // every package can have one package object;
        // you define it in the parent package and name it as the child package
        // e.g.

//        package a.b.c
//        package object d {
//            val defaultName = "John Q. Public"
//        }
//        package d {
//            class Person { var name = defaultName }
//        }

        // package object gets compiled to a.b.c.d.package.class
        // it's a good idea to place package object code to
        // a/b/c/d/package.scala source file

    }

    // package visibility
    def packageVisibility = {
        // like in java, if not specified public, private or protects:
        // restrict access to class members, only code inside specified package get access

//        package a.b.c.d
//        class Person {
//            private[d] def description = "A person ..."
//            // or extended to c
//            private[c] def desc = "Another ..."
//        }

    }

    // imports
    def imports = {
        // with imports you can use short names
        import java.awt.Color
        val c = Color.YELLOW

        // you can import all members, like '*' in java
        // but in scala '*' is a valid char for an identifier,
        // please don't use it as a package name
        import java.awt._
        // like import static in java
        val d = new Button("click me")
    }

    // imports can be anywhere
    def importsCanBeAnywhere = {
        // scope of the import statement extends until the end of the block

        class Manager {
            import scala.collection.mutable
            val subordinates = mutable.ArrayBuffer.empty[Int]
        }

        // imports can be anywhere
        // this is a very useful feature, you can greatly reduce the potential for conflicts

    }

    // renaming and hiding members
    def renamingAndHidingMembers = {
        // using import selector you can do stuff:

        // import a few members
        import java.awt.{Color, Font}

        // rename members
        import java.util.{HashMap => JavaHashMap}
        import scala.collection.mutable.{HashMap => ScalaHashMap}

        // hide members
        import java.util.{HashMap => _, _} // import all but HashMap

    }

    // implicit imports
    def implicitImports = {
        // pre-loaded namespaces

        import java.lang._
        import scala._ // unlike others, this is allowed to override the preceding
        import Predef._ // Predef object, was introduced before package object lang. feature
    }

}

object PackagesAndImports_Exercises {

    // 1. Write an example program to demonstrate that
    //    package com.horstmann.impatient
    // is not the same as
    //    package com
    //    package horstmann
    //    package impatient
    def ex1 = {
        import exercises.{ex1_1, ex1_2}
        val p1 = ex1_1.com.horstmann.impatient.a
        val p2 = ex1_2.com.horstmann.impatient.a
    }

    // 2. Write a puzzler that baffles your Scala friends, using a package com that isn’t at the top level.
    def ex2 = {
        import exercises.{ex2 => pex2}
        val p = pex2.horstmann.nut
    }

    // 3. Write a package random with functions nextInt(): Int, nextDouble(): Double,
    // and setSeed(seed: Int): Unit. To generate random numbers, use the linear
    // congruential generator
    //    next = (previous × a + b) mod 2^n
    // where a = 1664525, b = 1013904223, n = 32, and the initial value of previous is seed.
    def ex3 = {
        import exercises.{ex3 => pex3}
        val ri = pex3.random.nextInt()
        val t = pex3.random.test.a
    }

    // 4. Why do you think the Scala language designers provided the package object syntax
    // instead of simply letting you add functions and variables to a package?
    def ex4 = {
        // first: jvm limitations
        // second: all object code will be collected in one package.class anyway
    }

    // 5. What is the meaning of private[com] def giveRaise(rate: Double)? Is it
    // useful?
    def ex5 = {
        // method accessible only for 'com' package members. Yes and no:
        // fine-grained access control is good, com package is no-good.
    }

    // 6. Write a program that copies all elements from a Java hash map into a Scala hash map. Use
    // imports to rename both classes.
    def ex6 = {
        import java.util.{HashMap => juHashMap}
        import scala.collection.mutable.{HashMap => cmHashMap, Map}
        import scala.collection.JavaConverters._

        def copy[T, S](a: Map[T, S], b: cmHashMap[T, S]): Unit = {
            for ((k,v) <- a) b.update(k, v)
        }

        val shm = cmHashMap.empty[String, Int]
        val jhm: Map[String, Int] = new juHashMap[String, Int]().asScala
        jhm.put("one", 1)
        jhm.put("two", 2)

        copy(jhm, shm)
        for ((k,v) <- shm) println(s"$k: $v")
    }

    // 7. In the preceding exercise, move all imports into the innermost scope possible.
    def ex7 = {
        ex6
    }

    // 8. What is the effect of
    //    import java._
    //    import javax._
    // Is this a good idea?
    def ex8 = {
        import java._
        import javax._
        // same names in both packages,
        // javax._ will redefine java._ names
        // in these packages contains a lot of names, do you need them all?
        // bad idea
    }

    // 9. Write a program that imports the java.lang.System class, reads the user name from the
    // user.name system property, reads a password from the StdIn object, and prints a message
    // to the standard error stream if the password is not "secret". Otherwise, print a greeting to
    // the standard output stream. Do not use any other imports, and do not use any qualified names
    // (with dots).
    def ex9 = {
        // 1 - Do not use any other imports,
        // 2 - and do not use any qualified names (with dots).
        // I'm not sure, what you mean stating limitation #2?
        // repeat packages structure? package java.lang.System; scala.io
        object app extends App {
            import java.lang.{System => jls}
            val uname = jls.getProperty("user.name", "John/Jane Doe")
            val pwd = scala.io.StdIn.readLine(s"your name is $uname and your password is:")
            if (pwd == "secret") println(s"Hello $uname, welcome back!")
            else jls.err.println(s"$uname, wait please, we sending a car for you.")
        }
    }

    // 10. Apart from StringBuilder, what other members of java.lang does the scala package override?
    def ex10 = {
//        Boolean, Byte, Long, ...
// https://www.scala-lang.org/api/current/scala/index.html
// https://docs.oracle.com/javase/8/docs/api/java/lang/package-summary.html
    }
}

package exercises {

    package ex1_1 {
        package com.horstmann.impatient {
            // no problem
            object a { val coll = collection.mutable.Map.empty }
        }
        package com.horstmann.collection {
            object mutable { val Map = ??? }
        }
    }
    package ex1_2 {
        package com { package horstmann { package impatient {
            object a {
                // oops, problem
                // val coll = collection.mutable.Map.empty
            }
        }}}
        package com.horstmann.collection {
            object mutable { val Map = ??? }
        }
    }

    package ex2 {
        package horstmann.com {
            object nuts { val almond = ??? }
        }
        package horstmann {
            import com.nuts._
            object nut { val value = almond }
        }
    }

    package ex3 {
        package object random {
            def nextInt(): Int = next().toInt
            def nextDouble(): Double = next()
            def setSeed(seed: Int): Unit = { prev = seed }

            private val a = 1664525
            private val b = 1013904223
            private val n = 32
            private var prev = compat.Platform.currentTime.toDouble

            // linear congruential generator
            private def next(): Double = {
                prev = (prev * a + b) % math.pow(2, n)
                prev
            }
        }
        package random {
            object test { val a = nextInt() }
        }
    }

}
