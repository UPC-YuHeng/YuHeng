import chisel3._
import chisel3.util._

class exu extends Module {
  class buf_data extends Bundle {
    val valid = Bool()
    val clear = Bool()
    val out   = new mem_info()
    val intr  = new exu_intr()
    val cmp   = Bool()
  }
  val io = IO(new Bundle {
    val in    = Flipped(Decoupled(new exu_info()))
    val out   = Decoupled(new mem_info())
    val contr = Input (new exu_contr())
    val intr  = Output(new exu_intr())
    val cmp   = Output(Bool())
    val clear = Input (Bool())
    val ok    = Output(Bool())
  })

  val ret_out  = Wire(new mem_info())
  val ret_intr = Wire(new exu_intr())
  val ret_cmp  = Wire(Bool())

  val buf   = RegInit(Reg(new buf_data()))
  val valid = buf.valid & io.out.ready
  io.ok    := valid

  val clear  = buf.clear | io.clear
  buf.clear  := ~valid & clear

  buf.valid := ~valid & (buf.valid | io.in.valid)
  buf.out   := MuxCase(Reg(new mem_info()), Array(
    clear      -> Reg(new mem_info()),
    buf.valid  -> buf.out,
    io.in.valid -> ret_out
  ))
  buf.intr  := MuxCase(Reg(new exu_intr()), Array(
    clear       -> Reg(new exu_intr()),
    buf.valid   -> buf.intr,
    io.in.valid -> ret_intr
  ))
  buf.cmp  := MuxCase(false.B, Array(
    clear       -> false.B,
    buf.valid   -> buf.cmp,
    io.in.valid -> ret_cmp
  ))

  io.out.valid := valid & ~clear
  io.in.ready  := io.out.ready
  io.out.bits  := Mux(io.out.valid, buf.out, RegInit(Reg(new mem_info())))
  io.intr      := Mux(io.out.valid, buf.intr, RegInit(Reg(new exu_intr())))
  io.cmp       := Mux(io.out.valid, buf.cmp, false.B)

  val pc   = io.in.bits.pc

  val alu = Module(new alu())
  alu.io.in.alu_op := io.contr.alu_op
  alu.io.in.srca   := io.in.bits.srca
  alu.io.in.srcb   := io.in.bits.srcb

  ret_cmp := MuxLookup(Cat(io.contr.signed.asUInt(), io.contr.cmp), false.B, Array(
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

  ret_out.pc   := io.in.bits.pc
  ret_out.dest := Mux(io.contr.cmp === 0.U, alu.io.out.dest, ret_cmp.asUInt())
  ret_out.data := io.in.bits.data
  ret_out.rd   := io.in.bits.rd
  ret_out.hi   := Mux(io.contr.hilo_src, io.in.bits.srca, alu.io.out.dest_hi)
  ret_out.lo   := Mux(io.contr.hilo_src, io.in.bits.srca, alu.io.out.dest_lo)

  ret_intr.pc     := pc
  ret_intr.exceed := alu.io.out.exceed
}