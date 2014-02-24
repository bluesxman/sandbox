'''
Created on Feb 25, 2013

@author: jwn
'''
import cmath  # math functions for complex numbers
import math
# matplotlib depends on having "NumPy" installed: http://www.lfd.uci.edu/~gohlke/pythonlibs/
import matplotlib.pyplot as plot  # http://matplotlib.org/users/installing.html

# transform to the frequency domain as a tuple from 0 to half the sample rate
# "column" 0 is frequency (in Hz)
# "column" 1 is amplitude (relative?)
def analyze_spectrum(input_samples, sample_rate):
    if math.log(len(input_samples), 2) % 1 > 0.0:
        raise ValueError('Number of samples is not a power of 2: ' + len.input_samples)
    fft = __ditfft2(input_samples, len(input_samples))
    amplitude = []  
    frequency = []
    phase = []
    for i in range(0, len(input_samples) / 2 - 1):  # use the first half of the real component of the fft result
        real = fft[i].real
        imag = fft[i].imag
        amplitude.append(math.sqrt(real * real + imag * imag))
        frequency.append(i * sample_rate / len(input_samples))
        phase.append(math.atan2(imag, real))
    return (frequency, amplitude, phase) # frequency = x-axis, amplitude = y-axis

# radix-2 decimation-in-time FFT version of Cooley-Tukey algorithm
# http://en.wikipedia.org/wiki/Cooley%E2%80%93Tukey_FFT_algorithm#The_radix-2_DIT_case
#
# the leading underscores indicates private method by convention
def __ditfft2(x, N):  #DFT of (x0, xs, x2s, ..., x(N-1)s)
    if N == 1:
        return [x[0]]   #one-element list
    else:
        # divide and conquer the source array
        # creating copies of the array.  better ways of doing this
        result = __ditfft2(x[::2], N / 2)    #DFT of (x0, x2s, x4s, ...)
        result2 = __ditfft2(x[1::2], N / 2)  #DFT of (xs, xs+2s, xs+4s, ...)
        result.extend(result2)
        for k in range(0, N / 2 - 1):  # combine DFTs of two halves into full DFT
            t = result[k]
            u = cmath.exp(-2 * cmath.pi * 1j * k / N) * result[k + N / 2]  # e^(-2 * pi * i * k / N) * Xk+N/2
            result[k] = t + u
            result[k + N / 2] = t - u
        return result

# creates a sine wave as a *function* where the output is a complex number
def create_sine(freq, amplitude, phase):
    return lambda t: cmath.sin(2 * cmath.pi * freq * t + phase) * amplitude

# given the component sines, generate a sampling of the total waveform
def sample_waveform(components, time, n_samples):
    waveform = []
    for i in range(0, int(n_samples)): # runtime error without the int() defining range
        waveform.append(0.0)
        for component in components:
            waveform[i] += component(time * i / n_samples) # does float math since n_samples is a float
    return waveform

if __name__ == '__main__':
    components = [create_sine(121, 1, .333), 
                  create_sine(178,1,0), 
                  create_sine(23, 5, 2), 
                  create_sine(423, 1, 0), 
                  create_sine(19, 2, 1), 
                  create_sine(2019, 3.19, 1.77), 
                  create_sine(301, 0.05, 0)]
    
    time = 5
    samples = math.pow(2, 18)
    sample_rate = samples / time
    waveform = sample_waveform(components, time, samples)
    (freq, amp, phase) = analyze_spectrum(waveform, sample_rate)
    
    plot.plot(waveform)
    plot.suptitle("Time domain")
    plot.show()
    
    plot.plot(freq, amp)
    plot.suptitle("Frequency domain")
    plot.show()
    
    plot.plot(freq[0:1000], phase[0:1000])
    plot.suptitle("Phase")
    plot.show()
    
    

    


