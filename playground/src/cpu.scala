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
  val reg   = Module(new reg())
  val cp0   = Module(new cp0())

  val amu   = Module(new amu())
  val ifu   = Module(new ifu())
  val idu   = Module(new idu())
  val exu   = Module(new exu())
  val mem   = Module(new mem())

  // conflict
  val cidu = idu.io.conf
  val cexu = idu.io.out.bits.conf
  val cmem = exu.io.out.bits.conf
  val creg = mem.io.out.bits.conf
  val conflict_exu = cexu.rd.orR & (cidu.rs === cexu.rd | cidu.rt === cexu.rd)
  val conflict_mem = cmem.rd.orR & (cidu.rs === cmem.rd | cidu.rt === cmem.rd)
  val conflict_reg = creg.rd.orR & (cidu.rs === creg.rd | cidu.rt === creg.rd)
  val conflict     = conflict_exu | conflict_mem | conflict_reg

  // intr
  val eint = Mux(mem.io.out.valid & reg.io.in.ready,
    cp0.io.status.cp0_intr & (~cp0.io.status.exl) & cp0.io.status.ie,
    false.B
  )
  val tlb_intr = (
    mem.io.out.bits.intr.tlbs |
    mem.io.out.bits.intr.tlbl |
    mem.io.out.bits.intr.tlbd
  )
  val intr = (
    mem.io.out.bits.intr.instrd |
    mem.io.out.bits.intr.datard |
    mem.io.out.bits.intr.datawt |
    mem.io.out.bits.intr.syscall |
    mem.io.out.bits.intr.breakpt |
    mem.io.out.bits.intr.reserved |
    mem.io.out.bits.intr.exceed |
    tlb_intr |
    eint
  )

  // mmu
  mmu.io.inst_sram := ifu.io.rin
  mmu.io.data_sram := mem.io.rin
  mmu.io.in        := io.axi_in
  mmu.io.tlb_contr := cp0.io.tlb_contr
  cp0.io.tlb_data  := mmu.io.tlb_data
  io.axi_out       := mmu.io.out

  // amu
  amu.io.in.contr.branch := idu.io.out.bits.contr.branch & exu.io.cmp
  amu.io.in.contr.baddr  := idu.io.out.bits.contr.baddr
  amu.io.in.contr.jump   := idu.io.out.bits.contr.jump
  amu.io.in.contr.jaddr  := idu.io.out.bits.contr.jaddr
  amu.io.in.intr.intr    := intr
  amu.io.in.intr.refill  := mem.io.out.bits.intr.refill
  amu.io.in.intr.eret    := mem.io.out.bits.intr.eret
  amu.io.in.intr.eaddr   := cp0.io.status.epc
  
  // ifu
  ifu.io.in     <> amu.io.out
  ifu.io.rout   := mmu.io.inst_out
  ifu.io.flush  := intr | tlb_intr | mem.io.out.bits.intr.eret
  ifu.io.tlb_intr := mmu.io.tlb_ifu_intr

  // idu
  idu.io.in     <> ifu.io.out
  idu.io.regout <> reg.io.outa
  idu.io.lock   := conflict
  idu.io.flush  := intr | mem.io.out.bits.intr.eret

  // exu
  exu.io.in     <> idu.io.out
  exu.io.flush  := intr | mem.io.out.bits.intr.eret

  // mem
  mem.io.in     <> exu.io.out
  mem.io.rout   <> mmu.io.data_out
  mem.io.flush  := intr | mem.io.out.bits.intr.eret
  mem.io.tlb_intr := mmu.io.tlb_mem_intr

  // reg
  reg.io.ina      := idu.io.regin
  reg.io.inb.rt   := mem.io.out.bits.conf.rt
  reg.io.in       <> mem.io.out
  reg.io.cp0_data := cp0.io.out.data
  reg.io.flush    := intr | mem.io.out.bits.intr.eret

  val branch_delay = RegEnable(
    mem.io.out.bits.contr.jump | mem.io.out.bits.contr.branch,
    false.B,
    mem.io.out.valid & reg.io.in.ready
  )
  // cp0
  cp0.io.in.rd         := mem.io.out.bits.conf.rs       // decoding rs -> rd while dealing with cp0
  cp0.io.in.sel        := 0.U                           // no use for now
  cp0.io.in.data       := reg.io.outb.rt
  cp0.io.contr.write   := mem.io.out.bits.contr.cp0_write
  cp0.io.contr.tlbr    := mem.io.out.bits.contr.tlbr
  cp0.io.contr.tlbp    := mem.io.out.bits.contr.tlbp
  cp0.io.contr.tlbwi   := mem.io.out.bits.contr.tlbwi
  cp0.io.intr.intr     := intr
  cp0.io.intr.eint     := io.int
  cp0.io.intr.branch   := branch_delay
  cp0.io.intr.addrrd   := mem.io.out.bits.intr.instrd | mem.io.out.bits.intr.datard
  cp0.io.intr.addrwt   := mem.io.out.bits.intr.datawt
  cp0.io.intr.exceed   := mem.io.out.bits.intr.exceed
  cp0.io.intr.syscall  := mem.io.out.bits.intr.syscall
  cp0.io.intr.breakpt  := mem.io.out.bits.intr.breakpt
  cp0.io.intr.reserved := mem.io.out.bits.intr.reserved
  cp0.io.intr.eret     := mem.io.out.bits.intr.eret
  cp0.io.intr.epc      := Mux(branch_delay, mem.io.out.bits.data.pc - 4.U, mem.io.out.bits.data.pc)
  cp0.io.intr.vaddr    := mem.io.out.bits.intr.vaddr
  cp0.io.intr.tlbs     := mem.io.out.bits.intr.tlbs
  cp0.io.intr.tlbl     := mem.io.out.bits.intr.tlbl
  cp0.io.intr.tlbd     := mem.io.out.bits.intr.tlbd
  cp0.io.intr.tlb_vaddr:= mem.io.out.bits.intr.tlb_vaddr
  cp0.io.intr.tlb_vpn2 := mem.io.out.bits.intr.tlb_vpn2
  
  // debug_io
  io.debug_wb   := reg.io.debug_wb
}
