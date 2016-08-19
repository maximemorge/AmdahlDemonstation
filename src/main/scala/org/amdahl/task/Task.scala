// Copyright (C) Dialectics 2016
package org.amdahl.task

// A random number generator isolated to the current thread.
import java.util.concurrent.ThreadLocalRandom

/**
  * Task to be computed intensively
  */
class Task {

  /**
    * Method to run computation
    */
  def cpuIntensive(intensity: Int) : Double = computeRandomPi(intensity)

  /**
    * Compute randomly the value of PI.
    * @param nbOperations i.e. how many points in the square
    * @return results, i.e number of points in the circel
    * Each operation consists of:
    *  1. inscribing a circle in a square
    *  2. generating randomly points in the square
    *  3. check if the point in the square is also in the circle
    *  Please note that:
    *  - PI ~ 4* nbShootsIn/nbShoots.
    *  - The more points generated, the better the approximation is.
    */
  def computeRandomPi(nbOperations: Int) : Double = {
    var countInCircle=0
    for (i <- 0 until nbOperations) {
      //Randomly generate points in the square (-1,-1) - (1,1)
      val r = ThreadLocalRandom.current()
      val x = 2 * r.nextDouble() - 1
      val y = 2 * r.nextDouble() - 1
      val radius = math.sqrt(math.pow(x, 2) + math.pow(y, 2))
      if (radius <= 1) countInCircle+=1
    }
    countInCircle
  }


}
