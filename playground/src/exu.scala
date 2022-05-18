import chisel3._
import chisel3.util._

class exu extends Module {
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
    val cmp     = Bool()
  }
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

  io.out.cmp := MuxLookup(Cat(io.in.signed.asUInt(), io.in.cmp_op), false.B, Array(
     2.U -> alu.io.out.zero.asBool(),
     3.U -> (~alu.io.out.zero).asBool(),
     4.U -> alu.io.out.signu.asBool(),
     5.U -> (alu.io.out.signu & (~alu.io.out.zero)).asBool(),
     6.U -> ((~alu.io.out.signu) | alu.io.out.zero).asBool(),
     7.U -> (~alu.io.out.signu).asBool(),
    10.U -> alu.io.out.zero.asBool(),
    11.U -> (~alu.io.out.zero).asBool(),
    12.U -> alu.io.out.signs.asBool(),
    13.U -> (alu.io.out.signs & (~alu.io.out.zero)).asBool(),
    14.U -> ((~alu.io.out.signs) | alu.io.out.zero).asBool(),
    15.U -> (~alu.io.out.signs).asBool(),
    //unsigned
		// 0 -> NOP, 1 -> Reserved, 2 -> "==", 3 -> "!="
		// 4 -> ">=", 5 -> ">", 6 -> "<=", 7 -> "<"
    //signed
    // 8 -> NOP, 9 -> Reserved, 10 -> "==", 11 -> "!="
		// 12 -> ">=", 13 -> ">", 14 -> "<=", 15 -> "<"
  ))
  io.out.dest := Mux(io.out.cmp, 1.U, alu.io.out.dest)
}