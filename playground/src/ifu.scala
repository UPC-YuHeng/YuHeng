import chisel3._
import chisel3.util._

class ifu extends Module {
  class ram_io extends Bundle {
    val en    = Output(Bool())
    val wen   = Output(UInt(4.W))
    val addr  = Output(UInt(32.W))
    val wdata = Output(UInt(32.W))
    val rdata = Input(UInt(32.W))
  }
  class ifu_in extends Bundle {
    val addr = UInt(32.W)
  }
  class ifu_out extends Bundle {
    val inst = UInt(32.W)
  }
  class ifu_intr extends Bundle {
    val inst_addrrd = Bool()
  }
  val io = IO(new Bundle {
    val in   = Input (new ifu_in())
    val out  = Output(new ifu_out())
    val intr = Output(new ifu_intr())
    val ram  = new ram_io()
  })

  val tlb = Module(new tlb())
  tlb.io.in.addr := io.in.addr

  io.ram.en      := true.B
  io.ram.wen     := 0.U
  io.ram.addr    := tlb.io.out.addr
  io.ram.wdata   := 0.U
  io.out.inst    := io.ram.rdata

  io.intr.inst_addrrd := tlb.io.out.addr(1, 0).orR
}
