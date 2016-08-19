// Copyright (C) Dialectics 2016
package org.amdahl.thread

import org.amdahl.task.Task


/**
  * Worker which performs the local computation as a thread
  *
  * @param id of the worker for debugging
  * @param nbTasks to perform
  * */
class Worker(id: Int, nbTasks: Int) extends Thread{
  val debug= false

  //volatile synchronizes on every access
  @volatile var result : Double = _ //returned value by the worker

  override def run()={
    try{
      val task= new Task()
      result = task.cpuIntensive(nbTasks)
    }catch{
      case e: InterruptedException => println("Intterruption while running worker "+id)
    }
    if (debug) println("Worker "+id+" runs "+nbTasks+" operations")
  }

}
