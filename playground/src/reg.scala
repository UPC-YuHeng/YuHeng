import chisel3._
import chisel3.util._

class reg extends Module {
  val io = IO(new Bundle {
    // read ports
    val ina      = Input (new regread_in())
    val outa     = Output(new regread_out())
    val inb      = Input (new intr_reginfo())
    val outb     = Output(new intr_regdata())
    // write port
    val in       = Flipped(Decoupled(new reg_in()))
    val cp0_data = Input (UInt(32.W))
    val flush    = Input (Bool())
    val debug_wb = Output(new debug_io())
  })

  io.in.ready := true.B

  val in = Mux(io.in.valid & io.in.ready, io.in.bits, Reg(new reg_in()))
  val pc = in.data.pc

  val reg    = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
  val reg_hi = RegInit(0.U(32.W))
  val reg_lo = RegInit(0.U(32.W))

  // idu-read
  io.outa.rs := reg(io.ina.rs)
  io.outa.rt := reg(io.ina.rt)
  // intr-read
  io.outb.rt := reg(io.inb.rt)

  val reg_en = Mux(io.flush, false.B, in.contr.reg_write)
  val hi_en  = Mux(io.flush, false.B, in.contr.hi_write)
  val lo_en  = Mux(io.flush, false.B, in.contr.lo_write)
  val wdata = MuxCase(in.data.data, Array(
    in.contr.hi_read  -> reg_hi,
    in.contr.lo_read  -> reg_lo,
    in.contr.link     -> (pc + 8.U),
    in.contr.cp0_read -> io.cp0_data
  ))

  // normal regs
  when (reg_en) {
    reg(in.data.addr) := wdata
  }
  reg(0) := 0.U
  // hilo regs
  when (hi_en) {
    reg_hi := in.data.hi
  }
  when (lo_en) {
    reg_lo := in.data.lo
  }

  // debug io
  io.debug_wb.pc       := pc
  io.debug_wb.rf_wen   := Fill(4, reg_en)
  io.debug_wb.rf_wnum  := in.data.addr
  io.debug_wb.rf_wdata := wdata
}
