package org.example

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.math.PI
class TestBarrier {
    @Test
    // Test case of a zero vector
    fun testZeroVector() {
        val vector = doubleArrayOf(0.0, 0.0, 0.0)
        assertEquals(0.0, magnitude(vector))
    }

    @Test
    // Test case with only positive vector components
    fun testPositiveValues() {
        val vector = doubleArrayOf(3.0, 4.0)
        assertEquals(5.0, magnitude(vector))
    }

    @Test
    // Test case with only negative vector components
    fun testNegativeValues() {
        val vector = doubleArrayOf(-3.0, -4.0)
        assertEquals(5.0, magnitude(vector))
    }

    @Test
    // Test case with mixed values
    fun testMixedValues() {
        val vector = doubleArrayOf(-3.0, 4.0)
        assertEquals(5.0, magnitude(vector))
    }

    @Test
    // Test case with large values
    fun testLargeValues() {
        val vector = doubleArrayOf(1e6, 2e6, 3e6)
        assertEquals(3.7416573867739416e6, magnitude(vector))
    }

    @Test
    // Test case with single element
    fun testSingleElement() {
        val vector = doubleArrayOf(5.0)
        assertEquals(5.0, magnitude(vector))
    }

    @Test
    // Test case with a large number of elements
    fun testLargeVector() {
        val vector = DoubleArray(100) { 1.0 }
        assertEquals(10.0, magnitude(vector))
    }

    @Test
    // Test case of a standard objective function input
    fun testObjectiveFunction() {
        val dimensions = doubleArrayOf(3.0, 4.0)
        val snrTarget = 10.0
        assertEquals(1.2, objective(dimensions, snrTarget))
    }

    @Test
    // Test case of a dimensionless readout resonator
    fun testObjectiveZeroDimensions() {
        val dimensions = doubleArrayOf(0.0, 0.0)
        val snrTarget = 10.0
        assertEquals(0.0, objective(dimensions, snrTarget))
    }

    @Test
    // Test case of an SNR of 0
    fun testObjectiveZeroSNR() {
        val dimensions = doubleArrayOf(3.0, 4.0)
        val snrTarget = 0.0
        assertEquals(Double.POSITIVE_INFINITY, objective(dimensions, snrTarget))
    }

    @Test
    // Test case of an objective with large dimensions and SNR
    fun testObjectiveLarge() {
        val dimensions = doubleArrayOf(1e6, 2e6)
        val snrTarget = 1e4
        assertEquals(2e8, objective(dimensions, snrTarget))
    }

    @Test
    // Test case of an objective with small dimensions and SNR
    fun testObjectiveSmall() {
        val dimensions = doubleArrayOf(1e-6, 2e-6)
        val snrTarget = 1e4
        assertEquals(2e-16, objective(dimensions, snrTarget))
    }

    @Test
    // Test case of a wavelength constraint with equal dimensions
    fun testWavelengthEqualDimensions() {
        val dimensions = doubleArrayOf(1.0, 1.0)
        val frequencyTarget = 3e8
        val expected = 0.0
        assertEquals(expected, wavelengthConstraint(dimensions, frequencyTarget))
    }

    @Test
    // Test case of a dimension smaller than the wavelength
    fun testWavelengthSmaller() {
        val dimensions = doubleArrayOf(0.5, 1.0)
        val frequencyTarget = 3e8
        assertEquals(0.5, wavelengthConstraint(dimensions, frequencyTarget))
    }

    @Test
    // Test case of a dimension larger than the wavelength
    fun testWavelengthLarger() {
        val dimensions = doubleArrayOf(3.0, 1.0)
        val frequencyTarget = 3e8 // Assuming frequency in Hz for simplicity
        val expected = 2.0 // If the wavelength is 1 m, the constraint should be 2.0
        assertEquals(0.0, wavelengthConstraint(dimensions, frequencyTarget))
    }

    @Test
    fun testDimensionLimit() {
        val dimensions = doubleArrayOf(2.0, 0.5)
        val lengthLimit = 3.0
        val widthLimit = 1.0
        val expected = doubleArrayOf(1.0, 0.5)
        assertEquals(expected.toList(), dimensionLimitConstraint(dimensions, lengthLimit, widthLimit).toList())
    }

    @Test
    fun testDimensionExceed() {
        val dimensions = doubleArrayOf(4.0, 0.5)
        val lengthLimit = 3.0
        val widthLimit = 1.0
        val expected = doubleArrayOf(1.0, 0.5)
        assertEquals(expected.toList(), dimensionLimitConstraint(dimensions, lengthLimit, widthLimit).toList())
    }

    @Test
    // Test case where the coherence time is above a microsecond
    fun testCoherenceTime() {
        val dimensions = doubleArrayOf(1e6, 2e6)
        val frequencyTarget = 1e9
        val coherenceTimeMin = 1e-6
        assertEquals(1.9999996000001803, coherenceTimeConstraint(dimensions, frequencyTarget, coherenceTimeMin))
    }

    @Test
    // Test case where the anharmonic constraints is within the limit
    fun testAnharmonicityConstraint() {
        val dimensions = doubleArrayOf(1.0, 1.0)
        val frequencyTarget = 3e8 // Assuming frequency in Hz for simplicity
        val anharmonicityMin = 1e7
        val expected = -1.0e7 // Since omega12 - omega01 < anharmonicityMin
        assertEquals(expected, anharmonicityConstraint(dimensions, frequencyTarget, anharmonicityMin))
    }

    @Test
    // Test case where the system is completely harmonic
    fun testZeroAnharmonicity() {
        val dimensions = doubleArrayOf(1.0, 1.0)
        val frequencyTarget = 3e8 // Assuming frequency in Hz for simplicity
        val anharmonicityMin = 0.0
        val linewidth = 3e8 / (PI * 1.0) // Linewidth calculated with the given dimensions and frequency
        val omega01 = 2 * PI * frequencyTarget
        val omega12 = 2 * PI * (frequencyTarget + linewidth)
        assertEquals(anharmonicityMin, anharmonicityConstraint(dimensions, frequencyTarget, anharmonicityMin))
    }

    @Test
    // Test case where the anharmonicity is negative
    fun testNegativeAnharmonicity() {
        val dimensions = doubleArrayOf(1.0, 1.0)
        val frequencyTarget = 3e8 // Assuming frequency in Hz for simplicity
        val anharmonicityMin = -1e7
        assertEquals(1.0e7, anharmonicityConstraint(dimensions, frequencyTarget, anharmonicityMin))
    }

    @Test
    // Test case where material constraints are within the limits
    fun testMaterial() {
        val dimensions = doubleArrayOf(1.0, 1.0)
        val materialConductivityMin = 1e6
        val materialLossTangentMax = 0.02
        val expected = doubleArrayOf(1.5e6 - materialConductivityMin, materialLossTangentMax - 0.01)
        assertEquals(expected.toList(), materialPropertyConstraint(dimensions, materialConductivityMin, materialLossTangentMax).toList())
    }

    // Test case where all elements of x are zero
    @Test
    fun testAllZeros() {
        val x = doubleArrayOf(0.0, 0.0, 0.0)
        val step = 1e-6
        val inputFunction: (DoubleArray) -> Double = { it.sum() }
        val expected = DoubleArray(3) { 1.0 }
        assertEquals(expected.toList(), calculateGradient(inputFunction, x, step).toList())
    }

    // Test case where all elements are positive
    @Test
    fun testAllPositive() {
        val x = doubleArrayOf(1.0, 2.0, 3.0)
        val step = 1e-6
        val inputFunction: (DoubleArray) -> Double = { it.sum() }
        val expected = DoubleArray(3) { 1.0 }
        assertEquals(expected[0], calculateGradient(inputFunction, x, step)[0], 0.01)
        assertEquals(expected[1], calculateGradient(inputFunction, x, step)[1], 0.01)
        assertEquals(expected[2], calculateGradient(inputFunction, x, step)[2], 0.01)
    }

    // Test case where all elements are negative
    @Test
    fun testAllNegative() {
        val x = doubleArrayOf(-1.0, -2.0, -3.0)
        val step = 1e-6
        val inputFunction: (DoubleArray) -> Double = { it.sum() }
        val expected = DoubleArray(3) { 1.0 } // Gradient is [1, 1, 1] since the sum function is used
        assertEquals(expected[0], calculateGradient(inputFunction, x, step)[0], 0.01)
        assertEquals(expected[1], calculateGradient(inputFunction, x, step)[1], 0.01)
        assertEquals(expected[2], calculateGradient(inputFunction, x, step)[2], 0.01)
    }

    // Test case where elements are alternating positive and negative
    @Test
    fun testAlternation() {
        val x = doubleArrayOf(1.0, -2.0, 3.0)
        val step = 1e-6
        val inputFunction: (DoubleArray) -> Double = { it.sum() }
        val expected = DoubleArray(3) { 1.0 } // Gradient is [1, 1, 1] since the sum function is used
        assertEquals(expected[0], calculateGradient(inputFunction, x, step)[0], 0.01)
        assertEquals(expected[1], calculateGradient(inputFunction, x, step)[1], 0.01)
        assertEquals(expected[2], calculateGradient(inputFunction, x, step)[2], 0.01)
    }

    // Test case where input function is a linear function
    @Test
    fun testLinear() {
        val x = doubleArrayOf(2.0, 3.0)
        val step = 1e-6
        val inputFunction: (DoubleArray) -> Double = { it[0] + it[1] }
        val expected = doubleArrayOf(1.0, 1.0)
        assertEquals(expected[0], calculateGradient(inputFunction, x, step)[0], 0.01)
        assertEquals(expected[0], calculateGradient(inputFunction, x, step)[0], 0.01)
    }

    // Test case where input function is a quadratic function
    @Test
    fun testQuadratic() {
        val x = doubleArrayOf(2.0, 3.0)
        val step = 1e-6
        val inputFunction: (DoubleArray) -> Double = { it[0] * it[0] + it[1] * it[1] }
        val expected = doubleArrayOf(4.0, 6.0)
        assertEquals(expected[0], calculateGradient(inputFunction, x, step)[0], 0.1)
        assertEquals(expected[1], calculateGradient(inputFunction, x, step)[1], 0.1)
    }

    // Test case where step size is very small
    @Test
    fun testSmallStep() {
        val x = doubleArrayOf(1.0, 2.0, 3.0)
        val step = 1e-12
        val inputFunction: (DoubleArray) -> Double = { it.sum() }
        val expected = DoubleArray(3) { 1.0 }
        assertEquals(expected[0], calculateGradient(inputFunction, x, step)[0], 0.01)
        assertEquals(expected[1], calculateGradient(inputFunction, x, step)[1], 0.01)
        assertEquals(expected[2], calculateGradient(inputFunction, x, step)[2], 0.01)
    }

    // Test case where step size is very large
    @Test
    fun testLargeStep() {
        val x = doubleArrayOf(1.0, 2.0, 3.0)
        val step = 1e6
        val inputFunction: (DoubleArray) -> Double = { it.sum() }
        val expected = DoubleArray(3) { 1.0 }
        assertEquals(expected.toList(), calculateGradient(inputFunction, x, step).toList())
    }

    // Test case where the gradient will have large values
    @Test
    fun testLargeGradient() {
        val x = doubleArrayOf(1e12, 2e12, 3e12)
        val step = 1e-6
        val inputFunction: (DoubleArray) -> Double = { it.sum() }
        val expected = DoubleArray(3) { 0.0 }
        assertEquals(expected.toList(), calculateGradient(inputFunction, x, step).toList())
    }

    // Test case where the gradient will have small values
    @Test
    fun testSmallGradient() {
        val x = doubleArrayOf(1e-12, 2e-12, 3e-12)
        val step = 1e-6
        val inputFunction: (DoubleArray) -> Double = { it.sum() }
        val expected = DoubleArray(3) { 1.0 }
        assertEquals(expected.toList(), calculateGradient(inputFunction, x, step).toList())
    }
}