import chisel3._
import chisel3.util._

class contr extends Module {
  class valid_data extends Bundle {
    val idu_exu = Bool()
    val exu_mem = Bool()
    val mem_reg = Bool()
  }
  val io = IO(new Bundle {
    val valid    = Input (new valid_data())
    val conflict = Input (new conflict_data())
    val amu      = Output(new amu_contr())
    val idu      = Input (new inst_contr())
    val exu      = Output(new exu_contr())
    val mem      = Output(new mem_contr())
    val reg      = Output(new reg_contr())
    val cmp      = Input (Bool())
    val intr     = Input (Bool())
    val branch   = Output(Bool())
    val lock     = Output(Bool())
    val clear    = Output(Bool())
  })

  // contr
  val idu = io.idu
  val exu = RegInit(Reg(new inst_contr()))
  val mem = RegInit(Reg(new inst_contr()))
  val reg = RegInit(Reg(new inst_contr()))

  // conflict
  val cidu = io.conflict
  val cexu = RegInit(Reg(new conflict_data()))
  val cmem = RegInit(Reg(new conflict_data()))
  val creg = RegInit(Reg(new conflict_data()))
  // conflict (write-read)
  val conflict_exu = exu.reg_write & (cidu.rs === cexu.rd | cidu.rt === cexu.rd)
  val conflict_mem = mem.reg_write & (cidu.rs === cmem.rd | cidu.rt === cmem.rd)
  val conflict_reg = reg.reg_write & (cidu.rs === creg.rd | cidu.rt === creg.rd)
  val conflict     = conflict_exu | conflict_mem | conflict_reg
  io.lock         := conflict

  // exu := MuxCase(Reg(new inst_contr()), Array(
  //   io.intr          -> Reg(new inst_contr()),
  //   io.valid.idu_exu -> idu
  // ))
  // mem := MuxCase(Reg(new inst_contr()), Array(
  //   io.intr          -> Reg(new inst_contr()),
  //   io.valid.exu_mem -> exu
  // ))
  // reg := MuxCase(Reg(new inst_contr()), Array(
  //   io.intr          -> Reg(new inst_contr()),
  //   io.valid.mem_reg -> mem
  // ))
  
  exu := MuxCase(exu, Array(
    io.intr          -> Reg(new inst_contr()),
    io.valid.idu_exu -> idu,
    io.valid.exu_mem -> Reg(new inst_contr())
  ))
  mem := MuxCase(mem, Array(
    io.intr          -> Reg(new inst_contr()),
    io.valid.exu_mem -> exu,
    io.valid.mem_reg -> Reg(new inst_contr())
  ))
  reg := MuxCase(Reg(new inst_contr()), Array(
    io.intr          -> Reg(new inst_contr()),
    io.valid.mem_reg -> mem
  ))

  // cexu := Mux(io.valid.exu_mem, cidu, cexu)
  cexu := MuxCase(cexu, Array(
    io.intr          -> Reg(new conflict_data()),
    io.valid.idu_exu -> cidu,
    io.valid.mem_reg -> Reg(new conflict_data())
  ))
  cmem := MuxCase(cmem, Array(
    io.intr          -> Reg(new conflict_data()),
    io.valid.exu_mem -> cexu,
    io.valid.mem_reg -> Reg(new conflict_data())
  ))
  creg := MuxCase(Reg(new conflict_data()), Array(
    io.intr          -> Reg(new conflict_data()),
    io.valid.mem_reg -> cmem
  ))

  // amu
  io.amu.jump     := idu.jump
  io.amu.jaddr    := idu.jaddr
  io.amu.branch   := exu.branch & io.cmp
  io.amu.baddr    := exu.baddr

  // exu
  io.exu.alu_op   := Mux(io.valid.idu_exu, idu.alu_op, exu.alu_op)
  io.exu.cmp      := Mux(io.valid.idu_exu, idu.cmp, exu.cmp)
  io.exu.signed   := Mux(io.valid.idu_exu, idu.signed, exu.signed)
  io.exu.hilo_src := Mux(io.valid.idu_exu, idu.hilo_src, exu.hilo_src)

  // mem
  io.mem.mem_read  := Mux(io.valid.exu_mem, exu.mem_read, mem.mem_read)
  io.mem.mem_write := Mux(io.valid.exu_mem, exu.mem_write, mem.mem_write)
  io.mem.mem_mask  := Mux(io.valid.exu_mem, exu.mem_mask, mem.mem_mask)
  io.mem.signed    := Mux(io.valid.exu_mem, exu.signed, mem.signed)

  // reg
  io.reg.reg_write := Mux(io.valid.mem_reg, mem.reg_write, false.B)
  io.reg.hi_read   := Mux(io.valid.mem_reg, mem.hi_read, false.B)
  io.reg.lo_read   := Mux(io.valid.mem_reg, mem.lo_read, false.B)
  io.reg.hi_write  := Mux(io.valid.mem_reg, mem.hi_write, false.B)
  io.reg.lo_write  := Mux(io.valid.mem_reg, mem.lo_write, false.B)
  io.reg.link      := Mux(io.valid.mem_reg, mem.link, false.B)

  // branch_slot
  io.branch := io.idu.jump | io.idu.branch

  // clear ifu
  io.clear := io.amu.branch
}
