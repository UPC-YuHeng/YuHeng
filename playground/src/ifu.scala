import chisel3._
import chisel3.util._

class ifu extends Module {
  class bin_data extends Bundle {
    val ready = Bool()
    val bits  = new ifu_in()
  }
  class bout_data extends Bundle {
    val valid = Bool()
    val bits  = new ifu_out()
  }
  val io = IO(new Bundle {
    val in    = Flipped(Decoupled(new ifu_in()))
    val out   = Decoupled(new ifu_out())
    val rin   = Output(new ram_in())
    val rout  = Input (new ram_out())
    val flush = Input (Bool())
  })

  val flush = RegInit(false.B)
  val clear = flush | io.flush
  flush := ~io.rout.valid & clear

  val in    = io.in
  val out   = io.out
  val bin   = RegInit(Reg(new bin_data()))
  val bout  = RegInit(Reg(new bout_data()))

  val instrd = MuxCase(bout.bits.intr.instrd, Array(
    clear                   -> false.B,
    (in.valid & in.ready)   -> in.bits.data.addr(1, 0).orR, 
    (out.valid & out.ready) -> false.B
  ))

  val intr   = instrd
  val valid  = io.rout.valid | bout.valid

  in.ready := ~bin.ready

  bin.ready := MuxCase(bin.ready, Array(    // low active
    (io.rout.valid & clear) -> false.B,
    (in.valid & in.ready)   -> true.B,
    (out.valid & out.ready) -> false.B,
  ))
  bin.bits  := MuxCase(bin.bits, Array(
    (in.valid & in.ready)   -> in.bits,
    (out.valid & out.ready) -> Reg(new ifu_in())
  ))

  // fetch inst
  io.rin.en    := bin.ready & ~valid & ~intr
  io.rin.wen   := 0.U
  io.rin.addr  := bin.bits.data.addr
  io.rin.wdata := 0.U

  val ifu_intr = Wire(new inst_intr())
  bout.bits.data.pc     := MuxCase(bout.bits.data.pc, Array(
    clear                   -> 0.U,
    bin.ready               -> bin.bits.data.addr
  ))
  bout.bits.data.inst   := MuxCase(bout.bits.data.inst, Array(
    clear                   -> 0.U,
    io.rout.valid           -> io.rout.rdata
  ))
  bout.bits.intr        := MuxCase(bout.bits.intr, Array(
    clear                   -> Reg(new inst_intr()),
    (out.valid & out.ready) -> Reg(new inst_intr()),
    bin.ready               -> ifu_intr
  ))
  bout.valid            := MuxCase(bout.valid, Array(
    clear                   -> false.B,
    io.rout.valid           -> true.B,
    (out.valid & out.ready) -> false.B
  ))
  
  out.valid := bout.valid
  out.bits  := bout.bits
  
/****************************** intr ******************************/
  ifu_intr.instrd   := instrd
  ifu_intr.datard   := false.B
  ifu_intr.datawt   := false.B
  ifu_intr.vaddr    := bin.bits.data.addr
  ifu_intr.syscall  := false.B
  ifu_intr.breakpt  := false.B
  ifu_intr.reserved := false.B
  ifu_intr.eret     := false.B
  ifu_intr.exceed   := false.B
}
