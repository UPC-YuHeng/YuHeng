import chisel3._
import chisel3.util._

import ALUOperationList._

class alu extends Module {
  class alu_in extends Bundle {
    val alu_op = UInt(4.W)
    val srca   = UInt(32.W)
    val srcb   = UInt(32.W)
  }
  class alu_out extends Bundle {
    val dest    = UInt(32.W)
    val dest_hi = UInt(32.W)
    val dest_lo = UInt(32.W)
    val exceed  = Bool()
    val zero    = Bool()
    val signu   = Bool()
    val signs   = Bool()
  }
  val io = IO(new Bundle {
    val in  = Input(new alu_in())
    val out = Output(new alu_out())
  })

  val a = io.in.srca
  val b = io.in.srcb

  io.out.dest := MuxLookup(io.in.alu_op, 0.U, Array(
    alu_adds  -> (a + b),
    alu_addu  -> (a + b),
    alu_subs  -> (a - b),
    alu_subu  -> (a - b),
    alu_and   -> (a & b),
    alu_xor   -> (a ^ b),
    alu_nor   -> (~(a | b)),
    alu_or    -> (a | b),
    alu_sftrs -> ((a.asSInt() >> b(4,0)).asUInt()),
    alu_sftru -> (a >> b(4,0)),
    alu_sftl  -> (a << b(4,0))
  ))

  io.out.dest_hi := MuxLookup(io.in.alu_op, 0.U, Array(
    alu_mults -> (a.asSInt() * b.asSInt()).asUInt()(63, 32),
    alu_multu -> ((a * b)(63, 32)),
    alu_divs  -> (a.asSInt() % b.asSInt()).asUInt(),
    alu_divu  -> (a % b)
  ))

  io.out.dest_lo := MuxLookup(io.in.alu_op, 0.U, Array(
    alu_mults -> (a.asSInt() * b.asSInt()).asUInt()(31, 0),
    alu_multu -> ((a * b)(31, 0)),
    alu_divs  -> (a.asSInt() / b.asSInt()).asUInt(),
    alu_divu  -> (a / b)
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
}
