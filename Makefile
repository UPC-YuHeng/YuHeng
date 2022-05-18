TOPNAME = cpu
PWD = $(shell pwd)
CSRC = $(PWD)/csrc
VSRC = $(PWD)/vsrc
OBJ_DIR = $(PWD)/obj_dir
CSRCDIRS = $(shell find $(CSRC) -maxdepth 5 -type d)
CSRC_CPP = $(foreach dir, $(CSRCDIRS), $(wildcard $(dir)/*.cpp))
LLVM_INCLUDE = $(shell llvm-config --cxxflags | awk '{split($$0,a," ");print a[1]}')
LLVM_LIBS = $(shell llvm-config --libs)

verilog:
	mkdir -p $(VSRC)
	mill -i __.test.runMain Elaborate -td $(VSRC)

gtkwave:
	gtkwave waveform.vcd

clean:
	-rm -rf $(VSRC)

sim:
	verilator --trace --cc --exe $(CSRC_CPP) $(VSRC)/*.v --top-module $(TOPNAME) -CFLAGS $(LLVM_INCLUDE) -LDFLAGS $(LLVM_LIBS) -LDFLAGS -ldl -CFLAGS -I$(CSRC)/include -Wno-fatal 
	make -j -C $(OBJ_DIR)/ -f V$(TOPNAME).mk V$(TOPNAME)
	$(OBJ_DIR)/V$(TOPNAME)

.PHONY: verilog clean sim gtkwave
