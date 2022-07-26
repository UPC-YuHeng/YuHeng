import chisel3._
import chisel3.util._

class cpu extends Module {
  val io = IO(new Bundle {
    val int      = Input (UInt(6.W))
    val axi_in   = Input (new master_in())
    val axi_out  = Output(new master_out())
    val debug_wb = Output(new debug_io())
  })

  val mmu   = Module(new mmu())
  val contr = Module(new contr())
  val intr  = Module(new intr())

  val amu   = Module(new amu())
  val ifu   = Module(new ifu())
  val idu   = Module(new idu())
  val exu   = Module(new exu())
  val mem   = Module(new mem())
  val reg   = Module(new reg())

  // shaking hands
  val ifu_idu = ifu.io.out.valid & idu.io.in.ready
  val idu_exu = idu.io.out.valid & exu.io.in.ready
  val exu_mem = exu.io.out.valid & mem.io.in.ready
  val mem_reg = mem.io.out.valid & reg.io.memin.ready

  // mmu
  mmu.io.in        <> io.axi_in
  mmu.io.out       <> io.axi_out
  mmu.io.inst_sram <> ifu.io.ram
  mmu.io.data_sram <> mem.io.ram

  // contr
  contr.io.idu           := idu.io.contr
  contr.io.cmp           := exu.io.cmp
  contr.io.valid.idu_exu := idu_exu
  contr.io.valid.exu_mem := exu_mem
  contr.io.valid.mem_reg := mem_reg
  contr.io.conflict      := idu.io.conflict
  contr.io.intr          := intr.io.intr

  // intr
  intr.io.eint          := io.int
  intr.io.ifu           := ifu.io.intr
  intr.io.idu           := idu.io.intr
  intr.io.exu           := exu.io.intr
  intr.io.mem           := mem.io.intr
  intr.io.regout        := reg.io.introut
  intr.io.valid.ifu_idu := ifu_idu
  intr.io.valid.idu_exu := idu_exu
  intr.io.valid.exu_mem := exu_mem
  intr.io.branch        := contr.io.branch
  intr.io.lock          := contr.io.lock

  // amu
  amu.io.contr := contr.io.amu
  amu.io.intr  := intr.io.amu

  // ifu
  ifu.io.in    <> amu.io.out
  ifu.io.ram   <> mmu.io.inst_sram
  ifu.io.rdok  := mmu.io.ifu_ready
  ifu.io.clear := contr.io.clear | intr.io.intr
  ifu.io.rdata := mmu.io.inst_rdata
  // idu
  idu.io.in     <> ifu.io.out
  idu.io.rddata <> reg.io.iduout
  idu.io.clear  := intr.io.intr
  idu.io.lock   := contr.io.lock

  // exu
  exu.io.in    <> idu.io.out
  exu.io.contr := contr.io.exu
  exu.io.clear := intr.io.intr

  // mem
  mem.io.in    <> exu.io.out
  mem.io.rdok  := mmu.io.mem_ready
  mem.io.contr := contr.io.mem
  mem.io.clear := intr.io.intr
  mem.io.rdata := mmu.io.data_rdata

  // reg
  reg.io.memin  <> mem.io.out
  reg.io.iduin  := idu.io.rdinfo
  reg.io.intrin := intr.io.regin
  reg.io.contr  := contr.io.reg
  reg.io.intr   := intr.io.reg

  // debug_io
  io.debug_wb := reg.io.debug_wb
}
