import chisel3._
import chisel3.util._

class exu_mem extends Module {
  class data extends Bundle{
    val inst     = UInt(32.W)
    val rd       = UInt(5.W)
    val rs       = UInt(5.W)
    val dest     = UInt(32.W)
    val dest_hi  = UInt(32.W)
    val dest_lo  = UInt(32.W)
    val cmp      = Bool()
    val rt_data  = UInt(32.W)
    val cp0_data = UInt(32.W)
  }
  class contr extends Bundle {
    val reg_write = Bool()
    val hi_write  = Bool()
    val lo_write  = Bool()
    val hi_read   = Bool()
    val lo_read   = Bool()
    val hilo_src  = Bool()
    val mem_read  = Bool()
    val mem_write = Bool()
    val mem_mask  = UInt(2.W)
    val branch    = Bool()
    val cmp_op    = UInt(3.W)
    val jump      = Bool()
    val jsrc      = Bool()
    val call_src  = Bool()
    val signed    = Bool()
    val cp0_read  = Bool()
    val cp0_write = Bool()
  }
  class delay extends Bundle {
    val valid = Bool()
    val pc    = UInt(32.W)
    val data  = new data()
    val contr = new contr()
  }
  val io = IO(new Bundle {
    val pause = Input (Bool())
    val intr  = Input (Bool())
    val in    = Input (new delay())
    val out   = Output(new delay())
  })

  io.out := RegNext(Mux(io.in.valid & (~io.pause) & (~io.intr), io.in, Reg(new delay())), Reg(new delay()))
}
