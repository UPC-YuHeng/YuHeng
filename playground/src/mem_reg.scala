import chisel3._
import chisel3.util._

class mem_reg extends Module {
  class ifu_data extends Bundle{
    val pc   = UInt(32.W)
    val inst = UInt(32.W)
  }

  class idu_data extends Bundle{
    val rd  = UInt(5.W)
  }

  class idu_contr extends Bundle {
    val branch    = Bool()
    val mem_read  = Bool()
    val mem_mask  = Bool()
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

  // fetch inst from imem need a cycle.
  val ifu_data_pipe = Module(new Pipe(new ifu_data()))
  ifu_data_pipe.io.enq.bits  := io.ifu_data_in
  ifu_data_pipe.io.enq.valid := io.valid

  val idu_data_pipe = Module(new Pipe(new idu_data()))
  idu_data_pipe.io.enq.bits  := io.idu_data_in
  idu_data_pipe.io.enq.valid := io.valid

  val idu_contr_pipe = Module(new Pipe(new idu_contr()))
  idu_contr_pipe.io.enq.bits  := io.idu_contr_in
  idu_contr_pipe.io.enq.valid := io.valid

  val exu_data_pipe = Module(new Pipe(new exu_data()))
  exu_data_pipe.io.enq.bits  := io.exu_data_in
  exu_data_pipe.io.enq.valid := io.valid

  val mem_data_pipe = Module(new Pipe(new mem_data()))
  mem_data_pipe.io.enq.bits  := io.mem_data_in
  mem_data_pipe.io.enq.valid := io.valid

  io.valid_out     := ifu_data_pipe.io.deq.valid
  io.ifu_data_out  := ifu_data_pipe.io.deq.bits
  io.idu_data_out  := idu_data_pipe.io.deq.bits
  io.idu_contr_out := idu_contr_pipe.io.deq.bits
  io.exu_data_out  := exu_data_pipe.io.deq.bits
  io.mem_data_out  := mem_data_pipe.io.deq.bits
}
