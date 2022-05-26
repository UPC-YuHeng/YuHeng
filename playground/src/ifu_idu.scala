import chisel3._
import chisel3.util._

class ifu_idu extends Module {
  class ifu_data extends Bundle {
    val inst    = UInt(32.W)
    val pc      = UInt(32.W)
  }
  
  val io = IO(new Bundle {
    val valid        = Input(Bool())
    val valid_out    = Output(Bool())
    val ifu_data_in  = Input(new ifu_data())
    val ifu_data_out = Output(new ifu_data())
  })

  // fetch inst from imem need a cycle.
  val ifu_idu_pipe = Module(new Pipe(new ifu_data()))
  ifu_idu_pipe.io.enq.bits  := io.ifu_data_in
  ifu_idu_pipe.io.enq.valid := io.valid

  io.valid_out    := ifu_idu_pipe.io.deq.valid
  io.ifu_data_out := ifu_idu_pipe.io.deq.bits
}
