import chisel3._
import chisel3.util._

class mem extends Module {
  class bin_data extends Bundle {
    val ready = Bool()
    val bits  = new mem_in()
  }
  class bout_data extends Bundle {
    val valid = Bool()
    val bits  = new mem_out()
  }
  val io = IO(new Bundle {
    val in    = Flipped(Decoupled(new mem_in()))
    val out   = Decoupled(new mem_out())
    val rin   = Output(new ram_in())
    val rout  = Input (new ram_out())
    val flush = Input (Bool())
	  val tlb_intr = Input(new tlb_intr())
  })

  val in  = io.in
  val out = io.out
  val bin  = RegInit(Reg(new bin_data()))
  val bout = RegInit(Reg(new bout_data()))
  val mem_data  = Wire(new reg_info())
  val mem_contr = Wire(new inst_contr())
  val mem_conf  = Wire(new conflict_data())
  val mem_intr  = Wire(new inst_intr())


  val datard_in  = in.bits.contr.mem_read &
    MuxLookup(in.bits.contr.mem_mask, false.B, Array(
      2.U -> in.bits.data.dest(0),
      3.U -> in.bits.data.dest(1, 0).orR
    ))
  val datawt_in  = in.bits.contr.mem_write &
    MuxLookup(in.bits.contr.mem_mask, false.B, Array(
      2.U -> in.bits.data.dest(0),
      3.U -> in.bits.data.dest(1, 0).orR
    ))
  val datard_bin = bin.bits.contr.mem_read &
    MuxLookup(bin.bits.contr.mem_mask, false.B, Array(
      2.U -> bin.bits.data.dest(0),
      3.U -> bin.bits.data.dest(1, 0).orR
    ))
  val datawt_bin = bin.bits.contr.mem_write &
    MuxLookup(bin.bits.contr.mem_mask, false.B, Array(
      2.U -> bin.bits.data.dest(0),
      3.U -> bin.bits.data.dest(1, 0).orR
    ))
  val intr_in  = datard_in  | datawt_in
  val intr_bin = datard_bin | datawt_bin
  val mem_in   = in.bits.contr.mem_read  | in.bits.contr.mem_write 
  val mem_bin  = bin.bits.contr.mem_read | bin.bits.contr.mem_write
  val valid    = io.rout.valid | bout.valid

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
    (out.valid & out.ready) -> Reg(new mem_in())
  ))

  io.rin.en    := ((in.ready & mem_in & ~intr_in) | (bin.ready & mem_bin & ~intr_bin)) & ~valid
  io.rin.wen   := MuxCase(0.U, Array(
    (in.ready & in.bits.contr.mem_write)   -> MuxLookup(in.bits.contr.mem_mask, 0.U, Array(
      0.U -> MuxLookup(in.bits.data.dest(1, 0), 0.U, Array(
        "b00".U -> "b0001".U,
        "b01".U -> "b0010".U,
        "b10".U -> "b0100".U,
        "b11".U -> "b1000".U,
      )),
      1.U -> MuxLookup(in.bits.data.dest(1, 0), 0.U, Array(
        "b00".U -> "b0011".U,
        "b10".U -> "b1100".U
      )),
      2.U -> "b1111".U
    )),
    (bin.ready & bin.bits.contr.mem_write) -> MuxLookup(bin.bits.contr.mem_mask, 0.U, Array(
      0.U -> MuxLookup(bin.bits.data.dest(1, 0), 0.U, Array(
        "b00".U -> "b0001".U,
        "b01".U -> "b0010".U,
        "b10".U -> "b0100".U,
        "b11".U -> "b1000".U,
      )),
      1.U -> MuxLookup(bin.bits.data.dest(1, 0), 0.U, Array(
        "b00".U -> "b0011".U,
        "b10".U -> "b1100".U
      )),
      2.U -> "b1111".U
    ))
  ))
  io.rin.addr  := Mux(in.ready,
    Mux(in.bits.contr.mem_write,  Cat(in.bits.data.dest(31, 2), 0.U(2.W)),  in.bits.data.dest),
    Mux(bin.bits.contr.mem_write, Cat(bin.bits.data.dest(31, 2), 0.U(2.W)), bin.bits.data.dest)
  )
  io.rin.wdata := MuxCase(0.U, Array(
    in.ready  -> MuxLookup(in.bits.contr.mem_mask, 0.U, Array(
      0.U -> Fill(4, in.bits.data.data( 7, 0)),
      1.U -> Fill(2, in.bits.data.data(15, 0)),
      2.U -> in.bits.data.data
    )),
    bin.ready -> MuxLookup(bin.bits.contr.mem_mask, 0.U, Array(
      0.U -> Fill(4, bin.bits.data.data( 7, 0)),
      1.U -> Fill(2, bin.bits.data.data(15, 0)),
      2.U -> bin.bits.data.data
    ))
  ))
  io.rin.rsize := Mux(in.ready, in.bits.contr.mem_mask, bin.bits.contr.mem_mask)

  val tlb_intr = io.tlb_intr.tlbs | io.tlb_intr.tlbl | io.tlb_intr.tlbd

  bout.valid      := MuxCase(bout.valid, Array(
    clear                   -> false.B,
    (out.valid & out.ready) -> false.B,
    (bin.ready & ~mem_bin)  -> true.B,
    (valid | intr_bin | tlb_intr)      -> true.B
  ))
  bout.bits.data  := MuxCase(mem_data, Array(
    clear                   -> Reg(new reg_info()),
    (out.valid & out.ready) -> Reg(new reg_info()),
    bout.valid              -> bout.bits.data
  ))
  bout.bits.contr := MuxCase(mem_contr, Array(
    clear                   -> Reg(new inst_contr()),
    (out.valid & out.ready) -> Reg(new inst_contr()),
    bout.valid              -> bout.bits.contr
  ))
  bout.bits.conf  := Mux(out.valid & out.ready, Reg(new conflict_data()), out.bits.conf)
  bout.bits.intr  := MuxCase(mem_intr, Array(
    (out.valid & out.ready) -> Reg(new inst_intr()),
    bout.valid              -> bout.bits.intr
  ))

  out.valid      := bout.valid
  out.bits.data  := bout.bits.data
  out.bits.contr := bout.bits.contr
  out.bits.conf  := MuxCase(mem_conf, Array(
    clear                   -> Reg(new conflict_data()),
    bout.valid              -> bout.bits.conf
  ))
  out.bits.intr  := bout.bits.intr

/****************************** data ******************************/
  mem_data.pc   := bin.bits.data.pc
  mem_data.addr := bin.bits.conf.rd
  mem_data.data := Mux(io.rout.valid,
    MuxLookup(bin.bits.contr.mem_mask, 0.U, Array(
      0.U -> Cat(Fill(24, io.rout.rdata( 7) & bin.bits.contr.signed.asUInt()), io.rout.rdata( 7,  0)),
      1.U -> Cat(Fill(16, io.rout.rdata(15) & bin.bits.contr.signed.asUInt()), io.rout.rdata(15,  0)),
      2.U -> io.rout.rdata
    )),
    bin.bits.data.dest
  )
  mem_data.hi := bin.bits.data.hi
  mem_data.lo := bin.bits.data.lo

/****************************** contr ******************************/
  mem_contr.alu_op    := false.B
  mem_contr.mem_read  := false.B
  mem_contr.mem_write := false.B
  mem_contr.mem_mask  := false.B
  mem_contr.reg_write := bin.bits.contr.reg_write
  mem_contr.hi_write  := bin.bits.contr.hi_write
  mem_contr.lo_write  := bin.bits.contr.lo_write
  mem_contr.hi_read   := bin.bits.contr.hi_read
  mem_contr.lo_read   := bin.bits.contr.lo_read
  mem_contr.hilo_src  := false.B
  mem_contr.jump      := bin.bits.contr.jump
  mem_contr.jaddr     := 0.U
  mem_contr.branch    := bin.bits.contr.branch
  mem_contr.cmp       := 0.U
  mem_contr.baddr     := 0.U
  mem_contr.link      := bin.bits.contr.link
  mem_contr.signed    := bin.bits.contr.signed
  mem_contr.cp0_read  := bin.bits.contr.cp0_read
  mem_contr.cp0_write := bin.bits.contr.cp0_write
  mem_contr.tlbr      := bin.bits.contr.tlbr
  mem_contr.tlbp      := bin.bits.contr.tlbp
  mem_contr.tlbwi     := bin.bits.contr.tlbwi
  
/****************************** conf ******************************/
  mem_conf.rs := bin.bits.conf.rs
  mem_conf.rt := bin.bits.conf.rt
  mem_conf.rd := bin.bits.conf.rd

/****************************** intr ******************************/
  val ifu_intr = bin.bits.intr.tlbs | bin.bits.intr.tlbl | bin.bits.intr.tlbd
  mem_intr.instrd   := bin.bits.intr.instrd
  mem_intr.datard   := datard_bin
  mem_intr.datawt   := datawt_bin
  mem_intr.vaddr    := Mux(bin.bits.intr.instrd, bin.bits.intr.vaddr, bin.bits.data.dest)
  mem_intr.syscall  := bin.bits.intr.syscall
  mem_intr.breakpt  := bin.bits.intr.breakpt
  mem_intr.reserved := bin.bits.intr.reserved
  mem_intr.eret     := bin.bits.intr.eret
  mem_intr.exceed   := bin.bits.intr.exceed
  mem_intr.tlbs     := Mux(ifu_intr, bin.bits.intr.tlbs, io.tlb_intr.tlbs)
  mem_intr.tlbl     := Mux(ifu_intr, bin.bits.intr.tlbl, io.tlb_intr.tlbl)
  mem_intr.tlbd     := Mux(ifu_intr, bin.bits.intr.tlbd, io.tlb_intr.tlbd)
  mem_intr.refill   := Mux(ifu_intr, bin.bits.intr.refill, io.tlb_intr.refill)
  mem_intr.tlb_vaddr:= Mux(ifu_intr, bin.bits.intr.tlb_vaddr, io.tlb_intr.vaddr)
  mem_intr.tlb_vpn2 := Mux(ifu_intr, bin.bits.intr.tlb_vpn2, io.tlb_intr.vpn2)
}
