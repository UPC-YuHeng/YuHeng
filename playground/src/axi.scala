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
    val wid     = UInt(4.W)
    val wdata   = UInt(32.W)
    val wvalid  = Bool()
    val wlast   = Bool()
    val wen     = UInt(4.W)
  }
  class data_out extends Bundle{
    val rdata  = UInt(32.W)  
    val rid    = UInt(4.W)
    val rready = Bool()
    val wready = Bool()
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
  val rstate       = RegInit(ridle)
  val wstate       = RegInit(widle)

  io.data_out.rready := false.B
  io.data_out.wready := false.B
  io.data_out.rid    := 0.U
  io.data_out.rdata  := 0.U

  switch(rstate){
    is(ridle)
    {
      when(io.data_in.arvalid === true.B)
      {
        reg_out.araddr     := io.data_in.araddr
        reg_out.arvalid    := true.B
        reg_out.arid       := io.data_in.arid
        rstate             := sdraddr
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
        reg_out.rready     := false.B
        io.data_out.rready := true.B
        io.data_out.rid   := io.in.rid
        io.data_out.rdata := io.in.rdata
        when(io.in.rlast)
        {
          rstate := ridle
        }
      }
    }
  }

  switch(wstate)
  {
    is(widle)
    {
      when(io.data_in.awvalid === true.B)
      {
        reg_out.awvalid    := true.B
        reg_out.awaddr     := io.data_in.awaddr
        reg_out.awid       := io.data_in.awid
        wstate             := sdwaddr
      }
    }
    is(sdwaddr)
    {
      when(io.in.awready)
      {
        reg_out.awvalid := false.B
        wstate          := wrdata
      }
    }
    is(wrdata)
    {
      reg_out.wvalid := true.B
      reg_out.wlast  := true.B
      reg_out.wdata  := io.data_in.wdata
      reg_out.wid    := io.data_in.wid
      reg_out.wstrb  := io.data_in.wen
      when(io.in.wready)
      {
        wstate         := wrend
      }
    }
    is(wrend)
    {
      reg_out.wvalid := false.B
      reg_out.wlast  := false.B
      reg_out.bready := true.B
      when(io.in.bvalid === true.B)
      {
        io.data_out.wready := true.B
        reg_out.bready := false.B
        wstate         := widle
      }
    }
  }

  reg_out.arlen   := "b00000000".U
  reg_out.arburst := "b01".U
  reg_out.arlock  := "b00".U
  reg_out.arcache := "b0000".U
  reg_out.arprot  := "b000".U
  reg_out.arsize  := "b010".U
  
  reg_out.awid    := "b0001".U
  reg_out.awlen   := "b00000000".U
  reg_out.awburst := "b01".U 
  reg_out.awlock  := "b00".U 
  reg_out.awcache := "b0000".U 
  reg_out.awprot  := "b000".U 
  reg_out.awsize  := "b100".U

  reg_out.wid     := "b0001".U 
  reg_out.wlast   := true.B

  io.out      := reg_out
}