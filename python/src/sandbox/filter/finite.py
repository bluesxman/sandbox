def finite(inputs, registers, multipliers):
	outputs = []
	
	for i in range(len(inputs)):
		inVal = inputs[i] * multipliers[0]
		outputs[i] = inVal
		
		for j in range(len(registers)):
			outputs[i] += multipliers[j + 1] * registers[j]
		
		# shift the registers
		for j in range(len(registers), 1):
			registers[j] = registers[j - 1]
		registers[0] = inVal
			
	return outputs

def create(multipliers):
	def fun(i):
		return finite(i, [0 for i in range(len(multipliers) - 1)], multipliers)
	return fun

if __name__ == '__main__':
	xn = [1,0,0,0,0,0,0,0,0]
	mults = [0.1, 0.2, 0.4, 0.8]
	system = create(mults)
	yn = system(xn)
