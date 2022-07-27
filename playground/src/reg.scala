import chisel3._
import chisel3.util._

class reg extends Module {
  val io = IO(new Bundle {
    // read ports
    val iduin    = Input (new idu_reginfo())
    val iduout   = Output(new idu_regdata())
    val intrin   = Input (new intr_reginfo())
    val introut  = Output(new intr_regdata())
    // write port
    val memin    = Flipped(Decoupled(new reg_info()))
    // contr port
    val contr    = Input (new reg_contr())
    val intr     = Input (new reg_intr())
    val debug_wb = Output(new debug_io())
  })

  val pc = io.memin.bits.pc

  io.memin.ready := true.B

  val reg    = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
  val reg_hi = RegInit(0.U(32.W))
  val reg_lo = RegInit(0.U(32.W))

  // idu-read
  io.iduout.rs := reg(io.iduin.rs)
  io.iduout.rt := reg(io.iduin.rt)
  // intr-read
  io.introut.data := reg(io.intrin.rt)

  val wdata = MuxCase(io.memin.bits.data, Array(
    io.contr.hi_read  -> reg_hi,
    io.contr.lo_read  -> reg_lo,
    io.contr.link     -> (pc + 8.U),
    io.intr.cp0_read  -> io.intr.cp0_data
  ))

  // write
  // normal regs
  when (io.contr.reg_write) {
    reg(io.memin.bits.addr) := wdata
  }
  reg(0) := 0.U
  // hilo regs
  when (io.contr.hi_write) {
    reg_hi := io.memin.bits.hi
  }
  when (io.contr.lo_write) {
    reg_lo := io.memin.bits.lo
  }

  // debug io
  io.debug_wb.pc       := pc
  io.debug_wb.rf_wen   := Fill(4, io.contr.reg_write)
  io.debug_wb.rf_wnum  := io.memin.bits.addr
  io.debug_wb.rf_wdata := wdata
}
