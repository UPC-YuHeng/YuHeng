import chisel3._
import chisel3.util._

object ALUOperationList {
  val alu_adds  = 0.U
  val alu_addu  = 1.U
  val alu_subs  = 2.U
  val alu_subu  = 3.U
  val alu_mults = 4.U
  val alu_multu = 5.U
  val alu_divs  = 6.U
  val alu_divu  = 7.U
  val alu_and   = 8.U
  val alu_xor   = 9.U
  val alu_nor   = 10.U
  val alu_or    = 11.U
  val alu_sftrs = 12.U
  val alu_sftru = 13.U
  val alu_sftl  = 14.U
  val alu_nop   = 15.U
}

import ALUOperationList._

class alu_in extends Bundle {
  val alu_op = UInt(4.W)
  val srca   = UInt(32.W)
  val srcb   = UInt(32.W)
}

class alu_out extends Bundle {
  val dest    = UInt(32.W)
  val dest_hi = UInt(32.W)
  val dest_lo = UInt(32.W)
  val exceed  = UInt(1.W)
  val zero    = UInt(1.W)
  val signu   = UInt(1.W)
  val signs   = UInt(1.W)
}

class alu extends Module {
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
    alu_sftrs -> ((a.asSInt() >> b).asUInt()),
    alu_sftru -> (a >> b),
    alu_sftl  -> (a << b)
  ))

  io.out.dest_hi := MuxLookup(io.in.alu_op, 0.U, Array(
    alu_mults -> (a.asSInt() * b.asSInt()).asUInt()(63,32),
    alu_multu -> ((a * b)(63,32)),
    alu_divs  -> (a.asSInt() / b.asSInt()).asUInt()(63,32),
    alu_divu  -> ((a / b)(63,32))
  ))

  io.out.dest_lo := MuxLookup(io.in.alu_op, 0.U, Array(
    alu_mults -> (a.asSInt() * b.asSInt()).asUInt()(31,0),
    alu_multu -> ((a * b)(31,0)),
    alu_divs  -> (a.asSInt() / b.asSInt()).asUInt()(31,0),
    alu_divu  -> ((a / b)(31,0))
  ))

  io.out.exceed := MuxLookup(io.in.alu_op, 0.U, Array(
    alu_adds  -> ((Cat(a(31),a) + Cat(b(31),b))(32) =/= (Cat(a(31),a) + Cat(b(31),b))(31)),
    alu_subs  -> ((Cat(a(31),a) - Cat(b(31),b))(32) =/= (Cat(a(31),a) - Cat(b(31),b))(31))
  ))

  io.out.zero := (io.out.dest === 0.U).asUInt()

  io.out.signs := MuxLookup(Cat(a(31),b(31)), 0.U, Array(
    "b10".U -> 0.U,
    "b01".U -> 1.U,
    "b00".U -> ~io.out.dest(31),
    "b11".U -> ~io.out.dest(31),
  ))

  io.out.signu := MuxLookup(Cat(a(31),b(31)), 0.U, Array(
    "b10".U -> 1.U,
    "b01".U -> 0.U,
    "b00".U -> ~io.out.dest(31),
    "b11".U -> ~io.out.dest(31),
  ))

}