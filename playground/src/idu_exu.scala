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
    val alu_op    = UInt(4.W)
    val alu_src   = Bool()
    // reg
    val reg_write = Bool()
    val hi_write  = Bool()
    val lo_write  = Bool()
    val hi_read   = Bool()
    val lo_read   = Bool()
    val hilo_src  = Bool()
    // mem
    val mem_read  = Bool()
    val mem_write = Bool()
    val mem_mask  = UInt(2.W)
    // branch & jump
    val branch    = Bool()
    val cmp_op    = UInt(3.W)
    val jump      = Bool()
    val jsrc      = Bool()
    val call_src  = Bool()
    // signed / unsigned
    val signed    = Bool()
    // cp0
    val cp0_read  = Bool()
    val cp0_write = Bool()
  }
  val io = IO(new Bundle {
    val valid          = Input(Bool())
    val valid_out      = Output(Bool())
    val ifu_data_in    = Input(new ifu_data())
    val ifu_data_out   = Output(new ifu_data())
    val idu_data_in    = Input(new idu_data())
    val idu_data_out   = Output(new idu_data())
    val idu_contr_in   = Input(new idu_contr())
    val idu_contr_out  = Output(new idu_contr())
  })

  val ifu_data_pipe = Module(new Pipe(new ifu_data()))
  ifu_data_pipe.io.enq.bits  := io.ifu_data_in
  ifu_data_pipe.io.enq.valid := io.valid

  val idu_data_pipe = Module(new Pipe(new idu_data()))
  idu_data_pipe.io.enq.bits  := io.idu_data_in
  idu_data_pipe.io.enq.valid := io.valid

  val idu_contr_pipe = Module(new Pipe(new idu_contr()))
  idu_contr_pipe.io.enq.bits  := io.idu_contr_in
  idu_contr_pipe.io.enq.valid := io.valid

  io.valid_out     := ifu_data_pipe.io.deq.valid
  io.ifu_data_out  := ifu_data_pipe.io.deq.bits
  io.idu_data_out  := idu_data_pipe.io.deq.bits
  io.idu_contr_out := idu_contr_pipe.io.deq.bits
}
