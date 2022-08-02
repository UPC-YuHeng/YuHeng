import chisel3._
import chisel3.util._

class exu extends Module {
  class buf_data extends Bundle {
    val ready = Bool()
    val valid = Bool()
    val bits  = new exu_out()
  }
  val io = IO(new Bundle {
    val in    = Flipped(Decoupled(new exu_in()))
    val out   = Decoupled(new exu_out())
    val flush = Input (Bool())
    val cmp   = Output(Bool())
  })

  val in  = io.in
  val out = io.out
  val buf = RegInit(Reg(new buf_data()))

  val clear = io.flush

  in.ready := ~buf.ready

  buf.ready := MuxCase(buf.ready, Array(    // low active
    clear                   -> false.B,
    (in.valid & in.ready)   -> true.B,
    (out.valid & out.ready) -> false.B
  ))

  val alu     = Module(new alu())
  alu.io.flush     := clear
  alu.io.in.valid  := in.valid & in.ready
  alu.io.in.alu_op := in.bits.contr.alu_op
  alu.io.in.srca   := in.bits.data.srca
  alu.io.in.srcb   := in.bits.data.srcb

  val compare = MuxLookup(Cat(in.bits.contr.signed.asUInt(), in.bits.contr.cmp), false.B, Array(
		// 0 -> NOP, 1 -> Reserved, 2 -> "==", 3 -> "!="
		// 4 -> ">=", 5 -> ">", 6 -> "<=", 7 -> "<"
    0x2.U -> alu.io.out.zero.asBool(),
    0x3.U -> (~alu.io.out.zero).asBool(),
    0x4.U -> alu.io.out.signu.asBool(),
    0x5.U -> (alu.io.out.signu & (~alu.io.out.zero)).asBool(),
    0x6.U -> ((~alu.io.out.signu) | alu.io.out.zero).asBool(),
    0x7.U -> (~alu.io.out.signu).asBool(),
    0xa.U -> alu.io.out.zero.asBool(),
    0xb.U -> (~alu.io.out.zero).asBool(),
    0xc.U -> alu.io.out.signs.asBool(),
    0xd.U -> (alu.io.out.signs & (~alu.io.out.zero)).asBool(),
    0xe.U -> ((~alu.io.out.signs) | alu.io.out.zero).asBool(),
    0xf.U -> (~alu.io.out.signs).asBool(),
  ))

  val exu_contr = Wire(new inst_contr())
  val exu_conf  = Wire(new conflict_data())
  val exu_intr  = Wire(new inst_intr())
  buf.bits.contr := MuxCase(buf.bits.contr, Array(
    clear                   -> RegInit(Reg(new inst_contr())),
    (in.valid & in.ready)   -> exu_contr,
    (out.valid & out.ready) -> RegInit(Reg(new inst_contr()))
  ))
  buf.bits.conf :=  MuxCase(buf.bits.conf, Array(
    clear                   -> RegInit(Reg(new conflict_data())),
    (in.valid & in.ready)   -> exu_conf,
    (out.valid & out.ready) -> RegInit(Reg(new conflict_data()))
  ))
  buf.bits.intr := MuxCase(buf.bits.intr, Array(
    clear                   -> RegInit(Reg(new inst_intr())),
    (in.valid & in.ready)   -> exu_intr,
    (out.valid & out.ready) -> RegInit(Reg(new inst_intr()))
  ))

  buf.valid := MuxCase(buf.valid, Array(
    clear                   -> false.B,
    alu.io.out.valid        -> true.B,
    (in.valid & in.ready)   -> false.B,
    (out.valid & out.ready) -> false.B
  ))

  out.valid := buf.valid
  out.bits  := buf.bits
  io.cmp    := compare

/****************************** data ******************************/
  buf.bits.data.pc := MuxCase(buf.bits.data.pc, Array(
    clear                   -> 0.U,
    (in.valid & in.ready)   -> in.bits.data.pc,
    (out.valid & out.ready) -> 0.U
  ))
  buf.bits.data.dest := MuxCase(buf.bits.data.dest, Array(
    clear                   -> 0.U,
    (in.valid & in.ready)   -> Mux(in.bits.contr.cmp === 0.U, alu.io.out.dest, compare.asUInt()),
    (out.valid & out.ready) -> 0.U
  )) 
  buf.bits.data.data := MuxCase(buf.bits.data.data, Array(
    clear                   -> 0.U,
    (in.valid & in.ready)   -> in.bits.data.data,
    (out.valid & out.ready) -> 0.U
  ))
  buf.bits.data.hi := MuxCase(buf.bits.data.hi, Array(
    clear                   -> 0.U,
    (in.valid & in.ready)   -> in.bits.data.srca,
    alu.io.out.valid        -> alu.io.out.dest_hi,      // for divs/divu, cause it has delay
    (out.valid & out.ready) -> 0.U
  ))
  buf.bits.data.lo := MuxCase(buf.bits.data.lo, Array(
    clear                   -> 0.U,
    (in.valid & in.ready)   -> in.bits.data.srca,
    alu.io.out.valid        -> alu.io.out.dest_lo,      // for divs/divu, cause it has delay
    (out.valid & out.ready) -> 0.U
  ))

/****************************** contr ******************************/
  exu_contr.alu_op    := false.B
  exu_contr.mem_read  := in.bits.contr.mem_read
  exu_contr.mem_write := in.bits.contr.mem_write
  exu_contr.mem_mask  := in.bits.contr.mem_mask
  exu_contr.reg_write := in.bits.contr.reg_write
  exu_contr.hi_write  := in.bits.contr.hi_write
  exu_contr.lo_write  := in.bits.contr.lo_write
  exu_contr.hi_read   := in.bits.contr.hi_read
  exu_contr.lo_read   := in.bits.contr.lo_read
  exu_contr.hilo_src  := false.B
  exu_contr.jump      := in.bits.contr.jump
  exu_contr.jaddr     := 0.U
  exu_contr.branch    := in.bits.contr.branch
  exu_contr.cmp       := 0.U
  exu_contr.baddr     := 0.U
  exu_contr.link      := in.bits.contr.link
  exu_contr.signed    := in.bits.contr.signed
  exu_contr.cp0_read  := in.bits.contr.cp0_read
  exu_contr.cp0_write := in.bits.contr.cp0_write

/****************************** conf ******************************/
  exu_conf.rs := in.bits.conf.rs
  exu_conf.rt := in.bits.conf.rt
  exu_conf.rd := Mux(in.bits.contr.reg_write, in.bits.conf.rd, 0.U)

/****************************** intr ******************************/
  exu_intr.instrd   := in.bits.intr.instrd
  exu_intr.datard   := false.B
  exu_intr.datawt   := false.B
  exu_intr.vaddr    := in.bits.intr.vaddr
  exu_intr.syscall  := in.bits.intr.syscall
  exu_intr.breakpt  := in.bits.intr.breakpt
  exu_intr.reserved := in.bits.intr.reserved
  exu_intr.eret     := in.bits.intr.eret
  exu_intr.exceed   := alu.io.out.exceed
}