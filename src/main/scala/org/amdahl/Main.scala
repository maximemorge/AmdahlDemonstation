// Copyright (C) Dialectics 2016
package org.amdahl

//Required for actors
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

//Required for thread pool
import java.util.concurrent.{ExecutorService, Executors}

//Required for future
import scala.concurrent.{Future,ExecutionContext}
import scala.util.{Try,Success, Failure}
import java.util.concurrent.ThreadPoolExecutor

import org.amdahl.actor.{Start, Stop, Supervisor}

/**
  * Main app to compute Pi
  * */
object Main {
  val debug=false

  val nbProcessors= Runtime.getRuntime().availableProcessors()// number of logical processors
  val system = ActorSystem("AmdhalDemonstration")
  var idSupervisor=1// give a different id to each spuervisor
  val TIMEOUTVALUE=50 seconds// default timeout of a run
  implicit val timeout = Timeout(TIMEOUTVALUE)

  val NBTASKS=1e8.toInt// number of shoots
  val MAXNUMWORKER=50// maximum number of workers
  val NBRUNS=20// for each number of workers , we consider NBRUNS runs

  /**
    * Run the Actor system with the following default dispatcher and print a CSV file nbwWorkers,speedup
    * @see https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ForkJoinPool.html
    * fork-join-executor {
    *        # Min number of threads to cap factor-based parallelism number to
    *        parallelism-min = 8
    *
    *        # The parallelism factor is used to determine thread pool size using the
    *       # following formula: ceil(available processors * factor). Resulting size
    *        # is then bounded by the parallelism-min and parallelism-max values.
    *        parallelism-factor = 3.0
    *
    *        # Max number of threads to cap factor-based parallelism number to
    *        parallelism-max = 64
    *
    *        # Setting to "FIFO" to use queue like peeking mode which "poll" or "LIFO" to use stack
    *        # like peeking mode which "pop".
    *        task-peeking-mode = "FIFO"
    *      }
    */
  def main(args: Array[String]): Unit = {
    //This UNIX command returns the number physical core:
    // sysctl -n machdep.cpu.core_count
    //This command returns the number of logical threads in the JVM
    println("Number of logical CPUs "+nbProcessors)
    val referenceTime = runExperiment(1)// the reference time with a single worker
    println("1,1.0")
    for (nbWorkers <- 2 to MAXNUMWORKER.toInt by 1){// for each number of workers
    val runningTime = runExperiment(nbWorkers)// run an experiment
    val speedup = referenceTime / runningTime// compute the speedup
      println(nbWorkers + "," + speedup)
    }
    System.exit(0)
  }


  /**
    * Execute an experiment, i.e. we consider NBRUNS runs for each number of workers
    * @param nbWorkers how many workers
    * @return the average running time in nanoseconds
    *
    */
  def runExperiment(nbWorkers: Int): Double = {
    val runningTimes: Seq[Double] = for (run <- 1 to NBRUNS) yield {
      // The place where the method is selected
      //this.runActor(nbWorkers)
      //this.runThread(nbWorkers)
      //this.runThreadPool(nbWorkers)
      this.runFuture(nbWorkers)
    }
    val runningTime = runningTimes.sum / runningTimes.length
    runningTime
  }

  /**
    * Execute a single run with Actors
    * @param nbWorkers how many workers
    * @return the running time in nanoseconds
    *
    */
  def runActor(nbWorkers: Int): Double ={
    // Launch a new supervisor
    val supervisor = system.actorOf(Props(classOf[Supervisor], nbWorkers, NBTASKS), name = "supervisor" + idSupervisor)
    idSupervisor+=1// increment idSupervisor to have supervisors with different names
    // The current thread is blocked and it waits for the supervisor to "complete" the Future with it's reply.
    val future = supervisor ? Start
    val result = Await.result(future, timeout.duration).asInstanceOf[Stop]
    if (debug) println("Runtime= "+result.time + " ")
    return result.time
  }


  /**
    * Execute a single run with Thread
    * @param nbWorkers how many workers
    * @return the running time in nanoseconds
    *
    */
  def runThread(nbWorkers: Int): Double ={
    val startingTime=System.nanoTime// start clock
    // Creation of Threads
    val workers = for(i <- 1 to nbWorkers) yield new org.amdahl.thread.Worker(i,NBTASKS/nbWorkers)
    workers.foreach( _.start())// Running Thread
    // Waiting for the thread
    try {
      workers.foreach( _.join())
    }catch{
      case e: InterruptedException => println("Intterruption while waiting for workers")
    }
    var pi=0.0
    for (w <- workers) {pi+=w.result}
    pi=pi/NBTASKS*4
    //it should be feasible in a single line like val pi=workers.sum(w => w.result)
    if (debug) println("Pi= "+pi)
    val elapsedTime = System.nanoTime - startingTime// stop the clock
    if (debug) println("Runtime= "+elapsedTime+ " ")
    return elapsedTime
  }

  /**
    * Execute a single run with Thread pool
    * TODO debug : it seems the speedup factor is always 1
    * @param nbWorkers
    * @return the running time in nanoseconds
    *
    */
  def runThreadPool(nbWorkers: Int): Double ={
    // Creation of nb logical CPUs polls of threads
    val pool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
    val startingTime=System.nanoTime// start clock
    // Creation of Threads
    val workers = for(i <- 1 to nbWorkers) yield new org.amdahl.thread.Worker(i,NBTASKS/nbWorkers)
    workers.foreach(_ => pool.execute(_)) // Running Thread
    pool.shutdown()
    // Waiting for the thread
    while (!pool.isTerminated) {}
    var pi=0.0
    for (w <- workers) {pi+=w.result}
    pi=pi/NBTASKS*4
    //it should be feasible in a single line like val pi=workers.sum(w => w.result)
    if (debug) println("Pi= "+pi)
    var elapsedTime = System.nanoTime - startingTime// stop the clock
    if (debug) print(startingTime + " ")
    elapsedTime
  }

  /**
    * Execute a single run with Future
    * @param nbWorkers how many workers
    * @return the running time in nanoseconds
    *
    */
  def runFuture(nbWorkers: Int): Double = {
    import scala.concurrent.ExecutionContext.Implicits.global
    //By default ForkJoinPool whose desired degree of parallelism is the number of CPUs
    //TODO change context execution
    //implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(MAXNUMWORKER))
    //Otherwise see http://blog.jessitron.com/2014/01/choosing-executorservice.html
    val nbLocalTasks = NBTASKS/nbWorkers// nb of operators per workers
    var pi=0.0
    val startingTime=System.nanoTime// start clock
    val futures = for (i <- 1 to nbWorkers) yield {
      val future = Future[Double]{
        val task = new org.amdahl.task.Task()
        task.cpuIntensive(nbLocalTasks)
      }
      future
    }
    val f = Future.sequence(futures.toList)// convert a List[Future[X]] to a Future[List[X]]
    val result: Try[Seq[Double]] = Await.ready(f, Duration.Inf).value.get
    val resultEither : Double= result match{//When all the computation is ni
      case Success(results) => {
        for (result <- results) pi+=result
        pi=pi/NBTASKS*4
        if (debug) println("Pi= "+pi)
        val elapsedTime = System.nanoTime - startingTime// stop the clock
        elapsedTime
      }
      case Failure(t) => {
        println("An error has occured: " + t.getMessage)
        -1.0
      }
    }
    return resultEither
  }

}

