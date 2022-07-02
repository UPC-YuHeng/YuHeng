import chisel3._
import chisel3.util._

class branch extends Module {
  class branch_in extends Bundle {
    val pc          = UInt(32.W)
    val pc_idu      = UInt(32.W)
    val branch_exu  = Bool()
    val branch_mem  = Bool()
    val bcmp        = Bool()
    val jump        = Bool()
    val jsrc        = Bool()
    val imm         = UInt(32.W)
    val reg         = UInt(32.W)
  }
  class branch_out extends Bundle {
    val pc         = UInt(32.W)
    val epc        = UInt(32.W)
    val branch_cp0 = Bool()
    val branch_pc  = Bool()
  }
  class branch_intr extends Bundle {
    val intr = Bool()
    val eret = Bool()
    val epc  = UInt(32.W)
  }
  val io = IO(new Bundle {
    val in   = Input(new branch_in())
    val out  = Output(new branch_out())
    val intr = Input(new branch_intr())
  })
  
  val npc = Mux(io.in.jump,
    Mux(io.in.jsrc,
      io.in.reg,
      Cat(io.in.pc_idu(31, 28), io.in.imm(25, 0), 0.U(2.W))
    ),
    Mux(io.in.branch_exu & io.in.bcmp,
      io.in.pc_idu + Cat(Fill(14, io.in.imm(15)), io.in.imm(15, 0), 0.U(2.W)),
      io.in.pc + 4.U
    )
  )

  io.out.pc := Mux(io.intr.intr,
    "hbfc00380".U,
    Mux(io.intr.eret,
      io.intr.epc,
      npc
    )
  )
  io.out.epc := Mux(io.in.branch_mem, io.in.pc_idu - 4.U, io.in.pc_idu)

  io.out.branch_cp0 := io.in.branch_mem
  io.out.branch_pc  := (io.in.jump | (io.in.branch_exu & io.in.bcmp))
}
