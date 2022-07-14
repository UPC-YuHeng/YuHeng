import chisel3._
import chisel3.util._

class idu_exu extends Module {
  class data extends Bundle{
    val inst     = UInt(32.W)
    val rs       = UInt(5.W)
    val rt       = UInt(5.W)
    val rd       = UInt(5.W)
    val cp0_addr = UInt(5.W)
    val imm      = UInt(32.W)
  }
  class contr extends Bundle {
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
    val inst_addrrd = Bool()
    val syscall     = Bool()
    val breakpt     = Bool()
    val noinst      = Bool()
    val eret        = Bool()
  }
  class delay extends Bundle {
    val valid = Bool()
    val pc    = UInt(32.W)
    val data  = new data()
    val contr = new contr()
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
