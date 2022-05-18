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
    val pc = UInt(32.W)
  }
  val io = IO(new Bundle {
    val in  = Input(new branch_in())
    val out = Output(new branch_out())
  })

  val b_pipe = Module(new Pipe(Bool()))
  b_pipe.io.enq.bits := io.in.branch & io.in.bcmp
  b_pipe.io.enq.valid := true.B

  val jump_pipe = Module(new Pipe(Bool()))
  jump_pipe.io.enq.bits := io.in.jump
  jump_pipe.io.enq.valid := true.B

  val jsrc_pipe = Module(new Pipe(Bool()))
  jsrc_pipe.io.enq.bits := io.in.jump
  jsrc_pipe.io.enq.valid := true .B

  val imm_pipe = Module(new Pipe(UInt(32.W)))
  imm_pipe.io.enq.bits := io.in.imm
  imm_pipe.io.enq.valid := true.B

  val reg_pipe = Module(new Pipe(UInt(32.W)))
  reg_pipe.io.enq.bits := io.in.reg
  reg_pipe.io.enq.valid := true.B

  io.out.pc := Mux(jump_pipe.io.deq.bits,
    Mux(jsrc_pipe.io.deq.bits,
      Cat(io.in.pc(31, 28), imm_pipe.io.deq.bits(25, 0), 0.U(2.W)),
      reg_pipe.io.deq.bits
    ),
    Mux(b_pipe.io.deq.bits,
      io.in.pc + Cat(0.U(14.W), imm_pipe.io.deq.bits(15, 0), 0.U(2.W)),
      io.in.pc + 4.U
    )
  )
}
