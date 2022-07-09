import chisel3._
import chisel3.util._

class cpu extends Module {
  val io = IO(new Bundle {
    val int = Input(UInt(6.W))
    val inst_sram_addr  = Output(UInt(32.W))
    val inst_sram_wen   = Output(UInt(4.W))
    val inst_sram_en    = Output(Bool())
    val inst_sram_wdata = Output(UInt(32.W))
    val inst_sram_rdata = Input(UInt(32.W))

    val data_sram_addr  = Output(UInt(32.W))
    val data_sram_wen   = Output(UInt(4.W))
    val data_sram_en    = Output(Bool())
    val data_sram_wdata = Output(UInt(32.W))
    val data_sram_rdata = Input(UInt(32.W))

    val debug_wb_pc       = Output(UInt(32.W))
    val debug_wb_rf_wen   = Output(UInt(4.W))
    val debug_wb_rf_wnum  = Output(UInt(5.W))
    val debug_wb_rf_wdata = Output(UInt(32.W))
  })

  val pc = RegInit("hbfc00000".U(32.W))

  val ifu     = Module(new ifu())
  val ifu_idu = Module(new ifu_idu())
  val idu     = Module(new idu())
  val idu_exu = Module(new idu_exu())
  val exu     = Module(new exu())
  val exu_mem = Module(new exu_mem())
  val tlb     = Module(new tlb())
  val mem_reg = Module(new mem_reg())
  val reg     = Module(new reg())
  val branch  = Module(new branch())
  val cp0     = Module(new cp0())

  val clear_ifu     = Wire(Bool())
  val pause         = Wire(Bool())
  val intr          = Wire(Bool())

  // io debug
  io.debug_wb_pc       := mem_reg.io.ifu_data_out.pc
  io.debug_wb_rf_wen   := Fill(4,mem_reg.io.idu_contr_out.reg_write)
  io.debug_wb_rf_wnum  := reg.io.in.rd_addr
  io.debug_wb_rf_wdata := reg.io.out.rd_data

  // ifu
  ifu.io.in.addr     := Mux(pause, ifu_idu.io.ifu_data_out.pc, pc)
  io.inst_sram_addr  := ifu.io.inst_sram_addr
  io.inst_sram_wen   := "h0".U
  io.inst_sram_en    := true.B
  io.inst_sram_wdata := "h0".U

  // ifu_idu
  ifu_idu.io.pause            := pause & ~intr
  ifu_idu.io.valid            := ~(clear_ifu | intr)
  // ifu_idu.io.ifu_data_in.inst := ifu.io.out.inst
  ifu_idu.io.ifu_data_in.pc   := pc
  ifu_idu.io.intr_in.addrrd   := ifu.io.intr.addrrd

  // idu
  idu.io.in.inst := io.inst_sram_rdata

  // idu_exu   
  idu_exu.io.pause                     := pause & ~intr
  idu_exu.io.valid                     := (ifu_idu.io.valid_out & ~intr & ~idu_exu.io.intr_out.eret)
  idu_exu.io.ifu_data_in.pc            := ifu_idu.io.ifu_data_out.pc
  idu_exu.io.ifu_data_in.inst          := io.inst_sram_rdata
  idu_exu.io.idu_data_in.rs            := idu.io.out.rs
  idu_exu.io.idu_data_in.rt            := idu.io.out.rt
  idu_exu.io.idu_data_in.rd            := idu.io.out.rd
  idu_exu.io.idu_data_in.cp0_addr      := idu.io.out.cp0_addr
  idu_exu.io.idu_data_in.imm           := idu.io.out.imm
  idu_exu.io.idu_contr_in.alu_op       := idu.io.contr.alu_op
  idu_exu.io.idu_contr_in.alu_src      := idu.io.contr.alu_src
  idu_exu.io.idu_contr_in.reg_write    := idu.io.contr.reg_write
  idu_exu.io.idu_contr_in.hi_write     := idu.io.contr.hi_write 
  idu_exu.io.idu_contr_in.lo_write     := idu.io.contr.lo_write
  idu_exu.io.idu_contr_in.hi_read      := idu.io.contr.hi_read
  idu_exu.io.idu_contr_in.lo_read      := idu.io.contr.lo_read
  idu_exu.io.idu_contr_in.hilo_src     := idu.io.contr.hilo_src
  idu_exu.io.idu_contr_in.mem_read     := idu.io.contr.mem_read
  idu_exu.io.idu_contr_in.mem_write    := idu.io.contr.mem_write
  idu_exu.io.idu_contr_in.mem_mask     := idu.io.contr.mem_mask
  idu_exu.io.idu_contr_in.branch       := idu.io.contr.branch
  idu_exu.io.idu_contr_in.branch_delay := (idu_exu.io.idu_contr_out.branch | idu_exu.io.idu_contr_out.jump) & idu_exu.io.valid_out
  idu_exu.io.idu_contr_in.cmp_op       := idu.io.contr.cmp_op
  idu_exu.io.idu_contr_in.jump         := idu.io.contr.jump
  idu_exu.io.idu_contr_in.jsrc         := idu.io.contr.jsrc
  idu_exu.io.idu_contr_in.call_src     := idu.io.contr.call_src
  idu_exu.io.idu_contr_in.signed       := idu.io.contr.signed
  idu_exu.io.idu_contr_in.cp0_read     := idu.io.contr.cp0_read
  idu_exu.io.idu_contr_in.cp0_write    := idu.io.contr.cp0_write
  idu_exu.io.intr_in.addrrd            := ifu_idu.io.intr_out.addrrd
  idu_exu.io.intr_in.noinst            := idu.io.intr.noinst
  idu_exu.io.intr_in.breakpt           := idu.io.intr.breakpt
  idu_exu.io.intr_in.syscall           := idu.io.intr.syscall
  idu_exu.io.intr_in.eret              := idu.io.intr.eret

  // exu
  reg.io.in.rs_addr := idu_exu.io.idu_data_out.rs
  reg.io.in.rt_addr := idu_exu.io.idu_data_out.rt
  exu.io.in.alu_op  := idu_exu.io.idu_contr_out.alu_op
  exu.io.in.cmp_op  := idu_exu.io.idu_contr_out.cmp_op
  exu.io.in.signed  := idu_exu.io.idu_contr_out.signed
  exu.io.in.srca    := reg.io.out.rs_data
  exu.io.in.srcb    := Mux(idu_exu.io.idu_contr_out.alu_src, idu_exu.io.idu_data_out.imm, reg.io.out.rt_data)
  
  // exu_mem
  exu_mem.io.valid                  := idu_exu.io.valid_out & (~pause) & (~intr)
  exu_mem.io.ifu_data_in.pc         := idu_exu.io.ifu_data_out.pc
  exu_mem.io.ifu_data_in.inst       := idu_exu.io.ifu_data_out.inst
  exu_mem.io.idu_data_in.rd         := idu_exu.io.idu_data_out.rd
  exu_mem.io.idu_data_in.rs         := idu_exu.io.idu_data_out.rs
  exu_mem.io.idu_contr_in.mem_mask  := idu_exu.io.idu_contr_out.mem_mask
  exu_mem.io.idu_contr_in.cmp_op    := idu_exu.io.idu_contr_out.cmp_op
  exu_mem.io.idu_contr_in.reg_write := idu_exu.io.idu_contr_out.reg_write & exu_mem.io.valid
  exu_mem.io.idu_contr_in.hi_write  := idu_exu.io.idu_contr_out.hi_write  & exu_mem.io.valid
  exu_mem.io.idu_contr_in.lo_write  := idu_exu.io.idu_contr_out.lo_write  & exu_mem.io.valid
  exu_mem.io.idu_contr_in.hi_read   := idu_exu.io.idu_contr_out.hi_read   & exu_mem.io.valid
  exu_mem.io.idu_contr_in.lo_read   := idu_exu.io.idu_contr_out.lo_read   & exu_mem.io.valid
  exu_mem.io.idu_contr_in.hilo_src  := idu_exu.io.idu_contr_out.hilo_src  & exu_mem.io.valid
  exu_mem.io.idu_contr_in.mem_read  := idu_exu.io.idu_contr_out.mem_read  & exu_mem.io.valid
  exu_mem.io.idu_contr_in.mem_write := idu_exu.io.idu_contr_out.mem_write & exu_mem.io.valid
  exu_mem.io.idu_contr_in.branch    := idu_exu.io.idu_contr_out.branch    & exu_mem.io.valid
  exu_mem.io.idu_contr_in.jump      := idu_exu.io.idu_contr_out.jump      & exu_mem.io.valid
  exu_mem.io.idu_contr_in.jsrc      := idu_exu.io.idu_contr_out.jsrc      & exu_mem.io.valid
  exu_mem.io.idu_contr_in.call_src  := idu_exu.io.idu_contr_out.call_src  & exu_mem.io.valid
  exu_mem.io.idu_contr_in.signed    := idu_exu.io.idu_contr_out.signed    & exu_mem.io.valid
  exu_mem.io.idu_contr_in.cp0_read  := idu_exu.io.idu_contr_out.cp0_read  & exu_mem.io.valid
  exu_mem.io.idu_contr_in.cp0_write := idu_exu.io.idu_contr_out.cp0_write & exu_mem.io.valid
  exu_mem.io.exu_data_in.dest       := exu.io.out.dest
  exu_mem.io.exu_data_in.dest_hi    := exu.io.out.dest_hi
  exu_mem.io.exu_data_in.dest_lo    := exu.io.out.dest_lo
  exu_mem.io.exu_data_in.cmp        := exu.io.out.cmp
  exu_mem.io.exu_data_in.rt_data    := reg.io.out.rt_data
  exu_mem.io.exu_data_in.cp0_data   := cp0.io.out.data

  // tlb
  tlb.io.in.addr := exu.io.out.dest

  // mem
  io.data_sram_en   := ((idu_exu.io.idu_contr_out.mem_write | idu_exu.io.idu_contr_out.mem_read) & exu_mem.io.valid)
  io.data_sram_wen  := Mux(idu_exu.io.idu_contr_out.mem_write & exu_mem.io.valid,
    MuxLookup(idu_exu.io.idu_contr_out.mem_mask, 0.U, Array(
      1.U -> MuxLookup(tlb.io.out.addr(1, 0), 0.U, Array(
        "b00".U -> "b0001".U,
        "b01".U -> "b0010".U,
        "b10".U -> "b0100".U,
        "b11".U -> "b1000".U,
      )),
      2.U -> MuxLookup(tlb.io.out.addr(1, 0), 0.U, Array(
        "b00".U -> "b0011".U,
        "b10".U -> "b1100".U
      )),
      3.U -> 0xf.U
    )),
    0.U
  )
  io.data_sram_addr  := Cat(tlb.io.out.addr(31, 2), 0.U(2.W))
  io.data_sram_wdata := MuxLookup(idu_exu.io.idu_contr_out.mem_mask, 0.U, Array(
    1.U -> Fill(4, reg.io.out.rt_data( 7, 0)),
    2.U -> Fill(2, reg.io.out.rt_data(15, 0)),
    3.U -> reg.io.out.rt_data
  ))

  val mem_addrrd = (idu_exu.io.idu_contr_out.mem_read & (~pause) & idu_exu.io.valid_out & MuxLookup(idu_exu.io.idu_contr_out.mem_mask, false.B, Array(
    2.U -> tlb.io.out.addr(0),
    3.U -> tlb.io.out.addr(1, 0).orR
  )))
  val mem_addrwt = (idu_exu.io.idu_contr_out.mem_write & (~pause) & idu_exu.io.valid_out & MuxLookup(idu_exu.io.idu_contr_out.mem_mask, false.B, Array(
    2.U -> tlb.io.out.addr(0),
    3.U -> tlb.io.out.addr(1, 0).orR
  )))

  // mem_reg
  mem_reg.io.valid                  := exu_mem.io.valid_out
  mem_reg.io.ifu_data_in.pc         := exu_mem.io.ifu_data_out.pc
  mem_reg.io.ifu_data_in.inst       := exu_mem.io.ifu_data_out.inst
  mem_reg.io.idu_data_in.rd         := exu_mem.io.idu_data_out.rd
  mem_reg.io.idu_data_in.rs         := exu_mem.io.idu_data_out.rs
  mem_reg.io.idu_contr_in.branch    := exu_mem.io.idu_contr_out.branch 
  mem_reg.io.idu_contr_in.mem_read  := exu_mem.io.idu_contr_out.mem_read
  mem_reg.io.idu_contr_in.mem_mask  := exu_mem.io.idu_contr_out.mem_mask
  mem_reg.io.idu_contr_in.signed    := exu_mem.io.idu_contr_out.signed
  mem_reg.io.idu_contr_in.reg_write := exu_mem.io.idu_contr_out.reg_write
  mem_reg.io.idu_contr_in.hi_write  := exu_mem.io.idu_contr_out.hi_write
  mem_reg.io.idu_contr_in.lo_write  := exu_mem.io.idu_contr_out.lo_write
  mem_reg.io.idu_contr_in.hi_read   := exu_mem.io.idu_contr_out.hi_read
  mem_reg.io.idu_contr_in.lo_read   := exu_mem.io.idu_contr_out.lo_read
  mem_reg.io.idu_contr_in.hilo_src  := exu_mem.io.idu_contr_out.hilo_src
  mem_reg.io.idu_contr_in.cp0_read  := exu_mem.io.idu_contr_out.cp0_read
  mem_reg.io.idu_contr_in.cp0_write := exu_mem.io.idu_contr_out.cp0_write
  mem_reg.io.idu_contr_in.call_src  := exu_mem.io.idu_contr_out.call_src
  mem_reg.io.exu_data_in.dest       := exu_mem.io.exu_data_out.dest
  mem_reg.io.exu_data_in.dest_hi    := exu_mem.io.exu_data_out.dest_hi
  mem_reg.io.exu_data_in.dest_lo    := exu_mem.io.exu_data_out.dest_lo
  mem_reg.io.exu_data_in.rt_data    := exu_mem.io.exu_data_out.rt_data
  mem_reg.io.exu_data_in.cp0_data   := exu_mem.io.exu_data_out.cp0_data
  mem_reg.io.mem_data_in.rdata      := io.data_sram_rdata

  // reg
  reg.io.in.pc        := mem_reg.io.ifu_data_out.pc // pc for difftest
  reg.io.in.valid     := mem_reg.io.valid_out       // valid for difftest
  reg.io.in.reg_write := mem_reg.io.idu_contr_out.reg_write
  reg.io.in.rs_addr_hl:= mem_reg.io.idu_data_out.rs
  reg.io.in.rd_addr   := MuxLookup(Cat(mem_reg.io.idu_contr_out.mem_read, mem_reg.io.idu_contr_out.call_src), mem_reg.io.idu_data_out.rd , Array(
    1.U -> 31.U
  ))
  // mem_reg.io.exu_data_out.dest is tlb.io.out.addr (with delay)
  reg.io.in.rd_data   := Mux(mem_reg.io.idu_contr_out.mem_read,
    MuxLookup(mem_reg.io.idu_contr_out.mem_mask, 0.U, Array(
      1.U -> MuxLookup(mem_reg.io.exu_data_out.dest(1, 0), 0.U, Array(
          "b00".U -> Cat(Fill(24, mem_reg.io.mem_data_out.rdata( 7) & mem_reg.io.idu_contr_out.signed.asUInt()), mem_reg.io.mem_data_out.rdata( 7,  0)),
          "b01".U -> Cat(Fill(24, mem_reg.io.mem_data_out.rdata(15) & mem_reg.io.idu_contr_out.signed.asUInt()), mem_reg.io.mem_data_out.rdata(15,  8)),
          "b10".U -> Cat(Fill(24, mem_reg.io.mem_data_out.rdata(23) & mem_reg.io.idu_contr_out.signed.asUInt()), mem_reg.io.mem_data_out.rdata(23, 16)),
          "b11".U -> Cat(Fill(24, mem_reg.io.mem_data_out.rdata(31) & mem_reg.io.idu_contr_out.signed.asUInt()), mem_reg.io.mem_data_out.rdata(31, 24))
      )),
      2.U -> MuxLookup(mem_reg.io.exu_data_out.dest(1, 0), 0.U, Array(
          "b00".U -> Cat(Fill(16, mem_reg.io.mem_data_out.rdata(15) & mem_reg.io.idu_contr_out.signed.asUInt()), mem_reg.io.mem_data_out.rdata(15,  0)),
          "b10".U -> Cat(Fill(16, mem_reg.io.mem_data_out.rdata(31) & mem_reg.io.idu_contr_out.signed.asUInt()), mem_reg.io.mem_data_out.rdata(31, 16))
      )),
      3.U -> mem_reg.io.mem_data_out.rdata
    )),
    Mux(mem_reg.io.idu_contr_out.call_src,
      mem_reg.io.ifu_data_out.pc + 8.U,
      mem_reg.io.exu_data_out.dest
    )
  )

  // hi/lo
  reg.io.in.hi_write  := mem_reg.io.idu_contr_out.hi_write
  reg.io.in.lo_write  := mem_reg.io.idu_contr_out.lo_write
  reg.io.in.hi_read   := mem_reg.io.idu_contr_out.hi_read
  reg.io.in.lo_read   := mem_reg.io.idu_contr_out.lo_read
  reg.io.in.hilo_src  := mem_reg.io.idu_contr_out.hilo_src
  reg.io.in.hi_data   := mem_reg.io.exu_data_out.dest_hi
  reg.io.in.lo_data   := mem_reg.io.exu_data_out.dest_lo
  // cp0
  reg.io.in.cp0_read  := mem_reg.io.idu_contr_out.cp0_read
  reg.io.in.cp0_data  := mem_reg.io.exu_data_out.cp0_data

  // cp0
  cp0.io.in.write := idu_exu.io.idu_contr_out.cp0_write & exu_mem.io.valid
  cp0.io.in.addr  := idu_exu.io.idu_data_out.cp0_addr
  cp0.io.in.sel   := idu_exu.io.ifu_data_out.inst(3, 0)
  cp0.io.in.data  := reg.io.out.rt_data
  cp0.io.in.int   := io.int
  cp0.io.in.vaddr := Mux(idu_exu.io.intr_out.addrrd, idu_exu.io.ifu_data_out.pc, exu.io.out.dest)
  cp0.io.in.epc   := branch.io.out.epc
  // intr
  intr := ((cp0.io.out.ext_int  & (~cp0.io.out.exl) & (cp0.io.out.ie)) |
    idu_exu.io.intr_out.addrrd | mem_addrrd | mem_addrwt |
    exu.io.intr.exceed |
    idu_exu.io.intr_out.syscall |
    idu_exu.io.intr_out.breakpt |
    idu_exu.io.intr_out.noinst  |
    (cp0.io.out.soft_int  & (~cp0.io.out.exl) & (cp0.io.out.ie))) & idu_exu.io.valid_out
  cp0.io.intr.intr    := intr
  cp0.io.intr.branch  := branch.io.out.branch_cp0 & idu_exu.io.valid_out
  cp0.io.intr.addrrd  := (idu_exu.io.intr_out.addrrd | mem_addrrd) & idu_exu.io.valid_out
  cp0.io.intr.addrwt  := mem_addrwt                  & idu_exu.io.valid_out
  cp0.io.intr.exceed  := exu.io.intr.exceed          & idu_exu.io.valid_out
  cp0.io.intr.syscall := idu_exu.io.intr_out.syscall & idu_exu.io.valid_out
  cp0.io.intr.breakpt := idu_exu.io.intr_out.breakpt & idu_exu.io.valid_out
  cp0.io.intr.noinst  := idu_exu.io.intr_out.noinst  & idu_exu.io.valid_out
  cp0.io.intr.eret    := idu_exu.io.intr_out.eret    & idu_exu.io.valid_out
  cp0.io.intr.soft_int:= cp0.io.out.soft_int         & idu_exu.io.valid_out
  cp0.io.intr.ext_int := cp0.io.out.ext_int          & idu_exu.io.valid_out

  // branch
  // branch & jump
  branch.io.in.pc           := pc
  branch.io.in.pc_exu       := idu_exu.io.ifu_data_out.pc
  branch.io.in.pc_idu       := ifu_idu.io.ifu_data_out.pc // TODO, May hava error
  branch.io.in.branch_exu   := idu_exu.io.idu_contr_out.branch & exu_mem.io.valid
  branch.io.in.branch_delay := idu_exu.io.idu_contr_out.branch_delay
  branch.io.in.bcmp         := exu.io.out.cmp
  branch.io.in.jump         := idu_exu.io.idu_contr_out.jump   & exu_mem.io.valid
  branch.io.in.jsrc         := idu_exu.io.idu_contr_out.jsrc   & exu_mem.io.valid
  branch.io.in.imm          := idu_exu.io.idu_data_out.imm
  branch.io.in.reg          := reg.io.out.rs_data
  // intr
  branch.io.intr.intr := intr
  branch.io.intr.eret := idu_exu.io.intr_out.eret & exu_mem.io.valid
  branch.io.intr.epc  := cp0.io.out.epc

  clear_ifu := branch.io.out.branch_pc // when meeting branch clear the ifu
  pause := (((idu_exu.io.idu_data_out.rs === mem_reg.io.idu_data_out.rd) & mem_reg.io.valid_out & mem_reg.io.idu_contr_out.reg_write) | 
            ((idu_exu.io.idu_data_out.rs === exu_mem.io.idu_data_out.rd) & exu_mem.io.valid_out & exu_mem.io.idu_contr_out.reg_write) | 
            ((idu_exu.io.idu_data_out.rt === mem_reg.io.idu_data_out.rd) & mem_reg.io.valid_out & mem_reg.io.idu_contr_out.reg_write) | 
            ((idu_exu.io.idu_data_out.rt === exu_mem.io.idu_data_out.rd) & exu_mem.io.valid_out & exu_mem.io.idu_contr_out.reg_write)) & idu_exu.io.valid_out
  // next pc
  pc := Mux(branch.io.out.branch_pc, branch.io.out.pc,
    Mux(pause, pc, pc + "h4".U)
  )
}
