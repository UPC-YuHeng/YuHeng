import chisel3._
import chisel3.util._

import yuheng.debug.traceregs 

class reg extends Module {
  class reg_in extends Bundle {
    // regfile
    val reg_write = Bool()
    val rs_addr   = UInt(5.W)
    val rt_addr   = UInt(5.W)
    val rs_addr_hl= UInt(5.W)
    val rd_addr   = UInt(5.W)
    val rd_data   = UInt(32.W)
    // hi/lo
    val hi_write  = Bool()
    val lo_write  = Bool()
    val hi_read   = Bool()
    val lo_read   = Bool()
    val hilo_src  = Bool()
    val hi_data   = UInt(32.W)
    val lo_data   = UInt(32.W)
    // cp0
    val cp0_read  = Bool()
    val cp0_data  = UInt(32.W)
    // pc for difftest
    val valid     = Bool()
    val pc        = UInt(32.W)
  }
  class reg_out extends Bundle {
    // regfile
    val rs_data = UInt(32.W)
    val rt_data = UInt(32.W)
    val rd_data = UInt(32.W)
  }
  val io = IO(new Bundle {
    val in       = Input(new reg_in())
    val out      = Output(new reg_out())
  })

  val reg = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
  val reg_hi = RegInit(0.U(32.W))
  val reg_lo = RegInit(0.U(32.W))

  io.out.rs_data := reg(io.in.rs_addr)
  io.out.rt_data := reg(io.in.rt_addr)

  io.out.rd_data := Mux(io.in.hi_read,
    reg_hi, Mux(io.in.lo_read,
      reg_lo, Mux(io.in.cp0_read,
        io.in.cp0_data,
        io.in.rd_data
      )
    )
  )

  when (io.in.reg_write) {
    reg(io.in.rd_addr) := io.out.rd_data 
  }
  reg(0) := 0.U

  when (io.in.hi_write) {
    reg_hi := Mux(io.in.hilo_src, reg(io.in.rs_addr_hl), io.in.hi_data)
  }
  when (io.in.lo_write) {
    reg_lo := Mux(io.in.hilo_src, reg(io.in.rs_addr_hl), io.in.lo_data)
  }

  // val traceregs = Module(new traceregs())
  // val reg_pc = RegInit("h0".U(32.W))
  // when(io.in.valid){
  //   reg_pc := io.in.pc
  // }
  // traceregs.io.rf := reg
  // traceregs.io.pc := reg_pc
}
