import chisel3._
import chisel3.util._

import yuheng.debug.mem

class ifu extends Module {
  class ifu_in extends Bundle {
    val addr = UInt(32.W)
  }
  class ifu_intr extends Bundle {
    val addrrd = Bool()
  }
  val io = IO(new Bundle {
    val in   = Input(new ifu_in())
    val intr = Output(new ifu_intr())
    val inst_sram_addr = Output(UInt(32.W))
  })

  val tlb = Module(new tlb())
  tlb.io.in.addr := io.in.addr

  io.inst_sram_addr := tlb.io.out.addr

  io.intr.addrrd := tlb.io.out.addr(1, 0).orR
}
