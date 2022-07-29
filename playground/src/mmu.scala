import chisel3._
import chisel3.util._

//axi
class master_in extends Bundle{
  //request read
  val arready = Bool()

  //response read
  val rid     = UInt(4.W)
  val rdata   = UInt(32.W)
  val rresp   = UInt(2.W)
  val rlast   = Bool()
  val rvalid  = Bool()
  // request write
  val awready  = Bool()

  //write data
  val wready = Bool()

  //response write
  val bid    = UInt(4.W)
  val bresp  = UInt(2.W)
  val bvalid = Bool()
}

class master_out extends Bundle{
  //request read
  val arid    = UInt(4.W)
  val araddr  = UInt(32.W)
  val arlen   = UInt(8.W)
  val arsize  = UInt(3.W)
  val arburst = UInt(2.W)
  val arlock  = UInt(2.W)
  val arcache = UInt(4.W)
  val arprot  = UInt(3.W)
  val arvalid = Bool()
  
  //response read
  val rready  = Bool()

  //request write
  val awid    = UInt(4.W)
  val awaddr  = UInt(32.W)
  val awlen   = UInt(8.W)
  val awsize  = UInt(3.W)
  val awburst = UInt(2.W)
  val awlock  = UInt(2.W)
  val awcache = UInt(4.W)
  val awprot  = UInt(3.W)
  val awvalid = Bool()

  //write data
  val wid     = UInt(4.W)
  val wdata   = UInt(32.W)
  val wstrb   = UInt(4.W)
  val wlast   = Bool()
  val wvalid  = Bool()

  //response write
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
  val tlb_r    = Module(new tlb())
  val tlb_w    = Module(new tlb())
  val axi      = Module(new axi())
  val axi_sram = Mux(io.data_sram.en & io.data_sram.wen === 0.U, io.data_sram, io.inst_sram)

  tlb_r.io.in.addr  := axi_sram.addr
  tlb_w.io.in.addr  := io.data_sram.addr

  axi.io.data_in.arid    := Mux(io.data_sram.en & io.data_sram.wen === 0.U, "b0010".U(4.W), "b0001".U(4.W))
  axi.io.data_in.araddr  := tlb_r.io.out.addr
  axi.io.data_in.arvalid := axi_sram.en

  axi.io.data_in.awid    := "b0010".U(4.W)
  axi.io.data_in.awaddr  := tlb_w.io.out.addr
  axi.io.data_in.awvalid := io.data_sram.en & io.data_sram.wen =/= 0.U
  axi.io.data_in.wid     := "b0010".U(4.W)
  axi.io.data_in.wlast   := true.B
  axi.io.data_in.wvalid  := true.B
  axi.io.data_in.wen     := io.data_sram.wen
  axi.io.data_in.wdata   := io.data_sram.wdata
  
  io.inst_out.valid := (axi.io.data_out.rid === "b0001".U & axi.io.data_out.rready)
  io.data_out.valid := (axi.io.data_out.rid === "b0010".U & axi.io.data_out.rready & io.data_sram.wen === 0.U) | (axi.io.data_out.wready & io.data_sram.wen =/= 0.U)
  io.inst_out.rdata := Mux(axi.io.data_out.rid === "b0001".U & axi.io.data_out.rready, axi.io.data_out.rdata, 0.U(32.W))
  io.data_out.rdata := Mux(axi.io.data_out.rid === "b0010".U & axi.io.data_out.rready, axi.io.data_out.rdata, 0.U(32.W))

  axi.io.in := io.in
  io.out    := axi.io.out
}