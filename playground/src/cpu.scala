import chisel3._
import chisel3.util._

import yuheng.debug.mem

class cpu extends Module {
  val io = IO(new Bundle {
    val int = Input(UInt(6.W))
  })

  val pc = RegInit("hbfc00000".U(32.W))

  val ifu    = Module(new ifu())
  val idu    = Module(new idu())
  val exu    = Module(new exu())
  val branch = Module(new branch())

  val reg = Module(new reg())
  val mem = Module(new mem())
  val cp0 = Module(new cp0())
  val tlb = Module(new tlb())
  
  // reg
  reg.io.in.reg_write := idu.io.contr.reg_write
  reg.io.in.rs_addr   := idu.io.out.rs
  reg.io.in.rt_addr   := idu.io.out.rt
  reg.io.in.rd_addr   := MuxLookup(Cat(idu.io.contr.mem_read, idu.io.contr.call_src), idu.io.out.rd, Array(
    1.U -> 31.U
  ))
  reg.io.in.rd_data   := Mux(idu.io.contr.mem_read,
    MuxLookup(idu.io.contr.mem_mask, 0.U, Array(
      1.U -> Cat(Fill(24, mem.io.rdata( 7) & idu.io.contr.signed.asUInt()), mem.io.rdata( 7, 0)),
      2.U -> Cat(Fill(16, mem.io.rdata(15) & idu.io.contr.signed.asUInt()), mem.io.rdata(15, 0)),
      3.U -> mem.io.rdata
    )),
    Mux(idu.io.contr.call_src,
      pc + 8.U,
      exu.io.out.dest
    )
  )
  // hi/lo
  reg.io.in.hi_write  := idu.io.contr.hi_write
  reg.io.in.lo_write  := idu.io.contr.lo_write
  reg.io.in.hi_read   := idu.io.contr.hi_read
  reg.io.in.lo_read   := idu.io.contr.lo_read
  reg.io.in.hilo_src  := idu.io.contr.hilo_src
  reg.io.in.hi_data   := exu.io.out.dest_hi
  reg.io.in.lo_data   := exu.io.out.dest_lo
  // cp0
  reg.io.in.cp0_read  := idu.io.contr.cp0_read
  reg.io.in.cp0_data  := cp0.io.out.data

  // mem
  mem.io.ren   := idu.io.contr.mem_read
  mem.io.wen   := idu.io.contr.mem_write
  mem.io.raddr := tlb.io.out.addr
  mem.io.waddr := tlb.io.out.addr
  mem.io.wdata := reg.io.out.rt_data
  mem.io.mask  := MuxLookup(idu.io.contr.mem_mask, 0.U, Array(
    1.U -> 0x1.U,
    2.U -> 0x3.U,
    3.U -> 0xf.U
  ))
  val mem_addrrd = (idu.io.contr.mem_read & MuxLookup(idu.io.contr.mem_mask, false.B, Array(
    2.U -> tlb.io.out.addr(0),
    3.U -> tlb.io.out.addr(1, 0).orR
  )))
  val mem_addrwt = (idu.io.contr.mem_write & MuxLookup(idu.io.contr.mem_mask, false.B, Array(
    2.U -> tlb.io.out.addr(0),
    3.U -> tlb.io.out.addr(1, 0).orR
  )))

  // cp0
  cp0.io.in.write := idu.io.contr.cp0_write
  cp0.io.in.addr  := idu.io.out.rd
  cp0.io.in.sel   := ifu.io.out.inst(3, 0)
  cp0.io.in.data  := reg.io.out.rt_data
  cp0.io.in.int   := io.int
  cp0.io.in.pc    := pc
  cp0.io.in.epc   := branch.io.intr.epc
  // intr
  val intr = (io.int.orR |
    ifu.io.intr.addrrd | mem_addrrd |
    mem_addrwt |
    exu.io.intr.exceed |
    idu.io.intr.syscall |
    idu.io.intr.breakpt |
    idu.io.intr.resinst)
  cp0.io.intr.intr    := intr
  cp0.io.intr.branch  := branch.io.out.branch
  cp0.io.intr.addrrd  := ifu.io.intr.addrrd | mem_addrrd
  cp0.io.intr.addrwt  := mem_addrwt
  cp0.io.intr.exceed  := exu.io.intr.exceed
  cp0.io.intr.syscall := idu.io.intr.syscall
  cp0.io.intr.breakpt := idu.io.intr.breakpt
  cp0.io.intr.resinst := idu.io.intr.resinst
  cp0.io.intr.eret    := idu.io.intr.eret

  // tlb
  tlb.io.in.addr := exu.io.out.dest

  // ifu
  ifu.io.in.addr := pc

  // idu
  idu.io.in.inst := ifu.io.out.inst

  // exu
  exu.io.in.alu_op := idu.io.contr.alu_op
  exu.io.in.cmp_op := idu.io.contr.cmp_op
  exu.io.in.signed := idu.io.contr.signed
  exu.io.in.srca   := reg.io.out.rs_data
  exu.io.in.srcb   := Mux(idu.io.contr.alu_src, idu.io.out.imm, reg.io.out.rt_data)

  // branch
  // branch & jump
  branch.io.in.pc     := pc
  branch.io.in.branch := idu.io.contr.branch
  branch.io.in.bcmp   := exu.io.out.cmp
  branch.io.in.jump   := idu.io.contr.jump
  branch.io.in.jsrc   := idu.io.contr.jsrc
  branch.io.in.imm    := idu.io.out.imm
  branch.io.in.reg    := reg.io.out.rs_data
  // intr
  branch.io.intr.intr := intr
  branch.io.intr.eret := idu.io.intr.eret
  branch.io.intr.epc  := cp0.io.out.epc
  // next pc
  pc                  := branch.io.out.pc
}
