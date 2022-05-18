import chisel3._
import chisel3.util._

object CP0RegisterList {
  val badvaddr = 8.U
  val count    = 9.U
  val status   = 12.U
  val cause    = 13.U
  val epc      = 14.U
}

import CP0RegisterList._

class reg extends Module {
  class reg_in extends Bundle {
    // regfile
    val reg_write = Bool()
    val rs_addr   = UInt(5.W)
    val rt_addr   = UInt(5.W)
    val rd_addr   = UInt(5.W)
    val rd_data   = UInt(32.W)
    // hi/lo
    val hilo_en   = Bool()
    val trans_hi  = Bool()
    val trans_lo  = Bool()
    val hi_data   = UInt(32.W)
    val lo_data   = UInt(32.W)
    // cp0
    val cp0_read  = Bool()
    val cp0_write = Bool()
    val cp0_addr  = UInt(5.W)
    val cp0_sel   = UInt(3.W)
  }
  class reg_out extends Bundle {
    // regfile
    val rs_data = UInt(32.W)
    val rt_data = UInt(32.W)
  }
  val io = IO(new Bundle {
    val in  = Input(new reg_in())
    val out = Output(new reg_out())
  })

  val reg = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  io.out.rs_data := reg(io.in.rs_addr)
  io.out.rt_data := reg(io.in.rt_data)

  when (io.in.reg_write) {
    reg(io.in.rd_addr) := Mux(io.in.trans_hi,
      reg_hi, Mux(io.in.trans_lo,
        reg_lo, Mux(io.in.cp0_read,
          cp0(io.in.cp0_addr),
          io.in.rd_data
        )
      )
    )
  }

  val reg_hi = RegInit(0.U(32.W))
  val reg_lo = RegInit(0.U(32.W))

  when (io.in.hilo_en) {
    reg_hi := io.in.hi_data
    reg_lo := io.in.lo_data
  }

  val cp0 = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  when (io.reset) {
    // TODO(Zhang Sen): reset for cp0.
  }
  when (io.in.cp0_write) {
    cp0(cp0_addr) := reg(rt_addr)
  }

  reg(0) = 0.U
}