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
		// 0 -> NOP, 1 -> Reserved, 2 -> "==", 3 -> "!="
		// 4 -> ">=", 5 -> ">", 6 -> "<=", 7 -> "<"
    0x2.U -> alu.io.out.zero.asBool(),
    0x3.U -> (~alu.io.out.zero).asBool(),
    0x4.U -> alu.io.out.signu.asBool(),
    0x5.U -> (alu.io.out.signu & (~alu.io.out.zero)).asBool(),
    0x6.U -> ((~alu.io.out.signu) | alu.io.out.zero).asBool(),
    0x7.U -> (~alu.io.out.signu).asBool(),
    0xa.U -> alu.io.out.zero.asBool(),
    0xb.U -> (~alu.io.out.zero).asBool(),
    0xc.U -> alu.io.out.signs.asBool(),
    0xd.U -> (alu.io.out.signs & (~alu.io.out.zero)).asBool(),
    0xe.U -> ((~alu.io.out.signs) | alu.io.out.zero).asBool(),
    0xf.U -> (~alu.io.out.signs).asBool(),
  ))
  io.out.dest := Mux(io.in.cmp_op === 0.U, alu.io.out.dest, Mux(io.out.cmp, 1.U, 0.U))
}