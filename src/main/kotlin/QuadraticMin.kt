package org.example
import kotlin.math.*

// Relevant constants
const val SPEED_OF_LIGHT: Double = 3e8 // Speed of light in meters/second
const val REDUCED_PLANCK_CONSTANT: Double = 1.054e-34 // Reduced Planck's constant quantization

// Material consideration constants
const val TANTALUM_CONDUCTIVITY: Double = 1.5e6 // S/m (conductivity of tantalum)
const val TANTALUM_LOSS_TANGENT: Double = 0.01 // Loss tangent of tantalum

// Dimension limits for rectangular geometry
const val lengthLimit = 3.0 // Maximum length
const val widthLimit = 1.0 // Maximum width

// Operational constants
const val frequencyTarget = 5e9 // 5 GHz frequency
const val coherenceTimeMin = 1e-6 // Minimum coherence time requirement, 1 microsecond
const val anharmonicityMin = 1e7 // Minimum anharmonicity, 100 MHz

// Material property constraints
const val materialConductivityMin = 1e6 // Minimum effective conductivity (S/m)
const val materialLossTangentMax = 0.02 // Maximum effective loss tangent

/**
 * Implementation of finding the magnitude of the parameters for normalization.
 *
 * @param vector Array of doubles representing a list of values from the constraints.
 * @return The magnitude of the given constraints.
 */
fun magnitude(vector: DoubleArray): Double {
    var sum = 0.0
    for (element in vector) {
        sum += element * element
    }
    return sqrt(sum)
}

/**
 * A function intended for minimization to find an optimal solution for parameters determining the dimensions
 * of a readout resonator. We need to minimize the noise flux of the system.
 *
 * @param transReadDimensions An array of doubles representing the dimensions of a rectangular resonator in meters
 * @param snrTarget Target signal-to-noise ratio.
 * @return The value of the objective function for a given input.
 */
fun objective(transReadDimensions: DoubleArray, snrTarget: Double): Double {
    return (transReadDimensions[0]*transReadDimensions[1]) / (snrTarget)
}

/**
 * A function for the first constraint: One dimension should fit the target wavelength due to the Cooper Pair
 * oscillation which should be the size of the wavelength.
 *
 * @param transReadDimensions An array of doubles representing the dimensions of a rectangular resonator in meters.
 * @param frequencyTarget Target frequency of operation.
 * @return The constraint value for a dimension dependent on wavelength.
 */
fun wavelengthConstraint(transReadDimensions: DoubleArray, frequencyTarget: Double): Double {
    val wavelength = SPEED_OF_LIGHT / frequencyTarget
    return abs(min(transReadDimensions[0], transReadDimensions[1]) - wavelength)
}

/**
 * A function for the second constraint: Antenna dimensions of the resonator should be no greater than the specified
 * limits.
 *
 * @param transReadDimensions An array of doubles representing the dimensions of a rectangular resonator in meters
 * @param lengthLimit The maximum specified length limit.
 * @param widthLimit The maximum specified width limit.
 * @return The constraint values for length and width represented in an array of doubles.
 */
fun dimensionLimitConstraint(transReadDimensions: DoubleArray, lengthLimit: Double, widthLimit: Double): DoubleArray {
    return doubleArrayOf(abs(transReadDimensions[0] - lengthLimit), abs(transReadDimensions[1] - widthLimit))
}

/**
 * A function for the third constraint: The minimum coherence time should be no less than the value specified.
 *
 * @param transReadDimensions An array of doubles representing the dimensions of a rectangular resonator in meters
 * @param frequencyTarget Target frequency of operation.
 * @param coherenceTimeMin Minimum coherence time requirement.
 * @return The constraint value for the decoherence condition.
 */
fun coherenceTimeConstraint(transReadDimensions: DoubleArray, frequencyTarget: Double, coherenceTimeMin: Double): Double {
    val linewidth = wavelengthConstraint(transReadDimensions, frequencyTarget) / (PI * max(transReadDimensions[0], transReadDimensions[1]))
    return 1 / (PI * linewidth) - coherenceTimeMin
}

/**
 * A function for the fourth constraint: The anharmonic spacing offset should be no less than the desired anharmonicity
 *
 * @param transReadDimensions Array of doubles representing the antenna dimensions [length, width].
 * @param frequencyTarget Target frequency of operation.
 * @param anharmonicityMin Minimum anharmonicity requirement.
 * @return The constraint value for the anharmonicity.
 */
fun anharmonicityConstraint(transReadDimensions: DoubleArray, frequencyTarget: Double, anharmonicityMin: Double): Double {
    val linewidth = wavelengthConstraint(transReadDimensions, frequencyTarget) / (PI * max(transReadDimensions[0], transReadDimensions[1]))
    val omega01 = 2 * PI * frequencyTarget
    val omega12 = 2 * PI * (frequencyTarget + linewidth)
    return (omega12 - omega01) / REDUCED_PLANCK_CONSTANT - anharmonicityMin
}

/**
 * A function for the fifth constraint: The material must be able to allow current to flow efficiently with minimal
 * loss
 *
 * @param transReadDimensions An array of doubles representing the dimensions of a rectangular resonator in meters
 * @param materialConductivityMin Minimum effective conductivity.
 * @param materialLossTangentMax Maximum effective loss tangent.
 * @return The constraint values for conductivity and loss tangent of the material medium.
 */
fun materialPropertyConstraint(transReadDimensions: DoubleArray, materialConductivityMin: Double, materialLossTangentMax: Double): DoubleArray {
    val area = transReadDimensions[0] * transReadDimensions[1]
    val effectiveConductivity = TANTALUM_CONDUCTIVITY * area
    val effectiveLossTangent = TANTALUM_LOSS_TANGENT * area
    return doubleArrayOf(abs(effectiveConductivity - materialConductivityMin), abs(materialLossTangentMax - effectiveLossTangent))
}

/**
 * Barrier function for optimization that includes the restrictions and allows for exploration of the solution space.
 *
 * @param transReadDimensions An array of doubles representing the dimensions of a rectangular resonator in meters.
 * @param snrTarget Target signal-to-noise ratio.
 * @param frequencyTarget Target frequency of operation.
 * @param lengthLimit Maximum length limit.
 * @param widthLimit Maximum width limit.
 * @param coherenceTimeMin Minimum coherence time requirement.
 * @param anharmonicityMin Minimum anharmonicity requirement.
 * @param materialConductivityMin Minimum effective conductivity.
 * @param materialLossTangentMax Maximum effective loss tangent.
 * @param t Adjustable barrier parameter for tightening the solution space.
 * @return The relevant value of the barrier function that can be used iteratively in the optimization.
 */
fun barrierFunction(
    transReadDimensions: DoubleArray,
    snrTarget: Double,
    frequencyTarget: Double,
    lengthLimit: Double,
    widthLimit: Double,
    coherenceTimeMin: Double,
    anharmonicityMin: Double,
    materialConductivityMin: Double,
    materialLossTangentMax: Double,
    t: Double
): Double {
    // First calculate the input parameters for the objective function
    val obj = objective(transReadDimensions, snrTarget)

    // Next calculate the values of the constraints
    val wavelengthCons = wavelengthConstraint(transReadDimensions, frequencyTarget)
    val dimLimitCons = dimensionLimitConstraint(transReadDimensions, lengthLimit, widthLimit)
    val cohTimeCons = coherenceTimeConstraint(transReadDimensions, frequencyTarget, coherenceTimeMin)
    val anharmCons = anharmonicityConstraint(transReadDimensions, frequencyTarget, anharmonicityMin)
    val matPropCons = materialPropertyConstraint(transReadDimensions, materialConductivityMin, materialLossTangentMax)

    /** Then start the penalization process by applying a logarithm to the constraints.
     *  The constraints are combined by transmission line characteristics such as dimensions
     *  and operation mode parameters dependent on the material and quantum state.
     *  
     */
    return obj - t * (ln(wavelengthCons) * ln(dimLimitCons[0] * ln(dimLimitCons[1]) +
            ln(cohTimeCons) * ln(anharmCons) * ln(matPropCons[0]) * ln(matPropCons[1])))
}

/**
 * Calculates the effect of a perturbation to the barrier optimization.
 *
 * @param transReadDimensions An array of doubles representing the dimensions of a rectangular resonator in meters.
 * @param snrTarget Target signal-to-noise ratio.
 * @param frequencyTarget Target frequency of operation.
 * @param lengthLimit Maximum length limit.
 * @param widthLimit Maximum width limit.
 * @param coherenceTimeMin Minimum coherence time requirement.
 * @param anharmonicityMin Minimum anharmonicity requirement.
 * @param materialConductivityMin Minimum effective conductivity.
 * @param materialLossTangentMax Maximum effective loss tangent.
 * @param t Adjustable barrier parameter for tightening the solution space.
 * @param epsilon Tunable perturbation value.
 * @return The value of the barrier function with a small perturbation attached to the system.
 */
fun perturbedBarrierFunction(
    transReadDimensions: DoubleArray,
    snrTarget: Double,
    frequencyTarget: Double,
    lengthLimit: Double,
    widthLimit: Double,
    coherenceTimeMin: Double,
    anharmonicityMin: Double,
    materialConductivityMin: Double,
    materialLossTangentMax: Double,
    t: Double,
    epsilon: Double = 1e-6
): Double {
    
    // Calculate the value of the barrier input
    val barrier = barrierFunction(
        transReadDimensions,
        snrTarget,
        frequencyTarget,
        lengthLimit,
        widthLimit,
        coherenceTimeMin,
        anharmonicityMin,
        materialConductivityMin,
        materialLossTangentMax,
        t
    )
    
    // Then add the factor of a small perturbation which is a contribution dependent on each of the constraint
    // weights.
    return barrier + epsilon * magnitude(doubleArrayOf(wavelengthConstraint(transReadDimensions, frequencyTarget),
        *dimensionLimitConstraint(transReadDimensions, lengthLimit, widthLimit),
        coherenceTimeConstraint(transReadDimensions, frequencyTarget, coherenceTimeMin),
        anharmonicityConstraint(transReadDimensions, frequencyTarget, anharmonicityMin),
        *materialPropertyConstraint(transReadDimensions, materialConductivityMin, materialLossTangentMax)))
}

/**
 * Applying perturbation analysis to while handling inequality constraints to get a optimal solution
 *
 * @param perturbedBarrierFunc The perturbed barrier function.
 * @param x0 Initial guess for resonator dimensions.
 * @param maxIterations Maximum number of iterations.
 * @param epsilon Perturbation threshold.
 * @param tInitial Initial barrier parameter.
 * @return Optimal resonator dimensions satisfying all constraints.
 * @throws IllegalStateException If optimization does not satisfy the given constraints.
 */
fun performPerturbationAnalysis(
    perturbedBarrierFunc: (DoubleArray) -> Double,
    x0: DoubleArray,
    maxIterations: Int = 10000,
    epsilon: Double = 1e-20,
    tInitial: Double = 1.0
): DoubleArray {
    var t = tInitial
    for (iteration in 0 until maxIterations) {
        // Minimize the perturbed barrier function
        val result = minimizePerturbedBarrierFunction(perturbedBarrierFunc, x0, t)

        // Extract the solution and constraints
        val resonatorDimensionsOptimal = result.first
        val constraints = result.second

        // Check if all constraints are satisfied
        if (constraints.all { it >= epsilon }) {
            
            // Return the calculated resonator dimensions if true
            return resonatorDimensionsOptimal
        }

        // Decrease the barrier parameter for the next iteration
        t /= 2.71828
    }
    throw IllegalStateException("Optimization did not converge")
}

/**
 * Process for minimizing the function with the barrier method and with perturbation.
 *
 * @param perturbedBarrierFunc The perturbed barrier function.
 * @param x0 Initial guess for resonator dimensions.
 * @param t Adjustable barrier parameter.
 * @return A pair of optimal resonator dimensions and respective constraint values.
 */
fun minimizePerturbedBarrierFunction(
    perturbedBarrierFunc: (DoubleArray) -> Double,
    x0: DoubleArray,
    t: Double
): Pair<DoubleArray, DoubleArray> {
    // Gradient descent optimization
    val x = x0.copyOf()
    val alpha = REDUCED_PLANCK_CONSTANT// Learning rate (Set proportionally to the Reduced Planck's constant)
    val maxIterations = 1000
    var iterations = 0
    // Apply gradient descent until the iteration count has been reached
    while (iterations < maxIterations) {
        val gradient = calculateGradient(perturbedBarrierFunc, x)
        for (i in x.indices) {
            x[i] -= alpha * gradient[i]
        }
        iterations++
    }

    // Characteristics are scaled to the speed of light because the problem was treated like a transmission line
    // we must rescale the results to get the dimensions.
    x[0] = x[0] / SPEED_OF_LIGHT
    x[1] = x[1] / SPEED_OF_LIGHT

    // Evaluate perturbed barrier function and constraint and return said values
    val constraints = doubleArrayOf(
        wavelengthConstraint(x, frequencyTarget),
        *dimensionLimitConstraint(x, lengthLimit, widthLimit),
        coherenceTimeConstraint(x, frequencyTarget, coherenceTimeMin),
        anharmonicityConstraint(x, frequencyTarget, anharmonicityMin),
        *materialPropertyConstraint(x, materialConductivityMin, materialLossTangentMax)
    )
    return Pair(x, constraints)
}

/**
 * Calculate the gradient of a function at a given point.
 *
 * @param inputFunction The function where we want to calculate the gradient of
 * @param x The point at which to calculate the gradient.
 * @param step Step size for numerical differentiation.
 * @return The gradient of the function at the given point.
 */
fun calculateGradient(inputFunction: (DoubleArray) -> Double, x: DoubleArray, step: Double = 1e-6): DoubleArray {
    val gradient = DoubleArray(x.size)
    for (i in x.indices) {
        val xPlusEpsilon = x.copyOf()
        val xMinusEpsilon = x.copyOf()
        xPlusEpsilon[i] += step
        xMinusEpsilon[i] -= step
        gradient[i] = (inputFunction(xPlusEpsilon) - inputFunction(xMinusEpsilon)) / (2 * step)
    }
    return gradient
}

fun main() {
    // Initial guess for resonator dimensions [length, width]
    val x0 = doubleArrayOf(1e-6, 1.25e-6) // Initial dimensions, arbitrary

    // Target SNR
    val snrTarget = 10000.0

    // Perform optimization with perturbation analysis
    val optimalDimensions = performPerturbationAnalysis({ perturbedBarrierFunction(it, snrTarget, frequencyTarget, lengthLimit, widthLimit, coherenceTimeMin, anharmonicityMin, materialConductivityMin, materialLossTangentMax, 1.0) }, x0)

    // Print the optimal solution
    println("Optimal antenna dimensions:")
    println("Length: ${optimalDimensions[0]} m")
    println("Width: ${optimalDimensions[1]} m")
}
