import chisel3._
import chisel3.util._

class ifu_idu extends Module {
  class data extends Bundle {
  }
  class intr extends Bundle {
    val inst_addrrd = Bool()
  }
  class delay extends Bundle {
    val valid = Bool()
    val pc    = UInt(32.W)
    val intr  = new intr()
  }
  val io = IO(new Bundle {
    val pause = Input (Bool())
    val intr  = Input (Bool())
    val in    = Input (new delay())
    val out   = Output(new delay())
  })

  val ready = ~io.pause | io.intr

  // io.out := RegEnable(Mux(io.in.valid | io.intr, io.in, Reg(new delay())), Reg(new delay()), ready)
  io.out := RegEnable(io.in, Reg(new delay()), ~io.pause)
}
