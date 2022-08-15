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
	  val tlb_intr = Input(new tlb_intr())
  })

  val in    = io.in
  val out   = io.out
  val bin   = RegInit(Reg(new bin_data()))
  val bout  = RegInit(Reg(new bout_data()))
  val ifu_data = Wire(new idu_info())
  val ifu_intr = Wire(new inst_intr())

  val instrd_in  = in.bits.data.addr(1, 0).orR
  val instrd_bin = bin.bits.data.addr(1, 0).orR
  val intr_in    = instrd_in
  val intr_bin   = instrd_bin
  val valid      = io.rout.valid | bout.valid
  
  in.ready := (out.valid & out.ready) | ~bin.ready

  val flush = RegInit(false.B)
  val clear = flush | io.flush
  flush := ~valid & clear

  bin.ready := MuxCase(bin.ready, Array(    // low active
    (valid & clear)         -> false.B,
    (in.valid & in.ready)   -> true.B,
    (out.valid & out.ready) -> false.B
  ))
  bin.bits  := MuxCase(bin.bits, Array(
    (in.valid & in.ready)   -> in.bits,
    (out.valid & out.ready) -> Reg(new ifu_in())
  ))

  // fetch inst
  io.rin.en    := ((in.ready & ~intr_in) | (bin.ready & ~intr_bin)) & ~valid
  io.rin.wen   := 0.U
  io.rin.addr  := Mux(in.ready, in.bits.data.addr, bin.bits.data.addr)
  io.rin.wdata := 0.U
  io.rin.rsize := 2.U // 4 bytes

  bout.valid     := MuxCase(bout.valid, Array(
    clear                   -> false.B,
    (out.valid & out.ready) -> false.B,
    (valid | intr_bin | io.tlb_intr.tlbs | io.tlb_intr.tlbd | io.tlb_intr.tlbl)  -> true.B
  ))
  bout.bits.data := MuxCase(ifu_data, Array(
    clear                   -> Reg(new idu_info()),
    (out.valid & out.ready) -> Reg(new idu_info()),
    bout.valid              -> bout.bits.data
  ))
  bout.bits.intr := MuxCase(ifu_intr, Array(
    clear                   -> Reg(new inst_intr()),
    (out.valid & out.ready) -> Reg(new inst_intr()),
    bout.valid              -> bout.bits.intr
  ))

  out.valid := bout.valid
  out.bits  := bout.bits

/****************************** data ******************************/
  ifu_data.pc   := bin.bits.data.addr
  ifu_data.inst := io.rout.rdata
  
/****************************** intr ******************************/
  ifu_intr.instrd   := instrd_bin
  ifu_intr.datard   := false.B
  ifu_intr.datawt   := false.B
  ifu_intr.vaddr    := bin.bits.data.addr
  ifu_intr.syscall  := false.B
  ifu_intr.breakpt  := false.B
  ifu_intr.reserved := false.B
  ifu_intr.eret     := false.B
  ifu_intr.exceed   := false.B
  ifu_intr.tlbs     := io.tlb_intr.tlbs
  ifu_intr.tlbl     := io.tlb_intr.tlbl
  ifu_intr.tlbd     := io.tlb_intr.tlbd
  ifu_intr.refill   := io.tlb_intr.refill
  ifu_intr.tlb_vaddr:= io.tlb_intr.vaddr
  ifu_intr.tlb_vpn2 := io.tlb_intr.vpn2
}
