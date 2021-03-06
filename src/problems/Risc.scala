package TutorialProblems

import Chisel._
import Node._
import scala.collection.mutable.HashMap

class Risc extends Module {
  val io = new Bundle {
    val isWr   = Bool(INPUT)
    val wrAddr = UInt(INPUT, 8)
    val wrData = Bits(INPUT, 32)
    val boot   = Bool(INPUT)
    val valid  = Bool(OUTPUT)
    val out    = Bits(OUTPUT, 32)
  }
  val file = Mem(256, Bits(width = 32))
  val code = Mem(256, Bits(width = 32))
  val pc   = RegReset(UInt(0, 8))
  
  val add_op :: imm_op :: Nil = Enum(2){ Bits() }

  val inst = code(pc)
  val op   = inst(31,24)
  val rci  = inst(23,16)
  val rai  = inst(15, 8)
  val rbi  = inst( 7, 0)

  val ra = Mux(rai === Bits(0), Bits(0), file(rai))
  val rb = Mux(rbi === Bits(0), Bits(0), file(rbi))
  val rc = Bits(width = 32)

  io.valid := Bool(false)
  io.out   := Bits(0)
  rc       := Bits(0)

  when (io.isWr) {
    code(io.wrAddr) := io.wrData
  } .elsewhen (io.boot) {
    pc := UInt(0)
  } .otherwise {
    switch(op) {
      is(add_op) { rc := ra + rb }
      is(imm_op) { rc := (rai << UInt(8)) | rbi }
    }
    io.out := rc
    when (rci === UInt(255)) {
      io.valid := Bool(true)
    } .otherwise {
      file(rci) := rc
    }
    pc := pc + UInt(1)
  }
}

class RiscTests(c: Risc) extends Tester(c, Array(c.io, c.pc)) {  
  defTests {
    var allGood = true
    val svars = new HashMap[Node, Node]()
    val ovars = new HashMap[Node, Node]()
    def wr(addr: UInt, data: UInt)  = {
      svars.clear()
      svars(c.io.isWr)   = Bool(true)
      svars(c.io.wrAddr) = addr
      svars(c.io.wrData) = data
      step(svars, ovars)
    }
    def boot()  = {
      svars.clear()
      svars(c.io.boot)   = Bool(true)
      step(svars, ovars)
    }
    def tick()  = {
      svars.clear()
      svars(c.io.boot)   = Bool(false)
      step(svars, ovars)
    }
    def I (op: UInt, rc: Int, ra: Int, rb: Int) = 
      Cat(op, UInt(rc, 8), UInt(ra, 8), UInt(rb, 8))
    val app  = Array(I(c.imm_op,   1, 0, 1), // r1 <- 1
                     I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
                     I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
                     I(c.add_op, 255, 1, 0)) // rh <- r1
    wr(UInt(0), Bits(0)) // skip reset
    for (addr <- 0 until app.length) 
      wr(UInt(addr), app(addr))
    boot()
    do {
      tick()
    } while (ovars(c.io.valid).litValue() == 0)
    allGood = ovars(c.io.out).litValue() == 4 && allGood
    allGood
  }
}
