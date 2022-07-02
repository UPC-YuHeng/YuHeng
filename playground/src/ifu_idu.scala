import chisel3._
import chisel3.util._

class ifu_idu extends Module {
  class ifu_data extends Bundle {
    // val inst    = UInt(32.W)
    val pc      = UInt(32.W)
  }
  
  val io = IO(new Bundle {
    val valid        = Input(Bool())
    val pause        = Input(Bool())
    val valid_out    = Output(Bool())
    val ifu_data_in  = Input(new ifu_data())
    val ifu_data_out = Output(new ifu_data())
  })

  // fetch inst from imem need a cycle.
  val ifu_idu_reg = RegInit(Reg(new ifu_data()));
  val valid_reg   = RegInit(false.B);
  
  valid_reg   := Mux(io.pause, valid_reg, io.valid);
  ifu_idu_reg := Mux(io.pause, ifu_idu_reg, io.ifu_data_in);

  io.valid_out    := valid_reg
  io.ifu_data_out := ifu_idu_reg
}
