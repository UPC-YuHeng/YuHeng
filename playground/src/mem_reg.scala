import chisel3._
import chisel3.util._

class mem_reg extends Module {
  class ifu_data extends Bundle{
    val pc   = UInt(32.W)
    val inst = UInt(32.W)
  }

  class idu_data extends Bundle{
    val rd = UInt(5.W)
    val rs = UInt(5.W)
  }

  class idu_contr extends Bundle {
    val branch    = Bool()
    val mem_read  = Bool()
    val mem_mask  = UInt(2.W)
    val signed    = Bool()
    val reg_write = Bool()
    val hi_write  = Bool()
    val lo_write  = Bool()
    val hi_read   = Bool()
    val lo_read   = Bool()
    val hilo_src  = Bool()
    val cp0_read  = Bool()
    val cp0_write = Bool()
    val call_src  = Bool()
  }

  class exu_data extends Bundle {
    val dest      = UInt(32.W)
    val dest_hi   = UInt(32.W)
    val dest_lo   = UInt(32.W)
    val rt_data   = UInt(32.W)
  }

  class mem_data extends Bundle {
    val rdata = UInt(32.W)
  }

  val io = IO(new Bundle {
    val valid         = Input(Bool())
    val valid_out     = Output(Bool())
    val ifu_data_in   = Input(new ifu_data())
    val ifu_data_out  = Output(new ifu_data())
    val idu_data_in   = Input(new idu_data())
    val idu_data_out  = Output(new idu_data())
    val idu_contr_in  = Input(new idu_contr())
    val idu_contr_out = Output(new idu_contr())
    val exu_data_in   = Input(new exu_data())
    val exu_data_out  = Output(new exu_data())
    val mem_data_in   = Input(new mem_data())
    val mem_data_out  = Output(new mem_data())
  })
  val ifu_data_reg  = RegInit(Reg(new ifu_data()))
  val idu_data_reg  = RegInit(Reg(new idu_data()))
  val idu_contr_reg = RegInit(Reg(new idu_contr()))
  val exu_data_reg  = RegInit(Reg(new exu_data()))
  val mem_data_reg  = RegInit(Reg(new mem_data()))
  val valid_reg     = RegInit(false.B);
  
  valid_reg     := io.valid
  ifu_data_reg  := io.ifu_data_in
  idu_data_reg  := io.idu_data_in
  idu_contr_reg := io.idu_contr_in
  exu_data_reg  := io.exu_data_in
  mem_data_reg  := io.mem_data_in

  // fetch inst from imem need a cycle.
  io.valid_out     := valid_reg
  io.ifu_data_out  := ifu_data_reg
  io.idu_data_out  := idu_data_reg
  io.idu_contr_out := idu_contr_reg
  io.exu_data_out  := exu_data_reg
  io.mem_data_out  := mem_data_reg
}
