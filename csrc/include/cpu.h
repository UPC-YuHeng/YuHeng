// ========================= CONFIG =========================
#define CONFIG_GTKWAVE
// #define CONFIG_ITRACE
#define CONFIG_DIFFTEST

#include <cstdio>
// Verilator with trace
#include <Vcpu.h>
#include <verilated.h>
#include <verilated_vcd_c.h>
// DPI-C
#include <svdpi.h>
#include <Vcpu__Dpi.h>
#include <verilated_dpi.h>
// Difftest
#include <dlfcn.h>

typedef long long ll;

#define COLOR_RED     "\33[1;31m"
#define COLOR_GREEN   "\33[1;32m"
#define COLOR_NONE    "\33[0m"

// =============== Memory ===============
#define MEM_BASE 0x00000000
#define MEM_SIZE 512*1024*1204
#define IMG_START 0x1fc00000
extern uint8_t mem[MEM_SIZE];
extern long long img_size;
extern uint32_t *cpu_gpr;

void debug_exit(int status);
void init_difftest();
void checkregs(uint32_t *ref_regs);
void difftest_exec_once();
extern "C" void init_disasm(const char *triple);
uint8_t* cpu2mem(ll addr);
void itrace_record(uint32_t pc);
void itrace_output();