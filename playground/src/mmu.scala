import chisel3._
import chisel3.util._

// axi
class master_in extends Bundle{
  // request read
  val arready = Bool()

  // response read
  val rid     = UInt(4.W)
  val rdata   = UInt(32.W)
  val rresp   = UInt(2.W)
  val rlast   = Bool()
  val rvalid  = Bool()
  // request write
  val awready  = Bool()

  // write data
  val wready = Bool()

  // response write
  val bid    = UInt(4.W)
  val bresp  = UInt(2.W)
  val bvalid = Bool()
}

class master_out extends Bundle{
  // request read
  val arid    = UInt(4.W)
  val araddr  = UInt(32.W)
  val arlen   = UInt(8.W)
  val arsize  = UInt(3.W)
  val arburst = UInt(2.W)
  val arlock  = UInt(2.W)
  val arcache = UInt(4.W)
  val arprot  = UInt(3.W)
  val arvalid = Bool()
  
  // response read
  val rready  = Bool()

  // request write
  val awid    = UInt(4.W)
  val awaddr  = UInt(32.W)
  val awlen   = UInt(8.W)
  val awsize  = UInt(3.W)
  val awburst = UInt(2.W)
  val awlock  = UInt(2.W)
  val awcache = UInt(4.W)
  val awprot  = UInt(3.W)
  val awvalid = Bool()

  // write data
  val wid     = UInt(4.W)
  val wdata   = UInt(32.W)
  val wstrb   = UInt(4.W)
  val wlast   = Bool()
  val wvalid  = Bool()

  // response write
  val bready  = Bool()
}

class ram_io extends Bundle {
  val en    = Bool()
  val wen   = UInt(4.W)
  val addr  = UInt(32.W)
  val wdata = UInt(32.W)
}

class mmu extends Module{
  val io = IO(new Bundle{
    val inst_sram  = Input (new ram_in())
    val data_sram  = Input (new ram_in())  
    val inst_out   = Output(new ram_out())
    val data_out   = Output(new ram_out())
    val in         = Input (new master_in())
    val out        = Output(new master_out())
  })
  val axi    = Module(new axi())
  val icache = Module(new icache())
  val dcache = Module(new dcache())
  val tlb_i = Module(new tlb())
  val tlb_d = Module(new tlb())

  val i_ucache = ((io.inst_sram.addr(31, 28) === "ha".U) | (io.inst_sram.addr(31, 28) === "hb".U))
  val d_ucache = ((io.data_sram.addr(31, 28) === "ha".U) | (io.data_sram.addr(31, 28) === "hb".U))

  tlb_i.io.in.addr := io.inst_sram.addr
  tlb_d.io.in.addr := io.data_sram.addr
  
  icache.io.cin.ucache := i_ucache
  icache.io.cin.valid  := io.inst_sram.en
  icache.io.cin.op     := false.B
  icache.io.cin.index  := tlb_i.io.out.addr(11, 5)
  icache.io.cin.tag    := tlb_i.io.out.addr(31, 12)
  icache.io.cin.offset := tlb_i.io.out.addr(4, 0)
  icache.io.cin.wstrb  := 0.U
  icache.io.cin.wdata  := 0.U
  icache.io.cin.rsize  := io.inst_sram.rsize

  io.inst_out.valid    := icache.io.cout.data_ok
  io.inst_out.rdata    := icache.io.cout.rdata

  dcache.io.cin.ucache := d_ucache
  dcache.io.cin.valid  := io.data_sram.en
  dcache.io.cin.op     := (io.data_sram.wen =/= 0.U)
  dcache.io.cin.index  := tlb_d.io.out.addr(11, 5)
  dcache.io.cin.tag    := tlb_d.io.out.addr(31, 12)
  dcache.io.cin.offset := tlb_d.io.out.addr(4, 0)
  dcache.io.cin.wstrb  := io.data_sram.wen
  dcache.io.cin.wdata  := io.data_sram.wdata
  dcache.io.cin.rsize  := io.data_sram.rsize

  io.data_out.valid    := dcache.io.cout.data_ok
  io.data_out.rdata    := dcache.io.cout.rdata

  icache.io.ain.rd_rdy := axi.io.data_out.arready & (~dcache.io.aout.rd_req) & icache.io.aout.rd_req
  dcache.io.ain.rd_rdy := axi.io.data_out.arready & dcache.io.aout.rd_req
  icache.io.ain.wr_rdy := false.B
  dcache.io.ain.wr_rdy := axi.io.data_out.awready

  val axi_sram = MuxCase(Reg(new axi_out()), Array(
    dcache.io.ain.rd_rdy -> dcache.io.aout,
    icache.io.ain.rd_rdy -> icache.io.aout
  ))

  axi.io.data_in.arid    := MuxCase(0.U, Array(
    dcache.io.ain.rd_rdy -> 2.U,
    icache.io.ain.rd_rdy -> 1.U
  ))

  axi.io.data_in.araddr  := axi_sram.rd_addr
  axi.io.data_in.arvalid := axi_sram.rd_req
  axi.io.data_in.rlen    := axi_sram.rd_len
  axi.io.data_in.rsize   := axi_sram.rd_size

  axi.io.data_in.awid    := 3.U
  axi.io.data_in.awaddr  := dcache.io.aout.wr_addr
  axi.io.data_in.awvalid := dcache.io.aout.wr_req
  axi.io.data_in.wlen    := dcache.io.aout.wr_len
  axi.io.data_in.wen     := dcache.io.aout.wr_wstrb
  axi.io.data_in.wdata   := dcache.io.aout.wr_data

  icache.io.ain.ret_last := (axi.io.data_out.rid === 1.U & axi.io.data_out.rlast)
  dcache.io.ain.ret_last := (axi.io.data_out.rid === 2.U & axi.io.data_out.rlast)

  icache.io.ain.rd_valid := (axi.io.data_out.rid === 1.U & axi.io.data_out.rready)
  dcache.io.ain.rd_valid := (axi.io.data_out.rid === 2.U & axi.io.data_out.rready)
  
  icache.io.ain.ret_rdata := Mux(axi.io.data_out.rid === 1.U & axi.io.data_out.rready, axi.io.data_out.rdata, 0.U(32.W))
  dcache.io.ain.ret_rdata := Mux(axi.io.data_out.rid === 2.U & axi.io.data_out.rready, axi.io.data_out.rdata, 0.U(32.W))

  icache.io.ain.wr_valid := axi.io.data_out.wready
  dcache.io.ain.wr_valid := axi.io.data_out.wready
  
  axi.io.in := io.in
  io.out    := axi.io.out
}