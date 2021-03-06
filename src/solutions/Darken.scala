package TutorialSolutions

import Chisel._
import Node._
import scala.collection.mutable.HashMap

class Darken extends Module {
  val io = new Bundle {
    val in  = Bits(INPUT, 8)
    val out = Bits(OUTPUT, 8)
  }

  io.out := io.in << UInt(1)
}

class DarkenTests(c: Darken, val infilename: String, val outfilename: String) extends Tester(c, Array(c.io)) {  
  defTests {
    val svars = new HashMap[Node, Node]()
    val ovars = new HashMap[Node, Node]()

    val inPic  = Image(infilename)
    val outPic = Image(inPic.w, inPic.h, inPic.d)
    step(svars, ovars, false)
    for (i <- 0 until inPic.data.length) {
      val rin = inPic.data(i)
      val  in = if (rin < 0) 256 + rin else rin
      svars(c.io.in) = Bits(in)
      step(svars, ovars, false)
      val out = ovars(c.io.out).litValue()
      outPic.data(i) = out.toByte
    }
    outPic.write(outfilename)
    true
  }
}
