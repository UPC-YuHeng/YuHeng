import chisel3._
import chisel3.util._

class cpu extends Module {
  class ram_io extends Bundle {
    val en    = Output(Bool())
    val wen   = Output(UInt(4.W))
    val addr  = Output(UInt(32.W))
    val wdata = Output(UInt(32.W))
    val rdata = Input(UInt(32.W))
  }
  class debug_io extends Bundle {
    val pc       = Output(UInt(32.W))
    val rf_wen   = Output(UInt(4.W))
    val rf_wnum  = Output(UInt(5.W))
    val rf_wdata = Output(UInt(32.W))
  }
  val io = IO(new Bundle {
    val int       = Input(UInt(6.W))
    val inst_sram = new ram_io()
    val data_sram = new ram_io()
    val debug_wb  = new debug_io()
  })

  val pc = RegInit("hbfc00000".U(32.W))

  val ifu     = Module(new ifu())
  val ifu_idu = Module(new ifu_idu())
  val idu     = Module(new idu())
  val idu_exu = Module(new idu_exu())
  val exu     = Module(new exu())
  val exu_mem = Module(new exu_mem())
  val mem     = Module(new mem())
  val mem_reg = Module(new mem_reg())
  val reg     = Module(new reg())
  val branch  = Module(new branch())
  val cp0     = Module(new cp0())

  val clear_ifu     = Wire(Bool())
  val pause         = Wire(Bool())
  val intr          = Wire(Bool())

  // ifu
  ifu.io.in.addr := Mux(pause, ifu_idu.io.out.pc, pc)
  ifu.io.ram     <> io.inst_sram

  // ifu_idu
  ifu_idu.io.pause               := pause & (~intr)
  ifu_idu.io.intr                := intr
  ifu_idu.io.in.valid            := ~(clear_ifu | intr)
  ifu_idu.io.in.pc               := pc
  ifu_idu.io.in.intr.inst_addrrd := ifu.io.intr.inst_addrrd

  // idu
  idu.io.in.inst := ifu.io.out.inst

  // idu_exu   
  idu_exu.io.pause               := pause & ~intr
  idu_exu.io.intr                := intr
  idu_exu.io.in.valid            := (ifu_idu.io.out.valid & ~intr & ~idu_exu.io.out.intr.eret)
  idu_exu.io.in.pc               := ifu_idu.io.out.pc
  idu_exu.io.in.data.inst        := ifu.io.out.inst
  idu_exu.io.in.data.rs          := idu.io.out.rs
  idu_exu.io.in.data.rt          := idu.io.out.rt
  idu_exu.io.in.data.rd          := idu.io.out.rd
  idu_exu.io.in.data.cp0_addr    := idu.io.out.cp0_addr
  idu_exu.io.in.data.imm         := idu.io.out.imm
  idu_exu.io.in.contr.alu_op     := idu.io.contr.alu_op
  idu_exu.io.in.contr.alu_src    := idu.io.contr.alu_src
  idu_exu.io.in.contr.reg_write  := idu.io.contr.reg_write
  idu_exu.io.in.contr.hi_write   := idu.io.contr.hi_write 
  idu_exu.io.in.contr.lo_write   := idu.io.contr.lo_write
  idu_exu.io.in.contr.hi_read    := idu.io.contr.hi_read
  idu_exu.io.in.contr.lo_read    := idu.io.contr.lo_read
  idu_exu.io.in.contr.hilo_src   := idu.io.contr.hilo_src
  idu_exu.io.in.contr.mem_read   := idu.io.contr.mem_read
  idu_exu.io.in.contr.mem_write  := idu.io.contr.mem_write
  idu_exu.io.in.contr.mem_mask   := idu.io.contr.mem_mask
  idu_exu.io.in.contr.branch     := idu.io.contr.branch
  idu_exu.io.in.contr.branch_delay := (idu_exu.io.out.contr.branch | idu_exu.io.out.contr.jump) & idu_exu.io.out.valid
  idu_exu.io.in.contr.cmp_op     := idu.io.contr.cmp_op
  idu_exu.io.in.contr.jump       := idu.io.contr.jump
  idu_exu.io.in.contr.jsrc       := idu.io.contr.jsrc
  idu_exu.io.in.contr.call_src   := idu.io.contr.call_src
  idu_exu.io.in.contr.signed     := idu.io.contr.signed
  idu_exu.io.in.contr.cp0_read   := idu.io.contr.cp0_read
  idu_exu.io.in.contr.cp0_write  := idu.io.contr.cp0_write
  idu_exu.io.in.intr.inst_addrrd := ifu_idu.io.out.intr.inst_addrrd
  idu_exu.io.in.intr.noinst      := idu.io.intr.noinst
  idu_exu.io.in.intr.breakpt     := idu.io.intr.breakpt
  idu_exu.io.in.intr.syscall     := idu.io.intr.syscall
  idu_exu.io.in.intr.eret        := idu.io.intr.eret

  // exu
  reg.io.in.rs_addr := idu_exu.io.out.data.rs
  reg.io.in.rt_addr := idu_exu.io.out.data.rt
  exu.io.in.alu_op  := idu_exu.io.out.contr.alu_op
  exu.io.in.cmp_op  := idu_exu.io.out.contr.cmp_op
  exu.io.in.signed  := idu_exu.io.out.contr.signed
  exu.io.in.srca    := reg.io.out.rs_data
  exu.io.in.srcb    := Mux(idu_exu.io.out.contr.alu_src, idu_exu.io.out.data.imm, reg.io.out.rt_data)
  
  // exu_mem
  exu_mem.io.pause              := pause
  exu_mem.io.intr               := intr
  exu_mem.io.in.valid           := idu_exu.io.out.valid & (~pause) & (~intr)
  exu_mem.io.in.pc              := idu_exu.io.out.pc
  exu_mem.io.in.data.inst       := idu_exu.io.out.data.inst
  exu_mem.io.in.data.rd         := idu_exu.io.out.data.rd
  exu_mem.io.in.data.rs         := idu_exu.io.out.data.rs
  exu_mem.io.in.contr.mem_mask  := idu_exu.io.out.contr.mem_mask
  exu_mem.io.in.contr.cmp_op    := idu_exu.io.out.contr.cmp_op
  exu_mem.io.in.contr.reg_write := exu_mem.io.in.valid & idu_exu.io.out.contr.reg_write
  exu_mem.io.in.contr.hi_write  := exu_mem.io.in.valid & idu_exu.io.out.contr.hi_write
  exu_mem.io.in.contr.lo_write  := exu_mem.io.in.valid & idu_exu.io.out.contr.lo_write
  exu_mem.io.in.contr.hi_read   := exu_mem.io.in.valid & idu_exu.io.out.contr.hi_read
  exu_mem.io.in.contr.lo_read   := exu_mem.io.in.valid & idu_exu.io.out.contr.lo_read
  exu_mem.io.in.contr.hilo_src  := exu_mem.io.in.valid & idu_exu.io.out.contr.hilo_src
  exu_mem.io.in.contr.mem_read  := exu_mem.io.in.valid & idu_exu.io.out.contr.mem_read
  exu_mem.io.in.contr.mem_write := exu_mem.io.in.valid & idu_exu.io.out.contr.mem_write
  exu_mem.io.in.contr.branch    := exu_mem.io.in.valid & idu_exu.io.out.contr.branch
  exu_mem.io.in.contr.jump      := exu_mem.io.in.valid & idu_exu.io.out.contr.jump
  exu_mem.io.in.contr.jsrc      := exu_mem.io.in.valid & idu_exu.io.out.contr.jsrc
  exu_mem.io.in.contr.call_src  := exu_mem.io.in.valid & idu_exu.io.out.contr.call_src
  exu_mem.io.in.contr.signed    := exu_mem.io.in.valid & idu_exu.io.out.contr.signed
  exu_mem.io.in.contr.cp0_read  := exu_mem.io.in.valid & idu_exu.io.out.contr.cp0_read
  exu_mem.io.in.contr.cp0_write := exu_mem.io.in.valid & idu_exu.io.out.contr.cp0_write
  exu_mem.io.in.data.dest       := exu.io.out.dest
  exu_mem.io.in.data.dest_hi    := exu.io.out.dest_hi
  exu_mem.io.in.data.dest_lo    := exu.io.out.dest_lo
  exu_mem.io.in.data.cmp        := exu.io.out.cmp
  exu_mem.io.in.data.rt_data    := reg.io.out.rt_data
  exu_mem.io.in.data.cp0_data   := cp0.io.out.data

  // mem
  mem.io.exu_mem_valid   := exu_mem.io.in.valid
  mem.io.idu_exu_valid   := idu_exu.io.out.valid
  mem.io.pause           := pause
  mem.io.in.addr         := exu.io.out.dest
  mem.io.in.data         := reg.io.out.rt_data
  mem.io.contr.mem_read  := idu_exu.io.out.contr.mem_read
  mem.io.contr.mem_write := idu_exu.io.out.contr.mem_write
  mem.io.contr.mem_mask  := idu_exu.io.out.contr.mem_mask
  mem.io.ram             <> io.data_sram

  // mem_reg
  mem_reg.io.pause              := false.B
  mem_reg.io.intr               := intr
  mem_reg.io.in.valid           := exu_mem.io.out.valid
  mem_reg.io.in.pc              := exu_mem.io.out.pc
  mem_reg.io.in.data.inst       := exu_mem.io.out.data.inst
  mem_reg.io.in.data.rd         := exu_mem.io.out.data.rd
  mem_reg.io.in.data.rs         := exu_mem.io.out.data.rs
  mem_reg.io.in.data.dest       := exu_mem.io.out.data.dest
  mem_reg.io.in.data.dest_hi    := exu_mem.io.out.data.dest_hi
  mem_reg.io.in.data.dest_lo    := exu_mem.io.out.data.dest_lo
  mem_reg.io.in.data.rt_data    := exu_mem.io.out.data.rt_data
  mem_reg.io.in.data.cp0_data   := exu_mem.io.out.data.cp0_data
  mem_reg.io.in.data.rdata      := mem.io.out.data
  mem_reg.io.in.contr.branch    := exu_mem.io.out.contr.branch 
  mem_reg.io.in.contr.mem_read  := exu_mem.io.out.contr.mem_read
  mem_reg.io.in.contr.mem_mask  := exu_mem.io.out.contr.mem_mask
  mem_reg.io.in.contr.signed    := exu_mem.io.out.contr.signed
  mem_reg.io.in.contr.reg_write := exu_mem.io.out.contr.reg_write
  mem_reg.io.in.contr.hi_write  := exu_mem.io.out.contr.hi_write
  mem_reg.io.in.contr.lo_write  := exu_mem.io.out.contr.lo_write
  mem_reg.io.in.contr.hi_read   := exu_mem.io.out.contr.hi_read
  mem_reg.io.in.contr.lo_read   := exu_mem.io.out.contr.lo_read
  mem_reg.io.in.contr.hilo_src  := exu_mem.io.out.contr.hilo_src
  mem_reg.io.in.contr.cp0_read  := exu_mem.io.out.contr.cp0_read
  mem_reg.io.in.contr.cp0_write := exu_mem.io.out.contr.cp0_write
  mem_reg.io.in.contr.call_src  := exu_mem.io.out.contr.call_src

  // reg
  reg.io.in.reg_write  := mem_reg.io.out.contr.reg_write
  reg.io.in.rs_addr_hl := mem_reg.io.out.data.rs
  reg.io.in.rd_addr    := MuxLookup(Cat(mem_reg.io.out.contr.mem_read, mem_reg.io.out.contr.call_src), mem_reg.io.out.data.rd , Array(
    1.U -> 31.U
  ))
  // mem_reg.io.exu_data_out.dest is tlb.io.out.addr (with delay)
  reg.io.in.rd_data   := Mux(mem_reg.io.out.contr.mem_read,
    MuxLookup(mem_reg.io.out.contr.mem_mask, 0.U, Array(
      1.U -> MuxLookup(mem_reg.io.out.data.dest(1, 0), 0.U, Array(
          "b00".U -> Cat(Fill(24, mem_reg.io.out.data.rdata( 7) & mem_reg.io.out.contr.signed.asUInt()), mem_reg.io.out.data.rdata( 7,  0)),
          "b01".U -> Cat(Fill(24, mem_reg.io.out.data.rdata(15) & mem_reg.io.out.contr.signed.asUInt()), mem_reg.io.out.data.rdata(15,  8)),
          "b10".U -> Cat(Fill(24, mem_reg.io.out.data.rdata(23) & mem_reg.io.out.contr.signed.asUInt()), mem_reg.io.out.data.rdata(23, 16)),
          "b11".U -> Cat(Fill(24, mem_reg.io.out.data.rdata(31) & mem_reg.io.out.contr.signed.asUInt()), mem_reg.io.out.data.rdata(31, 24))
      )),
      2.U -> MuxLookup(mem_reg.io.out.data.dest(1, 0), 0.U, Array(
          "b00".U -> Cat(Fill(16, mem_reg.io.out.data.rdata(15) & mem_reg.io.out.contr.signed.asUInt()), mem_reg.io.out.data.rdata(15,  0)),
          "b10".U -> Cat(Fill(16, mem_reg.io.out.data.rdata(31) & mem_reg.io.out.contr.signed.asUInt()), mem_reg.io.out.data.rdata(31, 16))
      )),
      3.U -> mem_reg.io.out.data.rdata
    )),
    Mux(mem_reg.io.out.contr.call_src,
      mem_reg.io.out.pc + 8.U,
      mem_reg.io.out.data.dest
    )
  )

  // hi/lo
  reg.io.in.hi_write  := mem_reg.io.out.contr.hi_write
  reg.io.in.lo_write  := mem_reg.io.out.contr.lo_write
  reg.io.in.hi_read   := mem_reg.io.out.contr.hi_read
  reg.io.in.lo_read   := mem_reg.io.out.contr.lo_read
  reg.io.in.hilo_src  := mem_reg.io.out.contr.hilo_src
  reg.io.in.hi_data   := mem_reg.io.out.data.dest_hi
  reg.io.in.lo_data   := mem_reg.io.out.data.dest_lo
  // cp0
  reg.io.in.cp0_read  := mem_reg.io.out.contr.cp0_read
  reg.io.in.cp0_data  := mem_reg.io.out.data.cp0_data

  // cp0
  cp0.io.in.write := idu_exu.io.out.contr.cp0_write & exu_mem.io.in.valid
  cp0.io.in.addr  := idu_exu.io.out.data.cp0_addr
  cp0.io.in.sel   := idu_exu.io.out.data.inst(3, 0)
  cp0.io.in.data  := reg.io.out.rt_data
  cp0.io.in.int   := io.int
  cp0.io.in.vaddr := Mux(idu_exu.io.out.intr.inst_addrrd, idu_exu.io.out.pc, exu.io.out.dest)
  cp0.io.in.epc   := branch.io.out.epc
  // intr
  intr := ((cp0.io.out.ext_int  & (~cp0.io.out.exl) & (cp0.io.out.ie)) |
    idu_exu.io.out.intr.inst_addrrd | mem.io.intr.mem_addrrd | mem.io.intr.mem_addrwt |
    exu.io.intr.exceed |
    idu_exu.io.out.intr.syscall |
    idu_exu.io.out.intr.breakpt |
    idu_exu.io.out.intr.noinst  |
    (cp0.io.out.soft_int  & (~cp0.io.out.exl) & (cp0.io.out.ie))) & idu_exu.io.out.valid
  cp0.io.intr.intr    := intr
  cp0.io.intr.branch  := branch.io.out.branch_cp0 & idu_exu.io.out.valid
  cp0.io.intr.addrrd  := (idu_exu.io.out.intr.inst_addrrd | mem.io.intr.mem_addrrd) & idu_exu.io.out.valid
  cp0.io.intr.addrwt  := mem.io.intr.mem_addrwt  & idu_exu.io.out.valid
  cp0.io.intr.exceed  := exu.io.intr.exceed          & idu_exu.io.out.valid
  cp0.io.intr.syscall := idu_exu.io.out.intr.syscall & idu_exu.io.out.valid
  cp0.io.intr.breakpt := idu_exu.io.out.intr.breakpt & idu_exu.io.out.valid
  cp0.io.intr.noinst  := idu_exu.io.out.intr.noinst  & idu_exu.io.out.valid
  cp0.io.intr.eret    := idu_exu.io.out.intr.eret    & idu_exu.io.out.valid
  cp0.io.intr.soft_int:= cp0.io.out.soft_int         & idu_exu.io.out.valid
  cp0.io.intr.ext_int := cp0.io.out.ext_int          & idu_exu.io.out.valid

  // branch
  // branch & jump
  branch.io.in.pc           := pc
  branch.io.in.pc_exu       := idu_exu.io.out.pc
  branch.io.in.pc_idu       := ifu_idu.io.out.pc // TODO, May have error
  branch.io.in.branch_exu   := idu_exu.io.out.contr.branch & exu_mem.io.in.valid
  branch.io.in.branch_delay := idu_exu.io.out.contr.branch_delay
  branch.io.in.bcmp         := exu.io.out.cmp
  branch.io.in.jump         := idu_exu.io.out.contr.jump & exu_mem.io.in.valid
  branch.io.in.jsrc         := idu_exu.io.out.contr.jsrc & exu_mem.io.in.valid
  branch.io.in.imm          := idu_exu.io.out.data.imm
  branch.io.in.reg          := reg.io.out.rs_data
  // intr
  branch.io.intr.intr := intr
  branch.io.intr.eret := idu_exu.io.out.intr.eret
  branch.io.intr.epc  := cp0.io.out.epc

  clear_ifu := branch.io.out.branch_pc // when meeting branch clear the ifu
  pause := (((idu_exu.io.out.data.rs === mem_reg.io.out.data.rd) & mem_reg.io.out.valid & mem_reg.io.out.contr.reg_write) | 
            ((idu_exu.io.out.data.rs === exu_mem.io.out.data.rd) & exu_mem.io.out.valid & exu_mem.io.out.contr.reg_write) | 
            ((idu_exu.io.out.data.rt === mem_reg.io.out.data.rd) & mem_reg.io.out.valid & mem_reg.io.out.contr.reg_write) | 
            ((idu_exu.io.out.data.rt === exu_mem.io.out.data.rd) & exu_mem.io.out.valid & exu_mem.io.out.contr.reg_write)) & idu_exu.io.out.valid
  // next pc
  pc := Mux(branch.io.out.branch_pc, branch.io.out.pc,
    Mux(pause, pc, pc + "h4".U)
  )

  // io debug
  io.debug_wb.pc       := mem_reg.io.out.pc
  io.debug_wb.rf_wen   := Fill(4, mem_reg.io.out.contr.reg_write)
  io.debug_wb.rf_wnum  := reg.io.in.rd_addr
  io.debug_wb.rf_wdata := reg.io.out.rd_data
}
