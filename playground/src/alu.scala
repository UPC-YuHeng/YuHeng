import chisel3._
import chisel3.util._

import ALUOperationList._

class alu extends Module {
  class alu_in extends Bundle {
    val valid  = Bool()
    val alu_op = UInt(4.W)
    val srca   = UInt(32.W)
    val srcb   = UInt(32.W)
  }
  class alu_out extends Bundle {
    val valid   = Bool()
    val dest    = UInt(32.W)
    val dest_hi = UInt(32.W)
    val dest_lo = UInt(32.W)
    val exceed  = Bool()
    val zero    = Bool()
    val signu   = Bool()
    val signs   = Bool()
  }
  val io = IO(new Bundle {
    val in    = Input (new alu_in())
    val out   = Output(new alu_out())
    val flush = Input (Bool())
  })

  val mult    = Module(new mult())
  val div     = Module(new div())

  val is_mult   = (io.in.alu_op === alu_mults | io.in.alu_op === alu_multu)
  val is_div    = (io.in.alu_op === alu_divs  | io.in.alu_op === alu_divu)
  val is_signed = (io.in.alu_op === alu_mults | io.in.alu_op === alu_divs)

  val a = io.in.srca
  val b = io.in.srcb

  // normal alu

  io.out.dest := MuxLookup(io.in.alu_op, 0.U, Array(
    alu_adds  -> (a + b),
    alu_addu  -> (a + b),
    alu_subs  -> (a - b),
    alu_subu  -> (a - b),
    alu_and   -> (a & b),
    alu_xor   -> (a ^ b),
    alu_nor   -> (~(a | b)),
    alu_or    -> (a | b),
    alu_sftrs -> ((a.asSInt() >> b(4, 0)).asUInt()),
    alu_sftru -> (a >> b(4,0)),
    alu_sftl  -> (a << b(4,0))
  ))
  io.out.exceed := MuxLookup(io.in.alu_op, 0.U, Array(
    alu_adds  -> ((Cat(a(31), a) + Cat(b(31), b))(32) =/= (Cat(a(31), a) + Cat(b(31), b))(31)),
    alu_subs  -> ((Cat(a(31), a) - Cat(b(31), b))(32) =/= (Cat(a(31), a) - Cat(b(31), b))(31))
  ))
  io.out.zero := (io.out.dest === 0.U).asUInt()
  io.out.signs := MuxLookup(Cat(a(31), b(31)), 0.U, Array(
    "b10".U -> 0.U,
    "b01".U -> 1.U,
    "b00".U -> ~io.out.dest(31),
    "b11".U -> ~io.out.dest(31),
  ))
  io.out.signu := MuxLookup(Cat(a(31), b(31)), 0.U, Array(
    "b10".U -> 1.U,
    "b01".U -> 0.U,
    "b00".U -> ~io.out.dest(31),
    "b11".U -> ~io.out.dest(31),
  ))

  // mult
  mult.io.in.valid  := io.in.valid & is_mult
  mult.io.in.x      := a
  mult.io.in.y      := b
  mult.io.in.signed := is_signed

  // div
  div.io.in.valid  := io.in.valid & is_div
  div.io.flush     := io.flush
  div.io.in.x      := a
  div.io.in.y      := b
  div.io.in.signed := is_signed

  // hi & lo
  io.out.dest_hi := MuxCase(0.U, Array(
    mult.io.out.valid -> mult.io.out.h,
    div.io.out.valid  -> div.io.out.r
  ))
  io.out.dest_lo := MuxCase(0.U, Array(
    mult.io.out.valid -> mult.io.out.l,
    div.io.out.valid  -> div.io.out.s
  ))

  io.out.valid := MuxCase(false.B, Array(
    io.flush                            -> false.B,
    (io.in.valid & ~(is_mult | is_div)) -> true.B,
    mult.io.in.valid                    -> false.B,
    mult.io.out.valid                   -> true.B,
    div.io.in.valid                     -> false.B,
    div.io.out.valid                    -> true.B
  ))
}
