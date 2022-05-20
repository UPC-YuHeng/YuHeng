import chisel3._
import chisel3.util._

class branch extends Module {
  class branch_in extends Bundle {
    val pc     = UInt(32.W)
    val branch = Bool()
    val bcmp   = Bool()
    val jump   = Bool()
    val jsrc   = Bool()
    val imm    = UInt(32.W)
    val reg    = UInt(32.W)
  }
  class branch_out extends Bundle {
    val pc     = UInt(32.W)
    val epc    = UInt(32.W)
    val branch = Bool()
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

  val bran_pipe = Module(new Pipe(Bool()))
  bran_pipe.io.enq.bits  := io.in.branch
  bran_pipe.io.enq.valid := true.B

  val bcmp_pipe = Module(new Pipe(Bool()))
  bcmp_pipe.io.enq.bits  := io.in.bcmp
  bcmp_pipe.io.enq.valid := true.B

  val jump_pipe = Module(new Pipe(Bool()))
  jump_pipe.io.enq.bits  := io.in.jump
  jump_pipe.io.enq.valid := true.B

  val jsrc_pipe = Module(new Pipe(Bool()))
  jsrc_pipe.io.enq.bits  := io.in.jsrc
  jsrc_pipe.io.enq.valid := true.B

  val imm_pipe = Module(new Pipe(UInt(32.W)))
  imm_pipe.io.enq.bits  := io.in.imm
  imm_pipe.io.enq.valid := true.B

  val reg_pipe = Module(new Pipe(UInt(32.W)))
  reg_pipe.io.enq.bits  := io.in.reg
  reg_pipe.io.enq.valid := true.B

  val npc = Mux(jump_pipe.io.deq.bits,
    Mux(jsrc_pipe.io.deq.bits,
      reg_pipe.io.deq.bits,
      Cat(io.in.pc(31, 28), imm_pipe.io.deq.bits(25, 0), 0.U(2.W))
    ),
    Mux(bran_pipe.io.deq.bits & bcmp_pipe.io.deq.bits,
      io.in.pc + Cat(Fill(14, imm_pipe.io.deq.bits(15)), imm_pipe.io.deq.bits(15, 0), 0.U(2.W)),
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
  io.out.epc := Mux(bran_pipe.io.deq.bits, io.in.pc - 4.U, io.in.pc)

  io.out.branch := bran_pipe.io.deq.bits
}
