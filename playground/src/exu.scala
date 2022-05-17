import chisel3._
import chisel3.util._

class exu_in extends Bundle {
  val alu_op = UInt(4.W)
  val cmp_op = UInt(3.W)
  val signed = Bool()
  val srca   = UInt(32.W)
  val srcb   = UInt(32.W)
}

class exu_out extends Bundle {
  val dest    = UInt(32.W)
  val dest_hi = UInt(32.W)
  val dest_lo = UInt(32.W)
}

class exu extends Module {
  val io = IO(new Bundle {
    val in  = Input(new exu_in())
    val out = Output(new exu_out())
  })

  val alu = Module(new alu())
  alu.io.in.alu_op := io.in.alu_op
  alu.io.in.srca   := io.in.srca
  alu.io.in.srcb   := io.in.srcb
  io.out.dest_hi   := alu.io.out.dest_hi
  io.out.dest_lo   := alu.io.out.dest_lo

  exu.io.out.dest := ListLookup(io.in.cmp_op, alu.io.out.dest, Array(
		// 0 -> NOP, 1 -> Reserved, 2 -> "==", 3 -> "!="
		// 4 -> ">=", 5 -> ">", 6 -> "<=", 7 -> "<"
  ))
}