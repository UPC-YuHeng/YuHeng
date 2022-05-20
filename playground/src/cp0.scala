import chisel3._
import chisel3.util._

import CP0RegisterList._

class cp0 extends Module {
  class cp0_in extends Bundle {
    val write = Bool()
    val addr  = UInt(5.W)
    val sel   = UInt(3.W)
    val data  = UInt(32.W)
  }
  class cp0_out extends Bundle {
    val data = UInt(32.W)
    val epc  = UInt(32.W)
  }
  class cp0_intr extends Bundle {
    val eret = Bool()
  }
  val io = IO(new Bundle {
    val in   = Input(new cp0_in())
    val out  = Output(new cp0_out())
    val intr = Input(new cp0_intr())
  })

  val cp0 = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  when (reset.asBool()) {
    cp0(status) := Cat(0x0040.U(16.W), cp0(status)(15, 8), 0.U(8.W))
    cp0(cause)  := 0.U
  }

  when (io.in.write) {
    cp0(io.in.addr) := io.in.data
  }

  when (io.intr.eret) {
    cp0(status) := Cat(cp0(status)(31, 2), 0.U(1.W), cp0(status)(0))
  }

  io.out.data := cp0(io.in.addr)
  io.out.epc  := cp0(epc)
}