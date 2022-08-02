import chisel3._
import chisel3.util._

class axi extends Module {
  class data_in extends Bundle{
    val arid    = UInt(4.W)
    val araddr  = UInt(32.W)
    val arvalid = Bool()
    val awid    = UInt(4.W)
    val awaddr  = UInt(32.W)
    val awvalid = Bool()
    val rlen    = UInt(5.W)
    val wdata   = UInt(256.W)
    val wlen    = UInt(5.W)
    val wen     = UInt(4.W)
  }
  class data_out extends Bundle{
    val arready = Bool()
    val awready = Bool()
    val rid     = UInt(4.W)
    val rready  = Bool()
    val rlast   = Bool()
    val rdata   = UInt(32.W)  
    val wready  = Bool()
  }
  val io = IO (new Bundle {
    val data_in   = Input(new data_in())
    val data_out  = Output(new data_out())
    val in        = Input(new master_in())
    val out       = Output(new master_out())
  })
  val reg_out = RegInit(Reg(new master_out))
  
  val ridle :: sdraddr :: rddata :: nullr = Enum(4)
  val widle :: sdwaddr :: wrdata :: wrend :: nullw = Enum(5)
  val rstate        = RegInit(ridle)
  val wstate        = RegInit(widle)

  val reg_write_num = RegInit(0.U(5.W))
  val reg_wdata     = RegInit(VecInit(Seq.fill(8)(0.U(32.W))))
  val reg_wlen      = RegInit(0.U(5.W))
  val reg_wen       = RegInit(0.U(4.W))
 
  io.data_out.arready := (rstate === ridle)
  io.data_out.awready := (wstate === widle)
  io.data_out.rready  := false.B
  io.data_out.wready  := false.B
  io.data_out.rid     := 0.U
  io.data_out.rdata   := 0.U
  io.data_out.rlast   := false.B

  switch(rstate){
    is(ridle)
    {
      when(io.data_in.arvalid === true.B)
      {
        reg_out.araddr   := io.data_in.araddr
        reg_out.arvalid  := true.B
        reg_out.arid     := io.data_in.arid
        reg_out.arlen    := io.data_in.rlen
        rstate           := sdraddr
      }
    }
    is(sdraddr){
      when(io.in.arready)
      {
        reg_out.arvalid := false.B
        rstate          := rddata 
      }
    }
    is(rddata)
    {
      reg_out.rready := true.B
      when(io.in.rvalid)
      {
        io.data_out.rready := true.B
        io.data_out.rid    := io.in.rid
        io.data_out.rdata  := io.in.rdata
        when(io.in.rlast)
        {
          reg_out.rready     := false.B
          io.data_out.rlast := true.B
          rstate := ridle
        }
      }
    }
  }

  switch(wstate)
  {
    is(widle)
    {
      io.data_out.wready := true.B
      when(io.data_in.awvalid === true.B)
      {
        reg_out.awvalid    := true.B
        reg_out.awaddr     := io.data_in.awaddr
        reg_out.awid       := io.data_in.awid
        reg_out.awlen      := io.data_in.wlen
        reg_wlen           := io.data_in.wlen
        reg_wen            := io.data_in.wen
        for(i <- 0 to 7) {
          reg_wdata(i) := io.data_in.wdata(i * 32 + 31, i * 32);
        }
        wstate             := sdwaddr
      }
    }
    is(sdwaddr)
    {
      when(io.in.awready)
      {
        reg_out.awvalid := false.B
        reg_write_num   := 0.U
        wstate          := wrdata
      }
    }
    is(wrdata)
    {
      reg_out.wvalid := true.B
      reg_out.wlast  := (reg_write_num === reg_wlen)
      reg_out.wdata  := reg_wdata(reg_write_num)
      reg_out.wid    := reg_out.awid
      reg_out.wstrb  := reg_wen
      when(io.in.wready)
      {
        when(reg_write_num === reg_wlen) {
          wstate         := wrend
        }
        reg_write_num  := reg_write_num + 1.U
      }
    }
    is(wrend)
    {
      reg_out.wlast  := false.B
      reg_out.bready := true.B
      reg_out.wvalid := false.B
      when(io.in.bvalid)
      {
        reg_out.bready := false.B
        wstate         := widle
      }
    }
  }

  reg_out.arburst := "b01".U
  reg_out.arlock  := "b00".U
  reg_out.arcache := "b0000".U
  reg_out.arprot  := "b000".U
  reg_out.arsize  := "b010".U

  reg_out.awburst := "b01".U 
  reg_out.awlock  := "b00".U 
  reg_out.awcache := "b0000".U 
  reg_out.awprot  := "b000".U 
  reg_out.awsize  := "b010".U

  io.out      := reg_out
}