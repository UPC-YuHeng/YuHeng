import chisel3._
import chisel3.util._

class ifu_idu extends Module {
  class ifu_data extends Bundle {
    // val inst    = UInt(32.W)
    val pc      = UInt(32.W)
  }
  
  class intr extends Bundle {
    val addrrd  = Bool()
  }
  val io = IO(new Bundle {
    val valid        = Input(Bool())
    val pause        = Input(Bool())
    val valid_out    = Output(Bool())
    val int          = Input(UInt(6.W))
    val int_out      = Output(UInt(6.W))
    val ifu_data_in  = Input(new ifu_data())
    val ifu_data_out = Output(new ifu_data())
    val intr_in      = Input(new intr())
    val intr_out     = Output(new intr())
  })

  // fetch inst from imem need a cycle.
  val ifu_idu_reg = RegInit(Reg(new ifu_data()))
  val intr_reg    = RegInit(Reg(new intr))
  val valid_reg   = RegInit(false.B)
  val int_reg     = RegInit(0.U(6.W))
  
  valid_reg   := Mux(io.pause, valid_reg, io.valid);
  ifu_idu_reg := Mux(io.pause, ifu_idu_reg, io.ifu_data_in);
  intr_reg    := Mux(io.pause, intr_reg, io.intr_in);
  int_reg     := Mux(io.pause, int_reg | io.int, io.int);

  io.valid_out    := valid_reg
  io.ifu_data_out := ifu_idu_reg
  io.intr_out     := intr_reg
  io.int_out      := int_reg
}
