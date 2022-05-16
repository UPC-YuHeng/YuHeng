import chisel3._
import chisel3.util._

class exu_in extends Bundle {
  val aluop = UInt(4.U)
  val srca  = UInt(32.W)
  val srcb  = UInt(32.W)
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
  alu.io.in.aluop    := exu.io.in.aluop
  alu.io.in.srca     := exu.io.in.srca
  alu.io.in.srcb     := exu.io.in.srcb
  exu.io.out.dest    := alu.io.out.dest
  exu.io.out.dest_hi := alu.io.out.dest_hi
  exu.io.out.dest_lo := alu.io.out.dest_lo
}