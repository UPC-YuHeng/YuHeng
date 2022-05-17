import chisel3._
import chisel3.util._

class branch_in extends Bundle {
  val pc     = UInt(32.W)
  val branch = Bool()
}

class branch_out extends Bundle {
  val pc = UInt(32.W)
}

class branch extends Module {
  val io = IO(new Bundle {
    val in  = Input(new branch_in())
    val out = Output(new branch_out())
  })
}