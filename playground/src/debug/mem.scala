package yuheng.debug

import chisel3._
import chisel3.util._

class mem extends HasBlackBoxInline {
  val io = IO(new Bundle {
    val ren   = Input(Bool())
    val wen   = Input(Bool())
    val raddr = Input(UInt(32.W))
    val rdata = Output(UInt(32.W))
    val waddr = Input(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val mask  = Input(UInt(4.W))
  })
  setInline("mem.v",
    s"""
    |import "DPI-C" function void pmem_read(
    |  input int raddr, output int rdata);
    |import "DPI-C" function void pmem_write(
    |  input int waddr, input int wdata, input byte mask);
    |module mem(ren, wen, raddr, rdata, waddr, wdata, mask);
    |  input             ren;
    |  input             wen;
    |  input  reg [31:0] raddr;
    |  output reg [31:0] rdata;
    |  input  reg [31:0] waddr;
    |  input  reg [31:0] wdata;
    |  input  reg [3:0]  mask;
    |  always @(*) begin
    |    if (ren) pmem_read(raddr, rdata);
    |    else rdata = 32'b0;
    |    if (wen) pmem_write(waddr, wdata, mask);
    |  end
    |endmodule
    """.stripMargin)
}