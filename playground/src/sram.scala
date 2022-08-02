import chisel3._
import chisel3.util._

class sram_128_21 extends HasBlackBoxInline {
  val io = IO(new Bundle {
    val clka  = Input(Clock())
    val ena   = Input(Bool())
    val wea   = Input(Bool())
    val addra = Input(UInt(7.W))
    val dina  = Input(UInt(21.W))
    val douta = Output(UInt(21.W))
  })
  setInline("sram_128_21.v",
    s"""
    | module sram_128_21
    | #(parameter D_WIDTH = 21,A_WIDTH = 7)
    | (
    | input clk,
    | input csen_n,
    | input wren_n,
    | input [A_WIDTH-1:0] addr,
    | input [D_WIDTH-1 :0] wdata,
    | output reg [D_WIDTH-1 :0 ] rdata
    | );
    """.stripMargin)
}

class sram_128_32 extends HasBlackBoxInline {
  val io = IO(new Bundle {
    val clka  = Input(Clock())
    val ena   = Input(Bool())
    val wea   = Input(Bool())
    val addra = Input(UInt(7.W))
    val dina  = Input(UInt(32.W))
    val douta = Output(UInt(32.W))
  })
  setInline("sram_128_32.v",
    s"""
    | module sram_128_32
    | #(parameter D_WIDTH = 32,A_WIDTH = 7)
    | (
    | input clk,
    | input reset_n,
    | input csen_n,
    | input wren_n,
    | input [A_WIDTH-1:0] addr,
    | input [D_WIDTH-1 :0] wdata,
    | output reg [D_WIDTH-1 :0 ] rdata
    | );
    """.stripMargin)
}

