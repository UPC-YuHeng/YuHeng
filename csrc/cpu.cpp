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
  while (1)
  {
    cpu_sim_once();
#ifdef CONFIG_DIFFTEST
    difftest_exec_once();
#endif
  }
  return 0;
}
