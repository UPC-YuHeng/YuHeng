import chisel3._
import chisel3.util._

class div extends Module {
  class div_in extends Bundle {
    val valid  = Bool()
    val x = UInt(32.W)
    val y = UInt(32.W)
    val signed = Bool()
  }
  class div_out extends Bundle {
    val valid  = Bool()
    val s = UInt(32.W)
    val r = UInt(32.W)
  }
  class buf_data extends Bundle {
    val ready  = Bool()
    val valid  = Bool()
    val x = UInt(64.W)
    val y = UInt(32.W)
    val s = UInt(32.W)
    val signed = Bool()
    val signx  = Bool()
    val signy  = Bool()
  }
  val io = IO(new Bundle {
    val in    = Input (new div_in())
    val out   = Output(new div_out())
    val flush = Input (Bool())
  })

  val in  = io.in
  val out = io.out
  val x   = Mux(in.signed & in.x(31), (~in.x) + 1.U, in.x)
  val y   = Mux(in.signed & in.y(31), (~in.y) + 1.U, in.y)

  val cnt = RegInit(Reg(UInt(5.W)))
  cnt := Mux(in.valid, 0.U, cnt + 1.U)

  val buf = RegInit(Reg(new buf_data()))
  val res = buf.x(63, 31).asUInt() - Cat(0.U(1.W), buf.y(31, 0)).asUInt()

  buf.ready := MuxCase(buf.ready, Array(          // low active
    in.valid  -> true.B,
    out.valid -> false.B
  ))
  buf.x := MuxCase(Cat(buf.x(62, 0), 0.U(1.W)), Array(
    io.flush          -> 0.U,
    in.valid          -> Cat(0.U(32.W), x),
    ~res(32).asBool() -> Cat(res(31, 0), buf.x(30, 0), 0.U(1.W)),
    out.valid         -> 0.U
  ))
  buf.y := MuxCase(buf.y, Array(
    io.flush  -> 0.U,
    in.valid  -> y,
    out.valid -> 0.U
  ))
  buf.signed := MuxCase(buf.signed, Array(
    io.flush  -> false.B,
    in.valid  -> in.signed,
    out.valid -> false.B
  ))
  buf.signx := MuxCase(buf.signx, Array(
    io.flush  -> false.B,
    in.valid  -> in.x(31),
    out.valid -> false.B
  ))
  buf.signy := MuxCase(buf.signy, Array(
    io.flush  -> false.B,
    in.valid  -> in.y(31),
    out.valid -> false.B
  ))

  buf.s := MuxCase(Cat(buf.s(30, 0), ~res(32)), Array(
    io.flush -> 0.U,
    in.valid -> 0.U
  ))
  buf.valid := MuxCase(buf.valid, Array(
    io.flush               -> false.B,
    (buf.ready & cnt.andR) -> true.B,
    out.valid              -> false.B
  ))

  val bufs = buf.s
  val bufr = MuxCase(buf.x(63, 32), Array(
    io.flush -> 0.U,
    in.valid -> 0.U
  ))

  out.valid := buf.valid
  out.s := Mux(buf.signed & (buf.signx ^ buf.signy), (~bufs) + 1.U, bufs)
  out.r := Mux(buf.signed & buf.signx, (~bufr) + 1.U, bufr)
}