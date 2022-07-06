import chisel3._
import chisel3.util._

class idu_exu extends Module {
  class ifu_data extends Bundle{
    val pc   = UInt(32.W)
    val inst = UInt(32.W)
  }
  class idu_data extends Bundle {
    val rs  = UInt(5.W)
    val rt  = UInt(5.W)
    val rd  = UInt(5.W)
    val imm = UInt(32.W)
  }
  class idu_contr extends Bundle {
    // alu
    val alu_op       = UInt(4.W)
    val alu_src      = Bool()
    // reg   
    val reg_write    = Bool()
    val hi_write     = Bool()
    val lo_write     = Bool()
    val hi_read      = Bool()
    val lo_read      = Bool()
    val hilo_src     = Bool()
    // mem
    val mem_read     = Bool()
    val mem_write    = Bool()
    val mem_mask     = UInt(2.W)
    // branch & jump
    val branch       = Bool()
    val cmp_op       = UInt(3.W)
    val jump         = Bool()
    val jsrc         = Bool()
    val call_src     = Bool()
    val branch_delay = Bool()
    // signed / unsigned
    val signed       = Bool()
    // cp0
    val cp0_read     = Bool()
    val cp0_write    = Bool()
  }

  class intr extends Bundle {
    val addrrd  = Bool()
    val syscall = Bool()
    val breakpt = Bool()
    val noinst  = Bool()
    val eret    = Bool()  
  }

  val io = IO(new Bundle {
    val valid          = Input(Bool())
    val pause          = Input(Bool())
    val valid_out      = Output(Bool())
    val ifu_data_in    = Input(new ifu_data())
    val ifu_data_out   = Output(new ifu_data())
    val idu_data_in    = Input(new idu_data())
    val idu_data_out   = Output(new idu_data())
    val idu_contr_in   = Input(new idu_contr())
    val idu_contr_out  = Output(new idu_contr())
    val intr_in        = Input(new intr())
    val intr_out       = Output(new intr())
    val int            = Input(UInt(6.W))
    val int_out        = Output(UInt(6.W))
  })

  val ifu_data_reg  = RegInit(Reg(new ifu_data()))
  val idu_data_reg  = RegInit(Reg(new idu_data()))
  val idu_contr_reg = RegInit(Reg(new idu_contr()))
  val intr_reg      = RegInit(Reg(new intr))
  val valid_reg     = RegInit(false.B)
  val int_reg       = RegInit(0.U(6.W))

  valid_reg     := Mux(io.pause, valid_reg     , io.valid);
  ifu_data_reg  := Mux(io.pause, ifu_data_reg  , io.ifu_data_in);
  idu_data_reg  := Mux(io.pause, idu_data_reg  , io.idu_data_in);
  idu_contr_reg := Mux(io.pause, idu_contr_reg , io.idu_contr_in);
  intr_reg      := Mux(io.pause, intr_reg, io.intr_in);
  int_reg     := Mux(io.pause, int_reg | io.int, io.int);
  
  io.valid_out     := valid_reg
  io.ifu_data_out  := ifu_data_reg
  io.idu_data_out  := idu_data_reg
  io.idu_contr_out := idu_contr_reg
  io.intr_out      := intr_reg
  io.int_out      := int_reg
}
