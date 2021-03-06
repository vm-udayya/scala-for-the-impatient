package Chapter17

import scala.annotation.tailrec

object Futures {
// topics:
    // running tasks in the future
    // waiting for results
    // the Try class
    // callbacks
    // composing future tasks
    // other future transformations
    // methods in the Future Object
    // promises
    // execution contexts

    // as long as the computations don't have side effects, you can let them run concurrently
    // and combine the results when they become available

    // Future { concurrent block }
    // you can use callbacks, but chaining them is a pain in the ass (callback hell)
    // use map/flatMap, for-expr to compose futures
    // a promise has a future whose value can be set (once)
    // fork-join pool for cpu-bound tasks

    import java.time._
    import scala.util._
    import scala.concurrent._
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global
    import java.util.concurrent.Executors

    // running tasks in the future
    def runningTasksInTheFuture = {
        // future: an object that give you a result/failure eventually.
        // java.util.concurrent.Future interface: much more limited;
        // java CompletionStage interface more like scala future

        // execute a block of code in the future
        Future {
            // run on some thread from the pool defined in ExecutionContext
            // default ex.context: fork-join pool from import scala.concurrent.ExecutionContext.Implicits.global
            Thread.sleep(1.second.toMillis)
            println(s"this is the future at ${LocalTime.now}")
        }
        println(s"this is the present at ${LocalTime.now}")
        // this is the present at 15:01:39.659
        // this is the future at 15:01:40.684

        // in a real program use a custom exec.context
        // cpu bound vs i/o bound

        // concurrent execution for multiple futures
        Future { for (i <- 1 to 10) {print("A"); Thread.sleep(100)} }
        Future { for (i <- 1 to 10) {print("B"); Thread.sleep(100)} }
        // BABABABABABABABABA

        // future can have a result
        val f: Future[Int] = Future { Thread.sleep(1000); 42 }
        f // res2: scala.concurrent.Future[Int] = Future(<not completed>)
        //scala> f
        //res3: scala.concurrent.Future[Int] = Future(Success(42))

        // failure as a result
        val f2: Future[Int] = Future { if (LocalTime.now.getHour > 5) sys.error("too late"); 42 }
        // f2: scala.concurrent.Future[Int] = Future(<not completed>)
        //scala> f2
        //res7: scala.concurrent.Future[Int] = Future(Failure(java.lang.RuntimeException: too late))

        // stay away from side effects/shared mutable state, even threadsafe ones.
        // have each future compute a value and then combine them

    }

    // waiting for results
    def waitingForResults = {
        // check using 'isCompleted';

        // make a blocking call // better not
        val f = Future { Thread.sleep(1.second.toMillis); 42 }
        val res: Int = Await.result(f, 2.second) // blocks for 1 sec. and yield the result
        // or throw an error: java.util.concurrent.TimeoutException

        // if task throws an exception, it is rethrown in Await.result
        val f2: Future[Int] = Future { if (LocalTime.now.getHour > 5) sys.error("too late"); 42 }
        val res2: Int = Await.result(f2, 1.second) // java.lang.RuntimeException: too late

        // to avoid rethrown exception use 'ready'
        val res3: Future[Int] = Await.ready(f2, 1.second) // block, wait and return f2
        // scala> val res3 = Await.ready(f2, 1.second)
        //res3: f2.type = Future(Failure(java.lang.RuntimeException: too late))
        //
        //scala> val res3 = Await.ready(f, 1.second)
        //res3: f.type = Future(Success(42))

        res3.value // Option[scala.util.Try[Int]] = Some(Success(42))

        // Future class also has 'result' and 'ready': thread blocking methods, don't use them

        // not all exceptions are stored in the result, jvm errors and InterruptedException are allowed to propagate

    }

    // the Try class
    def theTryClass = {
        // monadic error wrapper, case classes Success[T], Failure[E <: Throwable]
        val res: Try[Int] = ???
        res match {
            case Success(v) => println(s"result $v")
            case Failure(ex) => println(s"error: ${ex.getMessage}")
        }

        res.isSuccess // Boolean
        res.isFailure

        // failed Try[T] into Try[Throwable]
        val fail: Try[Throwable] = res.failed

        // get value from try
        fail.get.getMessage

        // try to option
        res.toOption.getOrElse(-1)

        // construct try
        val res2: Try[Int] = Try("".toInt)

        val res3 = Try {
            val inp = scala.io.StdIn.readLine("enter a number:")
            inp.trim.toInt
        }

        // compose try
        def readInt(prompt: String) = Try(scala.io.StdIn.readLine(prompt).toInt)
        val t: Try[Int] = for (x <- readInt("X"); y <- readInt("Y")) yield x + y

    }

    // callbacks
    def callbacks = {
        // onComplete callback, avoid blocking
        def doStuff(x: Try[Int]): Unit = x match {
            case Success(res) => ???
            case Failure(ex) => ???
        }
        Future { 42 }.onComplete(doStuff)
        // onSuccess, onFailure: deprecated

        // callback hell: computations composition on callbacks
        // compose futures: much better
    }

    // composing future tasks
    def composingFutureTasks = {
        // n.b. async/await: https://github.com/scala/scala-async
        // uses scala macros

        // e.g. combine results from two web services
        // using callbacks // don't do that!
        def getData(str: String): Int = { Thread.sleep(Random.nextInt(1000)); Random.nextInt }
        val f1 = Future { getData("srv1") }
        val f2 = Future { getData("srv2") }
        f1 onComplete {
            case Success(n1) => f2 onComplete { // attach second callback after getting first result
                case Success(n2) => println(s"result: ${n1 + n2}")
                case Failure(ex) => ???
            }
            case Failure(ex) => ???
        }

        // think of a future as a collection with (hopefully, eventually) one element
        val combined = f1.map(n1 => n1 + getData("srv2")) // not concurrently
        val combined2: Future[Future[Int]] = f1.map(n1 => f2.map(n2 => n1 + n2)) // not good
        val combined3: Future[Int] = f1.flatMap(n1 => f2.map(n2 => n1 + n2)) // good enough

        // for-expr
        val combined4: Future[Int] = for (n1 <- f1; n2 <- f2) yield n1 + n2
        // with guard: NoSuchElement if guard fails
        val combined5: Future[Int] = for (n1 <- f1; n2 <- f2 if n1 != n2) yield n1 + n2
        // if one of the tasks fails, entire pipeline fails and exception is captured

        // a future starts when it is created; to delay the creation use functions
        def fut1 = Future { getData("srv1") } // or val
        def fut2(a: Int) = Future { getData(s"srv$a") } // def, no val!
        // evaluate fut2 only after fut1 is completed
        for (x <- fut1; y <- fut2(x)) yield x + y

    }

    // other future transformations
    def otherFutureTransformations = {
        // map/flatMap are the most fundamental

        // https://www.scala-lang.org/api/current/scala/concurrent/Future.html
        // collect
        // foreach // side-effects, convenient for harvesting
        // andThen
        // filter

        // recover // turn exception into a successful result
        // recoverWith
        // fallbackTo // cannot inspect the reason for the failure

        // failed // turns failed Future into a successful Future[Throwable]
        // transform
        // transformWith
        // zip, zipWith // result is a pair or exception

        def getData(str: String): Int = { Thread.sleep(Random.nextInt(1000)); Random.nextInt(42 + str.length) }

        // examples
        val f1 = Future { getData("srv1") } recover { case e: java.sql.SQLException => getData("srv3") }
        val f2 = Future { getData("srv2") } fallbackTo f1

        (for (n1 <- f1; n2 <- f2) yield n1 + n2).foreach(x => println(s"result: $x"))
        for (ex <- f1.failed) println(s"error: ${ex.getMessage}")

        val pair: Future[(Int, Int)] = f1.zip(f2)
        val fres: Future[Int] = f1.zipWith(f2)(_ + _)

    }

    // methods in the Future Object
    def methodsInTheFutureObject = {
        // Future companion object : useful methods for working with collections of futures
        // https://www.scala-lang.org/api/current/scala/concurrent/Future$.html

        val parts: List[Int] = (1 to 10).toList
        def doStuff(x: Int): Future[Int] = Future { Thread.sleep(Random.nextInt(x)); Random.nextInt(x) }
        // collections of futures
        val futures: List[Future[Int]] = parts.map(doStuff)

        // sequence: collection of results
        val results: Future[Seq[Int]] = Future.sequence(futures)
        // if any of the futures fail, pipeline fail

        // traverse combines map and sequence
        val results2: Future[Seq[Int]] = Future.traverse(parts)(doStuff)

        // reduce, fold
        val fsum: Future[Int] = Future.reduceLeft(futures)(_ + _)

        // firstCompletedOf: result from any part, first to compute
        val firstRes: Future[Int] = Future.firstCompletedOf(futures)

        // find: first to satisfy a predicate
        val foAnswer: Future[Option[Int]] = Future.find(futures)(_ == 42)

        // n.b. while 'find' and 'firstCompletedOf' is done, other threads may still be working;
        // no means to stop running future in scala, provide your own

        // generate simple future:
        // successful(r)
        // failed(ex)
        // fromTry(t)
        // unit
        // never // never completes
    }

    // promises
    def promises = {
        // future is read-only, value is set implicitly when task finished;
        // promise allows to set future value once;
        // check java 8 CompletableFuture

        def workHard(str: String): Int = ???

        // future style
        def f_computeAnswer(a: String): Future[Int] = Future {
            workHard(a)
        }

        // promise style
        def p_computeAnswer(a: String): Future[Int] = {
            // uses two different futures for result
            val p = Promise[Int]() //; val p2 = Promise[String]()
            Future { // working future
                p.success(workHard(a)) // complete promise future
                // can do other stuff here, e.g. fulfill another promise
                // p2.success(???)
            }
            p.future // promise future, unrelated to working future
        }
        // no difference for consumer;
        // producer has more flexibility

        // it is possible to have multiple tasks to fulfill a single promise
        def whoFirst(a: String): Future[Int] = {
            val p = Promise[Int]()
            Future { p.trySuccess(workHard(a)) } // might want to call p.isCompleted periodically
            Future { p.trySuccess(42) }
            p.future
        }

    }

    // execution contexts
    def executionContexts = {
        // default global fork-join pool, good enough for cpu-bound tasks;
        // not good for many i/o bound tasks, these could do a lot of blocking;

        // you can notify the execution context about a block
        def pullStuffFromNetwork(i: Int): Int = ???
        val f: Future[Int] = Future {
            val n = Random.nextInt(42)
            blocking { pullStuffFromNetwork(n) }
        }
        // exec.context may then increase the number of threads

        // better off using a different thread pool

        // cached pool works well for i/o workloads
        val pool = Executors.newCachedThreadPool() // see more in Executors class
        // pass as implicit to Future
        implicit val ec = ExecutionContext.fromExecutor(pool)
        // or explicitly
        val iof: Future[Int] = Future.apply(pullStuffFromNetwork(42))(ec)
    }

}

object Futures_Exercises {
    import java.time._
    import scala.util._
    import scala.concurrent._
    import scala.concurrent.duration._
    import java.util.concurrent.Executors

    // 1. Consider the expression
    //  for (n1 <- Future { Thread.sleep(1000) ; 2 }
    //      n2 <- Future { Thread.sleep(1000); 40 })
    //  println(n1 + n2)
    // How is the expression translated to map and flatMap calls?
    // Are the two futures executed concurrently or one after the other?
    // In which thread does the call to println occur?
    def ex1 = {
        import scala.concurrent.ExecutionContext.Implicits.global
        def tid(n: String): Unit = println(s"$n thread: ${Thread.currentThread.getId}")
        tid("main")

        Future { Thread.sleep(1000); tid("first"); 2 }.flatMap(x =>
            Future { Thread.sleep(1000); tid("second"); 40 }.map(y =>
            { tid("res"); println(x + y) }))

        // one after another;
        // second future thread;
    }

    // 2. Write a function doInOrder that, given two functions
    // f: T => Future[U]
    // and
    // g: U => Future[V]
    // produces a function
    // T => Future[U]
    // that, for a given t, eventually yields g(f(t))
    def ex2 = {
        import scala.concurrent.ExecutionContext.Implicits.global
        def doInOrder[T, U, V](f: T => Future[U], g: U => Future[V])(t: T): Future[V] = {
            for (a <- f(t); b <- g(a)) yield b
        }

        // test
        val f = (x: Int) => Future { Thread.sleep(100); x + 42 }
        val g = (x: Int) => Future { Thread.sleep(10); x * 2 }
        val test: Int => Future[Int] = doInOrder(f, g)
        assert(Await.result(test(1), 2.seconds) == 86)
    }

    // 3. Repeat the preceding exercise for any sequence of functions of type T => Future[T].
    def ex3 = {
        import scala.concurrent.ExecutionContext.Implicits.global
        def doInOrder[T](funcs: (T => Future[T])*)(t: T): Future[T] = {
            funcs.foldLeft(Future(t)){ case (ft, func) => ft.flatMap(func) }
        }

        // test
        val f = (x: Int) => Future { Thread.sleep(100); x * 42 }
        val g = (x: Int) => Future { Thread.sleep(10); x + 2 }
        val test: Int => Future[Int] = doInOrder(f, g)
        assert(Await.result(test(1), 2.seconds) == 44)
    }

    // 4. Write a function doTogether that, given two functions
    // f: T => Future[U]
    // and
    // g: U => Future[V]
    // produces a function
    // T => Future[(U, V)]
    // running the two computations in parallel and, for a given t, eventually yielding
    // (f(t), g(t)).
    def ex4 = {
        import scala.concurrent.ExecutionContext.Implicits.global
        def doTogether[T, U, V](f: T => Future[U], g: T => Future[V])(t: T): Future[(U, V)] = {
            f(t).zip(g(t))
        }

        // test
        val f = (x: Int) => Future { Thread.sleep(100); x * 42 }
        val g = (x: Int) => Future { Thread.sleep(10); x + 2 }
        val test = doTogether(f, g)(_)
        assert(Await.result(test(1), 2.seconds) == (42, 3))
    }

    // 5. Write a function that receives a sequence of futures and
    // returns a future that eventually yields a sequence of all results
    def ex5 = {
        import scala.concurrent.ExecutionContext.Implicits.global
        def seq[T](fs: Future[T]*): Future[Seq[T]] = {
            Future.sequence(fs)
        }

        // test
        val res = seq(Future { 42 }, Future { 3 })
        assert(Await.result(res, 1.second) == Seq(42, 3))
    }

    // 6. Write a method
    //  Future[T] repeat(action: => T, until: T => Boolean)
    // that asynchronously repeats the action until it produces a value that is accepted
    // by the 'until' predicate, which should also run asynchronously.
    // Test with a function that reads a password from the console, and
    // a function that simulates a validity check by sleeping for a second and then checking that
    // the password is "secret". Hint: Use recursion.
    def ex6 = {
        import scala.concurrent.ExecutionContext.Implicits.global
        def repeat[T](action: => T, until: T => Boolean): Unit = {
            val res = for {
                x <- Future(action)
                y <- Future { until(x) }
            } yield y

            // if (Await.result(res, 30.seconds)) ()
            // else repeat(action, until)
            res.foreach(y => if (!y) repeat(action, until) else ())
        }

        def repeat2[T](action: => T, until: T => Boolean, maxiter: Int = 10): Future[Boolean] = {
            @tailrec def loop(i: Int): Boolean = {
                until(action) || {
                    if (i > 0) loop(i - 1) else false
                }
            }

            Future { loop(maxiter) }
        }

        // test
        def action: String = { scala.io.StdIn.readLine("\nenter password: ") }
        def until(p: String): Boolean = { Thread.sleep(500); p == "secret" }
        //repeat(action, until)
        Await.result(repeat2(action, until), 60.seconds)
        println("leave main thread")
    }

    // 7. Write a program that counts the prime numbers between 1 and n,
    // as reported by BigInt.isProbablePrime
    // Divide the interval into 'p' parts, where p is the number of available processors.
    // Count the primes in each part in concurrent futures and combine the results.
    def ex7(upto: Int = 1000000, rounds: Int = 100) = {
        import scala.concurrent.ExecutionContext.Implicits.global
        def isPrime(x: Int): Boolean = BigInt(x).isProbablePrime(rounds*2)
        def probablePrimes_seq(from: Int, to: Int): Int = { (from to to).count(isPrime) }
        def probablePrimes_par(maxn: Int): Int = { (1 to maxn).par.count(isPrime) }

        def probablePrimes(maxn: Int): Int = {
            val numCores = Runtime.getRuntime.availableProcessors
            println(s"#cores: $numCores")
            val batchSize: Int = maxn / numCores

            def makeBatch(batchNum: Int): (Int, Int) = { // 1,2; 3,4; 5,6; 7,8
                val hi = batchSize * batchNum
                val lo = 1 + hi - batchSize
                (lo, hi)
            }

            def lastBatch: (Int, Int) = { (1 + maxn - (maxn % numCores), maxn) }

            val batches = (1 to numCores).map(makeBatch) :+ lastBatch
            val res = Future.traverse(batches)(b => Future { probablePrimes_seq(b._1, b._2) })
            Await.result(res.map(_.sum), 59.seconds)
        }

        // test
        assert(probablePrimes(100) == 25); println("25 / 100 OK")
        assert(probablePrimes(1000) == 168); println("168 / 1000 OK")
        assert(probablePrimes(100000) == 9592); println("9592 / 100 000 OK")

        probablePrimes(upto)
    }

    // 8. Write a program that asks the user for a URL,
    // reads the web page at that URL,
    // and displays all the hyperlinks.
    // Use a separate Future for each of these three steps.
    def ex8 = {
        import scala.concurrent.ExecutionContext.Implicits.global
        import scala.xml._

        def getUrl(prompt: String = "enter an URL:",
                   default: String = "http://horstmann.com/unblog/index.html"
                  ): String = Option(scala.io.StdIn.readLine(prompt)) match {
            case None | Some("") => default
            case Some(s) => s
        }

        def loadXml(url: String): Document = {
            val parser = new scala.xml.parsing.XhtmlParser(scala.io.Source.fromURL(url))
            parser.initialize.document
        }

        def displayLinks(doc: Document): Unit = {
            doc.docElem \\ "a" foreach { a => println(s"${a.toString}") }
        }

        val res = for {
            u <- Future(getUrl())
            d <- Future(loadXml(u))
            p <- Future(displayLinks(d))
        } yield Seq(u, d, p)

        Await.result(res, 42.seconds)
    }

    // 9. Write a program that asks the user for a URL,
    // reads the web page at that URL,
    // finds all the hyperlinks,
    // visits each of them concurrently, and locates the Server HTTP header for each of them.
    // Finally, print a table of which servers were found how often.
    // The futures that visit each page should return the header.
    def ex9 = {
        import scala.xml._
        import scala.collection.JavaConverters._

        val pool = Executors.newCachedThreadPool()
        implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(pool)

        def getUrl(prompt: String = "enter an URL:",
                   default: String = "http://horstmann.com/unblog/index.html"
                  ): String = Option(scala.io.StdIn.readLine(prompt)) match {
            case None | Some("") => default
            case Some(s) => s
        }

        def loadXml(url: String): Document = {
            println(s"loading doc: $url")
            val parser = new scala.xml.parsing.XhtmlParser(scala.io.Source.fromURL(url))
            parser.initialize.document
        }

        def getLinks(doc: Document): Iterable[String] = {
            println(s"collecting links: ${doc.toString.take(80)}")
            (doc.docElem \\ "a" \\ "@href").map(_.text)
        }

        // TODO: add timeout parameter
        //  https://alvinalexander.com/misc/scala-scalaj-http-client-response-headers-head-request
        def getHeaders(url: String): Map[String, String] = Try {
            println(s"getting headers: $url")
            val res = new java.net.URL(url).openConnection.getHeaderFields.asScala.toMap
            res.map { case (k,v) =>
                // println(s"${k}: ${v.asScala.head}")
                (k, v.asScala.mkString("\n"))
            }
        }.getOrElse(Map.empty)

        val flinks = for {
            url <- Future(getUrl())
            doc <- Future(loadXml(url))
            links <- Future(getLinks(doc))
        } yield links

        val headers = flinks.flatMap(links => Future.traverse(links)(link => Future(getHeaders(link))))

        val res = headers.map(ms => ms.map(m => m.getOrElse("Server", "undefined"))
            .groupBy(s => s).map { case (k,v) => (k, v.size) }.toList
            .sortBy(_._2)(scala.math.Ordering.fromLessThan(_ >= _)))

        val scoreboard = Await.result(res, 3.minutes)
        println("\nservers counts:\n")
        scoreboard foreach { case (s, n) => println(s"${s.padTo(30, ' ').take(30)}: $n")}
    }

    // 10. Change the preceding exercise where the futures that visit each header update
    // a shared Java ConcurrentHashMap or Scala TrieMap.
    // This isn’t as easy as it sounds. A threadsafe data structure is safe in the sense
    // that you cannot corrupt its implementation, but you have to make sure that sequences
    // of reads and updates are atomic.
    def ex10 = {
        import scala.xml._
        import scala.collection.JavaConverters._
        import scala.collection.concurrent

        val pool = Executors.newCachedThreadPool()
        implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(pool)
        implicit def longToInt(n: Long): Int = n.toInt

        object scoreboard extends Iterable[(String, Int)] {
            private val board = concurrent.TrieMap.empty[String, Int]
            def add(server: String): Unit = this.synchronized {
                board.update(server, 1 + board.getOrElse(server, 0))
            }

            override def iterator: Iterator[(String, Int)] = board.readOnlySnapshot.iterator
        }

        def getUrl(prompt: String = "enter an URL:",
                   default: String = "http://horstmann.com/unblog/index.html"
                  ): String = Option(scala.io.StdIn.readLine(prompt)) match {
            case None | Some("") => default
            case Some(s) => s
        }

        def loadXml(url: String): Document = {
            println(s"loading doc: $url")
            val parser = new scala.xml.parsing.XhtmlParser(scala.io.Source.fromURL(url))
            parser.initialize.document
        }

        def getLinks(doc: Document): Iterable[String] = {
            println(s"collecting links: ${doc.toString.take(80)}")
            (doc.docElem \\ "a" \\ "@href").map(_.text)
        }

        def getHeader(url: String): String = {
            println(s"getting headers: $url")
            val headers = Try {
                val conn = new java.net.URL(url).openConnection
                conn.setConnectTimeout(10.seconds.toMillis); conn.setReadTimeout(10.seconds.toMillis)
                val res = conn.getHeaderFields.asScala.toMap
                res.map { case (k,v) => (k, v.asScala.mkString("\n")) }
            }.getOrElse(Map.empty)

            val res = headers.getOrElse("Server", "undefined")
            scoreboard.add(res)
            res
        }

        val flinks = for {
            url <- Future(getUrl())
            doc <- Future(loadXml(url))
            links <- Future(getLinks(doc))
        } yield links

        val headers = flinks.flatMap(links => Future.traverse(links)(link => Future(getHeader(link))))

        Await.result(headers, 1.minute)
        println("\nservers counts:\n")
        scoreboard.toList.sortBy(_._2)(scala.math.Ordering.fromLessThan(_ >= _))
            .foreach { case (s, n) => println(s"${s.padTo(30, ' ').take(30)}: $n")}
    }

    // 11. Using futures, run four tasks that each sleep for ten seconds and then
    // print the current time.
    // If you have a reasonably modern computer, it is very likely that it reports four available
    // processors to the JVM, and the futures should all complete at around the same time.
    //
    // Now repeat with forty tasks.
    // What happens? Why?
    // Replace the execution context with a cached thread pool.
    // What happens now?
    // (Be careful to define the futures after replacing the implicit execution context.)
    def ex11 = {
        import java.time._

        implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
        //import scala.concurrent.ExecutionContext.Implicits.global

        def task(n: Int): Int = {
            Thread.sleep(10.seconds.toMillis)
            println(s"task $n, time ${LocalTime.now}")
            n
        }

        def run(n: Int): Future[Seq[Int]] = {
            Future.traverse((1 to n).toList)(n => Future(task(n)))
        }

        println(Await.result(run(4), 1.minute).mkString(","))
        println(Await.result(run(40), 1.minute).mkString(","))

        // pool: scala.concurrent.ExecutionContext.Implicits.global
        // first 8 tasks executes concurrently, other wait for free resources in pool.
        //task 3, time 13:43:11.527
        //task 4, time 13:43:11.527
        //task 1, time 13:43:11.527
        //task 2, time 13:43:11.527
        //1,2,3,4
        //task 1, time 13:43:21.537
        //task 2, time 13:43:21.537
        //task 3, time 13:43:21.538
        //task 4, time 13:43:21.538
        //task 5, time 13:43:21.538
        //task 6, time 13:43:21.538
        //task 7, time 13:43:21.538
        //task 8, time 13:43:21.539
        //task 9, time 13:43:31.538
        //task 11, time 13:43:31.538
        //task 10, time 13:43:31.538
        //task 12, time 13:43:31.538
        //task 13, time 13:43:31.538
        //task 14, time 13:43:31.538
        //task 15, time 13:43:31.538
        //task 16, time 13:43:31.539
        //task 19, time 13:43:41.538
        //...

        // pool: implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
        // enough threads for all tasks, all executes concurrently.
        //task 1, time 13:48:03.896
        //task 2, time 13:48:03.896
        //task 3, time 13:48:03.896
        //task 4, time 13:48:03.896
        //1,2,3,4
        //task 1, time 13:48:13.898
        //task 2, time 13:48:13.898
        //task 3, time 13:48:13.898
        //task 4, time 13:48:13.898
        //task 5, time 13:48:13.898
        //task 6, time 13:48:13.898
        //task 7, time 13:48:13.899
        //task 8, time 13:48:13.899
        //task 9, time 13:48:13.899
        //task 10, time 13:48:13.900
        //task 11, time 13:48:13.900
        //task 12, time 13:48:13.900
        //task 13, time 13:48:13.900
        //task 14, time 13:48:13.901
        //task 15, time 13:48:13.901
        //task 16, time 13:48:13.901
        //task 17, time 13:48:13.901
        //task 18, time 13:48:13.902
        //task 19, time 13:48:13.902
        //task 20, time 13:48:13.902
        //task 21, time 13:48:13.902
        //task 22, time 13:48:13.902
        //task 23, time 13:48:13.903
        // ...
    }

    // 12. Write a method that, given a URL, locates all hyperlinks,
    // makes a promise for each of them,
    // starts a task in which it will eventually fulfill all promises, and
    // returns a sequence of futures for the promises.
    // Why would it not be a good idea to return a sequence of promises?
    def ex12 = {
        // Why would it not be a good idea to return a sequence of promises?
        // promises are writable and you don't want to expose that to client.

        import scala.xml._

        implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

        def loadXml(url: String): Document = {
            println(s"loading doc: $url")
            val parser = new scala.xml.parsing.XhtmlParser(scala.io.Source.fromURL(url))
            parser.initialize.document
        }

        def getLinks(doc: Document): Iterable[String] = {
            println(s"collecting links: ${doc.toString.take(80)}")
            (doc.docElem \\ "a" \\ "@href").map(_.text)
        }

        def linkProcess(url: String): String = {
            Thread.sleep(100)
            println(url)
            url
        }

        def run(url: String = "http://horstmann.com/unblog/index.html"): Seq[Future[String]] = {
            val links = getLinks(loadXml(url)).toSet // to make a map we need unique keys
            val promises = links.map(h => (h, Promise[String]())).toMap
            println("start concurrent task")

            Future {
                links.foreach(h =>
                    promises(h).complete(Try(linkProcess(h))) )

                println("closing promises")
                Thread.sleep(100)
                promises.values.foreach(p => {
                    if (!p.isCompleted) p.failure(sys.error("lost a link somewhere"))
                })
                println("all promises closed")
            }

            println("return futures")
            promises.values.map(_.future).toList
        }

        Await.result(Future.sequence(run()), 42.seconds)
    }

    // 13. Use a promise for implementing cancellation.
    //
    // Given a range of big integers, split the range into subranges that
    // you concurrently search for palindromic primes.
    // When such a prime is found, set it as the value of the future.
    // All tasks should periodically check whether the promise is completed,
    // in which case they should terminate.
    def ex13(from: BigInt = 1, to: BigInt = 935, wait: Int = 600, rounds: Int = 100) = {
        implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
        // import scala.concurrent.ExecutionContext.Implicits.global

        def isPrime(x: BigInt): Boolean = x.isProbablePrime(rounds*2)

        def isPalindromicPrime(n: BigInt): Boolean = isPrime(n) && {
            val s = n.toString
            s == s.reverse
        }

        def searchPalindromicPrimes(from: BigInt, to: BigInt, p: Promise[Iterable[BigInt]]): Iterable[BigInt] = {
            @tailrec def loop(next: BigInt, acc: Seq[BigInt]): Seq[BigInt] = {
                Thread.sleep(Random.nextInt(10)) // slow down a little
                if (next > to || p.isCompleted) {
                    if (p.isCompleted) println(s"batch ${(from, to)}, thread: ${Thread.currentThread.getId}, cancelled")
                    acc
                }
                else {
                    if (isPalindromicPrime(next)) loop(next + 1, acc :+ next)
                    else loop(next + 1, acc)
                }
            }

            println(s"batch ${(from, to)}, thread: ${Thread.currentThread.getId}, searching ...")
            val res = loop(from, Seq.empty[BigInt])
            p.trySuccess(res)
            println(s"batch ${(from, to)}, thread: ${Thread.currentThread.getId}, done")
            res
        }

        def makeBatches(minn: BigInt, maxn: BigInt, numBatches: Int): Iterable[(BigInt, BigInt)] = {
            val batchSize: Int = ((1 + maxn - minn) / BigInt(numBatches)).toInt

            def makeBatch(batchNum: Int): (BigInt, BigInt) = {
                val hi = batchSize * batchNum + minn - 1
                val lo = 1 + hi - batchSize
                (lo, hi)
            }

            def lastBatch: (BigInt, BigInt) = {
                val res = makeBatch(numBatches + 1)
                (res._1, maxn)
            }

            (1 to numBatches).map(makeBatch) :+ lastBatch
        }

        def palindromicPrimes(minn: BigInt, maxn: BigInt) = {
            require(maxn > minn, "iteration goes from min to max")
            val batches = makeBatches(minn, maxn, Runtime.getRuntime.availableProcessors)
            println(s"batches: \n${batches.mkString("\n")}")

            val promises = batches.map{ case (from, to) => {
                println(s"start batch ${(from, to)}")
                val promise = Promise[Iterable[BigInt]]()
                Future {
                    Thread.sleep(100) // start searching after init. all batches, debug print is nicer this way
                    searchPalindromicPrimes(from, to, promise)
                }
                promise
            }}

            println(s"return promises: ${promises}")
            promises
        }

        val promises = palindromicPrimes(from, to)

        // simulate cancellation
        Thread.sleep(wait)
        //promises.foreach(p => p.tryFailure(sys.error("can't wait any longer")))
        //promises.foreach(p => p.tryComplete(Try(Seq.empty)))
        promises.foreach(p => p.tryComplete(Try(sys.error("cancelled"))))

        // show result
        println(s"completed promises: ${promises}\n")
        val futures = promises.map(_.future)
        val lst = futures.map(f => Await.ready(f, 59.seconds)) // Await.result will throw a future 'cancelled' exception
        lst.foreach(f => println(f))
        lst
    }

}
