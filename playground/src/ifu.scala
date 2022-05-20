import chisel3._
import chisel3.util._

import yuheng.debug.mem

class ifu extends Module {
  class ifu_in extends Bundle {
    val addr = UInt(32.W)
  }
  class ifu_out extends Bundle {
    val inst = UInt(32.W)
  }
  class ifu_intr extends Bundle {
    val addrrd = Bool()
  }
  val io = IO(new Bundle {
    val in   = Input(new ifu_in())
    val out  = Output(new ifu_out())
    val intr = Output(new ifu_intr())
  })

  val tlb = Module(new tlb())
  tlb.io.in.addr := io.in.addr

  val imem = Module(new mem())
  imem.io.ren   := true.B
  imem.io.wen   := false.B
  imem.io.raddr := tlb.io.out.addr
  io.out.inst   := imem.io.rdata

  io.intr.addrrd := tlb.io.out.addr(1, 0).orR
}
