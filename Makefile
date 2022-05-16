TOPNAME = cpu
CSRC = ./csrc
VSRC = ./vsrc
OBJ_DIR = ./obj_dir

verilog:
	mkdir -p $(VSRC)
	mill -i __.test.runMain Elaborate -td $(VSRC)

gtkwave:
	gtkwave waveform.vcd

clean:
	-rm -rf $(VSRC)

.PHONY: verilog clean

sim:
	verilator --trace --cc --exe $(CSRC)/*.cpp $(VSRC)/*.v --top-module $(TOPNAME)
	make -j -C $(OBJ_DIR)/ -f V$(TOPNAME).mk V$(TOPNAME)
	$(OBJ_DIR)/V$(TOPNAME)
