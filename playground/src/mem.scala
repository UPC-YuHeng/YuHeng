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
  })

  val in  = io.in
  val out = io.out

  val bin  = RegInit(Reg(new bin_data()))
  val bout = RegInit(Reg(new bout_data()))

  val datard = MuxCase(bout.bits.intr.datard, Array(
    bin.ready -> (bin.bits.contr.mem_read & MuxLookup(bin.bits.contr.mem_mask, false.B, Array(
      2.U -> bin.bits.data.dest(0),
      3.U -> bin.bits.data.dest(1, 0).orR
    )))
  ))
  val datawt  = MuxCase(bout.bits.intr.datawt, Array(
    bin.ready -> (bin.bits.contr.mem_write & MuxLookup(bin.bits.contr.mem_mask, false.B, Array(
      2.U -> bin.bits.data.dest(0),
      3.U -> bin.bits.data.dest(1, 0).orR
    )))
  ))

  val mem_en = (bin.bits.contr.mem_read | bin.bits.contr.mem_write) & ~(datard | datawt)

  val intr   = datard | datawt
  val valid  = io.rout.valid | bout.valid

  val flush = RegInit(false.B)
  val clear = flush | io.flush
  flush := ~valid & ~(bin.ready & ~mem_en) & clear

  in.ready := ~bin.ready

  bin.ready := MuxCase(bin.ready, Array(    // low active
    (io.rout.valid & clear) -> false.B,
    (in.valid & in.ready)   -> true.B,
    (out.valid & out.ready) -> false.B,
  ))
  bin.bits  := MuxCase(bin.bits, Array(
    (in.valid & in.ready)   -> in.bits,
    (out.valid & out.ready) -> Reg(new mem_in())
  ))

  io.rin.en    := bin.ready & mem_en & ~valid
  io.rin.wen   := Mux(bin.bits.contr.mem_write,
    MuxLookup(bin.bits.contr.mem_mask, 0.U, Array(
      1.U -> MuxLookup(bin.bits.data.dest(1, 0), 0.U, Array(
        "b00".U -> "b0001".U,
        "b01".U -> "b0010".U,
        "b10".U -> "b0100".U,
        "b11".U -> "b1000".U,
      )),
      2.U -> MuxLookup(bin.bits.data.dest(1, 0), 0.U, Array(
        "b00".U -> "b0011".U,
        "b10".U -> "b1100".U
      )),
      3.U -> "b1111".U
    )),
    0.U
  )
  io.rin.addr  := Mux(bin.bits.contr.mem_write, Cat(bin.bits.data.dest(31, 2), 0.U(2.W)), bin.bits.data.dest)
  io.rin.wdata := MuxLookup(bin.bits.contr.mem_mask, 0.U, Array(
    1.U -> Fill(4, bin.bits.data.data( 7, 0)),
    2.U -> Fill(2, bin.bits.data.data(15, 0)),
    3.U -> bin.bits.data.data
  ))
  io.rin.rsize := MuxLookup(bin.bits.contr.mem_mask, 0.U, Array(
    1.U -> 0.U,
    2.U -> 1.U,
    3.U -> 2.U
  ))

  val rdata = MuxLookup(bin.bits.contr.mem_mask, 0.U, Array(
    1.U -> Cat(Fill(24, io.rout.rdata( 7) & bin.bits.contr.signed.asUInt()), io.rout.rdata( 7,  0)),
    2.U -> Cat(Fill(16, io.rout.rdata(15) & bin.bits.contr.signed.asUInt()), io.rout.rdata(15,  0)),
    3.U -> io.rout.rdata
  ))

  val mem_contr = Wire(new inst_contr())
  val mem_conf1 = Wire(new conflict_data())
  val mem_conf2 = Wire(new conflict_data())
  val mem_intr  = Wire(new inst_intr())
  bout.bits.data.pc := MuxCase(bout.bits.data.pc, Array(
    clear                   -> 0.U,
    io.rout.valid           -> bin.bits.data.pc,
    (out.valid & out.ready) -> 0.U,
    (bin.ready & ~mem_en)   -> bin.bits.data.pc
  ))
  bout.bits.data.addr := MuxCase(bout.bits.data.addr, Array(
    clear                   -> 0.U,
    io.rout.valid           -> bin.bits.conf.rd,
    (out.valid & out.ready) -> 0.U,
    (bin.ready & ~mem_en)   -> bin.bits.conf.rd
  ))
  bout.bits.data.data := MuxCase(bout.bits.data.data, Array(
    clear                   -> 0.U,
    io.rout.valid           -> rdata,
    (out.valid & out.ready) -> 0.U,
    (bin.ready & ~mem_en)   -> bin.bits.data.dest
  ))
  bout.bits.data.hi := MuxCase(bout.bits.data.hi, Array(
    clear                   -> 0.U,
    io.rout.valid           -> bin.bits.data.hi,
    (out.valid & out.ready) -> 0.U,
    (bin.ready & ~mem_en)   -> bin.bits.data.hi
  ))
  bout.bits.data.lo := MuxCase(bout.bits.data.lo, Array(
    clear                   -> 0.U,
    io.rout.valid           -> bin.bits.data.lo,
    (out.valid & out.ready) -> 0.U,
    (bin.ready & ~mem_en)   -> bin.bits.data.lo
  ))
  bout.bits.contr := MuxCase(bout.bits.contr, Array(
    clear                   -> RegInit(Reg(new inst_contr())),
    io.rout.valid           -> mem_contr,
    (out.valid & out.ready) -> RegInit(Reg(new inst_contr())),
    (bin.ready & ~mem_en)   -> mem_contr
  ))
  bout.bits.conf1 :=  MuxCase(bout.bits.conf1, Array(
    clear                   -> RegInit(Reg(new conflict_data())),
    (in.valid & in.ready)   -> mem_conf1,
    (out.valid & out.ready) -> RegInit(Reg(new conflict_data())),
  ))
  bout.bits.conf2 :=  MuxCase(bout.bits.conf2, Array(
    clear                   -> RegInit(Reg(new conflict_data())),
    io.rout.valid           -> mem_conf2,
    (out.valid & out.ready) -> RegInit(Reg(new conflict_data())),
    (bin.ready & ~mem_en)   -> mem_conf2
  ))
  bout.bits.intr := MuxCase(bout.bits.intr, Array(
    clear                   -> RegInit(Reg(new inst_intr())),
    io.rout.valid           -> mem_intr,
    (out.valid & out.ready) -> RegInit(Reg(new inst_intr())),
    (bin.ready & ~mem_en)   -> mem_intr
  ))
  bout.valid := MuxCase(bout.valid, Array(
    clear                    -> false.B,
    io.rout.valid            -> true.B,
    (out.valid & out.ready)  -> false.B,
    (bin.ready & ~mem_en)    -> true.B
  ))

  out.valid := bout.valid
  out.bits  := bout.bits

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

/****************************** conf ******************************/
  mem_conf1.rs := in.bits.conf.rs
  mem_conf1.rt := in.bits.conf.rt
  mem_conf1.rd := Mux(in.bits.contr.reg_write, in.bits.conf.rd, 0.U)
  mem_conf2.rs := bout.bits.conf1.rs
  mem_conf2.rt := bout.bits.conf1.rt
  mem_conf2.rd := Mux(bin.bits.contr.reg_write, bout.bits.conf1.rd, 0.U)

/****************************** intr ******************************/
  mem_intr.instrd   := bin.bits.intr.instrd
  mem_intr.datard   := datard
  mem_intr.datawt   := datawt
  mem_intr.vaddr    := Mux(bin.bits.intr.instrd, bin.bits.intr.vaddr, bin.bits.data.dest)
  mem_intr.syscall  := bin.bits.intr.syscall
  mem_intr.breakpt  := bin.bits.intr.breakpt
  mem_intr.reserved := bin.bits.intr.reserved
  mem_intr.eret     := bin.bits.intr.eret
  mem_intr.exceed   := bin.bits.intr.exceed
}