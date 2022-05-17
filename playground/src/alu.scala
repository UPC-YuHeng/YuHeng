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
}

class alu extends Module {
  val io = IO(new Bundle {
    val in  = Input(new alu_in())
    val out = Output(new alu_out())
  })

  val status = ListLookup(io.in.alu_op, List(), Array(
    alu_adds  -> List(io.in.srca + io.in.srcb, 0.U, 0.U),
    alu_addu  -> List(io.in.srca + io.in.srcb, 0.U, 0.U),
    alu_subs  -> List(io.in.srca - io.in.srcb, 0.U, 0.U),
    alu_subu  -> List(io.in.srca - io.in.srcb, 0.U, 0.U),
    alu_mults -> List(0.U, (io.in.srca * io.in.srcb)(63,32), (io.in.srca * io.in.srcb)(31,0)),
    alu_multu -> List(0.U, (Cat(0.U(1.W), io.in.srca) * Cat(0.U(1.W), io.in.srca))(63,32), (Cat(0.U(1.W), io.in.srca) * Cat(0.U(1.W), io.in.srca))(31,0)),
    alu_divs  -> List(0.U, (io.in.srca % io.in.srcb)(63,32), (io.in.srca / io.in.srcb)(31,0)),
    alu_divu  -> List(0.U, (Cat(0.U(1.W), io.in.srca) % Cat(0.U(1.W), io.in.srca))(63,32), (Cat(0.U(1.W), io.in.srca) / Cat(0.U(1.W), io.in.srca))(31,0)),
    alu_and   -> List(io.in.srca & io.in.srcb, 0.U, 0.U),
    alu_xor   -> List(io.in.srca ^ io.in.srcb, 0.U, 0.U),
    alu_nor   -> List(~io.in.srca, 0.U, 0.U),
    alu_or    -> List(io.in.srca | io.in.srcb, 0.U, 0.U),
    alu_sftrs -> List((Fill(32, io.in.srca(31)) << (32.U - io.in.srcb)) | (io.in.srca >> io.in.srcb), 0.U, 0.U),
    alu_sftru -> List(io.in.srca >> io.in.srcb, 0.U, 0.U),
    alu_sftl  -> List(io.in.srca << io.in.srcb, 0.U, 0.U),
    alu_nop   -> List(0.U, 0.U, 0.U)
  ))

  io.out.dest    := status(0);
  io.out.dest_hi := status(1);
  io.out.dest_lo := status(2);
}