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

  val in = io.in
  val pc = in.bits.data.pc

  io.in.ready := true.B

  val reg    = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
  val reg_hi = RegInit(0.U(32.W))
  val reg_lo = RegInit(0.U(32.W))

  // idu-read
  io.outa.rs := reg(io.ina.rs)
  io.outa.rt := reg(io.ina.rt)
  // intr-read
  io.outb.rt := reg(io.inb.rt)

  val reg_en = Mux(io.flush, false.B, in.bits.contr.reg_write)
  val hi_en  = Mux(io.flush, false.B, in.bits.contr.hi_write)
  val lo_en  = Mux(io.flush, false.B, in.bits.contr.lo_write)
  val wdata = MuxCase(in.bits.data.data, Array(
    in.bits.contr.hi_read  -> reg_hi,
    in.bits.contr.lo_read  -> reg_lo,
    in.bits.contr.link     -> (pc + 8.U),
    in.bits.contr.cp0_read -> io.cp0_data
  ))

  // normal regs
  when (reg_en) {
    reg(in.bits.data.addr) := wdata
  }
  reg(0) := 0.U
  // hilo regs
  when (hi_en) {
    reg_hi := in.bits.data.hi
  }
  when (lo_en) {
    reg_lo := in.bits.data.lo
  }

  // debug io
  io.debug_wb.pc       := pc
  io.debug_wb.rf_wen   := Fill(4, reg_en)
  io.debug_wb.rf_wnum  := in.bits.data.addr
  io.debug_wb.rf_wdata := wdata
}
