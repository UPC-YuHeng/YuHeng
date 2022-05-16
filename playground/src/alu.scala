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

class alu_in extends Bundle {
  val aluop = UInt(4.U)
  val srca  = UInt(32.W)
  val srcb  = UInt(32.W)
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

  val status = ListLookup(io.in.aluop, List(), Array(
    alu_adds  -> List(),
    alu_addu  -> List(),
    alu_subs  -> List(),
    alu_subu  -> List(),
    alu_mults -> List(),
    alu_multu -> List(),
    alu_divs  -> List(),
    alu_divu  -> List(),
    alu_and   -> List(),
    alu_xor   -> List(),
    alu_nor   -> List(),
    alu_or    -> List(),
    alu_sftrs -> List(),
    alu_sftru -> List(),
    alu_sftl  -> List(),
    alu_nop   -> List()
  ))
}