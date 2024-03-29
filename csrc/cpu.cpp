#include "cpu.h"

// ========================= Environment =========================
VerilatedContext *contextp;
Vcpu *cpu;
// =============== GtkWave ===============
#ifdef CONFIG_GTKWAVE
VerilatedVcdC *m_trace;  // trace
vluint64_t sim_time = 0; // time of gtkwave
#endif

// ========================= Debug =========================

// =============== SDB ===============

// =============== Mtrace ===============

// =============== Ftrace ===============

struct cpu_top
{
  uint32_t io_inst_sram_wen;
  uint32_t io_inst_sram_en;
  uint32_t io_inst_sram_addr;
  uint32_t io_inst_sram_wdata;
  uint32_t io_data_sram_wen;
  uint32_t io_data_sram_en;
  uint32_t io_data_sram_addr;
  uint32_t io_data_sram_wdata;
};
cpu_top top_buf[2] = {0};
int buf_count = 0;

void debug_exit(int status)
{
#ifdef CONFIG_GTKWAVE
  m_trace->close();
#endif
#ifdef CONFIG_ITRACE
  if (status != 0)
    itrace_output();
#endif
  if (status == 0)
    puts("\33[1;32mSim Result: HIT GOOD TRAP\33[0m");
  else
    puts("\33[1;31mSim Result: HIT BAD TRAP\33[0m");
  exit(status);
}

// ========================= Functions =========================

// Load image from am-kernels (Makefile -> ./image.bin)
void load_image()
{
  char image_path[] = "/home/wcxmips/ics2020/nemu/main.bin";
  FILE *fp = fopen(image_path, "rb");
  fseek(fp, 0, SEEK_END);
  img_size = ftell(fp);
  fseek(fp, 0, SEEK_SET);
  int ret = fread((mem + IMG_START), img_size, 1, fp);
  fclose(fp);
}

void rw_data()
{
  top_buf[buf_count ^ 1].io_inst_sram_wen = cpu->io_inst_sram_wen;
  top_buf[buf_count ^ 1].io_inst_sram_en = cpu->io_inst_sram_en;
  top_buf[buf_count ^ 1].io_inst_sram_addr = cpu->io_inst_sram_addr;
  top_buf[buf_count ^ 1].io_inst_sram_wdata = cpu->io_inst_sram_wdata;
  top_buf[buf_count ^ 1].io_data_sram_wen = cpu->io_data_sram_wen;
  top_buf[buf_count ^ 1].io_data_sram_en = cpu->io_data_sram_en;
  top_buf[buf_count ^ 1].io_data_sram_addr = cpu->io_data_sram_addr;
  top_buf[buf_count ^ 1].io_data_sram_wdata = cpu->io_data_sram_wdata;

  pmem_read(top_buf[buf_count].io_inst_sram_addr, (int *)&cpu->io_inst_sram_rdata);
  pmem_write(top_buf[buf_count].io_inst_sram_addr, top_buf[buf_count].io_inst_sram_wdata, top_buf[buf_count].io_inst_sram_wen);

  pmem_read(top_buf[buf_count].io_data_sram_addr, (int *)&cpu->io_data_sram_rdata);
  pmem_write(top_buf[buf_count].io_data_sram_addr, top_buf[buf_count].io_data_sram_wdata, top_buf[buf_count].io_data_sram_wen);
  buf_count ^= 1;
}
void cpu_reset()
{
  cpu->clock = 0;
  cpu->reset = 1;
  cpu->eval();
#ifdef CONFIG_GTKWAVE
  m_trace->dump(sim_time++);
#endif
  cpu->clock = 1;
  cpu->reset = 1;
  cpu->eval();
  rw_data();
#ifdef CONFIG_GTKWAVE
  m_trace->dump(sim_time++);
#endif
  cpu->reset = 0;
}

void cpu_sim_once()
{
  int t = 10;
  uint64_t last_pc = cpu_gpr[32];
  while (cpu_gpr[32] == last_pc && t >= 0)
  {
    t--;
    exec_once();
  }
  if (t == -1)
  {
    puts("pipeline exit");
    debug_exit(1);
  }
}

void exec_once()
{
#ifdef CONFIG_ITRACE
  itrace_record(cpu_gpr[32]);
#endif
  cpu->clock = 0;
  cpu->eval();
#ifdef CONFIG_GTKWAVE
  m_trace->dump(sim_time++);
#endif
  cpu->clock = 1;
  cpu->eval();
  rw_data();
#ifdef CONFIG_GTKWAVE
  m_trace->dump(sim_time++);
#endif
}

int main(int argc, char **argv, char **env)
{
  // Prepare environment
  contextp = new VerilatedContext;
  contextp->commandArgs(argc, argv);
  cpu = new Vcpu(contextp);
#ifdef CONFIG_GTKWAVE
  Verilated::traceEverOn(true);
  m_trace = new VerilatedVcdC;
  cpu->trace(m_trace, 5);
  m_trace->open("waveform.vcd");
#endif

  load_image();
  int reset_time = 10;
  while (reset_time--)
    cpu_reset();
#ifdef CONFIG_ITRACE
  init_disasm("mips32-pc-linux-gnu");
#endif
#ifdef CONFIG_DIFFTEST
  init_difftest();
#endif
  int simtime1=10000;
  while (simtime1>=0)
  {
    simtime1--;
    cpu_sim_once();
#ifdef CONFIG_DIFFTEST
    difftest_exec_once();
#endif
  }
  debug_exit(1);
  return 0;
}
